package com.openlauncher.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_settings")

class SettingsRepository(private val context: Context) {

    private val gson = Gson()

    private object Keys {
        val VEHICLE_NAME       = stringPreferencesKey("vehicle_name")
        val ACCENT_COLOR       = intPreferencesKey("accent_color")
        val BG_COLOR           = intPreferencesKey("bg_color")
        val FONT_COLOR         = intPreferencesKey("font_color")
        val TILE_COLOR_DARK    = intPreferencesKey("tile_color_dark")
        val TILE_COLOR_LIGHT   = intPreferencesKey("tile_color_light")
        val TILE_ALPHA         = floatPreferencesKey("tile_alpha")
        val WALLPAPER_URI      = stringPreferencesKey("wallpaper_uri")
        val FONT_BOLD          = booleanPreferencesKey("font_bold")
        val TEXT_SCALE         = floatPreferencesKey("text_scale")
        val UI_SCALE           = floatPreferencesKey("ui_scale")
        val CLOCK_STYLE        = stringPreferencesKey("clock_style")
        val UNIT_SYSTEM        = stringPreferencesKey("unit_system")
        val APP_FONT           = stringPreferencesKey("app_font")
        val SHOW_WEATHER       = booleanPreferencesKey("show_weather")
        val SHOW_CLOCK         = booleanPreferencesKey("show_clock")
        val SHOW_TELEMETRY     = booleanPreferencesKey("show_telemetry")
        val SHOW_NOW_PLAYING   = booleanPreferencesKey("show_now_playing")
        val SHOW_ALTIMETER     = booleanPreferencesKey("show_altimeter")
        val SHOW_SPEEDOMETER   = booleanPreferencesKey("show_speedometer")
        val SHORTCUTS_JSON     = stringPreferencesKey("shortcuts_json")
        val WIDGET_LAYOUT_JSON = stringPreferencesKey("widget_layout_json")
        val HOME_PAGE_ALT_LAYOUT_JSON = stringPreferencesKey("home_page_alt_layout_json")
        val HOME_PAGE = intPreferencesKey("home_page")
        val AUTO_SWITCH_PAGES = booleanPreferencesKey("auto_switch_pages")
        val DRIVING_PAGE = intPreferencesKey("driving_page")
        val AUTO_SWITCH_SPEED = floatPreferencesKey("auto_switch_speed")
        val AUTO_SWITCH_IDLE = floatPreferencesKey("auto_switch_idle")
        val CAR_PLAY_PACKAGE      = stringPreferencesKey("car_play_package")
        val ANDROID_AUTO_PACKAGE  = stringPreferencesKey("android_auto_package")
        val USE_GRADIENT          = booleanPreferencesKey("use_gradient")
        val GRADIENT_END_COLOR    = intPreferencesKey("gradient_end_color")
        val WALLPAPER_DIM         = floatPreferencesKey("wallpaper_dim")
        val RIGHT_HAND_DRIVE      = booleanPreferencesKey("right_hand_drive") // kept for migration
        val SIDEBAR_POSITION           = stringPreferencesKey("sidebar_position")
        val SIDEBAR_WIDTH              = intPreferencesKey("sidebar_width")
        val SIDEBAR_ICON_SCALE         = floatPreferencesKey("sidebar_icon_scale")
        val BOTTOM_BAR_SHORTCUTS_RIGHT = booleanPreferencesKey("bottom_bar_shortcuts_right")
        val DAY_NIGHT_MODE        = stringPreferencesKey("day_night_mode")
        val SHOW_PIP              = booleanPreferencesKey("show_pip")
        val PIP_APP_PACKAGE       = stringPreferencesKey("pip_app_package")
        val RADIO_PACKAGE         = stringPreferencesKey("radio_package")
        val ONBOARDING_COMPLETED  = booleanPreferencesKey("onboarding_completed")
        val SHOW_VITALS           = booleanPreferencesKey("show_vitals")
        val SHOW_TRIP_TRACKER     = booleanPreferencesKey("show_trip_tracker")
        val COMPASS_OFFSET        = floatPreferencesKey("compass_offset")
        val ALTIMETER_ROLL_OFFSET = floatPreferencesKey("altimeter_roll_offset")
        val ALTIMETER_PITCH_OFFSET = floatPreferencesKey("altimeter_pitch_offset")
        val SHOW_SOUNDBOARD       = booleanPreferencesKey("show_soundboard")
        val SOUNDBOARD_PADS_JSON  = stringPreferencesKey("soundboard_pads_json")
        val VITALS_AS_BARS        = booleanPreferencesKey("vitals_as_bars")
        val VITALS_ELEMENTS_JSON  = stringPreferencesKey("vitals_elements_json")
        val SPEEDOMETER_DIGITAL_ONLY = booleanPreferencesKey("speedometer_digital_only")
        val GRADIENT_DIRECTION    = stringPreferencesKey("gradient_direction")
        val USE_CUSTOM_BG_COLOR   = booleanPreferencesKey("use_custom_bg_color")
        val SHOW_VOLUME           = booleanPreferencesKey("show_volume")
        val SHOW_QUICK_ACTIONS    = booleanPreferencesKey("show_quick_actions")
        val SHOW_APP_SHORTCUTS    = booleanPreferencesKey("show_app_shortcuts")
        val APP_WIDGET_PACKAGES_JSON = stringPreferencesKey("app_widget_packages_json")
        val QUICK_ACTION_BUTTONS_JSON = stringPreferencesKey("quick_action_buttons_json")
        val SELECTED_APP_PACKAGES_JSON = stringPreferencesKey("selected_app_packages_json")
        val TOOLS_APP_PACKAGES_JSON = stringPreferencesKey("tools_app_packages_json")
        val WEATHER_USE_GPS       = booleanPreferencesKey("weather_use_gps")
        val WEATHER_CUSTOM_LOCATION = stringPreferencesKey("weather_custom_location")
        val WEATHER_PROVIDER      = stringPreferencesKey("weather_provider")
        val WEATHER_FORECAST_DAYS = intPreferencesKey("weather_forecast_days")
        val ADVANCED_WEATHER_ICONS = booleanPreferencesKey("advanced_weather_icons")
        val SHOW_STATUS_ICONS     = booleanPreferencesKey("show_status_icons")
        val SHOW_NETWORK_SPEED    = booleanPreferencesKey("show_network_speed")
        val SHOW_NETWORK_SPEED_INDICATOR = booleanPreferencesKey("show_network_speed_indicator")
        val APP_SHORTCUT_COUNT    = intPreferencesKey("app_shortcut_count")
        val SOUNDBOARD_BUTTON_COUNT = intPreferencesKey("soundboard_button_count")
        val SHOW_SPECTRUM         = booleanPreferencesKey("show_spectrum")
        val SPECTRUM_BAR_COUNT    = intPreferencesKey("spectrum_bar_count")
        val SPECTRUM_STYLE        = stringPreferencesKey("spectrum_style")
        val SPECTRUM_MIRROR       = booleanPreferencesKey("spectrum_mirror")
        val SPECTRUM_COLOR        = intPreferencesKey("spectrum_color")
        val SPECTRUM_GAIN         = floatPreferencesKey("spectrum_gain")
        val SPECTRUM_FALLBACK_URI = stringPreferencesKey("spectrum_fallback_uri")
        val SPECTRUM_ATTACK_MS    = floatPreferencesKey("spectrum_attack_ms")
        val SPECTRUM_DECAY_MS     = floatPreferencesKey("spectrum_decay_ms")
        val SPECTRUM_RESPONSE     = floatPreferencesKey("spectrum_response")
        val APP_DRAWER_FILTER     = stringPreferencesKey("app_drawer_filter")
        val SHOW_OBD              = booleanPreferencesKey("show_obd")
        val OBD_DEVICE_ADDRESS    = stringPreferencesKey("obd_device_address")
        val OBD_DEVICE_NAME       = stringPreferencesKey("obd_device_name")
        val PREFERRED_MEDIA_PACKAGE = stringPreferencesKey("preferred_media_package")
        val LAST_MEDIA_PACKAGE    = stringPreferencesKey("last_media_package")
        val AUTO_LAUNCH_MEDIA     = booleanPreferencesKey("auto_launch_media")
        val VOLUME_PROTOCOL       = stringPreferencesKey("volume_protocol")
        val HELP_TEXT_COLOR       = intPreferencesKey("help_text_color")
        val DISPLAY_ANIMATION     = booleanPreferencesKey("display_animation")
        val CAR_IMAGE_URI         = stringPreferencesKey("car_image_uri")
        val WIDGET_CONTENT_ALT_JSON = stringPreferencesKey("widget_content_alt_json")
        val FLOATING_PANEL_ENABLED  = booleanPreferencesKey("floating_panel_enabled")
        val FLOATING_PANEL_ACTIONS_JSON = stringPreferencesKey("floating_panel_actions_json")
        val FLOATING_PANEL_X        = intPreferencesKey("floating_panel_x")
        val FLOATING_PANEL_Y        = intPreferencesKey("floating_panel_y")
        val FLOATING_PANEL_POS_SET  = booleanPreferencesKey("floating_panel_position_set")
        val SETTINGS_CATEGORY       = stringPreferencesKey("settings_category")
        val SHOW_RADIO_BANDS        = booleanPreferencesKey("show_radio_bands")
        val SHOW_RADIO_POPUP        = booleanPreferencesKey("show_radio_popup")
        val NOW_PLAYING_LOGO_SCALE  = floatPreferencesKey("now_playing_logo_scale")
        val NOW_PLAYING_LOGO_DIM    = floatPreferencesKey("now_playing_logo_dim")
        val FLOATING_PANEL_SCALE    = floatPreferencesKey("floating_panel_scale")
        val FLOATING_PANEL_OPACITY  = floatPreferencesKey("floating_panel_opacity")
        val FLOATING_PANEL_VERTICAL = booleanPreferencesKey("floating_panel_vertical")
        val AUTO_LAUNCH_ENABLED    = booleanPreferencesKey("auto_launch_enabled")
        val AUTO_LAUNCH_PACKAGE    = stringPreferencesKey("auto_launch_package")
        val AUTO_LAUNCH_DELAY_SEC  = intPreferencesKey("auto_launch_delay_sec")
        val GESTURE_TWO_FINGER     = stringPreferencesKey("gesture_two_finger")
        val GESTURE_THREE_FINGER   = stringPreferencesKey("gesture_three_finger")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> readSettings(prefs) }

