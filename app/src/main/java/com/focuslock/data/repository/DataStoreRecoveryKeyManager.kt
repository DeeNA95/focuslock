package com.focuslock.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.focuslock.domain.session.RecoveryKeyHasher
import com.focuslock.domain.session.RecoveryKeyManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.recoveryDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "focuslock_recovery",
)

/**
 * Persists only the salted hash of the recovery key; the plaintext key is never
 * stored and is shown to the user exactly once at generation time.
 */
@Singleton
class DataStoreRecoveryKeyManager @Inject constructor(
    @ApplicationContext private val context: Context,
) : RecoveryKeyManager {

    private object Keys {
        val SALT = stringPreferencesKey("recovery_salt")
        val HASH = stringPreferencesKey("recovery_hash")
    }

    override val isConfigured: Flow<Boolean> =
        context.recoveryDataStore.data.map { it[Keys.HASH] != null }

    override suspend fun generate(): String {
        val key = RecoveryKeyHasher.generateKey()
        val salt = RecoveryKeyHasher.generateSalt()
        val hash = RecoveryKeyHasher.hash(key, salt)
        context.recoveryDataStore.edit {
            it[Keys.SALT] = salt
            it[Keys.HASH] = hash
        }
        return key
    }

    override suspend fun verify(key: String): Boolean {
        val snapshot = context.recoveryDataStore.data.first()
        val salt = snapshot[Keys.SALT] ?: return false
        val hash = snapshot[Keys.HASH] ?: return false
        return RecoveryKeyHasher.verify(key, salt, hash)
    }
}
