package com.v7lthronyx.v7lpanel.vpn

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder

data class ParsedConfig(
    val label: String,
    val rawUri: String,
    val xrayOutboundJson: String,
    val engine: String = "xray",       // "xray" or "singbox"
    val protocol: String = "",          // vmess/vless/trojan/ss/hysteria2/tuic
    val serverAddress: String = "",
    val serverPort: Int = 0,
    val transport: String = "",         // ws/tcp/grpc/quic/xhttp
    val security: String = "",          // tls/reality/none
)

object ConfigParser {

    fun parse(uri: String, label: String = "proxy"): ParsedConfig? {
        return try {
            when {
                uri.startsWith("vmess://")  -> {
                    val json = parseVmess(uri)
                    val obj = JSONObject(json)
                    val vnext = obj.optJSONObject("settings")?.optJSONArray("vnext")?.optJSONObject(0)
                    val ss = obj.optJSONObject("streamSettings")
                    ParsedConfig(label = label, rawUri = uri, xrayOutboundJson = json,
                        engine = "xray", protocol = "vmess",
                        serverAddress = vnext?.optString("address", "") ?: "",
                        serverPort = vnext?.optInt("port", 443) ?: 443,
                        transport = ss?.optString("network", "tcp") ?: "tcp",
                        security = ss?.optString("security", "none") ?: "none")
                }
                uri.startsWith("vless://")  -> {
                    val json = parseVless(uri)
                    val obj = JSONObject(json)
                    val vnext = obj.optJSONObject("settings")?.optJSONArray("vnext")?.optJSONObject(0)
                    val ss = obj.optJSONObject("streamSettings")
                    ParsedConfig(label = label, rawUri = uri, xrayOutboundJson = json,
                        engine = "xray", protocol = "vless",
                        serverAddress = vnext?.optString("address", "") ?: "",
                        serverPort = vnext?.optInt("port", 443) ?: 443,
                        transport = ss?.optString("network", "tcp") ?: "tcp",
                        security = ss?.optString("security", "none") ?: "none")
                }
                uri.startsWith("trojan://") -> {
                    val json = parseTrojan(uri)
                    val obj = JSONObject(json)
                    val server = obj.optJSONObject("settings")?.optJSONArray("servers")?.optJSONObject(0)
                    val ss = obj.optJSONObject("streamSettings")
                    ParsedConfig(label = label, rawUri = uri, xrayOutboundJson = json,
                        engine = "xray", protocol = "trojan",
                        serverAddress = server?.optString("address", "") ?: "",
                        serverPort = server?.optInt("port", 443) ?: 443,
                        transport = ss?.optString("network", "tcp") ?: "tcp",
                        security = ss?.optString("security", "tls") ?: "tls")
                }
                uri.startsWith("ss://")     -> {
                    val json = parseSs(uri)
                    val obj = JSONObject(json)
                    val server = obj.optJSONObject("settings")?.optJSONArray("servers")?.optJSONObject(0)
                    ParsedConfig(label = label, rawUri = uri, xrayOutboundJson = json,
                        engine = "xray", protocol = "shadowsocks",
                        serverAddress = server?.optString("address", "") ?: "",
                        serverPort = server?.optInt("port", 8388) ?: 8388,
                        transport = "tcp", security = "none")
                }
                uri.startsWith("hysteria2://") || uri.startsWith("hy2://") -> {
                    parseHysteria2(uri, label)
                }
                uri.startsWith("hysteria://") -> {
                    parseHysteria1(uri, label)
                }
                uri.startsWith("tuic://") -> {
                    parseTuic(uri, label)
                }
                uri.startsWith("wireguard://") || uri.startsWith("wg://") -> {
                    parseWireguard(uri, label)
                }
                else -> null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ─── VMess ───────────────────────────────────────────────────────────────
    private fun parseVmess(uri: String): String {
        val b64 = uri.removePrefix("vmess://").trimEnd('=').let {
            val pad = (4 - it.length % 4) % 4
            it + "=".repeat(pad)
        }
        val json = JSONObject(String(Base64.decode(b64, Base64.DEFAULT or Base64.NO_WRAP)))
        val host     = json.optString("add")
        val port     = json.optString("port", "443").toIntOrNull() ?: 443
        val uuid     = json.optString("id")
        val alterId  = json.optInt("aid", 0)
        val security = json.optString("scy", "auto").takeIf { it.isNotEmpty() } ?: "auto"
        val net      = json.optString("net", "tcp")
        val path     = json.optString("path", "/")
        val wsHost   = json.optString("host", host)
        val tls      = json.optString("tls", "")
        val sni      = json.optString("sni", wsHost)
        val fp       = json.optString("fp", "chrome").takeIf { it.isNotEmpty() } ?: "chrome"
        val grpcSvc  = json.optString("path", "")
        val headerType = json.optString("type", "none")

        val o = JSONObject()
        o.put("tag", "proxy")
        o.put("protocol", "vmess")
        o.put("settings", JSONObject().put("vnext", JSONArray().put(
            JSONObject()
                .put("address", host)
                .put("port", port)
                .put("users", JSONArray().put(
                    JSONObject()
                        .put("id", uuid)
                        .put("alterId", alterId)
                        .put("security", security)
                ))
        )))
        val ss = buildStreamSettings(net, tls, sni, fp, path, wsHost, grpcSvc, headerType)
        if (ss.length() > 0) o.put("streamSettings", ss)
        return o.toString()
    }

    // ─── VLESS ───────────────────────────────────────────────────────────────
    private fun parseVless(uri: String): String {
        val withoutScheme = uri.removePrefix("vless://")
        val authority = withoutScheme.substringBefore("?").substringBefore("#")
        val query = withoutScheme.substringAfter("?", "").substringBefore("#")
        val uuid = authority.substringBefore("@").takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Missing VLESS user info")
        val endpoint = authority.substringAfter("@", "").takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Missing VLESS host")

        val (host, port) = parseHostAndPort(endpoint, 443)
        val params = parseQuery(query)
        val flow     = params["flow"] ?: ""
        val security = params["security"] ?: "none"
        val sni      = params["sni"] ?: params["peer"] ?: host
        val fp       = params["fp"] ?: "chrome"
        val pbk      = params["pbk"] ?: ""
        val sid      = params["sid"] ?: ""
        val net      = params["type"] ?: "tcp"
        val path     = decode(params["path"] ?: "/")
        val wsHost   = decode(params["host"] ?: host)
        val grpcSvc  = decode(params["serviceName"] ?: params["path"] ?: "")
        val alpn     = decode(params["alpn"] ?: "")
        val xhttpMode = params["mode"] ?: "auto"

        val user = JSONObject().put("id", uuid).put("encryption", "none")
        if (flow.isNotEmpty()) user.put("flow", flow)

        val o = JSONObject()
        o.put("tag", "proxy")
        o.put("protocol", "vless")
        o.put("settings", JSONObject().put("vnext", JSONArray().put(
            JSONObject()
                .put("address", host)
                .put("port", port)
                .put("users", JSONArray().put(user))
        )))
        val ss = buildStreamSettings(net, security, sni, fp, path, wsHost, grpcSvc, "none", alpn, pbk, sid, xhttpMode,
            params["v7lpin"] ?: "")
        if (ss.length() > 0) o.put("streamSettings", ss)
        return o.toString()
    }

    // ─── Trojan ──────────────────────────────────────────────────────────────
    private fun parseTrojan(uri: String): String {
        val withoutScheme = uri.removePrefix("trojan://")
        val password = withoutScheme.substringBefore("@")
        val hostPort = withoutScheme.substringAfter("@")
        val queryFrag = if (hostPort.contains("?")) hostPort.substringAfter("?") else ""
        val hostPortClean = hostPort.substringBefore("?")
        val host = hostPortClean.substringBeforeLast(":")
        val port = hostPortClean.substringAfterLast(":").substringBefore("#").toIntOrNull() ?: 443

        val params   = parseQuery(queryFrag.substringBefore("#"))
        val sni      = params["sni"] ?: params["peer"] ?: host
        val fp       = params["fp"] ?: "chrome"
        val net      = params["type"] ?: "tcp"
        val path     = decode(params["path"] ?: "/")
        val wsHost   = decode(params["host"] ?: host)
        val alpn     = decode(params["alpn"] ?: "")
        val security = params["security"] ?: "tls"
        val xhttpMode = params["mode"] ?: "auto"

        val o = JSONObject()
        o.put("tag", "proxy")
        o.put("protocol", "trojan")
        o.put("settings", JSONObject().put("servers", JSONArray().put(
            JSONObject()
                .put("address", host)
                .put("port", port)
                .put("password", password)
        )))
        val ss = buildStreamSettings(net, security, sni, fp, path, wsHost, "", "none", alpn, "", "", xhttpMode,
            params["v7lpin"] ?: "")
        if (ss.length() > 0) o.put("streamSettings", ss)
        return o.toString()
    }

    // ─── Shadowsocks ─────────────────────────────────────────────────────────
    // Supports the three real-world forms:
    //   1. SIP002 with base64 user info:  ss://base64url(method:password)@host:port
    //   2. SIP002 with plain user info:   ss://method:password@host:port
    //   3. Legacy fully-base64:           ss://base64(method:password@host:port)
    // The V7L panel emits form (1) for Shadowsocks-2022, where the decoded
    // user info is "method:server_key:uuid" — i.e. the password itself contains
    // a colon, so we must split on the FIRST colon only.
    private fun parseSs(uri: String): String {
        val withoutScheme = uri.removePrefix("ss://").substringBefore("#")

        val userInfoRaw: String
        val hostPortRaw: String
        if (withoutScheme.contains("@")) {
            userInfoRaw = withoutScheme.substringBefore("@")
            hostPortRaw = withoutScheme.substringAfter("@")
        } else {
            // Legacy form: the whole authority is base64-encoded.
            val decoded = decodeBase64OrNull(withoutScheme.substringBefore("?")) ?: withoutScheme
            userInfoRaw = decoded.substringBefore("@")
            hostPortRaw = decoded.substringAfter("@", "")
        }

        // Resolve method:password. Prefer a base64 decode of the user info
        // (SIP002 standard); fall back to the raw value when it is already
        // a plain "method:password" string.
        val creds = decodeBase64OrNull(userInfoRaw)?.takeIf { it.contains(":") }
            ?: userInfoRaw
        val method = creds.substringBefore(":")
        val password = creds.substringAfter(":", "")

        val hostPortClean = hostPortRaw.substringBefore("?").substringBefore("/")
        val host = hostPortClean.substringBeforeLast(":")
        val port = hostPortClean.substringAfterLast(":", "").toIntOrNull() ?: 8388

        val o = JSONObject()
        o.put("tag", "proxy")
        o.put("protocol", "shadowsocks")
        o.put("settings", JSONObject().put("servers", JSONArray().put(
            JSONObject()
                .put("address", host)
                .put("port", port)
                .put("method", method)
                .put("password", password)
        )))
        return o.toString()
    }

    // ─── Hysteria2 (sing-box) ──────────────────────────────────────────────────
    private fun parseHysteria2(uri: String, label: String): ParsedConfig {
        val withoutScheme = uri.removePrefix("hysteria2://").removePrefix("hy2://")
        val auth = withoutScheme.substringBefore("@")
        val hostPort = withoutScheme.substringAfter("@").substringBefore("?").substringBefore("#")
        val query = withoutScheme.substringAfter("?", "").substringBefore("#")

        val (host, port) = parseHostAndPort(hostPort, 443)
        val params = parseQuery(query)

        val outbound = JSONObject()
        outbound.put("type", "hysteria2")
        outbound.put("tag", "proxy")
        outbound.put("server", host)
        outbound.put("server_port", port)
        outbound.put("password", auth)

        val tls = JSONObject()
        tls.put("enabled", true)
        tls.put("server_name", params["sni"] ?: host)
        tls.put("insecure", false)
        (params["v7lspki"] ?: "").takeIf { it.isNotBlank() }?.let {
            tls.put("certificate_public_key_sha256", JSONArray().put(it.removePrefix("sha256/")))
        }
        if (params.containsKey("alpn")) {
            tls.put("alpn", JSONArray(params["alpn"]!!.split(",")))
        }
        outbound.put("tls", tls)

        if (params["obfs"] == "salamander") {
            val obfs = JSONObject()
            obfs.put("type", "salamander")
            obfs.put("password", params["obfs-password"] ?: "")
            outbound.put("obfs", obfs)
        }

        val wrapper = JSONObject()
        wrapper.put("_engine", "singbox")
        wrapper.put("outbound", outbound)

        return ParsedConfig(
            label = label, rawUri = uri, xrayOutboundJson = wrapper.toString(),
            engine = "singbox", protocol = "hysteria2",
            serverAddress = host, serverPort = port,
            transport = "quic", security = "tls"
        )
    }

    // ─── TUIC (sing-box) ──────────────────────────────────────────────────────
    private fun parseTuic(uri: String, label: String): ParsedConfig {
        val withoutScheme = uri.removePrefix("tuic://")
        val userInfo = withoutScheme.substringBefore("@")
        val uuid = userInfo.substringBefore(":")
        val password = userInfo.substringAfter(":", uuid)
        val hostPort = withoutScheme.substringAfter("@").substringBefore("?").substringBefore("#")
        val query = withoutScheme.substringAfter("?", "").substringBefore("#")
        val params = parseQuery(query)
        val (host, port) = parseHostAndPort(hostPort, 443)

        val outbound = JSONObject()
        outbound.put("type", "tuic")
        outbound.put("tag", "proxy")
        outbound.put("server", host)
        outbound.put("server_port", port)
        outbound.put("uuid", uuid)
        outbound.put("password", password)
        outbound.put("congestion_control", params["congestion_control"] ?: "bbr")
        outbound.put("udp_relay_mode", params["udp_relay_mode"] ?: "native")

        val tls = JSONObject()
        tls.put("enabled", true)
        tls.put("server_name", params["sni"] ?: host)
        tls.put("insecure", false)
        (params["v7lspki"] ?: "").takeIf { it.isNotBlank() }?.let {
            tls.put("certificate_public_key_sha256", JSONArray().put(it.removePrefix("sha256/")))
        }
        if (params.containsKey("alpn")) {
            tls.put("alpn", JSONArray(params["alpn"]!!.split(",")))
        }
        outbound.put("tls", tls)

        val wrapper = JSONObject()
        wrapper.put("_engine", "singbox")
        wrapper.put("outbound", outbound)

        return ParsedConfig(
            label = label, rawUri = uri, xrayOutboundJson = wrapper.toString(),
            engine = "singbox", protocol = "tuic",
            serverAddress = host, serverPort = port,
            transport = "quic", security = "tls"
        )
    }

    // ─── Hysteria v1 (sing-box) ──────────────────────────────────────────────
    private fun parseHysteria1(uri: String, label: String): ParsedConfig {
        val withoutScheme = uri.removePrefix("hysteria://")
        val hostPort = withoutScheme.substringBefore("?").substringBefore("#")
        val query = withoutScheme.substringAfter("?", "").substringBefore("#")
        val (host, port) = parseHostAndPort(hostPort, 443)
        val params = parseQuery(query)

        val outbound = JSONObject()
        outbound.put("type", "hysteria")
        outbound.put("tag", "proxy")
        outbound.put("server", host)
        outbound.put("server_port", port)
        outbound.put("up_mbps", (params["upmbps"] ?: params["up"] ?: "100").toIntOrNull() ?: 100)
        outbound.put("down_mbps", (params["downmbps"] ?: params["down"] ?: "100").toIntOrNull() ?: 100)

        val authStr = params["auth"] ?: ""
        if (authStr.isNotBlank()) outbound.put("auth_str", authStr)

        val protocol = params["protocol"] ?: "udp"
        if (protocol.isNotBlank()) outbound.put("recv_window_conn", 15728640)

        val tls = JSONObject()
        tls.put("enabled", true)
        tls.put("server_name", params["peer"] ?: params["sni"] ?: host)
        tls.put("insecure", false)
        (params["v7lspki"] ?: "").takeIf { it.isNotBlank() }?.let {
            tls.put("certificate_public_key_sha256", JSONArray().put(it.removePrefix("sha256/")))
        }
        if (params.containsKey("alpn")) {
            tls.put("alpn", JSONArray(params["alpn"]!!.split(",")))
        } else {
            tls.put("alpn", JSONArray().put("h3"))
        }
        outbound.put("tls", tls)

        val obfsType = params["obfsParam"] ?: params["obfs"] ?: ""
        if (obfsType.isNotBlank()) {
            outbound.put("obfs", obfsType)
        }

        val wrapper = JSONObject()
        wrapper.put("_engine", "singbox")
        wrapper.put("outbound", outbound)

        return ParsedConfig(
            label = label, rawUri = uri, xrayOutboundJson = wrapper.toString(),
            engine = "singbox", protocol = "hysteria",
            serverAddress = host, serverPort = port,
            transport = "quic", security = "tls"
        )
    }

    // ─── WireGuard / AmneziaWG (sing-box) ────────────────────────────────────
    // Accepts the common share-URI form used by Hiddify / Streisand / etc:
    //   wireguard://<private_key>@host:port?publickey=..&presharedkey=..
    //              &address=10.0.0.2/32,fd00::2/128&mtu=1420&reserved=0,0,0#label
    // The private key may be percent-encoded; base64 keys keep their '=' padding.
    private fun parseWireguard(uri: String, label: String): ParsedConfig {
        val withoutScheme = uri.removePrefix("wireguard://").removePrefix("wg://")
        val authority = withoutScheme.substringBefore("?").substringBefore("#")
        val query = withoutScheme.substringAfter("?", "").substringBefore("#")
        val privateKey = decode(authority.substringBefore("@"))
            .takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Missing WireGuard private key")
        val endpoint = authority.substringAfter("@", "").takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Missing WireGuard endpoint")
        val (host, port) = parseHostAndPort(endpoint, 51820)
        val params = parseQuery(query)

        // sing-box 1.11+ models WireGuard as an *endpoint* (the legacy
        // `wireguard` outbound is removed in 1.13). Build the endpoint shape.
        val peer = JSONObject()
        peer.put("address", host)
        peer.put("port", port)
        val peerPublic = params["publickey"] ?: params["public_key"] ?: params["peer_public_key"] ?: ""
        if (peerPublic.isNotBlank()) peer.put("public_key", peerPublic)
        val psk = params["presharedkey"] ?: params["pre_shared_key"] ?: ""
        if (psk.isNotBlank()) peer.put("pre_shared_key", psk)
        // Route everything through the tunnel by default.
        val allowed = (params["allowed_ips"] ?: params["allowedips"] ?: "0.0.0.0/0,::/0")
            .split(",").map { it.trim() }.filter { it.isNotBlank() }
        peer.put("allowed_ips", JSONArray(allowed))
        // reserved bytes (e.g. "0,0,0"), used by some WARP-style configs
        params["reserved"]?.split(",")?.mapNotNull { it.trim().toIntOrNull() }
            ?.takeIf { it.size == 3 }
            ?.let { peer.put("reserved", JSONArray(it)) }

        val endpointObj = JSONObject()
        endpointObj.put("type", "wireguard")
        endpointObj.put("tag", "proxy")
        // local interface addresses (comma-separated, IPv4/IPv6 with CIDR)
        val addresses = (params["address"] ?: params["ip"] ?: "10.0.0.2/32")
            .split(",").map { it.trim() }.filter { it.isNotBlank() }
        endpointObj.put("address", JSONArray(addresses))
        endpointObj.put("private_key", privateKey)
        endpointObj.put("peers", JSONArray().put(peer))
        (params["mtu"]?.toIntOrNull())?.let { endpointObj.put("mtu", it) }

        val wrapper = JSONObject()
        wrapper.put("_engine", "singbox")
        wrapper.put("_singbox_kind", "endpoint")
        wrapper.put("outbound", endpointObj)

        return ParsedConfig(
            label = label, rawUri = uri, xrayOutboundJson = wrapper.toString(),
            engine = "singbox", protocol = "wireguard",
            serverAddress = host, serverPort = port,
            transport = "udp", security = "none"
        )
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────
    private fun buildStreamSettings(
        net: String, security: String, sni: String, fp: String,
        path: String, host: String, grpcSvc: String,
        headerType: String = "none", alpn: String = "",
        pbk: String = "", sid: String = "",
        xhttpMode: String = "auto", certPin: String = ""
    ): JSONObject {
        val ss = JSONObject()
        ss.put("network", if (net == "h2") "http" else net)

        // TLS / Reality
        when (security) {
            "tls" -> {
                ss.put("security", "tls")
                val tls = JSONObject().put("serverName", sni).put("allowInsecure", false)
                if (certPin.isNotBlank()) {
                    tls.put("pinnedPeerCertificateChainSha256", JSONArray().put(certPin.removePrefix("sha256/")))
                }
                if (fp.isNotEmpty()) tls.put("fingerprint", fp)
                if (alpn.isNotEmpty()) tls.put("alpn", JSONArray(alpn.split(",")))
                ss.put("tlsSettings", tls)
            }
            "reality" -> {
                ss.put("security", "reality")
                val reality = JSONObject()
                    .put("serverName", sni)
                    .put("fingerprint", fp)
                    .put("publicKey", pbk)
                    .put("shortId", sid)
                ss.put("realitySettings", reality)
            }
            else -> ss.put("security", "none")
        }

        // Transport
        when (net) {
            "ws" -> ss.put("wsSettings", JSONObject()
                .put("path", path)
                .put("headers", JSONObject().put("Host", host)))
            "grpc" -> ss.put("grpcSettings", JSONObject()
                .put("serviceName", grpcSvc))
            "h2" -> ss.put("httpSettings", JSONObject()
                .put("host", JSONArray().put(host))
                .put("path", path))
            "httpupgrade" -> ss.put("httpupgradeSettings", JSONObject()
                .put("host", host)
                .put("path", path))
            "xhttp", "splithttp" -> {
                val xhttpObj = JSONObject()
                    .put("path", path)
                    .put("mode", xhttpMode)
                if (host.isNotBlank()) {
                    xhttpObj.put("host", host)
                    xhttpObj.put("headers", JSONObject().put("Host", host))
                }
                ss.put("xhttpSettings", xhttpObj)
            }
            "tcp" -> if (headerType == "http") {
                ss.put("tcpSettings", JSONObject()
                    .put("header", JSONObject()
                        .put("type", "http")
                        .put("request", JSONObject()
                            .put("headers", JSONObject().put("Host", JSONArray().put(host)))
                            .put("path", JSONArray().put(path)))))
            }
        }
        return ss
    }

    private fun parseQuery(query: String): Map<String, String> =
        query
            .split("&")
            .filter { it.isNotBlank() }
            .associate { entry ->
                val key = entry.substringBefore("=")
                val rawValue = entry.substringAfter("=", "")
                decode(key) to decode(rawValue)
            }

    private fun parseHostAndPort(endpoint: String, defaultPort: Int): Pair<String, Int> {
        if (endpoint.startsWith("[")) {
            val host = endpoint.substringAfter("[").substringBefore("]")
            val port = endpoint.substringAfter("]:", "").toIntOrNull() ?: defaultPort
            return host to port
        }

        val host = endpoint.substringBeforeLast(":", endpoint)
        val port = endpoint.substringAfterLast(":", "").toIntOrNull() ?: defaultPort
        if (host.isBlank()) throw IllegalArgumentException("Missing host")
        return host to port
    }

    private fun decode(s: String): String = try { URLDecoder.decode(s, "UTF-8") } catch (_: Exception) { s }

    /**
     * Decode a base64 string, tolerating both the URL-safe and standard
     * alphabets as well as missing padding. Returns null when the input is
     * not valid base64 (so callers can fall back to the raw value).
     */
    private fun decodeBase64OrNull(input: String): String? {
        val s = input.trim().trimEnd('=')
        if (s.isEmpty()) return null
        val pad = (4 - s.length % 4) % 4
        val padded = s + "=".repeat(pad)
        val flags = Base64.NO_WRAP
        for (alphabet in intArrayOf(Base64.URL_SAFE or flags, flags)) {
            try {
                val bytes = Base64.decode(padded, alphabet)
                if (bytes.isNotEmpty()) return String(bytes, Charsets.UTF_8)
            } catch (_: Exception) { /* try next alphabet */ }
        }
        return null
    }
}
