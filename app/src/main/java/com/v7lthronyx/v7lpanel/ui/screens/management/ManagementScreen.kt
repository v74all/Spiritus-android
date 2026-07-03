package com.v7lthronyx.v7lpanel.ui.screens.management

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v7lthronyx.v7lpanel.SessionHolder
import com.v7lthronyx.v7lpanel.data.api.models.UserDto
import com.v7lthronyx.v7lpanel.data.security.SecureTokenStore
import com.v7lthronyx.v7lpanel.ui.components.V7LPanelBackground
import com.v7lthronyx.v7lpanel.ui.theme.*
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagementScreen(
    onLogout: () -> Unit,
    viewModel: ManagementViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val tokenStore: SecureTokenStore = koinInject()
    var showUserDialog by remember { mutableStateOf(false) }
    var showAgentDialog by remember { mutableStateOf(false) }
    var renewUser by remember { mutableStateOf<UserDto?>(null) }
    var deleteUser by remember { mutableStateOf<UserDto?>(null) }
    var addTrafficUser by remember { mutableStateOf<UserDto?>(null) }
    var speedLimitUser by remember { mutableStateOf<UserDto?>(null) }
    var resetTrafficUser by remember { mutableStateOf<UserDto?>(null) }
    var noteUser by remember { mutableStateOf<UserDto?>(null) }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("${SessionHolder.role.replaceFirstChar { it.uppercase() }} console") },
                actions = {
                    IconButton(onClick = viewModel::refresh) { Icon(Icons.Default.Refresh, "Refresh") }
                    IconButton(onClick = viewModel::syncServer) { Icon(Icons.Default.Sync, "Sync server") }
                    IconButton(onClick = {
                        runCatching { SessionHolder.getOrCreateClient().close() }
                        tokenStore.clear(); SessionHolder.clear(); onLogout()
                    }) { Icon(Icons.Default.Logout, "Logout") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = V7LColors.bg1,
                    titleContentColor = V7LColors.t0,
                    actionIconContentColor = V7LColors.t1
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showUserDialog = true },
                containerColor = com.v7lthronyx.v7lpanel.ui.theme.LocalAccent.current,
                contentColor = androidx.compose.ui.graphics.Color.White
            ) {
                Icon(Icons.Default.PersonAdd, "Add user")
            }
        }
    ) { padding ->
        V7LPanelBackground(Modifier.fillMaxSize().padding(padding)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            // ── stat cards ──
            item {
                val online = state.users.count { it.onlineIps > 0 || it.active }
                val trafficGb = state.users.sumOf { it.usedGb }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminStat(Modifier.weight(1f), "${state.users.size}", "Users", Dz.tHi)
                    AdminStat(Modifier.weight(1f), "$online / ${state.users.size}", "Online", Dz.connected)
                    AdminStat(Modifier.weight(1f), trafficLabel(trafficGb), "Traffic", Dz.cyan)
                }
            }
            // ── users header ──
            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Users", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Dz.t1, fontFamily = Vazirmatn)
                    if (SessionHolder.role == "admin") {
                        Row(
                            Modifier.clip(RoundedCornerShape(11.dp)).background(LocalAccent.current.copy(0.12f))
                                .border(1.dp, LocalAccent.current.copy(0.3f), RoundedCornerShape(11.dp))
                                .clickable { showAgentDialog = true }.padding(horizontal = 13.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.SupportAgent, null, tint = LocalAccent.current, modifier = Modifier.size(14.dp))
                            Text("Add agent", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = LocalAccent.current, fontFamily = Vazirmatn)
                        }
                    }
                }
            }
            items(state.users, key = { it.uuid.ifBlank { it.name } }) { user ->
                AdminUserCard(
                    user = user,
                    onToggle = { viewModel.toggleUser(user.name) },
                    onRenew = { renewUser = user },
                    onDelete = { deleteUser = user },
                    onAddTraffic = { addTrafficUser = user },
                    onSpeedLimit = if (SessionHolder.role == "admin") { { speedLimitUser = user } } else null,
                    onResetTraffic = if (SessionHolder.role == "admin") { { resetTrafficUser = user } } else null,
                    onEditNote = if (SessionHolder.role == "admin") { { noteUser = user } } else null
                )
            }
            if (SessionHolder.role == "admin" && state.agents.isNotEmpty()) {
                item { Text("Agents", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Dz.t1, fontFamily = Vazirmatn, modifier = Modifier.padding(top = 16.dp)) }
                items(state.agents, key = { it.id }) { agent ->
                    AdminAgentCard(agent.name, agent.userCount, agent.trafficUsedGb) { viewModel.deleteAgent(agent.id) }
                }
            }
        }
        }
    }

    if (showUserDialog) UserEditorDialog(
        title = "Create user",
        onDismiss = { showUserDialog = false },
        onConfirm = { name, traffic, days ->
            showUserDialog = false; viewModel.createUser(name, traffic, days)
        }
    )
    renewUser?.let { user -> UserEditorDialog(
        title = "Renew ${user.name}", initialName = user.name, lockName = true,
        onDismiss = { renewUser = null },
        onConfirm = { _, traffic, days -> renewUser = null; viewModel.renewUser(user.name, traffic, days) }
    ) }
    deleteUser?.let { user -> AlertDialog(
        onDismissRequest = { deleteUser = null },
        title = { Text("Delete user") },
        text = { Text("Delete ${user.name}? This cannot be undone.") },
        confirmButton = { TextButton(onClick = { deleteUser = null; viewModel.deleteUser(user.name) }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { deleteUser = null }) { Text("Cancel") } }
    ) }
    if (showAgentDialog) AgentEditorDialog(
        onDismiss = { showAgentDialog = false },
        onConfirm = { name, password, quota ->
            showAgentDialog = false; viewModel.createAgent(name, password, quota)
        }
    )
    addTrafficUser?.let { user -> AddTrafficDialog(
        userName = user.name,
        onDismiss = { addTrafficUser = null },
        onConfirm = { gb -> addTrafficUser = null; viewModel.addTraffic(user.name, gb) }
    ) }
    speedLimitUser?.let { user -> SpeedLimitDialog(
        userName = user.name,
        initialDownKbps = user.speedLimitDown,
        initialUpKbps = user.speedLimitUp,
        onDismiss = { speedLimitUser = null },
        onConfirm = { down, up -> speedLimitUser = null; viewModel.setSpeedLimit(user.name, down, up) }
    ) }
    resetTrafficUser?.let { user -> AlertDialog(
        onDismissRequest = { resetTrafficUser = null },
        title = { Text("Reset traffic") },
        text = { Text("Reset used traffic for ${user.name} to zero?") },
        confirmButton = { TextButton(onClick = { resetTrafficUser = null; viewModel.resetTraffic(user.name) }) { Text("Reset") } },
        dismissButton = { TextButton(onClick = { resetTrafficUser = null }) { Text("Cancel") } }
    ) }
    noteUser?.let { user -> NoteDialog(
        userName = user.name,
        initialNote = user.note,
        onDismiss = { noteUser = null },
        onConfirm = { note -> noteUser = null; viewModel.updateNote(user.name, note) }
    ) }
    val notice = state.error ?: state.message
    if (notice != null) AlertDialog(
        onDismissRequest = viewModel::clearNotice,
        text = { Text(notice) },
        confirmButton = { TextButton(onClick = viewModel::clearNotice) { Text("OK") } }
    )
    if (state.loading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

private fun trafficLabel(gb: Double): String =
    if (gb >= 1024) "%.1f TB".format(gb / 1024) else "%.0f GB".format(gb)

@Composable
private fun AdminStat(modifier: Modifier, value: String, label: String, valueColor: Color) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(Dz.surf035)
            .border(1.dp, Dz.border, RoundedCornerShape(16.dp)).padding(14.dp)
    ) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = valueColor, fontFamily = JetBrainsMono, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 11.sp, color = Dz.t4, fontFamily = Vazirmatn)
    }
}

