package com.v7lthronyx.v7lpanel.ui.screens.subscription

import android.content.Context
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.v7lthronyx.v7lpanel.data.api.models.UserDto
import com.v7lthronyx.v7lpanel.data.db.entities.ManualConfig
import com.v7lthronyx.v7lpanel.data.local.ManualConfigDao
import com.v7lthronyx.v7lpanel.data.repository.SubscriptionRepository
import com.v7lthronyx.v7lpanel.util.SafeLog
import com.v7lthronyx.v7lpanel.vpn.ConfigParser
import com.v7lthronyx.v7lpanel.vpn.VpnManager
import com.v7lthronyx.v7lpanel.vpn.VpnStatus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Backs [com.v7lthronyx.v7lpanel.ui.screens.profile.ProfileScreen] for
 * subscriber sessions: loads the user's traffic/expiry metadata (and, as a
 * byproduct, the raw config list) from a single /sub-api round-trip.
 */
data class SubUiState(
    val user: UserDto?                   = null,
    val profileMetadataMissing: Boolean  = false,
    val configLinks: Map<String, String> = emptyMap(),
    val manualConfigs: List<ManualConfig> = emptyList(),
    val subUrl: String                   = "",
    val isLoading: Boolean               = false,
    val error: String?                   = null,
    val vpnStatus: VpnStatus             = VpnStatus.DISCONNECTED,
    val connectedLabel: String?          = null,
    val selectedLabel: String?           = null,
    val selectedUri: String?             = null,
    val rxSpeed: Long                    = 0L,
    val txSpeed: Long                    = 0L
)

class SubViewModel(
    private val repo: SubscriptionRepository,
    private val uuid: String,
    private val serverUrl: String,
    private val manualDao: ManualConfigDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubUiState())
    val uiState: StateFlow<SubUiState> = _uiState.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            manualDao.getAll().collect { list ->
                _uiState.update { it.copy(manualConfigs = list) }
            }
        }
        viewModelScope.launch {
            VpnManager.status.collect { s ->
                _uiState.update { it.copy(vpnStatus = s) }
            }
        }
        viewModelScope.launch {
            VpnManager.connectedLabel.collect { label ->
                _uiState.update { it.copy(connectedLabel = label) }
            }
        }
        viewModelScope.launch {
            VpnManager.errorMsg.collect { msg ->
                if (msg != null) _uiState.update { it.copy(error = msg) }
            }
        }
        viewModelScope.launch {
            VpnManager.rxSpeed.collect { rx ->
                _uiState.update { it.copy(rxSpeed = rx) }
            }
        }
        viewModelScope.launch {
            VpnManager.txSpeed.collect { tx ->
                _uiState.update { it.copy(txSpeed = tx) }
            }
        }
    }

    /** Select a config for the big button */
    fun selectConfig(label: String, uri: String) {
        _uiState.update { it.copy(selectedLabel = label, selectedUri = uri) }
    }

    /** Import one or more raw URIs (one per line, already validated). */
    fun importConfigs(uris: List<Pair<String, String>>) { // label to uri
        viewModelScope.launch {
            uris.forEach { (label, uri) ->
                manualDao.insert(ManualConfig(label = label, uri = uri))
            }
        }
    }

    fun deleteManualConfig(config: ManualConfig) {
        viewModelScope.launch { manualDao.delete(config) }
    }

    fun connect(context: Context, label: String, uri: String) {
        val config = ConfigParser.parse(uri, label) ?: run {
            _uiState.update { it.copy(error = "Cannot parse config: $label") }
            return
        }
        VpnManager.connect(context, config, uri)
    }

    fun disconnect(context: Context) {
        VpnManager.disconnect(context)
    }

    fun load() {
        viewModelScope.launch {
            SafeLog.d("SubVM", "load() uuid=$uuid serverUrl=$serverUrl")
            _uiState.update { it.copy(isLoading = true, error = null) }
            val subUrl = "$serverUrl/sub/$uuid"

            // Single round-trip: /sub-api gives the config list (body) AND the
            // user metadata (Subscription-Userinfo header + filename).
            val bundle = repo.getSubBundle(uuid).getOrElse { e ->
                SafeLog.e("SubVM", "getSubBundle error: ${SafeLog.throwableSummary(e)}")
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "No configs found")
                }
                return@launch
            }

            val configLinks = parseConfigLinks(bundle.rawBody)
            if (configLinks.isEmpty()) {
                _uiState.update { it.copy(isLoading = false, error = "No configs found") }
                return@launch
            }

            val user = bundle.user
            // "Metadata missing" = the panel returned configs but no usable
            // traffic/expiry info in the header.
            val metaMissing = user.trafficLimitBytes == 0L &&
                user.trafficLimitGb == 0.0 && user.daysLeft == 0

            _uiState.update {
                it.copy(
                    user                     = user,
                    profileMetadataMissing   = metaMissing,
                    configLinks              = configLinks,
                    subUrl                   = subUrl,
                    isLoading                = false,
                    error                    = null
                )
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    /**
     * Parse raw subscription body (Base64 or plain), returning only valid
     * VPN config URIs keyed by their display label.
     * Label comes from the URI #fragment if present, otherwise proto+index.
     */
    private fun parseConfigLinks(raw: String): Map<String, String> {
        val decoded = try {
            val b = Base64.decode(raw.trim(), Base64.URL_SAFE or Base64.NO_WRAP)
            if (b.isNotEmpty()) String(b, Charsets.UTF_8) else raw
        } catch (_: Exception) {
            try { String(Base64.decode(raw.trim(), Base64.DEFAULT)) }
            catch (_: Exception) { raw }
        }

        val result = LinkedHashMap<String, String>()
        val protoCount = mutableMapOf<String, Int>()

        for (rawLine in decoded.lines()) {
            val line = rawLine.trim()
            if (!isVpnUri(line)) continue

            val scheme = line.substringBefore("://").uppercase()
            val label  = extractLabel(line, scheme, protoCount)
            result[label] = line
        }
        return result
    }

    private val VPN_SCHEMES = setOf(
        "VLESS", "VMESS", "TROJAN", "SS", "TROJAN-GO",
        "HY2", "HYSTERIA2", "HYSTERIA", "TUIC", "WIREGUARD"
    )

    private fun isVpnUri(line: String): Boolean {
        if (line.length < 8) return false
        val scheme = line.substringBefore("://", "").uppercase()
        return scheme in VPN_SCHEMES
    }

    private fun extractLabel(
        uri: String,
        scheme: String,
        counts: MutableMap<String, Int>
    ): String {
        val fragment = uri.substringAfterLast("#", "").trim()
        val base = if (fragment.isNotBlank()) {
            try { java.net.URLDecoder.decode(fragment, "UTF-8") }
            catch (_: Exception) { fragment }
        } else {
            val n = counts.getOrDefault(scheme, 0) + 1
            if (n == 1) scheme else "$scheme-$n"
        }
        val count = counts.getOrDefault(scheme, 0) + 1
        counts[scheme] = count
        return base
    }
}
