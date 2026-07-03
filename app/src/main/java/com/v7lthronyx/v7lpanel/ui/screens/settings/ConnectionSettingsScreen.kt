package com.v7lthronyx.v7lpanel.ui.screens.settings

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v7lthronyx.v7lpanel.data.local.SettingsDataStore
import com.v7lthronyx.v7lpanel.ui.components.*
import com.v7lthronyx.v7lpanel.ui.theme.*
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun ConnectionSettingsScreen(
    onBack: () -> Unit
) {
    val settingsStore: SettingsDataStore = koinInject()
    val scope = rememberCoroutineScope()
    val lang by settingsStore.language.collectAsState(initial = "en")

    val dns by settingsStore.dnsServer.collectAsState(initial = "8.8.8.8")
    val ipv6 by settingsStore.enableIpv6.collectAsState(initial = false)
    val muxEnabled by settingsStore.muxEnabled.collectAsState(initial = true)
    val muxConcurrency by settingsStore.muxConcurrency.collectAsState(initial = 8)
    val proxyChain by settingsStore.proxyChainUri.collectAsState(initial = "")
    val domainStrategy by settingsStore.domainStrategy.collectAsState(initial = "AsIs")
    val mtu by settingsStore.mtuSize.collectAsState(initial = 0)

    var dnsInput by remember(dns) { mutableStateOf(dns) }
    var chainInput by remember(proxyChain) { mutableStateOf(proxyChain) }
    var mtuInput by remember(mtu) { mutableStateOf(if (mtu <= 0) "" else mtu.toString()) }
    var muxConcurrencyInput by remember(muxConcurrency) { mutableStateOf(muxConcurrency.toString()) }

    V7LPanelBackground(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Back", tint = V7LColors.t0)
            }
            Text(
                if (lang == "fa") "تنظیمات اتصال" else "Connection Settings",
                fontFamily = JetBrainsMono,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = V7LColors.t0
            )
        }

        Spacer(Modifier.height(16.dp))

        // DNS
        SettingTextField(
            label = "DNS Server",
            value = dnsInput,
            onValueChange = { dnsInput = it },
            onSave = { scope.launch { settingsStore.setDnsServer(dnsInput) } },
            placeholder = "8.8.8.8"
        )

        Spacer(Modifier.height(12.dp))

        // Domain Strategy
        Text(
            if (lang == "fa") "استراتژی دامنه" else "Domain Strategy",
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = V7LColors.t2
        )
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("AsIs", "UseIP", "UseIPv4", "UseIPv6").forEach { strategy ->
                FilterChip(
                    selected = domainStrategy == strategy,
                    onClick = { scope.launch { settingsStore.setDomainStrategy(strategy) } },
                    label = { Text(strategy, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = V7LColors.accentDark.copy(alpha = 0.3f),
                        selectedLabelColor = LocalAccent.current,
                        labelColor = V7LColors.t2
                    )
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // IPv6
        V7LGlassCard(modifier = Modifier.fillMaxWidth(), radius = 16.dp) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.SettingsEthernet, null, tint = LocalAccent.current, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("IPv6", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = V7LColors.t0)
                    Text(
                        if (lang == "fa") "فعال‌سازی مسیریابی IPv6" else "Enable IPv6 routing",
                        fontSize = 11.sp, color = V7LColors.t3
                    )
                }
                V7LPanelSwitch(
                    checked = ipv6,
                    onCheckedChange = { scope.launch { settingsStore.setEnableIpv6(it) } }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // MUX
        V7LGlassCard(modifier = Modifier.fillMaxWidth(), radius = 16.dp) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Hub, null, tint = LocalAccent.current, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("MUX", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = V7LColors.t0)
                    Spacer(Modifier.weight(1f))
                    V7LPanelSwitch(
                        checked = muxEnabled,
                        onCheckedChange = { scope.launch { settingsStore.setMuxEnabled(it) } }
                    )
                }
                if (muxEnabled) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = muxConcurrencyInput,
                        onValueChange = {
                            muxConcurrencyInput = it
                            it.toIntOrNull()?.let { v ->
                                scope.launch { settingsStore.setMuxConcurrency(v) }
                            }
                        },
                        label = { Text("Concurrency", color = V7LColors.t3) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LocalAccent.current,
                            unfocusedBorderColor = V7LColors.border,
                            cursorColor = LocalAccent.current,
                            focusedTextColor = V7LColors.t0,
                            unfocusedTextColor = V7LColors.t1
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // MTU
        SettingTextField(
            label = if (lang == "fa") "MTU (خالی = خودکار 1500)" else "MTU (empty = auto 1500)",
            value = mtuInput,
            onValueChange = { mtuInput = it },
            onSave = { scope.launch { settingsStore.setMtuSize(mtuInput.toIntOrNull() ?: 0) } },
            placeholder = "1500"
        )

        Spacer(Modifier.height(12.dp))

        // Proxy Chain
        SettingTextField(
            label = if (lang == "fa") "پروکسی زنجیره" else "Proxy Chain URI",
            value = chainInput,
            onValueChange = { chainInput = it },
            onSave = { scope.launch { settingsStore.setProxyChainUri(chainInput) } },
            placeholder = "vless://... or vmess://..."
        )

        Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun SettingTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    placeholder: String
) {
    Column {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = V7LColors.t2)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                onSave()
            },
            placeholder = { Text(placeholder, color = V7LColors.t3, fontFamily = FiraCode, fontSize = 13.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = FiraCode,
                fontSize = 13.sp,
                color = V7LColors.t0
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LocalAccent.current,
                unfocusedBorderColor = V7LColors.border,
                focusedContainerColor = V7LColors.bg2,
                unfocusedContainerColor = V7LColors.bg2,
                cursorColor = LocalAccent.current
            )
        )
    }
}
