package com.v7lthronyx.v7lpanel.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "manual_configs")
data class ManualConfig(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val label: String,
    val uri: String,
    val addedAt: Long = System.currentTimeMillis()
)
