package com.focuslock.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks whether FocusLock is exempt from battery optimization, and can launch
 * the system dialog to request it.
 *
 * On Samsung devices ("Freecess") an accessibility service can be frozen /
 * killed in the background, which would silently stop blocking. Requesting
 * exemption keeps the service reliably alive so blocked apps are intercepted
 * immediately instead of minutes later.
 */
@Singleton
class BatteryOptimizationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            ?: return false
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun requestExemption() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}
