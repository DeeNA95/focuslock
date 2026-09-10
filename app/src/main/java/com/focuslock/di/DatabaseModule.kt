package com.focuslock.di

import android.content.Context
import androidx.room.Room
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

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FocusLockDatabase =
        Room.databaseBuilder(context, FocusLockDatabase::class.java, "focuslock.db")
            .fallbackToDestructiveMigration()
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
