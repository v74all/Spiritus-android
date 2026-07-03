package com.v7lthronyx.v7lpanel.data.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActivityDto(
    val sites: List<SiteEntry> = emptyList(),
    val recent: List<RecentEntry> = emptyList(),
    val alerts: List<AlertEntry> = emptyList(),
    val analysis: AnalysisData? = null,
    val deep: DeepAnalysisData? = null
)

@Serializable
data class SiteEntry(
    val host: String,
    val port: Int = 443,
    val count: Int = 0,
    @SerialName("last_seen") val lastSeen: String = "",
    val service: String = "",
    val category: String = "",
    val risk: String = "safe",
    val flag: String = "",
    val country: String = "",
    val org: String = ""
)

@Serializable
data class RecentEntry(
    val host: String,
    val port: Int = 443,
    @SerialName("seen_at") val seenAt: String = "",
    val service: String = "",
    val risk: String = "safe"
)

@Serializable
data class AlertEntry(
    val host: String,
    val reason: String,
    val timestamp: String = ""
)

@Serializable
data class AnalysisData(
    val verdict: String = "normal",
    val categories: Map<String, Float> = emptyMap(),
    val summary: String = ""
)

@Serializable
data class DeepAnalysisData(
    val behavioral: String = "",
    val risk_score: Int = 0,
    val flags: List<String> = emptyList()
)
