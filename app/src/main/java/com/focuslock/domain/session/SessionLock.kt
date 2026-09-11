package com.focuslock.domain.session

import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-wide lock shared by [SessionManager] and [SessionReconciler].
 *
 * Activation, reconciliation and emergency release must never interleave: a
 * reconcile triggered by a package/time change while an activation is still
 * being applied would otherwise observe the transient `ACTIVATING` row and
 * "clean it up", fighting the activation. Serialising the three operations
 * makes the check-then-create in [SessionManager] safe within the process.
 */
@Singleton
class SessionLock @Inject constructor() {
    val mutex = Mutex()
}
