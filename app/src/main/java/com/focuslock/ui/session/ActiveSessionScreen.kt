package com.focuslock.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focuslock.domain.model.FocusSession
import com.focuslock.ui.components.AppIconRow
import com.focuslock.ui.design.FocusColors
import com.focuslock.ui.design.FocusSpacing
import com.focuslock.ui.design.FocusTextButton
import com.focuslock.ui.design.ProgressRing
import com.focuslock.ui.design.StatusBadge
import com.focuslock.ui.design.modeBadgeTone
import com.focuslock.ui.design.modeLabel
import com.focuslock.ui.design.monoStyle
import com.focuslock.ui.recovery.RecoveryDialog
import com.focuslock.ui.recovery.RecoveryViewModel
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ActiveSessionScreen(
    session: FocusSession,
    viewModel: SessionViewModel = hiltViewModel(),
) {
    val remaining by viewModel.remaining.collectAsStateWithLifecycle()
    val recoveryViewModel: RecoveryViewModel = hiltViewModel()
    var showRecovery by remember { mutableStateOf(false) }

    ActiveSessionContent(
        session = session,
        remaining = remaining,
        onOpenRecovery = { showRecovery = true },
    )

    if (showRecovery) {
        RecoveryDialog(
            isConfigured = recoveryViewModel.isConfigured.collectAsStateWithLifecycle().value,
            generatedKey = recoveryViewModel.generatedKey.collectAsStateWithLifecycle().value,
            releaseResult = recoveryViewModel.releaseResult.collectAsStateWithLifecycle().value,
            onGenerate = recoveryViewModel::generateKey,
            onDismissGenerated = recoveryViewModel::dismissGeneratedKey,
            onRelease = recoveryViewModel::release,
            onClearResult = recoveryViewModel::clearReleaseResult,
            onDismiss = { showRecovery = false },
        )
    }
}

/** Stateless active-session content, previewable and testable. */
@Composable
internal fun ActiveSessionContent(
    session: FocusSession,
    remaining: Duration,
    onOpenRecovery: () -> Unit,
) {
    val progress = if (session.duration.isZero) 0f else {
        (remaining.toMillis().toFloat() / session.duration.toMillis()).coerceIn(0f, 1f)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = FocusSpacing.XL),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(FocusSpacing.XXL))

        Text(
            session.profileNameSnapshot.uppercase(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        if (session.mottoSnapshot.isNotBlank()) {
            Spacer(Modifier.height(FocusSpacing.S))
            Text(
                session.mottoSnapshot,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(FocusSpacing.XXL))

        // Countdown anchor
        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center,
        ) {
            ProgressRing(
                progress = progress,
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 12.dp,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    formatRemaining(remaining),
                    style = monoStyle(weight = FontWeight.Medium, size = 40f),
                    color = FocusColors.Ivory,
                )
                Text(
                    "remaining",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(FocusSpacing.XXL))

        Text(
            "Unlocks ${formatTime(session.expiresAtWallClock)}",
            style = monoStyle(weight = FontWeight.Medium, size = 16f),
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(FocusSpacing.L))

        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusBadge(
                text = session.enforcementMode.modeLabel,
                tone = session.enforcementMode.modeBadgeTone,
            )
            Spacer(Modifier.width(FocusSpacing.M))
            Text(
                "${session.blockedPackagesSnapshot.size} apps unavailable",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (session.blockedPackagesSnapshot.isNotEmpty()) {
            Spacer(Modifier.height(FocusSpacing.L))
            AppIconRow(
                packages = session.blockedPackagesSnapshot,
                iconSize = 28.dp,
            )
        }

        Spacer(Modifier.height(FocusSpacing.XXL))

        Text(
            "You already made this decision.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(FocusSpacing.XXL))

        // Emergency recovery — visually isolated from normal actions.
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = FocusSpacing.M),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Emergency",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FocusTextButton(
                text = "Recovery",
                onClick = onOpenRecovery,
                color = FocusColors.Red,
            )
        }
        Spacer(Modifier.height(FocusSpacing.L))
    }
}

fun formatRemaining(duration: Duration): String {
    val total = duration.seconds.coerceAtLeast(0)
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}

private fun formatTime(instant: java.time.Instant): String =
    DateTimeFormatter.ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(instant)
