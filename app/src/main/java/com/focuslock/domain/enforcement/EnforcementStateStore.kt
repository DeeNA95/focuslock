package com.focuslock.domain.enforcement

import com.focuslock.domain.model.EnforcementMode

/**
 * Durable record of the enforcement state FocusLock itself created, independent
 * of any single [com.focuslock.domain.model.FocusSession].
 *
 * Session rows are the source of truth while a session is active, but they can
 * become unreachable (e.g. expiry reconciliation dies after the OS state was
 * already applied). This store records what was actually applied, keyed by the
 * [EnforcementMode] that applied it, so
 * [com.focuslock.domain.session.SessionReconciler] can always find and undo it
 * through the correct backend, even when there is no active session left.
 */
interface EnforcementStateStore {
    /** Packages suspended through [mode] and not yet verified as resumed. */
    suspend fun suspendedPackages(mode: EnforcementMode): Set<String>

    /** Record that [packages] were successfully suspended through [mode]. */
    suspend fun recordSuspended(mode: EnforcementMode, packages: Set<String>)

    /** Record that [packages] were successfully resumed through [mode]. */
    suspend fun clearSuspended(mode: EnforcementMode, packages: Set<String>)

    /** The automatic-time value observed before Fortress Mode changed it, if any. */
    suspend fun autoTimeBefore(): Boolean?

    /** Persist the automatic-time value observed before Fortress Mode changed it. */
    suspend fun setAutoTimeBefore(enabled: Boolean)

    /** Forget the remembered automatic-time value once it has been restored. */
    suspend fun clearAutoTimeBefore()

    /** The interruption filter observed before a session enabled DND, if any. */
    suspend fun dndFilterBefore(): Int?

    /** Persist the interruption filter observed before a session enabled DND. */
    suspend fun setDndFilterBefore(filter: Int)

    /** Forget the remembered interruption filter once it has been restored. */
    suspend fun clearDndFilterBefore()
}
