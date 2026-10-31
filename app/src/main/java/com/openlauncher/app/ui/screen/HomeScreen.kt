package com.openlauncher.app.ui.screen

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import com.openlauncher.app.data.AppSettings
import com.openlauncher.app.data.ClockStyle
import com.openlauncher.app.data.DayNightMode
import com.openlauncher.app.data.computeWidgetMove
import com.openlauncher.app.data.GRID_COLS
import com.openlauncher.app.data.GRID_ROWS
import com.openlauncher.app.data.WidgetBgMode
import com.openlauncher.app.data.WidgetConfig
import com.openlauncher.app.model.NowPlayingState
import com.openlauncher.app.model.WeatherState
import com.openlauncher.app.ui.theme.LocalDayMode
import com.openlauncher.app.ui.theme.surfaceFrom
import com.openlauncher.app.ui.widget.*
import java.util.Calendar
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.graphics.drawable.toBitmap
import com.openlauncher.app.model.AppInfo
import com.openlauncher.app.util.LocationData

private val WIDGET_RADIUS = RoundedCornerShape(0.dp)

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
    WidgetTypeInfo("OBD",           "OBD2",          Icons.Default.Bluetooth,     "Live engine & sensor telemetry")
)

private fun canAddWidget(settings: com.openlauncher.app.data.AppSettings): Boolean {
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
    }
    val activeWidgets = settings.widgetLayout.filter { it.enabled && it.id in visibleIds }
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
    onSetClockStyle: (ClockStyle) -> Unit,
    onSetVitalsAsBars: (Boolean) -> Unit = {},
    onSetSpeedometerDigitalOnly: (Boolean) -> Unit = {},
    onSetAppShortcutCount: (Int) -> Unit = {},
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
    onToggleWidgetTitle: (id: String) -> Unit = {},
    onZeroAltimeter: (Float, Float) -> Unit = { _, _ -> },
    onResetAltimeterZero: () -> Unit = {},
    onSetAdvancedWeatherIcons: (Boolean) -> Unit = {},
    isWeatherRefreshing: Boolean = false,
    onRefreshWeather: () -> Unit = {},
    onSelectObdAdapter: (address: String, name: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val accent       = Color(settings.accentColor)
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

    var resizingId           by remember { mutableStateOf<String?>(null) }
    var contextMenuId        by remember { mutableStateOf<String?>(null) }
    var colorPickerWidgetId  by remember { mutableStateOf<String?>(null) }
    var appPickerSlotIndex   by remember { mutableStateOf<Int?>(null) }
    var quickActionsConfigOpen by remember { mutableStateOf(false) }
    var vitalsConfigOpen by remember { mutableStateOf(false) }

    val configuration    = LocalConfiguration.current
    val isLandscape      = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var editMode         by remember { mutableStateOf(false) }
    var widgetLibraryOpen by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {

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
                            ) { editMode = false }
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
                            ) { widgetLibraryOpen = true }
                    )
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
                            ) { editMode = true }
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

                Text(
                    text          = settings.vehicleName.uppercase(),
                    style         = MaterialTheme.typography.titleLarge,
                    color         = headerTextColor,
                    letterSpacing = 3.sp,
                    fontSize      = 14.sp
                )
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
        }

        HorizontalDivider(color = if (isDayMode) Color(0xFFCCCCCC) else Color(0xFF141414))

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
            }

            // Keep only visible widgets exactly as configured in settings, allowing explicit resizing to dictate layout
            val visible = settings.widgetLayout.filter { it.enabled && it.id in visibleIds }
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
                                    onLongClick       = { contextMenuId = w.id }
                                )
                            } else Modifier
                        )
                ) {
                    when (w.id) {
                        "CLOCK" -> ClockWidget(
                            style              = settings.clockStyle,
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
                            isRefreshing       = isWeatherRefreshing,
                            onRefresh          = onRefreshWeather,
                            onLongClick        = { contextMenuId = "WEATHER" },
                            isEditing          = editMode,
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
                            onAssignRadio         = onAssignRadio
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
                            digitalOnly        = settings.speedometerDigitalOnly,
                            customContentColor = customContentColor,
                            modifier           = Modifier.fillMaxSize()
                        )
                        "VITALS" -> VitalsWidget(
                            accent             = widgetAccent,
                            isDayMode          = widgetDayMode,
                            asBars             = settings.vitalsAsBars,
                            vitalsElements     = settings.vitalsElements,
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
                            pads               = settings.soundboardPads,
                            accent             = widgetAccent,
                            isDayMode          = widgetDayMode,
                            customContentColor = customContentColor,
                            buttonCount        = settings.soundboardButtonCount,
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
                            enabledActions        = settings.quickActionButtons,
                            onToggleScreenBlanked = onToggleScreenBlanked,
                            onToggleDayNight      = onToggleDayNight,
                            onOpenConfig          = { quickActionsConfigOpen = true },
                            modifier              = Modifier.fillMaxSize()
                        )
                        "APP_SHORTCUTS" -> AppShortcutsWidget(
                            packages           = settings.appWidgetPackages,
                            installedApps      = installedApps,
                            accent             = widgetAccent,
                            isDayMode          = widgetDayMode,
                            customContentColor = customContentColor,
                            shortcutCount      = settings.appShortcutCount,
                            onLaunchApp        = onLaunchApp,
                            onPickAppForSlot   = { slot -> appPickerSlotIndex = slot },
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
                            modifier           = Modifier.fillMaxSize()
                        )
                    }

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

                        // Top-Right-Adjacent: Configure Quick Actions [⚙]
                        if (w.id == "QUICK_ACTIONS") {
                            Box(
                                modifier = Modifier
                                    .zIndex(5f)
                                    .align(Alignment.TopEnd)
                                    .padding(end = 36.dp, top = 4.dp)
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xE6000000))
                                    .border(1.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                    .clickable { quickActionsConfigOpen = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Configure Quick Actions",
                                    tint = accent,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        // Top-Right-Adjacent: Configure Vitals Elements [⚙]
                        if (w.id == "VITALS") {
                            Box(
                                modifier = Modifier
                                    .zIndex(5f)
                                    .align(Alignment.TopEnd)
                                    .padding(end = 36.dp, top = 4.dp)
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xE6000000))
                                    .border(1.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                    .clickable { vitalsConfigOpen = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Configure Vitals Elements",
                                    tint = accent,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
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
                                .clickable { colorPickerWidgetId = w.id },
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
                                .clickable { resizingId = w.id },
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
        }
    }

    // ── Widget context menu (long-press any cell) ────────────────────────────
    contextMenuId?.let { id ->
        val currentConfig = settings.widgetLayout.find { it.id == id }
        val isTitleShown = currentConfig?.isTitleVisible ?: true
        WidgetContextMenu(
            widgetId            = id,
            accent              = accent,
            clockStyle          = settings.clockStyle,
            vitalsAsBars        = settings.vitalsAsBars,
            speedometerDigitalOnly = settings.speedometerDigitalOnly,
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
            onAssignCarPlay     = { contextMenuId = null; onAssignCarPlay() },
            onAssignAndroidAuto = { contextMenuId = null; onAssignAndroidAuto() },
            onClearCarPlay      = { contextMenuId = null; onClearCarPlay() },
            onClearAndroidAuto  = { contextMenuId = null; onClearAndroidAuto() },
            onAssignPip         = { contextMenuId = null; onAssignPip() },
            onClearPip          = { contextMenuId = null; onClearPip() },
            onSetClockStyle     = { onSetClockStyle(it) },
            onSetVitalsAsBars   = { onSetVitalsAsBars(it) },
            onSetSpeedometerDigitalOnly = { onSetSpeedometerDigitalOnly(it) },
            appShortcutCount    = settings.appShortcutCount,
            onSetAppShortcutCount = onSetAppShortcutCount,
            soundboardButtonCount = settings.soundboardButtonCount,
            onSetSoundboardButtonCount = onSetSoundboardButtonCount,
            advancedWeatherIcons = settings.advancedWeatherIcons,
            onSetAdvancedWeatherIcons = onSetAdvancedWeatherIcons,
            onDismiss           = { contextMenuId = null }
        )
    }

    // ── Widget Background Glass Color Dialog ─────────────────────────────────
    colorPickerWidgetId?.let { id ->
        val cfg = settings.widgetLayout.find { it.id == id }
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

    // ── App Shortcuts Slot Assignment Dialog ─────────────────────────────────
    appPickerSlotIndex?.let { slot ->
        val currentPkg = settings.appWidgetPackages.getOrNull(slot).orEmpty()
        AppPickerModalDialog(
            slotIndex      = slot,
            currentPackage = currentPkg,
            apps           = installedApps,
            accent         = accent,
            isDayMode      = isDayMode,
            onSelectApp    = { pkg -> onSetAppShortcutPackage(slot, pkg) },
            onDismiss      = { appPickerSlotIndex = null }
        )
    }

    // ── Quick Actions Button Configuration Dialog ────────────────────────────
    if (quickActionsConfigOpen) {
        QuickActionsConfigDialog(
            enabledActions   = settings.quickActionButtons,
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
            enabledElements  = settings.vitalsElements,
            accent           = accent,
            isDayMode        = isDayMode,
            onToggleElement  = { elementId -> onToggleVitalsElement(elementId) },
            onResetToDefault = { onResetVitalsElements() },
            onDismiss        = { vitalsConfigOpen = false }
        )
    }

    // ── Resize dialog ────────────────────────────────────────────────────────
    resizingId?.let { id ->
        val config = settings.widgetLayout.find { it.id == id }
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
            accent    = accent,
            isDayMode = isDayMode,
            onAdd     = { id -> onAddWidget(id) },
            onRemove  = { id -> onRemoveWidget(id) },
            onDismiss = { widgetLibraryOpen = false }
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
}

@Composable
private fun WidgetContextMenu(
    widgetId: String,
    accent: Color,
    clockStyle: ClockStyle,
    vitalsAsBars: Boolean,
    speedometerDigitalOnly: Boolean,
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
    onAssignCarPlay: () -> Unit,
    onAssignAndroidAuto: () -> Unit,
    onClearCarPlay: () -> Unit,
    onClearAndroidAuto: () -> Unit,
    onAssignPip: () -> Unit,
    onClearPip: () -> Unit,
    onSetClockStyle: (ClockStyle) -> Unit,
    onSetVitalsAsBars: (Boolean) -> Unit,
    onSetSpeedometerDigitalOnly: (Boolean) -> Unit,
    appShortcutCount: Int = 4,
    onSetAppShortcutCount: (Int) -> Unit = {},
    soundboardButtonCount: Int = 6,
    onSetSoundboardButtonCount: (Int) -> Unit = {},
    advancedWeatherIcons: Boolean = true,
    onSetAdvancedWeatherIcons: (Boolean) -> Unit = {},
    onDismiss: () -> Unit
) {
    val menuBg    = if (isDayMode) Color(0xFFFFFFFF) else Color(0xFF111111)
    val menuBorder = if (isDayMode) Color(0xFFDDE1E5) else Color(0xFF1E1E1E)
    val menuDivider = if (isDayMode) Color(0xFFF1F3F5) else Color(0xFF1A1A1A)
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(menuBg)
                .border(1.dp, menuBorder, RoundedCornerShape(4.dp))
                .padding(vertical = 4.dp)
                .width(220.dp)
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
            }
            if (widgetId == "WEATHER") {
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
                    tint    = if (!speedometerDigitalOnly) accent else inactiveMenuTint,
                    onClick = { onSetSpeedometerDigitalOnly(false); onDismiss() },
                    isDayMode = isDayMode
                )
                HorizontalDivider(color = menuDivider)
                ContextRow(
                    label   = "DIGITAL ONLY",
                    icon    = Icons.Default.Dialpad,
                    tint    = if (speedometerDigitalOnly) accent else inactiveMenuTint,
                    onClick = { onSetSpeedometerDigitalOnly(true); onDismiss() },
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = finalTint, modifier = Modifier.size(16.dp))
        Text(label, color = finalTint, fontSize = 10.sp, letterSpacing = 1.sp)
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

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.border(1.dp, dialogBorder, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        title = {
            Text(
                text          = "RESIZE ${config.id.replace('_', ' ')}",
                color         = dialogText,
                fontSize      = 11.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text          = "Adjust the grid span (columns x rows) for this widget.",
                    color         = if (isDayMode) Color(0xFF6C757D) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    fontSize      = 9.sp,
                    letterSpacing = 0.5.sp
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
                    fontSize      = 11.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text          = "CANCEL",
                    color         = cancelColor,
                    fontSize      = 11.sp,
                    fontWeight    = FontWeight.SemiBold,
                    letterSpacing = 1.sp
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

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text          = label,
            color         = dimColor,
            fontSize      = 10.sp,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier      = Modifier.width(52.dp)
        )

        // Stepper: Minus [-]
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (value > min) buttonBg else buttonBg.copy(alpha = 0.5f))
                .border(1.dp, if (value > min) buttonBorder else buttonBorder.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .clickable(enabled = value > min) { onChange(value - 1) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.Remove,
                contentDescription = "Decrease $label",
                tint               = if (value > min) textColor else disabledC,
                modifier           = Modifier.size(16.dp)
            )
        }

        // Stepper: Value number
        Text(
            text       = "$value",
            color      = textColor,
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign  = TextAlign.Center,
            modifier   = Modifier.width(26.dp)
        )

        // Stepper: Plus [+]
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (value < max) buttonBg else buttonBg.copy(alpha = 0.5f))
                .border(1.dp, if (value < max) buttonBorder else buttonBorder.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .clickable(enabled = value < max) { onChange(value + 1) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.Add,
                contentDescription = "Increase $label",
                tint               = if (value < max) effectiveAccent else disabledC,
                modifier           = Modifier.size(16.dp)
            )
        }

        Spacer(Modifier.weight(1f))

        // Grid span pip indicator
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(max) { i ->
                val isActive = i < value
                Box(
                    modifier = Modifier
                        .size(width = 14.dp, height = 10.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(if (isActive) activePipBg else inactivePipBg)
                        .border(1.dp, if (isActive) activePipBorder else inactivePipBorder, RoundedCornerShape(1.5.dp))
                )
            }
        }
    }
}


// ── Widget Library ────────────────────────────────────────────────────────────

@Composable
private fun WidgetLibraryDialog(
    settings: AppSettings,
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

    val activeIds = buildSet {
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
    }
    val canAdd = canAddWidget(settings)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(dialogBg)
                .border(1.dp, dialogBorder, RoundedCornerShape(4.dp))
                .padding(16.dp)
                .widthIn(min = 320.dp, max = 520.dp)
        ) {
            Row(
                modifier          = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text          = "WIDGET LIBRARY",
                    color         = titleColor,
                    fontSize      = 9.sp,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, null, tint = closeColor, modifier = Modifier.size(14.dp))
                }
            }

            LazyVerticalGrid(
                columns               = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement   = Arrangement.spacedBy(6.dp),
                modifier              = Modifier.fillMaxWidth()
            ) {
                items(ALL_WIDGET_TYPES) { info ->
                    val isActive = info.id in activeIds
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
                Spacer(Modifier.height(10.dp))
                Text(
                    text          = "ALL ${GRID_COLS * GRID_ROWS} CELLS OCCUPIED — REMOVE A WIDGET TO ADD MORE",
                    color         = if (isDayMode) Color(0xFFE03131) else Color(0xFF3A3A3A),
                    fontSize      = 8.sp,
                    letterSpacing = 1.sp,
                    modifier      = Modifier.fillMaxWidth(),
                    textAlign     = TextAlign.Center
                )
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

    val enabled    = isActive || canAdd
    val cardBorder = if (isActive) effectiveAccent else if (isDayMode) Color(0xFFCCCCCC) else Color(0xFF1A1A1A)
    val cardBg     = if (isActive) effectiveAccent.copy(alpha = if (isDayMode) 0.12f else 0.15f) else if (isDayMode) Color(0xFFFFFFFF) else Color(0xFF0E0E0E)
    val iconTint   = if (isActive) effectiveAccent else if (isDayMode) Color(0xFF495057) else Color(0xFF333333)
    val labelColor = if (isActive) effectiveAccent else if (isDayMode) Color(0xFF212529) else Color(0xFF3A3A3A)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(4.dp))
            .clickable(enabled = enabled, onClick = onToggle)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(info.icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(5.dp))
        Text(
            text          = info.label,
            color         = labelColor,
            fontSize      = 7.sp,
            letterSpacing = 1.sp,
            textAlign     = TextAlign.Center,
            maxLines      = 2,
            lineHeight    = 9.sp
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text          = when {
                isActive -> "ACTIVE (REMOVE)"
                !canAdd  -> "FULL"
                else     -> "+ ADD"
            },
            color         = when {
                isActive -> effectiveAccent.copy(alpha = 0.85f)
                !canAdd  -> if (isDayMode) Color(0xFFADB5BD) else Color(0xFF282828)
                else     -> if (isDayMode) Color(0xFF495057) else effectiveAccent
            },
            fontSize      = 6.5.sp,
            letterSpacing = 1.sp,
            textAlign     = TextAlign.Center
        )
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
                .clip(RoundedCornerShape(8.dp))
                .background(dialogBg)
                .border(1.dp, dialogBorder, RoundedCornerShape(8.dp))
                .padding(16.dp)
                .widthIn(min = 320.dp, max = 500.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WIDGET GLASS TINT (${widgetId.replace('_', ' ')})",
                    color = if (isDayMode) Color(0xFF333333) else Color(0xFF888888),
                    fontSize = 10.sp,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, null, tint = if (isDayMode) Color(0xFF666666) else Color(0xFF888888), modifier = Modifier.size(16.dp))
                }
            }

            // "Follow theme" — clearly labelled, distinct from the preset swatches
            val followSelected = currentMode == WidgetBgMode.FOLLOW_THEME
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (followSelected) effectiveAccent.copy(alpha = 0.18f) else Color.Transparent)
                    .border(
                        width = if (followSelected) 2.dp else 1.dp,
                        color = if (followSelected) effectiveAccent else dialogBorder,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable {
                        onSelectMode(WidgetBgMode.FOLLOW_THEME)
                        onDismiss()
                    }
                    .padding(vertical = 12.dp, horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.BrightnessAuto,
                    null,
                    tint = if (followSelected) effectiveAccent else (if (isDayMode) Color(0xFF555555) else Color(0xFF888888)),
                    modifier = Modifier.size(16.dp)
                )
                Column {
                    Text(
                        "FOLLOW THEME",
                        color = if (followSelected) effectiveAccent else (if (isDayMode) Color(0xFF111111) else Color(0xFFDDDDDD)),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        "Uses the global tile color & transparency from Settings",
                        color = if (isDayMode) Color(0xFF666666) else Color(0xFF777777),
                        fontSize = 7.sp,
                        letterSpacing = 0.3.sp
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
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
                            .clip(RoundedCornerShape(6.dp))
                            .background(bgSample)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) borderHighlight else (if (isDayMode) Color(0xFFCCCCCC) else Color(0x33FFFFFF)),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable {
                                onSelectColor(colorVal)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = name,
                            color = sampleTextColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (colorVal == null) "DEFAULT" else if (lum > 0.52f) "DARK TEXT" else "LIGHT TEXT",
                            color = sampleTextColor.copy(alpha = 0.7f),
                            fontSize = 6.5.sp,
                            letterSpacing = 0.5.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}


// ── App Shortcuts Slot Assignment Dialog ──────────────────────────────────────

@Composable
private fun AppPickerModalDialog(
    slotIndex: Int,
    currentPackage: String,
    apps: List<AppInfo>,
    accent: Color,
    isDayMode: Boolean,
    onSelectApp: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val accentLum = 0.299f * accent.red + 0.587f * accent.green + 0.114f * accent.blue
    val effectiveAccent = if (isDayMode && accentLum > 0.65f) Color(0xFF111111)
                          else if (!isDayMode && accentLum < 0.25f) Color(0xFFEEEEEE)
                          else accent

    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = remember(searchQuery, apps) {
        if (searchQuery.isBlank()) apps
        else apps.filter { it.appName.contains(searchQuery, ignoreCase = true) }
    }

    val dialogBg = if (isDayMode) Color(0xFFFFFFFF) else Color(0xFF111111)
    val dialogBorder = if (isDayMode) Color(0xFFDDE1E5) else Color(0xFF222222)
    val searchBg = if (isDayMode) Color(0xFFF1F3F5) else Color(0xFF1A1A1A)
    val textColor = if (isDayMode) Color(0xFF111111) else Color(0xFFEEEEEE)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(dialogBg)
                .border(1.dp, dialogBorder, RoundedCornerShape(8.dp))
                .padding(16.dp)
                .widthIn(min = 340.dp, max = 540.dp)
                .heightIn(max = 480.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SELECT APP FOR SHORTCUT #${slotIndex + 1}",
                    color = if (isDayMode) Color(0xFF333333) else Color(0xFF888888),
                    fontSize = 10.sp,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                if (currentPackage.isNotBlank()) {
                    TextButton(
                        onClick = {
                            onSelectApp("")
                            onDismiss()
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("CLEAR", color = Color(0xFFE03131), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, null, tint = if (isDayMode) Color(0xFF666666) else Color(0xFF888888), modifier = Modifier.size(16.dp))
                }
            }

            // Search input field
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(searchBg)
                    .border(1.dp, if (isDayMode) Color(0xFFD0D0D0) else Color(0xFF282828), RoundedCornerShape(4.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    textStyle = TextStyle(color = textColor, fontSize = 12.sp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text("Search installed apps...", color = if (isDayMode) Color(0xFF999999) else Color(0xFF555555), fontSize = 12.sp)
                        }
                        innerTextField()
                    }
                )
            }

            Spacer(Modifier.height(10.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                items(filteredApps) { app ->
                    val isCurrent = app.packageName == currentPackage
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isCurrent) effectiveAccent.copy(alpha = if (isDayMode) 0.12f else 0.2f)
                                else if (isDayMode) Color(0xFFF8F9FA) else Color(0xFF161616)
                            )
                            .border(
                                1.dp,
                                if (isCurrent) effectiveAccent else (if (isDayMode) Color(0xFFCED4DA) else Color(0xFF242424)),
                                RoundedCornerShape(6.dp)
                            )
                            .clickable {
                                onSelectApp(app.packageName)
                                onDismiss()
                            }
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val bmp = remember(app.icon) {
                            runCatching { app.icon.toBitmap(56, 56) }.getOrNull()
                        }
                        if (bmp != null) {
                            Icon(
                                painter = BitmapPainter(bmp.asImageBitmap()),
                                contentDescription = app.appName,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(32.dp)
                            )
                        } else {
                            Icon(Icons.Default.Apps, null, tint = effectiveAccent, modifier = Modifier.size(32.dp))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = app.appName,
                            color = textColor,
                            fontSize = 8.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
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
                .clip(RoundedCornerShape(8.dp))
                .background(dialogBg)
                .border(1.dp, dialogBorder, RoundedCornerShape(8.dp))
                .padding(16.dp)
                .widthIn(min = 340.dp, max = 480.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CONFIGURE QUICK ACTIONS",
                        color = if (isDayMode) Color(0xFF333333) else Color(0xFF888888),
                        fontSize = 11.sp,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Select which buttons appear in the widget",
                        color = subTextColor,
                        fontSize = 9.sp
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, null, tint = if (isDayMode) Color(0xFF666666) else Color(0xFF888888), modifier = Modifier.size(16.dp))
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isChecked) effectiveAccent.copy(alpha = if (isDayMode) 0.08f else 0.15f) else cardBg)
                            .border(
                                width = 1.dp,
                                color = if (isChecked) effectiveAccent.copy(alpha = if (isDayMode) 0.45f else 0.6f) else (if (isDayMode) Color(0xFFCED4DA) else dialogBorder),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { onToggleAction(id) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isChecked) effectiveAccent.copy(alpha = if (isDayMode) 0.15f else 0.25f) else (if (isDayMode) Color(0xFFE9ECEF) else Color(0xFF222222))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = actionIcon,
                                contentDescription = title,
                                tint = if (isChecked) effectiveAccent else subTextColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                color = if (isChecked) textColor else textColor.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = desc,
                                color = subTextColor,
                                fontSize = 8.5.sp
                            )
                        }

                        // Check indicator chip
                        val checkmarkColor = if (effectiveAccent.luminance() > 0.5f) Color.Black else Color.White
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isChecked) effectiveAccent else Color.Transparent)
                                .border(1.dp, if (isChecked) effectiveAccent else (if (isDayMode) Color(0xFFCED4DA) else dialogBorder), RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isChecked) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = checkmarkColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RESET DEFAULT",
                    color = subTextColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(onClick = onResetToDefault)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )

                val doneContentColor = if (effectiveAccent.luminance() > 0.5f) Color.Black else Color.White
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(effectiveAccent)
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "DONE",
                        color = doneContentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

