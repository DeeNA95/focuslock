package com.focuslock.domain.safety

import com.focuslock.domain.model.EnforcementMode
import com.focuslock.domain.model.FocusProfile
import com.focuslock.enforcement.EnforcementBackendProvider
import com.focuslock.enforcement.FakeEnforcementBackend
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Duration
import java.util.UUID

class HardModePreflightTest {

    private fun profile(packages: Set<String> = setOf("a"), mode: EnforcementMode = EnforcementMode.HARD) =
        FocusProfile(
            id = UUID.randomUUID(),
            name = "Deep Work",
            duration = Duration.ofHours(8),
            blockedPackages = packages,
            enforcementMode = mode,
        )

    @Test
    fun `preflight passes when backend available and packages safe`() {
        val backend = FakeEnforcementBackend()
        val preflight = HardModePreflight(
            safetyPolicy = ProtectedPackageSafetyPolicy(emptySet()),
            backendProvider = EnforcementBackendProvider { backend },
        )

        assertThat(preflight.preflight(profile()).passed).isTrue()
    }

    @Test
    fun `preflight fails when backend unavailable`() {
        val backend = FakeEnforcementBackend(available = false)
        val preflight = HardModePreflight(
            safetyPolicy = ProtectedPackageSafetyPolicy(emptySet()),
            backendProvider = EnforcementBackendProvider { backend },
        )

        val result = preflight.preflight(profile())
        assertThat(result.passed).isFalse()
        assertThat(result.failures).contains(PreflightFailure.ENFORCEMENT_UNAVAILABLE)
    }

    @Test
    fun `preflight fails on unsafe configuration`() {
        val backend = FakeEnforcementBackend()
        val preflight = HardModePreflight(
            safetyPolicy = ProtectedPackageSafetyPolicy(setOf("com.android.settings")),
            backendProvider = EnforcementBackendProvider { backend },
        )

        val result = preflight.preflight(profile(packages = setOf("com.android.settings")))
        assertThat(result.passed).isFalse()
        assertThat(result.failures).contains(PreflightFailure.UNSAFE_CONFIGURATION)
    }
}
