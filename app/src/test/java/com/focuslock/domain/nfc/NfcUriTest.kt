package com.focuslock.domain.nfc

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NfcUriTest {

    @Test
    fun `build produces focuslock uri`() {
        assertThat(NfcUri.build("8d476a81-1234-5678-9abc-def012345678"))
            .isEqualTo("focuslock://tag/8d476a81-1234-5678-9abc-def012345678")
    }

    @Test
    fun `parse extracts token from valid uri`() {
        assertThat(NfcUri.parseToken("focuslock://tag/8d476a81-1234-5678-9abc-def012345678"))
            .isEqualTo("8d476a81-1234-5678-9abc-def012345678")
    }

    @Test
    fun `build then parse round trips`() {
        val token = "8d476a81-1234-5678-9abc-def012345678"
        assertThat(NfcUri.parseToken(NfcUri.build(token))).isEqualTo(token)
    }

    @Test
    fun `parse rejects wrong scheme`() {
        assertThat(NfcUri.parseToken("https://tag/abc")).isNull()
    }

    @Test
    fun `parse rejects wrong host`() {
        assertThat(NfcUri.parseToken("focuslock://other/abc")).isNull()
    }

    @Test
    fun `parse rejects empty token`() {
        assertThat(NfcUri.parseToken("focuslock://tag/")).isNull()
    }

    @Test
    fun `parse rejects extra path segments`() {
        assertThat(NfcUri.parseToken("focuslock://tag/abc/def")).isNull()
    }

    @Test
    fun `parse rejects query strings`() {
        assertThat(NfcUri.parseToken("focuslock://tag/abc?x=1")).isNull()
    }

    @Test
    fun `parse rejects fragment`() {
        assertThat(NfcUri.parseToken("focuslock://tag/abc#frag")).isNull()
    }
}
