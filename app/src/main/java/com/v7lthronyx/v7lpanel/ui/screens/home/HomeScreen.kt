package com.v7lthronyx.v7lpanel.ui.screens.home

import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v7lthronyx.v7lpanel.SessionHolder
import com.v7lthronyx.v7lpanel.ui.theme.*
import com.v7lthronyx.v7lpanel.util.TrafficFormatter
import com.v7lthronyx.v7lpanel.vpn.VpnStatus

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onGoServers: () -> Unit = {}
) {
    val context = LocalContext.current
    val status by viewModel.vpnStatus.collectAsState()
    val label by viewModel.connectedLabel.collectAsState()
    val uri by viewModel.connectedUri.collectAsState()
    val rxSpeed by viewModel.rxSpeed.collectAsState()
    val txSpeed by viewModel.txSpeed.collectAsState()
    val since by viewModel.connectedSince.collectAsState()
    val error by viewModel.errorMsg.collectAsState()
    val lang by viewModel.language.collectAsState(initial = "en")
    val fa = lang == "fa"
    val accent = LocalAccent.current

    val isConnected = status == VpnStatus.CONNECTED
    val isConnecting = status == VpnStatus.CONNECTING

    // VPN permission flow
    var pendingConnect by remember { mutableStateOf<Pair<String, String>?>(null) }
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        pendingConnect?.let { (lbl, u) -> viewModel.connect(context, lbl, u); pendingConnect = null }
    }
    fun connectOrAsk(lbl: String, u: String) {
        val intent = VpnService.prepare(context)
        if (intent != null) { pendingConnect = lbl to u; vpnPermissionLauncher.launch(intent) }
        else viewModel.connect(context, lbl, u)
    }
    fun onPower() {
        if (isConnected || isConnecting) viewModel.disconnect(context)
        else {
            val u = uri; val l = label
            if (u != null && l != null) connectOrAsk(l, u) else onGoServers()
        }
    }

    // Live sparkline history of download speed (rolling 30 samples).
    val spark = remember { mutableStateListOf<Float>() }
    LaunchedEffect(isConnected) { if (!isConnected) spark.clear() }
    LaunchedEffect(rxSpeed, isConnected) {
        if (isConnected) {
            val norm = (rxSpeed.toFloat() / (16f * 1024 * 1024)).coerceIn(0.06f, 1f)
            spark.add(norm); if (spark.size > 30) spark.removeAt(0)
        }
    }

    val statusColor by animateColorAsState(
        when (status) {
            VpnStatus.CONNECTED -> Dz.connected
            VpnStatus.CONNECTING -> Dz.connecting
            VpnStatus.ERROR -> Dz.danger
            else -> accent
        }, tween(450), label = "status"
    )
    val statusText = when (status) {
        VpnStatus.CONNECTED -> if (fa) "متصل" else "Connected"
        VpnStatus.CONNECTING -> if (fa) "در حال اتصال…" else "Connecting…"
        VpnStatus.ERROR -> if (fa) "خطا در اتصال" else "Connection error"
        else -> if (fa) "برای اتصال ضربه بزنید" else "Tap to connect"
    }
    val subText = when (status) {
        VpnStatus.CONNECTED -> if (fa) "اتصال شما محافظت‌شده است" else "Your connection is private"
        VpnStatus.CONNECTING -> ""
        else -> if (fa) "شما محافظت نشده‌اید" else "You are not protected"
    }

    com.v7lthronyx.v7lpanel.ui.components.AuroraBackground(
        Modifier.fillMaxSize().statusBarsPadding()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── header ──
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f).padding(end = 10.dp)) {
                    Text(if (fa) "خوش آمدید" else "Welcome back", fontSize = 12.sp, color = Dz.t4, fontFamily = Vazirmatn, maxLines = 1)
                    Text(
                        SessionHolder.principal.ifBlank { SessionHolder.uuid.take(8).ifBlank { "V7L" } },
                        fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Dz.tHi, fontFamily = SpaceGrotesk,
                        maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                LangChip(fa) { viewModel.setLanguage(if (fa) "en" else "fa") }
            }

            // ── hero ──
            Box(Modifier.size(248.dp).padding(top = 6.dp), contentAlignment = Alignment.Center) {
                StatusRing(status, statusColor, Modifier.fillMaxSize())
                PowerButton(status, statusColor, onClick = ::onPower)
            }

            Spacer(Modifier.height(14.dp))
            Text(statusText, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = statusColor, fontFamily = SpaceGrotesk)
            if (subText.isNotEmpty()) {
                Spacer(Modifier.height(5.dp))
                Text(subText, fontSize = 12.5.sp, color = Dz.t3, fontFamily = Vazirmatn)
            }

            // ── connected: throughput ──
            AnimatedVisibility(
                isConnected,
                enter = fadeIn(tween(380)) + slideInVertically { it / 4 },
                exit = fadeOut(tween(180))
            ) {
                ThroughputCard(
                    rx = TrafficFormatter.formatSpeed(rxSpeed),
                    tx = TrafficFormatter.formatSpeed(txSpeed),
                    spark = spark, accent = accent, fa = fa,
                    session = sessionText(since),
                    server = label ?: "—",
                    modifier = Modifier.padding(top = 18.dp)
                )
            }

            // ── no server configured yet: clear call-to-action ──
            if (label == null && !isConnected && !isConnecting) {
                EmptyServerCard(accent, fa, modifier = Modifier.padding(top = 18.dp), onClick = onGoServers)
            }

            // ── off: quick connect (only meaningful once a server exists) ──
            AnimatedVisibility(label != null && (status == VpnStatus.DISCONNECTED || status == VpnStatus.ERROR)) {
                GhostButton(
                    text = if (fa) "اتصال سریع — سریع‌ترین" else "Quick connect — fastest",
                    icon = Icons.Filled.Bolt, accent = accent,
                    modifier = Modifier.padding(top = 18.dp)
                ) { val u = uri; val l = label; if (u != null && l != null) connectOrAsk(l, u) else onGoServers() }
            }

            // ── current server ──
            if (label != null) {
                Spacer(Modifier.height(13.dp))
                CurrentServerCard(label!!, accent, statusColor, isConnected, fa, onGoServers)
            }

            // ── error ──
            error?.let {
                Spacer(Modifier.height(12.dp))
                ErrorCard(it) { viewModel.clearError() }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

private fun sessionText(since: Long?): String {
    if (since == null || since <= 0) return "00:00"
    val sec = ((System.currentTimeMillis() - since) / 1000).coerceAtLeast(0)
    val h = sec / 3600; val m = (sec % 3600) / 60; val s = sec % 60
    return if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

// ═══════════════════════ components ═══════════════════════

@Composable
private fun EmptyServerCard(accent: Color, fa: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    GlassCard(modifier.clickable { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() }) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(accent.copy(0.14f)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.AddCircle, null, tint = accent, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.height(12.dp))
            Text(
                if (fa) "هنوز سروری اضافه نکرده‌اید" else "No server added yet",
                fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = Dz.tHi, fontFamily = Vazirmatn
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (fa) "یک کانفیگ یا اشتراک اضافه کنید تا وصل شوید" else "Add a config or subscription to get connected",
                fontSize = 11.5.sp, color = Dz.t3, fontFamily = Vazirmatn, textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.clip(RoundedCornerShape(12.dp)).background(accent.copy(0.14f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Icon(Icons.Filled.Add, null, tint = accent, modifier = Modifier.size(15.dp))
                Text(if (fa) "افزودن سرور" else "Add a server", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = accent, fontFamily = Vazirmatn)
            }
        }
    }
}

@Composable
private fun LangChip(fa: Boolean, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Row(
        Modifier.clip(RoundedCornerShape(24.dp))
            .background(Dz.surf05).border(1.dp, Dz.border2, RoundedCornerShape(24.dp))
            .clickable { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() }.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(Icons.Filled.Language, null, tint = Dz.t2, modifier = Modifier.size(15.dp))
        Text(if (fa) "فارسی" else "EN", fontSize = 11.5.sp, color = Dz.t1, fontWeight = FontWeight.SemiBold, fontFamily = Vazirmatn)
    }
}

@Composable
private fun StatusRing(status: VpnStatus, color: Color, modifier: Modifier) {
    val active = status == VpnStatus.CONNECTED || status == VpnStatus.CONNECTING
    val tr = rememberInfiniteTransition(label = "ring")
    val rot by tr.animateFloat(0f, 360f, infiniteRepeatable(tween(if (status == VpnStatus.CONNECTING) 1300 else 8000, easing = LinearEasing)), label = "rot")
    val sweep by animateFloatAsState(
        when (status) { VpnStatus.CONNECTED -> 360f; VpnStatus.CONNECTING -> 270f; else -> 0f },
        tween(700, easing = FastOutSlowInEasing), label = "sweep"
    )
    val halo by tr.animateFloat(
        if (status == VpnStatus.DISCONNECTED) 0.10f else 0.20f,
        if (status == VpnStatus.DISCONNECTED) 0.18f else 0.34f,
        infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "halo"
    )
    Canvas(modifier) {
        val pad = 14.dp.toPx(); val sw = 7.dp.toPx()
        val arc = Size(size.width - pad * 2, size.height - pad * 2); val tl = Offset(pad, pad)
        // halo
        drawCircle(color.copy(alpha = halo), radius = size.minDimension / 2.6f, style = Stroke(width = 24.dp.toPx()))
        // track
        drawArc(Color.White.copy(0.07f), 0f, 360f, false, tl, arc, style = Stroke(sw, cap = StrokeCap.Round))
        if (active && sweep > 0f) {
            drawArc(color, rot - 90f, sweep, false, tl, arc, style = Stroke(sw, cap = StrokeCap.Round))
        }
    }
}

@Composable
private fun PowerButton(status: VpnStatus, color: Color, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val tr = rememberInfiniteTransition(label = "pb")
    val pulse by tr.animateFloat(1f, if (status == VpnStatus.CONNECTING) 1.045f else 1f,
        infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
    val press by animateFloatAsState(if (pressed) 0.92f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "press")
    Box(
        Modifier.size(140.dp).scale(pulse * press).clip(CircleShape)
            .background(Brush.radialGradient(listOf(Dz.powerA, Dz.powerB)))
            .border(1.dp, Color.White.copy(0.10f), CircleShape)
            .clickable(interaction, null) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.PowerSettingsNew, "Power", tint = color, modifier = Modifier.size(44.dp))
    }
}

@Composable
private fun ThroughputCard(
    rx: String, tx: String, spark: List<Float>, accent: Color, fa: Boolean,
    session: String, server: String, modifier: Modifier = Modifier
) {
    GlassCard(modifier) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            SpeedSide(Icons.Filled.ArrowDownward, rx, if (fa) "دانلود" else "Download", Dz.connected)
            Spacer(Modifier.weight(1f))
            Sparkline(spark, accent, Modifier.width(92.dp).height(36.dp))
            Spacer(Modifier.weight(1f))
            SpeedSide(Icons.Filled.ArrowUpward, tx, if (fa) "آپلود" else "Upload", accent, end = true)
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp).height(1.dp).background(Dz.surf06))
        Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            IconLabel(Icons.Filled.Schedule, session)
            IconLabel(Icons.Filled.Public, server, maxChars = 18)
        }
    }
}

