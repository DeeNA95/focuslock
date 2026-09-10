package com.focuslock.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_profile")
data class FocusProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val durationMillis: Long,
    val enforcementMode: String,
    val fortressModeEnabled: Boolean,
    val enabled: Boolean,
    val motto: String,
)
