package com.v7lthronyx.v7lpanel.ui.screens.locations

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v7lthronyx.v7lpanel.SessionHolder
import com.v7lthronyx.v7lpanel.ui.components.*
import com.v7lthronyx.v7lpanel.ui.theme.*
import com.v7lthronyx.v7lpanel.vpn.VpnManager
import com.v7lthronyx.v7lpanel.vpn.VpnStatus

@Composable
fun LocationsScreen(
    viewModel: LocationsViewModel,
    onConnect: (label: String, uri: String) -> Unit
) {
    val configs by viewModel.filteredConfigs.collectAsState(initial = emptyList())
    val query by viewModel.searchQuery.collectAsState()
    val isPinging by viewModel.isPinging.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMsg by viewModel.error.collectAsState()
    val lang by viewModel.language.collectAsState(initial = "en")
    val vpnStatus by VpnManager.status.collectAsState()
    val connectedUri by VpnManager.connectedUri.collectAsState()

    V7LPanelBackground(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                V7LHeaderTitle(if (lang == "fa") "سرورها" else "Servers")
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    HeaderIconButton(Icons.Filled.Refresh, V7LColors.t2) { viewModel.loadConfigs() }
                    HeaderIconButton(Icons.Filled.Speed, LocalAccent.current, enabled = !isPinging) { viewModel.pingAll() }
                }
            }

            Spacer(Modifier.height(14.dp))

            V7LGlassCard(modifier = Modifier.fillMaxWidth(), radius = 14.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Search, null, tint = V7LColors.t3, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(11.dp))
                    BasicTextFieldProxy(
                        value = query,
                        onValueChange = viewModel::setSearchQuery,
                        placeholder = if (lang == "fa") "جستجوی مکان یا پروتکل" else "Search location or protocol",
                        modifier = Modifier.weight(1f)
                    )
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Close, null, tint = V7LColors.t3, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            if (isLoading) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = LocalAccent.current,
                    trackColor = V7LColors.bg2
                )
            }

            if (errorMsg != null) {
                Spacer(Modifier.height(8.dp))
                V7LGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    radius = 14.dp,
                    containerColor = V7LColors.redBg,
                    borderColor = V7LColors.red.copy(alpha = 0.18f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Warning, null, tint = V7LColors.red, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(errorMsg ?: "", color = V7LColors.redLight, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.loadConfigs() }) {
                            Text(if (lang == "fa") "تلاش مجدد" else "Retry", color = LocalAccent.current, fontSize = 12.sp)
                        }
                    }
                }
            }

            val protocols = configs.map { it.protocol }.distinct()
            var filterMode by remember { mutableStateOf("all") } // "all" | "fastest" | "favorites"
            var selectedProtocol by remember { mutableStateOf<String?>(null) }

            // ── primary filter row: All / Fastest / Favorites (matches design) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 13.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                V7LPill(
                    selected = filterMode == "all",
                    text = if (lang == "fa") "همه" else "All",
                    onClick = { filterMode = "all" }
                )
                V7LPill(
                    selected = filterMode == "fastest",
                    text = if (lang == "fa") "سریع‌ترین" else "Fastest",
                    icon = Icons.Filled.Bolt,
                    onClick = { filterMode = "fastest" }
                )
                V7LPill(
                    selected = filterMode == "favorites",
                    text = if (lang == "fa") "برگزیده‌ها" else "Favorites",
                    icon = Icons.Filled.Star,
                    onClick = { filterMode = "favorites" }
                )
            }

            // ── secondary row: per-protocol filter (useful with many protocols) ──
            if (protocols.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 8.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    V7LPill(selected = selectedProtocol == null, text = if (lang == "fa") "همه پروتکل‌ها" else "All protocols", onClick = { selectedProtocol = null })
                    protocols.forEach { proto ->
                        V7LPill(
                            selected = selectedProtocol == proto,
                            text = proto.uppercase(),
                            icon = if (selectedProtocol == proto) Icons.Filled.CheckCircle else null,
                            onClick = { selectedProtocol = proto }
                        )
                    }
                }
            }

            var displayConfigs = if (selectedProtocol == null) configs
                else configs.filter { it.protocol == selectedProtocol }
            displayConfigs = when (filterMode) {
                "favorites" -> displayConfigs.filter { it.isFavorite }
                "fastest" -> displayConfigs.sortedBy { if (it.latency <= 0) Int.MAX_VALUE else it.latency }
                else -> displayConfigs
            }

            val fastestUri = configs.filter { it.latency > 0 }.minByOrNull { it.latency }?.uri

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 11.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                items(displayConfigs, key = { it.uri }) { item ->
                    val isConnected = connectedUri == item.uri && vpnStatus == VpnStatus.CONNECTED
                    ConfigItemCard(
                        item = item,
                        isConnected = isConnected,
                        isFastest = item.uri == fastestUri,
                        onConnect = { onConnect(item.label, item.uri) },
                        onFavorite = { viewModel.toggleFavorite(item) },
                        onPing = { viewModel.pingItem(item) }
                    )
                }

                // ── subscription link card (panel/subscriber mode only) ──
                if (SessionHolder.role != "local" && SessionHolder.uuid.isNotBlank() && SessionHolder.serverUrl.isNotBlank()) {
                    item { SubLinkCard("${SessionHolder.serverUrl.trimEnd('/')}/sub/${SessionHolder.uuid}", lang) }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun HeaderIconButton(
    icon: ImageVector,
    tint: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .background(V7LColors.bg2.copy(alpha = 0.72f), RoundedCornerShape(12.dp))
            .border(1.dp, V7LColors.border.copy(alpha = 0.72f), RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = if (enabled) tint else V7LColors.t4, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun BasicTextFieldProxy(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = true,
        textStyle = TextStyle(
            color = V7LColors.t0,
            fontSize = 13.sp,
            fontFamily = Inter
        ),
        cursorBrush = SolidColor(LocalAccent.current),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isBlank()) {
                    Text(placeholder, color = V7LColors.t3, fontSize = 13.sp)
                }
                inner()
            }
        }
    )
}

