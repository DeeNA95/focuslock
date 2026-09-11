package com.focuslock.domain.session

import com.focuslock.domain.model.ActivationWindow
import com.focuslock.domain.model.EnforcementMode
import com.focuslock.domain.model.FocusProfile
import com.focuslock.domain.model.SessionStatus
import com.focuslock.domain.rules.RuleEngine
import com.focuslock.domain.safety.ProtectedPackageSafetyPolicy
import com.focuslock.enforcement.EnforcementBackendProvider
import com.focuslock.enforcement.FakeEnforcementBackend
import com.focuslock.testutil.FakeEventLog
import com.focuslock.testutil.FakeEnforcementStateStore
import com.focuslock.testutil.FakeNotifier
import com.focuslock.testutil.FakeScheduler
import com.focuslock.testutil.FakeSessionRepository
import com.focuslock.testutil.FakeTemporaryPolicyManager
import com.focuslock.testutil.FakeTimeAuthority
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

class SessionManagerTest {

    private fun profile(
        enabled: Boolean = true,
        packages: Set<String> = setOf("com.instagram.android"),
        mode: EnforcementMode = EnforcementMode.SOFT,
        windows: List<ActivationWindow> = emptyList(),
    ) = FocusProfile(
        id = UUID.randomUUID(),
        name = "Deep Work",
        duration = Duration.ofHours(8),
        blockedPackages = packages,
        activationWindows = windows,
        enforcementMode = mode,
        enabled = enabled,
    )

    private fun buildManager(
        time: FakeTimeAuthority,
        backend: FakeEnforcementBackend,
        sessionRepo: FakeSessionRepository,
        safety: ProtectedPackageSafetyPolicy = ProtectedPackageSafetyPolicy(emptySet()),
        scheduler: FakeScheduler = FakeScheduler(),
        notifier: FakeNotifier = FakeNotifier(),
        events: FakeEventLog = FakeEventLog(),
    ) = SessionManager(
        sessionRepository = sessionRepo,
        ruleEngine = RuleEngine(time),
        safetyPolicy = safety,
        timeAuthority = time,
        backendProvider = EnforcementBackendProvider { backend },
        scheduler = scheduler,
        notifier = notifier,
        eventLog = events,
        policyManager = FakeTemporaryPolicyManager(),
        stateStore = FakeEnforcementStateStore(),
        lock = SessionLock(),
    )

    @Test
    fun `successful activation creates an active session with snapshot`() = runTest {
        val time = FakeTimeAuthority(Instant.parse("2026-01-01T20:00:00Z"))
        val backend = FakeEnforcementBackend()
        val repo = FakeSessionRepository()
        val manager = buildManager(time, backend, repo)

        val result = manager.activate(profile(packages = setOf("a", "b")))

        assertThat(result).isInstanceOf(SessionActivationResult.Activated::class.java)
        val session = (result as SessionActivationResult.Activated).session
        assertThat(session.status).isEqualTo(SessionStatus.ACTIVE)
        assertThat(session.blockedPackagesSnapshot).containsExactly("a", "b")
        assertThat(session.duration).isEqualTo(Duration.ofHours(8))
        assertThat(backend.suspendedPackages).containsExactly("a", "b")
        assertThat(repo.getActiveSession()?.id).isEqualTo(session.id)
    }

    @Test
    fun `session snapshot is unaffected by later profile changes`() = runTest {
        val time = FakeTimeAuthority(Instant.parse("2026-01-01T20:00:00Z"))
        val backend = FakeEnforcementBackend()
        val repo = FakeSessionRepository()
        val manager = buildManager(time, backend, repo)

        val original = profile(packages = setOf("a", "b"))
        val result = manager.activate(original) as SessionActivationResult.Activated

        // Simulate editing the profile after activation.
        val edited = original.copy(blockedPackages = setOf("a"))
        manager.activate(edited)

        assertThat(result.session.blockedPackagesSnapshot).containsExactly("a", "b")
    }

    @Test
    fun `second activation while active is rejected`() = runTest {
        val time = FakeTimeAuthority(Instant.parse("2026-01-01T20:00:00Z"))
        val backend = FakeEnforcementBackend()
        val repo = FakeSessionRepository()
        val manager = buildManager(time, backend, repo)

        manager.activate(profile())
        val second = manager.activate(profile())

        assertThat((second as SessionActivationResult.Failed).reason)
            .isEqualTo(ActivationFailure.SESSION_ALREADY_ACTIVE)
    }

    @Test
    fun `disabled profile is rejected`() = runTest {
        val time = FakeTimeAuthority(Instant.parse("2026-01-01T20:00:00Z"))
        val manager = buildManager(time, FakeEnforcementBackend(), FakeSessionRepository())

        val result = manager.activate(profile(enabled = false))

        assertThat((result as SessionActivationResult.Failed).reason)
            .isEqualTo(ActivationFailure.PROFILE_DISABLED)
    }

    @Test
    fun `outside window is rejected`() = runTest {
        val time = FakeTimeAuthority(
            Instant.parse("2026-01-01T12:00:00Z"),
            zone = ZoneId.of("UTC"),
        )
        val manager = buildManager(time, FakeEnforcementBackend(), FakeSessionRepository())
        val windows = listOf(
            ActivationWindow(start = LocalTime.of(18, 0), end = LocalTime.of(23, 59)),
        )

        val result = manager.activate(profile(windows = windows))

        assertThat((result as SessionActivationResult.Failed).reason)
            .isEqualTo(ActivationFailure.OUTSIDE_ALLOWED_WINDOW)
    }

    @Test
    fun `unsafe configuration is rejected`() = runTest {
        val time = FakeTimeAuthority(Instant.parse("2026-01-01T20:00:00Z"))
        val safety = ProtectedPackageSafetyPolicy(setOf("com.android.settings"))
        val manager = buildManager(
            time, FakeEnforcementBackend(), FakeSessionRepository(), safety = safety,
        )

        val result = manager.activate(profile(packages = setOf("com.android.settings")))

        assertThat((result as SessionActivationResult.Failed).reason)
            .isEqualTo(ActivationFailure.UNSAFE_CONFIGURATION)
    }

    @Test
    fun `unavailable backend is rejected`() = runTest {
        val time = FakeTimeAuthority(Instant.parse("2026-01-01T20:00:00Z"))
        val backend = FakeEnforcementBackend(available = false)
        val manager = buildManager(time, backend, FakeSessionRepository())

        val result = manager.activate(profile())

        assertThat((result as SessionActivationResult.Failed).reason)
            .isEqualTo(ActivationFailure.ENFORCEMENT_UNAVAILABLE)
    }

    @Test
    fun `enforcement failure rolls back and marks failed`() = runTest {
        val time = FakeTimeAuthority(Instant.parse("2026-01-01T20:00:00Z"))
        val backend = FakeEnforcementBackend(failOn = setOf("b"))
        val repo = FakeSessionRepository()
        val manager = buildManager(time, backend, repo)

        val result = manager.activate(profile(packages = setOf("a", "b")))

        assertThat((result as SessionActivationResult.Failed).reason)
            .isEqualTo(ActivationFailure.ENFORCEMENT_FAILURE)
        // Rollback: nothing left suspended.
        assertThat(backend.suspendedPackages).isEmpty()
        // Session persisted as FAILED, not ACTIVE.
        assertThat(repo.sessions.single().status).isEqualTo(SessionStatus.FAILED)
        assertThat(repo.getActiveSession()).isNull()
    }
}
