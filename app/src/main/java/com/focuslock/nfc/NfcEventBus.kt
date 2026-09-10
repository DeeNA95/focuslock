package com.focuslock.nfc

import android.nfc.Tag
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/** A tag delivered by the platform, with its parsed token (if any). */
data class NfcTagEvent(val tag: Tag, val token: String?)

/**
 * Broadcasts NFC tag events from the Activity to interested collectors
 * (pairing flow, scanning). Decouples platform dispatch from UI state.
 */
@Singleton
class NfcEventBus @Inject constructor() {
    private val _tags = MutableSharedFlow<NfcTagEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val tags: SharedFlow<NfcTagEvent> = _tags

    fun onTag(event: NfcTagEvent) {
        _tags.tryEmit(event)
    }
}
