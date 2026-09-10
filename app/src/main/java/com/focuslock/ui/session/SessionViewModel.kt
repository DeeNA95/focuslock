package com.focuslock.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focuslock.domain.model.FocusSession
import com.focuslock.domain.repository.SessionRepository
import com.focuslock.domain.session.SessionClock
import com.focuslock.domain.time.TimeAuthority
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.Duration
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    sessionRepository: SessionRepository,
    private val sessionClock: SessionClock,
    private val timeAuthority: TimeAuthority,
) : ViewModel() {

    val activeSession: StateFlow<FocusSession?> = sessionRepository.observeActiveSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val ticker: StateFlow<Long> = flow {
        while (true) {
            emit(timeAuthority.elapsedRealtimeMillis())
            delay(1_000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val remaining: StateFlow<Duration> = combine(ticker, activeSession) { _, session ->
        session?.let { sessionClock.remaining(it) } ?: Duration.ZERO
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Duration.ZERO)
}
