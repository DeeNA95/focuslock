package com.focuslock.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import com.focuslock.domain.nfc.NfcUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NfcTagWriter @Inject constructor() {

    /** Writes a FocusLock token to [tag]. Handles both NDEF and formatable tags. */
    suspend fun writeToken(tag: Tag, token: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val message = NdefMessage(arrayOf(NdefRecord.createUri(NfcUri.build(token))))
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                try {
                    ndef.writeNdefMessage(message)
                } finally {
                    ndef.close()
                }
            } else {
                val formatable = NdefFormatable.get(tag)
                    ?: error("Tag is not NDEF-compatible")
                formatable.connect()
                try {
                    formatable.format(message)
                } finally {
                    formatable.close()
                }
            }
        }
    }

    /** Reads the FocusLock token from [tag], or null if none is present. */
    suspend fun readToken(tag: Tag): String? = withContext(Dispatchers.IO) {
        runCatching {
            val ndef = Ndef.get(tag) ?: return@runCatching null
            ndef.connect()
            try {
                val message = ndef.cachedNdefMessage ?: ndef.ndefMessage
                message?.records?.firstNotNullOfOrNull { record ->
                    record.toUri()?.toString()?.let(NfcUri::parseToken)
                }
            } finally {
                ndef.close()
            }
        }.getOrNull()
    }
}
