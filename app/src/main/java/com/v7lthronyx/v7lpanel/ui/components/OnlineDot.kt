package com.v7lthronyx.v7lpanel.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.v7lthronyx.v7lpanel.ui.theme.V7LColors

@Composable
fun OnlineDot(isOnline: Boolean, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "online_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 0.35f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(modifier = modifier.size(14.dp), contentAlignment = Alignment.Center) {
        if (isOnline) {
            // Glow ring
            Box(
                Modifier
                    .fillMaxSize()
                    .alpha(alpha * 0.45f)
                    .background(V7LColors.green, CircleShape)
            )
        }
        // Core dot
        Box(
            Modifier
                .size(10.dp)
                .alpha(if (isOnline) alpha else 1f)
                .background(
                    color = if (isOnline) V7LColors.green else V7LColors.t3,
                    shape = CircleShape
                )
        )
    }
}
