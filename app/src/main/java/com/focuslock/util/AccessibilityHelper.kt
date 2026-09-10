package com.focuslock.util

import android.content.Context
import android.content.Intent
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccessibilityHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Opens the accessibility settings page for FocusLock directly (falls back
     * to the full list if the details page cannot be resolved).
     */
    fun openSettings() {
        val details = Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS")
            .putExtra(
                "accessibility_device_name",
                "com.focuslock/com.focuslock.enforcement.accessibility.FocusLockAccessibilityService",
            )
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val opened = runCatching { context.startActivity(details) }.isSuccess
        if (!opened) {
            val list = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(list) }
        }
    }
}
