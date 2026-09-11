package com.focuslock.domain.model

import java.time.Instant
import java.util.UUID

/**
 * A single entry in the local diagnostics event log.
 *
 * [type] is a stable string code; [detail] may carry a package name or other
 * non-sensitive context. Sensitive user content must never be logged.
 */
data class SessionEvent(
    val id: Long = 0,
    val timestamp: Instant,
    val type: String,
    val sessionId: UUID? = null,
    val detail: String? = null,
) {
    companion object {
        const val TYPE_NFC_TAG_DETECTED = "NFC_TAG_DETECTED"
        const val TYPE_PROFILE_RESOLVED = "PROFILE_RESOLVED"
        const val TYPE_SESSION_CREATED = "SESSION_CREATED"
        const val TYPE_SESSION_ACTIVE = "SESSION_ACTIVE"
        const val TYPE_SESSION_COMPLETED = "SESSION_COMPLETED"
        const val TYPE_SESSION_EMERGENCY_RELEASED = "SESSION_EMERGENCY_RELEASED"
        const val TYPE_SESSION_FAILED = "SESSION_FAILED"
        const val TYPE_BLOCKED_ATTEMPT = "BLOCKED_ATTEMPT"
        const val TYPE_PACKAGE_SUSPENDED = "PACKAGE_SUSPENDED"
        const val TYPE_PACKAGE_RESUMED = "PACKAGE_RESUMED"
        const val TYPE_RECONCILIATION = "RECONCILIATION"
        const val TYPE_BOOT_COMPLETED = "BOOT_COMPLETED"
    }
}
