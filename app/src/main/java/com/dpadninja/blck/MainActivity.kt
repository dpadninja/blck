package com.dpadninja.blck

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.dpadninja.blck.ui.AppPickerScreen
import com.dpadninja.blck.ui.SettingsActions
import com.dpadninja.blck.ui.SettingsScreen
import com.dpadninja.blck.ui.SettingsUiState

class MainActivity : ComponentActivity() {

    private var state by mutableStateOf(
        SettingsUiState(
            enabled = true,
            idleEnabled = true,
            timeoutSec = 300,
            allowedPackages = emptySet(),
            overlayGranted = false,
            monitorEnabled = false,
        ),
    )

    private var showAppPicker by mutableStateOf(false)

    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSettings.init(this)
        ensureServiceRunning(this)
        refresh()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            if (showAppPicker) {
                BackHandler { showAppPicker = false }
                AppPickerScreen(
                    selected = state.allowedPackages,
                    onToggle = { pkg ->
                        val current = AppSettings.allowedPackages
                        AppSettings.allowedPackages =
                            if (pkg in current) current - pkg else current + pkg
                        refresh()
                    },
                )
                return@setContent
            }

            SettingsScreen(
                state = state,
                actions = SettingsActions(
                    onToggleEnabled = {
                        AppSettings.enabled = !AppSettings.enabled
                        refresh()
                    },
                    onToggleIdle = {
                        AppSettings.idleTimeoutEnabled = !AppSettings.idleTimeoutEnabled
                        refresh()
                    },
                    onCycleTimeout = {
                        val list = AppSettings.TIMEOUTS
                        val next = (list.indexOf(AppSettings.idleTimeoutSec) + 1) % list.size
                        AppSettings.idleTimeoutSec = list[next]
                        refresh()
                    },
                    onOpenAppFilter = { showAppPicker = true },
                    onGrantOverlay = { openOverlaySettings() },
                    onOpenAccessibility = { openAccessibilitySettings() },
                ),
            )
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        state = readState()
    }

    private fun readState() = SettingsUiState(
        enabled = AppSettings.enabled,
        idleEnabled = AppSettings.idleTimeoutEnabled,
        timeoutSec = AppSettings.idleTimeoutSec,
        allowedPackages = AppSettings.allowedPackages,
        overlayGranted = Settings.canDrawOverlays(this),
        monitorEnabled = isAccessibilityServiceEnabled(this),
    )

    private fun openOverlaySettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:$packageName".toUri(),
        )
        if (!launch(intent)) {
            launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
        }
    }

    private fun openAccessibilitySettings() {

        if (!launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))) {
            toast(getString(R.string.toast_no_accessibility_screen))
        }
    }

    private fun launch(intent: Intent): Boolean = runCatching {
        startActivity(intent)
        true
    }.getOrDefault(false)

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()
}

fun isAccessibilityServiceEnabled(context: Context): Boolean {
    if (InputBus.monitorConnected.value) return true

    val master = Settings.Secure.getInt(
        context.contentResolver,
        Settings.Secure.ACCESSIBILITY_ENABLED,
        0,
    )
    if (master != 1) return false

    val expected = "${context.packageName}/${InputMonitorService::class.java.name}"
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ) ?: return false

    val splitter = TextUtils.SimpleStringSplitter(':')
    splitter.setString(enabledServices)
    for (entry in splitter) {
        if (entry.equals(expected, ignoreCase = true)) return true
    }
    return false
}
