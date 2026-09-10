package com.focuslock.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.focuslock.data.entity.TagBindingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(binding: TagBindingEntity): Long

    @Query("SELECT * FROM tag_binding WHERE token = :token")
    suspend fun getByToken(token: String): TagBindingEntity?

    @Query("SELECT * FROM tag_binding ORDER BY label")
    fun observeAll(): Flow<List<TagBindingEntity>>

    @Query("SELECT * FROM tag_binding ORDER BY label")
    suspend fun getAll(): List<TagBindingEntity>

    @Query("DELETE FROM tag_binding WHERE id = :id")
    suspend fun delete(id: String)
}
