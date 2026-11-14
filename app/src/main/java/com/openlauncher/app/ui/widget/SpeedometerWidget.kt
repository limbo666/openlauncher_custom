package com.openlauncher.app.ui.widget

import android.graphics.Paint as AndroidPaint
import android.graphics.Rect as AndroidRect
import android.graphics.RectF as AndroidRectF
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun SpeedometerWidget(
    location: LocationData?,
    isMetric: Boolean,
    accent: Color,
    isDayMode: Boolean = false,
    digitalOnly: Boolean = false,
    animalsMode: Boolean = false,
    customContentColor: Color? = null,
    modifier: Modifier = Modifier
) {
    val maxSpeed = if (isMetric) 200f else 100f

    // ── Hidden test mode ────────────────────────────────────────────────────
    // Three quick taps add +10 km/h to the indicated speed; the new value then
    // HOLDS for 500 ms before bleeding off at 20 km/h per second. Works in
    // every mode (dial, digital, animals) since it feeds the shared speed.
    var boostKmh by remember { mutableStateOf(0f) }
    var tapCount by remember { mutableStateOf(0) }
    var lastTapAt by remember { mutableStateOf(0L) }
    var holdUntil by remember { mutableStateOf(0L) }
    LaunchedEffect(boostKmh > 0f) {
        while (boostKmh > 0f) {
            val holdLeft = holdUntil - SystemClock.uptimeMillis()
            if (holdLeft > 0L) {
                delay(holdLeft)   // hold the boosted speed before dropping
                continue
            }
            delay(50)
            boostKmh = (boostKmh - 1f).coerceAtLeast(0f)   // 20 km/h per second
        }
    }

    val baseKmh = ((location?.speedMps ?: 0f) * 3.6f).let { v -> if (v.isFinite()) v.coerceAtLeast(0f) else 0f }
    val speedKmh = baseKmh + boostKmh
    val speedDisplay = if (isMetric) speedKmh else speedKmh * 0.621371f
    val unitLabel    = if (isMetric) "KM/H" else "MPH"
    val trackAlpha   = if (isDayMode) 0.18f else 0.07f
    val tickAlphaMaj = if (isDayMode) 0.50f else 0.28f
    val tickAlphaMin = if (isDayMode) 0.22f else 0.12f

    val contentColor = customContentColor ?: if (isDayMode) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground
    val subAlpha     = if (isDayMode) 0.55f else 0.32f
    val tickBaseColor = customContentColor ?: if (isDayMode) Color(0xFF222222) else MaterialTheme.colorScheme.onBackground
    val measurer = rememberTextMeasurer()

    BoxWithConstraints(
        modifier         = modifier
            .padding(top = 8.dp, bottom = 4.dp)
            // Non-consuming tap observer — registers the hidden test taps without
            // stealing the long-press menu or any other widget gesture.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        // Wait for a press (Initial pass, nothing consumed)
                        var down: androidx.compose.ui.input.pointer.PointerInputChange? = null
                        while (down == null) {
                            down = awaitPointerEvent(PointerEventPass.Initial)
                                .changes.firstOrNull { it.pressed }
                        }
                        val downAt = SystemClock.uptimeMillis()
                        var moved = false
                        var upAt = downAt
                        var pressed = true
                        while (pressed) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down!!.id }
                            if (change == null) break
                            if (!change.pressed) {
                                upAt = SystemClock.uptimeMillis()
                                pressed = false
                                break
                            }
                            if ((change.position - down!!.position).getDistance() > viewConfiguration.touchSlop) {
                                moved = true
                            }
                        }
                        if (!moved && upAt - downAt < 300L) {
                            val now = SystemClock.uptimeMillis()
                            if (now - lastTapAt > 1200L) tapCount = 0
                            lastTapAt = now
                            tapCount += 1
                            if (tapCount >= 3) {
                                tapCount = 0
                                boostKmh = (boostKmh + 10f).coerceAtMost(600f)
                                holdUntil = SystemClock.uptimeMillis() + 500L  // pause before dropping
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val w = maxWidth
        if (animalsMode) {
            // ── Animals mode: sprite animal for the speed range + small readout ──
            val context = LocalContext.current
            var sprite by remember { mutableStateOf(AnimalSprite.bitmap(context)) }
            LaunchedEffect(Unit) {
                if (sprite == null) {
                    sprite = withContext(Dispatchers.IO) { AnimalSprite.bitmap(context) }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val bmp = sprite ?: return@Canvas
                        val cellW = bmp.width / AnimalSprite.COLUMNS
                        val cellH = bmp.height / AnimalSprite.ROWS
                        if (cellW <= 0 || cellH <= 0) return@Canvas
                        val index = AnimalSprite.cellIndexForSpeed(speedKmh)
                        val row = index / AnimalSprite.COLUMNS
                        val col = index % AnimalSprite.COLUMNS
                        // Fit the cell into the available area, preserving aspect
                        val scale = minOf(size.width / cellW, size.height / cellH)
                        val dw = cellW * scale
                        val dh = cellH * scale
                        val left = (size.width - dw) / 2f
                        val top = (size.height - dh) / 2f
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawBitmap(
                                bmp,
                                AndroidRect(col * cellW, row * cellH, (col + 1) * cellW, (row + 1) * cellH),
                                AndroidRectF(left, top, left + dw, top + dh),
                                AndroidPaint(AndroidPaint.FILTER_BITMAP_FLAG)
                            )
                        }
                    }
                }
                // Small speed indicator along the bottom
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "%.0f".format(speedDisplay),
                        color = contentColor,
                        fontSize = minOf(w.value * 0.20f, 22f).coerceIn(13f, 22f).sp,
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
        } else if (digitalOnly) {
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
