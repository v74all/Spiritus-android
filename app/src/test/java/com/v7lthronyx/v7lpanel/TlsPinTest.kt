package com.v7lthronyx.v7lpanel

import com.v7lthronyx.v7lpanel.data.security.TlsPin
import org.junit.Assert.*
import org.junit.Test

class TlsPinTest {
    @Test fun digestIsStableAndStrictlyFormatted() {
        val der = "certificate".toByteArray()
        val pin = TlsPin.fromDer(der)
        assertTrue(pin.startsWith("sha256/"))
        assertEquals(pin, TlsPin.normalize(pin))
        assertNull(TlsPin.normalize("sha256/not-base64"))
        assertTrue(TlsPin.matchesDer(der, pin))
        assertFalse(TlsPin.matchesDer("different".toByteArray(), pin))
    }
}
