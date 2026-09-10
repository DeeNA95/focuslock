package com.focuslock.data.repository

import com.focuslock.data.dao.SessionDao
import com.focuslock.data.db.toBlockedPackageEntities
import com.focuslock.data.db.toDomain
import com.focuslock.data.db.toEntity
import com.focuslock.domain.model.FocusSession
import com.focuslock.domain.model.SessionStatus
import com.focuslock.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomSessionRepository @Inject constructor(
    private val sessionDao: SessionDao,
) : SessionRepository {

    override suspend fun createSession(session: FocusSession) {
        sessionDao.saveSession(session.toEntity(), session.toBlockedPackageEntities())
    }

    override suspend fun getSession(id: UUID): FocusSession? =
        sessionDao.getById(id.toString())?.toDomain()

    override suspend fun getActiveSession(): FocusSession? =
        sessionDao.getActive()?.toDomain()

    override fun observeActiveSession(): Flow<FocusSession?> =
        sessionDao.observeActive().map { it?.toDomain() }

    override suspend fun updateStatus(id: UUID, status: SessionStatus) {
        sessionDao.updateStatus(id.toString(), status.name)
    }
}
