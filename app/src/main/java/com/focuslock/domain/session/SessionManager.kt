package com.focuslock.domain.session

import com.focuslock.domain.enforcement.EnforcementStateStore
import com.focuslock.domain.model.ActivationDecision
import com.focuslock.domain.model.FocusProfile
import com.focuslock.domain.model.FocusSession
import com.focuslock.domain.model.SessionEvent
import com.focuslock.domain.model.SessionStatus
import com.focuslock.domain.repository.EventLogRepository
import com.focuslock.domain.repository.SessionRepository
import com.focuslock.domain.rules.RuleEngine
import com.focuslock.domain.safety.SafetyPolicy
import com.focuslock.domain.time.TimeAuthority
import com.focuslock.enforcement.EnforcementBackendProvider
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns a [FocusProfile] into an immutable [FocusSession] and applies
 * enforcement transactionally.
 *
 * A session is never persisted as ACTIVE until enforcement has been verified.
 * Activation is serialised with reconciliation through [SessionLock] so a
 * concurrent reconcile can never observe (and roll back) a half-applied
 * activation.
 */
@Singleton
class SessionManager @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val ruleEngine: RuleEngine,
    private val safetyPolicy: SafetyPolicy,
    private val timeAuthority: TimeAuthority,
    private val backendProvider: EnforcementBackendProvider,
    private val scheduler: SessionScheduler,
    private val notifier: SessionNotifier,
    private val eventLog: EventLogRepository,
    private val policyManager: TemporaryPolicyManager,
    private val stateStore: EnforcementStateStore,
    private val lock: SessionLock,
) {

    suspend fun activate(profile: FocusProfile): SessionActivationResult =
        lock.mutex.withLock { activateLocked(profile) }

    private suspend fun activateLocked(profile: FocusProfile): SessionActivationResult {
        if (sessionRepository.getActiveSession() != null) {
            return SessionActivationResult.Failed(ActivationFailure.SESSION_ALREADY_ACTIVE)
        }

        when (ruleEngine.canActivate(profile)) {
            ActivationDecision.ProfileDisabled ->
                return SessionActivationResult.Failed(ActivationFailure.PROFILE_DISABLED)
            ActivationDecision.OutsideWindow ->
                return SessionActivationResult.Failed(ActivationFailure.OUTSIDE_ALLOWED_WINDOW)
            ActivationDecision.Allowed -> Unit
        }

        if (!safetyPolicy.validateBlockList(profile.blockedPackages).safe) {
            return SessionActivationResult.Failed(ActivationFailure.UNSAFE_CONFIGURATION)
        }

        val backend = backendProvider.backendFor(profile.enforcementMode)
        if (!backend.isAvailable()) {
            return SessionActivationResult.Failed(ActivationFailure.ENFORCEMENT_UNAVAILABLE)
        }

        val session = buildSnapshot(profile, SessionStatus.ACTIVATING)
        sessionRepository.createSession(session)
        log(SessionEvent.TYPE_SESSION_CREATED, session.id, profile.name)

        val result = backend.suspendPackages(session.blockedPackagesSnapshot)
        if (!result.isComplete) {
            // Roll back anything we did suspend; never leave a partial lock.
            backend.resumePackages(result.successful)
            stateStore.clearSuspended(profile.enforcementMode, result.successful)
            updateStatus(session, SessionStatus.FAILED)
            log(SessionEvent.TYPE_SESSION_FAILED, session.id, profile.name)
            return SessionActivationResult.Failed(ActivationFailure.ENFORCEMENT_FAILURE)
        }

        val active = session.copy(status = SessionStatus.ACTIVE)
        updateStatus(session, SessionStatus.ACTIVE)
        stateStore.recordSuspended(profile.enforcementMode, session.blockedPackagesSnapshot)
        scheduler.scheduleExpiry(active)
        notifier.showActive(active)
        policyManager.apply(active)
        log(SessionEvent.TYPE_SESSION_ACTIVE, session.id, profile.name)
        return SessionActivationResult.Activated(active)
    }

    private fun buildSnapshot(profile: FocusProfile, status: SessionStatus): FocusSession {
        val now = timeAuthority.now()
        val duration = profile.duration
        return FocusSession(
            id = UUID.randomUUID(),
            profileId = profile.id,
            profileNameSnapshot = profile.name,
            startedAtWallClock = now,
            startedAtElapsedRealtimeMs = timeAuthority.elapsedRealtimeMillis(),
            startedAtBootId = timeAuthority.bootId(),
            expiresAtWallClock = now.plus(duration),
            duration = duration,
            blockedPackagesSnapshot = profile.blockedPackages,
            enforcementMode = profile.enforcementMode,
            fortressModeEnabled = profile.fortressModeEnabled,
            openBehavior = profile.openBehavior,
            enableDnd = profile.enableDnd,
            mottoSnapshot = profile.motto,
            status = status,
        )
    }

    private suspend fun updateStatus(session: FocusSession, to: SessionStatus): Boolean {
        if (!SessionStateMachine.canTransition(session.status, to)) {
            log(SessionEvent.TYPE_RECONCILIATION, session.id, "illegal transition ${session.status} -> $to")
            return false
        }
        sessionRepository.updateStatus(session.id, to)
        return true
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
