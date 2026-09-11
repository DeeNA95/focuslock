package com.focuslock.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun Drawable.toBitmap(width: Int, height: Int): Bitmap {
    if (this is BitmapDrawable && bitmap != null) return bitmap
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, width, height)
    draw(canvas)
    return bitmap
}

@Composable
fun AppIcon(packageName: String, size: Dp = 40.dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sizePx = with(context.resources.displayMetrics.density) { (size.value * this).toInt() }

    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null,
        key1 = packageName,
    ) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager.getApplicationIcon(packageName)
                    .toBitmap(sizePx, sizePx)
                    .asImageBitmap()
            }.getOrNull()
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = null,
            modifier = modifier.size(size),
        )
    }
}

/**
 * A compact row of the blocked apps' icons, capped at [max] with a `+N`
 * overflow so profile cards hint at what they will lock.
 */
@Composable
fun AppIconRow(
    packages: Set<String>,
    modifier: Modifier = Modifier,
    max: Int = 4,
    iconSize: Dp = 24.dp,
) {
    if (packages.isEmpty()) return
    val shown = packages.take(max)
    val overflow = packages.size - shown.size

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        shown.forEach { packageName ->
            AppIcon(
                packageName = packageName,
                size = iconSize,
                modifier = Modifier.padding(end = 6.dp),
            )
        }
        if (overflow > 0) {
            Text(
                text = "+$overflow",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
