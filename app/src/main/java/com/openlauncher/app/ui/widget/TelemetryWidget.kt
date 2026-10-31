package com.openlauncher.app.ui.widget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openlauncher.app.ui.theme.LocalHelpTextColor
import com.openlauncher.app.util.LocationData
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TelemetryWidget(
    location: LocationData?,
    bearing: Float,
    accent: Color,
    isDayMode: Boolean = false,
    customContentColor: Color? = null,
    modifier: Modifier = Modifier
) {
    val contentColor = customContentColor ?: if (isDayMode) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground
    val subColor     = LocalHelpTextColor.current ?: customContentColor?.copy(alpha = 0.6f) ?: if (isDayMode) Color(0xFF888888) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
    val ringColor    = customContentColor?.copy(alpha = 0.28f) ?: if (isDayMode) Color(0xFFB0B0B0) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.20f)
    val headingCardinal = bearingToCardinal(bearing)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        val w = maxWidth
        val h = maxHeight
        val isCompactH = h < 140.dp
        val isNarrow = w < 240.dp
        val readoutH = when {
            isCompactH -> 14.dp
            isNarrow -> 26.dp
            else -> 32.dp
        }
        val dialArea = (h - readoutH - 4.dp).coerceAtLeast(44.dp)
        val dialSize = minOf(w, dialArea).coerceAtLeast(44.dp)

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CompassDial(
                    dialSize = dialSize,
                    bearing = bearing,
                    accent = accent,
                    contentColor = contentColor,
                    subColor = subColor,
                    ringColor = ringColor
                )
            }

            // ── Bottom readout: LAT | heading cardinal | LON ─────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isNarrow) 2.dp else 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    if (!isCompactH) {
                        Text(
                            text = "LAT",
                            color = subColor,
                            letterSpacing = 1.sp,
                            fontSize = if (isNarrow) 6.5.sp else 7.sp
                        )
                    }
                    Text(
                        text = if (location != null) formatLat(location.latitude, compact = isNarrow || isCompactH) else "—",
                        color = contentColor,
                        fontSize = if (isCompactH) 8.sp else 9.5.sp,
                        maxLines = 1
                    )
                }
                Text(
                    text = headingCardinal,
                    color = accent,
                    fontSize = if (isCompactH) 9.sp else 11.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    maxLines = 1
                )
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    if (!isCompactH) {
                        Text(
                            text = "LON",
                            color = subColor,
                            letterSpacing = 1.sp,
                            fontSize = if (isNarrow) 6.5.sp else 7.sp
                        )
                    }
                    Text(
                        text = if (location != null) formatLon(location.longitude, compact = isNarrow || isCompactH) else "—",
                        color = contentColor,
                        fontSize = if (isCompactH) 8.sp else 9.5.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun CompassDial(
    dialSize: Dp,
    bearing: Float,
    accent: Color,
    contentColor: Color,
    subColor: Color,
    ringColor: Color
) {
    val measurer = rememberTextMeasurer()
    val heading = ((bearing % 360f) + 360f) % 360f

    Box(modifier = Modifier.size(dialSize), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val outerR = size.minDimension * 0.48f
            val strokeW = (outerR * 0.02f).coerceAtLeast(1f)

            // ── Rotating dial (turning with the vehicle bearing) ──────────────
            rotate(-bearing, pivot = Offset(cx, cy)) {
                drawCircle(ringColor, outerR, Offset(cx, cy), style = Stroke(strokeW))
                drawCircle(ringColor.copy(alpha = 0.45f), outerR * 0.80f, Offset(cx, cy), style = Stroke(strokeW * 0.6f))

                // Ticks: 5° minor, 10° medium, 30° major, 90° cardinal
                for (deg in 0 until 360 step 5) {
                    val isCardinal = deg % 90 == 0
                    val isMajor = deg % 30 == 0
                    val isMedium = deg % 10 == 0
                    val a = Math.toRadians((deg - 90).toDouble())
                    val cosA = cos(a).toFloat()
                    val sinA = sin(a).toFloat()
                    val len = when {
                        isCardinal -> outerR * 0.13f
                        isMajor    -> outerR * 0.085f
                        isMedium   -> outerR * 0.055f
                        else       -> outerR * 0.030f
                    }
                    val outer = outerR - strokeW
                    val inner = outer - len
                    val col = when {
                        isCardinal -> contentColor.copy(alpha = 0.92f)
                        isMajor    -> contentColor.copy(alpha = 0.62f)
                        isMedium   -> subColor.copy(alpha = 0.50f)
                        else       -> subColor.copy(alpha = 0.26f)
                    }
                    drawLine(
                        color = col,
                        start = Offset(cx + outer * cosA, cy + outer * sinA),
                        end = Offset(cx + inner * cosA, cy + inner * sinA),
                        strokeWidth = when {
                            isCardinal -> strokeW * 1.7f
                            isMajor    -> strokeW
                            else       -> strokeW * 0.65f
                        }
                    )
                }

                // Cardinal labels (kept well outside the centre so they never crowd the heading)
                val cardinalFont = (outerR * 0.17f).toSp()
                listOf("N" to 0, "E" to 90, "S" to 180, "W" to 270).forEach { (label, deg) ->
                    val a = Math.toRadians((deg - 90).toDouble())
                    val r = outerR * 0.77f
                    val tx = cx + r * cos(a).toFloat()
                    val ty = cy + r * sin(a).toFloat()
                    val style = TextStyle(
                        color = if (label == "N") accent else contentColor.copy(alpha = 0.88f),
                        fontSize = cardinalFont,
                        fontWeight = FontWeight.Bold
                    )
                    val layout = measurer.measure(AnnotatedString(label), style)
                    drawText(layout, topLeft = Offset(tx - layout.size.width / 2f, ty - layout.size.height / 2f))
                }

                // Degree numerals on larger dials only (avoids clutter when small)
                if (outerR > 78f) {
                    val degFont = (outerR * 0.080f).toSp()
                    for (deg in 30 until 360 step 30) {
                        if (deg % 90 == 0) continue
                        val a = Math.toRadians((deg - 90).toDouble())
                        val r = outerR * 0.86f
                        val tx = cx + r * cos(a).toFloat()
                        val ty = cy + r * sin(a).toFloat()
                        val layout = measurer.measure(AnnotatedString("$deg"), TextStyle(color = subColor, fontSize = degFont))
                        drawText(layout, topLeft = Offset(tx - layout.size.width / 2f, ty - layout.size.height / 2f))
                    }
                }

                // North marker (small accent triangle sitting on the ring at N)
                val markerH = outerR * 0.10f
                val markerW = outerR * 0.045f
                drawPath(
                    path = Path().apply {
                        moveTo(cx, cy - outerR + markerH)
                        lineTo(cx - markerW, cy - outerR)
                        lineTo(cx + markerW, cy - outerR)
                        close()
                    },
                    color = accent
                )
            }

            // ── Fixed lubber index at the top (vehicle heading reference) ─────
            val indexH = outerR * 0.11f
            val indexW = outerR * 0.05f
            drawPath(
                path = Path().apply {
                    moveTo(cx, cy - outerR + indexH)
                    lineTo(cx - indexW, cy - outerR)
                    lineTo(cx + indexW, cy - outerR)
                    close()
                },
                color = accent.copy(alpha = 0.55f)
            )

            // Centre backing disc so the heading always reads cleanly over the dial
            drawCircle(
                color = ringColor.copy(alpha = 0.62f),
                radius = outerR * 0.33f,
                center = Offset(cx, cy)
            )
        }

        // Fixed digital heading in the dial centre — degrees only, perfectly centred
        val headingFont = (dialSize.value * 0.20f).coerceIn(14f, 36f).sp
        Text(
            text = String.format(java.util.Locale.US, "%.0f°", heading),
            color = accent,
            fontSize = headingFont,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun bearingToCardinal(bearing: Float): String {
    val directions = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
    val normalized = (bearing % 360f + 360f) % 360f
    val index = (((normalized + 11.25f) / 22.5f).toInt()) % 16
    return directions[index]
}

private fun formatLat(lat: Double, compact: Boolean = false) =
    if (compact) String.format(java.util.Locale.US, "%.2f° %s", abs(lat), if (lat >= 0) "N" else "S")
    else String.format(java.util.Locale.US, "%.4f° %s", abs(lat), if (lat >= 0) "N" else "S")

private fun formatLon(lon: Double, compact: Boolean = false) =
    if (compact) String.format(java.util.Locale.US, "%.2f° %s", abs(lon), if (lon >= 0) "E" else "W")
    else String.format(java.util.Locale.US, "%.4f° %s", abs(lon), if (lon >= 0) "E" else "W")
