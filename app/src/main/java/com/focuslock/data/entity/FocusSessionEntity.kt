package com.focuslock.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_session")
data class FocusSessionEntity(
    @PrimaryKey val id: String,
    val profileId: String?,
    val profileNameSnapshot: String,
    val startedAtWallClockMillis: Long,
    val startedAtElapsedRealtimeMs: Long,
    @ColumnInfo(defaultValue = "0") val startedAtBootId: Long = 0L,
    val expiresAtWallClockMillis: Long,
    val durationMillis: Long,
    val enforcementMode: String,
    val fortressModeEnabled: Boolean,
    @ColumnInfo(defaultValue = "BLOCK") val openBehavior: String = "BLOCK",
    @ColumnInfo(defaultValue = "0") val enableDnd: Boolean = false,
    val mottoSnapshot: String,
    val status: String,
)
