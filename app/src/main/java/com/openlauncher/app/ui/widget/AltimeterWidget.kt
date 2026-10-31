package com.openlauncher.app.ui.widget

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openlauncher.app.util.LocationData
import kotlin.math.*

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
    val isLight = isDayMode || (customContentColor != null && (0.299f * customContentColor.red + 0.587f * customContentColor.green + 0.114f * customContentColor.blue) < 0.5f)
    val iconTint = customContentColor?.copy(alpha = 0.85f) ?: if (isLight) Color(0xFF333333) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
    val labelColor = customContentColor?.copy(alpha = 0.50f) ?: if (isLight) Color(0xFF888888) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
    val valueColor = customContentColor ?: if (isLight) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground
    val buttonBg = customContentColor?.let { if (isLight) Color(0x1F000000) else Color(0x1FFFFFFF) } ?: if (isLight) Color(0xFFE5E5E5) else Color(0xFF1E1E1E)
    val buttonBorder = customContentColor?.copy(alpha = 0.25f) ?: if (isLight) Color(0xFFCCCCCC) else Color(0xFF333333)

    val context = LocalContext.current

    // Accelerometer sensor state
    var rollDeg  by remember { mutableFloatStateOf(0f) }
    var pitchDeg by remember { mutableFloatStateOf(0f) }
    val gravBuf  = remember { FloatArray(3) { 0f } }
    var hasSensor by remember { mutableStateOf(false) }

    // GPS fallback state for sensor-less devices
    var lastLoc by remember { mutableStateOf<LocationData?>(null) }
    var gpsPitch by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                hasSensor = true
                val alpha = 0.12f
                gravBuf[0] = alpha * event.values[0] + (1f - alpha) * gravBuf[0]
                gravBuf[1] = alpha * event.values[1] + (1f - alpha) * gravBuf[1]
                gravBuf[2] = alpha * event.values[2] + (1f - alpha) * gravBuf[2]

                // Screen orientation aware mapping
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as? android.view.WindowManager
                val rotation = wm?.defaultDisplay?.rotation ?: android.view.Surface.ROTATION_0

                val sx: Float
                val sy: Float
                val sz: Float = gravBuf[2]

                when (rotation) {
                    android.view.Surface.ROTATION_90 -> {
                        sx = -gravBuf[1]
                        sy = gravBuf[0]
                    }
                    android.view.Surface.ROTATION_270 -> {
                        sx = gravBuf[1]
                        sy = -gravBuf[0]
                    }
                    android.view.Surface.ROTATION_180 -> {
                        sx = -gravBuf[0]
                        sy = -gravBuf[1]
                    }
                    else -> {
                        sx = gravBuf[0]
                        sy = gravBuf[1]
                    }
                }

                // In car dashboard mount:
                // sx is lateral (left/right roll)
                // sy is vertical (downward gravity)
                // sz is longitudinal (front/back pitch)
                rollDeg = Math.toDegrees(
                    atan2(sx.toDouble(), sy.toDouble().coerceAtLeast(0.01))
                ).toFloat()

                pitchDeg = Math.toDegrees(
                    atan2(sz.toDouble(), sqrt(sx.toDouble().pow(2) + sy.toDouble().pow(2)).coerceAtLeast(0.01))
                ).toFloat()
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { sm.unregisterListener(listener) }
    }

    // Mathematical GPS incline (pitch) calculation when offline/no accelerometer
    LaunchedEffect(location) {
        val newLoc = location ?: return@LaunchedEffect
        val oldLoc = lastLoc
        if (oldLoc != null && (oldLoc.latitude != newLoc.latitude || oldLoc.longitude != newLoc.longitude)) {
            val dist = FloatArray(1)
            runCatching {
                android.location.Location.distanceBetween(
                    oldLoc.latitude, oldLoc.longitude,
                    newLoc.latitude, newLoc.longitude,
                    dist
                )
                val distance = dist[0]
                val heightDiff = newLoc.altitude - oldLoc.altitude
                if (distance > 3f) {
                    val pitchRad = atan2(heightDiff, distance.toDouble())
                    val newPitchDeg = Math.toDegrees(pitchRad).toFloat().coerceIn(-45f, 45f)
                    val alpha = 0.15f
                    gpsPitch = alpha * newPitchDeg + (1f - alpha) * gpsPitch
                    lastLoc = newLoc
                }
            }
        } else {
            lastLoc = newLoc
        }
    }

    val rawRoll = if (hasSensor) rollDeg else 0f
    val rawPitch = if (hasSensor) pitchDeg else gpsPitch

    val effectiveRoll = rawRoll - rollOffset
    val effectivePitch = rawPitch - pitchOffset
    val isZeroed = abs(rollOffset) > 0.05f || abs(pitchOffset) > 0.05f

    val altVal = if (location != null) {
        if (isMetric) location.altitude else location.altitude * 3.28084
    } else null
    val unit = if (isMetric) "m" else "ft"
    val altText = altVal?.let { String.format(java.util.Locale.US, "%,.0f %s", it, unit) } ?: "—"

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        val isCompactH = maxHeight < 140.dp
        val isNarrow = maxWidth < 220.dp
        val carIconSize = if (isCompactH || isNarrow) 42.dp else 56.dp

        // ── Top: Prominent Altitude Header ────────────────────────────────────
        if (isCompactH) {
            // In compact 1x1 mode: Place ALT and altText cleanly at TopEnd to avoid clashing with the "ALTIMETER" header
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "ALT",
                    color = labelColor,
                    fontSize = 7.5.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = altText,
                    color = valueColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        } else {
            // Taller mode: Altitude centered cleanly below widget header
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ALTITUDE",
                    color = labelColor,
                    fontSize = 7.5.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = altText,
                    color = valueColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }

        // ── Center: Clean Vehicle Attitude Icon ───────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = if (isCompactH) 2.dp else 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier
                    .size(carIconSize)
                    .graphicsLayer { rotationZ = effectiveRoll }
            )
        }

        // ── Bottom: ROLL, ZERO / CAL, PITCH ──────────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = "ROLL",
                    color = labelColor,
                    fontSize = 7.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = String.format(java.util.Locale.US, "%+.1f°", effectiveRoll),
                    color = accent,
                    fontSize = if (isCompactH) 10.sp else 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Zero / Tare calibration button in the middle
            if (onZero != null) {
                if (!isZeroed) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(buttonBg)
                            .border(1.dp, buttonBorder, RoundedCornerShape(3.dp))
                            .clickable { onZero(rawRoll, rawPitch) }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ZERO",
                            color = accent,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(accent.copy(alpha = 0.15f))
                            .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                            .clickable { onResetZero?.invoke() }
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "CAL",
                            color = accent,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Reset Zero",
                            tint = accent,
                            modifier = Modifier.size(9.dp)
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "PITCH",
                    color = labelColor,
                    fontSize = 7.sp,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.End
                )
                Text(
                    text = String.format(java.util.Locale.US, "%+.1f°", effectivePitch),
                    color = accent,
                    fontSize = if (isCompactH) 10.sp else 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}
