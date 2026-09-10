package com.focuslock.ui.diagnostics

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focuslock.domain.model.SessionEvent
import com.focuslock.domain.repository.EventLogRepository
import com.focuslock.domain.repository.SessionRepository
import com.focuslock.domain.repository.SettingsRepository
import com.focuslock.enforcement.accessibility.AccessibilityEnforcementBackend
import com.focuslock.enforcement.deviceowner.DeviceOwnerEnforcementBackend
import com.focuslock.nfc.NfcManager
import com.focuslock.util.BatteryOptimizationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiagnosticsState(
    val nfcAvailable: Boolean = false,
    val nfcEnabled: Boolean = false,
    val accessibilityEnabled: Boolean = false,
    val deviceOwner: Boolean = false,
    val deviceAdminActive: Boolean = false,
    val exactAlarm: Boolean = false,
    val activeSessionName: String? = null,
    val desiredBlockedCount: Int = 0,
    val actuallySuspendedCount: Int = 0,
    val isHardLock: Boolean = false,
    val ignoringBatteryOptimizations: Boolean = false,
)

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val nfcManager: NfcManager,
    private val accessibilityBackend: AccessibilityEnforcementBackend,
    private val deviceOwnerBackend: DeviceOwnerEnforcementBackend,
    private val sessionRepository: SessionRepository,
    private val settingsRepository: SettingsRepository,
    private val batteryHelper: BatteryOptimizationHelper,
    eventLogRepository: EventLogRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DiagnosticsState())
    val state: StateFlow<DiagnosticsState> = _state.asStateFlow()

    val devMode: StateFlow<Boolean> = settingsRepository.devModeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val events: StateFlow<List<SessionEvent>> = eventLogRepository.observeRecent(50)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        refresh()
    }

    fun setDevMode(value: Boolean) {
        viewModelScope.launch { settingsRepository.setDevModeEnabled(value) }
    }

    fun refresh() {
        viewModelScope.launch {
            val activeSession = sessionRepository.getActiveSession()
            val desired = activeSession?.blockedPackagesSnapshot ?: emptySet()
            val actuallySuspended = if (activeSession != null) {
                deviceOwnerBackend.actuallySuspended(desired)
            } else {
                emptySet()
            }

            _state.value = DiagnosticsState(
                nfcAvailable = nfcManager.isAvailable(),
                nfcEnabled = nfcManager.isEnabled(),
                accessibilityEnabled = accessibilityBackend.isAvailable(),
                deviceOwner = deviceOwnerBackend.isDeviceOwner(),
                deviceAdminActive = deviceOwnerBackend.isAdminActive(),
                exactAlarm = canScheduleExactAlarms(),
                activeSessionName = activeSession?.profileNameSnapshot,
                desiredBlockedCount = desired.size,
                actuallySuspendedCount = actuallySuspended.size,
                isHardLock = activeSession?.enforcementMode?.name == "HARD",
                ignoringBatteryOptimizations = batteryHelper.isIgnoringBatteryOptimizations(),
            )
        }
    }

    fun requestBatteryExemption() {
        batteryHelper.requestExemption()
        refresh()
    }

    private fun canScheduleExactAlarms(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false
        return alarmManager.canScheduleExactAlarms()
    }
}
