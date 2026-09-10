package com.focuslock.data.db

import com.focuslock.data.entity.ActivationWindowEntity
import com.focuslock.data.entity.FocusProfileEntity
import com.focuslock.data.entity.FocusSessionEntity
import com.focuslock.data.entity.ProfileBlockedPackageEntity
import com.focuslock.data.entity.SessionBlockedPackageEntity
import com.focuslock.data.entity.SessionEventEntity
import com.focuslock.data.entity.TagBindingEntity
import com.focuslock.domain.model.ActivationWindow
import com.focuslock.domain.model.EnforcementMode
import com.focuslock.domain.model.FocusProfile
import com.focuslock.domain.model.FocusSession
import com.focuslock.domain.model.SessionEvent
import com.focuslock.domain.model.SessionStatus
import com.focuslock.domain.model.TagBinding
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.util.UUID

// ---------------------------------------------------------------------------
// Day-of-week mask helpers
// ---------------------------------------------------------------------------

private fun Set<DayOfWeek>.toMask(): Int = fold(0) { acc, day -> acc or (1 shl day.value) }

private fun Int.toDaysOfWeek(): Set<DayOfWeek> =
    DayOfWeek.entries.filter { (this and (1 shl it.value)) != 0 }.toSet()

// ---------------------------------------------------------------------------
// Profile
// ---------------------------------------------------------------------------

fun FocusProfile.toEntity(): FocusProfileEntity = FocusProfileEntity(
    id = id.toString(),
    name = name,
    durationMillis = duration.toMillis(),
    enforcementMode = enforcementMode.name,
    fortressModeEnabled = fortressModeEnabled,
    enabled = enabled,
    motto = motto,
)

fun FocusProfile.toBlockedPackageEntities(): List<ProfileBlockedPackageEntity> =
    blockedPackages.map { ProfileBlockedPackageEntity(profileId = id.toString(), packageName = it) }

fun FocusProfile.toActivationWindowEntities(): List<ActivationWindowEntity> =
    activationWindows.map { window ->
        ActivationWindowEntity(
            id = UUID.randomUUID().toString(),
            profileId = id.toString(),
            startSecondOfDay = window.start.toSecondOfDay(),
            endSecondOfDay = window.end.toSecondOfDay(),
            daysOfWeekMask = window.daysOfWeek.toMask(),
        )
    }

fun FocusProfileWithDetails.toDomain(): FocusProfile = FocusProfile(
    id = UUID.fromString(profile.id),
    name = profile.name,
    duration = Duration.ofMillis(profile.durationMillis),
    blockedPackages = blockedPackages.map { it.packageName }.toSet(),
    activationWindows = activationWindows.map { it.toDomain() },
    enforcementMode = EnforcementMode.valueOf(profile.enforcementMode),
    fortressModeEnabled = profile.fortressModeEnabled,
    enabled = profile.enabled,
    motto = profile.motto,
)

fun ActivationWindowEntity.toDomain(): ActivationWindow = ActivationWindow(
    start = LocalTime.ofSecondOfDay(startSecondOfDay.toLong()),
    end = LocalTime.ofSecondOfDay(endSecondOfDay.toLong()),
    daysOfWeek = daysOfWeekMask.toDaysOfWeek(),
)

// ---------------------------------------------------------------------------
// Session
// ---------------------------------------------------------------------------

fun FocusSession.toEntity(): FocusSessionEntity = FocusSessionEntity(
    id = id.toString(),
    profileId = profileId?.toString(),
    profileNameSnapshot = profileNameSnapshot,
    startedAtWallClockMillis = startedAtWallClock.toEpochMilli(),
    startedAtElapsedRealtimeMs = startedAtElapsedRealtimeMs,
    expiresAtWallClockMillis = expiresAtWallClock.toEpochMilli(),
    durationMillis = duration.toMillis(),
    enforcementMode = enforcementMode.name,
    fortressModeEnabled = fortressModeEnabled,
    mottoSnapshot = mottoSnapshot,
    status = status.name,
)

fun FocusSession.toBlockedPackageEntities(): List<SessionBlockedPackageEntity> =
    blockedPackagesSnapshot.map { SessionBlockedPackageEntity(sessionId = id.toString(), packageName = it) }

fun FocusSessionWithDetails.toDomain(): FocusSession = FocusSession(
    id = UUID.fromString(session.id),
    profileId = session.profileId?.let(UUID::fromString),
    profileNameSnapshot = session.profileNameSnapshot,
    startedAtWallClock = Instant.ofEpochMilli(session.startedAtWallClockMillis),
    startedAtElapsedRealtimeMs = session.startedAtElapsedRealtimeMs,
    expiresAtWallClock = Instant.ofEpochMilli(session.expiresAtWallClockMillis),
    duration = Duration.ofMillis(session.durationMillis),
    blockedPackagesSnapshot = blockedPackages.map { it.packageName }.toSet(),
    enforcementMode = EnforcementMode.valueOf(session.enforcementMode),
    fortressModeEnabled = session.fortressModeEnabled,
    mottoSnapshot = session.mottoSnapshot,
    status = SessionStatus.valueOf(session.status),
)

// ---------------------------------------------------------------------------
// Tag
// ---------------------------------------------------------------------------

fun TagBinding.toEntity(): TagBindingEntity = TagBindingEntity(
    id = id.toString(),
    token = token,
    label = label,
    profileId = profileId.toString(),
)

fun TagBindingEntity.toDomain(): TagBinding = TagBinding(
    id = UUID.fromString(id),
    token = token,
    label = label,
    profileId = UUID.fromString(profileId),
)

// ---------------------------------------------------------------------------
// Event
// ---------------------------------------------------------------------------

fun SessionEvent.toEntity(): SessionEventEntity = SessionEventEntity(
    id = id,
    timestampMillis = timestamp.toEpochMilli(),
    type = type,
    sessionId = sessionId?.toString(),
    detail = detail,
)

fun SessionEventEntity.toDomain(): SessionEvent = SessionEvent(
    id = id,
    timestamp = Instant.ofEpochMilli(timestampMillis),
    type = type,
    sessionId = sessionId?.let(UUID::fromString),
    detail = detail,
)
