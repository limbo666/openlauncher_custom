package com.openlauncher.app.ui.screen

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import com.openlauncher.app.data.AppFont
import com.openlauncher.app.data.AppSettings
import com.openlauncher.app.data.telegram.TelegramBot
import com.openlauncher.app.data.DayNightMode
import com.openlauncher.app.data.ParticleEffect
import com.openlauncher.app.data.SidebarPosition
import com.openlauncher.app.data.TapSoundType
import com.openlauncher.app.data.ShortcutConfig
import com.openlauncher.app.data.GradientDirection
import com.openlauncher.app.data.UnitSystem
import com.openlauncher.app.data.FloatingActionType
import com.openlauncher.app.data.FloatingPanelAction
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.openlauncher.app.ui.theme.LocalDayMode
import com.openlauncher.app.ui.theme.LocalIsCustomMode
import com.openlauncher.app.ui.theme.LocalUiScales
import com.openlauncher.app.ui.theme.LocalHelpTextColor
import com.openlauncher.app.ui.theme.accentOnSurface
import com.openlauncher.app.ui.theme.contentColorOn
import com.openlauncher.app.ui.theme.effectiveAccent
import com.openlauncher.app.ui.theme.toFontFamily
import com.openlauncher.app.util.FileLog
import com.openlauncher.app.util.SafeExport
import com.openlauncher.app.util.SunriseSunset
import com.openlauncher.app.util.TapSound
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.openlauncher.app.ui.components.AppPickerDialog
import com.openlauncher.app.ui.components.ColorPickerDialog
import com.openlauncher.app.ui.components.ConfirmDialog
import com.openlauncher.app.ui.components.floatingActionIcon
import com.openlauncher.app.ui.components.floatingActionLabel
import com.openlauncher.app.ui.components.tapClickable
import com.openlauncher.app.ui.components.TapFilterChip
import com.openlauncher.app.ui.components.TapSwitch
import com.openlauncher.app.data.SettingsBackup
import com.openlauncher.app.obd.pairedAdapters
import com.openlauncher.app.ui.widget.AdapterPickerDialog
import androidx.compose.runtime.saveable.rememberSaveable
import kotlin.math.roundToInt

private val LocalSettingsSearchQuery = compositionLocalOf { "" }

/** Tracks each section header's scroll offset so the sticky banner can show where you are. */
private class SectionTracker {
    val positions = mutableStateMapOf<String, Float>()
    var containerTop = 0f
}
private val LocalSectionTracker = compositionLocalOf<SectionTracker?> { null }

/**
 * One fixed colour per section — drawn as a left-edge gutter and as the dot in the
 * sticky "current section" banner. Adjacent sections always differ, so a colour
 * change while scrolling = a section change, readable at a glance on the head unit.
 */
private val SECTION_COLORS = mapOf(
    "Appearance"        to Color(0xFFEC407A),
    "Typography"        to Color(0xFF7E57C2),
    "Tiles"             to Color(0xFF5C6BC0),
    "Vehicle & OBD"     to Color(0xFF66BB6A),
    "Sidebar"           to Color(0xFF8D6E63),
    "Home Pages"        to Color(0xFF9CCC65),
    "Display Animation" to Color(0xFFFF7043),
    "GPS & Calibration" to Color(0xFFFFA726),
    "Profiles"          to Color(0xFF26C6DA),
    "Gestures"          to Color(0xFF78909C),
    "Startup"           to Color(0xFFFFB300),
    "Sound on Tap"      to Color(0xFF9E9D24),
    "Recovery"          to Color(0xFFD81B60),
    "Backup & Restore"  to Color(0xFF5E35B1),
    "Floating Panel"    to Color(0xFF00ACC1),
    "Permissions"       to Color(0xFFCDDC39),
    "About"             to Color(0xFF4DB6AC),
    "Maintenance"       to Color(0xFF64B5F6),
    "Telegram Messages" to Color(0xFF2AABEE)
)

private fun sectionColorFor(title: String): Color =
    SECTION_COLORS[title]
        ?: Color.hsv((kotlin.math.abs(title.hashCode()) % 360).toFloat(), 0.5f, 0.85f)

// Searchable terms per section so a query like "color" can highlight the sections
// that actually contain color settings (Appearance, Tiles) without filtering others out.
// Matching is substring-based: a section matches when its title OR any keyword here
// contains the user's query (case-insensitive), so multi-word keywords also catch
// shorter queries ("network speed" matches "speed", "two finger" matches "finger").
private val SECTION_KEYWORDS = mapOf(
    "Appearance" to listOf("accent", "color", "colour", "background", "font color", "help text", "gradient", "wallpaper", "status bar", "network speed", "dim", "theme", "day", "night", "light", "dark", "mode", "particle", "snow", "rain", "stars", "video", "indicator", "upload", "download", "display mode"),
    "Typography" to listOf("font", "bold", "text scale", "ui scale", "scale", "size", "text", "weight", "zoom", "enlarge", "bigger", "smaller", "letter", "spacing"),
    "Tiles" to listOf("tile", "color", "colour", "transparency", "opacity", "alpha", "widget background", "glass", "dark", "light"),
    "Sidebar" to listOf("sidebar", "menubar", "menu bar", "shortcuts", "slot", "icon size", "width", "position", "bar", "left", "right", "bottom", "horizontal", "vertical", "dock", "nav", "navigation", "add shortcut", "thickness", "height", "launcher bar"),
    "Vehicle & OBD" to listOf("vehicle", "name", "unit", "metric", "imperial", "obd", "obd2", "elm327", "adapter", "bluetooth", "fallback", "image", "km", "miles", "mph", "kmh", "speed unit", "temperature", "diagnostic", "can bus", "car name"),
    "Sound on Tap" to listOf("sound", "tap", "tick", "beep", "click", "feedback", "audio", "click sound", "press sound", "haptic"),
    "Home Pages" to listOf("auto-switch", "auto switch", "driving", "page", "speed", "idle", "switch", "parked", "park", "gps speed", "threshold", "stop", "driving page", "auto page"),
    "Display Animation" to listOf("animation", "car", "image", "vehicle", "picture", "logo", "moving car", "drive", "rest"),
    "GPS & Calibration" to listOf("gps", "a-gps", "agps", "magnetometer", "compass", "calibration", "offset", "heading", "north", "reset gps", "assist", "parking lot", "sweep", "alignment", "heading offset"),
    "Floating Panel" to listOf("floating", "panel", "overlay", "quick controls", "buttons", "opacity", "size", "draw over", "vertical", "horizontal", "orientation", "floating panel", "draw over other apps", "panel size", "panel opacity", "layout", "dock"),
    "Backup & Restore" to listOf("backup", "export", "restore", "json", "import", "save settings", "load settings", "transfer", "file", "migrate"),
    "Permissions" to listOf("permission", "launcher", "notification", "location", "draw over", "access", "microphone", "mic", "record", "usage", "accessibility", "back", "default launcher", "home app", "grant", "allow", "deny", "default home", "back key"),
    "Gestures" to listOf("gesture", "two finger", "three finger", "2 finger", "3 finger", "tap", "multi", "touch", "swipe", "pinch", "double tap", "finger", "assign app", "shortcut"),
    "Profiles" to listOf("profile", "driver", "user", "switch", "person", "account", "wifi", "hotspot", "ssid", "driver profile", "rename", "export", "import", "json", "backup profile", "multiple drivers", "preset"),
    "Startup" to listOf("startup", "auto-launch", "auto launch", "boot", "delay", "launch", "open app", "start", "auto open", "on boot", "autostart", "wait"),
    "Recovery" to listOf("restart", "recovery", "reload", "reboot", "crash", "fix", "defence", "defense", "restart launcher", "force restart", "stuck", "freeze", "unresponsive"),
    "About" to listOf("update", "github", "version", "fork", "repo", "source", "download", "latest", "check updates"),
    "Maintenance" to listOf("diagnostics", "status", "volume", "calibration", "debug", "log", "setup", "wizard", "reset", "defaults", "system status", "volume calibration", "mute probe", "debug log", "export log", "setup wizard", "onboarding", "reset settings", "factory reset", "troubleshoot", "repair", "health"),
    "Telegram Messages" to listOf("telegram", "bot", "message", "driver inbox", "inbox", "remote", "notify", "notification", "telegram bot", "botfather", "remote message", "sms", "message driver", "notify driver")
)

// Resolved at call site via LocalDayMode — see SettingsDivider / SettingsSection

