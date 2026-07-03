package com.v7lthronyx.v7lpanel.ui.theme

import androidx.compose.ui.graphics.Color

object V7LColors {
    // Imported Spiritus panel palette
    val bg0 = Color(0xFF08080C)    // deepest
    val bg1 = Color(0xFF0A0B10)    // main background
    val bg2 = Color(0xFF101219)    // cards
    val bg3 = Color(0xFF1B1E29)    // elevated cards
    val bg4 = Color(0xFF2A2E3A)    // controls
    val bg5 = Color(0xFF343A49)    // active states

    // Accent
    val accent  = Color(0xFF7B6CF6)
    val accent2 = Color(0xFF9D7BFF)
    val accentLight = Color(0xFFB9AEFF)
    val accentDark = Color(0xFF332E74)
    val accentDim = Color(0xFF29265A)

    // Status
    val connected = Color(0xFF34E5A4)
    val connecting = Color(0xFFFFB454)
    val disconnected = Color(0xFFFF5D6E)
    val error = Color(0xFFFF5D6E)

    // Text
    val t0 = Color(0xFFF4F6FB)
    val t1 = Color(0xFFDFE4EE)
    val t2 = Color(0xFFB7BECC)
    val t3 = Color(0xFF7C8598)
    val t4 = Color(0xFF535B6B)

    // Borders
    val border = Color(0x14FFFFFF)
    val border2 = Color(0x1FFFFFFF)
    val borderAccent = Color(0x4D7B6CF6)

    // Legacy status color aliases (used throughout UI components)
    val green      = Color(0xFF34E5A4)
    val greenLight = Color(0xFF79F3C6)
    val yellow     = Color(0xFFFFB454)
    val yellowLight = Color(0xFFFFD18A)
    val red        = Color(0xFFFF5D6E)
    val redLight   = Color(0xFFFF8D99)
    val purple     = accent2
    val purpleLight = Color(0xFFC8BCFF)
    val pink       = Color(0xFFF472B6)
    val orange     = Color(0xFFFF8A3D)
    val orangeLight = Color(0xFFFFB17A)
    val blue       = Color(0xFF3BC9F0)
    val blueLight  = Color(0xFF80E2FF)

    // Semantic backgrounds
    val greenBg  = Color(0x2234E5A4)
    val yellowBg = Color(0x22FFB454)
    val redBg    = Color(0x22FF5D6E)
    val blueBg   = Color(0x223BC9F0)
    val purpleBg = Color(0x227B6CF6)
    val orangeBg = Color(0x22FF8A3D)
    val accentBg = greenBg

    // Gradient colors for modern effects
    val gradientStart = accent
    val gradientEnd   = bg0
    val gradientAccent = accent2
}
