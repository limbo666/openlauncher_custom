package com.openlauncher.app.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

enum class ClockStyle { DIGITAL, ANALOG }
enum class UnitSystem { METRIC, IMPERIAL }
enum class AppFont {
    SYSTEM,
    JETBRAINS_MONO,
    SOURCE_CODE_PRO,
    CAL_SANS,
    FUGAZ_ONE,
    GEOM,
    NOTO_SANS_DISPLAY,
    ROBOTO
}
enum class DayNightMode { DARK, LIGHT, CUSTOM, AUTO, SYSTEM }
enum class AppDrawerFilter { SELECTED, TOOLS, USER, SYSTEM, ALL }
enum class SidebarPosition { LEFT, RIGHT, BOTTOM }
enum class GradientDirection { TOP_TO_BOTTOM, LEFT_TO_RIGHT, DIAGONAL, RADIAL }
enum class WeatherProvider { WTTR_IN, OPEN_METEO }
enum class WidgetBgMode { DEFAULT, FOLLOW_THEME, CUSTOM }

enum class FloatingActionType { HOME, VOL_UP, VOL_DOWN, MUTE, BLACKOUT, DAY_NIGHT, APP }

data class FloatingPanelAction(
    val type: FloatingActionType = FloatingActionType.HOME,
    val packageName: String = ""
)

fun defaultFloatingActions() = listOf(
    FloatingPanelAction(FloatingActionType.HOME),
    FloatingPanelAction(FloatingActionType.VOL_DOWN),
    FloatingPanelAction(FloatingActionType.VOL_UP)
)

enum class DefaultShortcutIcon {
    NONE,
    // Navigation & vehicle
    RADIO, CAMERA, PHONE, MAP, NAVIGATION, CAR, GAS_STATION, DASHBOARD,
    // Audio & media
    MUSIC, SPEAKER, HEADSET, EQUALIZER, VOLUME_UP, PLAY, PAUSE, MIC,
    // Connectivity & utility
    BLUETOOTH, WIFI, USB, CLOUD, STORE, GAMEPAD, CALCULATOR, NOTIFICATIONS, REFRESH,
    // Lighting & climate
    LIGHTBULB, BRIGHTNESS, AC, THERMOSTAT,
    // General utility & car
    TV, VIDEOCAM, STAR, MESSAGE, TIMER, LOCK, SETTINGS, FAVORITE,
    BATTERY, POWER, SHIELD, WRENCH, PARKING, EV_STATION, WARNING, INFO, EXPLORE, SPEEDOMETER, COMPASS,
    // Web / location
    GLOBE
}

data class SoundPadConfig(
    val label: String,
    val audioUri: String = "",
    val synthType: String = "BEEP"
)

fun defaultSoundboardPads() = listOf(
    SoundPadConfig("whats_up_doc", synthType = "whats_up_doc"),
    SoundPadConfig("be_back",      synthType = "adhimahadi_arnold_ill_be_back_8276"),
    SoundPadConfig("later",        synthType = "a_few_moments_later"),
    SoundPadConfig("stamata",      synthType = "stamataaaaaaa"),
    SoundPadConfig("meow",         synthType = "dragon_studio_cute_cat_meow_472372"),
    SoundPadConfig("wah_wah",      synthType = "universfield_wah_wah_horn_117724"),
    SoundPadConfig("rooster",      synthType = "rooster"),
    SoundPadConfig("boing",        synthType = "spring_boing"),
    SoundPadConfig("chime",        synthType = "attention_chime")
)

data class ShortcutConfig(
    val packageName: String = "",
    val label: String = "",
    val isDefault: Boolean = false,
    val defaultIcon: DefaultShortcutIcon = DefaultShortcutIcon.NONE,
    // null = native app icon; non-null = override with this vector icon
    val customIconOverride: DefaultShortcutIcon? = null
)

const val GRID_COLS = 6
const val GRID_ROWS = 4

