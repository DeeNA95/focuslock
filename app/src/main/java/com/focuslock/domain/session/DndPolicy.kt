package com.focuslock.domain.session

import com.focuslock.domain.model.FocusSession

/**
 * Applies and restores Do Not Disturb for sessions that request it.
 *
 * Implementations must record the interruption filter that was active before
 * changing it so the exact previous state can be restored, and must be
 * idempotent.
 */
interface DndPolicy {
    suspend fun apply(session: FocusSession)
    suspend fun restore(session: FocusSession)
    suspend fun restoreAll()
}
