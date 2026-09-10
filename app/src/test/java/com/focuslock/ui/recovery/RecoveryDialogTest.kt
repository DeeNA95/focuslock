package com.focuslock.ui.recovery

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.focuslock.domain.session.EmergencyReleaseResult
import com.focuslock.ui.theme.FocusLockTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RecoveryDialogTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `not configured offers key generation`() {
        var generated = false
        rule.setContent {
            FocusLockTheme {
                RecoveryDialog(
                    isConfigured = false,
                    generatedKey = null,
                    releaseResult = null,
                    onGenerate = { generated = true },
                    onDismissGenerated = {},
                    onRelease = {},
                    onClearResult = {},
                    onDismiss = {},
                )
            }
        }
        rule.onNodeWithText("Generate key").performClick()
        assert(generated)
    }

    @Test
    fun `generated key is shown once`() {
        rule.setContent {
            FocusLockTheme {
                RecoveryDialog(
                    isConfigured = false,
                    generatedKey = "sekret-key-1",
                    releaseResult = null,
                    onGenerate = {},
                    onDismissGenerated = {},
                    onRelease = {},
                    onClearResult = {},
                    onDismiss = {},
                )
            }
        }
        rule.onNodeWithText("sekret-key-1").assertExists()
    }

    @Test
    fun `release requires entering the full key`() {
        var releasedKey: String? = null
        rule.setContent {
            FocusLockTheme {
                RecoveryDialog(
                    isConfigured = true,
                    generatedKey = null,
                    releaseResult = null,
                    onGenerate = {},
                    onDismissGenerated = {},
                    onRelease = { releasedKey = it },
                    onClearResult = {},
                    onDismiss = {},
                )
            }
        }
        rule.onNodeWithText("Recovery key").performTextInput("full-key-123")
        rule.onNodeWithText("Release").assertIsEnabled().performClick()
        assert(releasedKey == "full-key-123")
    }

    @Test
    fun `invalid key result is surfaced`() {
        rule.setContent {
            FocusLockTheme {
                RecoveryDialog(
                    isConfigured = true,
                    generatedKey = null,
                    releaseResult = EmergencyReleaseResult.InvalidKey,
                    onGenerate = {},
                    onDismissGenerated = {},
                    onRelease = {},
                    onClearResult = {},
                    onDismiss = {},
                )
            }
        }
        rule.onNodeWithText("Invalid recovery key.").assertExists()
    }
}
