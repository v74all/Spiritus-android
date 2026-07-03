package com.v7lthronyx.v7lpanel.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "v7l_settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        val KEY_LAST_SERVER_ID   = intPreferencesKey("last_server_id")
        val KEY_LAST_SERVER_URL  = stringPreferencesKey("last_server_url")
        val KEY_SESSION_ROLE     = stringPreferencesKey("session_role")
        val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val KEY_AUTO_REFRESH_SEC = intPreferencesKey("auto_refresh_sec")
        val KEY_LANGUAGE         = stringPreferencesKey("language")
        private val KEY_SAVED_ADMIN_PASS = stringPreferencesKey("saved_admin_password")
        private val KEY_SAVED_AGENT_PASS = stringPreferencesKey("saved_agent_password")
        val KEY_MUX_ENABLED      = booleanPreferencesKey("mux_enabled")
        val KEY_MUX_CONCURRENCY  = intPreferencesKey("mux_concurrency")
        val KEY_DNS_SERVER       = stringPreferencesKey("dns_server")
        val KEY_PROXY_CHAIN_URI  = stringPreferencesKey("proxy_chain_uri")
        val KEY_ENABLE_IPV6      = booleanPreferencesKey("enable_ipv6")
        val KEY_DOMAIN_STRATEGY  = stringPreferencesKey("domain_strategy")

        // ── Kill Switch ──
        val KEY_KILL_SWITCH      = booleanPreferencesKey("kill_switch_enabled")

        // ── Split Tunneling ──
        val KEY_SPLIT_TUNNEL_MODE = stringPreferencesKey("split_tunnel_mode") // "off" | "allowlist" | "disallowlist"
        val KEY_SPLIT_TUNNEL_APPS = stringSetPreferencesKey("split_tunnel_apps")

        // ── Auto-Connect ──
        val KEY_AUTO_CONNECT     = booleanPreferencesKey("auto_connect")
        val KEY_AUTO_CONNECT_URI = stringPreferencesKey("auto_connect_config_uri")
        val KEY_AUTO_CONNECT_LABEL = stringPreferencesKey("auto_connect_config_label")

        // ── Fragment ──
        val KEY_FRAGMENT_ENABLED  = booleanPreferencesKey("fragment_enabled")
        val KEY_FRAGMENT_PACKETS  = stringPreferencesKey("fragment_packets")
        val KEY_FRAGMENT_LENGTH   = stringPreferencesKey("fragment_length")
        val KEY_FRAGMENT_INTERVAL = stringPreferencesKey("fragment_interval")

        // ── Noise ──
        val KEY_NOISE_ENABLED = booleanPreferencesKey("noise_enabled")
        val KEY_NOISE_PACKET  = stringPreferencesKey("noise_packet")
        val KEY_NOISE_DELAY   = stringPreferencesKey("noise_delay")

        // ── TLS Fingerprint ──
        val KEY_FINGERPRINT = stringPreferencesKey("tls_fingerprint")

        // ── Appearance ──
        val KEY_APPEARANCE = stringPreferencesKey("appearance") // "dark" | "amoled" | "system"
        val KEY_ACCENT     = stringPreferencesKey("accent_color") // hex, e.g. "#34E5A4"

        // ── MTU ──
        val KEY_MTU = intPreferencesKey("mtu_size")

        // ── Session (persisted for cold start) ──
        val KEY_SESSION_UUID = stringPreferencesKey("session_uuid")
        val KEY_SESSION_SERVER_URL = stringPreferencesKey("session_server_url")
        val KEY_SESSION_TLS_PIN = stringPreferencesKey("session_tls_pin")
    }

    // ── Existing ──
    val lastServerId: Flow<Int> = context.dataStore.data.map { it[KEY_LAST_SERVER_ID] ?: -1 }
    val lastServerUrl: Flow<String> = context.dataStore.data.map { it[KEY_LAST_SERVER_URL] ?: "" }
    val sessionRole: Flow<String> = context.dataStore.data.map { it[KEY_SESSION_ROLE] ?: "" }
    val biometricEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_BIOMETRIC_ENABLED] ?: false }
    val autoRefreshSec: Flow<Int> = context.dataStore.data.map { it[KEY_AUTO_REFRESH_SEC] ?: 30 }
    val language: Flow<String> = context.dataStore.data.map { it[KEY_LANGUAGE] ?: "en" }
    val muxEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_MUX_ENABLED] ?: true }
    val muxConcurrency: Flow<Int> = context.dataStore.data.map { it[KEY_MUX_CONCURRENCY] ?: 8 }
    val dnsServer: Flow<String> = context.dataStore.data.map { it[KEY_DNS_SERVER] ?: "8.8.8.8" }
    val proxyChainUri: Flow<String> = context.dataStore.data.map { it[KEY_PROXY_CHAIN_URI] ?: "" }
    val enableIpv6: Flow<Boolean> = context.dataStore.data.map { it[KEY_ENABLE_IPV6] ?: false }
    val domainStrategy: Flow<String> = context.dataStore.data.map { it[KEY_DOMAIN_STRATEGY] ?: "AsIs" }

    // ── Kill Switch ──
    val killSwitchEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_KILL_SWITCH] ?: false }

    // ── Split Tunneling ──
    val splitTunnelMode: Flow<String> = context.dataStore.data.map { it[KEY_SPLIT_TUNNEL_MODE] ?: "off" }
    val splitTunnelApps: Flow<Set<String>> = context.dataStore.data.map { it[KEY_SPLIT_TUNNEL_APPS] ?: emptySet() }

    // ── Auto-Connect ──
    val autoConnect: Flow<Boolean> = context.dataStore.data.map { it[KEY_AUTO_CONNECT] ?: false }
    val autoConnectUri: Flow<String> = context.dataStore.data.map { it[KEY_AUTO_CONNECT_URI] ?: "" }
    val autoConnectLabel: Flow<String> = context.dataStore.data.map { it[KEY_AUTO_CONNECT_LABEL] ?: "" }

    // ── Fragment ──
    val fragmentEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_FRAGMENT_ENABLED] ?: false }
    val fragmentPackets: Flow<String> = context.dataStore.data.map { it[KEY_FRAGMENT_PACKETS] ?: "tlshello" }
    val fragmentLength: Flow<String> = context.dataStore.data.map { it[KEY_FRAGMENT_LENGTH] ?: "100-200" }
    val fragmentInterval: Flow<String> = context.dataStore.data.map { it[KEY_FRAGMENT_INTERVAL] ?: "10-20" }

    // ── Noise ──
    val noiseEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_NOISE_ENABLED] ?: false }
    val noisePacket: Flow<String> = context.dataStore.data.map { it[KEY_NOISE_PACKET] ?: "rand:50-100" }
    val noiseDelay: Flow<String> = context.dataStore.data.map { it[KEY_NOISE_DELAY] ?: "10-20" }

    // ── TLS Fingerprint ──
    val tlsFingerprint: Flow<String> = context.dataStore.data.map { it[KEY_FINGERPRINT] ?: "chrome" }

    // ── Appearance ──
    val appearance: Flow<String> = context.dataStore.data.map { it[KEY_APPEARANCE] ?: "dark" }
    val accentColor: Flow<String> = context.dataStore.data.map { it[KEY_ACCENT] ?: "#34E5A4" }

    // ── MTU ──
    val mtuSize: Flow<Int> = context.dataStore.data.map { it[KEY_MTU] ?: 0 } // 0 = auto (1500)

    // ── Session (persisted for cold start) ──
    val sessionUuid: Flow<String> = context.dataStore.data.map { it[KEY_SESSION_UUID] ?: "" }
    val sessionServerUrl: Flow<String> = context.dataStore.data.map { it[KEY_SESSION_SERVER_URL] ?: "" }
    val sessionTlsPin: Flow<String> = context.dataStore.data.map { it[KEY_SESSION_TLS_PIN] ?: "" }

    // ── Setters (existing) ──
    suspend fun setLastServerId(id: Int) { context.dataStore.edit { it[KEY_LAST_SERVER_ID] = id } }
    suspend fun setLastServerUrl(url: String) { context.dataStore.edit { it[KEY_LAST_SERVER_URL] = url } }
    suspend fun setSessionRole(role: String) { context.dataStore.edit { it[KEY_SESSION_ROLE] = role } }
    suspend fun setBiometricEnabled(enabled: Boolean) { context.dataStore.edit { it[KEY_BIOMETRIC_ENABLED] = enabled } }
    suspend fun setAutoRefreshSec(sec: Int) { context.dataStore.edit { it[KEY_AUTO_REFRESH_SEC] = sec } }
    suspend fun setLanguage(lang: String) { context.dataStore.edit { it[KEY_LANGUAGE] = lang } }
    suspend fun setMuxEnabled(b: Boolean) { context.dataStore.edit { it[KEY_MUX_ENABLED] = b } }
    suspend fun setMuxConcurrency(i: Int) { context.dataStore.edit { it[KEY_MUX_CONCURRENCY] = i } }
    suspend fun setDnsServer(s: String) { context.dataStore.edit { it[KEY_DNS_SERVER] = s } }
    suspend fun setProxyChainUri(s: String) { context.dataStore.edit { it[KEY_PROXY_CHAIN_URI] = s } }
    suspend fun setEnableIpv6(b: Boolean) { context.dataStore.edit { it[KEY_ENABLE_IPV6] = b } }
    suspend fun setDomainStrategy(s: String) { context.dataStore.edit { it[KEY_DOMAIN_STRATEGY] = s } }

    // ── New setters ──
    suspend fun setKillSwitch(b: Boolean) { context.dataStore.edit { it[KEY_KILL_SWITCH] = b } }
    suspend fun setSplitTunnelMode(s: String) { context.dataStore.edit { it[KEY_SPLIT_TUNNEL_MODE] = s } }
    suspend fun setSplitTunnelApps(apps: Set<String>) { context.dataStore.edit { it[KEY_SPLIT_TUNNEL_APPS] = apps } }
    suspend fun setAutoConnect(b: Boolean) { context.dataStore.edit { it[KEY_AUTO_CONNECT] = b } }
    suspend fun setAutoConnectUri(s: String) { context.dataStore.edit { it[KEY_AUTO_CONNECT_URI] = s } }
    suspend fun setAutoConnectLabel(s: String) { context.dataStore.edit { it[KEY_AUTO_CONNECT_LABEL] = s } }
    suspend fun setFragmentEnabled(b: Boolean) { context.dataStore.edit { it[KEY_FRAGMENT_ENABLED] = b } }
    suspend fun setFragmentPackets(s: String) { context.dataStore.edit { it[KEY_FRAGMENT_PACKETS] = s } }
    suspend fun setFragmentLength(s: String) { context.dataStore.edit { it[KEY_FRAGMENT_LENGTH] = s } }
    suspend fun setFragmentInterval(s: String) { context.dataStore.edit { it[KEY_FRAGMENT_INTERVAL] = s } }
    suspend fun setNoiseEnabled(b: Boolean) { context.dataStore.edit { it[KEY_NOISE_ENABLED] = b } }
    suspend fun setNoisePacket(s: String) { context.dataStore.edit { it[KEY_NOISE_PACKET] = s } }
    suspend fun setNoiseDelay(s: String) { context.dataStore.edit { it[KEY_NOISE_DELAY] = s } }
    suspend fun setTlsFingerprint(s: String) { context.dataStore.edit { it[KEY_FINGERPRINT] = s } }
    suspend fun setAppearance(s: String) { context.dataStore.edit { it[KEY_APPEARANCE] = s } }
    suspend fun setAccentColor(hex: String) { context.dataStore.edit { it[KEY_ACCENT] = hex } }
    suspend fun setMtuSize(i: Int) { context.dataStore.edit { it[KEY_MTU] = i } }
    suspend fun setSessionUuid(s: String) { context.dataStore.edit { it[KEY_SESSION_UUID] = s } }
    suspend fun setSessionServerUrl(s: String) { context.dataStore.edit { it[KEY_SESSION_SERVER_URL] = s } }
    suspend fun setSessionTlsPin(s: String) { context.dataStore.edit { it[KEY_SESSION_TLS_PIN] = s } }

    suspend fun clearSession() {
        context.dataStore.edit {
            it.remove(KEY_SESSION_ROLE)
            it.remove(KEY_LAST_SERVER_ID)
            it.remove(KEY_SESSION_UUID)
            it.remove(KEY_SESSION_SERVER_URL)
            it.remove(KEY_SESSION_TLS_PIN)
        }
    }

    suspend fun purgeLegacyPasswords() {
        context.dataStore.edit {
            it.remove(KEY_SAVED_ADMIN_PASS)
            it.remove(KEY_SAVED_AGENT_PASS)
        }
    }
}
