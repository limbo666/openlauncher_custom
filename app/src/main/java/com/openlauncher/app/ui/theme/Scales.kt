package com.openlauncher.app.ui.theme

import androidx.compose.runtime.compositionLocalOf

/** User interface scale factors (Text Scale × UI Scale), shared app-wide. */
data class UiScales(val text: Float, val ui: Float) {
    fun ts(): Float = text.coerceIn(0.6f, 2.5f)
    fun us(): Float = ui.coerceIn(0.6f, 2.5f)
    companion object {
        val DEFAULT = UiScales(1f, 1f)
    }
}

/**
 * Provided once around the app content; every dialog reads this so its text
 * follows Text Scale and its panel/geometry follows UI Scale, consistently.
 */
val LocalUiScales = compositionLocalOf { UiScales.DEFAULT }
