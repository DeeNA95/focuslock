package com.focuslock.domain.repository

import com.focuslock.domain.model.FocusStats
import kotlinx.coroutines.flow.Flow

interface StatsRepository {
    fun observeStats(): Flow<FocusStats>
}
