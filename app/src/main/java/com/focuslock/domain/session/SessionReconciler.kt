package com.focuslock.domain.session

import com.focuslock.domain.enforcement.EnforcementStateStore
import com.focuslock.domain.model.EnforcementMode
import com.focuslock.domain.model.FocusSession
import com.focuslock.domain.model.SessionEvent
import com.focuslock.domain.model.SessionStatus
import com.focuslock.domain.repository.EventLogRepository
import com.focuslock.domain.repository.SessionRepository
import com.focuslock.domain.time.TimeAuthority
import com.focuslock.enforcement.EnforcementBackendProvider
import com.focuslock.enforcement.EnforcementBackendType
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Drives enforcement toward the desired state described by the active session.
 *
 * The database is the source of truth; the backend is reconciled *toward*
 * desired state. This operation is idempotent: running it N times produces the
 * same result as running it once.
 */
@Singleton
class SessionReconciler @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val sessionClock: SessionClock,
    private val backendProvider: EnforcementBackendProvider,
    private val scheduler: SessionScheduler,
    private val notifier: SessionNotifier,
    private val eventLog: EventLogRepository,
    private val timeAuthority: TimeAuthority,
    private val policyManager: TemporaryPolicyManager,
    private val recoveryKeyManager: RecoveryKeyManager,
    private val stateStore: EnforcementStateStore,
    private val lock: SessionLock,
) {

    suspend fun reconcile() = lock.mutex.withLock { reconcileLocked() }

    private suspend fun reconcileLocked() {
        val session = sessionRepository.getActiveSession()
        if (session == null) {
            ensureNoUnexpectedEnforcement()
            return
        }

        when (session.status) {
            SessionStatus.ACTIVATING -> cleanupInterruptedActivation(session)
            SessionStatus.EXPIRING -> finishExpiry(session)
            SessionStatus.ACTIVE -> reconcileActive(session)
            else -> Unit
        }
    }

    /**
     * Emergency release: ends the active session only when the full recovery
     * key is provided. This is deliberately not a one-tap confirmation.
     */
    suspend fun emergencyRelease(recoveryKey: String): EmergencyReleaseResult =
        lock.mutex.withLock {
            if (!recoveryKeyManager.verify(recoveryKey)) return@withLock EmergencyReleaseResult.InvalidKey
            val session = sessionRepository.getActiveSession()
                ?: return@withLock EmergencyReleaseResult.NoActiveSession
            completeSession(session, SessionEvent.TYPE_SESSION_EMERGENCY_RELEASED)
            EmergencyReleaseResult.Released
        }

    private suspend fun reconcileActive(session: FocusSession) {
        if (sessionClock.isExpired(session)) {
            finishExpiry(session)
            return
        }

        // Ensure packages are blocked (idempotent re-apply).
        val backend = backendProvider.backendFor(session.enforcementMode)
        val result = backend.suspendPackages(session.blockedPackagesSnapshot)
        stateStore.recordSuspended(session.enforcementMode, result.successful)
        if (!result.isComplete &&
            backend.backendType() == EnforcementBackendType.DEVICE_OWNER
        ) {
            // OS-level suspension can genuinely be retried. Accessibility only
            // enforces while its service is running, so retrying there cannot
            // help (and would needlessly wake the device).
            log(SessionEvent.TYPE_RECONCILIATION, session.id, "re-suspend incomplete, retrying")
            scheduler.scheduleRetry(session.id, RETRY_DELAY_MS)
        }
        policyManager.apply(session)
        notifier.showActive(session)
        scheduler.scheduleExpiry(session)
    }

    private suspend fun cleanupInterruptedActivation(session: FocusSession) {
        // An activation that never completed must not leave packages suspended.
        backendProvider.backendFor(session.enforcementMode)
            .resumePackages(session.blockedPackagesSnapshot)
        stateStore.clearSuspended(session.enforcementMode, session.blockedPackagesSnapshot)
        policyManager.restoreAll()
        updateStatus(session, SessionStatus.FAILED)
        scheduler.cancel(session.id)
        notifier.cancel()
    }

    private suspend fun finishExpiry(session: FocusSession) {
        completeSession(session, SessionEvent.TYPE_SESSION_COMPLETED)
    }

    private suspend fun completeSession(session: FocusSession, completionEventType: String) {
        var current = session
        if (current.status == SessionStatus.ACTIVE &&
            updateStatus(current, SessionStatus.EXPIRING)
        ) {
            current = current.copy(status = SessionStatus.EXPIRING)
        }

        val result = backendProvider.backendFor(current.enforcementMode)
            .resumePackages(current.blockedPackagesSnapshot)
        if (!result.isComplete) {
            // Never claim completion while apps are still locked. Keep the
            // session (and its snapshot) EXPIRING so reconciliation retries.
            log(
                SessionEvent.TYPE_RECONCILIATION,
                current.id,
                "unlock incomplete for ${result.failed.size} package(s), retrying",
            )
            scheduler.scheduleRetry(current.id, RETRY_DELAY_MS)
            return
        }

        stateStore.clearSuspended(current.enforcementMode, current.blockedPackagesSnapshot)
        policyManager.restore(current)
        scheduler.cancel(current.id)
        notifier.cancel()
        updateStatus(current, SessionStatus.COMPLETED)
        log(completionEventType, current.id, current.profileNameSnapshot)
    }

    /**
     * With no active session, nothing FocusLock applied should still be in
     * effect. This catches state left behind by a crash or a failed expiry so
     * suspended packages can never become permanently unreachable.
     */
    private suspend fun ensureNoUnexpectedEnforcement() {
        EnforcementMode.entries.forEach { mode ->
            val stillSuspended = stateStore.suspendedPackages(mode)
            if (stillSuspended.isEmpty()) return@forEach

            val result = backendProvider.backendFor(mode).resumePackages(stillSuspended)
            stateStore.clearSuspended(mode, result.successful)
            if (result.failed.isEmpty()) {
                log(
                    SessionEvent.TYPE_RECONCILIATION,
                    null,
                    "released ${stillSuspended.size} orphaned package(s) [${mode.name}]",
                )
            } else {
                log(
                    SessionEvent.TYPE_RECONCILIATION,
                    null,
                    "could not release ${result.failed.size} orphaned package(s) [${mode.name}], retrying",
                )
                scheduler.scheduleRetry(CLEANUP_RETRY_ID, RETRY_DELAY_MS)
            }
        }
        policyManager.restoreAll()
    }

    private suspend fun updateStatus(session: FocusSession, to: SessionStatus): Boolean {
        if (!SessionStateMachine.canTransition(session.status, to)) {
            log(SessionEvent.TYPE_RECONCILIATION, session.id, "illegal transition ${session.status} -> $to")
            return false
        }
        sessionRepository.updateStatus(session.id, to)
        return true
    }

    private suspend fun log(type: String, sessionId: UUID?, detail: String?) {
        eventLog.log(
            SessionEvent(
                timestamp = timeAuthority.now(),
                type = type,
                sessionId = sessionId,
                detail = detail,
            )
        )
    }

    private companion object {
        const val RETRY_DELAY_MS = 5_000L

        /** Stable id so repeated cleanup retries coalesce into one alarm. */
        val CLEANUP_RETRY_ID: UUID = UUID(0L, 0L)
    }
}
