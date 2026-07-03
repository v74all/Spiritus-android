package com.v7lthronyx.v7lpanel.util

import android.util.Log
import com.v7lthronyx.v7lpanel.BuildConfig

object SafeLog {
    private val urlRegex = Regex("""https?://[^\s'"]+""")
    private val uuidRegex = Regex("""\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\b""")
    private val pinRegex = Regex("""sha256/[A-Za-z0-9+/]{43}=""")
    private val bearerRegex = Regex("""(?i)\bBearer\s+[A-Za-z0-9._~+/=-]+""")
    private val vpnUriRegex = Regex("""(?i)\b(vless|vmess|trojan|ss|tuic|hy2|hysteria2|wireguard)://[^\s'"]+""")

    fun redact(message: String): String = message
        .replace(vpnUriRegex) { "${it.groupValues[1].lowercase()}://<redacted>" }
        .replace(urlRegex, "https://<redacted>")
        .replace(uuidRegex, "<uuid>")
        .replace(pinRegex, "sha256/<redacted>")
        .replace(bearerRegex, "Bearer <redacted>")

    fun throwableSummary(throwable: Throwable): String {
        val raw = throwable.message?.take(160).orEmpty()
        return "${throwable::class.java.simpleName}: ${redact(raw).ifBlank { "no message" }}"
    }

    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.d(tag, redact(message))
    }

    fun w(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.w(tag, redact(message))
    }

    fun e(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.e(tag, redact(message))
    }
}
