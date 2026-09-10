package com.focuslock.domain.session

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RecoveryKeyHasherTest {

    @Test
    fun `generated key is long and unique`() {
        val a = RecoveryKeyHasher.generateKey()
        val b = RecoveryKeyHasher.generateKey()

        assertThat(a).isNotEqualTo(b)
        assertThat(a.length).isGreaterThan(20)
    }

    @Test
    fun `hash and verify round trip`() {
        val key = RecoveryKeyHasher.generateKey()
        val salt = RecoveryKeyHasher.generateSalt()
        val hash = RecoveryKeyHasher.hash(key, salt)

        assertThat(RecoveryKeyHasher.verify(key, salt, hash)).isTrue()
    }

    @Test
    fun `wrong key is rejected`() {
        val key = RecoveryKeyHasher.generateKey()
        val salt = RecoveryKeyHasher.generateSalt()
        val hash = RecoveryKeyHasher.hash(key, salt)

        assertThat(RecoveryKeyHasher.verify("wrong-key", salt, hash)).isFalse()
    }

    @Test
    fun `wrong salt is rejected`() {
        val key = RecoveryKeyHasher.generateKey()
        val salt = RecoveryKeyHasher.generateSalt()
        val hash = RecoveryKeyHasher.hash(key, salt)

        assertThat(RecoveryKeyHasher.verify(key, "different-salt", hash)).isFalse()
    }
}
