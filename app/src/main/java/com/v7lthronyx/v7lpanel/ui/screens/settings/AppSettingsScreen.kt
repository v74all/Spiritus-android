package com.v7lthronyx.v7lpanel.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v7lthronyx.v7lpanel.data.local.SettingsDataStore
import com.v7lthronyx.v7lpanel.ui.components.*
import com.v7lthronyx.v7lpanel.ui.theme.*
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun AppSettingsScreen(
    onBack: () -> Unit
) {
    val settingsStore: SettingsDataStore = koinInject()
    val lang = LocalLang.current
    val scope = rememberCoroutineScope()

    V7LPanelBackground(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, S.back, tint = V7LColors.t1)
                }
                Text(
                    S.appSettings,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = V7LColors.t0,
                    modifier = Modifier.weight(1f)
                )
            }
            // ── Language Selection ──
            V7LGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Language, null, tint = LocalAccent.current, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            S.language,
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = V7LColors.t0
                        )
                    }

                    HorizontalDivider(color = V7LColors.border)

                    // English option
                    LanguageOption(
                        label = "English",
                        selected = lang == "en",
                        onClick = { scope.launch { settingsStore.setLanguage("en") } }
                    )

                    // Farsi option
                    LanguageOption(
                        label = "فارسی",
                        selected = lang == "fa",
                        onClick = { scope.launch { settingsStore.setLanguage("fa") } }
                    )
                }
            }

            // ── Advanced Settings ──
            val enableIpv6 by settingsStore.enableIpv6.collectAsState(initial = false)
            val proxyChainUri by settingsStore.proxyChainUri.collectAsState(initial = "")
            val domainStrategy by settingsStore.domainStrategy.collectAsState(initial = "AsIs")
            val dnsServer by settingsStore.dnsServer.collectAsState(initial = "8.8.8.8")
            val muxEnabled by settingsStore.muxEnabled.collectAsState(initial = true)
            
            var showChainDialog by remember { mutableStateOf(false) }

            if (showChainDialog) {
                var tempUri by remember { mutableStateOf(proxyChainUri) }
                AlertDialog(
                    onDismissRequest = { showChainDialog = false },
                    title = { Text("Proxychain Config", fontFamily = JetBrainsMono, fontSize = 16.sp) },
                    text = {
                        Column {
                            Text("Enter frontend proxy URI (e.g. socks5://127.0.0.1:1080) to chain connections.", fontSize=12.sp, color=V7LColors.t2)
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = tempUri,
                                onValueChange = { tempUri = it },
                                placeholder = { Text("socks://...", color=V7LColors.t3) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            scope.launch { settingsStore.setProxyChainUri(tempUri) }
                            showChainDialog = false
                        }) { Text("Save", color = LocalAccent.current) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showChainDialog = false }) { Text("Cancel", color = V7LColors.t3) }
                    },
                    containerColor = V7LColors.bg1
                )
            }

            V7LGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.SettingsSuggest, null, tint = LocalAccent.current, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Advanced Configuration",
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = V7LColors.t0
                        )
                    }

                    HorizontalDivider(color = V7LColors.border)

                    // IPv6
                    Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Enable IPv6 Routing", fontSize=14.sp, color=V7LColors.t1)
                            Text("Route IPv6 traffic through VPN", fontSize=11.sp, color=V7LColors.t3)
                        }
                        V7LPanelSwitch(checked=enableIpv6, onCheckedChange={ scope.launch { settingsStore.setEnableIpv6(it) } })
                    }

                    // Mux
                    Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Enable MUX (Multiplexing)", fontSize=14.sp, color=V7LColors.t1)
                            Text("Improves concurrency for TCP", fontSize=11.sp, color=V7LColors.t3)
                        }
                        V7LPanelSwitch(checked=muxEnabled, onCheckedChange={ scope.launch { settingsStore.setMuxEnabled(it) } })
                    }

                    // DNS
                    Row(Modifier.fillMaxWidth().clickable { /* Could show dialog */ }, verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Remote DNS Server", fontSize=14.sp, color=V7LColors.t1)
                            Text(dnsServer, fontSize=11.sp, color=LocalAccent.current)
                        }
                    }

                    // Proxychain
                    Row(Modifier.fillMaxWidth().clickable { showChainDialog = true }, verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Proxychain", fontSize=14.sp, color=V7LColors.t1)
                            Text(if (proxyChainUri.isBlank()) "Disabled" else "Enabled / URI Configured", fontSize=11.sp, color=if (proxyChainUri.isBlank()) V7LColors.t3 else V7LColors.green)
                        }
                    }

                    // Domain Strategy
                    Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Domain Strategy", fontSize=14.sp, color=V7LColors.t1)
                            Text(domainStrategy, fontSize=11.sp, color=LocalAccent.current)
                        }
                    }
                }
            }

            // ── About ──
            V7LGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Info, null, tint = LocalAccent.current, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            S.about,
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = V7LColors.t0
                        )
                    }

                    HorizontalDivider(color = V7LColors.border)

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(S.appVersion, fontFamily = FiraCode, fontSize = 13.sp, color = V7LColors.t2)
                        Text(S.version, fontFamily = FiraCode, fontSize = 13.sp, color = LocalAccent.current)
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(S.appName, fontFamily = FiraCode, fontSize = 13.sp, color = V7LColors.t2)
                        Text(S.appName, fontFamily = FiraCode, fontSize = 13.sp, color = LocalAccent.current)
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) V7LColors.accentBg else V7LColors.bg2,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = LocalAccent.current,
                    unselectedColor = V7LColors.t3
                )
            )
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                fontFamily = if (label == "فارسی") Inter else JetBrainsMono,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 15.sp,
                color = if (selected) LocalAccent.current else V7LColors.t1
            )
            if (selected) {
                Spacer(Modifier.weight(1f))
                Icon(Icons.Filled.Check, null, tint = LocalAccent.current, modifier = Modifier.size(20.dp))
            }
        }
    }
}
