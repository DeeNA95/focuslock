package com.focuslock.data.safety

import android.content.Context
import android.content.Intent
import android.os.SystemClock
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
 *
 * Runtime-discovered entries (launcher, IME, dialer) can change while the app
 * is alive, so the set is cached only briefly rather than snapshotted once.
 */
@Singleton
class AndroidSafetyPolicy @Inject constructor(
    @ApplicationContext private val context: Context,
) : SafetyPolicy {

    @Volatile
    private var cachedProtected: Set<String>? = null

    @Volatile
    private var cachedAtElapsedMs: Long = 0L

    override fun isProtected(packageName: String): Boolean = packageName in protectedSet()

    private fun protectedSet(): Set<String> {
        val now = SystemClock.elapsedRealtime()
        val cached = cachedProtected
        if (cached != null && now - cachedAtElapsedMs < CACHE_TTL_MS) return cached
        return buildProtectedSet(context).also {
            cachedProtected = it
            cachedAtElapsedMs = now
        }
    }

    companion object {
        private const val CACHE_TTL_MS = 30_000L

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
