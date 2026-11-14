package com.example.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.local.ClipCraftDatabase
import com.example.data.local.ExportedClipEntity
import com.example.data.model.YouTubeInspectionResult
import com.example.data.model.YouTubeUrlParser
import com.example.data.network.YouTubeOEmbedClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

class ClipRepository(private val context: Context) {

    private val dao = ClipCraftDatabase.getInstance(context).exportedClipDao()

    fun observeExportedClips(): Flow<List<ExportedClipEntity>> = dao.observeAll()

    suspend fun saveExportedClip(
        sourceFile: File,
        title: String,
        aspectRatioLabel: String,
        qualityLabel: String,
        durationMs: Long,
        audioMode: String,
        creatorHandle: String,
        youtubeTags: String
    ): Pair<ExportedClipEntity, Boolean> = withContext(Dispatchers.IO) {
        val galleryUri = publishToMediaStore(sourceFile, title)
        val savedToGallery = galleryUri != null
        val shareableUri = galleryUri
            ?: FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                sourceFile
            ).toString()

        val entity = ExportedClipEntity(
            title = title,
            filePath = sourceFile.absolutePath,
            contentUri = shareableUri,
            aspectRatioLabel = aspectRatioLabel,
            qualityLabel = qualityLabel,
            durationMs = durationMs,
            audioMode = audioMode,
            creatorHandle = creatorHandle,
            youtubeTags = youtubeTags,
            fileSizeBytes = sourceFile.length(),
            createdAtMs = System.currentTimeMillis()
        )
        val rowId = dao.insert(entity)
        entity.copy(id = rowId) to savedToGallery
    }

    suspend fun deleteClip(clip: ExportedClipEntity) = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(clip.filePath)
            if (file.exists()) file.delete()
        }
        runCatching {
            if (clip.contentUri.startsWith("content://media/")) {
                context.contentResolver.delete(Uri.parse(clip.contentUri), null, null)
            }
        }
        dao.delete(clip)
    }

    suspend fun inspectYouTubeLink(rawUrl: String): Result<YouTubeInspectionResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val videoId = YouTubeUrlParser.extractVideoId(rawUrl)
                    ?: throw IllegalArgumentException("Please enter a valid YouTube link (watch, shorts, or youtu.be).")
                val canonicalUrl = "https://www.youtube.com/watch?v=$videoId"
                val response = YouTubeOEmbedClient.api.getVideoMetadata(canonicalUrl)
                val isShorts = rawUrl.contains("/shorts/", ignoreCase = true) ||
                    (response.height > response.width && response.width > 0)
                YouTubeInspectionResult(
                    videoId = videoId,
                    canonicalUrl = canonicalUrl,
                    title = response.title.ifBlank { "Untitled Video" },
                    authorName = response.authorName.ifBlank { "Unknown Creator" },
                    authorUrl = response.authorUrl,
                    thumbnailUrl = response.thumbnailUrl.ifBlank {
                        "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                    },
                    isVerticalShorts = isShorts
                )
            }
        }

    private fun publishToMediaStore(sourceFile: File, title: String): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return runCatching {
            val resolver = context.contentResolver
            val safeName = title.replace(Regex("[^A-Za-z0-9_\\- ]"), "").trim()
                .ifBlank { "ClipCraft_${System.currentTimeMillis()}" }
                .take(48)
            val fileName = "${safeName}_${System.currentTimeMillis() % 10000}.mp4"
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/ClipCraft")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = resolver.insert(collection, values) ?: return null
            resolver.openOutputStream(uri)?.use { out ->
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(out)
                }
            }
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri.toString()
        }.getOrNull()
    }
}
