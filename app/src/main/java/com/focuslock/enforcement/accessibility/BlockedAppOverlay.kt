package com.focuslock.enforcement.accessibility

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.focuslock.R
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * The full-screen "resist the temptation" screen shown over a blocked app.
 *
 * Built with plain Android views so it can live in an accessibility overlay
 * without a window lifecycle. It mirrors the app's "Quiet Discipline" design
 * language (charcoal surfaces, amber accents, Manrope + Plex Mono).
 */
internal class BlockedAppOverlay(
    private val context: Context,
    private val windowManager: WindowManager,
) {

    private var rootView: View? = null
    private var timerView: TextView? = null
    private var progressView: CapsuleProgressView? = null

    val isShowing: Boolean get() = rootView != null

    fun show(state: BlockedOverlayState, animateIn: Boolean = true) {
        hide(animated = false)

        val root = build(state)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.CENTER }

        windowManager.addView(root, params)
        rootView = root

        if (animateIn && !prefersReducedMotion(context)) {
            root.alpha = 0f
            root.translationY = context.dp(16).toFloat()
            root.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(220L)
                .start()
        }
    }

    fun update(remaining: Duration, progress: Float) {
        timerView?.text = formatCountdown(remaining)
        progressView?.progress = progress
    }

    fun hide(animated: Boolean = true) {
        val view = rootView ?: return
        rootView = null
        timerView = null
        progressView = null
        val remove = Runnable { runCatching { windowManager.removeView(view) } }
        if (animated && !prefersReducedMotion(context)) {
            // Alpha-only fade on a hardware layer: translating a full-screen
            // accessibility overlay forces a re-composite every frame and looks
            // janky on some devices.
            view.animate()
                .alpha(0f)
                .setDuration(220L)
                .withLayer()
                .withEndAction(remove)
                .start()
        } else {
            remove.run()
        }
    }

    private fun build(state: BlockedOverlayState): View {
        val root = FrameLayout(context).apply {
            background = ContextCompat.getDrawable(context, R.drawable.bg_blocked_overlay)
            isClickable = true
            setOnClickListener { state.onDismiss() }
        }

        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(context.dp(32), context.dp(32), context.dp(32), context.dp(32))
        }

        // Mode badge: HARD LOCK / SOFT LOCK
        column.addView(badge(state.modeLabel))
        column.addView(spacer(28))

        // Blocked app icon in a rounded plate
        column.addView(iconPlate(state.appIcon))
        column.addView(spacer(24))

        column.addView(
            text(
                text = "${state.appLabel} is locked",
                sp = 22f,
                font = R.font.manrope_600,
                colorRes = R.color.focus_ivory,
            )
        )
        if (state.profileName.isNotBlank()) {
            column.addView(spacer(6))
            column.addView(
                text(
                    text = state.profileName.uppercase(),
                    sp = 12f,
                    font = R.font.manrope_500,
                    colorRes = R.color.focus_ivory_faint,
                ).apply { letterSpacing = 0.1f }
            )
        }
        column.addView(spacer(20))

        // Countdown
        timerView = text(
            text = "--:--:--",
            sp = 46f,
            font = R.font.plex_mono_medium,
            colorRes = R.color.focus_amber,
        ).also { column.addView(it) }

        column.addView(
            text(
                text = "unlocks at ${state.unlockTime}",
                sp = 14f,
                font = R.font.manrope_400,
                colorRes = R.color.focus_ivory_dim,
            )
        )
        column.addView(spacer(20))

        progressView = CapsuleProgressView(context).also {
            column.addView(
                it,
                LinearLayout.LayoutParams(context.dp(220), context.dp(6)),
            )
        }

        if (state.motto.isNotBlank()) {
            column.addView(spacer(32))
            column.addView(
                text(
                    text = "\u201C${state.motto}\u201D",
                    sp = 19f,
                    font = R.font.manrope_500,
                    colorRes = R.color.focus_amber_light,
                    center = true,
                )
            )
        }

        root.addView(
            column,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER,
            ),
        )

        root.addView(
            text(
                text = "Tap to return",
                sp = 12f,
                font = R.font.manrope_500,
                colorRes = R.color.focus_ivory_faint,
            ),
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
            ).apply { bottomMargin = context.dp(48) },
        )

        return root
    }

    private fun badge(label: String): TextView = text(
        text = label.uppercase(),
        sp = 11f,
        font = R.font.plex_mono_semibold,
        colorRes = R.color.focus_amber,
    ).apply {
        letterSpacing = 0.16f
        setPadding(context.dp(14), context.dp(7), context.dp(14), context.dp(7))
        background = ContextCompat.getDrawable(context, R.drawable.bg_lock_pill)
    }

    private fun iconPlate(icon: Drawable?): View {
        val plateSize = context.dp(104)
        val iconSize = context.dp(60)
        return FrameLayout(context).apply {
            background = ContextCompat.getDrawable(context, R.drawable.bg_icon_plate)
            addView(
                ImageView(context).apply {
                    setImageDrawable(
                        icon ?: ContextCompat.getDrawable(context, R.drawable.ic_lock_outline)
                    )
                    scaleType = ImageView.ScaleType.FIT_CENTER
                },
                FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER),
            )
        }.also {
            it.layoutParams = LinearLayout.LayoutParams(plateSize, plateSize)
        }
    }

    private fun text(
        text: String,
        sp: Float,
        font: Int,
        colorRes: Int,
        center: Boolean = false,
    ): TextView = TextView(context).apply {
        this.text = text
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        setTextColor(ContextCompat.getColor(context, colorRes))
        ResourcesCompat.getFont(context, font)?.let { typeface = it }
        if (center) {
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }
    }

    private fun spacer(heightDp: Int): View =
        View(context).also { it.layoutParams = LinearLayout.LayoutParams(1, context.dp(heightDp)) }

    private fun Context.dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        fun formatCountdown(duration: Duration): String {
            val total = duration.seconds.coerceAtLeast(0)
            val hours = total / 3600
            val minutes = (total % 3600) / 60
            val seconds = total % 60
            return "%02d:%02d:%02d".format(hours, minutes, seconds)
        }

        fun formatUnlockTime(instant: Instant): String =
            DateTimeFormatter.ofPattern("HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(instant)

        fun prefersReducedMotion(context: Context): Boolean = runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }
}

/** Everything the overlay needs to render a single block event. */
internal data class BlockedOverlayState(
    val profileName: String,
    val motto: String,
    val appLabel: String,
    val appIcon: Drawable?,
    val modeLabel: String,
    val unlockTime: String,
    val onDismiss: () -> Unit,
)

/** Thin amber session-progress capsule, drawn locally. */
internal class CapsuleProgressView(context: Context) : View(context) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.focus_charcoal_500)
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.focus_amber)
    }

    var progress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = height / 2f
        canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), radius, radius, trackPaint)
        if (progress > 0f) {
            canvas.drawRoundRect(0f, 0f, width * progress, height.toFloat(), radius, radius, fillPaint)
        }
    }
}
