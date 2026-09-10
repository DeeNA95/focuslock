package com.focuslock.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "profile_blocked_package",
    primaryKeys = ["profileId", "packageName"],
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
data class ProfileBlockedPackageEntity(
    val profileId: String,
    val packageName: String,
)
