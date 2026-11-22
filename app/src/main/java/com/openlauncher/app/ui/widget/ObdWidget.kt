package com.openlauncher.app.ui.widget

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.openlauncher.app.data.defaultObdMetrics
import com.openlauncher.app.obd.ObdReading
import com.openlauncher.app.obd.ObdSession
import com.openlauncher.app.obd.PairedAdapter
import com.openlauncher.app.obd.hasBluetoothPermission
import com.openlauncher.app.obd.pairedAdapters
import com.openlauncher.app.ui.theme.LocalDayMode
import com.openlauncher.app.ui.theme.LocalHelpTextColor
import com.openlauncher.app.ui.theme.LocalUiScales
import kotlin.math.roundToInt

/**
 * Real-time OBD-II telemetry widget from a paired ELM327 Bluetooth adapter.
 * Adapts to launcher styling, typography, light/dark/custom background colors, and responsive card sizes.
 */
@Composable
fun ObdWidget(
    adapterAddress: String,
    adapterName: String,
    metric: Boolean,
    accent: Color,
    isDayMode: Boolean = false,
    customContentColor: Color? = null,
    isEditing: Boolean = false,
    onSelectAdapter: (address: String, name: String) -> Unit,
    fallbackImageUri: String = "",
    enabledMetrics: List<String> = defaultObdMetrics(),
    pickRequested: Boolean = false,
    onPickRequestHandled: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var permitted by remember { mutableStateOf(hasBluetoothPermission(context)) }
    var picking by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permitted = granted
        if (granted) picking = true
    }

    DisposableEffect(adapterAddress, permitted, isEditing) {
        if (adapterAddress.isNotEmpty() && permitted && !isEditing) {
            ObdSession.start(context, adapterAddress)
        }
        onDispose { ObdSession.stop() }
    }

    val session by ObdSession.state.collectAsState()
    val contentColor = customContentColor ?: if (isDayMode) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground
    val labelColor = LocalHelpTextColor.current ?: customContentColor?.copy(alpha = 0.65f) ?: if (isDayMode) Color(0xFF666666) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.50f)
    val dimColor = LocalHelpTextColor.current ?: customContentColor?.copy(alpha = 0.45f) ?: if (isDayMode) Color(0xFF888888) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)

    if (picking) {
        AdapterPickerDialog(
            currentAddress = adapterAddress,
            adapters = remember { pairedAdapters(context) },
            accent = accent,
            onPick = { adapter ->
                picking = false
                onSelectAdapter(adapter.address, adapter.name)
            },
            onDismiss = { picking = false }
        )
    }

    fun choose() {
        if (permitted) {
            picking = true
        } else if (Build.VERSION.SDK_INT >= 31) {
            permissionLauncher.launch(android.Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            picking = true
        }
    }

    // "Choose adapter" now lives in the long-press context menu, not on the face.
    LaunchedEffect(pickRequested) {
        if (pickRequested) {
            choose()
            onPickRequestHandled()
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        // Adaptive cell sizing: the more room the tile has, the larger each metric
        // cell (and therefore its icon and value) grows, and the grid wraps into
        // as many columns as fit at that minimum cell size.
        val cellMinSize = when {
            maxWidth >= 340.dp || maxHeight >= 200.dp -> 110.dp
            maxWidth >= 240.dp -> 85.dp
            else -> 70.dp
        }
        val valueSize = when {
            cellMinSize >= 100.dp -> 16.sp
            cellMinSize >= 80.dp -> 13.sp
            else -> 11.sp
        }
        val labelSize = when {
            cellMinSize >= 100.dp -> 9.sp
            cellMinSize >= 80.dp -> 8.sp
            else -> 7.sp
        }
        val iconSize = when {
            cellMinSize >= 100.dp -> 30.dp
            cellMinSize >= 80.dp -> 24.dp
            else -> 18.dp
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Bar: status & adapter select button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (session.status == ObdSession.Status.CONNECTED) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (session.status == ObdSession.Status.CONNECTED) accent else dimColor
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = when {
                        adapterAddress.isEmpty() -> "OBD2 ADAPTER NOT CHOSEN"
                        else -> adapterName.ifBlank { adapterAddress }.uppercase()
                    },
                    modifier = Modifier.weight(1f),
                    color = contentColor,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Main Telemetry Readout
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val connected = adapterAddress.isNotEmpty() && permitted &&
                    session.status == ObdSession.Status.CONNECTED
                if (!connected && fallbackImageUri.isNotEmpty()) {
                    AsyncImage(
                        model = Uri.parse(fallbackImageUri),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else when {
                    adapterAddress.isEmpty() -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Pair ELM327 in Android Bluetooth settings",
                                color = labelColor,
                                fontSize = 11.sp,
                                fontFamily = MaterialTheme.typography.bodyLarge.fontFamily
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Long-press → Choose Adapter",
                                color = dimColor,
                                fontSize = 10.sp
                            )
                        }
                    }
                    !permitted -> {
                        Box(
                            modifier = Modifier
                                .clickable(enabled = !isEditing) { choose() }
                                .padding(4.dp)
                        ) {
                            Text(
                                text = "Tap to grant Bluetooth permission",
                                color = accent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    session.status != ObdSession.Status.CONNECTED -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = session.message.ifBlank { "Connecting to adapter…" },
                                color = labelColor,
                                fontSize = 11.sp,
                                fontFamily = MaterialTheme.typography.bodyLarge.fontFamily
                            )
                        }
                    }
                    else -> {
                        val metrics = buildObdMetrics(session.reading, metric, enabledMetrics)
                        if (metrics.isEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "No metrics selected",
                                    color = labelColor,
                                    fontSize = 11.sp,
                                    fontFamily = MaterialTheme.typography.bodyLarge.fontFamily
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Long-press → Configure Metrics",
                                    color = dimColor,
                                    fontSize = 10.sp
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = cellMinSize),
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(metrics) { m ->
                                    ObdCell(
                                        metric = m,
                                        contentColor = contentColor,
                                        labelColor = labelColor,
                                        accent = accent,
                                        isDayMode = isDayMode,
                                        valueSize = valueSize,
                                        labelSize = labelSize,
                                        iconSize = iconSize
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Status message footer (if ignition is off while connected)
            if (session.status == ObdSession.Status.CONNECTED && session.message.contains("off", ignoreCase = true)) {
                Text(
                    text = session.message,
                    fontSize = 9.sp,
                    color = dimColor,
                    maxLines = 1,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

private data class ObdMetric(val label: String, val value: String, val icon: ImageVector)

private fun buildObdMetrics(
    reading: ObdReading,
    metric: Boolean,
    enabled: List<String>
): List<ObdMetric> {
    fun temp(celsius: Int?): String = celsius?.let {
        if (metric) "$it°C" else "${(it * 9f / 5f + 32f).roundToInt()}°F"
    } ?: "—"

    fun pct(value: Int?): String = value?.let { "$it%" } ?: "—"

    val speed = reading.speedKmh?.let {
        if (metric) "$it km/h" else "${(it * 0.621371f).roundToInt()} mph"
    } ?: "—"

    val enabledSet = enabled.toSet()
    // Canonical order; only metrics the user has enabled are emitted.
    return listOf(
        ObdMetric("RPM", reading.rpm?.toString() ?: "—", Icons.Default.Timeline),
        ObdMetric("SPEED", speed, Icons.Default.Speed),
        ObdMetric("COOLANT", temp(reading.coolantC), Icons.Default.Thermostat),
        ObdMetric("THROTTLE", pct(reading.throttlePct), Icons.Default.TrendingUp),
        ObdMetric("LOAD", pct(reading.loadPct), Icons.Default.Memory),
        ObdMetric("FUEL", pct(reading.fuelPct), Icons.Default.LocalGasStation),
        ObdMetric("INTAKE", temp(reading.intakeC), Icons.Default.Air),
        ObdMetric("BATTERY", reading.batteryVolts?.let { "%.1fV".format(it) } ?: "—", Icons.Default.BatteryFull)
    ).filter { it.label in enabledSet }
}

@Composable
private fun ObdCell(
    metric: ObdMetric,
    contentColor: Color,
    labelColor: Color,
    accent: Color,
    isDayMode: Boolean,
    valueSize: TextUnit,
    labelSize: TextUnit,
    iconSize: Dp
) {
    val cardBg = if (isDayMode) Color(0x0D000000) else Color(0x14FFFFFF)
    val cardBorder = if (isDayMode) Color(0x1A000000) else Color(0x14FFFFFF)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(metric.icon, null, tint = accent, modifier = Modifier.size(iconSize))
            Text(
                text = metric.label,
                fontSize = labelSize,
                fontWeight = FontWeight.Bold,
                color = labelColor,
                letterSpacing = 0.8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = metric.value,
            fontSize = valueSize,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun AdapterPickerDialog(
    currentAddress: String,
    adapters: List<PairedAdapter>,
    accent: Color = MaterialTheme.colorScheme.primary,
    onPick: (PairedAdapter) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDayMode = LocalDayMode.current
    val accentLum = 0.299f * accent.red + 0.587f * accent.green + 0.114f * accent.blue
    val effectiveAccent = if (isDayMode && accentLum > 0.65f) Color(0xFF111111)
                          else if (!isDayMode && accentLum < 0.25f) Color(0xFFEEEEEE)
                          else accent

    val dialogBg = if (isDayMode) Color(0xFFFFFFFF) else Color(0xFF141414)
    val cardBorder = if (isDayMode) Color(0xFFE2E8F0) else Color(0xFF262626)
    val itemBg = if (isDayMode) Color(0xFFF8F9FA) else Color(0xFF1A1A1A)
    val itemBorder = if (isDayMode) Color(0xFFCED4DA) else Color(0xFF282828)
    val textPrimary = if (isDayMode) Color(0xFF111111) else Color.White
    val textSecondary = if (isDayMode) Color(0xFF666666) else Color(0xFF888888)

    val scales = LocalUiScales.current
    val ts = scales.ts()
    val us = scales.us()

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width((380 * us).dp)
                .heightIn(max = (560 * us).dp)
                .clip(RoundedCornerShape((12 * us).dp))
                .background(dialogBg)
                .border(1.dp, cardBorder, RoundedCornerShape((12 * us).dp))
                .padding((18 * us).dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((8 * us).dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = effectiveAccent,
                        modifier = Modifier.size((18 * us).dp)
                    )
                    Text(
                        text = "OBD2 ADAPTERS",
                        color = textPrimary,
                        fontSize = (11 * ts).sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (2 * ts).sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size((28 * us).dp)
                        .clip(RoundedCornerShape((6 * us).dp))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = textSecondary,
                        modifier = Modifier.size((16 * us).dp)
                    )
                }
            }

            Text(
                text = "SELECT PAIRED ELM327 BLUETOOTH ADAPTER",
                color = textSecondary,
                fontSize = (9 * ts).sp,
                letterSpacing = (1 * ts).sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = (2 * us).dp)
            )

            HorizontalDivider(
                color = cardBorder,
                modifier = Modifier.padding(vertical = (12 * us).dp)
            )

            if (adapters.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape((8 * us).dp))
                        .background(itemBg)
                        .border(1.dp, itemBorder, RoundedCornerShape((8 * us).dp))
                        .padding((16 * us).dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy((8 * us).dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BluetoothSearching,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size((32 * us).dp)
                    )
                    Text(
                        text = "NO PAIRED BLUETOOTH DEVICES FOUND",
                        color = textPrimary,
                        fontSize = (10 * ts).sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (1 * ts).sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Pair your ELM327 OBD-II adapter in Android Bluetooth Settings before connecting.",
                        color = textSecondary,
                        fontSize = (10 * ts).sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = (240 * us).dp),
                    verticalArrangement = Arrangement.spacedBy((6 * us).dp)
                ) {
                    items(adapters) { adapter ->
                        val isSelected = adapter.address.equals(currentAddress, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape((8 * us).dp))
                                .background(
                                    if (isSelected) effectiveAccent.copy(alpha = if (isDayMode) 0.12f else 0.18f)
                                    else itemBg
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) effectiveAccent else itemBorder,
                                    RoundedCornerShape((8 * us).dp)
                                )
                                .clickable { onPick(adapter) }
                                .padding(horizontal = (12 * us).dp, vertical = (10 * us).dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy((10 * us).dp)
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = if (isSelected) effectiveAccent else textSecondary,
                                modifier = Modifier.size((20 * us).dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = adapter.name.ifBlank { "Unknown OBD2 Device" },
                                    color = if (isSelected) effectiveAccent else textPrimary,
                                    fontSize = (12 * ts).sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (0.5f * ts).sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = adapter.address,
                                    color = textSecondary,
                                    fontSize = (10 * ts).sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape((4 * us).dp))
                                        .background(effectiveAccent)
                                        .padding(horizontal = (6 * us).dp, vertical = (3 * us).dp)
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        color = if (effectiveAccent.luminance() > 0.5f) Color.Black else Color.White,
                                        fontSize = (8 * ts).sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (1 * ts).sp
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape((4 * us).dp))
                                        .border(1.dp, itemBorder, RoundedCornerShape((4 * us).dp))
                                        .padding(horizontal = (6 * us).dp, vertical = (3 * us).dp)
                                ) {
                                    Text(
                                        text = "SELECT",
                                        color = textSecondary,
                                        fontSize = (8 * ts).sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = (1 * ts).sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height((14 * us).dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy((8 * us).dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape((6 * us).dp))
                        .background(itemBg)
                        .border(1.dp, itemBorder, RoundedCornerShape((6 * us).dp))
                        .clickable {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }
                        .padding(vertical = (10 * us).dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = textPrimary,
                        modifier = Modifier.size((13 * us).dp)
                    )
                    Spacer(Modifier.width((6 * us).dp))
                    Text(
                        text = "PAIR IN SETTINGS",
                        color = textPrimary,
                        fontSize = (10 * ts).sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (1 * ts).sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape((6 * us).dp))
                        .background(effectiveAccent.copy(alpha = if (isDayMode) 0.12f else 0.18f))
                        .border(1.dp, effectiveAccent, RoundedCornerShape((6 * us).dp))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = (16 * us).dp, vertical = (10 * us).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "CLOSE",
                        color = effectiveAccent,
                        fontSize = (10 * ts).sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (1 * ts).sp
                    )
                }
            }
        }
    }
}
