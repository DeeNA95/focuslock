package com.focuslock.di

import com.focuslock.data.repository.DataStoreSettingsRepository
import com.focuslock.data.repository.DataStoreRecoveryKeyManager
import com.focuslock.data.repository.AndroidAppRepository
import com.focuslock.data.repository.RoomEventLogRepository
import com.focuslock.data.repository.RoomProfileRepository
import com.focuslock.data.repository.RoomSessionRepository
import com.focuslock.data.repository.RoomTagRepository
import com.focuslock.domain.repository.AppRepository
import com.focuslock.domain.repository.EventLogRepository
import com.focuslock.domain.repository.ProfileRepository
import com.focuslock.domain.repository.SessionRepository
import com.focuslock.domain.repository.SettingsRepository
import com.focuslock.domain.repository.TagRepository
import com.focuslock.domain.session.RecoveryKeyManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: RoomProfileRepository): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: RoomSessionRepository): SessionRepository

    @Binds
    @Singleton
    abstract fun bindTagRepository(impl: RoomTagRepository): TagRepository

    @Binds
    @Singleton
    abstract fun bindEventLogRepository(impl: RoomEventLogRepository): EventLogRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindAppRepository(impl: AndroidAppRepository): AppRepository

    @Binds
    @Singleton
    abstract fun bindRecoveryKeyManager(impl: DataStoreRecoveryKeyManager): RecoveryKeyManager
}
