package com.openlauncher.app.ui.screen

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import com.openlauncher.app.data.AppSettings
import com.openlauncher.app.data.WidgetContent
import com.openlauncher.app.data.telegram.TelegramBot
import com.openlauncher.app.data.ClockStyle
import com.openlauncher.app.data.WeatherProvider
import com.openlauncher.app.data.MapProvider
import com.openlauncher.app.data.MapType
import com.openlauncher.app.data.MapControlScale
import com.openlauncher.app.data.DayNightMode
import com.openlauncher.app.data.computeWidgetMove
import com.openlauncher.app.data.GRID_COLS
import com.openlauncher.app.data.GRID_ROWS
import com.openlauncher.app.data.WidgetBgMode
import com.openlauncher.app.data.WidgetConfig
import com.openlauncher.app.data.widgetContentFor
import com.openlauncher.app.model.NowPlayingState
import com.openlauncher.app.model.WeatherState
import com.openlauncher.app.ui.components.AppPickerDialog
import com.openlauncher.app.ui.components.CarImage
import com.openlauncher.app.ui.components.tapClickable
import com.openlauncher.app.ui.theme.LocalDayMode
import com.openlauncher.app.ui.theme.LocalUiScales
import com.openlauncher.app.ui.theme.effectiveAccent
import com.openlauncher.app.ui.theme.surfaceFrom
import com.openlauncher.app.ui.widget.*
import java.util.Calendar
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.graphics.drawable.toBitmap
import com.openlauncher.app.model.AppInfo
import com.openlauncher.app.util.LocationData
import com.openlauncher.app.util.NetworkSpeedState
import com.openlauncher.app.util.TapSound
import com.openlauncher.app.util.formatBitRateShort
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import android.provider.DocumentsContract
import com.openlauncher.app.data.GalleryEffect
import com.openlauncher.app.data.SpectrumStyle
import com.openlauncher.app.data.withWidgetContent
import com.openlauncher.app.ui.components.ColorPickerDialog

private val WIDGET_RADIUS = RoundedCornerShape(0.dp)

/**
 * Detects a multi-finger tap (2 or 3 fingers down together, then all up quickly
 * with minimal movement) and reports the finger count. Passive — it does not
 * consume events, so single-finger swipes/drags still work.
 */
private suspend fun PointerInputScope.detectMultiFingerTap(onTap: (Int) -> Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val startPos = down.position
        var fingers = 1
        var moved = false
        val startTime = System.currentTimeMillis()
        do {
            val event = awaitPointerEvent()
            val pressed = event.changes.count { it.pressed }
            if (pressed > fingers) fingers = pressed
            if (event.changes.any { (it.position - startPos).getDistance() > 18f }) moved = true
        } while (event.changes.any { it.pressed })
        val elapsed = System.currentTimeMillis() - startTime
        if (!moved && fingers in 2..3 && elapsed < 500) {
            onTap(fingers)
        }
    }
}

/** Easter egg: the vehicle name tints with speed. */
private fun speedColor(speedKmh: Float, default: Color): Color = when {
    speedKmh >= 120f -> Color(0xFFE03131)  // red
    speedKmh >= 100f -> Color(0xFFFF8800)  // orange
    speedKmh >= 50f  -> Color(0xFF33AA55)  // green
    else             -> default
}

private data class WidgetTypeInfo(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val description: String
)

private val ALL_WIDGET_TYPES = listOf(
    WidgetTypeInfo("CLOCK",         "CLOCK",         Icons.Default.AccessTime,    "Time & date"),
    WidgetTypeInfo("WEATHER",       "WEATHER",       Icons.Default.Cloud,         "Current conditions"),
    WidgetTypeInfo("NOW_PLAYING",   "NOW PLAYING",   Icons.Default.MusicNote,     "Media controls"),
    WidgetTypeInfo("TELEMETRY",     "COMPASS",       Icons.Default.Explore,       "Speed & heading"),
    WidgetTypeInfo("VOLUME",        "VOLUME",        Icons.AutoMirrored.Filled.VolumeUp, "Volume controls & mute"),
    WidgetTypeInfo("QUICK_ACTIONS", "QUICK ACTIONS", Icons.Default.Widgets,       "System toggles & night mode"),
    WidgetTypeInfo("ALTIMETER",     "ALTIMETER",     Icons.Default.FlightTakeoff, "Roll, pitch & altitude"),
    WidgetTypeInfo("SPEEDOMETER",   "SPEED",         Icons.Default.Speed,         "GPS speed"),
    WidgetTypeInfo("VITALS",        "VITALS",        Icons.Default.Dns,           "Head Unit Health / Vitals"),
    WidgetTypeInfo("TRIP_TRACKER",  "TRIP TRACKER",  Icons.Default.Map,           "Trip logs & stats"),
    WidgetTypeInfo("SOUNDBOARD",    "SOUNDBOARD",    Icons.Default.Piano,         "Custom sound pads"),
    WidgetTypeInfo("APP_SHORTCUTS", "APP SHORTCUTS", Icons.Default.Apps,          "Quick app launch pad"),
    WidgetTypeInfo("OBD",           "OBD2",          Icons.Default.Bluetooth,     "Live engine & sensor telemetry"),
    WidgetTypeInfo("NETWORK_SPEED", "NETWORK SPEED", Icons.Default.SwapVert,      "Upload & download speed"),
    WidgetTypeInfo("SPECTRUM",      "SPECTRUM",      Icons.Default.GraphicEq,     "Live sound spectrum"),
    WidgetTypeInfo("MAP",           "MAP",           Icons.Default.Map,           "Google map with live position")
)

private fun canAddWidget(settings: AppSettings, layout: List<WidgetConfig>): Boolean {
    val visibleIds = buildSet {
        if (settings.showClock) add("CLOCK")
        if (settings.showWeather) add("WEATHER")
        if (settings.showNowPlaying) add("NOW_PLAYING")
        if (settings.showTelemetry) add("TELEMETRY")
        if (settings.showAltimeter) add("ALTIMETER")
        if (settings.showSpeedometer) add("SPEEDOMETER")
        if (settings.showVitals) add("VITALS")
        if (settings.showTripTracker) add("TRIP_TRACKER")
        if (settings.showSoundboard) add("SOUNDBOARD")
        if (settings.showVolume) add("VOLUME")
        if (settings.showQuickActions) add("QUICK_ACTIONS")
        if (settings.showAppShortcuts) add("APP_SHORTCUTS")
        if (settings.showObd) add("OBD")
        if (settings.showNetworkSpeed) add("NETWORK_SPEED")
        if (settings.showSpectrum) add("SPECTRUM")
        if (settings.showMap) add("MAP")
    }
    val activeWidgets = layout.filter { it.enabled && it.id in visibleIds }
    val occupied = buildSet<Pair<Int, Int>> {
        activeWidgets.forEach { w ->
            for (dx in 0 until w.spanX) for (dy in 0 until w.spanY) add(w.gridX + dx to w.gridY + dy)
        }
    }
    val hasFreeCell = (0 until com.openlauncher.app.data.GRID_ROWS).any { r ->
        (0 until com.openlauncher.app.data.GRID_COLS).any { c -> (c to r) !in occupied }
    }
    // Also true if any active widget spans >1 cell and can be shrunk to make room
    val hasShrinkable = activeWidgets.any { it.spanX * it.spanY > 1 }
    return hasFreeCell || hasShrinkable
}

