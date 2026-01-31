package com.runshare.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 跑步记录数据库
 */
@Database(
    entities = [
        RunEntity::class,
        UserEntity::class,
        CheckInEntity::class,
        GroupEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class RunDatabase : RoomDatabase() {

    abstract fun runDao(): RunDao
    abstract fun userDao(): UserDao
    abstract fun checkInDao(): CheckInDao
    abstract fun groupDao(): GroupDao

    companion object {
        @Volatile
        private var INSTANCE: RunDatabase? = null

        // 迁移：版本1 -> 2
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建用户表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS users (
                        id TEXT PRIMARY KEY NOT NULL,
                        username TEXT NOT NULL,
                        avatarUrl TEXT NOT NULL DEFAULT '',
                        totalDistance REAL NOT NULL DEFAULT 0.0,
                        totalRuns INTEGER NOT NULL DEFAULT 0,
                        totalDuration INTEGER NOT NULL DEFAULT 0,
                        checkInDays INTEGER NOT NULL DEFAULT 0,
                        lastCheckIn INTEGER NOT NULL DEFAULT 0,
                        groupId TEXT,
                        createdAt INTEGER NOT NULL
                    )
                """)
                
                // 创建打卡表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS check_ins (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId TEXT NOT NULL,
                        date INTEGER NOT NULL,
                        runId INTEGER,
                        note TEXT NOT NULL DEFAULT ''
                    )
                """)
                
                // 创建小组表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS groups (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        creatorId TEXT NOT NULL,
                        memberCount INTEGER NOT NULL DEFAULT 0,
                        totalDistance REAL NOT NULL DEFAULT 0.0,
                        inviteCode TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """)
            }
        }

        fun getInstance(context: Context): RunDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RunDatabase::class.java,
                    "run_database"
                )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

