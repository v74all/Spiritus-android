package com.v7lthronyx.v7lpanel.ui.components

import com.v7lthronyx.v7lpanel.ui.theme.LocalAccent
import com.v7lthronyx.v7lpanel.ui.theme.LocalAccentLight
import androidx.compose.animation.core.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.v7lthronyx.v7lpanel.ui.theme.JetBrainsMono
import com.v7lthronyx.v7lpanel.ui.theme.V7LColors
import kotlinx.coroutines.delay

@Composable
fun AsciiTypewriter(
    text: String,
    modifier: Modifier = Modifier,
    durationMs: Int = 2000,
    fontSize: Int = 14,
    onComplete: () -> Unit = {}
) {
    var visibleCount by remember(text) { mutableIntStateOf(0) }

    LaunchedEffect(text) {
        visibleCount = 0
        if (text.isEmpty()) { onComplete(); return@LaunchedEffect }
        val delayPerChar = (durationMs.toLong() / text.length).coerceAtLeast(20L)
        for (i in text.indices) {
            delay(delayPerChar)
            visibleCount = i + 1
        }
        onComplete()
    }

    val blinkTransition = rememberInfiniteTransition(label = "cursor_blink")
    val cursorAlpha by blinkTransition.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "cursor_alpha"
    )

    Text(
        text = buildString {
            append(text.take(visibleCount))
            if (visibleCount < text.length || (visibleCount == text.length && cursorAlpha > 0.5f)) {
                append("█")
            }
        },
        fontFamily = JetBrainsMono,
        fontSize    = fontSize.sp,
        color       = LocalAccent.current,
        lineHeight  = (fontSize * 1.2f).sp,
        modifier    = modifier
    )
}
