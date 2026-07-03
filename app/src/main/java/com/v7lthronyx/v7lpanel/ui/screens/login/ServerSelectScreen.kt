package com.v7lthronyx.v7lpanel.ui.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v7lthronyx.v7lpanel.data.db.entities.ServerProfile
import com.v7lthronyx.v7lpanel.data.security.TlsPin
import com.v7lthronyx.v7lpanel.SessionHolder
import com.v7lthronyx.v7lpanel.ui.components.*
import com.v7lthronyx.v7lpanel.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import java.net.HttpURLConnection
import java.net.URL

// ── URL parser: auto-detect role, base URL, UUID / agent name ────────────────

data class ParsedUrl(
    val baseUrl: String,
    val role: String,          // admin | agent | subscriber
    val uuid: String? = null,
    val agentName: String? = null,
    val tlsPinSha256: String? = null,
    val suggestedName: String = ""
)

/**
 * Smart URL parser:
 *  • raw VPN share links are rejected; only panel URLs/import URIs are accepted
 *  • https://ip:8080/sub/UUID         → subscriber
 *  • https://ip:8080/sub-info/UUID    → subscriber
 *  • https://ip:8080/sub-api/UUID     → subscriber
 *  • https://ip:8080/agent            → agent
 *  • https://ip:8080                  → admin
 *  • Fixes missing scheme, trailing slashes, double-ports, etc.
 */
fun parseServerUrl(raw: String): ParsedUrl? {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return null
    val vpnPrefixes = listOf("vless://", "vmess://", "trojan://", "ss://", "tuic://", "hy2://", "wireguard://")
    if (vpnPrefixes.any { trimmed.startsWith(it, ignoreCase = true) }) return null

    fun query(uri: java.net.URI): Map<String, String> = (uri.rawQuery ?: "")
        .split('&').filter { it.isNotBlank() }.associate { item ->
            val key = item.substringBefore('=')
            val value = item.substringAfter('=', "")
            java.net.URLDecoder.decode(key, "UTF-8") to java.net.URLDecoder.decode(value, "UTF-8")
        }
    fun validPin(value: String?): String? = value?.trim()?.takeIf {
        it.matches(Regex("^sha256/[A-Za-z0-9+/]{43}=$"))
    }
    fun validUuid(value: String?): String? = value?.takeIf {
        runCatching { java.util.UUID.fromString(it) }.isSuccess
    }

    var source = trimmed
    var requestedRole: String? = null
    var requestedUuid: String? = null
    var requestedAgent: String? = null
    var requestedPin: String? = null
    val importUri = runCatching { java.net.URI(trimmed) }.getOrNull()
    if (importUri?.scheme?.lowercase() in setOf("spiritus", "v7l") && importUri?.host == "import") {
        val params = query(importUri)
        source = params["server"] ?: return null
        requestedRole = params["role"]
        requestedUuid = validUuid(params["uuid"])
        requestedAgent = params["agent"]?.takeIf { it.matches(Regex("^[A-Za-z0-9_.-]{1,100}$")) }
        requestedPin = validPin(params["pin"]) ?: if (params.containsKey("pin")) return null else null
    }

    if (!source.startsWith("http://", true) && !source.startsWith("https://", true)) {
        source = "https://$source"
    }
    val uri = runCatching { java.net.URI(source) }.getOrNull() ?: return null
    if (uri.scheme !in listOf("http", "https") || uri.host.isNullOrBlank() || uri.userInfo != null) return null
    if (uri.scheme == "http" && uri.host !in setOf("localhost", "127.0.0.1", "::1")) return null

    val authority = uri.rawAuthority ?: return null
    val baseUrl = "${uri.scheme}://$authority"
    val path = uri.path?.trimEnd('/') ?: ""
    val host = uri.host
    val sub = Regex("^/(?:sub|sub-info|sub-api|sub-json)/([0-9a-fA-F-]{36})$").matchEntire(path)
    val role = when {
        requestedRole in setOf("admin", "agent", "subscriber") -> requestedRole!!
        sub != null -> "subscriber"
        path == "/agent" || path.startsWith("/agent/") -> "agent"
        path.isBlank() || path == "/" -> "admin"
        else -> return null
    }
    val uuid = requestedUuid ?: validUuid(sub?.groupValues?.get(1))
    if (role == "subscriber" && uuid == null) return null
    return ParsedUrl(
        baseUrl = baseUrl,
        role = role,
        uuid = uuid,
        agentName = requestedAgent,
        tlsPinSha256 = requestedPin,
        suggestedName = "$host (${role.replaceFirstChar { it.uppercase() }})"
    )
}

