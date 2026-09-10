package com.focuslock.enforcement.accessibility

import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AccessibilityEnforcementBackendTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun `not available when service is not enabled`() {
        Settings.Secure.putString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, "")
        assertThat(AccessibilityEnforcementBackend.isServiceEnabled(context)).isFalse()
    }

    @Test
    fun `available when service is enabled`() {
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            "com.other/com.other.Service:${FocusLockAccessibilityService.COMPONENT_FLATTENED}",
        )
        assertThat(AccessibilityEnforcementBackend.isServiceEnabled(context)).isTrue()
    }

    @Test
    fun `backend reports accessibility type and availability`() {
        val backend = AccessibilityEnforcementBackend(context)
        assertThat(backend.backendType()).isEqualTo(com.focuslock.enforcement.EnforcementBackendType.ACCESSIBILITY)
    }
}
