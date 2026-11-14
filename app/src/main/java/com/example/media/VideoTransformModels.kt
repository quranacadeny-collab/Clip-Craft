package com.example.media

import android.net.Uri

enum class AspectRatioPreset(
    val label: String,
    val badge: String,
    val ratio: Float?
) {
    SHORTS_9_16("9:16 Shorts", "9:16", 9f / 16f),
    LANDSCAPE_16_9("16:9 YouTube", "16:9", 16f / 9f),
    SQUARE_1_1("1:1 Square", "1:1", 1f),
    PORTRAIT_4_5("4:5 Feed", "4:5", 4f / 5f),
    FREE("Free Crop", "Free", null)
}

enum class AudioTransformMode(val label: String) {
    KEEP_ORIGINAL("Keep Original"),
    MUTE("Mute Audio"),
    REPLACE_AUDIO("Replace Audio")
}

enum class ExportQuality(
    val label: String,
    val badge: String,
    val targetHeight: Int,
    val bitrateBps: Int
) {
    HD_720P("720p HD (Fast)", "720p", 720, 6_000_000),
    FULL_HD_1080P("1080p Full HD (High)", "1080p", 1080, 12_000_000)
}

data class CropRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
) {
    val width: Float get() = (right - left).coerceIn(0.1f, 1f)
    val height: Float get() = (bottom - top).coerceIn(0.1f, 1f)

    fun isFullFrame(): Boolean =
        left <= 0.005f && top <= 0.005f && right >= 0.995f && bottom >= 0.995f
}

data class VideoSourceMetadata(
    val uri: Uri,
    val displayName: String,
    val width: Int,
    val height: Int,
    val durationMs: Long
) {
    val aspectRatio: Float
        get() = if (width > 0 && height > 0) width.toFloat() / height.toFloat() else 16f / 9f
}

data class TransformSpec(
    val videoUri: Uri,
    val outputTitle: String,
    val cropRect: CropRect,
    val aspectRatioLabel: String,
    val trimStartMs: Long,
    val trimEndMs: Long,
    val mirrorHorizontal: Boolean,
    val rotationDegrees: Int,
    val audioMode: AudioTransformMode,
    val replacementAudioUri: Uri?,
    val creatorHandle: String,
    val hookCaption: String = "",
    val youtubeTags: String = "",
    val quality: ExportQuality = ExportQuality.FULL_HD_1080P
)

sealed interface ExportState {
    data object Idle : ExportState
    data class Exporting(
        val progressPercent: Int,
        val currentClip: Int = 1,
        val totalClips: Int = 1,
        val currentTitle: String = ""
    ) : ExportState

    data class Success(
        val clipId: Long,
        val title: String,
        val contentUri: String,
        val durationMs: Long,
        val exportedCount: Int = 1,
        val savedToGallery: Boolean = true
    ) : ExportState

    data class Error(val message: String) : ExportState
}
