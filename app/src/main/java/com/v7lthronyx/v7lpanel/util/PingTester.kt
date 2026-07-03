package com.v7lthronyx.v7lpanel.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

object PingTester {
    suspend fun tcpPing(host: String, port: Int): Long = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            Socket().use {
                it.connect(InetSocketAddress(host, port), 2500)
            }
            System.currentTimeMillis() - start
        } catch (e: Exception) {
            -1L
        }
    }

    fun extractHostPort(uri: String): Pair<String, Int>? {
        try {
            // VMess: vmess://BASE64_JSON — decode to extract "add" and "port"
            if (uri.lowercase().startsWith("vmess://")) {
                try {
                    val b64 = uri.substringAfter("://").substringBefore("?").substringBefore("#")
                    val padded = b64.trimEnd('=').let { it + "=".repeat((4 - it.length % 4) % 4) }
                    val json = org.json.JSONObject(
                        String(android.util.Base64.decode(padded, android.util.Base64.DEFAULT or android.util.Base64.NO_WRAP))
                    )
                    val host = json.optString("add", "").trim()
                    val port = json.optString("port", "443").trim().toIntOrNull() ?: 443
                    if (host.isNotBlank() && port > 0) {
                        return host to port
                    }
                } catch (_: Exception) {}
                return null
            }

            // Other protocols: scheme://user@host:port?...
            val withoutScheme = uri.substringAfter("://")
            val hostPortSection = if (withoutScheme.contains("@")) withoutScheme.substringAfter("@") else withoutScheme
            val clean = hostPortSection.substringBefore("?").substringBefore("#").substringBefore("/")
            
            val host = if (clean.contains("[")) {
                clean.substringAfter("[").substringBefore("]")
            } else {
                clean.substringBeforeLast(":")
            }
            
            val portStr = clean.substringAfterLast(":", "")
            val port = portStr.toIntOrNull() ?: 443
            if (host.isNotBlank() && port > 0) {
                return host to port
            }
        } catch (e: Exception) {}
        return null
    }
}
