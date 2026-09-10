package com.focuslock.domain.session

import com.focuslock.domain.model.EnforcementMode
import com.focuslock.domain.model.FocusSession
import com.focuslock.domain.model.SessionStatus
import com.focuslock.testutil.FakeTimeAuthority
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.util.UUID

class SessionClockTest {

    private fun session(
        startedAtWallClock: Instant = Instant.parse("2026-01-01T20:00:00Z"),
        startedAtElapsedMs: Long = 1_000_000L,
        duration: Duration = Duration.ofHours(8),
    ) = FocusSession(
        id = UUID.randomUUID(),
        profileId = null,
        profileNameSnapshot = "Deep Work",
        startedAtWallClock = startedAtWallClock,
        startedAtElapsedRealtimeMs = startedAtElapsedMs,
        expiresAtWallClock = startedAtWallClock.plus(duration),
        duration = duration,
        blockedPackagesSnapshot = emptySet(),
        enforcementMode = EnforcementMode.HARD,
        fortressModeEnabled = false,
        status = SessionStatus.ACTIVE,
    )

    @Test
    fun `session is not expired when elapsed less than duration`() {
        val clock = FakeTimeAuthority(elapsedMillis = 1_000_000L)
        val sessionClock = SessionClock(clock)
        val s = session(startedAtElapsedMs = 1_000_000L, duration = Duration.ofHours(8))

        clock.advanceElapsed(Duration.ofHours(4).toMillis())

        assertThat(sessionClock.isExpired(s)).isFalse()
    }

    @Test
    fun `session expires exactly at duration`() {
        val clock = FakeTimeAuthority(elapsedMillis = 1_000_000L)
        val sessionClock = SessionClock(clock)
        val s = session(startedAtElapsedMs = 1_000_000L, duration = Duration.ofHours(8))

        clock.advanceElapsed(Duration.ofHours(8).toMillis())

        assertThat(sessionClock.isExpired(s)).isTrue()
    }

    @Test
    fun `session expires past duration`() {
        val clock = FakeTimeAuthority(elapsedMillis = 1_000_000L)
        val sessionClock = SessionClock(clock)
        val s = session(startedAtElapsedMs = 1_000_000L, duration = Duration.ofHours(8))

        clock.advanceElapsed(Duration.ofHours(8).toMillis() + 1)

        assertThat(sessionClock.isExpired(s)).isTrue()
    }

    @Test
    fun `moving wall clock forward does not expire session within same boot`() {
        val clock = FakeTimeAuthority(
            nowInstant = Instant.parse("2026-01-01T20:00:00Z"),
            elapsedMillis = 1_000_000L,
        )
        val sessionClock = SessionClock(clock)
        val s = session(
            startedAtWallClock = Instant.parse("2026-01-01T20:00:00Z"),
            startedAtElapsedMs = 1_000_000L,
            duration = Duration.ofHours(8),
        )

        // User moves wall clock +8 hours forward. Monotonic clock unchanged.
        clock.advanceWallClock(Duration.ofHours(8))

        assertThat(sessionClock.isExpired(s)).isFalse()
        assertThat(sessionClock.remaining(s)).isEqualTo(Duration.ofHours(8))
    }

    @Test
    fun `reboot falls back to wall clock expiry`() {
        val clock = FakeTimeAuthority(
            nowInstant = Instant.parse("2026-01-02T04:00:00Z"),
            // After reboot elapsed clock has reset to a small value.
            elapsedMillis = 5_000L,
        )
        val sessionClock = SessionClock(clock)
        val s = session(
            startedAtWallClock = Instant.parse("2026-01-01T20:00:00Z"),
            startedAtElapsedMs = 1_000_000L,
            duration = Duration.ofHours(8),
        )

        // 8 hours have passed by wall clock, so it should be expired.
        assertThat(sessionClock.hasRebooted(s)).isTrue()
        assertThat(sessionClock.isExpired(s)).isTrue()
    }

    @Test
    fun `reboot before wall clock expiry keeps session active`() {
        val clock = FakeTimeAuthority(
            nowInstant = Instant.parse("2026-01-01T23:00:00Z"),
            elapsedMillis = 5_000L,
        )
        val sessionClock = SessionClock(clock)
        val s = session(
            startedAtWallClock = Instant.parse("2026-01-01T20:00:00Z"),
            startedAtElapsedMs = 1_000_000L,
            duration = Duration.ofHours(8),
        )

        assertThat(sessionClock.hasRebooted(s)).isTrue()
        assertThat(sessionClock.isExpired(s)).isFalse()
    }

    @Test
    fun `remaining clamps to zero after expiry`() {
        val clock = FakeTimeAuthority(elapsedMillis = 1_000_000L)
        val sessionClock = SessionClock(clock)
        val s = session(startedAtElapsedMs = 1_000_000L, duration = Duration.ofHours(1))

        clock.advanceElapsed(Duration.ofHours(2).toMillis())

        assertThat(sessionClock.remaining(s)).isEqualTo(Duration.ZERO)
    }

    @Test
    fun `remaining decreases as elapsed advances`() {
        val clock = FakeTimeAuthority(elapsedMillis = 1_000_000L)
        val sessionClock = SessionClock(clock)
        val s = session(startedAtElapsedMs = 1_000_000L, duration = Duration.ofHours(8))

        clock.advanceElapsed(Duration.ofHours(2).toMillis())

        assertThat(sessionClock.remaining(s)).isEqualTo(Duration.ofHours(6))
    }

    @Test
    fun `timezone change does not affect same-boot expiry`() {
        val clock = FakeTimeAuthority(
            nowInstant = Instant.parse("2026-01-01T20:00:00Z"),
            elapsedMillis = 1_000_000L,
        )
        val sessionClock = SessionClock(clock)
        val s = session(
            startedAtWallClock = Instant.parse("2026-01-01T20:00:00Z"),
            startedAtElapsedMs = 1_000_000L,
            duration = Duration.ofHours(8),
        )

        // Advance 7 hours of real time (monotonic) regardless of timezone.
        clock.advanceElapsed(Duration.ofHours(7).toMillis())

        assertThat(sessionClock.isExpired(s)).isFalse()
        assertThat(sessionClock.remaining(s)).isEqualTo(Duration.ofHours(1))
    }
}