@Composable
private fun HomeWidgetGrid(
    settings: AppSettings,
    content: WidgetContent,
    pageLayout: List<WidgetConfig>,
    gap: Dp,
    accent: Color,
    widgetBg: Color,
    widgetBorder: Color,
    headerTextColor: Color,
    isDayMode: Boolean,
    page: Int,
    editMode: Boolean,
    isScreenBlanked: Boolean,
    weather: WeatherState?,
    nowPlaying: NowPlayingState?,
    location: LocationData?,
    bearing: Float,
    isOnline: Boolean,
    installedApps: List<AppInfo>,
    networkSpeed: NetworkSpeedState,
    soundSpectrum: FloatArray,
    soundVisualizerAvailable: Boolean,
    soundVisualizerHasSignal: Boolean,
    hardwareRadio: com.openlauncher.app.viewmodel.LauncherViewModel.HardwareRadioState?,
    isWeatherRefreshing: Boolean,
    onRefreshWeather: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onLaunchCarPlay: () -> Unit,
    onLaunchAndroidAuto: () -> Unit,
    onTapNowPlaying: () -> Unit,
    onLaunchHardwareRadio: () -> Unit,
    onStopHardwareRadio: () -> Unit,
    onRadioSeekUp: () -> Unit,
    onRadioSeekDown: () -> Unit,
    onRadioCycleFm: () -> Unit,
    onRadioSwitchAm: () -> Unit,
    onRadioTune: (String, Float) -> Unit,
    onAssignRadio: () -> Unit,
    onLaunchApp: (String) -> Unit,
    onToggleDayNight: (() -> Unit)?,
    onToggleScreenBlanked: () -> Unit,
    onToggleQuickActionButton: (String) -> Unit,
    onToggleVitalsElement: (String) -> Unit,
    onToggleObdMetric: (String) -> Unit,
    onToggleWidgetTitle: (String) -> Unit,
    onZeroAltimeter: (Float, Float) -> Unit,
    onResetAltimeterZero: () -> Unit,
    onSelectObdAdapter: (String, String) -> Unit,
    onSetWidgetBgColor: (String, Long?) -> Unit,
    onSetWidgetBgMode: (String, WidgetBgMode) -> Unit,
    onSetAllWidgetsBgColor: (Long?) -> Unit,
    onSetAllWidgetsBgMode: (WidgetBgMode) -> Unit,
    onSetMapType: (com.openlauncher.app.data.MapType) -> Unit,
    onToggleMapTraffic: () -> Unit,
    onUpdateSoundPad: (Int, com.openlauncher.app.data.SoundPadConfig) -> Unit,
    onSetClockStyle: (ClockStyle) -> Unit,
    onSetVitalsAsBars: (Boolean) -> Unit,
    onSetSpeedometerDigitalOnly: (Boolean) -> Unit,
    onSetSpeedometerAnimals: (Boolean) -> Unit,
    onSetAppShortcutCount: (Int) -> Unit,
    onSetSoundboardButtonCount: (Int) -> Unit,
    onMoveWidget: (String, Int, Int) -> Unit,
    onRemoveWidget: (String) -> Unit,
    onSetAppShortcutPackage: (Int, String) -> Unit,
    onOpenContextMenu: (String) -> Unit,
    onOpenResize: (String) -> Unit,
    onOpenColorPicker: (String) -> Unit,
    onPickAppForSlot: (Int) -> Unit,
    onOpenQuickActionsConfig: () -> Unit,
    onOpenVitalsConfig: () -> Unit,
    obdPickRequested: Boolean = false,
    onObdPickHandled: () -> Unit = {},
    glareSweep: Animatable<Float, AnimationVector1D>,
    glareAngle: State<Float>,
    glareEnabled: Boolean,
    glareIntensity: Float
) {
        // ── Widget Grid ─────────────────────────────────────────────────────
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(gap)
        ) {
            val cellW = (maxWidth  - gap * (GRID_COLS - 1)) / GRID_COLS
            val cellH = (maxHeight - gap * (GRID_ROWS - 1)) / GRID_ROWS
            val density = LocalDensity.current
            val cellStepXPx = with(density) { (cellW + gap).toPx() }
            val cellStepYPx = with(density) { (cellH + gap).toPx() }

            // WEATHER stays in the set even with no data: the commit path
            // (LauncherViewModel.moveWidgetConfig) computes against settings flags
            // only, so dropping it here would make the drop ghost and the committed
            // layout disagree. With no data the cell renders fully transparent.
            val visibleIds = buildSet {
                if (settings.showClock) add("CLOCK")
                if (settings.showWeather) add("WEATHER")
                if (settings.showNowPlaying) add("NOW_PLAYING")
                if (settings.showTelemetry) add("TELEMETRY")
                if (settings.showAltimeter) add("ALTIMETER")
                if (settings.showSpeedometer) add("SPEEDOMETER")
                if (settings.showVitals) add("VITALS")
                if (settings.showTripTracker) add("TRIP_TRACKER")
                if (settings.showSoundboard) add("SOUNDBOARD")
                if (settings.showVolume) add("VOLUME")
                if (settings.showQuickActions) add("QUICK_ACTIONS")
                if (settings.showAppShortcuts) add("APP_SHORTCUTS")
                if (settings.showObd) add("OBD")
                if (settings.showNetworkSpeed) add("NETWORK_SPEED")
                if (settings.showSpectrum) add("SPECTRUM")
        if (settings.showMap) add("MAP")
                if (settings.showMap) add("MAP")
            }

            // Keep only visible widgets exactly as configured in settings, allowing explicit resizing to dictate layout
            val visible = pageLayout.filter { it.enabled && it.id in visibleIds }
            val rendered = visible

            // ── Drag state ───────────────────────────────────────────────────
            var draggingId   by remember { mutableStateOf<String?>(null) }
            var dragOffsetPx by remember { mutableStateOf(Offset.Zero) }

            // Compute snap target for the widget being dragged (uses original spanX)
            val draggingOriginal = if (draggingId != null) visible.find { it.id == draggingId } else null
            val targetGridX = draggingOriginal?.let {
                (it.gridX + (dragOffsetPx.x / cellStepXPx).roundToInt()).coerceIn(0, GRID_COLS - it.spanX)
            }
            val targetGridY = draggingOriginal?.let {
                (it.gridY + (dragOffsetPx.y / cellStepYPx).roundToInt()).coerceIn(0, GRID_ROWS - it.spanY)
            }

            // Compute proposed layout (push preview) while dragging
            val proposedLayout = if (draggingOriginal != null && targetGridX != null && targetGridY != null)
                computeWidgetMove(visible, draggingOriginal.id, targetGridX, targetGridY)
            else null

            // Drop ghost — rendered before widgets so it appears beneath them
            if (draggingOriginal != null && targetGridX != null && targetGridY != null) {
                val gX = (cellW + gap) * targetGridX
                val gY = (cellH + gap) * targetGridY
                val gW = cellW * draggingOriginal.spanX + gap * (draggingOriginal.spanX - 1)
                val gH = cellH * draggingOriginal.spanY + gap * (draggingOriginal.spanY - 1)
                Box(
                    modifier = Modifier
                        .absoluteOffset(x = gX, y = gY)
                        .size(gW, gH)
                        .background(accent.copy(alpha = 0.08f))
                        .border(1.dp, accent.copy(alpha = 0.5f), WIDGET_RADIUS)
                )
            }

            // Displacement ghosts — show where pushed widgets will land
            if (proposedLayout != null && draggingOriginal != null) {
                proposedLayout
                    .filter { it.id != draggingOriginal.id }
                    .forEach { proposed ->
                        val original = visible.find { it.id == proposed.id } ?: return@forEach
                        if (proposed.gridX != original.gridX || proposed.gridY != original.gridY) {
                            val dX = (cellW + gap) * proposed.gridX
                            val dY = (cellH + gap) * proposed.gridY
                            val dW = cellW * proposed.spanX + gap * (proposed.spanX - 1)
                            val dH = cellH * proposed.spanY + gap * (proposed.spanY - 1)
                            Box(
                                modifier = Modifier
                                    .absoluteOffset(x = dX, y = dY)
                                    .size(dW, dH)
                                    .border(1.dp, Color.White.copy(alpha = 0.25f), WIDGET_RADIUS)
                            )
                        }
                    }
            }

            if (rendered.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (page == 0) "NO WIDGETS" else "PAGE ${page + 1} IS EMPTY",
                        color = headerTextColor.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Swipe left, right, up or down to switch pages",
                        color = headerTextColor.copy(alpha = 0.4f),
                        fontSize = 9.sp,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            rendered.forEach { w ->
                val xOff   = (cellW + gap) * w.gridX
                val yOff   = (cellH + gap) * w.gridY
                val safeSpanX = w.spanX.coerceAtLeast(1)
                val safeSpanY = w.spanY.coerceAtLeast(1)
                val width  = cellW * safeSpanX + gap * (safeSpanX - 1)
                val height = cellH * safeSpanY + gap * (safeSpanY - 1)

                val label = when (w.id) {
                    "CLOCK"         -> clockTimeLabel(Calendar.getInstance())
                    "WEATHER"       -> "WEATHER"
                    "NOW_PLAYING"   -> "NOW PLAYING"
                    "TELEMETRY"     -> "COMPASS"
                    "ALTIMETER"     -> "ALTIMETER"
                    "SPEEDOMETER"   -> "SPEED"
                    "TRIP_TRACKER"  -> "TRIP"
                    "SOUNDBOARD"    -> "SOUND"
                    "VOLUME"        -> "VOLUME"
                    "QUICK_ACTIONS" -> "QUICK ACTIONS"
                    "APP_SHORTCUTS" -> "APP SHORTCUTS"
                    "OBD"           -> "OBD2"
                    "SPECTRUM"      -> "SPECTRUM"
                    "MAP"           -> "MAP"
                    else            -> w.id
                }

                // Original (pre-auto-expand) spanX needed for drag boundary clamping
                val origSpanX  = visible.find { it.id == w.id }?.spanX ?: 1
                val isDragging = draggingId == w.id
                // Weather with no data reserves its cell but draws nothing
                // (still visible in edit mode so it can be moved/removed)
                val isGhost    = w.id == "WEATHER" && weather == null && !editMode
                val dragDpX    = if (isDragging) with(density) { dragOffsetPx.x.toDp() } else 0.dp
                val dragDpY    = if (isDragging) with(density) { dragOffsetPx.y.toDp() } else 0.dp

                // Widget background: default (fixed), follow-theme, or per-widget custom
                val isCustomBg  = w.bgMode == WidgetBgMode.CUSTOM && w.customBgColor != null
                val followTheme = w.bgMode == WidgetBgMode.FOLLOW_THEME
                val followThemeColor = if (settings.dayNightMode == DayNightMode.CUSTOM) {
                    surfaceFrom(Color(settings.backgroundColor))
                } else {
                    Color(if (isDayMode) settings.tileColorLight else settings.tileColorDark)
                }
                val effectiveBg = when {
                    isGhost     -> Color.Transparent
                    followTheme -> followThemeColor.copy(alpha = settings.tileAlpha)
                    isCustomBg  -> Color(w.customBgColor!!).copy(alpha = settings.tileAlpha)
                    else        -> widgetBg
                }

                val effectiveBorder = when {
                    isDragging  -> accent
                    editMode    -> accent.copy(alpha = 0.55f)
                    isGhost     -> Color.Transparent
                    isCustomBg  -> Color(w.customBgColor!!).copy(alpha = 0.40f)
                    else        -> widgetBorder
                }

                // Follow-theme and custom tiles decide their content color from tile luminance
                val widgetIsLight = when {
                    isCustomBg  -> Color(w.customBgColor!!).luminance() > 0.50f
                    followTheme -> followThemeColor.luminance() > 0.50f
                    else        -> isDayMode
                }
                val widgetDayMode = widgetIsLight

                val customContentColor: Color? = if (isCustomBg || followTheme) {
                    if (widgetIsLight) Color(0xFF111111) else Color(0xFFEEEEEE)
                } else null

                // Ensure accent has high contrast on the widget card
                val accentLum = 0.299f * accent.red + 0.587f * accent.green + 0.114f * accent.blue
                val widgetAccent = if (widgetIsLight && accentLum > 0.65f) {
                    Color(0xFF111111)
                } else if (!widgetIsLight && accentLum < 0.25f) {
                    Color(0xFFEEEEEE)
                } else {
                    accent
                }

                @OptIn(ExperimentalFoundationApi::class)
                Box(
                    modifier = Modifier
                        .absoluteOffset(x = xOff + dragDpX, y = yOff + dragDpY)
                        .size(width, height)
                        .zIndex(if (isDragging) 10f else 0f)
                        .clip(WIDGET_RADIUS)
                        .background(effectiveBg)
                        .border(
                            width = if (isDragging) 2.dp else if (editMode) 1.5.dp else 1.dp,
                            color = effectiveBorder,
                            shape = WIDGET_RADIUS
                        )
                        .then(
                            if (!editMode && w.id != "WEATHER") {
                                Modifier.combinedClickable(
                                    indication        = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick           = {},
                                    onLongClick       = { onOpenContextMenu(w.id) }
                                )
                            } else Modifier
                        )
                ) {
                    WidgetContent(
                        widgetId = w.id, content = content, widgetAccent = widgetAccent,
                        widgetDayMode = widgetDayMode, customContentColor = customContentColor,
                        widgetBackground = effectiveBg,
                        settings = settings, weather = weather, nowPlaying = nowPlaying,
                        location = location, bearing = bearing, isOnline = isOnline,
                        installedApps = installedApps, networkSpeed = networkSpeed,
                        soundSpectrum = soundSpectrum, soundVisualizerAvailable = soundVisualizerAvailable,
                        soundVisualizerHasSignal = soundVisualizerHasSignal, hardwareRadio = hardwareRadio,
                        isWeatherRefreshing = isWeatherRefreshing, editMode = editMode,
                        onRefreshWeather = onRefreshWeather, onPlayPause = onPlayPause, onNext = onNext, onPrev = onPrev,
                        onLaunchCarPlay = onLaunchCarPlay, onLaunchAndroidAuto = onLaunchAndroidAuto,
                        onTapNowPlaying = onTapNowPlaying, onLaunchHardwareRadio = onLaunchHardwareRadio,
                        onStopHardwareRadio = onStopHardwareRadio, onRadioSeekUp = onRadioSeekUp,
                        onRadioSeekDown = onRadioSeekDown, onRadioCycleFm = onRadioCycleFm,
                        onRadioSwitchAm = onRadioSwitchAm, onRadioTune = onRadioTune, onAssignRadio = onAssignRadio,
                        onToggleScreenBlanked = onToggleScreenBlanked, onToggleDayNight = onToggleDayNight,
                        onZeroAltimeter = onZeroAltimeter, onResetAltimeterZero = onResetAltimeterZero,
                        onUpdateSoundPad = onUpdateSoundPad, onLaunchApp = onLaunchApp,
                        onSelectObdAdapter = onSelectObdAdapter, onSetMapType = onSetMapType,
                        onToggleMapTraffic = onToggleMapTraffic, onPickAppForSlot = onPickAppForSlot,
                        onOpenQuickActionsConfig = onOpenQuickActionsConfig, onOpenVitalsConfig = onOpenVitalsConfig,
                        onOpenContextMenu = onOpenContextMenu,
                        obdPickRequested = obdPickRequested, onObdPickHandled = onObdPickHandled
                    )

                    // Label — hide when album art fills the widget background
                    val labelColor = when {
                        isGhost -> Color.Transparent
                        w.id == "NOW_PLAYING" && nowPlaying?.albumArt != null && nowPlaying.title.isNotEmpty() -> Color.Transparent
                        customContentColor != null -> customContentColor.copy(alpha = if (widgetIsLight) 0.65f else 0.45f)
                        widgetDayMode -> Color(0xFF666666)
                        else          -> Color(0xFF4A4A4A)
                    }
                    if (w.isTitleVisible) {
                        Text(
                            text          = label,
                            style         = MaterialTheme.typography.labelSmall,
                            color         = labelColor,
                            letterSpacing = 2.sp,
                            fontSize      = 8.sp,
                            modifier      = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = 10.dp, top = 7.dp)
                        )
                    }

                    // Edit Mode Controls & Drag Overlay
                    if (editMode) {
                        // Drag overlay covering the widget body for immediate touch drag
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(w.id, w.gridX, w.gridY) {
                                    detectDragGestures(
                                        onDragStart = { _ ->
                                            draggingId   = w.id
                                            dragOffsetPx = Offset.Zero
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffsetPx += dragAmount
                                        },
                                        onDragEnd = {
                                            val newX = (w.gridX + (dragOffsetPx.x / cellStepXPx).roundToInt())
                                                .coerceIn(0, GRID_COLS - origSpanX)
                                            val newY = (w.gridY + (dragOffsetPx.y / cellStepYPx).roundToInt())
                                                .coerceIn(0, GRID_ROWS - w.spanY)
                                            if (newX != w.gridX || newY != w.gridY) {
                                                onMoveWidget(w.id, newX, newY)
                                            }
                                            draggingId   = null
                                            dragOffsetPx = Offset.Zero
                                        },
                                        onDragCancel = {
                                            draggingId   = null
                                            dragOffsetPx = Offset.Zero
                                        }
                                    )
                                }
                        )

                        // Top-Left: Instant Toggle Widget Title Visibility [👁]
                        Box(
                            modifier = Modifier
                                .zIndex(5f)
                                .align(Alignment.TopStart)
                                .padding(4.dp)
                                .size(28.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xE6000000))
                                .border(1.dp, if (w.isTitleVisible) accent.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                                .clickable { onToggleWidgetTitle(w.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (w.isTitleVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (w.isTitleVisible) "Hide title" else "Show title",
                                tint = if (w.isTitleVisible) accent else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        // Top-Right: Instant Remove Widget [✕] button
                        Box(
                            modifier = Modifier
                                .zIndex(5f)
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(28.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xE6000000))
                                .border(1.dp, Color(0xFFE03131).copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                .clickable { onRemoveWidget(w.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove widget",
                                tint = Color(0xFFFF6B6B),
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        // Bottom-Left: Instant Widget Background Glass Color [🎨] button
                        Box(
                            modifier = Modifier
                                .zIndex(5f)
                                .align(Alignment.BottomStart)
                                .padding(4.dp)
                                .size(28.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xE6000000))
                                .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                                .clickable { onOpenColorPicker(w.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Widget background color",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        // Bottom-Right: Instant Resize Widget [⛶] button
                        Box(
                            modifier = Modifier
                                .zIndex(5f)
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .size(28.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xE6000000))
                                .border(1.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                .clickable { onOpenResize(w.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AspectRatio,
                                contentDescription = "Resize widget",
                                tint = accent,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            // Single global glare — one light source sweeping across the whole
            // widget area, masked to the tiles (passing glint, random 25–60° angle).
            if (glareEnabled && !editMode && draggingId == null && rendered.isNotEmpty()) {
                val glareRects = rendered
                    .filterNot { it.id == "WEATHER" && weather == null }
                    .map { w ->
                        val xDp = (cellW + gap) * w.gridX
                        val yDp = (cellH + gap) * w.gridY
                        val wDp = cellW * w.spanX + gap * (w.spanX - 1)
                        val hDp = cellH * w.spanY + gap * (w.spanY - 1)
                        with(density) {
                            Rect(xDp.toPx(), yDp.toPx(), (xDp + wDp).toPx(), (yDp + hDp).toPx())
                        }
                    }
                GlareOverlay(
                    sweep     = { glareSweep.value },
                    angle     = glareAngle,
                    intensity = glareIntensity,
                    rects     = glareRects,
                    modifier  = Modifier.fillMaxSize()
                )
            }
        }
}

@Composable
private fun WidgetContent(
    widgetId: String,
    content: com.openlauncher.app.data.WidgetContent,
    widgetAccent: Color,
    widgetDayMode: Boolean,
    customContentColor: Color?,
    widgetBackground: Color,
    settings: AppSettings,
    weather: WeatherState?,
    nowPlaying: NowPlayingState?,
    location: LocationData?,
    bearing: Float,
    isOnline: Boolean,
    installedApps: List<AppInfo>,
    networkSpeed: NetworkSpeedState,
    soundSpectrum: FloatArray,
    soundVisualizerAvailable: Boolean,
    soundVisualizerHasSignal: Boolean,
    hardwareRadio: com.openlauncher.app.viewmodel.LauncherViewModel.HardwareRadioState?,
    isWeatherRefreshing: Boolean,
    editMode: Boolean,
    onRefreshWeather: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onLaunchCarPlay: () -> Unit,
    onLaunchAndroidAuto: () -> Unit,
    onTapNowPlaying: () -> Unit,
    onLaunchHardwareRadio: () -> Unit,
    onStopHardwareRadio: () -> Unit,
    onRadioSeekUp: () -> Unit,
    onRadioSeekDown: () -> Unit,
    onRadioCycleFm: () -> Unit,
    onRadioSwitchAm: () -> Unit,
    onRadioTune: (String, Float) -> Unit,
    onAssignRadio: () -> Unit,
    onToggleScreenBlanked: () -> Unit,
    onToggleDayNight: (() -> Unit)?,
    onZeroAltimeter: (Float, Float) -> Unit,
    onResetAltimeterZero: () -> Unit,
    onUpdateSoundPad: (Int, com.openlauncher.app.data.SoundPadConfig) -> Unit,
    onLaunchApp: (String) -> Unit,
    onSelectObdAdapter: (String, String) -> Unit,
    onSetMapType: (com.openlauncher.app.data.MapType) -> Unit,
    onToggleMapTraffic: () -> Unit,
    onPickAppForSlot: (Int) -> Unit,
    onOpenQuickActionsConfig: () -> Unit,
    onOpenVitalsConfig: () -> Unit,
    onOpenContextMenu: (String) -> Unit,
    obdPickRequested: Boolean = false,
    onObdPickHandled: () -> Unit = {}
) {
    when (widgetId) {
        "CLOCK" -> ClockWidget(
            style              = content.clockStyle,
            accent             = widgetAccent,
            isDayMode          = widgetDayMode,
            customContentColor = customContentColor,
            modifier           = Modifier.fillMaxSize()
        )
        "WEATHER" -> WeatherWidget(
            state              = weather,
            accent             = widgetAccent,
            metric             = settings.unitSystem.name == "METRIC",
            isDayMode          = widgetDayMode,
            customContentColor = customContentColor,
            isOnline           = isOnline,
            forecastDaysCount  = settings.weatherForecastDays,
            showForecast       = content.showWeatherForecast,
            isRefreshing       = isWeatherRefreshing,
            onRefresh          = onRefreshWeather,
            onLongClick        = { onOpenContextMenu("WEATHER") },
            isEditing          = editMode,
            galleryEnabled     = settings.galleryFallbackEnabled,
            galleryDirectoryUri = settings.galleryDirectoryUri,
            galleryIntervalMs  = settings.galleryIntervalMs,
            galleryTransitionMs = settings.galleryTransitionMs,
            galleryEffect      = settings.galleryEffect,
            modifier           = Modifier.fillMaxSize()
        )
        "NOW_PLAYING" -> NowPlayingWidget(
            state               = nowPlaying,
            accent              = widgetAccent,
            carPlayPackage      = settings.carPlayPackage,
            androidAutoPackage  = settings.androidAutoPackage,
            onPlayPause         = onPlayPause,
            onNext              = onNext,
            onPrev              = onPrev,
            onLaunchCarPlay     = onLaunchCarPlay,
            onLaunchAndroidAuto = onLaunchAndroidAuto,
            onTapToOpenApp      = onTapNowPlaying,
            modifier            = Modifier.fillMaxSize(),
            isEditing           = editMode,
            isDayMode           = widgetDayMode,
            customContentColor  = customContentColor,
            hardwareRadio         = hardwareRadio,
            onLaunchHardwareRadio = onLaunchHardwareRadio,
            onStopHardwareRadio   = onStopHardwareRadio,
            onRadioSeekUp         = onRadioSeekUp,
            onRadioSeekDown       = onRadioSeekDown,
            onRadioCycleFm        = onRadioCycleFm,
            onRadioSwitchAm       = onRadioSwitchAm,
            onRadioTune           = onRadioTune,
            onAssignRadio         = onAssignRadio,
            showRadioBands        = settings.showRadioBands,
            logoScale             = settings.nowPlayingLogoScale,
            logoDim               = settings.nowPlayingLogoDim,
            artBlend              = settings.nowPlayingArtBlend,
            widgetBackground      = widgetBackground,
            assignedRadioPackage  = settings.radioPackage
        )
        "TELEMETRY" -> TelemetryWidget(
            location           = location,
            bearing            = (bearing + settings.compassOffset + 360f) % 360f,
            accent             = widgetAccent,
            isDayMode          = widgetDayMode,
            customContentColor = customContentColor,
            modifier           = Modifier.fillMaxSize()
        )
        "ALTIMETER" -> AltimeterWidget(
            location           = location,
            isMetric           = settings.unitSystem == com.openlauncher.app.data.UnitSystem.METRIC,
            accent             = widgetAccent,
            isDayMode          = widgetDayMode,
            customContentColor = customContentColor,
            rollOffset         = settings.altimeterRollOffset,
            pitchOffset        = settings.altimeterPitchOffset,
            onZero             = onZeroAltimeter,
            onResetZero        = onResetAltimeterZero,
            modifier           = Modifier.fillMaxSize()
        )
        "SPEEDOMETER" -> SpeedometerWidget(
            location           = location,
            isMetric           = settings.unitSystem == com.openlauncher.app.data.UnitSystem.METRIC,
            accent             = widgetAccent,
            isDayMode          = widgetDayMode,
            digitalOnly        = content.speedometerDigitalOnly,
            animalsMode        = content.speedometerAnimals,
            customContentColor = customContentColor,
            modifier           = Modifier.fillMaxSize()
        )
        "VITALS" -> VitalsWidget(
            accent             = widgetAccent,
            isDayMode          = widgetDayMode,
            asBars             = content.vitalsAsBars,
            vitalsElements     = content.vitalsElements,
            customContentColor = customContentColor,
            modifier           = Modifier.fillMaxSize()
        )
        "TRIP_TRACKER" -> TripTrackerWidget(
            location           = location,
            isMetric           = settings.unitSystem == com.openlauncher.app.data.UnitSystem.METRIC,
            accent             = widgetAccent,
            isDayMode          = widgetDayMode,
            customContentColor = customContentColor,
            modifier           = Modifier.fillMaxSize()
        )
        "SOUNDBOARD" -> SoundboardWidget(
            pads               = content.soundboardPads,
            accent             = widgetAccent,
            isDayMode          = widgetDayMode,
            customContentColor = customContentColor,
            buttonCount        = content.soundboardButtonCount,
            isEditing          = editMode,
            onUpdatePad        = onUpdateSoundPad,
            modifier           = Modifier.fillMaxSize()
        )
        "VOLUME" -> VolumeWidget(
            accent             = widgetAccent,
            isDayMode          = widgetDayMode,
            customContentColor = customContentColor,
            modifier           = Modifier.fillMaxSize()
        )
        "QUICK_ACTIONS" -> QuickActionsWidget(
            accent                = widgetAccent,
            isDayMode             = widgetDayMode,
            dayNightMode          = settings.dayNightMode,
            customContentColor    = customContentColor,
            enabledActions        = content.quickActionButtons,
            onToggleScreenBlanked = onToggleScreenBlanked,
            onToggleDayNight      = onToggleDayNight,
            onOpenConfig          = onOpenQuickActionsConfig,
            modifier              = Modifier.fillMaxSize()
        )
        "APP_SHORTCUTS" -> AppShortcutsWidget(
            packages           = content.appWidgetPackages,
            installedApps      = installedApps,
            accent             = widgetAccent,
            isDayMode          = widgetDayMode,
            customContentColor = customContentColor,
            shortcutCount      = content.appShortcutCount,
            iconScale          = content.appShortcutIconScale,
            onLaunchApp        = onLaunchApp,
            onPickAppForSlot   = onPickAppForSlot,
            modifier           = Modifier.fillMaxSize()
        )
        "OBD" -> ObdWidget(
            adapterAddress     = settings.obdDeviceAddress,
            adapterName        = settings.obdDeviceName,
            metric             = settings.unitSystem == com.openlauncher.app.data.UnitSystem.METRIC,
            accent             = widgetAccent,
            isDayMode          = widgetDayMode,
            customContentColor = customContentColor,
            isEditing          = editMode,
            onSelectAdapter    = onSelectObdAdapter,
            fallbackImageUri   = settings.obdFallbackImageUri,
            enabledMetrics     = content.obdMetrics,
            pickRequested      = obdPickRequested,
            onPickRequestHandled = onObdPickHandled,
            modifier           = Modifier.fillMaxSize()
        )
        "NETWORK_SPEED" -> NetworkSpeedWidget(
            speed              = networkSpeed,
            accent             = widgetAccent,
            isDayMode          = widgetDayMode,
            customContentColor = customContentColor,
            modifier           = Modifier.fillMaxSize()
        )
        "SPECTRUM" -> SpectrumWidget(
            spectrum          = soundSpectrum,
            available         = soundVisualizerAvailable,
            hasSignal         = soundVisualizerHasSignal,
            barCount          = content.spectrumBarCount,
            style             = content.spectrumStyle,
            mirror            = content.spectrumMirror,
            gain              = content.spectrumGain,
            color             = content.spectrumColor?.let { Color(it) } ?: widgetAccent,
            fallbackImageUri  = content.spectrumFallbackImageUri,
            isDayMode         = widgetDayMode,
            modifier          = Modifier.fillMaxSize()
        )
        "MAP" -> MapWidget(
            location          = location,
            bearing           = bearing,
            mapProvider       = settings.mapProvider,
            mapType           = settings.mapWidgetType,
            showTraffic       = settings.mapShowTraffic,
            isOnline          = isOnline,
            accent            = widgetAccent,
            isDayMode         = widgetDayMode,
            editMode          = editMode,
            controlScale      = settings.mapControlScale.multiplier,
            onSetMapType      = onSetMapType,
            onToggleTraffic   = onToggleMapTraffic,
            onOpenSettings    = { onOpenContextMenu("MAP") },
            modifier          = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun HomeScreen(
    settings: AppSettings,
    weather: WeatherState?,
    nowPlaying: NowPlayingState?,
    location: LocationData?,
    bearing: Float,
    isWifi: Boolean,
    isData: Boolean,
    isOnline: Boolean = true,
    isBluetoothOn: Boolean = false,
    isBluetoothConnected: Boolean = false,
    isDayMode: Boolean = false,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onLaunchCarPlay: () -> Unit,
    onLaunchAndroidAuto: () -> Unit,
    onAssignCarPlay: () -> Unit,
    onAssignAndroidAuto: () -> Unit,
    onClearCarPlay: () -> Unit,
    onClearAndroidAuto: () -> Unit,
    onAssignPip: () -> Unit,
    onClearPip: () -> Unit,
    onLaunchPip: () -> Unit,
    onTapNowPlaying: () -> Unit,
    onUpdateWidget: (id: String, spanX: Int, spanY: Int) -> Unit,
    onMoveWidget: (id: String, gridX: Int, gridY: Int) -> Unit,
    onAddWidget: (id: String) -> Unit,
    onRemoveWidget: (id: String) -> Unit,
    widgetLayoutOverride: List<WidgetConfig>? = null,
    page: Int = 0,
    onSetClockStyle: (ClockStyle) -> Unit,
    onSetVitalsAsBars: (Boolean) -> Unit = {},
    onSetSpeedometerDigitalOnly: (Boolean) -> Unit = {},
    onSetSpeedometerAnimals: (Boolean) -> Unit = {},
    onSetAppShortcutCount: (Int) -> Unit = {},
    onSetAppShortcutIconScale: (Float) -> Unit = {},
    onSetSoundboardButtonCount: (Int) -> Unit = {},
    onUpdateSoundPad: (index: Int, pad: com.openlauncher.app.data.SoundPadConfig) -> Unit = { _, _ -> },
    hardwareRadio: com.openlauncher.app.viewmodel.LauncherViewModel.HardwareRadioState? = null,
    onLaunchHardwareRadio: () -> Unit = {},
    onStopHardwareRadio: () -> Unit = {},
    onRadioSeekUp: () -> Unit = {},
    onRadioSeekDown: () -> Unit = {},
    onRadioCycleFm: () -> Unit = {},
    onRadioSwitchAm: () -> Unit = {},
    onRadioTune: (band: String, freq: Float) -> Unit = { _, _ -> },
    onAssignRadio: () -> Unit = {},
    isScreenBlanked: Boolean = false,
    onToggleScreenBlanked: () -> Unit = {},
    installedApps: List<AppInfo> = emptyList(),
    onLaunchApp: (String) -> Unit = {},
    onSetWidgetBgColor: (id: String, color: Long?) -> Unit = { _, _ -> },
    onSetWidgetBgMode: (id: String, mode: WidgetBgMode) -> Unit = { _, _ -> },
    onSetAppShortcutPackage: (slotIndex: Int, packageName: String) -> Unit = { _, _ -> },
    onToggleDayNight: (() -> Unit)? = null,
    onToggleQuickActionButton: (String) -> Unit = {},
    onResetQuickActionButtons: () -> Unit = {},
    onToggleVitalsElement: (String) -> Unit = {},
    onResetVitalsElements: () -> Unit = {},
    onToggleObdMetric: (String) -> Unit = {},
    onResetObdMetrics: () -> Unit = {},
    onToggleWidgetTitle: (id: String) -> Unit = {},
    onZeroAltimeter: (Float, Float) -> Unit = { _, _ -> },
    onResetAltimeterZero: () -> Unit = {},
    onSetAdvancedWeatherIcons: (Boolean) -> Unit = {},
    onSetShowWeatherForecast: (Boolean) -> Unit = {},
    weatherProvider: WeatherProvider = WeatherProvider.OPEN_METEO,
    weatherUseGps: Boolean = true,
    weatherCustomLocation: String = "Athens",
    weatherForecastDays: Int = 5,
    onSetWeatherProvider: (WeatherProvider) -> Unit = {},
    onSetWeatherUseGps: (Boolean) -> Unit = {},
    onSetWeatherCustomLocation: (String) -> Unit = {},
    onSetWeatherForecastDays: (Int) -> Unit = {},
    isWeatherRefreshing: Boolean = false,
    onRefreshWeather: () -> Unit = {},
    onSelectObdAdapter: (address: String, name: String) -> Unit = { _, _ -> },
    carImageUri: String = "",
    carRestingVisible: Boolean = false,
    networkSpeed: NetworkSpeedState = NetworkSpeedState(),
    soundSpectrum: FloatArray = FloatArray(0),
    soundVisualizerAvailable: Boolean = false,
    soundVisualizerHasSignal: Boolean = false,
    onSwitchProfile: (Int) -> Unit = {},
    profileSwitchedAt: Long = 0L,
    onSetAllWidgetsBgColor: (Long?) -> Unit = { _ -> },
    onSetAllWidgetsBgMode: (WidgetBgMode) -> Unit = { _ -> },
    onSetMapType: (com.openlauncher.app.data.MapType) -> Unit = { _ -> },
    onToggleMapTraffic: () -> Unit = {},
    onSetMapProvider: (MapProvider) -> Unit = { _ -> },
    onSetMapControlScale: (MapControlScale) -> Unit = { _ -> },
    onUpdateSettings: (AppSettings.() -> AppSettings) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val accent       = settings.effectiveAccent()
    val content      = settings.widgetContentFor(page)
    val pageLayout   = widgetLayoutOverride ?: settings.widgetLayout
    val gap          = 6.dp
    val hasWallpaper = settings.wallpaperUri.isNotEmpty()
    val widgetBg     = when {
        isDayMode    -> Color(0xFFFFFFFF)
        hasWallpaper -> Color(0xCC000000)
        else         -> Color.Black.copy(alpha = 0.35f)
    }
    val widgetBorder = when {
        isDayMode    -> Color(0xFFCCCCCC)
        hasWallpaper -> Color(0x22FFFFFF)
        else         -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
    }
    val headerTextColor   = if (isDayMode) Color(0xFF111111) else accent
    val statusIconColor   = if (isDayMode) Color(0xFF444444) else Color(0xFF666666)
    val controlIconColor  = if (isDayMode) Color(0xFF666666) else Color(0xFF444444)

    // Header title: the driver name is the title unless the car has been
    // renamed from the default. Right after a profile switch the driver name
    // flashes for 5 seconds even when a custom car name exists.
    val profileName = settings.profileNames.getOrElse(settings.activeProfile) { "DRIVER ${settings.activeProfile + 1}" }
    val hasCustomCarName = settings.vehicleName.isNotBlank() &&
            !settings.vehicleName.equals("MY CAR", ignoreCase = true)
    var profileFlash by remember { mutableStateOf(false) }
    LaunchedEffect(profileSwitchedAt) {
        // Flash only within the 5 s window after the switch itself — re-entering
        // the home screen later (page switch, nav) must not re-trigger it.
        if (profileSwitchedAt > 0L) {
            val age = android.os.SystemClock.elapsedRealtime() - profileSwitchedAt
            val remaining = 5_000L - age
            if (remaining > 0L) {
                profileFlash = true
                delay(remaining)
                profileFlash = false
            }
        }
    }
    val headerTitle = when {
        profileFlash     -> profileName
        hasCustomCarName -> settings.vehicleName
        else             -> profileName
    }

    var resizingId           by remember { mutableStateOf<String?>(null) }
    var contextMenuId        by remember { mutableStateOf<String?>(null) }
    var colorPickerWidgetId  by remember { mutableStateOf<String?>(null) }
    var appPickerSlotIndex   by remember { mutableStateOf<Int?>(null) }
    var quickActionsConfigOpen by remember { mutableStateOf(false) }
    var vitalsConfigOpen by remember { mutableStateOf(false) }
    var obdMetricsConfigOpen by remember { mutableStateOf(false) }
    var weatherSettingsOpen by remember { mutableStateOf(false) }
    var mapSettingsOpen by remember { mutableStateOf(false) }
    var spectrumSettingsOpen by remember { mutableStateOf(false) }
    var nowPlayingSettingsOpen by remember { mutableStateOf(false) }
    var gallerySettingsOpen by remember { mutableStateOf(false) }
    var obdPickRequested by remember { mutableStateOf(false) }
    var obdImagePickRequested by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val obdImagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onUpdateSettings { copy(obdFallbackImageUri = it.toString()) }
        }
        obdImagePickRequested = false
    }
    LaunchedEffect(obdImagePickRequested) {
        if (obdImagePickRequested) {
            obdImagePicker.launch(arrayOf("image/*"))
            obdImagePickRequested = false
        }
    }

    val configuration    = LocalConfiguration.current
    val isLandscape      = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var editMode         by remember { mutableStateOf(false) }
    var widgetLibraryOpen by remember { mutableStateOf(false) }
    var profilePickerOpen by remember { mutableStateOf(false) }
    var allWidgetsColorOpen by remember { mutableStateOf(false) }

    // Car "resting" indication beside the vehicle name — soft fade in/out.
    val restingAlpha by animateFloatAsState(
        targetValue = if (carRestingVisible) 1f else 0f,
        animationSpec = tween(600)
    )

    // "Passing glint" glare — one light source sweeping across the whole widget
    // area. Off most of the time, then a single sweep at a random 25–60° angle.
    val glareAngle = remember { mutableStateOf(45f) }
    val glareSweep = remember { Animatable(-1f) } // -1 = resting (fully hidden)
    LaunchedEffect(settings.glareEnabled) {
        if (!settings.glareEnabled) return@LaunchedEffect
        while (true) {
            glareSweep.snapTo(-1f)
            delay(Random.nextLong(6000L, 10000L))       // rest between glints
            glareAngle.value = 25f + Random.nextFloat() * 35f
            glareSweep.animateTo(1f, tween(2600, easing = FastOutSlowInEasing))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(settings.gestureTwoFingerPackage, settings.gestureThreeFingerPackage) {
                detectMultiFingerTap { fingers ->
                    when (fingers) {
                        2 -> if (settings.gestureTwoFingerPackage.isNotEmpty()) onLaunchApp(settings.gestureTwoFingerPackage)
                        3 -> if (settings.gestureThreeFingerPackage.isNotEmpty()) onLaunchApp(settings.gestureThreeFingerPackage)
                    }
                }
            }
    ) {
    Column(modifier = Modifier.fillMaxSize()) {

        // ── Header ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Edit / Done + Add Element icons in front of vehicle name without button box
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (editMode) {
                    val headerActionTint = if (isDayMode && accent.luminance() > 0.5f) Color(0xFF111111) else accent

                    // 1. Done editing icon
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Done editing",
                        tint = headerActionTint,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { TapSound.play(); editMode = false }
                    )

                    Spacer(Modifier.width(10.dp))

                    // 2. Add element icon (moved immediately to the right of Done button, no button face)
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add widget",
                        tint = headerActionTint,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { TapSound.play(); widgetLibraryOpen = true }
                    )

                    // 3. Tint-all-widgets palette — appears (animated) only in edit
                    // mode and only while at least one widget is active.
                    val hasActiveWidgets = pageLayout.any { it.enabled }
                    AnimatedVisibility(
                        visible = hasActiveWidgets,
                        enter = fadeIn(tween(180)) + expandHorizontally(expandFrom = Alignment.Start, animationSpec = tween(180)),
                        exit  = fadeOut(tween(120)) + shrinkHorizontally(shrinkTowards = Alignment.Start, animationSpec = tween(120))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(Modifier.width(10.dp))
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(headerActionTint.copy(alpha = if (isDayMode) 0.10f else 0.14f))
                                    .border(
                                        1.dp,
                                        headerActionTint.copy(alpha = 0.55f),
                                        RoundedCornerShape(7.dp)
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { TapSound.play(); allWidgetsColorOpen = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Tint all widgets",
                                    tint = headerActionTint,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit widgets",
                        tint = controlIconColor,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { TapSound.play(); editMode = true }
                    )
                }

                Spacer(Modifier.width(10.dp))

                Text(
                    text = "|",
                    color = if (isDayMode) Color(0xFFCCCCCC) else Color(0xFF333333),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Light
                )

                Spacer(Modifier.width(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { TapSound.play(); profilePickerOpen = true }
                ) {
                    Text(
                        text          = headerTitle.uppercase(),
                        style         = MaterialTheme.typography.titleLarge,
                        color         = speedColor((location?.speedMps ?: 0f) * 3.6f, headerTextColor),
                        letterSpacing = 3.sp,
                        fontSize      = 14.sp
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Switch driver profile",
                        tint = statusIconColor,
                        modifier = Modifier.size(13.dp)
                    )
                }

                if (carImageUri.isNotEmpty() && restingAlpha > 0.01f) {
                    Spacer(Modifier.width(8.dp))
                    CarImage(
                        carImageUri = carImageUri,
                        alpha = restingAlpha,
                        modifier = Modifier.size(width = 56.dp, height = 38.dp)
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // ── Top-right status icons (Bluetooth, Wi-Fi, Data) ───────────────
            if (settings.showStatusIcons) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                // Bluetooth status
                when {
                    isBluetoothConnected -> Icon(
                        imageVector = Icons.Default.BluetoothConnected,
                        contentDescription = "Bluetooth Connected",
                        tint = accent,
                        modifier = Modifier.size(16.dp)
                    )
                    isBluetoothOn -> Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = "Bluetooth On",
                        tint = statusIconColor,
                        modifier = Modifier.size(16.dp)
                    )
                    else -> Icon(
                        imageVector = Icons.Default.BluetoothDisabled,
                        contentDescription = "Bluetooth Off",
                        tint = statusIconColor.copy(alpha = 0.35f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Wi-Fi status
                if (isWifi) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "Wi-Fi Connected",
                        tint = accent,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = "Wi-Fi Disconnected",
                        tint = statusIconColor.copy(alpha = 0.35f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Cellular data
                if (isData) {
                    Icon(
                        imageVector = Icons.Default.SignalCellularAlt,
                        contentDescription = "Data Connected",
                        tint = statusIconColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            }

            // Telegram remote-messages indicator (independent of the status-icons toggle)
            if (settings.telegramShowIndicator && settings.telegramEnabled) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Forum,
                    contentDescription = if (TelegramBot.connected) "Telegram connected" else "Telegram not connected",
                    tint = if (TelegramBot.connected) accent else statusIconColor.copy(alpha = 0.35f),
                    modifier = Modifier.size(16.dp)
                )
            }

            // Optional total up/down speed readout (independent of the status icons toggle)
            if (settings.showNetworkSpeedIndicator) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "↓${formatBitRateShort(networkSpeed.rxBytesPerSec)}  ↑${formatBitRateShort(networkSpeed.txBytesPerSec)}",
                    color = accent,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.width(84.dp)
                )
            }
        }

        HorizontalDivider(color = if (isDayMode) Color(0xFFCCCCCC) else Color(0xFF141414))

        HomeWidgetGrid(
            settings = settings, pageLayout = pageLayout, content = content, gap = gap,
            accent = accent, widgetBg = widgetBg, widgetBorder = widgetBorder,
            headerTextColor = headerTextColor, isDayMode = isDayMode, page = page,
            editMode = editMode, isScreenBlanked = isScreenBlanked,
            weather = weather, nowPlaying = nowPlaying, location = location, bearing = bearing,
            isOnline = isOnline, installedApps = installedApps, networkSpeed = networkSpeed,
            soundSpectrum = soundSpectrum, soundVisualizerAvailable = soundVisualizerAvailable,
            soundVisualizerHasSignal = soundVisualizerHasSignal, hardwareRadio = hardwareRadio,
            isWeatherRefreshing = isWeatherRefreshing, onRefreshWeather = onRefreshWeather,
            onPlayPause = onPlayPause, onNext = onNext, onPrev = onPrev,
            onLaunchCarPlay = onLaunchCarPlay, onLaunchAndroidAuto = onLaunchAndroidAuto,
            onTapNowPlaying = onTapNowPlaying, onLaunchHardwareRadio = onLaunchHardwareRadio,
            onStopHardwareRadio = onStopHardwareRadio, onRadioSeekUp = onRadioSeekUp,
            onRadioSeekDown = onRadioSeekDown, onRadioCycleFm = onRadioCycleFm,
            onRadioSwitchAm = onRadioSwitchAm, onRadioTune = onRadioTune, onAssignRadio = onAssignRadio,
            onLaunchApp = onLaunchApp, onToggleDayNight = onToggleDayNight,
            onToggleScreenBlanked = onToggleScreenBlanked, onToggleQuickActionButton = onToggleQuickActionButton,
            onToggleVitalsElement = onToggleVitalsElement, onToggleObdMetric = onToggleObdMetric,
            onToggleWidgetTitle = onToggleWidgetTitle,
            onZeroAltimeter = onZeroAltimeter, onResetAltimeterZero = onResetAltimeterZero,
            onSelectObdAdapter = onSelectObdAdapter, onSetWidgetBgColor = onSetWidgetBgColor,
            onSetWidgetBgMode = onSetWidgetBgMode, onSetAllWidgetsBgColor = onSetAllWidgetsBgColor,
            onSetAllWidgetsBgMode = onSetAllWidgetsBgMode, onSetMapType = onSetMapType,
            onToggleMapTraffic = onToggleMapTraffic, onUpdateSoundPad = onUpdateSoundPad,
            onSetClockStyle = onSetClockStyle, onSetVitalsAsBars = onSetVitalsAsBars,
            onSetSpeedometerDigitalOnly = onSetSpeedometerDigitalOnly, onSetSpeedometerAnimals = onSetSpeedometerAnimals,
            onSetAppShortcutCount = onSetAppShortcutCount, onSetSoundboardButtonCount = onSetSoundboardButtonCount,
            onMoveWidget = onMoveWidget, onRemoveWidget = onRemoveWidget,
            onSetAppShortcutPackage = onSetAppShortcutPackage,
            onOpenContextMenu = { contextMenuId = it }, onOpenResize = { resizingId = it },
            onOpenColorPicker = { colorPickerWidgetId = it }, onPickAppForSlot = { appPickerSlotIndex = it },
            onOpenQuickActionsConfig = { quickActionsConfigOpen = true },
            onOpenVitalsConfig = { vitalsConfigOpen = true },
            obdPickRequested = obdPickRequested,
            onObdPickHandled = { obdPickRequested = false },
            glareSweep = glareSweep, glareAngle = glareAngle,
            glareEnabled = settings.glareEnabled, glareIntensity = settings.glareIntensity
        )
    }

    // ── Widget context menu (long-press any cell) ────────────────────────────
    contextMenuId?.let { id ->
        val currentConfig = pageLayout.find { it.id == id }
        val isTitleShown = currentConfig?.isTitleVisible ?: true
        WidgetContextMenu(
            widgetId            = id,
            accent              = accent,
            clockStyle          = content.clockStyle,
            vitalsAsBars        = content.vitalsAsBars,
            speedometerDigitalOnly = content.speedometerDigitalOnly,
            speedometerAnimals  = content.speedometerAnimals,
            carPlayPackage      = settings.carPlayPackage,
            androidAutoPackage  = settings.androidAutoPackage,
            pipAppPackage       = settings.pipAppPackage,
            isDayMode           = isDayMode,
            showTitle           = isTitleShown,
            onToggleTitle       = { onToggleWidgetTitle(id) },
            onResize            = { contextMenuId = null; resizingId = id },
            onSelectBgColor     = { contextMenuId = null; colorPickerWidgetId = id },
            onEnterEditMode     = { contextMenuId = null; editMode = true },
            onRemoveWidget      = { contextMenuId = null; onRemoveWidget(id) },
            onConfigureQuickActions = { contextMenuId = null; quickActionsConfigOpen = true },
            onConfigureVitals   = { contextMenuId = null; vitalsConfigOpen = true },
            onConfigureObdMetrics = { contextMenuId = null; obdMetricsConfigOpen = true },
            onChooseObdAdapter   = { contextMenuId = null; obdPickRequested = true },
            onSetObdFallbackImage = { contextMenuId = null; obdImagePickRequested = true },
            onClearObdFallbackImage = { onUpdateSettings { copy(obdFallbackImageUri = "") }; contextMenuId = null },
            obdFallbackImageSet   = settings.obdFallbackImageUri.isNotEmpty(),
            onAssignCarPlay     = { contextMenuId = null; onAssignCarPlay() },
            onAssignAndroidAuto = { contextMenuId = null; onAssignAndroidAuto() },
            onClearCarPlay      = { contextMenuId = null; onClearCarPlay() },
            onClearAndroidAuto  = { contextMenuId = null; onClearAndroidAuto() },
            onAssignPip         = { contextMenuId = null; onAssignPip() },
            onClearPip          = { contextMenuId = null; onClearPip() },
            onSetClockStyle     = { onSetClockStyle(it) },
            onSetVitalsAsBars   = { onSetVitalsAsBars(it) },
            onSetSpeedometerDigitalOnly = { onSetSpeedometerDigitalOnly(it) },
            onSetSpeedometerAnimals = { onSetSpeedometerAnimals(it) },
            appShortcutCount    = content.appShortcutCount,
            onSetAppShortcutCount = onSetAppShortcutCount,
            appShortcutIconScale = content.appShortcutIconScale,
            onSetAppShortcutIconScale = onSetAppShortcutIconScale,
            soundboardButtonCount = content.soundboardButtonCount,
            onSetSoundboardButtonCount = onSetSoundboardButtonCount,
            advancedWeatherIcons = settings.advancedWeatherIcons,
            onSetAdvancedWeatherIcons = onSetAdvancedWeatherIcons,
            showWeatherForecast = content.showWeatherForecast,
            onSetShowWeatherForecast = onSetShowWeatherForecast,
            onOpenWeatherSettings = { contextMenuId = null; weatherSettingsOpen = true },
            onOpenGallerySettings = { contextMenuId = null; gallerySettingsOpen = true },
            onOpenMapSettings     = { contextMenuId = null; mapSettingsOpen = true },
            onOpenSpectrumSettings = { contextMenuId = null; spectrumSettingsOpen = true },
            onOpenNowPlayingSettings = { contextMenuId = null; nowPlayingSettingsOpen = true },
            onDismiss           = { contextMenuId = null }
        )
    }

    // ── Widget Background Glass Color Dialog ─────────────────────────────────
    colorPickerWidgetId?.let { id ->
        val cfg = pageLayout.find { it.id == id }
        WidgetColorDialog(
            widgetId          = id,
            currentMode       = cfg?.bgMode ?: WidgetBgMode.DEFAULT,
            currentColor      = cfg?.customBgColor,
            accent            = accent,
            isDayMode         = isDayMode,
            onSelectColor     = { color -> onSetWidgetBgColor(id, color) },
            onSelectMode      = { mode -> onSetWidgetBgMode(id, mode) },
            onDismiss         = { colorPickerWidgetId = null }
        )
    }

    // ── Bulk tint: color dialog for ALL active widgets (edit-mode palette) ────
    if (allWidgetsColorOpen) {
        val representative = pageLayout.firstOrNull { it.enabled }
        WidgetColorDialog(
            widgetId          = "ALL WIDGETS",
            currentMode       = representative?.bgMode ?: WidgetBgMode.DEFAULT,
            currentColor      = representative?.customBgColor,
            accent            = accent,
            isDayMode         = isDayMode,
            onSelectColor     = { color ->
                onSetAllWidgetsBgColor(color)
                allWidgetsColorOpen = false
            },
            onSelectMode      = { mode ->
                onSetAllWidgetsBgMode(mode)
                allWidgetsColorOpen = false
            },
            onDismiss         = { allWidgetsColorOpen = false }
        )
    }

    // ── App Shortcuts Slot Assignment Dialog ─────────────────────────────────
    appPickerSlotIndex?.let { slot ->
        val currentPkg = content.appWidgetPackages.getOrNull(slot).orEmpty()
        AppPickerDialog(
            title          = "APP SHORTCUT ${slot + 1}",
            currentPackage = currentPkg,
            apps           = installedApps,
            accent         = accent,
            isDayMode      = isDayMode,
            selectedAppPackages = settings.selectedAppPackages,
            toolsAppPackages    = settings.toolsAppPackages,
            onSelect       = { pkg ->
                onSetAppShortcutPackage(slot, pkg)
                appPickerSlotIndex = null
            },
            onClear        = { onSetAppShortcutPackage(slot, "") },
            onDismiss      = { appPickerSlotIndex = null }
        )
    }

    // ── Quick Actions Button Configuration Dialog ────────────────────────────
    if (quickActionsConfigOpen) {
        QuickActionsConfigDialog(
            enabledActions   = content.quickActionButtons,
            accent           = accent,
            isDayMode        = isDayMode,
            onToggleAction   = { actionId -> onToggleQuickActionButton(actionId) },
            onResetToDefault = { onResetQuickActionButtons() },
            onDismiss        = { quickActionsConfigOpen = false }
        )
    }

    // ── Vitals Elements Configuration Dialog ─────────────────────────────────
    if (vitalsConfigOpen) {
        VitalsConfigDialog(
            enabledElements  = content.vitalsElements,
            accent           = accent,
            isDayMode        = isDayMode,
            onToggleElement  = { elementId -> onToggleVitalsElement(elementId) },
            onResetToDefault = { onResetVitalsElements() },
            onDismiss        = { vitalsConfigOpen = false }
        )
    }

    // ── OBD Metrics Configuration Dialog ─────────────────────────────────────
    if (obdMetricsConfigOpen) {
        ObdMetricsConfigDialog(
            enabled          = content.obdMetrics,
            accent           = accent,
            isDayMode        = isDayMode,
            onToggle         = { metricId -> onToggleObdMetric(metricId) },
            onResetToDefault = { onResetObdMetrics() },
            onDismiss        = { obdMetricsConfigOpen = false }
        )
    }

    // ── Weather Settings Configuration Dialog ────────────────────────────────
    if (weatherSettingsOpen) {
        WeatherSettingsDialog(
            provider           = weatherProvider,
            useGps             = weatherUseGps,
            customLocation     = weatherCustomLocation,
            forecastDays       = weatherForecastDays,
            accent             = accent,
            isDayMode          = isDayMode,
            onSetProvider      = onSetWeatherProvider,
            onSetUseGps        = onSetWeatherUseGps,
            onSetCustomLocation = onSetWeatherCustomLocation,
            onSetForecastDays  = onSetWeatherForecastDays,
            onDismiss          = { weatherSettingsOpen = false }
        )
    }

    // ── Map Settings Configuration Dialog ─────────────────────────────────────
    if (mapSettingsOpen) {
        MapSettingsDialog(
            provider            = settings.mapProvider,
            controlScale        = settings.mapControlScale,
            mapType             = settings.mapWidgetType,
            showTraffic         = settings.mapShowTraffic,
            accent              = accent,
            isDayMode           = isDayMode,
            onSetProvider       = onSetMapProvider,
            onSetControlScale   = onSetMapControlScale,
            onSetMapType        = onSetMapType,
            onToggleTraffic     = onToggleMapTraffic,
            onDismiss           = { mapSettingsOpen = false }
        )
    }

    // ── Spectrum Settings Configuration Dialog ────────────────────────────────
    if (spectrumSettingsOpen) {
        SpectrumSettingsDialog(
            page               = page,
            settings           = settings,
            onUpdateSettings   = onUpdateSettings,
            accent             = accent,
            isDayMode          = isDayMode,
            onDismiss          = { spectrumSettingsOpen = false }
        )
    }

    // ── Now Playing Settings Configuration Dialog ─────────────────────────────
    if (nowPlayingSettingsOpen) {
        NowPlayingSettingsDialog(
            settings           = settings,
            onUpdateSettings   = onUpdateSettings,
            installedApps      = installedApps,
            accent             = accent,
            isDayMode          = isDayMode,
            onDismiss          = { nowPlayingSettingsOpen = false }
        )
    }

    // ── Weather Fallback Gallery Settings Dialog ──────────────────────────────
    if (gallerySettingsOpen) {
        GalleryFallbackSettingsDialog(
            settings         = settings,
            onUpdateSettings = onUpdateSettings,
            accent           = accent,
            isDayMode        = isDayMode,
            onDismiss        = { gallerySettingsOpen = false }
        )
    }

    // ── Resize dialog ────────────────────────────────────────────────────────
    resizingId?.let { id ->
        val config = pageLayout.find { it.id == id }
        if (config != null) {
            WidgetResizeDialog(
                config    = config,
                accent    = accent,
                isDayMode = isDayMode,
                onDismiss = { resizingId = null },
                onConfirm = { sx, sy ->
                    onUpdateWidget(id, sx, sy)
                    resizingId = null
                }
            )
        }
    }

    // ── Widget library ────────────────────────────────────────────────────────
    if (widgetLibraryOpen) {
        WidgetLibraryDialog(
            settings  = settings,
            layout    = pageLayout,
            accent    = accent,
            isDayMode = isDayMode,
            onAdd     = { id -> onAddWidget(id) },
            onRemove  = { id -> onRemoveWidget(id) },
            onDismiss = { widgetLibraryOpen = false }
        )
    }

    // ── Driver Profile Picker (tap the vehicle name) ──────────────────────────
    if (profilePickerOpen) {
        ProfilePickerDialog(
            profileNames    = settings.profileNames,
            activeProfile   = settings.activeProfile,
            accent          = accent,
            isDayMode       = isDayMode,
            onSelectProfile = { id ->
                if (id != settings.activeProfile) onSwitchProfile(id)
                profilePickerOpen = false
            },
            onDismiss       = { profilePickerOpen = false }
        )
    }

    // ── Night Screen Blackout Overlay ─────────────────────────────────────────
    if (isScreenBlanked) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(9999f)
                .background(Color.Black)
                .clickable { onToggleScreenBlanked() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text          = "TAP ANYWHERE TO WAKE",
                color         = Color(0xFF222222),
                fontSize      = 11.sp,
                letterSpacing = 2.sp
            )
        }
    }
    } // end Box
}

@Composable
private fun WidgetContextMenu(
    widgetId: String,
    accent: Color,
    clockStyle: ClockStyle,
    vitalsAsBars: Boolean,
    speedometerDigitalOnly: Boolean,
    speedometerAnimals: Boolean = false,
    carPlayPackage: String = "",
    androidAutoPackage: String = "",
    pipAppPackage: String = "",
    isDayMode: Boolean,
    showTitle: Boolean = true,
    onToggleTitle: () -> Unit = {},
    onResize: () -> Unit,
    onSelectBgColor: () -> Unit,
    onEnterEditMode: () -> Unit,
    onRemoveWidget: () -> Unit,
    onConfigureQuickActions: () -> Unit = {},
    onConfigureVitals: () -> Unit = {},
    onConfigureObdMetrics: () -> Unit = {},
    onChooseObdAdapter: () -> Unit = {},
    onSetObdFallbackImage: () -> Unit = {},
    onClearObdFallbackImage: () -> Unit = {},
    obdFallbackImageSet: Boolean = false,
    onAssignCarPlay: () -> Unit,
    onAssignAndroidAuto: () -> Unit,
    onClearCarPlay: () -> Unit,
    onClearAndroidAuto: () -> Unit,
    onAssignPip: () -> Unit,
    onClearPip: () -> Unit,
    onSetClockStyle: (ClockStyle) -> Unit,
    onSetVitalsAsBars: (Boolean) -> Unit,
    onSetSpeedometerDigitalOnly: (Boolean) -> Unit,
    onSetSpeedometerAnimals: (Boolean) -> Unit = {},
    appShortcutCount: Int = 4,
    onSetAppShortcutCount: (Int) -> Unit = {},
    appShortcutIconScale: Float = 1.0f,
    onSetAppShortcutIconScale: (Float) -> Unit = {},
    soundboardButtonCount: Int = 6,
    onSetSoundboardButtonCount: (Int) -> Unit = {},
    advancedWeatherIcons: Boolean = true,
    onSetAdvancedWeatherIcons: (Boolean) -> Unit = {},
    showWeatherForecast: Boolean = true,
    onSetShowWeatherForecast: (Boolean) -> Unit = {},
    onOpenWeatherSettings: () -> Unit = {},
    onOpenGallerySettings: () -> Unit = {},
    onOpenMapSettings: () -> Unit = {},
    onOpenSpectrumSettings: () -> Unit = {},
    onOpenNowPlayingSettings: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val menuBg    = if (isDayMode) Color(0xFFFFFFFF) else Color(0xFF111111)
    val menuBorder = if (isDayMode) Color(0xFFDDE1E5) else Color(0xFF1E1E1E)
    val menuDivider = if (isDayMode) Color(0xFFF1F3F5) else Color(0xFF1A1A1A)
    val scales = LocalUiScales.current
    val us = scales.us()
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width((220 * us).dp)
                .heightIn(max = (540 * us).dp)
                .clip(RoundedCornerShape((4 * us).dp))
                .background(menuBg)
                .border(1.dp, menuBorder, RoundedCornerShape((4 * us).dp))
                .verticalScroll(rememberScrollState())
                .padding(vertical = (4 * us).dp)
        ) {
            val inactiveMenuTint = if (isDayMode) Color(0xFF777777) else Color(0xFF555555)
            ContextRow("RESIZE", Icons.Default.AspectRatio, accent, onResize, isDayMode = isDayMode)
            HorizontalDivider(color = menuDivider)
            ContextRow("BACKGROUND COLOR", Icons.Default.Palette, accent, onSelectBgColor, isDayMode = isDayMode)
            HorizontalDivider(color = menuDivider)
            ContextRow(
                label = if (showTitle) "HIDE WIDGET TITLE" else "SHOW WIDGET TITLE",
                icon = if (showTitle) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                tint = accent,
                onClick = { onToggleTitle(); onDismiss() },
                isDayMode = isDayMode
            )
            HorizontalDivider(color = menuDivider)
            ContextRow("MOVE / EDIT LAYOUT", Icons.Default.OpenWith, accent, { onEnterEditMode(); onDismiss() }, isDayMode = isDayMode)
            if (widgetId == "QUICK_ACTIONS") {
                HorizontalDivider(color = menuDivider)
                ContextRow(
                    label = "CONFIGURE BUTTONS",
                    icon = Icons.Default.Tune,
                    tint = accent,
                    onClick = onConfigureQuickActions,
                    isDayMode = isDayMode
                )
            }
            if (widgetId == "CLOCK") {
                HorizontalDivider(color = menuDivider)
                ContextRow(
                    label   = "DIGITAL",
                    icon    = Icons.Default.Schedule,
                    tint    = if (clockStyle == ClockStyle.DIGITAL) accent else inactiveMenuTint,
                    onClick = { onSetClockStyle(ClockStyle.DIGITAL); onDismiss() },
                    isDayMode = isDayMode
                )
                HorizontalDivider(color = menuDivider)
                ContextRow(
                    label   = "ANALOG",
                    icon    = Icons.Default.Watch,
                    tint    = if (clockStyle == ClockStyle.ANALOG) accent else inactiveMenuTint,
                    onClick = { onSetClockStyle(ClockStyle.ANALOG); onDismiss() },
                    isDayMode = isDayMode
                )
            }
            if (widgetId == "VITALS") {
                HorizontalDivider(color = menuDivider)
                ContextRow(
                    label   = "CONFIGURE ELEMENTS",
                    icon    = Icons.Default.Tune,
                    tint    = accent,
                    onClick = onConfigureVitals,
                    isDayMode = isDayMode
                )
                HorizontalDivider(color = menuDivider)
                ContextRow(
                    label   = "DIAL GAUGES",
                    icon    = Icons.Default.Adjust,
                    tint    = if (!vitalsAsBars) accent else inactiveMenuTint,
                    onClick = { onSetVitalsAsBars(false); onDismiss() },
                    isDayMode = isDayMode
                )
                HorizontalDivider(color = menuDivider)
                ContextRow(
                    label   = "BARS VIEW",
                    icon    = Icons.Default.FormatAlignLeft,
                    tint    = if (vitalsAsBars) accent else inactiveMenuTint,
                    onClick = { onSetVitalsAsBars(true); onDismiss() },
                    isDayMode = isDayMode
                )
            }
            if (widgetId == "OBD") {
                HorizontalDivider(color = menuDivider)
                ContextRow(
                    label   = "CHOOSE ADAPTER",
                    icon    = Icons.Default.Bluetooth,
                    tint    = accent,
                    onClick = onChooseObdAdapter,
                    isDayMode = isDayMode
                )
                HorizontalDivider(color = menuDivider)
                ContextRow(
                    label   = "CONFIGURE METRICS",
                    icon    = Icons.Default.Tune,
                    tint    = accent,
                    onClick = onConfigureObdMetrics,
                    isDayMode = isDayMode
                )
                HorizontalDivider(color = menuDivider)
                ContextRow(
                    label   = "SET FALLBACK IMAGE",
                    icon    = Icons.Default.Image,
                    tint    = accent,
                    onClick = onSetObdFallbackImage,
                    isDayMode = isDayMode
                )
                if (obdFallbackImageSet) {
                    HorizontalDivider(color = menuDivider)
                    ContextRow(
                        label   = "CLEAR FALLBACK IMAGE",
                        icon    = Icons.Default.Delete,
                        tint    = accent,
                        onClick = onClearObdFallbackImage,
                        isDayMode = isDayMode
                    )
                }
            }
            if (widgetId == "APP_SHORTCUTS") {
                for (c in 1..4) {
                    HorizontalDivider(color = menuDivider)
                    ContextRow(
                        label   = "$c ${if (c == 1) "ICON" else "ICONS"}",
                        icon    = Icons.Default.Apps,
                        tint    = if (appShortcutCount == c) accent else inactiveMenuTint,
                        onClick = { onSetAppShortcutCount(c); onDismiss() },
                        isDayMode = isDayMode
                    )
                }
                HorizontalDivider(color = menuDivider)
                ContextRow(
                    label   = "SMALL ICONS",
                    icon    = Icons.Default.Apps,
                    tint    = if (appShortcutIconScale < 0.9f) accent else inactiveMenuTint,
                    onClick = { onSetAppShortcutIconScale(0.72f); onDismiss() },
                    isDayMode = isDayMode
                )
                HorizontalDivider(color = menuDivider)
                ContextRow(
                    label   = "NORMAL ICONS",
                    icon    = Icons.Default.Apps,
                    tint    = if (appShortcutIconScale in 0.9f..1.1f) accent else inactiveMenuTint,
                    onClick = { onSetAppShortcutIconScale(1.0f); onDismiss() },
                    isDayMode = isDayMode
                )
                HorizontalDivider(color = menuDivider)
                ContextRow(
                    label   = "LARGE ICONS",
                    icon    = Icons.Default.Apps,
                    tint    = if (appShortcutIconScale > 1.1f) accent else inactiveMenuTint,
                    onClick = { onSetAppShortcutIconScale(1.35f); onDismiss() },
                    isDayMode = isDayMode
                )
            }
            if (widgetId == "MAP") {
                HorizontalDivider(color = menuDivider)
                ContextRow(
                    label   = "MAP SETTINGS",
                    icon    = Icons.Default.Settings,
                    tint    = accent,
                    onClick = onOpenMapSettings,
                    isDayMode = isDayMode
                )
            }
            if (widgetId == "SPECTRUM") {
                HorizontalDivider(color = menuDivider)
                ContextRow(
                    label   = "SPECTRUM SETTINGS",
                    icon    = Icons.Default.Settings,
                    tint    = accent,
                    onClick = onOpenSpectrumSettings,
                    isDayMode = isDayMode
                )
            }
            if (widgetId == "WEATHER") {
                HorizontalDivider(color = menuDivider)
                ContextRow(
                    label   = "WEATHER SETTINGS",
                    icon    = Icons.Default.Settings,
                    tint    = accent,
                    onClick = onOpenWeatherSettings,
                    isDayMode = isDayMode
                )
                HorizontalDivider(color = menuDivider)
                ContextRow(
                    label   = "FALLBACK GALLERY",
                    icon    = Icons.Default.PhotoLibrary,
                    tint    = accent,
                    onClick = onOpenGallerySettings,
                    isDayMode = isDayMode
                )
                HorizontalDivider(color = menuDivider)
                ContextRow(
                    label   = "STANDARD ICONS",
                    icon    = Icons.Default.CloudQueue,
                    tint    = if (!advancedWeatherIcons) accent else inactiveMenuTint,
                    onClick = { onSetAdvancedWeatherIcons(false); onDismiss() },
                    isDayMode = isDayMode
                )
                HorizontalDivider(color = menuDivider)
                ContextRow(
                    label   = "ADVANCED ICONS",
                    icon    = Icons.Default.Cloud,
                    tint    = if (advancedWeatherIcons) accent else inactiveMenuTint,
                    onClick = { onSetAdvancedWeatherIcons(true); onDismiss() },
                    isDayMode = isDayMode
                )
                HorizontalDivider(color = menuDivider)
                ContextRow(
                    label   = if (showWeatherForecast) "HIDE FORECAST DAYS" else "SHOW FORECAST DAYS",
                    icon    = Icons.Default.DateRange,
                    tint    = accent,
                    onClick = { onSetShowWeatherForecast(!showWeatherForecast); onDismiss() },
                    isDayMode = isDayMode
                )
            }
            if (widgetId == "SOUNDBOARD") {
                for (b in listOf(1, 2, 3, 4, 6)) {
                    HorizontalDivider(color = menuDivider)
                    ContextRow(
                        label   = "$b ${if (b == 1) "BUTTON" else "BUTTONS"}",
                        icon    = Icons.Default.Piano,
                        tint    = if (soundboardButtonCount == b) accent else inactiveMenuTint,
                        onClick = { onSetSoundboardButtonCount(b); onDismiss() },
                        isDayMode = isDayMode
                    )
                }
            }
            if (widgetId == "SPEEDOMETER") {
                HorizontalDivider(color = menuDivider)
                ContextRow(
                    label   = "DIAL TRACK",
                    icon    = Icons.Default.Speed,
                    tint    = if (!speedometerDigitalOnly && !speedometerAnimals) accent else inactiveMenuTint,
                    onClick = { onSetSpeedometerAnimals(false); onSetSpeedometerDigitalOnly(false); onDismiss() },
                    isDayMode = isDayMode
                )
                HorizontalDivider(color = menuDivider)
                ContextRow(
                    label   = "DIGITAL ONLY",
                    icon    = Icons.Default.Dialpad,
                    tint    = if (speedometerDigitalOnly && !speedometerAnimals) accent else inactiveMenuTint,
                    onClick = { onSetSpeedometerAnimals(false); onSetSpeedometerDigitalOnly(true); onDismiss() },
                    isDayMode = isDayMode
                )
                HorizontalDivider(color = menuDivider)
                ContextRow(
                    label   = "ANIMALS",
                    icon    = Icons.Default.Pets,
                    tint    = if (speedometerAnimals) accent else inactiveMenuTint,
                    onClick = { onSetSpeedometerAnimals(true); onDismiss() },
                    isDayMode = isDayMode
                )
            }
            if (widgetId == "NOW_PLAYING") {
                HorizontalDivider(color = menuDivider)
                ContextRow("ASSIGN CARPLAY APP",      Icons.Default.PhoneAndroid,  accent, onAssignCarPlay, isDayMode = isDayMode)
                if (carPlayPackage.isNotEmpty()) {
                    HorizontalDivider(color = menuDivider)
                    ContextRow("CLEAR CARPLAY APP", Icons.Default.PhoneAndroid, Color(0xFF884444), onClearCarPlay, isDayMode = isDayMode)
                }
                HorizontalDivider(color = menuDivider)
                ContextRow("ASSIGN ANDROID AUTO APP", Icons.Default.DirectionsCar, accent, onAssignAndroidAuto, isDayMode = isDayMode)
                if (androidAutoPackage.isNotEmpty()) {
                    HorizontalDivider(color = menuDivider)
                    ContextRow("CLEAR ANDROID AUTO APP", Icons.Default.DirectionsCar, Color(0xFF884444), onClearAndroidAuto, isDayMode = isDayMode)
                }
                HorizontalDivider(color = menuDivider)
                ContextRow(
                    label   = "NOW PLAYING SETTINGS",
                    icon    = Icons.Default.Settings,
                    tint    = accent,
                    onClick = onOpenNowPlayingSettings,
                    isDayMode = isDayMode
                )
            }
            HorizontalDivider(color = menuDivider)
            ContextRow(
                label = "REMOVE WIDGET",
                icon = Icons.Default.Delete,
                tint = Color(0xFFE03131),
                onClick = { onRemoveWidget(); onDismiss() },
                isDayMode = isDayMode
            )
        }
    }
}

@Composable
private fun ContextRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit,
    isDayMode: Boolean = false
) {
    val finalTint = if (isDayMode) {
        if (tint == Color(0xFF884444) || tint == Color(0xFFE03131)) {
            tint
        } else if (tint == Color(0xFF777777)) {
            Color(0xFF888888)
        } else {
            Color(0xFF111111)
        }
    } else {
        tint
    }
    val scales = LocalUiScales.current
    val ts = scales.ts()
    val us = scales.us()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tapClickable(onClick = onClick)
            .padding(horizontal = (16 * us).dp, vertical = (12 * us).dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((12 * us).dp)
    ) {
        Icon(icon, null, tint = finalTint, modifier = Modifier.size((16 * us).dp))
        Text(label, color = finalTint, fontSize = (10 * ts).sp, letterSpacing = (1 * ts).sp)
    }
}

@Composable
private fun WidgetResizeDialog(
    config: WidgetConfig,
    accent: Color,
    isDayMode: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (spanX: Int, spanY: Int) -> Unit
) {
    var spanX by remember { mutableStateOf(config.spanX) }
    var spanY by remember { mutableStateOf(config.spanY) }

    val maxSpanX = GRID_COLS - config.gridX
    val maxSpanY = GRID_ROWS - config.gridY

    val accentLum = 0.299f * accent.red + 0.587f * accent.green + 0.114f * accent.blue
    val effectiveAccent = if (isDayMode && accentLum > 0.65f) Color(0xFF111111) else if (!isDayMode && accentLum < 0.25f) Color(0xFFEEEEEE) else accent

    val dialogBg     = if (isDayMode) Color(0xFFFFFFFF) else MaterialTheme.colorScheme.background
    val dialogText   = if (isDayMode) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground
    val dialogBorder = if (isDayMode) Color(0xFFDEE2E6) else Color(0xFF2A2A2A)
    val cancelColor  = if (isDayMode) Color(0xFF6C757D) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)

    val scales = LocalUiScales.current
    val ts = scales.ts()
    val us = scales.us()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.border(1.dp, dialogBorder, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        title = {
            Text(
                text          = "RESIZE ${config.id.replace('_', ' ')}",
                color         = dialogText,
                fontSize      = (11 * ts).sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = (2 * ts).sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy((16 * us).dp)) {
                Text(
                    text          = "Adjust the grid span (columns x rows) for this widget.",
                    color         = if (isDayMode) Color(0xFF6C757D) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    fontSize      = (9 * ts).sp,
                    letterSpacing = (0.5f * ts).sp
                )
                SpanRow(label = "WIDTH",  value = spanX, min = 1, max = maxSpanX, accent = accent, isDayMode = isDayMode) { spanX = it }
                SpanRow(label = "HEIGHT", value = spanY, min = 1, max = maxSpanY, accent = accent, isDayMode = isDayMode) { spanY = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(spanX, spanY) }) {
                Text(
                    text          = "APPLY",
                    color         = effectiveAccent,
                    fontSize      = (11 * ts).sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = (1 * ts).sp
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text          = "CANCEL",
                    color         = cancelColor,
                    fontSize      = (11 * ts).sp,
                    fontWeight    = FontWeight.SemiBold,
                    letterSpacing = (1 * ts).sp
                )
            }
        },
        containerColor    = dialogBg,
        titleContentColor = dialogText,
        textContentColor  = dialogText
    )
}

@Composable
private fun SpanRow(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    accent: Color,
    isDayMode: Boolean,
    onChange: (Int) -> Unit
) {
    val accentLum = 0.299f * accent.red + 0.587f * accent.green + 0.114f * accent.blue
    val effectiveAccent = if (isDayMode && accentLum > 0.65f) Color(0xFF111111) else if (!isDayMode && accentLum < 0.25f) Color(0xFFEEEEEE) else accent

    val textColor          = if (isDayMode) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground
    val dimColor           = if (isDayMode) Color(0xFF343A40) else Color(0xFF888888)
    val disabledC          = if (isDayMode) Color(0xFFCED4DA) else Color(0xFF444444)
    val buttonBg           = if (isDayMode) Color(0xFFF1F3F5) else Color(0xFF1E1E1E)
    val buttonBorder       = if (isDayMode) Color(0xFFDEE2E6) else Color(0xFF2E2E2E)
    val inactivePipBg      = if (isDayMode) Color(0xFFE9ECEF) else Color(0xFF262626)
    val inactivePipBorder  = if (isDayMode) Color(0xFFCED4DA) else Color(0xFF383838)
    val activePipBg        = if (isDayMode && accentLum > 0.65f) Color(0xFF212529) else effectiveAccent
    val activePipBorder    = if (isDayMode && accentLum > 0.65f) Color(0xFF111111) else effectiveAccent

    val scales = LocalUiScales.current
    val ts = scales.ts()
    val us = scales.us()

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((8 * us).dp)
    ) {
        Text(
            text          = label,
            color         = dimColor,
            fontSize      = (10 * ts).sp,
            fontWeight    = FontWeight.Bold,
            letterSpacing = (1 * ts).sp,
            modifier      = Modifier.width((52 * us).dp)
        )

        // Stepper: Minus [-]
        Box(
            modifier = Modifier
                .size((32 * us).dp)
                .clip(RoundedCornerShape((4 * us).dp))
                .background(if (value > min) buttonBg else buttonBg.copy(alpha = 0.5f))
                .border(1.dp, if (value > min) buttonBorder else buttonBorder.copy(alpha = 0.5f), RoundedCornerShape((4 * us).dp))
                .clickable(enabled = value > min) { onChange(value - 1) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.Remove,
                contentDescription = "Decrease $label",
                tint               = if (value > min) textColor else disabledC,
                modifier           = Modifier.size((16 * us).dp)
            )
        }

        // Stepper: Value number
        Text(
            text       = "$value",
            color      = textColor,
            fontSize   = (16 * ts).sp,
            fontWeight = FontWeight.Bold,
            textAlign  = TextAlign.Center,
            modifier   = Modifier.width((26 * us).dp)
        )

        // Stepper: Plus [+]
        Box(
            modifier = Modifier
                .size((32 * us).dp)
                .clip(RoundedCornerShape((4 * us).dp))
                .background(if (value < max) buttonBg else buttonBg.copy(alpha = 0.5f))
                .border(1.dp, if (value < max) buttonBorder else buttonBorder.copy(alpha = 0.5f), RoundedCornerShape((4 * us).dp))
                .clickable(enabled = value < max) { onChange(value + 1) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.Add,
                contentDescription = "Increase $label",
                tint               = if (value < max) effectiveAccent else disabledC,
                modifier           = Modifier.size((16 * us).dp)
            )
        }

        Spacer(Modifier.weight(1f))

        // Grid span pip indicator
        Row(horizontalArrangement = Arrangement.spacedBy((3 * us).dp)) {
            repeat(max) { i ->
                val isActive = i < value
                Box(
                    modifier = Modifier
                        .size(width = (14 * us).dp, height = (10 * us).dp)
                        .clip(RoundedCornerShape((1.5f * us).dp))
                        .background(if (isActive) activePipBg else inactivePipBg)
                        .border(1.dp, if (isActive) activePipBorder else inactivePipBorder, RoundedCornerShape((1.5f * us).dp))
                )
            }
        }
    }
}


// ── Widget Library ────────────────────────────────────────────────────────────

@Composable
private fun WidgetLibraryDialog(
    settings: AppSettings,
    layout: List<WidgetConfig>,
    accent: Color,
    isDayMode: Boolean,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val dialogBg    = if (isDayMode) Color(0xFFEEEEEE) else Color(0xFF0C0C0C)
    val dialogBorder = if (isDayMode) Color(0xFFCCCCCC) else Color(0xFF1E1E1E)
    val titleColor  = if (isDayMode) Color(0xFF495057) else Color(0xFF555555)
    val closeColor  = if (isDayMode) Color(0xFF495057) else Color(0xFF444444)

    val scales = LocalUiScales.current
    val ts = scales.ts()
    val us = scales.us()

    // Match exactly what HomeScreen renders: a widget is "on this page" only when the
    // global show-flag is ON *and* the page layout has it enabled.
    val visibleIds = buildSet {
        if (settings.showClock) add("CLOCK")
        if (settings.showWeather) add("WEATHER")
        if (settings.showNowPlaying) add("NOW_PLAYING")
        if (settings.showTelemetry) add("TELEMETRY")
        if (settings.showAltimeter) add("ALTIMETER")
        if (settings.showSpeedometer) add("SPEEDOMETER")
        if (settings.showVitals) add("VITALS")
        if (settings.showTripTracker) add("TRIP_TRACKER")
        if (settings.showSoundboard) add("SOUNDBOARD")
        if (settings.showVolume) add("VOLUME")
        if (settings.showQuickActions) add("QUICK_ACTIONS")
        if (settings.showAppShortcuts) add("APP_SHORTCUTS")
        if (settings.showObd) add("OBD")
        if (settings.showNetworkSpeed) add("NETWORK_SPEED")
        if (settings.showSpectrum) add("SPECTRUM")
        if (settings.showMap) add("MAP")
    }

    val canAdd = canAddWidget(settings, layout)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .widthIn(min = (320 * us).dp, max = (520 * us).dp)
                .heightIn(max = (560 * us).dp)
                .clip(RoundedCornerShape((4 * us).dp))
                .background(dialogBg)
                .border(1.dp, dialogBorder, RoundedCornerShape((4 * us).dp))
                .padding((16 * us).dp)
        ) {
            Row(
                modifier          = Modifier.fillMaxWidth().padding(bottom = (14 * us).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text          = "WIDGET LIBRARY",
                    color         = titleColor,
                    fontSize      = (9 * ts).sp,
                    letterSpacing = (2 * ts).sp
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size((24 * us).dp)) {
                    Icon(Icons.Default.Close, null, tint = closeColor, modifier = Modifier.size((14 * us).dp))
                }
            }

            LazyVerticalGrid(
                columns               = GridCells.Adaptive(minSize = (92 * us).dp),
                horizontalArrangement = Arrangement.spacedBy((6 * us).dp),
                verticalArrangement   = Arrangement.spacedBy((6 * us).dp),
                modifier              = Modifier.weight(1f, fill = false).fillMaxWidth()
            ) {
                items(ALL_WIDGET_TYPES) { info ->
                    val isActive = info.id in visibleIds && layout.any { it.id == info.id && it.enabled }
                    WidgetLibraryCard(
                        info     = info,
                        isActive = isActive,
                        canAdd   = canAdd,
                        accent   = accent,
                        isDayMode = isDayMode,
                        onToggle = { if (isActive) onRemove(info.id) else onAdd(info.id) }
                    )
                }
            }

            if (!canAdd) {
                Spacer(Modifier.height((10 * us).dp))
                Text(
                    text          = "ALL ${GRID_COLS * GRID_ROWS} CELLS OCCUPIED — REMOVE A WIDGET TO ADD MORE",
                    color         = if (isDayMode) Color(0xFFE03131) else Color(0xFF3A3A3A),
                    fontSize      = (8 * ts).sp,
                    letterSpacing = (1 * ts).sp,
                    modifier      = Modifier.fillMaxWidth(),
                    textAlign     = TextAlign.Center
                )
            }
        }
    }
}


@Composable
private fun ProfilePickerDialog(
    profileNames: List<String>,
    activeProfile: Int,
    accent: Color,
    isDayMode: Boolean,
    onSelectProfile: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val dialogBg     = if (isDayMode) Color(0xFFEEEEEE) else Color(0xFF0C0C0C)
    val dialogBorder = if (isDayMode) Color(0xFFCCCCCC) else Color(0xFF1E1E1E)
    val titleColor   = if (isDayMode) Color(0xFF495057) else Color(0xFF555555)
    val closeColor   = if (isDayMode) Color(0xFF495057) else Color(0xFF444444)
    val nameColor    = if (isDayMode) Color(0xFF333333) else Color(0xFFAAAAAA)

    val scales = LocalUiScales.current
    val ts = scales.ts()
    val us = scales.us()

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape((4 * us).dp))
                .background(dialogBg)
                .border(1.dp, dialogBorder, RoundedCornerShape((4 * us).dp))
                .padding((16 * us).dp)
                .widthIn(min = (280 * us).dp, max = (420 * us).dp)
        ) {
            Row(
                modifier          = Modifier.fillMaxWidth().padding(bottom = (4 * us).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text          = "DRIVER PROFILE",
                    color         = titleColor,
                    fontSize      = (9 * ts).sp,
                    letterSpacing = (2 * ts).sp
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { TapSound.play(); onDismiss() }, modifier = Modifier.size((24 * us).dp)) {
                    Icon(Icons.Default.Close, null, tint = closeColor, modifier = Modifier.size((14 * us).dp))
                }
            }

            Text(
                text          = "Tap to switch — each profile keeps its own widgets, colors, layout & settings",
                color         = closeColor,
                fontSize      = (10 * ts).sp,
                lineHeight    = (14 * ts).sp,
                modifier      = Modifier.padding(bottom = (12 * us).dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy((6 * us).dp)) {
                (0..3).forEach { id ->
                    val isActive = id == activeProfile
                    val name     = profileNames.getOrElse(id) { "DRIVER ${id + 1}" }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape((4 * us).dp))
                            .background(if (isActive) accent.copy(alpha = 0.18f) else Color.Transparent)
                            .border(
                                width = 1.dp,
                                color = if (isActive) accent else dialogBorder,
                                shape = RoundedCornerShape((4 * us).dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { TapSound.play(); onSelectProfile(id) }
                            .padding(horizontal = (12 * us).dp, vertical = (12 * us).dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text          = name.uppercase(),
                            color         = if (isActive) accent else nameColor,
                            fontSize      = (12 * ts).sp,
                            fontWeight    = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            letterSpacing = (1 * ts).sp,
                            maxLines      = 1,
                            overflow      = TextOverflow.Ellipsis,
                            modifier      = Modifier.weight(1f)
                        )
                        if (isActive) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active",
                                tint        = accent,
                                modifier    = Modifier.size((14 * us).dp)
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun WidgetLibraryCard(
    info: WidgetTypeInfo,
    isActive: Boolean,
    canAdd: Boolean,
    accent: Color,
    isDayMode: Boolean,
    onToggle: () -> Unit
) {
    val accentLum = 0.299f * accent.red + 0.587f * accent.green + 0.114f * accent.blue
    val effectiveAccent = if (isDayMode && accentLum > 0.65f) Color(0xFF111111) else if (!isDayMode && accentLum < 0.25f) Color(0xFFEEEEEE) else accent

    val enabled = isActive || canAdd
    // Accent is concentrated on border + icon + status dot; the label stays neutral so
    // the card reads cleanly instead of being flooded with a single colour. Inactive
    // cards stay clearly visible (grey) so "available to add" is obvious.
    val cardBorder = when {
        isActive  -> effectiveAccent.copy(alpha = 0.9f)
        isDayMode -> Color(0xFFCCCCCC)
        else      -> Color(0xFF333333)
    }
    val cardBg = when {
        isActive  -> effectiveAccent.copy(alpha = if (isDayMode) 0.08f else 0.10f)
        isDayMode -> Color(0xFFFFFFFF)
        else      -> Color(0xFF171717)
    }
    val iconTint = when {
        isActive  -> effectiveAccent
        isDayMode -> Color(0xFF6C757D)
        else      -> Color(0xFF9A9A9A)
    }
    val labelColor = when {
        isActive  -> if (isDayMode) Color(0xFF212529) else Color(0xFFF0F0F0)
        isDayMode -> Color(0xFF495057)
        else      -> Color(0xFFC0C0C0)
    }

    val scales = LocalUiScales.current
    val ts = scales.ts()
    val us = scales.us()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape((6 * us).dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape((6 * us).dp))
            .clickable(enabled = enabled, onClick = { TapSound.play(); onToggle() })
            .padding((6 * us).dp)
    ) {
        val side = minOf(maxWidth, maxHeight)
        val iconSize   = (side * 0.22f).coerceIn((16 * us).dp, (28 * us).dp)
        val labelSize  = ((side.value * 0.070f).coerceIn(7f, 9.5f) * ts).sp
        val statusSize = ((side.value * 0.060f).coerceIn(6f, 8f) * ts).sp

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(info.icon, null, tint = iconTint, modifier = Modifier.size(iconSize))
            Spacer(Modifier.height((4 * us).dp))
            Text(
                text          = info.label,
                color         = labelColor,
                fontSize      = labelSize,
                fontWeight    = if (isActive) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = (0.5f * ts).sp,
                textAlign     = TextAlign.Center,
                maxLines      = 2,
                lineHeight    = (labelSize.value * 1.15f).sp
            )
            Spacer(Modifier.height((4 * us).dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((3 * us).dp)
            ) {
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .size((5 * us).dp)
                            .clip(RoundedCornerShape(50))
                            .background(effectiveAccent)
                    )
                }
                Text(
                    text          = when {
                        isActive -> "ACTIVE"
                        !canAdd  -> "FULL"
                        else     -> "+ ADD"
                    },
                    color         = when {
                        isActive -> effectiveAccent
                        !canAdd  -> if (isDayMode) Color(0xFFADB5BD) else Color(0xFF5A5A5A)
                        else     -> if (isDayMode) Color(0xFF495057) else Color(0xFFB0B0B0)
                    },
                    fontSize      = statusSize,
                    fontWeight    = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    letterSpacing = (0.5f * ts).sp,
                    textAlign     = TextAlign.Center
                )
            }
        }
    }
}

// ── Widget Background Glass Color Dialog ──────────────────────────────────────

@Composable
private fun WidgetColorDialog(
    widgetId: String,
    currentMode: WidgetBgMode,
    currentColor: Long?,
    accent: Color,
    isDayMode: Boolean,
    onSelectColor: (Long?) -> Unit,
    onSelectMode: (WidgetBgMode) -> Unit,
    onDismiss: () -> Unit
) {
    val accentLum = 0.299f * accent.red + 0.587f * accent.green + 0.114f * accent.blue
    val effectiveAccent = if (isDayMode && accentLum > 0.65f) Color(0xFF111111)
                          else if (!isDayMode && accentLum < 0.25f) Color(0xFFEEEEEE)
                          else accent

    val dialogBg = if (isDayMode) Color(0xFFFFFFFF) else Color(0xFF111111)
    val dialogBorder = if (isDayMode) Color(0xFFDDE1E5) else Color(0xFF222222)

    val scales = LocalUiScales.current
    val ts = scales.ts()
    val us = scales.us()

    val presets = listOf(
        Pair("DEFAULT (SMOKE)", null),
        Pair("FROSTED WHITE", 0xCCFFFFFFL),
        Pair("GRAPHITE SLATE", 0xCC2A2D34L),
        Pair("CARBON BLACK", 0xCC111215L),
        Pair("SAPPHIRE BLUE", 0xCC153B6AL),
        Pair("EMERALD GREEN", 0xCC134E35L),
        Pair("AMBER GOLD", 0xCCB86200L),
        Pair("CRIMSON RED", 0xCC8B1E1EL),
        Pair("VIOLET PURPLE", 0xCC4A1E6DL),
        Pair("CYAN TEAL", 0xCC0E666EL),
        Pair("WARM TITANIUM", 0xCC4A4843L),
        Pair("NEON LIME", 0xCC74A116L)
    )

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape((8 * us).dp))
                .background(dialogBg)
                .border(1.dp, dialogBorder, RoundedCornerShape((8 * us).dp))
                .padding((16 * us).dp)
                .widthIn(min = (320 * us).dp, max = (500 * us).dp)
                .heightIn(max = (560 * us).dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = (12 * us).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WIDGET GLASS TINT (${widgetId.replace('_', ' ')})",
                    color = if (isDayMode) Color(0xFF333333) else Color(0xFF888888),
                    fontSize = (10 * ts).sp,
                    letterSpacing = (1.5f * ts).sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size((24 * us).dp)) {
                    Icon(Icons.Default.Close, null, tint = if (isDayMode) Color(0xFF666666) else Color(0xFF888888), modifier = Modifier.size((16 * us).dp))
                }
            }

            // "Follow theme" — clearly labelled, distinct from the preset swatches
            val followSelected = currentMode == WidgetBgMode.FOLLOW_THEME
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape((6 * us).dp))
                    .background(if (followSelected) effectiveAccent.copy(alpha = 0.18f) else Color.Transparent)
                    .border(
                        width = if (followSelected) 2.dp else 1.dp,
                        color = if (followSelected) effectiveAccent else dialogBorder,
                        shape = RoundedCornerShape((6 * us).dp)
                    )
                    .clickable {
                        onSelectMode(WidgetBgMode.FOLLOW_THEME)
                        onDismiss()
                    }
                    .padding(vertical = (12 * us).dp, horizontal = (10 * us).dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((8 * us).dp)
            ) {
                Icon(
                    Icons.Default.BrightnessAuto,
                    null,
                    tint = if (followSelected) effectiveAccent else (if (isDayMode) Color(0xFF555555) else Color(0xFF888888)),
                    modifier = Modifier.size((16 * us).dp)
                )
                Column {
                    Text(
                        "FOLLOW THEME",
                        color = if (followSelected) effectiveAccent else (if (isDayMode) Color(0xFF111111) else Color(0xFFDDDDDD)),
                        fontSize = (10 * ts).sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (1 * ts).sp
                    )
                    Text(
                        "Uses the global tile color & transparency from Settings",
                        color = if (isDayMode) Color(0xFF666666) else Color(0xFF777777),
                        fontSize = (7 * ts).sp,
                        letterSpacing = (0.3f * ts).sp
                    )
                }
            }

            Spacer(Modifier.height((8 * us).dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy((8 * us).dp),
                verticalArrangement = Arrangement.spacedBy((8 * us).dp),
                modifier = Modifier.weight(1f, fill = false).fillMaxWidth()
            ) {
                items(presets) { (name, colorVal) ->
                    val isSelected = when {
                        currentMode == WidgetBgMode.FOLLOW_THEME -> false
                        currentMode == WidgetBgMode.CUSTOM -> currentColor == colorVal
                        else -> colorVal == null
                    }
                    val bgSample = if (colorVal != null) Color(colorVal) else (if (isDayMode) Color(0xFFDDDDDD) else Color(0x66000000))
                    val lum = if (colorVal != null) {
                        val c = Color(colorVal)
                        0.299f * c.red + 0.587f * c.green + 0.114f * c.blue
                    } else 0.2f
                    val sampleTextColor = if (lum > 0.52f) Color(0xFF111111) else Color(0xFFEEEEEE)
                    val borderHighlight = if (isDayMode) {
                        if (lum > 0.6f) Color(0xFF111111) else effectiveAccent
                    } else {
                        if (lum < 0.3f) Color(0xFFEEEEEE) else effectiveAccent
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape((6 * us).dp))
                            .background(bgSample)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) borderHighlight else (if (isDayMode) Color(0xFFCCCCCC) else Color(0x33FFFFFF)),
                                shape = RoundedCornerShape((6 * us).dp)
                            )
                            .clickable {
                                onSelectColor(colorVal)
                                onDismiss()
                            }
                            .padding(vertical = (12 * us).dp, horizontal = (6 * us).dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = name,
                            color = sampleTextColor,
                            fontSize = (8 * ts).sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (1 * ts).sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height((4 * us).dp))
                        Text(
                            text = if (colorVal == null) "DEFAULT" else if (lum > 0.52f) "DARK TEXT" else "LIGHT TEXT",
                            color = sampleTextColor.copy(alpha = 0.7f),
                            fontSize = (6.5f * ts).sp,
                            letterSpacing = (0.5f * ts).sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// ── Quick Actions Button Configuration Dialog ─────────────────────────────────

@Composable
private fun QuickActionsConfigDialog(
    enabledActions: List<String>,
    accent: Color,
    isDayMode: Boolean,
    onToggleAction: (String) -> Unit,
    onResetToDefault: () -> Unit,
    onDismiss: () -> Unit
) {
    val accentLum = 0.299f * accent.red + 0.587f * accent.green + 0.114f * accent.blue
    val effectiveAccent = if (isDayMode && accentLum > 0.65f) Color(0xFF111111)
                          else if (!isDayMode && accentLum < 0.25f) Color(0xFFEEEEEE)
                          else accent

    val dialogBg = if (isDayMode) Color(0xFFFFFFFF) else Color(0xFF111111)
    val dialogBorder = if (isDayMode) Color(0xFFDDE1E5) else Color(0xFF222222)
    val cardBg = if (isDayMode) Color(0xFFF8F9FA) else Color(0xFF181818)
    val textColor = if (isDayMode) Color(0xFF111111) else Color(0xFFEEEEEE)
    val subTextColor = if (isDayMode) Color(0xFF666666) else Color(0xFF888888)

    val scales = LocalUiScales.current
    val ts = scales.ts()
    val us = scales.us()

    val allActions = listOf(
        Triple("BLUETOOTH", "Bluetooth", "Toggle Bluetooth wireless adapter"),
        Triple("WIFI", "Wi-Fi", "Toggle Wi-Fi network connection"),
        Triple("BLACKOUT", "Screen Off / Blackout", "Instant screen blanking for night driving"),
        Triple("MUTE", "Media Audio Mute", "Mute / unmute music and media volume"),
        Triple("SETTINGS", "System Settings", "One-tap shortcut to Android system settings"),
        Triple("DAY_NIGHT", "Day / Night Theme", "Switch between daylight and night themes")
    )

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape((8 * us).dp))
                .background(dialogBg)
                .border(1.dp, dialogBorder, RoundedCornerShape((8 * us).dp))
                .padding((16 * us).dp)
                .widthIn(min = (340 * us).dp, max = (480 * us).dp)
                .heightIn(max = (560 * us).dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = (12 * us).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CONFIGURE QUICK ACTIONS",
                        color = if (isDayMode) Color(0xFF333333) else Color(0xFF888888),
                        fontSize = (11 * ts).sp,
                        letterSpacing = (1.5f * ts).sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Select which buttons appear in the widget",
                        color = subTextColor,
                        fontSize = (9 * ts).sp
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size((24 * us).dp)) {
                    Icon(Icons.Default.Close, null, tint = if (isDayMode) Color(0xFF666666) else Color(0xFF888888), modifier = Modifier.size((16 * us).dp))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy((8 * us).dp)
            ) {
                allActions.forEach { (id, title, desc) ->
                    val isChecked = enabledActions.any { it.equals(id, ignoreCase = true) }
                    val actionIcon = when (id) {
                        "BLUETOOTH" -> Icons.Default.Bluetooth
                        "WIFI" -> Icons.Default.Wifi
                        "BLACKOUT" -> Icons.Default.NightsStay
                        "MUTE" -> Icons.AutoMirrored.Filled.VolumeOff
                        "SETTINGS" -> Icons.Default.Settings
                        "DAY_NIGHT" -> Icons.Default.Brightness4
                        else -> Icons.Default.Tune
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape((6 * us).dp))
                            .background(if (isChecked) effectiveAccent.copy(alpha = if (isDayMode) 0.08f else 0.15f) else cardBg)
                            .border(
                                width = 1.dp,
                                color = if (isChecked) effectiveAccent.copy(alpha = if (isDayMode) 0.45f else 0.6f) else (if (isDayMode) Color(0xFFCED4DA) else dialogBorder),
                                shape = RoundedCornerShape((6 * us).dp)
                            )
                            .clickable { onToggleAction(id) }
                            .padding(horizontal = (12 * us).dp, vertical = (8 * us).dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy((10 * us).dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size((32 * us).dp)
                                .clip(RoundedCornerShape((4 * us).dp))
                                .background(if (isChecked) effectiveAccent.copy(alpha = if (isDayMode) 0.15f else 0.25f) else (if (isDayMode) Color(0xFFE9ECEF) else Color(0xFF222222))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = actionIcon,
                                contentDescription = title,
                                tint = if (isChecked) effectiveAccent else subTextColor,
                                modifier = Modifier.size((18 * us).dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                color = if (isChecked) textColor else textColor.copy(alpha = 0.7f),
                                fontSize = (11 * ts).sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = desc,
                                color = subTextColor,
                                fontSize = (8.5f * ts).sp
                            )
                        }

                        // Check indicator chip
                        val checkmarkColor = if (effectiveAccent.luminance() > 0.5f) Color.Black else Color.White
                        Box(
                            modifier = Modifier
                                .size((22 * us).dp)
                                .clip(RoundedCornerShape((4 * us).dp))
                                .background(if (isChecked) effectiveAccent else Color.Transparent)
                                .border(1.dp, if (isChecked) effectiveAccent else (if (isDayMode) Color(0xFFCED4DA) else dialogBorder), RoundedCornerShape((4 * us).dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isChecked) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = checkmarkColor,
                                    modifier = Modifier.size((14 * us).dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height((14 * us).dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RESET DEFAULT",
                    color = subTextColor,
                    fontSize = (10 * ts).sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (1 * ts).sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape((4 * us).dp))
                        .clickable(onClick = onResetToDefault)
                        .padding(horizontal = (8 * us).dp, vertical = (6 * us).dp)
                )

                val doneContentColor = if (effectiveAccent.luminance() > 0.5f) Color.Black else Color.White
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape((4 * us).dp))
                        .background(effectiveAccent)
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = (16 * us).dp, vertical = (8 * us).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "DONE",
                        color = doneContentColor,
                        fontSize = (11 * ts).sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (1 * ts).sp
                    )
                }
            }
        }
    }
}

// ── Widget light glare ──────────────────────────────────────────────────────

/**
 * Single "passing glint" light source for the whole widget area. [sweep] is -1
 * while resting (hidden) and 0..1 during one pass; [angle] is the pass's random
 * 25–60° sweep direction. The band is drawn once here and clipped to the union of
 * the widget [rects], so it appears only over the tiles and sweeps across all of
 * them coherently regardless of their individual proportions.
 */
@Composable
private fun GlareOverlay(
    sweep: () -> Float,
    angle: State<Float>,
    intensity: Float,
    rects: List<Rect>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.drawBehind {
            val p = sweep()
            if (p < 0f || rects.isEmpty()) return@drawBehind

            val env = sin(PI.toFloat() * p)             // fade in → peak → fade out
            val alpha = env * intensity
            if (alpha <= 0.002f) return@drawBehind

            val rad = angle.value * (PI.toFloat() / 180f)
            val dx = cos(rad)                          // sweep direction
            val dy = sin(rad)
            val px = -dy                               // band normal (soft edges)
            val py = dx

            val cxm = size.width * 0.5f
            val cym = size.height * 0.5f
            // Half-extent of the screen projected onto the sweep direction, plus
            // band half-width, so the band is fully off-screen at p=0 and p=1.
            val extent = (size.width * abs(dx) + size.height * abs(dy)) * 0.5f
            val bandHalfW = (size.width + size.height) * 0.28f
            val travel = extent + bandHalfW
            val center = Offset(
                cxm + dx * (p * 2f - 1f) * travel,
                cym + dy * (p * 2f - 1f) * travel
            )

            val brush = Brush.linearGradient(
                colorStops = arrayOf(
                    0.00f to Color.Transparent,
                    0.38f to Color.Transparent,
                    0.50f to Color.White.copy(alpha = alpha),
                    0.62f to Color.Transparent,
                    1.00f to Color.Transparent
                ),
                start = Offset(center.x - px * bandHalfW, center.y - py * bandHalfW),
                end = Offset(center.x + px * bandHalfW, center.y + py * bandHalfW)
            )
            // Clip to each widget rect so the light appears only over the tiles.
            for (r in rects) {
                clipRect(r.left, r.top, r.right, r.bottom) {
                    drawRect(brush = brush)
                }
            }
        }
    )
}

// ── Vitals Elements Configuration Dialog ──────────────────────────────────────

@Composable
private fun ObdMetricsConfigDialog(
    enabled: List<String>,
    accent: Color,
    isDayMode: Boolean,
    onToggle: (String) -> Unit,
    onResetToDefault: () -> Unit,
    onDismiss: () -> Unit
) {
    val accentLum = 0.299f * accent.red + 0.587f * accent.green + 0.114f * accent.blue
    val effectiveAccent = if (isDayMode && accentLum > 0.65f) Color(0xFF111111)
                          else if (!isDayMode && accentLum < 0.25f) Color(0xFFEEEEEE)
                          else accent

    val dialogBg = if (isDayMode) Color(0xFFFFFFFF) else Color(0xFF111111)
    val dialogBorder = if (isDayMode) Color(0xFFDDE1E5) else Color(0xFF222222)
    val cardBg = if (isDayMode) Color(0xFFF8F9FA) else Color(0xFF181818)
    val textColor = if (isDayMode) Color(0xFF111111) else Color(0xFFEEEEEE)
    val subTextColor = if (isDayMode) Color(0xFF666666) else Color(0xFF888888)

    val scales = LocalUiScales.current
    val ts = scales.ts()
    val us = scales.us()

    val allMetrics = listOf(
        Triple("RPM", "RPM", "Engine revolutions per minute"),
        Triple("SPEED", "SPEED", "Vehicle road speed"),
        Triple("COOLANT", "COOLANT", "Engine coolant temperature"),
        Triple("THROTTLE", "THROTTLE", "Throttle position percentage"),
        Triple("LOAD", "LOAD", "Calculated engine load"),
        Triple("FUEL", "FUEL", "Fuel level percentage"),
        Triple("INTAKE", "INTAKE", "Intake air temperature"),
        Triple("BATTERY", "BATTERY", "Battery voltage")
    )

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape((8 * us).dp))
                .background(dialogBg)
                .border(1.dp, dialogBorder, RoundedCornerShape((8 * us).dp))
                .padding((16 * us).dp)
                .widthIn(min = (340 * us).dp, max = (480 * us).dp)
                .heightIn(max = (560 * us).dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = (12 * us).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CONFIGURE OBD METRICS",
                        color = if (isDayMode) Color(0xFF333333) else Color(0xFF888888),
                        fontSize = (11 * ts).sp,
                        letterSpacing = (1.5f * ts).sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Select which live values appear in the OBD2 widget",
                        color = subTextColor,
                        fontSize = (9 * ts).sp
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size((24 * us).dp)) {
                    Icon(Icons.Default.Close, null, tint = if (isDayMode) Color(0xFF666666) else Color(0xFF888888), modifier = Modifier.size((16 * us).dp))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy((8 * us).dp)
            ) {
                allMetrics.forEach { (id, title, desc) ->
                    val isChecked = enabled.any { it.equals(id, ignoreCase = true) }
                    val metricIcon = when (id) {
                        "RPM" -> Icons.Default.Timeline
                        "SPEED" -> Icons.Default.Speed
                        "COOLANT" -> Icons.Default.Thermostat
                        "THROTTLE" -> Icons.Default.TrendingUp
                        "LOAD" -> Icons.Default.Memory
                        "FUEL" -> Icons.Default.LocalGasStation
                        "INTAKE" -> Icons.Default.Air
                        "BATTERY" -> Icons.Default.BatteryFull
                        else -> Icons.Default.Speed
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape((6 * us).dp))
                            .background(if (isChecked) effectiveAccent.copy(alpha = if (isDayMode) 0.08f else 0.15f) else cardBg)
                            .border(
                                width = 1.dp,
                                color = if (isChecked) effectiveAccent.copy(alpha = if (isDayMode) 0.45f else 0.6f) else (if (isDayMode) Color(0xFFCED4DA) else dialogBorder),
                                shape = RoundedCornerShape((6 * us).dp)
                            )
                            .clickable { onToggle(id) }
                            .padding(horizontal = (12 * us).dp, vertical = (8 * us).dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy((10 * us).dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size((32 * us).dp)
                                .clip(RoundedCornerShape((4 * us).dp))
                                .background(if (isChecked) effectiveAccent.copy(alpha = if (isDayMode) 0.15f else 0.25f) else (if (isDayMode) Color(0xFFE9ECEF) else Color(0xFF222222))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = metricIcon,
                                contentDescription = title,
                                tint = if (isChecked) effectiveAccent else subTextColor,
                                modifier = Modifier.size((18 * us).dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                color = if (isChecked) textColor else textColor.copy(alpha = 0.7f),
                                fontSize = (11 * ts).sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = desc,
                                color = subTextColor,
                                fontSize = (8.5f * ts).sp
                            )
                        }

                        // Check indicator chip
                        val checkmarkColor = if (effectiveAccent.luminance() > 0.5f) Color.Black else Color.White
                        Box(
                            modifier = Modifier
                                .size((22 * us).dp)
                                .clip(RoundedCornerShape((4 * us).dp))
                                .background(if (isChecked) effectiveAccent else Color.Transparent)
                                .border(1.dp, if (isChecked) effectiveAccent else (if (isDayMode) Color(0xFFCED4DA) else dialogBorder), RoundedCornerShape((4 * us).dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isChecked) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = checkmarkColor,
                                    modifier = Modifier.size((14 * us).dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height((14 * us).dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RESET DEFAULT",
                    color = subTextColor,
                    fontSize = (10 * ts).sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (1 * ts).sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape((4 * us).dp))
                        .clickable(onClick = onResetToDefault)
                        .padding(horizontal = (8 * us).dp, vertical = (6 * us).dp)
                )

                val doneContentColor = if (effectiveAccent.luminance() > 0.5f) Color.Black else Color.White
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape((4 * us).dp))
                        .background(effectiveAccent)
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = (16 * us).dp, vertical = (8 * us).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "DONE",
                        color = doneContentColor,
                        fontSize = (11 * ts).sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (1 * ts).sp
                    )
                }
            }
        }
    }
}

@Composable
private fun WeatherSettingsDialog(
    provider: WeatherProvider,
    useGps: Boolean,
    customLocation: String,
    forecastDays: Int,
    accent: Color,
    isDayMode: Boolean,
    onSetProvider: (WeatherProvider) -> Unit,
    onSetUseGps: (Boolean) -> Unit,
    onSetCustomLocation: (String) -> Unit,
    onSetForecastDays: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val accentLum = 0.299f * accent.red + 0.587f * accent.green + 0.114f * accent.blue
    val effectiveAccent = if (isDayMode && accentLum > 0.65f) Color(0xFF111111)
                          else if (!isDayMode && accentLum < 0.25f) Color(0xFFEEEEEE)
                          else accent

    val dialogBg = if (isDayMode) Color(0xFFFFFFFF) else Color(0xFF111111)
    val dialogBorder = if (isDayMode) Color(0xFFDDE1E5) else Color(0xFF222222)
    val cardBg = if (isDayMode) Color(0xFFF8F9FA) else Color(0xFF181818)
    val textColor = if (isDayMode) Color(0xFF111111) else Color(0xFFEEEEEE)
    val subTextColor = if (isDayMode) Color(0xFF666666) else Color(0xFF888888)

    val scales = LocalUiScales.current
    val ts = scales.ts()
    val us = scales.us()

    var locationInput by remember(customLocation) { mutableStateOf(customLocation) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape((8 * us).dp))
                .background(dialogBg)
                .border(1.dp, dialogBorder, RoundedCornerShape((8 * us).dp))
                .padding((16 * us).dp)
                .widthIn(min = (340 * us).dp, max = (480 * us).dp)
                .heightIn(max = (560 * us).dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = (12 * us).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "WEATHER SETTINGS",
                        color = if (isDayMode) Color(0xFF333333) else Color(0xFF888888),
                        fontSize = (11 * ts).sp,
                        letterSpacing = (1.5f * ts).sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Configure provider, location and forecast",
                        color = subTextColor,
                        fontSize = (9 * ts).sp
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size((24 * us).dp)) {
                    Icon(Icons.Default.Close, null, tint = if (isDayMode) Color(0xFF666666) else Color(0xFF888888), modifier = Modifier.size((16 * us).dp))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy((8 * us).dp)
            ) {
                // Service provider
                Text(
                    text = "SERVICE PROVIDER",
                    color = subTextColor,
                    fontSize = (9 * ts).sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (1 * ts).sp
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape((4 * us).dp))
                        .background(if (isDayMode) Color(0xFFE9ECEF) else Color(0xFF222222))
                        .padding((2 * us).dp)
                ) {
                    WeatherProvider.entries.forEach { prov ->
                        val selected = provider == prov
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape((3 * us).dp))
                                .background(if (selected) effectiveAccent else Color.Transparent)
                                .clickable { onSetProvider(prov) }
                                .padding(horizontal = (10 * us).dp, vertical = (6 * us).dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (prov == WeatherProvider.WTTR_IN) "wttr.in" else "Open-Meteo",
                                color = if (selected) {
                                    if (effectiveAccent.luminance() > 0.5f) Color.Black else Color.White
                                } else subTextColor,
                                fontSize = (11 * ts).sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Use dynamic GPS location toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape((6 * us).dp))
                        .background(cardBg)
                        .border(1.dp, if (isDayMode) Color(0xFFCED4DA) else dialogBorder, RoundedCornerShape((6 * us).dp))
                        .padding(horizontal = (12 * us).dp, vertical = (8 * us).dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((10 * us).dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Use Dynamic GPS Location",
                            color = textColor,
                            fontSize = (11 * ts).sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (useGps) "Weather tracks vehicle GPS coordinates" else "Uses the custom location below",
                            color = subTextColor,
                            fontSize = (8.5f * ts).sp
                        )
                    }
                    Switch(
                        checked = useGps,
                        onCheckedChange = onSetUseGps,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = effectiveAccent,
                            uncheckedThumbColor = subTextColor,
                            uncheckedTrackColor = if (isDayMode) Color(0xFFE9ECEF) else Color(0xFF222222)
                        )
                    )
                }

                // Custom location / fallback city
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape((6 * us).dp))
                        .background(cardBg)
                        .border(1.dp, if (isDayMode) Color(0xFFCED4DA) else dialogBorder, RoundedCornerShape((6 * us).dp))
                        .padding(horizontal = (12 * us).dp, vertical = (8 * us).dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((10 * us).dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Custom Location / Fallback City",
                            color = textColor,
                            fontSize = (11 * ts).sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "City name or coordinates",
                            color = subTextColor,
                            fontSize = (8.5f * ts).sp
                        )
                    }
                    BasicTextField(
                        value = locationInput,
                        onValueChange = {
                            locationInput = it
                            onSetCustomLocation(it)
                        },
                        textStyle = TextStyle(
                            color = textColor,
                            fontSize = (11 * ts).sp,
                            fontWeight = FontWeight.Medium
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(effectiveAccent),
                        modifier = Modifier
                            .widthIn(min = (120 * us).dp, max = (180 * us).dp)
                            .clip(RoundedCornerShape((4 * us).dp))
                            .background(if (isDayMode) Color(0xFFE0E0E0) else Color(0xFF222222))
                            .padding(horizontal = (8 * us).dp, vertical = (6 * us).dp)
                    )
                }

                // Forecast days
                Text(
                    text = "FORECAST DAYS",
                    color = subTextColor,
                    fontSize = (9 * ts).sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (1 * ts).sp
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape((4 * us).dp))
                        .background(if (isDayMode) Color(0xFFE9ECEF) else Color(0xFF222222))
                        .padding((2 * us).dp)
                ) {
                    listOf(3, 5, 7).forEach { days ->
                        val selected = forecastDays == days
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape((3 * us).dp))
                                .background(if (selected) effectiveAccent else Color.Transparent)
                                .clickable { onSetForecastDays(days) }
                                .padding(horizontal = (10 * us).dp, vertical = (6 * us).dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$days Days",
                                color = if (selected) {
                                    if (effectiveAccent.luminance() > 0.5f) Color.Black else Color.White
                                } else subTextColor,
                                fontSize = (11 * ts).sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height((14 * us).dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val doneContentColor = if (effectiveAccent.luminance() > 0.5f) Color.Black else Color.White
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape((4 * us).dp))
                        .background(effectiveAccent)
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = (16 * us).dp, vertical = (8 * us).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "DONE",
                        color = doneContentColor,
                        fontSize = (11 * ts).sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (1 * ts).sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MapSettingsDialog(
    provider: MapProvider,
    controlScale: MapControlScale,
    mapType: MapType,
    showTraffic: Boolean,
    accent: Color,
    isDayMode: Boolean,
    onSetProvider: (MapProvider) -> Unit,
    onSetControlScale: (MapControlScale) -> Unit,
    onSetMapType: (MapType) -> Unit,
    onToggleTraffic: () -> Unit,
    onDismiss: () -> Unit
) {
    val accentLum = 0.299f * accent.red + 0.587f * accent.green + 0.114f * accent.blue
    val effectiveAccent = if (isDayMode && accentLum > 0.65f) Color(0xFF111111)
                          else if (!isDayMode && accentLum < 0.25f) Color(0xFFEEEEEE)
                          else accent

    val dialogBg = if (isDayMode) Color(0xFFFFFFFF) else Color(0xFF111111)
    val dialogBorder = if (isDayMode) Color(0xFFDDE1E5) else Color(0xFF222222)
    val cardBg = if (isDayMode) Color(0xFFF8F9FA) else Color(0xFF181818)
    val textColor = if (isDayMode) Color(0xFF111111) else Color(0xFFEEEEEE)
    val subTextColor = if (isDayMode) Color(0xFF666666) else Color(0xFF888888)

    val scales = LocalUiScales.current
    val ts = scales.ts()
    val us = scales.us()

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape((8 * us).dp))
                .background(dialogBg)
                .border(1.dp, dialogBorder, RoundedCornerShape((8 * us).dp))
                .padding((16 * us).dp)
                .widthIn(min = (340 * us).dp, max = (480 * us).dp)
                .heightIn(max = (560 * us).dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = (12 * us).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MAP SETTINGS",
                        color = if (isDayMode) Color(0xFF333333) else Color(0xFF888888),
                        fontSize = (11 * ts).sp,
                        letterSpacing = (1.5f * ts).sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Provider, map style, controls and traffic",
                        color = subTextColor,
                        fontSize = (9 * ts).sp
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size((24 * us).dp)) {
                    Icon(Icons.Default.Close, null, tint = if (isDayMode) Color(0xFF666666) else Color(0xFF888888), modifier = Modifier.size((16 * us).dp))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy((8 * us).dp)
            ) {
                // Map provider
                Text(
                    text = "MAP PROVIDER",
                    color = subTextColor,
                    fontSize = (9 * ts).sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (1 * ts).sp
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape((4 * us).dp))
                        .background(if (isDayMode) Color(0xFFE9ECEF) else Color(0xFF222222))
                        .padding((2 * us).dp)
                ) {
                    MapProvider.entries.forEach { prov ->
                        val selected = provider == prov
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape((3 * us).dp))
                                .background(if (selected) effectiveAccent else Color.Transparent)
                                .clickable { onSetProvider(prov) }
                                .padding(horizontal = (10 * us).dp, vertical = (6 * us).dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (prov == MapProvider.GOOGLE) "Google" else "OSM",
                                color = if (selected) {
                                    if (effectiveAccent.luminance() > 0.5f) Color.Black else Color.White
                                } else subTextColor,
                                fontSize = (11 * ts).sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Control size
                Text(
                    text = "CONTROL SIZE",
                    color = subTextColor,
                    fontSize = (9 * ts).sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (1 * ts).sp
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape((4 * us).dp))
                        .background(if (isDayMode) Color(0xFFE9ECEF) else Color(0xFF222222))
                        .padding((2 * us).dp)
                ) {
                    MapControlScale.entries.forEach { scale ->
                        val selected = controlScale == scale
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape((3 * us).dp))
                                .background(if (selected) effectiveAccent else Color.Transparent)
                                .clickable { onSetControlScale(scale) }
                                .padding(horizontal = (10 * us).dp, vertical = (6 * us).dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (scale == MapControlScale.NORMAL) "Normal" else "Large",
                                color = if (selected) {
                                    if (effectiveAccent.luminance() > 0.5f) Color.Black else Color.White
                                } else subTextColor,
                                fontSize = (11 * ts).sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Map type
                Text(
                    text = "MAP TYPE",
                    color = subTextColor,
                    fontSize = (9 * ts).sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (1 * ts).sp
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape((4 * us).dp))
                        .background(if (isDayMode) Color(0xFFE9ECEF) else Color(0xFF222222))
                        .padding((2 * us).dp)
                ) {
                    val typeLabels = mapOf(
                        MapType.ROADMAP to "Roadmap",
                        MapType.SATELLITE to "Satellite",
                        MapType.HYBRID to "Hybrid",
                        MapType.TERRAIN to "Terrain"
                    )
                    MapType.entries.forEach { type ->
                        val selected = mapType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape((3 * us).dp))
                                .background(if (selected) effectiveAccent else Color.Transparent)
                                .clickable { onSetMapType(type) }
                                .padding(horizontal = (6 * us).dp, vertical = (6 * us).dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = typeLabels[type] ?: type.name,
                                color = if (selected) {
                                    if (effectiveAccent.luminance() > 0.5f) Color.Black else Color.White
                                } else subTextColor,
                                fontSize = (10 * ts).sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Live traffic toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape((6 * us).dp))
                        .background(cardBg)
                        .border(1.dp, if (isDayMode) Color(0xFFCED4DA) else dialogBorder, RoundedCornerShape((6 * us).dp))
                        .padding(horizontal = (12 * us).dp, vertical = (8 * us).dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((10 * us).dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Live Traffic",
                            color = textColor,
                            fontSize = (11 * ts).sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Overlay traffic conditions on the map",
                            color = subTextColor,
                            fontSize = (8.5f * ts).sp
                        )
                    }
                    Switch(
                        checked = showTraffic,
                        onCheckedChange = { onToggleTraffic() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = effectiveAccent,
                            uncheckedThumbColor = subTextColor,
                            uncheckedTrackColor = if (isDayMode) Color(0xFFE9ECEF) else Color(0xFF222222)
                        )
                    )
                }
            }

            Spacer(Modifier.height((14 * us).dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val doneContentColor = if (effectiveAccent.luminance() > 0.5f) Color.Black else Color.White
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape((4 * us).dp))
                        .background(effectiveAccent)
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = (16 * us).dp, vertical = (8 * us).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "DONE",
                        color = doneContentColor,
                        fontSize = (11 * ts).sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (1 * ts).sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogSliderRow(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    accent: Color,
    isDayMode: Boolean,
    onChange: (Float) -> Unit
) {
    val accentLum = 0.299f * accent.red + 0.587f * accent.green + 0.114f * accent.blue
    val effectiveAccent = if (isDayMode && accentLum > 0.65f) Color(0xFF111111)
                          else if (!isDayMode && accentLum < 0.25f) Color(0xFFEEEEEE)
                          else accent

    val cardBg = if (isDayMode) Color(0xFFF8F9FA) else Color(0xFF181818)
    val dialogBorder = if (isDayMode) Color(0xFFDDE1E5) else Color(0xFF222222)
    val textColor = if (isDayMode) Color(0xFF111111) else Color(0xFFEEEEEE)
    val subTextColor = if (isDayMode) Color(0xFF666666) else Color(0xFF888888)

    val scales = LocalUiScales.current
    val ts = scales.ts()
    val us = scales.us()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape((6 * us).dp))
            .background(cardBg)
            .border(1.dp, if (isDayMode) Color(0xFFCED4DA) else dialogBorder, RoundedCornerShape((6 * us).dp))
            .padding(horizontal = (12 * us).dp, vertical = (8 * us).dp)
    ) {
        Column {
            Text(
                text = label,
                color = textColor,
                fontSize = (11 * ts).sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = valueText,
                color = subTextColor,
                fontSize = (8.5f * ts).sp
            )
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = effectiveAccent,
                activeTrackColor = effectiveAccent,
                inactiveTrackColor = if (isDayMode) Color(0xFFCCCCCC) else Color(0xFF2A2A2A)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SpectrumSettingsDialog(
    page: Int,
    settings: AppSettings,
    onUpdateSettings: (AppSettings.() -> AppSettings) -> Unit,
    accent: Color,
    isDayMode: Boolean,
    onDismiss: () -> Unit
) {
    val accentLum = 0.299f * accent.red + 0.587f * accent.green + 0.114f * accent.blue
    val effectiveAccent = if (isDayMode && accentLum > 0.65f) Color(0xFF111111)
                          else if (!isDayMode && accentLum < 0.25f) Color(0xFFEEEEEE)
                          else accent

    val dialogBg = if (isDayMode) Color(0xFFFFFFFF) else Color(0xFF111111)
    val dialogBorder = if (isDayMode) Color(0xFFDDE1E5) else Color(0xFF222222)
    val cardBg = if (isDayMode) Color(0xFFF8F9FA) else Color(0xFF181818)
    val textColor = if (isDayMode) Color(0xFF111111) else Color(0xFFEEEEEE)
    val subTextColor = if (isDayMode) Color(0xFF666666) else Color(0xFF888888)

    val scales = LocalUiScales.current
    val ts = scales.ts()
    val us = scales.us()

    val context = LocalContext.current
    var showColorPicker by remember { mutableStateOf(false) }

    val spectrumImagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onUpdateSettings { withWidgetContent(page) { c -> c.copy(spectrumFallbackImageUri = it.toString()) } }
        }
    }

    val content = settings.widgetContentFor(page)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape((8 * us).dp))
                .background(dialogBg)
                .border(1.dp, dialogBorder, RoundedCornerShape((8 * us).dp))
                .padding((16 * us).dp)
                .widthIn(min = (340 * us).dp, max = (480 * us).dp)
                .heightIn(max = (560 * us).dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = (12 * us).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SPECTRUM SETTINGS",
                        color = if (isDayMode) Color(0xFF333333) else Color(0xFF888888),
                        fontSize = (11 * ts).sp,
                        letterSpacing = (1.5f * ts).sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Motion, style, color and fallback image",
                        color = subTextColor,
                        fontSize = (9 * ts).sp
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size((24 * us).dp)) {
                    Icon(Icons.Default.Close, null, tint = if (isDayMode) Color(0xFF666666) else Color(0xFF888888), modifier = Modifier.size((16 * us).dp))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy((8 * us).dp)
            ) {
                DialogSliderRow(
                    label = "Rise Speed",
                    valueText = "${"%.0f".format(content.spectrumAttackMs)}ms — how fast bars rise (lower = snappier)",
                    value = content.spectrumAttackMs,
                    valueRange = 5f..200f,
                    steps = 19,
                    accent = accent,
                    isDayMode = isDayMode,
                    onChange = { v -> onUpdateSettings { withWidgetContent(page) { c -> c.copy(spectrumAttackMs = v) } } }
                )
                DialogSliderRow(
                    label = "Fall-off Speed",
                    valueText = "${"%.0f".format(content.spectrumDecayMs)}ms — how fast bars fall (lower = faster fall)",
                    value = content.spectrumDecayMs,
                    valueRange = 50f..1500f,
                    steps = 28,
                    accent = accent,
                    isDayMode = isDayMode,
                    onChange = { v -> onUpdateSettings { withWidgetContent(page) { c -> c.copy(spectrumDecayMs = v) } } }
                )
                DialogSliderRow(
                    label = "Peak / Low Contrast",
                    valueText = "${"%.2f".format(content.spectrumResponse)}× — higher = bigger gap between loud peaks and quiet lows",
                    value = content.spectrumResponse,
                    valueRange = 0.4f..2.5f,
                    steps = 20,
                    accent = accent,
                    isDayMode = isDayMode,
                    onChange = { v -> onUpdateSettings { withWidgetContent(page) { c -> c.copy(spectrumResponse = v) } } }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape((6 * us).dp))
                        .background(cardBg)
                        .border(1.dp, if (isDayMode) Color(0xFFCED4DA) else dialogBorder, RoundedCornerShape((6 * us).dp))
                        .padding(horizontal = (12 * us).dp, vertical = (8 * us).dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((10 * us).dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto Gain (AGC)",
                            color = textColor,
                            fontSize = (11 * ts).sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Boosts quiet audio and tames loud audio so the bars track the music level",
                            color = subTextColor,
                            fontSize = (8.5f * ts).sp
                        )
                    }
                    Switch(
                        checked = content.spectrumAgcEnabled,
                        onCheckedChange = { on -> onUpdateSettings { withWidgetContent(page) { c -> c.copy(spectrumAgcEnabled = on) } } },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = effectiveAccent,
                            uncheckedThumbColor = subTextColor,
                            uncheckedTrackColor = if (isDayMode) Color(0xFFE9ECEF) else Color(0xFF222222)
                        )
                    )
                }
                DialogSliderRow(
                    label = "AGC Target Level",
                    valueText = "${"%.0f".format(content.spectrumAgcTarget * 100)}% — how high the bars should reach",
                    value = content.spectrumAgcTarget,
                    valueRange = 0.2f..1f,
                    steps = 15,
                    accent = accent,
                    isDayMode = isDayMode,
                    onChange = { v -> onUpdateSettings { withWidgetContent(page) { c -> c.copy(spectrumAgcTarget = v) } } }
                )
                DialogSliderRow(
                    label = "Spectrum Bars",
                    valueText = "${content.spectrumBarCount} bars — the audio spectrum resolution",
                    value = content.spectrumBarCount.toFloat(),
                    valueRange = 8f..64f,
                    steps = 13,
                    accent = accent,
                    isDayMode = isDayMode,
                    onChange = { v -> onUpdateSettings { withWidgetContent(page) { c -> c.copy(spectrumBarCount = v.toInt().coerceIn(8, 64)) } } }
                )

                // Spectrum style (segmented)
                Text(
                    text = "SPECTRUM STYLE",
                    color = subTextColor,
                    fontSize = (9 * ts).sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (1 * ts).sp
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape((4 * us).dp))
                        .background(if (isDayMode) Color(0xFFE9ECEF) else Color(0xFF222222))
                        .padding((2 * us).dp)
                ) {
                    listOf(
                        "BARS" to SpectrumStyle.BARS,
                        "ROUND" to SpectrumStyle.ROUNDED,
                        "DOTS" to SpectrumStyle.DOTS
                    ).forEach { (label, st) ->
                        val selected = content.spectrumStyle == st
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape((3 * us).dp))
                                .background(if (selected) effectiveAccent else Color.Transparent)
                                .clickable { onUpdateSettings { withWidgetContent(page) { c -> c.copy(spectrumStyle = st) } } }
                                .padding(vertical = (6 * us).dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (selected) {
                                    if (effectiveAccent.luminance() > 0.5f) Color.Black else Color.White
                                } else subTextColor,
                                fontSize = (10 * ts).sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Mirror spectrum (toggle)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape((6 * us).dp))
                        .background(cardBg)
                        .border(1.dp, if (isDayMode) Color(0xFFCED4DA) else dialogBorder, RoundedCornerShape((6 * us).dp))
                        .padding(horizontal = (12 * us).dp, vertical = (8 * us).dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((10 * us).dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mirror Spectrum",
                            color = textColor,
                            fontSize = (11 * ts).sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Reflect the bars around the vertical center",
                            color = subTextColor,
                            fontSize = (8.5f * ts).sp
                        )
                    }
                    Switch(
                        checked = content.spectrumMirror,
                        onCheckedChange = { mirror -> onUpdateSettings { withWidgetContent(page) { c -> c.copy(spectrumMirror = mirror) } } },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = effectiveAccent,
                            uncheckedThumbColor = subTextColor,
                            uncheckedTrackColor = if (isDayMode) Color(0xFFE9ECEF) else Color(0xFF222222)
                        )
                    )
                }

                // Spectrum color (accent-follow toggle + pick)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape((6 * us).dp))
                        .background(cardBg)
                        .border(1.dp, if (isDayMode) Color(0xFFCED4DA) else dialogBorder, RoundedCornerShape((6 * us).dp))
                        .padding(horizontal = (12 * us).dp, vertical = (8 * us).dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((6 * us).dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Spectrum Color",
                            color = textColor,
                            fontSize = (11 * ts).sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (content.spectrumColor == null) "Follows the accent color" else "Custom color",
                            color = subTextColor,
                            fontSize = (8.5f * ts).sp
                        )
                    }
                    Text("Accent", color = subTextColor, fontSize = (10 * ts).sp)
                    Switch(
                        checked = content.spectrumColor == null,
                        onCheckedChange = { follow ->
                            onUpdateSettings {
                                withWidgetContent(page) {
                                    it.copy(spectrumColor = if (follow) null else (it.spectrumColor ?: accent.toArgb()))
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = effectiveAccent,
                            uncheckedThumbColor = subTextColor,
                            uncheckedTrackColor = if (isDayMode) Color(0xFFE9ECEF) else Color(0xFF222222)
                        )
                    )
                    TextButton(onClick = { showColorPicker = true }) {
                        Text("Pick", color = effectiveAccent, fontSize = (11 * ts).sp, fontWeight = FontWeight.Bold)
                    }
                }

                DialogSliderRow(
                    label = "Spectrum Gain",
                    valueText = "${"%.0f".format(content.spectrumGain * 100)}% — bar sensitivity",
                    value = content.spectrumGain,
                    valueRange = 0.3f..3f,
                    steps = 13,
                    accent = accent,
                    isDayMode = isDayMode,
                    onChange = { v -> onUpdateSettings { withWidgetContent(page) { c -> c.copy(spectrumGain = v) } } }
                )

                // Fallback image
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape((6 * us).dp))
                        .background(cardBg)
                        .border(1.dp, if (isDayMode) Color(0xFFCED4DA) else dialogBorder, RoundedCornerShape((6 * us).dp))
                        .padding(horizontal = (12 * us).dp, vertical = (8 * us).dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((6 * us).dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Fallback Image",
                            color = textColor,
                            fontSize = (11 * ts).sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (content.spectrumFallbackImageUri.isNotEmpty())
                                "Shown when there's no audio signal"
                            else
                                "Optional — e.g. a car logo shown when there's no audio signal",
                            color = subTextColor,
                            fontSize = (8.5f * ts).sp
                        )
                    }
                    TextButton(onClick = { spectrumImagePicker.launch(arrayOf("image/*")) }) {
                        Text(
                            if (content.spectrumFallbackImageUri.isEmpty()) "Choose" else "Change",
                            color = effectiveAccent,
                            fontSize = (11 * ts).sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (content.spectrumFallbackImageUri.isNotEmpty()) {
                        TextButton(onClick = { onUpdateSettings { withWidgetContent(page) { c -> c.copy(spectrumFallbackImageUri = "") } } }) {
                            Text("Clear", color = subTextColor, fontSize = (11 * ts).sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height((14 * us).dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val doneContentColor = if (effectiveAccent.luminance() > 0.5f) Color.Black else Color.White
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape((4 * us).dp))
                        .background(effectiveAccent)
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = (16 * us).dp, vertical = (8 * us).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "DONE",
                        color = doneContentColor,
                        fontSize = (11 * ts).sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (1 * ts).sp
                    )
                }
            }
        }
    }

    if (showColorPicker) {
        ColorPickerDialog(
            title = "Spectrum Color",
            initialColor = content.spectrumColor?.let { Color(it) } ?: accent,
            onColorSelected = { c ->
                onUpdateSettings { withWidgetContent(page) { it.copy(spectrumColor = c.toArgb()) } }
            },
            onDismiss = { showColorPicker = false }
        )
    }
}

@Composable
private fun GalleryFallbackSettingsDialog(
    settings: AppSettings,
    onUpdateSettings: (AppSettings.() -> AppSettings) -> Unit,
    accent: Color,
    isDayMode: Boolean,
    onDismiss: () -> Unit
) {
    val accentLum = 0.299f * accent.red + 0.587f * accent.green + 0.114f * accent.blue
    val effectiveAccent = if (isDayMode && accentLum > 0.65f) Color(0xFF111111)
                          else if (!isDayMode && accentLum < 0.25f) Color(0xFFEEEEEE)
                          else accent

    val dialogBg = if (isDayMode) Color(0xFFFFFFFF) else Color(0xFF111111)
    val dialogBorder = if (isDayMode) Color(0xFFDDE1E5) else Color(0xFF222222)
    val cardBg = if (isDayMode) Color(0xFFF8F9FA) else Color(0xFF181818)
    val textColor = if (isDayMode) Color(0xFF111111) else Color(0xFFEEEEEE)
    val subTextColor = if (isDayMode) Color(0xFF666666) else Color(0xFF888888)

    val scales = LocalUiScales.current
    val ts = scales.ts()
    val us = scales.us()

    val context = LocalContext.current

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onUpdateSettings { copy(galleryDirectoryUri = it.toString()) }
        }
    }

    val folderName = remember(settings.galleryDirectoryUri) {
        val uriStr = settings.galleryDirectoryUri
        if (uriStr.isEmpty()) {
            "None selected"
        } else {
            runCatching {
                val treeUri = Uri.parse(uriStr)
                val docId = DocumentsContract.getTreeDocumentId(treeUri)
                val queried = context.contentResolver.query(
                    DocumentsContract.buildDocumentUriUsingTree(treeUri, docId),
                    arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                    null, null, null
                )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
                queried?.takeIf { it.isNotBlank() }
                    ?: docId.substringAfterLast(':').substringAfterLast('/').ifBlank { "Selected folder" }
            }.getOrNull() ?: "Selected folder"
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape((8 * us).dp))
                .background(dialogBg)
                .border(1.dp, dialogBorder, RoundedCornerShape((8 * us).dp))
                .padding((16 * us).dp)
                .widthIn(min = (340 * us).dp, max = (480 * us).dp)
                .heightIn(max = (560 * us).dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = (12 * us).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "FALLBACK GALLERY",
                        color = if (isDayMode) Color(0xFF333333) else Color(0xFF888888),
                        fontSize = (11 * ts).sp,
                        letterSpacing = (1.5f * ts).sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Slideshow shown while weather is offline",
                        color = subTextColor,
                        fontSize = (9 * ts).sp
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size((24 * us).dp)) {
                    Icon(Icons.Default.Close, null, tint = if (isDayMode) Color(0xFF666666) else Color(0xFF888888), modifier = Modifier.size((16 * us).dp))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy((8 * us).dp)
            ) {
                // Enable toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape((6 * us).dp))
                        .background(cardBg)
                        .border(1.dp, if (isDayMode) Color(0xFFCED4DA) else dialogBorder, RoundedCornerShape((6 * us).dp))
                        .padding(horizontal = (12 * us).dp, vertical = (8 * us).dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((10 * us).dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable",
                            color = textColor,
                            fontSize = (11 * ts).sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (settings.galleryFallbackEnabled) "Shows a folder slideshow when offline" else "Uses the standard offline placeholder",
                            color = subTextColor,
                            fontSize = (8.5f * ts).sp
                        )
                    }
                    Switch(
                        checked = settings.galleryFallbackEnabled,
                        onCheckedChange = { onUpdateSettings { copy(galleryFallbackEnabled = it) } },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = effectiveAccent,
                            uncheckedThumbColor = subTextColor,
                            uncheckedTrackColor = if (isDayMode) Color(0xFFE9ECEF) else Color(0xFF222222)
                        )
                    )
                }

                // Choose folder
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape((6 * us).dp))
                        .background(cardBg)
                        .border(1.dp, if (isDayMode) Color(0xFFCED4DA) else dialogBorder, RoundedCornerShape((6 * us).dp))
                        .padding(horizontal = (12 * us).dp, vertical = (8 * us).dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((6 * us).dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Choose Folder",
                            color = textColor,
                            fontSize = (11 * ts).sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = folderName,
                            color = subTextColor,
                            fontSize = (8.5f * ts).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(onClick = { folderPicker.launch(null) }) {
                        Text(
                            if (settings.galleryDirectoryUri.isEmpty()) "Choose" else "Change",
                            color = effectiveAccent,
                            fontSize = (11 * ts).sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (settings.galleryDirectoryUri.isNotEmpty()) {
                        TextButton(onClick = { onUpdateSettings { copy(galleryDirectoryUri = "") } }) {
                            Text("Clear", color = subTextColor, fontSize = (11 * ts).sp)
                        }
                    }
                }

                // Time per image
                DialogSliderRow(
                    label = "Time per Image",
                    valueText = "${settings.galleryIntervalMs / 1000}s — how long each image shows",
                    value = settings.galleryIntervalMs.toFloat(),
                    valueRange = 2000f..30000f,
                    steps = 27,
                    accent = accent,
                    isDayMode = isDayMode,
                    onChange = { v -> onUpdateSettings { copy(galleryIntervalMs = v.toLong().coerceIn(2000L, 30000L)) } }
                )

                // Transition duration
                DialogSliderRow(
                    label = "Transition Duration",
                    valueText = "${settings.galleryTransitionMs}ms — crossfade/slide/zoom length",
                    value = settings.galleryTransitionMs.toFloat(),
                    valueRange = 200f..3000f,
                    steps = 27,
                    accent = accent,
                    isDayMode = isDayMode,
                    onChange = { v -> onUpdateSettings { copy(galleryTransitionMs = v.toLong().coerceIn(200L, 3000L)) } }
                )

                // Effect (segmented)
                Text(
                    text = "EFFECT",
                    color = subTextColor,
                    fontSize = (9 * ts).sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (1 * ts).sp
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape((4 * us).dp))
                        .background(if (isDayMode) Color(0xFFE9ECEF) else Color(0xFF222222))
                        .padding((2 * us).dp)
                ) {
                    listOf(
                        "Crossfade" to GalleryEffect.CROSSFADE,
                        "Slide" to GalleryEffect.SLIDE,
                        "Zoom" to GalleryEffect.ZOOM,
                        "Fade" to GalleryEffect.FADE_THROUGH
                    ).forEach { (label, eff) ->
                        val selected = settings.galleryEffect == eff
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape((3 * us).dp))
                                .background(if (selected) effectiveAccent else Color.Transparent)
                                .clickable { onUpdateSettings { copy(galleryEffect = eff) } }
                                .padding(vertical = (6 * us).dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (selected) {
                                    if (effectiveAccent.luminance() > 0.5f) Color.Black else Color.White
                                } else subTextColor,
                                fontSize = (10 * ts).sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height((14 * us).dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val doneContentColor = if (effectiveAccent.luminance() > 0.5f) Color.Black else Color.White
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape((4 * us).dp))
                        .background(effectiveAccent)
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = (16 * us).dp, vertical = (8 * us).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "DONE",
                        color = doneContentColor,
                        fontSize = (11 * ts).sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (1 * ts).sp
                    )
                }
            }
        }
    }
}

