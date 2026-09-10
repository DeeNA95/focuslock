package com.focuslock.domain.model

/**
 * How a Focus Session is enforced. The domain layer only references this enum;
 * it never knows how blocking is actually performed.
 */
enum class EnforcementMode {
    /** Accessibility-based interception (soft lock, not a security boundary). */
    SOFT,

    /** Device Owner package suspension (hard lock). */
    HARD,
}
