package com.dpadninja.blck

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private const val CHANNEL_ID = "blck_service"
private const val NOTIFICATION_ID = 1

class BlackoutService : Service() {

    companion object {
        const val ACTION_HIDE = "com.dpadninja.blck.HIDE"

        private val DISMISS_REASONS = setOf("homekey", "recentapps", "assist", "dream")

        private const val DISMISS_GRACE_MS = 400L

        val running = MutableStateFlow(false)
        val overlayVisible = MutableStateFlow(false)
    }

    private val scope = CoroutineScope(Dispatchers.Main.immediate + Job())
    private lateinit var overlay: BlackoutOverlay
    private var idleJob: Job? = null
    private var started = false
    private var screenOn = true

    private var shownAt = 0L

    @Suppress("DEPRECATION")
    private val systemReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    screenOn = false
                    if (overlay.isShowing) Log.w("blck-dismiss", "the screen turned off")
                    hideOverlay()
                    idleJob?.cancel()
                }

                Intent.ACTION_SCREEN_ON -> {
                    screenOn = true
                    resetIdleTimer()
                }

                Intent.ACTION_CLOSE_SYSTEM_DIALOGS -> {
                    val reason = intent.getStringExtra("reason")
                    if (overlay.isShowing) {
                        Log.w("blck-dismiss", "close_system_dialogs reason=$reason " +
                            "(гасим: ${reason in DISMISS_REASONS})")
                    }
                    if (overlay.isShowing && reason in DISMISS_REASONS) {
                        dismissAndReset("close_system_dialogs/$reason")
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        AppSettings.init(this)
        overlay = BlackoutOverlay(this)
        running.value = true
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundSafely()

        if (!started) {
            started = true
            @Suppress("DEPRECATION")
            registerReceiver(
                systemReceiver,
                IntentFilter().apply {
                    addAction(Intent.ACTION_SCREEN_ON)
                    addAction(Intent.ACTION_SCREEN_OFF)
                    addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
                },
                RECEIVER_EXPORTED
            )
            observeInput()
            observeSettings()
            WatchdogReceiver.schedule(this)
        }

        when (intent?.action) {
            ACTION_HIDE -> dismissAndReset("cmd ACTION_HIDE")
            else -> resetIdleTimer()
        }
        return START_STICKY
    }

    private fun observeInput() = scope.launch {
        InputBus.events.collect { event ->
            when {
                event == InputBus.Event.BLACKOUT_TOGGLE -> when {
                    overlay.isShowing -> dismissAndReset("chord")
                    AppSettings.enabled -> showOverlay(force = true)
                    else -> Unit
                }

                overlay.isShowing && event == InputBus.Event.SYSTEM_NAVIGATION ->
                    dismissAndReset("acc event: switch to the launcher")

                overlay.isShowing && event == InputBus.Event.USER_KEY_UP ->
                    if (SystemClock.elapsedRealtime() - shownAt > DISMISS_GRACE_MS) {
                        dismissAndReset("dpad key")
                    } else {
                        Log.w("blck-dismiss", "dpad key first time $DISMISS_GRACE_MS - ignoring")
                    }
                overlay.isShowing -> Unit
                else -> resetIdleTimer()
            }
        }
    }

    private fun observeSettings() = scope.launch {
        AppSettings.revision.drop(1).collect {
            if (!AppSettings.enabled) hideOverlay()
            resetIdleTimer()
        }
    }

    private fun canBlackout(): Boolean =
        AppSettings.enabled &&
            AppSettings.idleTimeoutEnabled &&
            screenOn &&
            Settings.canDrawOverlays(this) &&
            packageAllowed()

    private fun packageAllowed(): Boolean {
        val allowed = AppSettings.allowedPackages
        if (allowed.isEmpty()) return false
        return InputBus.foregroundPackage.value in allowed
    }

    private fun resetIdleTimer() {
        idleJob?.cancel()
        if (!canBlackout()) return
        val timeoutMs = AppSettings.idleTimeoutSec * 1000L
        idleJob = scope.launch {
            delay(timeoutMs.milliseconds)
            showOverlay()
        }
    }

    private fun showOverlay(force: Boolean = false) {
        if (!force && !canBlackout()) return
        if (!Settings.canDrawOverlays(this)) return

        if (force) idleJob?.cancel()
        overlay.show()
        if (overlay.isShowing) shownAt = SystemClock.elapsedRealtime()
        overlayVisible.value = overlay.isShowing
    }

    private fun hideOverlay() {
        overlay.hide()
        overlayVisible.value = false
    }

    private fun dismissAndReset(source: String) {
        Log.w("blck-dismiss", "hide overlay: $source")
        hideOverlay()
        resetIdleTimer()
    }

    // --- живучесть ----------------------------------------------------------

    override fun onTaskRemoved(rootIntent: Intent?) {
        ensureServiceRunning(this)
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        hideOverlay()
        running.value = false
        runCatching { unregisterReceiver(systemReceiver) }
        scope.cancel()

        WatchdogReceiver.schedule(this)
        super.onDestroy()
    }

    private fun startForegroundSafely() {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply { setShowBadge(false) },
            )
        }

        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.presence_invisible)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }

        try {
            ServiceCompat.startForeground(
                this@BlackoutService,
                NOTIFICATION_ID,
                notification,
                type,
            )
        } catch (e: Exception) {
            Log.w("blck", "startForeground failed: $e")
        }
    }
}
