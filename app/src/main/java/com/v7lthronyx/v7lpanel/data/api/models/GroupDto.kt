package com.v7lthronyx.v7lpanel.data.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupDto(
    // /api/groups returns "id" (string prefix), count, active, disabled, traffic_gb, latest_expire
    val id: String = "",
    val count: Int = 0,
    val active: Int = 0,
    val disabled: Int = 0,
    @SerialName("traffic_gb")      val trafficGb: Double = 0.0,
    @SerialName("latest_expire")   val latestExpire: String = ""
)
