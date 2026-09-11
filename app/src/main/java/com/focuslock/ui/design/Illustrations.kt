package com.focuslock.ui.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Original vector illustrations drawn locally with Canvas. No stock assets,
 * no network — everything scales cleanly.
 */

@Composable
fun EmptyProfilesIllustration(modifier: Modifier = Modifier) {
    val amber = FocusColors.Amber
    val dim = FocusColors.Charcoal500
    val faint = FocusColors.Outline
    Canvas(modifier = modifier.size(140.dp)) {
        val w = size.width
        val h = size.height

        // Back card
        drawRoundRect(
            color = faint.copy(alpha = 0.5f),
            topLeft = Offset(w * 0.18f, h * 0.16f),
            size = Size(w * 0.64f, h * 0.52f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.06f),
        )
        // Front card
        drawRoundRect(
            color = dim,
            topLeft = Offset(w * 0.26f, h * 0.30f),
            size = Size(w * 0.64f, h * 0.52f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.06f),
        )
        // Shackle
        val shackle = Path().apply {
            moveTo(w * 0.44f, h * 0.34f)
            lineTo(w * 0.44f, h * 0.28f)
            cubicTo(w * 0.44f, h * 0.20f, w * 0.66f, h * 0.20f, w * 0.66f, h * 0.28f)
            lineTo(w * 0.66f, h * 0.34f)
        }
        drawPath(shackle, color = amber, style = Stroke(width = w * 0.03f, cap = StrokeCap.Round))
        // Lock body
        drawRoundRect(
            color = amber,
            topLeft = Offset(w * 0.40f, h * 0.34f),
            size = Size(w * 0.30f, h * 0.20f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.03f),
        )
    }
}

@Composable
fun EmptyTagsIllustration(modifier: Modifier = Modifier) {
    val amber = FocusColors.Amber
    val cream = FocusColors.Ivory
    Canvas(modifier = modifier.size(140.dp)) {
        val w = size.width
        val h = size.height
        // Tag body
        drawRoundRect(
            color = FocusColors.Charcoal600,
            topLeft = Offset(w * 0.28f, h * 0.26f),
            size = Size(w * 0.44f, w * 0.44f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.06f),
        )
        // Ring hole
        drawCircle(
            color = FocusColors.Charcoal900,
            radius = w * 0.055f,
            center = Offset(w * 0.42f, h * 0.38f),
        )
        drawCircle(
            color = amber,
            radius = w * 0.055f,
            center = Offset(w * 0.42f, h * 0.38f),
            style = Stroke(width = w * 0.02f),
        )
        // NFC waves
        for (i in 0..2) {
            val inset = w * (0.05f + i * 0.045f)
            drawArc(
                color = if (i == 0) cream else amber,
                startAngle = -60f,
                sweepAngle = 120f,
                useCenter = false,
                topLeft = Offset(w * 0.48f + inset, h * 0.38f + inset * 0.2f),
                size = Size(w * 0.22f - inset, w * 0.22f - inset),
                style = Stroke(width = w * 0.022f, cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
fun NfcPairingIllustration(
    modifier: Modifier = Modifier,
    iconSize: androidx.compose.ui.unit.Dp = 140.dp,
) {
    val amber = FocusColors.Amber
    val cream = FocusColors.Ivory
    Canvas(modifier = modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        // Phone body
        drawRoundRect(
            color = FocusColors.Charcoal600,
            topLeft = Offset(w * 0.14f, h * 0.22f),
            size = Size(w * 0.24f, h * 0.56f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f),
        )
        // Phone screen line
        drawRoundRect(
            color = FocusColors.Charcoal900,
            topLeft = Offset(w * 0.17f, h * 0.28f),
            size = Size(w * 0.18f, h * 0.40f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.02f),
        )
        // Waves from phone to tag
        for (i in 0..2) {
            val inset = w * (0.05f + i * 0.04f)
            drawArc(
                color = if (i == 0) cream else amber,
                startAngle = -50f,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = Offset(w * 0.36f + inset * 0.6f, h * 0.34f + inset * 0.4f),
                size = Size(w * 0.30f - inset, w * 0.30f - inset),
                style = Stroke(width = w * 0.02f, cap = StrokeCap.Round),
            )
        }
        // Tag
        drawRoundRect(
            color = FocusColors.Charcoal600,
            topLeft = Offset(w * 0.62f, h * 0.40f),
            size = Size(w * 0.24f, w * 0.24f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.04f),
        )
        drawCircle(
            color = amber,
            radius = w * 0.04f,
            center = Offset(w * 0.74f, h * 0.52f),
            style = Stroke(width = w * 0.015f),
        )
    }
}

@Composable
fun ActiveCommitmentIllustration(modifier: Modifier = Modifier) {
    val amber = FocusColors.Amber
    val cream = FocusColors.Ivory
    Canvas(modifier = modifier.size(140.dp)) {
        val w = size.width
        val h = size.height
        val c = Offset(w * 0.5f, h * 0.5f)
        // Progress ring (static artwork)
        drawCircle(
            color = FocusColors.Charcoal600,
            radius = w * 0.36f,
            center = c,
        )
        drawCircle(
            color = amber,
            radius = w * 0.36f,
            center = c,
            style = Stroke(width = w * 0.025f),
        )
        // Lock
        drawRoundRect(
            color = amber,
            topLeft = Offset(w * 0.40f, h * 0.54f),
            size = Size(w * 0.20f, h * 0.14f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.02f),
        )
        val shackle = Path().apply {
            moveTo(w * 0.43f, h * 0.54f)
            lineTo(w * 0.43f, h * 0.48f)
            cubicTo(w * 0.43f, h * 0.42f, w * 0.57f, h * 0.42f, w * 0.57f, h * 0.48f)
            lineTo(w * 0.57f, h * 0.54f)
        }
        drawPath(shackle, color = cream, style = Stroke(width = w * 0.02f, cap = StrokeCap.Round))
    }
}
