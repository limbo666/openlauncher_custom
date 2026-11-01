package com.openlauncher.app.ui.widget

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.openlauncher.app.data.SpectrumStyle
import kotlin.math.min

/**
 * Real-time sound spectrum. Bars animate only while [available] and [hasSignal]
 * (i.e. real audio is actually flowing); otherwise it shows a user-selected
 * [fallbackImageUri] (e.g. a car logo) so the cell never feels empty, or a plain
 * "NO AUDIO CAPTURE" hint when no image is configured.
 */
@Composable
fun SpectrumWidget(
    spectrum: FloatArray,
    available: Boolean,
    hasSignal: Boolean,
    barCount: Int,
    style: SpectrumStyle,
    mirror: Boolean,
    gain: Float,
    color: Color,
    fallbackImageUri: String,
    modifier: Modifier = Modifier,
    isDayMode: Boolean = false
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (available && hasSignal) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val n = barCount.coerceIn(4, 64)
                val values = resample(spectrum, n)
                val slot = size.width / n
                val barWidth = slot * 0.72f
                val centerY = size.height / 2f
                val g = gain.coerceIn(0.3f, 3f)

                for (i in 0 until n) {
                    val x = i * slot + (slot - barWidth) / 2f
                    val v = (values[i] * g).coerceIn(0f, 1f)

                    if (mirror) {
                        val halfH = v * centerY
                        when (style) {
                            SpectrumStyle.BARS -> drawRect(
                                color = color,
                                topLeft = Offset(x, centerY - halfH),
                                size = Size(barWidth, halfH * 2f)
                            )
                            SpectrumStyle.ROUNDED -> drawRoundRect(
                                color = color,
                                topLeft = Offset(x, centerY - halfH),
                                size = Size(barWidth, halfH * 2f),
                                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                            )
                            SpectrumStyle.DOTS -> {
                                val r = barWidth / 2f
                                drawCircle(color, r, Offset(x + barWidth / 2f, centerY - halfH))
                                drawCircle(color, r, Offset(x + barWidth / 2f, centerY + halfH))
                            }
                        }
                    } else {
                        val h = v * size.height
                        when (style) {
                            SpectrumStyle.BARS -> drawRect(
                                color = color,
                                topLeft = Offset(x, size.height - h),
                                size = Size(barWidth, h)
                            )
                            SpectrumStyle.ROUNDED -> drawRoundRect(
                                color = color,
                                topLeft = Offset(x, size.height - h),
                                size = Size(barWidth, h),
                                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                            )
                            SpectrumStyle.DOTS -> {
                                val r = barWidth / 2f
                                drawCircle(color, r, Offset(x + barWidth / 2f, size.height - h - r))
                            }
                        }
                    }
                }
            }
        } else if (fallbackImageUri.isNotEmpty()) {
            AsyncImage(
                model = Uri.parse(fallbackImageUri),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val baseline = size.height - 6.dp.toPx()
                val dotColor = color.copy(alpha = 0.35f)
                val n = 12
                val slot = size.width / n
                for (i in 0 until n) {
                    val x = i * slot + slot / 2f
                    drawCircle(dotColor, 2.dp.toPx(), Offset(x, baseline))
                }
            }
            Text(
                text = "NO AUDIO CAPTURE",
                color = color.copy(alpha = 0.55f),
                fontSize = 7.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

/** Bucket-averages the fixed-resolution spectrum down to [n] bars. */
private fun resample(src: FloatArray, n: Int): FloatArray {
    if (src.isEmpty()) return FloatArray(n)
    val out = FloatArray(n)
    for (i in 0 until n) {
        val start = i * src.size / n
        val end = min(((i + 1) * src.size / n), src.size).coerceAtLeast(start + 1)
        var sum = 0f
        for (j in start until end) sum += src[j]
        out[i] = (sum / (end - start)).coerceIn(0f, 1f)
    }
    return out
}
