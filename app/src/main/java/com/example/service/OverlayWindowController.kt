package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.domain.model.AppSettings
import com.example.domain.model.LiveSubtitleState
import com.example.domain.model.TranslatorStatus
import kotlin.math.roundToInt

class OverlayWindowController(
    private val context: Context,
    private val onTogglePlayStop: () -> Unit,
    private val onClearSubtitles: () -> Unit,
    private val onToggleMinimize: () -> Unit,
    private val onCloseOverlay: () -> Unit,
    private val onBoundsChanged: (x: Int, y: Int, widthDp: Int, heightDp: Int) -> Unit,
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var rootView: FrameLayout? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var expandedContainer: LinearLayout? = null
    private var minimizedContainer: LinearLayout? = null

    private var statusDotView: View? = null
    private var statusBadgeText: TextView? = null
    private var playStopBtn: ImageButton? = null
    private var minimizeBtn: ImageButton? = null
    private var clearBtn: ImageButton? = null
    private var closeBtn: ImageButton? = null
    private var subtitleScrollView: ScrollView? = null
    private var subtitleTextView: TextView? = null
    private var resizeHandleView: View? = null

    private var miniDotView: View? = null
    private var miniPreviewText: TextView? = null
    private var miniExpandBtn: ImageButton? = null
    private var miniStopBtn: ImageButton? = null

    private var currentSettings = AppSettings()
    private val density = context.resources.displayMetrics.density

    private val burmeseTypeface: Typeface? by lazy {
        ResourcesCompat.getFont(context, R.font.noto_sans_myanmar)
    }

    private val monoTypeface: Typeface? by lazy {
        ResourcesCompat.getFont(context, R.font.jetbrains_mono)
    }

    val isAttached: Boolean
        get() = rootView != null

    @SuppressLint("ClickableViewAccessibility")
    fun show(settings: AppSettings, state: LiveSubtitleState) {
        currentSettings = settings
        if (rootView != null) {
            updateAppearance(settings)
            updateState(state)
            return
        }

        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val widthPx = if (state.isOverlayMinimized) {
            WindowManager.LayoutParams.WRAP_CONTENT
        } else {
            dpToPx(settings.overlayWidthDp)
        }
        val heightPx = if (state.isOverlayMinimized) {
            WindowManager.LayoutParams.WRAP_CONTENT
        } else {
            dpToPx(settings.overlayHeightDp)
        }

        val params = WindowManager.LayoutParams(
            widthPx,
            heightPx,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = settings.overlayX
            y = settings.overlayY
        }

        val container = buildOverlayView(settings)
        layoutParams = params
        rootView = container

        runCatching {
            windowManager.addView(container, params)
            updateState(state)
        }.onFailure {
            rootView = null
            layoutParams = null
        }
    }

    fun hide() {
        val view = rootView ?: return
        runCatching { windowManager.removeViewImmediate(view) }
        rootView = null
        layoutParams = null
    }

    fun updateAppearance(settings: AppSettings) {
        currentSettings = settings
        val root = rootView ?: return
        root.background = createGlassBackground(settings.overlayOpacity)
        subtitleTextView?.setTextSize(TypedValue.COMPLEX_UNIT_SP, settings.overlayFontSizeSp)
        val params = layoutParams ?: return
        if (expandedContainer?.visibility == View.VISIBLE) {
            params.width = dpToPx(settings.overlayWidthDp)
            params.height = dpToPx(settings.overlayHeightDp)
            runCatching { windowManager.updateViewLayout(root, params) }
        }
    }

    fun updateState(state: LiveSubtitleState) {
        val root = rootView ?: return
        val params = layoutParams ?: return

        val isMinimized = state.isOverlayMinimized
        expandedContainer?.visibility = if (isMinimized) View.GONE else View.VISIBLE
        minimizedContainer?.visibility = if (isMinimized) View.VISIBLE else View.GONE

        if (isMinimized) {
            params.width = WindowManager.LayoutParams.WRAP_CONTENT
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
        } else {
            params.width = dpToPx(currentSettings.overlayWidthDp)
            params.height = dpToPx(currentSettings.overlayHeightDp)
        }
        runCatching { windowManager.updateViewLayout(root, params) }

        val dotColor = when (state.status) {
            TranslatorStatus.LISTENING -> Color.parseColor("#10B981")
            TranslatorStatus.TRANSLATING -> Color.parseColor("#22D3EE")
            TranslatorStatus.CONNECTING -> Color.parseColor("#F59E0B")
            TranslatorStatus.ERROR -> Color.parseColor("#F87171")
            TranslatorStatus.IDLE -> Color.parseColor("#64748B")
        }
        statusDotView?.background = createCircleDrawable(dotColor)
        miniDotView?.background = createCircleDrawable(dotColor)

        val statusLabel = when (state.status) {
            TranslatorStatus.IDLE -> "PAUSED"
            TranslatorStatus.CONNECTING -> "CONN"
            TranslatorStatus.LISTENING -> "LIVE"
            TranslatorStatus.TRANSLATING -> "MM"
            TranslatorStatus.ERROR -> "ERR"
        }
        statusBadgeText?.text = "$statusLabel • ${state.activeSource.shortBadge}"

        val isRunning = state.isCapturing ||
            state.status == TranslatorStatus.CONNECTING ||
            state.status == TranslatorStatus.LISTENING ||
            state.status == TranslatorStatus.TRANSLATING

        playStopBtn?.setImageResource(
            if (isRunning) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        )
        playStopBtn?.setColorFilter(
            if (isRunning) Color.parseColor("#F59E0B") else Color.parseColor("#22D3EE")
        )
        miniStopBtn?.setImageResource(
            if (isRunning) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        )

        val combinedText = buildString {
            if (!state.errorMessage.isNullOrBlank()) {
                append("⚠ ")
                append(state.errorMessage)
            } else {
                val recent = state.recentLines.takeLast(4)
                recent.forEachIndexed { idx, line ->
                    if (idx > 0) append("\n")
                    append(line)
                }
                if (state.currentSubtitle.isNotBlank()) {
                    if (isNotEmpty()) append("\n")
                    append(state.currentSubtitle)
                }
            }
        }

        val displayText = if (combinedText.isBlank()) {
            if (isRunning) {
                "နားထောင်နေပါသည်… မြန်မာစာတန်းထိုး ပေါ်လာပါမည်"
            } else {
                "Start ကိုနှိပ်ပြီး တိုက်ရိုက် မြန်မာစာတန်းထိုး စတင်ပါ"
            }
        } else {
            combinedText
        }

        subtitleTextView?.apply {
            text = displayText
            setTextColor(
                if (!state.errorMessage.isNullOrBlank()) {
                    Color.parseColor("#FCA5A5")
                } else if (combinedText.isBlank()) {
                    Color.parseColor("#94A3B8")
                } else {
                    Color.parseColor("#F8FAFC")
                }
            )
        }

        miniPreviewText?.text = when {
            !state.errorMessage.isNullOrBlank() -> "Error: ${state.errorMessage}"
            state.currentSubtitle.isNotBlank() -> state.currentSubtitle
            state.recentLines.isNotEmpty() -> state.recentLines.last()
            isRunning -> "မြန်မာစာတန်းထိုး LIVE…"
            else -> "Gemini Live MM"
        }

        subtitleScrollView?.post {
            subtitleScrollView?.fullScroll(View.FOCUS_DOWN)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun buildOverlayView(settings: AppSettings): FrameLayout {
        val root = FrameLayout(context).apply {
            background = createGlassBackground(settings.overlayOpacity)
            elevation = dpToPx(8).toFloat()
        }

        // Expanded Container
        val expanded = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(10))
        }

        // Top Drag & Control Header
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 0, 0, dpToPx(6))
        }

        val dot = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(8), dpToPx(8)).apply {
                marginEnd = dpToPx(6)
            }
            background = createCircleDrawable(Color.parseColor("#64748B"))
        }
        statusDotView = dot

        val badge = TextView(context).apply {
            text = "PAUSED • ${settings.audioSource.shortBadge}"
            setTextColor(Color.parseColor("#94A3B8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = monoTypeface ?: Typeface.MONOSPACE
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        statusBadgeText = badge

        val playBtn = createHeaderIconButton(
            iconRes = android.R.drawable.ic_media_play,
            tint = Color.parseColor("#22D3EE"),
            contentDesc = context.getString(R.string.overlay_cd_toggle)
        ) { onTogglePlayStop() }
        playStopBtn = playBtn

        val clrBtn = createHeaderIconButton(
            iconRes = android.R.drawable.ic_menu_delete,
            tint = Color.parseColor("#CBD5E1"),
            contentDesc = context.getString(R.string.overlay_cd_clear)
        ) { onClearSubtitles() }
        clearBtn = clrBtn

        val minBtn = createHeaderIconButton(
            iconRes = android.R.drawable.arrow_down_float,
            tint = Color.parseColor("#CBD5E1"),
            contentDesc = context.getString(R.string.overlay_cd_minimize)
        ) { onToggleMinimize() }
        minimizeBtn = minBtn

        val clsBtn = createHeaderIconButton(
            iconRes = android.R.drawable.ic_menu_close_clear_cancel,
            tint = Color.parseColor("#F87171"),
            contentDesc = context.getString(R.string.overlay_cd_close)
        ) { onCloseOverlay() }
        closeBtn = clsBtn

        header.addView(dot)
        header.addView(badge)
        header.addView(playBtn)
        header.addView(clrBtn)
        header.addView(minBtn)
        header.addView(clsBtn)

        // Subtle divider
        val divider = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(1)
            ).apply {
                bottomMargin = dpToPx(6)
            }
            setBackgroundColor(Color.parseColor("#22FFFFFF"))
        }

        // Scrollable Burmese subtitle body
        val scroll = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            isVerticalScrollBarEnabled = false
        }
        val subText = TextView(context).apply {
            typeface = burmeseTypeface ?: Typeface.SANS_SERIF
            setTextSize(TypedValue.COMPLEX_UNIT_SP, settings.overlayFontSizeSp)
            setTextColor(Color.parseColor("#F8FAFC"))
            setLineSpacing(dpToPx(4).toFloat(), 1.18f)
            setShadowLayer(4f, 1f, 1.5f, Color.BLACK)
            setPadding(dpToPx(2), dpToPx(2), dpToPx(14), dpToPx(6))
        }
        scroll.addView(subText)
        subtitleScrollView = scroll
        subtitleTextView = subText

        expanded.addView(header)
        expanded.addView(divider)
        expanded.addView(scroll)
        expandedContainer = expanded

        // Bottom-Right Resize Grip (only visible in expanded mode)
        val resizeHandle = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_menu_crop)
            setColorFilter(Color.parseColor("#8822D3EE"))
            contentDescription = context.getString(R.string.overlay_cd_resize)
            val size = dpToPx(22)
            layoutParams = FrameLayout.LayoutParams(size, size).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                marginEnd = dpToPx(4)
                bottomMargin = dpToPx(4)
            }
            setPadding(dpToPx(3), dpToPx(3), dpToPx(3), dpToPx(3))
        }
        resizeHandleView = resizeHandle

        // Minimized Pill Container
        val minimized = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                dpToPx(48)
            )
            setPadding(dpToPx(12), dpToPx(6), dpToPx(8), dpToPx(6))
        }

        val mDot = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(9), dpToPx(9)).apply {
                marginEnd = dpToPx(8)
            }
            background = createCircleDrawable(Color.parseColor("#22D3EE"))
        }
        miniDotView = mDot

        val mPreview = TextView(context).apply {
            typeface = burmeseTypeface ?: Typeface.SANS_SERIF
            setTextColor(Color.parseColor("#F8FAFC"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            maxWidth = dpToPx(170)
            text = "Gemini Live MM"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dpToPx(8)
            }
        }
        miniPreviewText = mPreview

        val mPlayStop = createHeaderIconButton(
            iconRes = android.R.drawable.ic_media_pause,
            tint = Color.parseColor("#F59E0B"),
            contentDesc = context.getString(R.string.overlay_cd_toggle)
        ) { onTogglePlayStop() }
        miniStopBtn = mPlayStop

        val mExpand = createHeaderIconButton(
            iconRes = android.R.drawable.arrow_up_float,
            tint = Color.parseColor("#22D3EE"),
            contentDesc = context.getString(R.string.overlay_cd_expand)
        ) { onToggleMinimize() }
        miniExpandBtn = mExpand

        minimized.addView(mDot)
        minimized.addView(mPreview)
        minimized.addView(mPlayStop)
        minimized.addView(mExpand)
        minimizedContainer = minimized

        root.addView(expanded)
        root.addView(resizeHandle)
        root.addView(minimized)

        attachDragListener(header)
        attachDragListener(subText)
        attachDragListener(minimized)
        attachResizeListener(resizeHandle, root)

        return root
    }

    private fun createHeaderIconButton(
        iconRes: Int,
        tint: Int,
        contentDesc: String,
        onClick: () -> Unit
    ): ImageButton {
        return ImageButton(context).apply {
            setImageResource(iconRes)
            setColorFilter(tint)
            contentDescription = contentDesc
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#1AFFFFFF"))
            }
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            val btnSize = dpToPx(32)
            layoutParams = LinearLayout.LayoutParams(btnSize, btnSize).apply {
                marginStart = dpToPx(4)
            }
            setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6))
            setOnClickListener { onClick() }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachDragListener(targetView: View) {
        var startX = 0
        var startY = 0
        var touchStartX = 0f
        var touchStartY = 0f
        var moved = false

        targetView.setOnTouchListener { _, event ->
            val params = layoutParams ?: return@setOnTouchListener false
            val root = rootView ?: return@setOnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    moved = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchStartX).roundToInt()
                    val dy = (event.rawY - touchStartY).roundToInt()
                    if (kotlin.math.abs(dx) > 4 || kotlin.math.abs(dy) > 4) {
                        moved = true
                        params.x = (startX + dx).coerceAtLeast(0)
                        params.y = (startY + dy).coerceAtLeast(0)
                        runCatching { windowManager.updateViewLayout(root, params) }
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (moved) {
                        onBoundsChanged(
                            params.x,
                            params.y,
                            currentSettings.overlayWidthDp,
                            currentSettings.overlayHeightDp
                        )
                    }
                    moved
                }

                else -> false
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachResizeListener(handle: View, root: FrameLayout) {
        var startWidthPx = 0
        var startHeightPx = 0
        var touchStartX = 0f
        var touchStartY = 0f

        handle.setOnTouchListener { _, event ->
            val params = layoutParams ?: return@setOnTouchListener false
            val minWidthPx = dpToPx(220)
            val maxWidthPx = (context.resources.displayMetrics.widthPixels - dpToPx(16)).coerceAtLeast(minWidthPx)
            val minHeightPx = dpToPx(96)
            val maxHeightPx = dpToPx(440)

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startWidthPx = params.width.takeIf { it > 0 } ?: dpToPx(currentSettings.overlayWidthDp)
                    startHeightPx = params.height.takeIf { it > 0 } ?: dpToPx(currentSettings.overlayHeightDp)
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchStartX).roundToInt()
                    val dy = (event.rawY - touchStartY).roundToInt()
                    params.width = (startWidthPx + dx).coerceIn(minWidthPx, maxWidthPx)
                    params.height = (startHeightPx + dy).coerceIn(minHeightPx, maxHeightPx)
                    runCatching { windowManager.updateViewLayout(root, params) }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val newWidthDp = (params.width / density).roundToInt().coerceIn(220, 620)
                    val newHeightDp = (params.height / density).roundToInt().coerceIn(96, 440)
                    currentSettings = currentSettings.copy(
                        overlayWidthDp = newWidthDp,
                        overlayHeightDp = newHeightDp
                    )
                    onBoundsChanged(params.x, params.y, newWidthDp, newHeightDp)
                    true
                }

                else -> false
            }
        }
    }

    private fun createGlassBackground(opacity: Float): GradientDrawable {
        val alphaInt = (opacity.coerceIn(0.4f, 0.96f) * 255).roundToInt()
        val bgColor = Color.argb(alphaInt, 9, 13, 22)
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(18).toFloat()
            setColor(bgColor)
            setStroke(dpToPx(1), Color.parseColor("#6622D3EE"))
        }
    }

    private fun createCircleDrawable(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
    }

    private fun dpToPx(dp: Int): Int = (dp * density).roundToInt()
}
