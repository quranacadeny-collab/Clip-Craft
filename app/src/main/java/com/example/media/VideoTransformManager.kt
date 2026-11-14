package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.Crop
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import com.example.data.repository.ClipRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@androidx.annotation.OptIn(UnstableApi::class)
class VideoTransformManager(
    private val context: Context,
    private val repository: ClipRepository,
    private val scope: CoroutineScope
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var activeTransformer: Transformer? = null
    private var progressRunnable: Runnable? = null
    private var pendingQueue: ArrayDeque<TransformSpec> = ArrayDeque()
    private var totalQueueCount: Int = 1
    private var currentQueueIndex: Int = 1

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    suspend fun loadVideoMetadata(uri: Uri): VideoSourceMetadata = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        var width = 1920
        var height = 1080
        var durationMs = 10_000L
        try {
            retriever.setDataSource(context, uri)
            val rawW = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 1920
            val rawH = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 1080
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 10_000L

            if (rotation == 90 || rotation == 270) {
                width = rawH
                height = rawW
            } else {
                width = rawW
                height = rawH
            }
        } catch (_: Exception) {
            // Keep defaults if restricted
        } finally {
            runCatching { retriever.release() }
        }

        val displayName = resolveDisplayName(uri) ?: "Clip_${System.currentTimeMillis()}"
        VideoSourceMetadata(
            uri = uri,
            displayName = displayName,
            width = width.coerceAtLeast(1),
            height = height.coerceAtLeast(1),
            durationMs = durationMs.coerceAtLeast(1_000L)
        )
    }

    fun startExport(spec: TransformSpec) {
        startBatchExport(listOf(spec))
    }

    fun startBatchExport(specs: List<TransformSpec>) {
        if (specs.isEmpty()) return
        mainHandler.post {
            cancelInternal(resetState = false)
            pendingQueue = ArrayDeque(specs)
            totalQueueCount = specs.size
            currentQueueIndex = 0
            processNextInQueue()
        }
    }

    private fun processNextInQueue() {
        val spec = pendingQueue.removeFirstOrNull() ?: return
        currentQueueIndex += 1
        _exportState.value = ExportState.Exporting(
            progressPercent = 0,
            currentClip = currentQueueIndex,
            totalClips = totalQueueCount,
            currentTitle = spec.outputTitle
        )

        val exportsDir = File(context.filesDir, "exports").apply { mkdirs() }
        val outputFile = File(exportsDir, "clipcraft_${System.currentTimeMillis()}_${currentQueueIndex}.mp4")
        if (outputFile.exists()) outputFile.delete()

        val clippedDurationMs = (spec.trimEndMs - spec.trimStartMs).coerceAtLeast(500L)

        val inputMediaItem = MediaItem.Builder()
            .setUri(spec.videoUri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(spec.trimStartMs.coerceAtLeast(0L))
                    .setEndPositionMs(spec.trimEndMs.coerceAtLeast(spec.trimStartMs + 500L))
                    .build()
            )
            .build()

        val videoEffects = buildVideoEffects(spec)

        val editedVideoItem = EditedMediaItem.Builder(inputMediaItem)
            .setRemoveAudio(spec.audioMode != AudioTransformMode.KEEP_ORIGINAL)
            .setEffects(Effects(emptyList(), videoEffects))
            .build()

        val encoderFactory = DefaultEncoderFactory.Builder(context)
            .setRequestedVideoEncoderSettings(
                VideoEncoderSettings.Builder()
                    .setBitrate(spec.quality.bitrateBps)
                    .build()
            )
            .build()

        val listener = object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                stopProgressPolling()
                activeTransformer = null
                scope.launch {
                    runCatching {
                        repository.saveExportedClip(
                            sourceFile = outputFile,
                            title = spec.outputTitle.ifBlank { "ClipCraft Short" },
                            aspectRatioLabel = spec.aspectRatioLabel,
                            qualityLabel = spec.quality.badge,
                            durationMs = clippedDurationMs,
                            audioMode = spec.audioMode.label,
                            creatorHandle = spec.creatorHandle.trim(),
                            youtubeTags = spec.youtubeTags.trim()
                        )
                    }.onSuccess { (saved, savedToGallery) ->
                        if (pendingQueue.isNotEmpty()) {
                            mainHandler.post { processNextInQueue() }
                        } else {
                            _exportState.value = ExportState.Success(
                                clipId = saved.id,
                                title = saved.title,
                                contentUri = saved.contentUri,
                                durationMs = saved.durationMs,
                                exportedCount = totalQueueCount,
                                savedToGallery = savedToGallery
                            )
                        }
                    }.onFailure { err ->
                        pendingQueue.clear()
                        _exportState.value = ExportState.Error(
                            err.message ?: "Failed to save exported clip."
                        )
                    }
                }
            }

            override fun onError(
                composition: Composition,
                exportResult: ExportResult,
                exception: ExportException
            ) {
                stopProgressPolling()
                activeTransformer = null
                pendingQueue.clear()
                runCatching { if (outputFile.exists()) outputFile.delete() }
                _exportState.value = ExportState.Error(
                    exception.message ?: "Video export failed (${exception.errorCodeName})."
                )
            }
        }

        val transformer = Transformer.Builder(context)
            .setEncoderFactory(encoderFactory)
            .addListener(listener)
            .build()
        activeTransformer = transformer

        try {
            if (spec.audioMode == AudioTransformMode.REPLACE_AUDIO && spec.replacementAudioUri != null) {
                val audioMediaItem = MediaItem.Builder()
                    .setUri(spec.replacementAudioUri)
                    .build()
                val editedAudioItem = EditedMediaItem.Builder(audioMediaItem).build()

                val videoSequence = EditedMediaItemSequence.withVideoFrom(listOf(editedVideoItem))
                val audioSequence = EditedMediaItemSequence.withAudioFrom(listOf(editedAudioItem))
                    .buildUpon()
                    .setIsLooping(true)
                    .build()

                val composition = Composition.Builder(videoSequence, audioSequence).build()
                transformer.start(composition, outputFile.absolutePath)
            } else {
                transformer.start(editedVideoItem, outputFile.absolutePath)
            }
            startProgressPolling(transformer, spec.outputTitle)
        } catch (e: Exception) {
            stopProgressPolling()
            activeTransformer = null
            pendingQueue.clear()
            _exportState.value = ExportState.Error(e.message ?: "Unable to start export.")
        }
    }

    fun cancelExport() {
        mainHandler.post {
            cancelInternal(resetState = true)
        }
    }

    fun resetExportState() {
        _exportState.value = ExportState.Idle
    }

    private fun cancelInternal(resetState: Boolean) {
        stopProgressPolling()
        pendingQueue.clear()
        runCatching { activeTransformer?.cancel() }
        activeTransformer = null
        if (resetState) {
            _exportState.value = ExportState.Idle
        }
    }

    private fun startProgressPolling(transformer: Transformer, title: String) {
        val progressHolder = ProgressHolder()
        val runnable = object : Runnable {
            override fun run() {
                if (activeTransformer !== transformer) return
                val state = transformer.getProgress(progressHolder)
                if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                    _exportState.value = ExportState.Exporting(
                        progressPercent = progressHolder.progress.coerceIn(0, 100),
                        currentClip = currentQueueIndex,
                        totalClips = totalQueueCount,
                        currentTitle = title
                    )
                }
                if (state != Transformer.PROGRESS_STATE_NOT_STARTED) {
                    mainHandler.postDelayed(this, 250L)
                }
            }
        }
        progressRunnable = runnable
        mainHandler.post(runnable)
    }

    private fun stopProgressPolling() {
        progressRunnable?.let { mainHandler.removeCallbacks(it) }
        progressRunnable = null
    }

    private fun buildVideoEffects(spec: TransformSpec): List<Effect> {
        val effects = mutableListOf<Effect>()

        if (!spec.cropRect.isFullFrame()) {
            val ndcLeft = (spec.cropRect.left * 2f - 1f).coerceIn(-1f, 0.9f)
            val ndcRight = (spec.cropRect.right * 2f - 1f).coerceIn(ndcLeft + 0.1f, 1f)
            val ndcTop = (1f - spec.cropRect.top * 2f).coerceIn(-0.9f, 1f)
            val ndcBottom = (1f - spec.cropRect.bottom * 2f).coerceIn(-1f, ndcTop - 0.1f)
            effects.add(Crop(ndcLeft, ndcRight, ndcBottom, ndcTop))
        }

        if (spec.mirrorHorizontal || spec.rotationDegrees != 0) {
            val scaleBuilder = ScaleAndRotateTransformation.Builder()
            if (spec.mirrorHorizontal) {
                scaleBuilder.setScale(-1f, 1f)
            }
            if (spec.rotationDegrees != 0) {
                scaleBuilder.setRotationDegrees(spec.rotationDegrees.toFloat())
            }
            effects.add(scaleBuilder.build())
        }

        effects.add(Presentation.createForHeight(spec.quality.targetHeight))

        val cleanHandle = spec.creatorHandle.trim()
        val cleanHook = spec.hookCaption.trim()
        if (cleanHandle.isNotEmpty() || cleanHook.isNotEmpty()) {
            val overlayBitmap = createStudioOverlayBitmap(cleanHook, cleanHandle)
            val overlay = BitmapOverlay.createStaticBitmapOverlay(overlayBitmap)
            effects.add(OverlayEffect(listOf(overlay)))
        }

        return effects
    }

    private fun createStudioOverlayBitmap(hookCaption: String, handleText: String): Bitmap {
        val fullWidth = 720
        val fullHeight = 960
        val bitmap = Bitmap.createBitmap(fullWidth, fullHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        if (hookCaption.isNotEmpty()) {
            val hookPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 36f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val padH = 32f
            val padV = 18f
            val textW = hookPaint.measureText(hookCaption).coerceAtMost(fullWidth - 80f)
            val pillW = (textW + padH * 2).coerceAtLeast(200f)
            val pillH = hookPaint.textSize + padV * 2
            val left = (fullWidth - pillW) / 2f
            val top = 46f
            val rect = RectF(left, top, left + pillW, top + pillH)

            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(215, 225, 29, 72) // Vibrant coral-red Shorts hook pill
            }
            canvas.drawRoundRect(rect, 22f, 22f, bgPaint)
            val textY = top + pillH / 2f - (hookPaint.descent() + hookPaint.ascent()) / 2f
            canvas.drawText(hookCaption, left + padH, textY, hookPaint)
        }

        if (handleText.isNotEmpty()) {
            val label = if (handleText.startsWith("@")) handleText else "@$handleText"
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 30f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val paddingH = 26f
            val paddingV = 14f
            val textWidth = paint.measureText(label)
            val width = (textWidth + paddingH * 2).coerceAtLeast(140f)
            val height = (paint.textSize + paddingV * 2).coerceAtLeast(56f)

            val pillLeft = (fullWidth - width) / 2f
            val pillTop = fullHeight - height - 42f
            val rect = RectF(pillLeft, pillTop, pillLeft + width, pillTop + height)

            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(190, 17, 20, 28)
            }
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(220, 45, 212, 191)
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }
            canvas.drawRoundRect(rect, height / 2f, height / 2f, bgPaint)
            canvas.drawRoundRect(rect, height / 2f, height / 2f, strokePaint)

            val textY = pillTop + height / 2f - (paint.descent() + paint.ascent()) / 2f
            canvas.drawText(label, pillLeft + paddingH, textY, paint)
        }

        return bitmap
    }

    private fun resolveDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx)?.substringBeforeLast(".") else null
                    } else null
                }
        }.getOrNull()
    }
}
