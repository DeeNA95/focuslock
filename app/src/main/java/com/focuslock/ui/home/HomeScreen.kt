package com.focuslock.ui.home

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
    onOpenDiagnostics: () -> Unit,
) {
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val active by sessionViewModel.activeSession.collectAsStateWithLifecycle()

    if (active != null) {
        ActiveSessionScreen(session = active!!, viewModel = sessionViewModel)
    } else {
        ProfileListScreen(
            onCreateProfile = onCreateProfile,
            onEditProfile = onEditProfile,
            onOpenTags = onOpenTags,
            onOpenDiagnostics = onOpenDiagnostics,
        )
    }
}
