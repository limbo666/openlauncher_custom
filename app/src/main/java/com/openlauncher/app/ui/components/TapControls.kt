package com.openlauncher.app.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.openlauncher.app.util.TapSound

/**
 * Tap-sound variants of the Material3 Switch and FilterChip. Drop-in replacements:
 * they play the tap confirmation and then delegate, so settings toggles and chip
 * selections also give audible feedback (part of the essential control convention).
 */
@Composable
fun TapSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    interactionSource: MutableInteractionSource? = null
) {
    Switch(
        checked = checked,
        onCheckedChange = { v -> TapSound.play(); onCheckedChange?.invoke(v) },
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource
    )
}

@Composable
fun TapFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    colors: SelectableChipColors = FilterChipDefaults.filterChipColors()
) {
    FilterChip(
        selected = selected,
        onClick = { TapSound.play(); onClick() },
        label = label,
        modifier = modifier,
        colors = colors
    )
}
