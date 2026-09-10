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
    /** Whether the elapsed clock indicates the device has rebooted since the session started. */
    fun hasRebooted(session: FocusSession): Boolean =
        timeAuthority.elapsedRealtimeMillis() < session.startedAtElapsedRealtimeMs

    /** Time elapsed since the session started, using the correct clock. */
    fun elapsedSinceStart(session: FocusSession): Duration {
        val elapsedNow = timeAuthority.elapsedRealtimeMillis()
        return if (elapsedNow >= session.startedAtElapsedRealtimeMs) {
            Duration.ofMillis(elapsedNow - session.startedAtElapsedRealtimeMs)
        } else {
            // Rebooted: monotonic clock reset, so fall back to wall clock.
            Duration.between(session.startedAtWallClock, timeAuthority.now())
        }
    }

    /** True when the session has reached or passed its duration. */
    fun isExpired(session: FocusSession): Boolean {
        val elapsedNow = timeAuthority.elapsedRealtimeMillis()
        return if (elapsedNow >= session.startedAtElapsedRealtimeMs) {
            // Same boot: monotonic comparison, immune to wall-clock changes.
            elapsedNow - session.startedAtElapsedRealtimeMs >= session.duration.toMillis()
        } else {
            // Rebooted: rely on the wall-clock expiry snapshot.
            !timeAuthority.now().isBefore(session.expiresAtWallClock)
        }
    }

    /** Remaining time, clamped to zero. */
    fun remaining(session: FocusSession): Duration {
        val elapsed = elapsedSinceStart(session)
        val left = session.duration.minus(elapsed)
        return if (left.isNegative) Duration.ZERO else left
    }
}
