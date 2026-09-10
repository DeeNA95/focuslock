package com.focuslock.domain.repository

import com.focuslock.domain.model.FocusProfile
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface ProfileRepository {
    fun observeProfiles(): Flow<List<FocusProfile>>
    fun observeProfile(id: UUID): Flow<FocusProfile?>
    suspend fun getProfile(id: UUID): FocusProfile?
    suspend fun getAllProfiles(): List<FocusProfile>
    suspend fun saveProfile(profile: FocusProfile)
    suspend fun deleteProfile(id: UUID)
}
