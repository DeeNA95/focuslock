package com.focuslock.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.focuslock.domain.model.EnforcementMode
import com.focuslock.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "focuslock_settings",
)

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsRepository {

    private object Keys {
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val DEFAULT_ENFORCEMENT_MODE = stringPreferencesKey("default_enforcement_mode")
        val CONFIRMATION_REQUIRED = booleanPreferencesKey("confirmation_required")
        val DEV_MODE_ENABLED = booleanPreferencesKey("dev_mode_enabled")
        val DEFAULTS_SEEDED = booleanPreferencesKey("defaults_seeded")
    }

    override val onboardingComplete: Flow<Boolean> =
        context.settingsDataStore.data.map { it[Keys.ONBOARDING_COMPLETE] ?: false }

    override val defaultEnforcementMode: Flow<EnforcementMode> =
        context.settingsDataStore.data.map {
            EnforcementMode.valueOf(it[Keys.DEFAULT_ENFORCEMENT_MODE] ?: EnforcementMode.SOFT.name)
        }

    override val confirmationRequired: Flow<Boolean> =
        context.settingsDataStore.data.map { it[Keys.CONFIRMATION_REQUIRED] ?: true }

    override val devModeEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { it[Keys.DEV_MODE_ENABLED] ?: false }

    override val defaultsSeeded: Flow<Boolean> =
        context.settingsDataStore.data.map { it[Keys.DEFAULTS_SEEDED] ?: false }

    override suspend fun setOnboardingComplete(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.ONBOARDING_COMPLETE] = value }
    }

    override suspend fun setDefaultEnforcementMode(mode: EnforcementMode) {
        context.settingsDataStore.edit { it[Keys.DEFAULT_ENFORCEMENT_MODE] = mode.name }
    }

    override suspend fun setConfirmationRequired(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.CONFIRMATION_REQUIRED] = value }
    }

    override suspend fun setDevModeEnabled(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.DEV_MODE_ENABLED] = value }
    }

    override suspend fun setDefaultsSeeded(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.DEFAULTS_SEEDED] = value }
    }
}