    private fun readSettings(prefs: Preferences): AppSettings {
            val defaults = AppSettings()
            val shortcutsJson = prefs[Keys.SHORTCUTS_JSON]
            val shortcuts = if (shortcutsJson != null) {
                runCatching {
                    gson.fromJson<List<ShortcutConfig>>(
                        shortcutsJson,
                        object : TypeToken<List<ShortcutConfig>>() {}.type
                    )
                }.getOrNull() ?: defaults.shortcuts
            } else defaults.shortcuts

            val widgetJson = prefs[Keys.WIDGET_LAYOUT_JSON]
            val widgets = if (widgetJson != null) {
                val loaded = runCatching {
                    gson.fromJson<List<WidgetConfig>>(
                        widgetJson,
                        object : TypeToken<List<WidgetConfig>>() {}.type
                    )
                }.getOrNull() ?: defaults.widgetLayout
                // Migrate: old widgets carrying a customBgColor but no bgMode → CUSTOM
                val migrated = loaded.map { w ->
                    if (w.customBgColor != null && w.bgMode == WidgetBgMode.DEFAULT) w.copy(bgMode = WidgetBgMode.CUSTOM) else w
                }
                // Migrate: old 3×2 layout coordinates (max spanX <= 3, spanY <= 2) to 6×4
                if (migrated.isNotEmpty() && migrated.all { it.gridX + it.spanX <= 3 && it.gridY + it.spanY <= 2 }) {
                    migrated.map { it.copy(gridX = it.gridX * 2, gridY = it.gridY * 2, spanX = it.spanX * 2, spanY = it.spanY * 2) }
                } else if (migrated.none { it.gridX >= 2 }) defaults.widgetLayout else migrated
            } else defaults.widgetLayout

            return AppSettings(
                vehicleName    = prefs[Keys.VEHICLE_NAME]     ?: defaults.vehicleName,
                accentColor    = prefs[Keys.ACCENT_COLOR]     ?: defaults.accentColor,
                backgroundColor = prefs[Keys.BG_COLOR]        ?: defaults.backgroundColor,
                fontColor      = prefs[Keys.FONT_COLOR]       ?: defaults.fontColor,
                tileColorDark  = prefs[Keys.TILE_COLOR_DARK]  ?: defaults.tileColorDark,
                tileColorLight = prefs[Keys.TILE_COLOR_LIGHT] ?: defaults.tileColorLight,
                tileAlpha      = prefs[Keys.TILE_ALPHA]?.coerceIn(0f, 1f) ?: defaults.tileAlpha,
                wallpaperUri   = prefs[Keys.WALLPAPER_URI]    ?: defaults.wallpaperUri,
                fontBold       = prefs[Keys.FONT_BOLD]        ?: defaults.fontBold,
                textScale      = prefs[Keys.TEXT_SCALE]       ?: defaults.textScale,
                uiScale        = prefs[Keys.UI_SCALE]         ?: defaults.uiScale,
                clockStyle     = prefs[Keys.CLOCK_STYLE]?.let { runCatching { ClockStyle.valueOf(it) }.getOrNull() } ?: defaults.clockStyle,
                unitSystem     = prefs[Keys.UNIT_SYSTEM]?.let { runCatching { UnitSystem.valueOf(it) }.getOrNull() } ?: defaults.unitSystem,
                appFont        = prefs[Keys.APP_FONT]?.let { runCatching { AppFont.valueOf(it) }.getOrNull() } ?: defaults.appFont,
                showWeather    = prefs[Keys.SHOW_WEATHER]     ?: defaults.showWeather,
                showClock      = prefs[Keys.SHOW_CLOCK]       ?: defaults.showClock,
                showTelemetry  = prefs[Keys.SHOW_TELEMETRY]   ?: defaults.showTelemetry,
                showNowPlaying = prefs[Keys.SHOW_NOW_PLAYING] ?: defaults.showNowPlaying,
                showAltimeter   = prefs[Keys.SHOW_ALTIMETER]   ?: defaults.showAltimeter,
                showSpeedometer = prefs[Keys.SHOW_SPEEDOMETER] ?: defaults.showSpeedometer,
                shortcuts      = shortcuts,
                widgetLayout   = widgets,
                homePageAltLayout = prefs[Keys.HOME_PAGE_ALT_LAYOUT_JSON]?.let { json ->
                    runCatching {
                        gson.fromJson<List<WidgetConfig>>(json, object : com.google.gson.reflect.TypeToken<List<WidgetConfig>>() {}.type)
                    }.getOrNull()
                } ?: defaults.homePageAltLayout,
                homePage       = (prefs[Keys.HOME_PAGE] ?: 0).coerceIn(0, 1),
                autoSwitchPages = prefs[Keys.AUTO_SWITCH_PAGES] ?: defaults.autoSwitchPages,
                drivingPage    = (prefs[Keys.DRIVING_PAGE] ?: 0).coerceIn(0, 1),
                autoSwitchSpeedKmh = prefs[Keys.AUTO_SWITCH_SPEED]?.coerceIn(5f, 20f) ?: defaults.autoSwitchSpeedKmh,
                autoSwitchIdleSeconds = prefs[Keys.AUTO_SWITCH_IDLE]?.coerceIn(5f, 30f) ?: defaults.autoSwitchIdleSeconds,
                carPlayPackage      = prefs[Keys.CAR_PLAY_PACKAGE]      ?: defaults.carPlayPackage,
                androidAutoPackage  = prefs[Keys.ANDROID_AUTO_PACKAGE]  ?: defaults.androidAutoPackage,
                useGradient      = prefs[Keys.USE_GRADIENT]        ?: defaults.useGradient,
                gradientEndColor = prefs[Keys.GRADIENT_END_COLOR]  ?: defaults.gradientEndColor,
                wallpaperDim     = prefs[Keys.WALLPAPER_DIM]       ?: defaults.wallpaperDim,
                sidebarPosition  = prefs[Keys.SIDEBAR_POSITION]?.let { runCatching { SidebarPosition.valueOf(it) }.getOrNull() }
                                   ?: if (prefs[Keys.RIGHT_HAND_DRIVE] == true) SidebarPosition.RIGHT else defaults.sidebarPosition,
                sidebarWidth     = prefs[Keys.SIDEBAR_WIDTH]?.coerceIn(40, 120) ?: defaults.sidebarWidth,
                sidebarIconScale = prefs[Keys.SIDEBAR_ICON_SCALE]?.coerceIn(0.6f, 1.8f) ?: defaults.sidebarIconScale,
                bottomBarShortcutsRight = prefs[Keys.BOTTOM_BAR_SHORTCUTS_RIGHT] ?: defaults.bottomBarShortcutsRight,
                dayNightMode     = prefs[Keys.DAY_NIGHT_MODE]?.let { runCatching { DayNightMode.valueOf(it) }.getOrNull() } ?: defaults.dayNightMode,
                showPip          = prefs[Keys.SHOW_PIP]         ?: defaults.showPip,
                pipAppPackage    = prefs[Keys.PIP_APP_PACKAGE]  ?: defaults.pipAppPackage,
                radioPackage     = prefs[Keys.RADIO_PACKAGE]    ?: defaults.radioPackage,
                onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: defaults.onboardingCompleted,
                showVitals       = prefs[Keys.SHOW_VITALS]      ?: defaults.showVitals,
                showTripTracker  = prefs[Keys.SHOW_TRIP_TRACKER] ?: defaults.showTripTracker,
                compassOffset    = prefs[Keys.COMPASS_OFFSET]    ?: defaults.compassOffset,
                altimeterRollOffset = prefs[Keys.ALTIMETER_ROLL_OFFSET] ?: defaults.altimeterRollOffset,
                altimeterPitchOffset = prefs[Keys.ALTIMETER_PITCH_OFFSET] ?: defaults.altimeterPitchOffset,
                showSoundboard   = prefs[Keys.SHOW_SOUNDBOARD]   ?: defaults.showSoundboard,
                soundboardPads   = prefs[Keys.SOUNDBOARD_PADS_JSON]?.let {
                    runCatching {
                        val list = gson.fromJson<List<SoundPadConfig>>(it, object : com.google.gson.reflect.TypeToken<List<SoundPadConfig>>() {}.type)
                        list?.map { pad ->
                            when (pad.synthType.lowercase().trim()) {
                                "mario_jump", "horn", "beep", "alert" -> pad.copy(label = if (pad.label == "mario_jump" || pad.label == "jump") "doc" else pad.label, synthType = "whats_up_doc")
                                "mario_coin", "kick", "snare", "bass" -> pad.copy(label = if (pad.label == "mario_coin" || pad.label == "coin") "be_back" else pad.label, synthType = "adhimahadi_arnold_ill_be_back_8276")
                                "boom" -> pad.copy(label = if (pad.label == "boom") "later" else pad.label, synthType = "a_few_moments_later")
                                "loud_fart", "fart" -> pad.copy(label = if (pad.label == "loud_fart" || pad.label == "fart") "stamata" else pad.label, synthType = "stamataaaaaaa")
                                else -> pad
                            }
                        }
                    }.getOrNull()
                } ?: defaults.soundboardPads,
                vitalsAsBars     = prefs[Keys.VITALS_AS_BARS] ?: defaults.vitalsAsBars,
                vitalsElements   = prefs[Keys.VITALS_ELEMENTS_JSON]?.let { json ->
                    runCatching {
                        val parsed = gson.fromJson<List<Any?>>(json, object : com.google.gson.reflect.TypeToken<List<Any?>>() {}.type)
                        parsed?.mapNotNull { item ->
                            when (item) {
                                is String -> item.trim()
                                is Map<*, *> -> (item["id"] ?: item["label"])?.toString()?.trim()
                                else -> item?.toString()?.trim()
                            }
                        }?.filter { it.isNotBlank() }
                    }.getOrNull()
                }?.takeIf { it.isNotEmpty() } ?: defaults.vitalsElements,
                speedometerDigitalOnly = prefs[Keys.SPEEDOMETER_DIGITAL_ONLY] ?: defaults.speedometerDigitalOnly,
                gradientDirection = prefs[Keys.GRADIENT_DIRECTION]?.let { runCatching { GradientDirection.valueOf(it) }.getOrNull() } ?: defaults.gradientDirection,
                useCustomBackgroundColor = prefs[Keys.USE_CUSTOM_BG_COLOR] ?: defaults.useCustomBackgroundColor,
                showVolume       = prefs[Keys.SHOW_VOLUME] ?: defaults.showVolume,
                showQuickActions = prefs[Keys.SHOW_QUICK_ACTIONS] ?: defaults.showQuickActions,
                showAppShortcuts = prefs[Keys.SHOW_APP_SHORTCUTS] ?: defaults.showAppShortcuts,
                appWidgetPackages = prefs[Keys.APP_WIDGET_PACKAGES_JSON]?.let {
                    runCatching {
                        gson.fromJson<List<String>>(it, object : com.google.gson.reflect.TypeToken<List<String>>() {}.type)
                    }.getOrNull()
                } ?: defaults.appWidgetPackages,
                quickActionButtons = prefs[Keys.QUICK_ACTION_BUTTONS_JSON]?.let { json ->
                    runCatching {
                        val parsed = gson.fromJson<List<Any?>>(json, object : com.google.gson.reflect.TypeToken<List<Any?>>() {}.type)
                        parsed?.mapNotNull { item ->
                            when (item) {
                                is String -> item.trim()
                                is Map<*, *> -> (item["id"] ?: item["label"])?.toString()?.trim()
                                else -> item?.toString()?.trim()
                            }
                        }?.filter { it.isNotBlank() }
                    }.getOrNull()
                }?.takeIf { it.isNotEmpty() } ?: defaults.quickActionButtons,
                selectedAppPackages = prefs[Keys.SELECTED_APP_PACKAGES_JSON]?.let {
                    runCatching {
                        gson.fromJson<List<String>>(it, object : com.google.gson.reflect.TypeToken<List<String>>() {}.type)
                    }.getOrNull()
                } ?: defaults.selectedAppPackages,
                toolsAppPackages = prefs[Keys.TOOLS_APP_PACKAGES_JSON]?.let {
                    runCatching {
                        gson.fromJson<List<String>>(it, object : com.google.gson.reflect.TypeToken<List<String>>() {}.type)
                    }.getOrNull()
                } ?: defaults.toolsAppPackages,
                appDrawerFilter = prefs[Keys.APP_DRAWER_FILTER]?.let { runCatching { AppDrawerFilter.valueOf(it) }.getOrNull() } ?: defaults.appDrawerFilter,
                weatherUseGps = prefs[Keys.WEATHER_USE_GPS] ?: defaults.weatherUseGps,
                weatherCustomLocation = prefs[Keys.WEATHER_CUSTOM_LOCATION] ?: defaults.weatherCustomLocation,
                weatherProvider = prefs[Keys.WEATHER_PROVIDER]?.let { runCatching { WeatherProvider.valueOf(it) }.getOrNull() } ?: defaults.weatherProvider,
                weatherForecastDays = prefs[Keys.WEATHER_FORECAST_DAYS] ?: defaults.weatherForecastDays,
                advancedWeatherIcons = prefs[Keys.ADVANCED_WEATHER_ICONS] ?: defaults.advancedWeatherIcons,
                showStatusIcons       = prefs[Keys.SHOW_STATUS_ICONS] ?: defaults.showStatusIcons,
                showNetworkSpeed      = prefs[Keys.SHOW_NETWORK_SPEED] ?: defaults.showNetworkSpeed,
                showNetworkSpeedIndicator = prefs[Keys.SHOW_NETWORK_SPEED_INDICATOR] ?: defaults.showNetworkSpeedIndicator,
                appShortcutCount      = prefs[Keys.APP_SHORTCUT_COUNT]?.coerceIn(1, 4) ?: defaults.appShortcutCount,
                soundboardButtonCount = prefs[Keys.SOUNDBOARD_BUTTON_COUNT]?.coerceIn(1, 6) ?: defaults.soundboardButtonCount,
                showSpectrum          = prefs[Keys.SHOW_SPECTRUM] ?: defaults.showSpectrum,
                spectrumBarCount      = prefs[Keys.SPECTRUM_BAR_COUNT]?.coerceIn(8, 64) ?: defaults.spectrumBarCount,
                spectrumStyle         = prefs[Keys.SPECTRUM_STYLE]?.let { runCatching { com.openlauncher.app.data.SpectrumStyle.valueOf(it) }.getOrNull() } ?: defaults.spectrumStyle,
                spectrumMirror        = prefs[Keys.SPECTRUM_MIRROR] ?: defaults.spectrumMirror,
                spectrumColor         = prefs[Keys.SPECTRUM_COLOR],
                spectrumGain          = prefs[Keys.SPECTRUM_GAIN]?.coerceIn(0.3f, 3f) ?: defaults.spectrumGain,
                spectrumFallbackImageUri = prefs[Keys.SPECTRUM_FALLBACK_URI] ?: defaults.spectrumFallbackImageUri,
                spectrumAttackMs      = prefs[Keys.SPECTRUM_ATTACK_MS]?.coerceIn(5f, 200f) ?: defaults.spectrumAttackMs,
                spectrumDecayMs       = prefs[Keys.SPECTRUM_DECAY_MS]?.coerceIn(50f, 1500f) ?: defaults.spectrumDecayMs,
                spectrumResponse      = prefs[Keys.SPECTRUM_RESPONSE]?.coerceIn(0.4f, 2.5f) ?: defaults.spectrumResponse,
                showObd               = prefs[Keys.SHOW_OBD] ?: defaults.showObd,
                obdDeviceAddress      = prefs[Keys.OBD_DEVICE_ADDRESS] ?: defaults.obdDeviceAddress,
                obdDeviceName         = prefs[Keys.OBD_DEVICE_NAME] ?: defaults.obdDeviceName,
                preferredMediaPackage = prefs[Keys.PREFERRED_MEDIA_PACKAGE] ?: defaults.preferredMediaPackage,
                lastMediaPackage      = prefs[Keys.LAST_MEDIA_PACKAGE] ?: defaults.lastMediaPackage,
                autoLaunchMedia       = prefs[Keys.AUTO_LAUNCH_MEDIA] ?: defaults.autoLaunchMedia,
                volumeProtocol        = prefs[Keys.VOLUME_PROTOCOL] ?: defaults.volumeProtocol,
                helpTextColor         = prefs[Keys.HELP_TEXT_COLOR],
                displayAnimation      = prefs[Keys.DISPLAY_ANIMATION] ?: defaults.displayAnimation,
                carImageUri           = prefs[Keys.CAR_IMAGE_URI] ?: defaults.carImageUri,
                widgetContentAlt      = prefs[Keys.WIDGET_CONTENT_ALT_JSON]?.let { json ->
                    runCatching {
                        gson.fromJson<WidgetContent>(json, WidgetContent::class.java)
                    }.getOrNull()
                },
                floatingPanelEnabled  = prefs[Keys.FLOATING_PANEL_ENABLED] ?: defaults.floatingPanelEnabled,
                floatingPanelActions  = prefs[Keys.FLOATING_PANEL_ACTIONS_JSON]?.let { json ->
                    runCatching {
                        gson.fromJson<List<FloatingPanelAction>>(json, object : com.google.gson.reflect.TypeToken<List<FloatingPanelAction>>() {}.type)
                    }.getOrNull()
                } ?: defaults.floatingPanelActions,
                floatingPanelX        = prefs[Keys.FLOATING_PANEL_X] ?: defaults.floatingPanelX,
                floatingPanelY        = prefs[Keys.FLOATING_PANEL_Y] ?: defaults.floatingPanelY,
                floatingPanelPositionSet = prefs[Keys.FLOATING_PANEL_POS_SET] ?: defaults.floatingPanelPositionSet,
                settingsCategory        = prefs[Keys.SETTINGS_CATEGORY] ?: defaults.settingsCategory,
                showRadioBands          = prefs[Keys.SHOW_RADIO_BANDS] ?: defaults.showRadioBands,
                showRadioPopup          = prefs[Keys.SHOW_RADIO_POPUP] ?: defaults.showRadioPopup,
                nowPlayingLogoScale     = prefs[Keys.NOW_PLAYING_LOGO_SCALE]?.coerceIn(0.5f, 1f) ?: defaults.nowPlayingLogoScale,
                nowPlayingLogoDim       = prefs[Keys.NOW_PLAYING_LOGO_DIM]?.coerceIn(0f, 0.8f) ?: defaults.nowPlayingLogoDim,
                floatingPanelScale    = prefs[Keys.FLOATING_PANEL_SCALE]?.coerceIn(0.5f, 2f) ?: defaults.floatingPanelScale,
                floatingPanelOpacity  = prefs[Keys.FLOATING_PANEL_OPACITY]?.coerceIn(0f, 1f) ?: defaults.floatingPanelOpacity,
                floatingPanelVertical = prefs[Keys.FLOATING_PANEL_VERTICAL] ?: defaults.floatingPanelVertical,
                autoLaunchEnabled    = prefs[Keys.AUTO_LAUNCH_ENABLED] ?: defaults.autoLaunchEnabled,
                autoLaunchPackage    = prefs[Keys.AUTO_LAUNCH_PACKAGE] ?: defaults.autoLaunchPackage,
                autoLaunchDelaySec   = prefs[Keys.AUTO_LAUNCH_DELAY_SEC]?.coerceIn(1, 30) ?: defaults.autoLaunchDelaySec,
                gestureTwoFingerPackage = prefs[Keys.GESTURE_TWO_FINGER] ?: defaults.gestureTwoFingerPackage,
                gestureThreeFingerPackage = prefs[Keys.GESTURE_THREE_FINGER] ?: defaults.gestureThreeFingerPackage
            ).let { s -> reconcileWidgetFlags(s) }
    }

