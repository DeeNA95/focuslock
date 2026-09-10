package com.focuslock.domain.model

import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * An immutable, snapshot-based record of a single commitment.
 *
 * All rules relevant to this session (name, duration, blocked packages,
 * enforcement mode) are copied here at activation time. They cannot be changed
 * afterwards and are unaffected by later edits to the source [FocusProfile].
 *
 * Timekeeping:
 *  - [startedAtElapsedRealtimeMs] + [duration] is authoritative for expiry
 *    while the device remains in the same boot (monotonic clock).
 *  - [startedAtWallClock] / [expiresAtWallClock] are retained for UI display,
 *    reboot recovery and diagnostics.
 */
data class FocusSession(
    val id: UUID,

    val profileId: UUID?,

    val profileNameSnapshot: String,

    val startedAtWallClock: Instant,

    val startedAtElapsedRealtimeMs: Long,

    val expiresAtWallClock: Instant,

    val duration: Duration,

    val blockedPackagesSnapshot: Set<String>,

    val enforcementMode: EnforcementMode,

    val fortressModeEnabled: Boolean,

    val mottoSnapshot: String = "",

    val status: SessionStatus,
)
