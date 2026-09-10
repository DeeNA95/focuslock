package com.focuslock.domain.repository

import com.focuslock.domain.model.EnforcementMode
import kotlinx.coroutines.flow.Flow

/**
 * Simple app preferences persisted via DataStore. Only flat, non-relational
 * preferences belong here; structured product state lives in Room.
 */
interface SettingsRepository {
    val onboardingComplete: Flow<Boolean>
    val defaultEnforcementMode: Flow<EnforcementMode>
    val confirmationRequired: Flow<Boolean>
    val devModeEnabled: Flow<Boolean>

    suspend fun setOnboardingComplete(value: Boolean)
    suspend fun setDefaultEnforcementMode(mode: EnforcementMode)
    suspend fun setConfirmationRequired(value: Boolean)
    suspend fun setDevModeEnabled(value: Boolean)
}
