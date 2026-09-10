package com.focuslock.domain.session

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * Pure, testable recovery-key hashing. Generates random keys/salts and verifies
 * keys against a stored salted hash using constant-time comparison.
 */
object RecoveryKeyHasher {

    private val random = SecureRandom()

    fun generateKey(): String = randomBytes(32)

    fun generateSalt(): String = randomBytes(16)

    fun hash(key: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest((salt + key).toByteArray(Charsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun verify(key: String, salt: String, expectedHash: String): Boolean {
        val actual = hash(key, salt)
        return MessageDigest.isEqual(
            actual.toByteArray(Charsets.UTF_8),
            expectedHash.toByteArray(Charsets.UTF_8),
        )
    }

    private fun randomBytes(count: Int): String {
        val bytes = ByteArray(count)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
