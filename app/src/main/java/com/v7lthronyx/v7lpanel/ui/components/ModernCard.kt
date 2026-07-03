package com.v7lthronyx.v7lpanel.ui.components

import com.v7lthronyx.v7lpanel.ui.theme.LocalAccent
import com.v7lthronyx.v7lpanel.ui.theme.LocalAccentLight
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.v7lthronyx.v7lpanel.ui.theme.V7LColors

@Composable
fun ModernCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    elevation: Dp = 2.dp,
    cornerRadius: Dp = 16.dp,
    borderColor: Color = V7LColors.border,
    backgroundColor: Color = V7LColors.bg2,
    gradient: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val animatedElevation by animateDpAsState(
        targetValue = if (isPressed) elevation * 0.5f else elevation,
        animationSpec = tween(150),
        label = "elevation"
    )

    val animatedBorderColor by animateColorAsState(
        targetValue = if (isPressed && enabled) LocalAccent.current else borderColor,
        animationSpec = tween(200),
        label = "borderColor"
    )

    val animatedBackgroundColor by animateColorAsState(
        targetValue = if (isPressed && enabled) V7LColors.bg3 else backgroundColor,
        animationSpec = tween(200),
        label = "backgroundColor"
    )

    // Subtle springy press-scale for a more tactile feel on tappable cards.
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && enabled && onClick != null) 0.985f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale"
    )

    Card(
        modifier = modifier
            .scale(pressScale)
            .clip(RoundedCornerShape(cornerRadius))
            .then(
                if (onClick != null && enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            ),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = if (gradient) Color.Transparent else animatedBackgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = animatedElevation,
            pressedElevation = animatedElevation * 0.7f
        ),
        border = BorderStroke(1.dp, animatedBorderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (gradient) {
                        Modifier.background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    V7LColors.bg2,
                                    V7LColors.bg3.copy(alpha = 0.6f)
                                )
                            )
                        )
                    } else Modifier
                )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    }
}
