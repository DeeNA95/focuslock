package com.focuslock.ui.home

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focuslock.R
import com.focuslock.domain.model.FocusProfile
import com.focuslock.ui.components.AppIconRow
import com.focuslock.ui.design.EmptyProfilesIllustration
import com.focuslock.ui.design.FocusCard
import com.focuslock.ui.design.FocusColors
import com.focuslock.ui.design.FocusEmptyState
import com.focuslock.ui.design.FocusSpacing
import com.focuslock.ui.design.FocusTextButton
import com.focuslock.ui.design.FocusTopBar
import com.focuslock.ui.design.NfcPairingIllustration
import com.focuslock.ui.design.SectionHeader
import com.focuslock.ui.design.StatusBadge
import com.focuslock.ui.design.modeBadgeTone
import com.focuslock.ui.design.modeShortLabel
import java.time.Duration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileListScreen(
    onCreateProfile: () -> Unit,
    onEditProfile: (String) -> Unit,
    onOpenTags: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    viewModel: ProfileListViewModel = hiltViewModel(),
) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val devMode by viewModel.devMode.collectAsStateWithLifecycle()
    val accessibilityEnabled by viewModel.accessibilityEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleResumeEffect(Unit) {
        viewModel.refreshAccessibilityState()
        onPauseOrDispose { }
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FocusTopBar(
                title = "FocusLock",
                actions = {
                    IconButton(onClick = onOpenStats) {
                        Icon(
                            painter = painterResource(R.drawable.ic_stats),
                            contentDescription = "Stats",
                        )
                    }
                    IconButton(onClick = onOpenDiagnostics) {
                        Icon(
                            painter = painterResource(R.drawable.ic_tune),
                            contentDescription = "Diagnostics",
                        )
                    }
                    IconButton(onClick = onOpenTags) {
                        Icon(
                            painter = painterResource(R.drawable.ic_nfc),
                            contentDescription = "NFC tags",
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateProfile,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(6.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Create profile")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!accessibilityEnabled) {
                AccessibilityBanner(onEnable = viewModel::openAccessibilitySettings)
            }
            if (profiles.isEmpty()) {
                FocusEmptyState(
                    illustration = { EmptyProfilesIllustration() },
                    title = "No commitments yet",
                    body = "Create a Focus Profile to define a commitment: duration, blocked apps and an activation window.",
                    action = {
                        if (devMode) {
                            FocusTextButton(text = "Re-add starter profiles", onClick = viewModel::seedDefaults)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(FocusSpacing.L),
                    verticalArrangement = Arrangement.spacedBy(FocusSpacing.M),
                ) {
                    item {
                        TapToStartHero(onOpenTags = onOpenTags)
                    }
                    item {
                        SectionHeader(
                            title = "Profiles",
                            subtitle = "Profiles are templates. Sessions are commitments.",
                        )
                        Spacer(Modifier.height(FocusSpacing.S))
                    }
                    items(profiles, key = { it.id.toString() }) { profile ->
                        CommitmentCard(
                            profile = profile,
                            showRun = devMode,
                            onClick = { onEditProfile(profile.id.toString()) },
                            onRun = { viewModel.activateProfile(profile.id) },
                            onDelete = { viewModel.deleteProfile(profile.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccessibilityBanner(onEnable: () -> Unit) {
    FocusCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FocusSpacing.L, vertical = FocusSpacing.S),
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FocusSpacing.L),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Blocking is off", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(FocusSpacing.XS))
                Text(
                    "Enable the FocusLock accessibility service to block apps.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            FocusTextButton(text = "Enable", onClick = onEnable, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun TapToStartHero(onOpenTags: () -> Unit) {
    FocusCard(onClick = onOpenTags, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FocusSpacing.L),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NfcPairingIllustration(iconSize = 84.dp)
            Spacer(Modifier.width(FocusSpacing.L))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Tap a tag to begin",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(FocusSpacing.XS))
                Text(
                    "Hold your FocusLock tag to the phone to start a commitment.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(FocusSpacing.S))
                Text(
                    "Manage tags",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
internal fun CommitmentCard(
    profile: FocusProfile,
    showRun: Boolean,
    onClick: () -> Unit,
    onRun: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    FocusCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FocusSpacing.L, vertical = FocusSpacing.M),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    profile.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (profile.motto.isNotBlank()) {
                    Spacer(Modifier.height(FocusSpacing.XS))
                    Text(
                        profile.motto,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(FocusSpacing.S))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(
                        text = profile.enforcementMode.modeShortLabel,
                        tone = profile.enforcementMode.modeBadgeTone,
                    )
                    Spacer(Modifier.width(FocusSpacing.S))
                    Text(
                        "${formatDuration(profile.duration)} · ${profile.blockedPackages.size} apps",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (profile.blockedPackages.isNotEmpty()) {
                    Spacer(Modifier.height(FocusSpacing.M))
                    AppIconRow(packages = profile.blockedPackages)
                }
            }
            if (showRun) {
                FocusTextButton(text = "Run", onClick = onRun)
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Profile actions")
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            menuOpen = false
                            onClick()
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

private fun formatDuration(duration: Duration): String {
    val hours = duration.toHours()
    val minutes = duration.toMinutes() % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}
