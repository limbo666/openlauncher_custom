package com.openlauncher.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.openlauncher.app.data.DayNightMode
import com.openlauncher.app.data.SidebarPosition
import com.openlauncher.app.data.GradientDirection
import com.openlauncher.app.data.AppFont
import com.openlauncher.app.data.ParticleEffect
import com.openlauncher.app.model.NavDestination
import com.openlauncher.app.service.FloatingPanelService
import com.openlauncher.app.service.MediaListenerService
import com.openlauncher.app.ui.components.CarDriveOverlay
import com.openlauncher.app.ui.components.ParticleOverlay
import com.openlauncher.app.ui.components.RadioChangePopup
import com.openlauncher.app.ui.components.RetroDisplayOverlay
import com.openlauncher.app.ui.components.Sidebar
import com.openlauncher.app.ui.components.VideoWallpaperLayer
import com.openlauncher.app.ui.screen.*
import com.openlauncher.app.ui.theme.OpenLauncherTheme
import com.openlauncher.app.ui.theme.RetroBackground
import com.openlauncher.app.ui.theme.RetroPrimary
import com.openlauncher.app.util.TapSound
import com.openlauncher.app.util.VolumeController
import com.openlauncher.app.viewmodel.LauncherViewModel

class MainActivity : ComponentActivity() {

    private val vm: LauncherViewModel by viewModels()
    private var autoLaunchDone = false

    // Compose state held by the activity: whether the launcher is currently in
    // the foreground. The video wallpaper pauses itself while false (onStop).
    private val launcherInForeground = mutableStateOf(true)

