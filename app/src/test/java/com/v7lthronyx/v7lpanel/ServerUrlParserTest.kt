package com.v7lthronyx.v7lpanel

import com.v7lthronyx.v7lpanel.ui.screens.login.parseServerUrl
import org.junit.Assert.*
import org.junit.Test
import java.net.URLEncoder

class ServerUrlParserTest {
    private val uuid = "123e4567-e89b-12d3-a456-426614174000"
    private val pin = "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="

    @Test fun importUriPreservesPortRoleUuidAndPin() {
        val server = URLEncoder.encode("https://panel.example.com:38471", "UTF-8")
        val encodedPin = URLEncoder.encode(pin, "UTF-8")
        val parsed = parseServerUrl("spiritus://import?server=$server&role=subscriber&uuid=$uuid&pin=$encodedPin")
        assertNotNull(parsed)
        assertEquals("https://panel.example.com:38471", parsed!!.baseUrl)
        assertEquals("subscriber", parsed.role)
        assertEquals(uuid, parsed.uuid)
        assertEquals(pin, parsed.tlsPinSha256)
    }

    @Test fun legacyV7lImportUriStillWorks() {
        val server = URLEncoder.encode("https://panel.example.com:38471", "UTF-8")
        val parsed = parseServerUrl("v7l://import?server=$server&role=subscriber&uuid=$uuid")
        assertNotNull(parsed)
        assertEquals("https://panel.example.com:38471", parsed!!.baseUrl)
        assertEquals(uuid, parsed.uuid)
    }

    @Test fun agentAndAdminRolesAreDetected() {
        assertEquals("agent", parseServerUrl("https://panel.example.com:38471/agent")?.role)
        assertEquals("admin", parseServerUrl("https://panel.example.com:38471")?.role)
    }

    @Test fun remoteCleartextAndRawVpnLinksAreRejected() {
        assertNull(parseServerUrl("http://panel.example.com:38471"))
        assertNull(parseServerUrl("vless://$uuid@example.com:443?security=tls"))
    }
}
