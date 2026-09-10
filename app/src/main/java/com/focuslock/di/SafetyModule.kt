package com.focuslock.di

import com.focuslock.data.safety.AndroidSafetyPolicy
import com.focuslock.domain.safety.SafetyPolicy
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SafetyModule {

    @Binds
    @Singleton
    abstract fun bindSafetyPolicy(impl: AndroidSafetyPolicy): SafetyPolicy
}