/** Friendly label for a raw config: its #fragment, else the protocol scheme. */
fun rawConfigLabel(uri: String): String {
    val frag = uri.substringAfterLast("#", "").trim()
    if (frag.isNotBlank()) return runCatching { java.net.URLDecoder.decode(frag, "UTF-8") }.getOrDefault(frag)
    return uri.substringBefore("://").uppercase().ifBlank { "Config" }
}

@Composable
fun ServerSelectScreen(
    onSubscriber: (String, String) -> Unit,
    onManagement: (ServerProfile) -> Unit,
    onLocalConfig: (String, String) -> Unit = { _, _ -> },
    onQRScanner:  () -> Unit = {},
    scannedUrl:   String = "",
    viewModel:    LoginViewModel = koinViewModel()
) {
    val profiles by viewModel.profiles.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var prefillUrl    by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var pingResults by remember { mutableStateOf(mapOf<Int, Long>()) }

    LaunchedEffect(scannedUrl) {
        if (scannedUrl.isNotBlank()) { prefillUrl = scannedUrl; showAddDialog = true }
    }

    LaunchedEffect(profiles) {
        profiles.forEach { p ->
            scope.launch { pingResults = pingResults + (p.id to measurePing(p.url)) }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallFloatingActionButton(
                    onClick = onQRScanner,
                    containerColor = V7LColors.bg2,
                    contentColor   = LocalAccent.current
                ) { Icon(Icons.Filled.QrCodeScanner, "Scan QR") }

                SmallFloatingActionButton(
                    onClick = {
                        val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                                as android.content.ClipboardManager
                        val clip = cm.primaryClip?.getItemAt(0)?.text?.toString()?.trim() ?: ""
                        if (clip.isNotBlank()) {
                            val rawCfg = if (clip.length > 6) com.v7lthronyx.v7lpanel.vpn.ConfigParser.parse(clip, rawConfigLabel(clip)) else null
                            val parsed = if (clip.length > 6) parseServerUrl(clip) else null
                            if (rawCfg != null) {
                                onLocalConfig(rawCfg.label, clip)
                            } else if (parsed != null && !parsed.uuid.isNullOrBlank() && parsed.baseUrl.isNotBlank()) {
                                viewModel.addProfile(parsed.suggestedName.ifBlank { parsed.baseUrl },
                                    parsed.baseUrl, parsed.role, parsed.agentName, parsed.uuid, parsed.tlsPinSha256)
                                onSubscriber(parsed.baseUrl, parsed.uuid)
                            } else { prefillUrl = clip; showAddDialog = true }
                        } else showAddDialog = true
                    },
                    containerColor = V7LColors.bg2,
                    contentColor   = V7LColors.green
                ) { Icon(Icons.Filled.ContentPaste, "Paste Link") }

                FloatingActionButton(
                    onClick        = { showAddDialog = true },
                    containerColor = LocalAccentLight.current,
                    contentColor   = V7LColors.t0
                ) { Icon(Icons.Filled.Add, "Add Server") }
            }
        }
    ) { padding ->
        V7LPanelBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(52.dp))
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                com.v7lthronyx.v7lpanel.ui.theme.LocalAccent.current,
                                com.v7lthronyx.v7lpanel.ui.theme.LocalAccentLight.current
                            )
                        ),
                        RoundedCornerShape(26.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Shield, null, tint = Color.White, modifier = Modifier.size(46.dp))
            }
            Spacer(Modifier.height(22.dp))
            Row {
                Text(
                    "Spirit",
                    fontFamily = com.v7lthronyx.v7lpanel.ui.theme.SpaceGrotesk,
                    fontWeight = FontWeight.Bold, fontSize = 31.sp, color = V7LColors.t0
                )
                Text(
                    "us",
                    fontFamily = com.v7lthronyx.v7lpanel.ui.theme.SpaceGrotesk,
                    fontWeight = FontWeight.Bold, fontSize = 31.sp,
                    color = com.v7lthronyx.v7lpanel.ui.theme.LocalAccent.current
                )
            }
            Text(
                "Your private tunnel - fast, quiet, encrypted.",
                modifier = Modifier.padding(top = 8.dp),
                color = V7LColors.t3,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(38.dp))
            Spacer(Modifier.height(16.dp))
            Text(S.serverConnections, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold,
                fontSize = 20.sp, color = V7LColors.t0)
            Spacer(Modifier.height(4.dp))
            Text(S.selectServer, style = MaterialTheme.typography.bodySmall, color = V7LColors.t3)
            Spacer(Modifier.height(16.dp))

            if (profiles.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(S.emptyServerHint, color = V7LColors.t3, fontSize = 14.sp)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(profiles) { profile ->
                        ServerProfileCard(
                            profile   = profile,
                            pingMs    = pingResults[profile.id],
                            onConnect = {
                                SessionHolder.profileId = profile.id
                                SessionHolder.serverUrl = profile.url
                                SessionHolder.role = profile.role
                                SessionHolder.principal = profile.agentName.orEmpty()
                                SessionHolder.tlsPinSha256 = profile.tlsPinSha256
                                if (profile.role == "subscriber") {
                                    onSubscriber(profile.url, profile.subscriberUuid ?: "")
                                } else {
                                    onManagement(profile)
                                }
                            },
                            onDelete  = { viewModel.deleteProfile(profile) }
                        )
                    }
                }
            }
        }
        }
    }

    if (showAddDialog) {
        SmartAddDialog(
            initialUrl = prefillUrl,
            onDismiss  = { showAddDialog = false; prefillUrl = "" },
            onLocalConfig = { label, uri ->
                showAddDialog = false; prefillUrl = ""
                onLocalConfig(label, uri)
            },
            onAdd      = { name, url, actualRole, actualAgentName, uuid, pin ->
                showAddDialog = false
                prefillUrl = ""
                val safeUuid = if (actualRole == "subscriber") uuid else null
                SessionHolder.serverUrl = url
                SessionHolder.role = actualRole
                SessionHolder.principal = actualAgentName.orEmpty()
                SessionHolder.tlsPinSha256 = pin
                viewModel.addProfile(name, url, actualRole, actualAgentName, safeUuid, pin) { id ->
                    SessionHolder.profileId = id
                    if (actualRole != "subscriber") {
                        onManagement(ServerProfile(id, name, url, actualRole,
                            System.currentTimeMillis(), null, actualAgentName, pin))
                    }
                }
                if (actualRole == "subscriber") {
                    onSubscriber(url, safeUuid ?: "")
                }
            }
        )
    }
}

