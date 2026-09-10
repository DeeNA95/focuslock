package com.focuslock.domain.session

import com.focuslock.domain.model.FocusSession

/**
 * Posts/clears the persistent "session active" notification. Kept behind an
 * interface so the domain/session layer stays free of Android notification
 * types.
 */
interface SessionNotifier {
    fun showActive(session: FocusSession)
    fun cancel()
}
