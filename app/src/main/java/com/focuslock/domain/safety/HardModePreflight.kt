package com.focuslock.domain.safety

import com.focuslock.domain.model.FocusProfile
import com.focuslock.enforcement.EnforcementBackendProvider
import com.focuslock.enforcement.Suspendability
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Verifies, before a Hard Lock session begins, that every precondition holds.
 *
 * If preflight fails, a partially-trusted hard session must not be activated;
 * the UI shows exactly what failed.
 */
@Singleton
class HardModePreflight @Inject constructor(
    private val safetyPolicy: SafetyPolicy,
    private val backendProvider: EnforcementBackendProvider,
) {
    fun preflight(profile: FocusProfile): PreflightResult {
        val failures = mutableListOf<PreflightFailure>()

        val backend = backendProvider.backendFor(profile.enforcementMode)
        if (!backend.isAvailable()) {
            failures += PreflightFailure.ENFORCEMENT_UNAVAILABLE
        }

        if (!safetyPolicy.validateBlockList(profile.blockedPackages).safe) {
            failures += PreflightFailure.UNSAFE_CONFIGURATION
        }

        val unsuspendable = profile.blockedPackages
            .filter { backend.suspendability(it) != Suspendability.SUSPENDABLE }
        if (unsuspendable.isNotEmpty()) {
            failures += PreflightFailure.UNSUSPENDABLE_PACKAGES
        }

        return PreflightResult(passed = failures.isEmpty(), failures = failures)
    }
}

data class PreflightResult(
    val passed: Boolean,
    val failures: List<PreflightFailure>,
)

enum class PreflightFailure {
    ENFORCEMENT_UNAVAILABLE,
    UNSAFE_CONFIGURATION,
    UNSUSPENDABLE_PACKAGES,
}
