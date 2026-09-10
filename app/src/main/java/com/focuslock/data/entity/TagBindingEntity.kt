package com.focuslock.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tag_binding",
    indices = [Index(value = ["token"], unique = true)],
)
data class TagBindingEntity(
    @PrimaryKey val id: String,
    val token: String,
    val label: String,
    val profileId: String,
)