data class WidgetConfig(
    val id: String,          // "CLOCK" | "WEATHER" | "TELEMETRY" | "NOW_PLAYING" | "VOLUME" | "QUICK_ACTIONS" | "APP_SHORTCUTS"
    val gridX: Int,          // column 0..(GRID_COLS-1)
    val gridY: Int,          // row    0..(GRID_ROWS-1)
    val spanX: Int = 2,
    val spanY: Int = 2,
    val enabled: Boolean = true,
    val bgMode: WidgetBgMode = WidgetBgMode.DEFAULT,
    val customBgColor: Long? = null,
    val showTitle: Boolean? = true
) {
    val isTitleVisible: Boolean get() = showTitle != false
}

data class AppSettings(
    val vehicleName: String = "MY CAR",
    val accentColor: Int = Color.White.toArgb(),
    val backgroundColor: Int = Color.Black.toArgb(),
    val fontColor: Int = Color.White.toArgb(),
    val tileColorDark: Int = Color.Black.toArgb(),
    val tileColorLight: Int = Color.White.toArgb(),
    val tileAlpha: Float = 0.65f,
    val wallpaperUri: String = "",
    val fontBold: Boolean = false,
    val textScale: Float = 1.2f,
    val uiScale: Float = 1.0f,
    val clockStyle: ClockStyle = ClockStyle.DIGITAL,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val appFont: AppFont = AppFont.JETBRAINS_MONO,
    val showWeather: Boolean = true,
    val showClock: Boolean = true,
    val showTelemetry: Boolean = true,
    val showNowPlaying: Boolean = true,
    val shortcuts: List<ShortcutConfig> = defaultShortcuts(),
    val widgetLayout: List<WidgetConfig> = defaultWidgetLayout(),
    // Second home page ("swipe left/right") — independent widget placement per page
    val homePageAltLayout: List<WidgetConfig> = emptyList(),
    // Which home page is currently shown (persisted across restarts)
    val homePage: Int = 0,
    // Auto-switch pages by driving state (GPS speed) — Stage 2
    val autoSwitchPages: Boolean = false,
    // Which page shows while driving (the other shows while parked)
    val drivingPage: Int = 0,
    // Enter "driving" above this speed (km/h)
    val autoSwitchSpeedKmh: Float = 8f,
    // Seconds stopped before switching back to the parked page
    val autoSwitchIdleSeconds: Float = 10f,
    val carPlayPackage: String = "",
    val androidAutoPackage: String = "",
    val useGradient: Boolean = false,
    val gradientEndColor: Int = Color.Black.toArgb(),
    val wallpaperDim: Float = 0.55f,
    val sidebarPosition: SidebarPosition = SidebarPosition.LEFT,
    val sidebarWidth: Int = 56,
    val sidebarIconScale: Float = 1.0f,
    val bottomBarShortcutsRight: Boolean = false,
    val showAltimeter: Boolean = true,
    val showSpeedometer: Boolean = true,
    val dayNightMode: DayNightMode = DayNightMode.DARK,
    val showPip: Boolean = false,
    val pipAppPackage: String = "",
    // Head unit's radio app — mirrored & controlled via its MediaSession
    val radioPackage: String = "",
    val onboardingCompleted: Boolean = true,
    val showVitals: Boolean = true,
    val showTripTracker: Boolean = false,
    val compassOffset: Float = 0f,
    val altimeterRollOffset: Float = 0f,
    val altimeterPitchOffset: Float = 0f,
    val showSoundboard: Boolean = true,
    val soundboardPads: List<SoundPadConfig> = defaultSoundboardPads(),
    val vitalsAsBars: Boolean = false,
    val vitalsElements: List<String> = defaultVitalsElements(),
    val speedometerDigitalOnly: Boolean = false,
    val gradientDirection: GradientDirection = GradientDirection.DIAGONAL,
    val useCustomBackgroundColor: Boolean = false,
    val showVolume: Boolean = true,
    val showQuickActions: Boolean = true,
    val showAppShortcuts: Boolean = true,
    val appWidgetPackages: List<String> = listOf("", "", "", ""),
    val quickActionButtons: List<String> = defaultQuickActionButtons(),
    val selectedAppPackages: List<String> = emptyList(),
    val toolsAppPackages: List<String> = emptyList(),
    val appDrawerFilter: AppDrawerFilter = AppDrawerFilter.USER,
    val weatherUseGps: Boolean = true,
    val weatherCustomLocation: String = "Athens",
    val weatherProvider: WeatherProvider = WeatherProvider.OPEN_METEO,
    val weatherForecastDays: Int = 5,
    val advancedWeatherIcons: Boolean = true,
    val showStatusIcons: Boolean = true,
    val showNetworkSpeed: Boolean = false,
    val showNetworkSpeedIndicator: Boolean = false,
    val appShortcutCount: Int = 4,
    val soundboardButtonCount: Int = 6,
    val showObd: Boolean = false,
    val obdDeviceAddress: String = "",
    val obdDeviceName: String = "",
    val preferredMediaPackage: String = "",
    val lastMediaPackage: String = "",
    val autoLaunchMedia: Boolean = true,
    // Head-unit volume channel id ("auto" = detect, or a VolumeProtocols id)
    val volumeProtocol: String = "auto",
    // Global "help text" / secondary text color (settings categories, sublabels,
    // widget secondary text). null = follow the active theme.
    val helpTextColor: Int? = null,
    // Decorative "display animation": a user-selected car PNG that rests beside
    // the vehicle name and "drives" across the screen when movement is detected.
    val displayAnimation: Boolean = false,
    val carImageUri: String = "",
    // Page 1 (second home screen) overrides for widget content. null = mirror page 0.
    val widgetContentAlt: WidgetContent? = null,
    // Last settings category the user was viewing (persisted so Settings reopens there)
    val settingsCategory: String = "Display",
    // Now Playing / Radio widget options
    val showRadioBands: Boolean = false,       // FM/AM band chips in the radio deck
    val showRadioPopup: Boolean = false,       // glassmorphism station-change popup on the home screen
    val nowPlayingLogoScale: Float = 1.0f,     // 0.5..1.0 — album-art/logo size in the player card
    val nowPlayingLogoDim: Float = 0.25f,      // 0..0.8 — dark veil over the artwork for control contrast
    // Floating quick-controls panel (drawn over other apps via SYSTEM_ALERT_WINDOW)
    val floatingPanelEnabled: Boolean = false,
    val floatingPanelActions: List<FloatingPanelAction> = defaultFloatingActions(),
    val floatingPanelX: Int = -1,          // -1 = auto/default placement
    val floatingPanelY: Int = -1,
    val floatingPanelPositionSet: Boolean = false,  // true once the user has dragged it
    val floatingPanelScale: Float = 1.0f,  // scales icons + panel padding
    val floatingPanelOpacity: Float = 0.55f,
    val floatingPanelVertical: Boolean = false  // stack buttons vertically instead of side-by-side
)

