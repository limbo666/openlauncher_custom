package com.openlauncher.app.ui.widget

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import com.openlauncher.app.ui.theme.LocalHelpTextColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import com.airbnb.lottie.compose.*
import com.openlauncher.app.model.DailyForecast
import com.openlauncher.app.model.WeatherState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WeatherWidget(
    state: WeatherState?,
    accent: Color,
    metric: Boolean,
    isDayMode: Boolean = false,
    customContentColor: Color? = null,
    isOnline: Boolean = true,
    forecastDaysCount: Int = 5,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    isEditing: Boolean = false,
    modifier: Modifier = Modifier
) {
    val contentColor = customContentColor ?: if (isDayMode) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground
    val subColor     = LocalHelpTextColor.current ?: customContentColor?.copy(alpha = 0.65f) ?: if (isDayMode) Color(0xFF6C757D) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
    val cardBg       = if (isDayMode) Color(0x10000000) else Color(0x18FFFFFF)
    val cardBorder   = if (isDayMode) Color(0x18000000) else Color(0x20FFFFFF)
    val effectiveAccent = if (isDayMode && accent.luminance() > 0.65f) Color(0xFF111111) else accent

    val context = LocalContext.current
    val svgImageLoader = remember {
        ImageLoader.Builder(context)
            .components { add(SvgDecoder.Factory()) }
            .build()
    }

    val isActuallyOffline = state == null || state.isOffline || !isOnline

    // ── Tap to refresh with cooldown ─────────────────────────────────────────
    var lastTapEpochMs by remember { mutableStateOf(0L) }
    var cooldownFeedback by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val handleRefreshTap = {
        if (!isEditing) {
            val now = System.currentTimeMillis()
            val elapsed = now - lastTapEpochMs
            val cooldownDurationMs = if (isActuallyOffline) 5_000L else 30_000L
            if (elapsed < cooldownDurationMs && lastTapEpochMs > 0L) {
                val rem = ((cooldownDurationMs - elapsed) / 1000L).coerceAtLeast(1L)
                cooldownFeedback = "Wait ${rem}s"
                scope.launch {
                    delay(1500)
                    cooldownFeedback = null
                }
            } else {
                lastTapEpochMs = now
                cooldownFeedback = null
                onRefresh()
            }
        }
    }

    // Refresh spinning animation
    val infiniteTransition = rememberInfiniteTransition(label = "refresh_spin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_angle"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .pointerInput(isEditing) {
                if (!isEditing) {
                    detectTapGestures(
                        onTap = { handleRefreshTap() },
                        onLongPress = { onLongClick?.invoke() }
                    )
                }
            }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        val w = maxWidth
        val h = maxHeight
        val is1x1 = w < 180.dp && h < 145.dp
        val isCompactH = h < 145.dp
        val isTall = w < 180.dp && h >= 145.dp
        val isWideStrip = isCompactH && w >= 180.dp
        val isLarge = w >= 380.dp && h >= 145.dp
        val isMedium = !is1x1 && !isWideStrip && !isTall && !isLarge
        val today = state?.dailyForecast?.firstOrNull()

        // Subtle refresh indicator / status badge in top right corner
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 1.dp, end = 2.dp)
        ) {
            if (cooldownFeedback != null) {
                Text(
                    text = cooldownFeedback!!,
                    color = subColor,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(cardBg)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            } else if (isRefreshing) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refreshing",
                    tint = effectiveAccent,
                    modifier = Modifier
                        .size(13.dp)
                        .rotate(spinAngle)
                )
            } else if (isActuallyOffline) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(cardBg)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF9800))
                    )
                    Text(
                        text = "OFFLINE",
                        color = subColor,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            } else if (!is1x1) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Tap to Refresh",
                    tint = subColor.copy(alpha = 0.35f),
                    modifier = Modifier.size(11.dp)
                )
            }
        }

        val showFullOfflinePlaceholder = state == null || (isActuallyOffline && state.dailyForecast.isEmpty() && state.temperatureCelsius == 0.0)

        if (showFullOfflinePlaceholder) {
            // ── OFFLINE STATE (Dynamic scaling, zero text cut off in 1x1) ──────────
            if (is1x1) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OfflineWeatherGraphic(
                        width = 54.dp,
                        height = 40.dp,
                        svgLoader = svgImageLoader
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isRefreshing) "UPDATING" else "OFFLINE",
                            color = contentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            maxLines = 1
                        )
                        Spacer(Modifier.height(1.dp))
                        Text(
                            text = if (isRefreshing) "Connecting..." else "No Connection",
                            color = subColor,
                            fontSize = 8.sp,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(1.dp))
                        Text(
                            text = if (isRefreshing) "Please wait" else "Tap to retry",
                            color = effectiveAccent,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp,
                            maxLines = 1
                        )
                    }
                }
            } else {
                // Wide, Medium, or Large Offline layout
                val graphicW = (h * 0.75f).coerceIn(80.dp, 160.dp)
                val graphicH = (graphicW * 0.76f)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OfflineWeatherGraphic(
                        width = graphicW,
                        height = graphicH,
                        svgLoader = svgImageLoader
                    )
                    Column(
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isRefreshing) "UPDATING..." else "OFFLINE",
                            color = contentColor,
                            fontSize = if (isLarge) 20.sp else if (isMedium) 16.sp else 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = if (isRefreshing) "CONNECTING TO WEATHER SERVICE" else "NO INTERNET CONNECTION",
                            color = subColor,
                            fontSize = if (isLarge) 11.sp else if (isMedium) 9.5.sp else 8.5.sp,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (isRefreshing) "Fetching current conditions..." else "Tap widget to refresh weather & GPS",
                            color = effectiveAccent,
                            fontSize = if (isLarge) 9.5.sp else 8.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }
        } else if (is1x1) {
            // ── 1x1 COMPACT ONLINE LAYOUT (Balanced proportions, zero truncation) ──
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MeteoconAnimatedIcon(
                    slug = state.conditionSlug,
                    modifier = Modifier.size((minOf(w, h) * 0.6f).coerceIn(42.dp, 74.dp))
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.temperatureDisplay(metric),
                        color = contentColor,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                        maxLines = 1
                    )
                    val condLen = state.conditionLabel.length
                    val condSize = when {
                        condLen > 14 -> 7.sp
                        condLen > 10 -> 7.5.sp
                        else -> 8.sp
                    }
                    Text(
                        text = state.conditionLabel.uppercase(),
                        color = effectiveAccent,
                        fontSize = condSize,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (state.locationName.isNotEmpty()) {
                        val locLen = state.locationName.length
                        val locSize = when {
                            locLen > 12 -> 6.5.sp
                            locLen > 8 -> 7.sp
                            else -> 7.5.sp
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = subColor,
                                modifier = Modifier.size(8.dp)
                            )
                            Text(
                                text = state.locationName.uppercase(),
                                color = subColor,
                                fontSize = locSize,
                                letterSpacing = 0.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        } else if (isWideStrip) {
            // ── WIDE STRIP LAYOUT (e.g. 2x1, 3x1, 4x1): Current left, dynamic forecast right ──
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left cluster: the symbol with location + condition directly beneath it
                // (the icon column is wider than the squeezed text column), then the big
                // temperature and the stats beside it.
                val leftIconSize = (h * 0.6f).coerceIn(52.dp, 88.dp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        MeteoconAnimatedIcon(
                            slug = state.conditionSlug,
                            modifier = Modifier.size(leftIconSize)
                        )
                        if (state.locationName.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(Icons.Default.LocationOn, null, tint = effectiveAccent, modifier = Modifier.size(9.dp))
                                Text(
                                    text = state.locationName.uppercase(),
                                    color = subColor,
                                    fontSize = 8.5.sp,
                                    letterSpacing = 0.3.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Text(
                            text = state.conditionLabel.uppercase(),
                            color = effectiveAccent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = state.temperatureDisplay(metric),
                            color = contentColor,
                            fontSize = if (h > 120.dp) 34.sp else 30.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                            maxLines = 1,
                            softWrap = false
                        )
                        StatsRow(today, state, metric, contentColor, subColor, effectiveAccent, iconSize = 10.dp, fontSize = 8.5.sp)
                    }
                }

                if (state.dailyForecast.isNotEmpty()) {
                    // Divider
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .fillMaxHeight(0.7f)
                            .width(1.dp)
                            .background(if (isDayMode) Color(0x25000000) else Color(0x20FFFFFF))
                    )

                    // Right section: Dynamic 3 to 7 Days Forecast based on available width
                    val availableForecastW = w - 150.dp
                    val maxFitDays = (availableForecastW / 52.dp).toInt().coerceIn(2, 7)
                    val days = state.dailyForecast.take(minOf(forecastDaysCount, maxFitDays))
                    Row(
                        modifier = Modifier
                            .wrapContentWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(cardBg)
                            .border(1.dp, cardBorder, RoundedCornerShape(6.dp))
                            .padding(vertical = 4.dp, horizontal = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        days.forEach { day ->
                            DailyForecastCard(
                                forecast = day,
                                metric = metric,
                                contentColor = contentColor,
                                subColor = subColor,
                                svgLoader = svgImageLoader,
                                iconSize = (h * 0.3f).coerceIn(24.dp, 38.dp),
                                labelSize = 9.sp,
                                tempSize = 9.5.sp
                            )
                        }
                    }
                }
            }
        } else if (isTall) {
            // ── TALL NARROW (1x2, 1x3) — vertical hero so nothing gets squeezed ──
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MeteoconAnimatedIcon(
                        slug = state.conditionSlug,
                        modifier = Modifier.size((w * 0.62f).coerceIn(64.dp, 116.dp))
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = state.temperatureDisplay(metric),
                        color = contentColor,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1).sp,
                        maxLines = 1,
                        softWrap = false
                    )
                    if (state.locationName.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Default.LocationOn, null, tint = effectiveAccent, modifier = Modifier.size(11.dp))
                            Text(
                                text = state.locationName.uppercase(),
                                color = subColor,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.5.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Text(
                        text = state.conditionLabel.uppercase(),
                        color = effectiveAccent,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatsRow(today, state, metric, contentColor, subColor, effectiveAccent, iconSize = 10.dp, fontSize = 9.sp)
                    if (state.dailyForecast.isNotEmpty()) {
                        val days = state.dailyForecast.take(forecastDaysCount.coerceIn(2, 5))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(cardBg)
                                .border(1.dp, cardBorder, RoundedCornerShape(8.dp))
                                .padding(vertical = 4.dp, horizontal = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            days.forEach { day ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = day.dayOfWeek,
                                        color = subColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(48.dp),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    AsyncImage(
                                        model = "file:///android_asset/meteocons/svg/${day.conditionSlug}.svg",
                                        imageLoader = svgImageLoader,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Text(day.maxTempDisplay(metric), color = contentColor, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                    Spacer(Modifier.width(6.dp))
                                    Text(day.minTempDisplay(metric), color = subColor, fontSize = 8.5.sp, maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // ── MEDIUM / LARGE (2x2, 3x2, 4x2, etc.) ──
            // Huge Hero Animated Icon and balanced vertical flow without bottom-snapping
            val heroIconSize = when {
                isLarge  -> (h * 0.62f).coerceIn(110.dp, 190.dp)
                isMedium -> (minOf(w, h) * 0.55f).coerceIn(78.dp, 132.dp)
                else     -> (minOf(w, h) * 0.55f).coerceIn(78.dp, 132.dp)
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Upper Section: Prominent Hero Icon + Detailed Conditions
                Row(
                    modifier = Modifier.weight(1f, fill = true).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isLarge) 18.dp else 12.dp)
                ) {
                    MeteoconAnimatedIcon(
                        slug = state.conditionSlug,
                        modifier = Modifier.size(heroIconSize)
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = state.temperatureDisplay(metric),
                            color = contentColor,
                            fontSize = if (isLarge) 50.sp else 36.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-1.5).sp,
                            maxLines = 1,
                            softWrap = false
                        )
                        if (state.locationName.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = effectiveAccent,
                                    modifier = Modifier.size(if (isLarge) 13.dp else 10.dp)
                                )
                                Text(
                                    text = state.locationName.uppercase(),
                                    color = subColor,
                                    fontSize = if (isLarge) 11.sp else 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.8.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Text(
                            text = state.conditionLabel.uppercase(),
                            color = effectiveAccent,
                            fontSize = if (isLarge) 13.sp else 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        StatsRow(today, state, metric, contentColor, subColor, effectiveAccent, iconSize = if (isLarge) 12.dp else 10.dp, fontSize = if (isLarge) 10.5.sp else 9.sp)
                    }
                }

                // Lower Section: Multi-Day Forecast (3 to 7 Days)
                if (state.dailyForecast.isNotEmpty()) {
                    val days = state.dailyForecast.take(forecastDaysCount.coerceIn(3, 7))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(cardBg)
                            .border(1.dp, cardBorder, RoundedCornerShape(8.dp))
                            .padding(vertical = if (isLarge) 6.dp else 4.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        days.forEach { day ->
                            DailyForecastCard(
                                forecast = day,
                                metric = metric,
                                contentColor = contentColor,
                                subColor = subColor,
                                svgLoader = svgImageLoader,
                                iconSize = if (isLarge) 40.dp else 30.dp,
                                labelSize = if (isLarge) 10.sp else 8.5.sp,
                                tempSize = if (isLarge) 11.sp else 9.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatsRow(
    today: DailyForecast?,
    state: WeatherState,
    metric: Boolean,
    contentColor: Color,
    subColor: Color,
    accent: Color,
    iconSize: Dp = 9.dp,
    fontSize: TextUnit = 8.sp
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.padding(top = 3.dp)
    ) {
        if (today != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Icon(Icons.Default.ArrowUpward, null, tint = accent, modifier = Modifier.size(iconSize))
                Text(today.maxTempDisplay(metric), color = contentColor, fontSize = fontSize, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                Icon(Icons.Default.ArrowDownward, null, tint = subColor, modifier = Modifier.size(iconSize))
                Text(today.minTempDisplay(metric), color = subColor, fontSize = fontSize, maxLines = 1, softWrap = false)
            }
        }
        if (state.windspeedKmh > 0) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Icon(Icons.Default.Air, null, tint = subColor, modifier = Modifier.size(iconSize))
                Text("%.0f km/h".format(state.windspeedKmh), color = subColor, fontSize = fontSize, maxLines = 1, softWrap = false)
            }
        }
        if (state.humidity > 0) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Icon(Icons.Default.WaterDrop, null, tint = subColor, modifier = Modifier.size(iconSize))
                Text("${state.humidity}%", color = subColor, fontSize = fontSize, maxLines = 1, softWrap = false)
            }
        }
    }
}

@Composable
private fun DailyForecastCard(
    forecast: DailyForecast,
    metric: Boolean,
    contentColor: Color,
    subColor: Color,
    svgLoader: ImageLoader,
    iconSize: Dp = 24.dp,
    labelSize: TextUnit = 8.sp,
    tempSize: TextUnit = 8.5.sp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(
            text = forecast.dayOfWeek,
            color = subColor,
            fontSize = labelSize,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center
        )

        AsyncImage(
            model = "file:///android_asset/meteocons/svg/${forecast.conditionSlug}.svg",
            imageLoader = svgLoader,
            contentDescription = forecast.conditionLabel,
            modifier = Modifier.size(iconSize)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = forecast.maxTempDisplay(metric),
                color = contentColor,
                fontSize = tempSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = forecast.minTempDisplay(metric),
                color = subColor,
                fontSize = (tempSize.value * 0.88f).sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun OfflineWeatherGraphic(
    width: Dp,
    height: Dp,
    svgLoader: ImageLoader,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(width = width, height = height),
        contentAlignment = Alignment.Center
    ) {
        val sunSize = height * 0.52f
        val sunOffsetX = width * 0.22f
        val sunOffsetY = -height * 0.18f

        val treeSize = height * 0.56f
        val treeOffsetX = -width * 0.26f
        val treeOffsetY = height * 0.05f

        val stormSize = height * 0.72f
        val stormOffsetX = -width * 0.02f
        val stormOffsetY = -height * 0.02f

        val snowSize = height * 0.46f
        val snowOffsetX = -width * 0.15f
        val snowOffsetY = height * 0.20f

        val thermoSize = height * 0.44f
        val thermoOffsetX = width * 0.28f
        val thermoOffsetY = height * 0.12f

        // Level 1: Sun (back, top-right)
        AsyncImage(
            model = "file:///android_asset/meteocons/svg/clear-day.svg",
            imageLoader = svgLoader,
            contentDescription = "Sun",
            modifier = Modifier
                .size(sunSize)
                .offset(x = sunOffsetX, y = sunOffsetY)
                .graphicsLayer { alpha = 0.90f }
        )

        // Level 2: Pollen Tree (midground, left)
        AsyncImage(
            model = "file:///android_asset/meteocons/svg/pollen-tree.svg",
            imageLoader = svgLoader,
            contentDescription = "Pollen Tree",
            modifier = Modifier
                .size(treeSize)
                .offset(x = treeOffsetX, y = treeOffsetY)
                .graphicsLayer { alpha = 0.95f }
        )

        // Level 3: Thunderstorm Cloud (anchor, center)
        AsyncImage(
            model = "file:///android_asset/meteocons/svg/thunderstorms.svg",
            imageLoader = svgLoader,
            contentDescription = "Thunderstorm",
            modifier = Modifier
                .size(stormSize)
                .offset(x = stormOffsetX, y = stormOffsetY)
        )

        // Level 4: Snow (foreground, lower-left)
        AsyncImage(
            model = "file:///android_asset/meteocons/svg/snow.svg",
            imageLoader = svgLoader,
            contentDescription = "Snow",
            modifier = Modifier
                .size(snowSize)
                .offset(x = snowOffsetX, y = snowOffsetY)
        )

        // Level 5: Thermometer (foreground, right)
        AsyncImage(
            model = "file:///android_asset/meteocons/svg/thermometer.svg",
            imageLoader = svgLoader,
            contentDescription = "Thermometer",
            modifier = Modifier
                .size(thermoSize)
                .offset(x = thermoOffsetX, y = thermoOffsetY)
        )
    }
}

@Composable
fun MeteoconAnimatedIcon(
    slug: String,
    modifier: Modifier = Modifier
) {
    val safeSlug = slug.ifEmpty { "partly-cloudy-day" }
    val assetPath = "meteocons/lottie/$safeSlug.json"
    val compositionResult = rememberLottieComposition(LottieCompositionSpec.Asset(assetPath))
    val fallbackResult = rememberLottieComposition(
        if (compositionResult.isFailure) LottieCompositionSpec.Asset("meteocons/lottie/partly-cloudy-day.json")
        else LottieCompositionSpec.Asset(assetPath)
    )
    val composition = compositionResult.value ?: fallbackResult.value
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier
    )
}
