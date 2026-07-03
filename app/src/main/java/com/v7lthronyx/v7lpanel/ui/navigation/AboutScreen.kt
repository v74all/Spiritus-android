package com.v7lthronyx.v7lpanel.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v7lthronyx.v7lpanel.ui.theme.*

@Composable
fun AboutScreen(onBack: () -> Unit) {
    com.v7lthronyx.v7lpanel.ui.components.AuroraBackground(Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Back", tint = V7LColors.t0)
            }
            Text(
                "About",
                fontFamily = JetBrainsMono,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = V7LColors.t0
            )
        }

        Spacer(Modifier.height(40.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Spiritus",
                fontFamily = JetBrainsMono,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = LocalAccent.current,
                letterSpacing = 4.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "v2.0",
                fontFamily = FiraCode,
                fontSize = 14.sp,
                color = V7LColors.t2
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "Secure, fast, and private VPN.\nSupports VMess, VLESS, Trojan,\nShadowsocks, Hysteria2 & TUIC.",
                fontSize = 14.sp,
                color = V7LColors.t1,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            Spacer(Modifier.height(32.dp))
            Text(
                "Powered by Xray-core & sing-box",
                fontSize = 12.sp,
                fontFamily = FiraCode,
                color = V7LColors.t3
            )
        }
    }
    }
}