    suspend fun saveSettings(s: AppSettings) {
        context.dataStore.edit { prefs -> writeSettings(prefs, s) }
    }

    /**
     * Atomic read-modify-write. DataStore serializes edit blocks, so concurrent
     * updates can't overwrite each other (unlike transforming a stale snapshot).
     */
    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs -> writeSettings(prefs, transform(readSettings(prefs))) }
    }

    private fun writeSettings(prefs: MutablePreferences, s: AppSettings) {
            prefs[Keys.VEHICLE_NAME]       = s.vehicleName
            prefs[Keys.ACCENT_COLOR]       = s.accentColor
            prefs[Keys.BG_COLOR]           = s.backgroundColor
            prefs[Keys.FONT_COLOR]         = s.fontColor
            prefs[Keys.TILE_COLOR_DARK]    = s.tileColorDark
            prefs[Keys.TILE_COLOR_LIGHT]   = s.tileColorLight
            prefs[Keys.TILE_ALPHA]         = s.tileAlpha
            prefs[Keys.WALLPAPER_URI]      = s.wallpaperUri
            prefs[Keys.FONT_BOLD]          = s.fontBold
            prefs[Keys.TEXT_SCALE]         = s.textScale
            prefs[Keys.UI_SCALE]           = s.uiScale
            prefs[Keys.CLOCK_STYLE]        = s.clockStyle.name
            prefs[Keys.UNIT_SYSTEM]        = s.unitSystem.name
            prefs[Keys.APP_FONT]           = s.appFont.name
            prefs[Keys.SHOW_WEATHER]       = s.showWeather
            prefs[Keys.SHOW_CLOCK]         = s.showClock
            prefs[Keys.SHOW_TELEMETRY]     = s.showTelemetry
            prefs[Keys.SHOW_NOW_PLAYING]   = s.showNowPlaying
            prefs[Keys.SHOW_ALTIMETER]     = s.showAltimeter
            prefs[Keys.SHOW_SPEEDOMETER]   = s.showSpeedometer
            prefs[Keys.SHORTCUTS_JSON]     = gson.toJson(s.shortcuts)
            prefs[Keys.WIDGET_LAYOUT_JSON] = gson.toJson(s.widgetLayout)
            prefs[Keys.HOME_PAGE_ALT_LAYOUT_JSON] = gson.toJson(s.homePageAltLayout)
            prefs[Keys.HOME_PAGE] = s.homePage
            prefs[Keys.AUTO_SWITCH_PAGES] = s.autoSwitchPages
            prefs[Keys.DRIVING_PAGE] = s.drivingPage
            prefs[Keys.AUTO_SWITCH_SPEED] = s.autoSwitchSpeedKmh
            prefs[Keys.AUTO_SWITCH_IDLE] = s.autoSwitchIdleSeconds
            prefs[Keys.CAR_PLAY_PACKAGE]      = s.carPlayPackage
            prefs[Keys.ANDROID_AUTO_PACKAGE]  = s.androidAutoPackage
            prefs[Keys.USE_GRADIENT]       = s.useGradient
            prefs[Keys.GRADIENT_END_COLOR] = s.gradientEndColor
            prefs[Keys.WALLPAPER_DIM]      = s.wallpaperDim
            prefs[Keys.SIDEBAR_POSITION]           = s.sidebarPosition.name
            prefs[Keys.SIDEBAR_WIDTH]              = s.sidebarWidth
            prefs[Keys.SIDEBAR_ICON_SCALE]         = s.sidebarIconScale
            prefs[Keys.BOTTOM_BAR_SHORTCUTS_RIGHT] = s.bottomBarShortcutsRight
            prefs[Keys.DAY_NIGHT_MODE]     = s.dayNightMode.name
            prefs[Keys.SHOW_PIP]           = s.showPip
            prefs[Keys.PIP_APP_PACKAGE]    = s.pipAppPackage
            prefs[Keys.RADIO_PACKAGE]      = s.radioPackage
            prefs[Keys.ONBOARDING_COMPLETED] = s.onboardingCompleted
            prefs[Keys.SHOW_VITALS]        = s.showVitals
            prefs[Keys.SHOW_TRIP_TRACKER]  = s.showTripTracker
            prefs[Keys.COMPASS_OFFSET]     = s.compassOffset
            prefs[Keys.ALTIMETER_ROLL_OFFSET] = s.altimeterRollOffset
            prefs[Keys.ALTIMETER_PITCH_OFFSET] = s.altimeterPitchOffset
            prefs[Keys.SHOW_SOUNDBOARD]    = s.showSoundboard
            prefs[Keys.SOUNDBOARD_PADS_JSON] = gson.toJson(s.soundboardPads)
            prefs[Keys.VITALS_AS_BARS]     = s.vitalsAsBars
            prefs[Keys.VITALS_ELEMENTS_JSON] = gson.toJson(s.vitalsElements)
            prefs[Keys.SPEEDOMETER_DIGITAL_ONLY] = s.speedometerDigitalOnly
            prefs[Keys.GRADIENT_DIRECTION] = s.gradientDirection.name
            prefs[Keys.USE_CUSTOM_BG_COLOR] = s.useCustomBackgroundColor
            prefs[Keys.SHOW_VOLUME]        = s.showVolume
            prefs[Keys.SHOW_QUICK_ACTIONS] = s.showQuickActions
            prefs[Keys.SHOW_APP_SHORTCUTS] = s.showAppShortcuts
            prefs[Keys.APP_WIDGET_PACKAGES_JSON] = gson.toJson(s.appWidgetPackages)
            prefs[Keys.QUICK_ACTION_BUTTONS_JSON] = gson.toJson(s.quickActionButtons)
            prefs[Keys.SELECTED_APP_PACKAGES_JSON] = gson.toJson(s.selectedAppPackages)
            prefs[Keys.TOOLS_APP_PACKAGES_JSON]    = gson.toJson(s.toolsAppPackages)
            prefs[Keys.WEATHER_USE_GPS]         = s.weatherUseGps
            prefs[Keys.WEATHER_CUSTOM_LOCATION] = s.weatherCustomLocation
            prefs[Keys.WEATHER_PROVIDER]        = s.weatherProvider.name
            prefs[Keys.WEATHER_FORECAST_DAYS]   = s.weatherForecastDays
            prefs[Keys.ADVANCED_WEATHER_ICONS]  = s.advancedWeatherIcons
            prefs[Keys.SHOW_STATUS_ICONS]       = s.showStatusIcons
            prefs[Keys.SHOW_NETWORK_SPEED]      = s.showNetworkSpeed
            prefs[Keys.SHOW_NETWORK_SPEED_INDICATOR] = s.showNetworkSpeedIndicator
            prefs[Keys.APP_SHORTCUT_COUNT]      = s.appShortcutCount
            prefs[Keys.SOUNDBOARD_BUTTON_COUNT] = s.soundboardButtonCount
            prefs[Keys.SHOW_SPECTRUM]           = s.showSpectrum
            prefs[Keys.SPECTRUM_BAR_COUNT]      = s.spectrumBarCount
            prefs[Keys.SPECTRUM_STYLE]          = s.spectrumStyle.name
            prefs[Keys.SPECTRUM_MIRROR]         = s.spectrumMirror
            if (s.spectrumColor != null) prefs[Keys.SPECTRUM_COLOR] = s.spectrumColor else prefs.remove(Keys.SPECTRUM_COLOR)
            prefs[Keys.SPECTRUM_GAIN]           = s.spectrumGain
            prefs[Keys.SPECTRUM_FALLBACK_URI]   = s.spectrumFallbackImageUri
            prefs[Keys.SPECTRUM_ATTACK_MS]      = s.spectrumAttackMs
            prefs[Keys.SPECTRUM_DECAY_MS]       = s.spectrumDecayMs
            prefs[Keys.SPECTRUM_RESPONSE]       = s.spectrumResponse
            prefs[Keys.APP_DRAWER_FILTER]       = s.appDrawerFilter.name
            prefs[Keys.SHOW_OBD]                = s.showObd
            prefs[Keys.OBD_DEVICE_ADDRESS]     = s.obdDeviceAddress
            prefs[Keys.OBD_DEVICE_NAME]        = s.obdDeviceName
            prefs[Keys.PREFERRED_MEDIA_PACKAGE] = s.preferredMediaPackage
            prefs[Keys.LAST_MEDIA_PACKAGE]     = s.lastMediaPackage
            prefs[Keys.AUTO_LAUNCH_MEDIA]      = s.autoLaunchMedia
            prefs[Keys.VOLUME_PROTOCOL]        = s.volumeProtocol
            val htc = s.helpTextColor
            if (htc != null) prefs[Keys.HELP_TEXT_COLOR] = htc else prefs.remove(Keys.HELP_TEXT_COLOR)
            prefs[Keys.DISPLAY_ANIMATION]      = s.displayAnimation
            prefs[Keys.CAR_IMAGE_URI]          = s.carImageUri
            val wca = s.widgetContentAlt
            if (wca != null) prefs[Keys.WIDGET_CONTENT_ALT_JSON] = gson.toJson(wca) else prefs.remove(Keys.WIDGET_CONTENT_ALT_JSON)
            prefs[Keys.FLOATING_PANEL_ENABLED]   = s.floatingPanelEnabled
            prefs[Keys.FLOATING_PANEL_ACTIONS_JSON] = gson.toJson(s.floatingPanelActions)
            prefs[Keys.FLOATING_PANEL_X]         = s.floatingPanelX
            prefs[Keys.FLOATING_PANEL_Y]         = s.floatingPanelY
            prefs[Keys.FLOATING_PANEL_POS_SET]   = s.floatingPanelPositionSet
            prefs[Keys.SETTINGS_CATEGORY]        = s.settingsCategory
            prefs[Keys.SHOW_RADIO_BANDS]         = s.showRadioBands
            prefs[Keys.SHOW_RADIO_POPUP]         = s.showRadioPopup
            prefs[Keys.NOW_PLAYING_LOGO_SCALE]   = s.nowPlayingLogoScale
            prefs[Keys.NOW_PLAYING_LOGO_DIM]     = s.nowPlayingLogoDim
            prefs[Keys.FLOATING_PANEL_SCALE]     = s.floatingPanelScale
            prefs[Keys.FLOATING_PANEL_OPACITY]   = s.floatingPanelOpacity
            prefs[Keys.FLOATING_PANEL_VERTICAL]  = s.floatingPanelVertical
            prefs[Keys.AUTO_LAUNCH_ENABLED]     = s.autoLaunchEnabled
            prefs[Keys.AUTO_LAUNCH_PACKAGE]     = s.autoLaunchPackage
            prefs[Keys.AUTO_LAUNCH_DELAY_SEC]   = s.autoLaunchDelaySec
            prefs[Keys.GESTURE_TWO_FINGER]      = s.gestureTwoFingerPackage
            prefs[Keys.GESTURE_THREE_FINGER]    = s.gestureThreeFingerPackage
    }

    suspend fun resetToDefaults() {
        context.dataStore.edit { it.clear() }
    }
}