@Composable
private fun SpeedSide(icon: ImageVector, value: String, label: String, tint: Color, end: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (end) SpeedText(value, label, tint, end = true)
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(tint.copy(0.14f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(17.dp))
        }
        if (!end) SpeedText(value, label, tint, end = false)
    }
}

@Composable
private fun SpeedText(value: String, label: String, tint: Color, end: Boolean) {
    Column(horizontalAlignment = if (end) Alignment.End else Alignment.Start) {
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Dz.tHi, fontFamily = JetBrainsMono)
        Text(label, fontSize = 10.5.sp, color = Dz.t4, fontFamily = Vazirmatn)
    }
}

@Composable
private fun Sparkline(points: List<Float>, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        if (points.size < 2) {
            drawLine(color.copy(0.5f), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 2.4f.dp.toPx(), StrokeCap.Round)
            return@Canvas
        }
        val n = points.size; val path = Path()
        points.forEachIndexed { i, v ->
            val x = i / (n - 1f) * size.width
            val y = size.height - v.coerceIn(0f, 1f) * size.height * 0.92f - size.height * 0.04f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 2.4f.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun IconLabel(icon: ImageVector, text: String, maxChars: Int = 40) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Icon(icon, null, tint = Dz.t4, modifier = Modifier.size(14.dp))
        Text(text.take(maxChars), fontSize = 13.sp, color = Dz.t1, fontFamily = JetBrainsMono, maxLines = 1)
    }
}

