package com.focuslock.ui.navigation

object Routes {
    const val PROFILES = "profiles"
    const val PROFILE_EDITOR = "profile_editor"
    const val TAGS = "tags"
    const val DIAGNOSTICS = "diagnostics"

    fun profileEditor(profileId: String? = null): String =
        if (profileId == null) PROFILE_EDITOR else "$PROFILE_EDITOR?profileId=$profileId"
}
