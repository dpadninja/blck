package com.dpadninja.blck

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.provider.Settings
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.view.doOnAttach

private const val FADE_MS = 600L

class BlackoutOverlay(context: Context) {
    private val windowContext: Context = runCatching {
        val display = context.getSystemService(DisplayManager::class.java)
            .getDisplay(Display.DEFAULT_DISPLAY)
        context.createDisplayContext(display)
            .createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null)
    }.getOrElse {
        Log.w("blck", "window context unavailable, falling back: $it")
        context
    }
    private val wm = windowContext.getSystemService(WindowManager::class.java)

    private var view: View? = null

    val isShowing: Boolean get() = view != null

    fun show() {
        if (view != null) return
        if (!Settings.canDrawOverlays(windowContext)) {
            Log.w("blck", "overlay permission missing")
            return
        }

        val v = OverlayView(windowContext)

        val flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            title = "blck"
        }

        v.doOnAttach {
            it.windowInsetsController?.hide(WindowInsets.Type.systemBars())
        }

        runCatching { wm.addView(v, params) }
            .onFailure { e ->
                Log.e("blck", "addView failed: $e")
                return
            }

        view = v
        v.animate().alpha(1f).setDuration(FADE_MS).start()
    }

    fun hide() {
        val v = view ?: return
        view = null
        v.animate().cancel()
        runCatching { wm.removeView(v) }
            .onFailure { Log.w("blck", "removeView failed: $it") }
    }
}

private class OverlayView(context: Context) : FrameLayout(context) {

    init {
        setBackgroundColor(Color.BLACK)
        alpha = 0f
        isFocusable = false
        isFocusableInTouchMode = false
        descendantFocusability = FOCUS_BLOCK_DESCENDANTS
    }
}
