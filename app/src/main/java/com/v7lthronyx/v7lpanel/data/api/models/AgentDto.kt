package com.v7lthronyx.v7lpanel.data.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AgentDto(
    val id: Int = 0,
    val name: String = "",
    @SerialName("traffic_quota_gb")  val trafficQuotaGb: Double = 0.0,
    // /api/agents returns "traffic_used_gb", /api/agent/me returns "traffic_used_gb"
    @SerialName("traffic_used_gb")   val trafficUsedGb: Double = 0.0,
    @SerialName("traffic_remaining_gb") val trafficRemainingGb: Double = 0.0,
    val active: Boolean = true,
    @SerialName("created_at")        val createdAt: String = "",
    @SerialName("user_count")        val userCount: Int = 0,
    @SerialName("brand_name")        val brandName: String = ""
)
