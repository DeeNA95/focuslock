package com.focuslock.data.time

import android.os.SystemClock
import com.focuslock.domain.time.TimeAuthority
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemTimeAuthority @Inject constructor() : TimeAuthority {
    override fun now(): Instant = Instant.now()
    override fun elapsedRealtimeMillis(): Long = SystemClock.elapsedRealtime()
    override fun zoneId(): ZoneId = ZoneId.systemDefault()
}
