package com.openlauncher.app.ui.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openlauncher.app.ui.theme.LocalHelpTextColor
import com.openlauncher.app.util.LocationData

/**
 * Altitude-only gauge.
 *
 * The previous roll/pitch "vehicle attitude" + ZERO/CAL design was removed: on a
 * fixed head-unit mount the accelerometer only reports the static mounting tilt
 * (near 0° and unchanging), so it read as dead and wasted the tile. GPS altitude
 * is the meaningful readout, so this widget is now just a clean altitude display.
 *
 * The roll/pitch/zero parameters are kept in the signature (unused) so existing
 * call sites compile unchanged; they can be stripped in a later cleanup.
 */
@Composable
fun AltimeterWidget(
    location: LocationData?,
    isMetric: Boolean,
    accent: Color,
    isDayMode: Boolean = false,
    customContentColor: Color? = null,
    rollOffset: Float = 0f,
    pitchOffset: Float = 0f,
    onZero: ((Float, Float) -> Unit)? = null,
    onResetZero: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isLight = isDayMode || (customContentColor != null &&
        (0.299f * customContentColor.red + 0.587f * customContentColor.green + 0.114f * customContentColor.blue) < 0.5f)
    val labelColor = LocalHelpTextColor.current
        ?: customContentColor?.copy(alpha = 0.55f)
        ?: if (isLight) Color(0xFF888888) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
    val valueColor = customContentColor ?: if (isLight) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground

    val altVal = location?.let {
        if (isMetric) it.altitude else it.altitude * 3.28084
    }
    val unit = if (isMetric) "m" else "ft"
    val altNum = altVal?.let { String.format(java.util.Locale.US, "%,.0f", it) } ?: "—"
    val gpsLabel = if (location != null) "GPS" else "NO GPS"

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        val compact = maxHeight < 140.dp || maxWidth < 200.dp
        val numberSize = if (compact) 20.sp else 28.sp

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "ALTITUDE",
                color = labelColor,
                fontSize = 8.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = altNum,
                    color = valueColor,
                    fontSize = numberSize,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = unit,
                    color = labelColor,
                    fontSize = if (compact) 10.sp else 12.sp,
                    modifier = Modifier.padding(bottom = if (compact) 2.dp else 3.dp)
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = gpsLabel,
                color = labelColor.copy(alpha = 0.85f),
                fontSize = 7.sp,
                letterSpacing = 1.sp,
                maxLines = 1
            )
        }
    }
}
