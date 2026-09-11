package com.focuslock.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.focuslock.ui.home.HomeScreen
import com.focuslock.ui.profile.ProfileEditorScreen
import com.focuslock.ui.stats.StatsScreen
import com.focuslock.ui.tags.TagsScreen
import com.focuslock.ui.diagnostics.DiagnosticsScreen

@Composable
fun FocusLockNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.PROFILES,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { it / 5 }) + fadeIn()
        },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { it / 5 }) + fadeOut()
        },
    ) {
        composable(Routes.PROFILES) {
            HomeScreen(
                onCreateProfile = { navController.navigate(Routes.profileEditor()) },
                onEditProfile = { id -> navController.navigate(Routes.profileEditor(id)) },
                onOpenTags = { navController.navigate(Routes.TAGS) },
                onOpenStats = { navController.navigate(Routes.STATS) },
                onOpenDiagnostics = { navController.navigate(Routes.DIAGNOSTICS) },
            )
        }
        composable(
            route = "${Routes.PROFILE_EDITOR}?profileId={profileId}",
            arguments = listOf(
                navArgument("profileId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
        ) {
            ProfileEditorScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Routes.TAGS) {
            TagsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.STATS) {
            StatsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.DIAGNOSTICS) {
            DiagnosticsScreen(onBack = { navController.popBackStack() })
        }
    }
}
