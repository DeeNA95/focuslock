package com.focuslock.ui.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focuslock.domain.model.FocusProfile
import com.focuslock.domain.model.TagBinding
import com.focuslock.domain.repository.ProfileRepository
import com.focuslock.domain.repository.TagRepository
import com.focuslock.nfc.NfcEventBus
import com.focuslock.nfc.NfcTagEvent
import com.focuslock.nfc.NfcTagWriter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed interface PairingState {
    data object Idle : PairingState
    data class WaitingForTag(val token: String, val label: String, val profileId: UUID) : PairingState
    data object Success : PairingState
    data class Error(val message: String) : PairingState
}

@HiltViewModel
class TagsViewModel @Inject constructor(
    private val tagRepository: TagRepository,
    private val profileRepository: ProfileRepository,
    private val nfcTagWriter: NfcTagWriter,
    private val nfcEventBus: NfcEventBus,
) : ViewModel() {

    val bindings: StateFlow<List<TagBinding>> = tagRepository.observeBindings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val profiles: StateFlow<List<FocusProfile>> = profileRepository.observeProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _pairingState = MutableStateFlow<PairingState>(PairingState.Idle)
    val pairingState: StateFlow<PairingState> = _pairingState.asStateFlow()

    init {
        viewModelScope.launch {
            nfcEventBus.tags.collect { event -> onTagEvent(event) }
        }
    }

    fun startPairing(profileId: UUID, label: String) {
        val token = UUID.randomUUID().toString()
        _pairingState.value = PairingState.WaitingForTag(
            token = token,
            label = label.ifBlank { "Tag" },
            profileId = profileId,
        )
    }

    fun cancelPairing() {
        _pairingState.value = PairingState.Idle
    }

    fun dismissResult() {
        _pairingState.value = PairingState.Idle
    }

    fun deleteBinding(id: UUID) {
        viewModelScope.launch { tagRepository.deleteBinding(id) }
    }

    private fun onTagEvent(event: NfcTagEvent) {
        val state = _pairingState.value as? PairingState.WaitingForTag ?: return
        val token = state.token
        viewModelScope.launch {
            val writeResult = nfcTagWriter.writeToken(event.tag, token)
            if (writeResult.isFailure) {
                _pairingState.value = PairingState.Error(
                    "Write failed: ${writeResult.exceptionOrNull()?.message ?: "unknown error"}"
                )
                return@launch
            }
            val readBack = nfcTagWriter.readToken(event.tag)
            if (readBack != token) {
                _pairingState.value = PairingState.Error("Verification failed")
                return@launch
            }
            tagRepository.saveBinding(
                TagBinding(
                    id = UUID.randomUUID(),
                    token = token,
                    label = state.label,
                    profileId = state.profileId,
                )
            )
            _pairingState.value = PairingState.Success
        }
    }
}
