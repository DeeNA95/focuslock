package com.focuslock.ui.tags

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focuslock.domain.model.FocusProfile
import com.focuslock.ui.design.BadgeTone
import com.focuslock.ui.design.EmptyTagsIllustration
import com.focuslock.ui.design.FocusCard
import com.focuslock.ui.design.FocusEmptyState
import com.focuslock.ui.design.FocusPrimaryButton
import com.focuslock.ui.design.FocusSpacing
import com.focuslock.ui.design.FocusTextButton
import com.focuslock.ui.design.FocusTopBar
import com.focuslock.ui.design.NfcPairingIllustration
import com.focuslock.ui.design.StatusBadge
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsScreen(
    onBack: () -> Unit,
    viewModel: TagsViewModel = hiltViewModel(),
) {
    val bindings by viewModel.bindings.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val pairingState by viewModel.pairingState.collectAsStateWithLifecycle()
    var showPairDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FocusTopBar(
                title = "NFC Tags",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showPairDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(6.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Pair tag")
            }
        },
    ) { padding ->
        if (bindings.isEmpty()) {
            FocusEmptyState(
                illustration = { EmptyTagsIllustration() },
                title = "No tags paired",
                body = "Pair a physical NFC tag to a profile to use it as an activation device.",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(FocusSpacing.L),
                verticalArrangement = Arrangement.spacedBy(FocusSpacing.M),
            ) {
                items(bindings, key = { it.id.toString() }) { binding ->
                    val profile = profiles.firstOrNull { it.id == binding.profileId }
                    FocusCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(FocusSpacing.L),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(binding.label, style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(FocusSpacing.XS))
                                Text(
                                    profile?.name ?: "Unknown profile",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            StatusBadge(
                                text = "Paired",
                                tone = BadgeTone.Green,
                            )
                            IconButton(onClick = { viewModel.deleteBinding(binding.id) }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPairDialog) {
        PairTagDialog(
            profiles = profiles,
            pairingState = pairingState,
            onStartPairing = viewModel::startPairing,
            onCancel = {
                viewModel.cancelPairing()
                showPairDialog = false
            },
            onDismissResult = {
                viewModel.dismissResult()
                showPairDialog = false
            },
        )
    }
}

@Composable
internal fun PairTagDialog(
    profiles: List<FocusProfile>,
    pairingState: PairingState,
    onStartPairing: (UUID, String) -> Unit,
    onCancel: () -> Unit,
    onDismissResult: () -> Unit,
) {
    when (pairingState) {
        is PairingState.WaitingForTag -> {
            AlertDialog(
                onDismissRequest = onCancel,
                title = { Text("Hold the tag to the phone") },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        NfcPairingIllustration()
                        Spacer(Modifier.height(FocusSpacing.M))
                        Text(
                            "Touching ${pairingState.label} to the back of the phone will write it.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                },
                confirmButton = {},
                dismissButton = {
                    FocusTextButton(text = "Cancel", onClick = onCancel)
                },
            )
        }
        PairingState.Success -> {
            AlertDialog(
                onDismissRequest = onDismissResult,
                title = { Text("Tag paired") },
                text = { Text("The tag was written and verified successfully.") },
                confirmButton = {
                    FocusTextButton(text = "Done", onClick = onDismissResult)
                },
            )
        }
        is PairingState.Error -> {
            AlertDialog(
                onDismissRequest = onDismissResult,
                title = { Text("Pairing failed") },
                text = { Text(pairingState.message) },
                confirmButton = {
                    FocusTextButton(text = "OK", onClick = onDismissResult)
                },
            )
        }
        PairingState.Idle -> {
            PairTagSetup(
                profiles = profiles,
                onStartPairing = onStartPairing,
                onCancel = onCancel,
            )
        }
    }
}

@Composable
private fun PairTagSetup(
    profiles: List<FocusProfile>,
    onStartPairing: (UUID, String) -> Unit,
    onCancel: () -> Unit,
) {
    var selectedProfileId by remember { mutableStateOf<UUID?>(null) }
    var label by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Pair a tag") },
        text = {
            Column {
                Text("Choose a profile", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(FocusSpacing.S))
                profiles.forEach { profile ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedProfileId = profile.id },
                    ) {
                        RadioButton(
                            selected = selectedProfileId == profile.id,
                            onClick = { selectedProfileId = profile.id },
                        )
                        Text(profile.name)
                    }
                }
                Spacer(Modifier.height(FocusSpacing.M))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Tag label (e.g. Desk)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedProfileId?.let { onStartPairing(it, label) } },
                enabled = selectedProfileId != null,
            ) { Text("Next") }
        },
        dismissButton = {
            FocusTextButton(text = "Cancel", onClick = onCancel)
        },
    )
}
