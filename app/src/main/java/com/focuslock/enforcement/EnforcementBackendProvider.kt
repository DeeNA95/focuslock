package com.focuslock.enforcement

import com.focuslock.domain.model.EnforcementMode
import com.focuslock.enforcement.accessibility.AccessibilityEnforcementBackend
import com.focuslock.enforcement.deviceowner.DeviceOwnerEnforcementBackend
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Routes an [EnforcementMode] to the matching backend. The domain layer depends
 * on this provider so it can select the backend per session/profile without
 * knowing anything about the concrete implementations.
 */
fun interface EnforcementBackendProvider {
    fun backendFor(mode: EnforcementMode): EnforcementBackend
}

@Singleton
class DefaultEnforcementBackendProvider @Inject constructor(
    private val accessibility: AccessibilityEnforcementBackend,
    private val deviceOwner: DeviceOwnerEnforcementBackend,
) : EnforcementBackendProvider {
    override fun backendFor(mode: EnforcementMode): EnforcementBackend = when (mode) {
        EnforcementMode.SOFT -> accessibility
        EnforcementMode.HARD -> deviceOwner
    }
}
