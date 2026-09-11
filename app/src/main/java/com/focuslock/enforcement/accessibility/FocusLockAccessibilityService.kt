package com.focuslock.enforcement.accessibility

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import com.focuslock.R
import com.focuslock.domain.model.EnforcementMode
import com.focuslock.domain.model.FocusSession
import com.focuslock.domain.model.OpenBehavior
import com.focuslock.domain.model.SessionEvent
import com.focuslock.domain.model.SessionStatus
import com.focuslock.domain.repository.EventLogRepository
import com.focuslock.domain.repository.SessionRepository
import com.focuslock.domain.session.SessionClock
import com.focuslock.domain.time.TimeAuthority
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
 * the app icon, the profile motto and remaining time, then sends the user HOME.
 *
 * Only `event.packageName` and `event.eventType` are inspected; a window overlay
 * is drawn over any app but view content is never read.
 */
@AndroidEntryPoint
class FocusLockAccessibilityService : AccessibilityService() {

    @Inject lateinit var sessionRepository: SessionRepository
    @Inject lateinit var sessionClock: SessionClock
    @Inject lateinit var eventLog: EventLogRepository
    @Inject lateinit var timeAuthority: TimeAuthority

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val activeSession = MutableStateFlow<FocusSession?>(null)
    private val lastNotified = ConcurrentHashMap<String, Long>()
    private val allowedUntil = ConcurrentHashMap<String, Long>()
    private val windowManager: WindowManager
        get() = getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val overlay: BlockedAppOverlay by lazy { BlockedAppOverlay(this, windowManager) }
    private val breatheOverlay: BreatheOverlay by lazy { BreatheOverlay(this, windowManager) }
    private var overlayJob: Job? = null
    private var closeJob: Job? = null
    private var breatheJob: Job? = null
    private var overlayPackage: String? = null
    private var closing = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        createChannel()
        scope.launch {
            sessionRepository.observeActiveSession().collect { session ->
                activeSession.value = session
                if (session == null || session.status != SessionStatus.ACTIVE) {
                    dismissAll()
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) return

        val session = activeSession.value ?: return
        if (session.status != SessionStatus.ACTIVE) return

        val packageName = event.packageName?.toString()?.takeIf { it.isNotEmpty() } ?: return
        if (packageName !in session.blockedPackagesSnapshot) return

        val now = SystemClock.elapsedRealtime()
        if (now < (allowedUntil[packageName] ?: 0L)) return

        // Enforce on every foreground transition. Only the notification is
        // rate-limited, so returning to a blocked app via Recents is blocked
        // immediately rather than being suppressed by a cooldown.
        when (session.openBehavior) {
            OpenBehavior.BREATHE -> showBreatheOverlay(session, packageName)
            OpenBehavior.BLOCK -> {
                showBlockOverlay(session, packageName)
                val last = lastNotified[packageName] ?: 0L
                if (now - last >= NOTIFICATION_RATE_LIMIT_MS) {
                    lastNotified[packageName] = now
                    notifyBlocked(packageName, session)
                }
            }
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        super.onDestroy()
        dismissAll()
        scope.cancel()
    }

    private fun showBlockOverlay(session: FocusSession, packageName: String) {
        // Ignore events that arrive while the cover is fading out, otherwise a
        // burst of window events during the transition stacks overlays.
        if (closing) return
        // Already covering this app: keep the existing overlay and timer rather
        // than re-animating on every window event.
        if (overlay.isShowing && overlayPackage == packageName) return

        overlayPackage = packageName
        val label = resolveLabel(packageName) ?: packageName
        logBlockAttempt(session, packageName)

        // Snap the cover on with no entrance animation so it fully hides the
        // blocked app immediately.
        overlay.show(
            BlockedOverlayState(
                profileName = session.profileNameSnapshot,
                motto = session.mottoSnapshot,
                appLabel = label,
                appIcon = resolveIcon(packageName),
                modeLabel = if (session.enforcementMode == EnforcementMode.HARD) "Hard Lock" else "Soft Lock",
                unlockTime = BlockedAppOverlay.formatUnlockTime(session.expiresAtWallClock),
                onDismiss = {
                    // Tapping early must still remove the app from the
                    // foreground, otherwise it would be a one-tap bypass.
                    if (overlay.isShowing) performGlobalAction(GLOBAL_ACTION_HOME)
                    dismissOverlay()
                },
            ),
            animateIn = false,
        )

        overlayJob?.cancel()
        overlayJob = scope.launch {
            while (true) {
                val remaining = sessionClock.remaining(session)
                overlay.update(remaining, progressOf(session, remaining))
                delay(1_000)
            }
        }

        closeJob?.cancel()
        closeJob = scope.launch {
            // Let the opaque cover render, then send the app Home behind it so
            // the app's own close animation is never visible. The cover stays
            // for a beat and is the only thing that animates away.
            delay(HOME_DELAY_MS)
            performGlobalAction(GLOBAL_ACTION_HOME)
            delay(OVERLAY_DURATION_MS)
            dismissOverlay()
        }
    }

    private fun showBreatheOverlay(session: FocusSession, packageName: String) {
        if (closing) return
        if (breatheOverlay.isShowing && overlayPackage == packageName) return

        overlayPackage = packageName
        logBlockAttempt(session, packageName)
        val label = resolveLabel(packageName) ?: packageName

        breatheOverlay.show(
            BreatheOverlayState(
                appLabel = label,
                appIcon = resolveIcon(packageName),
                onDismiss = { allowAndDismiss(packageName) },
            )
        )

        breatheJob?.cancel()
        breatheJob = scope.launch {
            for (remaining in BREATHE_SECONDS downTo 1) {
                breatheOverlay.updateCountdown(remaining)
                delay(1_000)
            }
            allowAndDismiss(packageName)
        }
    }

    /** Let the user through after the pause, with a short grace period. */
    private fun allowAndDismiss(packageName: String) {
        allowedUntil[packageName] = SystemClock.elapsedRealtime() + ALLOW_COOLDOWN_MS
        dismissBreathe()
    }

    private fun dismissBreathe() {
        val wasShowing = breatheOverlay.isShowing
        breatheJob?.cancel()
        breatheJob = null
        overlayPackage = null
        breatheOverlay.hide()
        if (wasShowing) markClosing()
    }

    private fun logBlockAttempt(session: FocusSession, packageName: String) {
        scope.launch {
            eventLog.log(
                SessionEvent(
                    timestamp = timeAuthority.now(),
                    type = SessionEvent.TYPE_BLOCKED_ATTEMPT,
                    sessionId = session.id,
                    detail = packageName,
                )
            )
        }
    }

    private fun dismissOverlay() {
        val wasShowing = overlay.isShowing
        overlayJob?.cancel()
        overlayJob = null
        closeJob?.cancel()
        closeJob = null
        overlayPackage = null
        overlay.hide()
        if (wasShowing) markClosing()
    }

    private fun dismissAll() {
        dismissOverlay()
        dismissBreathe()
    }

    private fun markClosing() {
        closing = true
        scope.launch {
            delay(CLOSE_ANIM_MS)
            closing = false
        }
    }

    private fun progressOf(session: FocusSession, remaining: java.time.Duration): Float =
        if (session.duration.isZero) 0f
        else (remaining.toMillis().toFloat() / session.duration.toMillis()).coerceIn(0f, 1f)

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

    private fun resolveIcon(packageName: String): Drawable? = runCatching {
        packageManager.getApplicationIcon(packageName)
    }.getOrNull()

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
        const val NOTIFICATION_RATE_LIMIT_MS = 3_000L
        const val HOME_DELAY_MS = 120L
        const val OVERLAY_DURATION_MS = 2_500L
        const val CLOSE_ANIM_MS = 240L
        const val BREATHE_SECONDS = 8
        const val ALLOW_COOLDOWN_MS = 60_000L
    }
}
