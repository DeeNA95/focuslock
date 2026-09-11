package com.focuslock.domain.model

import java.time.Duration

/** Aggregate focus statistics shown on the Stats screen. */
data class FocusStats(
    val totalFocus: Duration = Duration.ZERO,
    val recentFocus: Duration = Duration.ZERO,
    val completedSessions: Int = 0,
    val currentStreakDays: Int = 0,
    val bestStreakDays: Int = 0,
    val blockedAttempts: List<BlockedAppAttempt> = emptyList(),
)

/** How many times a blocked app was opened during a session. */
data class BlockedAppAttempt(
    val packageName: String,
    val label: String,
    val count: Int,
)
