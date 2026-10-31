package com.openlauncher.app.ui.widget

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.openlauncher.app.model.MediaAction
import com.openlauncher.app.util.MediaToggle
import com.openlauncher.app.util.mediaToggle

/** Use the player's own action icon, retaining its exact label for accessibility. */
@Composable
internal fun MediaActionIcon(action: MediaAction, packageName: String?, tint: Color) {
    val context = LocalContext.current
    val pixels = with(LocalDensity.current) { 24.dp.roundToPx() }
    val bitmap = remember(packageName, action.icon, pixels) {
        runCatching {
            if (packageName == null || action.icon == 0) null else
                context.packageManager.getResourcesForApplication(packageName)
                    .getDrawable(action.icon, null)?.toBitmap(pixels, pixels)?.asImageBitmap()
        }.getOrNull()
    }
    val stateIcon = action.active?.let { active ->
        when (mediaToggle(action.label)) {
            MediaToggle.LIKE -> if (active) Icons.Default.Favorite else Icons.Default.FavoriteBorder
            MediaToggle.DISLIKE -> if (active) Icons.Default.ThumbDown else Icons.Outlined.ThumbDown
            MediaToggle.SAVE -> if (active) Icons.Default.Bookmark else Icons.Default.BookmarkBorder
            else -> null
        }
    }
    if (stateIcon != null) Icon(stateIcon, action.label, Modifier.size(24.dp), tint = tint)
    else if (bitmap != null) Icon(BitmapPainter(bitmap), action.label, Modifier.size(24.dp), tint = tint)
    else Icon(
        when {
            action.label.contains("dislike", true) -> Icons.Default.ThumbDown
            action.label.contains("like", true) || action.label.contains("favorite", true) ->
                if (action.label.contains("remove", true) || action.label.contains("unlike", true)) Icons.Default.Favorite else Icons.Default.FavoriteBorder
            action.label.contains("shuffle", true) -> Icons.Default.Shuffle
            action.label.contains("repeat", true) -> Icons.Default.Repeat
            action.label.contains("queue", true) -> Icons.Default.QueueMusic
            action.label.contains("save", true) -> Icons.Default.BookmarkBorder
            action.label.contains("radio", true) -> Icons.Default.Radio
            else -> Icons.Default.PlayArrow
        },
        action.label,
        Modifier.size(24.dp),
        tint = tint
    )
}
