package com.openlauncher.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.openlauncher.app.util.FileLog
import com.openlauncher.app.util.HeadUnitInspector
import com.openlauncher.app.util.TapSound
import com.openlauncher.app.util.VolumeController
import com.openlauncher.app.util.VolumeProtocols
import kotlinx.coroutines.delay

/**
 * "IR remote"-style volume calibration wizard. Each protocol is a probe: tapping TEST
 * sends volume=0 then restores, so the user can hear whether that channel actually
 * reaches the amplifier. Ignoring a protocol that the unit does not listen to is safe.
 */
@Composable
fun VolumeCalibrationScreen(
    currentProtocol: String,
    accent: Color,
    isDayMode: Boolean,
    onSelectProtocol: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var testingId by remember { mutableStateOf<String?>(null) }
    var confirmId by remember { mutableStateOf<String?>(null) }

    val audioInfo = remember { VolumeController.collectAudioInfo(context) }
    LaunchedEffect(Unit) { FileLog.log("AudioInfo", audioInfo) }

    var snifferArmed by remember { mutableStateOf(false) }
    val snifferEvents = remember { mutableStateListOf<String>() }
    DisposableEffect(snifferArmed) {
        if (snifferArmed) {
            HeadUnitInspector.start(context) { event ->
                snifferEvents.add(0, event)
                if (snifferEvents.size > 40) snifferEvents.removeAt(snifferEvents.lastIndex)
            }
        } else {
            HeadUnitInspector.stop()
        }
        onDispose { HeadUnitInspector.stop() }
    }

    val screenBg = MaterialTheme.colorScheme.background
    val textColor = if (isDayMode) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground
    val subColor = if (isDayMode) Color(0xFF666666) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
    val cardBg = if (isDayMode) Color(0xFFF5F5F5) else Color(0xFF141414)
    val dividerColor = if (isDayMode) Color(0xFFCCCCCC) else Color(0xFF1E1E1E)

    fun startTest(id: String) {
        if (testingId != null) return
        testingId = id
        confirmId = null
    }

    LaunchedEffect(testingId) {
        val id = testingId ?: return@LaunchedEffect
        val proto = VolumeProtocols.byId(id)
        val before = (VolumeController.level.value ?: 0).takeIf { it > 0 }
            ?: (VolumeController.max.value / 2).coerceAtLeast(1)
        FileLog.log("Volume", "wizard start ${proto.id} before=$before")
        VolumeController.applyProtocolForTest(context, proto, 0)
        delay(1700)
        VolumeController.applyProtocolForTest(context, proto, before)
        testingId = null
        confirmId = id
    }

    if (confirmId != null) {
        AlertDialog(
            onDismissRequest = { confirmId = null },
            containerColor = cardBg,
            title = { Text("Did the volume mute?", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "If you heard the sound cut out for a moment and come back, this protocol works.",
                    color = subColor,
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    TapSound.play()
                    val id = confirmId!!
                    onSelectProtocol(id)
                    FileLog.log("Volume", "wizard confirmed $id")
                    confirmId = null
                }) { Text("YES, USE IT", color = accent, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            },
            dismissButton = {
                TextButton(onClick = { TapSound.play(); confirmId = null }) {
                    Text("NO", color = subColor, fontSize = 12.sp)
                }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize().background(screenBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.ArrowBack, "Back",
                    tint = textColor,
                    modifier = Modifier.size(20.dp).clickable { TapSound.play(); onBack() }
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "VOLUME CALIBRATION",
                    style = MaterialTheme.typography.titleLarge,
                    color = textColor,
                    letterSpacing = 2.sp,
                    fontSize = 14.sp
                )
                Spacer(Modifier.weight(1f))
                Text(
                    if (VolumeController.isMicrontek.value) "MCU DETECTED" else "AUTO",
                    color = accent,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp
                )
            }
            HorizontalDivider(color = dividerColor)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    "Every head unit exposes volume differently. Press TEST for a protocol, listen for the sound to cut out for ~1.5 s, then confirm. Choose the one that works — then it is used everywhere.",
                    color = subColor,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tip: play audio from an Android app (Spotify, YouTube) while testing — the hardware radio is MCU audio and may sit on a separate path that no protocol here can reach.",
                    color = subColor,
                    fontSize = 9.sp,
                    lineHeight = 12.sp
                )
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(cardBg)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ACTIVE", color = subColor, fontSize = 9.sp, letterSpacing = 1.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (currentProtocol == VolumeProtocols.AUTO)
                            "AUTO (${VolumeProtocols.byId(if (VolumeController.isMicrontek.value) "microntek_volset" else "stream").name})"
                        else VolumeProtocols.byId(currentProtocol).name,
                        color = textColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(14.dp))

                VolumeProtocols.all.forEach { proto ->
                    val isActive = currentProtocol == proto.id
                    val testing = testingId == proto.id
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(cardBg)
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(proto.name, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(2.dp))
                                Text(proto.detail, color = subColor, fontSize = 9.sp, lineHeight = 12.sp)
                            }
                            if (isActive) {
                                Spacer(Modifier.width(8.dp))
                                Text("✓", color = accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { TapSound.play(); startTest(proto.id) },
                                enabled = testingId == null,
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (testing) accent else accent.copy(alpha = 0.18f)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    if (testing) "LISTENING…" else "TEST",
                                    color = if (testing) Color.Black else accent,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                            Button(
                                onClick = {
                                    TapSound.play()
                                    onSelectProtocol(proto.id)
                                    FileLog.log("Volume", "wizard selected ${proto.id}")
                                },
                                enabled = testingId == null,
                                shape = RoundedCornerShape(6.dp),
                                border = if (isActive) null else BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isActive) accent.copy(alpha = 0.25f) else Color.Transparent
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    if (isActive) "ACTIVE" else "USE THIS",
                                    color = accent,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(16.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(cardBg).padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("KNOB SNIFFER", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(2.dp))
                            Text("Arm this, then turn the physical volume knob. It logs every Settings key or broadcast that changes — that reveals the real volume channel.", color = subColor, fontSize = 9.sp, lineHeight = 12.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Button(
                            onClick = { TapSound.play(); snifferArmed = !snifferArmed },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (snifferArmed) Color(0xFFE03131) else accent),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                if (snifferArmed) "STOP" else "ARM",
                                color = if (snifferArmed) Color.White else Color.Black,
                                fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                            )
                        }
                    }
                    if (snifferEvents.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        snifferEvents.take(20).forEach { ev ->
                            Text(ev, color = subColor, fontSize = 8.sp, fontFamily = FontFamily.Monospace, lineHeight = 11.sp)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("UNIT AUDIO INFO", color = subColor, fontSize = 9.sp, letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(cardBg)
                        .padding(10.dp)
                ) {
                    Text(
                        audioInfo,
                        color = subColor,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 12.sp
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Nothing worked? Use \"Standard Android\" first, then note the unit model — we can add a dedicated protocol.",
                    color = subColor,
                    fontSize = 9.sp,
                    lineHeight = 12.sp
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