/**
 * Per-page "content" for widgets whose inner content is user-selectable (clock
 * style, vitals elements, quick actions, soundboard pads, app shortcuts, etc.).
 * Page 0 is backed by AppSettings' legacy flat fields; page 1 uses [widgetContentAlt]
 * and mirrors page 0 when null.
 */
data class WidgetContent(
    val clockStyle: ClockStyle = ClockStyle.DIGITAL,
    val speedometerDigitalOnly: Boolean = false,
    val vitalsAsBars: Boolean = false,
    val vitalsElements: List<String> = defaultVitalsElements(),
    val quickActionButtons: List<String> = defaultQuickActionButtons(),
    val soundboardPads: List<SoundPadConfig> = defaultSoundboardPads(),
    val soundboardButtonCount: Int = 6,
    val appWidgetPackages: List<String> = listOf("", "", "", ""),
    val appShortcutCount: Int = 4
)

fun AppSettings.pageZeroWidgetContent(): WidgetContent = WidgetContent(
    clockStyle              = clockStyle,
    speedometerDigitalOnly  = speedometerDigitalOnly,
    vitalsAsBars            = vitalsAsBars,
    vitalsElements          = vitalsElements,
    quickActionButtons      = quickActionButtons,
    soundboardPads          = soundboardPads,
    soundboardButtonCount   = soundboardButtonCount,
    appWidgetPackages       = appWidgetPackages,
    appShortcutCount        = appShortcutCount
)

