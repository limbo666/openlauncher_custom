package com.openlauncher.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Bundle
import android.os.SystemClock
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.openlauncher.app.data.DayNightMode
import com.openlauncher.app.data.SidebarPosition
import com.openlauncher.app.data.GradientDirection
import com.openlauncher.app.data.AppFont
import com.openlauncher.app.data.ParticleEffect
import com.openlauncher.app.data.telegram.IncomingTelegramMessage
import com.openlauncher.app.data.telegram.TelegramBot
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val vm: LauncherViewModel by viewModels()
    private var autoLaunchDone = false

    // Bump when the setup wizard gets a meaningful revamp — forces it to show
    // once on installs that already completed an older wizard.
    private val ONBOARDING_WIZARD_VERSION = 2

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

            // ── Telegram remote messages ──────────────────────────────────────
            var telegramBanner by remember { mutableStateOf<TgBanner?>(null) }
            var telegramWarn by remember { mutableStateOf<TgWarn?>(null) }
            var telegramTrackJob by remember { mutableStateOf<Job?>(null) }
            val tgScope = rememberCoroutineScope()
            val tgSettings by rememberUpdatedState(settings)
            val tgLocation by rememberUpdatedState(location)
            val tgBearing by rememberUpdatedState(bearing)

            fun tgSend(chatId: Long, text: String) {
                val token = tgSettings.telegramBotToken
                tgScope.launch {
                    if (TelegramBot.send(token, chatId, text)) TelegramBot.log("→ ${text.take(48)}")
                    else TelegramBot.log("Send failed")
                }
            }

            fun tgAck(chatId: Long) {
                if (tgSettings.telegramReadReceipts) tgSend(chatId, "✅ Read")
            }

            fun startTrack(chatId: Long, minutes: Int) {
                telegramTrackJob?.cancel()
                telegramTrackJob = tgScope.launch {
                    val endAt = SystemClock.elapsedRealtime() + minutes * 60_000L
                    tgSend(chatId, "🛰 Tracking for $minutes min")
                    while (SystemClock.elapsedRealtime() < endAt) {
                        val loc = tgLocation
                        if (loc != null) {
                            val spd = loc.speedMps * 3.6f
                            val dir = bearingToCompass(tgBearing)
                            tgSend(chatId, "🛰 %.0f km/h · %s · %.5f, %.5f\nhttps://maps.google.com/?q=%.5f,%.5f"
                                .format(spd, dir, loc.latitude, loc.longitude, loc.latitude, loc.longitude))
                        }
                        delay(60_000L)
                    }
                    telegramTrackJob = null
                    tgSend(chatId, "✅ Tracking finished")
                }
            }

            fun handleTelegram(m: IncomingTelegramMessage) {
                val s = tgSettings
                if (!s.telegramEnabled) return
                val entries = s.telegramAllowedSenders.split(',')
                    .map { it.trim() }.filter { it.isNotEmpty() }
                val allowAll = entries.any { it.equals("@all", true) || it.equals("all", true) || it == "*" }
                val allowed = allowAll || entries.any { e ->
                    if (e.startsWith("@")) {
                        m.fromUsername.isNotEmpty() && e.substring(1).equals(m.fromUsername, ignoreCase = true)
                    } else {
                        e.toLongOrNull() == m.fromId
                    }
                }
                if (!allowed) {
                    TelegramBot.log("Ignored ${m.fromName} @${m.fromUsername} (not allowed)")
                    return
                }
                val ageMin = (System.currentTimeMillis() / 1000 - m.dateSec) / 60.0
                if (ageMin > s.telegramLookbackHours * 60.0) {
                    TelegramBot.log("Ignored old message from ${m.fromName}")
                    return
                }
                TelegramBot.log("← ${m.fromName}: ${m.text.take(48)}")
                val cmd = m.text.trim()
                when {
                    s.telegramCommandWarn && cmd.startsWith("WARN", ignoreCase = true) -> {
                        val t = cmd.substringAfter(" ", "").ifBlank { cmd }
                        telegramWarn = TgWarn(m.chatId, t, SystemClock.elapsedRealtime())
                    }
                    s.telegramCommandClear && cmd.equals("CLEAR", ignoreCase = true) -> {
                        telegramTrackJob?.cancel(); telegramTrackJob = null
                        telegramBanner = null; telegramWarn = null
                        tgSend(m.chatId, "✅ Cleared")
                    }
                    s.telegramCommandGps && cmd.equals("GPS", ignoreCase = true) -> {
                        val loc = tgLocation
                        if (loc == null) tgSend(m.chatId, "📍 No GPS fix yet")
                        else tgSend(m.chatId, "📍 %.5f, %.5f\nhttps://maps.google.com/?q=%.5f,%.5f"
                            .format(loc.latitude, loc.longitude, loc.latitude, loc.longitude))
                    }
                    s.telegramCommandTrack && cmd.startsWith("TRACK", ignoreCase = true) -> {
                        val mins = cmd.substringAfter(" ", "3").trim().toIntOrNull()?.coerceIn(1, 60) ?: 3
                        startTrack(m.chatId, mins)
                    }
                    s.telegramCommandStatus && cmd.equals("STATUS", ignoreCase = true) -> {
                        val loc = tgLocation
                        val spd = (loc?.speedMps ?: 0f) * 3.6f
                        val profile = s.profileNames.getOrElse(s.activeProfile) { "Driver ${s.activeProfile + 1}" }
                        val batt = batteryLevel(this@MainActivity)
                        val uptime = SystemClock.elapsedRealtime() / 60_000L
                        val locStr = loc?.let { "%.5f, %.5f".format(it.latitude, it.longitude) } ?: "no fix"
                        tgSend(m.chatId, "🚗 Status\nSpeed: %.0f km/h\nGPS: %s\nProfile: %s\nBattery: %d%%\nUptime: %d min"
                            .format(spd, locStr, profile, batt, uptime))
                    }
                    else -> telegramBanner = TgBanner(m.chatId, m.fromName, m.text, SystemClock.elapsedRealtime())
                }
            }

            // Register the listener once, then keep polling in sync with settings
            LaunchedEffect(Unit) { TelegramBot.listener = { handleTelegram(it) } }
            LaunchedEffect(settings.telegramEnabled, settings.telegramBotToken) {
                if (settings.telegramEnabled && settings.telegramBotToken.isNotBlank()) {
                    TelegramBot.start(settings.telegramBotToken)
                } else {
                    TelegramBot.stop()
                }
            }
            // Auto-dismiss the transient banner (read receipt on timeout)
            LaunchedEffect(telegramBanner, settings.telegramAutoDismiss, settings.telegramDismissSeconds) {
                val b = telegramBanner ?: return@LaunchedEffect
                if (settings.telegramAutoDismiss) {
                    delay(settings.telegramDismissSeconds * 1000L)
                    if (telegramBanner == b) { telegramBanner = null; tgAck(b.chatId) }
                }
            }

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
                // Wizard gate: shows on a fresh install (default false), and once
                // more on upgraded installs whenever the wizard itself was revamped
                // (onboardingVersion below the current wizard version).
                if (!settings.onboardingCompleted || settings.onboardingVersion < ONBOARDING_WIZARD_VERSION) {
                    OnboardingScreen(
                        accent = accent,
                        onComplete = {
                            vm.updateSettings {
                                copy(
                                    onboardingCompleted = true,
                                    onboardingVersion = ONBOARDING_WIZARD_VERSION
                                )
                            }
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
                                        onSetSpeedometerAnimals = { animals -> vm.setSpeedometerAnimals(page, animals) },
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
                                        onSetMapType      = { type -> vm.updateSettings { copy(mapWidgetType = type) } },
                                        onToggleMapTraffic = { vm.updateSettings { copy(mapShowTraffic = !mapShowTraffic) } },
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
                                        onOpenTelegram = { vm.navigate(NavDestination.TELEGRAM) },
                                        onSwitchProfile = { id -> vm.switchProfile(id) },
                                        apps = apps
                                    )

                                    NavDestination.TELEGRAM -> TelegramSettingsScreen(
                                        settings = settings,
                                        accent   = accent,
                                        onUpdate = { block -> vm.updateSettings(block) },
                                        onBack   = { vm.navigate(NavDestination.SETTINGS) },
                                        isOnline = isOnline
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

                        // ── Telegram message overlay (above everything) ─────────
                        if (telegramWarn != null) {
                            val w = telegramWarn!!
                            val elapsed = SystemClock.elapsedRealtime() - w.atMs
                            val canDismiss = elapsed >= 30_000L
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 12.dp)
                                    .widthIn(max = 640.dp)
                                    .zIndex(1200f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xE6B71C1C))
                                    .padding(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "⚠ WARNING",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.5.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        if (canDismiss) "DISMISS" else "${((30_000 - elapsed) / 1000 + 1)}s",
                                        color = if (canDismiss) Color.White else Color(0xFFFFB4AB),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable(enabled = canDismiss) {
                                                telegramWarn = null; tgAck(w.chatId)
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                                Text(w.text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (telegramBanner != null && telegramWarn == null) {
                            val b = telegramBanner!!
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 12.dp)
                                    .widthIn(max = 640.dp)
                                    .zIndex(1200f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xE61A1A1A))
                                    .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        b.from.uppercase(),
                                        color = accent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        "✕",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable { telegramBanner = null; tgAck(b.chatId) }
                                            .padding(6.dp)
                                    )
                                }
                                Text(b.text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                if (tgSettings.telegramQuickReplies.isNotEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                    QuickRepliesRow(tgSettings.telegramQuickReplies, accent) { r ->
                                        telegramBanner = null
                                        tgSend(b.chatId, r)
                                    }
                                }
                            }
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

// ── Telegram overlay state + helpers ─────────────────────────────────────────

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun QuickRepliesRow(replies: List<String>, accent: Color, onReply: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement   = Arrangement.spacedBy(6.dp)
    ) {
        replies.take(6).forEach { r ->
            Text(
                r,
                color = Color.White,
                fontSize = 11.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(accent.copy(alpha = 0.85f))
                    .clickable { onReply(r) }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}

private data class TgBanner(val chatId: Long, val from: String, val text: String, val atMs: Long)
private data class TgWarn(val chatId: Long, val text: String, val atMs: Long)

private fun bearingToCompass(b: Float): String {
    val dirs = listOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
    val idx = ((((b % 360f) + 360f) % 360f) / 22.5f).toInt() % 16
    return dirs[idx]
}

private fun batteryLevel(ctx: android.content.Context): Int = runCatching {
    val bm = ctx.getSystemService(android.content.Context.BATTERY_SERVICE) as? BatteryManager
    bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
}.getOrDefault(-1)
