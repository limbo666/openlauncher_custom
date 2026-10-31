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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
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
    val maxSpeed     = if (isMetric) 200f else 100f
    val speedDisplay = ((location?.speedMps ?: 0f) * if (isMetric) 3.6f else 2.237f).coerceAtLeast(0f)
    val unitLabel    = if (isMetric) "KM/H" else "MPH"
    val trackAlpha   = if (isDayMode) 0.18f else 0.07f
    val tickAlphaMaj = if (isDayMode) 0.50f else 0.28f
    val tickAlphaMin = if (isDayMode) 0.22f else 0.12f

    val contentColor = customContentColor ?: if (isDayMode) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground
    val subAlpha     = if (isDayMode) 0.55f else 0.32f
    val tickBaseColor = customContentColor ?: if (isDayMode) Color(0xFF222222) else MaterialTheme.colorScheme.onBackground
    val measurer = rememberTextMeasurer()

    BoxWithConstraints(
        modifier         = modifier.padding(top = 8.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        val w = maxWidth
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
                    fontWeight    = FontWeight.SemiBold,
                    letterSpacing = (-1.5).sp,
                    maxLines      = 1
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text          = unitLabel,
                    color         = contentColor.copy(alpha = subAlpha * 1.5f),
                    fontSize      = 10.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    maxLines      = 1
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── Dial (analog gauge with needle, no centre text) ─────────────
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx    = size.width  / 2f
                        val cy    = size.height / 2f
                        val arcR  = minOf(size.width, size.height) * 0.38f
                        val trackW = arcR * 0.14f
                        val startAngle    = 150f
                        val sweepTotal    = 240f
                        val progress      = (speedDisplay / maxSpeed).coerceIn(0f, 1f)
                        val progressSweep = progress * sweepTotal

                        val tl   = Offset(cx - arcR, cy - arcR)
                        val sz   = Size(arcR * 2f, arcR * 2f)

                        // Track
                        drawArc(
                            color      = contentColor.copy(alpha = trackAlpha),
                            startAngle = startAngle,
                            sweepAngle = sweepTotal,
                            useCenter  = false,
                            topLeft    = tl,
                            size       = sz,
                            style      = Stroke(width = trackW, cap = StrokeCap.Round)
                        )

                        // Progress
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

                        // Ticks
                        for (i in 0..10) {
                            val angle   = startAngle + i * (sweepTotal / 10f)
                            val rad     = Math.toRadians(angle.toDouble())
                            val isMajor = i % 2 == 0
                            val outerR  = arcR - trackW / 2f - 2.dp.toPx()
                            val innerR  = outerR - if (isMajor) 6.dp.toPx() else 3.5.dp.toPx()
                            drawLine(
                                color       = tickBaseColor.copy(alpha = if (isMajor) tickAlphaMaj else tickAlphaMin),
                                start       = Offset(cx + (outerR * cos(rad)).toFloat(), cy + (outerR * sin(rad)).toFloat()),
                                end         = Offset(cx + (innerR * cos(rad)).toFloat(), cy + (innerR * sin(rad)).toFloat()),
                                strokeWidth = if (isMajor) 1.6.dp.toPx() else 0.8.dp.toPx()
                            )
                        }

                        // Scale numerals outside the track
                        if (arcR > 46f) {
                            val labelR = arcR + trackW * 0.5f + 7.dp.toPx()
                            val labelFont = (arcR * 0.105f).toSp()
                            val labelStyle = TextStyle(color = contentColor.copy(alpha = 0.45f), fontSize = labelFont)
                            for (i in 0..10 step 2) {
                                val angle = startAngle + i * (sweepTotal / 10f)
                                val rad = Math.toRadians(angle.toDouble())
                                val value = (maxSpeed * i / 10f).toInt()
                                val tx = cx + (labelR * cos(rad)).toFloat()
                                val ty = cy + (labelR * sin(rad)).toFloat()
                                val layout = measurer.measure(AnnotatedString("$value"), labelStyle)
                                drawText(layout, topLeft = Offset(tx - layout.size.width / 2f, ty - layout.size.height / 2f))
                            }
                        }

                        // Needle from the hub to the inner edge of the track
                        val needleAngle = startAngle + progressSweep
                        val na = Math.toRadians(needleAngle.toDouble())
                        val needleStart = arcR * 0.12f
                        val needleEnd   = arcR - trackW * 0.5f - 2.dp.toPx()
                        drawLine(
                            color = accent,
                            start = Offset(cx + (needleStart * cos(na)).toFloat(), cy + (needleStart * sin(na)).toFloat()),
                            end   = Offset(cx + (needleEnd * cos(na)).toFloat(), cy + (needleEnd * sin(na)).toFloat()),
                            strokeWidth = 2.4.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // Hub
                        drawCircle(
                            color = contentColor.copy(alpha = 0.9f),
                            radius = (arcR * 0.07f).coerceAtLeast(2.5f),
                            center = Offset(cx, cy)
                        )
                        drawCircle(
                            color = accent,
                            radius = (arcR * 0.035f).coerceAtLeast(1.2f),
                            center = Offset(cx, cy)
                        )
                    }
                }

                // ── Bottom digital readout (clean, below the dial) ─────────────
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "%.0f".format(speedDisplay),
                        color = contentColor,
                        fontSize = minOf(w.value * 0.16f, 24f).coerceIn(14f, 24f).sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.5).sp,
                        maxLines = 1
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = unitLabel,
                        color = contentColor.copy(alpha = subAlpha),
                        fontSize = if (w < 120.dp) 7.sp else 8.sp,
                        letterSpacing = 1.5.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