@Composable
private fun NowPlayingSettingsDialog(
    settings: AppSettings,
    onUpdateSettings: (AppSettings.() -> AppSettings) -> Unit,
    installedApps: List<AppInfo>,
    accent: Color,
    isDayMode: Boolean,
    onDismiss: () -> Unit
) {
    val accentLum = 0.299f * accent.red + 0.587f * accent.green + 0.114f * accent.blue
    val effectiveAccent = if (isDayMode && accentLum > 0.65f) Color(0xFF111111)
                          else if (!isDayMode && accentLum < 0.25f) Color(0xFFEEEEEE)
                          else accent

    val dialogBg = if (isDayMode) Color(0xFFFFFFFF) else Color(0xFF111111)
    val dialogBorder = if (isDayMode) Color(0xFFDDE1E5) else Color(0xFF222222)
    val cardBg = if (isDayMode) Color(0xFFF8F9FA) else Color(0xFF181818)
    val textColor = if (isDayMode) Color(0xFF111111) else Color(0xFFEEEEEE)
    val subTextColor = if (isDayMode) Color(0xFF666666) else Color(0xFF888888)

    val scales = LocalUiScales.current
    val ts = scales.ts()
    val us = scales.us()

    var showRadioAppPicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape((8 * us).dp))
                .background(dialogBg)
                .border(1.dp, dialogBorder, RoundedCornerShape((8 * us).dp))
                .padding((16 * us).dp)
                .widthIn(min = (340 * us).dp, max = (480 * us).dp)
                .heightIn(max = (560 * us).dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = (12 * us).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "NOW PLAYING SETTINGS",
                        color = if (isDayMode) Color(0xFF333333) else Color(0xFF888888),
                        fontSize = (11 * ts).sp,
                        letterSpacing = (1.5f * ts).sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Radio app, bands, popup and artwork",
                        color = subTextColor,
                        fontSize = (9 * ts).sp
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size((24 * us).dp)) {
                    Icon(Icons.Default.Close, null, tint = if (isDayMode) Color(0xFF666666) else Color(0xFF888888), modifier = Modifier.size((16 * us).dp))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy((8 * us).dp)
            ) {
                // Radio app
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape((6 * us).dp))
                        .background(cardBg)
                        .border(1.dp, if (isDayMode) Color(0xFFCED4DA) else dialogBorder, RoundedCornerShape((6 * us).dp))
                        .padding(horizontal = (12 * us).dp, vertical = (8 * us).dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((6 * us).dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Radio App",
                            color = textColor,
                            fontSize = (11 * ts).sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (settings.radioPackage.isNotEmpty()) settings.radioPackage else "Auto-detect — or pick your head unit's radio app",
                            color = subTextColor,
                            fontSize = (8.5f * ts).sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(onClick = { showRadioAppPicker = true }) {
                        Text(
                            if (settings.radioPackage.isEmpty()) "Choose" else "Change",
                            color = effectiveAccent,
                            fontSize = (11 * ts).sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (settings.radioPackage.isNotEmpty()) {
                        TextButton(onClick = { onUpdateSettings { copy(radioPackage = "") } }) {
                            Text("Auto", color = subTextColor, fontSize = (11 * ts).sp)
                        }
                    }
                }

                // Show FM/AM band chips (toggle)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape((6 * us).dp))
                        .background(cardBg)
                        .border(1.dp, if (isDayMode) Color(0xFFCED4DA) else dialogBorder, RoundedCornerShape((6 * us).dp))
                        .padding(horizontal = (12 * us).dp, vertical = (8 * us).dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((10 * us).dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Show FM/AM Band Chips",
                            color = textColor,
                            fontSize = (11 * ts).sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Band switch chips in the radio deck (FM1/FM2/FM3/AM)",
                            color = subTextColor,
                            fontSize = (8.5f * ts).sp
                        )
                    }
                    Switch(
                        checked = settings.showRadioBands,
                        onCheckedChange = { bands -> onUpdateSettings { copy(showRadioBands = bands) } },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = effectiveAccent,
                            uncheckedThumbColor = subTextColor,
                            uncheckedTrackColor = if (isDayMode) Color(0xFFE9ECEF) else Color(0xFF222222)
                        )
                    )
                }

                // Station change popup (toggle)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape((6 * us).dp))
                        .background(cardBg)
                        .border(1.dp, if (isDayMode) Color(0xFFCED4DA) else dialogBorder, RoundedCornerShape((6 * us).dp))
                        .padding(horizontal = (12 * us).dp, vertical = (8 * us).dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((10 * us).dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Station Change Popup",
                            color = textColor,
                            fontSize = (11 * ts).sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Fade in a glass panel with station name, frequency and logo on seek/preset change",
                            color = subTextColor,
                            fontSize = (8.5f * ts).sp
                        )
                    }
                    Switch(
                        checked = settings.showRadioPopup,
                        onCheckedChange = { popup -> onUpdateSettings { copy(showRadioPopup = popup) } },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = effectiveAccent,
                            uncheckedThumbColor = subTextColor,
                            uncheckedTrackColor = if (isDayMode) Color(0xFFE9ECEF) else Color(0xFF222222)
                        )
                    )
                }

                DialogSliderRow(
                    label = "Artwork Size",
                    valueText = "${"%.0f".format(settings.nowPlayingLogoScale * 100)}% — player card logo/album art",
                    value = settings.nowPlayingLogoScale,
                    valueRange = 0.5f..1f,
                    steps = 4,
                    accent = accent,
                    isDayMode = isDayMode,
                    onChange = { v -> onUpdateSettings { copy(nowPlayingLogoScale = v) } }
                )
                DialogSliderRow(
                    label = "Artwork Dim",
                    valueText = "${"%.0f".format(settings.nowPlayingLogoDim * 100)}% dark veil — improves control/text contrast",
                    value = settings.nowPlayingLogoDim,
                    valueRange = 0f..0.8f,
                    steps = 7,
                    accent = accent,
                    isDayMode = isDayMode,
                    onChange = { v -> onUpdateSettings { copy(nowPlayingLogoDim = v) } }
                )
                DialogSliderRow(
                    label = "Artwork Edge Blend",
                    valueText = "${"%.0f".format(settings.nowPlayingArtBlend * 100)}% — fades artwork edges into the tile background",
                    value = settings.nowPlayingArtBlend,
                    valueRange = 0f..1f,
                    steps = 9,
                    accent = accent,
                    isDayMode = isDayMode,
                    onChange = { v -> onUpdateSettings { copy(nowPlayingArtBlend = v) } }
                )
            }

            Spacer(Modifier.height((14 * us).dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val doneContentColor = if (effectiveAccent.luminance() > 0.5f) Color.Black else Color.White
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape((4 * us).dp))
                        .background(effectiveAccent)
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = (16 * us).dp, vertical = (8 * us).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "DONE",
                        color = doneContentColor,
                        fontSize = (11 * ts).sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (1 * ts).sp
                    )
                }
            }
        }
    }

    if (showRadioAppPicker) {
        AppPickerDialog(
            title = "RADIO APP",
            currentPackage = settings.radioPackage,
            apps = installedApps,
            accent = accent,
            isDayMode = isDayMode,
            selectedAppPackages = settings.selectedAppPackages,
            toolsAppPackages = settings.toolsAppPackages,
            clearLabel = "AUTO-DETECT",
            onSelect = { pkg ->
                onUpdateSettings { copy(radioPackage = pkg) }
                showRadioAppPicker = false
            },
            onClear = { onUpdateSettings { copy(radioPackage = "") } },
            onDismiss = { showRadioAppPicker = false }
        )
    }
}

