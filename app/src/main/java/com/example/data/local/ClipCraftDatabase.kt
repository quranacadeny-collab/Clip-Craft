package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ExportedClipEntity::class],
    version = 2,
    exportSchema = false
)
abstract class ClipCraftDatabase : RoomDatabase() {
    abstract fun exportedClipDao(): ExportedClipDao

    companion object {
        @Volatile
        private var INSTANCE: ClipCraftDatabase? = null

        fun getInstance(context: Context): ClipCraftDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ClipCraftDatabase::class.java,
                    "clipcraft_db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