@Composable
private fun ConfigItemCard(
    item: ConfigItem,
    isConnected: Boolean,
    isFastest: Boolean = false,
    onConnect: () -> Unit,
    onFavorite: () -> Unit,
    onPing: () -> Unit
) {
    val accent = LocalAccent.current
    val protoColor = protoColorFor(item.protocol, accent)
    val latencyColor = if (item.latency <= 0) V7LColors.t4 else Dz.pingColor(item.latency)
    val cc = item.label.filter { it.isLetter() }.take(2).uppercase().ifBlank { item.protocol.take(2).uppercase() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (isConnected) Dz.connected.copy(alpha = 0.08f) else Dz.surf035)
            .border(
                1.dp,
                if (isConnected) Dz.connected.copy(alpha = 0.4f) else Dz.border,
                RoundedCornerShape(18.dp)
            )
            .clickable { onConnect() }
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(protoColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Text(cc, fontFamily = JetBrainsMono, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = protoColor)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(item.label, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = Dz.tHi, maxLines = 1, fontFamily = Vazirmatn)
                if (isFastest) {
                    Row(
                        Modifier.clip(RoundedCornerShape(20.dp)).background(Dz.connected.copy(0.14f))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.Filled.Bolt, null, tint = Dz.connected, modifier = Modifier.size(9.dp))
                        Text("Fastest", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Dz.connected, fontFamily = Vazirmatn)
                    }
                }
            }
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(item.protocol.uppercase(), fontSize = 10.5.sp, fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, color = protoColor)
                Text("${item.serverAddress}:${item.serverPort}", fontSize = 10.sp, color = Dz.tMute, maxLines = 1, fontFamily = JetBrainsMono)
            }
        }
        if (item.latency <= 0) {
            IconButton(onClick = onPing, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Speed, "Ping", tint = Dz.t4, modifier = Modifier.size(16.dp))
            }
        } else {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${item.latency} ms", fontFamily = JetBrainsMono, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = latencyColor)
                LatencyBars(item.latency, latencyColor)
            }
        }
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onFavorite, modifier = Modifier.size(28.dp)) {
            Icon(
                if (item.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder, "Favorite",
                tint = if (item.isFavorite) Dz.gold else Dz.tMute, modifier = Modifier.size(17.dp)
            )
        }
        Icon(
            if (isConnected) Icons.Filled.VerifiedUser else Icons.Filled.Circle, "Status",
            tint = if (isConnected) Dz.connected else Dz.tFaint,
            modifier = Modifier.size(if (isConnected) 20.dp else 12.dp)
        )
    }
}

/** Protocol → accent-aware color, matching the design palette. */
@Composable
private fun SubLinkCard(subUrl: String, lang: String) {
    val accent = LocalAccent.current
    val context = androidx.compose.ui.platform.LocalContext.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Dz.surf035)
            .border(1.dp, Dz.border, RoundedCornerShape(16.dp)).padding(horizontal = 15.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Link, null, tint = Dz.t4, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(if (lang == "fa") "لینک اشتراک" else "Subscription link", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Dz.t4, fontFamily = Vazirmatn)
            Text(subUrl, fontSize = 10.5.sp, color = Dz.t3, maxLines = 1, fontFamily = JetBrainsMono)
        }
        Icon(
            Icons.Filled.ContentCopy, null, tint = accent,
            modifier = Modifier.size(17.dp).clickable {
                val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(android.content.ClipData.newPlainText("Subscription URL", subUrl))
            }
        )
    }
}

private fun protoColorFor(proto: String, accent: Color): Color = when (proto.lowercase()) {
    "vless" -> accent
    "vmess" -> Dz.protoVMess
    "trojan" -> Dz.protoTrojan
    "shadowsocks", "ss" -> Dz.protoSS
    "hysteria2", "hysteria", "hy2" -> Dz.protoHy2
    "tuic" -> Dz.cyan
    else -> Color(0xFF9AA3B5)
}

@Composable
private fun LatencyBars(latency: Int, color: Color) {
    val level = when {
        latency <= 120 -> 3
        latency <= 260 -> 2
        else -> 1
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        repeat(3) { idx ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((7 + idx * 4).dp)
                    .background(
                        if (idx < level) color else Color.White.copy(alpha = 0.14f),
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}
