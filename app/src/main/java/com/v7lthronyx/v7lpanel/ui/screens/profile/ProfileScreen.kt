package com.v7lthronyx.v7lpanel.ui.screens.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v7lthronyx.v7lpanel.SessionHolder
import com.v7lthronyx.v7lpanel.data.api.models.UserDto
import com.v7lthronyx.v7lpanel.ui.components.*
import com.v7lthronyx.v7lpanel.ui.theme.Dz
import com.v7lthronyx.v7lpanel.ui.theme.FiraCode
import com.v7lthronyx.v7lpanel.ui.theme.JetBrainsMono
import com.v7lthronyx.v7lpanel.ui.theme.LocalAccent
import com.v7lthronyx.v7lpanel.ui.theme.LocalAccentLight
import com.v7lthronyx.v7lpanel.ui.theme.LocalLang
import com.v7lthronyx.v7lpanel.ui.theme.SpaceGrotesk
import com.v7lthronyx.v7lpanel.ui.theme.V7LColors
import com.v7lthronyx.v7lpanel.ui.theme.Vazirmatn
import com.v7lthronyx.v7lpanel.util.TrafficFormatter

@Composable
fun ProfileScreen(
    userInfo: UserDto?,
    profileMetadataMissing: Boolean = false,
    isLoading: Boolean = false,
    error: String? = null,
    /** True when there is no panel subscription to load at all (local/manual
     *  config mode) — as opposed to a fetch that failed and can be retried. */
    noAccount: Boolean = false,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    onAdminConsole: () -> Unit = {}
) {
    val context = LocalContext.current
    val lang = LocalLang.current

    V7LPanelBackground(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        ProfileHeader(
            // Real name from the server first; otherwise fall back through
            // whatever we actually know about this session — never a made-up
            // placeholder identity like the old hardcoded "aria_vip".
            userName = userInfo?.name?.ifBlank { null }
                ?: SessionHolder.principal.ifBlank { null }
                ?: SessionHolder.uuid.take(8).ifBlank { null }
                ?: if (lang == "fa") "مشترک" else "Subscriber",
            lang = lang
        )

        Spacer(Modifier.height(18.dp))

        if (userInfo != null) {
            if (profileMetadataMissing) {
                V7LGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    radius = 14.dp,
                    containerColor = LocalAccent.current.copy(alpha = 0.10f),
                    borderColor = LocalAccent.current.copy(alpha = 0.20f)
                ) {
                    Text(
                        text = if (lang == "fa") {
                            "مشخصات ترافیک و انقضا از سرور دریافت نشد. بروزرسانی را بزنید یا بعداً دوباره امتحان کنید."
                        } else {
                            "Traffic and expiry details are unavailable from the server. Tap refresh or try again later."
                        },
                        modifier = Modifier.padding(14.dp),
                        fontSize = 13.sp,
                        color = V7LColors.t1
                    )
                }
                Spacer(Modifier.height(16.dp))
            } else {
                UsageRingCard(
                    usedGb = userInfo.usedGb,
                    limitGb = userInfo.limitGb,
                    daysLeft = userInfo.daysLeft,
                    lang = lang
                )
                Spacer(Modifier.height(16.dp))
            }

            // Account info card
            V7LGlassCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val limitUnlimited = userInfo.limitGb <= 0.0 && userInfo.trafficLimitBytes <= 0L
                    val pct = TrafficFormatter.trafficPercent(userInfo.usedGb, userInfo.limitGb)
                    ProfileInfoRow(
                        icon = Icons.Filled.Person,
                        label = if (lang == "fa") "نام" else "Name",
                        value = userInfo.name.ifBlank { "—" }
                    )
                    ProfileInfoRow(
                        icon = Icons.Filled.Key,
                        label = "UUID",
                        value = userInfo.uuid,
                        monospace = true
                    )
                    ProfileInfoRow(
                        icon = Icons.Filled.CalendarToday,
                        label = if (lang == "fa") "انقضا" else "Expires",
                        value = when {
                            userInfo.expireAt.isNotBlank() -> userInfo.expireAt
                            userInfo.daysLeft <= 0 && userInfo.active && limitUnlimited ->
                                if (lang == "fa") "بدون محدودیت زمانی" else "No expiry limit"
                            else -> "—"
                        }
                    )
                    ProfileInfoRow(
                        icon = Icons.Filled.Timer,
                        label = if (lang == "fa") "روزهای باقیمانده" else "Days Left",
                        value = when {
                            userInfo.daysLeft > 0 -> "${userInfo.daysLeft}"
                            userInfo.expireAt.isBlank() && userInfo.active -> "—"
                            else -> "0"
                        }
                    )
                    ProfileInfoRow(
                        icon = Icons.Filled.Percent,
                        label = if (lang == "fa") "درصد مصرف" else "Usage %",
                        value = if (limitUnlimited && userInfo.usedGb <= 0.0) "—" else "$pct%"
                    )
                    ProfileInfoRow(
                        icon = Icons.Filled.Devices,
                        label = if (lang == "fa") "دستگاه‌های آنلاین" else "Online Devices",
                        value = "${userInfo.onlineIps}"
                    )
                    if (userInfo.uploadBytes > 0L || userInfo.downloadBytes > 0L) {
                        ProfileInfoRow(
                            icon = Icons.Filled.ArrowUpward,
                            label = if (lang == "fa") "آپلود (مجموع)" else "Upload (total)",
                            value = TrafficFormatter.formatBytes(userInfo.uploadBytes)
                        )
                        ProfileInfoRow(
                            icon = Icons.Filled.ArrowDownward,
                            label = if (lang == "fa") "دانلود (مجموع)" else "Download (total)",
                            value = TrafficFormatter.formatBytes(userInfo.downloadBytes)
                        )
                    }
                    if (userInfo.liveUp > 0L || userInfo.liveDown > 0L) {
                        ProfileInfoRow(
                            icon = Icons.Filled.Speed,
                            label = if (lang == "fa") "ترافیک لحظه‌ای ↑" else "Live session ↑",
                            value = TrafficFormatter.formatBytes(userInfo.liveUp)
                        )
                        ProfileInfoRow(
                            icon = Icons.Filled.Speed,
                            label = if (lang == "fa") "ترافیک لحظه‌ای ↓" else "Live session ↓",
                            value = TrafficFormatter.formatBytes(userInfo.liveDown)
                        )
                    }
                    ProfileInfoRow(
                        icon = Icons.Filled.CloudDownload,
                        label = if (lang == "fa") "ترافیک (مصرف / سقف)" else "Traffic (used / limit)",
                        value = when {
                            limitUnlimited ->
                                "${TrafficFormatter.formatGb(userInfo.usedGb)} / " +
                                    if (lang == "fa") "نامحدود" else "Unlimited"
                            else ->
                                "${TrafficFormatter.formatGb(userInfo.usedGb)} / ${TrafficFormatter.formatGb(userInfo.limitGb)}"
                        }
                    )
                    if (!limitUnlimited && userInfo.limitGb > 0.0) {
                        val remainGb = (userInfo.limitGb - userInfo.usedGb).coerceAtLeast(0.0)
                        ProfileInfoRow(
                            icon = Icons.Filled.DataSaverOn,
                            label = if (lang == "fa") "باقیمانده" else "Remaining",
                            value = TrafficFormatter.formatGb(remainGb)
                        )
                    }
                    if (userInfo.createdAt.isNotBlank()) {
                        ProfileInfoRow(
                            icon = Icons.Filled.Event,
                            label = if (lang == "fa") "تاریخ ایجاد" else "Created",
                            value = userInfo.createdAt
                        )
                    }
                    ProfileInfoRow(
                        icon = Icons.Filled.VerifiedUser,
                        label = if (lang == "fa") "وضعیت" else "Status",
                        value = if (userInfo.active) {
                            if (lang == "fa") "فعال" else "Active"
                        } else {
                            if (lang == "fa") "غیرفعال" else "Inactive"
                        }
                    )
                    if (userInfo.agentId != null) {
                        ProfileInfoRow(
                            icon = Icons.Filled.Badge,
                            label = if (lang == "fa") "شناسه نماینده" else "Agent ID",
                            value = "${userInfo.agentId}"
                        )
                    }
                    if (userInfo.supportUrl.isNotBlank()) {
                        ProfileInfoRow(
                            icon = Icons.Filled.SupportAgent,
                            label = if (lang == "fa") "پشتیبانی" else "Support",
                            value = userInfo.supportUrl
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Subscription URL actions
            V7LGlassCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        if (lang == "fa") "لینک اشتراک" else "Subscription Link",
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = V7LColors.t2
                    )
                    Spacer(Modifier.height(8.dp))

                    val subUrl =
                        "${SessionHolder.serverUrl.trim().trimEnd('/')}/sub/${userInfo.uuid}"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        QRCodeView(content = subUrl, modifier = Modifier.size(108.dp), size = 360)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (lang == "fa") "برای وارد کردن در کلاینت‌های v2ray اسکن کنید." else "Scan in any v2ray client to import your servers.",
                                fontSize = 11.sp,
                                color = V7LColors.t3,
                                lineHeight = 17.sp
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cb.setPrimaryClip(ClipData.newPlainText("Sub URL", subUrl))
                                        Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LocalAccent.current)
                                ) {
                                    Icon(Icons.Filled.ContentCopy, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (lang == "fa") "کپی" else "Copy", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = { onRefresh() },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LocalAccent.current)
                                ) {
                                    Icon(Icons.Filled.Refresh, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (lang == "fa") "بروزرسانی" else "Refresh", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        } else if (isLoading) {
            Spacer(Modifier.height(40.dp))
            CircularProgressIndicator(color = LocalAccent.current)
            Spacer(Modifier.height(12.dp))
            Text(
                if (lang == "fa") "در حال بارگذاری..." else "Loading profile...",
                color = V7LColors.t3
            )
        } else if (noAccount) {
            // Local/manual-config mode: there is no panel subscription to
            // fetch, so a "Retry" button here would just do nothing forever.
            // Say plainly why the profile is empty instead.
            Spacer(Modifier.height(32.dp))
            Icon(Icons.Filled.PersonOff, null, tint = Dz.t4, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (lang == "fa") "بدون حساب اشتراک" else "No subscription account",
                color = Dz.tHi,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = Vazirmatn
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (lang == "fa")
                    "شما با یک کانفیگ محلی وصل شده‌اید، نه یک اشتراک پنل — پروفایلی برای نمایش وجود ندارد."
                else
                    "You're connected with a local config, not a panel subscription — there's no profile to show.",
                color = V7LColors.t3,
                fontSize = 12.5.sp,
                fontFamily = Vazirmatn,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Spacer(Modifier.height(40.dp))
            Text(
                text = error ?: if (lang == "fa") "اطلاعات پروفایل در دسترس نیست" else "Profile info unavailable",
                color = V7LColors.t3,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth()
            )
            if (SessionHolder.uuid.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (lang == "fa") "شناسه: ${SessionHolder.uuid}" else "ID: ${SessionHolder.uuid}",
                    color = V7LColors.t2,
                    fontSize = 11.sp,
                    fontFamily = FiraCode,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onRefresh,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = LocalAccent.current)
            ) {
                Icon(Icons.Filled.Refresh, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (lang == "fa") "تلاش مجدد" else "Retry")
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Admin / Agent console — separate login, mirrors the design's Profile row ──
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Dz.surf035)
                .border(1.dp, Dz.border, RoundedCornerShape(16.dp))
                .clickable(onClick = onAdminConsole)
                .padding(horizontal = 15.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Dz.cyan.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Dashboard, null, tint = Dz.cyan, modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(if (lang == "fa") "کنسول مدیریت" else "Admin console", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Dz.tHi, fontFamily = Vazirmatn)
                Text(if (lang == "fa") "مدیریت کاربران و نمایندگان" else "Manage users & agents", fontSize = 11.sp, color = Dz.t4, fontFamily = Vazirmatn)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = Dz.tMute, modifier = Modifier.size(18.dp))
        }

        Spacer(Modifier.height(24.dp))

        // Logout
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = V7LColors.red),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.Logout, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (lang == "fa") "خروج" else "Logout", fontSize = 14.sp)
        }

        Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun UsageRingCard(usedGb: Double, limitGb: Double, daysLeft: Int, lang: String) {
    val accent = LocalAccent.current
    val unlimited = limitGb <= 0.0
    val pct = if (unlimited) 0f else (usedGb / limitGb).coerceIn(0.0, 1.0).toFloat()
    val pctInt = (pct * 100).toInt()
    val daysColor = when { daysLeft <= 0 -> Dz.danger; daysLeft < 7 -> Dz.connecting; else -> Dz.connected }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Dz.surf035)
            .border(1.dp, Dz.border, RoundedCornerShape(22.dp)).padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(112.dp), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Canvas(Modifier.size(112.dp)) {
                // Matches the mockup's conic-gradient donut: a sharp-edged
                // (butt-cap) band, not rounded — a pie-slice look, not a bar.
                val sw = 9.dp.toPx()
                drawCircle(androidx.compose.ui.graphics.Color.White.copy(0.07f), style = androidx.compose.ui.graphics.drawscope.Stroke(sw))
                if (!unlimited && pct > 0f) drawArc(
                    color = accent, startAngle = -90f, sweepAngle = pct * 360f, useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(sw, cap = androidx.compose.ui.graphics.StrokeCap.Butt)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (unlimited) "∞" else "$pctInt%",
                    fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Dz.tHi, fontFamily = JetBrainsMono
                )
                Text(if (lang == "fa") "مصرف" else "used", fontSize = 9.5.sp, color = Dz.t4, fontFamily = Vazirmatn)
            }
        }
        Spacer(Modifier.width(20.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Column {
                Text(if (lang == "fa") "مصرف داده" else "Data usage", fontSize = 11.sp, color = Dz.t4, fontFamily = Vazirmatn)
                Text(
                    if (unlimited) "${"%.1f".format(usedGb)} / ${if (lang == "fa") "نامحدود" else "Unlimited"}"
                    else "${"%.1f".format(usedGb)} / ${"%.0f".format(limitGb)} GB",
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Dz.tHi, fontFamily = JetBrainsMono
                )
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Dz.surf06))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.EventAvailable, null, tint = daysColor, modifier = Modifier.size(16.dp))
                Text(if (daysLeft > 0) "$daysLeft" else "—", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = daysColor, fontFamily = JetBrainsMono)
                Text(if (lang == "fa") "روز مانده" else "days left", fontSize = 12.sp, color = Dz.t3, fontFamily = Vazirmatn)
            }
        }
    }
}

@Composable
private fun ProfileHeader(userName: String, lang: String) {
    val accent = LocalAccent.current
    val accentLight = LocalAccentLight.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .background(
                    Brush.linearGradient(listOf(accent, accentLight)),
                    RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                userName.firstOrNull()?.uppercaseChar()?.toString() ?: "A",
                color = androidx.compose.ui.graphics.Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SpaceGrotesk
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(userName, color = Dz.tHi, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = SpaceGrotesk)
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .background(Dz.gold.copy(alpha = 0.13f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(Icons.Filled.WorkspacePremium, null, tint = Dz.gold, modifier = Modifier.size(13.dp))
                Text(
                    if (lang == "fa") "اشتراک ویژه" else "Premium plan",
                    color = Dz.gold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Vazirmatn
                )
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    monospace: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = LocalAccent.current, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, fontSize = 12.sp, color = V7LColors.t3, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            fontSize = if (monospace) 12.sp else 13.sp,
            fontFamily = if (monospace) FiraCode else null,
            color = V7LColors.t0,
            modifier = Modifier
                .padding(start = 26.dp)
                .fillMaxWidth()
        )
    }
}
