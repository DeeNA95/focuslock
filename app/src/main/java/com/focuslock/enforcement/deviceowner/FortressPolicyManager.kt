package com.focuslock.enforcement.deviceowner

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.UserManager
import android.provider.Settings
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
 * Tracks the previous automatic-time state so it can be restored, and removes
 * only FocusLock's own user restriction.
 */
@Singleton
class FortressPolicyManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceOwnerBackend: DeviceOwnerEnforcementBackend,
) : TemporaryPolicyManager {

    private val dpm = context.getSystemService(DevicePolicyManager::class.java)
    private val admin = ComponentName(context, FocusLockDeviceAdminReceiver::class.java)
    private var previousAutoTime: Boolean? = null

    override suspend fun apply(session: FocusSession) {
        if (!shouldApply(session)) return
        withContext(Dispatchers.IO) {
            if (previousAutoTime == null) {
                previousAutoTime = runCatching {
                    Settings.Global.getInt(context.contentResolver, Settings.Global.AUTO_TIME, 1) == 1
                }.getOrDefault(true)
            }
            runCatching { dpm.addUserRestriction(admin, UserManager.DISALLOW_CONFIG_DATE_TIME) }
            runCatching { dpm.setAutoTimeEnabled(admin, true) }
            runCatching { dpm.setUninstallBlocked(admin, context.packageName, true) }
        }
    }

    override suspend fun restore(session: FocusSession) {
        if (!shouldApply(session)) return
        withContext(Dispatchers.IO) {
            runCatching { dpm.setUninstallBlocked(admin, context.packageName, false) }
            previousAutoTime?.let { prior -> runCatching { dpm.setAutoTimeEnabled(admin, prior) } }
            runCatching { dpm.clearUserRestriction(admin, UserManager.DISALLOW_CONFIG_DATE_TIME) }
            previousAutoTime = null
        }
    }

    private fun shouldApply(session: FocusSession): Boolean =
        session.fortressModeEnabled &&
            session.enforcementMode == EnforcementMode.HARD &&
            deviceOwnerBackend.isDeviceOwner()
}
