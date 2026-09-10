package com.focuslock.domain.session

import com.focuslock.domain.model.EnforcementMode
import com.focuslock.domain.model.FocusSession
import com.focuslock.domain.model.SessionStatus
import com.focuslock.enforcement.EnforcementBackendProvider
import com.focuslock.enforcement.FakeEnforcementBackend
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

class SessionReconcilerTest {

    private fun session(
        startedAtElapsedMs: Long,
        duration: Duration = Duration.ofHours(8),
        packages: Set<String> = setOf("a", "b"),
        status: SessionStatus = SessionStatus.ACTIVE,
    ) = FocusSession(
        id = UUID.randomUUID(),
        profileId = null,
        profileNameSnapshot = "Deep Work",
        startedAtWallClock = Instant.parse("2026-01-01T20:00:00Z"),
        startedAtElapsedRealtimeMs = startedAtElapsedMs,
        expiresAtWallClock = Instant.parse("2026-01-02T04:00:00Z"),
        duration = duration,
        blockedPackagesSnapshot = packages,
        enforcementMode = EnforcementMode.SOFT,
        fortressModeEnabled = false,
        status = status,
    )

    private fun buildReconciler(
        time: FakeTimeAuthority,
        backend: FakeEnforcementBackend,
        repo: FakeSessionRepository,
        scheduler: FakeScheduler = FakeScheduler(),
        notifier: FakeNotifier = FakeNotifier(),
        events: FakeEventLog = FakeEventLog(),
    ) = SessionReconciler(
        sessionRepository = repo,
        sessionClock = SessionClock(time),
        backendProvider = EnforcementBackendProvider { backend },
        scheduler = scheduler,
        notifier = notifier,
        eventLog = events,
        timeAuthority = time,
        policyManager = FakeTemporaryPolicyManager(),
        recoveryKeyManager = FakeRecoveryKeyManager(),
    )

    @Test
    fun `active session not expired re-suspends packages and schedules expiry`() = runTest {
        val time = FakeTimeAuthority(elapsedMillis = 1_000L)
        val backend = FakeEnforcementBackend()
        val repo = FakeSessionRepository()
        val scheduler = FakeScheduler()
        val reconciler = buildReconciler(time, backend, repo, scheduler = scheduler)

        repo.createSession(session(startedAtElapsedMs = 1_000L, packages = setOf("a", "b")))
        reconciler.reconcile()

        assertThat(backend.suspendedPackages).containsExactly("a", "b")
        assertThat(scheduler.scheduled).hasSize(1)
        assertThat(repo.getActiveSession()?.status).isEqualTo(SessionStatus.ACTIVE)
    }

    @Test
    fun `expired session resumes packages and completes`() = runTest {
        val time = FakeTimeAuthority(elapsedMillis = 1_000L)
        val backend = FakeEnforcementBackend()
        backend.suspendPackages(setOf("a", "b"))
        val repo = FakeSessionRepository()
        val scheduler = FakeScheduler()
        val reconciler = buildReconciler(time, backend, repo, scheduler = scheduler)

        val s = session(startedAtElapsedMs = 1_000L, packages = setOf("a", "b"))
        repo.createSession(s)
        time.advanceElapsed(Duration.ofHours(9).toMillis())

        reconciler.reconcile()

        assertThat(backend.suspendedPackages).isEmpty()
        assertThat(scheduler.cancelled).contains(s.id)
        assertThat(repo.sessions.single().status).isEqualTo(SessionStatus.COMPLETED)
        assertThat(repo.getActiveSession()).isNull()
    }

    @Test
    fun `interrupted activation is cleaned up as failed`() = runTest {
        val time = FakeTimeAuthority(elapsedMillis = 1_000L)
        val backend = FakeEnforcementBackend()
        backend.suspendPackages(setOf("a"))
        val repo = FakeSessionRepository()
        val reconciler = buildReconciler(time, backend, repo)

        repo.createSession(session(startedAtElapsedMs = 1_000L, status = SessionStatus.ACTIVATING))

        reconciler.reconcile()

        assertThat(backend.suspendedPackages).isEmpty()
        assertThat(repo.sessions.single().status).isEqualTo(SessionStatus.FAILED)
    }

    @Test
    fun `no active session is a no-op`() = runTest {
        val time = FakeTimeAuthority(elapsedMillis = 1_000L)
        val backend = FakeEnforcementBackend()
        val reconciler = buildReconciler(time, backend, FakeSessionRepository())

        reconciler.reconcile()
        reconciler.reconcile()

        assertThat(backend.suspendedPackages).isEmpty()
    }

    @Test
    fun `reconcile is idempotent`() = runTest {
        val time = FakeTimeAuthority(elapsedMillis = 1_000L)
        val backend = FakeEnforcementBackend()
        val repo = FakeSessionRepository()
        val scheduler = FakeScheduler()
        val reconciler = buildReconciler(time, backend, repo, scheduler = scheduler)

        repo.createSession(session(startedAtElapsedMs = 1_000L, packages = setOf("a")))

        reconciler.reconcile()
        reconciler.reconcile()
        reconciler.reconcile()

        assertThat(backend.suspendedPackages).containsExactly("a")
        assertThat(repo.sessions.single().status).isEqualTo(SessionStatus.ACTIVE)
    }
}
