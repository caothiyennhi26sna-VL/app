package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ThinkingSessionEntity::class, ThinkingMessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ThinkingDatabase : RoomDatabase() {
    abstract fun thinkingDao(): ThinkingDao

    companion object {
        @Volatile
        private var INSTANCE: ThinkingDatabase? = null

        fun getDatabase(context: Context): ThinkingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ThinkingDatabase::class.java,
                    "thinking_mentor_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
