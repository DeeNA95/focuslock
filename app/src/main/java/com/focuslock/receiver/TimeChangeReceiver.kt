package com.focuslock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.focuslock.domain.session.SessionReconciler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Reconciles on time/timezone changes. Same-boot expiry uses the monotonic
 * clock so these changes cannot shorten a session, but reconciling keeps the
 * scheduled alarm and notification consistent.
 */
@AndroidEntryPoint
class TimeChangeReceiver : BroadcastReceiver() {

    @Inject lateinit var reconciler: SessionReconciler

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        reconciler.reconcile()
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
