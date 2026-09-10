package com.focuslock.domain.repository

import com.focuslock.domain.model.InstalledApp

interface AppRepository {
    /** All launchable user + system applications, user apps first. */
    suspend fun getLaunchableApps(): List<InstalledApp>
}
