package com.openlauncher.app.ui.widget

import android.content.Context
import android.media.AudioManager
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
import androidx.compose.ui.graphics.vector.ImageVector
import com.openlauncher.app.ui.theme.LocalHelpTextColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openlauncher.app.util.TapSound
import com.openlauncher.app.util.VolumeController

@Composable
fun VolumeWidget(
    accent: Color,
    isDayMode: Boolean = false,
    customContentColor: Color? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Drive the head-unit MCU volume (Microntek broadcast on MTC units; STREAM_MUSIC
    // fallback elsewhere). VolumeController owns the 0..30 level + mute state.
    LaunchedEffect(Unit) {
        VolumeController.init(context)
    }
    val mcuLevel by VolumeController.level.collectAsState()
    val mcuMuted by VolumeController.muted.collectAsState()
    val currentVol = mcuLevel ?: 0
    val maxVol by VolumeController.max.collectAsState()
    val isMuted = mcuMuted || currentVol == 0

    val isDarkContent = customContentColor != null && (0.299f * customContentColor.red + 0.587f * customContentColor.green + 0.114f * customContentColor.blue) < 0.5f
    val contentColor = customContentColor ?: if (isDayMode) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground
    val subColor     = LocalHelpTextColor.current ?: customContentColor?.copy(alpha = 0.65f) ?: if (isDayMode) Color(0xFF888888) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
    val barTrack     = if (isDarkContent) Color(0x33000000) else if (isDayMode) Color(0xFFDDDDDD) else Color(0xFF222222)
    val buttonBg     = if (isDarkContent) Color(0x18000000) else if (isDayMode) Color(0xFFE5E5E5) else Color(0xFF161616)
    val buttonBorder = if (isDarkContent) Color(0x30000000) else if (isDayMode) Color(0xFFCCCCCC) else Color(0xFF2A2A2A)

    val setVolume = { target: Int -> VolumeController.set(context, target) }
    val adjustVolume = { direction: Int -> VolumeController.adjust(context, direction) }
    val toggleMute = { VolumeController.toggleMute(context) }

    val volumePercent = ((currentVol.toFloat() / maxVol) * 100).toInt()
    val volumeFraction = if (maxVol > 0) (currentVol.toFloat() / maxVol).coerceIn(0f, 1f) else 0f
    val fillColor = if (isMuted) Color(0xFF777777) else accent

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        val totalWidth = maxWidth
        val totalHeight = maxHeight
        val isWide   = totalWidth >= 200.dp
        val isNarrow = totalWidth < 150.dp
        val isShort  = totalHeight < 100.dp    // one-row-height tiles (2x1 / 3x1)
        val isUltra  = totalHeight < 55.dp     // ultra-thin strip

        if (isUltra) {
            // Ultra-compact strip: [DOWN][MUTE][UP] then bar + percentage.
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                VolumeKey(
                    onClick = { TapSound.play(); adjustVolume(AudioManager.ADJUST_LOWER) },
                    height = 28.dp, bg = buttonBg, border = buttonBorder,
                    contentColor = contentColor, icon = Icons.Default.Remove,
                    modifier = Modifier.size(28.dp)
                )
                VolumeKey(
                    onClick = { TapSound.play(); toggleMute() },
                    height = 28.dp, bg = buttonBg, border = buttonBorder,
                    contentColor = contentColor,
                    icon = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    highlight = if (isMuted) Color(0xFFE03131) else null,
                    modifier = Modifier.size(28.dp)
                )
                VolumeKey(
                    onClick = { TapSound.play(); adjustVolume(AudioManager.ADJUST_RAISE) },
                    height = 28.dp, bg = buttonBg, border = buttonBorder,
                    contentColor = contentColor, icon = Icons.Default.Add,
                    modifier = Modifier.size(28.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isMuted) "MUTED" else "$volumePercent%",
                        color = if (isMuted) Color(0xFFFF5252) else contentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(3.dp))
                    VolumeBar(
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        fraction = volumeFraction,
                        barTrack = barTrack,
                        fillColor = fillColor,
                        maxVol = maxVol,
                        setVolume = setVolume
                    )
                }
            }
        } else {
            // Two rows: number + bar on top; [DOWN][MUTE][UP] on the bottom. This
            // keeps everything on two horizontal levels so short 2x1 tiles never
            // clip the buttons.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = if (isShort) 6.dp else 12.dp, bottom = if (isShort) 4.dp else 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top row: percentage + (label) on the left, bar fills the rest.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isShort) 10.dp else 12.dp)
                ) {
                    Column(modifier = Modifier.widthIn(min = 40.dp)) {
                        Text(
                            text = if (isMuted) "MUTED" else "$volumePercent%",
                            color = if (isMuted) Color(0xFFFF5252) else contentColor,
                            fontSize = when {
                                isNarrow -> 16.sp
                                isShort  -> 18.sp
                                else     -> 22.sp
                            },
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                            maxLines = 1
                        )
                        if (totalHeight >= 90.dp) {
                            Text(
                                text = "SYSTEM VOLUME",
                                color = subColor,
                                fontSize = 7.sp,
                                letterSpacing = 1.sp,
                                maxLines = 1
                            )
                        }
                    }
                    VolumeBar(
                        modifier = Modifier.weight(1f).height(if (isShort) 10.dp else 14.dp),
                        fraction = volumeFraction,
                        barTrack = barTrack,
                        fillColor = fillColor,
                        maxVol = maxVol,
                        setVolume = setVolume
                    )
                }

                // Bottom row: [DOWN][MUTE][UP] with the mute in the middle.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VolumeKey(
                        onClick = { TapSound.play(); adjustVolume(AudioManager.ADJUST_LOWER) },
                        height = if (isShort) 30.dp else 36.dp,
                        bg = buttonBg, border = buttonBorder, contentColor = contentColor,
                        icon = Icons.Default.Remove,
                        label = "DOWN", showLabel = isWide, isNarrow = isNarrow,
                        modifier = Modifier.weight(1f)
                    )
                    VolumeKey(
                        onClick = { TapSound.play(); toggleMute() },
                        height = if (isShort) 30.dp else 36.dp,
                        bg = buttonBg, border = buttonBorder, contentColor = contentColor,
                        icon = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        label = if (isMuted) "MUTED" else "MUTE",
                        showLabel = isWide, isNarrow = isNarrow,
                        highlight = if (isMuted) Color(0xFFE03131) else null,
                        modifier = Modifier.weight(1f)
                    )
                    VolumeKey(
                        onClick = { TapSound.play(); adjustVolume(AudioManager.ADJUST_RAISE) },
                        height = if (isShort) 30.dp else 36.dp,
                        bg = buttonBg, border = buttonBorder, contentColor = contentColor,
                        icon = Icons.Default.Add,
                        label = "UP", showLabel = isWide, isNarrow = isNarrow,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * A square-ish key used for DOWN / MUTE / UP. [highlight] (when non-null) tints the
 * key red — used to show the muted state without changing the key's shape or size.
 */
@Composable
private fun VolumeKey(
    onClick: () -> Unit,
    height: Dp,
    bg: Color,
    border: Color,
    contentColor: Color,
    icon: ImageVector,
    label: String? = null,
    showLabel: Boolean = false,
    isNarrow: Boolean = false,
    highlight: Color? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(4.dp))
            .background(highlight?.copy(alpha = 0.22f) ?: bg)
            .border(1.dp, highlight?.copy(alpha = 0.75f) ?: border, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                null,
                tint = if (highlight != null) Color(0xFFFF5252) else contentColor,
                modifier = Modifier.size(if (isNarrow) 16.dp else 14.dp)
            )
            if (showLabel && label != null) {
                Text(
                    label,
                    color = if (highlight != null) Color(0xFFFF5252) else contentColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    maxLines = 1
                )
            }
        }
    }
}

/** Interactive horizontal volume bar with a filled portion. */
@Composable
private fun VolumeBar(
    modifier: Modifier,
    fraction: Float,
    barTrack: Color,
    fillColor: Color,
    maxVol: Int,
    setVolume: (Int) -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(barTrack)
            .pointerInput(maxVol) {
                detectTapGestures { offset ->
                    val f = (offset.x / size.width).coerceIn(0f, 1f)
                    setVolume((f * maxVol).toInt())
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .background(fillColor)
        )
    }
}
