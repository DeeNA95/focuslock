package com.focuslock.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import com.focuslock.domain.model.InstalledApp
import com.focuslock.domain.repository.AppRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidAppRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppRepository {

    override suspend fun getLaunchableApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = pm.queryIntentActivities(intent, 0)

        resolveInfos
            .map { resolveInfo ->
                val pkg = resolveInfo.activityInfo.packageName
                InstalledApp(
                    packageName = pkg,
                    label = resolveInfo.loadLabel(pm).toString(),
                    isSystem = (resolveInfo.activityInfo.applicationInfo.flags and
                        ApplicationInfo.FLAG_SYSTEM) != 0,
                )
            }
            .distinctBy { it.packageName }
            .sortedWith(compareBy({ it.isSystem }, { it.label.lowercase() }))
    }
}