@Composable
private fun VitalsConfigDialog(
    enabledElements: List<String>,
    accent: Color,
    isDayMode: Boolean,
    onToggleElement: (String) -> Unit,
    onResetToDefault: () -> Unit,
    onDismiss: () -> Unit
) {
    val accentLum = 0.299f * accent.red + 0.587f * accent.green + 0.114f * accent.blue
    val effectiveAccent = if (isDayMode && accentLum > 0.65f) Color(0xFF111111)
                          else if (!isDayMode && accentLum < 0.25f) Color(0xFFEEEEEE)
                          else accent

    val dialogBg = if (isDayMode) Color(0xFFFFFFFF) else Color(0xFF111111)
    val dialogBorder = if (isDayMode) Color(0xFFDDE1E5) else Color(0xFF222222)
    val cardBg = if (isDayMode) Color(0xFFF8F9FA) else Color(0xFF181818)
    val textColor = if (isDayMode) Color(0xFF111111) else Color(0xFFEEEEEE)
    val subTextColor = if (isDayMode) Color(0xFF666666) else Color(0xFF888888)

    val scales = LocalUiScales.current
    val ts = scales.ts()
    val us = scales.us()

    val allElements = listOf(
        Triple("CPU", "CPU Usage", "Real-time processor activity and load percentage"),
        Triple("RAM", "RAM Memory", "Active memory consumption and available headroom"),
        Triple("TEMP", "Temperature", "Thermal sensors and battery temperature diagnostics"),
        Triple("STORAGE", "Disk Storage", "Internal device storage capacity and space usage")
    )

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape((8 * us).dp))
                .background(dialogBg)
                .border(1.dp, dialogBorder, RoundedCornerShape((8 * us).dp))
                .padding((16 * us).dp)
                .widthIn(min = (340 * us).dp, max = (480 * us).dp)
                .heightIn(max = (560 * us).dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = (12 * us).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CONFIGURE VITALS",
                        color = if (isDayMode) Color(0xFF333333) else Color(0xFF888888),
                        fontSize = (11 * ts).sp,
                        letterSpacing = (1.5f * ts).sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Select which gauges appear in the vitals widget",
                        color = subTextColor,
                        fontSize = (9 * ts).sp
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size((24 * us).dp)) {
                    Icon(Icons.Default.Close, null, tint = if (isDayMode) Color(0xFF666666) else Color(0xFF888888), modifier = Modifier.size((16 * us).dp))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy((8 * us).dp)
            ) {
                allElements.forEach { (id, title, desc) ->
                    val isChecked = enabledElements.any { it.equals(id, ignoreCase = true) }
                    val elementIcon = when (id) {
                        "CPU" -> Icons.Default.Memory
                        "RAM" -> Icons.Default.DeveloperBoard
                        "TEMP" -> Icons.Default.DeviceThermostat
                        "STORAGE" -> Icons.Default.Folder
                        else -> Icons.Default.Speed
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape((6 * us).dp))
                            .background(if (isChecked) effectiveAccent.copy(alpha = if (isDayMode) 0.08f else 0.15f) else cardBg)
                            .border(
                                width = 1.dp,
                                color = if (isChecked) effectiveAccent.copy(alpha = if (isDayMode) 0.45f else 0.6f) else (if (isDayMode) Color(0xFFCED4DA) else dialogBorder),
                                shape = RoundedCornerShape((6 * us).dp)
                            )
                            .clickable { onToggleElement(id) }
                            .padding(horizontal = (12 * us).dp, vertical = (8 * us).dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy((10 * us).dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size((32 * us).dp)
                                .clip(RoundedCornerShape((4 * us).dp))
                                .background(if (isChecked) effectiveAccent.copy(alpha = if (isDayMode) 0.15f else 0.25f) else (if (isDayMode) Color(0xFFE9ECEF) else Color(0xFF222222))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = elementIcon,
                                contentDescription = title,
                                tint = if (isChecked) effectiveAccent else subTextColor,
                                modifier = Modifier.size((18 * us).dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                color = if (isChecked) textColor else textColor.copy(alpha = 0.7f),
                                fontSize = (11 * ts).sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = desc,
                                color = subTextColor,
                                fontSize = (8.5f * ts).sp
                            )
                        }

                        // Check indicator chip
                        val checkmarkColor = if (effectiveAccent.luminance() > 0.5f) Color.Black else Color.White
                        Box(
                            modifier = Modifier
                                .size((22 * us).dp)
                                .clip(RoundedCornerShape((4 * us).dp))
                                .background(if (isChecked) effectiveAccent else Color.Transparent)
                                .border(1.dp, if (isChecked) effectiveAccent else (if (isDayMode) Color(0xFFCED4DA) else dialogBorder), RoundedCornerShape((4 * us).dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isChecked) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = checkmarkColor,
                                    modifier = Modifier.size((14 * us).dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height((14 * us).dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RESET DEFAULT",
                    color = subTextColor,
                    fontSize = (10 * ts).sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (1 * ts).sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape((4 * us).dp))
                        .clickable(onClick = onResetToDefault)
                        .padding(horizontal = (8 * us).dp, vertical = (6 * us).dp)
                )

                val doneContentColor = if (effectiveAccent.luminance() > 0.5f) Color.Black else Color.White
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape((4 * us).dp))
                        .background(effectiveAccent)
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = (16 * us).dp, vertical = (8 * us).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "DONE",
                        color = doneContentColor,
                        fontSize = (11 * ts).sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (1 * ts).sp
                    )
                }
            }
        }
    }
}
