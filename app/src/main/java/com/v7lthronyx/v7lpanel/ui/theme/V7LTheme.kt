package com.v7lthronyx.v7lpanel.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary          = V7LColors.accent,
    onPrimary        = V7LColors.bg0,
    primaryContainer = V7LColors.accentDark,
    onPrimaryContainer = V7LColors.accentLight,

    secondary        = V7LColors.purple,
    onSecondary      = V7LColors.bg0,
    secondaryContainer = V7LColors.purpleBg,
    onSecondaryContainer = V7LColors.purpleLight,

    tertiary         = V7LColors.green,
    onTertiary       = V7LColors.bg0,
    tertiaryContainer = V7LColors.greenBg,
    onTertiaryContainer = V7LColors.greenLight,

    background       = V7LColors.bg0,
    onBackground     = V7LColors.t1,

    surface          = V7LColors.bg1,
    onSurface        = V7LColors.t1,
    surfaceVariant   = V7LColors.bg2,
    onSurfaceVariant = V7LColors.t2,
    surfaceTint      = V7LColors.accent,

    surfaceBright    = V7LColors.bg3,
    surfaceDim       = V7LColors.bg0,
    surfaceContainer = V7LColors.bg2,
    surfaceContainerHigh = V7LColors.bg3,
    surfaceContainerHighest = V7LColors.bg4,
    surfaceContainerLow = V7LColors.bg1,
    surfaceContainerLowest = V7LColors.bg0,

    outline          = V7LColors.border,
    outlineVariant   = V7LColors.border2,

    error            = V7LColors.red,
    onError          = V7LColors.bg0,
    errorContainer   = V7LColors.redBg,
    onErrorContainer = V7LColors.redLight,

    inverseSurface       = V7LColors.t0,
    inverseOnSurface     = V7LColors.bg1,
    inversePrimary       = V7LColors.accentDark,
    scrim                = Color(0xCC06080F),
)

@Composable
fun V7LTheme(
    accentHex: String = "#34E5A4",
    content: @Composable () -> Unit
) {
    val opt = accentOptionForHex(accentHex)
    CompositionLocalProvider(
        LocalAccent provides opt.color,
        LocalAccentLight provides opt.light
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme.copy(primary = opt.color, surfaceTint = opt.color),
            typography  = V7LTypography,
            shapes      = V7LShapes,
            content     = content
        )
    }
}
