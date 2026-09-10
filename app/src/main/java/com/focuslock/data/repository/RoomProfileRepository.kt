package com.focuslock.data.repository

import com.focuslock.data.dao.ProfileDao
import com.focuslock.data.db.toBlockedPackageEntities
import com.focuslock.data.db.toActivationWindowEntities
import com.focuslock.data.db.toDomain
import com.focuslock.data.db.toEntity
import com.focuslock.domain.model.FocusProfile
import com.focuslock.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomProfileRepository @Inject constructor(
    private val profileDao: ProfileDao,
) : ProfileRepository {

    override fun observeProfiles(): Flow<List<FocusProfile>> =
        profileDao.observeAllWithDetails().map { list -> list.map { it.toDomain() } }

    override fun observeProfile(id: UUID): Flow<FocusProfile?> =
        profileDao.observeById(id.toString()).map { it?.toDomain() }

    override suspend fun getProfile(id: UUID): FocusProfile? =
        profileDao.getById(id.toString())?.toDomain()

    override suspend fun getAllProfiles(): List<FocusProfile> =
        profileDao.getAll().map { it.toDomain() }

    override suspend fun saveProfile(profile: FocusProfile) {
        profileDao.saveProfile(
            profile.toEntity(),
            profile.toBlockedPackageEntities(),
            profile.toActivationWindowEntities(),
        )
    }

    override suspend fun deleteProfile(id: UUID) {
        profileDao.deleteProfile(id.toString())
    }
}
