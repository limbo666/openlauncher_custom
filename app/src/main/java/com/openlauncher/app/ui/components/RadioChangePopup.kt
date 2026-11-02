package com.openlauncher.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.openlauncher.app.viewmodel.LauncherViewModel

/**
 * A small glassmorphism toast that fades in/out over the home-screen header when
 * the radio station changes (seek / preset / band). Opt-in via settings.
 *
 * The ViewModel holds the latest snapshot and auto-clears it after a few seconds,
 * so this only *renders* — it mirrors the last non-null event during the fade-out.
 */
@Composable
fun BoxScope.RadioChangePopup(
    event: LauncherViewModel.RadioPopupEvent?,
    enabled: Boolean,
    accent: Color,
    isDayMode: Boolean,
    modifier: Modifier = Modifier
) {
    // Keep the last non-null event so the exit animation still has content after
    // the ViewModel clears the event (fade-out doesn't flash empty).
    var displayedEvent by remember { mutableStateOf<LauncherViewModel.RadioPopupEvent?>(null) }
    LaunchedEffect(event?.id) {
        if (event != null) displayedEvent = event
    }

    AnimatedVisibility(
        visible = enabled && event != null,
        enter = fadeIn(tween(250)),
        exit = fadeOut(tween(500)),
        modifier = modifier
    ) {
        val ev = displayedEvent ?: return@AnimatedVisibility

        val bgColor   = if (isDayMode) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.10f)
        val borderCol = if (isDayMode) Color.Black.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.24f)
        val titleCol  = if (isDayMode) Color(0xFF111111) else Color.White

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = bgColor,
            border = BorderStroke(1.dp, borderCol),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (ev.artUri != null || ev.albumArt != null) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (isDayMode) Color.Black.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.08f))
                    ) {
                        if (ev.artUri != null) {
                            AsyncImage(
                                model = ev.artUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                filterQuality = FilterQuality.High,
                                modifier = Modifier.size(30.dp)
                            )
                        } else if (ev.albumArt != null) {
                            Image(
                                bitmap = ev.albumArt.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                filterQuality = FilterQuality.High,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }

                Column {
                    Text(
                        text = ev.stationName?.takeIf { it.isNotBlank() } ?: "RADIO",
                        color = titleCol,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = ev.freqText,
                        color = accent,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
