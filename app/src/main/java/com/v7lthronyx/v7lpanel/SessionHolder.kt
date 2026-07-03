package com.v7lthronyx.v7lpanel

import com.v7lthronyx.v7lpanel.data.api.V7LApiClient
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Lightweight in-memory session store set after a successful login.
 * Survives the activity lifecycle but is cleared on process death.
 * SplashScreen is responsible for re-populating from DataStore on cold start.
 */
object SessionHolder {
    @Volatile var serverUrl: String = ""
    @Volatile var role: String = ""        // "admin" | "agent" | "subscriber"
    @Volatile var uuid: String = ""        // subscriber UUID (empty for admin/agent)
    @Volatile var tlsPinSha256: String? = null
    @Volatile var accessToken: String? = null
    @Volatile var profileId: Int = -1
    @Volatile var principal: String = ""

    private var _apiClient: V7LApiClient? = null
    @Volatile private var currentPin: String? = null

    /**
     * Returns a shared API client for the given URL, reusing the existing one
     * if the URL matches. This ensures cookies from login are shared across
     * all ViewModels.
     */
    @Synchronized
    fun getOrCreateClient(
        url: String = serverUrl,
        pin: String? = tlsPinSha256,
        token: String? = accessToken
    ): V7LApiClient {
        val normalized = url.trim().trimEnd('/')
        val current = _apiClient
        if (current != null && current.serverUrl == normalized && currentPin == pin) {
            current.setAccessToken(token)
            return current
        }
        current?.close()
        val client = V7LApiClient(normalized, certificatePinSha256 = pin, initialAccessToken = token)
        _apiClient = client
        currentPin = pin
        return client
    }

    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    fun emitSessionExpired() {
        serverUrl = ""
        role = ""
        uuid = ""
        tlsPinSha256 = null
        accessToken = null
        profileId = -1
        principal = ""
        _apiClient?.close()
        _apiClient = null
        currentPin = null
        _sessionExpired.tryEmit(Unit)
    }

    fun clear() {
        serverUrl = ""
        role = ""
        uuid = ""
        tlsPinSha256 = null
        accessToken = null
        profileId = -1
        principal = ""
        _apiClient?.close()
        _apiClient = null
        currentPin = null
    }
}
