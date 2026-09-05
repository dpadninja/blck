package com.dpadninja.blck.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dpadninja.blck.BlackoutService
import com.dpadninja.blck.InputBus
import com.dpadninja.blck.R
import com.dpadninja.blck.ui.core.BasicWindow
import com.dpadninja.blck.ui.core.ErrorColor
import com.dpadninja.blck.ui.core.Section
import com.dpadninja.blck.ui.core.SettingItem
import com.dpadninja.blck.ui.core.SettingPermissionItem
import com.dpadninja.blck.ui.core.SettingSwitchItem
import com.dpadninja.blck.ui.core.SettingValueItem
import com.dpadninja.blck.ui.core.WindowHeader

data class SettingsUiState(
    val enabled: Boolean,
    val idleEnabled: Boolean,
    val timeoutSec: Int,
    val allowedPackages: Set<String>,
    val overlayGranted: Boolean,
    val monitorEnabled: Boolean,
)

data class SettingsActions(
    val onToggleEnabled: () -> Unit,
    val onToggleIdle: () -> Unit,
    val onCycleTimeout: () -> Unit,
    val onOpenAppFilter: () -> Unit,
    val onGrantOverlay: () -> Unit,
    val onOpenAccessibility: () -> Unit,
)

@Composable
fun SettingsScreen(state: SettingsUiState, actions: SettingsActions) {
    val monitorConnected by InputBus.monitorConnected.collectAsState()
    val monitorEnabled = state.monitorEnabled || monitorConnected
    val ready = state.overlayGranted && monitorEnabled
//    val serviceRunning by BlackoutService.running.collectAsState()
    val idleSettingsEnabled = state.enabled && state.idleEnabled

    BasicWindow {
        WindowHeader(title = stringResource(R.string.settings_title))
        Section {
            SettingSwitchItem(
                caption = stringResource(R.string.functionality_title),
                switchState = state.enabled,
                description = "When enabled press Back+Ok to dim the screen",
                setFocus = true,
                onClick = { actions.onToggleEnabled() },
            )
        }
        Section(title = stringResource(R.string.idle_header)) {
            SettingSwitchItem(
                caption = stringResource(R.string.idle_enabled_title),
                switchState = state.idleEnabled,
                enabled = state.enabled,
                onClick = { actions.onToggleIdle() },
            )
            SettingValueItem(
                caption = stringResource(R.string.timeout_title),
                value = formatTimeout(state.timeoutSec),
                enabled = idleSettingsEnabled,
                onClick = actions.onCycleTimeout,
            )
            SettingItem(
                caption = stringResource(R.string.apps_title),
                value = if (state.allowedPackages.isEmpty()) {
                    stringResource(R.string.apps_none)
                } else {
                    stringResource(R.string.apps_selected, state.allowedPackages.size)
                },
                enabled = idleSettingsEnabled,
                onClick = actions.onOpenAppFilter,
            )
        }
        Section(title = stringResource(R.string.permissions_header)) {
            SettingPermissionItem(
                caption = stringResource(R.string.overlay_title),
                description = stringResource(R.string.overlay_summary),
                value = state.overlayGranted,
                enabled = !state.overlayGranted,
                onClick = actions.onGrantOverlay,
            )
            SettingPermissionItem(
                caption = stringResource(R.string.accessibility_title),
                description = stringResource(R.string.accessibility_title_summary),
                value = monitorEnabled,
                enabled = !monitorEnabled,
                onClick = actions.onOpenAccessibility,
            )
        }

//        val keys by InputBus.keyEventCount.collectAsState()
//        val uiEvents by InputBus.uiEventCount.collectAsState()
//        val foreground by InputBus.foregroundPackage.collectAsState()


        if (!ready) {
        Column(modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.status_missing_permissions),
                color = ErrorColor,
                fontSize = 12.sp,
            )
        }}
    }
}

@Composable
private fun formatTimeout(sec: Int): String = when {
    sec < 60 -> stringResource(R.string.timeout_seconds, sec)
    else -> stringResource(R.string.timeout_minutes, sec / 60)
}
