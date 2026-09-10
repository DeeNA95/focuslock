package com.focuslock.enforcement.accessibility

import android.content.Context
import android.provider.Settings
import com.focuslock.enforcement.EnforcementBackend
import com.focuslock.enforcement.EnforcementBackendType
import com.focuslock.enforcement.EnforcementResult
import com.focuslock.enforcement.Suspendability
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Soft-lock backend. The actual interception is performed by
 * [FocusLockAccessibilityService], which reads the active session directly
 * from the database. This backend therefore treats suspend/resume as no-ops
 * (success) and reports availability based on whether the service is enabled.
 *
 * Soft Lock is NOT a security boundary: the user can disable the accessibility
 * service.
 */
@Singleton
class AccessibilityEnforcementBackend @Inject constructor(
    @ApplicationContext private val context: Context,
) : EnforcementBackend {

    override suspend fun suspendPackages(packages: Set<String>): EnforcementResult =
        EnforcementResult(successful = packages, failed = emptySet())

    override suspend fun resumePackages(packages: Set<String>): EnforcementResult =
        EnforcementResult(successful = packages, failed = emptySet())

    override fun suspendability(packageName: String): Suspendability =
        Suspendability.SUSPENDABLE

    override fun isAvailable(): Boolean = isServiceEnabled(context)

    override fun backendType(): EnforcementBackendType = EnforcementBackendType.ACCESSIBILITY

    companion object {
        fun isServiceEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ) ?: return false
            return enabled
                .split(':')
                .any { it == FocusLockAccessibilityService.COMPONENT_FLATTENED }
        }
    }
}
