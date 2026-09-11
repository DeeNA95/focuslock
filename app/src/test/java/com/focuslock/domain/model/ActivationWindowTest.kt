package com.focuslock.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

class ActivationWindowTest {

    @Test
    fun `normal daytime window contains inner time`() {
        val window = ActivationWindow(
            start = LocalTime.of(18, 0),
            end = LocalTime.of(23, 59),
        )
        assertThat(window.containsTime(LocalTime.of(21, 13))).isTrue()
        assertThat(window.containsTime(LocalTime.of(8, 0))).isFalse()
    }

    @Test
    fun `midnight crossing window contains evening and early morning`() {
        val window = ActivationWindow(
            start = LocalTime.of(20, 0),
            end = LocalTime.of(1, 0),
        )
        assertThat(window.containsTime(LocalTime.of(22, 30))).isTrue()
        assertThat(window.containsTime(LocalTime.of(0, 30))).isTrue()
        assertThat(window.containsTime(LocalTime.of(12, 0))).isFalse()
    }

    @Test
    fun `exact start boundary is inclusive`() {
        val window = ActivationWindow(
            start = LocalTime.of(18, 0),
            end = LocalTime.of(23, 59),
        )
        assertThat(window.containsTime(LocalTime.of(18, 0))).isTrue()
    }

    @Test
    fun `exact end boundary is exclusive`() {
        val window = ActivationWindow(
            start = LocalTime.of(18, 0),
            end = LocalTime.of(23, 59),
        )
        assertThat(window.containsTime(LocalTime.of(23, 59))).isFalse()
    }

    @Test
    fun `midnight crossing exact end boundary is exclusive`() {
        val window = ActivationWindow(
            start = LocalTime.of(20, 0),
            end = LocalTime.of(1, 0),
        )
        assertThat(window.containsTime(LocalTime.of(1, 0))).isFalse()
        assertThat(window.containsTime(LocalTime.of(0, 59))).isTrue()
    }

    @Test
    fun `day of week filtering`() {
        val window = ActivationWindow(
            start = LocalTime.of(18, 0),
            end = LocalTime.of(23, 59),
            daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
        )
        assertThat(window.isOpenAt(LocalTime.of(19, 0), DayOfWeek.MONDAY)).isTrue()
        assertThat(window.isOpenAt(LocalTime.of(19, 0), DayOfWeek.TUESDAY)).isFalse()
        assertThat(window.isOpenAt(LocalTime.of(19, 0), DayOfWeek.WEDNESDAY)).isTrue()
    }

    @Test
    fun `full day window`() {
        val window = ActivationWindow(
            start = LocalTime.MIN,
            end = LocalTime.MAX,
        )
        assertThat(window.containsTime(LocalTime.of(0, 0))).isTrue()
        assertThat(window.containsTime(LocalTime.of(23, 59, 59))).isTrue()
    }

    @Test
    fun `midnight crossing honors the previous day selection after midnight`() {
        val window = ActivationWindow(
            start = LocalTime.of(20, 0),
            end = LocalTime.of(1, 0),
            daysOfWeek = setOf(DayOfWeek.MONDAY),
        )

        // Monday evening belongs to Monday.
        assertThat(window.isOpenAt(LocalTime.of(22, 0), DayOfWeek.MONDAY)).isTrue()
        // Tuesday 00:30 is the tail of Monday's window.
        assertThat(window.isOpenAt(LocalTime.of(0, 30), DayOfWeek.TUESDAY)).isTrue()
        // Tuesday evening does not.
        assertThat(window.isOpenAt(LocalTime.of(22, 0), DayOfWeek.TUESDAY)).isFalse()
        // Sunday 00:30 is the tail of Saturday, which is not selected.
        assertThat(window.isOpenAt(LocalTime.of(0, 30), DayOfWeek.SUNDAY)).isFalse()
    }
}
