package com.v7lthronyx.v7lpanel.data.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// /api/live returns {"username": {"up": 123, "down": 456}}
@Serializable
data class TrafficEntry(
    val up: Long = 0,
    val down: Long = 0
)

typealias LiveDataDto = Map<String, TrafficEntry>
