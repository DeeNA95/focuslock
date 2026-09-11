package com.focuslock.data

import com.focuslock.domain.model.ActivationWindow
import com.focuslock.domain.model.EnforcementMode
import com.focuslock.domain.model.FocusProfile
import com.focuslock.domain.repository.AppRepository
import com.focuslock.domain.repository.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds two starter profiles ("Deep Work" and "Sleep") with sensible defaults.
 *
 * Blocked apps are auto-selected from a curated list of well-known distracting
 * apps, filtered to only those actually installed on the device.
 */
@Singleton
class DefaultProfilesInstaller @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val appRepository: AppRepository,
) {

    private val workBlocklist = listOf(
        "com.instagram.android",
        "com.twitter.android",
        "com.reddit.frontpage",
        "com.zhiliaoapp.musically", // TikTok
        "com.google.android.youtube",
        "com.android.chrome",
        "com.facebook.katana",
        "com.facebook.orca",
    )

    private val sleepBlocklist = listOf(
        "com.instagram.android",
        "com.twitter.android",
        "com.reddit.frontpage",
        "com.zhiliaoapp.musically",
        "com.google.android.youtube",
        "com.netflix.mediaclient",
        "com.snapchat.android",
        "com.facebook.katana",
    )

    /**
     * Installs defaults only if no default profiles exist yet. Returns true when
     * new profiles were created.
     */
    suspend fun installIfNeeded(): Boolean = withContext(Dispatchers.IO) {
        val existing = profileRepository.getAllProfiles()
        if (existing.any { it.name == DEEP_WORK || it.name == SLEEP }) return@withContext false

        val installed = appRepository.getLaunchableApps().map { it.packageName }.toSet()

        profileRepository.saveProfile(
            FocusProfile(
                id = UUID.randomUUID(),
                name = DEEP_WORK,
                duration = Duration.ofHours(8),
                blockedPackages = workBlocklist.filter { it in installed }.toSet(),
                activationWindows = listOf(
                    ActivationWindow(
                        start = LocalTime.of(8, 0),
                        end = LocalTime.of(18, 0),
                        daysOfWeek = allDays(),
                    )
                ),
                enforcementMode = EnforcementMode.SOFT,
                motto = "Hold your resolve.",
            )
        )
        profileRepository.saveProfile(
            FocusProfile(
                id = UUID.randomUUID(),
                name = SLEEP,
                duration = Duration.ofHours(7),
                blockedPackages = sleepBlocklist.filter { it in installed }.toSet(),
                activationWindows = listOf(
                    ActivationWindow(
                        start = LocalTime.of(23, 0),
                        end = LocalTime.of(6, 0),
                        daysOfWeek = allDays(),
                    )
                ),
                enforcementMode = EnforcementMode.SOFT,
                enableDnd = true,
                motto = "Put the phone down.",
            )
        )
        true
    }

    private fun allDays(): Set<DayOfWeek> = DayOfWeek.entries.toSet()

    companion object {
        const val DEEP_WORK = "Deep Work"
        const val SLEEP = "Sleep"
    }
}
