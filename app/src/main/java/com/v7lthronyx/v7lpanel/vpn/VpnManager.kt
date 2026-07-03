package com.v7lthronyx.v7lpanel.vpn

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class VpnStatus { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

object VpnManager {

    private val _status = MutableStateFlow(VpnStatus.DISCONNECTED)
    val status: StateFlow<VpnStatus> = _status.asStateFlow()

    private val _errorMsg = MutableStateFlow<String?>(null)
    val errorMsg: StateFlow<String?> = _errorMsg.asStateFlow()

    private val _connectedLabel = MutableStateFlow<String?>(null)
    val connectedLabel: StateFlow<String?> = _connectedLabel.asStateFlow()

    private val _connectedUri = MutableStateFlow<String?>(null)
    val connectedUri: StateFlow<String?> = _connectedUri.asStateFlow()

    private val _rxSpeed = MutableStateFlow(0L)
    val rxSpeed: StateFlow<Long> = _rxSpeed.asStateFlow()

    private val _txSpeed = MutableStateFlow(0L)
    val txSpeed: StateFlow<Long> = _txSpeed.asStateFlow()
    private val _rxTotal = MutableStateFlow(0L)
    val rxTotal: StateFlow<Long> = _rxTotal.asStateFlow()
    private val _txTotal = MutableStateFlow(0L)
    val txTotal: StateFlow<Long> = _txTotal.asStateFlow()

    private val _connectedSince = MutableStateFlow<Long?>(null)
    val connectedSince: StateFlow<Long?> = _connectedSince.asStateFlow()

    fun updateStatus(s: VpnStatus, error: String? = null) {
        _status.value = s
        _errorMsg.value = error
        if (s == VpnStatus.CONNECTED) {
            _connectedSince.value = System.currentTimeMillis()
        } else if (s == VpnStatus.DISCONNECTED || s == VpnStatus.ERROR) {
            _connectedLabel.value = null
            _connectedUri.value = null
            _rxSpeed.value = 0L
            _txSpeed.value = 0L
            _connectedSince.value = null
        }
    }

    fun connect(context: Context, config: ParsedConfig, uri: String) {
        _status.value = VpnStatus.CONNECTING
        _connectedLabel.value = config.label
        _connectedUri.value = uri
        val intent = Intent(context, V7LVpnService::class.java).apply {
            action = V7LVpnService.ACTION_START
            putExtra(V7LVpnService.EXTRA_LABEL, config.label)
            putExtra(V7LVpnService.EXTRA_OUTBOUND_JSON, config.xrayOutboundJson)
            putExtra(V7LVpnService.EXTRA_ENGINE, config.engine)
        }
        context.startForegroundService(intent)
    }

    fun disconnect(context: Context) {
        val intent = Intent(context, V7LVpnService::class.java).apply {
            action = V7LVpnService.ACTION_STOP
        }
        context.startService(intent)
    }

    fun clearError() { _errorMsg.value = null }

    fun updateSpeed(rx: Long, tx: Long) {
        _rxSpeed.value = rx
        _txSpeed.value = tx
        _rxTotal.value += rx.coerceAtLeast(0)
        _txTotal.value += tx.coerceAtLeast(0)
    }

    fun resetTrafficTotals() {
        _rxTotal.value = 0L
        _txTotal.value = 0L
    }
}
