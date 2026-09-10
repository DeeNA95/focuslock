package com.focuslock.domain.safety

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SafetyPolicyTest {

    private val protected = setOf(
        "com.focuslock",
        "com.sec.android.app.launcher",
        "com.android.systemui",
        "com.android.settings",
        "com.android.phone",
    )

    private fun policy() = ProtectedPackageSafetyPolicy(protected)

    @Test
    fun `normal app is allowed`() {
        val result = policy().validateBlockList(setOf("com.instagram.android"))
        assertThat(result.safe).isTrue()
        assertThat(result.allowedPackages).containsExactly("com.instagram.android")
        assertThat(result.rejectedPackages).isEmpty()
    }

    @Test
    fun `launcher is rejected`() {
        val result = policy().validateBlockList(
            setOf("com.instagram.android", "com.sec.android.app.launcher")
        )
        assertThat(result.safe).isFalse()
        assertThat(result.rejectedPackages).containsExactly("com.sec.android.app.launcher")
        assertThat(result.allowedPackages).containsExactly("com.instagram.android")
    }

    @Test
    fun `focuslock itself is rejected`() {
        val result = policy().validateBlockList(setOf("com.focuslock"))
        assertThat(result.safe).isFalse()
        assertThat(result.rejectedPackages).containsExactly("com.focuslock")
    }

    @Test
    fun `system package is rejected`() {
        val result = policy().validateBlockList(setOf("com.android.systemui"))
        assertThat(result.safe).isFalse()
    }

    @Test
    fun `empty requested list is safe`() {
        val result = policy().validateBlockList(emptySet())
        assertThat(result.safe).isTrue()
        assertThat(result.allowedPackages).isEmpty()
    }

    @Test
    fun `isProtected matches the protected set`() {
        val p = policy()
        assertThat(p.isProtected("com.focuslock")).isTrue()
        assertThat(p.isProtected("com.instagram.android")).isFalse()
    }
}
