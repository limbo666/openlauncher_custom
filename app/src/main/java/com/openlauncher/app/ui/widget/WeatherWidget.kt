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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
    val subColor     = customContentColor?.copy(alpha = 0.65f) ?: if (isDayMode) Color(0xFF6C757D) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
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
                    modifier = Modifier.size(46.dp)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.temperatureDisplay(metric),
                        color = contentColor,
                        fontSize = 18.sp,
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
                // Left section: Current conditions with scaled icon
                val leftIconSize = (h * 0.65f).coerceIn(58.dp, 78.dp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    MeteoconAnimatedIcon(
                        slug = state.conditionSlug,
                        modifier = Modifier.size(leftIconSize)
                    )

                    Column(verticalArrangement = Arrangement.Center) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = state.temperatureDisplay(metric),
                                color = contentColor,
                                fontSize = if (h > 110.dp) 24.sp else 21.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp,
                                maxLines = 1
                            )

                            if (state.locationName.isNotEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = effectiveAccent,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = state.locationName.uppercase(),
                                        color = subColor,
                                        fontSize = 8.sp,
                                        letterSpacing = 1.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Text(
                            text = state.conditionLabel.uppercase(),
                            color = effectiveAccent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            maxLines = 1
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 1.dp)
                        ) {
                            if (state.windspeedKmh > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Icon(Icons.Default.Air, null, tint = subColor, modifier = Modifier.size(9.dp))
                                    Text("%.0f km/h".format(state.windspeedKmh), color = subColor, fontSize = 7.5.sp)
                                }
                            }
                            if (state.humidity > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Icon(Icons.Default.WaterDrop, null, tint = subColor, modifier = Modifier.size(9.dp))
                                    Text("${state.humidity}%", color = subColor, fontSize = 7.5.sp)
                                }
                            }
                        }
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
                    val availableForecastW = w - 180.dp
                    val maxFitDays = (availableForecastW / 44.dp).toInt().coerceIn(3, 7)
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
                                svgLoader = svgImageLoader
                            )
                        }
                    }
                }
            }
        } else {
            // ── MEDIUM, LARGE, OR TALL VERTICAL LAYOUT (2x2, 3x2, 4x2, etc.) ──────
            // Huge Hero Animated Icon and balanced vertical flow without bottom-snapping
            val heroIconSize = when {
                isLarge  -> (h * 0.42f).coerceIn(90.dp, 130.dp)
                isMedium -> (h * 0.36f).coerceIn(60.dp, 76.dp)
                else     -> (w * 0.36f).coerceIn(48.dp, 66.dp)
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Upper Section: Prominent Hero Icon + Detailed Conditions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isLarge) 16.dp else 10.dp)
                ) {
                    MeteoconAnimatedIcon(
                        slug = state.conditionSlug,
                        modifier = Modifier.size(heroIconSize)
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = state.temperatureDisplay(metric),
                                color = contentColor,
                                fontSize = if (isLarge) 38.sp else if (isMedium) 26.sp else 20.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-1).sp,
                                maxLines = 1
                            )

                            if (state.locationName.isNotEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = if (isLarge) 5.dp else 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = effectiveAccent,
                                        modifier = Modifier.size(if (isLarge) 12.dp else 9.dp)
                                    )
                                    Text(
                                        text = state.locationName.uppercase(),
                                        color = subColor,
                                        fontSize = if (isLarge) 10.sp else 8.sp,
                                        fontWeight = FontWeight.Medium,
                                        letterSpacing = 1.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Text(
                            text = state.conditionLabel.uppercase(),
                            color = effectiveAccent,
                            fontSize = if (isLarge) 12.sp else if (isMedium) 9.5.sp else 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            maxLines = 1
                        )

                        // Vitals row: Wind, Humidity, Today's High/Low
                        val today = state.dailyForecast.firstOrNull()
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            if (today != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(Icons.Default.ArrowUpward, null, tint = effectiveAccent, modifier = Modifier.size(9.dp))
                                    Text(today.maxTempDisplay(metric), color = contentColor, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
                                    Icon(Icons.Default.ArrowDownward, null, tint = subColor, modifier = Modifier.size(9.dp))
                                    Text(today.minTempDisplay(metric), color = subColor, fontSize = 8.sp)
                                }
                            }

                            if (state.windspeedKmh > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Icon(Icons.Default.Air, null, tint = subColor, modifier = Modifier.size(9.dp))
                                    Text("%.0f km/h".format(state.windspeedKmh), color = subColor, fontSize = 7.5.sp)
                                }
                            }

                            if (state.humidity > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Icon(Icons.Default.WaterDrop, null, tint = subColor, modifier = Modifier.size(9.dp))
                                    Text("${state.humidity}%", color = subColor, fontSize = 7.5.sp)
                                }
                            }
                        }
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
                                iconSize = if (isLarge) 28.dp else 22.dp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
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
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
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
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = forecast.minTempDisplay(metric),
                color = subColor,
                fontSize = 7.5.sp,
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
