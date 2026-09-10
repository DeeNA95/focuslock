package com.focuslock.ui.recovery

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.focuslock.domain.session.EmergencyReleaseResult
import com.focuslock.ui.design.FocusPrimaryButton
import com.focuslock.ui.design.FocusTextButton
import com.focuslock.ui.design.monoStyle

/**
 * Emergency recovery dialog — stateless so it can be tested directly.
 *
 * This is deliberately not a one-tap confirmation: releasing a session requires
 * entering the full recovery key.
 */
@Composable
fun RecoveryDialog(
    isConfigured: Boolean,
    generatedKey: String?,
    releaseResult: EmergencyReleaseResult?,
    onGenerate: () -> Unit,
    onDismissGenerated: () -> Unit,
    onRelease: (String) -> Unit,
    onClearResult: () -> Unit,
    onDismiss: () -> Unit,
) {
    when {
        generatedKey != null -> {
            AlertDialog(
                onDismissRequest = onDismissGenerated,
                title = { Text("Store your recovery key") },
                text = {
                    Column {
                        Text(
                            "Store this key somewhere off-device. It is shown only once and cannot be recovered.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            generatedKey,
                            style = monoStyle(weight = androidx.compose.ui.text.font.FontWeight.Medium, size = 13f),
                        )
                    }
                },
                confirmButton = {
                    FocusTextButton(text = "I've stored it", onClick = onDismissGenerated)
                },
            )
        }

        releaseResult != null -> {
            AlertDialog(
                onDismissRequest = onClearResult,
                title = { Text("Recovery") },
                text = {
                    Text(
                        when (releaseResult) {
                            EmergencyReleaseResult.Released -> "Session released."
                            EmergencyReleaseResult.InvalidKey -> "Invalid recovery key."
                            EmergencyReleaseResult.NoActiveSession -> "No active session."
                            null -> ""
                        }
                    )
                },
                confirmButton = {
                    FocusTextButton(text = "OK", onClick = {
                        onClearResult()
                        onDismiss()
                    })
                },
            )
        }

        !isConfigured -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Set up recovery") },
                text = {
                    Text(
                        "A recovery key lets you end a session early in a genuine emergency. " +
                            "It is only shown once."
                    )
                },
                confirmButton = {
                    FocusPrimaryButton(text = "Generate key", onClick = onGenerate)
                },
                dismissButton = {
                    FocusTextButton(text = "Cancel", onClick = onDismiss)
                },
            )
        }

        else -> {
            var key by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Emergency release") },
                text = {
                    Column {
                        Text("Enter your full recovery key to end the active session.")
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = key,
                            onValueChange = { key = it },
                            label = { Text("Recovery key") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                confirmButton = {
                    FocusPrimaryButton(text = "Release", onClick = { onRelease(key) })
                },
                dismissButton = {
                    FocusTextButton(text = "Cancel", onClick = onDismiss)
                },
            )
        }
    }
}
