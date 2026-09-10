package com.focuslock.domain.session

import com.focuslock.domain.model.SessionStatus

/**
 * Explicit state machine guarding [SessionStatus] transitions.
 *
 * There is deliberately no transition out of ACTIVE other than EXPIRING: a
 * session cannot be cancelled early.
 */
object SessionStateMachine {

    private val allowedTransitions: Map<SessionStatus, Set<SessionStatus>> = mapOf(
        SessionStatus.IDLE to setOf(SessionStatus.ACTIVATING),
        SessionStatus.ACTIVATING to setOf(SessionStatus.ACTIVE, SessionStatus.FAILED),
        SessionStatus.ACTIVE to setOf(SessionStatus.EXPIRING),
        SessionStatus.EXPIRING to setOf(SessionStatus.COMPLETED),
        SessionStatus.COMPLETED to emptySet(),
        SessionStatus.FAILED to emptySet(),
    )

    /** Whether a transition from [from] to [to] is legal. */
    fun canTransition(from: SessionStatus, to: SessionStatus): Boolean =
        to in (allowedTransitions[from] ?: emptySet())

    /** Whether [status] is a terminal state. */
    fun isTerminal(status: SessionStatus): Boolean =
        status == SessionStatus.COMPLETED || status == SessionStatus.FAILED
}
