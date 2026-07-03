package com.v7lthronyx.v7lpanel.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "server_profiles")
data class ServerProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val url: String,                        // e.g. "https://188.121.110.176:8080"
    val role: String,                       // "admin" | "agent" | "subscriber"
    val lastUsed: Long = 0L,
    val subscriberUuid: String? = null,     // Only for subscriber role
    val agentName: String? = null,          // Agent username, if role == "agent"
    val tlsPinSha256: String? = null        // sha256/<base64 DER certificate digest>
)
