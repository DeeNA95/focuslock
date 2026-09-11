package com.focuslock.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.focuslock.data.dao.EventDao
import com.focuslock.data.dao.ProfileDao
import com.focuslock.data.dao.SessionDao
import com.focuslock.data.dao.TagDao
import com.focuslock.data.entity.ActivationWindowEntity
import com.focuslock.data.entity.FocusProfileEntity
import com.focuslock.data.entity.FocusSessionEntity
import com.focuslock.data.entity.ProfileBlockedPackageEntity
import com.focuslock.data.entity.SessionBlockedPackageEntity
import com.focuslock.data.entity.SessionEventEntity
import com.focuslock.data.entity.TagBindingEntity

@Database(
    entities = [
        FocusProfileEntity::class,
        ProfileBlockedPackageEntity::class,
        ActivationWindowEntity::class,
        TagBindingEntity::class,
        FocusSessionEntity::class,
        SessionBlockedPackageEntity::class,
        SessionEventEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class FocusLockDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun sessionDao(): SessionDao
    abstract fun tagDao(): TagDao
    abstract fun eventDao(): EventDao
}
