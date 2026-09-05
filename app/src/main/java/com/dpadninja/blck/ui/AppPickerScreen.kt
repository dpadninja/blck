package com.dpadninja.blck.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.dpadninja.blck.InputMonitorService
import com.dpadninja.blck.R
import com.dpadninja.blck.ui.core.AppItem
import com.dpadninja.blck.ui.core.BasicWindow
import com.dpadninja.blck.ui.core.Section
import com.dpadninja.blck.ui.core.TextSecondary
import com.dpadninja.blck.ui.core.WindowHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?,
    val launchable: Boolean,
)

@Composable
fun AppPickerScreen(
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    val context = LocalContext.current
    val apps by produceState<List<InstalledApp>?>(initialValue = null, context) {
        value = loadInstalledApps(context)
    }

    BasicWindow(scrollable = false) {
        WindowHeader(
            title = stringResource(R.string.picker_title),
            subtitle = stringResource(R.string.picker_summary),
        )

        val list = apps
        if (list == null) {
            Text(
                text = stringResource(R.string.picker_loading),
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 14.dp, top = 8.dp, bottom = 8.dp),
            )
            return@BasicWindow
        }

        if (selected.isEmpty()) {
            Text(
                text = stringResource(R.string.picker_empty_hint),
                color = TextSecondary,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 2.dp),
            )
        }

        Section {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 2.dp),
            ) {
                val launchable = list.filter { it.launchable }
                val rest = list.filterNot { it.launchable }

                itemsIndexed(launchable, key = { _, app -> app.packageName }) { index, app ->
                    AppItem(
                        label = app.label,
                        packageName = app.packageName,
                        icon = app.icon,
                        checked = app.packageName in selected,
                        setFocus = index == 0,
                        onClick = { onToggle(app.packageName) },
                    )
                }

                if (rest.isNotEmpty()) {
                    item(key = "rest-header") {
                        Text(
                            text = stringResource(R.string.picker_other_apps).uppercase(),
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.6.sp,
                            modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 4.dp),
                        )
                    }
                    itemsIndexed(rest, key = { _, app -> app.packageName }) { index, app ->
                        AppItem(
                            label = app.label,
                            packageName = app.packageName,
                            icon = app.icon,
                            checked = app.packageName in selected,
                            setFocus = launchable.isEmpty() && index == 0,
                            onClick = { onToggle(app.packageName) },
                        )
                    }
                }
            }
        }
    }
}

private fun hasActivities(pm: android.content.pm.PackageManager, pkg: String): Boolean =
    runCatching {
        pm.getPackageInfo(pkg, android.content.pm.PackageManager.GET_ACTIVITIES)
            .activities
            ?.isNotEmpty() == true
    }.getOrDefault(false)

private suspend fun loadInstalledApps(context: Context): List<InstalledApp> =
    withContext(Dispatchers.IO) {
        val pm = context.packageManager

        val launchable = InputMonitorService.LAUNCHER_CATEGORIES.flatMapTo(mutableSetOf()) { category ->
            pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(category), 0)
                .map { it.activityInfo.packageName }
        }

        pm.getInstalledApplications(0)
            .asSequence()
            .filter { it.packageName != context.packageName }
            .filter { it.packageName in launchable || hasActivities(pm, it.packageName) }
            .map { info ->
                InstalledApp(
                    packageName = info.packageName,
                    label = runCatching { pm.getApplicationLabel(info).toString() }
                        .getOrDefault(info.packageName),
                    icon = runCatching {
                        pm.getApplicationIcon(info).toBitmap(96, 96).asImageBitmap()
                    }.getOrNull(),
                    launchable = info.packageName in launchable,
                )
            }
            .sortedWith(compareBy({ it.label.lowercase() }, { it.packageName }))
            .toList()
    }
