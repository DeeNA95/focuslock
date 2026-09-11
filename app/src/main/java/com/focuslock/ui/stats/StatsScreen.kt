package com.focuslock.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focuslock.domain.model.BlockedAppAttempt
import com.focuslock.ui.components.AppIcon
import com.focuslock.ui.design.ActiveCommitmentIllustration
import com.focuslock.ui.design.BadgeTone
import com.focuslock.ui.design.FocusCard
import com.focuslock.ui.design.FocusEmptyState
import com.focuslock.ui.design.FocusSpacing
import com.focuslock.ui.design.FocusTopBar
import com.focuslock.ui.design.SectionHeader
import com.focuslock.ui.design.StatusBadge
import com.focuslock.ui.design.monoStyle
import java.time.Duration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FocusTopBar(
                title = "Stats",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (stats.completedSessions == 0 && stats.blockedAttempts.isEmpty()) {
            FocusEmptyState(
                illustration = { ActiveCommitmentIllustration() },
                title = "Nothing to show yet",
                body = "Complete a focus session and your time, streaks and resisted apps will appear here.",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FocusSpacing.L),
            verticalArrangement = Arrangement.spacedBy(FocusSpacing.M),
        ) {
            Spacer(Modifier.height(FocusSpacing.XS))

            FocusCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(FocusSpacing.XL),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "FOCUS · LAST 7 DAYS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(FocusSpacing.S))
                    Text(
                        formatDuration(stats.recentFocus),
                        style = monoStyle(weight = FontWeight.SemiBold, size = 40f),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(FocusSpacing.XS))
                    Text(
                        "${formatDuration(stats.totalFocus)} all time",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(FocusSpacing.M)) {
                StatCard("Sessions", stats.completedSessions.toString(), Modifier.weight(1f))
                StatCard("Streak", "${stats.currentStreakDays}d", Modifier.weight(1f))
                StatCard("Best", "${stats.bestStreakDays}d", Modifier.weight(1f))
            }

            if (stats.blockedAttempts.isNotEmpty()) {
                SectionHeader(
                    title = "Most resisted apps",
                    subtitle = "Times you tried to open them during a session.",
                )
                FocusCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(FocusSpacing.L)) {
                        stats.blockedAttempts.forEach { attempt ->
                            AttemptRow(attempt)
                        }
                    }
                }
            }

            Spacer(Modifier.height(FocusSpacing.XL))
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    FocusCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = FocusSpacing.L, horizontal = FocusSpacing.S),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(FocusSpacing.XS))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AttemptRow(attempt: BlockedAppAttempt) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = FocusSpacing.S),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(packageName = attempt.packageName, size = 28.dp)
        Spacer(Modifier.width(FocusSpacing.M))
        Text(
            attempt.label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        StatusBadge(text = "×${attempt.count}", tone = BadgeTone.Neutral)
    }
}

private fun formatDuration(duration: Duration): String {
    val totalMinutes = duration.toMinutes().coerceAtLeast(0)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}
