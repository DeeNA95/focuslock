package com.focuslock.domain.nfc

/**
 * FocusLock NFC URI scheme. A physical tag stores only an opaque random token
 * in the form `focuslock://tag/<token>`. The profile configuration lives in the
 * app database, never on the tag.
 */
object NfcUri {
    const val SCHEME = "focuslock"
    const val HOST = "tag"
    const val PREFIX = "focuslock://tag/"

    fun build(token: String): String = PREFIX + token

    /**
     * Extracts the token from a FocusLock URI, or null if the URI does not
     * match the FocusLock scheme.
     */
    fun parseToken(uri: String): String? {
        if (!uri.startsWith(PREFIX)) return null
        val token = uri.removePrefix(PREFIX).trim()
        if (token.isBlank()) return null
        if (token.any { it == '/' || it == '?' || it == '#' || it.isWhitespace() }) return null
        return token
    }
}
