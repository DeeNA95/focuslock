package com.focuslock.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.focuslock.data.entity.SessionEventEntity
import kotlinx.coroutines.flow.Flow

/** Projection: how often a package was blocked from opening. */
data class BlockedAppCount(
    val packageName: String,
    val count: Int,
)

@Dao
interface EventDao {

    @Insert
    suspend fun insert(event: SessionEventEntity): Long

    @Query("SELECT * FROM session_event ORDER BY timestampMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<SessionEventEntity>>

    @Query("SELECT * FROM session_event ORDER BY timestampMillis DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<SessionEventEntity>

    @Query(
        "SELECT detail AS packageName, COUNT(*) AS count FROM session_event " +
            "WHERE type = 'BLOCKED_ATTEMPT' AND detail IS NOT NULL " +
            "GROUP BY detail ORDER BY count DESC LIMIT :limit"
    )
    fun observeBlockedAppCounts(limit: Int): Flow<List<BlockedAppCount>>

    @Query("DELETE FROM session_event")
    suspend fun clear()
}
