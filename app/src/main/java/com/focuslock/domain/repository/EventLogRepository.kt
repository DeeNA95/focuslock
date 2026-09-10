package com.focuslock.domain.repository

import com.focuslock.domain.model.SessionEvent
import kotlinx.coroutines.flow.Flow

interface EventLogRepository {
    suspend fun log(event: SessionEvent)
    fun observeRecent(limit: Int): Flow<List<SessionEvent>>
    suspend fun getRecent(limit: Int): List<SessionEvent>
}
