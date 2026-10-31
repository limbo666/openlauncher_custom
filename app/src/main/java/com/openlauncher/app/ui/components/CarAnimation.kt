package com.openlauncher.app.ui.components

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlin.math.roundToInt

/**
 * A user-selected car PNG, scaled to fit. Renders with its alpha channel so the
 * transparent background stays transparent (Coil + ContentScale.Fit).
 */
@Composable
fun CarImage(
    carImageUri: String,
    modifier: Modifier = Modifier,
    alpha: Float = 1f
) {
    if (carImageUri.isEmpty()) return
    AsyncImage(
        model = Uri.parse(carImageUri),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier.graphicsLayer { this.alpha = alpha }
    )
}

/**
 * Full-screen, touch-transparent overlay that drives the car image from off the
 * left edge to off the right edge — twice — with a soft fade-in/fade-out, then
 * reports completion via [onDone].
 */
@Composable
fun CarDriveOverlay(
    carImageUri: String,
    active: Boolean,
    onDone: () -> Unit
) {
    if (carImageUri.isEmpty()) return

    val alpha = remember { Animatable(0f) }
    val progress = remember { Animatable(0f) }
    val currentOnDone by rememberUpdatedState(onDone)

    LaunchedEffect(active, carImageUri) {
        if (active) {
            alpha.snapTo(0f)
            alpha.animateTo(1f, tween(400))
            repeat(2) {
                progress.snapTo(0f)
                progress.animateTo(1f, tween(1400, easing = LinearEasing))
            }
            alpha.animateTo(0f, tween(400))
            currentOnDone()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val carWidthPx = with(density) { 88.dp.toPx() }
        val screenWidthPx = with(density) { maxWidth.toPx() }

        // progress 0 → fully off the left edge; 1 → fully off the right edge
        val xPx = -carWidthPx + (screenWidthPx + carWidthPx) * progress.value

        CarImage(
            carImageUri = carImageUri,
            alpha = alpha.value,
            modifier = Modifier
                .size(width = 88.dp, height = 52.dp)
                .offset { IntOffset(xPx.roundToInt(), 0) }
                .align(Alignment.CenterStart)
        )
    }
}
