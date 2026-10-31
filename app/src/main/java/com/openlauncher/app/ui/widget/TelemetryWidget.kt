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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openlauncher.app.util.LocationData
import kotlin.math.abs

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
    val subColor     = customContentColor?.copy(alpha = 0.6f) ?: if (isDayMode) Color(0xFF888888) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
    val ringColor    = customContentColor?.copy(alpha = 0.25f) ?: if (isDayMode) Color(0xFFCCCCCC) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.18f)
    val cardinalMain = customContentColor?.copy(alpha = 0.85f) ?: if (isDayMode) Color(0xFF333333) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
    val cardinalSub  = customContentColor?.copy(alpha = 0.50f) ?: if (isDayMode) Color(0xFF888888) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
    val arrowColor   = customContentColor ?: if (isDayMode) Color(0xFF222222) else MaterialTheme.colorScheme.onBackground

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        val isCompactH = maxHeight < 140.dp
        val isNarrow = maxWidth < 240.dp
        val bottomHeight = if (isCompactH) 20.dp else 36.dp
        val availableDialH = (maxHeight - bottomHeight - 8.dp).coerceAtLeast(30.dp)
        val dialSize = minOf(maxWidth - 12.dp, availableDialH).coerceAtLeast(30.dp)
        val radius = dialSize * 0.36f

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── Compass Ring (Strict Square prevents rotation distortion) ─────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Rotating dial layer
                Box(
                    modifier = Modifier
                        .size(dialSize)
                        .graphicsLayer { rotationZ = -bearing },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val r = radius.toPx()
                        drawCircle(
                            color = ringColor,
                            radius = r,
                            center = Offset(cx, cy),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }

                    val cardPad = (dialSize / 2 - radius - 6.dp).coerceAtLeast(0.dp)
                    Text(
                        text = "N",
                        color = cardinalMain,
                        fontSize = if (isCompactH) 9.sp else 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = cardPad)
                    )
                    Text(
                        text = "S",
                        color = cardinalMain,
                        fontSize = if (isCompactH) 9.sp else 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = cardPad)
                    )
                    Text(
                        text = "E",
                        color = cardinalSub,
                        fontSize = if (isCompactH) 8.sp else 9.5.sp,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = cardPad)
                    )
                    Text(
                        text = "W",
                        color = cardinalSub,
                        fontSize = if (isCompactH) 8.sp else 9.5.sp,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = cardPad)
                    )
                }

                // Fixed layer: Arrow always points straight up + pivot
                Canvas(modifier = Modifier.size(dialSize)) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val r = radius.toPx()

                    val arrowH = r * 0.22f
                    val arrowW = arrowH * 0.48f
                    drawPath(
                        path = Path().apply {
                            moveTo(cx, cy - arrowH)
                            lineTo(cx + arrowW, cy + arrowH * 0.42f)
                            lineTo(cx, cy + arrowH * 0.08f)
                            lineTo(cx - arrowW, cy + arrowH * 0.42f)
                            close()
                        },
                        color = arrowColor
                    )
                    drawCircle(
                        color = Color(0xFF777777),
                        radius = 2.5.dp.toPx(),
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                }
            }

            // ── Telemetry readouts at bottom ───────────────────────────────────
            if (isCompactH) {
                // 3-item single-line row for 1x1: Lat (left), Heading (center), Lon (right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatCompactLat(location?.latitude),
                        color = subColor,
                        fontSize = 8.sp,
                        maxLines = 1
                    )
                    Text(
                        text = String.format(java.util.Locale.US, "%.0f° %s", bearing, bearingToCardinal(bearing)),
                        color = accent,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = formatCompactLon(location?.longitude),
                        color = subColor,
                        fontSize = 8.sp,
                        maxLines = 1
                    )
                }
            } else if (isNarrow) {
                // 3-column layout for narrow cards (LATITUDE left, HEADING center, LONGITUDE right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "LATITUDE",
                            style = MaterialTheme.typography.labelSmall,
                            color = subColor,
                            letterSpacing = 0.5.sp,
                            fontSize = 6.5.sp
                        )
                        Text(
                            text = if (location != null) formatLat(location.latitude, narrow = true) else "—",
                            color = contentColor,
                            fontSize = 8.5.sp
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "HEADING",
                            style = MaterialTheme.typography.labelSmall,
                            color = subColor,
                            letterSpacing = 0.5.sp,
                            fontSize = 6.5.sp
                        )
                        Text(
                            text = String.format(java.util.Locale.US, "%.0f° %s", bearing, bearingToCardinal(bearing)),
                            color = accent,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "LONGITUDE",
                            style = MaterialTheme.typography.labelSmall,
                            color = subColor,
                            letterSpacing = 0.5.sp,
                            fontSize = 6.5.sp,
                            textAlign = TextAlign.End
                        )
                        Text(
                            text = if (location != null) formatLon(location.longitude, narrow = true) else "—",
                            color = contentColor,
                            fontSize = 8.5.sp,
                            textAlign = TextAlign.End
                        )
                    }
                }
            } else {
                // Multi-line detailed layout for wide cards (2x2 and larger)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "LATITUDE",
                            style = MaterialTheme.typography.labelSmall,
                            color = subColor,
                            letterSpacing = 1.sp,
                            fontSize = 7.sp
                        )
                        Text(
                            text = if (location != null) formatLat(location.latitude) else "—",
                            color = contentColor,
                            fontSize = 11.sp
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "HEADING",
                            style = MaterialTheme.typography.labelSmall,
                            color = subColor,
                            letterSpacing = 1.sp,
                            fontSize = 7.sp
                        )
                        Text(
                            text = String.format(java.util.Locale.US, "%.0f° %s", bearing, bearingToCardinal(bearing)),
                            color = accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "LONGITUDE",
                            style = MaterialTheme.typography.labelSmall,
                            color = subColor,
                            letterSpacing = 1.sp,
                            fontSize = 7.sp,
                            textAlign = TextAlign.End
                        )
                        Text(
                            text = if (location != null) formatLon(location.longitude) else "—",
                            color = contentColor,
                            fontSize = 11.sp,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

private fun bearingToCardinal(bearing: Float): String {
    val directions = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
    val normalized = (bearing % 360f + 360f) % 360f
    val index = (((normalized + 11.25f) / 22.5f).toInt()) % 16
    return directions[index]
}

private fun formatCompactLat(lat: Double?) =
    if (lat != null) String.format(java.util.Locale.US, "%.2f° %s", abs(lat), if (lat >= 0) "N" else "S") else "—"

private fun formatCompactLon(lon: Double?) =
    if (lon != null) String.format(java.util.Locale.US, "%.2f° %s", abs(lon), if (lon >= 0) "E" else "W") else "—"

private fun formatLat(lat: Double, narrow: Boolean = false) =
    if (narrow) String.format(java.util.Locale.US, "%.2f° %s", abs(lat), if (lat >= 0) "N" else "S")
    else String.format(java.util.Locale.US, "%.4f° %s", abs(lat), if (lat >= 0) "N" else "S")

private fun formatLon(lon: Double, narrow: Boolean = false) =
    if (narrow) String.format(java.util.Locale.US, "%.2f° %s", abs(lon), if (lon >= 0) "E" else "W")
    else String.format(java.util.Locale.US, "%.4f° %s", abs(lon), if (lon >= 0) "E" else "W")
