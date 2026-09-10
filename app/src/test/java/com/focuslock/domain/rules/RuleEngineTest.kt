package com.focuslock.domain.rules

import com.focuslock.domain.model.ActivationDecision
import com.focuslock.domain.model.ActivationWindow
import com.focuslock.domain.model.EnforcementMode
import com.focuslock.domain.model.FocusProfile
import com.focuslock.testutil.FakeTimeAuthority
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

class RuleEngineTest {

    private fun profile(
        enabled: Boolean = true,
        windows: List<ActivationWindow> = emptyList(),
    ) = FocusProfile(
        id = UUID.randomUUID(),
        name = "Deep Work",
        duration = Duration.ofHours(8),
        blockedPackages = setOf("com.instagram.android"),
        activationWindows = windows,
        enforcementMode = EnforcementMode.HARD,
        enabled = enabled,
    )

    @Test
    fun `profile with no windows is always activatable`() {
        val time = FakeTimeAuthority(Instant.parse("2026-01-01T12:00:00Z"))
        val engine = RuleEngine(time)
        assertThat(engine.canActivate(profile())).isEqualTo(ActivationDecision.Allowed)
    }

    @Test
    fun `disabled profile is rejected regardless of time`() {
        val time = FakeTimeAuthority(Instant.parse("2026-01-01T12:00:00Z"))
        val engine = RuleEngine(time)
        assertThat(engine.canActivate(profile(enabled = false)))
            .isEqualTo(ActivationDecision.ProfileDisabled)
    }

    @Test
    fun `profile inside window is allowed`() {
        val time = FakeTimeAuthority(
            Instant.parse("2026-01-01T18:30:00Z"),
            zone = ZoneId.of("UTC"),
        )
        val engine = RuleEngine(time)
        val windows = listOf(
            ActivationWindow(
                start = LocalTime.of(18, 0),
                end = LocalTime.of(23, 59),
            ),
        )
        assertThat(engine.canActivate(profile(windows = windows)))
            .isEqualTo(ActivationDecision.Allowed)
    }

    @Test
    fun `profile outside window is rejected`() {
        val time = FakeTimeAuthority(
            Instant.parse("2026-01-01T12:00:00Z"),
            zone = ZoneId.of("UTC"),
        )
        val engine = RuleEngine(time)
        val windows = listOf(
            ActivationWindow(
                start = LocalTime.of(18, 0),
                end = LocalTime.of(23, 59),
            ),
        )
        assertThat(engine.canActivate(profile(windows = windows)))
            .isEqualTo(ActivationDecision.OutsideWindow)
    }

    @Test
    fun `midnight crossing window allowed during early morning`() {
        val time = FakeTimeAuthority(
            Instant.parse("2026-01-02T00:30:00Z"),
            zone = ZoneId.of("UTC"),
        )
        val engine = RuleEngine(time)
        val windows = listOf(
            ActivationWindow(
                start = LocalTime.of(20, 0),
                end = LocalTime.of(1, 0),
            ),
        )
        assertThat(engine.canActivate(profile(windows = windows)))
            .isEqualTo(ActivationDecision.Allowed)
    }

    @Test
    fun `midnight crossing window rejected during afternoon`() {
        val time = FakeTimeAuthority(
            Instant.parse("2026-01-02T13:00:00Z"),
            zone = ZoneId.of("UTC"),
        )
        val engine = RuleEngine(time)
        val windows = listOf(
            ActivationWindow(
                start = LocalTime.of(20, 0),
                end = LocalTime.of(1, 0),
            ),
        )
        assertThat(engine.canActivate(profile(windows = windows)))
            .isEqualTo(ActivationDecision.OutsideWindow)
    }

    @Test
    fun `day of week mismatch rejects`() {
        // 2026-01-01 is a Thursday.
        val time = FakeTimeAuthority(
            Instant.parse("2026-01-01T19:00:00Z"),
            zone = ZoneId.of("UTC"),
        )
        val engine = RuleEngine(time)
        val windows = listOf(
            ActivationWindow(
                start = LocalTime.of(18, 0),
                end = LocalTime.of(23, 0),
                daysOfWeek = setOf(DayOfWeek.MONDAY),
            ),
        )
        assertThat(engine.canActivate(profile(windows = windows)))
            .isEqualTo(ActivationDecision.OutsideWindow)
    }

    @Test
    fun `multiple windows where only one is open`() {
        val time = FakeTimeAuthority(
            Instant.parse("2026-01-01T07:00:00Z"),
            zone = ZoneId.of("UTC"),
        )
        val engine = RuleEngine(time)
        val windows = listOf(
            ActivationWindow(
                start = LocalTime.of(18, 0),
                end = LocalTime.of(23, 0),
            ),
            ActivationWindow(
                start = LocalTime.of(6, 0),
                end = LocalTime.of(8, 0),
            ),
        )
        assertThat(engine.canActivate(profile(windows = windows)))
            .isEqualTo(ActivationDecision.Allowed)
    }
}
