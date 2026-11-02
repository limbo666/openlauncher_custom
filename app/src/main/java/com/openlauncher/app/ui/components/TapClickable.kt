package com.openlauncher.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.Modifier
import com.openlauncher.app.util.TapSound

/**
 * Canonical tap-sound wrappers for Open Launcher controls.
 *
 * Convention (essential — keep this for every future control):
 *  - For `Modifier`-based controls, use [tapClickable] / [tapCombinedClickable].
 *  - For Material3 components (Button / IconButton / TextButton / Switch /
 *    FilterChip / Surface(onClick) / Slider), call [TapSound.play]() inside their
 *    handler lambda (onClick / onCheckedChange / onSelect / onValueChangeFinished).
 *
 * This plays an audible "tap registered" through the car speakers so a driver can
 * confirm an action without looking. It deliberately does NOT fire for empty-space
 * taps, scroll, long-press, or multi-finger gestures — only real control activation.
 */
fun Modifier.tapClickable(onClick: () -> Unit): Modifier =
    this.clickable { TapSound.play(); onClick() }

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.tapCombinedClickable(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
): Modifier = this.combinedClickable(
    onClick = { TapSound.play(); onClick() },
    onLongClick = onLongClick
)
