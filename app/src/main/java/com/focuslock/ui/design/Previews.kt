package com.focuslock.ui.design

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.focuslock.domain.model.EnforcementMode
import com.focuslock.domain.model.FocusProfile
import com.focuslock.domain.model.FocusSession
import com.focuslock.domain.model.SessionStatus
import com.focuslock.domain.model.TagBinding
import com.focuslock.ui.home.CommitmentCard
import com.focuslock.ui.session.ActiveSessionContent
import com.focuslock.ui.tags.PairTagDialog
import com.focuslock.ui.tags.PairingState
import com.focuslock.ui.theme.FocusLockTheme
import java.time.Duration
import java.time.Instant
import java.util.UUID

private val previewProfile = FocusProfile(
    id = UUID.randomUUID(),
    name = "Deep Work",
    motto = "Hold your resolve",
    duration = Duration.ofHours(8),
    blockedPackages = setOf("a", "b", "c", "d", "e", "f"),
    enforcementMode = EnforcementMode.SOFT,
)

private val previewSession = FocusSession(
    id = UUID.randomUUID(),
    profileId = null,
    profileNameSnapshot = "Deep Work",
    startedAtWallClock = Instant.parse("2026-01-01T21:00:00Z"),
    startedAtElapsedRealtimeMs = 0L,
    expiresAtWallClock = Instant.parse("2026-01-02T05:00:00Z"),
    duration = Duration.ofHours(8),
    blockedPackagesSnapshot = setOf("a", "b", "c"),
    enforcementMode = EnforcementMode.HARD,
    fortressModeEnabled = true,
    mottoSnapshot = "Hold your resolve",
    status = SessionStatus.ACTIVE,
)

@Composable
private fun PreviewWrapper(content: @Composable () -> Unit) {
    FocusLockTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Preview(name = "Commitment card (populated)", showBackground = true)
@Composable
private fun CommitmentCardPreview() {
    PreviewWrapper {
        CommitmentCard(
            profile = previewProfile,
            showRun = true,
            onClick = {},
            onRun = {},
            onDelete = {},
        )
    }
}

@Preview(name = "Empty profiles", showBackground = true)
@Composable
private fun EmptyProfilesPreview() {
    PreviewWrapper {
        FocusEmptyState(
            illustration = { EmptyProfilesIllustration() },
            title = "No commitments yet",
            body = "Create a Focus Profile to define a commitment.",
        )
    }
}

@Preview(name = "Empty tags", showBackground = true)
@Composable
private fun EmptyTagsPreview() {
    PreviewWrapper {
        FocusEmptyState(
            illustration = { EmptyTagsIllustration() },
            title = "No tags paired",
            body = "Pair a physical NFC tag to a profile.",
        )
    }
}

@Preview(name = "Active session (countdown)", showBackground = true)
@Composable
private fun ActiveSessionPreview() {
    FocusLockTheme {
        ActiveSessionContent(
            session = previewSession,
            remaining = Duration.ofHours(5).plusMinutes(42).plusSeconds(17),
            onOpenRecovery = {},
        )
    }
}

@Preview(name = "Pairing waiting", showBackground = true)
@Composable
private fun PairingWaitingPreview() {
    FocusLockTheme {
        PairTagDialog(
            profiles = listOf(previewProfile),
            pairingState = PairingState.WaitingForTag(
                token = "t",
                label = "Desk",
                profileId = previewProfile.id,
            ),
            onStartPairing = { _, _ -> },
            onCancel = {},
            onDismissResult = {},
        )
    }
}

@Preview(name = "Status badges", showBackground = true)
@Composable
private fun BadgesPreview() {
    PreviewWrapper {
        Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            StatusBadge("Soft", BadgeTone.Neutral)
            StatusBadge("Hard", BadgeTone.Amber)
            StatusBadge("On", BadgeTone.Green)
            StatusBadge("Off", BadgeTone.Red)
        }
    }
}

@Preview(name = "Progress ring", showBackground = true)
@Composable
private fun ProgressRingPreview() {
    PreviewWrapper {
        ProgressRing(
            progress = 0.72f,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
        )
    }
}

@Preview(name = "NFC pairing illustration", showBackground = true)
@Composable
private fun NfcIllustrationPreview() {
    PreviewWrapper {
        NfcPairingIllustration()
    }
}
