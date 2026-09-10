package com.focuslock.data.safety

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.telecom.TelecomManager
import com.focuslock.domain.safety.SafetyPolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android-aware [SafetyPolicy] that discovers critical package names at
 * runtime (launcher, default IME, default dialer, FocusLock itself) and
 * supplements them with a small, stable set of AOSP system packages.
 */
@Singleton
class AndroidSafetyPolicy @Inject constructor(
    @ApplicationContext context: Context,
) : SafetyPolicy {

    private val protectedSet: Set<String> = buildProtectedSet(context)

    override fun isProtected(packageName: String): Boolean =
        packageName in protectedSet

    companion object {
        fun buildProtectedSet(context: Context): Set<String> {
            val result = mutableSetOf<String>()

            // FocusLock itself.
            result.add(context.packageName)

            // Default launcher.
            resolvePackage(
                context,
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            )?.let(result::add)

            // Default dialer (runtime discovery).
            runCatching {
                val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                telecom?.defaultDialerPackage?.let(result::add)
            }

            // Active input method (runtime discovery).
            runCatching {
                Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.DEFAULT_INPUT_METHOD,
                )?.substringBefore('/')?.takeIf { it.isNotBlank() }?.let(result::add)
            }

            // Stable AOSP critical packages.
            result.addAll(
                setOf(
                    "com.android.systemui",
                    "com.android.settings",
                    "com.android.permissioncontroller",
                    "com.google.android.permissioncontroller",
                    "com.android.packageinstaller",
                    "com.google.android.packageinstaller",
                    "com.google.android.gms",
                    "com.android.phone",
                    "com.android.emergency",
                )
            )

            return result
        }

        private fun resolvePackage(context: Context, intent: Intent): String? =
            runCatching {
                context.packageManager.resolveActivity(intent, 0)?.activityInfo?.packageName
            }.getOrNull()
    }
}
