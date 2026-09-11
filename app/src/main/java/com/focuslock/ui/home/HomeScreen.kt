package com.focuslock.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focuslock.ui.session.ActiveSessionScreen
import com.focuslock.ui.session.SessionViewModel

@Composable
fun HomeScreen(
    onCreateProfile: () -> Unit,
    onEditProfile: (String) -> Unit,
    onOpenTags: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenDiagnostics: () -> Unit,
) {
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val active by sessionViewModel.activeSession.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = active,
        contentKey = { it != null },
        transitionSpec = {
            (fadeIn() + slideInVertically { it / 14 }) togetherWith
                (fadeOut() + slideOutVertically { -it / 14 })
        },
        label = "home-content",
    ) { session ->
        if (session != null) {
            ActiveSessionScreen(session = session, viewModel = sessionViewModel)
        } else {
            ProfileListScreen(
                onCreateProfile = onCreateProfile,
                onEditProfile = onEditProfile,
                onOpenTags = onOpenTags,
                onOpenStats = onOpenStats,
                onOpenDiagnostics = onOpenDiagnostics,
            )
        }
    }
}
