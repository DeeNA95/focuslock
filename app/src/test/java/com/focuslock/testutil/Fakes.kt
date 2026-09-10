package com.focuslock.testutil

import com.focuslock.domain.model.FocusSession
import com.focuslock.domain.model.SessionEvent
import com.focuslock.domain.model.SessionStatus
import com.focuslock.domain.repository.EventLogRepository
import com.focuslock.domain.repository.SessionRepository
import com.focuslock.domain.session.RecoveryKeyManager
import com.focuslock.domain.session.SessionNotifier
import com.focuslock.domain.session.SessionScheduler
import com.focuslock.domain.session.TemporaryPolicyManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import java.util.UUID

class FakeSessionRepository : SessionRepository {
    val sessions = mutableListOf<FocusSession>()
    private val activeFlow = MutableStateFlow<FocusSession?>(null)

    override suspend fun createSession(session: FocusSession) {
        sessions.add(session)
        activeFlow.value = currentActive()
    }

    override suspend fun getSession(id: UUID): FocusSession? =
        sessions.firstOrNull { it.id == id }

    override suspend fun getActiveSession(): FocusSession? = currentActive()

    override fun observeActiveSession(): Flow<FocusSession?> = activeFlow

    override suspend fun updateStatus(id: UUID, status: SessionStatus) {
        val idx = sessions.indexOfFirst { it.id == id }
        if (idx >= 0) sessions[idx] = sessions[idx].copy(status = status)
        activeFlow.value = currentActive()
    }

    private fun currentActive(): FocusSession? =
        sessions.firstOrNull { it.status in setOf(SessionStatus.ACTIVATING, SessionStatus.ACTIVE, SessionStatus.EXPIRING) }
}

class FakeScheduler : SessionScheduler {
    val scheduled = mutableListOf<FocusSession>()
    val cancelled = mutableListOf<UUID>()

    override fun scheduleExpiry(session: FocusSession) {
        scheduled += session
    }

    override fun cancel(sessionId: UUID) {
        cancelled += sessionId
    }
}

class FakeNotifier : SessionNotifier {
    var shown: FocusSession? = null
    var cancelled = false

    override fun showActive(session: FocusSession) {
        shown = session
    }

    override fun cancel() {
        cancelled = true
    }
}

class FakeEventLog : EventLogRepository {
    val events = mutableListOf<SessionEvent>()

    override suspend fun log(event: SessionEvent) {
        events += event
    }

    override fun observeRecent(limit: Int): Flow<List<SessionEvent>> = flowOf(events)

    override suspend fun getRecent(limit: Int): List<SessionEvent> = events
}

class FakeTemporaryPolicyManager : TemporaryPolicyManager {
    var appliedCount = 0
    var restoredCount = 0

    override suspend fun apply(session: FocusSession) {
        appliedCount++
    }

    override suspend fun restore(session: FocusSession) {
        restoredCount++
    }
}

class FakeRecoveryKeyManager(
    private var validKey: String? = null,
) : RecoveryKeyManager {
    override val isConfigured: Flow<Boolean> = MutableStateFlow(validKey != null)

    override suspend fun generate(): String {
        val key = "generated-key"
        validKey = key
        return key
    }

    override suspend fun verify(key: String): Boolean = key == validKey
}
