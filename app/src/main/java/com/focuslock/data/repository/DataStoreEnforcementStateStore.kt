package com.focuslock.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.focuslock.domain.enforcement.EnforcementStateStore
import com.focuslock.domain.model.EnforcementMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.enforcementDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "focuslock_enforcement",
)

/**
 * DataStore-backed [EnforcementStateStore]. The state is not sensitive (only
 * package names FocusLock already knows about), so no encryption is required.
 */
@Singleton
class DataStoreEnforcementStateStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : EnforcementStateStore {

    private object Keys {
        val SOFT_SUSPENDED = stringSetPreferencesKey("suspended_soft")
        val HARD_SUSPENDED = stringSetPreferencesKey("suspended_hard")
        val AUTO_TIME_BEFORE = stringPreferencesKey("auto_time_before")
        val DND_FILTER_BEFORE = stringPreferencesKey("dnd_filter_before")
    }

    private fun key(mode: EnforcementMode) = when (mode) {
        EnforcementMode.SOFT -> Keys.SOFT_SUSPENDED
        EnforcementMode.HARD -> Keys.HARD_SUSPENDED
    }

    override suspend fun suspendedPackages(mode: EnforcementMode): Set<String> =
        context.enforcementDataStore.data.first()[key(mode)].orEmpty()

    override suspend fun recordSuspended(mode: EnforcementMode, packages: Set<String>) {
        if (packages.isEmpty()) return
        context.enforcementDataStore.edit { prefs ->
            prefs[key(mode)] = prefs[key(mode)].orEmpty() + packages
        }
    }

    override suspend fun clearSuspended(mode: EnforcementMode, packages: Set<String>) {
        if (packages.isEmpty()) return
        context.enforcementDataStore.edit { prefs ->
            val remaining = prefs[key(mode)].orEmpty() - packages
            if (remaining.isEmpty()) {
                prefs.remove(key(mode))
            } else {
                prefs[key(mode)] = remaining
            }
        }
    }

    override suspend fun autoTimeBefore(): Boolean? =
        context.enforcementDataStore.data.first()[Keys.AUTO_TIME_BEFORE]
            ?.toBooleanStrictOrNull()

    override suspend fun setAutoTimeBefore(enabled: Boolean) {
        context.enforcementDataStore.edit { it[Keys.AUTO_TIME_BEFORE] = enabled.toString() }
    }

    override suspend fun clearAutoTimeBefore() {
        context.enforcementDataStore.edit { it.remove(Keys.AUTO_TIME_BEFORE) }
    }

    override suspend fun dndFilterBefore(): Int? =
        context.enforcementDataStore.data.first()[Keys.DND_FILTER_BEFORE]?.toIntOrNull()

    override suspend fun setDndFilterBefore(filter: Int) {
        context.enforcementDataStore.edit { it[Keys.DND_FILTER_BEFORE] = filter.toString() }
    }

    override suspend fun clearDndFilterBefore() {
        context.enforcementDataStore.edit { it.remove(Keys.DND_FILTER_BEFORE) }
    }
}
