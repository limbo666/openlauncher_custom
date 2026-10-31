package com.openlauncher.app.ui.widget

import android.content.Intent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.openlauncher.app.obd.ObdReading
import com.openlauncher.app.obd.ObdSession
import com.openlauncher.app.obd.PairedAdapter
import com.openlauncher.app.obd.hasBluetoothPermission
import com.openlauncher.app.obd.pairedAdapters
import com.openlauncher.app.ui.theme.LocalDayMode
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
    val labelColor = customContentColor?.copy(alpha = 0.65f) ?: if (isDayMode) Color(0xFF666666) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.50f)
    val dimColor = customContentColor?.copy(alpha = 0.45f) ?: if (isDayMode) Color(0xFF888888) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)

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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        val roomyH = maxHeight >= 130.dp
        val isNarrow = maxWidth < 180.dp

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
                if (!isEditing) {
                    TextButton(
                        onClick = ::choose,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text(
                            text = if (adapterAddress.isEmpty()) "CHOOSE" else "CHANGE",
                            color = accent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Main Telemetry Readout
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when {
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
                                text = "Tap 'CHOOSE' above to connect",
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
                        ObdMetricsGrid(
                            reading = session.reading,
                            metric = metric,
                            showSecondary = roomyH,
                            isNarrow = isNarrow,
                            contentColor = contentColor,
                            labelColor = labelColor
                        )
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

@Composable
private fun ObdMetricsGrid(
    reading: ObdReading,
    metric: Boolean,
    showSecondary: Boolean,
    isNarrow: Boolean,
    contentColor: Color,
    labelColor: Color
) {
    fun temp(celsius: Int?): String = celsius?.let {
        if (metric) "$it°C" else "${(it * 9f / 5f + 32f).roundToInt()}°F"
    } ?: "—"

    fun pct(value: Int?): String = value?.let { "$it%" } ?: "—"

    val speed = reading.speedKmh?.let {
        if (metric) "$it km/h" else "${(it * 0.621371f).roundToInt()} mph"
    } ?: "—"

    val primaryCells = listOf(
        "RPM" to (reading.rpm?.toString() ?: "—"),
        "COOLANT" to temp(reading.coolantC),
        "BATTERY" to (reading.batteryVolts?.let { "%.1fV".format(it) } ?: "—"),
        "SPEED" to speed
    )

    val secondaryCells = if (showSecondary) listOf(
        "THROTTLE" to pct(reading.throttlePct),
        "LOAD" to pct(reading.loadPct),
        "FUEL" to pct(reading.fuelPct),
        "INTAKE" to temp(reading.intakeC)
    ) else emptyList()

    val cols = if (isNarrow) 2 else 4

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        primaryCells.chunked(cols).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { (label, value) ->
                    ObdCell(label, value, contentColor, labelColor, Modifier.weight(1f))
                }
            }
        }
        if (showSecondary) {
            secondaryCells.chunked(cols).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row.forEach { (label, value) ->
                        ObdCell(label, value, contentColor, labelColor, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ObdCell(
    label: String,
    value: String,
    contentColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = labelColor,
            letterSpacing = 1.sp,
            maxLines = 1
        )
        Text(
            text = value,
            fontSize = 17.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            maxLines = 1,
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

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(380.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(dialogBg)
                .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                .padding(18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = effectiveAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "OBD2 ADAPTERS",
                        color = textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = "SELECT PAIRED ELM327 BLUETOOTH ADAPTER",
                color = textSecondary,
                fontSize = 9.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 2.dp)
            )

            HorizontalDivider(
                color = cardBorder,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            if (adapters.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(itemBg)
                        .border(1.dp, itemBorder, RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BluetoothSearching,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "NO PAIRED BLUETOOTH DEVICES FOUND",
                        color = textPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Pair your ELM327 OBD-II adapter in Android Bluetooth Settings before connecting.",
                        color = textSecondary,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(adapters) { adapter ->
                        val isSelected = adapter.address.equals(currentAddress, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) effectiveAccent.copy(alpha = if (isDayMode) 0.12f else 0.18f)
                                    else itemBg
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) effectiveAccent else itemBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onPick(adapter) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = if (isSelected) effectiveAccent else textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = adapter.name.ifBlank { "Unknown OBD2 Device" },
                                    color = if (isSelected) effectiveAccent else textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = adapter.address,
                                    color = textSecondary,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(effectiveAccent)
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        color = if (effectiveAccent.luminance() > 0.5f) Color.Black else Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .border(1.dp, itemBorder, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "SELECT",
                                        color = textSecondary,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(itemBg)
                        .border(1.dp, itemBorder, RoundedCornerShape(6.dp))
                        .clickable {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = textPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "PAIR IN SETTINGS",
                        color = textPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(effectiveAccent.copy(alpha = if (isDayMode) 0.12f else 0.18f))
                        .border(1.dp, effectiveAccent, RoundedCornerShape(6.dp))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "CLOSE",
                        color = effectiveAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
