package com.focuslock.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_session")
data class FocusSessionEntity(
    @PrimaryKey val id: String,
    val profileId: String?,
    val profileNameSnapshot: String,
    val startedAtWallClockMillis: Long,
    val startedAtElapsedRealtimeMs: Long,
    val expiresAtWallClockMillis: Long,
    val durationMillis: Long,
    val enforcementMode: String,
    val fortressModeEnabled: Boolean,
    val mottoSnapshot: String,
    val status: String,
)
