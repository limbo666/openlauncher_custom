package com.openlauncher.app.ui.widget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openlauncher.app.util.LocationData
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SpeedometerWidget(
    location: LocationData?,
    isMetric: Boolean,
    accent: Color,
    isDayMode: Boolean = false,
    digitalOnly: Boolean = false,
    customContentColor: Color? = null,
    modifier: Modifier = Modifier
) {
    val maxSpeed     = if (isMetric) 200f else 124f
    val speedDisplay = ((location?.speedMps ?: 0f) * if (isMetric) 3.6f else 2.237f).coerceAtLeast(0f)
    val unitLabel    = if (isMetric) "KM/H" else "MPH"
    val trackAlpha   = if (isDayMode) 0.18f else 0.07f
    val tickAlphaMaj = if (isDayMode) 0.50f else 0.28f
    val tickAlphaMin = if (isDayMode) 0.25f else 0.13f

    val contentColor = customContentColor ?: if (isDayMode) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground
    val subAlpha     = if (isDayMode) 0.55f else 0.32f
    val tickBaseColor = customContentColor ?: if (isDayMode) Color(0xFF222222) else MaterialTheme.colorScheme.onBackground

    BoxWithConstraints(
        modifier         = modifier.padding(top = 10.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (digitalOnly) {
            val numFontSize = minOf(maxWidth.value * 0.40f, maxHeight.value * 0.45f).coerceIn(24f, 54f).sp
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier            = Modifier.fillMaxSize()
            ) {
                Text(
                    text          = "%.0f".format(speedDisplay),
                    color         = contentColor,
                    fontSize      = numFontSize,
                    fontWeight    = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    letterSpacing = (-1.5).sp,
                    maxLines      = 1
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text          = unitLabel,
                    color         = contentColor.copy(alpha = subAlpha * 1.5f),
                    fontSize      = 10.sp,
                    fontWeight    = androidx.compose.ui.text.font.FontWeight.Bold,
                    letterSpacing = 2.sp,
                    maxLines      = 1
                )
            }
        } else {
            val speedFontSize = minOf(maxWidth.value * 0.28f, maxHeight.value * 0.30f).coerceIn(20f, 38f).sp
            val unitFontSize = if (maxWidth < 120.dp) 7.5.sp else 8.5.sp

            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx    = size.width  / 2f
                val cy    = size.height / 2f
                val arcR  = minOf(size.width, size.height) * 0.39f
                val trackW = arcR * 0.12f
                val startAngle    = 150f
                val sweepTotal    = 240f
                val progressSweep = (speedDisplay / maxSpeed).coerceIn(0f, 1f) * sweepTotal

                val tl   = Offset(cx - arcR, cy - arcR)
                val sz   = Size(arcR * 2f, arcR * 2f)

                drawArc(
                    color      = contentColor.copy(alpha = trackAlpha),
                    startAngle = startAngle,
                    sweepAngle = sweepTotal,
                    useCenter  = false,
                    topLeft    = tl,
                    size       = sz,
                    style      = Stroke(width = trackW, cap = StrokeCap.Round)
                )

                if (progressSweep > 0.5f) {
                    drawArc(
                        color      = accent,
                        startAngle = startAngle,
                        sweepAngle = progressSweep,
                        useCenter  = false,
                        topLeft    = tl,
                        size       = sz,
                        style      = Stroke(width = trackW, cap = StrokeCap.Round)
                    )
                }

                for (i in 0..10) {
                    val angle   = startAngle + i * (sweepTotal / 10f)
                    val rad     = Math.toRadians(angle.toDouble())
                    val isMajor = i % 2 == 0
                    val outerR  = arcR - trackW / 2f - 2.5.dp.toPx()
                    val innerR  = outerR - if (isMajor) 6.dp.toPx() else 3.5.dp.toPx()
                    drawLine(
                        color       = tickBaseColor.copy(alpha = if (isMajor) tickAlphaMaj else tickAlphaMin),
                        start       = Offset(cx + (outerR * cos(rad)).toFloat(), cy + (outerR * sin(rad)).toFloat()),
                        end         = Offset(cx + (innerR * cos(rad)).toFloat(), cy + (innerR * sin(rad)).toFloat()),
                        strokeWidth = if (isMajor) 1.5.dp.toPx() else 0.8.dp.toPx()
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier            = Modifier.offset(y = (-2).dp)
            ) {
                Text(
                    text          = "%.0f".format(speedDisplay),
                    color         = contentColor,
                    fontSize      = speedFontSize,
                    letterSpacing = (-1).sp,
                    maxLines      = 1
                )
                Text(
                    text          = unitLabel,
                    color         = contentColor.copy(alpha = subAlpha),
                    fontSize      = unitFontSize,
                    letterSpacing = 1.5.sp,
                    maxLines      = 1
                )
            }
        }
    }
}
