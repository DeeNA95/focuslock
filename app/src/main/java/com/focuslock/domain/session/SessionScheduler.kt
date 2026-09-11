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

    /**
     * Re-run reconciliation after [delayMs]. Used when an expiry could not be
     * fully verified (e.g. the backend failed to resume some packages) so the
     * session is retried instead of being declared complete prematurely.
     */
    fun scheduleRetry(sessionId: UUID, delayMs: Long)

    fun cancel(sessionId: UUID)
}
