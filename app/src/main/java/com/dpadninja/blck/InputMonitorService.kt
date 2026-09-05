package com.dpadninja.blck

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent


class InputMonitorService : AccessibilityService() {
    private var homePackage: String? = null
    private var lastEnsureAt = 0L

    /** Состояние аккорда «назад + ок». */
    private var backDownAt = 0L
    private var chordFired = false
    private var eatConfirmUp = false

    private val swallowedKeys = mutableSetOf<Int>()


    private val handler = Handler(Looper.getMainLooper())

    private var launchablePackages: Set<String> = emptySet()
    private var lastPackageScanAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        AppSettings.init(this)
        InputBus.monitorConnected.value = true
        homePackage = resolveHomePackage()
        refreshLaunchablePackages()
        ensureServiceRunning(this)
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            InputBus.keyEventCount.value = InputBus.keyEventCount.value + 1
        }

        if (AppSettings.enabled && handleChord(event)) return true

        val swallowed = if (event.keyCode in VOLUME_KEYS) {
            handleVolumeKey(event)
        } else {
            swallowWhileBlackout(event)
        }

        if (event.keyCode == KeyEvent.KEYCODE_HOME) {
            InputBus.post(InputBus.Event.SYSTEM_NAVIGATION)
        } else if (event.action == KeyEvent.ACTION_UP &&
            event.keyCode !in VOLUME_KEYS &&
            !event.isCanceled
        ) {
            InputBus.post(InputBus.Event.USER_KEY_UP)
        } else {
            InputBus.post(InputBus.Event.USER_INPUT)
        }
        ensurePeriodically()
        return swallowed
    }

    private fun swallowWhileBlackout(event: KeyEvent): Boolean {
        if (event.keyCode in SYSTEM_KEYS) return false
        return when (event.action) {
            // Нажатие съедаем, только пока окно показано, и запоминаем клавишу.
            KeyEvent.ACTION_DOWN -> {
                if (!BlackoutService.overlayVisible.value) return false
                swallowedKeys.add(event.keyCode)
                true
            }

            KeyEvent.ACTION_UP -> swallowedKeys.remove(event.keyCode)

            else -> false
        }
    }

    private fun handleVolumeKey(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) ensureOneVolumeStep(event.keyCode)
        return true
    }

    private fun ensureOneVolumeStep(keyCode: Int) {
        val audio = getSystemService(AudioManager::class.java) ?: return
        val before = volumeState(audio)
        handler.postDelayed({
            if (volumeState(audio) != before) return@postDelayed
            val direction = when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> AudioManager.ADJUST_RAISE
                KeyEvent.KEYCODE_VOLUME_DOWN -> AudioManager.ADJUST_LOWER
                else -> AudioManager.ADJUST_TOGGLE_MUTE
            }

        }, VOLUME_FALLBACK_MS)
    }

    private fun volumeState(audio: AudioManager): Pair<Int, Boolean> =
        audio.getStreamVolume(AudioManager.STREAM_MUSIC) to
            audio.isStreamMute(AudioManager.STREAM_MUSIC)

    private fun handleChord(event: KeyEvent): Boolean {
        when (event.keyCode) {
            KeyEvent.KEYCODE_BACK -> when (event.action) {
                KeyEvent.ACTION_DOWN -> if (event.repeatCount == 0) {
                    backDownAt = SystemClock.elapsedRealtime()
                    chordFired = false
                }

                KeyEvent.ACTION_UP -> {
                    backDownAt = 0L
                    val eat = chordFired
                    chordFired = false
                    if (eat) return true
                }
            }

            in CONFIRM_KEYS -> when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    if (!isBackHeld() || event.repeatCount > 0) return false
                    backDownAt = 0L
                    chordFired = true
                    eatConfirmUp = true
                    InputBus.post(InputBus.Event.BLACKOUT_TOGGLE)
                    return true
                }

                KeyEvent.ACTION_UP -> {
                    if (!eatConfirmUp) return false
                    eatConfirmUp = false
                    return true
                }
            }
        }
        return false
    }

    private fun isBackHeld(): Boolean =
        backDownAt != 0L && SystemClock.elapsedRealtime() - backDownAt < CHORD_WINDOW_MS

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString()
        InputBus.uiEventCount.value = InputBus.uiEventCount.value + 1

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            updateForegroundPackage(pkg)
        }

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && pkg == homePackage) {
            InputBus.post(InputBus.Event.SYSTEM_NAVIGATION)
        } else {
            InputBus.post(InputBus.Event.USER_INPUT)
        }
        ensurePeriodically()
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        InputBus.monitorConnected.value = false
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        InputBus.monitorConnected.value = false
        handler.removeCallbacksAndMessages(null)
        swallowedKeys.clear()
        super.onDestroy()
    }

    private fun ensurePeriodically() {
        val now = SystemClock.elapsedRealtime()
        if (BlackoutService.running.value || now - lastEnsureAt < 30_000L) return
        lastEnsureAt = now
        ensureServiceRunning(this)
    }

    private fun updateForegroundPackage(pkg: String?) {
        if (pkg == null || pkg == packageName) return
        if (!isRealApp(pkg)) {
            if (!rescanIfStale()) return
            if (!isRealApp(pkg)) return
        }
        InputBus.foregroundPackage.value = pkg
    }

    private fun isRealApp(pkg: String): Boolean =
        pkg in launchablePackages || pkg in AppSettings.allowedPackages

    private fun rescanIfStale(): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (now - lastPackageScanAt < 5 * 60_000L) return false
        refreshLaunchablePackages()
        return true
    }

    private fun refreshLaunchablePackages() {
        lastPackageScanAt = SystemClock.elapsedRealtime()
        launchablePackages = LAUNCHER_CATEGORIES.flatMapTo(mutableSetOf()) { category ->
            packageManager
                .queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(category), 0)
                .map { it.activityInfo.packageName }
        }
    }

    private fun resolveHomePackage(): String? {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return packageManager.resolveActivity(intent, 0)?.activityInfo?.packageName
    }

    companion object {
        private const val CHORD_WINDOW_MS = 3_000L

        private const val VOLUME_FALLBACK_MS = 150L

        private val VOLUME_KEYS = setOf(
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_MUTE,
        )

        private val SYSTEM_KEYS = setOf(
            KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_POWER,
            KeyEvent.KEYCODE_TV_POWER,
            KeyEvent.KEYCODE_SLEEP,
            KeyEvent.KEYCODE_WAKEUP,
        )

        private val CONFIRM_KEYS = setOf(
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_BUTTON_A,
        )

        val LAUNCHER_CATEGORIES = listOf(
            Intent.CATEGORY_LEANBACK_LAUNCHER,
            Intent.CATEGORY_LAUNCHER,
        )
    }
}
