package com.focuslock.data.repository

import com.focuslock.data.dao.EventDao
import com.focuslock.data.db.toDomain
import com.focuslock.data.db.toEntity
import com.focuslock.domain.model.SessionEvent
import com.focuslock.domain.repository.EventLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomEventLogRepository @Inject constructor(
    private val eventDao: EventDao,
) : EventLogRepository {

    override suspend fun log(event: SessionEvent) {
        eventDao.insert(event.toEntity())
    }

    override fun observeRecent(limit: Int): Flow<List<SessionEvent>> =
        eventDao.observeRecent(limit).map { list -> list.map { it.toDomain() } }

    override suspend fun getRecent(limit: Int): List<SessionEvent> =
        eventDao.getRecent(limit).map { it.toDomain() }
}