    private val locationPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            vm.startLocationUpdates()
        }
    }

    private fun checkAndRequestLocationPermission() {
        val fineGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) {
            locationPermissions.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            vm.startLocationUpdates()
        }
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
            vm.startLocationUpdates()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()
        checkAndRequestLocationPermission()

        setContent {
            val settingsLoaded by vm.settingsLoaded.collectAsStateWithLifecycle()
            val settings       by vm.settings.collectAsStateWithLifecycle()
            val nav            by vm.nav.collectAsStateWithLifecycle()
            val apps        by vm.apps.collectAsStateWithLifecycle()
            val appsLoading by vm.appsLoading.collectAsStateWithLifecycle()
            val nowPlaying  by vm.nowPlaying.collectAsStateWithLifecycle()
            val weather     by vm.weather.collectAsStateWithLifecycle()
            val weatherError by vm.weatherError.collectAsStateWithLifecycle()
            val isWeatherRefreshing by vm.isWeatherRefreshing.collectAsStateWithLifecycle()
            val location    by vm.location.collectAsStateWithLifecycle()
            val bearing     by vm.compassBearing.collectAsStateWithLifecycle()
            val isWifi      by vm.isWifi.collectAsStateWithLifecycle()
            val isData      by vm.isData.collectAsStateWithLifecycle()
            val isOnline    by vm.isOnline.collectAsStateWithLifecycle()
            val isBluetoothOn by vm.isBluetoothOn.collectAsStateWithLifecycle()
            val isBluetoothConnected by vm.isBluetoothConnected.collectAsStateWithLifecycle()
            val isDayModeVM by vm.isDayMode.collectAsStateWithLifecycle()
            val isScreenBlanked by vm.isScreenBlanked.collectAsStateWithLifecycle()
            val hardwareRadio by vm.hardwareRadio.collectAsStateWithLifecycle()
            val radioPopup by vm.radioPopup.collectAsStateWithLifecycle()
            val mediaSessions by MediaListenerService.sessions.collectAsStateWithLifecycle()
            val rearrangeMode by vm.rearrangeMode.collectAsStateWithLifecycle()
            val carPhase by vm.carPhase.collectAsStateWithLifecycle()
            val networkSpeed by vm.networkSpeed.collectAsStateWithLifecycle()
            val soundSpectrum by vm.soundSpectrum.collectAsStateWithLifecycle()
            val soundVisualizerAvailable by vm.soundVisualizerAvailable.collectAsStateWithLifecycle()
            val soundVisualizerHasSignal by vm.soundVisualizerHasSignal.collectAsStateWithLifecycle()
            val soundVisualizerStatus by vm.soundVisualizerStatus.collectAsStateWithLifecycle()
            val systemIsDark = isSystemInDarkTheme()
            val isCustom = settings.dayNightMode == DayNightMode.CUSTOM
            // Retro LCD easter egg: force the dark phosphor look regardless of
            // the day/night setting — the green-on-black palette is inherently dark.
            val retroOn = settings.retroThemeEnabled
            val isDayMode = if (retroOn) false
                else if (settings.dayNightMode == DayNightMode.SYSTEM) !systemIsDark else isDayModeVM
            val appPickerTarget by vm.appPickerTarget.collectAsStateWithLifecycle()
            val profileSwitchedAt by vm.profileSwitchedAt.collectAsStateWithLifecycle()

            LaunchedEffect(settings.volumeProtocol) {
                VolumeController.setProtocol(settings.volumeProtocol)
            }

            // Auto-launch a chosen app once per session, after a configurable delay.
            LaunchedEffect(settingsLoaded) {
                if (settingsLoaded && !autoLaunchDone && settings.autoLaunchEnabled && settings.autoLaunchPackage.isNotEmpty()) {
                    autoLaunchDone = true
                    kotlinx.coroutines.delay(settings.autoLaunchDelaySec * 1000L)
                    vm.launchApp(settings.autoLaunchPackage)
                }
            }

            // Keep the tap-sound util in sync with settings.
            LaunchedEffect(settings.tapSoundEnabled, settings.tapSoundType) {
                TapSound.enabled = settings.tapSoundEnabled
                TapSound.type = settings.tapSoundType
            }

            // Start/stop the floating quick-controls service with its toggle.
            LaunchedEffect(settings.floatingPanelEnabled) {
                if (settings.floatingPanelEnabled) {
                    FloatingPanelService.start(this@MainActivity)
                } else {
                    FloatingPanelService.stop(this@MainActivity)
                }
            }

            // RECORD_AUDIO enables the spectrum visualizer's master-audio capture
            // (REMOTE_SUBMIX) and the output-mix fallback on some ROMs.
            val recordAudioLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted -> vm.setRecordAudioGranted(granted) }
            LaunchedEffect(settings.showSpectrum) {
                if (settings.showSpectrum) {
                    val granted = ContextCompat.checkSelfPermission(
                        this@MainActivity, Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    vm.setRecordAudioGranted(granted)
                    if (!granted) recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                } else {
                    vm.setRecordAudioGranted(false)
                }
            }

            // System Back on any non-home screen returns to the launcher home page.
            BackHandler(enabled = nav != NavDestination.HOME) {
                vm.navigate(NavDestination.HOME)
            }

            val accent         = if (retroOn) RetroPrimary else Color(settings.accentColor)
            val bg             = if (retroOn) {
                RetroBackground
            } else if (isCustom && settings.useCustomBackgroundColor) {
                Color(settings.backgroundColor)
            } else {
                if (isDayMode) Color(0xFFEEEEEE) else Color.Black
            }
            val textColor      = if (retroOn) {
                Color.White
            } else if (isDayMode) {
                Color(0xFF111111)
            } else if (isCustom) {
                Color(settings.fontColor)
            } else {
                Color.White
            }
            val bgGradientEnd  = Color(settings.gradientEndColor)
            val bgBrush        = if (isCustom && settings.useCustomBackgroundColor && settings.useGradient) {
                val colors = listOf(bg, bgGradientEnd)
                when (settings.gradientDirection) {
                    GradientDirection.TOP_TO_BOTTOM -> androidx.compose.ui.graphics.Brush.verticalGradient(colors)
                    GradientDirection.LEFT_TO_RIGHT -> androidx.compose.ui.graphics.Brush.horizontalGradient(colors)
                    GradientDirection.DIAGONAL -> androidx.compose.ui.graphics.Brush.linearGradient(colors)
                    GradientDirection.RADIAL -> androidx.compose.ui.graphics.Brush.radialGradient(colors)
                }
            } else null

            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density   = baseDensity.density * settings.uiScale,
                    fontScale = baseDensity.fontScale
                )
            ) {
                if (!settingsLoaded) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black))
                } else OpenLauncherTheme(
                    accent     = accent,
                    background = bg,
                    textColor  = textColor,
                    fontBold   = settings.fontBold,
                    textScale  = settings.textScale,
                    appFont    = if (retroOn) AppFont.IBM_VGA_9X16 else settings.appFont,
                    isDayMode  = isDayMode,
                    isCustom   = isCustom,
                    useCustomBg = isCustom && settings.useCustomBackgroundColor && !retroOn,
                    helpTextColor = if (retroOn) null else settings.helpTextColor?.let { Color(it) }
                ) {
                if (!settings.onboardingCompleted) {
                    OnboardingScreen(
                        accent = accent,
                        onComplete = {
                            vm.updateSettings { copy(onboardingCompleted = true) }
                            // Start location updates immediately upon completion
                            vm.startLocationUpdates()
                        }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().let { m ->
                        if (bgBrush != null) m.background(bgBrush) else m.background(bg)
                    }) {
                        // Optional wallpaper layer (static image — also the fallback
                        // behind the video wallpaper if the video can't decode)
                        if (settings.wallpaperUri.isNotEmpty()) {
                            AsyncImage(
                                model              = android.net.Uri.parse(settings.wallpaperUri),
                                contentDescription = null,
                                contentScale       = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier           = Modifier.fillMaxSize()
                            )
                            Box(modifier = Modifier.fillMaxSize()
                                .background(Color.Black.copy(alpha = settings.wallpaperDim)))
                        }

                        // Video wallpaper (muted, looping) — drawn above the static
                        // image; hidden in retro mode where the CRT look takes over.
                        if (settings.wallpaperVideoUri.isNotEmpty() && !retroOn) {
                            VideoWallpaperLayer(
                                videoUri = settings.wallpaperVideoUri,
                                dim      = settings.wallpaperDim,
                                visible  = launcherInForeground.value,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        val isBottomBar    = settings.sidebarPosition == SidebarPosition.BOTTOM
                        val layoutDivColor = if (isDayMode) Color(0xFFCCCCCC) else Color(0xFF1A1A1A)

                        val sidebarContent: @Composable () -> Unit = {
                            val sidebarDensity = Density(
                                density = baseDensity.density * (1.0f + (settings.uiScale - 1.0f) * 0.35f),
                                fontScale = baseDensity.fontScale
                            )
                            CompositionLocalProvider(LocalDensity provides sidebarDensity) {
                                Sidebar(
                                    currentDest   = nav,
                                    settings      = settings,
                                    isHorizontal  = isBottomBar,
                                    apps          = apps,
                                    installedIconFor = { pkg ->
                                        apps.find { it.packageName == pkg }?.icon
                                    },
                                    onNavigate    = { dest ->
                                        vm.cancelCarPlayPicker()
                                        vm.exitRearrangeMode()
                                        vm.navigate(dest)
                                    },
                                    onShortcutClick = { slot ->
                                        val shortcut = settings.shortcuts.getOrNull(slot)
                                        if (shortcut != null && shortcut.packageName.isNotEmpty()) {
                                            vm.launchApp(shortcut.packageName)
                                        }
                                    },
                                    onShortcutAssignApp = { slot, pkg ->
                                        apps.find { it.packageName == pkg }?.let { vm.assignShortcut(slot, it) }
                                    },
                                    onShortcutRemove     = { slot -> vm.removeShortcut(slot) },
                                    onShortcutSetIcon    = { slot, icon -> vm.setShortcutIcon(slot, icon) },
                                    onReorder            = { from, to -> vm.reorderShortcut(from, to) }
                                )
                            }
                        }

                        val mainPane: @Composable (Modifier) -> Unit = { paneModifier ->
                            // ── Main content pane ─────────────────────────────
                            AnimatedContent(
                                targetState   = nav,
                                transitionSpec = {
                                    fadeIn() + slideInHorizontally { it / 10 } togetherWith
                                    fadeOut() + slideOutHorizontally { -it / 10 }
                                },
                                modifier = paneModifier,
                                label    = "pane_transition"
                            ) { destination ->
                                when (destination) {
                                    NavDestination.VOLUME_CALIBRATION -> VolumeCalibrationScreen(
                                        currentProtocol = settings.volumeProtocol,
                                        accent = accent,
                                        isDayMode = isDayMode,
                                        onSelectProtocol = { id -> vm.setVolumeProtocol(id) },
                                        onBack = { vm.navigate(NavDestination.SETTINGS) }
                                    )

                                    NavDestination.HOME -> {
                                        val pagerState = rememberPagerState(initialPage = settings.homePage, pageCount = { 2 })
                                        LaunchedEffect(settings.homePage) {
                                            if (pagerState.currentPage != settings.homePage) {
                                                pagerState.animateScrollToPage(settings.homePage)
                                            }
                                        }
                                        LaunchedEffect(pagerState) {
                                            snapshotFlow { pagerState.currentPage }.collect { page ->
                                                if (page != settings.homePage) vm.setHomePage(page)
                                            }
                                        }
                                        Box(modifier = Modifier.fillMaxSize()) {
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            HorizontalPager(
                                                state = pagerState,
                                                userScrollEnabled = !rearrangeMode,
                                                modifier = Modifier.weight(1f).fillMaxWidth()
                                            ) { page ->
                                        HomeScreen(
                                        settings            = settings,
                                        weather             = weather,
                                        nowPlaying          = nowPlaying,
                                        location            = location,
                                        bearing             = bearing,
                                        isWifi              = isWifi,
                                        isData              = isData,
                                        isOnline            = isOnline,
                                        isBluetoothOn       = isBluetoothOn,
                                        isBluetoothConnected = isBluetoothConnected,
                                        isDayMode           = isDayMode,
                                        isWeatherRefreshing = isWeatherRefreshing,
                                        onRefreshWeather    = { vm.refreshWeather() },
                                        onPlayPause         = vm::playPause,
                                        onNext              = vm::skipNext,
                                        onPrev              = vm::skipPrev,
                                        onLaunchCarPlay     = { vm.launchApp(settings.carPlayPackage) },
                                        onLaunchAndroidAuto = { vm.launchApp(settings.androidAutoPackage) },
                                        onAssignCarPlay     = { vm.startCarPlayPicker() },
                                        onAssignAndroidAuto = { vm.startAndroidAutoPicker() },
                                        onClearCarPlay      = { vm.clearCarPlayApp() },
                                        onClearAndroidAuto  = { vm.clearAndroidAutoApp() },
                                        onAssignPip         = { vm.startPipPicker() },
                                        onClearPip          = { vm.clearPipApp() },
                                        onLaunchPip         = { vm.launchApp(settings.pipAppPackage) },
                                        onTapNowPlaying     = {
                                            val pkg = nowPlaying?.controller?.packageName
                                            if (!pkg.isNullOrEmpty()) vm.launchApp(pkg)
                                        },
                                        widgetLayoutOverride = if (page == 0) settings.widgetLayout else settings.homePageAltLayout,
                                        isAltPage          = page == 1,
                                        onUpdateWidget      = { id, sx, sy -> vm.updateWidgetConfig(page, id, sx, sy) },
                                        onMoveWidget        = { id, gx, gy -> vm.moveWidgetConfig(page, id, gx, gy) },
                                        onAddWidget         = { id -> vm.addWidget(page, id) },
                                        onRemoveWidget      = { id -> vm.removeWidget(page, id) },
                                        onSetClockStyle     = { style -> vm.setClockStyle(page, style) },
                                        onSetVitalsAsBars   = { asBars -> vm.setVitalsAsBars(page, asBars) },
                                        onSetSpeedometerDigitalOnly = { digital -> vm.setSpeedometerDigitalOnly(page, digital) },
                                         onSetAppShortcutCount = { count -> vm.setAppShortcutCount(page, count) },
                                         onSetSoundboardButtonCount = { count -> vm.setSoundboardButtonCount(page, count) },
                                        onUpdateSoundPad    = { idx, pad -> vm.updateSoundboardPad(page, idx, pad) },
                                        hardwareRadio         = hardwareRadio,
                                        onLaunchHardwareRadio = { vm.launchHardwareRadioApp() },
                                        onStopHardwareRadio   = { vm.stopHardwareRadioApp() },
                                        onRadioSeekUp         = { vm.radioSeekUp() },
                                        onRadioSeekDown       = { vm.radioSeekDown() },
                                        onRadioCycleFm        = { vm.radioCycleFm() },
                                        onRadioSwitchAm       = { vm.radioSwitchAm() },
                                        onRadioTune           = { band, freq -> vm.radioTune(band, freq) },
                                        onAssignRadio         = { vm.startRadioPicker() },
                                        isScreenBlanked       = isScreenBlanked,
                                        onToggleScreenBlanked = { vm.toggleScreenBlanked() },
                                        installedApps         = apps,
                                        onLaunchApp           = { pkg -> vm.launchApp(pkg) },
                                        onSetWidgetBgColor    = { id, color -> vm.setWidgetBgColor(page, id, color) },
                                        onSetWidgetBgMode     = { id, mode -> vm.setWidgetBgMode(page, id, mode) },
                                        onSetAllWidgetsBgColor = { color -> vm.setAllWidgetsBgColor(color) },
                                        onSetAllWidgetsBgMode  = { mode -> vm.setAllWidgetsBgMode(mode) },
                                        onSetAppShortcutPackage = { slot, pkg -> vm.setAppShortcutPackage(page, slot, pkg) },
                                        onToggleDayNight      = { vm.toggleDayNightMode() },
                                        onToggleQuickActionButton = { id -> vm.toggleQuickActionButton(page, id) },
                                        onResetQuickActionButtons = { vm.resetQuickActionButtons(page) },
                                        onToggleVitalsElement = { id -> vm.toggleVitalsElement(page, id) },
                                        onResetVitalsElements = { vm.resetVitalsElements(page) },
                                        onToggleWidgetTitle   = { id -> vm.toggleWidgetTitle(page, id) },
                                        onZeroAltimeter       = { roll, pitch -> vm.zeroAltimeter(roll, pitch) },
                                        onResetAltimeterZero  = { vm.resetAltimeterZero() },
                                        onSetAdvancedWeatherIcons = { advanced -> vm.setAdvancedWeatherIcons(advanced) },
                                        onSelectObdAdapter    = { address, name ->
                                            vm.updateSettings { copy(obdDeviceAddress = address, obdDeviceName = name) }
                                        },
                                        carImageUri           = settings.carImageUri,
                                        carRestingVisible     = settings.displayAnimation && carPhase == LauncherViewModel.CarPhase.RESTING,
                                        networkSpeed          = networkSpeed,
                                        soundSpectrum         = soundSpectrum,
                                        soundVisualizerAvailable = soundVisualizerAvailable,
                                        soundVisualizerHasSignal = soundVisualizerHasSignal,
                                        onSwitchProfile      = { id -> vm.switchProfile(id) },
                                        profileSwitchedAt    = profileSwitchedAt
                                    )
                                            }
                                            // Page indicator dots (tap to switch — safer while driving than swiping)
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                repeat(2) { page ->
                                                    val activeDot = pagerState.currentPage == page
                                                    Box(
                                                        modifier = Modifier
                                                            .padding(horizontal = 4.dp)
                                                            .size(if (activeDot) 9.dp else 7.dp)
                                                            .clip(CircleShape)
                                                            .background(if (activeDot) accent else textColor.copy(alpha = 0.3f))
                                                            .clickable { vm.setHomePage(page) }
                                                    )
                                                }
                                            }
                                        }
                                        CarDriveOverlay(
                                            carImageUri = settings.carImageUri,
                                            active = settings.displayAnimation && carPhase == LauncherViewModel.CarPhase.DRIVING,
                                            onDone = { vm.clearCarPhase() }
                                        )
                                        RadioChangePopup(
                                            event = radioPopup,
                                            enabled = settings.showRadioPopup,
                                            accent = accent,
                                            isDayMode = isDayMode,
                                            modifier = Modifier
                                                .align(Alignment.TopCenter)
                                                .padding(top = 4.dp)
                                        )
                                        }
                                    }

                                    NavDestination.APP_LIBRARY -> AppLibraryScreen(
                                        apps                = apps,
                                        isLoading           = appsLoading,
                                        isCarPlayPickerMode = appPickerTarget != null,
                                        carPlayPickerLabel  = when (appPickerTarget) {
                                            com.openlauncher.app.viewmodel.LauncherViewModel.AppPickerTarget.ANDROID_AUTO -> "CHOOSE ANDROID AUTO APP"
                                            com.openlauncher.app.viewmodel.LauncherViewModel.AppPickerTarget.PIP          -> "CHOOSE PIP APP"
                                            com.openlauncher.app.viewmodel.LauncherViewModel.AppPickerTarget.RADIO        -> "CHOOSE RADIO APP"
                                            else -> "CHOOSE CARPLAY APP"
                                        },
                                        accent              = accent,
                                        selectedAppPackages = settings.selectedAppPackages,
                                        toolsAppPackages    = settings.toolsAppPackages,
                                        currentFilter       = settings.appDrawerFilter,
                                        onFilterChange      = { filter -> vm.updateSettings { copy(appDrawerFilter = filter) } },
                                        onAppClick          = { app -> vm.launchApp(app.packageName) },
                                        onCarPlaySelect     = { app -> vm.assignPickerApp(app) },
                                        onToggleSelectedApp = vm::toggleSelectedApp,
                                        onToggleToolsApp    = vm::toggleToolsApp,
                                        onUninstallApp      = { pkg -> vm.uninstallApp(this@MainActivity, pkg) }
                                    )

                                    NavDestination.SETTINGS -> SettingsScreen(
                                        settings = settings,
                                        accent   = accent,
                                        onUpdate = { block -> vm.updateSettings(block) },
                                        onReset  = { vm.resetSettings() },
                                        onOpenDiagnostics = { vm.navigate(NavDestination.DIAGNOSTICS) },
                                        onOpenVolumeCalibration = { vm.navigate(NavDestination.VOLUME_CALIBRATION) },
                                        onSwitchProfile = { id -> vm.switchProfile(id) },
                                        apps = apps
                                    )

                                    NavDestination.DIAGNOSTICS -> DiagnosticsScreen(
                                        settings     = settings,
                                        accent       = accent,
                                        isDayMode    = isDayMode,
                                        location     = location,
                                        bearing      = bearing,
                                        isOnline     = isOnline,
                                        isWifi       = isWifi,
                                        isData       = isData,
                                        weather      = weather,
                                        weatherError = weatherError,
                                        onRefresh    = {
                                            vm.refreshConnectivity()
                                            vm.refreshBluetoothStatus()
                                            vm.startLocationUpdates()
                                            vm.refreshWeather(force = true)
                                        },
                                        onBack       = { vm.navigate(NavDestination.SETTINGS) },
                                        onOpenVolumeCalibration = { vm.navigate(NavDestination.VOLUME_CALIBRATION) },
                                        sessions = mediaSessions,
                                        apps = apps,
                                        hardwareRadio = hardwareRadio,
                                        onRadioSeekUp = { vm.radioSeekUp() },
                                        onRadioSeekDown = { vm.radioSeekDown() },
                                        visualizerStatus = soundVisualizerStatus,
                                        visualizerAvailable = soundVisualizerAvailable
                                    )
                                }
                            }
                        }

                        if (isBottomBar) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                mainPane(Modifier.weight(1f).fillMaxWidth())
                                androidx.compose.material3.HorizontalDivider(color = layoutDivColor)
                                sidebarContent()
                            }
                        } else {
                            Row(modifier = Modifier.fillMaxSize()) {
                                val vDivider: @Composable () -> Unit = {
                                    androidx.compose.material3.VerticalDivider(
                                        modifier = Modifier.fillMaxHeight(),
                                        color    = layoutDivColor
                                    )
                                }
                                if (settings.sidebarPosition == SidebarPosition.LEFT) {
                                    sidebarContent()
                                    vDivider()
                                }
                                mainPane(Modifier.weight(1f).fillMaxHeight())
                                if (settings.sidebarPosition == SidebarPosition.RIGHT) {
                                    vDivider()
                                    sidebarContent()
                                }
                            }
                        }

                        // Retro LCD easter egg: full-screen CRT overlay drawn on
                        // top of everything (draw-only — cannot intercept touches).
                        if (retroOn) {
                            RetroDisplayOverlay(
                                modifier = Modifier
                                    .matchParentSize()
                                    .zIndex(1000f)
                            )
                        }

                        // Ambient particles (snow / rain / stars / constellation) above the content
                        if (settings.particleEffect != ParticleEffect.NONE) {
                            ParticleOverlay(
                                style    = settings.particleEffect,
                                amount   = settings.particleEffectAmount,
                                accent   = accent,
                                modifier = Modifier
                                    .matchParentSize()
                                    .zIndex(900f)
                            )
                        }
                    }
                }
            }
            } // CompositionLocalProvider
        }
    }

    override fun onResume() {
        super.onResume()
        FloatingPanelService.setLauncherVisible(true)
        vm.refreshConnectivity()
        vm.refreshBluetoothStatus()
        vm.refreshMedia()
    }

    /**
     * Because this activity is the HOME app in `singleTask` mode, pressing the device Home
     * button re-delivers the HOME intent here instead of restarting the activity — so we
     * reset the internal navigation back to the launcher home page.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            vm.navigate(NavDestination.HOME)
        }
    }

    override fun onStop() {
        super.onStop()
        launcherInForeground.value = false
        FloatingPanelService.setLauncherVisible(false)
        vm.stopLocationUpdates()
    }

    override fun onStart() {
        super.onStart()
        launcherInForeground.value = true
        checkAndRequestLocationPermission()
    }
}
