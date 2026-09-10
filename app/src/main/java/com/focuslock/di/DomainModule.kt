package com.focuslock.di

import com.focuslock.data.time.SystemTimeAuthority
import com.focuslock.domain.time.TimeAuthority
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DomainModule {

    @Binds
    @Singleton
    abstract fun bindTimeAuthority(impl: SystemTimeAuthority): TimeAuthority
}
