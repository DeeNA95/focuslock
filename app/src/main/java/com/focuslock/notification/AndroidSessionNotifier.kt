package com.focuslock.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.focuslock.R
import com.focuslock.domain.model.FocusSession
import com.focuslock.domain.session.SessionNotifier
import com.focuslock.ui.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidSessionNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) : SessionNotifier {

    private val notificationManager =
        context.getSystemService(NotificationManager::class.java)

    init {
        createChannel()
    }

    override fun showActive(session: FocusSession) {
        val manager = notificationManager ?: return
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(session.profileNameSnapshot)
            .setContentText(
                session.mottoSnapshot.ifBlank {
                    "${session.blockedPackagesSnapshot.size} apps locked"
                }
            )
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun cancel() {
        notificationManager?.cancel(NOTIFICATION_ID)
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Focus sessions",
            NotificationManager.IMPORTANCE_LOW,
        )
        notificationManager?.createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "focuslock_session"
        const val NOTIFICATION_ID = 1
    }
}
