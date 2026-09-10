package com.focuslock.enforcement.accessibility

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.focuslock.R
import com.focuslock.domain.model.FocusSession
import com.focuslock.domain.model.SessionStatus
import com.focuslock.domain.repository.SessionRepository
import com.focuslock.domain.session.SessionClock
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * Observes the foreground package. When a blocked app becomes foreground this
 * service shows a full-screen overlay (the "resist the temptation" screen) with
 * the profile name, motto and remaining time, then sends the user HOME.
 *
 * Only `event.packageName` and `event.eventType` are inspected; a window overlay
 * is drawn over any app but view content is never read.
 */
@AndroidEntryPoint
class FocusLockAccessibilityService : AccessibilityService() {

    @Inject lateinit var sessionRepository: SessionRepository
    @Inject lateinit var sessionClock: SessionClock

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val activeSession = MutableStateFlow<FocusSession?>(null)
    private val lastBlocked = ConcurrentHashMap<String, Long>()
    private val windowManager: WindowManager
        get() = getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var overlayView: View? = null
    private var overlayJob: Job? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        createChannel()
        scope.launch {
            sessionRepository.observeActiveSession().collect { session ->
                activeSession.value = session
                if (session == null || session.status != SessionStatus.ACTIVE) {
                    dismissOverlay()
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) return

        val session = activeSession.value ?: return
        if (session.status != SessionStatus.ACTIVE) return
        if (packageName !in session.blockedPackagesSnapshot) return

        val now = SystemClock.elapsedRealtime()
        val last = lastBlocked[packageName] ?: 0L
        if (now - last < RATE_LIMIT_MS) return
        lastBlocked[packageName] = now

        showBlockOverlay(session, resolveLabel(packageName) ?: packageName)
        notifyBlocked(packageName, session)

        // Give the overlay a moment, then force the user back to the launcher.
        scope.launch {
            delay(OVERLAY_DURATION_MS)
            performGlobalAction(GLOBAL_ACTION_HOME)
            dismissOverlay()
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        super.onDestroy()
        overlayJob?.cancel()
        dismissOverlay()
        scope.cancel()
    }

    private fun showBlockOverlay(session: FocusSession, blockedLabel: String) {
        dismissOverlay()

        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0E1116"))
            setPadding(dp(32), dp(32), dp(32), dp(32))
        }

        val nameView = TextView(this).apply {
            text = session.profileNameSnapshot.uppercase()
            setTextColor(Color.WHITE)
            textSize = 18f
            gravity = Gravity.CENTER
        }
        root.addView(nameView)

        val appView = TextView(this).apply {
            text = "$blockedLabel is locked"
            setTextColor(Color.parseColor("#9AA4B2"))
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, 0)
        }
        root.addView(appView)

        if (session.mottoSnapshot.isNotBlank()) {
            val mottoView = TextView(this).apply {
                text = "\u201C${session.mottoSnapshot}\u201D"
                setTextColor(Color.parseColor("#4CAF50"))
                textSize = 20f
                gravity = Gravity.CENTER
                setPadding(0, dp(32), 0, 0)
            }
            root.addView(mottoView)
        }

        val remainingView = TextView(this).apply {
            setTextColor(Color.parseColor("#9AA4B2"))
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, dp(48), 0, 0)
        }
        root.addView(remainingView)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.CENTER
        }

        windowManager.addView(root, params)
        overlayView = root

        overlayJob = scope.launch {
            while (true) {
                remainingView.text = formatRemaining(sessionClock.remaining(session))
                delay(1_000)
            }
        }
    }

    private fun dismissOverlay() {
        overlayJob?.cancel()
        overlayJob = null
        overlayView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        overlayView = null
    }

    private fun notifyBlocked(packageName: String, session: FocusSession) {
        val label = resolveLabel(packageName)
        val title = "${label ?: packageName} is locked"
        val text = session.mottoSnapshot.ifBlank { session.profileNameSnapshot }
        val notification = NotificationCompat.Builder(this, BLOCKED_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        getSystemService(NotificationManager::class.java)?.notify(BLOCKED_NOTIFICATION_ID, notification)
    }

    private fun resolveLabel(packageName: String): String? = runCatching {
        val appInfo = packageManager.getApplicationInfo(packageName, 0)
        packageManager.getApplicationLabel(appInfo).toString()
    }.getOrNull()

    private fun formatRemaining(duration: java.time.Duration): String {
        val total = duration.seconds.coerceAtLeast(0)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return "Unlocks in %02d:%02d:%02d".format(h, m, s)
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            BLOCKED_CHANNEL_ID,
            "Blocked app attempts",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            enableVibration(true)
            enableLights(false)
        }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    companion object {
        const val COMPONENT_FLATTENED =
            "com.focuslock/com.focuslock.enforcement.accessibility.FocusLockAccessibilityService"
        const val BLOCKED_CHANNEL_ID = "focuslock_blocked_v2"
        const val BLOCKED_NOTIFICATION_ID = 2
        const val RATE_LIMIT_MS = 3_000L
        const val OVERLAY_DURATION_MS = 2_500L
    }
}
