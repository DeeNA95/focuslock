package com.focuslock.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import com.focuslock.domain.model.FocusSession
import com.focuslock.domain.session.SessionClock
import com.focuslock.domain.session.SessionScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules a wake-up at session expiry via [AlarmManager].
 *
 * The trigger is computed from the monotonic elapsed-realtime clock plus the
 * remaining duration, so manually changing the wall clock cannot fire (or delay)
 * the alarm incorrectly. After a reboot the [SessionClock] falls back to the
 * wall-clock snapshot and the BootReceiver re-schedules.
 */
@Singleton
class SessionAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionClock: SessionClock,
) : SessionScheduler {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun scheduleExpiry(session: FocusSession) {
        val alarm = alarmManager ?: return
        val remainingMs = sessionClock.remaining(session).toMillis().coerceAtLeast(MIN_TRIGGER_MS)
        val triggerAt = SystemClock.elapsedRealtime() + remainingMs

        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarm.canScheduleExactAlarms()

        if (canExact) {
            alarm.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAt,
                buildPendingIntent(session.id),
            )
        } else {
            alarm.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAt,
                buildPendingIntent(session.id),
            )
        }
    }

    override fun cancel(sessionId: UUID) {
        alarmManager?.cancel(buildPendingIntent(sessionId))
    }

    private fun buildPendingIntent(sessionId: UUID): PendingIntent {
        val intent = Intent(context, SessionExpiryReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            sessionId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        const val MIN_TRIGGER_MS = 1_000L
    }
}
