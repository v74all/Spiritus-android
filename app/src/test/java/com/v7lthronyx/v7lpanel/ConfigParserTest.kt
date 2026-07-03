package com.v7lthronyx.v7lpanel

import com.v7lthronyx.v7lpanel.vpn.ConfigParser
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConfigParserTest {
    @Test
    fun testParseVlessWithoutQuery() {
        val uri = "vless://b831b050-0000-0000-0000-000000000000@google.com:443"
        val parsed = ConfigParser.parse(uri, "test")
        assertNotNull(parsed)
    }

    @Test
    fun testParseVlessWithQueryAndFragment() {
        val uri = "vless://b831b050-0000-0000-0000-000000000000@example.com:8443?type=ws&security=tls&host=cdn.example.com&path=%2Fws#edge"
        val parsed = ConfigParser.parse(uri, "test")

        assertNotNull(parsed)
        assertTrue(parsed!!.xrayOutboundJson.contains("\"protocol\":\"vless\""))
        assertTrue(parsed.xrayOutboundJson.contains("\"network\":\"ws\""))
        assertTrue(parsed.xrayOutboundJson.contains("cdn.example.com"))
    }

    @Test
    fun testParseRejectsInvalidConfig() {
        assertNull(ConfigParser.parse("vless://@missing-host"))
    }
}
