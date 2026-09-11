package com.focuslock.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.focuslock.data.db.FocusSessionWithDetails
import com.focuslock.data.entity.FocusSessionEntity
import com.focuslock.data.entity.SessionBlockedPackageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSessionEntity): Long

    @Insert
    suspend fun insertBlockedPackages(packages: List<SessionBlockedPackageEntity>)

    @Query("DELETE FROM session_blocked_package WHERE sessionId = :sessionId")
    suspend fun deleteBlockedPackages(sessionId: String)

    @Transaction
    suspend fun saveSession(
        session: FocusSessionEntity,
        blockedPackages: List<SessionBlockedPackageEntity>,
    ) {
        insertSession(session)
        deleteBlockedPackages(session.id)
        insertBlockedPackages(blockedPackages)
    }

    @Query("UPDATE focus_session SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Transaction
    @Query(
        "SELECT * FROM focus_session WHERE status IN ('ACTIVATING','ACTIVE','EXPIRING') " +
            "ORDER BY startedAtWallClockMillis DESC LIMIT 1"
    )
    suspend fun getActive(): FocusSessionWithDetails?

    @Transaction
    @Query(
        "SELECT * FROM focus_session WHERE status IN ('ACTIVATING','ACTIVE','EXPIRING') " +
            "ORDER BY startedAtWallClockMillis DESC LIMIT 1"
    )
    fun observeActive(): Flow<FocusSessionWithDetails?>

    @Transaction
    @Query("SELECT * FROM focus_session WHERE id = :id")
    suspend fun getById(id: String): FocusSessionWithDetails?

    @Transaction
    @Query("SELECT * FROM focus_session ORDER BY startedAtWallClockMillis DESC")
    fun observeAll(): Flow<List<FocusSessionWithDetails>>

    @Query(
        "SELECT * FROM focus_session WHERE status = 'COMPLETED' " +
            "ORDER BY startedAtWallClockMillis DESC"
    )
    fun observeCompletedSessions(): Flow<List<FocusSessionEntity>>
}
