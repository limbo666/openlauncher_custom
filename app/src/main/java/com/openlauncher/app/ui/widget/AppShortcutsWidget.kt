package com.openlauncher.app.ui.widget

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.openlauncher.app.model.AppInfo

/**
 * App Shortcuts Widget: Displays configurable number of app icons targeting apps on the device.
 * Smartly arranges icons horizontally, vertically, or in a 2x2 grid based on widget size and count.
 */
@Composable
fun AppShortcutsWidget(
    packages: List<String>,
    installedApps: List<AppInfo>,
    accent: Color,
    isDayMode: Boolean = false,
    customContentColor: Color? = null,
    shortcutCount: Int = 4,
    onLaunchApp: (String) -> Unit,
    onPickAppForSlot: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isLight = isDayMode || (customContentColor != null && (0.299f * customContentColor.red + 0.587f * customContentColor.green + 0.114f * customContentColor.blue) < 0.5f)
    val contentColor = customContentColor ?: if (isLight) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground
    val subColor = customContentColor?.copy(alpha = 0.7f) ?: if (isLight) Color(0xFF666666) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
    val slotBg = customContentColor?.let { if (isLight) Color(0x1F000000) else Color(0x22FFFFFF) } ?: if (isLight) Color(0xFFEFEFEF).copy(alpha = 0.8f) else Color(0xFF141414).copy(alpha = 0.65f)
    val slotBorder = customContentColor?.copy(alpha = 0.25f) ?: if (isLight) Color(0xFFD0D0D0) else Color(0xFF282828)

    val count = shortcutCount.coerceIn(1, 4)
    val slots = remember(packages, count) {
        val list = packages.take(count).toMutableList()
        while (list.size < count) list.add("")
        list
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        val isWide = maxWidth > maxHeight * 1.6f
        val isTall = maxHeight > maxWidth * 1.4f

        when (count) {
            1 -> {
                AppShortcutSlot(
                    index = 0,
                    packageName = slots[0],
                    appInfo = installedApps.find { it.packageName == slots[0] },
                    accent = accent,
                    contentColor = contentColor,
                    subColor = subColor,
                    slotBg = slotBg,
                    slotBorder = slotBorder,
                    onLaunch = onLaunchApp,
                    onConfigure = onPickAppForSlot,
                    modifier = Modifier.fillMaxSize()
                )
            }
            2 -> {
                if (isTall) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        for (i in 0 until 2) {
                            AppShortcutSlot(
                                index = i,
                                packageName = slots[i],
                                appInfo = installedApps.find { it.packageName == slots[i] },
                                accent = accent,
                                contentColor = contentColor,
                                subColor = subColor,
                                slotBg = slotBg,
                                slotBorder = slotBorder,
                                onLaunch = onLaunchApp,
                                onConfigure = onPickAppForSlot,
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until 2) {
                            AppShortcutSlot(
                                index = i,
                                packageName = slots[i],
                                appInfo = installedApps.find { it.packageName == slots[i] },
                                accent = accent,
                                contentColor = contentColor,
                                subColor = subColor,
                                slotBg = slotBg,
                                slotBorder = slotBorder,
                                onLaunch = onLaunchApp,
                                onConfigure = onPickAppForSlot,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                    }
                }
            }
            3 -> {
                if (isTall) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        for (i in 0 until 3) {
                            AppShortcutSlot(
                                index = i,
                                packageName = slots[i],
                                appInfo = installedApps.find { it.packageName == slots[i] },
                                accent = accent,
                                contentColor = contentColor,
                                subColor = subColor,
                                slotBg = slotBg,
                                slotBorder = slotBorder,
                                onLaunch = onLaunchApp,
                                onConfigure = onPickAppForSlot,
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until 3) {
                            AppShortcutSlot(
                                index = i,
                                packageName = slots[i],
                                appInfo = installedApps.find { it.packageName == slots[i] },
                                accent = accent,
                                contentColor = contentColor,
                                subColor = subColor,
                                slotBg = slotBg,
                                slotBorder = slotBorder,
                                onLaunch = onLaunchApp,
                                onConfigure = onPickAppForSlot,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                    }
                }
            }
            else -> { // count == 4
                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until 4) {
                            AppShortcutSlot(
                                index = i,
                                packageName = slots[i],
                                appInfo = installedApps.find { it.packageName == slots[i] },
                                accent = accent,
                                contentColor = contentColor,
                                subColor = subColor,
                                slotBg = slotBg,
                                slotBorder = slotBorder,
                                onLaunch = onLaunchApp,
                                onConfigure = onPickAppForSlot,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                    }
                } else if (isTall) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        for (i in 0 until 4) {
                            AppShortcutSlot(
                                index = i,
                                packageName = slots[i],
                                appInfo = installedApps.find { it.packageName == slots[i] },
                                accent = accent,
                                contentColor = contentColor,
                                subColor = subColor,
                                slotBg = slotBg,
                                slotBorder = slotBorder,
                                onLaunch = onLaunchApp,
                                onConfigure = onPickAppForSlot,
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            )
                        }
                    }
                } else {
                    // 2x2 grid
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Row(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (i in 0 until 2) {
                                AppShortcutSlot(
                                    index = i,
                                    packageName = slots[i],
                                    appInfo = installedApps.find { it.packageName == slots[i] },
                                    accent = accent,
                                    contentColor = contentColor,
                                    subColor = subColor,
                                    slotBg = slotBg,
                                    slotBorder = slotBorder,
                                    onLaunch = onLaunchApp,
                                    onConfigure = onPickAppForSlot,
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (i in 2 until 4) {
                                AppShortcutSlot(
                                    index = i,
                                    packageName = slots[i],
                                    appInfo = installedApps.find { it.packageName == slots[i] },
                                    accent = accent,
                                    contentColor = contentColor,
                                    subColor = subColor,
                                    slotBg = slotBg,
                                    slotBorder = slotBorder,
                                    onLaunch = onLaunchApp,
                                    onConfigure = onPickAppForSlot,
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppShortcutSlot(
    index: Int,
    packageName: String,
    appInfo: AppInfo?,
    accent: Color,
    contentColor: Color,
    subColor: Color,
    slotBg: Color,
    slotBorder: Color,
    onLaunch: (String) -> Unit,
    onConfigure: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAssigned = packageName.isNotBlank() && appInfo != null

    BoxWithConstraints(
        modifier = modifier.padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        val iconSize = (minOf(maxWidth, maxHeight) * 0.55f).coerceIn(24.dp, 60.dp)
        val showLabel = maxHeight > 65.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(if (isAssigned) Color.Transparent else slotBg)
                .border(
                    width = if (isAssigned) 0.dp else 1.dp,
                    color = if (isAssigned) Color.Transparent else slotBorder,
                    shape = RoundedCornerShape(8.dp)
                )
                .combinedClickable(
                    onClick = {
                        if (isAssigned) onLaunch(packageName) else onConfigure(index)
                    },
                    onLongClick = {
                        onConfigure(index)
                    }
                )
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isAssigned) {
                val bmp = remember(appInfo.icon) {
                    runCatching { appInfo.icon.toBitmap(64, 64) }.getOrNull()
                }
                if (bmp != null) {
                    Icon(
                        painter = BitmapPainter(bmp.asImageBitmap()),
                        contentDescription = appInfo.appName,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(iconSize)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = appInfo.appName,
                        tint = contentColor,
                        modifier = Modifier.size(iconSize)
                    )
                }

                if (showLabel) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = appInfo.appName,
                        color = contentColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Empty slot with + prompt
                Box(
                    modifier = Modifier
                        .size(iconSize * 0.85f)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Assign App",
                        tint = accent,
                        modifier = Modifier.size(iconSize * 0.5f)
                    )
                }
                if (showLabel) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "ADD APP",
                        color = subColor,
                        fontSize = 7.sp,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
