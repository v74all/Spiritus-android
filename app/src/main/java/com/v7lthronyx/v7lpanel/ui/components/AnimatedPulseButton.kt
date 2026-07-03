package com.v7lthronyx.v7lpanel.ui.components

import com.v7lthronyx.v7lpanel.ui.theme.LocalAccent
import com.v7lthronyx.v7lpanel.ui.theme.LocalAccentLight
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.v7lthronyx.v7lpanel.ui.theme.V7LColors
import com.v7lthronyx.v7lpanel.vpn.VpnStatus

@Composable
fun AnimatedPulseButton(
    status: VpnStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = status == VpnStatus.CONNECTED
    val isConnecting = status == VpnStatus.CONNECTING

    // Colors with gradient support
    val (baseColor, lightColor) = when (status) {
        VpnStatus.CONNECTED -> V7LColors.green to V7LColors.greenLight
        VpnStatus.CONNECTING -> V7LColors.yellow to V7LColors.yellowLight
        VpnStatus.ERROR -> V7LColors.red to V7LColors.redLight
        VpnStatus.DISCONNECTED -> LocalAccent.current to V7LColors.accentLight
    }

    val animatedColor by animateColorAsState(
        targetValue = baseColor,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "buttonColor"
    )

    val animatedLightColor by animateColorAsState(
        targetValue = lightColor,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "buttonLightColor"
    )

    // Enhanced Pulse Animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isConnected || isConnecting) 1.6f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    // Button scale on press
    val buttonScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "buttonScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(150.dp)
    ) {
        // Outer pulsing rings (multiple layers)
        if (isConnected || isConnecting) {
            // Outer ring
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                animatedColor.copy(alpha = pulseAlpha * 0.8f),
                                animatedLightColor.copy(alpha = pulseAlpha * 0.4f)
                            )
                        )
                    )
            )
            // Inner ring
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .scale(pulseScale * 0.7f)
                    .clip(CircleShape)
                    .background(
                        animatedColor.copy(alpha = pulseAlpha * 1.2f)
                    )
            )
        }

        // Main button with gradient
        Surface(
            modifier = Modifier
                .size(90.dp)
                .scale(buttonScale)
                .clip(CircleShape)
                .clickable { onClick() },
            color = Color.Transparent,
            shadowElevation = 12.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                animatedLightColor,
                                animatedColor
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PowerSettingsNew,
                    contentDescription = "Power",
                    tint = V7LColors.bg0,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}
