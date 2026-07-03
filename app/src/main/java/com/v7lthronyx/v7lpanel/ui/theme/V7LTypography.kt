package com.v7lthronyx.v7lpanel.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.v7lthronyx.v7lpanel.R

// ── Real bundled type families (match the V7L design system) ──
// Space Grotesk → display / headings.  Vazirmatn → UI body (Latin + Persian).
// JetBrains Mono → numbers, IPs, traffic, monospaced values.
val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk_regular,  FontWeight.Normal),
    Font(R.font.space_grotesk_medium,   FontWeight.Medium),
    Font(R.font.space_grotesk_semibold, FontWeight.SemiBold),
    Font(R.font.space_grotesk_bold,     FontWeight.Bold),
)

val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_regular,  FontWeight.Normal),
    Font(R.font.vazirmatn_medium,   FontWeight.Medium),
    Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold,     FontWeight.Bold),
)

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular,  FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium,   FontWeight.Medium),
    Font(R.font.jetbrains_mono_semibold, FontWeight.SemiBold),
    Font(R.font.jetbrains_mono_bold,     FontWeight.Bold),
)

// Back-compat aliases used across existing screens.
val Inter    = Vazirmatn   // UI body
val FiraCode = JetBrainsMono

val V7LTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = SpaceGrotesk,
        fontSize = 31.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
        color = V7LColors.t0
    ),
    displayMedium = TextStyle(
        fontFamily = SpaceGrotesk,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.3).sp,
        color = V7LColors.t0
    ),
    displaySmall = TextStyle(
        fontFamily = SpaceGrotesk,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = V7LColors.t0
    ),
    headlineMedium = TextStyle(
        fontFamily = SpaceGrotesk,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = V7LColors.t0
    ),
    headlineSmall = TextStyle(
        fontFamily = SpaceGrotesk,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = V7LColors.t0
    ),
    titleLarge = TextStyle(
        fontFamily = SpaceGrotesk,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = V7LColors.t0
    ),
    titleMedium = TextStyle(
        fontFamily = Vazirmatn,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = V7LColors.t1
    ),
    titleSmall = TextStyle(
        fontFamily = Vazirmatn,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = V7LColors.t1
    ),
    bodyLarge = TextStyle(
        fontFamily = Vazirmatn,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        color = V7LColors.t1
    ),
    bodyMedium = TextStyle(
        fontFamily = Vazirmatn,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        color = V7LColors.t2
    ),
    bodySmall = TextStyle(
        fontFamily = Vazirmatn,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        color = V7LColors.t2
    ),
    labelLarge = TextStyle(
        fontFamily = Vazirmatn,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = V7LColors.t1
    ),
    labelMedium = TextStyle(
        fontFamily = JetBrainsMono,
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        color = V7LColors.accent
    ),
    labelSmall = TextStyle(
        fontFamily = JetBrainsMono,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        color = V7LColors.accent,
        letterSpacing = 0.sp
    )
)
