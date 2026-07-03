package com.v7lthronyx.v7lpanel.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_configs")
data class FavoriteConfig(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val label: String,
    val uri: String,
    val protocol: String,
    val serverAddress: String,
    val serverPort: Int,
    val lastLatency: Int = -1,
    val lastUsed: Long = 0,
    val order: Int = 0
)