@Composable
private fun GhostButton(text: String, icon: ImageVector, accent: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(15.dp))
            .background(Dz.surf05).border(1.dp, Dz.border3, RoundedCornerShape(15.dp))
            .clickable { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(9.dp))
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Dz.tHi, fontFamily = Vazirmatn)
    }
}

@Composable
private fun CurrentServerCard(label: String, accent: Color, statusColor: Color, connected: Boolean, fa: Boolean, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    GlassCard(Modifier.clickable { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() }) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(accent.copy(0.14f)), contentAlignment = Alignment.Center) {
                Text(label.filter { it.isLetter() }.take(2).uppercase().ifBlank { "V7" },
                    fontSize = 13.sp, fontWeight = FontWeight.Bold, color = accent, fontFamily = JetBrainsMono)
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(if (fa) "سرور فعلی" else "Current server", fontSize = 10.5.sp, color = Dz.t4, fontFamily = Vazirmatn)
                Text(label, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = Dz.tHi, maxLines = 1, fontFamily = Vazirmatn)
            }
            if (connected) Box(Modifier.size(8.dp).clip(CircleShape).background(statusColor))
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Filled.ChevronRight, null, tint = Dz.tMute, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ErrorCard(msg: String, onClose: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(Dz.danger.copy(0.08f)).border(1.dp, Dz.danger.copy(0.18f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.ErrorOutline, null, tint = Dz.danger, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(9.dp))
        Text(msg, fontSize = 12.sp, color = Dz.dangerSoft, modifier = Modifier.weight(1f), fontFamily = Vazirmatn)
        Icon(Icons.Filled.Close, null, tint = Dz.danger, modifier = Modifier.size(16.dp).clickable(onClick = onClose))
    }
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    androidx.compose.foundation.layout.Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(Dz.surf035).border(1.dp, Dz.border, RoundedCornerShape(20.dp)),
        content = content
    )
}
