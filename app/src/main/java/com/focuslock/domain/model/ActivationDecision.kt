package com.focuslock.domain.model

/**
 * Result of evaluating whether a profile may currently be activated.
 *
 * Each branch maps to explicit UI; failures must never be silently ignored.
 */
sealed interface ActivationDecision {
    /** The profile may be activated right now. */
    data object Allowed : ActivationDecision

    /** The profile is disabled and can never be activated. */
    data object ProfileDisabled : ActivationDecision

    /** The current time/day does not fall inside any activation window. */
    data object OutsideWindow : ActivationDecision
}
