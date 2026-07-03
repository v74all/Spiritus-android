package com.v7lthronyx.v7lpanel.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.v7lthronyx.v7lpanel.ui.theme.V7LColors

@Composable
fun TrafficProgressBar(
    percent: Float,
    modifier: Modifier = Modifier.fillMaxWidth(),
    height: Int = 8
) {
    val animatedPercent by animateFloatAsState(
        targetValue = percent.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800),
        label = "progressAnimation"
    )

    val (startColor, endColor) = when {
        animatedPercent < 0.5f -> V7LColors.green to V7LColors.greenLight
        animatedPercent < 0.8f -> V7LColors.yellow to V7LColors.yellowLight
        else -> V7LColors.red to V7LColors.redLight
    }

    Box(
        modifier = modifier
            .height(height.dp)
            .clip(RoundedCornerShape(height.dp))
            .background(V7LColors.bg3)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedPercent)
                .clip(RoundedCornerShape(height.dp))
                .shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(height.dp),
                    clip = false
                )
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            startColor.copy(alpha = 0.9f),
                            endColor
                        )
                    )
                )
        )
    }
}
