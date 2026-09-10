package com.focuslock.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focuslock.data.DefaultProfilesInstaller
import com.focuslock.domain.model.FocusProfile
import com.focuslock.domain.repository.ProfileRepository
import com.focuslock.domain.repository.SettingsRepository
import com.focuslock.domain.session.ActivationFailure
import com.focuslock.domain.session.SessionActivationResult
import com.focuslock.domain.session.SessionManager
import com.focuslock.enforcement.accessibility.AccessibilityEnforcementBackend
import com.focuslock.util.AccessibilityHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ProfileListViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val settingsRepository: SettingsRepository,
    private val sessionManager: SessionManager,
    private val defaultsInstaller: DefaultProfilesInstaller,
    private val accessibilityBackend: AccessibilityEnforcementBackend,
    private val accessibilityHelper: AccessibilityHelper,
) : ViewModel() {

    val profiles: StateFlow<List<FocusProfile>> = profileRepository.observeProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val devMode: StateFlow<Boolean> = settingsRepository.devModeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _accessibilityEnabled = MutableStateFlow(accessibilityBackend.isAvailable())
    val accessibilityEnabled: StateFlow<Boolean> = _accessibilityEnabled.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    /** Re-reads the accessibility service grant (call on screen resume). */
    fun refreshAccessibilityState() {
        _accessibilityEnabled.value = accessibilityBackend.isAvailable()
    }

    fun openAccessibilitySettings() {
        accessibilityHelper.openSettings()
    }

    fun deleteProfile(id: UUID) {
        viewModelScope.launch { profileRepository.deleteProfile(id) }
    }

    fun seedDefaults() {
        viewModelScope.launch {
            val installed = defaultsInstaller.installIfNeeded()
            _messages.emit(if (installed) "Default profiles added" else "Defaults already present")
        }
    }

    /** Dev-mode only: activate a profile manually, without an NFC tag. */
    fun activateProfile(id: UUID) {
        viewModelScope.launch {
            val profile = profileRepository.getProfile(id) ?: return@launch
            when (val result = sessionManager.activate(profile)) {
                is SessionActivationResult.Activated -> Unit
                is SessionActivationResult.Failed -> _messages.emit(messageFor(result.reason))
            }
        }
    }

    private fun messageFor(reason: ActivationFailure): String = when (reason) {
        ActivationFailure.SESSION_ALREADY_ACTIVE -> "A session is already active"
        ActivationFailure.OUTSIDE_ALLOWED_WINDOW -> "Outside the allowed activation window"
        ActivationFailure.PROFILE_DISABLED -> "This profile is disabled"
        ActivationFailure.ENFORCEMENT_UNAVAILABLE ->
            "Enforcement backend unavailable — enable the accessibility service first"
        ActivationFailure.UNSAFE_CONFIGURATION -> "This profile would block an unsafe app"
        ActivationFailure.ENFORCEMENT_FAILURE -> "Failed to apply blocking"
    }
}
