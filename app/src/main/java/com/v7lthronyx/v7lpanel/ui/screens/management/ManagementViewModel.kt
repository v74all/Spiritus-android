package com.v7lthronyx.v7lpanel.ui.screens.management

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.v7lthronyx.v7lpanel.SessionHolder
import com.v7lthronyx.v7lpanel.data.api.models.AgentDto
import com.v7lthronyx.v7lpanel.data.api.models.ServerInfoDto
import com.v7lthronyx.v7lpanel.data.api.models.UserDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ManagementUiState(
    val loading: Boolean = false,
    val users: List<UserDto> = emptyList(),
    val agents: List<AgentDto> = emptyList(),
    val server: ServerInfoDto = ServerInfoDto(),
    val error: String? = null,
    val message: String? = null
)

class ManagementViewModel : ViewModel() {
    private val api get() = SessionHolder.getOrCreateClient()
    private val _state = MutableStateFlow(ManagementUiState())
    val state: StateFlow<ManagementUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() = action("Refreshed") {
        val users = if (SessionHolder.role == "agent") api.getAgentUsers() else api.getUsers()
        val server = if (SessionHolder.role == "agent") api.getAgentServerInfo() else api.getServerInfo()
        val agents = if (SessionHolder.role == "admin") api.getAgents() else emptyList()
        _state.value = _state.value.copy(users = users, agents = agents, server = server)
    }

    fun createUser(name: String, trafficGb: Double, days: Int) = action("User created") {
        if (SessionHolder.role == "agent") api.addAgentUser(name, trafficGb, days)
        else api.addUser(name, trafficGb, days)
        reloadData()
    }

    fun renewUser(name: String, trafficGb: Double, days: Int) = action("User renewed") {
        if (SessionHolder.role == "agent") api.renewAgentUser(name, trafficGb, days)
        else api.renewUser(name, trafficGb, days)
        reloadData()
    }

    fun toggleUser(name: String) = action("User status updated") {
        if (SessionHolder.role == "agent") api.toggleAgentUser(name) else api.toggleUser(name)
        reloadData()
    }

    fun deleteUser(name: String) = action("User deleted") {
        if (SessionHolder.role == "agent") api.deleteAgentUser(name) else api.deleteUser(name)
        reloadData()
    }

    fun addTraffic(name: String, gb: Double) = action("Traffic added") {
        if (SessionHolder.role == "agent") api.addAgentTraffic(name, gb) else api.addTraffic(name, gb)
        reloadData()
    }

    /** Admin-only: the panel has no agent-scoped speed-limit endpoint. */
    fun setSpeedLimit(name: String, downKbps: Int, upKbps: Int) = action("Speed limit updated") {
        check(SessionHolder.role != "agent")
        api.setSpeedLimit(name, downKbps, upKbps)
        reloadData()
    }

    /** Admin-only: the panel has no agent-scoped reset-traffic endpoint. */
    fun resetTraffic(name: String) = action("Traffic reset") {
        check(SessionHolder.role != "agent")
        api.resetTraffic(name)
        reloadData()
    }

    /** Admin-only: the panel has no agent-scoped update-note endpoint. */
    fun updateNote(name: String, note: String) = action("Note updated") {
        check(SessionHolder.role != "agent")
        api.updateNote(name, note)
        reloadData()
    }

    fun syncServer() = action("Server synchronized") {
        if (SessionHolder.role == "agent") api.agentSync() else api.sync()
        reloadData()
    }

    fun createAgent(name: String, password: String, quotaGb: Double) = action("Agent created") {
        check(SessionHolder.role == "admin")
        api.addAgent(name, password, quotaGb)
        reloadData()
    }

    fun deleteAgent(id: Int) = action("Agent deleted") {
        check(SessionHolder.role == "admin")
        api.deleteAgent(id)
        reloadData()
    }

    fun clearNotice() { _state.value = _state.value.copy(error = null, message = null) }

    private suspend fun reloadData() {
        val users = if (SessionHolder.role == "agent") api.getAgentUsers() else api.getUsers()
        val agents = if (SessionHolder.role == "admin") api.getAgents() else emptyList()
        _state.value = _state.value.copy(users = users, agents = agents)
    }

    private fun action(success: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null, message = null)
            runCatching { block() }
                .onSuccess { _state.value = _state.value.copy(loading = false, message = success) }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.message ?: "Request failed") }
        }
    }
}
