package com.focuslock.ui.apppicker

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.focuslock.domain.model.InstalledApp
import com.focuslock.ui.theme.FocusLockTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppPickerDialogTest {

    @get:Rule
    val rule = createComposeRule()

    private val apps = listOf(
        InstalledApp("com.instagram.android", "Instagram", isSystem = false),
        InstalledApp("com.android.settings", "Settings", isSystem = true),
    )

    @Test
    fun `toggling an app calls onToggle`() {
        var toggled: String? = null
        rule.setContent {
            FocusLockTheme {
                AppPickerDialog(
                    apps = apps,
                    selectedPackages = emptySet(),
                    isProtected = { it == "com.android.settings" },
                    onToggle = { toggled = it },
                    onDismiss = {},
                )
            }
        }
        rule.onNodeWithText("Instagram").performClick()
        assert(toggled == "com.instagram.android")
    }

    @Test
    fun `protected app cannot be toggled`() {
        var toggled: String? = null
        rule.setContent {
            FocusLockTheme {
                AppPickerDialog(
                    apps = apps,
                    selectedPackages = emptySet(),
                    isProtected = { it == "com.android.settings" },
                    onToggle = { toggled = it },
                    onDismiss = {},
                )
            }
        }
        rule.onNodeWithText("Settings").assertIsNotEnabled().performClick()
        assert(toggled == null)
    }

    @Test
    fun `search filters the list`() {
        rule.setContent {
            FocusLockTheme {
                AppPickerDialog(
                    apps = apps,
                    selectedPackages = emptySet(),
                    isProtected = { false },
                    onToggle = {},
                    onDismiss = {},
                )
            }
        }
        rule.onNodeWithText("Search apps...").performTextInput("Insta")
        rule.onNodeWithText("Instagram").assertExists()
        rule.onNodeWithText("Settings").assertDoesNotExist()
    }
}
