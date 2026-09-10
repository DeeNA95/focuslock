package com.focuslock.domain.session

import com.focuslock.domain.model.FocusSession
import com.focuslock.domain.model.SessionEvent
import com.focuslock.domain.model.SessionStatus
import com.focuslock.domain.repository.EventLogRepository
import com.focuslock.domain.repository.SessionRepository
import com.focuslock.domain.time.TimeAuthority
import com.focuslock.enforcement.EnforcementBackendProvider
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
) {

    suspend fun reconcile() {
        val session = sessionRepository.getActiveSession() ?: return

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
    suspend fun emergencyRelease(recoveryKey: String): EmergencyReleaseResult {
        if (!recoveryKeyManager.verify(recoveryKey)) return EmergencyReleaseResult.InvalidKey
        val session = sessionRepository.getActiveSession()
            ?: return EmergencyReleaseResult.NoActiveSession
        completeSession(session, SessionEvent.TYPE_SESSION_EMERGENCY_RELEASED)
        return EmergencyReleaseResult.Released
    }

    private suspend fun reconcileActive(session: FocusSession) {
        if (sessionClock.isExpired(session)) {
            finishExpiry(session)
        } else {
            // Ensure packages are blocked (idempotent re-apply).
            backendProvider.backendFor(session.enforcementMode)
                .suspendPackages(session.blockedPackagesSnapshot)
            policyManager.apply(session)
            notifier.showActive(session)
            scheduler.scheduleExpiry(session)
        }
    }

    private suspend fun cleanupInterruptedActivation(session: FocusSession) {
        // An activation that never completed must not leave packages suspended.
        backendProvider.backendFor(session.enforcementMode)
            .resumePackages(session.blockedPackagesSnapshot)
        policyManager.restore(session)
        sessionRepository.updateStatus(session.id, SessionStatus.FAILED)
        scheduler.cancel(session.id)
        notifier.cancel()
    }

    private suspend fun finishExpiry(session: FocusSession) {
        completeSession(session, SessionEvent.TYPE_SESSION_COMPLETED)
    }

    private suspend fun completeSession(session: FocusSession, completionEventType: String) {
        sessionRepository.updateStatus(session.id, SessionStatus.EXPIRING)
        backendProvider.backendFor(session.enforcementMode)
            .resumePackages(session.blockedPackagesSnapshot)
        policyManager.restore(session)
        scheduler.cancel(session.id)
        notifier.cancel()
        sessionRepository.updateStatus(session.id, SessionStatus.COMPLETED)
        log(completionEventType, session.id, session.profileNameSnapshot)
    }

    private suspend fun log(type: String, sessionId: UUID, detail: String?) {
        eventLog.log(
            SessionEvent(
                timestamp = timeAuthority.now(),
                type = type,
                sessionId = sessionId,
                detail = detail,
            )
        )
    }
}
