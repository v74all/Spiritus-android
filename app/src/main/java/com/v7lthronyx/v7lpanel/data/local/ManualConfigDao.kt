package com.v7lthronyx.v7lpanel.data.local

import androidx.room.*
import com.v7lthronyx.v7lpanel.data.db.entities.ManualConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface ManualConfigDao {

    @Query("SELECT * FROM manual_configs ORDER BY addedAt DESC")
    fun getAll(): Flow<List<ManualConfig>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: ManualConfig): Long

    @Delete
    suspend fun delete(config: ManualConfig)

    @Query("DELETE FROM manual_configs")
    suspend fun deleteAll()
}
