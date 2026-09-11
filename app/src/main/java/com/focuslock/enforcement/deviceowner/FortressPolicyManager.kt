package com.focuslock.enforcement.deviceowner

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.UserManager
import android.provider.Settings
import com.focuslock.domain.enforcement.EnforcementStateStore
import com.focuslock.domain.model.EnforcementMode
import com.focuslock.domain.model.FocusSession
import com.focuslock.domain.session.TemporaryPolicyManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fortress Mode device policies. Only active for HARD sessions with
 * [FocusSession.fortressModeEnabled] and only while FocusLock is Device Owner.
 *
 * The previous automatic-time state is persisted through
 * [EnforcementStateStore] so it survives process death and can be restored even
 * when the originating session is gone. Only FocusLock's own user restriction
 * is ever removed.
 */
@Singleton
class FortressPolicyManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceOwnerBackend: DeviceOwnerEnforcementBackend,
    private val stateStore: EnforcementStateStore,
) : TemporaryPolicyManager {

    private val dpm = context.getSystemService(DevicePolicyManager::class.java)
    private val admin = ComponentName(context, FocusLockDeviceAdminReceiver::class.java)

    override suspend fun apply(session: FocusSession) {
        if (!shouldApply(session)) return
        withContext(Dispatchers.IO) {
            if (stateStore.autoTimeBefore() == null) {
                val previous = runCatching {
                    Settings.Global.getInt(context.contentResolver, Settings.Global.AUTO_TIME, 1) == 1
                }.getOrDefault(true)
                stateStore.setAutoTimeBefore(previous)
            }
            runCatching { dpm.addUserRestriction(admin, UserManager.DISALLOW_CONFIG_DATE_TIME) }
            runCatching { dpm.setAutoTimeEnabled(admin, true) }
            runCatching { dpm.setUninstallBlocked(admin, context.packageName, true) }
        }
    }

    override suspend fun restore(session: FocusSession) {
        if (!shouldApply(session)) return
        withContext(Dispatchers.IO) { restoreInternal() }
    }

    override suspend fun restoreAll() {
        withContext(Dispatchers.IO) {
            // Only act when Fortress Mode actually recorded a change; otherwise
            // an idle app would poke DevicePolicyManager on every reconcile.
            if (stateStore.autoTimeBefore() == null) return@withContext
            restoreInternal()
        }
    }

    private suspend fun restoreInternal() {
        if (dpm == null) return
        runCatching { dpm.setUninstallBlocked(admin, context.packageName, false) }
        stateStore.autoTimeBefore()?.let { prior ->
            runCatching { dpm.setAutoTimeEnabled(admin, prior) }
            stateStore.clearAutoTimeBefore()
        }
        runCatching { dpm.clearUserRestriction(admin, UserManager.DISALLOW_CONFIG_DATE_TIME) }
    }

    private fun shouldApply(session: FocusSession): Boolean =
        session.fortressModeEnabled &&
            session.enforcementMode == EnforcementMode.HARD &&
            deviceOwnerBackend.isDeviceOwner()
}
