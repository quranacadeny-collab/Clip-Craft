package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exported_clips")
data class ExportedClipEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val filePath: String,
    val contentUri: String,
    val aspectRatioLabel: String,
    val qualityLabel: String = "1080p",
    val durationMs: Long,
    val audioMode: String,
    val creatorHandle: String,
    val youtubeTags: String = "",
    val fileSizeBytes: Long,
    val createdAtMs: Long
)