// ── Vitals Elements Configuration Dialog ──────────────────────────────────────

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

    val allElements = listOf(
        Triple("CPU", "CPU Usage", "Real-time processor activity and load percentage"),
        Triple("RAM", "RAM Memory", "Active memory consumption and available headroom"),
        Triple("TEMP", "Temperature", "Thermal sensors and battery temperature diagnostics"),
        Triple("STORAGE", "Disk Storage", "Internal device storage capacity and space usage")
    )

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(dialogBg)
                .border(1.dp, dialogBorder, RoundedCornerShape(8.dp))
                .padding(16.dp)
                .widthIn(min = 340.dp, max = 480.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CONFIGURE VITALS",
                        color = if (isDayMode) Color(0xFF333333) else Color(0xFF888888),
                        fontSize = 11.sp,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Select which gauges appear in the vitals widget",
                        color = subTextColor,
                        fontSize = 9.sp
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, null, tint = if (isDayMode) Color(0xFF666666) else Color(0xFF888888), modifier = Modifier.size(16.dp))
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isChecked) effectiveAccent.copy(alpha = if (isDayMode) 0.08f else 0.15f) else cardBg)
                            .border(
                                width = 1.dp,
                                color = if (isChecked) effectiveAccent.copy(alpha = if (isDayMode) 0.45f else 0.6f) else (if (isDayMode) Color(0xFFCED4DA) else dialogBorder),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { onToggleElement(id) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isChecked) effectiveAccent.copy(alpha = if (isDayMode) 0.15f else 0.25f) else (if (isDayMode) Color(0xFFE9ECEF) else Color(0xFF222222))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = elementIcon,
                                contentDescription = title,
                                tint = if (isChecked) effectiveAccent else subTextColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                color = if (isChecked) textColor else textColor.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = desc,
                                color = subTextColor,
                                fontSize = 8.5.sp
                            )
                        }

                        // Check indicator chip
                        val checkmarkColor = if (effectiveAccent.luminance() > 0.5f) Color.Black else Color.White
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isChecked) effectiveAccent else Color.Transparent)
                                .border(1.dp, if (isChecked) effectiveAccent else (if (isDayMode) Color(0xFFCED4DA) else dialogBorder), RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isChecked) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = checkmarkColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RESET DEFAULT",
                    color = subTextColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(onClick = onResetToDefault)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )

                val doneContentColor = if (effectiveAccent.luminance() > 0.5f) Color.Black else Color.White
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(effectiveAccent)
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "DONE",
                        color = doneContentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