@Composable
private fun ServerProfileCard(
    profile: ServerProfile,
    pingMs: Long?,
    onConnect: () -> Unit,
    onDelete: () -> Unit
) {
    val icon: ImageVector = when (profile.role) {
        "admin"      -> Icons.Filled.AdminPanelSettings
        "agent"      -> Icons.Filled.SupportAgent
        "subscriber" -> Icons.Filled.Person
        else         -> Icons.Filled.Dns
    }
    val roleColor = when (profile.role) {
        "admin"      -> LocalAccent.current
        "agent"      -> V7LColors.purple
        "subscriber" -> V7LColors.green
        else         -> V7LColors.t3
    }

    V7LGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onConnect() },
        radius = 18.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            V7LIconTile(icon, roleColor, size = 44.dp, iconSize = 22.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(profile.name, fontFamily = Inter, fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp, color = V7LColors.t0, maxLines = 1)
                Text(profile.url, fontFamily = FiraCode, fontSize = 11.sp, color = V7LColors.t3, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        profile.role.replaceFirstChar { it.titlecase() },
                        fontFamily = Inter, fontSize = 12.sp, color = roleColor
                    )
                    when {
                        pingMs == null -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(10.dp),
                                strokeWidth = 1.5.dp,
                                color = V7LColors.t3
                            )
                        }
                        pingMs < 0 -> Text(S.offline, fontFamily = FiraCode, fontSize = 10.sp, color = V7LColors.red)
                        else -> {
                            val pingColor = when {
                                pingMs < 200  -> V7LColors.green
                                pingMs < 500  -> V7LColors.yellow
                                else          -> V7LColors.red
                            }
                            Text("${pingMs}ms", fontFamily = FiraCode, fontSize = 10.sp, color = pingColor)
                        }
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Close, "Remove", tint = V7LColors.t4, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ── Smart Add Dialog: paste URL → auto-detect everything ─────────────────────