@Composable
private fun AdminUserCard(
    user: UserDto,
    onToggle: () -> Unit,
    onRenew: () -> Unit,
    onDelete: () -> Unit,
    onAddTraffic: () -> Unit,
    onSpeedLimit: (() -> Unit)?,
    onResetTraffic: (() -> Unit)?,
    onEditNote: (() -> Unit)?
) {
    val accent = LocalAccent.current
    var showMore by remember { mutableStateOf(false) }
    val pct = if (user.limitGb > 0) (user.usedGb / user.limitGb).coerceIn(0.0, 1.0).toFloat() else 0f
    val daysColor = when { user.daysLeft <= 0 -> Dz.danger; user.daysLeft < 7 -> Dz.connecting; else -> Dz.connected }
    val pctColor = when { pct >= 0.9f -> Dz.danger; pct >= 0.7f -> Dz.connecting; else -> accent }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Dz.surf035)
            .border(1.dp, Dz.border, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(0.15f)), contentAlignment = Alignment.Center) {
                Text(user.name.firstOrNull()?.uppercase() ?: "?", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = accent, fontFamily = SpaceGrotesk)
                Box(
                    Modifier.align(Alignment.BottomEnd).size(11.dp).clip(CircleShape)
                        .background(if (user.onlineIps > 0) Dz.connected else Dz.tMute)
                        .border(2.dp, V7LColors.bg1, CircleShape)
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(user.name, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = Dz.tHi, fontFamily = JetBrainsMono, maxLines = 1)
                Text("${"%.1f".format(user.usedGb)} / ${"%.0f".format(user.limitGb)} GB", fontSize = 11.sp, color = Dz.t3, fontFamily = JetBrainsMono)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(if (user.daysLeft <= 0) "0" else "${user.daysLeft}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = daysColor, fontFamily = JetBrainsMono)
                Text("days", fontSize = 9.5.sp, color = Dz.t4, fontFamily = Vazirmatn)
            }
        }
        Spacer(Modifier.height(11.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(4.dp)).background(Dz.surf06)) {
                Box(Modifier.fillMaxWidth(pct).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(pctColor))
            }
            AdminActionBtn(Icons.Default.PowerSettingsNew, if (user.active) Dz.connected else Dz.tMute, Dz.surf05, onToggle)
            AdminActionBtn(Icons.Default.Update, Dz.t3, Dz.surf05, onRenew)
            Box {
                AdminActionBtn(Icons.Default.MoreVert, Dz.t3, Dz.surf05) { showMore = true }
                DropdownMenu(expanded = showMore, onDismissRequest = { showMore = false }) {
                    DropdownMenuItem(
                        text = { Text("Add traffic") },
                        leadingIcon = { Icon(Icons.Default.DataUsage, null) },
                        onClick = { showMore = false; onAddTraffic() }
                    )
                    if (onSpeedLimit != null) DropdownMenuItem(
                        text = { Text("Speed limit") },
                        leadingIcon = { Icon(Icons.Default.Speed, null) },
                        onClick = { showMore = false; onSpeedLimit() }
                    )
                    if (onResetTraffic != null) DropdownMenuItem(
                        text = { Text("Reset traffic") },
                        leadingIcon = { Icon(Icons.Default.RestartAlt, null) },
                        onClick = { showMore = false; onResetTraffic() }
                    )
                    if (onEditNote != null) DropdownMenuItem(
                        text = { Text("Edit note") },
                        leadingIcon = { Icon(Icons.Default.EditNote, null) },
                        onClick = { showMore = false; onEditNote() }
                    )
                }
            }
            AdminActionBtn(Icons.Default.Delete, Dz.danger, Dz.danger.copy(0.1f), onDelete)
        }
    }
}

