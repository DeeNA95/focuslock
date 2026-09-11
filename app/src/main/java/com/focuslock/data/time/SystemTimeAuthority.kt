package com.focuslock.data.time

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import com.focuslock.domain.time.TimeAuthority
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemTimeAuthority @Inject constructor(
    @ApplicationContext private val context: Context,
) : TimeAuthority {
    override fun now(): Instant = Instant.now()
    override fun elapsedRealtimeMillis(): Long = SystemClock.elapsedRealtime()

    override fun bootId(): Long = runCatching {
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, 0).toLong()
    }.getOrDefault(0L)

    override fun zoneId(): ZoneId = ZoneId.systemDefault()
}
