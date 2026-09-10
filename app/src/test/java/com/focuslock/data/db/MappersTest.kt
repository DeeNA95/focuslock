package com.focuslock.data.db

import com.focuslock.domain.model.ActivationWindow
import com.focuslock.domain.model.EnforcementMode
import com.focuslock.domain.model.FocusProfile
import com.focuslock.domain.model.FocusSession
import com.focuslock.domain.model.SessionEvent
import com.focuslock.domain.model.SessionStatus
import com.focuslock.domain.model.TagBinding
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.util.UUID

class MappersTest {

    @Test
    fun `profile round trips through entities`() {
        val profile = FocusProfile(
            id = UUID.randomUUID(),
            name = "Deep Work",
            duration = Duration.ofHours(8),
            blockedPackages = setOf("com.instagram.android", "com.reddit.frontpage"),
            activationWindows = listOf(
                ActivationWindow(
                    start = LocalTime.of(20, 0),
                    end = LocalTime.of(1, 0),
                    daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SUNDAY),
                ),
            ),
            enforcementMode = EnforcementMode.HARD,
            fortressModeEnabled = true,
            enabled = true,
        )

        val entity = profile.toEntity()
        val windows = profile.toActivationWindowEntities()
        val packages = profile.toBlockedPackageEntities()

        val roundTripped = FocusProfileWithDetails(
            profile = entity,
            blockedPackages = packages,
            activationWindows = windows,
        ).toDomain()

        assertThat(roundTripped).isEqualTo(profile)
    }

    @Test
    fun `session round trips through entities`() {
        val started = Instant.parse("2026-01-01T20:00:00Z")
        val session = FocusSession(
            id = UUID.randomUUID(),
            profileId = UUID.randomUUID(),
            profileNameSnapshot = "Deep Work",
            startedAtWallClock = started,
            startedAtElapsedRealtimeMs = 1_234_567L,
            expiresAtWallClock = started.plus(Duration.ofHours(8)),
            duration = Duration.ofHours(8),
            blockedPackagesSnapshot = setOf("com.instagram.android"),
            enforcementMode = EnforcementMode.HARD,
            fortressModeEnabled = false,
            status = SessionStatus.ACTIVE,
        )

        val roundTripped = FocusSessionWithDetails(
            session = session.toEntity(),
            blockedPackages = session.toBlockedPackageEntities(),
        ).toDomain()

        assertThat(roundTripped).isEqualTo(session)
    }

    @Test
    fun `tag binding round trips`() {
        val binding = TagBinding(
            id = UUID.randomUUID(),
            token = "8d476a81-1234-5678-9abc-def012345678",
            label = "Desk",
            profileId = UUID.randomUUID(),
        )

        assertThat(binding.toEntity().toDomain()).isEqualTo(binding)
    }

    @Test
    fun `session event round trips`() {
        val event = SessionEvent(
            id = 42L,
            timestamp = Instant.parse("2026-01-01T21:13:43Z"),
            type = SessionEvent.TYPE_SESSION_ACTIVE,
            sessionId = UUID.randomUUID(),
            detail = "Deep Work",
        )

        assertThat(event.toEntity().toDomain()).isEqualTo(event)
    }

    @Test
    fun `session with null profile and no detail round trips`() {
        val event = SessionEvent(
            timestamp = Instant.parse("2026-01-01T21:13:42Z"),
            type = SessionEvent.TYPE_NFC_TAG_DETECTED,
            sessionId = null,
            detail = null,
        )

        val roundTripped = event.toEntity().toDomain()

        assertThat(roundTripped.sessionId).isNull()
        assertThat(roundTripped.detail).isNull()
        assertThat(roundTripped.type).isEqualTo(SessionEvent.TYPE_NFC_TAG_DETECTED)
    }
}
