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
}
