package com.focuslock.domain.rules

import com.focuslock.domain.model.ActivationDecision
import com.focuslock.domain.model.FocusProfile
import com.focuslock.domain.time.TimeAuthority
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Evaluates whether a profile may be activated at the current time.
 *
 * Only governs *starting* a session; it is never involved in expiry.
 */
@Singleton
class RuleEngine @Inject constructor(
    private val timeAuthority: TimeAuthority,
) {
    /**
     * Decides whether [profile] may be activated right now.
     *
     * A profile with no activation windows is considered always activatable
     * (no restriction). Otherwise at least one window must be open at the
     * current local time/day.
     */
    fun canActivate(profile: FocusProfile): ActivationDecision {
        if (!profile.enabled) return ActivationDecision.ProfileDisabled

        val windows = profile.activationWindows
        if (windows.isEmpty()) return ActivationDecision.Allowed

        val now = timeAuthority.now().atZone(timeAuthority.zoneId())
        val localTime = now.toLocalTime()
        val day = now.dayOfWeek

        val anyOpen = windows.any { it.isOpenAt(localTime, day) }
        return if (anyOpen) ActivationDecision.Allowed else ActivationDecision.OutsideWindow
    }
}
