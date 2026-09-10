package com.focuslock.ui.profile

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.focuslock.domain.model.EnforcementMode
import com.focuslock.ui.apppicker.AppPickerDialog
import com.focuslock.ui.design.FocusCard
import com.focuslock.ui.design.FocusPrimaryButton
import com.focuslock.ui.design.FocusSpacing
import com.focuslock.ui.design.FocusTextButton
import com.focuslock.ui.design.FocusTopBar
import com.focuslock.ui.design.SectionHeader
import java.time.DayOfWeek

private val DAYS_ORDER = listOf(
    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY,
)

@Composable
fun ProfileEditorScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileEditorViewModel = hiltViewModel(),
) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    var showAppPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.saved.collect { onNavigateBack() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FocusTopBar(
                title = if (draft.id == null) "New Profile" else "Edit Profile",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            EditorSaveBar(
                isValid = draft.isValid,
                onSave = viewModel::save,
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

            // Identity
            SectionHeader(title = "Identity", subtitle = "Name and the message shown while locked.")
            FocusCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(FocusSpacing.L),
                    verticalArrangement = Arrangement.spacedBy(FocusSpacing.M),
                ) {
                    OutlinedTextField(
                        value = draft.name,
                        onValueChange = viewModel::updateName,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Profile name") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = draft.motto,
                        onValueChange = viewModel::updateMotto,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Motto (e.g. Hold your resolve)") },
                        singleLine = true,
                    )
                }
            }

            // Duration
            SectionHeader(title = "Duration", subtitle = "How long the commitment lasts.")
            FocusCard(modifier = Modifier.fillMaxWidth()) {
                DurationEditor(
                    hours = draft.durationHours,
                    minutes = draft.durationMinutes,
                    onHoursChange = { h -> viewModel.setDuration(h, draft.durationMinutes) },
                    onMinutesChange = { m -> viewModel.setDuration(draft.durationHours, m) },
                    modifier = Modifier.padding(FocusSpacing.L),
                )
            }

            // Blocked apps
            SectionHeader(
                title = "Blocked apps",
                subtitle = if (draft.blockedPackages.isEmpty()) {
                    "No apps selected — the session will block nothing."
                } else {
                    "${draft.blockedPackages.size} app(s) selected"
                },
            )
            FocusCard(modifier = Modifier.fillMaxWidth()) {
                FocusTextButton(
                    text = if (draft.blockedPackages.isEmpty()) "Choose apps..." else "Edit apps...",
                    onClick = { showAppPicker = true },
                    modifier = Modifier.padding(horizontal = FocusSpacing.L, vertical = FocusSpacing.XS),
                )
            }

            // Activation rules
            SectionHeader(title = "Activation window", subtitle = "When a tag may start this profile.")
            FocusCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(FocusSpacing.L),
                    verticalArrangement = Arrangement.spacedBy(FocusSpacing.M),
                ) {
                    SwitchRow(
                        title = "Restrict activation time",
                        subtitle = "Only allow activation within a window.",
                        checked = draft.restrictActivation,
                        onCheckedChange = viewModel::setRestrictActivation,
                    )
                    if (draft.restrictActivation) {
                        Row(horizontalArrangement = Arrangement.spacedBy(FocusSpacing.M)) {
                            TimeEditor(
                                label = "Start",
                                hour = draft.startHour,
                                minute = draft.startMinute,
                                onChange = viewModel::setStartTime,
                                modifier = Modifier.weight(1f),
                            )
                            TimeEditor(
                                label = "End",
                                hour = draft.endHour,
                                minute = draft.endMinute,
                                onChange = viewModel::setEndTime,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            DAYS_ORDER.forEach { day ->
                                FilterChip(
                                    selected = day in draft.daysOfWeek,
                                    onClick = { viewModel.toggleDay(day) },
                                    label = {
                                        Text(
                                            day.getDisplayName(
                                                java.time.format.TextStyle.SHORT,
                                                java.util.Locale.ENGLISH,
                                            )
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // Enforcement
            SectionHeader(title = "Enforcement", subtitle = "How blocking is applied.")
            FocusCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(FocusSpacing.L)) {
                    ModeRow(
                        label = "Soft Lock",
                        description = "Accessibility interception. Not a security boundary.",
                        selected = draft.enforcementMode == EnforcementMode.SOFT,
                        onClick = { viewModel.setEnforcementMode(EnforcementMode.SOFT) },
                    )
                    ModeRow(
                        label = "Hard Lock",
                        description = "Device Owner suspension (requires setup).",
                        selected = draft.enforcementMode == EnforcementMode.HARD,
                        onClick = { viewModel.setEnforcementMode(EnforcementMode.HARD) },
                    )
                    Spacer(Modifier.height(FocusSpacing.S))
                    SwitchRow(
                        title = "Fortress Mode",
                        subtitle = "Extra anti-circumvention policies (Hard Lock only).",
                        checked = draft.fortressModeEnabled,
                        onCheckedChange = viewModel::setFortressMode,
                    )
                }
            }

            Spacer(Modifier.height(FocusSpacing.XS))
        }
    }

    if (showAppPicker) {
        AppPickerDialog(
            apps = installedApps,
            selectedPackages = draft.blockedPackages,
            isProtected = viewModel::isPackageProtected,
            onToggle = viewModel::togglePackage,
            onDismiss = { showAppPicker = false },
        )
    }
}

@Composable
internal fun EditorSaveBar(
    isValid: Boolean,
    onSave: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        FocusPrimaryButton(
            text = "Save profile",
            onClick = onSave,
            enabled = isValid,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FocusSpacing.L, vertical = FocusSpacing.M),
        )
    }
}

@Composable
private fun DurationEditor(
    hours: Int,
    minutes: Int,
    onHoursChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(FocusSpacing.M)) {
        Row(horizontalArrangement = Arrangement.spacedBy(FocusSpacing.M)) {
            NumberField(
                label = "Hours",
                value = hours,
                onValueChange = onHoursChange,
                modifier = Modifier.weight(1f),
            )
            NumberField(
                label = "Minutes",
                value = minutes,
                onValueChange = onMinutesChange,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(FocusSpacing.S)) {
            listOf(30 to "30m", 60 to "1h", 120 to "2h", 480 to "8h").forEach { (mins, label) ->
                FilterChip(
                    selected = hours * 60 + minutes == mins,
                    onClick = { onHoursChange(mins / 60); onMinutesChange(mins % 60) },
                    label = { Text(label) },
                )
            }
        }
    }
}

@Composable
private fun TimeEditor(
    label: String,
    hour: Int,
    minute: Int,
    onChange: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(FocusSpacing.XS))
        Row(horizontalArrangement = Arrangement.spacedBy(FocusSpacing.S)) {
            NumberField(
                label = "HH",
                value = hour,
                onValueChange = { h -> onChange(h.coerceIn(0, 23), minute) },
                modifier = Modifier.weight(1f),
            )
            NumberField(
                label = "MM",
                value = minute,
                onValueChange = { m -> onChange(hour, m.coerceIn(0, 59)) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Local text state so the field can be cleared completely; an empty field
    // is parsed as 0. The key resets the text when the value changes
    // externally (e.g. preset chips).
    var text by remember(value) { mutableStateOf(value.toString()) }

    OutlinedTextField(
        value = text,
        onValueChange = { newText ->
            text = newText.filter { it.isDigit() }
            onValueChange(text.toIntOrNull() ?: 0)
        },
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
    )
}

@Composable
private fun ModeRow(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
