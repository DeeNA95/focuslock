package com.focuslock.domain.model

/**
 * What happens when a blocked app is opened during a session.
 */
enum class OpenBehavior {
    /** Show the block cover and send the user Home. */
    BLOCK,

    /**
     * Show a short breathing pause, then let the user continue into the app.
     * A gentle nudge rather than a wall.
     */
    BREATHE,
}