fun AppSettings.widgetContentFor(page: Int): WidgetContent =
    if (page == 0) pageZeroWidgetContent() else (widgetContentAlt ?: pageZeroWidgetContent())

fun AppSettings.withWidgetContent(page: Int, transform: (WidgetContent) -> WidgetContent): AppSettings {
    val updated = transform(widgetContentFor(page))
    if (page == 0) {
        return copy(
            clockStyle             = updated.clockStyle,
            speedometerDigitalOnly = updated.speedometerDigitalOnly,
            vitalsAsBars           = updated.vitalsAsBars,
            vitalsElements         = updated.vitalsElements,
            quickActionButtons     = updated.quickActionButtons,
            soundboardPads         = updated.soundboardPads,
            soundboardButtonCount  = updated.soundboardButtonCount,
            appWidgetPackages      = updated.appWidgetPackages,
            appShortcutCount       = updated.appShortcutCount
        )
    }
    return copy(widgetContentAlt = updated)
}

/**
 * Reconcile the per-page `enabled` flag with the global show flags.
 *
 * Fixes settings migrated from the old single-page model, where `removeWidget` only
 * toggled the show flag and left `enabled = true` in the layout. Without this,
 * re-adding a widget on page 2 (which turns the show flag back on) would resurrect a
 * stale `enabled = true` copy on page 1.
 */
fun reconcileWidgetFlags(s: AppSettings): AppSettings {
    val flags = mapOf(
        "CLOCK" to s.showClock,
        "WEATHER" to s.showWeather,
        "NOW_PLAYING" to s.showNowPlaying,
        "TELEMETRY" to s.showTelemetry,
        "ALTIMETER" to s.showAltimeter,
        "SPEEDOMETER" to s.showSpeedometer,
        "VITALS" to s.showVitals,
        "TRIP_TRACKER" to s.showTripTracker,
        "SOUNDBOARD" to s.showSoundboard,
        "VOLUME" to s.showVolume,
        "QUICK_ACTIONS" to s.showQuickActions,
        "APP_SHORTCUTS" to s.showAppShortcuts,
        "OBD" to s.showObd,
        "NETWORK_SPEED" to s.showNetworkSpeed
    )
    fun rec(layout: List<WidgetConfig>): List<WidgetConfig> =
        layout.map { w -> if (flags[w.id] == false) w.copy(enabled = false) else w }
    return s.copy(
        widgetLayout = rec(s.widgetLayout),
        homePageAltLayout = rec(s.homePageAltLayout)
    )
}

fun defaultQuickActionButtons() = listOf("BLUETOOTH", "WIFI", "BLACKOUT", "MUTE", "SETTINGS")
fun defaultVitalsElements() = listOf("CPU", "RAM", "TEMP")

fun defaultShortcuts() = listOf(
    ShortcutConfig(label = "Radio", isDefault = true, defaultIcon = DefaultShortcutIcon.RADIO),
    ShortcutConfig(label = "Camera", isDefault = true, defaultIcon = DefaultShortcutIcon.CAMERA),
    ShortcutConfig(label = "Music", isDefault = true, defaultIcon = DefaultShortcutIcon.MUSIC),
    ShortcutConfig(label = "Phone", isDefault = true, defaultIcon = DefaultShortcutIcon.PHONE)
)

fun defaultWidgetLayout() = listOf(
    WidgetConfig("CLOCK",         gridX = 0, gridY = 0, spanX = 1, spanY = 1),
    WidgetConfig("TELEMETRY",     gridX = 1, gridY = 0, spanX = 1, spanY = 1),
    WidgetConfig("QUICK_ACTIONS", gridX = 2, gridY = 0, spanX = 2, spanY = 1),
    WidgetConfig("VITALS",        gridX = 4, gridY = 0, spanX = 2, spanY = 1),
    WidgetConfig("NOW_PLAYING",   gridX = 0, gridY = 1, spanX = 2, spanY = 2),
    WidgetConfig("SPEEDOMETER",   gridX = 2, gridY = 1, spanX = 1, spanY = 1),
    WidgetConfig("ALTIMETER",     gridX = 3, gridY = 1, spanX = 1, spanY = 1),
    WidgetConfig("WEATHER",       gridX = 4, gridY = 1, spanX = 2, spanY = 2),
    WidgetConfig("VOLUME",        gridX = 2, gridY = 2, spanX = 2, spanY = 1),
    WidgetConfig("SOUNDBOARD",    gridX = 0, gridY = 3, spanX = 1, spanY = 1),
    WidgetConfig("APP_SHORTCUTS", gridX = 2, gridY = 3, spanX = 2, spanY = 1),
    WidgetConfig("OBD",           gridX = 4, gridY = 3, spanX = 2, spanY = 1)
)

