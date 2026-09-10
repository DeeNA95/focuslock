package com.focuslock.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.focuslock.data.db.FocusProfileWithDetails
import com.focuslock.data.entity.ActivationWindowEntity
import com.focuslock.data.entity.FocusProfileEntity
import com.focuslock.data.entity.ProfileBlockedPackageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Transaction
    @Query("SELECT * FROM focus_profile ORDER BY name")
    fun observeAllWithDetails(): Flow<List<FocusProfileWithDetails>>

    @Transaction
    @Query("SELECT * FROM focus_profile WHERE id = :id")
    fun observeById(id: String): Flow<FocusProfileWithDetails?>

    @Transaction
    @Query("SELECT * FROM focus_profile WHERE id = :id")
    suspend fun getById(id: String): FocusProfileWithDetails?

    @Transaction
    @Query("SELECT * FROM focus_profile ORDER BY name")
    suspend fun getAll(): List<FocusProfileWithDetails>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: FocusProfileEntity): Long

    @Insert
    suspend fun insertBlockedPackages(packages: List<ProfileBlockedPackageEntity>)

    @Insert
    suspend fun insertActivationWindows(windows: List<ActivationWindowEntity>)

    @Query("DELETE FROM profile_blocked_package WHERE profileId = :profileId")
    suspend fun deleteBlockedPackages(profileId: String)

    @Query("DELETE FROM activation_window WHERE profileId = :profileId")
    suspend fun deleteActivationWindows(profileId: String)

    @Query("DELETE FROM focus_profile WHERE id = :id")
    suspend fun deleteProfile(id: String)

    @Transaction
    suspend fun saveProfile(
        profile: FocusProfileEntity,
        blockedPackages: List<ProfileBlockedPackageEntity>,
        activationWindows: List<ActivationWindowEntity>,
    ) {
        upsertProfile(profile)
        deleteBlockedPackages(profile.id)
        deleteActivationWindows(profile.id)
        insertBlockedPackages(blockedPackages)
        insertActivationWindows(activationWindows)
    }
}
