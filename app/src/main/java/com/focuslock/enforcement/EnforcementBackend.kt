package com.focuslock.enforcement

/**
 * The single abstraction through which the rest of the application performs
 * (and reverses) app blocking.
 *
 * The session/rule/domain layers must never know how apps are actually
 * blocked; they only ever talk to this interface.
 */
interface EnforcementBackend {

    /**
     * Block [packages]. Returns a result describing exactly which packages
     * were successfully blocked and which failed. Implementations must never
     * silently claim success.
     */
    suspend fun suspendPackages(packages: Set<String>): EnforcementResult

    /**
     * Restore [packages] to a usable state.
     */
    suspend fun resumePackages(packages: Set<String>): EnforcementResult

    /** Whether [packageName] can be blocked at all by this backend. */
    fun suspendability(packageName: String): Suspendability

    /** Whether the backend is currently usable (service enabled, admin active, etc.). */
    fun isAvailable(): Boolean

    fun backendType(): EnforcementBackendType
}

enum class EnforcementBackendType {
    ACCESSIBILITY,
    DEVICE_OWNER,
}

enum class Suspendability {
    SUSPENDABLE,
    UNSAFE,
    NOT_SUSPENDABLE,
}

/**
 * Result of a suspend/resume operation.
 *
 * [successful] and [failed] partition the requested package set so that callers
 * can reconcile or roll back precisely.
 */
data class EnforcementResult(
    val successful: Set<String> = emptySet(),
    val failed: Set<String> = emptySet(),
) {
    val isComplete: Boolean get() = failed.isEmpty()
}
