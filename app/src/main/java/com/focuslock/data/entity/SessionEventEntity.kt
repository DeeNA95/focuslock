package com.focuslock.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session_event")
data class SessionEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMillis: Long,
    val type: String,
    val sessionId: String?,
    val detail: String?,
)
