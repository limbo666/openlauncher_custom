package com.openlauncher.app.ui.screen

import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.LocationManager
import android.media.VolumeProvider
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openlauncher.app.data.AppSettings
import com.openlauncher.app.model.AppInfo
import com.openlauncher.app.model.NowPlayingState
import com.openlauncher.app.model.WeatherState
import com.openlauncher.app.service.MediaListenerService
import com.openlauncher.app.viewmodel.LauncherViewModel
import com.openlauncher.app.util.FileLog
import com.openlauncher.app.util.LocationData
import com.openlauncher.app.util.SafeExport
import kotlinx.coroutines.launch

private enum class DiagStatus { OK, WARN, ERROR }

@Composable
fun DiagnosticsScreen(
    settings: AppSettings,
    accent: Color,
    isDayMode: Boolean,
    location: LocationData?,
    bearing: Float,
    isOnline: Boolean,
    isWifi: Boolean,
    isData: Boolean,
    weather: WeatherState?,
    weatherError: String?,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    onOpenVolumeCalibration: () -> Unit = {},
    sessions: List<NowPlayingState> = emptyList(),
    apps: List<AppInfo> = emptyList(),
    hardwareRadio: LauncherViewModel.HardwareRadioState? = null,
    onRadioSeekUp: () -> Unit = {},
    onRadioSeekDown: () -> Unit = {},
    visualizerStatus: String = "",
    visualizerAvailable: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isMediaConnected by MediaListenerService.isConnected.collectAsState()

    var refreshTick by remember { mutableIntStateOf(0) }

    // ── Local system checks (re-evaluated on manual refresh) ─────────────────
    val locationPermission = remember(refreshTick) {
        val fine = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION)
        fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }
    val gpsProviderEnabled = remember(refreshTick) {
        runCatching {
            val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as? LocationManager
            lm?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
        }.getOrDefault(false)
    }
    val magnetometer = remember(refreshTick) {
        val sm = context.getSystemService(android.content.Context.SENSOR_SERVICE) as? SensorManager
        sm?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null
    }
    val accelerometer = remember(refreshTick) {
        val sm = context.getSystemService(android.content.Context.SENSOR_SERVICE) as? SensorManager
        sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
    }
    val overlayAllowed = remember(refreshTick) {
        Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(context)
    }
    val isDefaultLauncher = remember(refreshTick) {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        context.packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName == context.packageName
    }
    val screen = remember(refreshTick) {
        val dm = context.resources.displayMetrics
        val cfg = context.resources.configuration
        val size = when (cfg.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) {
            Configuration.SCREENLAYOUT_SIZE_SMALL -> "small"
            Configuration.SCREENLAYOUT_SIZE_NORMAL -> "normal"
            Configuration.SCREENLAYOUT_SIZE_LARGE -> "large"
            Configuration.SCREENLAYOUT_SIZE_XLARGE -> "xlarge"
            else -> "unknown"
        }
        ScreenInfo(
            widthPx = dm.widthPixels,
            heightPx = dm.heightPixels,
            densityDpi = dm.densityDpi,
            density = dm.density,
            scaledDensity = dm.scaledDensity,
            sizeLabel = size,
            orientation = if (cfg.orientation == Configuration.ORIENTATION_LANDSCAPE) "landscape" else "portrait"
        )
    }
    val appVersion = remember(refreshTick) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
    }
    val vendorInfo = remember(refreshTick) {
        val known = listOf(
            "com.microntek" to "Microntek/MTC",
            "com.syu" to "FYT/SYU",
            "com.forfan" to "Junsun/forfan",
            "com.topway" to "TopWay/TS",
            "com.nwd" to "NWD",
            "com.hct" to "HCT",
            "com.szchoiceway" to "szchoiceway",
            "com.navimods" to "NaviMods radio"
        )
        val pkgs = runCatching { context.packageManager.getInstalledApplications(0).map { it.packageName } }.getOrDefault(emptyList())
        val found = known.filter { (prefix, _) -> pkgs.any { it.startsWith(prefix) } }.map { it.second }
        found.ifEmpty { listOf("none detected") }
    }

    val adbEnabled = remember(refreshTick) {
        runCatching {
            Settings.Global.getInt(context.contentResolver, "adb_enabled", 0) == 1
        }.getOrDefault(false)
    }
    val hasRoot = remember(refreshTick) {
        listOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su",
            "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su"
        ).any { java.io.File(it).exists() }
    }

    val reportScope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            reportScope.launch {
                val text = runCatching {
                    buildReport(
                        context = context,
                        settings = settings,
                        appVersion = appVersion,
                        screen = screen,
                        vendor = vendorInfo.joinToString(", "),
                        isOnline = isOnline, isWifi = isWifi, isData = isData,
                        locationPermission = locationPermission,
                        gpsProviderEnabled = gpsProviderEnabled,
                        location = location, bearing = bearing,
                        magnetometer = magnetometer, accelerometer = accelerometer,
                        weather = weather, weatherError = weatherError,
                        isMediaConnected = isMediaConnected, overlayAllowed = overlayAllowed,
                        isDefaultLauncher = isDefaultLauncher
                    )
                }.getOrElse { e ->
                    "REPORT GENERATION FAILED: ${e}\n"
                }
                val mediaReport = buildMediaReport(sessions, apps, hardwareRadio, settings, visualizerAvailable, visualizerStatus)
                val fullText = text + mediaReport
                val exportError = SafeExport.write(context, uri, fullText, "status_report")
                FileLog.log("Diagnostics", "report export len=${fullText.length} result=${exportError ?: "OK"}")
            }
        }
    }

    fun statusOf(ok: Boolean, warn: Boolean = false): DiagStatus = when {
        ok -> DiagStatus.OK
        warn -> DiagStatus.WARN
        else -> DiagStatus.ERROR
    }

    val screenBg = MaterialTheme.colorScheme.background
    val textColor = if (isDayMode) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground
    val subColor = if (isDayMode) Color(0xFF666666) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
    val dividerColor = if (isDayMode) Color(0xFFCCCCCC) else Color(0xFF1E1E1E)
    val okColor = Color(0xFF3FA24A)
    val warnColor = Color(0xFFE0A020)
    val errColor = Color(0xFFE03131)

    fun dotColor(s: DiagStatus): Color = when (s) {
        DiagStatus.OK -> okColor
        DiagStatus.WARN -> warnColor
        DiagStatus.ERROR -> errColor
    }

    Box(modifier = modifier.fillMaxSize().background(screenBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Header ─────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.ArrowBack, "Back",
                    tint = textColor,
                    modifier = Modifier.size(20.dp).clickable { onBack() }
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "SYSTEM STATUS",
                    style = MaterialTheme.typography.titleLarge,
                    color = textColor,
                    letterSpacing = 3.sp,
                    fontSize = 14.sp
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Default.Refresh, "Refresh",
                    tint = accent,
                    modifier = Modifier.size(20.dp).clickable {
                        refreshTick++
                        onRefresh()
                    }
                )
            }
            HorizontalDivider(color = dividerColor)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                // ── Connectivity ───────────────────────────────────────────────
                DiagSection("Connectivity", textColor, dividerColor) {
                    DiagRow("Internet", if (isOnline) "ONLINE" else "OFFLINE", statusOf(isOnline), textColor, subColor, ::dotColor, accent, isDayMode)
                    DiagRow("Wi-Fi", if (isWifi) "CONNECTED" else "OFF", statusOf(isWifi), textColor, subColor, ::dotColor, accent, isDayMode)
                    DiagRow("Mobile data", if (isData) "CONNECTED" else "OFF", statusOf(isData), textColor, subColor, ::dotColor, accent, isDayMode)
                }

                // ── Location ───────────────────────────────────────────────────
                DiagSection("Location", textColor, dividerColor) {
                    DiagRow("Permission", if (locationPermission) "GRANTED" else "MISSING", statusOf(locationPermission), textColor, subColor, ::dotColor, accent, isDayMode)
                    DiagRow("GPS provider", if (gpsProviderEnabled) "ENABLED" else "DISABLED", statusOf(gpsProviderEnabled), textColor, subColor, ::dotColor, accent, isDayMode)
                    DiagRow("GPS fix", if (location != null) "FIX ACQUIRED" else "NO FIX", statusOf(location != null), textColor, subColor, ::dotColor, accent, isDayMode)
                    DiagRow(
                        "Coordinates",
                        if (location != null) "%.5f, %.5f".format(location.latitude, location.longitude) else "—",
                        if (location != null) DiagStatus.OK else DiagStatus.WARN, textColor, subColor, ::dotColor, accent, isDayMode
                    )
                    DiagRow(
                        "Altitude",
                        if (location != null) "%.1f m".format(location.altitude) else "—",
                        if (location != null) DiagStatus.OK else DiagStatus.WARN, textColor, subColor, ::dotColor, accent, isDayMode
                    )
                    DiagRow("Bearing", "%.0f°".format(bearing), DiagStatus.OK, textColor, subColor, ::dotColor, accent, isDayMode)
                }

                // ── Sensors ────────────────────────────────────────────────────
                DiagSection("Sensors", textColor, dividerColor) {
                    DiagRow("Compass (magnetometer)", if (magnetometer) "PRESENT" else "MISSING", statusOf(magnetometer), textColor, subColor, ::dotColor, accent, isDayMode)
                    DiagRow("Accelerometer", if (accelerometer) "PRESENT" else "MISSING", statusOf(accelerometer), textColor, subColor, ::dotColor, accent, isDayMode)
                }

                // ── Weather ────────────────────────────────────────────────────
                DiagSection("Weather", textColor, dividerColor) {
                    val hasData = weather != null && weather.isOffline == false
                    DiagRow("Data", when { hasData -> "AVAILABLE"; weather == null -> "NONE"; else -> "OFFLINE" }, statusOf(hasData, warn = weather != null), textColor, subColor, ::dotColor, accent, isDayMode)
                    DiagRow(
                        "Temperature",
                        weather?.temperatureDisplay(settings.unitSystem == com.openlauncher.app.data.UnitSystem.METRIC) ?: "—",
                        if (weather != null) DiagStatus.OK else DiagStatus.WARN, textColor, subColor, ::dotColor, accent, isDayMode
                    )
                    DiagRow("Location", weather?.locationName?.ifBlank { "—" } ?: "—", if (weather != null) DiagStatus.OK else DiagStatus.WARN, textColor, subColor, ::dotColor, accent, isDayMode)
                    DiagRow("Last error", weatherError ?: "none", if (weatherError == null) DiagStatus.OK else DiagStatus.ERROR, textColor, subColor, ::dotColor, accent, isDayMode)
                }

                // ── Permissions ────────────────────────────────────────────────
                DiagSection("Permissions", textColor, dividerColor) {
                    DiagRow("Notification access", if (isMediaConnected) "GRANTED" else "MISSING", statusOf(isMediaConnected), textColor, subColor, ::dotColor, accent, isDayMode) {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                    DiagRow("Draw over apps", if (overlayAllowed) "GRANTED" else "MISSING", statusOf(overlayAllowed), textColor, subColor, ::dotColor, accent, isDayMode) {
                        if (Build.VERSION.SDK_INT >= 23) {
                            context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    }
                    DiagRow("Location", if (locationPermission) "GRANTED" else "MISSING", statusOf(locationPermission), textColor, subColor, ::dotColor, accent, isDayMode) {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                    DiagRow("Default launcher", if (isDefaultLauncher) "ACTIVE" else "NOT SET", statusOf(isDefaultLauncher), textColor, subColor, ::dotColor, accent, isDayMode) {
                        context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }

                // ── Device & Screen ────────────────────────────────────────────
                DiagSection("Device & Screen", textColor, dividerColor) {
                    DiagRow("Resolution", "${screen.widthPx}×${screen.heightPx} px", DiagStatus.OK, textColor, subColor, ::dotColor, accent, isDayMode)
                    DiagRow("Density", "${screen.densityDpi} dpi (${screen.density}x)", DiagStatus.OK, textColor, subColor, ::dotColor, accent, isDayMode)
                    DiagRow("Scaled density", "${screen.scaledDensity}", DiagStatus.OK, textColor, subColor, ::dotColor, accent, isDayMode)
                    DiagRow("Screen size", screen.sizeLabel, DiagStatus.OK, textColor, subColor, ::dotColor, accent, isDayMode)
                    DiagRow("Orientation", screen.orientation, DiagStatus.OK, textColor, subColor, ::dotColor, accent, isDayMode)
                    DiagRow("Device", Build.MODEL, DiagStatus.OK, textColor, subColor, ::dotColor, accent, isDayMode)
                    DiagRow("Android", "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})", DiagStatus.OK, textColor, subColor, ::dotColor, accent, isDayMode)
                    DiagRow("Vendor packages", vendorInfo.joinToString(", "), DiagStatus.OK, textColor, subColor, ::dotColor, accent, isDayMode)
                    DiagRow("App", "v$appVersion", DiagStatus.OK, textColor, subColor, ::dotColor, accent, isDayMode)
                    DiagRow("UI scale", "${settings.uiScale} · text ${settings.textScale}", DiagStatus.OK, textColor, subColor, ::dotColor, accent, isDayMode)
                    DiagRow("Sidebar", "width ${settings.sidebarWidth}dp · icons ${"%.0f".format(settings.sidebarIconScale * 100)}%", DiagStatus.OK, textColor, subColor, ::dotColor, accent, isDayMode)
                    DiagRow("Tile alpha", "${"%.0f".format(settings.tileAlpha * 100)}%", DiagStatus.OK, textColor, subColor, ::dotColor, accent, isDayMode)
                    DiagRow("Day/Night mode", settings.dayNightMode.name, DiagStatus.OK, textColor, subColor, ::dotColor, accent, isDayMode)
                }

                // ── Access & Root ─────────────────────────────────────────────
                DiagSection("Access & Root", textColor, dividerColor) {
                    DiagRow("ADB debugging", if (adbEnabled) "ENABLED" else "DISABLED", statusOf(adbEnabled, warn = true), textColor, subColor, ::dotColor, accent, isDayMode)
                    DiagRow("Root (su)", if (hasRoot) "PRESENT" else "NOT FOUND", statusOf(hasRoot, warn = true), textColor, subColor, ::dotColor, accent, isDayMode)
                }

                // ── Media & Radio ─────────────────────────────────────────────
                DiagSection("Media & Radio", textColor, dividerColor) {
                    DiagRow(
                        "Radio package",
                        settings.radioPackage.ifBlank { "auto (heuristic)" },
                        DiagStatus.OK, textColor, subColor, ::dotColor, accent, isDayMode
                    )
                    DiagRow(
                        "Radio now",
                        hardwareRadio?.let { r -> "${r.band} ${r.freq}${r.stationName?.let { " · $it" } ?: ""}" } ?: "—",
                        if (hardwareRadio != null) DiagStatus.OK else DiagStatus.WARN, textColor, subColor, ::dotColor, accent, isDayMode
                    )
                    DiagRow(
                        "Spectrum capture",
                        (if (visualizerAvailable) "ACTIVE" else "NO CAPTURE") + " — " + visualizerStatus.ifBlank { "idle" },
                        if (visualizerAvailable) DiagStatus.OK else DiagStatus.WARN, textColor, subColor, ::dotColor, accent, isDayMode
                    )
                    if (sessions.isEmpty()) {
                        DiagRow("Media sessions", "NONE ACTIVE", DiagStatus.WARN, textColor, subColor, ::dotColor, accent, isDayMode)
                    } else {
                        sessions.forEach { s ->
                            DiagRow(
                                s.controller?.packageName ?: "?",
                                mediaSessionSummary(s),
                                DiagStatus.OK, textColor, subColor, ::dotColor, accent, isDayMode
                            )
                        }
                    }
                    val radioApps = apps.map { it.packageName }.filter { isRadioLikePackage(it) }.distinct()
                    DiagRow(
                        "Radio apps found",
                        if (radioApps.isEmpty()) "none" else radioApps.joinToString(", "),
                        if (radioApps.isNotEmpty()) DiagStatus.OK else DiagStatus.WARN, textColor, subColor, ::dotColor, accent, isDayMode
                    )
                }

                Spacer(Modifier.height(16.dp))

                // ── Export report ──────────────────────────────────────────────
                Button(
                    onClick = {
                        val ts = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
                        exportLauncher.launch("openlauncher_diag_$ts.txt")
                    },
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Icon(Icons.Default.FileDownload, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("SAVE STATUS REPORT", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onOpenVolumeCalibration,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Icon(Icons.Default.Tune, null, tint = accent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("VOLUME CALIBRATION", color = accent, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onRadioSeekUp,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent.copy(alpha = 0.2f)),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("RADIO SEEK ▲", color = accent, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
                    }
                    Button(
                        onClick = onRadioSeekDown,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent.copy(alpha = 0.2f)),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("RADIO SEEK ▼", color = accent, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

private data class ScreenInfo(
    val widthPx: Int,
    val heightPx: Int,
    val densityDpi: Int,
    val density: Float,
    val scaledDensity: Float,
    val sizeLabel: String,
    val orientation: String
)

@Composable
private fun DiagSection(
    title: String,
    textColor: Color,
    dividerColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.padding(top = 16.dp)) {
        Text(
            title.uppercase(),
            color = textColor.copy(alpha = 0.45f),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        HorizontalDivider(color = dividerColor)
        Column(content = content)
        HorizontalDivider(color = dividerColor)
    }
}

@Composable
private fun DiagRow(
    label: String,
    value: String,
    status: DiagStatus,
    textColor: Color,
    subColor: Color,
    dotColor: (DiagStatus) -> Color,
    accent: Color,
    isDayMode: Boolean,
    onClick: (() -> Unit)? = null
) {
    val rowModifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    Row(
        modifier = rowModifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor(status))
        )
        Spacer(Modifier.width(10.dp))
        Text(
            label,
            color = textColor,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            color = if (status == DiagStatus.ERROR) dotColor(status) else (if (status == DiagStatus.WARN) dotColor(status) else subColor),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            maxLines = 2
        )
        if (onClick != null) {
            Spacer(Modifier.width(4.dp))
            Text("›", color = accent, fontSize = 14.sp)
        }
    }
}

private fun describePlaybackType(t: Int): String = when (t) {
    MediaController.PlaybackInfo.PLAYBACK_TYPE_LOCAL -> "LOCAL"
    MediaController.PlaybackInfo.PLAYBACK_TYPE_REMOTE -> "REMOTE"
    else -> "UNKNOWN"
}

private fun describeVolumeControl(v: Int): String = when (v) {
    VolumeProvider.VOLUME_CONTROL_ABSOLUTE -> "ABS"
    VolumeProvider.VOLUME_CONTROL_RELATIVE -> "REL"
    VolumeProvider.VOLUME_CONTROL_FIXED -> "FIXED"
    else -> "NONE"
}

private fun describeMediaActions(actions: Long): String {
    val names = mutableListOf<String>()
    if (actions and PlaybackState.ACTION_PLAY != 0L) names += "PLAY"
    if (actions and PlaybackState.ACTION_PAUSE != 0L) names += "PAUSE"
    if (actions and PlaybackState.ACTION_SKIP_TO_NEXT != 0L) names += "NEXT"
    if (actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L) names += "PREV"
    if (actions and PlaybackState.ACTION_SEEK_TO != 0L) names += "SEEK"
    if (actions and PlaybackState.ACTION_STOP != 0L) names += "STOP"
    if (actions and PlaybackState.ACTION_FAST_FORWARD != 0L) names += "FF"
    if (actions and PlaybackState.ACTION_REWIND != 0L) names += "REW"
    return if (names.isEmpty()) "NONE" else names.joinToString(",")
}

private fun mediaSessionSummary(s: NowPlayingState): String {
    val info = s.controller?.playbackInfo
    val type = info?.let { describePlaybackType(it.playbackType) } ?: "-"
    val volCtrl = info?.let { describeVolumeControl(it.volumeControl) } ?: "-"
    val vol = info?.let { "${it.currentVolume}/${it.maxVolume}" } ?: "-"
    val state = if (s.isPlaying) "PLAYING" else "IDLE"
    val sid = if (s.audioSessionId > 0) "sid=${s.audioSessionId}" else "no-sid"
    return "$type · vol=$volCtrl($vol) · $state · $sid · ${describeMediaActions(s.actions)}"
}

private fun isRadioLikePackage(pkg: String): Boolean {
    val p = pkg.lowercase()
    return "radio" in p || "fmradio" in p || "tuner" in p || p.endsWith(".fm") || ".fm." in p
}

private fun buildMediaReport(
    sessions: List<NowPlayingState>,
    apps: List<AppInfo>,
    hardwareRadio: LauncherViewModel.HardwareRadioState?,
    settings: AppSettings,
    visualizerAvailable: Boolean = false,
    visualizerStatus: String = ""
): String {
    val sb = StringBuilder()
    sb.appendLine()
    sb.appendLine("MEDIA & RADIO")
    sb.appendLine("  Radio package: ${settings.radioPackage.ifBlank { "auto (heuristic)" }}")
    sb.appendLine("  Radio now: ${hardwareRadio?.let { "${it.band} ${it.freq}${it.stationName?.let { s -> " · $s" } ?: ""}" } ?: "—"}")
    sb.appendLine("  Spectrum capture: ${if (visualizerAvailable) "ACTIVE" else "NO CAPTURE"} — ${visualizerStatus.ifBlank { "idle" }}")
    sb.appendLine("  Media sessions (${sessions.size}):")
    if (sessions.isEmpty()) {
        sb.appendLine("    none active")
    } else {
        sessions.forEach { s ->
            val info = s.controller?.playbackInfo
            val type = info?.let { describePlaybackType(it.playbackType) } ?: "-"
            val volCtrl = info?.let { describeVolumeControl(it.volumeControl) } ?: "-"
            val vol = info?.let { "${it.currentVolume}/${it.maxVolume}" } ?: "-"
            val state = if (s.isPlaying) "PLAYING" else "IDLE"
            val sid = if (s.audioSessionId > 0) "sid=${s.audioSessionId}" else "no-sid"
            sb.appendLine("    ${s.controller?.packageName ?: "?"} | $type | vol=$volCtrl($vol) | $state | $sid | ${describeMediaActions(s.actions)} | \"${s.title}\"")
        }
    }
    val radioApps = apps.map { it.packageName }.filter { isRadioLikePackage(it) }.distinct()
    sb.appendLine("  Radio apps found: ${if (radioApps.isEmpty()) "none" else radioApps.joinToString(", ")}")
    return sb.toString()
}

private fun buildReport(
    context: android.content.Context,
    settings: AppSettings,
    appVersion: String,
    screen: ScreenInfo,
    vendor: String,
    isOnline: Boolean, isWifi: Boolean, isData: Boolean,
    locationPermission: Boolean, gpsProviderEnabled: Boolean,
    location: LocationData?, bearing: Float,
    magnetometer: Boolean, accelerometer: Boolean,
    weather: WeatherState?, weatherError: String?,
    isMediaConnected: Boolean, overlayAllowed: Boolean,
    isDefaultLauncher: Boolean
): String {
    val sb = StringBuilder()
    sb.appendLine("Open Launcher — System Status Report")
    sb.appendLine("Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}")
    sb.appendLine("=".repeat(40))
    sb.appendLine()
    sb.appendLine("APP")
    sb.appendLine("  Version: v$appVersion")
    sb.appendLine("  Day/Night mode: ${settings.dayNightMode.name}")
    sb.appendLine("  UI scale: ${settings.uiScale} | Text scale: ${settings.textScale}")
    sb.appendLine("  Sidebar width: ${settings.sidebarWidth}dp | Icon scale: ${settings.sidebarIconScale}")
    sb.appendLine("  Tile alpha: ${settings.tileAlpha}")
    sb.appendLine()
    sb.appendLine("DEVICE & SCREEN")
    sb.appendLine("  Model: ${Build.MODEL}")
    sb.appendLine("  Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
    sb.appendLine("  Vendor packages: $vendor")
    sb.appendLine("  Resolution: ${screen.widthPx}x${screen.heightPx} px")
    sb.appendLine("  Density: ${screen.densityDpi} dpi (${screen.density}x)")
    sb.appendLine("  Scaled density: ${screen.scaledDensity}")
    sb.appendLine("  Screen size: ${screen.sizeLabel}")
    sb.appendLine("  Orientation: ${screen.orientation}")
    sb.appendLine()
    sb.appendLine("CONNECTIVITY")
    sb.appendLine("  Online: $isOnline | Wi-Fi: $isWifi | Mobile data: $isData")
    sb.appendLine()
    sb.appendLine("LOCATION")
    sb.appendLine("  Permission: $locationPermission")
    sb.appendLine("  GPS provider: $gpsProviderEnabled")
    sb.appendLine("  Fix: ${location != null}")
    sb.appendLine("  Lat/Lon: ${location?.let { "%.5f, %.5f".format(it.latitude, it.longitude) } ?: "—"}")
    sb.appendLine("  Altitude: ${location?.let { "%.1f m".format(it.altitude) } ?: "—"}")
    sb.appendLine("  Bearing: ${"%.0f°".format(bearing)}")
    sb.appendLine()
    sb.appendLine("SENSORS")
    sb.appendLine("  Magnetometer (compass): $magnetometer")
    sb.appendLine("  Accelerometer: $accelerometer")
    sb.appendLine()
    sb.appendLine("WEATHER")
    sb.appendLine("  Data: ${when { weather == null -> "NONE"; weather.isOffline -> "OFFLINE"; else -> "AVAILABLE" }}")
    sb.appendLine("  Temperature: ${weather?.temperatureDisplay(settings.unitSystem == com.openlauncher.app.data.UnitSystem.METRIC) ?: "—"}")
    sb.appendLine("  Location: ${weather?.locationName?.ifBlank { "—" } ?: "—"}")
    sb.appendLine("  Last error: ${weatherError ?: "none"}")
    sb.appendLine()
    sb.appendLine("PERMISSIONS")
    sb.appendLine("  Notification access: $isMediaConnected")
    sb.appendLine("  Draw over apps: $overlayAllowed")
    sb.appendLine("  Location: $locationPermission")
    sb.appendLine("  Default launcher: $isDefaultLauncher")
    sb.appendLine()
    return sb.toString()
}
