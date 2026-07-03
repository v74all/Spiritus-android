package com.v7lthronyx.v7lpanel

import com.v7lthronyx.v7lpanel.util.SafeLog
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeLogTest {
    @Test
    fun redactMasksSensitiveNetworkAndSubscriptionValues() {
        val message = """
            url=https://panel.example.com:8443/sub/123e4567-e89b-12d3-a456-426614174000
            uuid=123e4567-e89b-12d3-a456-426614174000
            pin=sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=
            auth=Bearer abc.def.ghi
            config=vless://123e4567-e89b-12d3-a456-426614174000@example.com:443?security=tls#edge
        """.trimIndent()

        val redacted = SafeLog.redact(message)

        assertTrue(redacted.contains("https://<redacted>"))
        assertTrue(redacted.contains("<uuid>"))
        assertTrue(redacted.contains("sha256/<redacted>"))
        assertTrue(redacted.contains("Bearer <redacted>"))
        assertTrue(redacted.contains("vless://<redacted>"))
        assertFalse(redacted.contains("panel.example.com"))
        assertFalse(redacted.contains("123e4567-e89b-12d3-a456-426614174000"))
        assertFalse(redacted.contains("abc.def.ghi"))
    }
}
