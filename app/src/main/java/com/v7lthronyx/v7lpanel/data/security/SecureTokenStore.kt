package com.v7lthronyx.v7lpanel.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class StoredMobileSession(
    val token: String,
    val profileId: Int,
    val role: String,
    val principal: String
)

/** Stores only a short-lived access token; passwords are never persisted. */
class SecureTokenStore(context: Context) {
    private val prefs = context.getSharedPreferences("v7l_secure_session", Context.MODE_PRIVATE)

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
            generateKey()
        }
    }

    fun save(token: String, profileId: Int, role: String, principal: String) {
        require(role == "admin" || role == "agent")
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        prefs.edit()
            .putString("ciphertext", Base64.encodeToString(cipher.doFinal(token.toByteArray()), Base64.NO_WRAP))
            .putString("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putInt("profile_id", profileId)
            .putString("role", role)
            .putString("principal", principal)
            .apply()
    }

    fun load(): StoredMobileSession? = runCatching {
        val encrypted = prefs.getString("ciphertext", null) ?: return null
        val iv = prefs.getString("iv", null) ?: return null
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            key(),
            GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP))
        )
        StoredMobileSession(
            token = String(cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP))),
            profileId = prefs.getInt("profile_id", -1),
            role = prefs.getString("role", "") ?: "",
            principal = prefs.getString("principal", "") ?: ""
        ).takeIf { it.profileId >= 0 && it.role in setOf("admin", "agent") }
    }.getOrElse {
        clear()
        null
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_ALIAS = "v7l_mobile_session_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
