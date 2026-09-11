package com.focuslock.domain.session

import com.focuslock.domain.model.EnforcementMode
import com.focuslock.domain.model.FocusProfile
import com.focuslock.enforcement.FakeEnforcementBackend
import com.focuslock.testutil.FakeEnforcementStateStore
import com.focuslock.testutil.FakeEventLog
import com.focuslock.testutil.FakeNotifier
import com.focuslock.testutil.FakeRecoveryKeyManager
import com.focuslock.testutil.FakeScheduler
import com.focuslock.testutil.FakeSessionRepository
import com.focuslock.testutil.FakeTemporaryPolicyManager
import com.focuslock.testutil.FakeTimeAuthority
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.util.UUID

class EmergencyReleaseTest {

    private fun activeSession() = com.focuslock.domain.model.FocusSession(
        id = UUID.randomUUID(),
        profileId = null,
        profileNameSnapshot = "Deep Work",
        startedAtWallClock = Instant.parse("2026-01-01T20:00:00Z"),
        startedAtElapsedRealtimeMs = 1_000L,
        expiresAtWallClock = Instant.parse("2026-01-02T04:00:00Z"),
        duration = Duration.ofHours(8),
        blockedPackagesSnapshot = setOf("a", "b"),
        enforcementMode = EnforcementMode.SOFT,
        fortressModeEnabled = false,
        status = com.focuslock.domain.model.SessionStatus.ACTIVE,
    )

    private fun buildReconciler(
        backend: FakeEnforcementBackend,
        repo: FakeSessionRepository,
        recovery: FakeRecoveryKeyManager,
    ) = SessionReconciler(
        sessionRepository = repo,
        sessionClock = SessionClock(FakeTimeAuthority(elapsedMillis = 1_000L)),
        backendProvider = com.focuslock.enforcement.EnforcementBackendProvider { backend },
        scheduler = FakeScheduler(),
        notifier = FakeNotifier(),
        eventLog = FakeEventLog(),
        timeAuthority = FakeTimeAuthority(elapsedMillis = 1_000L),
        policyManager = FakeTemporaryPolicyManager(),
        recoveryKeyManager = recovery,
        stateStore = FakeEnforcementStateStore(),
        lock = SessionLock(),
    )

    @Test
    fun `invalid key does not release`() = runTest {
        val backend = FakeEnforcementBackend()
        backend.suspendPackages(setOf("a", "b"))
        val repo = FakeSessionRepository()
        repo.createSession(activeSession())
        val recovery = FakeRecoveryKeyManager(validKey = "correct-key")
        val reconciler = buildReconciler(backend, repo, recovery)

        val result = reconciler.emergencyRelease("wrong-key")

        assertThat(result).isEqualTo(EmergencyReleaseResult.InvalidKey)
        assertThat(backend.suspendedPackages).containsExactly("a", "b")
        assertThat(repo.getActiveSession()).isNotNull()
    }

    @Test
    fun `valid key releases the active session`() = runTest {
        val backend = FakeEnforcementBackend()
        backend.suspendPackages(setOf("a", "b"))
        val repo = FakeSessionRepository()
        val session = activeSession()
        repo.createSession(session)
        val recovery = FakeRecoveryKeyManager(validKey = "correct-key")
        val reconciler = buildReconciler(backend, repo, recovery)

        val result = reconciler.emergencyRelease("correct-key")

        assertThat(result).isEqualTo(EmergencyReleaseResult.Released)
        assertThat(backend.suspendedPackages).isEmpty()
        assertThat(repo.sessions.single().status)
            .isEqualTo(com.focuslock.domain.model.SessionStatus.COMPLETED)
        assertThat(repo.getActiveSession()).isNull()
    }

    @Test
    fun `no active session returns no active session`() = runTest {
        val reconciler = buildReconciler(
            FakeEnforcementBackend(),
            FakeSessionRepository(),
            FakeRecoveryKeyManager(validKey = "correct-key"),
        )

        val result = reconciler.emergencyRelease("correct-key")

        assertThat(result).isEqualTo(EmergencyReleaseResult.NoActiveSession)
    }
}
