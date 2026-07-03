package com.v7lthronyx.v7lpanel.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.v7lthronyx.v7lpanel.data.local.ConnectionRecordDao
import com.v7lthronyx.v7lpanel.data.local.FavoriteConfigDao
import com.v7lthronyx.v7lpanel.data.local.SettingsDataStore
import com.v7lthronyx.v7lpanel.vpn.ConfigParser
import com.v7lthronyx.v7lpanel.vpn.VpnManager
import com.v7lthronyx.v7lpanel.vpn.VpnStatus
import com.v7lthronyx.v7lpanel.data.db.entities.ConnectionRecord
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(
    private val settingsDataStore: SettingsDataStore,
    private val favoriteConfigDao: FavoriteConfigDao,
    private val connectionRecordDao: ConnectionRecordDao
) : ViewModel() {

    val vpnStatus = VpnManager.status
    val connectedLabel = VpnManager.connectedLabel
    val connectedUri = VpnManager.connectedUri
    val rxSpeed = VpnManager.rxSpeed
    val txSpeed = VpnManager.txSpeed
    val rxTotal = VpnManager.rxTotal
    val txTotal = VpnManager.txTotal
    val connectedSince = VpnManager.connectedSince
    val errorMsg = VpnManager.errorMsg

    val autoConnect = settingsDataStore.autoConnect
    val language = settingsDataStore.language

    private var currentRecordId: Int? = null

    fun connect(context: Context, label: String, uri: String) {
        val config = ConfigParser.parse(uri, label) ?: run {
            VpnManager.updateStatus(VpnStatus.ERROR, "Cannot parse config: $label")
            return
        }
        VpnManager.resetTrafficTotals()
        VpnManager.connect(context, config, uri)

        viewModelScope.launch {
            val id = connectionRecordDao.insert(
                ConnectionRecord(
                    configLabel = label,
                    protocol = config.protocol,
                    connectedAt = System.currentTimeMillis()
                )
            )
            currentRecordId = id.toInt()
        }
    }

    fun disconnect(context: Context) {
        viewModelScope.launch {
            currentRecordId?.let { id ->
                connectionRecordDao.finishRecord(
                    id = id,
                    time = System.currentTimeMillis(),
                    down = rxTotal.value,
                    up = txTotal.value,
                    reason = "user"
                )
            }
            currentRecordId = null
        }
        VpnManager.disconnect(context)
    }

    fun toggleConnection(context: Context, label: String, uri: String) {
        when (vpnStatus.value) {
            VpnStatus.CONNECTED, VpnStatus.CONNECTING -> disconnect(context)
            else -> connect(context, label, uri)
        }
    }

    fun clearError() = VpnManager.clearError()

    fun setLanguage(lang: String) {
        viewModelScope.launch { settingsDataStore.setLanguage(lang) }
    }
}