@Composable
fun SettingsScreen(
    settings: AppSettings,
    accent: Color,
    onUpdate: (AppSettings.() -> AppSettings) -> Unit,
    onReset: () -> Unit,
    onOpenDiagnostics: () -> Unit = {},
    onOpenVolumeCalibration: () -> Unit = {},
    onOpenTelegram: () -> Unit = {},
    onSwitchProfile: (Int) -> Unit = {},
    onExportProfile: (Int, (String?) -> Unit) -> Unit = { _, _ -> },
    onImportProfile: (Int, String, (String?) -> Unit) -> Unit = { _, _, _ -> },
    apps: List<com.openlauncher.app.model.AppInfo> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    // Sliders emit dozens of values per second while dragging — debounce them so
    // DataStore writes are batched (one write per ~250 ms of inactivity) instead
    // of hammering the disk on slow head-unit flash. The final value is always
    // flushed when the screen leaves composition.
    val debouncedUpdate = rememberDebouncedUpdate(onUpdate)
    var showResetDialog       by remember { mutableStateOf(false) }
    var showAccentPicker      by remember { mutableStateOf(false) }
    var showBgPicker          by remember { mutableStateOf(false) }
    var showGradientEndPicker by remember { mutableStateOf(false) }
    var showFontColorPicker   by remember { mutableStateOf(false) }
    var showHelpTextPicker    by remember { mutableStateOf(false) }
    var showTileDarkPicker    by remember { mutableStateOf(false) }
    var showTileLightPicker   by remember { mutableStateOf(false) }
    var floatingSlotEditor    by remember { mutableStateOf<Int?>(null) }
    var showFontPicker        by remember { mutableStateOf(false) }
    // Hidden glare options — revealed as an easter egg by tapping "Tiles" 5×.
    var glareRevealed by rememberSaveable { mutableStateOf(false) }
    var glareTaps     by remember { mutableIntStateOf(0) }
    // Hidden Gestures section — revealed as an easter egg by tapping "Sound on Tap" 5×.
    var gesturesRevealed by rememberSaveable { mutableStateOf(false) }
    var gesturesTaps     by remember { mutableIntStateOf(0) }
    // 0 = auto-launch app, 1 = two-finger gesture, 2 = three-finger gesture
    var pendingAppPick        by remember { mutableStateOf<Int?>(null) }

    // OpenDocument (not GetContent): only SAF document URIs carry a persistable
    // grant, so this is what actually keeps the wallpaper readable after reboot
    val wallpaperPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            // Picking a static image takes over the background — turn the video
            // wallpaper off so it doesn't keep covering the new picture.
            onUpdate { copy(wallpaperUri = it.toString(), wallpaperVideoUri = "") }
        }
    }

    // Video wallpaper — persistable SAF grant. Uses a hand-built intent so we
    // can pass EXTRA_INITIAL_URI: without it some ROMs' file pickers reopen the
    // last-used pictures view (hiding videos) instead of the all-files view.
    val videoWallpaperPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        if (result.resultCode == android.app.Activity.RESULT_OK && uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onUpdate { copy(wallpaperVideoUri = uri.toString()) }
        }
    }

    // Car image for the "display animation" feature (same persistable SAF grant)
    val carImagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onUpdate { copy(carImageUri = it.toString()) }
        }
    }

    val isDayMode = LocalDayMode.current
    val screenBg  = MaterialTheme.colorScheme.background
    val swatchBorderColor = if (isDayMode) Color(0xFFCCCCCC) else Color.White.copy(alpha = 0.35f)
    val selectedChipContainer = accentOnSurface(accent, screenBg)
    val selectedChipLabel     = contentColorOn(selectedChipContainer)
    val specialCardColor      = if (isDayMode) Color(0xFFFFFFFF) else Color(0xFF161616)
    val specialBorderColor    = if (isDayMode) Color(0xFFCCCCCC) else Color(0xFF282828)
    val specialIconTint       = accentOnSurface(accent, specialCardColor)

    val categories = listOf(
        CategoryItem("All", "All", Icons.Default.Apps),
        CategoryItem("Display", "Display", Icons.Default.Palette),
        CategoryItem("Dashboard", "Dashboard", Icons.Default.Dashboard),
        CategoryItem("Vehicle & OBD", "Vehicle", Icons.Default.DirectionsCar),
        CategoryItem("Launcher", "Launcher", Icons.Default.Settings)
    )
    var selectedCategory by rememberSaveable { mutableStateOf(
        settings.settingsCategory.takeIf { c -> categories.any { it.id == c } } ?: "Display"
    ) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val scrollState = rememberScrollState()
    val sectionTracker = remember { SectionTracker() }
    LaunchedEffect(selectedCategory, searchQuery) { scrollState.scrollTo(0) }
    LaunchedEffect(selectedCategory) {
        if (selectedCategory != settings.settingsCategory) {
            onUpdate { copy(settingsCategory = selectedCategory) }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
    ) {
    CompositionLocalProvider(
        LocalSettingsSearchQuery provides searchQuery,
        LocalSectionTracker provides sectionTracker
    ) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .onGloballyPositioned {
                val top = it.boundsInRoot().top
                if (sectionTracker.containerTop != top) sectionTracker.containerTop = top
            }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ── Title ─────────────────────────────────────────────────────────────
        Text(
            text          = "SETTINGS",
            style         = MaterialTheme.typography.titleLarge,
            color         = MaterialTheme.colorScheme.onBackground,
            letterSpacing = 3.sp,
            fontSize      = 14.sp
        )

        // ── Category tiles (3 per row, two rows) ───────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.take(3).forEach { cat ->
                    CategoryTile(
                        cat       = cat,
                        selected  = selectedCategory == cat.id,
                        accent    = accent,
                        isDayMode = isDayMode,
                        modifier  = Modifier.weight(1f),
                        onClick   = { selectedCategory = cat.id }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.drop(3).forEach { cat ->
                    CategoryTile(
                        cat       = cat,
                        selected  = selectedCategory == cat.id,
                        accent    = accent,
                        isDayMode = isDayMode,
                        modifier  = Modifier.weight(1f),
                        onClick   = { selectedCategory = cat.id }
                    )
                }
                SearchField(
                    query         = searchQuery,
                    onQueryChange = { searchQuery = it },
                    accent        = accent,
                    isDayMode     = isDayMode,
                    modifier      = Modifier.weight(1f).height(48.dp)
                )
            }
        }

        // ── Open System Settings (own row, half width) ─────────────────────────
        Surface(
            onClick = {
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            },
            shape = RoundedCornerShape(8.dp),
            color = specialCardColor,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                specialBorderColor
            ),
            modifier = Modifier.fillMaxWidth(0.5f).height(48.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp).fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    tint = specialIconTint,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Open System Settings",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDayMode) Color(0xFF111111) else Color(0xFFDDDDDD),
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Access Android system settings, Wi-Fi, Bluetooth & display",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDayMode) Color(0xFF888888) else Color(0xFF666666),
                        fontSize = 11.sp
                    )
                }
                Icon(
                    Icons.Default.OpenInNew,
                    contentDescription = null,
                    tint = if (isDayMode) Color(0xFF888888) else Color(0xFF555555),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // ── Appearance ───────────────────────────────────────────────────────
        if (searchQuery.isNotBlank() || selectedCategory == "All" || selectedCategory == "Display") {
        SettingsSection("Appearance") {
            // Display Mode
            SettingsRow(
                label    = "Display Mode",
                sublabel = when (settings.dayNightMode) {
                    DayNightMode.DARK   -> "Always dark (pure black)"
                    DayNightMode.LIGHT  -> "Always light"
                    DayNightMode.CUSTOM -> "Custom colors & gradient"
                    DayNightMode.AUTO   -> "Sunrise / sunset"
                    DayNightMode.SYSTEM -> "Follows system theme"
                },
                icon = when (settings.dayNightMode) {
                    DayNightMode.DARK   -> Icons.Default.NightlightRound
                    DayNightMode.LIGHT  -> Icons.Default.LightMode
                    DayNightMode.CUSTOM -> Icons.Default.Palette
                    DayNightMode.AUTO   -> Icons.Default.Brightness4
                    DayNightMode.SYSTEM -> Icons.Default.PhoneAndroid
                }
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DayNightMode.entries.forEach { mode ->
                        TapFilterChip(
                            selected = settings.dayNightMode == mode,
                            onClick  = {
                                onUpdate {
                                    copy(
                                        dayNightMode = mode,
                                        useCustomBackgroundColor = (mode == DayNightMode.CUSTOM)
                                    )
                                }
                            },
                            label    = {
                                Text(
                                    text      = when (mode) {
                                        DayNightMode.DARK   -> "Dark"
                                        DayNightMode.LIGHT  -> "Light"
                                        DayNightMode.CUSTOM -> "Custom"
                                        DayNightMode.AUTO   -> "Sunset"
                                        DayNightMode.SYSTEM -> "System"
                                    },
                                    fontSize  = 9.sp,
                                    letterSpacing = 0.5.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = selectedChipContainer,
                                selectedLabelColor     = selectedChipLabel
                            )
                        )
                    }
                }
            }

            SettingsDivider()

            // Accent color - always available
            SettingsRow(
                label    = "Accent Color",
                sublabel = "UI highlight color",
                icon     = Icons.Default.Colorize
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(settings.effectiveAccent())
                        .border(1.dp, swatchBorderColor, RoundedCornerShape(8.dp))
                        .clickable { showAccentPicker = true }
                )
            }

            // Custom mode only controls
            if (settings.dayNightMode == DayNightMode.CUSTOM) {
                SettingsDivider()

                // Background color + gradient
                SettingsRow(
                    label    = "Background",
                    sublabel = if (settings.useGradient) "Gradient" else "Solid color",
                    icon     = Icons.Default.FormatColorFill
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        // Start color swatch
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(settings.backgroundColor))
                                .border(1.dp, swatchBorderColor, RoundedCornerShape(4.dp))
                                .clickable { showBgPicker = true }
                        )
                        if (settings.useGradient) {
                            androidx.compose.material3.Icon(
                                Icons.Default.ArrowForward, null,
                                tint = if (isDayMode) Color(0xFF999999) else Color(0xFF555555), modifier = Modifier.size(14.dp)
                            )
                            // End color swatch
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(settings.gradientEndColor))
                                    .border(1.dp, swatchBorderColor, RoundedCornerShape(4.dp))
                                    .clickable { showGradientEndPicker = true }
                            )
                            // Combined gradient preview swatch
                            Box(
                                modifier = Modifier
                                    .size(width = 38.dp, height = 28.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(settings.backgroundColor), Color(settings.gradientEndColor))
                                        )
                                    )
                                    .border(1.dp, swatchBorderColor, RoundedCornerShape(4.dp))
                            )
                        }
                        TextButton(
                            onClick = {
                                onUpdate {
                                    copy(
                                        backgroundColor = Color.Black.toArgb(),
                                        gradientEndColor = Color.Black.toArgb(),
                                        useGradient = false
                                    )
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("RESET", color = accent, fontSize = 9.sp, letterSpacing = 1.sp)
                        }
                    }
                }

                SettingsDivider()

                // Font Color row
                SettingsRow(
                    label    = "Font Color",
                    sublabel = "Custom text color in custom mode",
                    icon     = Icons.Default.FormatSize
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(settings.fontColor))
                            .border(1.dp, swatchBorderColor, RoundedCornerShape(8.dp))
                            .clickable { showFontColorPicker = true }
                    )
                }

                SettingsDivider()

                // Help Text Color row
                SettingsRow(
                    label    = "Help Text Color",
                    sublabel = "Color for categories, explanations & widget secondary text (all themes)",
                    icon     = Icons.Default.TextSnippet
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(settings.helpTextColor?.let { Color(it) } ?: if (isDayMode) Color(0xFF888888) else Color(0xFF444444))
                                .border(1.dp, swatchBorderColor, RoundedCornerShape(8.dp))
                                .clickable { showHelpTextPicker = true }
                        )
                        Spacer(Modifier.width(6.dp))
                        TextButton(
                            onClick = { onUpdate { copy(helpTextColor = null) } },
                            contentPadding = PaddingValues(horizontal = 6.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("AUTO", color = accent, fontSize = 9.sp, letterSpacing = 1.sp)
                        }
                    }
                }

                SettingsDivider()

                SettingsRow(label = "Use Gradient", sublabel = "Blend two colors as background", icon = Icons.Default.Gradient) {
                    TapSwitch(checked = settings.useGradient,
                        onCheckedChange = { onUpdate { copy(useGradient = it, useCustomBackgroundColor = true) } },
                        colors = switchColors(accent))
                }

                if (settings.useGradient) {
                    SettingsDivider()
                    SettingsRow(
                        label    = "Gradient Direction",
                        sublabel = when (settings.gradientDirection) {
                            GradientDirection.TOP_TO_BOTTOM -> "Top to Bottom"
                            GradientDirection.LEFT_TO_RIGHT -> "Left to Right"
                            GradientDirection.DIAGONAL      -> "Diagonal (Linear)"
                            GradientDirection.RADIAL        -> "Radial (Circular)"
                        },
                        icon     = Icons.Default.TrendingFlat
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            GradientDirection.entries.forEach { dir ->
                                TapFilterChip(
                                    selected = settings.gradientDirection == dir,
                                    onClick  = { onUpdate { copy(gradientDirection = dir) } },
                                    label    = {
                                        Text(
                                            text      = when (dir) {
                                                GradientDirection.TOP_TO_BOTTOM -> "Vertical"
                                                GradientDirection.LEFT_TO_RIGHT -> "Horizontal"
                                                GradientDirection.DIAGONAL      -> "Diagonal"
                                                GradientDirection.RADIAL        -> "Radial"
                                            },
                                            fontSize  = 9.sp,
                                            letterSpacing = 0.5.sp
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = selectedChipContainer,
                                        selectedLabelColor     = selectedChipLabel
                                    )
                                )
                            }
                        }
                    }
                }
            }

            SettingsDivider()

            // Wallpaper
            SettingsButton(
                label    = "Set Wallpaper",
                sublabel = if (settings.wallpaperUri.isNotEmpty())
                               if (settings.wallpaperVideoUri.isNotEmpty()) "Custom image active — hidden behind the video wallpaper"
                               else "Custom wallpaper active"
                           else "Choose image from gallery",
                icon     = Icons.Default.Wallpaper,
                accent   = accent,
                onClick  = { wallpaperPicker.launch(arrayOf("image/*")) }
            )
            if (settings.wallpaperUri.isNotEmpty() || settings.wallpaperVideoUri.isNotEmpty()) {
                Column {
                    SettingsRow(
                        label    = "Wallpaper Dim",
                        sublabel = "${"%.0f".format(settings.wallpaperDim * 100)}%",
                        icon     = Icons.Default.BrightnessLow
                    ) {}
                    Slider(
                        value         = settings.wallpaperDim,
                        onValueChange = { debouncedUpdate { copy(wallpaperDim = it) } },
                        valueRange    = 0f..0.95f,
                        steps         = 18,
                        colors        = sliderColors(accent),
                        modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        // One action removes the whole background: image AND video.
                        onClick  = { onUpdate { copy(wallpaperUri = "", wallpaperVideoUri = "") } },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("REMOVE WALLPAPER", color = Color(0xFF993333), fontSize = 9.sp, letterSpacing = 1.sp)
                    }
                }
            }

            SettingsDivider()

            // Video wallpaper — muted looping video behind the UI
            SettingsButton(
                label    = "Video Wallpaper",
                sublabel = if (settings.wallpaperVideoUri.isNotEmpty())
                               "Animated wallpaper active — H.264 MP4 recommended. Picking a new image turns the video off"
                           else "Choose a looping video (plays muted, over the image wallpaper)",
                icon     = Icons.Default.Videocam,
                accent   = accent,
                onClick  = {
                    // Show ALL files, and pin the picker to the Downloads folder:
                    // the ROM's video MIME filter can hide perfectly fine videos,
                    // and some builds reopen a stale pictures-only view otherwise.
                    val intent = android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(android.content.Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                        putExtra(android.content.Intent.EXTRA_MIME_TYPES, arrayOf("*/*"))
                        runCatching {
                            putExtra(
                                android.provider.DocumentsContract.EXTRA_INITIAL_URI,
                                android.provider.DocumentsContract.buildDocumentUri(
                                    "com.android.externalstorage.documents",
                                    "primary:Download"
                                )
                            )
                        }
                    }
                    videoWallpaperPicker.launch(intent)
                }
            )
            if (settings.wallpaperVideoUri.isNotEmpty()) {
                TextButton(
                    onClick  = { onUpdate { copy(wallpaperVideoUri = "") } },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("REMOVE VIDEO WALLPAPER", color = Color(0xFF993333), fontSize = 9.sp, letterSpacing = 1.sp)
                }
            }

            SettingsDivider()

            // Ambient particles
            SettingsRow(
                label    = "Particle Effect",
                sublabel = "Ambient snow, rain, stars or constellation drifting over the home screen",
                icon     = Icons.Default.AutoAwesome
            ) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        ParticleEffect.NONE to "Off",
                        ParticleEffect.SNOW to "Snow",
                        ParticleEffect.RAIN to "Rain",
                        ParticleEffect.STARS to "Stars",
                        ParticleEffect.CONSTELLATION to "Constellation"
                    ).forEach { (effect, label) ->
                        TapFilterChip(
                            selected = settings.particleEffect == effect,
                            onClick  = { onUpdate { copy(particleEffect = effect) } },
                            label    = { Text(label, fontSize = 10.sp) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = selectedChipContainer,
                                selectedLabelColor     = selectedChipLabel
                            )
                        )
                    }
                }
            }
            if (settings.particleEffect != ParticleEffect.NONE) {
                SettingsRow(
                    label    = "Particle Amount",
                    sublabel = "${"%.0f".format(settings.particleEffectAmount * 100)}% — how many particles",
                    icon     = Icons.Default.ScatterPlot
                ) {}
                Slider(
                    value         = settings.particleEffectAmount,
                    onValueChange = { debouncedUpdate { copy(particleEffectAmount = it) } },
                    valueRange    = 0.2f..1.5f,
                    steps         = 12,
                    colors        = sliderColors(accent),
                    modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
            }

            SettingsDivider()

            SettingsRow(
                label    = "Status Bar Indicators",
                sublabel = if (settings.showStatusIcons) "Wi-Fi, Bluetooth & Cellular icons visible in top bar" else "Hidden (prevents duplication with system status bar)",
                icon     = Icons.Default.Wifi
            ) {
                TapSwitch(
                    checked         = settings.showStatusIcons,
                    onCheckedChange = { onUpdate { copy(showStatusIcons = it) } },
                    colors          = switchColors(accent)
                )
            }

            SettingsDivider()

            SettingsRow(
                label    = "Network Speed Indicator",
                sublabel = "Show live upload/download speed in the top bar",
                icon     = Icons.Default.SwapVert
            ) {
                TapSwitch(
                    checked         = settings.showNetworkSpeedIndicator,
                    onCheckedChange = { onUpdate { copy(showNetworkSpeedIndicator = it) } },
                    colors          = switchColors(accent)
                )
            }
        }
        }

        // ── Typography ───────────────────────────────────────────────────────
        if (searchQuery.isNotBlank() || selectedCategory == "All" || selectedCategory == "Display") {
        SettingsSection("Typography") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                SettingsButton(
                    label    = "Font",
                    sublabel = fontDisplayName(settings.appFont),
                    icon     = Icons.Default.FontDownload,
                    accent   = accent,
                    onClick  = { TapSound.play(); showFontPicker = true }
                )
            }

            SettingsDivider()

            SettingsRow(label = "Bold Font", sublabel = "Heavier weight across all text", icon = Icons.Default.FormatBold) {
                TapSwitch(
                    checked         = settings.fontBold,
                    onCheckedChange = { onUpdate { copy(fontBold = it) } },
                    colors          = switchColors(accent)
                )
            }

            SettingsDivider()

            Column {
                SettingsRow(
                    label    = "Text Scale",
                    sublabel = "${"%.0f".format(settings.textScale * 100)}%",
                    icon     = Icons.Default.TextFields
                ) {}
                Slider(
                    value         = settings.textScale,
                    onValueChange = { debouncedUpdate { copy(textScale = it) } },
                    valueRange    = 0.8f..2.0f,
                    steps         = 12,
                    colors        = sliderColors(accent),
                    modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
            }

            SettingsDivider()

            Column {
                SettingsRow(
                    label    = "UI Scale",
                    sublabel = "${"%.0f".format(settings.uiScale * 100)}%  — scales all elements",
                    icon     = Icons.Default.ZoomIn
                ) {}
                Slider(
                    value         = settings.uiScale,
                    onValueChange = { debouncedUpdate { copy(uiScale = it) } },
                    valueRange    = 0.7f..2.0f,
                    steps         = 13,
                    colors        = sliderColors(accent),
                    modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
            }
        }
        }

        // ── Tiles ─────────────────────────────────────────────────────────────
        if (searchQuery.isNotBlank() || selectedCategory == "All" || selectedCategory == "Display") {
        SettingsSection(
            title       = "Tiles",
            onHeaderTap = {
                if (!glareRevealed) {
                    glareTaps++
                    if (glareTaps >= 5) { glareRevealed = true; glareTaps = 0; TapSound.play() }
                }
            }
        ) {
            SettingsRow(
                label    = "Tile Color (Dark)",
                sublabel = "Widget background in dark / custom mode",
                icon     = Icons.Default.DarkMode
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(settings.tileColorDark))
                        .border(1.dp, swatchBorderColor, RoundedCornerShape(8.dp))
                        .clickable { showTileDarkPicker = true }
                )
            }

            SettingsDivider()

            SettingsRow(
                label    = "Tile Color (Light)",
                sublabel = "Widget background in light mode",
                icon     = Icons.Default.LightMode
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(settings.tileColorLight))
                        .border(1.dp, swatchBorderColor, RoundedCornerShape(8.dp))
                        .clickable { showTileLightPicker = true }
                )
            }

            SettingsDivider()

            Column {
                SettingsRow(
                    label    = "Tile Transparency",
                    sublabel = "${"%.0f".format(settings.tileAlpha * 100)}% opacity",
                    icon     = Icons.Default.Opacity
                ) {}
                Slider(
                    value         = settings.tileAlpha,
                    onValueChange = { debouncedUpdate { copy(tileAlpha = it) } },
                    valueRange    = 0.1f..1f,
                    steps         = 8,
                    colors        = sliderColors(accent),
                    modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
            }

            // Hidden — only shown after the "Tiles" header easter-egg tap.
            if (glareRevealed) {
                SettingsDivider()

                SettingsRow(
                    label    = "Light Glare",
                    sublabel = "A soft light sweep passing across the widgets",
                    icon     = Icons.Default.Flare
                ) {
                    TapSwitch(
                        checked         = settings.glareEnabled,
                        onCheckedChange = { onUpdate { copy(glareEnabled = it) } },
                        colors          = switchColors(accent)
                    )
                }

                if (settings.glareEnabled) {
                    Column {
                        SettingsRow(
                            label    = "Glare Strength",
                            sublabel = "${"%.0f".format(settings.glareIntensity * 100)}%",
                            icon     = Icons.Default.BrightnessHigh
                        ) {}
                        Slider(
                            value         = settings.glareIntensity,
                            onValueChange = { debouncedUpdate { copy(glareIntensity = it) } },
                            valueRange    = 0.15f..1f,
                            steps         = 8,
                            colors        = sliderColors(accent),
                            modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
        }

        // ── Vehicle & OBD ────────────────────────────────────────────────────
        if (searchQuery.isNotBlank() || selectedCategory == "All" || selectedCategory == "Vehicle & OBD") {
        SettingsSection("Vehicle & OBD") {
            var nameInput by remember(settings.vehicleName) { mutableStateOf(settings.vehicleName) }
            SettingsRow(
                label    = "Vehicle Name",
                sublabel = "",
                icon     = Icons.Default.DirectionsCar
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value         = nameInput,
                        onValueChange = { nameInput = it },
                        placeholder   = { Text("MY CAR", color = if (isDayMode) Color(0xFF999999) else Color(0xFF444444), fontSize = 12.sp) },
                        singleLine    = true,
                        textStyle     = LocalTextStyle.current.copy(fontSize = 12.sp, color = if (isDayMode) Color(0xFF111111) else Color.White),
                        colors        = outlinedFieldColors(accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                        modifier      = Modifier.width(140.dp)
                    )
                    if (nameInput != settings.vehicleName) {
                        IconButton(onClick = { onUpdate { copy(vehicleName = nameInput) } }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Check, "Save", tint = accent, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            SettingsDivider()

            SettingsRow(
                label    = "Unit System",
                sublabel = if (settings.unitSystem == UnitSystem.METRIC) "Metric (°C, km/h, km)" else "Imperial (°F, mph, mi)",
                icon     = Icons.Default.Straighten
            ) {
                Row {
                    TapFilterChip(
                        selected = settings.unitSystem == UnitSystem.METRIC,
                        onClick  = { onUpdate { copy(unitSystem = UnitSystem.METRIC) } },
                        label    = { Text("Metric", fontSize = 11.sp) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = selectedChipContainer,
                            selectedLabelColor     = selectedChipLabel
                        )
                    )
                    Spacer(Modifier.width(6.dp))
                    TapFilterChip(
                        selected = settings.unitSystem == UnitSystem.IMPERIAL,
                        onClick  = { onUpdate { copy(unitSystem = UnitSystem.IMPERIAL) } },
                        label    = { Text("Imperial", fontSize = 11.sp) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = selectedChipContainer,
                            selectedLabelColor     = selectedChipLabel
                        )
                    )
                }
            }

            SettingsDivider()

            // OBD2 Device Selection
            var showObdPicker by remember { mutableStateOf(false) }
            if (showObdPicker) {
                AdapterPickerDialog(
                    currentAddress = settings.obdDeviceAddress,
                    adapters       = remember { pairedAdapters(context) },
                    accent         = accent,
                    onPick         = { adapter ->
                        showObdPicker = false
                        onUpdate { copy(obdDeviceAddress = adapter.address, obdDeviceName = adapter.name) }
                    },
                    onDismiss      = { showObdPicker = false }
                )
            }

            SettingsRow(
                label    = "OBD2 Bluetooth Adapter",
                sublabel = if (settings.obdDeviceAddress.isNotBlank())
                    "${settings.obdDeviceName.ifBlank { "Adapter" }} (${settings.obdDeviceAddress})"
                else
                    "No adapter selected — pair an ELM327 in Bluetooth settings",
                icon     = Icons.Default.Bluetooth
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { showObdPicker = true }) {
                        Text(
                            text = if (settings.obdDeviceAddress.isBlank()) "Choose" else "Change",
                            color = accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (settings.obdDeviceAddress.isNotBlank()) {
                        TextButton(onClick = { onUpdate { copy(obdDeviceAddress = "", obdDeviceName = "") } }) {
                            Text("Clear", color = if (isDayMode) Color(0xFF666666) else Color(0xFFAAAAAA), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
        }

        // ── Sidebar ──────────────────────────────────────────────────────────
        if (searchQuery.isNotBlank() || selectedCategory == "All" || selectedCategory == "Display") {
        SettingsSection("Sidebar") {
            SettingsRow(
                label    = "Sidebar Position",
                sublabel = when (settings.sidebarPosition) {
                    SidebarPosition.LEFT   -> "Left side"
                    SidebarPosition.RIGHT  -> "Right side"
                    SidebarPosition.BOTTOM -> "Bottom"
                },
                icon     = Icons.Default.SwapHoriz
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SidebarPosition.entries.forEach { pos ->
                        TapFilterChip(
                            selected = settings.sidebarPosition == pos,
                            onClick  = { onUpdate { copy(sidebarPosition = pos) } },
                            label    = {
                                Text(
                                    when (pos) {
                                        SidebarPosition.LEFT   -> "Left"
                                        SidebarPosition.RIGHT  -> "Right"
                                        SidebarPosition.BOTTOM -> "Bottom"
                                    },
                                    fontSize = 9.sp,
                                    letterSpacing = 0.5.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = selectedChipContainer,
                                selectedLabelColor     = selectedChipLabel
                            )
                        )
                    }
                }
            }

            SettingsDivider()

            Column {
                SettingsRow(
                    label    = "Menubar Width",
                    sublabel = "${settings.sidebarWidth}dp — bar thickness (left / right / bottom)",
                    icon     = Icons.Default.Straighten
                ) {}
                Slider(
                    value         = settings.sidebarWidth.toFloat(),
                    onValueChange = { debouncedUpdate { copy(sidebarWidth = it.roundToInt()) } },
                    valueRange    = 40f..120f,
                    steps         = 15,
                    colors        = sliderColors(accent),
                    modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
            }

            SettingsDivider()

            Column {
                SettingsRow(
                    label    = "Menubar Icon Size",
                    sublabel = "${"%.0f".format(settings.sidebarIconScale * 100)}%",
                    icon     = Icons.Default.ZoomIn
                ) {}
                Slider(
                    value         = settings.sidebarIconScale,
                    onValueChange = { debouncedUpdate { copy(sidebarIconScale = it) } },
                    valueRange    = 0.6f..1.8f,
                    steps         = 11,
                    colors        = sliderColors(accent),
                    modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
            }

            if (settings.sidebarPosition == SidebarPosition.BOTTOM) {
                SettingsDivider()
                SettingsRow(
                    label    = "Shortcuts Side",
                    sublabel = if (settings.bottomBarShortcutsRight) "Right — nav buttons on left" else "Left — nav buttons on right",
                    icon     = Icons.Default.FormatAlignRight
                ) {
                    TapSwitch(
                        checked         = settings.bottomBarShortcutsRight,
                        onCheckedChange = { onUpdate { copy(bottomBarShortcutsRight = it) } },
                        colors          = switchColors(accent)
                    )
                }
            }

            SettingsDivider()

            settings.shortcuts.forEachIndexed { index, shortcut ->
                if (index > 0) SettingsDivider()
                SettingsRow(
                    label    = "Slot ${index + 1}",
                    sublabel = when {
                        shortcut.label.isNotEmpty()       -> shortcut.label
                        shortcut.packageName.isNotEmpty() -> shortcut.packageName
                        else                              -> "Empty"
                    },
                    icon     = Icons.Default.Apps
                ) {
                    if (settings.shortcuts.size > 1) {
                        IconButton(
                            onClick  = {
                                onUpdate {
                                    copy(shortcuts = shortcuts.toMutableList().also { it.removeAt(index) })
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, null, tint = Color(0xFF993333), modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            SettingsDivider()

            SettingsButton(
                label    = "Add Slot",
                sublabel = "Append an empty shortcut to the sidebar",
                icon     = Icons.Default.Add,
                accent   = accent,
                onClick  = { onUpdate { copy(shortcuts = shortcuts + ShortcutConfig()) } }
            )
        }
        }

        // ── Home Pages (swipe left/right) ─────────────────────────────────────
        if (searchQuery.isNotBlank() || selectedCategory == "All" || selectedCategory == "Dashboard") {
        SettingsSection("Home Pages") {
            SettingsRow(
                label    = "Auto-Switch Pages",
                sublabel = "Switch pages automatically by driving state (GPS speed)",
                icon     = Icons.Default.SwapHoriz
            ) {
                TapSwitch(
                    checked         = settings.autoSwitchPages,
                    onCheckedChange = { onUpdate { copy(autoSwitchPages = it) } },
                    colors          = switchColors(accent)
                )
            }
            SettingsDivider()
            SettingsRow(
                label    = "Driving Column",
                sublabel = if (settings.autoSwitchPages) "RIGHT = pages 2 & 4 while driving, LEFT = pages 1 & 3 while parked" else "Enable auto-switch to select",
                icon     = Icons.Default.DirectionsCar
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isDayMode) Color(0xFFE0E0E0) else Color(0xFF222222))
                        .padding(2.dp)
                ) {
                    listOf(0 to "LEFT", 1 to "RIGHT").forEach { (page, label) ->
                        val selected = settings.drivingPage == page
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (selected) accent else Color.Transparent)
                                .clickable { onUpdate { copy(drivingPage = page) } }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (selected) Color.Black else (if (isDayMode) Color(0xFF333333) else Color(0xFFAAAAAA)),
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
            SettingsDivider()
            SettingsRow(
                label    = "Switch Speed",
                sublabel = "${"%.0f".format(settings.autoSwitchSpeedKmh)} km/h — counts as driving above this speed",
                icon     = Icons.Default.Speed
            ) {
                Slider(
                    value         = settings.autoSwitchSpeedKmh,
                    onValueChange = { debouncedUpdate { copy(autoSwitchSpeedKmh = it) } },
                    valueRange    = 5f..20f,
                    steps         = 14,
                    colors        = sliderColors(accent),
                    modifier      = Modifier.width(140.dp)
                )
            }
            SettingsDivider()
            SettingsRow(
                label    = "Idle Time",
                sublabel = "${"%.0f".format(settings.autoSwitchIdleSeconds)} s stopped before switching back to the parked page",
                icon     = Icons.Default.Timer
            ) {
                Slider(
                    value         = settings.autoSwitchIdleSeconds,
                    onValueChange = { debouncedUpdate { copy(autoSwitchIdleSeconds = it) } },
                    valueRange    = 5f..30f,
                    steps         = 24,
                    colors        = sliderColors(accent),
                    modifier      = Modifier.width(140.dp)
                )
            }
        }
        }

        // ── Display Animation (car image) ──────────────────────────────────────
        if (searchQuery.isNotBlank() || selectedCategory == "All" || selectedCategory == "Dashboard") {
        SettingsSection("Display Animation") {
            SettingsRow(
                label    = "Display Animation",
                sublabel = "Car image beside the vehicle name — drives across when moving, rests when stopped",
                icon     = Icons.Default.DirectionsCar
            ) {
                TapSwitch(
                    checked         = settings.displayAnimation,
                    onCheckedChange = { onUpdate { copy(displayAnimation = it) } },
                    colors          = switchColors(accent)
                )
            }
            if (settings.displayAnimation) {
                SettingsDivider()
                SettingsButton(
                    label    = "Choose Car Image",
                    sublabel = if (settings.carImageUri.isNotEmpty()) "Custom car image active" else "Select a PNG with transparency",
                    icon     = Icons.Default.Image,
                    accent   = accent,
                    onClick  = { carImagePicker.launch(arrayOf("image/*")) }
                )
                if (settings.carImageUri.isNotEmpty()) {
                    TextButton(
                        onClick  = { onUpdate { copy(carImageUri = "") } },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("REMOVE CAR IMAGE", color = Color(0xFF993333), fontSize = 9.sp, letterSpacing = 1.sp)
                    }
                }
            }
        }
        }

        // ── GPS & Calibration ───────────────────────────────────────────────
        if (searchQuery.isNotBlank() || selectedCategory == "All" || selectedCategory == "Vehicle & OBD") {
        SettingsSection("GPS & Calibration") {
            var calibrationStatus by remember { mutableStateOf<String?>(null) }
            val coroutineScope = rememberCoroutineScope()
            var isCalibratingCompass by remember { mutableStateOf(false) }
            var compassCountdown by remember { mutableIntStateOf(0) }

            // 1. Reset A-GPS Button
            SettingsButton(
                label    = "Reset A-GPS Assistance Data",
                sublabel = calibrationStatus ?: "Forces cold start to download fresh satellite orbits entirely offline",
                icon     = Icons.Default.MyLocation,
                accent   = accent,
                onClick  = {
                    calibrationStatus = "Clearing A-GPS cache..."
                    val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
                    var success = false
                    try {
                        // "delete_aiding_data" is the command AOSP's GPS provider
                        // actually recognizes (requires ACCESS_LOCATION_EXTRA_COMMANDS)
                        success = lm.sendExtraCommand(android.location.LocationManager.GPS_PROVIDER, "delete_aiding_data", android.os.Bundle())
                        lm.sendExtraCommand(android.location.LocationManager.GPS_PROVIDER, "force_xtra_injection", null)
                        lm.sendExtraCommand(android.location.LocationManager.GPS_PROVIDER, "force_time_injection", null)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    calibrationStatus = if (success) {
                        "Cold start forced — go outdoors for a fresh satellite lock (2–3 min)"
                    } else {
                        "Not supported by this device's GPS driver — no data was cleared"
                    }
                }
            )
            
            SettingsDivider()
            
            // 2. Drive-in-circles magnetometer sweep. Android's sensor stack
            // self-calibrates the magnetometer continuously — the circles feed it
            // diverse readings. The timer guides the sweep; it does not (and
            // cannot) apply offsets itself, so the message must not claim it did.
            SettingsButton(
                label    = "Magnetometer Sweep (Parking Lot)",
                sublabel = if (isCalibratingCompass) {
                    "Sweep active: Drive slowly in two 360° circles... (${compassCountdown}s remaining)"
                } else {
                    "Guided sweep — Android self-calibrates the compass while you circle"
                },
                icon     = Icons.Default.Navigation,
                accent   = if (isCalibratingCompass) Color.Green else accent,
                onClick  = {
                    if (!isCalibratingCompass) {
                        isCalibratingCompass = true
                        compassCountdown = 30
                        coroutineScope.launch {
                            while (compassCountdown > 0) {
                                delay(1000)
                                compassCountdown--
                            }
                            isCalibratingCompass = false
                            calibrationStatus = "Sweep complete — check the compass widget; if heading is still off, use the manual offset below"
                        }
                    }
                }
            )

            SettingsDivider()

            // 4. Manual Compass Heading Offset Slider
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                SettingsRow(
                    label    = "Compass Heading Offset",
                    sublabel = "Manual Alignment: ${if (settings.compassOffset >= 0) "+" else ""}${settings.compassOffset.toInt()}°  — aligns compass with vehicle front",
                    icon     = Icons.Default.Explore
                ) {}
                Slider(
                    value         = settings.compassOffset,
                    onValueChange = { debouncedUpdate { copy(compassOffset = it) } },
                    valueRange    = -180f..180f,
                    steps         = 71, // 5 degree steps: 360 / 5 - 1 = 71 steps
                    colors        = sliderColors(accent),
                    modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
            }
        }
        }

        // ── Profiles ──────────────────────────────────────────────────────────
        if (searchQuery.isNotBlank() || selectedCategory == "All" || selectedCategory == "Launcher") {
        SettingsSection("Profiles") {
            SettingsRow(
                label    = "Active Profile",
                sublabel = "Each driver keeps their own widgets, colors, layout & settings — hardware (Bluetooth OBD, radio app, volume channel) stays shared",
                icon     = Icons.Default.Person
            ) {}
            val scales = LocalUiScales.current
            val ts = scales.ts()
            val us = scales.us()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = (16 * us).dp)
                    .clip(RoundedCornerShape((4 * us).dp))
                    .background(if (isDayMode) Color(0xFFE0E0E0) else Color(0xFF222222))
                    .padding((2 * us).dp)
            ) {
                (0..3).forEach { id ->
                    val active = settings.activeProfile == id
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape((3 * us).dp))
                            .background(if (active) accent else Color.Transparent)
                            .clickable { TapSound.play(); if (!active) onSwitchProfile(id) }
                            .padding(vertical = (8 * us).dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = settings.profileNames.getOrElse(id) { "DRIVER ${id + 1}" },
                                color = if (active) Color.Black else (if (isDayMode) Color(0xFF333333) else Color(0xFFAAAAAA)),
                                fontSize = (10 * ts).sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                letterSpacing = (1 * ts).sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            SettingsDivider()

            val profileNameInput = remember(settings.activeProfile, settings.profileNames) {
                mutableStateOf(settings.profileNames.getOrElse(settings.activeProfile) { "DRIVER ${settings.activeProfile + 1}" })
            }
            SettingsRow(
                label    = "Rename Profile",
                sublabel = "Name the currently active driver profile",
                icon     = Icons.Default.Edit
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value         = profileNameInput.value,
                        onValueChange = { profileNameInput.value = it },
                        placeholder   = { Text("DRIVER ${settings.activeProfile + 1}", color = if (isDayMode) Color(0xFF999999) else Color(0xFF444444), fontSize = 12.sp) },
                        singleLine    = true,
                        textStyle     = LocalTextStyle.current.copy(fontSize = 12.sp, color = if (isDayMode) Color(0xFF111111) else Color.White),
                        colors        = outlinedFieldColors(accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                        modifier      = Modifier.width(140.dp)
                    )
                    if (profileNameInput.value.isNotBlank() &&
                        profileNameInput.value != settings.profileNames.getOrElse(settings.activeProfile) { "DRIVER ${settings.activeProfile + 1}" }) {
                        IconButton(
                            onClick = {
                                TapSound.play()
                                onUpdate {
                                    val updated = profileNames.toMutableList()
                                    updated[activeProfile] = profileNameInput.value.trim()
                                    copy(profileNames = updated.toList())
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Check, "Save", tint = accent, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            SettingsDivider()

            // ── Auto-switch profile by Wi-Fi hotspot ──────────────────────────
            SettingsRow(
                label    = "Auto-Switch by Wi-Fi",
                sublabel = "When the unit connects to a phone's hotspot, the matching profile is activated automatically",
                icon     = Icons.Default.Wifi
            ) {
                TapSwitch(
                    checked         = settings.profileAutoSwitchEnabled,
                    onCheckedChange = { onUpdate { copy(profileAutoSwitchEnabled = it) } },
                    colors          = switchColors(accent)
                )
            }

            if (settings.profileAutoSwitchEnabled) {
                SettingsDivider()
                Text(
                    text          = "Set the hotspot name (SSID) for each profile — leave blank to skip. The active profile switches the moment that Wi-Fi connects.",
                    color         = if (isDayMode) Color(0xFF888888) else Color(0xFF444444),
                    fontSize      = 10.sp,
                    lineHeight    = 14.sp,
                    modifier      = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                (0..3).forEach { id ->
                    val ssidInput = remember(id, settings.profileHotspotSsids) {
                        mutableStateOf(settings.profileHotspotSsids.getOrElse(id) { "" })
                    }
                    SettingsRow(
                        label    = "Hotspot for ${settings.profileNames.getOrElse(id) { "DRIVER ${id + 1}" }}",
                        sublabel = "Profile ${id + 1} activates on this Wi-Fi",
                        icon     = if (id == settings.activeProfile) Icons.Default.Person else Icons.Default.PersonOutline
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(
                                value         = ssidInput.value,
                                onValueChange = { ssidInput.value = it },
                                placeholder   = { Text("No rule", color = if (isDayMode) Color(0xFF999999) else Color(0xFF444444), fontSize = 12.sp) },
                                singleLine    = true,
                                textStyle     = LocalTextStyle.current.copy(fontSize = 12.sp, color = if (isDayMode) Color(0xFF111111) else Color.White),
                                colors        = outlinedFieldColors(accent),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                                modifier      = Modifier.width(180.dp)
                            )
                            if (ssidInput.value.trim() != settings.profileHotspotSsids.getOrElse(id) { "" }) {
                                IconButton(
                                    onClick = {
                                        TapSound.play()
                                        onUpdate {
                                            val updated = profileHotspotSsids.toMutableList()
                                            while (updated.size <= id) updated.add("")
                                            updated[id] = ssidInput.value.trim()
                                            copy(profileHotspotSsids = updated.toList())
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Check, "Save", tint = accent, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // ── Export / Import a single profile ──────────────────────────────
            SettingsDivider()
            var profileSlot by remember { mutableStateOf(settings.activeProfile) }
            var profileMenuOpen by remember { mutableStateOf(false) }
            var profileIoStatus by remember { mutableStateOf<String?>(null) }
            var pendingImportJson by remember { mutableStateOf<String?>(null) }
            val pioScope = rememberCoroutineScope()

            val profileExportLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("application/json")
            ) { uri ->
                if (uri != null) {
                    pioScope.launch {
                        onExportProfile(profileSlot) { json ->
                            if (json.isNullOrBlank()) {
                                profileIoStatus = "Profile ${profileSlot + 1} is empty — nothing to export"
                            } else {
                                val err = SafeExport.write(context, uri, json, "profile_export")
                                profileIoStatus = if (err == null)
                                    "Profile ${profileSlot + 1} exported successfully"
                                else "Export failed: $err"
                            }
                        }
                    }
                }
            }

            val profileImportLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    pioScope.launch {
                        val json = runCatching {
                            context.contentResolver.openInputStream(uri)?.use {
                                it.bufferedReader().use { r -> r.readText() }
                            }
                        }.getOrNull()
                        if (json.isNullOrBlank()) {
                            profileIoStatus = "Could not read the file"
                        } else {
                            pendingImportJson = json
                        }
                    }
                }
            }

            SettingsRow(
                label    = "Export / Import Profile",
                sublabel = "Copy one profile's layout, colors & options to/from a JSON file — hardware stays shared",
                icon     = Icons.Default.SwapHoriz
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((6 * us).dp)
                ) {
                    // Profile slot dropdown (defaults to the active profile)
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape((6 * us).dp))
                                .background(if (isDayMode) Color(0xFFE8E8E8) else Color(0xFF242424))
                                .clickable { TapSound.play(); profileMenuOpen = true }
                                .padding(horizontal = (10 * us).dp, vertical = (7 * us).dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy((4 * us).dp)
                        ) {
                            Text(
                                text = settings.profileNames.getOrElse(profileSlot) { "DRIVER ${profileSlot + 1}" },
                                color = if (isDayMode) Color(0xFF222222) else Color(0xFFDDDDDD),
                                fontSize = (10 * ts).sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = (120 * us).dp)
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                null,
                                tint = if (isDayMode) Color(0xFF555555) else Color(0xFF999999),
                                modifier = Modifier.size((14 * us).dp)
                            )
                        }
                        DropdownMenu(expanded = profileMenuOpen, onDismissRequest = { profileMenuOpen = false }) {
                            (0..3).forEach { id ->
                                DropdownMenuItem(
                                    text = { Text(settings.profileNames.getOrElse(id) { "DRIVER ${id + 1}" }, fontSize = (12 * ts).sp) },
                                    onClick = { TapSound.play(); profileSlot = id; profileMenuOpen = false }
                                )
                            }
                        }
                    }
                    Text(
                        "EXPORT",
                        color = accent,
                        fontSize = (10 * ts).sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (1 * ts).sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape((6 * us).dp))
                            .clickable {
                                TapSound.play()
                                profileIoStatus = null
                                val stamp = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.US).format(java.util.Date())
                                profileExportLauncher.launch("openlauncher_profile_${profileSlot + 1}_$stamp.json")
                            }
                            .padding(horizontal = (10 * us).dp, vertical = (7 * us).dp)
                    )
                    Text(
                        "IMPORT",
                        color = accent,
                        fontSize = (10 * ts).sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (1 * ts).sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape((6 * us).dp))
                            .clickable {
                                TapSound.play()
                                profileIoStatus = null
                                profileImportLauncher.launch(arrayOf("application/json", "*/*"))
                            }
                            .padding(horizontal = (10 * us).dp, vertical = (7 * us).dp)
                    )
                }
            }

            pendingImportJson?.let { json ->
                ConfirmDialog(
                    title        = "Import Profile?",
                    message      = "This will overwrite profile ${profileSlot + 1} (${settings.profileNames.getOrElse(profileSlot) { "DRIVER ${profileSlot + 1}" }}) with the file's layout, colors and options. Hardware/shared settings stay untouched.",
                    confirmLabel = "Import",
                    onConfirm    = {
                        pioScope.launch {
                            onImportProfile(profileSlot, json) { err ->
                                profileIoStatus = if (err == null)
                                    "Profile ${profileSlot + 1} imported successfully"
                                else "Import failed: $err"
                            }
                        }
                        pendingImportJson = null
                    },
                    onDismiss = { pendingImportJson = null }
                )
            }

            if (profileIoStatus != null) {
                SettingsDivider()
                Text(
                    text = profileIoStatus!!,
                    color = accent,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
        }

        // ── Sound on Tap ──────────────────────────────────────────────────────
        if (searchQuery.isNotBlank() || selectedCategory == "All" || selectedCategory == "Launcher") {
        SettingsSection(title = "Sound on Tap", onHeaderTap = { if (!gesturesRevealed) { gesturesTaps++; if (gesturesTaps >= 5) { gesturesRevealed = true; gesturesTaps = 0; TapSound.play() } } }) {
            SettingsRow(
                label    = "Tap Sound",
                sublabel = "Play a short sound when tapping home-screen widgets",
                icon     = Icons.Default.VolumeUp
            ) {
                TapSwitch(
                    checked         = settings.tapSoundEnabled,
                    onCheckedChange = { onUpdate { copy(tapSoundEnabled = it) } },
                    colors          = switchColors(accent)
                )
            }

            if (settings.tapSoundEnabled) {
                SettingsDivider()
                SettingsRow(
                    label    = "Sound",
                    sublabel = "Tick (short click) or a light beep",
                    icon     = Icons.Default.GraphicEq
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isDayMode) Color(0xFFE0E0E0) else Color(0xFF222222))
                            .padding(2.dp)
                    ) {
                        listOf(
                            "TICK" to TapSoundType.TICK,
                            "BEEP" to TapSoundType.BEEP
                        ).forEach { (label, st) ->
                            val selected = settings.tapSoundType == st
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (selected) accent else Color.Transparent)
                                    .clickable { onUpdate { copy(tapSoundType = st) } }
                                    .padding(horizontal = 12.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (selected) Color.Black else (if (isDayMode) Color(0xFF333333) else Color(0xFFAAAAAA)),
                                    fontSize = 10.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            }
        }
        }

        // ── Gestures ──────────────────────────────────────────────────────────
        if (gesturesRevealed && (searchQuery.isNotBlank() || selectedCategory == "All" || selectedCategory == "Launcher")) {
        SettingsSection("Gestures") {
            val twoFingerLabel = if (settings.gestureTwoFingerPackage.isNotEmpty())
                apps.find { it.packageName == settings.gestureTwoFingerPackage }?.appName ?: settings.gestureTwoFingerPackage
            else "Disabled"
            SettingsRow(
                label    = "Two-Finger Tap",
                sublabel = if (settings.gestureTwoFingerPackage.isNotEmpty()) "Launches $twoFingerLabel"
                           else "Tap the home screen with two fingers to launch a chosen app",
                icon     = Icons.Default.TouchApp
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { pendingAppPick = 1 }) {
                        Text(if (settings.gestureTwoFingerPackage.isEmpty()) "Choose" else "Change", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    if (settings.gestureTwoFingerPackage.isNotEmpty()) {
                        TextButton(onClick = { onUpdate { copy(gestureTwoFingerPackage = "") } }) {
                            Text("Clear", color = if (isDayMode) Color(0xFF666666) else Color(0xFFAAAAAA), fontSize = 11.sp)
                        }
                    }
                }
            }

            SettingsDivider()

            val threeFingerLabel = if (settings.gestureThreeFingerPackage.isNotEmpty())
                apps.find { it.packageName == settings.gestureThreeFingerPackage }?.appName ?: settings.gestureThreeFingerPackage
            else "Disabled"
            SettingsRow(
                label    = "Three-Finger Tap",
                sublabel = if (settings.gestureThreeFingerPackage.isNotEmpty()) "Launches $threeFingerLabel"
                           else "Tap the home screen with three fingers to launch a chosen app",
                icon     = Icons.Default.TouchApp
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { pendingAppPick = 2 }) {
                        Text(if (settings.gestureThreeFingerPackage.isEmpty()) "Choose" else "Change", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    if (settings.gestureThreeFingerPackage.isNotEmpty()) {
                        TextButton(onClick = { onUpdate { copy(gestureThreeFingerPackage = "") } }) {
                            Text("Clear", color = if (isDayMode) Color(0xFF666666) else Color(0xFFAAAAAA), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
        }

        // ── Telegram ─────────────────────────────────────────────────────────
        if (searchQuery.isNotBlank() || selectedCategory == "All" || selectedCategory == "Launcher") {
        SettingsSection("Telegram Messages") {
            SettingsButton(
                label    = "Driver Inbox",
                sublabel = "Remote messages from a Telegram bot — setup, commands, quick replies & status",
                icon     = Icons.Default.Forum,
                accent   = accent,
                onClick  = onOpenTelegram
            )
        }
        }

        // ── Startup ───────────────────────────────────────────────────────────
        if (searchQuery.isNotBlank() || selectedCategory == "All" || selectedCategory == "Launcher") {
        SettingsSection("Startup") {
            SettingsRow(
                label    = "Auto-Launch App",
                sublabel = "Open a chosen app automatically when the launcher starts",
                icon     = Icons.Default.PlayArrow
            ) {
                TapSwitch(
                    checked         = settings.autoLaunchEnabled,
                    onCheckedChange = { onUpdate { copy(autoLaunchEnabled = it) } },
                    colors          = switchColors(accent)
                )
            }

            if (settings.autoLaunchEnabled) {
                SettingsDivider()
                SettingsRow(
                    label    = "App",
                    sublabel = if (settings.autoLaunchPackage.isNotEmpty())
                        apps.find { it.packageName == settings.autoLaunchPackage }?.appName ?: settings.autoLaunchPackage
                    else "No app selected",
                    icon     = Icons.Default.Apps
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { pendingAppPick = 0 }) {
                            Text(if (settings.autoLaunchPackage.isEmpty()) "Choose" else "Change", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        if (settings.autoLaunchPackage.isNotEmpty()) {
                            TextButton(onClick = { onUpdate { copy(autoLaunchPackage = "") } }) {
                                Text("Clear", color = if (isDayMode) Color(0xFF666666) else Color(0xFFAAAAAA), fontSize = 11.sp)
                            }
                        }
                    }
                }
                SettingsDivider()
                SettingsRow(
                    label    = "Delay",
                    sublabel = "${settings.autoLaunchDelaySec}s after launch",
                    icon     = Icons.Default.Timer
                ) {}
                Slider(
                    value         = settings.autoLaunchDelaySec.toFloat(),
                    onValueChange = { v -> debouncedUpdate { copy(autoLaunchDelaySec = v.toInt().coerceIn(1, 30)) } },
                    valueRange    = 1f..30f,
                    steps         = 28,
                    colors        = sliderColors(accent),
                    modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
            }
        }
        }

        // ── Recovery ──────────────────────────────────────────────────────────
        if (searchQuery.isNotBlank() || selectedCategory == "All" || selectedCategory == "Launcher") {
        SettingsSection("Recovery") {
            SettingsButton(
                label    = "Restart Launcher",
                sublabel = "Force-restart the launcher to clear any runtime issue (last line of defence)",
                icon     = Icons.Default.RestartAlt,
                accent   = Color(0xFF993333),
                onClick  = {
                    runCatching {
                        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        }
                        if (intent != null) {
                            val pending = android.app.PendingIntent.getActivity(
                                context, 1234, intent,
                                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_ONE_SHOT
                            )
                            val am = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
                            am.set(android.app.AlarmManager.RTC, System.currentTimeMillis() + 250, pending)
                        }
                    }
                    android.os.Process.killProcess(android.os.Process.myPid())
                    kotlin.system.exitProcess(0)
                }
            )
        }
        }

        // ── Backup & Restore ──────────────────────────────────────────────────
        if (searchQuery.isNotBlank() || selectedCategory == "All" || selectedCategory == "Launcher") {
        SettingsSection("Backup & Restore") {
            var backupStatusMessage by remember { mutableStateOf<String?>(null) }
            var pendingRestoredSettings by remember { mutableStateOf<AppSettings?>(null) }
            val coroutineScope = rememberCoroutineScope()

            val exportLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("application/json")
            ) { uri ->
                if (uri != null) {
                    coroutineScope.launch {
                        val json = SettingsBackup.encode(settings)
                        val err = SafeExport.write(context, uri, json, "settings_backup")
                        backupStatusMessage = if (err == null) "Settings exported successfully" else "Export failed: $err"
                    }
                }
            }

            val importLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    coroutineScope.launch {
                        try {
                            val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                inputStream.bufferedReader().use { it.readText() }
                            }
                            if (!json.isNullOrBlank()) {
                                pendingRestoredSettings = SettingsBackup.decode(json)
                            }
                        } catch (e: Exception) {
                            backupStatusMessage = "Invalid backup file: ${e.message}"
                        }
                    }
                }
            }

            if (pendingRestoredSettings != null) {
                ConfirmDialog(
                    title = "Restore Settings?",
                    message = "This will replace your current dashboard layout, appearance, shortcuts, and vehicle preferences with the backup. Reassign local wallpapers/custom sounds if needed. Proceed?",
                    confirmLabel = "Restore",
                    onConfirm = {
                        pendingRestoredSettings?.let { restored ->
                            onUpdate { restored }
                            backupStatusMessage = "Settings restored successfully"
                        }
                        pendingRestoredSettings = null
                    },
                    onDismiss = { pendingRestoredSettings = null }
                )
            }

            SettingsButton(
                label    = "Export Settings (JSON)",
                sublabel = "Save current layout, colors, typography, shortcuts & preferences to a file",
                icon     = Icons.Default.FileDownload,
                accent   = accent,
                onClick  = {
                    val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.US).format(java.util.Date())
                    exportLauncher.launch("openlauncher_backup_$timestamp.json")
                }
            )

            SettingsDivider()

            SettingsButton(
                label    = "Restore Settings (JSON)",
                sublabel = "Import and apply a previously exported Open Launcher backup file",
                icon     = Icons.Default.FileUpload,
                accent   = accent,
                onClick  = {
                    importLauncher.launch(arrayOf("application/json", "*/*"))
                }
            )

            if (backupStatusMessage != null) {
                SettingsDivider()
                Text(
                    text = backupStatusMessage!!,
                    color = accent,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
        }

        // ── Floating Panel ────────────────────────────────────────────────────
        if (searchQuery.isNotBlank() || selectedCategory == "All" || selectedCategory == "Dashboard") {
        SettingsSection("Floating Panel") {
            SettingsRow(
                label    = "Floating Quick Controls",
                sublabel = "A draggable panel of buttons that stays on top of other apps",
                icon     = Icons.Default.Dashboard
            ) {
                TapSwitch(
                    checked         = settings.floatingPanelEnabled,
                    onCheckedChange = { onUpdate { copy(floatingPanelEnabled = it) } },
                    colors          = switchColors(accent)
                )
            }

            if (settings.floatingPanelEnabled) {
                if (!Settings.canDrawOverlays(context)) {
                    SettingsDivider()
                    SettingsButton(
                        label    = "Overlay Permission Required",
                        sublabel = "Grant 'Draw over other apps' in Permissions to show the panel over other apps",
                        icon     = Icons.Default.Warning,
                        accent   = Color(0xFF993333),
                        onClick  = {
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }
                    )
                }

                SettingsDivider()

                SettingsRow(
                    label    = "Panel Size",
                    sublabel = "${"%.0f".format(settings.floatingPanelScale * 100)}% — scales buttons and spacing",
                    icon     = Icons.Default.AspectRatio
                ) {}
                Slider(
                    value         = settings.floatingPanelScale,
                    onValueChange = { debouncedUpdate { copy(floatingPanelScale = it) } },
                    valueRange    = 0.7f..1.6f,
                    steps         = 8,
                    colors        = sliderColors(accent),
                    modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )

                SettingsDivider()

                SettingsRow(
                    label    = "Panel Opacity",
                    sublabel = "${"%.0f".format(settings.floatingPanelOpacity * 100)}% background",
                    icon     = Icons.Default.Opacity
                ) {}
                Slider(
                    value         = settings.floatingPanelOpacity,
                    onValueChange = { debouncedUpdate { copy(floatingPanelOpacity = it) } },
                    valueRange    = 0.2f..0.9f,
                    steps         = 6,
                    colors        = sliderColors(accent),
                    modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )

                SettingsDivider()

                SettingsRow(
                    label    = "Vertical Layout",
                    sublabel = "Stack the buttons vertically instead of side-by-side",
                    icon     = Icons.Default.SwapVert
                ) {
                    TapSwitch(
                        checked         = settings.floatingPanelVertical,
                        onCheckedChange = { onUpdate { copy(floatingPanelVertical = it) } },
                        colors          = switchColors(accent)
                    )
                }

                SettingsDivider()

                SettingsRow(
                    label    = "Buttons",
                    sublabel = "Tap a button to change its action (up to 4)",
                    icon     = Icons.Default.TouchApp
                ) {}

                val slotLabelColor = if (isDayMode) Color(0xFF111111) else Color(0xFFDDDDDD)
                val slotSubColor   = LocalHelpTextColor.current ?: if (isDayMode) Color(0xFF888888) else Color(0xFF444444)
                val slotBg         = if (isDayMode) Color(0xFFF0F0F0) else Color(0xFF161616)

                settings.floatingPanelActions.forEachIndexed { index, action ->
                    val appName = if (action.type == FloatingActionType.APP && action.packageName.isNotBlank()) {
                        apps.find { it.packageName == action.packageName }?.appName ?: action.packageName
                    } else null

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(slotBg)
                            .clickable { floatingSlotEditor = index }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(floatingActionIcon(action.type), null, tint = accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(floatingActionLabel(action.type), color = slotLabelColor, fontSize = 12.sp)
                            if (appName != null) {
                                Text(appName, color = slotSubColor, fontSize = 10.sp, maxLines = 1, softWrap = false)
                            }
                        }
                        IconButton(
                            onClick = { onUpdate { copy(floatingPanelActions = floatingPanelActions.filterIndexed { i, _ -> i != index }) } },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, "Remove", tint = Color(0xFFCC5555), modifier = Modifier.size(15.dp))
                        }
                    }
                }

                if (settings.floatingPanelActions.size < 4) {
                    TextButton(
                        onClick = { onUpdate { copy(floatingPanelActions = floatingPanelActions + FloatingPanelAction(FloatingActionType.HOME)) } }
                    ) {
                        Text(
                            "+ ADD BUTTON (${settings.floatingPanelActions.size}/4)",
                            color = accent,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        }

        if (searchQuery.isNotBlank() || selectedCategory == "All" || selectedCategory == "Launcher") {
        // ── Permissions ──────────────────────────────────────────────────────
        SettingsSection("Permissions") {
            val isMediaConnected by com.openlauncher.app.service.MediaListenerService.isConnected.collectAsState()

            // Bumped on ON_RESUME so statuses refresh when the user returns from
            // system settings (recomposition alone doesn't re-run these checks)
            var permissionRefresh by remember { mutableIntStateOf(0) }
            val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) permissionRefresh++
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            val hasLocation = remember(permissionRefresh) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
            val hasMic = remember(permissionRefresh) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.RECORD_AUDIO
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
            val hasUsageAccess = remember(permissionRefresh) {
                runCatching {
                    val appOps = context.getSystemService(android.app.AppOpsManager::class.java)
                    val mode = if (android.os.Build.VERSION.SDK_INT >= 29) {
                        appOps.unsafeCheckOpNoThrow(
                            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                            android.os.Process.myUid(), context.packageName
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        appOps.checkOpNoThrow(
                            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                            android.os.Process.myUid(), context.packageName
                        )
                    }
                    mode == android.app.AppOpsManager.MODE_ALLOWED
                }.getOrDefault(false)
            }
            val hasAccessibility = remember(permissionRefresh) {
                val enabledServices = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                ) ?: ""
                enabledServices.contains("${context.packageName}/") ||
                    enabledServices.contains(context.packageName)
            }
            val isDefaultLauncher = remember(permissionRefresh) {
                val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                context.packageManager.resolveActivity(
                    home, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
                )?.activityInfo?.packageName == context.packageName
            }

            val homeRoleLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { permissionRefresh++ }

            val locationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissionRefresh++ }

            val recordAudioLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { permissionRefresh++ }

            SettingsButton(
                label    = "Set as Default Launcher",
                sublabel = if (isDefaultLauncher) "Active — the unit boots into Open Launcher and the Home key returns here"
                           else "Required so the unit boots into Open Launcher and the Home key returns here",
                icon     = Icons.Default.Home,
                accent   = if (isDefaultLauncher) accent else Color(0xFF993333),
                onClick  = {
                    // Preferred: the system home-role dialog (API 29+). Vendor ROMs
                    // sometimes ship without it, so fall through to the home-settings
                    // screen, then the default-apps screen.
                    var launched = false
                    if (android.os.Build.VERSION.SDK_INT >= 29) {
                        val rm = context.getSystemService(android.app.role.RoleManager::class.java)
                        if (rm != null && rm.isRoleAvailable(android.app.role.RoleManager.ROLE_HOME) &&
                            !rm.isRoleHeld(android.app.role.RoleManager.ROLE_HOME)
                        ) {
                            launched = runCatching {
                                homeRoleLauncher.launch(rm.createRequestRoleIntent(android.app.role.RoleManager.ROLE_HOME))
                            }.isSuccess
                        }
                    }
                    if (!launched) {
                        launched = runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }.isSuccess
                    }
                    if (!launched) {
                        runCatching {
                            context.startActivity(
                                Intent("android.settings.MANAGE_DEFAULT_APPS_SETTINGS")
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                }
            )
            SettingsDivider()
            SettingsButton(
                label    = "Notification Access",
                sublabel = if (isMediaConnected) "Granted — the Now Playing widget & radio mirroring can read and control media"
                           else "Required for the Now Playing widget and radio mirroring to read/control media",
                icon     = if (isMediaConnected) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                accent   = if (isMediaConnected) accent else Color(0xFF993333),
                onClick  = {
                    context.startActivity(
                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            )
            SettingsDivider()
            SettingsButton(
                label    = "Location Access",
                sublabel = if (hasLocation) "Granted — GPS speedometer, compass, altimeter & weather are live" else "Required for the GPS speedometer, compass, altimeter & weather",
                icon     = if (hasLocation) Icons.Default.LocationOn else Icons.Default.LocationOff,
                accent   = if (hasLocation) accent else Color(0xFF993333),
                onClick  = {
                    if (!hasLocation) {
                        // Ask in-app first — previously the only grant path was the
                        // onboarding flow; skipping it left GPS features dead forever
                        locationPermissionLauncher.launch(arrayOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        ))
                    } else {
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:${context.packageName}")
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                }
            )
            SettingsDivider()
            SettingsButton(
                label    = "Draw Over Other Apps",
                sublabel = if (Settings.canDrawOverlays(context)) "Granted — the Floating Panel can show over other apps" else "Required for the Floating Panel quick controls",
                icon     = Icons.Default.Layers,
                accent   = if (Settings.canDrawOverlays(context)) accent else Color(0xFF993333),
                onClick  = {
                    if (android.os.Build.VERSION.SDK_INT >= 23) {
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                }
            )
            SettingsDivider()
            SettingsButton(
                label    = "Modify System Settings",
                sublabel = if (Settings.System.canWrite(context)) "Granted — the volume widget can set head-unit volume" else "Required for the volume widget to change head-unit volume (NWD/Qualcomm)",
                icon     = Icons.Default.Tune,
                accent   = if (Settings.System.canWrite(context)) accent else Color(0xFF993333),
                onClick  = {
                    runCatching {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                Uri.parse("package:${context.packageName}")
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
            )
            SettingsDivider()
            SettingsButton(
                label    = "Microphone (Spectrum)",
                sublabel = if (hasMic) "Granted — the sound visualizer can analyze playing audio"
                           else "Required for the sound visualizer to analyze playing audio",
                icon     = if (hasMic) Icons.Default.Mic else Icons.Default.MicOff,
                accent   = if (hasMic) accent else Color(0xFF993333),
                onClick  = {
                    if (!hasMic) {
                        recordAudioLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                    } else {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                }
            )
            SettingsDivider()
            SettingsButton(
                label    = "Usage Access",
                sublabel = if (hasUsageAccess) "Granted — the floating panel auto-hides over Play Store & permission dialogs"
                           else "Required so the floating panel can auto-hide over sensitive screens",
                icon     = Icons.Default.Visibility,
                accent   = if (hasUsageAccess) accent else Color(0xFF993333),
                onClick  = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
            )
            SettingsDivider()
            SettingsButton(
                label    = "Accessibility (Back Key)",
                sublabel = if (hasAccessibility) "Granted — the floating panel Back button works"
                           else "Required for the floating panel Back button to send the Back key",
                icon     = if (hasAccessibility) Icons.Default.Accessibility else Icons.Default.AccessibilityNew,
                accent   = if (hasAccessibility) accent else Color(0xFF993333),
                onClick  = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
            )
        }
        }

        // ── Maintenance ──────────────────────────────────────────────────────
        if (searchQuery.isNotBlank() || selectedCategory == "All" || selectedCategory == "Launcher") {
        SettingsSection("Maintenance") {
            SettingsButton(
                label    = "System Status",
                sublabel = "Live status of permissions, sensors, network & screen",
                icon     = Icons.Default.MonitorHeart,
                accent   = accent,
                onClick  = onOpenDiagnostics
            )
            SettingsDivider()
            SettingsButton(
                label    = "Volume Calibration",
                sublabel = "Find which volume channel your head unit responds to (mute-probe wizard)",
                icon     = Icons.Default.Tune,
                accent   = accent,
                onClick  = onOpenVolumeCalibration
            )
            SettingsDivider()
            SettingsRow(
                label    = "Log Collector",
                sublabel = if (settings.debugLogEnabled)
                    "Enabled — recording media / radio / volume events to a log file"
                else
                    "Disabled — no logging overhead; enable before reproducing an issue",
                icon     = Icons.Default.BugReport
            ) {
                TapSwitch(
                    checked         = settings.debugLogEnabled,
                    onCheckedChange = { onUpdate { copy(debugLogEnabled = it) } },
                    colors          = switchColors(accent)
                )
            }
            SettingsDivider()
            val logScope = rememberCoroutineScope()
            val logExportLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("text/plain")
            ) { uri ->
                if (uri != null) {
                    logScope.launch {
                        val text = FileLog.logFile()?.takeIf { it.exists() }?.readText()
                            ?: "No log data recorded yet.\n"
                        SafeExport.write(context, uri, text, "debug_log")
                    }
                }
            }
            SettingsButton(
                label    = "Export Debug Log",
                sublabel = if (settings.debugLogEnabled) "Save the collected log to a file" else "Enable Log Collector first, then export here",
                icon     = Icons.Default.BugReport,
                accent   = accent,
                onClick  = {
                    val ts = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
                    logExportLauncher.launch("openlauncher_log_$ts.txt")
                }
            )
            SettingsDivider()
            SettingsButton(
                label    = "Run Setup Wizard",
                sublabel = "Show the first-run onboarding & permission walkthrough again",
                icon     = Icons.Default.AutoAwesome,
                accent   = accent,
                onClick  = { onUpdate { copy(onboardingCompleted = false) } }
            )
            SettingsDivider()
            Spacer(Modifier.height(8.dp))
            val resetButtonBg = if (isDayMode) Color(0xFFFFECEC) else Color(0xFF240A0A)
            val resetBorderColor = if (isDayMode) Color(0xFFFFC9C9) else Color(0xFF5A1A1A)
            val resetTextColor = if (isDayMode) Color(0xFFD32F2F) else Color(0xFFFF6B6B)
            Button(
                onClick  = { showResetDialog = true },
                shape    = RoundedCornerShape(8.dp),
                border   = androidx.compose.foundation.BorderStroke(1.dp, resetBorderColor),
                colors   = ButtonDefaults.buttonColors(containerColor = resetButtonBg),
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Icon(Icons.Default.RestartAlt, null, tint = resetTextColor, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Reset to Defaults", color = resetTextColor, fontSize = 13.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
        }
        }

        // ── About ────────────────────────────────────────────────────────────
        if (searchQuery.isNotBlank() || selectedCategory == "All" || selectedCategory == "Launcher") {
        SettingsSection("About") {
            SettingsButton(
                label    = "Visit github (fork)",
                sublabel = "https://github.com/limbo666/openlauncher_custom",
                icon     = Icons.Default.SystemUpdate,
                accent   = accent,
                onClick  = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/limbo666/openlauncher_custom"))
                    context.startActivity(intent)
                }
            )
        }
        }

        Spacer(Modifier.height(32.dp))

        val appVersionName = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "0.0.8"

        Text(
            text          = "v$appVersionName • Original by David Lam • Improved by Nikos Georgousis • 2026",
            color         = if (isDayMode) Color(0xFFAAAAAA) else Color(0xFF666666),
            fontSize      = 10.sp,
            letterSpacing = 1.sp,
            modifier      = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 16.dp)
                // Hidden easter egg: long-press toggles the retro 80s LCD theme.
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            TapSound.play()
                            onUpdate { copy(retroThemeEnabled = !retroThemeEnabled) }
                        }
                    )
                }
        )
    }

    // ── Sticky "current section" banner ────────────────────────────────────────
    // Shows which subcategory you're in while scrolling, with the section's
    // colour dot — pairs with the coloured left-edge gutter on each section.
    val bannerName by remember(sectionTracker) {
        derivedStateOf {
            val cut = scrollState.value + 96f
            sectionTracker.positions.entries
                .filter { it.value <= cut }
                .maxByOrNull { it.value }
                ?.key
        }
    }
    val currentBanner = bannerName
    if (currentBanner != null && scrollState.value > 40) {
        val bannerColor = sectionColorFor(currentBanner)
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = 24.dp, vertical = 10.dp)
                .zIndex(600f)
                .clip(RoundedCornerShape(20.dp))
                .background(screenBg.copy(alpha = 0.94f))
                .border(1.dp, bannerColor.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(bannerColor)
            )
            Text(
                text          = currentBanner.uppercase(),
                color         = if (isDayMode) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground,
                fontSize      = 11.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                maxLines      = 1
            )
        }
    }

    // ── Floating "back to top" chip ───────────────────────────────────────────
    // Appears once the category tiles / search have scrolled out of view, shows
    // where you are (category, or the live filter while searching), and taps
    // scroll back to the top.
    val showBackToTop by remember { derivedStateOf { scrollState.value > 420 } }
    AnimatedVisibility(
        visible = showBackToTop,
        enter = fadeIn(tween(160)) + scaleIn(initialScale = 0.85f, animationSpec = tween(160)),
        exit  = fadeOut(tween(120)) + scaleOut(targetScale = 0.85f, animationSpec = tween(120)),
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 18.dp, bottom = 16.dp)
            .zIndex(500f)
    ) {
        val chipScope = rememberCoroutineScope()
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(accent.copy(alpha = if (isDayMode) 0.16f else 0.24f))
                .border(1.dp, accent.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    TapSound.play()
                    chipScope.launch { scrollState.animateScrollTo(0) }
                }
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.ArrowUpward, null, tint = accent, modifier = Modifier.size(14.dp))
            Text(
                text = if (searchQuery.isNotBlank()) "Filter: $searchQuery" else selectedCategory.uppercase(),
                color = accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 220.dp)
            )
        }
    }
    } // end CompositionLocalProvider
    } // end Box

    // ── Dialogs ──────────────────────────────────────────────────────────────
    if (showResetDialog) {
        ConfirmDialog(
            title        = "Reset Settings",
            message      = "Are you sure you want to reset all settings to default? This cannot be undone.",
            confirmLabel = "Reset",
            onConfirm    = { onReset(); showResetDialog = false },
            onDismiss    = { showResetDialog = false }
        )
    }

    if (showAccentPicker) {
        ColorPickerDialog(
            title           = "Accent Color",
            initialColor    = settings.effectiveAccent(),
            onColorSelected = { c -> onUpdate { copy(accentColor = c.toArgb()) } },
            onDismiss       = { showAccentPicker = false }
        )
    }

    if (showBgPicker) {
        ColorPickerDialog(
            title           = "Background Color",
            initialColor    = Color(settings.backgroundColor),
            onColorSelected = { c -> 
                onUpdate { 
                    copy(
                        backgroundColor = c.toArgb(),
                        useCustomBackgroundColor = true
                    ) 
                } 
            },
            onDismiss       = { showBgPicker = false }
        )
    }

    if (showGradientEndPicker) {
        ColorPickerDialog(
            title           = "Gradient End Color",
            initialColor    = Color(settings.gradientEndColor),
            onColorSelected = { c -> 
                onUpdate { 
                    copy(
                        gradientEndColor = c.toArgb(),
                        useCustomBackgroundColor = true
                    ) 
                } 
            },
            onDismiss       = { showGradientEndPicker = false }
        )
    }

    if (showFontColorPicker) {
        ColorPickerDialog(
            title           = "Font Color",
            initialColor    = Color(settings.fontColor),
            onColorSelected = { c -> onUpdate { copy(fontColor = c.toArgb()) } },
            onDismiss       = { showFontColorPicker = false }
        )
    }

    if (showHelpTextPicker) {
        ColorPickerDialog(
            title           = "Help Text Color",
            initialColor    = settings.helpTextColor?.let { Color(it) } ?: Color(0xFF888888),
            onColorSelected = { c -> onUpdate { copy(helpTextColor = c.toArgb()) } },
            onDismiss       = { showHelpTextPicker = false }
        )
    }

    if (showTileDarkPicker) {
        ColorPickerDialog(
            title           = "Tile Color (Dark)",
            initialColor    = Color(settings.tileColorDark),
            onColorSelected = { c -> onUpdate { copy(tileColorDark = c.toArgb()) } },
            onDismiss       = { showTileDarkPicker = false }
        )
    }

    if (showTileLightPicker) {
        ColorPickerDialog(
            title           = "Tile Color (Light)",
            initialColor    = Color(settings.tileColorLight),
            onColorSelected = { c -> onUpdate { copy(tileColorLight = c.toArgb()) } },
            onDismiss       = { showTileLightPicker = false }
        )
    }

    // ── Font picker (live preview grid) ───────────────────────────────────────
    if (showFontPicker) {
        FontPickerDialog(
            current   = settings.appFont,
            accent    = accent,
            isDayMode = isDayMode,
            onSelect  = { font ->
                onUpdate { copy(appFont = font) }
                showFontPicker = false
            },
            onDismiss = { showFontPicker = false }
        )
    }

    floatingSlotEditor?.let { slot ->
        FloatingSlotDialog(
            slotIndex = slot,
            current   = settings.floatingPanelActions.getOrNull(slot),
            apps      = apps,
            accent    = accent,
            isDayMode = isDayMode,
            selectedAppPackages = settings.selectedAppPackages,
            toolsAppPackages    = settings.toolsAppPackages,
            onSelectType = { type ->
                onUpdate {
                    copy(floatingPanelActions = floatingPanelActions.toMutableList().also { list ->
                        if (slot in list.indices) list[slot] = list[slot].copy(type = type, packageName = if (type == FloatingActionType.APP) list[slot].packageName else "")
                    })
                }
                floatingSlotEditor = null
            },
            onSelectApp = { pkg ->
                onUpdate {
                    copy(floatingPanelActions = floatingPanelActions.toMutableList().also { list ->
                        if (slot in list.indices) list[slot] = list[slot].copy(type = FloatingActionType.APP, packageName = pkg)
                    })
                }
                floatingSlotEditor = null
            },
            onDismiss = { floatingSlotEditor = null }
        )
    }

    pendingAppPick?.let { target ->
        AppPickerDialog(
            title          = when (target) {
                0 -> "AUTO-LAUNCH APP"
                1 -> "TWO-FINGER TAP"
                else -> "THREE-FINGER TAP"
            },
            currentPackage = when (target) {
                0 -> settings.autoLaunchPackage
                1 -> settings.gestureTwoFingerPackage
                else -> settings.gestureThreeFingerPackage
            },
            apps           = apps,
            accent         = accent,
            isDayMode      = isDayMode,
            selectedAppPackages = settings.selectedAppPackages,
            toolsAppPackages    = settings.toolsAppPackages,
            onSelect       = { pkg ->
                onUpdate {
                    when (target) {
                        0 -> copy(autoLaunchPackage = pkg)
                        1 -> copy(gestureTwoFingerPackage = pkg)
                        else -> copy(gestureThreeFingerPackage = pkg)
                    }
                }
                pendingAppPick = null
            },
            onClear        = {
                onUpdate {
                    when (target) {
                        0 -> copy(autoLaunchPackage = "")
                        1 -> copy(gestureTwoFingerPackage = "")
                        else -> copy(gestureThreeFingerPackage = "")
                    }
                }
                pendingAppPick = null
            },
            onDismiss = { pendingAppPick = null }
        )
    }
}

// ── Telegram remote-messages settings page ──────────────────────────────────

@Composable
fun TelegramSettingsScreen(
    settings: AppSettings,
    accent: Color,
    onUpdate: (AppSettings.() -> AppSettings) -> Unit,
    onBack: () -> Unit,
    isOnline: Boolean
) {
    val isDayMode  = LocalDayMode.current
    val keyboard   = LocalSoftwareKeyboardController.current
    val screenBg   = MaterialTheme.colorScheme.background
    val titleColor = if (isDayMode) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground
    val subColor   = LocalHelpTextColor.current ?: if (isDayMode) Color(0xFF888888) else Color(0xFF666666)
    val chipContainer = accentOnSurface(accent, screenBg)
    val chipLabel     = contentColorOn(chipContainer)
    val scope      = rememberCoroutineScope()
    var testResult by remember { mutableStateOf<String?>(null) }
    // Local cursor-stable buffers — the settings value round-trips through DataStore
    // asynchronously, so binding the field straight to it makes the cursor jump.
    var tokenText    by remember { mutableStateOf(settings.telegramBotToken) }
    var sendersText  by remember { mutableStateOf(settings.telegramAllowedSenders) }
    val connected  = TelegramBot.connected
    val logRev     = TelegramBot.logRevision   // read so the log window recomposes on new entries
    val chips      = listOf(1, 2, 3, 6, 12)

    BackHandler { onBack() }

    Box(modifier = Modifier.fillMaxSize().background(screenBg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = titleColor)
                }
                Text(
                    text          = "TELEGRAM MESSAGES",
                    style         = MaterialTheme.typography.titleLarge,
                    color         = titleColor,
                    letterSpacing = 3.sp,
                    fontSize      = 14.sp
                )
            }

            SettingsSection("Setup") {
                Text(
                    "Create a bot with @BotFather in Telegram (one message). Paste its token below, add your Telegram user ID as an allowed sender, then message the bot — the driver sees it here.",
                    style = MaterialTheme.typography.labelSmall,
                    color = subColor,
                    fontSize = 11.sp
                )
                SettingsDivider()
                SettingsRow(
                    label    = "Master Switch",
                    sublabel = if (connected) "Connected — listening for messages" else "Enable polling for the bot",
                    icon     = Icons.Default.PowerSettingsNew
                ) {
                    TapSwitch(
                        checked         = settings.telegramEnabled,
                        onCheckedChange = { onUpdate { copy(telegramEnabled = it) } },
                        colors          = switchColors(accent)
                    )
                }
                SettingsDivider()
                SettingsRow(
                    label    = "Bot Token",
                    sublabel = "From @BotFather — e.g. 123456789:AAH1q2w3e4r5t6y7u8i9o0p (never share it)",
                    icon     = Icons.Default.Key
                ) {
                    OutlinedTextField(
                        value        = tokenText,
                        onValueChange = { v -> tokenText = v; onUpdate { copy(telegramBotToken = v.trim()) } },
                        singleLine   = true,
                        textStyle    = MaterialTheme.typography.bodySmall.copy(color = titleColor),
                        colors       = outlinedFieldColors(accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                        modifier     = Modifier.weight(1f)
                    )
                }
                SettingsDivider()
                SettingsRow(
                    label    = "Allowed Senders",
                    sublabel = "Comma-separated IDs or @usernames, e.g. 123456789, @wife, @son — or '@all' for anyone (insecure)",
                    icon     = Icons.Default.Security
                ) {
                    OutlinedTextField(
                        value        = sendersText,
                        onValueChange = { v -> sendersText = v; onUpdate { copy(telegramAllowedSenders = v.trim()) } },
                        singleLine   = true,
                        textStyle    = MaterialTheme.typography.bodySmall.copy(color = titleColor),
                        colors       = outlinedFieldColors(accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                        modifier     = Modifier.weight(1f)
                    )
                }
            }

            SettingsSection("Behaviour") {
                SettingsRow(
                    label    = "Look-back Window",
                    sublabel = "Messages older than this are ignored after being offline",
                    icon     = Icons.Default.History
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        chips.forEach { h ->
                            TapFilterChip(
                                selected = settings.telegramLookbackHours == h,
                                onClick  = { onUpdate { copy(telegramLookbackHours = h) } },
                                label    = { Text("${h}h", fontSize = 10.sp) },
                                colors   = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = chipContainer,
                                    selectedLabelColor     = chipLabel
                                )
                            )
                        }
                    }
                }
                SettingsDivider()
                SettingsRow(
                    label    = "Auto Dismiss",
                    sublabel = "Hide normal messages automatically",
                    icon     = Icons.Default.Timer
                ) {
                    TapSwitch(
                        checked         = settings.telegramAutoDismiss,
                        onCheckedChange = { onUpdate { copy(telegramAutoDismiss = it) } },
                        colors          = switchColors(accent)
                    )
                }
                SettingsDivider()
                SettingsRow(
                    label    = "Dismiss After",
                    sublabel = "${settings.telegramDismissSeconds} s",
                    icon     = Icons.Default.Schedule
                ) {
                    Slider(
                        value       = settings.telegramDismissSeconds.toFloat(),
                        onValueChange = { onUpdate { copy(telegramDismissSeconds = it.toInt().coerceIn(5, 60)) } },
                        valueRange  = 5f..60f,
                        steps       = 54,
                        colors      = sliderColors(accent),
                        modifier    = Modifier.width(200.dp)
                    )
                }
                SettingsDivider()
                SettingsRow(
                    label    = "Read Receipts",
                    sublabel = "Bot replies “✅ Read” when a message is dismissed",
                    icon     = Icons.Default.Done
                ) {
                    TapSwitch(
                        checked         = settings.telegramReadReceipts,
                        onCheckedChange = { onUpdate { copy(telegramReadReceipts = it) } },
                        colors          = switchColors(accent)
                    )
                }
                SettingsDivider()
                SettingsRow(
                    label    = "Status Indicator",
                    sublabel = "Header dot while connected to the bot",
                    icon     = Icons.Default.Circle
                ) {
                    TapSwitch(
                        checked         = settings.telegramShowIndicator,
                        onCheckedChange = { onUpdate { copy(telegramShowIndicator = it) } },
                        colors          = switchColors(accent)
                    )
                }
            }

            SettingsSection("Commands") {
                SettingsRow("GPS", "Replies with location + a tappable Maps link", Icons.Default.Place) {
                    TapSwitch(settings.telegramCommandGps, { onUpdate { copy(telegramCommandGps = it) } }, colors = switchColors(accent))
                }
                SettingsDivider()
                SettingsRow("TRACK XX", "Location + speed + heading every minute for XX minutes", Icons.Default.Route) {
                    TapSwitch(settings.telegramCommandTrack, { onUpdate { copy(telegramCommandTrack = it) } }, colors = switchColors(accent))
                }
                SettingsDivider()
                SettingsRow("CLEAR", "Clears pinned/pending messages and stops tracking", Icons.Default.DeleteSweep) {
                    TapSwitch(settings.telegramCommandClear, { onUpdate { copy(telegramCommandClear = it) } }, colors = switchColors(accent))
                }
                SettingsDivider()
                SettingsRow("STATUS", "Replies with speed, location, profile & battery", Icons.Default.MonitorHeart) {
                    TapSwitch(settings.telegramCommandStatus, { onUpdate { copy(telegramCommandStatus = it) } }, colors = switchColors(accent))
                }
                SettingsDivider()
                SettingsRow("WARN xxx", "Pinned alert — dismiss locked for 30 s", Icons.Default.WarningAmber) {
                    TapSwitch(settings.telegramCommandWarn, { onUpdate { copy(telegramCommandWarn = it) } }, colors = switchColors(accent))
                }
            }

            SettingsSection("Quick Replies") {
                settings.telegramQuickReplies.forEachIndexed { i, r ->
                    val replyLocal = remember(i) { mutableStateOf(r) }
                    SettingsRow(label = "Reply ${i + 1}", sublabel = "", icon = Icons.Default.Reply) {
                        OutlinedTextField(
                            value        = replyLocal.value,
                            onValueChange = { v ->
                                replyLocal.value = v
                                onUpdate {
                                    copy(telegramQuickReplies = telegramQuickReplies.toMutableList().also { if (i in it.indices) it[i] = v })
                                }
                            },
                            singleLine   = true,
                            textStyle    = MaterialTheme.typography.bodySmall.copy(color = titleColor),
                            colors       = outlinedFieldColors(accent),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                            modifier     = Modifier.weight(1f)
                        )
                    }
                    if (i < settings.telegramQuickReplies.lastIndex) SettingsDivider()
                }
                if (settings.telegramQuickReplies.size < 6) {
                    SettingsDivider()
                    SettingsButton(
                        label    = "Add Reply",
                        sublabel = "Buttons shown on the message banner",
                        icon     = Icons.Default.Add,
                        accent   = accent,
                        onClick  = { onUpdate { copy(telegramQuickReplies = telegramQuickReplies + "") } }
                    )
                }
            }

            SettingsSection("Diagnostics") {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (connected) "● Bot connected" else "○ Not connected",
                        color = if (connected) Color(0xFF43A047) else subColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (isOnline) "● Internet on" else "○ Internet off",
                        color = if (isOnline) Color(0xFF43A047) else Color(0xFFE53935),
                        fontSize = 11.sp
                    )
                }
                SettingsDivider()
                SettingsButton(
                    label    = "Send Test Message",
                    sublabel = "Validates the token & connectivity (needs an allowed sender)",
                    icon     = Icons.Default.Send,
                    accent   = accent,
                    onClick  = {
                        scope.launch {
                            testResult = null
                            val t = settings.telegramBotToken
                            if (t.isBlank()) {
                                TelegramBot.log("Test skipped — no token")
                                testResult = "Set a bot token first"
                            } else {
                                TelegramBot.log("Testing token…")
                                val res = TelegramBot.test(t)
                                TelegramBot.log(res ?: "Test FAILED — check token / internet")
                                testResult = res ?: "Failed — check token / internet"
                            }
                        }
                    }
                )
                testResult?.let {
                    SettingsDivider()
                    Text(it, color = if (it.startsWith("Bot ok")) Color(0xFF43A047) else Color(0xFFE53935), fontSize = 11.sp)
                }
                SettingsDivider()
                Text("LOG", color = subColor, fontSize = 10.sp, letterSpacing = 2.sp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isDayMode) Color(0xFFF0F0F0) else Color(0xFF111111))
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp)
                ) {
                    Text(
                        TelegramBot.log.joinToString("\n").ifEmpty { "No activity yet." },
                        color = subColor,
                        fontSize = 9.sp,
                        lineHeight = 12.sp
                    )
                }
            }
        }
    }
}

// ── Helpers ─────────────────────────────────────────────────────────────────

@Composable
private fun SettingsSection(
    title: String,
    onHeaderTap: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val query = LocalSettingsSearchQuery.current
    val searching = query.isNotBlank()
    val keywords = SECTION_KEYWORDS[title] ?: emptyList()
    val matched = title.contains(query, ignoreCase = true) || keywords.any { it.contains(query, ignoreCase = true) }
    if (searching && !matched) return  // keep only sections that contain the term
    val isDayMode     = LocalDayMode.current
    val sectionColor  = if (searching && matched) MaterialTheme.colorScheme.primary
                        else LocalHelpTextColor.current ?: if (isDayMode) Color(0xFF888888) else Color(0xFF3A3A3A)
    val dividerColor  = if (isDayMode) Color(0xFFCCCCCC) else Color(0xFF1E1E1E)
    val tracker       = LocalSectionTracker.current
    val accentGutter  = sectionColorFor(title)

    // Report this section's scroll offset for the sticky banner, and unregister
    // when the section leaves the composition (category switch / search filter).
    DisposableEffect(tracker) {
        onDispose { tracker?.positions?.remove(title) }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                // Coloured left edge — a 7dp strip spanning the ENTIRE subcategory
                // block (drawn on the section background, so it always covers the
                // full height regardless of child measurement)
                drawRoundRect(
                    color        = accentGutter,
                    topLeft      = Offset(0f, 0f),
                    size         = Size(7.dp.toPx(), size.height),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }
            .onGloballyPositioned { coords ->
                val t = tracker ?: return@onGloballyPositioned
                val v = coords.boundsInRoot().top - t.containerTop
                // Write only on real movement — avoids needless state-map invalidations
                if (t.positions[title] != v) t.positions[title] = v
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = if (onHeaderTap != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onHeaderTap
                    )
                } else Modifier
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accentGutter)
                )
                Text(
                    text          = title.uppercase(),
                    style         = MaterialTheme.typography.labelSmall,
                    color         = sectionColor,
                    letterSpacing = 2.sp,
                    fontWeight    = if (searching && matched) FontWeight.Bold else FontWeight.Normal,
                    modifier      = Modifier.padding(top = 16.dp, bottom = 6.dp)
                )
            }
            HorizontalDivider(color = dividerColor)
            Column(modifier = Modifier.fillMaxWidth(), content = content)
            HorizontalDivider(color = dividerColor)
        }
    }
}

@Composable
private fun SettingsRow(
    label: String,
    sublabel: String = "",
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable RowScope.() -> Unit
) {
    val isDayMode    = LocalDayMode.current
    val isCustomMode = LocalIsCustomMode.current
    val labelColor   = when {
        isDayMode    -> Color(0xFF111111)
        isCustomMode -> MaterialTheme.colorScheme.onBackground
        else         -> Color(0xFFDDDDDD)
    }
    val subColor    = LocalHelpTextColor.current ?: if (isDayMode) Color(0xFF888888) else Color(0xFF444444)
    val iconTint    = if (isDayMode) Color(0xFF777777) else MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = labelColor, fontSize = 13.sp)
            if (sublabel.isNotEmpty())
                Text(sublabel, style = MaterialTheme.typography.labelSmall, color = subColor, fontSize = 11.sp)
        }
        content()
    }
}

@Composable
private fun ColumnScope.SettingsButton(
    label: String,
    sublabel: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClick: () -> Unit
) {
    val isDayMode    = LocalDayMode.current
    val isCustomMode = LocalIsCustomMode.current
    val labelColor   = when {
        isDayMode    -> Color(0xFF111111)
        isCustomMode -> MaterialTheme.colorScheme.onBackground
        else         -> Color(0xFFDDDDDD)
    }
    val subColor   = LocalHelpTextColor.current ?: if (isDayMode) Color(0xFF888888) else Color(0xFF444444)
    val chevronC   = if (isDayMode) Color(0xFFBBBBBB) else Color(0xFF2A2A2A)
    val iconTint   = if (isDayMode) Color(0xFF777777) else MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tapClickable(onClick = onClick)
            .padding(horizontal = 0.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = labelColor, fontSize = 13.sp)
            if (sublabel.isNotEmpty())
                Text(sublabel, style = MaterialTheme.typography.labelSmall, color = subColor, fontSize = 11.sp)
        }
        Icon(Icons.Default.ChevronRight, null, tint = chevronC, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun ColumnScope.SettingsDivider() {
    val isDayMode = LocalDayMode.current
    HorizontalDivider(color = if (isDayMode) Color(0xFFDDDDDD) else Color(0xFF141414))
}

/**
 * Trailing-debounced settings writer for sliders. Rapid drag events coalesce
 * into one DataStore write after [delayMs] of inactivity, and the last pending
 * value is always flushed when the screen leaves composition so nothing is
 * lost. One-shot controls (switches/buttons) keep using [onUpdate] directly.
 */
@Composable
private fun rememberDebouncedUpdate(
    onUpdate: (AppSettings.() -> AppSettings) -> Unit,
    delayMs: Long = 250L
): (AppSettings.() -> AppSettings) -> Unit {
    val pending = remember { mutableStateOf<(AppSettings.() -> AppSettings)?>(null) }
    val currentOnUpdate by rememberUpdatedState(onUpdate)
    LaunchedEffect(Unit) {
        while (true) {
            delay(delayMs)
            val task = pending.value
            if (task != null) {
                pending.value = null
                currentOnUpdate(task)
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            pending.value?.let { currentOnUpdate(it) }
        }
    }
    return { block -> pending.value = block }
}

@Composable
private fun outlinedFieldColors(accent: Color): androidx.compose.material3.TextFieldColors {
    val isDayMode = LocalDayMode.current
    val textColor = if (isDayMode) Color(0xFF111111) else Color.White
    val borderU   = if (isDayMode) Color(0xFFCCCCCC) else Color(0xFF2A2A2A)
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor   = accent,
        unfocusedBorderColor = borderU,
        focusedTextColor     = textColor,
        unfocusedTextColor   = textColor,
        cursorColor          = accent,
        focusedLabelColor    = accent,
        unfocusedLabelColor  = if (isDayMode) Color(0xFF888888) else Color(0xFF666666)
    )
}

@Composable
private fun switchColors(accent: Color): androidx.compose.material3.SwitchColors {
    val isDayMode = LocalDayMode.current
    return SwitchDefaults.colors(
        checkedThumbColor    = if (isDayMode) Color.White else Color.Black,
        checkedTrackColor    = accent,
        uncheckedThumbColor  = if (isDayMode) Color(0xFFBBBBBB) else Color(0xFF888888),
        uncheckedTrackColor  = if (isDayMode) Color(0xFFDDDDDD) else Color(0xFF1E1E1E),
        uncheckedBorderColor = if (isDayMode) Color(0xFFCCCCCC) else Color(0xFF3A3A3A)
    )
}

@Composable
private fun sliderColors(accent: Color): androidx.compose.material3.SliderColors {
    val isDayMode = LocalDayMode.current
    return SliderDefaults.colors(
        thumbColor         = accent,
        activeTrackColor   = accent,
        inactiveTrackColor = if (isDayMode) Color(0xFFCCCCCC) else Color(0xFF2A2A2A)
    )
}


private fun fontDisplayName(font: AppFont): String = when (font) {
    AppFont.SYSTEM            -> "System"
    AppFont.JETBRAINS_MONO    -> "JetBrains Mono"
    AppFont.SOURCE_CODE_PRO   -> "Source Code Pro"
    AppFont.CAL_SANS          -> "Cal Sans"
    AppFont.FUGAZ_ONE         -> "Fugaz One"
    AppFont.GEOM              -> "Geom"
    AppFont.NOTO_SANS_DISPLAY -> "Noto Sans Display"
    AppFont.ROBOTO            -> "Roboto"
    AppFont.DSEG14_CLASSIC    -> "DSEG14 LCD"
    AppFont.IBM_VGA_9X16      -> "IBM VGA 9x16"
    AppFont.COMMISSIONER      -> "Commissioner"
    AppFont.DIDACT            -> "Didact Gothic"
    AppFont.FIRA              -> "Fira Sans"
    AppFont.HANDJET           -> "Handjet"
    AppFont.NEOHELLENIC       -> "GFS Neohellenic"
    AppFont.PLAY              -> "Play"
    AppFont.PLAYPEN           -> "Playpen Sans"
    AppFont.SANSATION         -> "Sansation"
    AppFont.TEKTUR            -> "Tektur"
    AppFont.UBUNTU            -> "Ubuntu"
}

private data class CategoryItem(
    val id: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
private fun CategoryTile(
    cat: CategoryItem,
    selected: Boolean,
    accent: Color,
    isDayMode: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (selected) accent else if (isDayMode) Color(0xFFFFFFFF) else Color(0xFF161616)
    val fg = if (selected) Color.Black else if (isDayMode) Color(0xFF111111) else Color(0xFFDDDDDD)
    val border = if (selected) accent else if (isDayMode) Color(0xFFCCCCCC) else Color(0xFF282828)
    Surface(
        onClick = { TapSound.play(); onClick() },
        shape = RoundedCornerShape(10.dp),
        color = bg,
        border = androidx.compose.foundation.BorderStroke(1.dp, border),
        modifier = modifier.height(48.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(cat.icon, null, tint = fg, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text = cat.label,
                color = fg,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    accent: Color,
    isDayMode: Boolean,
    modifier: Modifier = Modifier
) {
    // Accent-tinted so it reads as "search", distinct from the neutral category
    // tiles and the Open System Settings card.
    val bg = accent.copy(alpha = if (isDayMode) 0.12f else 0.20f)
    val textColor = if (isDayMode) Color(0xFF111111) else Color(0xFFF0F0F0)
    val keyboard = LocalSoftwareKeyboardController.current
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg,
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp).fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, null, tint = accent, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(10.dp))
            // The row's fixed height coerces BasicTextField's internal 48 dp
            // minimum — fixing the height on the field itself has no effect.
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(color = textColor, fontSize = 12.sp),
                    cursorBrush = SolidColor(accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                    modifier = Modifier.fillMaxSize(),
                    decorationBox = { innerTextField ->
                        // Center both the placeholder and the typed text
                        // vertically inside the (now shorter) search row.
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (query.isEmpty()) {
                                Text("Search…", color = textColor.copy(alpha = 0.55f), fontSize = 12.sp, maxLines = 1, softWrap = false)
                            }
                            innerTextField()
                        }
                    }
                )
            }
            // Clear button — the soft keyboard makes clearing the term painful
            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .clickable { TapSound.play(); onQueryChange(""); keyboard?.hide() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close,
                        "Clear search",
                        tint = textColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FontPickerDialog(
    current: AppFont,
    accent: Color,
    isDayMode: Boolean,
    onSelect: (AppFont) -> Unit,
    onDismiss: () -> Unit
) {
    val accentLum = 0.299f * accent.red + 0.587f * accent.green + 0.114f * accent.blue
    val effectiveAccent = if (isDayMode && accentLum > 0.65f) Color(0xFF111111)
                          else if (!isDayMode && accentLum < 0.25f) Color(0xFFEEEEEE)
                          else accent

    val dialogBg     = if (isDayMode) Color(0xFFFFFFFF) else Color(0xFF111111)
    val dialogBorder = if (isDayMode) Color(0xFFDDE1E5) else Color(0xFF222222)
    val textColor    = if (isDayMode) Color(0xFF111111) else Color(0xFFEEEEEE)
    val subColor     = if (isDayMode) Color(0xFF666666) else Color(0xFF888888)
    val cardBg       = if (isDayMode) Color(0xFFF8F9FA) else Color(0xFF161616)
    val cardBorder   = if (isDayMode) Color(0xFFCED4DA) else Color(0xFF242424)

    val scales = LocalUiScales.current
    val ts = scales.ts()
    val us = scales.us()

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .widthIn(min = (420 * us).dp, max = (680 * us).dp)
                .fillMaxWidth(0.94f)
                .heightIn(max = (480 * us).dp)
                .clip(RoundedCornerShape((10 * us).dp))
                .background(dialogBg)
                .border(1.dp, dialogBorder, RoundedCornerShape((10 * us).dp))
                .padding((16 * us).dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = (12 * us).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text          = "FONT",
                    color         = effectiveAccent,
                    fontSize      = (11 * ts).sp,
                    letterSpacing = (2 * ts).sp,
                    fontWeight    = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { TapSound.play(); onDismiss() }, modifier = Modifier.size((26 * us).dp)) {
                    Icon(Icons.Default.Close, null, tint = subColor, modifier = Modifier.size((16 * us).dp))
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = (180 * us).dp),
                horizontalArrangement = Arrangement.spacedBy((8 * us).dp),
                verticalArrangement = Arrangement.spacedBy((8 * us).dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(AppFont.entries) { font ->
                    val selected = font == current
                    val family = font.toFontFamily()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape((8 * us).dp))
                            .background(if (selected) effectiveAccent.copy(alpha = if (isDayMode) 0.12f else 0.2f) else cardBg)
                            .border(1.dp, if (selected) effectiveAccent else cardBorder, RoundedCornerShape((8 * us).dp))
                            .clickable { TapSound.play(); onSelect(font) }
                            .padding(horizontal = (12 * us).dp, vertical = (12 * us).dp),
                        verticalArrangement = Arrangement.spacedBy((6 * us).dp)
                    ) {
                        // Two elements only: the font name on top, the sample below.
                        Text(
                            text = fontDisplayName(font),
                            fontFamily = family,
                            color = if (selected) effectiveAccent else textColor,
                            fontSize = (18 * ts).sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Free by Limbo666",
                            fontFamily = family,
                            color = textColor.copy(alpha = 0.75f),
                            fontSize = (14 * ts).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingSlotDialog(
    slotIndex: Int,
    current: FloatingPanelAction?,
    apps: List<com.openlauncher.app.model.AppInfo>,
    accent: Color,
    isDayMode: Boolean,
    selectedAppPackages: List<String> = emptyList(),
    toolsAppPackages: List<String> = emptyList(),
    onSelectType: (FloatingActionType) -> Unit,
    onSelectApp: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var showAppPicker by remember { mutableStateOf(false) }
    val dialogBg   = if (isDayMode) Color(0xFFF7F7F7) else Color(0xFF161616)
    val borderC    = if (isDayMode) Color(0xFFCCCCCC) else Color(0xFF282828)
    val textColor  = if (isDayMode) Color(0xFF111111) else Color(0xFFDDDDDD)

    val scales = LocalUiScales.current
    val ts = scales.ts()
    val us = scales.us()

    if (showAppPicker) {
        AppPickerDialog(
            title          = "BUTTON ${slotIndex + 1} APP",
            currentPackage = current?.packageName.orEmpty(),
            apps           = apps,
            accent         = accent,
            isDayMode      = isDayMode,
            selectedAppPackages = selectedAppPackages,
            toolsAppPackages    = toolsAppPackages,
            clearLabel     = null,
            onSelect       = { pkg -> onSelectApp(pkg) },
            onDismiss      = { showAppPicker = false }
        )
        return
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .widthIn(min = (280 * us).dp, max = (460 * us).dp)
                .heightIn(max = (480 * us).dp)
                .clip(RoundedCornerShape((10 * us).dp))
                .background(dialogBg)
                .border(1.dp, borderC, RoundedCornerShape((10 * us).dp))
                .verticalScroll(rememberScrollState())
                .padding((16 * us).dp)
        ) {
            Text(
                text          = "BUTTON ${slotIndex + 1}",
                color         = accent,
                fontSize      = (12 * ts).sp,
                letterSpacing = (2 * ts).sp,
                fontWeight    = FontWeight.Bold
            )
            Spacer(Modifier.height((12 * us).dp))

            FloatingActionType.entries.forEach { type ->
                val selected = current?.type == type
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape((6 * us).dp))
                        .background(if (selected) accent.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable {
                            TapSound.play()
                            if (type == FloatingActionType.APP) showAppPicker = true else onSelectType(type)
                        }
                        .padding(horizontal = (10 * us).dp, vertical = (10 * us).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(floatingActionIcon(type), null, tint = accent, modifier = Modifier.size((18 * us).dp))
                    Spacer(Modifier.width((10 * us).dp))
                    Text(floatingActionLabel(type), color = textColor, fontSize = (13 * ts).sp)
                    if (type == FloatingActionType.APP) {
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.ChevronRight, null, tint = textColor.copy(alpha = 0.5f), modifier = Modifier.size((16 * us).dp))
                    }
                }
            }
        }
    }
}

