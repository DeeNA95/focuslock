package com.focuslock.domain.model

import java.util.UUID

/**
 * Maps a physical NFC tag to a [FocusProfile].
 *
 * The tag itself only carries an opaque random [token]; the actual profile
 * configuration lives in the app database.
 */
data class TagBinding(
    val id: UUID,
    val token: String,
    val label: String,
    val profileId: UUID,
)
