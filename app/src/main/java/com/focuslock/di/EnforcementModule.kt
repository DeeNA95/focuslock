package com.focuslock.di

import com.focuslock.domain.session.DndPolicy
import com.focuslock.domain.session.SessionNotifier
import com.focuslock.domain.session.SessionScheduler
import com.focuslock.domain.session.TemporaryPolicyManager
import com.focuslock.enforcement.DefaultEnforcementBackendProvider
import com.focuslock.enforcement.EnforcementBackendProvider
import com.focuslock.enforcement.deviceowner.FortressPolicyManager
import com.focuslock.notification.AndroidDndPolicy
import com.focuslock.notification.AndroidSessionNotifier
import com.focuslock.notification.CompositePolicyManager
import com.focuslock.scheduling.SessionAlarmScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EnforcementModule {

    @Binds
    @Singleton
    abstract fun bindBackendProvider(impl: DefaultEnforcementBackendProvider): EnforcementBackendProvider

    @Binds
    @Singleton
    abstract fun bindTemporaryPolicyManager(impl: CompositePolicyManager): TemporaryPolicyManager

    @Binds
    @Singleton
    abstract fun bindDndPolicy(impl: AndroidDndPolicy): DndPolicy
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SessionModule {

    @Binds
    @Singleton
    abstract fun bindSessionScheduler(impl: SessionAlarmScheduler): SessionScheduler

    @Binds
    @Singleton
    abstract fun bindSessionNotifier(impl: AndroidSessionNotifier): SessionNotifier
}
