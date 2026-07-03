package com.v7lthronyx.v7lpanel.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.v7lthronyx.v7lpanel.ui.theme.Dz
import com.v7lthronyx.v7lpanel.ui.theme.LocalAccent

/**
 * The signature dark background of the V7L redesign: a deep navy base with
 * three soft radial blooms — accent (top-right), cyan (top-left), and a dark
 * vignette (bottom) — reproducing the mockup's:
 * `radial-gradient(520px 380px at 80% -8%, auroraA 0%, transparent 60%),
 *  radial-gradient(480px 420px at 5% 8%, auroraB 0%, transparent 58%),
 *  radial-gradient(700px 600px at 50% 118%, #10131c 0%, transparent 60%)`
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val accent = LocalAccent.current
    Box(modifier.fillMaxSize().background(Dz.phoneBg)) {
        Canvas(Modifier.fillMaxSize()) {
            val auroraA = accent.copy(alpha = 0.16f)
            val auroraB = Color(0xFF3BC9F0).copy(alpha = 0.10f)

            val centerA = Offset(size.width * 0.80f, -size.height * 0.05f)
            val radiusA = size.width * 0.95f
            drawCircle(
                brush = Brush.radialGradient(listOf(auroraA, Color.Transparent), center = centerA, radius = radiusA),
                radius = radiusA, center = centerA
            )

            val centerB = Offset(size.width * 0.05f, size.height * 0.06f)
            val radiusB = size.width * 0.85f
            drawCircle(
                brush = Brush.radialGradient(listOf(auroraB, Color.Transparent), center = centerB, radius = radiusB),
                radius = radiusB, center = centerB
            )

            val centerC = Offset(size.width * 0.5f, size.height * 1.06f)
            val radiusC = size.width * 1.2f
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFF10131C).copy(alpha = 0.9f), Color.Transparent),
                    center = centerC, radius = radiusC
                ),
                radius = radiusC, center = centerC
            )
        }
        content()
    }
}
