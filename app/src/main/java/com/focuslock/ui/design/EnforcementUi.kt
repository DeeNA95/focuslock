package com.focuslock.ui.design

import com.focuslock.domain.model.EnforcementMode

/**
 * Single source of truth for how an [EnforcementMode] is presented in the UI,
 * so screens do not compare raw enum names.
 */
val EnforcementMode.isHardLock: Boolean
    get() = this == EnforcementMode.HARD

val EnforcementMode.modeLabel: String
    get() = if (isHardLock) "Hard Lock" else "Soft Lock"

val EnforcementMode.modeShortLabel: String
    get() = if (isHardLock) "Hard" else "Soft"

val EnforcementMode.modeBadgeTone: BadgeTone
    get() = if (isHardLock) BadgeTone.Amber else BadgeTone.Neutral
