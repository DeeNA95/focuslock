package com.focuslock.domain.safety

/**
 * Guards against blocking packages that could make the phone unusable or
 * defeat the app's own ability to operate (launcher, system UI, FocusLock
 * itself, the IME, the dialer, etc.).
 */
interface SafetyPolicy {
    /** Whether [packageName] must never be blocked. */
    fun isProtected(packageName: String): Boolean

    /**
     * Splits [requested] into allowed vs rejected packages.
     *
     * [SafetyValidationResult.safe] is true only when nothing was rejected.
     */
    fun validateBlockList(requested: Set<String>): SafetyValidationResult {
        val rejected = requested.filter { isProtected(it) }.toSet()
        val allowed = requested - rejected
        return SafetyValidationResult(
            safe = rejected.isEmpty(),
            allowedPackages = allowed,
            rejectedPackages = rejected,
        )
    }
}
