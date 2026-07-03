package com.v7lthronyx.v7lpanel.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.v7lthronyx.v7lpanel.vpn.V7LVpnService
import com.v7lthronyx.v7lpanel.vpn.VpnManager
import com.v7lthronyx.v7lpanel.vpn.VpnStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class VpnQuickSettingsTile : TileService() {

    private var statusJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        statusJob = CoroutineScope(Dispatchers.Main).launch {
            VpnManager.status.collectLatest { status ->
                updateTile(status)
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        statusJob?.cancel()
        statusJob = null
    }

    override fun onClick() {
        super.onClick()
        when (VpnManager.status.value) {
            VpnStatus.CONNECTED, VpnStatus.CONNECTING -> {
                VpnManager.disconnect(this)
            }
            VpnStatus.DISCONNECTED, VpnStatus.ERROR -> {
                val connectedUri = VpnManager.connectedUri.value
                val connectedLabel = VpnManager.connectedLabel.value
                if (connectedUri != null && connectedLabel != null) {
                    val config = com.v7lthronyx.v7lpanel.vpn.ConfigParser.parse(connectedUri, connectedLabel)
                    if (config != null) {
                        VpnManager.connect(this, config, connectedUri)
                    }
                }
            }
        }
    }

    private fun updateTile(status: VpnStatus) {
        val tile = qsTile ?: return
        when (status) {
            VpnStatus.CONNECTED -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "Spiritus"
                tile.setSubtitleCompat("Connected")
            }
            VpnStatus.CONNECTING -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "Spiritus"
                tile.setSubtitleCompat("Connecting...")
            }
            VpnStatus.DISCONNECTED -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "Spiritus"
                tile.setSubtitleCompat("Disconnected")
            }
            VpnStatus.ERROR -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "Spiritus"
                tile.setSubtitleCompat("Error")
            }
        }
        tile.updateTile()
    }

    private fun Tile.setSubtitleCompat(value: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            subtitle = value
        }
    }
}
