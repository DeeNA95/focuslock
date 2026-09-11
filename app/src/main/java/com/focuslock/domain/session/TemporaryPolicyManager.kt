package com.focuslock.domain.session

import com.focuslock.domain.model.FocusSession

/**
 * Applies and restores temporary device policies used by Fortress Mode
 * (date/time configuration restriction, automatic time, uninstall protection).
 *
 * Implementations must track exactly what they changed so that the previous
 * state can be restored correctly on session expiry, and must never blindly
 * overwrite global settings.
 */
interface TemporaryPolicyManager {
    suspend fun apply(session: FocusSession)
    suspend fun restore(session: FocusSession)

    /**
     * Restore any temporary policy FocusLock may have applied, even when the
     * originating session is no longer available (e.g. crash during expiry).
     * Must be idempotent and only ever undo changes FocusLock itself made.
     */
    suspend fun restoreAll()
}
