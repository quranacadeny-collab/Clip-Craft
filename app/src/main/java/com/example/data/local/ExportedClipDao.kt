package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExportedClipDao {
    @Query("SELECT * FROM exported_clips ORDER BY createdAtMs DESC")
    fun observeAll(): Flow<List<ExportedClipEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(clip: ExportedClipEntity): Long

    @Delete
    suspend fun delete(clip: ExportedClipEntity)
}
