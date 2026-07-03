package com.v7lthronyx.v7lpanel.data.local

import androidx.room.*
import com.v7lthronyx.v7lpanel.data.db.entities.FavoriteConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteConfigDao {

    @Query("SELECT * FROM favorite_configs ORDER BY `order` ASC, lastUsed DESC")
    fun getAll(): Flow<List<FavoriteConfig>>

    @Query("SELECT uri FROM favorite_configs")
    suspend fun getAllUris(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: FavoriteConfig): Long

    @Delete
    suspend fun delete(config: FavoriteConfig)

    @Query("DELETE FROM favorite_configs WHERE uri = :uri")
    suspend fun deleteByUri(uri: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_configs WHERE uri = :uri)")
    suspend fun isFavorite(uri: String): Boolean

    @Query("UPDATE favorite_configs SET lastLatency = :latency WHERE id = :id")
    suspend fun updateLatency(id: Int, latency: Int)

    @Query("UPDATE favorite_configs SET lastUsed = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(id: Int, timestamp: Long)
}
