package com.v7lthronyx.v7lpanel.ui.components

import com.v7lthronyx.v7lpanel.ui.theme.LocalAccent
import com.v7lthronyx.v7lpanel.ui.theme.LocalAccentLight
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v7lthronyx.v7lpanel.ui.theme.JetBrainsMono
import com.v7lthronyx.v7lpanel.ui.theme.V7LColors

@Composable
fun V7LTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    onSearch: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = V7LColors.t0,
                    letterSpacing = 0.5.sp
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = V7LColors.t3,
                        fontSize = 12.sp
                    )
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = V7LColors.t1
                    )
                }
            }
        },
        actions = {
            if (onSearch != null) {
                IconButton(onClick = onSearch) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = LocalAccent.current
                    )
                }
            }
            actions()
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = V7LColors.bg0,
            scrolledContainerColor = V7LColors.bg1,
            navigationIconContentColor = V7LColors.t1,
            titleContentColor = V7LColors.t0,
            actionIconContentColor = LocalAccent.current
        )
    )
}
