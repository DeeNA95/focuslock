package com.focuslock.domain.session

import kotlinx.coroutines.flow.Flow

/**
 * Manages the emergency recovery key.
 *
 * During setup a long random key is generated and shown exactly once; the user
 * is expected to store it off-device. Only a salted hash is stored locally.
 * Early session release requires entering the complete key.
 *
 * This makes recovery possible during a genuine emergency while remaining
 * deliberately inconvenient during boredom.
 */
interface RecoveryKeyManager {
    val isConfigured: Flow<Boolean>

    /** Generates and stores a new recovery key, returning the key exactly once. */
    suspend fun generate(): String

    /** Whether [key] matches the stored hash. */
    suspend fun verify(key: String): Boolean
}

sealed interface EmergencyReleaseResult {
    data object Released : EmergencyReleaseResult
    data object InvalidKey : EmergencyReleaseResult
    data object NoActiveSession : EmergencyReleaseResult
}
