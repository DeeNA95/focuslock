package com.focuslock.domain.session

import com.focuslock.domain.model.FocusSession
import com.focuslock.domain.time.TimeAuthority
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Authoritative session expiry calculation.
 *
 * While the device remains within the same boot, expiry is computed from the
 * monotonic elapsed-realtime clock so that changing the wall clock forward
 * cannot shorten a session.
 *
 * After a reboot the elapsed clock resets, so we fall back to the wall-clock
 * expiry snapshot.
 */
@Singleton
class SessionClock @Inject constructor(
    private val timeAuthority: TimeAuthority,
) {
    /** Whether the device has rebooted since the session started. */
    fun hasRebooted(session: FocusSession): Boolean {
        val currentBoot = timeAuthority.bootId()
        // Prefer the persisted boot id; fall back to the elapsed-clock heuristic
        // for sessions created before boot ids were recorded (or devices that
        // do not expose a boot count).
        if (session.startedAtBootId != 0L && currentBoot != 0L) {
            return currentBoot != session.startedAtBootId
        }
        return timeAuthority.elapsedRealtimeMillis() < session.startedAtElapsedRealtimeMs
    }

    /** Time elapsed since the session started, using the correct clock. */
    fun elapsedSinceStart(session: FocusSession): Duration {
        if (hasRebooted(session)) {
            // Rebooted: monotonic clock reset, so fall back to wall clock.
            return Duration.between(session.startedAtWallClock, timeAuthority.now())
        }
        val elapsed = timeAuthority.elapsedRealtimeMillis() - session.startedAtElapsedRealtimeMs
        return Duration.ofMillis(elapsed)
    }

    /** True when the session has reached or passed its duration. */
    fun isExpired(session: FocusSession): Boolean {
        if (hasRebooted(session)) {
            // Rebooted: rely on the wall-clock expiry snapshot.
            return !timeAuthority.now().isBefore(session.expiresAtWallClock)
        }
        // Same boot: monotonic comparison, immune to wall-clock changes.
        val elapsed = timeAuthority.elapsedRealtimeMillis() - session.startedAtElapsedRealtimeMs
        return elapsed >= session.duration.toMillis()
    }

    /** Remaining time, clamped to zero. */
    fun remaining(session: FocusSession): Duration {
        val elapsed = elapsedSinceStart(session)
        val left = session.duration.minus(elapsed)
        return if (left.isNegative) Duration.ZERO else left
    }
}
