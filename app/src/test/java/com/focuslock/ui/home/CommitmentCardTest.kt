package com.focuslock.ui.home

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.focuslock.domain.model.EnforcementMode
import com.focuslock.domain.model.FocusProfile
import com.focuslock.ui.theme.FocusLockTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.time.Duration
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CommitmentCardTest {

    @get:Rule
    val rule = createComposeRule()

    private val profile = FocusProfile(
        id = UUID.randomUUID(),
        name = "Deep Work",
        motto = "Hold your resolve",
        duration = Duration.ofHours(8),
        blockedPackages = setOf("a", "b", "c"),
        enforcementMode = EnforcementMode.SOFT,
    )

    @Test
    fun `run button triggers onRun when dev mode`() {
        var ran = false
        rule.setContent {
            FocusLockTheme {
                CommitmentCard(
                    profile = profile,
                    showRun = true,
                    onClick = {},
                    onRun = { ran = true },
                    onDelete = {},
                )
            }
        }
        rule.onNodeWithText("Run").assertIsEnabled().performClick()
        assert(ran)
    }

    @Test
    fun `delete lives behind overflow menu`() {
        var deleted = false
        rule.setContent {
            FocusLockTheme {
                CommitmentCard(
                    profile = profile,
                    showRun = false,
                    onClick = {},
                    onRun = {},
                    onDelete = { deleted = true },
                )
            }
        }
        // No direct delete action exposed on the card.
        rule.onNodeWithContentDescription("Delete").assertDoesNotExist()

        rule.onNodeWithContentDescription("Profile actions").performClick()
        rule.onNodeWithText("Delete").performClick()
        assert(deleted)
    }

    @Test
    fun `card click opens editor`() {
        var edited = false
        rule.setContent {
            FocusLockTheme {
                CommitmentCard(
                    profile = profile,
                    showRun = false,
                    onClick = { edited = true },
                    onRun = {},
                    onDelete = {},
                )
            }
        }
        rule.onNodeWithText("Deep Work").performClick()
        assert(edited)
    }

    @Test
    fun `save bar disabled when invalid`() {
        var saved = false
        rule.setContent {
            FocusLockTheme {
                com.focuslock.ui.profile.EditorSaveBar(isValid = false, onSave = { saved = true })
            }
        }
        rule.onNodeWithText("Save profile").assertIsNotEnabled().performClick()
        assert(!saved)
    }

    @Test
    fun `save bar enabled when valid`() {
        var saved = false
        rule.setContent {
            FocusLockTheme {
                com.focuslock.ui.profile.EditorSaveBar(isValid = true, onSave = { saved = true })
            }
        }
        rule.onNodeWithText("Save profile").assertIsEnabled().performClick()
        assert(saved)
    }
}
