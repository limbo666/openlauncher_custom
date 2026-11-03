package com.openlauncher.app.ui.components

import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Looping, muted video wallpaper rendered behind the launcher UI. Uses the
 * framework [VideoView] (hardware-decoded H.264 plays smoothly even on weak
 * head-unit chips) — no extra dependencies.
 *
 * The static wallpaper stays rendered UNDER this layer, so if the video fails
 * to decode the user still sees their image. Playback pauses automatically
 * while the launcher is not in the foreground.
 */
@Composable
fun VideoWallpaperLayer(
    videoUri: String,
    dim: Float,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val videoView = remember(context) { VideoView(context) }

    DisposableEffect(videoView, videoUri) {
        runCatching {
            videoView.setVideoURI(android.net.Uri.parse(videoUri))
            videoView.setOnPreparedListener { mp ->
                mp.isLooping = true
                mp.setVolume(0f, 0f) // always silent — never clash with radio/music
                videoView.start()
            }
            videoView.setOnErrorListener { _, _, _ -> true } // swallow; static image shows through
        }
        onDispose { runCatching { videoView.stopPlayback() } }
    }

    LaunchedEffect(visible, videoView) {
        if (visible) {
            runCatching { if (!videoView.isPlaying) videoView.start() }
        } else {
            runCatching { videoView.pause() }
        }
    }

    Box(modifier = modifier) {
        AndroidView(factory = { videoView }, modifier = Modifier.fillMaxSize())
        // Same adjustable dim veil used for the static wallpaper
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = dim.coerceIn(0f, 0.95f)))
        )
    }
}
