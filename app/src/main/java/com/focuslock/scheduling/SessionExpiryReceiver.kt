package com.focuslock.scheduling

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
 * Wakes the app at session expiry and runs reconciliation.
 */
@AndroidEntryPoint
class SessionExpiryReceiver : BroadcastReceiver() {

    @Inject lateinit var reconciler: SessionReconciler

    override fun onReceive(context: Context, intent: Intent) {
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
