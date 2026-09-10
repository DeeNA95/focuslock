package com.focuslock.ui.diagnostics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focuslock.domain.model.SessionEvent
import com.focuslock.ui.design.BadgeTone
import com.focuslock.ui.design.FocusCard
import com.focuslock.ui.design.FocusSpacing
import com.focuslock.ui.design.FocusTextButton
import com.focuslock.ui.design.FocusTopBar
import com.focuslock.ui.design.SectionHeader
import com.focuslock.ui.design.StatusBadge
import com.focuslock.ui.design.StatusRow
import com.focuslock.ui.design.monoStyle
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DiagnosticsScreen(
    onBack: () -> Unit,
    viewModel: DiagnosticsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val devMode by viewModel.devMode.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FocusTopBar(
                title = "Diagnostics",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FocusSpacing.L),
            verticalArrangement = Arrangement.spacedBy(FocusSpacing.M),
        ) {
            Spacer(Modifier.height(FocusSpacing.XS))

            SectionHeader(title = "Services")
            FocusCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(FocusSpacing.L),
                    verticalArrangement = Arrangement.spacedBy(FocusSpacing.XS),
                ) {
                    ServiceStatusRow(
                        label = "Accessibility service",
                        value = state.accessibilityEnabled,
                    )
                    ServiceStatusRow(label = "NFC", value = state.nfcEnabled)
                    ServiceStatusRow(label = "Device Admin", value = state.deviceAdminActive)
                    ServiceStatusRow(label = "Device Owner", value = state.deviceOwner)
                    ServiceStatusRow(label = "Exact alarm", value = state.exactAlarm)
                }
            }

            SectionHeader(title = "Session")
            FocusCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(FocusSpacing.L),
                    verticalArrangement = Arrangement.spacedBy(FocusSpacing.XS),
                ) {
                    StatusRow("Active session", state.activeSessionName ?: "None")
                    StatusRow("Desired blocked packages", state.desiredBlockedCount.toString())
                    StatusRow("Actually suspended", state.actuallySuspendedCount.toString())
                    StatusRow("Hard lock", if (state.isHardLock) "Yes" else "No")
                }
            }

            SectionHeader(title = "Battery & dev")
            FocusCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(FocusSpacing.L),
                    verticalArrangement = Arrangement.spacedBy(FocusSpacing.XS),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Battery optimization", fontWeight = FontWeight.Medium)
                            Text(
                                if (state.ignoringBatteryOptimizations)
                                    "Exempt — FocusLock won't be frozen in the background."
                                else
                                    "Not exempt — Samsung may freeze the service, delaying blocking.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (!state.ignoringBatteryOptimizations) {
                            FocusTextButton(
                                text = "Exempt",
                                onClick = viewModel::requestBatteryExemption,
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Dev mode", fontWeight = FontWeight.Medium)
                            Text(
                                "Manual session triggers from the profile list (no NFC needed).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = devMode, onCheckedChange = viewModel::setDevMode)
                    }
                }
            }

            SectionHeader(title = "Event log")
            FocusCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(FocusSpacing.L)) {
                    if (events.isEmpty()) {
                        Text(
                            "No events recorded",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        events.forEach { event ->
                            EventRow(event)
                        }
                    }
                }
            }

            Spacer(Modifier.height(FocusSpacing.XL))
        }
    }
}

@Composable
private fun ServiceStatusRow(label: String, value: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        StatusBadge(
            text = if (value) "On" else "Off",
            tone = if (value) BadgeTone.Green else BadgeTone.Red,
        )
    }
}

@Composable
private fun EventRow(event: SessionEvent) {
    Column(modifier = Modifier.padding(vertical = FocusSpacing.XS)) {
        Text(
            event.type,
            style = monoStyle(size = 12f),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row {
            Text(
                formatTimestamp(event.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                event.detail?.let { "  $it" } ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatTimestamp(instant: java.time.Instant): String =
    DateTimeFormatter.ofPattern("HH:mm:ss")
        .withZone(ZoneId.systemDefault())
        .format(instant)
