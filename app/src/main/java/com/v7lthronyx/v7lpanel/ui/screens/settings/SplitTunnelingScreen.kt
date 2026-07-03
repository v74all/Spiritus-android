package com.v7lthronyx.v7lpanel.ui.screens.settings

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.v7lthronyx.v7lpanel.data.local.SettingsDataStore
import com.v7lthronyx.v7lpanel.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

data class AppInfo(
    val packageName: String,
    val label: String,
    val isSystem: Boolean
)

@Composable
fun SplitTunnelingScreen(
    onBack: () -> Unit
) {
    val settingsStore: SettingsDataStore = koinInject()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val mode by settingsStore.splitTunnelMode.collectAsState(initial = "off")
    val selectedApps by settingsStore.splitTunnelApps.collectAsState(initial = emptySet())
    val lang by settingsStore.language.collectAsState(initial = "en")

    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var showSystem by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { it.packageName != context.packageName }
                .map { ai ->
                    AppInfo(
                        packageName = ai.packageName,
                        label = ai.loadLabel(pm).toString(),
                        isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    )
                }
                .sortedBy { it.label.lowercase() }
        }
    }

    val filteredApps = apps.filter { app ->
        (showSystem || !app.isSystem) &&
        (searchQuery.isBlank() || app.label.contains(searchQuery, true) || app.packageName.contains(searchQuery, true))
    }

    com.v7lthronyx.v7lpanel.ui.components.AuroraBackground(Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Back", tint = V7LColors.t0)
            }
            Text(
                if (lang == "fa") "تونل تقسیم" else "Split Tunneling",
                fontFamily = JetBrainsMono,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = V7LColors.t0
            )
        }

        // Mode selector
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("off" to (if (lang == "fa") "خاموش" else "Off"),
                "allowlist" to (if (lang == "fa") "مجاز" else "Allowlist"),
                "disallowlist" to (if (lang == "fa") "غیرمجاز" else "Disallowlist")
            ).forEach { (value, label) ->
                FilterChip(
                    selected = mode == value,
                    onClick = { scope.launch { settingsStore.setSplitTunnelMode(value) } },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = V7LColors.accentDark.copy(alpha = 0.3f),
                        selectedLabelColor = LocalAccent.current,
                        labelColor = V7LColors.t2
                    )
                )
            }
        }

        if (mode != "off") {
            Text(
                text = when (mode) {
                    "allowlist" -> if (lang == "fa") "فقط اپ‌های انتخاب شده از VPN استفاده می‌کنند"
                        else "Only selected apps use VPN"
                    else -> if (lang == "fa") "اپ‌های انتخاب شده از VPN عبور نمی‌کنند"
                        else "Selected apps bypass VPN"
                },
                fontSize = 11.sp,
                color = V7LColors.t3,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(if (lang == "fa") "جستجوی اپ..." else "Search apps...", color = V7LColors.t3) },
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = V7LColors.t3) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LocalAccent.current,
                    unfocusedBorderColor = V7LColors.border,
                    focusedContainerColor = V7LColors.bg2,
                    unfocusedContainerColor = V7LColors.bg2,
                    cursorColor = LocalAccent.current,
                    focusedTextColor = V7LColors.t0,
                    unfocusedTextColor = V7LColors.t1
                )
            )

            // Show system toggle
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (lang == "fa") "نمایش اپ‌های سیستمی" else "Show system apps",
                    fontSize = 12.sp, color = V7LColors.t2,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = showSystem,
                    onCheckedChange = { showSystem = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = LocalAccent.current,
                        checkedTrackColor = V7LColors.accentDark
                    )
                )
            }

            // App list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    val isSelected = app.packageName in selectedApps
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    val updated = if (checked) selectedApps + app.packageName
                                        else selectedApps - app.packageName
                                    settingsStore.setSplitTunnelApps(updated)
                                }
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = LocalAccent.current,
                                uncheckedColor = V7LColors.t3
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(app.label, fontSize = 13.sp, color = V7LColors.t0, maxLines = 1)
                            Text(app.packageName, fontSize = 10.sp, color = V7LColors.t3, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
    }
}
