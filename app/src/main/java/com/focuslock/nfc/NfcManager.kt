package com.focuslock.nfc

import android.content.Context
import android.nfc.NfcAdapter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NfcManager @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(context)

    fun isAvailable(): Boolean = adapter != null

    fun isEnabled(): Boolean = adapter?.isEnabled == true

    fun adapter(): NfcAdapter? = adapter
}
