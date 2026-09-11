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

    /**
     * Identifier of the current boot, stable for the lifetime of a boot and
     * different after a reboot. Unlike [elapsedRealtimeMillis] it can be
     * compared against a value persisted before a reboot, so reboot detection
     * does not depend on elapsed uptime heuristics.
     */
    fun bootId(): Long

    /** The zone used to interpret local-time rules (activation windows, etc.). */
    fun zoneId(): ZoneId
}
