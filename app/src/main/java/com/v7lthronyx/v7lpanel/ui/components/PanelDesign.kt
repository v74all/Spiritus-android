package com.v7lthronyx.v7lpanel.ui.components

import com.v7lthronyx.v7lpanel.ui.theme.LocalAccent
import com.v7lthronyx.v7lpanel.ui.theme.LocalAccentLight
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v7lthronyx.v7lpanel.ui.theme.Inter
import com.v7lthronyx.v7lpanel.ui.theme.JetBrainsMono
import com.v7lthronyx.v7lpanel.ui.theme.Vazirmatn
import com.v7lthronyx.v7lpanel.ui.theme.V7LColors

@Composable
fun V7LPanelBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    AuroraBackground(modifier = modifier.fillMaxWidth(), content = content)
}

@Composable
fun V7LGlassCard(
    modifier: Modifier = Modifier,
    radius: Dp = 20.dp,
    containerColor: Color = V7LColors.bg2.copy(alpha = 0.72f),
    borderColor: Color = V7LColors.border.copy(alpha = 0.72f),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(radius),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(content = content)
    }
}

@Composable
fun V7LIconTile(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    iconSize: Dp = 20.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(13.dp))
            .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
fun V7LSectionLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text.uppercase(),
        modifier = modifier.padding(start = 2.dp, bottom = 9.dp),
        fontFamily = Vazirmatn,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = LocalAccent.current,
        letterSpacing = 1.4.sp
    )
}

@Composable
fun V7LPill(
    selected: Boolean,
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null
) {
    val bg = if (selected) LocalAccent.current.copy(alpha = 0.12f) else V7LColors.bg2.copy(alpha = 0.72f)
    val border = if (selected) LocalAccent.current.copy(alpha = 0.32f) else V7LColors.border.copy(alpha = 0.72f)
    val fg = if (selected) LocalAccent.current else V7LColors.t1
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(30.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(30.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 15.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icon != null) Icon(icon, null, tint = fg, modifier = Modifier.size(13.dp))
        Text(text, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun V7LPanelSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val track by animateColorAsState(
        targetValue = if (checked) LocalAccent.current else V7LColors.bg4,
        animationSpec = tween(220),
        label = "switchTrack"
    )
    val knob by animateColorAsState(
        targetValue = if (checked) Color.White else V7LColors.t2,
        animationSpec = tween(220),
        label = "switchKnob"
    )
    val x by animateDpAsState(
        targetValue = if (checked) 19.dp else 0.dp,
        animationSpec = tween(220),
        label = "switchOffset"
    )
    Box(
        modifier = modifier
            .width(46.dp)
            .height(27.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(track)
            .clickable { onCheckedChange(!checked) }
            .padding(3.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = x)
                .size(21.dp)
                .clip(CircleShape)
                .background(knob)
        )
    }
}

@Composable
fun V7LInfoRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color = LocalAccent.current,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(V7LColors.bg2.copy(alpha = 0.72f))
            .border(1.dp, V7LColors.border.copy(alpha = 0.72f), RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        V7LIconTile(icon = icon, tint = tint, size = 38.dp, iconSize = 18.dp)
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = V7LColors.t0, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = V7LColors.t3, fontSize = 11.sp, lineHeight = 15.sp)
            }
        }
        if (trailing != null) Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
    }
}

@Composable
fun V7LMonoBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(13.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontFamily = JetBrainsMono,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
        )
    }
}

@Composable
fun V7LHeaderTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        modifier = modifier,
        fontFamily = Inter,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = V7LColors.t0
    )
}

fun protocolAccent(protocol: String): Color = when (protocol.uppercase()) {
    "VLESS" -> V7LColors.accent
    "VMESS" -> V7LColors.blue
    "TROJAN" -> V7LColors.yellow
    "SS", "SHADOWSOCKS" -> V7LColors.green
    "HY2", "HYSTERIA2", "HYSTERIA" -> V7LColors.pink
    "TUIC" -> V7LColors.orange
    "WIREGUARD", "WG" -> V7LColors.blueLight
    else -> V7LColors.t2
}

fun pingAccent(ms: Int): Color = when {
    ms <= 0 -> V7LColors.t3
    ms <= 120 -> V7LColors.green
    ms <= 260 -> V7LColors.yellow
    else -> V7LColors.red
}
