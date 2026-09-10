package com.focuslock.domain.safety

/**
 * A [SafetyPolicy] backed by an immutable set of protected package names.
 *
 * The concrete package set is assembled at runtime in later phases (launcher,
 * default IME, dialer, etc.); this class keeps the policy logic itself
 * independent of Android.
 */
class ProtectedPackageSafetyPolicy(
    private val protectedPackages: Set<String>,
) : SafetyPolicy {
    override fun isProtected(packageName: String): Boolean =
        packageName in protectedPackages
}
