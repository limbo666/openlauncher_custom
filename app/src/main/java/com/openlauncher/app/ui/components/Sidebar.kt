package com.openlauncher.app.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.core.graphics.drawable.toBitmap
import com.openlauncher.app.data.AppSettings
import com.openlauncher.app.data.DefaultShortcutIcon
import com.openlauncher.app.data.ShortcutConfig
import com.openlauncher.app.model.NavDestination
import com.openlauncher.app.ui.theme.LocalDayMode
import kotlin.math.roundToInt

// Menubar sizing is user-configurable (AppSettings.sidebarWidth / sidebarIconScale).
// These are the unscaled base dimensions the scale multiplies from.
private const val BASE_ICON_SIZE_DP        = 22f
private const val BASE_NATIVE_ICON_SIZE_DP = 26f
private const val BASE_NATIVE_BITMAP_PX    = 44f
private const val MIN_BAR_SIZE             = 40
private const val MAX_BAR_SIZE             = 120
private const val MIN_SLOT_SIZE            = 36

@Composable
fun Sidebar(
    currentDest: NavDestination,
    settings: AppSettings,
    installedIconFor: (String) -> Drawable?,
    onNavigate: (NavDestination) -> Unit,
    onShortcutClick: (Int) -> Unit,
    onShortcutLongPress: (Int) -> Unit,
    onShortcutRemove: (Int) -> Unit,
    onShortcutSetIcon: (Int, DefaultShortcutIcon?) -> Unit,
    onReorder: (from: Int, to: Int) -> Unit,
    isHorizontal: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isDayMode    = LocalDayMode.current
    val accent       = Color(settings.accentColor)
    val sidebarBg    = if (isDayMode) Color(0xFFE0E0E0) else MaterialTheme.colorScheme.background.copy(alpha = 0.7f)
    val iconInactive = if (isDayMode) Color(0xFF777777) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
    val dividerColor = if (isDayMode) Color(0xFFCCCCCC) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
    val density      = LocalDensity.current
    val barSize      = settings.sidebarWidth.coerceIn(MIN_BAR_SIZE, MAX_BAR_SIZE).dp
    val slotSize     = (settings.sidebarWidth - 4).coerceAtLeast(MIN_SLOT_SIZE).dp
    val iconScale    = settings.sidebarIconScale.coerceIn(0.6f, 1.8f)
    val iconSize     = (BASE_ICON_SIZE_DP * iconScale).dp
    val slotSizePx   = with(density) { slotSize.toPx() }

    var actionSheetSlot by remember { mutableStateOf<Int?>(null) }
    var iconPickerSlot  by remember { mutableStateOf<Int?>(null) }

    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetPx  by remember { mutableFloatStateOf(0f) }

    fun dragTargetIndex(): Int = if (draggingIndex < 0) -1 else
        (draggingIndex + (dragOffsetPx / slotSizePx).roundToInt())
            .coerceIn(0, settings.shortcuts.size - 1)

    fun slotTranslation(index: Int): Float {
        if (draggingIndex < 0 || index == draggingIndex) return 0f
        val from = draggingIndex
        val to   = dragTargetIndex()
        return when {
            from < to && index in (from + 1)..to -> -slotSizePx
            from > to && index in to until from  ->  slotSizePx
            else -> 0f
        }
    }

    val shortcutsContent: @Composable () -> Unit = {
        settings.shortcuts.forEachIndexed { index, shortcut ->
            val isDragging  = (index == draggingIndex)
            val translation = if (isDragging) dragOffsetPx else slotTranslation(index)

            ShortcutSlot(
                shortcut        = shortcut,
                accent          = accent,
                resolvedIcon    = if (shortcut.packageName.isNotEmpty())
                                      installedIconFor(shortcut.packageName) else null,
                isDragging      = isDragging,
                dragTranslation = translation,
                isHorizontal    = isHorizontal,
                slotSize        = slotSize,
                iconScale       = iconScale,
                onClick         = { onShortcutClick(index) },
                onLongPress     = { actionSheetSlot = index },
                onDragStart = {
                    draggingIndex = index
                    dragOffsetPx  = 0f
                },
                onDragDelta = { d -> dragOffsetPx += d },
                onDragEnd   = {
                    val from = draggingIndex
                    val to   = dragTargetIndex()
                    if (from >= 0 && to != from) onReorder(from, to)
                    draggingIndex = -1
                    dragOffsetPx  = 0f
                }
            )
        }
    }

    val navButtons: @Composable () -> Unit = {
        NavButton(
            icon         = Icons.Default.Apps,
            label        = "Apps",
            isActive     = currentDest == NavDestination.APP_LIBRARY,
            accent       = accent,
            iconInactive = iconInactive,
            isHorizontal = isHorizontal,
            slotSize     = slotSize,
            iconSize     = iconSize,
            onClick      = { onNavigate(NavDestination.APP_LIBRARY) }
        )
        NavButton(
            icon         = Icons.Default.Settings,
            label        = "Settings",
            isActive     = currentDest == NavDestination.SETTINGS,
            accent       = accent,
            iconInactive = iconInactive,
            isHorizontal = isHorizontal,
            slotSize     = slotSize,
            iconSize     = iconSize,
            onClick      = { onNavigate(NavDestination.SETTINGS) }
        )
        NavButton(
            icon         = Icons.Default.Home,
            label        = "Home",
            isActive     = currentDest == NavDestination.HOME,
            accent       = accent,
            iconInactive = iconInactive,
            isHorizontal = isHorizontal,
            slotSize     = slotSize,
            iconSize     = iconSize,
            onClick      = { onNavigate(NavDestination.HOME) }
        )
    }

    if (isHorizontal) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(barSize)
                .background(sidebarBg)
        ) {
            // Shortcuts centred, inset past the edge-pinned nav buttons and
            // scrollable — an unbounded row ran beneath the nav buttons and off
            // both screen edges once enough slots were added
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxHeight()
                    .padding(horizontal = 150.dp)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                shortcutsContent()
            }

            // Nav buttons pinned to one edge, Home always outermost
            Row(
                modifier = Modifier
                    .align(if (settings.bottomBarShortcutsRight) Alignment.CenterEnd else Alignment.CenterStart)
                    .fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!settings.bottomBarShortcutsRight) {
                    NavButton(Icons.Default.Home,     "Home",     currentDest == NavDestination.HOME,        accent, iconInactive, true, slotSize, iconSize) { onNavigate(NavDestination.HOME) }
                    NavButton(Icons.Default.Settings, "Settings", currentDest == NavDestination.SETTINGS,    accent, iconInactive, true, slotSize, iconSize) { onNavigate(NavDestination.SETTINGS) }
                    NavButton(Icons.Default.Apps,     "Apps",     currentDest == NavDestination.APP_LIBRARY, accent, iconInactive, true, slotSize, iconSize) { onNavigate(NavDestination.APP_LIBRARY) }
                } else {
                    NavButton(Icons.Default.Apps,     "Apps",     currentDest == NavDestination.APP_LIBRARY, accent, iconInactive, true, slotSize, iconSize) { onNavigate(NavDestination.APP_LIBRARY) }
                    NavButton(Icons.Default.Settings, "Settings", currentDest == NavDestination.SETTINGS,    accent, iconInactive, true, slotSize, iconSize) { onNavigate(NavDestination.SETTINGS) }
                    NavButton(Icons.Default.Home,     "Home",     currentDest == NavDestination.HOME,        accent, iconInactive, true, slotSize, iconSize) { onNavigate(NavDestination.HOME) }
                }
            }
        }
    } else {
        Column(
            modifier = modifier
                .width(barSize)
                .fillMaxHeight()
                .background(sidebarBg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 6.dp, bottom = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                shortcutsContent()
            }

            HorizontalDivider(color = dividerColor)
            navButtons()
            Spacer(Modifier.height(4.dp))
        }
    }

    // ── Action sheet dialog ──────────────────────────────────────────────────
    actionSheetSlot?.let { slot ->
        val shortcut = settings.shortcuts.getOrNull(slot)
        val hasApp = shortcut?.packageName?.isNotEmpty() == true
        ShortcutActionDialog(
            accent      = accent,
            hasApp      = hasApp,
            onLaunchApp = {
                actionSheetSlot = null
                onShortcutClick(slot)
            },
            onChangeApp = {
                actionSheetSlot = null
                onShortcutLongPress(slot)
            },
            onCustomizeIcon = {
                actionSheetSlot = null
                iconPickerSlot = slot
            },
            onRemove = {
                actionSheetSlot = null
                onShortcutRemove(slot)
            },
            onDismiss = { actionSheetSlot = null }
        )
    }

    // ── Icon picker dialog ───────────────────────────────────────────────────
    iconPickerSlot?.let { slot ->
        IconPickerDialog(
            accent          = accent,
            hasNativeIcon   = settings.shortcuts.getOrNull(slot)?.packageName?.isNotEmpty() == true,
            currentOverride = settings.shortcuts.getOrNull(slot)?.customIconOverride,
            onPick  = { icon ->
                iconPickerSlot = null
                onShortcutSetIcon(slot, icon)
            },
            onDismiss = { iconPickerSlot = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShortcutSlot(
    shortcut: ShortcutConfig,
    accent: Color,
    resolvedIcon: Drawable?,
    isDragging: Boolean,
    dragTranslation: Float,
    isHorizontal: Boolean,
    slotSize: Dp,
    iconScale: Float,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onDragStart: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val iconSize       = (BASE_ICON_SIZE_DP * iconScale).dp
    val nativeIconSize = (BASE_NATIVE_ICON_SIZE_DP * iconScale).dp

    val currentOnClick      by rememberUpdatedState(onClick)
    val currentOnLongPress  by rememberUpdatedState(onLongPress)
    val currentOnDragStart  by rememberUpdatedState(onDragStart)
    val currentOnDragDelta  by rememberUpdatedState(onDragDelta)
    val currentOnDragEnd    by rememberUpdatedState(onDragEnd)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .then(
                if (isHorizontal) Modifier.fillMaxHeight().width(slotSize)
                else              Modifier.fillMaxWidth().height(slotSize)
            )
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer {
                if (isHorizontal) translationX = dragTranslation else translationY = dragTranslation
                alpha = if (isDragging) 0.55f else 1f
            }
            .combinedClickable(
                onClick     = { currentOnClick() },
                onLongClick = { }
            )
            .pointerInput(isHorizontal) {
                var longPressTriggered = false
                var hasSignificantDrag = false
                var totalDrag          = 0f
                detectDragGesturesAfterLongPress(
                    onDragStart = { _ ->
                        longPressTriggered = true
                        hasSignificantDrag = false
                        totalDrag          = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val delta = if (isHorizontal) dragAmount.x else dragAmount.y
                        totalDrag += delta
                        if (!hasSignificantDrag && kotlin.math.abs(totalDrag) > viewConfiguration.touchSlop) {
                            hasSignificantDrag = true
                            currentOnDragStart()
                        }
                        if (hasSignificantDrag) currentOnDragDelta(delta)
                    },
                    onDragEnd = {
                        if (longPressTriggered && !hasSignificantDrag) currentOnLongPress()
                        if (hasSignificantDrag) currentOnDragEnd()
                        longPressTriggered = false
                        hasSignificantDrag = false
                        totalDrag          = 0f
                    },
                    onDragCancel = {
                        if (hasSignificantDrag) currentOnDragEnd()
                        longPressTriggered = false
                        hasSignificantDrag = false
                        totalDrag          = 0f
                    }
                )
            }
    ) {
        val iconInactive = if (LocalDayMode.current) Color(0xFF777777) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
        val override = shortcut.customIconOverride
        when {
            override != null && override != DefaultShortcutIcon.NONE -> {
                Icon(
                    imageVector        = override.toIcon(),
                    contentDescription = shortcut.label,
                    tint               = iconInactive,
                    modifier           = Modifier.size(iconSize)
                )
            }
            resolvedIcon != null -> {
                // Cache per icon — every slot recomposes each drag frame, and an
                // un-remembered toBitmap allocated a fresh bitmap per slot per frame
                val bmp = remember(resolvedIcon, iconScale) {
                    val px = (BASE_NATIVE_BITMAP_PX * iconScale).roundToInt().coerceAtLeast(1)
                    resolvedIcon.toBitmap(px, px)
                }
                Icon(
                    painter            = BitmapPainter(bmp.asImageBitmap()),
                    contentDescription = shortcut.label,
                    tint               = Color.Unspecified,
                    modifier           = Modifier.size(nativeIconSize)
                )
            }
            shortcut.isDefault -> {
                Icon(
                    imageVector        = shortcut.defaultIcon.toIcon(),
                    contentDescription = shortcut.label,
                    tint               = iconInactive,
                    modifier           = Modifier.size(iconSize)
                )
            }
            else -> {
                Icon(
                    imageVector        = Icons.Default.Add,
                    contentDescription = "Add shortcut",
                    tint               = if (LocalDayMode.current) Color(0xFFBBBBBB) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f),
                    modifier           = Modifier.size(iconSize)
                )
            }
        }
    }
}

@Composable
private fun ShortcutActionDialog(
    accent: Color,
    hasApp: Boolean = false,
    onLaunchApp: () -> Unit = {},
    onChangeApp: () -> Unit,
    onCustomizeIcon: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDayMode = LocalDayMode.current
    val accentLum = 0.299f * accent.red + 0.587f * accent.green + 0.114f * accent.blue
    val effectiveAccent = if (isDayMode && accentLum > 0.65f) Color(0xFF111111)
                          else if (!isDayMode && accentLum < 0.25f) Color(0xFFEEEEEE)
                          else accent

    val dialogBg = if (isDayMode) Color(0xFFFFFFFF) else Color(0xFF111111)
    val dialogBorder = if (isDayMode) Color(0xFFDDE1E5) else Color(0xFF1E1E1E)
    val dividerColor = if (isDayMode) Color(0xFFF1F3F5) else Color(0xFF1A1A1A)
    val actionTint = if (isDayMode) Color(0xFF111111) else effectiveAccent
    val removeTint = if (isDayMode) Color(0xFFE03131) else Color(0xFFFF6B6B)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(dialogBg)
                .border(1.dp, dialogBorder, RoundedCornerShape(8.dp))
                .padding(vertical = 6.dp)
                .width(220.dp)
        ) {
            if (hasApp) {
                ActionRow("OPEN APP", Icons.Default.Launch, actionTint, onLaunchApp)
                HorizontalDivider(color = dividerColor)
            }
            ActionRow(if (hasApp) "CHANGE APP" else "ASSIGN APP", Icons.Default.SwapHoriz, actionTint, onChangeApp)
            HorizontalDivider(color = dividerColor)
            ActionRow("CUSTOMIZE ICON", Icons.Default.Palette, actionTint, onCustomizeIcon)
            HorizontalDivider(color = dividerColor)
            ActionRow("REMOVE", Icons.Default.Delete, removeTint, onRemove)
        }
    }
}

