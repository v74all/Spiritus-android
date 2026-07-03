package com.v7lthronyx.v7lpanel.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.v7lthronyx.v7lpanel.ui.theme.Inter
import com.v7lthronyx.v7lpanel.ui.theme.V7LColors
import kotlinx.coroutines.delay

@Composable
fun ConnectionTimer(
    connectedSince: Long?,
    modifier: Modifier = Modifier,
    /** Matches connection status colour (same family as “Connected” label). */
    emphasisColor: Color = V7LColors.connected
) {
    var elapsed by remember { mutableLongStateOf(0L) }

    LaunchedEffect(connectedSince) {
        if (connectedSince == null) {
            elapsed = 0L
            return@LaunchedEffect
        }
        while (true) {
            elapsed = (System.currentTimeMillis() - connectedSince) / 1000
            delay(1000)
        }
    }

    val hours = elapsed / 3600
    val minutes = (elapsed % 3600) / 60
    val seconds = elapsed % 60

    Text(
        text = "%02d:%02d:%02d".format(hours, minutes, seconds),
        modifier = modifier,
        style = TextStyle(
            fontFamily = Inter,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.1.sp,
            fontFeatureSettings = "tnum",
            color = if (connectedSince != null) emphasisColor else V7LColors.t3
        )
    )
}
