package com.focuslock.data.db

import androidx.room.Embedded
import androidx.room.Relation
import com.focuslock.data.entity.FocusSessionEntity
import com.focuslock.data.entity.SessionBlockedPackageEntity

data class FocusSessionWithDetails(
    @Embedded val session: FocusSessionEntity,
    @Relation(parentColumn = "id", entityColumn = "sessionId")
    val blockedPackages: List<SessionBlockedPackageEntity>,
)
