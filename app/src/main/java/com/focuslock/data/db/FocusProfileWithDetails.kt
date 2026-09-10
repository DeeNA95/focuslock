package com.focuslock.data.db

import androidx.room.Embedded
import androidx.room.Relation
import com.focuslock.data.entity.ActivationWindowEntity
import com.focuslock.data.entity.FocusProfileEntity
import com.focuslock.data.entity.ProfileBlockedPackageEntity

data class FocusProfileWithDetails(
    @Embedded val profile: FocusProfileEntity,
    @Relation(parentColumn = "id", entityColumn = "profileId")
    val blockedPackages: List<ProfileBlockedPackageEntity>,
    @Relation(parentColumn = "id", entityColumn = "profileId")
    val activationWindows: List<ActivationWindowEntity>,
)
