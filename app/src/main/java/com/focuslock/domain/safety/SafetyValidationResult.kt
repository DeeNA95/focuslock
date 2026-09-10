package com.focuslock.domain.safety

/**
 * Result of validating a requested block list against [SafetyPolicy].
 *
 * A session must not begin when [safe] is false, i.e. when any requested
 * package is in the protected set and would put the phone into an unsafe state.
 */
data class SafetyValidationResult(
    val safe: Boolean,
    val allowedPackages: Set<String> = emptySet(),
    val rejectedPackages: Set<String> = emptySet(),
)
