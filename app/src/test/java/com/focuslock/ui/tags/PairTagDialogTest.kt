package com.focuslock.ui.tags

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
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
class PairTagDialogTest {

    @get:Rule
    val rule = createComposeRule()

    private val profile = FocusProfile(
        id = UUID.randomUUID(),
        name = "Deep Work",
        duration = Duration.ofHours(8),
        blockedPackages = emptySet(),
        enforcementMode = EnforcementMode.SOFT,
    )

    @Test
    fun `idle shows setup and requires a profile before next`() {
        var started = false
        rule.setContent {
            FocusLockTheme {
                PairTagDialog(
                    profiles = listOf(profile),
                    pairingState = PairingState.Idle,
                    onStartPairing = { _, _ -> started = true },
                    onCancel = {},
                    onDismissResult = {},
                )
            }
        }
        rule.onNodeWithText("Next").assertIsNotEnabled()
        rule.onNodeWithText("Deep Work").performClick()
        rule.onNodeWithText("Next").assertIsEnabled().performClick()
        assert(started)
    }

    @Test
    fun `waiting shows hold-tag state`() {
        rule.setContent {
            FocusLockTheme {
                PairTagDialog(
                    profiles = listOf(profile),
                    pairingState = PairingState.WaitingForTag(
                        token = "token",
                        label = "Desk",
                        profileId = profile.id,
                    ),
                    onStartPairing = { _, _ -> },
                    onCancel = {},
                    onDismissResult = {},
                )
            }
        }
        rule.onNodeWithText("Hold the tag to the phone").assertExists()
    }

    @Test
    fun `success state is shown`() {
        rule.setContent {
            FocusLockTheme {
                PairTagDialog(
                    profiles = listOf(profile),
                    pairingState = PairingState.Success,
                    onStartPairing = { _, _ -> },
                    onCancel = {},
                    onDismissResult = {},
                )
            }
        }
        rule.onNodeWithText("Tag paired").assertExists()
    }

    @Test
    fun `error state shows the message`() {
        rule.setContent {
            FocusLockTheme {
                PairTagDialog(
                    profiles = listOf(profile),
                    pairingState = PairingState.Error("Verification failed"),
                    onStartPairing = { _, _ -> },
                    onCancel = {},
                    onDismissResult = {},
                )
            }
        }
        rule.onNodeWithText("Verification failed").assertExists()
    }
}
