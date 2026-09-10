package com.focuslock.domain.model

import java.time.Duration
import java.util.UUID

/**
 * A reusable template for a focus commitment.
 *
 * Profiles are templates; [FocusSession]s are commitments. Editing or deleting
 * a profile must never mutate an already-active session.
 */
data class FocusProfile(
    val id: UUID,
    val name: String,
    val duration: Duration,
    val blockedPackages: Set<String>,
    val activationWindows: List<ActivationWindow> = emptyList(),
    val enforcementMode: EnforcementMode = EnforcementMode.SOFT,
    val fortressModeEnabled: Boolean = false,
    val enabled: Boolean = true,
    val motto: String = "",
)
