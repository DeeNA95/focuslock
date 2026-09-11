package com.focuslock.util

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings

/** Helpers for the Do Not Disturb access grant. */
object NotificationPolicyHelper {

    fun hasAccess(context: Context): Boolean =
        context.getSystemService(NotificationManager::class.java)
            ?.isNotificationPolicyAccessGranted == true

    fun openSettings(context: Context) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
