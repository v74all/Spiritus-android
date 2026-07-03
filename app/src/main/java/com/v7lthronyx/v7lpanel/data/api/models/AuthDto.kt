package com.v7lthronyx.v7lpanel.data.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val password: String
)

@Serializable
data class AgentLoginRequest(
    val name: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val ok: Boolean = false,
    val error: String? = null,
    val remaining: Int? = null,
    @SerialName("agent_id") val agentId: Int? = null
)

@Serializable
data class AuthDto(
    val ok: Boolean = false,
    val error: String? = null,
    val remaining: Int? = null,
    @SerialName("agent_id") val agentId: Int? = null
)

@Serializable
data class MobileAuthDto(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("token_type") val tokenType: String = "bearer",
    @SerialName("expires_in") val expiresIn: Int = 0,
    val role: String = "",
    val principal: String = "",
    @SerialName("totp_required") val totpRequired: Boolean = false,
    val challenge: String? = null
)

@Serializable
data class MobileSessionDto(
    val role: String = "",
    val principal: String = "",
    @SerialName("expires_at") val expiresAt: Long = 0
)
