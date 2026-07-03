package com.v7lthronyx.v7lpanel.data.local

import androidx.room.*
import com.v7lthronyx.v7lpanel.data.db.entities.ConnectionRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ConnectionRecordDao {

    @Query("SELECT * FROM connection_history ORDER BY connectedAt DESC LIMIT :limit")
    fun getRecent(limit: Int = 50): Flow<List<ConnectionRecord>>

    @Insert
    suspend fun insert(record: ConnectionRecord): Long

    @Query("UPDATE connection_history SET disconnectedAt = :time, bytesDown = :down, bytesUp = :up, disconnectReason = :reason WHERE id = :id")
    suspend fun finishRecord(id: Int, time: Long, down: Long, up: Long, reason: String)

    @Query("DELETE FROM connection_history")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM connection_history")
    suspend fun count(): Int
}