@Composable
private fun SmartAddDialog(
    initialUrl: String = "",
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String?, String?, String?) -> Unit,
    onLocalConfig: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current

    // Single input field: the URL
    var rawUrl by remember(initialUrl) {
        mutableStateOf(if (initialUrl.isNotBlank()) initialUrl else "")
    }
    // Parsed result — updates live
    val parsed = remember(rawUrl) {
        if (rawUrl.length > 6) parseServerUrl(rawUrl) else null
    }
    // Raw VPN config (vless://, vmess://, …) — a normal client config to use locally.
    val rawConfig = remember(rawUrl) {
        if (rawUrl.length > 6) com.v7lthronyx.v7lpanel.vpn.ConfigParser.parse(rawUrl.trim(), rawConfigLabel(rawUrl.trim())) else null
    }
    // Editable overrides
    var name by remember(parsed?.suggestedName) { mutableStateOf(parsed?.suggestedName ?: "") }
    var role by remember(parsed?.role) { mutableStateOf(parsed?.role ?: "subscriber") }
    var uuid by remember(parsed?.uuid) { mutableStateOf(parsed?.uuid ?: "") }
    var agentName by remember(parsed?.agentName) { mutableStateOf(parsed?.agentName ?: "") }
    var tlsPin by remember(parsed?.tlsPinSha256) { mutableStateOf(parsed?.tlsPinSha256 ?: "") }
    var showAdvanced by remember { mutableStateOf(false) }
    val normalizedPin = TlsPin.normalize(tlsPin.ifBlank { null })
    val validUuid = role != "subscriber" || runCatching { java.util.UUID.fromString(uuid) }.isSuccess
    val validAgent = role != "agent" || agentName.isBlank() ||
        agentName.matches(Regex("^[A-Za-z0-9_.-]{1,100}$"))
    val canAdd = rawConfig != null || (parsed != null && validUuid && validAgent &&
        (tlsPin.isBlank() || normalizedPin != null))

    fun pasteFromClipboard(): String {
        val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                as android.content.ClipboardManager
        return cm.primaryClip?.getItemAt(0)?.text?.toString()?.trim() ?: ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = V7LColors.bg2,
        titleContentColor = V7LColors.t0,
        title = { Text(S.addServer, fontFamily = JetBrainsMono, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // URL field with paste
                Text(S.pasteLink, fontFamily = Inter, fontSize = 12.sp, color = V7LColors.t3)
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    V7LTextField(
                        value = rawUrl,
                        onValueChange = { rawUrl = it },
                        label = S.linkUrl,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            val clip = pasteFromClipboard()
                            if (clip.isNotBlank()) rawUrl = clip
                        }
                    ) {
                        Icon(Icons.Filled.ContentPaste, "Paste", tint = LocalAccent.current)
                    }
                }

                // Raw config detected — a normal client config
                rawConfig?.let { c ->
                    Surface(color = V7LColors.bg3, shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("✓ ${c.protocol.uppercase()} config", fontFamily = FiraCode, fontSize = 12.sp, color = V7LColors.green)
                            Text("${c.serverAddress}:${c.serverPort}", fontFamily = FiraCode, fontSize = 10.sp, color = V7LColors.t3)
                        }
                    }
                }

                // Auto-detected info
                if (rawConfig == null) parsed?.let { p ->
                    Surface(color = V7LColors.bg3, shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            val roleLabel = when (p.role) {
                                "subscriber" -> S.subscriber
                                "agent"      -> S.agent
                                else         -> S.admin
                            }
                            Text("✓ $roleLabel", fontFamily = FiraCode, fontSize = 12.sp,
                                color = when (p.role) {
                                    "subscriber" -> V7LColors.green
                                    "agent"      -> V7LColors.purple
                                    else         -> LocalAccent.current
                                })
                            Text(p.baseUrl, fontFamily = FiraCode, fontSize = 10.sp, color = V7LColors.t3)
                            if (p.uuid != null) {
                                Text("UUID: ${p.uuid}", fontFamily = FiraCode, fontSize = 10.sp, color = V7LColors.green)
                            }
                        }
                    }
                }
                if (rawUrl.length > 6 && parsed == null) {
                    Text("Invalid secure panel URL", color = V7LColors.red, fontSize = 11.sp)
                }

                // Advanced: edit name  
                TextButton(onClick = { showAdvanced = !showAdvanced }) {
                    Text(if (showAdvanced) S.advancedSettingsUp else S.advancedSettings,
                        fontFamily = FiraCode, fontSize = 11.sp, color = V7LColors.t3)
                }
                if (showAdvanced) {
                    V7LTextField(value = name, onValueChange = { name = it }, label = S.serverName)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("subscriber", "agent", "admin").forEach { candidate ->
                            FilterChip(
                                selected = role == candidate,
                                onClick = { role = candidate },
                                label = { Text(candidate) }
                            )
                        }
                    }
                    if (role == "subscriber") {
                        V7LTextField(value = uuid, onValueChange = { uuid = it }, label = "UUID")
                    }
                    if (role == "agent") {
                        V7LTextField(value = agentName, onValueChange = { agentName = it }, label = "Agent username")
                    }
                    V7LTextField(value = tlsPin, onValueChange = { tlsPin = it }, label = "TLS pin (sha256/...)")
                    if (tlsPin.isNotBlank() && normalizedPin == null) {
                        Text("Invalid SHA-256 certificate pin", color = V7LColors.red, fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (rawConfig != null) {
                        onLocalConfig(rawConfig.label, rawUrl.trim()); return@TextButton
                    }
                    val finalUrl  = parsed?.baseUrl ?: return@TextButton
                    val finalName = name.ifBlank { parsed?.suggestedName ?: finalUrl }
                    val finalUuid = if (role == "subscriber") uuid.ifBlank { null } else null
                    val finalAgent = if (role == "agent") agentName.ifBlank { null } else null
                    onAdd(finalName, finalUrl, role, finalAgent, finalUuid, normalizedPin)
                },
                enabled = canAdd
            ) {
                Text(S.add, color = LocalAccent.current, fontFamily = JetBrainsMono)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(S.cancel, color = V7LColors.t3) }
        }
    )
}

@Composable
private fun V7LTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = LocalAccent.current,
            unfocusedBorderColor = V7LColors.border,
            focusedLabelColor    = LocalAccent.current,
            unfocusedLabelColor  = V7LColors.t3,
            cursorColor          = LocalAccent.current,
            focusedTextColor     = V7LColors.t1,
            unfocusedTextColor   = V7LColors.t1
        ),
        modifier = modifier
    )
}

// ── Ping helper ──────────────────────────────────────────────────────────────

private suspend fun measurePing(baseUrl: String): Long = withContext(Dispatchers.IO) {
    try {
        val hostPort = com.v7lthronyx.v7lpanel.util.PingTester.extractHostPort(baseUrl)
        if (hostPort != null) {
            com.v7lthronyx.v7lpanel.util.PingTester.tcpPing(hostPort.first, hostPort.second)
        } else {
            -1L
        }
    } catch (_: Exception) { -1L }
}
