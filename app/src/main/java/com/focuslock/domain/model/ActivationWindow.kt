package com.focuslock.domain.model

import java.time.DayOfWeek
import java.time.LocalTime

/**
 * A recurring window during which a profile may be activated.
 *
 * Supports midnight-crossing windows (e.g. 20:00 -> 01:00) and a selection of
 * days of the week. The window only governs *when a session may start*; once a
 * session is active its duration is independent of the window.
 */
data class ActivationWindow(
    val start: LocalTime,
    val end: LocalTime,
    val daysOfWeek: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
) {
    /** Whether this window is defined for the given day. */
    fun includes(day: DayOfWeek): Boolean = day in daysOfWeek

    /**
     * Whether [time] falls within [start, end), correctly handling midnight
     * crossing where `start > end`.
     */
    fun containsTime(time: LocalTime): Boolean = isWithinWindow(time, start, end)

    /** True when the window is open at [time] on [day]. */
    fun isOpenAt(time: LocalTime, day: DayOfWeek): Boolean =
        includes(day) && containsTime(time)

    companion object {
        /**
         * Returns true when [now] lies within [start, end).
         *
         * When [start] <= [end] the window is a normal daytime window.
         * When [start] > [end] the window crosses midnight:
         *   now >= start  OR  now < end
         */
        fun isWithinWindow(now: LocalTime, start: LocalTime, end: LocalTime): Boolean {
            return if (start <= end) {
                now >= start && now < end
            } else {
                now >= start || now < end
            }
        }
    }
}
