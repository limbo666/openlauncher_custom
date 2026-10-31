package com.openlauncher.app.ui.widget

import android.content.Intent
import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.openlauncher.app.data.SoundPadConfig

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SoundboardWidget(
    pads: List<SoundPadConfig>,
    accent: Color,
    isDayMode: Boolean = false,
    customContentColor: Color? = null,
    buttonCount: Int = 6,
    isEditing: Boolean = false,
    onUpdatePad: (index: Int, pad: SoundPadConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val context      = LocalContext.current
    val isLightMode  = isDayMode || (customContentColor != null && (0.299f * customContentColor.red + 0.587f * customContentColor.green + 0.114f * customContentColor.blue) < 0.5f)
    val contentColor = customContentColor ?: if (isLightMode) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground
    val dimColor     = customContentColor?.copy(alpha = 0.55f) ?: if (isLightMode) Color(0xFF888888) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
    val borderColor  = customContentColor?.copy(alpha = 0.25f) ?: if (isLightMode) Color(0xFFCCCCCC) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)

    var activePadIndex by remember { mutableStateOf<Int?>(null) }
    var assigningIndex by remember { mutableStateOf<Int?>(null) }

    val count = buttonCount.coerceIn(1, 6)
    val safePads = remember(pads, count) {
        if (pads.size >= count) pads.take(count)
        else pads + List(count - pads.size) { SoundPadConfig("+", synthType = "") }
    }

    // Outer grid with top padding = 22.dp to leave room for card header label "SOUNDBOARD"
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTall = maxHeight > maxWidth * 1.3f
        val isWide = maxWidth > maxHeight * 2.2f

        val cols = when (count) {
            1 -> 1
            2 -> if (isTall) 1 else 2
            3 -> if (isTall) 1 else 3
            4 -> if (isWide) 4 else if (isTall) 1 else 2
            5 -> 3
            else -> 3
        }
        val padWidth = (maxWidth - 20.dp - (6.dp * (cols - 1))) / cols
        val isNarrowPad = padWidth < 70.dp

        val baseFontSize = when {
            count == 1 -> 14.sp
            count == 2 -> 12.sp
            count <= 4 -> 10.sp
            !isNarrowPad -> 9.sp
            else -> null
        }

        val renderPad: @Composable (Int, Modifier) -> Unit = { idx, padModifier ->
            val pad = safePads[idx]
            val isActive = activePadIndex == idx
            SoundPadButton(
                idx = idx,
                pad = pad,
                isActive = isActive,
                isLightMode = isLightMode,
                isNarrowPad = isNarrowPad,
                accent = accent,
                borderColor = borderColor,
                contentColor = contentColor,
                dimColor = dimColor,
                isEditing = isEditing,
                overrideFontSize = baseFontSize,
                onPlay = {
                    activePadIndex = idx
                    playSoundPad(
                        context = context,
                        pad = pad,
                        onDone = { activePadIndex = null }
                    )
                },
                onAssign = { assigningIndex = idx },
                modifier = padModifier
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 10.dp, end = 10.dp, top = 22.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            when (count) {
                1 -> {
                    renderPad(0, Modifier.fillMaxSize())
                }
                2 -> {
                    if (isTall) {
                        repeat(2) { idx ->
                            renderPad(idx, Modifier.fillMaxWidth().weight(1f))
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            repeat(2) { idx ->
                                renderPad(idx, Modifier.fillMaxHeight().weight(1f))
                            }
                        }
                    }
                }
                3 -> {
                    if (isTall) {
                        repeat(3) { idx ->
                            renderPad(idx, Modifier.fillMaxWidth().weight(1f))
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            repeat(3) { idx ->
                                renderPad(idx, Modifier.fillMaxHeight().weight(1f))
                            }
                        }
                    }
                }
                4 -> {
                    if (isWide) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            repeat(4) { idx ->
                                renderPad(idx, Modifier.fillMaxHeight().weight(1f))
                            }
                        }
                    } else if (isTall) {
                        repeat(4) { idx ->
                            renderPad(idx, Modifier.fillMaxWidth().weight(1f))
                        }
                    } else {
                        // 2x2 grid
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            renderPad(0, Modifier.fillMaxHeight().weight(1f))
                            renderPad(1, Modifier.fillMaxHeight().weight(1f))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            renderPad(2, Modifier.fillMaxHeight().weight(1f))
                            renderPad(3, Modifier.fillMaxHeight().weight(1f))
                        }
                    }
                }
                5 -> {
                    // Row 1: 3 pads, Row 2: 2 pads
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (idx in 0..2) {
                            renderPad(idx, Modifier.fillMaxHeight().weight(1f))
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (idx in 3..4) {
                            renderPad(idx, Modifier.fillMaxHeight().weight(1f))
                        }
                    }
                }
                else -> { // count == 6
                    repeat(2) { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            repeat(3) { col ->
                                val idx = row * 3 + col
                                renderPad(idx, Modifier.fillMaxHeight().weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }

    assigningIndex?.let { idx ->
        PadAssignDialog(
            pad = safePads[idx],
            accent = accent,
            isDayMode = isDayMode,
            onDismiss = { assigningIndex = null },
            onSave = { updated ->
                onUpdatePad(idx, updated)
                assigningIndex = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SoundPadButton(
    idx: Int,
    pad: SoundPadConfig,
    isActive: Boolean,
    isLightMode: Boolean,
    isNarrowPad: Boolean,
    accent: Color,
    borderColor: Color,
    contentColor: Color,
    dimColor: Color,
    isEditing: Boolean,
    onPlay: () -> Unit,
    onAssign: () -> Unit,
    modifier: Modifier = Modifier,
    overrideFontSize: androidx.compose.ui.unit.TextUnit? = null
) {
    Box(
        modifier = modifier
            .border(
                1.dp,
                if (isActive) accent else borderColor,
                RoundedCornerShape(3.dp)
            )
            .clip(RoundedCornerShape(3.dp))
            .background(if (isActive) accent.copy(alpha = 0.18f) else (if (isLightMode) Color(0x10000000) else Color(0x10FFFFFF)))
            .then(
                if (!isEditing) Modifier.combinedClickable(
                    onClick = {
                        if (pad.label == "+" || (pad.audioUri.isEmpty() && pad.synthType.isEmpty())) {
                            onAssign()
                        } else {
                            onPlay()
                        }
                    },
                    onLongClick = onAssign
                ) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        val isPlus = pad.label == "+"
        val displayLabel = if (isPlus) "+" else pad.label.replace('_', '\n')
        val padFontSize = overrideFontSize ?: when {
            isPlus -> 16.sp
            !isNarrowPad -> 9.sp
            displayLabel.length <= 4 -> 8.5.sp
            displayLabel.length <= 7 -> 7.5.sp
            else -> 6.5.sp
        }

        val hasMultiLine = displayLabel.contains('\n')
        val padMaxLines = if (hasMultiLine || isNarrowPad) 2 else 1

        Text(
            text = displayLabel,
            color = if (isActive) accent else if (isPlus) dimColor else contentColor,
            fontSize = padFontSize,
            fontWeight = FontWeight.Bold,
            letterSpacing = if (isNarrowPad) 0.sp else 0.4.sp,
            maxLines = padMaxLines,
            lineHeight = if (padMaxLines > 1) (padFontSize.value + 1.5f).sp else padFontSize,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

@Composable
private fun PadAssignDialog(
    pad: SoundPadConfig,
    accent: Color,
    isDayMode: Boolean,
    onDismiss: () -> Unit,
    onSave: (SoundPadConfig) -> Unit
) {
    val context    = LocalContext.current
    val accentLum = 0.299f * accent.red + 0.587f * accent.green + 0.114f * accent.blue
    val effectiveAccent = if (isDayMode && accentLum > 0.65f) Color(0xFF111111)
                          else if (!isDayMode && accentLum < 0.25f) Color(0xFFEEEEEE)
                          else accent

    val menuBg     = if (isDayMode) Color(0xFFFFFFFF) else MaterialTheme.colorScheme.background
    val menuBorder = if (isDayMode) Color(0xFFDDE1E5) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
    val contentColor = if (isDayMode) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground
    val dimColor   = if (isDayMode) Color(0xFF666666) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
    val fieldBorder = if (isDayMode) Color(0xFFCED4DA) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)

    var labelText   by remember { mutableStateOf(pad.label) }
    var synthType   by remember { mutableStateOf(pad.synthType) }
    var audioUri    by remember { mutableStateOf(pad.audioUri) }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            audioUri = uri.toString()
            val rawName = uri.path?.substringAfterLast('/') ?: "custom_sound"
            labelText = rawName.substringAfterLast(':').substringBeforeLast('.')
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(menuBg)
                .border(1.dp, menuBorder, RoundedCornerShape(8.dp))
                .padding(18.dp)
                .width(340.dp), // Fixed size: increased from 220dp to 340dp for landscape headunit displays
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "ASSIGN PAD SOUND",
                color = contentColor,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            // Label field
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("PAD LABEL", color = dimColor, fontSize = 7.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
                BasicTextField(
                    value = labelText,
                    onValueChange = { if (it.length <= 12) labelText = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = contentColor,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    cursorBrush = SolidColor(effectiveAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, fieldBorder, RoundedCornerShape(2.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }

            // Preloaded Audio Selector (replaces old raw waveform synth generation)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("PRELOADED AUDIO", color = dimColor, fontSize = 7.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
                val preloadedSounds = listOf(
                    Triple("whats_up_doc", "doc", "whats_up_doc"),
                    Triple("adhimahadi_arnold_ill_be_back_8276", "be back", "be_back"),
                    Triple("a_few_moments_later", "later", "later"),
                    Triple("stamataaaaaaa", "stamata", "stamata"),
                    Triple("dragon_studio_cute_cat_meow_472372", "meow", "meow"),
                    Triple("universfield_wah_wah_horn_117724", "wah wah", "wah_wah"),
                    Triple("rooster", "rooster", "rooster"),
                    Triple("spring_boing", "boing", "boing"),
                    Triple("attention_chime", "chime", "chime")
                )
                preloadedSounds.chunked(3).forEach { rowItems ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowItems.forEach { (type, chipLabel, padLabel) ->
                            val active = synthType == type && audioUri.isEmpty()
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(28.dp)
                                    .border(1.dp, if (active) effectiveAccent else fieldBorder, RoundedCornerShape(3.dp))
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (active) effectiveAccent.copy(alpha = if (isDayMode) 0.12f else 0.20f) else Color.Transparent)
                                    .clickable { 
                                        synthType = type
                                        audioUri = ""
                                        labelText = padLabel
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    chipLabel,
                                    color = if (active) effectiveAccent else dimColor,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Custom audio file picker
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("CUSTOM AUDIO FILE", color = dimColor, fontSize = 7.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { filePicker.launch(arrayOf("audio/*")) },
                        modifier = Modifier.weight(1f).height(30.dp),
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = effectiveAccent),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (audioUri.isNotEmpty()) effectiveAccent else fieldBorder),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.AudioFile, null, tint = effectiveAccent, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (audioUri.isNotEmpty()) "CUSTOM FILE ASSIGNED" else "PICK AUDIO FILE",
                            color = effectiveAccent,
                            fontSize = 7.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (audioUri.isNotEmpty()) {
                        IconButton(
                            onClick = { audioUri = "" },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Clear, null, tint = dimColor, modifier = Modifier.size(14.dp))
                        }
                    }
                }
                if (audioUri.isNotEmpty()) {
                    Text(
                        audioUri.substringAfterLast('/').take(36),
                        color = dimColor.copy(alpha = 0.6f),
                        fontSize = 6.5.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (pad.label != "+" || pad.audioUri.isNotEmpty() || pad.synthType.isNotEmpty()) {
                OutlinedButton(
                    onClick = {
                        onSave(SoundPadConfig(
                            label     = "+",
                            audioUri  = "",
                            synthType = ""
                        ))
                    },
                    modifier = Modifier.fillMaxWidth().height(32.dp),
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF884444)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF884444)),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("CLEAR SOUND", color = Color(0xFF884444), fontSize = 7.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }

            // Save / Cancel row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(32.dp),
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = dimColor),
                    border = androidx.compose.foundation.BorderStroke(1.dp, fieldBorder),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("CANCEL", color = dimColor, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        onSave(SoundPadConfig(
                            label     = labelText.trim().ifEmpty { pad.label },
                            audioUri  = audioUri,
                            synthType = synthType
                        ))
                    },
                    modifier = Modifier.weight(1f).height(32.dp),
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = effectiveAccent,
                        contentColor = if (effectiveAccent.luminance() > 0.5f) Color.Black else Color.White
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("SAVE SOUND", color = if (effectiveAccent.luminance() > 0.5f) Color.Black else Color.White, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun playSoundPad(context: android.content.Context, pad: SoundPadConfig, onDone: () -> Unit) {
    val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    mainHandler.post {
        try {
            if (pad.audioUri.isNotEmpty()) {
                val player = MediaPlayer()
                try {
                    player.setDataSource(context, android.net.Uri.parse(pad.audioUri))
                    player.setOnCompletionListener { mp ->
                        mp.release()
                        onDone()
                    }
                    // Async playback errors (revoked SAF grant, deleted file) must
                    // also release the player and un-highlight the pad
                    player.setOnErrorListener { mp, _, _ ->
                        mp.release()
                        onDone()
                        true
                    }
                    // prepareAsync: a blocking prepare() on the main thread is an
                    // ANR risk for content URIs on slow head-unit storage
                    player.setOnPreparedListener { it.start() }
                    player.prepareAsync()
                } catch (e: Exception) {
                    // Release on synchronous failure — this leaked a native player per failed tap
                    runCatching { player.release() }
                    throw e
                }
            } else {
                var resName = pad.synthType.lowercase().trim()
                var resId = context.resources.getIdentifier(resName, "raw", context.packageName)
                if (resId == 0) {
                    // Fallback mapping for legacy synth types saved in user settings
                    resName = when (resName) {
                        "horn", "beep", "alert", "mario_jump", "jump" -> "whats_up_doc"
                        "kick", "snare", "bass", "mario_coin", "coin" -> "adhimahadi_arnold_ill_be_back_8276"
                        "boom" -> "a_few_moments_later"
                        "loud_fart", "fart" -> "stamataaaaaaa"
                        else -> "whats_up_doc"
                    }
                    resId = context.resources.getIdentifier(resName, "raw", context.packageName)
                }
                if (resId != 0) {
                    val player = MediaPlayer.create(context, resId)
                    if (player != null) {
                        player.setOnCompletionListener { mp ->
                            mp.release()
                            onDone()
                        }
                        player.setOnErrorListener { mp, _, _ ->
                            mp.release()
                            onDone()
                            true
                        }
                        player.start()
                    } else {
                        onDone()
                    }
                } else {
                    onDone()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("Soundboard", "Error playing pad", e)
            onDone()
        }
    }
}