@Composable
private fun ActionRow(label: String, icon: ImageVector, tint: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
        Text(label, color = tint, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
    }
}

@Composable
private fun IconPickerDialog(
    accent: Color,
    hasNativeIcon: Boolean,
    currentOverride: DefaultShortcutIcon?,
    onPick: (DefaultShortcutIcon?) -> Unit,
    onDismiss: () -> Unit
) {
    val isDayMode = LocalDayMode.current
    val accentLum = 0.299f * accent.red + 0.587f * accent.green + 0.114f * accent.blue
    val effectiveAccent = if (isDayMode && accentLum > 0.65f) Color(0xFF111111)
                          else if (!isDayMode && accentLum < 0.25f) Color(0xFFEEEEEE)
                          else accent

    val dialogBg = if (isDayMode) Color(0xFFFFFFFF) else Color(0xFF111111)
    val dialogBorder = if (isDayMode) Color(0xFFDDE1E5) else Color(0xFF1E1E1E)
    val headerColor = if (isDayMode) Color(0xFF495057) else Color(0xFF888888)
    val dividerColor = if (isDayMode) Color(0xFFF1F3F5) else Color(0xFF1A1A1A)
    val unselectedBoxBg = if (isDayMode) Color(0xFFF8F9FA) else Color(0xFF1A1A1A)
    val unselectedBoxBorder = if (isDayMode) Color(0xFFCED4DA) else Color(0xFF282828)
    val unselectedIconTint = if (isDayMode) Color(0xFF495057) else Color(0xFF888888)

    val vectorOptions = DefaultShortcutIcon.entries.filter { it != DefaultShortcutIcon.NONE }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(dialogBg)
                .border(1.dp, dialogBorder, RoundedCornerShape(8.dp))
                .padding(14.dp)
        ) {
            Text(
                "CHOOSE ICON",
                color         = headerColor,
                fontSize      = 9.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier      = Modifier.padding(bottom = 10.dp)
            )

            if (hasNativeIcon) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (currentOverride == null) effectiveAccent.copy(alpha = if (isDayMode) 0.10f else 0.18f) else Color.Transparent)
                        .border(1.dp, if (currentOverride == null) effectiveAccent else Color.Transparent, RoundedCornerShape(6.dp))
                        .clickable { onPick(null) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.Apps,
                        null,
                        tint = if (currentOverride == null) effectiveAccent else unselectedIconTint,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "NATIVE APP ICON",
                        color         = if (currentOverride == null) effectiveAccent else (if (isDayMode) Color(0xFF333333) else Color(0xFF888888)),
                        fontSize      = 9.sp,
                        fontWeight    = if (currentOverride == null) FontWeight.Bold else FontWeight.Normal,
                        letterSpacing = 1.sp
                    )
                }
                HorizontalDivider(color = dividerColor, modifier = Modifier.padding(vertical = 8.dp))
            }

            LazyVerticalGrid(
                columns               = GridCells.Fixed(5),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement   = Arrangement.spacedBy(6.dp),
                modifier              = Modifier.heightIn(max = 300.dp)
            ) {
                items(vectorOptions) { iconOption ->
                    val isSelected = currentOverride == iconOption
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSelected) effectiveAccent.copy(alpha = if (isDayMode) 0.12f else 0.22f)
                                else unselectedBoxBg
                            )
                            .border(
                                1.dp,
                                if (isSelected) effectiveAccent else unselectedBoxBorder,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { onPick(iconOption) }
                    ) {
                        Icon(
                            imageVector        = iconOption.toIcon(),
                            contentDescription = iconOption.name,
                            tint               = if (isSelected) effectiveAccent else unselectedIconTint,
                            modifier           = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    accent: Color,
    iconInactive: Color,
    isHorizontal: Boolean = false,
    slotSize: Dp = 52.dp,
    iconSize: Dp = 22.dp,
    onClick: () -> Unit
) {
    val isDayMode = LocalDayMode.current
    val activeIconColor = if (isDayMode) Color(0xFF111111) else Color.White
    val activeBg = if (isDayMode) Color(0xFF000000).copy(alpha = 0.08f) else Color.White.copy(alpha = 0.06f)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .then(
                if (isHorizontal) Modifier.fillMaxHeight().width(slotSize)
                else              Modifier.fillMaxWidth().height(slotSize)
            )
            .background(if (isActive) activeBg else Color.Transparent)
            .clickable(onClick = onClick)
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = label,
            tint               = if (isActive) activeIconColor else iconInactive,
            modifier           = Modifier.size(iconSize)
        )
    }
}

