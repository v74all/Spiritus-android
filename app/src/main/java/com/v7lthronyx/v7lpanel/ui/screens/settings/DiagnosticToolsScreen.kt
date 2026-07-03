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
import com.v7lthronyx.v7lpanel.ui.theme.*
import com.v7lthronyx.v7lpanel.util.PingTester
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun DiagnosticToolsScreen(
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var publicIp by remember { mutableStateOf<String?>(null) }
    var dnsResult by remember { mutableStateOf<String?>(null) }
    var latencyResult by remember { mutableStateOf<String?>(null) }
    var isRunning by remember { mutableStateOf(false) }
    var connectionLog by remember { mutableStateOf("") }

    com.v7lthronyx.v7lpanel.ui.components.AuroraBackground(Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top bar
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Back", tint = V7LColors.t0)
            }
            Text(
                "Diagnostic Tools",
                fontFamily = JetBrainsMono,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = V7LColors.t0
            )
        }

        Spacer(Modifier.height(16.dp))

        // Run all tests button
        Button(
            onClick = {
                scope.launch {
                    isRunning = true
                    connectionLog = ""

                    // IP Check
                    connectionLog += "▸ Checking public IP...\n"
                    publicIp = withContext(Dispatchers.IO) {
                        try {
                            val conn = URL("https://api.ipify.org").openConnection() as HttpURLConnection
                            conn.connectTimeout = 5000
                            conn.readTimeout = 5000
                            BufferedReader(InputStreamReader(conn.inputStream)).readLine()
                        } catch (e: Exception) { "Error: ${e.message}" }
                    }
                    connectionLog += "  IP: $publicIp\n\n"

                    // DNS Leak Test
                    connectionLog += "▸ Testing DNS...\n"
                    dnsResult = withContext(Dispatchers.IO) {
                        try {
                            val addrs = java.net.InetAddress.getAllByName("whoami.akamai.net")
                            addrs.joinToString(", ") { it.hostAddress ?: "?" }
                        } catch (e: Exception) { "Error: ${e.message}" }
                    }
                    connectionLog += "  DNS: $dnsResult\n\n"

                    // Latency
                    connectionLog += "▸ Testing latency...\n"
                    val ping8 = PingTester.tcpPing("8.8.8.8", 443)
                    val ping1 = PingTester.tcpPing("1.1.1.1", 443)
                    latencyResult = "8.8.8.8: ${if (ping8 > 0) "${ping8}ms" else "timeout"}\n1.1.1.1: ${if (ping1 > 0) "${ping1}ms" else "timeout"}"
                    connectionLog += "  $latencyResult\n\n"

                    connectionLog += "✓ All tests completed.\n"
                    isRunning = false
                }
            },
            enabled = !isRunning,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = LocalAccent.current,
                contentColor = V7LColors.bg0
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = V7LColors.bg0
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(if (isRunning) "Running..." else "Run All Tests", fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(16.dp))

        // Results
        DiagnosticCard(
            icon = Icons.Filled.Language,
            title = "Public IP",
            result = publicIp
        )

        DiagnosticCard(
            icon = Icons.Filled.Dns,
            title = "DNS Test",
            result = dnsResult
        )

        DiagnosticCard(
            icon = Icons.Filled.Speed,
            title = "Latency",
            result = latencyResult
        )

        Spacer(Modifier.height(16.dp))

        // Connection log
        if (connectionLog.isNotBlank()) {
            Text("Connection Log", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = V7LColors.t2)
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = V7LColors.bg2),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = connectionLog,
                    fontFamily = FiraCode,
                    fontSize = 11.sp,
                    color = V7LColors.t1,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Xray log viewer
        Spacer(Modifier.height(16.dp))
        var xrayLog by remember { mutableStateOf<String?>(null) }
        OutlinedButton(
            onClick = {
                scope.launch {
                    xrayLog = withContext(Dispatchers.IO) {
                        try {
                            val f = java.io.File(
                                android.os.Environment.getDataDirectory(),
                                "data/com.v7lthronyx.v7lpanel/code_cache/xray.log"
                            )
                            if (f.exists()) f.readText().takeLast(2000) else "No log file found"
                        } catch (e: Exception) { "Error: ${e.message}" }
                    }
                }
            },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = LocalAccent.current)
        ) {
            Icon(Icons.Filled.Article, null, Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("View Xray Log", fontSize = 12.sp)
        }

        xrayLog?.let { log ->
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = V7LColors.bg2),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = log,
                    fontFamily = FiraCode,
                    fontSize = 10.sp,
                    color = V7LColors.t2,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(Modifier.height(80.dp))
    }
    }
}

@Composable
private fun DiagnosticCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    result: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = V7LColors.bg2),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = LocalAccent.current, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = V7LColors.t0)
                Text(
                    result ?: "—",
                    fontSize = 12.sp,
                    fontFamily = FiraCode,
                    color = if (result != null) LocalAccent.current else V7LColors.t3
                )
            }
        }
    }
}
