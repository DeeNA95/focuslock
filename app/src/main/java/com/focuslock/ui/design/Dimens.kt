package com.focuslock.ui.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Spacing scale. */
object FocusSpacing {
    val XS = 4.dp
    val S = 8.dp
    val M = 12.dp
    val L = 16.dp
    val XL = 24.dp
    val XXL = 32.dp
}

/** Shape tokens. */
object FocusShapes {
    val Small = RoundedCornerShape(10.dp)
    val Medium = RoundedCornerShape(16.dp)
    val Large = RoundedCornerShape(24.dp)
}

/** Material shapes wired from the tokens above. */
val FocusMaterialShapes = Shapes(
    extraSmall = FocusShapes.Small,
    small = FocusShapes.Small,
    medium = FocusShapes.Medium,
    large = FocusShapes.Large,
    extraLarge = FocusShapes.Large,
)

/** Minimum touch target (WCAG / Material guidance). */
object FocusTouch {
    val Min = 48.dp
}
