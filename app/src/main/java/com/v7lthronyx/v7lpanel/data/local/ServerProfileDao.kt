package com.v7lthronyx.v7lpanel.data.local

import androidx.room.*
import com.v7lthronyx.v7lpanel.data.db.entities.ServerProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerProfileDao {
    @Query("SELECT * FROM server_profiles WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): ServerProfile?

    @Query("SELECT * FROM server_profiles ORDER BY lastUsed DESC")
    fun getAllProfiles(): Flow<List<ServerProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: ServerProfile): Long

    @Update
    suspend fun update(profile: ServerProfile)

    @Delete
    suspend fun delete(profile: ServerProfile)

    @Query("UPDATE server_profiles SET lastUsed = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(id: Int, timestamp: Long)
}
