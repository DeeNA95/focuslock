package com.focuslock.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.focuslock.data.entity.SessionEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Insert
    suspend fun insert(event: SessionEventEntity): Long

    @Query("SELECT * FROM session_event ORDER BY timestampMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<SessionEventEntity>>

    @Query("SELECT * FROM session_event ORDER BY timestampMillis DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<SessionEventEntity>

    @Query("DELETE FROM session_event")
    suspend fun clear()
}
