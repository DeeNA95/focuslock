package com.focuslock.domain.model

/**
 * A launchable application surfaced by the app picker.
 *
 * Icons are intentionally not part of this model: they are resolved lazily in
 * the UI layer to keep the domain free of Android graphics types.
 */
data class InstalledApp(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
)
