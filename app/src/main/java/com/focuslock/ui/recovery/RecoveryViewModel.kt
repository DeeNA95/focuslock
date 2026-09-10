package com.focuslock.ui.recovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focuslock.domain.session.EmergencyReleaseResult
import com.focuslock.domain.session.RecoveryKeyManager
import com.focuslock.domain.session.SessionReconciler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecoveryViewModel @Inject constructor(
    private val recoveryKeyManager: RecoveryKeyManager,
    private val sessionReconciler: SessionReconciler,
) : ViewModel() {

    val isConfigured: StateFlow<Boolean> = recoveryKeyManager.isConfigured
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _generatedKey = MutableStateFlow<String?>(null)
    val generatedKey: StateFlow<String?> = _generatedKey.asStateFlow()

    private val _releaseResult = MutableStateFlow<EmergencyReleaseResult?>(null)
    val releaseResult: StateFlow<EmergencyReleaseResult?> = _releaseResult.asStateFlow()

    fun generateKey() {
        viewModelScope.launch {
            _generatedKey.value = recoveryKeyManager.generate()
        }
    }

    fun dismissGeneratedKey() {
        _generatedKey.value = null
    }

    fun release(key: String) {
        viewModelScope.launch {
            _releaseResult.value = sessionReconciler.emergencyRelease(key.trim())
        }
    }

    fun clearReleaseResult() {
        _releaseResult.value = null
    }
}
