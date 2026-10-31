package com.openlauncher.app.ui.widget

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openlauncher.app.data.DayNightMode
import com.openlauncher.app.data.defaultQuickActionButtons
import com.openlauncher.app.util.FileLog
import com.openlauncher.app.util.MuteState
import kotlinx.coroutines.delay

private data class QuickActionItem(
    val id: String,
    val label: String,
    val shortLabel: String,
    val status: String,
    val isActive: Boolean,
    val activeColor: Color,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun QuickActionsWidget(
    accent: Color,
    isDayMode: Boolean = false,
    dayNightMode: DayNightMode = DayNightMode.DARK,
    customContentColor: Color? = null,
    enabledActions: List<String> = emptyList(),
    onToggleScreenBlanked: () -> Unit,
    onToggleDayNight: (() -> Unit)? = null,
    onOpenConfig: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val audioManager = remember { runCatching { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }.getOrNull() }
    val wifiManager = remember { runCatching { context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager }.getOrNull() }
    val btAdapter = remember { runCatching { BluetoothAdapter.getDefaultAdapter() }.getOrNull() }

    var isBtOn by remember {
        mutableStateOf(
            runCatching { btAdapter?.isEnabled == true }.getOrDefault(false)
        )
    }
    var isWifiOn by remember {
        mutableStateOf(
            runCatching { wifiManager?.isWifiEnabled == true }.getOrDefault(false)
        )
    }
    var isMuted by remember {
        mutableStateOf(
            runCatching { audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) == 0 }.getOrDefault(false)
        )
    }

    // Periodic state check to catch changes from notification drawer or settings
    LaunchedEffect(Unit) {
        while (true) {
            delay(2000L)
            isBtOn = runCatching { btAdapter?.isEnabled == true }.getOrDefault(false)
            isWifiOn = runCatching { wifiManager?.isWifiEnabled == true }.getOrDefault(false)
            isMuted = runCatching { audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) == 0 }.getOrDefault(false)
        }
    }

    val isLight = isDayMode || (customContentColor != null && (0.299f * customContentColor.red + 0.587f * customContentColor.green + 0.114f * customContentColor.blue) < 0.5f)
    val contentColor = customContentColor ?: if (isLight) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground
    val subColor     = customContentColor?.copy(alpha = 0.6f) ?: if (isLight) Color(0xFF666666) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
    val buttonBg     = if (customContentColor != null) {
        if (isLight) Color(0x1F000000) else Color(0x22FFFFFF)
    } else {
        if (isLight) Color(0xFFE8E8E8) else Color(0xFF141414)
    }
    val buttonBorder = customContentColor?.copy(alpha = 0.25f) ?: if (isLight) Color(0xFFCCCCCC) else Color(0xFF222222)

    val toggleBluetooth: () -> Unit = {
        try {
            if (btAdapter != null && runCatching { btAdapter.isEnabled }.getOrDefault(false)) {
                @Suppress("DEPRECATION")
                val success = runCatching { btAdapter.disable() }.getOrDefault(false)
                if (success) {
                    isBtOn = false
                } else {
                    context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
            } else if (btAdapter != null) {
                @Suppress("DEPRECATION")
                val success = runCatching { btAdapter.enable() }.getOrDefault(false)
                if (success) {
                    isBtOn = true
                } else {
                    context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
            } else {
                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        } catch (_: Throwable) {
            runCatching {
                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        }
    }

    val toggleWifi: () -> Unit = {
        try {
            if (wifiManager != null) {
                val currentState = runCatching { wifiManager.isWifiEnabled }.getOrDefault(false)
                val newState = !currentState
                @Suppress("DEPRECATION")
                val changed = runCatching { wifiManager.isWifiEnabled = newState; true }.getOrDefault(false)
                if (changed && runCatching { wifiManager.isWifiEnabled }.getOrDefault(currentState) == newState) {
                    isWifiOn = newState
                } else {
                    context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
            } else {
                context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        } catch (_: Throwable) {
            runCatching {
                context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        }
    }

    val toggleMute: () -> Unit = {
        runCatching {
            val am = audioManager ?: return@runCatching
            val curr = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            if (curr > 0) {
                MuteState.preMuteVolume = curr
                am.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                isMuted = true
                FileLog.log("Mute", "quickaction mute from=$curr")
            } else {
                val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                val restore = if (MuteState.preMuteVolume > 0) MuteState.preMuteVolume else (max / 3).coerceAtLeast(1)
                am.setStreamVolume(AudioManager.STREAM_MUSIC, restore, 0)
                isMuted = false
                FileLog.log("Mute", "quickaction unmute to=$restore prev=${MuteState.preMuteVolume}")
                MuteState.preMuteVolume = -1
            }
        }
    }

    val openSettings: () -> Unit = {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    // Build the resolved list of configured action items
    val actions = remember(enabledActions, isBtOn, isWifiOn, isMuted, isDayMode, dayNightMode, accent) {
        val sanitized = runCatching {
            enabledActions.mapNotNull { item ->
                item.toString().trim().takeIf { it.isNotBlank() }
            }
        }.getOrNull() ?: emptyList()

        val activeKeys = if (sanitized.isNotEmpty()) sanitized else defaultQuickActionButtons()
        activeKeys.mapNotNull { rawId ->
            when (rawId.uppercase().trim()) {
                "BLUETOOTH" -> QuickActionItem(
                    id = "BLUETOOTH",
                    label = "BLUETOOTH",
                    shortLabel = "BT",
                    status = if (isBtOn) "ON" else "OFF",
                    isActive = isBtOn,
                    activeColor = accent,
                    icon = Icons.Default.Bluetooth,
                    onClick = toggleBluetooth
                )
                "WIFI" -> QuickActionItem(
                    id = "WIFI",
                    label = "WI-FI",
                    shortLabel = "WIFI",
                    status = if (isWifiOn) "ON" else "OFF",
                    isActive = isWifiOn,
                    activeColor = accent,
                    icon = Icons.Default.Wifi,
                    onClick = toggleWifi
                )
                "BLACKOUT" -> QuickActionItem(
                    id = "BLACKOUT",
                    label = "SCREEN OFF",
                    shortLabel = "SCREEN",
                    status = "DIM",
                    isActive = false,
                    activeColor = accent,
                    icon = Icons.Default.NightsStay,
                    onClick = onToggleScreenBlanked
                )
                "MUTE" -> QuickActionItem(
                    id = "MUTE",
                    label = if (isMuted) "UNMUTE" else "AUDIO",
                    shortLabel = if (isMuted) "UNMUTE" else "MUTE",
                    status = if (isMuted) "MUTED" else "ACTIVE",
                    isActive = isMuted,
                    activeColor = Color(0xFFFF4444),
                    icon = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    onClick = toggleMute
                )
                "SETTINGS" -> QuickActionItem(
                    id = "SETTINGS",
                    label = "SETTINGS",
                    shortLabel = "SYS",
                    status = "SYSTEM",
                    isActive = false,
                    activeColor = accent,
                    icon = Icons.Default.Settings,
                    onClick = openSettings
                )
                "DAY_NIGHT" -> QuickActionItem(
                    id = "DAY_NIGHT",
                    label = "THEME",
                    shortLabel = when (dayNightMode) {
                        DayNightMode.LIGHT  -> "LIGHT"
                        DayNightMode.DARK   -> "DARK"
                        DayNightMode.CUSTOM -> "CUSTOM"
                        DayNightMode.AUTO   -> "AUTO"
                        DayNightMode.SYSTEM -> "SYSTEM"
                    },
                    status = when (dayNightMode) {
                        DayNightMode.LIGHT  -> "LIGHT"
                        DayNightMode.DARK   -> "DARK"
                        DayNightMode.CUSTOM -> "CUSTOM"
                        DayNightMode.AUTO   -> "AUTO"
                        DayNightMode.SYSTEM -> "SYSTEM"
                    },
                    isActive = isDayMode,
                    activeColor = accent,
                    icon = when (dayNightMode) {
                        DayNightMode.LIGHT  -> Icons.Default.LightMode
                        DayNightMode.DARK   -> Icons.Default.DarkMode
                        DayNightMode.CUSTOM -> Icons.Default.Palette
                        DayNightMode.AUTO   -> Icons.Default.BrightnessAuto
                        DayNightMode.SYSTEM -> Icons.Default.PhoneAndroid
                    },
                    onClick = { onToggleDayNight?.invoke() }
                )
                else -> null
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        val totalHeight = maxHeight
        val totalWidth = maxWidth
        val count = actions.size

        // Available height below the widget title header
        val availHeight = (totalHeight - 18.dp).coerceAtLeast(20.dp)

        // Decide single-row vs 2-row layout:
        // Use single-row if height is constrained (< 75dp), or if there are <= 2 items,
        // or if width is wide enough that each button gets plenty of horizontal room
        val useSingleRow = availHeight < 75.dp || count <= 2 || (totalWidth / count.coerceAtLeast(1) >= 85.dp && count <= 3)

        if (useSingleRow) {
            // Proportional single horizontal row (vertically centered, NEVER stretched full height)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp, bottom = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 36.dp, max = 52.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    actions.forEach { item ->
                        QuickActionTile(
                            item = item,
                            contentColor = contentColor,
                            subColor = subColor,
                            bgColor = buttonBg,
                            borderColor = buttonBorder,
                            isLight = isLight,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        } else {
            // Smart 2-row adaptive automotive grid
            // Distribute items cleanly across 2 rows
            val row1Count = when (count) {
                3 -> 2
                4 -> 2  // 2x2 grid (previous favorite)
                5 -> 3  // 3 on top, 2 on bottom
                6 -> 3  // 3 on top, 3 on bottom
                else -> (count + 1) / 2
            }
            val row1Items = actions.take(row1Count)
            val row2Items = actions.drop(row1Count)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 18.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Row 1
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row1Items.forEach { item ->
                        QuickActionTile(
                            item = item,
                            contentColor = contentColor,
                            subColor = subColor,
                            bgColor = buttonBg,
                            borderColor = buttonBorder,
                            isLight = isLight,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }

                // Row 2
                if (row2Items.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row2Items.forEach { item ->
                            QuickActionTile(
                                item = item,
                                contentColor = contentColor,
                                subColor = subColor,
                                bgColor = buttonBg,
                                borderColor = buttonBorder,
                                isLight = isLight,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }
                }
            }
        }

        // Mini configure button in widget header
        if (onOpenConfig != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 4.dp, top = 2.dp)
                    .size(20.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .clickable(onClick = onOpenConfig),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Configure Actions",
                    tint = subColor,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    item: QuickActionItem,
    contentColor: Color,
    subColor: Color,
    bgColor: Color,
    borderColor: Color,
    isLight: Boolean,
    modifier: Modifier = Modifier
) {
    val inactiveLedColor = if (isLight) Color(0xFF888888) else Color(0xFF444444)
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(if (item.isActive) item.activeColor.copy(alpha = if (isLight) 0.22f else 0.18f) else bgColor)
            .border(1.dp, if (item.isActive) item.activeColor.copy(alpha = 0.85f) else borderColor, RoundedCornerShape(5.dp))
            .clickable(onClick = item.onClick)
    ) {
        val tileHeight = maxHeight
        val tileWidth = maxWidth

        if (tileHeight < 48.dp) {
            // Horizontal compact tile layout (icon on left, label & status center, LED dot right)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = if (item.isActive) item.activeColor else contentColor,
                    modifier = Modifier.size(16.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (tileWidth < 68.dp) item.shortLabel else item.label,
                        color = contentColor,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (tileHeight >= 34.dp) {
                        Text(
                            text = item.status,
                            color = if (item.isActive) item.activeColor else subColor,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(if (item.isActive) item.activeColor else inactiveLedColor)
                )
            }
        } else {
            // Stacked automotive card tile layout (Icon top-left, LED dot top-right, label & status bottom)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (item.isActive) item.activeColor else contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (item.isActive) item.activeColor else inactiveLedColor)
                    )
                }

                Column {
                    val tileLabel = if (tileWidth < 74.dp) item.shortLabel else item.label
                    Text(
                        text = tileLabel,
                        color = contentColor,
                        fontSize = if (tileWidth < 74.dp) 8.sp else 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.status,
                        color = if (item.isActive) item.activeColor else subColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
