package com.focuslock.domain.session

import com.focuslock.domain.model.FocusSession

/**
 * Result of attempting to activate a Focus Profile.
 */
sealed interface SessionActivationResult {
    data class Activated(val session: FocusSession) : SessionActivationResult
    data class Failed(val reason: ActivationFailure) : SessionActivationResult
}

enum class ActivationFailure {
    SESSION_ALREADY_ACTIVE,
    OUTSIDE_ALLOWED_WINDOW,
    PROFILE_DISABLED,
    ENFORCEMENT_UNAVAILABLE,
    UNSAFE_CONFIGURATION,
    ENFORCEMENT_FAILURE,
}