fun AppSettings.activeWidgetIds(): Set<String> = buildSet {
    if (showClock) add("CLOCK")
    if (showWeather) add("WEATHER")
    if (showNowPlaying) add("NOW_PLAYING")
    if (showTelemetry) add("TELEMETRY")
    if (showAltimeter) add("ALTIMETER")
    if (showSpeedometer) add("SPEEDOMETER")
    if (showVitals) add("VITALS")
    if (showTripTracker) add("TRIP_TRACKER")
    if (showSoundboard) add("SOUNDBOARD")
    if (showVolume) add("VOLUME")
    if (showQuickActions) add("QUICK_ACTIONS")
    if (showAppShortcuts) add("APP_SHORTCUTS")
    if (showObd) add("OBD")
    if (showNetworkSpeed) add("NETWORK_SPEED")
}

/**
 * Moves [movingId] to ([targetX], [targetY]) and pushes any displaced widgets to the
 * first available free cell, cascading until all conflicts are resolved.
 * Operates only on the supplied [layout] list — callers should pass only active/enabled widgets.
 */
fun computeWidgetMove(
    layout: List<WidgetConfig>,
    movingId: String,
    targetX: Int,
    targetY: Int
): List<WidgetConfig> {
    val moving = layout.find { it.id == movingId } ?: return layout
    val placed = moving.copy(
        gridX = targetX.coerceIn(0, GRID_COLS - moving.spanX),
        gridY = targetY.coerceIn(0, GRID_ROWS - moving.spanY)
    )

    val others  = layout.filter { it.id != movingId }
    val result  = mutableListOf(placed)
    val occupied = buildOccupied(result).toMutableSet()

    // Stable widgets that don't conflict go first; displaced ones are pushed afterwards
    val (stable, displaced) = others.partition { w -> result.none { widgetsOverlap(it, w) } }

    for (w in stable) {
        result.add(w)
        for (dx in 0 until w.spanX) for (dy in 0 until w.spanY) occupied.add(w.gridX + dx to w.gridY + dy)
    }

    for (w in displaced) {
        val pos = firstFreeGridPos(w.spanX, w.spanY, occupied)
        val resolved = if (pos != null) w.copy(gridX = pos.first, gridY = pos.second) else w
        result.add(resolved)
        for (dx in 0 until resolved.spanX) for (dy in 0 until resolved.spanY) occupied.add(resolved.gridX + dx to resolved.gridY + dy)
    }

    return result
}

private fun buildOccupied(widgets: List<WidgetConfig>) = buildSet<Pair<Int, Int>> {
    widgets.forEach { w -> for (dx in 0 until w.spanX) for (dy in 0 until w.spanY) add(w.gridX + dx to w.gridY + dy) }
}

private fun widgetsOverlap(a: WidgetConfig, b: WidgetConfig): Boolean =
    a.gridX < b.gridX + b.spanX && a.gridX + a.spanX > b.gridX &&
    a.gridY < b.gridY + b.spanY && a.gridY + a.spanY > b.gridY

private fun firstFreeGridPos(spanX: Int, spanY: Int, occupied: Set<Pair<Int, Int>>): Pair<Int, Int>? {
    for (row in 0 until GRID_ROWS) for (col in 0 until GRID_COLS) {
        if (col + spanX > GRID_COLS || row + spanY > GRID_ROWS) continue
        if ((0 until spanX).all { dx -> (0 until spanY).all { dy -> (col + dx to row + dy) !in occupied } })
            return col to row
    }
    return null
}
