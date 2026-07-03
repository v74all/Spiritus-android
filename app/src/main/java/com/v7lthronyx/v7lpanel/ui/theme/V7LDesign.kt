package com.v7lthronyx.v7lpanel.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

/**
 * Design tokens for the Spiritus app redesign (Claude Design handoff).
 *
 * The base palette is a fixed dark theme; only the **accent** is user-
 * customisable (see [AccentPalette] + [LocalAccent]). Screens read exact hex
 * values from here so the implementation matches the mockup pixel-for-pixel.
 */
object Dz {
    // Backgrounds
    val phoneBg   = Color(0xFF0A0B10)   // phone body
    val sheetBg   = Color(0xFF101219)   // inner surfaces (button core, profile ring center)
    val powerA    = Color(0xFF1B1E29)   // power button gradient top
    val powerB    = Color(0xFF101219)   // power button gradient bottom

    // Text ramp
    val tHi   = Color(0xFFF4F6FB)   // headings
    val tIn   = Color(0xFFEEF1F8)   // inputs / status bar
    val t1    = Color(0xFFDFE4EE)
    val t2    = Color(0xFFB7BECC)
    val t3    = Color(0xFF8A92A6)   // secondary
    val t4    = Color(0xFF7C8598)   // tertiary
    val tMute = Color(0xFF535B6B)   // muted / dividers text
    val tNav  = Color(0xFF5A6273)   // inactive nav
    val tFaint = Color(0xFF3A4151)

    // Surfaces (white overlays on dark) — alpha-encoded
    val surf035 = Color(0x09FFFFFF)   // .035 card
    val surf04  = Color(0x0AFFFFFF)   // .04 input
    val surf05  = Color(0x0DFFFFFF)   // .05 chip/button
    val surf06  = Color(0x0FFFFFFF)   // .06 divider
    val border  = Color(0x12FFFFFF)   // .07 border
    val border2 = Color(0x14FFFFFF)   // .08 stronger border
    val border3 = Color(0x17FFFFFF)   // .09

    // Status / semantic
    val connected  = Color(0xFF34E5A4)   // green
    val connecting = Color(0xFFFFB454)   // amber
    val danger     = Color(0xFFFF5D6E)   // red
    val dangerSoft = Color(0xFFFF8D99)
    val cyan       = Color(0xFF3BC9F0)
    val pink       = Color(0xFFF472B6)
    val gold       = Color(0xFFFFC154)

    // Protocol accent colors (accent-relative ones resolved at call site)
    val protoVMess = cyan
    val protoTrojan = connecting
    val protoSS = connected
    val protoHy2 = pink

    fun pingColor(ms: Int): Color = when {
        ms <= 70 -> connected
        ms <= 110 -> connecting
        else -> danger
    }
}

/** A selectable accent and its complementary gradient/lighter shade. */
data class AccentOption(val name: String, val color: Color, val light: Color)

val AccentPalette = listOf(
    AccentOption("Emerald", Color(0xFF34E5A4), Color(0xFF6FF3C3)),
    AccentOption("Violet",  Color(0xFF7B6CF6), Color(0xFF9D7BFF)),
    AccentOption("Sky",     Color(0xFF3BC9F0), Color(0xFF74DCFA)),
    AccentOption("Rose",    Color(0xFFF472B6), Color(0xFFFA9BD0)),
    AccentOption("Amber",   Color(0xFFFF8A3D), Color(0xFFFFB073)),
)

/** Currently active accent color, provided by [V7LTheme]. */
val LocalAccent = staticCompositionLocalOf { AccentPalette.first().color }

/** Lighter companion for the active accent (gradient end / glows). */
val LocalAccentLight = staticCompositionLocalOf { AccentPalette.first().light }

// Accent-derived helpers
fun Color.soft(alpha: Float = 0.12f): Color = copy(alpha = alpha)
fun Color.glow(alpha: Float = 0.55f): Color = copy(alpha = alpha)

fun Color.toHex(): String {
    val r = (red * 255f).roundToInt()
    val g = (green * 255f).roundToInt()
    val b = (blue * 255f).roundToInt()
    return "#%02X%02X%02X".format(r, g, b)
}

/** Resolve the accent for an accent hex stored in settings (falls back to Emerald). */
fun accentOptionForHex(hex: String?): AccentOption {
    if (hex.isNullOrBlank()) return AccentPalette.first()
    val want = hex.trim()
    return AccentPalette.firstOrNull { it.color.toHex().equals(want, ignoreCase = true) }
        ?: AccentPalette.first()
}
