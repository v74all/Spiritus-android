package com.v7lthronyx.v7lpanel.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v7lthronyx.v7lpanel.ui.theme.FiraCode
import com.v7lthronyx.v7lpanel.ui.theme.V7LColors
import kotlinx.coroutines.launch

@Composable
fun TerminalLogView(
    lines: List<String>,
    modifier: Modifier = Modifier,
    highlightRed: (String) -> Boolean = { false }
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(lines.lastIndex)
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .background(Color.Black)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(lines) { line ->
            val isRed = highlightRed(line)
            Text(
                text       = "> $line",
                fontFamily = FiraCode,
                fontSize   = 12.sp,
                color      = if (isRed) V7LColors.red else V7LColors.green,
                lineHeight = 16.sp
            )
        }
        item {
            Text(
                text       = "> _",
                fontFamily = FiraCode,
                fontSize   = 12.sp,
                color      = V7LColors.green
            )
        }
    }
}
