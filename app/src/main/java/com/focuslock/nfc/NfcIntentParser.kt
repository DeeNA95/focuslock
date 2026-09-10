package com.focuslock.nfc

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import com.focuslock.domain.nfc.NfcUri

/**
 * Extracts a FocusLock tag token from a tag-dispatched [Intent].
 */
object NfcIntentParser {

    fun extractToken(intent: Intent?): String? {
        val action = intent?.action ?: return null
        if (action != NfcAdapter.ACTION_NDEF_DISCOVERED) return null

        @Suppress("DEPRECATION")
        val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            ?: return null

        for (raw in rawMessages) {
            val message = raw as? NdefMessage ?: continue
            for (record in message.records) {
                val uri = record.toUri()?.toString() ?: continue
                val token = NfcUri.parseToken(uri)
                if (token != null) return token
            }
        }
        return null
    }
}
