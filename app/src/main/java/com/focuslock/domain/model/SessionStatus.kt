package com.focuslock.domain.model

/**
 * Explicit lifecycle of a [FocusSession].
 *
 * ```
 * IDLE
 *   -> ACTIVATING
 *        -> ACTIVE
 *        -> FAILED
 *   ACTIVE
 *        -> EXPIRING
 *             -> COMPLETED
 * ```
 *
 * COMPLETED and FAILED are terminal. A session is never persisted as ACTIVE
 * until enforcement has been verified as applied.
 */
enum class SessionStatus {
    IDLE,
    ACTIVATING,
    ACTIVE,
    EXPIRING,
    COMPLETED,
    FAILED,
}
