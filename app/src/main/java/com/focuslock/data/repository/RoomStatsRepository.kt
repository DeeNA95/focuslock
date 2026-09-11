package com.focuslock.data.repository

import com.focuslock.data.dao.BlockedAppCount
import com.focuslock.data.dao.EventDao
import com.focuslock.data.dao.SessionDao
import com.focuslock.data.entity.FocusSessionEntity
import com.focuslock.domain.model.BlockedAppAttempt
import com.focuslock.domain.model.FocusStats
import com.focuslock.domain.repository.AppRepository
import com.focuslock.domain.repository.StatsRepository
import com.focuslock.domain.time.TimeAuthority
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomStatsRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val eventDao: EventDao,
    private val appRepository: AppRepository,
    private val timeAuthority: TimeAuthority,
) : StatsRepository {

    override fun observeStats(): Flow<FocusStats> = combine(
        sessionDao.observeCompletedSessions(),
        eventDao.observeBlockedAppCounts(TOP_BLOCKED_APPS),
    ) { sessions, blocked ->
        val labels = runCatching {
            appRepository.getLaunchableApps().associate { it.packageName to it.label }
        }.getOrDefault(emptyMap())
        buildStats(sessions, blocked, labels)
    }

    private fun buildStats(
        sessions: List<FocusSessionEntity>,
        blocked: List<BlockedAppCount>,
        labels: Map<String, String>,
    ): FocusStats {
        val zone = timeAuthority.zoneId()
        val today = timeAuthority.now().atZone(zone).toLocalDate()
        val recentCutoff = today.minusDays(RECENT_DAYS - 1L)

        val days = sessions
            .map { Instant.ofEpochMilli(it.startedAtWallClockMillis).atZone(zone).toLocalDate() }
            .toSortedSet()

        val totalMillis = sessions.sumOf { it.durationMillis }
        val recentMillis = sessions
            .filter {
                Instant.ofEpochMilli(it.startedAtWallClockMillis).atZone(zone).toLocalDate() >= recentCutoff
            }
            .sumOf { it.durationMillis }

        return FocusStats(
            totalFocus = Duration.ofMillis(totalMillis),
            recentFocus = Duration.ofMillis(recentMillis),
            completedSessions = sessions.size,
            currentStreakDays = currentStreak(days, today),
            bestStreakDays = bestStreak(days),
            blockedAttempts = blocked.map {
                BlockedAppAttempt(
                    packageName = it.packageName,
                    label = labels[it.packageName] ?: it.packageName,
                    count = it.count,
                )
            },
        )
    }

    private fun currentStreak(days: Set<LocalDate>, today: LocalDate): Int {
        var cursor = if (today in days) today else today.minusDays(1)
        if (cursor !in days) return 0
        var streak = 0
        while (cursor in days) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    private fun bestStreak(days: Iterable<LocalDate>): Int {
        var best = 0
        var run = 0
        var previous: LocalDate? = null
        for (day in days) {
            run = if (previous != null && day == previous.plusDays(1)) run + 1 else 1
            best = maxOf(best, run)
            previous = day
        }
        return best
    }

    private companion object {
        const val TOP_BLOCKED_APPS = 8
        const val RECENT_DAYS = 7L
    }
}
