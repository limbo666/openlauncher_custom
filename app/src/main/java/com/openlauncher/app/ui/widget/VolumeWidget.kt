package com.openlauncher.app.ui.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openlauncher.app.util.FileLog
import com.openlauncher.app.util.MuteState

@Composable
fun VolumeWidget(
    accent: Color,
    isDayMode: Boolean = false,
    customContentColor: Color? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    var currentVol by remember {
        mutableIntStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
    }
    val maxVol = remember {
        audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    }
    // Is muted check
    val isMuted = currentVol == 0 || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioManager.isStreamMute(AudioManager.STREAM_MUSIC))
    LaunchedEffect(Unit) {
        FileLog.log("Volume", "init current=$currentVol max=$maxVol muted=$isMuted")
    }

    // Listen to physical volume changes (hardware knobs, steering wheel controls, third-party apps)
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                    val newVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    currentVol = newVol
                    if (newVol > 0) MuteState.preMuteVolume = newVol
                }
            }
        }
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        context.registerReceiver(receiver, filter)
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    val isDarkContent = customContentColor != null && (0.299f * customContentColor.red + 0.587f * customContentColor.green + 0.114f * customContentColor.blue) < 0.5f
    val contentColor = customContentColor ?: if (isDayMode) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground
    val subColor     = customContentColor?.copy(alpha = 0.65f) ?: if (isDayMode) Color(0xFF888888) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
    val barTrack     = if (isDarkContent) Color(0x33000000) else if (isDayMode) Color(0xFFDDDDDD) else Color(0xFF222222)
    val buttonBg     = if (isDarkContent) Color(0x18000000) else if (isDayMode) Color(0xFFE5E5E5) else Color(0xFF161616)
    val buttonBorder = if (isDarkContent) Color(0x30000000) else if (isDayMode) Color(0xFFCCCCCC) else Color(0xFF2A2A2A)

    val setVolume = { target: Int ->
        val safe = target.coerceIn(0, maxVol)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, safe, 0)
        currentVol = safe
        if (safe > 0) MuteState.preMuteVolume = safe
        FileLog.log("Volume", "set target=$target safe=$safe max=$maxVol")
    }

    val toggleMute = {
        if (isMuted) {
            val restore = if (MuteState.preMuteVolume > 0) MuteState.preMuteVolume else (maxVol / 3).coerceAtLeast(1)
            setVolume(restore)
        } else {
            MuteState.preMuteVolume = currentVol
            setVolume(0)
        }
    }

    val volumePercent = ((currentVol.toFloat() / maxVol) * 100).toInt()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        val totalWidth = maxWidth
        val totalHeight = maxHeight
        val isWide = totalWidth >= 220.dp
        val isNarrow = totalWidth < 160.dp
        val isCompactH = totalHeight < 140.dp

        if (totalHeight < 55.dp) {
            // Ultra-compact single horizontal bar
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Mute button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isMuted) Color(0xFFE03131).copy(alpha = 0.22f) else buttonBg)
                        .border(1.dp, if (isMuted) Color(0xFFE03131).copy(alpha = 0.7f) else buttonBorder, RoundedCornerShape(4.dp))
                        .clickable(onClick = toggleMute),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Mute",
                        tint = if (isMuted) Color(0xFFFF5252) else accent,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Volume Down
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(buttonBg)
                        .border(1.dp, buttonBorder, RoundedCornerShape(4.dp))
                        .clickable { setVolume(currentVol - 1) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Remove, "Volume Down", tint = contentColor, modifier = Modifier.size(15.dp))
                }

                // Center bar + percentage
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isMuted) "MUTED" else "$volumePercent%",
                            color = if (isMuted) Color(0xFFFF5252) else contentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "MEDIA STREAM",
                            color = subColor,
                            fontSize = 7.5.sp,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    // Interactive progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(barTrack)
                            .pointerInput(maxVol) {
                                detectTapGestures { offset ->
                                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                    setVolume((fraction * maxVol).toInt())
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(if (maxVol > 0) currentVol.toFloat() / maxVol else 0f)
                                .background(if (isMuted) Color(0xFF777777) else accent)
                        )
                    }
                }

                // Volume Up
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(buttonBg)
                        .border(1.dp, buttonBorder, RoundedCornerShape(4.dp))
                        .clickable { setVolume(currentVol + 1) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, "Volume Up", tint = contentColor, modifier = Modifier.size(15.dp))
                }
            }
        } else {
            // Adaptive layout for 1x1, 2x1, 2x2, etc. (guaranteed no overlap)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = if (isCompactH) 12.dp else 16.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top row: Volume % (with optional MEDIA STREAM underneath) + Tactile Mute Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Percentage on top, MEDIA STREAM directly underneath
                    Column(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isMuted) "MUTED" else "$volumePercent%",
                            color = if (isMuted) Color(0xFFFF5252) else contentColor,
                            fontSize = if (isNarrow) 17.sp else if (isCompactH) 19.sp else 24.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                            maxLines = 1
                        )
                        if (!isNarrow || totalHeight >= 115.dp) {
                            Text(
                                text = "MEDIA STREAM",
                                color = subColor,
                                fontSize = 7.sp,
                                letterSpacing = 1.sp,
                                maxLines = 1
                            )
                        }
                    }

                    // Right: Mute button (tactile, unpressed/pressed feedback, clean)
                    Box(
                        modifier = Modifier
                            .height(if (isCompactH) 28.dp else 32.dp)
                            .then(
                                when {
                                    isWide -> Modifier.widthIn(min = 60.dp).padding(horizontal = 2.dp)
                                    isNarrow -> Modifier.width(30.dp)
                                    else -> Modifier.width(36.dp)
                                }
                            )
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isMuted) Color(0xFFE03131).copy(alpha = 0.22f) else buttonBg)
                            .border(
                                1.dp,
                                if (isMuted) Color(0xFFE03131).copy(alpha = 0.75f) else buttonBorder,
                                RoundedCornerShape(4.dp)
                            )
                            .clickable(onClick = toggleMute),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = if (isNarrow) 2.dp else 6.dp)
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (isMuted) "Unmute" else "Mute",
                                tint = if (isMuted) Color(0xFFFF5252) else accent,
                                modifier = Modifier.size(if (isNarrow) 14.dp else 15.dp)
                            )
                            if (isWide) {
                                Text(
                                    text = if (isMuted) "MUTED" else "MUTE",
                                    color = if (isMuted) Color(0xFFFF5252) else contentColor,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }

                // Bottom section: Progress bar + Down/Up buttons grouped with a tight, ergonomic gap
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(if (isCompactH) 6.dp else 8.dp)
                ) {
                    // Volume Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isCompactH) 8.dp else 10.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(barTrack)
                            .pointerInput(maxVol) {
                                detectTapGestures { offset ->
                                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                    setVolume((fraction * maxVol).toInt())
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(if (maxVol > 0) currentVol.toFloat() / maxVol else 0f)
                                .background(if (isMuted) Color(0xFF777777) else accent)
                        )
                    }

                    // Down & Up buttons: in narrow 1x1 mode show clean tactile +/- icons; in wide mode show DOWN / UP text
                    val showButtonText = totalWidth >= 150.dp
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(if (isCompactH) 28.dp else 34.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(buttonBg)
                                .border(1.dp, buttonBorder, RoundedCornerShape(4.dp))
                                .clickable { setVolume(currentVol - 1) },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Remove, null, tint = contentColor, modifier = Modifier.size(if (isNarrow) 16.dp else 14.dp))
                                if (showButtonText) {
                                    Text("DOWN", color = contentColor, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(if (isCompactH) 28.dp else 34.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(buttonBg)
                                .border(1.dp, buttonBorder, RoundedCornerShape(4.dp))
                                .clickable { setVolume(currentVol + 1) },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Add, null, tint = contentColor, modifier = Modifier.size(if (isNarrow) 16.dp else 14.dp))
                                if (showButtonText) {
                                    Text("UP", color = contentColor, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
