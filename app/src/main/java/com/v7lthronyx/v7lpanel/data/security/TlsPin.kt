package com.v7lthronyx.v7lpanel.data.security

import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.util.Base64

object TlsPin {
    private val format = Regex("^sha256/[A-Za-z0-9+/]{43}=$")

    fun normalize(value: String?): String? = value?.trim()?.takeIf(format::matches)

    fun fromDer(encoded: ByteArray): String =
        "sha256/" + Base64.getEncoder().encodeToString(
            MessageDigest.getInstance("SHA-256").digest(encoded)
        )

    fun matches(certificate: X509Certificate, expected: String): Boolean {
        return matchesDer(certificate.encoded, expected)
    }

    fun matchesDer(encoded: ByteArray, expected: String): Boolean {
        val actual = fromDer(encoded)
        return MessageDigest.isEqual(actual.toByteArray(), expected.toByteArray())
    }
}
