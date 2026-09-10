package com.focuslock.domain.session

import com.focuslock.domain.model.FocusSession
import java.util.UUID

/**
 * Schedules (and cancels) the wake-up that triggers expiry reconciliation.
 *
 * The deadline stored in [FocusSession] is authoritative; this scheduler only
 * wakes the app to reconcile state toward it.
 */
interface SessionScheduler {
    fun scheduleExpiry(session: FocusSession)
    fun cancel(sessionId: UUID)
}
