package com.openlauncher.app.data

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * Versioned, local JSON export and restore for Open Launcher settings.
 * Serializes all dashboard preferences, widget placements, and vehicle settings.
 * Safe validation guarantees no invalid settings or corrupt layouts crash the launcher.
 */
object SettingsBackup {
    private val gson = GsonBuilder().setPrettyPrinting().create()

    private val VALID_WIDGET_IDS = setOf(
        "CLOCK", "WEATHER", "NOW_PLAYING", "TELEMETRY", "VOLUME",
        "QUICK_ACTIONS", "ALTIMETER", "SPEEDOMETER", "VITALS",
        "TRIP_TRACKER", "SOUNDBOARD", "APP_SHORTCUTS", "OBD", "NETWORK_SPEED", "SPECTRUM"
    )

    fun encode(settings: AppSettings): String {
        val root = JsonObject().apply {
            addProperty("format", "openlauncher-settings")
            addProperty("version", 1)
            addProperty("exportedAtMs", System.currentTimeMillis())
            add("settings", gson.toJsonTree(settings))
        }
        return gson.toJson(root)
    }

    fun decode(json: String): AppSettings {
        require(json.length <= 1_500_000) { "Backup file is too large" }
        val root = JsonParser.parseString(json).asJsonObject
        require(root.get("format")?.asString == "openlauncher-settings") {
            "Unsupported backup format. Expected 'openlauncher-settings'."
        }
        val version = root.get("version")?.asInt ?: 1
        require(version in 1..2) { "Unsupported backup version: $version" }

        val input = root.getAsJsonObject("settings") ?: error("Settings object is missing in backup file")

        // Merge onto default AppSettings so newly added settings in future versions are safely preserved
        val defaultTree = gson.toJsonTree(AppSettings()).asJsonObject
        input.entrySet().forEach { (key, value) ->
            if (value != null && !value.isJsonNull && defaultTree.has(key)) {
                defaultTree.add(key, value)
            }
        }

        // Validate Enums safely
        val enumChecks = mapOf(
            "clockStyle" to ClockStyle.entries.map { it.name },
            "unitSystem" to UnitSystem.entries.map { it.name },
            "appFont" to AppFont.entries.map { it.name },
            "dayNightMode" to DayNightMode.entries.map { it.name },
            "sidebarPosition" to SidebarPosition.entries.map { it.name },
            "gradientDirection" to GradientDirection.entries.map { it.name },
            "appDrawerFilter" to AppDrawerFilter.entries.map { it.name },
            "weatherProvider" to WeatherProvider.entries.map { it.name },
            "spectrumStyle" to SpectrumStyle.entries.map { it.name }
        )

        enumChecks.forEach { (key, validNames) ->
            if (defaultTree.has(key)) {
                val strVal = defaultTree.get(key).asString
                if (strVal !in validNames) {
                    defaultTree.remove(key) // fallback to default
                }
            }
        }

        val restored = gson.fromJson(defaultTree, AppSettings::class.java)

        // Validate numeric ranges
        val safeTextScale = restored.textScale.coerceIn(0.6f, 2.5f)
        val safeUiScale = restored.uiScale.coerceIn(0.6f, 2.5f)
        val safeWallpaperDim = restored.wallpaperDim.coerceIn(0f, 1f)
        val safeForecastDays = restored.weatherForecastDays.coerceIn(1, 14)
        val safeShortcutCount = restored.appShortcutCount.coerceIn(1, 4)
        val safeSoundboardCount = restored.soundboardButtonCount.coerceIn(1, 6)
        val safeSidebarWidth = restored.sidebarWidth.coerceIn(40, 120)
        val safeSidebarIconScale = restored.sidebarIconScale.coerceIn(0.6f, 1.8f)
        val safeTileAlpha = restored.tileAlpha.coerceIn(0f, 1f)
        val safeSpectrumGain = restored.spectrumGain.coerceIn(0.3f, 3f)
        val safeSpectrumBarCount = restored.spectrumBarCount.coerceIn(8, 64)
        val safeSpectrumAttackMs = restored.spectrumAttackMs.coerceIn(5f, 200f)
        val safeSpectrumDecayMs = restored.spectrumDecayMs.coerceIn(50f, 1500f)
        val safeSpectrumResponse = restored.spectrumResponse.coerceIn(0.4f, 2.5f)
        val safeAutoLaunchDelay = restored.autoLaunchDelaySec.coerceIn(1, 30)

        // Validate widget layouts (both home pages)
        fun sanitizeLayout(layout: List<WidgetConfig>): List<WidgetConfig> =
            layout.filter { config ->
                config.id in VALID_WIDGET_IDS &&
                config.gridX in 0 until GRID_COLS &&
                config.gridY in 0 until GRID_ROWS &&
                config.spanX in 1..GRID_COLS &&
                config.spanY in 1..GRID_ROWS
            }.map { w ->
                if (w.customBgColor != null && w.bgMode == WidgetBgMode.DEFAULT) w.copy(bgMode = WidgetBgMode.CUSTOM) else w
            }

        val sanitizedLayout    = sanitizeLayout(restored.widgetLayout)
        val sanitizedAltLayout = sanitizeLayout(restored.homePageAltLayout)

        // Sanitize page-1 widget content overrides (guard against null/oversized lists)
        val safeWidgetContentAlt = restored.widgetContentAlt?.let { alt ->
            alt.copy(
                vitalsElements       = alt.vitalsElements.orEmpty().ifEmpty { defaultVitalsElements() },
                quickActionButtons   = alt.quickActionButtons.orEmpty().ifEmpty { defaultQuickActionButtons() },
                soundboardPads       = alt.soundboardPads.orEmpty(),
                appWidgetPackages    = (alt.appWidgetPackages.orEmpty() + List(4) { "" }).take(4),
                appShortcutCount     = alt.appShortcutCount.coerceIn(1, 4),
                soundboardButtonCount = alt.soundboardButtonCount.coerceIn(1, 6)
            )
        }

        // Sanitize the floating quick-controls panel (cap slots, valid actions, ranges)
        val safeFloatingActions = restored.floatingPanelActions
            .orEmpty()
            .filter { it.packageName.isNotBlank() || it.type != FloatingActionType.APP }
            .take(4)
            .let { if (it.isEmpty()) defaultFloatingActions() else it }
        val safeFloatingScale = restored.floatingPanelScale.coerceIn(0.5f, 2f)
        val safeFloatingOpacity = restored.floatingPanelOpacity.coerceIn(0f, 1f)

        return reconcileWidgetFlags(restored.copy(
            textScale = safeTextScale,
            uiScale = safeUiScale,
            wallpaperDim = safeWallpaperDim,
            weatherForecastDays = safeForecastDays,
            appShortcutCount = safeShortcutCount,
            soundboardButtonCount = safeSoundboardCount,
            sidebarWidth = safeSidebarWidth,
            sidebarIconScale = safeSidebarIconScale,
            tileAlpha = safeTileAlpha,
            spectrumGain = safeSpectrumGain,
            spectrumBarCount = safeSpectrumBarCount,
            spectrumAttackMs = safeSpectrumAttackMs,
            spectrumDecayMs = safeSpectrumDecayMs,
            spectrumResponse = safeSpectrumResponse,
            autoLaunchDelaySec = safeAutoLaunchDelay,
            widgetLayout = if (sanitizedLayout.isNotEmpty()) sanitizedLayout else defaultWidgetLayout(),
            homePageAltLayout = sanitizedAltLayout,
            widgetContentAlt = safeWidgetContentAlt,
            floatingPanelActions = safeFloatingActions,
            floatingPanelScale = safeFloatingScale,
            floatingPanelOpacity = safeFloatingOpacity
        ))
    }
}
