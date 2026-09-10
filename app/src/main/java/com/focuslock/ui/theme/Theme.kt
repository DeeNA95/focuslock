package com.focuslock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import com.focuslock.ui.design.FocusColors
import com.focuslock.ui.design.FocusMaterialShapes
import com.focuslock.ui.design.FocusTypography

private val FocusDarkColorScheme = darkColorScheme(
    primary = FocusColors.Amber,
    onPrimary = FocusColors.OnAmber,
    primaryContainer = FocusColors.AmberDark,
    onPrimaryContainer = FocusColors.AmberLight,
    secondary = FocusColors.IvoryDim,
    onSecondary = FocusColors.Charcoal900,
    secondaryContainer = FocusColors.Charcoal600,
    onSecondaryContainer = FocusColors.Ivory,
    tertiary = FocusColors.Green,
    onTertiary = FocusColors.Charcoal900,
    error = FocusColors.Red,
    onError = FocusColors.OnRed,
    errorContainer = FocusColors.RedDark,
    onErrorContainer = FocusColors.Ivory,
    background = FocusColors.Charcoal900,
    onBackground = FocusColors.Ivory,
    surface = FocusColors.Charcoal800,
    onSurface = FocusColors.Ivory,
    surfaceVariant = FocusColors.Charcoal600,
    onSurfaceVariant = FocusColors.IvoryDim,
    surfaceContainerLowest = FocusColors.Charcoal900,
    surfaceContainerLow = FocusColors.Charcoal700,
    surfaceContainer = FocusColors.Charcoal700,
    surfaceContainerHigh = FocusColors.Charcoal600,
    surfaceContainerHighest = FocusColors.Charcoal500,
    outline = FocusColors.Outline,
    outlineVariant = FocusColors.OutlineFaint,
)

@Composable
fun FocusLockTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FocusDarkColorScheme,
        typography = FocusTypography,
        shapes = FocusMaterialShapes,
        content = content,
    )
}
