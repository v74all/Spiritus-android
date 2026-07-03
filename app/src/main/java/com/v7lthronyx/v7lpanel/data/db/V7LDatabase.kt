package com.v7lthronyx.v7lpanel.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.v7lthronyx.v7lpanel.data.db.entities.ConnectionRecord
import com.v7lthronyx.v7lpanel.data.db.entities.FavoriteConfig
import com.v7lthronyx.v7lpanel.data.db.entities.ManualConfig
import com.v7lthronyx.v7lpanel.data.db.entities.ServerProfile
import com.v7lthronyx.v7lpanel.data.local.ConnectionRecordDao
import com.v7lthronyx.v7lpanel.data.local.FavoriteConfigDao
import com.v7lthronyx.v7lpanel.data.local.ManualConfigDao
import com.v7lthronyx.v7lpanel.data.local.ServerProfileDao

@Database(
    entities = [ServerProfile::class, ManualConfig::class, FavoriteConfig::class, ConnectionRecord::class],
    version = 4,
    exportSchema = false
)
abstract class V7LDatabase : RoomDatabase() {

    abstract fun serverProfileDao(): ServerProfileDao
    abstract fun manualConfigDao(): ManualConfigDao
    abstract fun favoriteConfigDao(): FavoriteConfigDao
    abstract fun connectionRecordDao(): ConnectionRecordDao

    companion object {
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE server_profiles ADD COLUMN tlsPinSha256 TEXT")
            }
        }

        @Volatile private var INSTANCE: V7LDatabase? = null

        fun getInstance(context: Context): V7LDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    V7LDatabase::class.java,
                    "v7l_database"
                )
                    .addMigrations(MIGRATION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
