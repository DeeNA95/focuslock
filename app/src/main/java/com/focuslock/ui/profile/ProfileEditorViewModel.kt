package com.focuslock.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focuslock.domain.model.ActivationWindow
import com.focuslock.domain.model.EnforcementMode
import com.focuslock.domain.model.FocusProfile
import com.focuslock.domain.model.InstalledApp
import com.focuslock.domain.model.OpenBehavior
import com.focuslock.domain.repository.AppRepository
import com.focuslock.domain.repository.ProfileRepository
import com.focuslock.domain.safety.SafetyPolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject

data class ProfileDraft(
    val id: UUID? = null,
    val name: String = "",
    val durationHours: Int = 8,
    val durationMinutes: Int = 0,
    val blockedPackages: Set<String> = emptySet(),
    val restrictActivation: Boolean = false,
    val startHour: Int = 18,
    val startMinute: Int = 0,
    val endHour: Int = 23,
    val endMinute: Int = 59,
    val daysOfWeek: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val enforcementMode: EnforcementMode = EnforcementMode.SOFT,
    val fortressModeEnabled: Boolean = false,
    val openBehavior: OpenBehavior = OpenBehavior.BLOCK,
    val enableDnd: Boolean = false,
    val motto: String = "",
    /**
     * Activation windows the editor does not expose but must not discard when
     * re-saving an existing profile (the UI only edits the first window).
     */
    val extraActivationWindows: List<ActivationWindow> = emptyList(),
) {
    val totalMinutes: Int get() = durationHours * 60 + durationMinutes
    val isValid: Boolean get() = name.isNotBlank() && totalMinutes >= 1

    fun toProfile(): FocusProfile = FocusProfile(
        id = id ?: UUID.randomUUID(),
        name = name.trim(),
        duration = Duration.ofMinutes(totalMinutes.toLong()),
        blockedPackages = blockedPackages,
        activationWindows = if (restrictActivation) {
            listOf(
                ActivationWindow(
                    start = LocalTime.of(startHour, startMinute),
                    end = LocalTime.of(endHour, endMinute),
                    daysOfWeek = daysOfWeek,
                )
            ) + extraActivationWindows
        } else {
            emptyList()
        },
        enforcementMode = enforcementMode,
        fortressModeEnabled = fortressModeEnabled,
        openBehavior = openBehavior,
        enableDnd = enableDnd,
        enabled = true,
        motto = motto.trim(),
    )
}

@HiltViewModel
class ProfileEditorViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val appRepository: AppRepository,
    private val safetyPolicy: SafetyPolicy,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val profileId: String? = savedStateHandle["profileId"]

    private val _draft = MutableStateFlow(ProfileDraft())
    val draft: StateFlow<ProfileDraft> = _draft.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps.asStateFlow()

    private val _saved = Channel<Unit>(Channel.BUFFERED)
    val saved = _saved.receiveAsFlow()

    init {
        viewModelScope.launch {
            profileId?.let { idString ->
                profileRepository.getProfile(UUID.fromString(idString))?.let { profile ->
                    _draft.value = profile.toDraft()
                }
            }
        }
        viewModelScope.launch {
            _installedApps.value = appRepository.getLaunchableApps()
        }
    }

    fun updateName(value: String) = _draft.update { it.copy(name = value) }

    fun setDuration(hours: Int, minutes: Int) =
        _draft.update { it.copy(durationHours = hours, durationMinutes = minutes) }

    fun togglePackage(packageName: String) = _draft.update { draft ->
        val current = draft.blockedPackages
        draft.copy(
            blockedPackages = if (packageName in current) current - packageName else current + packageName,
        )
    }

    fun setRestrictActivation(value: Boolean) =
        _draft.update { it.copy(restrictActivation = value) }

    fun setStartTime(hour: Int, minute: Int) =
        _draft.update { it.copy(startHour = hour, startMinute = minute) }

    fun setEndTime(hour: Int, minute: Int) =
        _draft.update { it.copy(endHour = hour, endMinute = minute) }

    fun toggleDay(day: DayOfWeek) = _draft.update { draft ->
        val current = draft.daysOfWeek
        draft.copy(daysOfWeek = if (day in current) current - day else current + day)
    }

    fun setEnforcementMode(mode: EnforcementMode) =
        _draft.update { it.copy(enforcementMode = mode) }

    fun setFortressMode(value: Boolean) =
        _draft.update { it.copy(fortressModeEnabled = value) }

    fun setOpenBehavior(value: OpenBehavior) =
        _draft.update { it.copy(openBehavior = value) }

    fun setEnableDnd(value: Boolean) =
        _draft.update { it.copy(enableDnd = value) }

    fun updateMotto(value: String) =
        _draft.update { it.copy(motto = value) }

    fun isPackageProtected(packageName: String): Boolean =
        safetyPolicy.isProtected(packageName)

    fun save() {
        val draft = _draft.value
        if (!draft.isValid) return
        viewModelScope.launch {
            profileRepository.saveProfile(draft.toProfile())
            _saved.send(Unit)
        }
    }

    private fun FocusProfile.toDraft(): ProfileDraft {
        val hours = duration.toHours()
        val minutes = (duration.toMinutes() % 60).toInt()
        val window = activationWindows.firstOrNull()
        return ProfileDraft(
            id = id,
            name = name,
            durationHours = hours.toInt(),
            durationMinutes = minutes,
            blockedPackages = blockedPackages,
            restrictActivation = window != null,
            startHour = window?.start?.hour ?: 18,
            startMinute = window?.start?.minute ?: 0,
            endHour = window?.end?.hour ?: 23,
            endMinute = window?.end?.minute ?: 59,
            daysOfWeek = window?.daysOfWeek ?: DayOfWeek.entries.toSet(),
            enforcementMode = enforcementMode,
            fortressModeEnabled = fortressModeEnabled,
            openBehavior = openBehavior,
            enableDnd = enableDnd,
            motto = motto,
            extraActivationWindows = activationWindows.drop(1),
        )
    }

    private inline fun <T> MutableStateFlow<T>.update(transform: (T) -> T) {
        value = transform(value)
    }
}
