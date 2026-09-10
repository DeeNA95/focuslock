package com.focuslock.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "activation_window",
    foreignKeys = [
        ForeignKey(
            entity = FocusProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("profileId")],
)
data class ActivationWindowEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val startSecondOfDay: Int,
    val endSecondOfDay: Int,
    val daysOfWeekMask: Int,
)