fun DefaultShortcutIcon.toIcon(): ImageVector = when (this) {
    // Navigation & vehicle
    DefaultShortcutIcon.RADIO       -> Icons.Default.Radio
    DefaultShortcutIcon.CAMERA      -> Icons.Default.CameraAlt
    DefaultShortcutIcon.PHONE       -> Icons.Default.Phone
    DefaultShortcutIcon.MAP         -> Icons.Default.Map
    DefaultShortcutIcon.NAVIGATION  -> Icons.Default.Navigation
    DefaultShortcutIcon.CAR         -> Icons.Default.DirectionsCar
    DefaultShortcutIcon.GAS_STATION -> Icons.Default.LocalGasStation
    DefaultShortcutIcon.DASHBOARD   -> Icons.Default.Speed
    DefaultShortcutIcon.PARKING     -> Icons.Default.LocalParking
    DefaultShortcutIcon.EV_STATION  -> Icons.Default.EvStation
    DefaultShortcutIcon.SPEEDOMETER -> Icons.Default.Speed
    DefaultShortcutIcon.COMPASS     -> Icons.Default.Explore
    DefaultShortcutIcon.EXPLORE     -> Icons.Default.Explore
    // Audio & media
    DefaultShortcutIcon.MUSIC       -> Icons.Default.MusicNote
    DefaultShortcutIcon.SPEAKER     -> Icons.Default.Speaker
    DefaultShortcutIcon.HEADSET     -> Icons.Default.Headset
    DefaultShortcutIcon.EQUALIZER   -> Icons.Default.Equalizer
    DefaultShortcutIcon.VOLUME_UP   -> Icons.Default.VolumeUp
    DefaultShortcutIcon.PLAY        -> Icons.Default.PlayArrow
    DefaultShortcutIcon.PAUSE       -> Icons.Default.Pause
    DefaultShortcutIcon.MIC         -> Icons.Default.Mic
    // Connectivity & utility
    DefaultShortcutIcon.BLUETOOTH   -> Icons.Default.Bluetooth
    DefaultShortcutIcon.WIFI        -> Icons.Default.Wifi
    DefaultShortcutIcon.USB         -> Icons.Default.Usb
    DefaultShortcutIcon.CLOUD       -> Icons.Default.Cloud
    DefaultShortcutIcon.STORE       -> Icons.Default.Store
    DefaultShortcutIcon.GAMEPAD     -> Icons.Default.Gamepad
    DefaultShortcutIcon.CALCULATOR  -> Icons.Default.Calculate
    DefaultShortcutIcon.NOTIFICATIONS -> Icons.Default.Notifications
    DefaultShortcutIcon.REFRESH     -> Icons.Default.Refresh
    // Lighting & climate
    DefaultShortcutIcon.LIGHTBULB   -> Icons.Default.Lightbulb
    DefaultShortcutIcon.BRIGHTNESS  -> Icons.Default.BrightnessHigh
    DefaultShortcutIcon.AC          -> Icons.Default.AcUnit
    DefaultShortcutIcon.THERMOSTAT  -> Icons.Default.Thermostat
    // General utility & car
    DefaultShortcutIcon.TV          -> Icons.Default.Tv
    DefaultShortcutIcon.VIDEOCAM    -> Icons.Default.Videocam
    DefaultShortcutIcon.STAR        -> Icons.Default.Star
    DefaultShortcutIcon.MESSAGE     -> Icons.Default.Message
    DefaultShortcutIcon.TIMER       -> Icons.Default.Timer
    DefaultShortcutIcon.LOCK        -> Icons.Default.Lock
    DefaultShortcutIcon.SETTINGS    -> Icons.Default.Settings
    DefaultShortcutIcon.FAVORITE    -> Icons.Default.Favorite
    DefaultShortcutIcon.BATTERY     -> Icons.Default.BatteryFull
    DefaultShortcutIcon.POWER       -> Icons.Default.PowerSettingsNew
    DefaultShortcutIcon.SHIELD      -> Icons.Default.Shield
    DefaultShortcutIcon.WRENCH      -> Icons.Default.Build
    DefaultShortcutIcon.WARNING     -> Icons.Default.Warning
    DefaultShortcutIcon.INFO        -> Icons.Default.Info
    // Web / location
    DefaultShortcutIcon.GLOBE       -> Icons.Default.Language
    DefaultShortcutIcon.NONE        -> Icons.Default.Apps
}
