package com.openlauncher.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.ui.graphics.vector.ImageVector
import com.openlauncher.app.data.FloatingActionType

fun floatingActionIcon(type: FloatingActionType): ImageVector = when (type) {
    FloatingActionType.HOME -> Icons.Default.Home
    FloatingActionType.BACK -> Icons.AutoMirrored.Filled.ArrowBack
    FloatingActionType.VOL_UP -> Icons.Default.VolumeUp
    FloatingActionType.VOL_DOWN -> Icons.Default.VolumeDown
    FloatingActionType.MUTE -> Icons.Default.VolumeOff
    FloatingActionType.BLACKOUT -> Icons.Default.DarkMode
    FloatingActionType.DAY_NIGHT -> Icons.Default.LightMode
    FloatingActionType.APP -> Icons.Default.Apps
}

fun floatingActionLabel(type: FloatingActionType): String = when (type) {
    FloatingActionType.HOME -> "Home"
    FloatingActionType.BACK -> "Back"
    FloatingActionType.VOL_UP -> "Volume Up"
    FloatingActionType.VOL_DOWN -> "Volume Down"
    FloatingActionType.MUTE -> "Mute"
    FloatingActionType.BLACKOUT -> "Screen Blackout"
    FloatingActionType.DAY_NIGHT -> "Day / Night"
    FloatingActionType.APP -> "Launch App"
}
