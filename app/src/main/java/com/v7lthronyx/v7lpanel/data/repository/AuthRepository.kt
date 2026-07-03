package com.v7lthronyx.v7lpanel.data.repository

import com.v7lthronyx.v7lpanel.data.api.V7LApiClient
import com.v7lthronyx.v7lpanel.data.api.models.AuthDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(private val api: V7LApiClient) {

    suspend fun adminLogin(password: String): Result<Boolean> =
        withContext(Dispatchers.IO) { api.login(password) }

    suspend fun adminLogout(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { api.logout(); Unit }
    }

    suspend fun agentLogin(name: String, password: String): Result<Int> =
        withContext(Dispatchers.IO) { api.agentLogin(name, password) }

    suspend fun agentLogout(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { api.agentLogout(); Unit }
    }
}
