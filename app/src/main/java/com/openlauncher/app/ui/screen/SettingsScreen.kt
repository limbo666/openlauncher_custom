package com.openlauncher.app.ui.screen

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openlauncher.app.data.AppFont
import com.openlauncher.app.data.AppSettings
import com.openlauncher.app.data.DayNightMode
import com.openlauncher.app.data.SidebarPosition
import com.openlauncher.app.data.ShortcutConfig
import com.openlauncher.app.data.GradientDirection
import com.openlauncher.app.data.UnitSystem
import com.openlauncher.app.data.WeatherProvider
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.openlauncher.app.ui.theme.LocalDayMode
import com.openlauncher.app.ui.theme.LocalIsCustomMode
import com.openlauncher.app.ui.theme.accentOnSurface
import com.openlauncher.app.ui.theme.contentColorOn
import com.openlauncher.app.util.FileLog
import com.openlauncher.app.util.SunriseSunset
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.openlauncher.app.ui.components.ColorPickerDialog
import com.openlauncher.app.ui.components.ConfirmDialog
import com.openlauncher.app.data.SettingsBackup
import com.openlauncher.app.obd.pairedAdapters
import com.openlauncher.app.ui.widget.AdapterPickerDialog
import androidx.compose.runtime.saveable.rememberSaveable
import kotlin.math.roundToInt

// Resolved at call site via LocalDayMode — see SettingsDivider / SettingsSection

