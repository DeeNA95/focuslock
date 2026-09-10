package com.focuslock.domain.time

import java.time.Instant
import java.time.ZoneId

/**
 * The single source of time for all domain logic.
 *
 * Business logic must never call the system clock directly; it must always go
 * through this interface so that tests can inject a fake clock.
 */
interface TimeAuthority {
    /** Current wall-clock instant. */
    fun now(): Instant

    /** Monotonic elapsed realtime in milliseconds (same boot only). */
    fun elapsedRealtimeMillis(): Long

    /** The zone used to interpret local-time rules (activation windows, etc.). */
    fun zoneId(): ZoneId
}
