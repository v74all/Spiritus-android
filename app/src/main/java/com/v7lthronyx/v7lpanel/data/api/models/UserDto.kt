package com.v7lthronyx.v7lpanel.data.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val name: String = "",
    val uuid: String = "",
    /** Bytes from subscription-userinfo upload= (not always in JSON APIs) */
    @SerialName("upload_bytes")          val uploadBytes: Long = 0L,
    @SerialName("download_bytes")        val downloadBytes: Long = 0L,
    @SerialName("support_url")           val supportUrl: String = "",
    @SerialName("traffic_limit")         val trafficLimitGb: Double = 0.0,
    @SerialName("traffic_used")          val trafficUsedGb: Double = 0.0,
    @SerialName("traffic_percent")       val trafficPercent: Double = 0.0,
    @SerialName("traffic_limit_bytes")   val trafficLimitBytes: Long = 0L,
    @SerialName("traffic_used_bytes")    val trafficUsedBytes: Long = 0L,
    @SerialName("expire_at")             val expireAt: String = "",
    @SerialName("days_left")             val daysLeft: Int = 0,
    val active: Boolean = true,
    @SerialName("created_at")            val createdAt: String = "",
    @SerialName("online_ips")            val onlineIps: Int = 0,
    @SerialName("agent_id")              val agentId: Int? = null,
    @SerialName("live_up")               val liveUp: Long = 0L,
    @SerialName("live_down")             val liveDown: Long = 0L,
    val note: String = "",
    @SerialName("speed_limit_up")        val speedLimitUp: Int = 0,
    @SerialName("speed_limit_down")      val speedLimitDown: Int = 0,
    // Individual protocol config links returned by /api/users
    val vmess: String = "",
    val vless: String = "",
    val trojan: String = "",
    val grpc: String = "",
    val httpupgrade: String = "",
    val ss2022: String = "",
    @SerialName("vless_ws")              val vlessWs: String = "",
    val cdn: String = "",
    // sub-info endpoint may return links as a top-level object
    val links: Map<String, String> = emptyMap(),
    // sub-info also returns these directly
    @SerialName("traffic_limit_gb")      val trafficLimitGbAlt: Double = 0.0,
    @SerialName("traffic_used_gb")       val trafficUsedGbAlt: Double = 0.0
) {
    // Normalise: /api/users uses "traffic_limit"/"traffic_used",
    // /sub-info uses "traffic_limit_gb"/"traffic_used_gb"; some panels only send *_bytes
    val limitGb: Double get() = when {
        trafficLimitGb != 0.0     -> trafficLimitGb
        trafficLimitGbAlt != 0.0 -> trafficLimitGbAlt
        trafficLimitBytes > 0L  -> trafficLimitBytes / BYTES_PER_GB
        else                      -> 0.0
    }
    val usedGb: Double get() = when {
        trafficUsedGb != 0.0      -> trafficUsedGb
        trafficUsedGbAlt != 0.0   -> trafficUsedGbAlt
        trafficUsedBytes > 0L     -> trafficUsedBytes / BYTES_PER_GB
        uploadBytes + downloadBytes > 0L -> (uploadBytes + downloadBytes) / BYTES_PER_GB
        else                      -> 0.0
    }

    /** Aggregate individual protocol fields + links map into one map.
     *  Only entries whose value is a recognised VPN URI are included. */
    val linksMap: Map<String, String> get() {
        val m = LinkedHashMap<String, String>()
        fun add(key: String, uri: String) {
            if (uri.isBlank() || !vpnSchemes.any { uri.trimStart().startsWith(it, ignoreCase = true) }) return
            val frag = uri.substringAfterLast("#", "").trim()
            val label = if (frag.isNotBlank()) {
                try { java.net.URLDecoder.decode(frag, "UTF-8") } catch (_: Exception) { frag }
            } else key
            m[label] = uri.trim()
        }
        add("VMess",      vmess)
        add("VLESS",      vless)
        add("Trojan",     trojan)
        add("gRPC",       grpc)
        add("HTTPUpgrade",httpupgrade)
        add("SS2022",     ss2022)
        add("VLESS-WS",   vlessWs)
        add("CDN",        cdn)
        links.forEach { (k, v) -> add(k, v) }
        return m
    }

    companion object {
        private const val BYTES_PER_GB: Double = 1024.0 * 1024.0 * 1024.0
        private val vpnSchemes = listOf(
            "vless://", "vmess://", "trojan://", "ss://",
            "hy2://", "hysteria2://", "hysteria://", "tuic://",
            "wireguard://", "wg://"
        )
    }
}
