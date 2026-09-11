package com.focuslock.notification

import android.app.NotificationManager
import android.content.Context
import com.focuslock.domain.enforcement.EnforcementStateStore
import com.focuslock.domain.model.FocusSession
import com.focuslock.domain.session.DndPolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Do Not Disturb via [NotificationManager]. Requires the user to grant
 * "Do Not Disturb access"; if it has not been granted the policy is skipped so
 * a session still starts. The previous interruption filter is persisted so it
 * is restored even across process death.
 */
@Singleton
class AndroidDndPolicy @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stateStore: EnforcementStateStore,
) : DndPolicy {

    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    override suspend fun apply(session: FocusSession) {
        if (!session.enableDnd) return
        withContext(Dispatchers.IO) {
            val manager = notificationManager ?: return@withContext
            if (!manager.isNotificationPolicyAccessGranted) return@withContext
            if (stateStore.dndFilterBefore() == null) {
                stateStore.setDndFilterBefore(manager.currentInterruptionFilter)
            }
            runCatching {
                manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            }
        }
    }

    override suspend fun restore(session: FocusSession) {
        if (!session.enableDnd) return
        withContext(Dispatchers.IO) { restoreInternal() }
    }

    override suspend fun restoreAll() {
        withContext(Dispatchers.IO) {
            if (stateStore.dndFilterBefore() == null) return@withContext
            restoreInternal()
        }
    }

    private suspend fun restoreInternal() {
        val manager = notificationManager ?: return
        stateStore.dndFilterBefore()?.let { prior ->
            runCatching { manager.setInterruptionFilter(prior) }
            stateStore.clearDndFilterBefore()
        }
    }
}
