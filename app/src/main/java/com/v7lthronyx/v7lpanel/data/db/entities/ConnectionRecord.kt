package com.v7lthronyx.v7lpanel.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "connection_history")
data class ConnectionRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val configLabel: String,
    val protocol: String,
    val connectedAt: Long,
    val disconnectedAt: Long = 0,
    val bytesDown: Long = 0,
    val bytesUp: Long = 0,
    val disconnectReason: String = ""
)
