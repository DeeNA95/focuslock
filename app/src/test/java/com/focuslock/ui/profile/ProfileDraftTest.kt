package com.focuslock.ui.profile

import com.focuslock.domain.model.EnforcementMode
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek
import java.time.Duration

class ProfileDraftTest {

    @Test
    fun `toProfile maps name and duration`() {
        val draft = ProfileDraft(
            name = "  Deep Work  ",
            durationHours = 8,
            durationMinutes = 30,
        )
        val profile = draft.toProfile()

        assertThat(profile.name).isEqualTo("Deep Work")
        assertThat(profile.duration).isEqualTo(Duration.ofHours(8).plusMinutes(30))
    }

    @Test
    fun `toProfile with no activation window restriction produces empty windows`() {
        val draft = ProfileDraft(restrictActivation = false)
        assertThat(draft.toProfile().activationWindows).isEmpty()
    }

    @Test
    fun `toProfile builds a single activation window when restricted`() {
        val draft = ProfileDraft(
            restrictActivation = true,
            startHour = 20,
            startMinute = 15,
            endHour = 1,
            endMinute = 30,
            daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY),
        )
        val windows = draft.toProfile().activationWindows

        assertThat(windows).hasSize(1)
        assertThat(windows[0].start.hour).isEqualTo(20)
        assertThat(windows[0].start.minute).isEqualTo(15)
        assertThat(windows[0].end.hour).isEqualTo(1)
        assertThat(windows[0].daysOfWeek).containsExactly(DayOfWeek.MONDAY, DayOfWeek.TUESDAY)
    }

    @Test
    fun `toProfile preserves blocked packages and mode`() {
        val draft = ProfileDraft(
            blockedPackages = setOf("com.instagram.android", "com.reddit.frontpage"),
            enforcementMode = EnforcementMode.HARD,
            fortressModeEnabled = true,
        )
        val profile = draft.toProfile()

        assertThat(profile.blockedPackages)
            .containsExactly("com.instagram.android", "com.reddit.frontpage")
        assertThat(profile.enforcementMode).isEqualTo(EnforcementMode.HARD)
        assertThat(profile.fortressModeEnabled).isTrue()
    }

    @Test
    fun `isValid requires a name and at least one minute`() {
        assertThat(ProfileDraft(name = "", durationHours = 8).isValid).isFalse()
        assertThat(ProfileDraft(name = "Deep Work", durationHours = 0, durationMinutes = 0).isValid).isFalse()
        assertThat(ProfileDraft(name = "Deep Work", durationHours = 0, durationMinutes = 1).isValid).isTrue()
        assertThat(ProfileDraft(name = "Deep Work", durationHours = 8).isValid).isTrue()
    }

    @Test
    fun `existing profile id is preserved in toProfile`() {
        val id = java.util.UUID.randomUUID()
        val draft = ProfileDraft(id = id, name = "Gym", durationHours = 2)
        assertThat(draft.toProfile().id).isEqualTo(id)
    }

    @Test
    fun `save preserves activation windows the editor does not expose`() {
        val extra = com.focuslock.domain.model.ActivationWindow(
            start = java.time.LocalTime.of(6, 0),
            end = java.time.LocalTime.of(7, 0),
        )
        val draft = ProfileDraft(
            name = "Deep Work",
            restrictActivation = true,
            extraActivationWindows = listOf(extra),
        )

        val windows = draft.toProfile().activationWindows

        assertThat(windows).hasSize(2)
        assertThat(windows[1]).isEqualTo(extra)
    }
}
