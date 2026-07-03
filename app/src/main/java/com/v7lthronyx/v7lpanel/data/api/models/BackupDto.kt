package com.v7lthronyx.v7lpanel.data.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// GET /api/backup/list returns {"backups": [{"name", "path", "size", "created"}]}
@Serializable
data class BackupDto(
    val name: String = "",
    val path: String = "",
    val size: Long = 0L,
    @SerialName("created") val time: String = ""
)
