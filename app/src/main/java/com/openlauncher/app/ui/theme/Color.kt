package com.openlauncher.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

val Black       = Color(0xFF000000)
val White       = Color(0xFFFFFFFF)
val DimSurface  = Color(0xFF0D0D0D)
val CardSurface = Color(0xFF141414)
val DividerGray = Color(0xFF2A2A2A)
val TextMuted   = Color(0xFF888888)

// Preset accent colours
val AccentBlack = Color(0xFF000000)
val AccentWhite = Color(0xFFFFFFFF)
val AccentBlue  = Color(0xFF2979FF)
val AccentGreen = Color(0xFF00E676)
val AccentAmber = Color(0xFFFFAB00)
val AccentRed   = Color(0xFFFF1744)

val accentPresets      = listOf(AccentBlack, AccentWhite, AccentBlue, AccentGreen, AccentAmber, AccentRed)
val accentPresetLabels = listOf("Black", "White", "Blue", "Green", "Amber", "Red")

// ── Contrast-aware accent derivation ────────────────────────────────────────

private fun Color.lum(): Float = 0.299f * red + 0.587f * green + 0.114f * blue

private fun contrastRatio(a: Color, b: Color): Float {
    val la = a.lum() + 0.05f
    val lb = b.lum() + 0.05f
    return maxOf(la, lb) / minOf(la, lb)
}

/**
 * Nudges [accent] darker (on light surfaces) or lighter (on dark surfaces) in small
 * steps until it clears [minContrast] against [surface]. Never snaps to pure black or
 * white, so a coloured accent keeps its hue — a white accent on a light surface
 * resolves to a neutral dark gray, and vice versa.
 */
fun accentOnSurface(accent: Color, surface: Color, minContrast: Float = 3f): Color {
    if (contrastRatio(accent, surface) >= minContrast) return accent
    val toward = if (surface.lum() > 0.5f) Black else White
    var result = accent
    var steps = 0
    while (contrastRatio(result, surface) < minContrast && steps < 16) {
        result = lerp(result, toward, 0.12f)
        steps++
    }
    return result
}

/** Black or white, whichever reads better on [background]. */
fun contentColorOn(background: Color): Color =
    if (background.lum() > 0.5f) Black else White

/** A surface color derived from [base] — slightly lighter on dark bases, darker on
 *  light bases — so a tile reads as distinct from its background/sidebar. */
fun surfaceFrom(base: Color, strength: Float = 0.08f): Color =
    if (base.lum() > 0.5f) lerp(base, Black, strength) else lerp(base, White, strength)