@Composable
private fun AdminActionBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, bg: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(15.dp)) }
}

@Composable
private fun AdminAgentCard(name: String, users: Int, usedGb: Double, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Dz.surf035)
            .border(1.dp, Dz.border, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Dz.cyan.copy(0.13f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Dns, null, tint = Dz.cyan, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(name, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = Dz.tHi, fontFamily = JetBrainsMono)
            Text("$users users · ${"%.1f".format(usedGb)} GB", fontSize = 11.sp, color = Dz.t3, fontFamily = Vazirmatn)
        }
        AdminActionBtn(Icons.Default.Delete, Dz.danger, Dz.danger.copy(0.1f), onDelete)
    }
}

@Composable
private fun UserEditorDialog(
    title: String,
    initialName: String = "",
    lockName: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Int) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var traffic by remember { mutableStateOf("10") }
    var days by remember { mutableStateOf("30") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Name") }, enabled = !lockName)
            OutlinedTextField(traffic, { traffic = it }, label = { Text("Traffic GB") })
            OutlinedTextField(days, { days = it }, label = { Text("Days") })
        } },
        confirmButton = { TextButton(
            enabled = name.isNotBlank() && traffic.toDoubleOrNull() != null && days.toIntOrNull() != null,
            onClick = { onConfirm(name, traffic.toDouble(), days.toInt()) }
        ) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AddTrafficDialog(userName: String, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var gb by remember { mutableStateOf("10") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add traffic to $userName") },
        text = {
            OutlinedTextField(gb, { gb = it }, label = { Text("Extra traffic (GB)") })
        },
        confirmButton = { TextButton(
            enabled = (gb.toDoubleOrNull() ?: 0.0) > 0.0,
            onClick = { onConfirm(gb.toDouble()) }
        ) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SpeedLimitDialog(
    userName: String,
    initialDownKbps: Int,
    initialUpKbps: Int,
    onDismiss: () -> Unit,
    onConfirm: (down: Int, up: Int) -> Unit
) {
    var down by remember { mutableStateOf(if (initialDownKbps > 0) initialDownKbps.toString() else "") }
    var up by remember { mutableStateOf(if (initialUpKbps > 0) initialUpKbps.toString() else "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Speed limit for $userName") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Leave a field empty for unlimited.", fontSize = 12.sp, color = Dz.t3)
            OutlinedTextField(down, { down = it }, label = { Text("Download (KB/s)") })
            OutlinedTextField(up, { up = it }, label = { Text("Upload (KB/s)") })
        } },
        confirmButton = { TextButton(
            enabled = down.toIntOrNull() != null || down.isBlank(),
            onClick = { onConfirm(down.toIntOrNull() ?: 0, up.toIntOrNull() ?: 0) }
        ) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun NoteDialog(userName: String, initialNote: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var note by remember { mutableStateOf(initialNote) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Note for $userName") },
        text = {
            OutlinedTextField(note, { note = it }, label = { Text("Note") }, minLines = 3)
        },
        confirmButton = { TextButton(onClick = { onConfirm(note) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AgentEditorDialog(onDismiss: () -> Unit, onConfirm: (String, String, Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var quota by remember { mutableStateOf("100") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create agent") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Name") })
            OutlinedTextField(password, { password = it }, label = { Text("Strong password") })
            OutlinedTextField(quota, { quota = it }, label = { Text("Quota GB") })
        } },
        confirmButton = { TextButton(
            enabled = name.isNotBlank() && password.length >= 12 && quota.toDoubleOrNull() != null,
            onClick = { onConfirm(name, password, quota.toDouble()) }
        ) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
