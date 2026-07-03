package com.v7lthronyx.v7lpanel.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.v7lthronyx.v7lpanel.data.db.entities.ServerProfile
import com.v7lthronyx.v7lpanel.data.local.ServerProfileDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LoginViewModel(
    private val dao: ServerProfileDao
) : ViewModel() {

    val profiles: StateFlow<List<ServerProfile>> = dao.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun addProfile(
        name:      String,
        url:       String,
        role:      String,
        agentName: String?,
        uuid:      String?,
        tlsPinSha256: String? = null,
        onInserted: ((Int) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val id = dao.insert(
                ServerProfile(
                    name           = name,
                    url            = url.trimEnd('/'),
                    role           = role,
                    subscriberUuid = uuid,
                    agentName      = agentName,
                    tlsPinSha256   = tlsPinSha256,
                    lastUsed       = System.currentTimeMillis()
                )
            )
            onInserted?.invoke(id.toInt())
        }
    }

    fun deleteProfile(profile: ServerProfile) {
        viewModelScope.launch { dao.delete(profile) }
    }
}
