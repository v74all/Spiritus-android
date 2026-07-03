package com.v7lthronyx.v7lpanel.ui.components

import com.v7lthronyx.v7lpanel.ui.theme.LocalAccent
import com.v7lthronyx.v7lpanel.ui.theme.LocalAccentLight
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v7lthronyx.v7lpanel.ui.theme.FiraCode
import com.v7lthronyx.v7lpanel.ui.theme.V7LColors

data class ProtocolStyle(val text: Color, val bg: Color)

fun protocolStyle(key: String): ProtocolStyle = when (key.lowercase()) {
    "vmess"       -> ProtocolStyle(V7LColors.accent,  V7LColors.blueBg)
    "vless"       -> ProtocolStyle(V7LColors.purple,  Color(0x1FBC8CFF))
    "cdn"         -> ProtocolStyle(V7LColors.orange,  Color(0x1FF0883E))
    "trojan"      -> ProtocolStyle(V7LColors.red,     V7LColors.redBg)
    "grpc"        -> ProtocolStyle(V7LColors.green,   V7LColors.greenBg)
    "ss2022"      -> ProtocolStyle(V7LColors.yellow,  V7LColors.yellowBg)
    "httpupgrade" -> ProtocolStyle(V7LColors.t2,      V7LColors.bg3)
    else          -> ProtocolStyle(V7LColors.t2,      V7LColors.bg3)
}

@Composable
fun ProtocolBadge(key: String, modifier: Modifier = Modifier) {
    val style = protocolStyle(key)
    Text(
        text = key.uppercase(),
        fontFamily = FiraCode,
        fontSize = 10.sp,
        color = style.text,
        modifier = modifier
            .background(style.bg, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}
