package com.focuslock.testutil

import com.focuslock.domain.time.TimeAuthority
import java.time.Instant
import java.time.ZoneId

/**
 * Controllable clock for tests. Wall-clock and monotonic elapsed time can be
 * advanced independently to simulate wall-clock manipulation vs real time.
 */
class FakeTimeAuthority(
    private var nowInstant: Instant = Instant.parse("2026-01-01T12:00:00Z"),
    private var elapsedMillis: Long = 0L,
    private val zone: ZoneId = ZoneId.of("UTC"),
    private var boot: Long = 0L,
) : TimeAuthority {

    override fun now(): Instant = nowInstant

    override fun elapsedRealtimeMillis(): Long = elapsedMillis

    override fun bootId(): Long = boot

    override fun zoneId(): ZoneId = zone

    fun advanceWallClock(duration: java.time.Duration) {
        nowInstant = nowInstant.plus(duration)
    }

    fun advanceElapsed(millis: Long) {
        elapsedMillis += millis
    }

    fun setWallClock(instant: Instant) {
        nowInstant = instant
    }

    fun setElapsed(millis: Long) {
        elapsedMillis = millis
    }

    /** Simulate a reboot: elapsed resets and the boot id changes. */
    fun reboot(newBootId: Long = boot + 1) {
        boot = newBootId
        elapsedMillis = 0L
    }
}
