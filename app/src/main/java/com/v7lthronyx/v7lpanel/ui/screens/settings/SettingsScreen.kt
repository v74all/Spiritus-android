package com.v7lthronyx.v7lpanel.ui.screens.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v7lthronyx.v7lpanel.data.local.SettingsDataStore
import com.v7lthronyx.v7lpanel.ui.components.*
import com.v7lthronyx.v7lpanel.ui.theme.*
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun SettingsScreen(
    onNavigateToConnectionSettings: () -> Unit,
    onNavigateToSplitTunneling: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val settingsStore: SettingsDataStore = koinInject()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val lang by settingsStore.language.collectAsState(initial = "en")
    val killSwitch by settingsStore.killSwitchEnabled.collectAsState(initial = false)
    val autoConnect by settingsStore.autoConnect.collectAsState(initial = false)
    val fragmentEnabled by settingsStore.fragmentEnabled.collectAsState(initial = false)
    val noiseEnabled by settingsStore.noiseEnabled.collectAsState(initial = false)
    val fingerprint by settingsStore.tlsFingerprint.collectAsState(initial = "chrome")
    val accentHex by settingsStore.accentColor.collectAsState(initial = "#34E5A4")
    val fragmentPackets by settingsStore.fragmentPackets.collectAsState(initial = "tlshello")
    val fragmentLength by settingsStore.fragmentLength.collectAsState(initial = "100-200")
    val fragmentInterval by settingsStore.fragmentInterval.collectAsState(initial = "10-20")
    val noisePacket by settingsStore.noisePacket.collectAsState(initial = "rand:50-100")
    val noiseDelay by settingsStore.noiseDelay.collectAsState(initial = "10-20")

    V7LPanelBackground(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
        V7LHeaderTitle(if (lang == "fa") "تنظیمات" else "Settings")

        Spacer(Modifier.height(16.dp))

        // ── Connection ──
        SettingsGroupHeader(if (lang == "fa") "اتصال" else "Connection")

        SettingsToggleItem(
            icon = Icons.Filled.Wifi,
            title = if (lang == "fa") "اتصال خودکار" else "Auto-Connect",
            subtitle = if (lang == "fa") "اتصال هنگام باز شدن برنامه" else "Connect on app launch",
            checked = autoConnect,
            onCheckedChange = { scope.launch { settingsStore.setAutoConnect(it) } }
        )

        SettingsNavItem(
            icon = Icons.Filled.Tune,
            title = if (lang == "fa") "تنظیمات اتصال" else "Connection Settings",
            subtitle = if (lang == "fa") "DNS، IPv6، MUX، MTU، پروکسی زنجیره" else "DNS, IPv6, MUX, MTU, Proxy Chain",
            onClick = onNavigateToConnectionSettings
        )

        Spacer(Modifier.height(16.dp))

        // ── Security ──
        SettingsGroupHeader(if (lang == "fa") "امنیت" else "Security")

        SettingsToggleItem(
            icon = Icons.Filled.Shield,
            title = if (lang == "fa") "قطع‌کننده اضطراری" else "Kill switch",
            subtitle = if (lang == "fa") "قطع اینترنت هنگام افت VPN" else "Block traffic if the VPN drops",
            checked = killSwitch,
            onCheckedChange = { scope.launch { settingsStore.setKillSwitch(it) } }
        )

        SettingsNavItem(
            icon = Icons.Filled.AppBlocking,
            title = if (lang == "fa") "تونل تقسیم" else "Split Tunneling",
            subtitle = if (lang == "fa") "مدیریت اپلیکیشن‌ها" else "Manage app routing",
            onClick = onNavigateToSplitTunneling
        )

        SettingsNavItem(
            icon = Icons.Filled.VpnLock,
            title = if (lang == "fa") "VPN همیشه روشن" else "Always-on VPN",
            subtitle = if (lang == "fa") "تنظیمات VPN اندروید" else "Android VPN settings",
            onClick = {
                try {
                    context.startActivity(Intent(Settings.ACTION_VPN_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                } catch (_: Exception) {}
            }
        )

        Spacer(Modifier.height(16.dp))

        // ── Advanced / Anti-Censorship ──
        SettingsGroupHeader(if (lang == "fa") "دور زدن فیلترینگ" else "Anti-Censorship")

        SettingsToggleItem(
            icon = Icons.Filled.BrokenImage,
            title = if (lang == "fa") "فرگمنت TCP" else "Fragment TCP",
            subtitle = if (fragmentEnabled)
                "$fragmentPackets | $fragmentLength | $fragmentInterval"
            else if (lang == "fa") "غیرفعال" else "Disabled",
            checked = fragmentEnabled,
            onCheckedChange = { scope.launch { settingsStore.setFragmentEnabled(it) } }
        )

        if (fragmentEnabled) {
            FragmentPresetSelector(
                packets = fragmentPackets,
                length = fragmentLength,
                interval = fragmentInterval,
                lang = lang,
                onSelect = { p, l, i ->
                    scope.launch {
                        settingsStore.setFragmentPackets(p)
                        settingsStore.setFragmentLength(l)
                        settingsStore.setFragmentInterval(i)
                    }
                }
            )
        }

        SettingsToggleItem(
            icon = Icons.Filled.Waves,
            title = if (lang == "fa") "نویز" else "Noise Packets",
            subtitle = if (noiseEnabled) "$noisePacket | ${noiseDelay}ms"
                else if (lang == "fa") "غیرفعال" else "Disabled",
            checked = noiseEnabled,
            onCheckedChange = { scope.launch { settingsStore.setNoiseEnabled(it) } }
        )

        // TLS Fingerprint dropdown
        FingerprintSelector(
            selected = fingerprint,
            lang = lang,
            onSelect = { scope.launch { settingsStore.setTlsFingerprint(it) } }
        )

        Spacer(Modifier.height(16.dp))

        // ── Tools ──
        SettingsGroupHeader(if (lang == "fa") "ابزارها" else "Tools")

        SettingsNavItem(
            icon = Icons.Filled.BugReport,
            title = if (lang == "fa") "ابزار تشخیصی" else "Diagnostic Tools",
            subtitle = if (lang == "fa") "تست DNS، IP، تاخیر" else "DNS leak, IP check, latency",
            onClick = onNavigateToDiagnostics
        )

        Spacer(Modifier.height(16.dp))

        // ── General ──
        SettingsGroupHeader(if (lang == "fa") "عمومی" else "General")

        LanguageSelector(lang) { scope.launch { settingsStore.setLanguage(it) } }

        AccentPickerRow(
            currentHex = accentHex,
            label = if (lang == "fa") "رنگ تم" else "Accent color"
        ) { hex -> scope.launch { settingsStore.setAccentColor(hex) } }

        SettingsNavItem(
            icon = Icons.Filled.Info,
            title = if (lang == "fa") "درباره" else "About",
            subtitle = "Spiritus v2.0",
            onClick = onNavigateToAbout
        )

        Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun SettingsGroupHeader(title: String) {
    V7LSectionLabel(title, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    V7LInfoRow(
        icon = icon,
        title = title,
        subtitle = subtitle,
        tint = if (checked) LocalAccent.current else V7LColors.t3,
        modifier = Modifier.padding(vertical = 3.dp),
        trailing = { V7LPanelSwitch(checked = checked, onCheckedChange = onCheckedChange) }
    )
}

@Composable
private fun SettingsNavItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    V7LInfoRow(
        icon = icon,
        title = title,
        subtitle = subtitle,
        tint = LocalAccent.current,
        modifier = Modifier.padding(vertical = 3.dp),
        trailing = { Icon(Icons.Filled.ChevronRight, null, tint = V7LColors.t4, modifier = Modifier.size(20.dp)) },
        onClick = onClick
    )
}

@Composable
private fun FragmentPresetSelector(
    packets: String,
    length: String,
    interval: String,
    lang: String,
    onSelect: (packets: String, length: String, interval: String) -> Unit
) {
    val presets = listOf(
        Triple("tlshello", "100-200", "10-20"),
        Triple("tlshello", "1-3", "1-3"),
        Triple("1-1", "100-200", "10-20"),
        Triple("1-3", "1-5", "1-5"),
    )

    V7LGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp, horizontal = 4.dp),
        radius = 14.dp,
        containerColor = V7LColors.bg3.copy(alpha = 0.60f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                if (lang == "fa") "پیش‌فرض‌ها" else "Presets",
                fontSize = 11.sp, color = V7LColors.t2
            )
            Spacer(Modifier.height(8.dp))
            presets.forEach { (p, l, i) ->
                val selected = packets == p && length == l && interval == i
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clickable { onSelect(p, l, i) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (selected) V7LColors.accentDark.copy(alpha = 0.2f) else V7LColors.bg2
                ) {
                    Text(
                        text = "$p  |  $l  |  $i",
                        fontFamily = FiraCode,
                        fontSize = 12.sp,
                        color = if (selected) LocalAccent.current else V7LColors.t1,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FingerprintSelector(
    selected: String,
    lang: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("chrome", "firefox", "safari", "edge", "random", "none")

    V7LGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        radius = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 13.dp)
                .clickable { expanded = true },
            verticalAlignment = Alignment.CenterVertically
        ) {
            V7LIconTile(Icons.Filled.Fingerprint, LocalAccent.current, size = 38.dp, iconSize = 18.dp)
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (lang == "fa") "اثر انگشت TLS" else "TLS Fingerprint",
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = V7LColors.t0
                )
                Text(selected, fontSize = 11.sp, fontFamily = FiraCode, color = LocalAccent.current)
            }
            Icon(Icons.Filled.ArrowDropDown, null, tint = V7LColors.t4)

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = V7LColors.bg3
            ) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(opt, color = if (opt == selected) LocalAccent.current else V7LColors.t1) },
                        onClick = { onSelect(opt); expanded = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageSelector(current: String, onSelect: (String) -> Unit) {
    V7LInfoRow(
        icon = Icons.Filled.Language,
        title = if (current == "fa") "زبان" else "Language",
        subtitle = "",
        tint = LocalAccent.current,
        modifier = Modifier.padding(vertical = 3.dp),
        trailing = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                V7LPill(selected = current == "en", text = "EN", onClick = { onSelect("en") })
                V7LPill(selected = current == "fa", text = "فا", onClick = { onSelect("fa") })
            }
        }
    )
}

@Composable
private fun AccentPickerRow(currentHex: String, label: String, onPick: (String) -> Unit) {
    val current = accentOptionForHex(currentHex)
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    androidx.compose.foundation.layout.Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Dz.surf035)
            .border(1.dp, Dz.border, RoundedCornerShape(16.dp))
            .padding(horizontal = 15.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(current.color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Palette, null, tint = current.color, modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.width(13.dp))
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Dz.tHi, fontFamily = Vazirmatn, modifier = Modifier.weight(1f))
            Text(current.name, fontSize = 12.sp, color = current.color, fontFamily = JetBrainsMono)
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            AccentPalette.forEach { opt ->
                val selected = opt.color == current.color
                Box(
                    Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.verticalGradient(listOf(opt.color, opt.light))
                        )
                        .border(
                            width = if (selected) 2.5.dp else 0.dp,
                            color = if (selected) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            onPick(opt.color.toHex())
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) Icon(Icons.Filled.Check, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