@Composable
fun SettingsScreen(
    settings: AppSettings,
    accent: Color,
    onUpdate: (AppSettings.() -> AppSettings) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showResetDialog       by remember { mutableStateOf(false) }
    var showAccentPicker      by remember { mutableStateOf(false) }
    var showBgPicker          by remember { mutableStateOf(false) }
    var showGradientEndPicker by remember { mutableStateOf(false) }
    var showFontColorPicker   by remember { mutableStateOf(false) }
    var showTileDarkPicker    by remember { mutableStateOf(false) }
    var showTileLightPicker   by remember { mutableStateOf(false) }

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
            onUpdate { copy(wallpaperUri = it.toString()) }
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

    var selectedCategory by rememberSaveable { mutableStateOf("All") }
    val categories = listOf("All", "Display", "Dashboard", "Vehicle & OBD", "System")
    val scrollState = rememberScrollState()
    LaunchedEffect(selectedCategory) { scrollState.scrollTo(0) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
    ) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ── Title & Category Chips ──────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text          = "SETTINGS",
                style         = MaterialTheme.typography.titleLarge,
                color         = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 3.sp,
                fontSize      = 14.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick  = { selectedCategory = cat },
                        label    = {
                            Text(
                                text = cat,
                                fontSize = 9.sp,
                                letterSpacing = 0.5.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = selectedChipContainer,
                            selectedLabelColor     = selectedChipLabel,
                            labelColor             = if (isDayMode) Color(0xFF999999) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }

        // ── Open System Settings ─────────────────────────────────────────────
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
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    tint = specialIconTint,
                    modifier = Modifier.size(20.dp)
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
        if (selectedCategory == "All" || selectedCategory == "Display") {
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
                        FilterChip(
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
                        .background(Color(settings.accentColor))
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

                SettingsRow(label = "Use Gradient", sublabel = "Blend two colors as background", icon = Icons.Default.Gradient) {
                    Switch(checked = settings.useGradient,
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
                                FilterChip(
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
                sublabel = if (settings.wallpaperUri.isNotEmpty()) "Custom wallpaper active" else "Choose image from gallery",
                icon     = Icons.Default.Wallpaper,
                accent   = accent,
                onClick  = { wallpaperPicker.launch(arrayOf("image/*")) }
            )
            if (settings.wallpaperUri.isNotEmpty()) {
                Column {
                    SettingsRow(
                        label    = "Wallpaper Dim",
                        sublabel = "${"%.0f".format(settings.wallpaperDim * 100)}%",
                        icon     = Icons.Default.BrightnessLow
                    ) {}
                    Slider(
                        value         = settings.wallpaperDim,
                        onValueChange = { onUpdate { copy(wallpaperDim = it) } },
                        valueRange    = 0f..0.95f,
                        steps         = 18,
                        colors        = sliderColors(accent),
                        modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        onClick  = { onUpdate { copy(wallpaperUri = "") } },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("REMOVE WALLPAPER", color = Color(0xFF993333), fontSize = 9.sp, letterSpacing = 1.sp)
                    }
                }
            }

            SettingsDivider()

            SettingsRow(
                label    = "Status Bar Indicators",
                sublabel = if (settings.showStatusIcons) "Wi-Fi, Bluetooth & Cellular icons visible in top bar" else "Hidden (prevents duplication with system status bar)",
                icon     = Icons.Default.Wifi
            ) {
                Switch(
                    checked         = settings.showStatusIcons,
                    onCheckedChange = { onUpdate { copy(showStatusIcons = it) } },
                    colors          = switchColors(accent)
                )
            }
        }
        }

        // ── Typography ───────────────────────────────────────────────────────
        if (selectedCategory == "All" || selectedCategory == "Display") {
        SettingsSection("Typography") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                SettingsRow(
                    label    = "Font",
                    sublabel = fontDisplayName(settings.appFont),
                    icon     = Icons.Default.FontDownload
                ) {}
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 32.dp, end = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    com.openlauncher.app.data.AppFont.entries.forEach { font ->
                        FilterChip(
                            selected = settings.appFont == font,
                            onClick  = { onUpdate { copy(appFont = font) } },
                            label    = { Text(fontDisplayName(font), fontSize = 11.sp, letterSpacing = 0.5.sp) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = selectedChipContainer,
                                selectedLabelColor     = selectedChipLabel
                            )
                        )
                    }
                }
            }

            SettingsDivider()

            SettingsRow(label = "Bold Font", sublabel = "Heavier weight across all text", icon = Icons.Default.FormatBold) {
                Switch(
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
                    onValueChange = { onUpdate { copy(textScale = it) } },
                    valueRange    = 0.8f..1.4f,
                    steps         = 5,
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
                    onValueChange = { onUpdate { copy(uiScale = it) } },
                    valueRange    = 0.7f..1.5f,
                    steps         = 7,
                    colors        = sliderColors(accent),
                    modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
            }
        }
        }

        // ── Tiles ─────────────────────────────────────────────────────────────
        if (selectedCategory == "All" || selectedCategory == "Display") {
        SettingsSection("Tiles") {
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
                    onValueChange = { onUpdate { copy(tileAlpha = it) } },
                    valueRange    = 0.1f..1f,
                    steps         = 8,
                    colors        = sliderColors(accent),
                    modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
            }
        }
        }

        // ── Vehicle & OBD ────────────────────────────────────────────────────
        if (selectedCategory == "All" || selectedCategory == "Vehicle & OBD") {
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
                    FilterChip(
                        selected = settings.unitSystem == UnitSystem.METRIC,
                        onClick  = { onUpdate { copy(unitSystem = UnitSystem.METRIC) } },
                        label    = { Text("Metric", fontSize = 11.sp) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = selectedChipContainer,
                            selectedLabelColor     = selectedChipLabel
                        )
                    )
                    Spacer(Modifier.width(6.dp))
                    FilterChip(
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

            SettingsDivider()

            SettingsRow(
                label    = "Show OBD2 Widget",
                sublabel = "Displays real-time RPM, speed, coolant temp & battery voltage on dashboard",
                icon     = Icons.Default.Speed
            ) {
                Switch(
                    checked         = settings.showObd,
                    onCheckedChange = { on ->
                        onUpdate {
                            val hasObd = widgetLayout.any { it.id == "OBD" }
                            val newLayout = if (on && !hasObd) {
                                widgetLayout + com.openlauncher.app.data.WidgetConfig("OBD", gridX = 4, gridY = 3, spanX = 2, spanY = 1)
                            } else {
                                widgetLayout
                            }
                            copy(showObd = on, widgetLayout = newLayout)
                        }
                    },
                    colors          = switchColors(accent)
                )
            }
        }
        }

        // ── Sidebar ──────────────────────────────────────────────────────────
        if (selectedCategory == "All" || selectedCategory == "Display") {
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
                        FilterChip(
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
                    onValueChange = { onUpdate { copy(sidebarWidth = it.roundToInt()) } },
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
                    onValueChange = { onUpdate { copy(sidebarIconScale = it) } },
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
                    Switch(
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

        // ── Widgets ───────────────────────────────────────────────────────────
        if (selectedCategory == "All" || selectedCategory == "Dashboard") {
        SettingsSection("Widgets") {
            // App Shortcuts Count (1..4)
            SettingsRow(
                label    = "App Shortcuts Count",
                sublabel = "${settings.appShortcutCount} ${if (settings.appShortcutCount == 1) "shortcut" else "shortcuts"} — icons expand to fill widget space",
                icon     = Icons.Default.Apps
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isDayMode) Color(0xFFE0E0E0) else Color(0xFF222222))
                        .padding(2.dp)
                ) {
                    (1..4).forEach { count ->
                        val selected = settings.appShortcutCount == count
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (selected) accent else Color.Transparent)
                                .clickable { onUpdate { copy(appShortcutCount = count) } }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$count",
                                color = if (selected) Color.Black else (if (isDayMode) Color(0xFF333333) else Color(0xFFAAAAAA)),
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            SettingsDivider()

            // Soundboard Button Count (1, 2, 3, 4, 6)
            SettingsRow(
                label    = "Soundboard Buttons",
                sublabel = "${settings.soundboardButtonCount} sound pads — active buttons expand to take empty space",
                icon     = Icons.Default.Piano
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isDayMode) Color(0xFFE0E0E0) else Color(0xFF222222))
                        .padding(2.dp)
                ) {
                    listOf(1, 2, 3, 4, 6).forEach { count ->
                        val selected = settings.soundboardButtonCount == count
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (selected) accent else Color.Transparent)
                                .clickable { onUpdate { copy(soundboardButtonCount = count) } }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$count",
                                color = if (selected) Color.Black else (if (isDayMode) Color(0xFF333333) else Color(0xFFAAAAAA)),
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
        }

        // ── Weather Service & Location ───────────────────────────────────────
        if (selectedCategory == "All" || selectedCategory == "Dashboard") {
        SettingsSection("Weather") {
            // 1. Dynamic GPS vs Manual Location Toggle
            SettingsRow(
                label    = "Use Dynamic GPS Location",
                sublabel = if (settings.weatherUseGps) "Weather automatically tracks vehicle GPS coordinates" else "Using custom location specified below",
                icon     = Icons.Default.MyLocation
            ) {
                Switch(
                    checked         = settings.weatherUseGps,
                    onCheckedChange = { onUpdate { copy(weatherUseGps = it) } },
                    colors          = switchColors(accent)
                )
            }

            // 2. Custom Location Input (when GPS is false, or fallback)
            SettingsDivider()
            var customLocInput by remember(settings.weatherCustomLocation) { mutableStateOf(settings.weatherCustomLocation) }
            SettingsRow(
                label    = "Custom Location / Fallback City",
                sublabel = "City name (e.g. Athens, London, New York) or coordinates",
                icon     = Icons.Default.LocationCity
            ) {
                BasicTextField(
                    value = customLocInput,
                    onValueChange = {
                        customLocInput = it
                        onUpdate { copy(weatherCustomLocation = it) }
                    },
                    textStyle = TextStyle(
                        color = if (isDayMode) Color(0xFF111111) else Color(0xFFEEEEEE),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .widthIn(min = 120.dp, max = 180.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isDayMode) Color(0xFFE0E0E0) else Color(0xFF222222))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }

            // 3. Weather Service Provider
            SettingsDivider()
            SettingsRow(
                label    = "Weather Service Provider",
                sublabel = if (settings.weatherProvider == WeatherProvider.WTTR_IN) "wttr.in — Free one-liner & curl service" else "Open-Meteo — Free high-precision 7-day forecast",
                icon     = Icons.Default.Cloud
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isDayMode) Color(0xFFE0E0E0) else Color(0xFF222222))
                        .padding(2.dp)
                ) {
                    WeatherProvider.entries.forEach { prov ->
                        val selected = settings.weatherProvider == prov
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (selected) accent else Color.Transparent)
                                .clickable { onUpdate { copy(weatherProvider = prov) } }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (prov == WeatherProvider.WTTR_IN) "wttr.in" else "Open-Meteo",
                                color = if (selected) Color.Black else (if (isDayMode) Color(0xFF333333) else Color(0xFFAAAAAA)),
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // 4. Forecast Days Count (3, 5, 7)
            SettingsDivider()
            SettingsRow(
                label    = "Weather Animated Icons",
                sublabel = if (settings.advancedWeatherIcons) "Advanced (100+ dynamic condition-specific Meteocons)" else "Standard (simplified broad categories)",
                icon     = Icons.Default.Cloud
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isDayMode) Color(0xFFE0E0E0) else Color(0xFF222222))
                        .padding(2.dp)
                ) {
                    listOf(false to "Standard", true to "Advanced").forEach { (advanced, label) ->
                        val selected = settings.advancedWeatherIcons == advanced
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (selected) accent else Color.Transparent)
                                .clickable { onUpdate { copy(advancedWeatherIcons = advanced) } }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (selected) Color.Black else (if (isDayMode) Color(0xFF333333) else Color(0xFFAAAAAA)),
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            SettingsDivider()
            SettingsRow(
                label    = "Forecast Days",
                sublabel = "Number of upcoming daily forecast cards to display in widget",
                icon     = Icons.Default.CalendarToday
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isDayMode) Color(0xFFE0E0E0) else Color(0xFF222222))
                        .padding(2.dp)
                ) {
                    listOf(3, 5, 7).forEach { days ->
                        val selected = settings.weatherForecastDays == days
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (selected) accent else Color.Transparent)
                                .clickable { onUpdate { copy(weatherForecastDays = days) } }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$days Days",
                                color = if (selected) Color.Black else (if (isDayMode) Color(0xFF333333) else Color(0xFFAAAAAA)),
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
        }

        // ── GPS & Calibration ───────────────────────────────────────────────
        if (selectedCategory == "All" || selectedCategory == "Dashboard") {
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
                    onValueChange = { onUpdate { copy(compassOffset = it) } },
                    valueRange    = -180f..180f,
                    steps         = 71, // 5 degree steps: 360 / 5 - 1 = 71 steps
                    colors        = sliderColors(accent),
                    modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
            }
        }
        }

        // ── Backup & Restore ──────────────────────────────────────────────────
        if (selectedCategory == "All" || selectedCategory == "System") {
        SettingsSection("Backup & Restore") {
            var backupStatusMessage by remember { mutableStateOf<String?>(null) }
            var pendingRestoredSettings by remember { mutableStateOf<AppSettings?>(null) }
            val coroutineScope = rememberCoroutineScope()

            val exportLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("application/json")
            ) { uri ->
                if (uri != null) {
                    coroutineScope.launch {
                        try {
                            val json = SettingsBackup.encode(settings)
                            context.contentResolver.openOutputStream(uri)?.use { os ->
                                os.bufferedWriter().use { it.write(json) }
                            }
                            backupStatusMessage = "Settings exported successfully"
                        } catch (e: Exception) {
                            backupStatusMessage = "Failed to export settings: ${e.message}"
                        }
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
                sublabel = "Optional — reserved for a future PiP feature (not active yet)",
                icon     = Icons.Default.Layers,
                accent   = accent,
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
        }

        // ── Updates ──────────────────────────────────────────────────────────
        SettingsSection("Updates") {
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

        // ── Maintenance ──────────────────────────────────────────────────────
        SettingsSection("Maintenance") {
            val logScope = rememberCoroutineScope()
            val logExportLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("text/plain")
            ) { uri ->
                if (uri != null) {
                    logScope.launch {
                        val text = FileLog.logFile()?.takeIf { it.exists() }?.readText()
                            ?: "No log data recorded yet.\n"
                        runCatching {
                            context.contentResolver.openOutputStream(uri)?.use { os ->
                                os.bufferedWriter().use { it.write(text) }
                            }
                        }
                    }
                }
            }
            SettingsButton(
                label    = "Export Diagnostics Log",
                sublabel = "Save the app's debug log (artwork / radio / volume) to a file",
                icon     = Icons.Default.BugReport,
                accent   = accent,
                onClick  = {
                    val ts = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
                    logExportLauncher.launch("openlauncher_log_$ts.txt")
                }
            )
            SettingsDivider()
            SettingsButton(
                label    = "Re-run Setup Wizard",
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
        )
    }
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
            initialColor    = Color(settings.accentColor),
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
}

// ── Helpers ─────────────────────────────────────────────────────────────────

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDayMode     = LocalDayMode.current
    val sectionColor  = if (isDayMode) Color(0xFF888888) else Color(0xFF3A3A3A)
    val dividerColor  = if (isDayMode) Color(0xFFCCCCCC) else Color(0xFF1E1E1E)
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Text(
            text          = title.uppercase(),
            style         = MaterialTheme.typography.labelSmall,
            color         = sectionColor,
            letterSpacing = 2.sp,
            modifier      = Modifier.padding(top = 16.dp, bottom = 6.dp)
        )
        HorizontalDivider(color = dividerColor)
        Column(modifier = Modifier.fillMaxWidth(), content = content)
        HorizontalDivider(color = dividerColor)
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
    val subColor    = if (isDayMode) Color(0xFF888888) else Color(0xFF444444)
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
    val subColor   = if (isDayMode) Color(0xFF888888) else Color(0xFF444444)
    val chevronC   = if (isDayMode) Color(0xFFBBBBBB) else Color(0xFF2A2A2A)
    val iconTint   = if (isDayMode) Color(0xFF777777) else MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
}
