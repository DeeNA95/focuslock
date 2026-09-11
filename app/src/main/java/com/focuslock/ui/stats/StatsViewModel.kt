package com.focuslock.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focuslock.domain.model.FocusStats
import com.focuslock.domain.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    statsRepository: StatsRepository,
) : ViewModel() {

    val stats: StateFlow<FocusStats> = statsRepository.observeStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FocusStats())
}
