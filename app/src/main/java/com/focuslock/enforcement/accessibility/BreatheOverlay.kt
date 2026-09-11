package com.focuslock.enforcement.accessibility

import android.animation.ValueAnimator
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
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.focuslock.R

/**
 * The "pre-open" breathing pause shown over a blocked app for profiles using
 * [com.focuslock.domain.model.OpenBehavior.BREATHE]. Unlike the hard cover it
 * does not send the user Home: after the pause (or a tap) the app is allowed.
 */
internal class BreatheOverlay(
    private val context: Context,
    private val windowManager: WindowManager,
) {

    private var rootView: View? = null
    private var countdownView: TextView? = null

    val isShowing: Boolean get() = rootView != null

    fun show(state: BreatheOverlayState) {
        hide()

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

        column.addView(badge("PAUSE"))
        column.addView(spacer(24))
        column.addView(iconPlate(state.appIcon))
        column.addView(spacer(20))
        column.addView(
            text(
                "${state.appLabel} is one tap away",
                sp = 20f,
                font = R.font.manrope_600,
                colorRes = R.color.focus_ivory,
            )
        )
        column.addView(spacer(4))
        column.addView(
            text(
                "Take a breath first.",
                sp = 14f,
                font = R.font.manrope_400,
                colorRes = R.color.focus_ivory_dim,
            )
        )
        column.addView(spacer(28))
        column.addView(
            BreathingCircleView(context),
            LinearLayout.LayoutParams(context.dp(170), context.dp(170)),
        )
        column.addView(spacer(24))
        countdownView = text(
            "--",
            sp = 30f,
            font = R.font.plex_mono_medium,
            colorRes = R.color.focus_amber,
        ).also { column.addView(it) }

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
                "Tap to continue",
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
    }

    fun updateCountdown(secondsLeft: Int) {
        countdownView?.text = secondsLeft.coerceAtLeast(0).toString()
    }

    fun hide(animated: Boolean = true) {
        val view = rootView ?: return
        rootView = null
        countdownView = null
        val remove = Runnable { runCatching { windowManager.removeView(view) } }
        if (animated && !prefersReducedMotion(context)) {
            view.animate()
                .alpha(0f)
                .setDuration(200L)
                .withLayer()
                .withEndAction(remove)
                .start()
        } else {
            remove.run()
        }
    }

    private fun badge(label: String): TextView = text(
        label.uppercase(),
        sp = 11f,
        font = R.font.plex_mono_semibold,
        colorRes = R.color.focus_amber,
    ).apply {
        letterSpacing = 0.16f
        setPadding(context.dp(14), context.dp(7), context.dp(14), context.dp(7))
        background = ContextCompat.getDrawable(context, R.drawable.bg_lock_pill)
    }

    private fun iconPlate(icon: Drawable?): View {
        val plateSize = context.dp(84)
        val iconSize = context.dp(48)
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
        }.also { it.layoutParams = LinearLayout.LayoutParams(plateSize, plateSize) }
    }

    private fun text(
        text: String,
        sp: Float,
        font: Int,
        colorRes: Int,
    ): TextView = TextView(context).apply {
        this.text = text
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        setTextColor(ContextCompat.getColor(context, colorRes))
        ResourcesCompat.getFont(context, font)?.let { typeface = it }
    }

    private fun spacer(heightDp: Int): View =
        View(context).also { it.layoutParams = LinearLayout.LayoutParams(1, context.dp(heightDp)) }

    private fun Context.dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private companion object {
        fun prefersReducedMotion(context: Context): Boolean = runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }
}

/** Everything the breathing pause needs to render. */
internal data class BreatheOverlayState(
    val appLabel: String,
    val appIcon: Drawable?,
    val onDismiss: () -> Unit,
)

/** A gently pulsing circle to pace the breath. */
internal class BreathingCircleView(context: Context) : View(context) {

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.focus_amber)
        style = Paint.Style.STROKE
        strokeWidth = context.resources.displayMetrics.density * 2f
        alpha = 70
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.focus_amber)
        style = Paint.Style.FILL
        alpha = 190
    }

    private var phase = 0f
    private var animator: ValueAnimator? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 4_000L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                phase = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val maxRadius = minOf(width, height) / 2f
        canvas.drawCircle(cx, cy, maxRadius, ringPaint)
        val radius = maxRadius * (0.42f + 0.54f * phase)
        canvas.drawCircle(cx, cy, radius, fillPaint)
    }
}
