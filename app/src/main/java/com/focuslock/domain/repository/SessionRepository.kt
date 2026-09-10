package com.focuslock.domain.repository

import com.focuslock.domain.model.FocusSession
import com.focuslock.domain.model.SessionStatus
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface SessionRepository {
    suspend fun createSession(session: FocusSession)
    suspend fun getSession(id: UUID): FocusSession?
    suspend fun getActiveSession(): FocusSession?
    fun observeActiveSession(): Flow<FocusSession?>
    suspend fun updateStatus(id: UUID, status: SessionStatus)
}
