package com.openlauncher.app.util

import android.media.Rating
import android.media.session.MediaController
import android.media.session.PlaybackState

fun projectedPosition(position: Long, updatedAt: Long, now: Long, speed: Float, playing: Boolean, duration: Long): Long {
    val elapsed = if (playing && updatedAt > 0 && now >= updatedAt && speed.isFinite()) ((now - updatedAt) * speed).toLong() else 0L
    return (position.coerceAtLeast(0) + elapsed).coerceIn(0, duration.coerceAtLeast(0))
}

fun supportsAction(actions: Long, action: Long) = actions and action != 0L

fun sendExposedMediaAction(controller: MediaController?, id: String): Boolean = runCatching {
    val action = controller?.playbackState?.customActions?.firstOrNull { it.action == id } ?: return false
    controller.transportControls.sendCustomAction(action, action.extras)
    true
}.getOrDefault(false)

fun rateExposedMedia(controller: MediaController?, rating: Rating): Boolean = runCatching {
    if (controller == null || controller.ratingType != rating.ratingStyle ||
        !supportsAction(controller.playbackState?.actions ?: 0, PlaybackState.ACTION_SET_RATING)) return false
    controller.transportControls.setRating(rating)
    true
}.getOrDefault(false)
