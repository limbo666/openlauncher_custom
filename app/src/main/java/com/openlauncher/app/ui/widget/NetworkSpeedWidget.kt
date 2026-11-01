package com.openlauncher.app.ui.widget

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openlauncher.app.ui.theme.LocalHelpTextColor
import com.openlauncher.app.util.NetworkSpeedState
import com.openlauncher.app.util.formatBitRate

@Composable
fun NetworkSpeedWidget(
    speed: NetworkSpeedState,
    accent: Color,
    isDayMode: Boolean = false,
    customContentColor: Color? = null,
    modifier: Modifier = Modifier
) {
    val isLight = isDayMode || (customContentColor != null &&
        (0.299f * customContentColor.red + 0.587f * customContentColor.green + 0.114f * customContentColor.blue) < 0.5f)
    val contentColor = customContentColor ?: if (isLight) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground

    BoxWithConstraints(
        modifier = modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        val wide = maxWidth > maxHeight * 1.5f
        if (wide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SpeedCell(
                    icon = Icons.Default.Download,
                    label = "DOWN",
                    value = formatBitRate(speed.rxBytesPerSec),
                    accent = accent,
                    contentColor = contentColor
                )
                SpeedCell(
                    icon = Icons.Default.Upload,
                    label = "UP",
                    value = formatBitRate(speed.txBytesPerSec),
                    accent = accent,
                    contentColor = contentColor
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                SpeedCell(
                    icon = Icons.Default.Download,
                    label = "DOWN",
                    value = formatBitRate(speed.rxBytesPerSec),
                    accent = accent,
                    contentColor = contentColor
                )
                Spacer(Modifier.height(4.dp))
                SpeedCell(
                    icon = Icons.Default.Upload,
                    label = "UP",
                    value = formatBitRate(speed.txBytesPerSec),
                    accent = accent,
                    contentColor = contentColor
                )
            }
        }
    }
}

@Composable
private fun SpeedCell(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
    contentColor: Color
) {
    val subColor = LocalHelpTextColor.current ?: contentColor.copy(alpha = 0.55f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = accent,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Column {
            Text(
                text = label,
                color = subColor,
                fontSize = 8.sp,
                letterSpacing = 1.5.sp,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = value,
                color = contentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
