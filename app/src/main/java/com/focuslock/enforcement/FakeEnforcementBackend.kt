package com.focuslock.enforcement

/**
 * In-memory enforcement backend used during Phase 5 development and in tests.
 * It records the set of "suspended" packages without touching the OS.
 */
class FakeEnforcementBackend(
    private val type: EnforcementBackendType = EnforcementBackendType.ACCESSIBILITY,
    private val available: Boolean = true,
    private val failOn: Set<String> = emptySet(),
) : EnforcementBackend {

    private val suspended = mutableSetOf<String>()

    val suspendedPackages: Set<String> get() = suspended.toSet()

    override suspend fun suspendPackages(packages: Set<String>): EnforcementResult {
        val failed = packages.filter { it in failOn }.toSet()
        val success = packages - failed
        suspended.addAll(success)
        return EnforcementResult(successful = success, failed = failed)
    }

    override suspend fun resumePackages(packages: Set<String>): EnforcementResult {
        suspended.removeAll(packages)
        return EnforcementResult(successful = packages.toSet(), failed = emptySet())
    }

    override fun suspendability(packageName: String): Suspendability = Suspendability.SUSPENDABLE

    override fun isAvailable(): Boolean = available

    override fun backendType(): EnforcementBackendType = type
}
