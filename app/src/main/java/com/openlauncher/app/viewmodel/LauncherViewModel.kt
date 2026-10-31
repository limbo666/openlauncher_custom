package com.openlauncher.app.viewmodel

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings as AndroidSettings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.openlauncher.app.data.AppSettings
import com.openlauncher.app.data.DayNightMode
import com.openlauncher.app.data.DefaultShortcutIcon
import com.openlauncher.app.data.GRID_COLS
import com.openlauncher.app.data.GRID_ROWS
import com.openlauncher.app.data.SettingsRepository
import com.openlauncher.app.data.ShortcutConfig
import com.openlauncher.app.data.SoundPadConfig
import com.openlauncher.app.data.WeatherApi
import com.openlauncher.app.data.WeatherProvider
import com.openlauncher.app.data.WidgetBgMode
import com.openlauncher.app.data.activeWidgetIds
import com.openlauncher.app.data.computeWidgetMove
import com.openlauncher.app.data.defaultQuickActionButtons
import com.openlauncher.app.data.defaultVitalsElements
import com.openlauncher.app.data.defaultShortcuts
import com.openlauncher.app.util.SunriseSunset
import com.openlauncher.app.model.AppInfo
import com.openlauncher.app.model.NavDestination
import com.openlauncher.app.model.NowPlayingState
import com.openlauncher.app.model.WeatherState
import com.openlauncher.app.service.MediaListenerService
import com.openlauncher.app.util.FileLog
import com.openlauncher.app.util.LocationCompassManager
import com.openlauncher.app.util.LocationData
import com.openlauncher.app.util.musicStartupPackage
import com.openlauncher.app.util.selectMediaSession
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    private val locationMgr  = LocationCompassManager(application)

    // ── Settings ──────────────────────────────────────────────────────────────
    private val _settingsLoaded = MutableStateFlow(false)
    val settingsLoaded: StateFlow<Boolean> = _settingsLoaded

    val settings: StateFlow<AppSettings> = settingsRepo.settingsFlow
        .onEach { _settingsLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    init {
        viewModelScope.launch {
            settings
                .map { it.advancedWeatherIcons }
                .distinctUntilChanged()
                .drop(1)
                .collect { advanced ->
                    _weather.value = _weather.value?.remap(advanced)
                }
        }
    }

    fun updateSettings(block: AppSettings.() -> AppSettings) {
        viewModelScope.launch { settingsRepo.updateSettings { it.block() } }
    }

    fun resetSettings() {
        viewModelScope.launch { settingsRepo.resetToDefaults() }
    }

    // ── Screen Blanking / Night Blackout ───────────────────────────────────────
    private val _isScreenBlanked = MutableStateFlow(false)
    val isScreenBlanked: StateFlow<Boolean> = _isScreenBlanked
    fun toggleScreenBlanked() { _isScreenBlanked.value = !_isScreenBlanked.value }
    fun setScreenBlanked(blanked: Boolean) { _isScreenBlanked.value = blanked }

    // ── Navigation ────────────────────────────────────────────────────────────
    private val _nav = MutableStateFlow(NavDestination.HOME)
    val nav: StateFlow<NavDestination> = _nav

    fun navigate(dest: NavDestination) { _nav.value = dest }

    // ── Shortcut picker ───────────────────────────────────────────────────────
    private val _shortcutPickerSlot = MutableStateFlow<Int?>(null)
    val shortcutPickerSlot: StateFlow<Int?> = _shortcutPickerSlot

    fun startShortcutPicker(slot: Int) {
        _shortcutPickerSlot.value = slot
        _nav.value = NavDestination.APP_LIBRARY
    }

    fun assignShortcut(slot: Int, app: AppInfo) {
        updateSettings {
            copy(shortcuts = shortcuts.toMutableList().also { list ->
                while (list.size <= slot) list.add(ShortcutConfig())
                val existing = list[slot]
                list[slot] = existing.copy(
                    packageName = app.packageName,
                    label = app.appName,
                    customIconOverride = existing.customIconOverride ?: if (existing.defaultIcon != DefaultShortcutIcon.NONE) existing.defaultIcon else null
                )
            })
        }
        _shortcutPickerSlot.value = null
        _nav.value = NavDestination.HOME
    }

    fun removeShortcut(slot: Int) {
        updateSettings {
            copy(shortcuts = shortcuts.toMutableList().also { list ->
                if (slot in list.indices) {
                    list[slot] = defaultShortcuts().getOrNull(slot) ?: ShortcutConfig()
                }
            })
        }
    }

    fun reorderShortcut(from: Int, to: Int) {
        updateSettings {
            copy(shortcuts = shortcuts.toMutableList().also { list ->
                val item = list.removeAt(from)
                list.add(to.coerceIn(0, list.size), item)
            })
        }
    }

    fun setShortcutIcon(slot: Int, icon: DefaultShortcutIcon?) {
        updateSettings {
            copy(shortcuts = shortcuts.toMutableList().also { list ->
                if (slot in list.indices) list[slot] = list[slot].copy(customIconOverride = icon)
            })
        }
    }

    fun cancelShortcutPicker() {
        _shortcutPickerSlot.value = null
    }

    // ── CarPlay / Android Auto picker ─────────────────────────────────────────
    enum class AppPickerTarget { CARPLAY, ANDROID_AUTO, PIP, RADIO }

    private val _appPickerTarget = MutableStateFlow<AppPickerTarget?>(null)
    val carPlayPickerActive: StateFlow<Boolean> = MutableStateFlow(false) // kept for compat
    val appPickerTarget: StateFlow<AppPickerTarget?> = _appPickerTarget

    fun startCarPlayPicker() {
        _appPickerTarget.value = AppPickerTarget.CARPLAY
        _nav.value = NavDestination.APP_LIBRARY
    }

    fun startAndroidAutoPicker() {
        _appPickerTarget.value = AppPickerTarget.ANDROID_AUTO
        _nav.value = NavDestination.APP_LIBRARY
    }

    fun startPipPicker() {
        _appPickerTarget.value = AppPickerTarget.PIP
        _nav.value = NavDestination.APP_LIBRARY
    }

    fun startRadioPicker() {
        _appPickerTarget.value = AppPickerTarget.RADIO
        _nav.value = NavDestination.APP_LIBRARY
    }

    fun assignPickerApp(app: AppInfo) {
        when (_appPickerTarget.value) {
            AppPickerTarget.CARPLAY      -> updateSettings { copy(carPlayPackage = app.packageName) }
            AppPickerTarget.ANDROID_AUTO -> updateSettings { copy(androidAutoPackage = app.packageName) }
            AppPickerTarget.PIP          -> updateSettings { copy(pipAppPackage = app.packageName) }
            AppPickerTarget.RADIO        -> updateSettings { copy(radioPackage = app.packageName) }
            null -> {}
        }
        _appPickerTarget.value = null
        _nav.value = NavDestination.HOME
    }

    fun clearCarPlayApp()      { updateSettings { copy(carPlayPackage = "") } }
    fun clearAndroidAutoApp()  { updateSettings { copy(androidAutoPackage = "") } }
    fun clearPipApp()          { updateSettings { copy(pipAppPackage = "") } }
    fun clearRadioApp()        { updateSettings { copy(radioPackage = "") } }

    fun updateWidgetConfig(id: String, spanX: Int, spanY: Int) {
        updateSettings {
            val resized = widgetLayout.map { w ->
                if (w.id == id) w.copy(
                    spanX = spanX.coerceIn(1, GRID_COLS - w.gridX),
                    spanY = spanY.coerceIn(1, GRID_ROWS - w.gridY)
                ) else w
            }
            // Re-run collision resolution so enlarging a widget pushes neighbors
            // aside instead of stacking on top of them
            val activeIds = activeWidgetIds()
            val active    = resized.filter { it.enabled && it.id in activeIds }
            val inactive  = resized.filter { !it.enabled || it.id !in activeIds }
            val target    = active.find { it.id == id }
            copy(widgetLayout = if (target != null)
                computeWidgetMove(active, id, target.gridX, target.gridY) + inactive
            else resized)
        }
    }

    fun moveWidgetConfig(id: String, gridX: Int, gridY: Int) {
        updateSettings {
            val activeIds = activeWidgetIds()
            val active   = widgetLayout.filter { it.enabled && it.id in activeIds }
            val inactive = widgetLayout.filter { !it.enabled || it.id !in activeIds }
            copy(widgetLayout = computeWidgetMove(active, id, gridX, gridY) + inactive)
        }
    }

    fun addWidget(id: String) {
        updateSettings {
            val activeIds = activeWidgetIds()
            var layout    = widgetLayout

            val defaultSpan = when (id) {
                "VOLUME"        -> 2 to 1
                "QUICK_ACTIONS" -> 2 to 1
                "APP_SHORTCUTS" -> 2 to 1
                "OBD"           -> 2 to 2
                "NOW_PLAYING"   -> 4 to 2
                "TELEMETRY"     -> 2 to 4
                "TRIP_TRACKER"  -> 4 to 2
                "SOUNDBOARD"    -> 4 to 2
                else            -> 2 to 2
            }

            var cell = freeAreaIn(layout, activeIds, defaultSpan.first, defaultSpan.second)
                ?: freeAreaIn(layout, activeIds, 2, 1)
                ?: freeCellIn(layout, activeIds)

            // If grid is full, shrink the largest multi-cell widget by one span to make room
            if (cell == null) {
                val candidate = layout
                    .filter { it.enabled && it.id in activeIds && it.spanX * it.spanY > 1 }
                    .maxByOrNull { it.spanX * it.spanY }
                if (candidate != null) {
                    layout = layout.map { w ->
                        if (w.id == candidate.id)
                            if (w.spanY > 1) w.copy(spanY = w.spanY - 1) else w.copy(spanX = w.spanX - 1)
                        else w
                    }
                    cell = freeCellIn(layout, activeIds)
                }
            }

            val cell_ = cell ?: return@updateSettings this

            val withShow = when (id) {
                "CLOCK"         -> copy(showClock = true)
                "WEATHER"       -> copy(showWeather = true)
                "NOW_PLAYING"   -> copy(showNowPlaying = true)
                "TELEMETRY"     -> copy(showTelemetry = true)
                "ALTIMETER"     -> copy(showAltimeter = true)
                "SPEEDOMETER"   -> copy(showSpeedometer = true)
                "VITALS"        -> copy(showVitals = true)
                "TRIP_TRACKER"  -> copy(showTripTracker = true)
                "SOUNDBOARD"    -> copy(showSoundboard = true)
                "VOLUME"        -> copy(showVolume = true)
                "QUICK_ACTIONS" -> copy(showQuickActions = true)
                "APP_SHORTCUTS" -> copy(showAppShortcuts = true)
                "OBD"           -> copy(showObd = true)
                else            -> this
            }
            val idx       = layout.indexOfFirst { it.id == id }
            val newLayout = if (idx >= 0) layout.toMutableList().also { list ->
                val w = list[idx]
                // Try to keep the widget's previous span; if no free area fits it,
                // fall back to defaultSpan or 1×1 at the free cell so it can't overlap neighbors
                val area = if (w.spanX > 1 || w.spanY > 1) freeAreaIn(layout, activeIds, w.spanX, w.spanY) else null
                list[idx] = if (area != null)
                    w.copy(enabled = true, gridX = area.first, gridY = area.second)
                else
                    w.copy(
                        enabled = true,
                        gridX = cell_.first,
                        gridY = cell_.second,
                        spanX = (defaultSpan.first.coerceAtMost(GRID_COLS - cell_.first)).coerceAtLeast(1),
                        spanY = (defaultSpan.second.coerceAtMost(GRID_ROWS - cell_.second)).coerceAtLeast(1)
                    )
            } else {
                val spanX = (defaultSpan.first.coerceAtMost(GRID_COLS - cell_.first)).coerceAtLeast(1)
                val spanY = (defaultSpan.second.coerceAtMost(GRID_ROWS - cell_.second)).coerceAtLeast(1)
                layout + com.openlauncher.app.data.WidgetConfig(id, cell_.first, cell_.second, spanX, spanY)
            }
            withShow.copy(widgetLayout = newLayout)
        }
    }

    fun removeWidget(id: String) {
        updateSettings {
            when (id) {
                "CLOCK"         -> copy(showClock = false)
                "WEATHER"       -> copy(showWeather = false)
                "NOW_PLAYING"   -> copy(showNowPlaying = false)
                "TELEMETRY"     -> copy(showTelemetry = false)
                "ALTIMETER"     -> copy(showAltimeter = false)
                "SPEEDOMETER"   -> copy(showSpeedometer = false)
                "VITALS"        -> copy(showVitals = false)
                "TRIP_TRACKER"  -> copy(showTripTracker = false)
                "SOUNDBOARD"    -> copy(showSoundboard = false)
                "VOLUME"        -> copy(showVolume = false)
                "QUICK_ACTIONS" -> copy(showQuickActions = false)
                "APP_SHORTCUTS" -> copy(showAppShortcuts = false)
                "OBD"           -> copy(showObd = false)
                else            -> this
            }
        }
    }

    fun setWidgetBgColor(id: String, color: Long?) {
        updateSettings {
            copy(widgetLayout = widgetLayout.map { w ->
                if (w.id == id) w.copy(
                    customBgColor = color,
                    bgMode = if (color != null) WidgetBgMode.CUSTOM else WidgetBgMode.DEFAULT
                ) else w
            })
        }
    }

    fun setWidgetBgMode(id: String, mode: WidgetBgMode) {
        updateSettings {
            copy(widgetLayout = widgetLayout.map { w ->
                if (w.id == id) w.copy(bgMode = mode) else w
            })
        }
    }

    fun toggleWidgetTitle(id: String) {
        updateSettings {
            copy(widgetLayout = widgetLayout.map { w ->
                if (w.id == id) w.copy(showTitle = !w.isTitleVisible) else w
            })
        }
    }

    fun zeroAltimeter(currentRoll: Float, currentPitch: Float) {
        updateSettings {
            copy(altimeterRollOffset = currentRoll, altimeterPitchOffset = currentPitch)
        }
    }

    fun resetAltimeterZero() {
        updateSettings {
            copy(altimeterRollOffset = 0f, altimeterPitchOffset = 0f)
        }
    }

    fun setAppShortcutPackage(slotIndex: Int, packageName: String) {
        updateSettings {
            val list = appWidgetPackages.toMutableList()
            while (list.size <= slotIndex) list.add("")
            list[slotIndex] = packageName
            copy(appWidgetPackages = list)
        }
    }

    fun toggleQuickActionButton(actionId: String) {
        updateSettings {
            val list = (quickActionButtons.mapNotNull { (it as? Any)?.toString() }).toMutableList()
            if (actionId in list) {
                if (list.size > 1) {
                    list.remove(actionId)
                }
            } else {
                list.add(actionId)
            }
            copy(quickActionButtons = list)
        }
    }

    fun resetQuickActionButtons() {
        updateSettings {
            copy(quickActionButtons = defaultQuickActionButtons())
        }
    }

    fun toggleVitalsElement(elementId: String) {
        updateSettings {
            val list = (vitalsElements.mapNotNull { (it as? Any)?.toString() }).toMutableList()
            if (elementId in list) {
                if (list.size > 1) {
                    list.remove(elementId)
                }
            } else {
                list.add(elementId)
            }
            copy(vitalsElements = list)
        }
    }

    fun resetVitalsElements() {
        updateSettings {
            copy(vitalsElements = defaultVitalsElements())
        }
    }

    fun toggleDayNightMode() {
        updateSettings {
            val next = when (dayNightMode) {
                DayNightMode.LIGHT  -> DayNightMode.DARK
                DayNightMode.DARK   -> DayNightMode.CUSTOM
                DayNightMode.CUSTOM -> DayNightMode.LIGHT
                else                -> DayNightMode.LIGHT  // AUTO / SYSTEM → restart cycle at Light
            }
            copy(dayNightMode = next)
        }
    }

    fun updateSoundboardPad(index: Int, pad: SoundPadConfig) {
        updateSettings {
            // Persisted lists from older versions may be shorter than the 6 pads
            // the widget displays — pad before assigning to avoid IndexOutOfBounds
            val padded = soundboardPads.toMutableList()
            while (padded.size <= index) padded.add(SoundPadConfig("+", synthType = ""))
            padded[index] = pad
            copy(soundboardPads = padded)
        }
    }

    private fun freeCellIn(
        layout: List<com.openlauncher.app.data.WidgetConfig>,
        activeIds: Set<String>
    ): Pair<Int, Int>? = freeAreaIn(layout, activeIds, 1, 1)

    private fun freeAreaIn(
        layout: List<com.openlauncher.app.data.WidgetConfig>,
        activeIds: Set<String>,
        spanX: Int,
        spanY: Int
    ): Pair<Int, Int>? {
        val occupied = buildSet<Pair<Int, Int>> {
            layout.filter { it.enabled && it.id in activeIds }.forEach { w ->
                for (dx in 0 until w.spanX) for (dy in 0 until w.spanY) add(w.gridX + dx to w.gridY + dy)
            }
        }
        for (row in 0 until GRID_ROWS) for (col in 0 until GRID_COLS) {
            if (col + spanX > GRID_COLS || row + spanY > GRID_ROWS) continue
            if ((0 until spanX).all { dx -> (0 until spanY).all { dy -> (col + dx to row + dy) !in occupied } })
                return col to row
        }
        return null
    }

    fun cancelCarPlayPicker() {
        _appPickerTarget.value = null
    }

    // ── Rearrange mode ────────────────────────────────────────────────────────
    private val _rearrangeMode = MutableStateFlow(false)
    val rearrangeMode: StateFlow<Boolean> = _rearrangeMode

    fun toggleRearrangeMode() { _rearrangeMode.value = !_rearrangeMode.value }
    fun exitRearrangeMode()   { _rearrangeMode.value = false }

    // ── Installed apps ────────────────────────────────────────────────────────
    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps

    private val _appsLoading = MutableStateFlow(false)
    val appsLoading: StateFlow<Boolean> = _appsLoading

    fun loadInstalledApps() {
        if (_appsLoading.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _appsLoading.value = true
            val pm = getApplication<Application>().packageManager

            // Use getInstalledApplications — same source Android Settings uses,
            // catches apps with no launcher/ACTION_MAIN activity (e.g. CarPlay companions)
            _apps.value = pm.getInstalledApplications(0)
                .mapNotNull { appInfo ->
                    try {
                        val label = pm.getApplicationLabel(appInfo).toString()
                        if (label.isBlank()) return@mapNotNull null
                        AppInfo(
                            packageName = appInfo.packageName,
                            appName     = label,
                            icon        = pm.getApplicationIcon(appInfo),
                            isSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                        )
                    } catch (_: Exception) { null }
                }
                .distinctBy { it.packageName }
                .sortedBy { it.appName }
            _appsLoading.value = false
        }
    }

    fun launchApp(packageName: String) {
        val app = getApplication<Application>()
        val pm  = app.packageManager
        // Try standard launch intent first; fall back to first ACTION_MAIN activity in package
        val intent = pm.getLaunchIntentForPackage(packageName)
            ?: pm.queryIntentActivities(
                Intent(Intent.ACTION_MAIN).setPackage(packageName), 0
            ).firstOrNull()?.activityInfo?.let { ai ->
                Intent(Intent.ACTION_MAIN).apply {
                    setClassName(ai.packageName, ai.name)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        intent?.let { app.startActivity(it) }
    }

    // ── Now Playing ───────────────────────────────────────────────────────────
    val nowPlaying: StateFlow<NowPlayingState?> = combine(
        MediaListenerService.sessions,
        settings
    ) { sessions, s ->
        selectMediaSession(
            sessions,
            musicStartupPackage(s.preferredMediaPackage, s.lastMediaPackage),
            { it.controller?.packageName.orEmpty() },
            { it.isPlaying }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun playPause() {
        val ctrl = nowPlaying.value?.controller
        val state = ctrl?.playbackState?.state
        FileLog.log("MediaAction", "playPause pkg=${ctrl?.packageName ?: "none"} hasController=${ctrl != null} state=$state")
        ctrl?.also {
            if (state == android.media.session.PlaybackState.STATE_PLAYING)
                it.transportControls?.pause()
            else
                it.transportControls?.play()
        }
    }

    fun skipNext() {
        val ctrl = nowPlaying.value?.controller
        FileLog.log("MediaAction", "skipNext pkg=${ctrl?.packageName ?: "none"} hasController=${ctrl != null} actions=${ctrl?.playbackState?.actions}")
        ctrl?.transportControls?.skipToNext()
    }

    fun skipPrev() {
        val ctrl = nowPlaying.value?.controller
        FileLog.log("MediaAction", "skipPrev pkg=${ctrl?.packageName ?: "none"} hasController=${ctrl != null} actions=${ctrl?.playbackState?.actions}")
        ctrl?.transportControls?.skipToPrevious()
    }

    // ── Weather ───────────────────────────────────────────────────────────────
    private val _weather = MutableStateFlow<WeatherState?>(null)
    val weather: StateFlow<WeatherState?> = _weather

    private val _weatherError = MutableStateFlow<String?>(null)
    val weatherError: StateFlow<String?> = _weatherError

    private val _isWeatherRefreshing = MutableStateFlow(false)
    val isWeatherRefreshing: StateFlow<Boolean> = _isWeatherRefreshing

    private var lastRefreshEpochMs = 0L
    private var weatherJob: Job? = null

    fun fetchWeather() {
        weatherJob?.cancel()
        weatherJob = viewModelScope.launch {
            _isWeatherRefreshing.value = true
            try {
                val s = settings.value
                val loc = locationMgr.location.value
                val gpsLat = loc?.latitude
                val gpsLon = loc?.longitude
                val isDay = isDayMode.value
                val state = WeatherApi.fetchWeather(
                    useGps = s.weatherUseGps,
                    customLocation = s.weatherCustomLocation,
                    gpsLat = gpsLat,
                    gpsLon = gpsLon,
                    provider = s.weatherProvider,
                    isDay = isDay,
                    advancedIcons = s.advancedWeatherIcons
                )
                _weather.value = state.copy(isOffline = false)
                _weatherError.value = null
                _isOnline.value = true
            } catch (e: Exception) {
                android.util.Log.e("WeatherApi", "Weather fetch failed: ${e.javaClass.simpleName}: ${e.message}", e)
                _weatherError.value = e.message
                if (_weather.value != null) {
                    _weather.value = _weather.value?.copy(isOffline = true)
                } else {
                    _weather.value = WeatherState(
                        temperatureCelsius = 0.0,
                        weatherCode = 0,
                        isOffline = true
                    )
                }
            } finally {
                _isWeatherRefreshing.value = false
            }
        }
    }

    fun setWeatherUseGps(enabled: Boolean) {
        updateSettings { copy(weatherUseGps = enabled) }
        fetchWeather()
    }

    fun setWeatherCustomLocation(location: String) {
        updateSettings { copy(weatherCustomLocation = location) }
        fetchWeather()
    }

    fun setWeatherProvider(provider: WeatherProvider) {
        updateSettings { copy(weatherProvider = provider) }
        fetchWeather()
    }

    fun setWeatherForecastDays(days: Int) {
        updateSettings { copy(weatherForecastDays = days.coerceIn(3, 7)) }
    }

    fun setAdvancedWeatherIcons(enabled: Boolean) {
        updateSettings { copy(advancedWeatherIcons = enabled) }
    }

    fun refreshWeather(force: Boolean = false): Boolean {
        val now = System.currentTimeMillis()
        val isOffline = !isOnline.value || _weather.value == null || _weather.value?.isOffline == true
        val cooldownMs = if (isOffline) 5_000L else 30_000L
        if (!force && now - lastRefreshEpochMs < cooldownMs && lastRefreshEpochMs > 0L) {
            return false // Cooldown active
        }
        lastRefreshEpochMs = now
        locationMgr.refreshLocation()
        fetchWeather()
        return true
    }

    // ── Location & Compass ────────────────────────────────────────────────────
    val location: StateFlow<LocationData?> = locationMgr.location
    val compassBearing: StateFlow<Float> = locationMgr.bearing

    // Re-evaluated every minute: a parked car produces no location updates
    // (minDistance filters), so AUTO mode must also flip on time alone.
    private val minuteTicker = flow { while (true) { emit(Unit); delay(60_000L) } }

    val isDayMode: StateFlow<Boolean> = combine(settings, locationMgr.location, minuteTicker) { s, loc, _ ->
        when (s.dayNightMode) {
            DayNightMode.DARK   -> false
            DayNightMode.LIGHT  -> true
            DayNightMode.CUSTOM -> false
            DayNightMode.AUTO   -> if (loc != null) SunriseSunset.isDay(loc.latitude, loc.longitude) else false
            DayNightMode.SYSTEM -> false // placeholder — overridden in MainActivity via isSystemInDarkTheme()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun startLocationUpdates() = locationMgr.start()
    fun stopLocationUpdates()  = locationMgr.stop()

    // ── Connectivity & Bluetooth ──────────────────────────────────────────────
    private val _isWifi = MutableStateFlow(false)
    private val _isData = MutableStateFlow(false)
    private val _isOnline = MutableStateFlow(true)
    val isWifi: StateFlow<Boolean> = _isWifi
    val isData: StateFlow<Boolean> = _isData
    val isOnline: StateFlow<Boolean> = _isOnline

    private val _isBluetoothOn = MutableStateFlow(false)
    private val _isBluetoothConnected = MutableStateFlow(false)
    val isBluetoothOn: StateFlow<Boolean> = _isBluetoothOn
    val isBluetoothConnected: StateFlow<Boolean> = _isBluetoothConnected

    fun toggleSelectedApp(packageName: String) {
        updateSettings {
            val list = selectedAppPackages.toMutableList()
            if (packageName in list) list.remove(packageName) else list.add(packageName)
            copy(selectedAppPackages = list)
        }
    }

    fun toggleToolsApp(packageName: String) {
        updateSettings {
            val list = toolsAppPackages.toMutableList()
            if (packageName in list) list.remove(packageName) else list.add(packageName)
            copy(toolsAppPackages = list)
        }
    }

    fun uninstallApp(context: Context, packageName: String) {
        android.util.Log.d("LauncherVM", "uninstallApp called for $packageName")
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.fromParts("package", packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            android.util.Log.d("LauncherVM", "startActivity ACTION_DELETE succeeded for $packageName")
        } catch (e: Exception) {
            android.util.Log.e("LauncherVM", "ACTION_DELETE failed, trying ACTION_UNINSTALL_PACKAGE", e)
            try {
                val fallbackIntent = Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
                    data = Uri.fromParts("package", packageName, null)
                    putExtra(Intent.EXTRA_RETURN_RESULT, false)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
                android.util.Log.d("LauncherVM", "startActivity ACTION_UNINSTALL_PACKAGE succeeded for $packageName")
            } catch (e2: Exception) {
                android.util.Log.e("LauncherVM", "Both uninstall intents failed", e2)
            }
        }
    }

    fun refreshMedia() {
        MediaListenerService.requestRefresh()
    }

    // ── Radio ─────────────────────────────────────────────────────────────────
    // Android has no public FM tuner API (the Broadcast Radio HAL is @SystemApi),
    // so the radio widget mirrors the unit's real tuner through two backends:
    //   1. szchoiceway MCU units — Settings.Global JSON + canbus broadcasts.
    //      Full control: seek, band switching, direct frequency tuning.
    //   2. Any other unit — the vendor radio app's MediaSession. Frequency and
    //      station are parsed from session metadata; seek maps to skip next/prev
    //      (how steering-wheel keys drive these apps). No direct tuning.
    data class HardwareRadioState(
        val band: String,
        val freq: String,
        val stationName: String? = null,
        // Direct tuning + FM1/FM2/FM3/AM switching — vendor MCU backend only
        val canTune: Boolean = false
    ) {
        val display get() = "$band  $freq"
        val isAm    get() = band.equals("AM", ignoreCase = true)
    }

    private val _mcuRadio = MutableStateFlow<HardwareRadioState?>(null)

    private fun packageInstalled(pkg: String) = runCatching {
        getApplication<Application>().packageManager.getPackageInfo(pkg, 0)
    }.isSuccess

    private val hasSzchoicewayMcu: Boolean by lazy {
        packageInstalled("com.szchoiceway.radio") ||
        packageInstalled("com.szchoiceway.eventcenter") ||
        runCatching {
            AndroidSettings.Global.getString(
                getApplication<Application>().contentResolver, "SYS_MEDIA_INFO_JSON"
            ) != null
        }.getOrDefault(false)
    }

    private fun parseRadioJson(): HardwareRadioState? {
        return try {
            val json = AndroidSettings.Global.getString(
                getApplication<Application>().contentResolver, "SYS_MEDIA_INFO_JSON"
            ) ?: return null
            val title = org.json.JSONObject(json).optString("mediaTitle", "") // e.g. "FM1 90.10"
            if (title.isEmpty()) return null
            val parts = title.trim().split("\\s+".toRegex())
            if (parts.size < 2) return null
            HardwareRadioState(band = parts[0], freq = parts[1], canTune = true)
        } catch (e: Exception) {
            android.util.Log.e("RadioMcu", "parseRadioJson error", e)
            null
        }
    }

    private var radioObserver: ContentObserver? = null

    private fun startHardwareRadioObserver() {
        if (radioObserver != null) return
        _mcuRadio.value = parseRadioJson()
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                _mcuRadio.value = parseRadioJson()
            }
        }
        getApplication<Application>().contentResolver.registerContentObserver(
            AndroidSettings.Global.getUriFor("SYS_MEDIA_INFO_JSON"),
            false, observer
        )
        radioObserver = observer
    }

    // ── MediaSession radio backend (universal fallback) ───────────────────────

    private fun looksLikeRadioPackage(pkg: String): Boolean {
        val p = pkg.lowercase()
        return "radio" in p || "fmradio" in p || "tuner" in p || p.endsWith(".fm") || ".fm." in p
    }

    private fun isRadioSessionPackage(pkg: String): Boolean {
        val assigned = settings.value.radioPackage
        return if (assigned.isNotEmpty()) pkg == assigned else looksLikeRadioPackage(pkg)
    }

    // Matches "FM1 90.10", "99.9 MHz", "FM 99.9 WXYZ", "1040 kHz", "99,9" …
    private val sessionFreqPattern =
        Regex("""(?:\b(FM\s?\d?|AM)\b)?\s*(\d{2,4}(?:[.,]\d{1,2})?)\s*(MHz|kHz)?""", RegexOption.IGNORE_CASE)

    private fun parseSessionRadio(np: NowPlayingState): HardwareRadioState? {
        val pkg = np.controller?.packageName ?: return null
        if (!isRadioSessionPackage(pkg)) return null

        val text = listOf(np.title, np.artist)
            .filter { it.isNotBlank() && it != "Unknown" }
            .joinToString("  ")
        val match   = sessionFreqPattern.find(text)
        val rawFreq = match?.groupValues?.get(2)?.replace(',', '.')?.takeIf { it.isNotEmpty() }
        val freqVal = rawFreq?.toFloatOrNull()
        val explicitBand = match?.groupValues?.get(1)?.replace(" ", "")?.uppercase().orEmpty()
        val band = when {
            explicitBand.isNotEmpty()                 -> explicitBand
            freqVal != null && freqVal in 520f..1710f -> "AM"
            else                                      -> "FM"
        }
        // Whatever isn't the frequency is the station / RDS text
        val station = (if (match != null) text.replace(match.value, "") else text)
            .trim(' ', '-', '|', '/', '•')
            .takeIf { it.isNotBlank() }
        val result = HardwareRadioState(band = band, freq = rawFreq ?: "—", stationName = station, canTune = false)
        FileLog.log("Radio", "pkg=$pkg text=\"$text\" band=$band freq=${rawFreq ?: "—"} station=${station ?: "null"}")
        return result
    }

    // MCU backend wins when present; otherwise mirror the radio app's session
    val hardwareRadio: StateFlow<HardwareRadioState?> =
        combine(_mcuRadio, nowPlaying, settings) { mcu, np, _ ->
            mcu ?: np?.let { parseSessionRadio(it) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun radioSessionController() = nowPlaying.value?.controller?.takeIf { c ->
        c.packageName?.let { isRadioSessionPackage(it) } == true
    }

    // Send a radio keyCode to the MCU via the EventService broadcast channel.
    // Permission com.szchoiceway.permission.broadcast (prot=normal) is required and declared.
    // keyCodes: 15=seek_up, 14=seek_down, 30=fm_cycle, 31=am
    private fun sendMcuByteArray(bytes: ByteArray) {
        runCatching {
            val intent = Intent("com.szchoiceway.eventcenter.EventUtils.ACTION_MCU_CMD_EVENT").apply {
                putExtra("EventUtils.MCU_CMD_DATA", bytes)
            }
            getApplication<Application>().sendBroadcast(intent)
        }
    }

    private fun sendMcuBytes(vararg bytes: Byte) = sendMcuByteArray(bytes)

    // Tune to a specific frequency.
    // Canbus frame: 0x5a 0xa5 0x0d [0x91 bandByte bank(2) zeros... freqAscii] checksum
    // Checksum = low byte of sum of all preceding bytes.
    // Wrapped with 0x0d 0x08 sub-command prefix for MCU_CMD_DATA.
    fun radioTune(band: String, freqMhz: Float) {
        if (!hasSzchoicewayMcu) return // direct tuning is MCU-only
        val bandByte  = when (band) { "FM3" -> 0x03.toByte(); "AM" -> 0x04.toByte(); else -> 0x01.toByte() }
        val bankStr   = if (band == "FM2") "02" else "01"
        val freqStr   = if (band == "AM") String.format(java.util.Locale.US, "%.0f", freqMhz) else String.format(java.util.Locale.US, "%.1f", freqMhz)
        val padding   = 9 - freqStr.length
        val header    = byteArrayOf(0x5a.toByte(), 0xa5.toByte(), 0x0d.toByte())
        val data      = byteArrayOf(0x91.toByte(), bandByte,
                            bankStr[0].code.toByte(), bankStr[1].code.toByte()) +
                        ByteArray(padding) +
                        freqStr.map { it.code.toByte() }.toByteArray()
        val frame     = header + data
        val checksum  = (frame.sumOf { it.toInt() and 0xFF } and 0xFF).toByte()
        val finalPayload = byteArrayOf(0x0d.toByte(), 0x08.toByte()) + frame + byteArrayOf(checksum)
        sendMcuByteArray(finalPayload)
    }

    private val mcuActive get() = _mcuRadio.value != null

    // Seek routes to the MCU when that backend is live, otherwise to the radio
    // app's MediaSession — skip next/prev is how these apps expose seek.
    fun radioSeekUp() {
        FileLog.log("Radio", "seekUp mcu=$mcuActive")
        if (mcuActive) sendMcuBytes(0x02, 0x0f) else radioSessionController()?.transportControls?.skipToNext()
    }
    fun radioSeekDown() {
        FileLog.log("Radio", "seekDown mcu=$mcuActive")
        if (mcuActive) sendMcuBytes(0x02, 0x0e) else radioSessionController()?.transportControls?.skipToPrevious()
    }
    // Band switching and direct tuning only exist on the MCU backend
    fun radioCycleFm()  { if (mcuActive) sendMcuBytes(0x02, 0x1e) }
    fun radioSwitchAm() { if (mcuActive) sendMcuBytes(0x02, 0x1f) }
    fun radioStart()    { sendMcuBytes(0x01, 0x01) }
    fun radioStop()     { sendMcuBytes(0x01, 0x63) }

    fun stopHardwareRadioApp() {
        FileLog.log("Radio", "powerOff mcu=$mcuActive")
        if (mcuActive) radioStop()
        else radioSessionController()?.transportControls?.pause()
    }

    fun launchHardwareRadioApp() {
        FileLog.log("Radio", "powerOn mcu=$hasSzchoicewayMcu assignedPkg=${settings.value.radioPackage.ifEmpty { "auto" }}")
        if (hasSzchoicewayMcu) radioStart()
        val pkg = settings.value.radioPackage.ifEmpty {
            when {
                hasSzchoicewayMcu -> "com.szchoiceway.radio"
                else              -> radioSessionController()?.packageName ?: ""
            }
        }
        if (pkg.isNotEmpty()) {
            runCatching {
                val intent = getApplication<Application>().packageManager
                    .getLaunchIntentForPackage(pkg)
                    ?: Intent(Intent.ACTION_MAIN).apply {
                        `package` = pkg
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                getApplication<Application>().startActivity(intent)
            }
        }
        // Resume playback if the session is just paused
        radioSessionController()?.transportControls?.play()
    }

    fun refreshConnectivity() {
        val app = getApplication<Application>()
        val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (cm == null) {
            _isOnline.value = true
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val net = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(net)
            val hasWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            val hasCell = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
            val hasEth  = caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true
            val hasVpn  = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
            val hasBt   = caps?.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) == true
            val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

            // Legacy fallback for headunits / local hotspots where caps may be null
            @Suppress("DEPRECATION")
            val legacyInfo = cm.activeNetworkInfo
            @Suppress("DEPRECATION")
            val legacyConnected = legacyInfo?.isConnected == true
            @Suppress("DEPRECATION")
            val legacyType = legacyInfo?.type

            _isWifi.value = hasWifi || (legacyConnected && legacyType == ConnectivityManager.TYPE_WIFI)
            _isData.value = hasCell || (legacyConnected && legacyType == ConnectivityManager.TYPE_MOBILE)
            _isOnline.value = hasInternet || hasWifi || hasCell || hasEth || hasVpn || hasBt || legacyConnected
        } else {
            // activeNetwork requires API 23 — legacy path for Android 5.x head units
            @Suppress("DEPRECATION")
            val info = cm.activeNetworkInfo
            @Suppress("DEPRECATION")
            val connected = info?.isConnected == true
            @Suppress("DEPRECATION")
            val type = info?.type
            _isWifi.value = connected && type == ConnectivityManager.TYPE_WIFI
            _isData.value = connected && type == ConnectivityManager.TYPE_MOBILE
            _isOnline.value = connected
        }
    }

    fun refreshBluetoothStatus() {
        runCatching {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter == null) {
                _isBluetoothOn.value = false
                _isBluetoothConnected.value = false
                return
            }
            val enabled = adapter.isEnabled
            _isBluetoothOn.value = enabled
            if (!enabled) {
                _isBluetoothConnected.value = false
                return
            }
            val a2dp = adapter.getProfileConnectionState(BluetoothProfile.A2DP) == BluetoothProfile.STATE_CONNECTED
            val headset = adapter.getProfileConnectionState(BluetoothProfile.HEADSET) == BluetoothProfile.STATE_CONNECTED
            _isBluetoothConnected.value = a2dp || headset
        }.onFailure {
            _isBluetoothOn.value = false
            _isBluetoothConnected.value = false
        }
    }

    private var statusReceiver: BroadcastReceiver? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onCleared() {
        super.onCleared()
        locationMgr.stop()
        radioObserver?.let { getApplication<Application>().contentResolver.unregisterContentObserver(it) }
        radioObserver = null
        statusReceiver?.let {
            runCatching { getApplication<Application>().unregisterReceiver(it) }
            statusReceiver = null
        }
        networkCallback?.let {
            val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            runCatching { cm?.unregisterNetworkCallback(it) }
            networkCallback = null
        }
    }

    init {
        FileLog.init(getApplication())
        FileLog.log("Radio", "szchoicewayMcu=$hasSzchoicewayMcu")
        loadInstalledApps()
        refreshConnectivity()
        refreshBluetoothStatus()

        // Listen for Bluetooth and Package changes
        statusReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothAdapter.ACTION_STATE_CHANGED,
                    BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED,
                    "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED",
                    "android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED" -> {
                        refreshBluetoothStatus()
                    }
                    Intent.ACTION_PACKAGE_REMOVED,
                    Intent.ACTION_PACKAGE_ADDED -> {
                        loadInstalledApps()
                    }
                }
            }
        }
        val btFilter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
            addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED")
            addAction("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED")
        }
        runCatching { getApplication<Application>().registerReceiver(statusReceiver, btFilter) }

        val pkgFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }
        runCatching { getApplication<Application>().registerReceiver(statusReceiver, pkgFilter) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val cb = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) { refreshConnectivity() }
                override fun onLost(network: Network) { refreshConnectivity() }
                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    refreshConnectivity()
                }
            }
            networkCallback = cb
            runCatching { cm?.registerDefaultNetworkCallback(cb) }
        }

        // Periodic ticker: refresh connectivity and BT every 3 seconds to ensure real-time accuracy
        viewModelScope.launch {
            while (isActive) {
                delay(3000L)
                refreshConnectivity()
                refreshBluetoothStatus()
            }
        }

        if (hasSzchoicewayMcu) startHardwareRadioObserver()
        // Initial weather fetch once settings are loaded, on location updates, or every 30 minutes
        viewModelScope.launch {
            var lastFetchMs = 0L
            var hadGpsWeather = false
            merge(
                settingsLoaded.filter { it }.map { null },
                locationMgr.location.filterNotNull(),
                minuteTicker.map { null }
            ).collect { loc ->
                val now = System.currentTimeMillis()
                val isGpsFix = loc != null
                val needsGpsUpgrade = isGpsFix && !hadGpsWeather && settings.value.weatherUseGps
                if (needsGpsUpgrade || now - lastFetchMs >= 20 * 60 * 1_000L || _weather.value == null) {
                    lastFetchMs = now
                    if (isGpsFix) hadGpsWeather = true
                    fetchWeather()
                }
            }
        }

        // React immediately when network status changes
        viewModelScope.launch {
            isOnline.collect { online ->
                if (!online) {
                    _weather.value = _weather.value?.copy(isOffline = true) ?: WeatherState(
                        temperatureCelsius = 0.0,
                        weatherCode = 0,
                        isOffline = true
                    )
                } else if (_weather.value?.isOffline == true || _weather.value == null) {
                    fetchWeather()
                }
            }
        }
    }
}
