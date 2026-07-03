package com.v7lthronyx.v7lpanel.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.v7lthronyx.v7lpanel.ui.theme.V7LColors
import kotlin.math.roundToInt

@Composable
fun SwipeToAction(
    onDelete: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(targetValue = offsetX, label = "swipe_offset")
    val threshold = 120f

    Box(modifier = modifier.fillMaxWidth()) {
        // Delete (swipe left)
        if (animatedOffset < -20f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(V7LColors.redBg, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = V7LColors.red,
                    modifier = Modifier.padding(end = 24.dp)
                )
            }
        }
        // Toggle (swipe right)
        if (animatedOffset > 20f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(V7LColors.yellowBg, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    Icons.Filled.ToggleOn,
                    contentDescription = "Toggle",
                    tint = V7LColors.yellow,
                    modifier = Modifier.padding(start = 24.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            when {
                                offsetX < -threshold -> { onDelete(); offsetX = 0f }
                                offsetX > threshold  -> { onToggle(); offsetX = 0f }
                                else                 -> offsetX = 0f
                            }
                        },
                        onHorizontalDrag = { _, delta ->
                            offsetX = (offsetX + delta).coerceIn(-threshold * 1.2f, threshold * 1.2f)
                        }
                    )
                }
        ) {
            content()
        }
    }
}
