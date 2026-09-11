package com.focuslock.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.focuslock.data.dao.EventDao
import com.focuslock.data.dao.ProfileDao
import com.focuslock.data.dao.SessionDao
import com.focuslock.data.dao.TagDao
import com.focuslock.data.db.FocusLockDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * v2 -> v3 persists the boot id used for reboot detection.
     *
     * Sessions created before this migration get `0`, which the SessionClock
     * interprets with the legacy elapsed-clock fallback.
     */
    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE focus_session ADD COLUMN startedAtBootId INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    /**
     * v3 -> v4 adds per-profile open behavior and DND.
     */
    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE focus_profile ADD COLUMN openBehavior TEXT NOT NULL DEFAULT 'BLOCK'")
            db.execSQL("ALTER TABLE focus_profile ADD COLUMN enableDnd INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE focus_session ADD COLUMN openBehavior TEXT NOT NULL DEFAULT 'BLOCK'")
            db.execSQL("ALTER TABLE focus_session ADD COLUMN enableDnd INTEGER NOT NULL DEFAULT 0")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FocusLockDatabase =
        Room.databaseBuilder(context, FocusLockDatabase::class.java, "focuslock.db")
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
            // Only destructive on downgrade (a developer-only scenario). A normal
            // upgrade must ship a migration rather than silently wiping data.
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

    @Provides
    fun provideProfileDao(db: FocusLockDatabase): ProfileDao = db.profileDao()

    @Provides
    fun provideSessionDao(db: FocusLockDatabase): SessionDao = db.sessionDao()

    @Provides
    fun provideTagDao(db: FocusLockDatabase): TagDao = db.tagDao()

    @Provides
    fun provideEventDao(db: FocusLockDatabase): EventDao = db.eventDao()
}
