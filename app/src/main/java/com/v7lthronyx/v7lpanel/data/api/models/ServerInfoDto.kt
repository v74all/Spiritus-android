package com.v7lthronyx.v7lpanel.data.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServerInfoDto(
    val vmess: Boolean = true,
    val vless: Boolean = false,
    val cdn: Boolean = false,
    val trojan: Boolean = false,
    val grpc: Boolean = false,
    val httpupgrade: Boolean = false,
    val ss2022: Boolean = false,
    @SerialName("vless_ws")            val vlessWs: Boolean = false,
    @SerialName("kill_switch")         val killSwitch: Boolean = false,
    @SerialName("fragment_enabled")    val fragmentEnabled: Boolean = false,
    @SerialName("mux_enabled")         val muxEnabled: Boolean = false,
    @SerialName("vmess_port")          val vmessPort: Int = 443,
    @SerialName("vmess_sni")           val vmessSni: String = "",
    @SerialName("vmess_path")          val vmessPath: String = "",
    @SerialName("vless_port")          val vlessPort: Int = 2053,
    @SerialName("vless_sni")           val vlessSni: String = "",
    @SerialName("vless_public_key")    val vlessPublicKey: String = "",
    @SerialName("vless_short_id")      val vlessShortId: String = "",
    @SerialName("vless_ws_port")       val vlessWsPort: Int = 2057,
    @SerialName("vless_ws_path")       val vlessWsPath: String = "/vless-ws",
    @SerialName("cdn_domain")          val cdnDomain: String = "",
    @SerialName("cdn_port")            val cdnPort: Int = 2082,
    @SerialName("cdn_ws_path")         val cdnWsPath: String = "/cdn-ws",
    @SerialName("trojan_port")         val trojanPort: Int = 443,
    @SerialName("grpc_port")           val grpcPort: Int = 443,
    @SerialName("grpc_service")        val grpcService: String = "GunService",
    @SerialName("httpupgrade_port")    val httpupgradePort: Int = 2055,
    @SerialName("httpupgrade_path")    val httpupgradePath: String = "/httpupgrade",
    @SerialName("ss2022_port")         val ss2022Port: Int = 8388,
    @SerialName("ss2022_method")       val ss2022Method: String = "",
    @SerialName("ss2022_key")          val ss2022Key: String = "",
    @SerialName("mux_concurrency")     val muxConcurrency: Int = 8,
    @SerialName("fingerprint")         val fingerprint: String = "chrome",
    @SerialName("prefix")              val prefix: String = ""
)
