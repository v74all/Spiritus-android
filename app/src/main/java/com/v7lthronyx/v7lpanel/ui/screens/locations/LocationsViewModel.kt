package com.v7lthronyx.v7lpanel.ui.screens.locations

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.v7lthronyx.v7lpanel.SessionHolder
import com.v7lthronyx.v7lpanel.data.db.entities.FavoriteConfig
import com.v7lthronyx.v7lpanel.data.local.FavoriteConfigDao
import com.v7lthronyx.v7lpanel.data.local.ManualConfigDao
import com.v7lthronyx.v7lpanel.data.local.SettingsDataStore
import com.v7lthronyx.v7lpanel.data.repository.SubscriptionRepository
import com.v7lthronyx.v7lpanel.util.SafeLog
import com.v7lthronyx.v7lpanel.util.PingTester
import com.v7lthronyx.v7lpanel.vpn.ConfigParser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ConfigItem(
    val label: String,
    val uri: String,
    val protocol: String,
    val serverAddress: String,
    val serverPort: Int,
    val isFavorite: Boolean = false,
    val latency: Int = -1
)

class LocationsViewModel(
    private val favoriteConfigDao: FavoriteConfigDao,
    private val manualConfigDao: ManualConfigDao,
    private val settingsDataStore: SettingsDataStore,
    private val repo: SubscriptionRepository
) : ViewModel() {

    private val _configs = MutableStateFlow<List<ConfigItem>>(emptyList())
    val configs: StateFlow<List<ConfigItem>> = _configs.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val favorites: Flow<List<FavoriteConfig>> = favoriteConfigDao.getAll()

    val language = settingsDataStore.language

    private val _isPinging = MutableStateFlow(false)
    val isPinging: StateFlow<Boolean> = _isPinging.asStateFlow()

    init {
        loadConfigs()
        // Also watch manual configs and merge them
        viewModelScope.launch {
            manualConfigDao.getAll().collect { manualList ->
                val favUris = favoriteConfigDao.getAllUris().toSet()
                val manualItems = manualList.mapNotNull { mc ->
                    val parsed = ConfigParser.parse(mc.uri, mc.label) ?: return@mapNotNull null
                    ConfigItem(
                        label = mc.label,
                        uri = mc.uri,
                        protocol = parsed.protocol,
                        serverAddress = parsed.serverAddress,
                        serverPort = parsed.serverPort,
                        isFavorite = mc.uri in favUris
                    )
                }
                // Merge: keep subscription configs + manual configs (avoid duplicates)
                val subUris = _configs.value.filter { c -> manualList.none { it.uri == c.uri } }
                // distinctBy: the config list feeds LazyColumn(key = uri) — any
                // duplicate URI (double import, repeated sub entry) is a crash.
                _configs.value = (subUris + manualItems).distinctBy { it.uri }
            }
        }
    }

    fun loadConfigs() {
        val uuid = SessionHolder.uuid
        val serverUrl = SessionHolder.serverUrl
        SafeLog.d("LocVM", "loadConfigs: uuidPresent=${uuid.isNotBlank()} serverUrlPresent=${serverUrl.isNotBlank()}")
        // Local "general client" mode: no panel subscription — just show the
        // manually-imported configs (merged from manualConfigDao in init). Not
        // an error; quietly skip the subscription fetch.
        if (uuid.isBlank() || serverUrl.isBlank() || SessionHolder.role == "local") {
            _isLoading.value = false
            _error.value = null
            SafeLog.d("LocVM", "local mode (no panel sub) — showing manual configs only")
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // Single round-trip to /sub-api for the whole config list.
            SafeLog.d("LocVM", "Trying getSubBundle")
            val bundle = repo.getSubBundle(uuid).getOrElse { e ->
                SafeLog.e("LocVM", "getSubBundle failed: ${SafeLog.throwableSummary(e)}")
                _isLoading.value = false
                _error.value = e.message ?: "No configs found"
                return@launch
            }
            val configLinks = parseConfigLinks(bundle.rawBody)
            SafeLog.d("LocVM", "Parsed ${configLinks.size} config links")

            if (configLinks.isEmpty()) {
                _isLoading.value = false
                _error.value = "No configs found"
                SafeLog.w("LocVM", "No configs found")
                return@launch
            }

            val favUris = favoriteConfigDao.getAllUris().toSet()
            val items = configLinks.mapNotNull { (label, uri) ->
                val parsed = ConfigParser.parse(uri, label)
                if (parsed == null) SafeLog.w("LocVM", "Parse failed for one config")
                parsed ?: return@mapNotNull null
                ConfigItem(
                    label = label,
                    uri = uri,
                    protocol = parsed.protocol,
                    serverAddress = parsed.serverAddress,
                    serverPort = parsed.serverPort,
                    isFavorite = uri in favUris
                )
            }
            SafeLog.d("LocVM", "Loaded ${items.size} config items")
            // Subscriptions can repeat the same URI; LazyColumn keys must be unique.
            _configs.value = items.distinctBy { it.uri }
            _isLoading.value = false
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    val filteredConfigs: Flow<List<ConfigItem>> = combine(_configs, _searchQuery) { list, query ->
        if (query.isBlank()) list
        else list.filter {
            it.label.contains(query, ignoreCase = true) ||
            it.protocol.contains(query, ignoreCase = true) ||
            it.serverAddress.contains(query, ignoreCase = true)
        }
    }

    fun toggleFavorite(item: ConfigItem) {
        viewModelScope.launch {
            if (item.isFavorite) {
                favoriteConfigDao.deleteByUri(item.uri)
            } else {
                favoriteConfigDao.insert(
                    FavoriteConfig(
                        label = item.label,
                        uri = item.uri,
                        protocol = item.protocol,
                        serverAddress = item.serverAddress,
                        serverPort = item.serverPort
                    )
                )
            }
            val favUris = favoriteConfigDao.getAllUris().toSet()
            _configs.value = _configs.value.map { it.copy(isFavorite = it.uri in favUris) }
        }
    }

    fun pingAll() {
        viewModelScope.launch {
            _isPinging.value = true
            val updated = _configs.value.map { item ->
                val hp = PingTester.extractHostPort(item.uri)
                if (hp != null) {
                    val latency = PingTester.tcpPing(hp.first, hp.second).toInt()
                    item.copy(latency = latency)
                } else item
            }
            _configs.value = updated
            _isPinging.value = false
        }
    }

    fun pingItem(item: ConfigItem) {
        viewModelScope.launch {
            val hp = PingTester.extractHostPort(item.uri)
            if (hp != null) {
                val latency = PingTester.tcpPing(hp.first, hp.second).toInt()
                _configs.value = _configs.value.map {
                    if (it.uri == item.uri) it.copy(latency = latency) else it
                }
            }
        }
    }

    fun getBestConfig(): ConfigItem? {
        return _configs.value
            .filter { it.latency > 0 }
            .minByOrNull { it.latency }
    }

    fun clearError() { _error.value = null }

    // ── Config parsing helpers ──

    private val VPN_SCHEMES = setOf(
        "VLESS", "VMESS", "TROJAN", "SS", "TROJAN-GO",
        "HY2", "HYSTERIA2", "HYSTERIA", "TUIC", "WIREGUARD", "WG"
    )

    private fun parseConfigLinks(raw: String): Map<String, String> {
        val decoded = try {
            val b = Base64.decode(raw.trim(), Base64.URL_SAFE or Base64.NO_WRAP)
            if (b.isNotEmpty()) String(b, Charsets.UTF_8) else raw
        } catch (_: Exception) {
            try { String(Base64.decode(raw.trim(), Base64.DEFAULT)) }
            catch (_: Exception) { raw }
        }
        SafeLog.d("LocVM", "parseConfigLinks decodedLines=${decoded.lines().size}")

        val result = LinkedHashMap<String, String>()
        val protoCount = mutableMapOf<String, Int>()

        for (rawLine in decoded.lines()) {
            val line = rawLine.trim()
            if (line.length < 8) continue
            val scheme = line.substringBefore("://", "").uppercase()
            if (scheme !in VPN_SCHEMES) continue

            val fragment = line.substringAfterLast("#", "").trim()
            val label = if (fragment.isNotBlank()) {
                try { java.net.URLDecoder.decode(fragment, "UTF-8") }
                catch (_: Exception) { fragment }
            } else {
                val n = protoCount.getOrDefault(scheme, 0) + 1
                if (n == 1) scheme else "$scheme-$n"
            }
            protoCount[scheme] = (protoCount[scheme] ?: 0) + 1
            result[label] = line
        }
        return result
    }
}
