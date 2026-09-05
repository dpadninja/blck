package com.dpadninja.blck.ui.core

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.dpadninja.blck.R


@Composable
private fun ItemCard(
    modifier: Modifier = Modifier,
    setFocus: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable RowScope.(focused: Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    var focusRequested by remember { mutableStateOf(!setFocus) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(enabled) {
        if (enabled && !focusRequested) {
            runCatching { focusRequester.requestFocus() }
            focusRequested = true
        }
    }

    Card(
        shape = ItemCornerShape,
        enabled = enabled,
        colors = CardDefaults.cardColors(
            containerColor = if (focused) ItemFocused else ItemBackground,
            disabledContainerColor = ItemBackground,
        ),
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused },
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            content(focused)
        }
    }
}

@Composable
private fun ItemLabel(
    caption: String,
    description: String?,
    focused: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(modifier = modifier) {
        Text(
            text = caption,
            color = when {
                !enabled -> TextDisabled
                focused -> TextOnFocus
                else -> TextPrimary
            },
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        if (description != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                color = when {
                    !enabled -> TextDisabled
                    focused -> TextOnFocusSecondary
                    else -> TextSecondary
                },
                fontSize = 11.sp,
                lineHeight = 1.3.em,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
fun SettingItem(
    caption: String,
    description: String? = null,
    value: String? = null,
    setFocus: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    ItemCard(setFocus = setFocus, enabled = enabled, onClick = onClick) { focused ->
        ItemLabel(caption, description, focused, Modifier.weight(1f, fill = false), enabled)
//        Spacer(Modifier.width(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value != null) {
                Text(
                    text = value,
                    color = when {
                        !enabled -> TextDisabled
                        focused -> TextOnFocus
                        else -> TextPrimary
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
fun SettingValueItem(
    caption: String,
    value: String,
    description: String? = null,
    valueEmphasis: Boolean = false,
    setFocus: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    ItemCard(setFocus = setFocus, enabled = enabled, onClick = onClick) { focused ->
        ItemLabel(caption, description, focused, Modifier.weight(1f, fill = false), enabled)
        Text(
            text = value,
            color = when {
                !enabled -> TextDisabled
                focused -> TextOnFocus
                else -> TextPrimary
            },
            fontSize = 14.sp,
            fontWeight = if (valueEmphasis) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
fun SettingPermissionItem(
    caption: String,
    value: Boolean,
    description: String? = null,
    valueEmphasis: Boolean = false,
    setFocus: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    ItemCard(setFocus = setFocus, enabled = enabled, onClick = onClick) { focused ->
        ItemLabel(caption, description, focused, Modifier.weight(1f, fill = false), enabled)
        Text(
            text = when {
                value -> stringResource(R.string.permission_granted)
                else -> stringResource(R.string.permission_grant)},
            color = when {
                value -> TextDisabled
                else -> WarningColor
            },
            fontSize = 14.sp,
            fontWeight = if (valueEmphasis) FontWeight.Bold else FontWeight.Medium,
        )
    }
}


@Composable
fun SettingSwitchItem(
    caption: String,
    description: String? = null,
    switchState: Boolean,
    setFocus: Boolean = false,
    enabled: Boolean = true,
    onClick: (Boolean) -> Unit,
) {
    ItemCard(setFocus = setFocus, enabled = enabled, onClick = { onClick(!switchState) }) { focused ->
        ItemLabel(caption, description, focused, Modifier.weight(1f, fill = false), enabled)
        Switch(
            modifier = Modifier.scale(0.7f).height(0.dp),
            checked = switchState,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = TextColor1,
                checkedThumbColor = if (focused) AccentColor900 else Graphite,
                checkedBorderColor = Color.Transparent,
                uncheckedTrackColor = if (focused) AccentColor600 else Graphite,
                uncheckedThumbColor = if (focused) TextColor1 else TextColor2,
                uncheckedBorderColor = if (focused) TextColor1 else TextColor2,
                disabledCheckedTrackColor = TextColor2,
                disabledCheckedThumbColor = Graphite,
                disabledCheckedBorderColor = Color.Transparent,
                disabledUncheckedTrackColor = Graphite,
                disabledUncheckedThumbColor = TextColor2,
                disabledUncheckedBorderColor = TextColor2,
            ),
        )
    }
}

@Composable
fun AppItem(
    label: String,
    packageName: String,
    icon: ImageBitmap?,
    checked: Boolean,
    setFocus: Boolean = false,
    onClick: () -> Unit,
) {
    ItemCard(setFocus = setFocus, onClick = onClick) { focused ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false),
        ) {
            Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                if (icon != null) {
                    Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            ItemLabel(label, packageName, focused)
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = if (checked) "✓" else "",
            color = if (focused) TextOnFocus else TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
