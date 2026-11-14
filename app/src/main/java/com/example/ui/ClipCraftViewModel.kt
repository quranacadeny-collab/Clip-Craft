package com.example.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.ShortsTopicAnalyzer
import com.example.ai.ShortsTopicSuggestion
import com.example.data.local.ExportedClipEntity
import com.example.data.model.YouTubeInspectionResult
import com.example.data.repository.ClipRepository
import com.example.media.AspectRatioPreset
import com.example.media.AudioTransformMode
import com.example.media.CropRect
import com.example.media.ExportQuality
import com.example.media.ExportState
import com.example.media.TransformSpec
import com.example.media.VideoSourceMetadata
import com.example.media.VideoTransformManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AiTopicsState {
    data object Idle : AiTopicsState
    data object Analyzing : AiTopicsState
    data class Ready(val topics: List<ShortsTopicSuggestion>) : AiTopicsState
    data class Error(val message: String) : AiTopicsState
}

data class MonetizationCheckItem(
    val id: String,
    val titleEn: String,
    val titleBn: String,
    val subtitleEn: String,
    val subtitleBn: String,
    val isChecked: Boolean = false
)

data class StudioUiState(
    val sourceVideo: VideoSourceMetadata? = null,
    val isLoadingVideo: Boolean = false,
    val aspectPreset: AspectRatioPreset = AspectRatioPreset.SHORTS_9_16,
    val cropRect: CropRect = CropRect(),
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L,
    val mirrorHorizontal: Boolean = false,
    val rotationDegrees: Int = 0,
    val audioMode: AudioTransformMode = AudioTransformMode.KEEP_ORIGINAL,
    val replacementAudioUri: Uri? = null,
    val replacementAudioName: String? = null,
    val creatorHandle: String = "",
    val hookCaption: String = "",
    val outputTitle: String = "",
    val youtubeTags: String = "",
    val exportQuality: ExportQuality = ExportQuality.FULL_HD_1080P,
    val aiTopicsState: AiTopicsState = AiTopicsState.Idle,
    val selectedTopicId: String? = null
)

data class LinkCheckUiState(
    val urlInput: String = "",
    val isInspecting: Boolean = false,
    val errorMessage: String? = null,
    val inspectedVideo: YouTubeInspectionResult? = null,
    val isBanglaMode: Boolean = false,
    val checklist: List<MonetizationCheckItem> = defaultChecklist()
) {
    val completedChecks: Int get() = checklist.count { it.isChecked }
    val totalChecks: Int get() = checklist.size

    fun buildAttributionText(): String {
        val video = inspectedVideo ?: return ""
        return buildString {
            appendLine("Source / Reference Video: ${video.title}")
            appendLine("Original Creator: ${video.authorName}")
            appendLine("Original Link: ${video.canonicalUrl}")
            appendLine("License / Permission: Creative Commons / Fair Use Transformative Commentary")
        }.trim()
    }

    companion object {
        fun defaultChecklist() = listOf(
            MonetizationCheckItem(
                id = "rights",
                titleEn = "Original, CC-BY, or Licensed Source",
                titleBn = "নিজের ভিডিও অথবা CC-BY / অনুমতি নেওয়া সোর্স",
                subtitleEn = "Only edit footage you filmed, own, or have permission/Creative Commons license to reuse.",
                subtitleBn = "শুধু নিজের তৈরি ভিডিও, Creative Commons (CC-BY), বা অনুমতি পাওয়া ক্লিপ ব্যবহার করুন।"
            ),
            MonetizationCheckItem(
                id = "audio",
                titleEn = "Copyrighted Audio Muted or Replaced",
                titleBn = "কপিরাইট গান/অডিও মিউট বা পরিবর্তন করা হয়েছে",
                subtitleEn = "Content ID catches background music even in cropped clips. Use Voiceover or YouTube Audio Library.",
                subtitleBn = "ক্রপ করলেও YouTube Content ID ব্যাকগ্রাউন্ড মিউজিক ধরে ফেলে। নিজের ভয়েস বা ফ্রি মিউজিক দিন।"
            ),
            MonetizationCheckItem(
                id = "transform",
                titleEn = "Substantial Original Value Added",
                titleBn = "নিজস্ব কমেন্টারি, রিঅ্যাকশন বা এডিট যোগ করা হয়েছে",
                subtitleEn = "YouTube Reused Content policy requires commentary, educational narration, or clear transformation.",
                subtitleBn = "শুধু ক্রপ করলে Reused Content পলিসিতে মনিটাইজেশন বন্ধ হয়—নিজস্ব ভয়েস, টেক্সট বা বিশ্লেষণ যোগ করুন।"
            ),
            MonetizationCheckItem(
                id = "watermark",
                titleEn = "Creator Handle & Clean 9:16 Framing",
                titleBn = "নিজস্ব চ্যানেল হ্যান্ডেল এবং পরিষ্কার 9:16 ফ্রেম",
                subtitleEn = "No third-party TikTok/Reels watermarks; clean vertical framing with your own channel handle.",
                subtitleBn = "অন্য অ্যাপের ওয়াটারমার্ক রাখবেন না; পরিষ্কার 9:16 ফ্রেমে নিজের @ChannelName যোগ করুন।"
            ),
            MonetizationCheckItem(
                id = "credit",
                titleEn = "Proper Attribution in Description",
                titleBn = "ডেসক্রিপশনে সঠিক ক্রেডিট (Attribution) দেওয়া হয়েছে",
                subtitleEn = "Credit the original creator when using CC-BY or transformative commentary clips.",
                subtitleBn = "CC-BY বা ফেয়ার-ইউজ ক্লিপ ব্যবহার করলে ডেসক্রিপশনে মূল ক্রিয়েটরের নাম ও লিংক দিন।"
            )
        )
    }
}

class ClipCraftViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ClipRepository(application.applicationContext)
    private val topicAnalyzer = ShortsTopicAnalyzer(application.applicationContext)
    private val transformManager = VideoTransformManager(
        context = application.applicationContext,
        repository = repository,
        scope = viewModelScope
    )

    private val _studioState = MutableStateFlow(StudioUiState())
    val studioState: StateFlow<StudioUiState> = _studioState.asStateFlow()

    private val _linkCheckState = MutableStateFlow(LinkCheckUiState())
    val linkCheckState: StateFlow<LinkCheckUiState> = _linkCheckState.asStateFlow()

    val exportState: StateFlow<ExportState> = transformManager.exportState

    val exportedClips: StateFlow<List<ExportedClipEntity>> =
        repository.observeExportedClips()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    fun selectVideoUri(uri: Uri) {
        viewModelScope.launch {
            _studioState.update { it.copy(isLoadingVideo = true) }
            val meta = transformManager.loadVideoMetadata(uri)
            val initialPreset = AspectRatioPreset.SHORTS_9_16
            val initialCrop = computeCenteredCrop(meta.aspectRatio, initialPreset.ratio)
            _studioState.update {
                it.copy(
                    sourceVideo = meta,
                    isLoadingVideo = false,
                    aspectPreset = initialPreset,
                    cropRect = initialCrop,
                    trimStartMs = 0L,
                    trimEndMs = meta.durationMs,
                    mirrorHorizontal = false,
                    rotationDegrees = 0,
                    hookCaption = "",
                    youtubeTags = "#shorts #youtube",
                    outputTitle = "${meta.displayName}_Short",
                    aiTopicsState = AiTopicsState.Idle,
                    selectedTopicId = null
                )
            }
            transformManager.resetExportState()
        }
    }

    fun analyzeVideoTopicsWithAi() {
        val source = _studioState.value.sourceVideo ?: return
        viewModelScope.launch {
            _studioState.update { it.copy(aiTopicsState = AiTopicsState.Analyzing) }
            val result = topicAnalyzer.analyzeVideoForShorts(
                videoUri = source.uri,
                videoTitle = source.displayName,
                durationMs = source.durationMs
            )
            result.onSuccess { topics ->
                _studioState.update {
                    it.copy(aiTopicsState = AiTopicsState.Ready(topics))
                }
                topics.firstOrNull()?.let { first ->
                    applyTopicSuggestion(first)
                }
            }.onFailure { err ->
                _studioState.update {
                    it.copy(
                        aiTopicsState = AiTopicsState.Error(
                            err.message ?: "Failed to detect video topics."
                        )
                    )
                }
            }
        }
    }

    fun applyTopicSuggestion(topic: ShortsTopicSuggestion) {
        val meta = _studioState.value.sourceVideo ?: return
        val shortsCrop = computeCenteredCrop(meta.aspectRatio, AspectRatioPreset.SHORTS_9_16.ratio)
        _studioState.update { state ->
            state.copy(
                selectedTopicId = topic.id,
                aspectPreset = AspectRatioPreset.SHORTS_9_16,
                cropRect = shortsCrop,
                trimStartMs = topic.startMs.coerceIn(0L, meta.durationMs),
                trimEndMs = topic.endMs.coerceIn(topic.startMs + 500L, meta.durationMs),
                hookCaption = topic.hookCaption,
                outputTitle = topic.youtubeTitle,
                youtubeTags = topic.youtubeTags
            )
        }
    }

    fun applyAutoYouTubeReadyPreset() {
        val meta = _studioState.value.sourceVideo ?: return
        val shortsCrop = computeCenteredCrop(meta.aspectRatio, AspectRatioPreset.SHORTS_9_16.ratio)
        _studioState.update { state ->
            val nextAudioMode = if (state.replacementAudioUri != null) {
                AudioTransformMode.REPLACE_AUDIO
            } else if (state.audioMode == AudioTransformMode.KEEP_ORIGINAL) {
                AudioTransformMode.MUTE
            } else {
                state.audioMode
            }
            state.copy(
                aspectPreset = AspectRatioPreset.SHORTS_9_16,
                cropRect = shortsCrop,
                exportQuality = ExportQuality.FULL_HD_1080P,
                audioMode = nextAudioMode,
                mirrorHorizontal = true,
                hookCaption = state.hookCaption.ifBlank { "Highlight Clip" },
                youtubeTags = state.youtubeTags.ifBlank { "#shorts #trending #youtube" }
            )
        }
    }

    fun selectAspectPreset(preset: AspectRatioPreset) {
        val meta = _studioState.value.sourceVideo
        val videoAspect = meta?.aspectRatio ?: (16f / 9f)
        val updatedCrop = if (preset.ratio == null) {
            _studioState.value.cropRect
        } else {
            computeCenteredCrop(videoAspect, preset.ratio)
        }
        _studioState.update {
            it.copy(aspectPreset = preset, cropRect = updatedCrop)
        }
    }

    fun panCrop(deltaXFraction: Float, deltaYFraction: Float) {
        _studioState.update { state ->
            val rect = state.cropRect
            val w = rect.width
            val h = rect.height
            val newLeft = (rect.left + deltaXFraction).coerceIn(0f, 1f - w)
            val newTop = (rect.top + deltaYFraction).coerceIn(0f, 1f - h)
            state.copy(
                cropRect = CropRect(
                    left = newLeft,
                    top = newTop,
                    right = newLeft + w,
                    bottom = newTop + h
                )
            )
        }
    }

    fun updateCropEdges(
        leftDelta: Float = 0f,
        topDelta: Float = 0f,
        rightDelta: Float = 0f,
        bottomDelta: Float = 0f
    ) {
        _studioState.update { state ->
            val cur = state.cropRect
            val minSize = 0.2f
            val newLeft = (cur.left + leftDelta).coerceIn(0f, cur.right - minSize)
            val newTop = (cur.top + topDelta).coerceIn(0f, cur.bottom - minSize)
            val newRight = (cur.right + rightDelta).coerceIn(newLeft + minSize, 1f)
            val newBottom = (cur.bottom + bottomDelta).coerceIn(newTop + minSize, 1f)
            state.copy(
                aspectPreset = AspectRatioPreset.FREE,
                cropRect = CropRect(newLeft, newTop, newRight, newBottom)
            )
        }
    }

    fun resetCrop() {
        val videoAspect = _studioState.value.sourceVideo?.aspectRatio ?: (16f / 9f)
        val preset = _studioState.value.aspectPreset
        _studioState.update {
            it.copy(cropRect = computeCenteredCrop(videoAspect, preset.ratio))
        }
    }

    fun updateTrimRange(startMs: Long, endMs: Long) {
        val duration = _studioState.value.sourceVideo?.durationMs ?: return
        val clampedStart = startMs.coerceIn(0L, (duration - 500L).coerceAtLeast(0L))
        val clampedEnd = endMs.coerceIn(clampedStart + 500L, duration)
        _studioState.update {
            it.copy(trimStartMs = clampedStart, trimEndMs = clampedEnd)
        }
    }

    fun toggleMirror() {
        _studioState.update { it.copy(mirrorHorizontal = !it.mirrorHorizontal) }
    }

    fun rotate90() {
        _studioState.update { it.copy(rotationDegrees = (it.rotationDegrees + 90) % 360) }
    }

    fun setAudioMode(mode: AudioTransformMode) {
        _studioState.update { it.copy(audioMode = mode) }
    }

    fun selectReplacementAudio(uri: Uri) {
        val name = resolveFileName(uri) ?: "Custom Audio Track"
        _studioState.update {
            it.copy(
                audioMode = AudioTransformMode.REPLACE_AUDIO,
                replacementAudioUri = uri,
                replacementAudioName = name
            )
        }
    }

    fun setExportQuality(quality: ExportQuality) {
        _studioState.update { it.copy(exportQuality = quality) }
    }

    fun updateCreatorHandle(handle: String) {
        _studioState.update { it.copy(creatorHandle = handle.take(28)) }
    }

    fun updateHookCaption(caption: String) {
        _studioState.update { it.copy(hookCaption = caption.take(40)) }
    }

    fun updateOutputTitle(title: String) {
        _studioState.update { it.copy(outputTitle = title.take(70)) }
    }

    fun startExport() {
        val state = _studioState.value
        val source = state.sourceVideo ?: return
        transformManager.startExport(
            TransformSpec(
                videoUri = source.uri,
                outputTitle = state.outputTitle.ifBlank { "${source.displayName}_ClipCraft" },
                cropRect = state.cropRect,
                aspectRatioLabel = state.aspectPreset.badge,
                trimStartMs = state.trimStartMs,
                trimEndMs = state.trimEndMs,
                mirrorHorizontal = state.mirrorHorizontal,
                rotationDegrees = state.rotationDegrees,
                audioMode = state.audioMode,
                replacementAudioUri = state.replacementAudioUri,
                creatorHandle = state.creatorHandle,
                hookCaption = state.hookCaption,
                youtubeTags = state.youtubeTags,
                quality = state.exportQuality
            )
        )
    }

    fun exportAllAiTopics() {
        val state = _studioState.value
        val source = state.sourceVideo ?: return
        val topicsState = state.aiTopicsState as? AiTopicsState.Ready ?: return
        if (topicsState.topics.isEmpty()) return

        val shortsCrop = computeCenteredCrop(source.aspectRatio, AspectRatioPreset.SHORTS_9_16.ratio)
        val specs = topicsState.topics.map { topic ->
            TransformSpec(
                videoUri = source.uri,
                outputTitle = topic.youtubeTitle.ifBlank { topic.topicTitle },
                cropRect = shortsCrop,
                aspectRatioLabel = AspectRatioPreset.SHORTS_9_16.badge,
                trimStartMs = topic.startMs,
                trimEndMs = topic.endMs,
                mirrorHorizontal = state.mirrorHorizontal,
                rotationDegrees = state.rotationDegrees,
                audioMode = state.audioMode,
                replacementAudioUri = state.replacementAudioUri,
                creatorHandle = state.creatorHandle,
                hookCaption = topic.hookCaption,
                youtubeTags = topic.youtubeTags,
                quality = state.exportQuality
            )
        }
        transformManager.startBatchExport(specs)
    }

    fun cancelExport() {
        transformManager.cancelExport()
    }

    fun dismissExportBanner() {
        transformManager.resetExportState()
    }

    fun updateUrlInput(url: String) {
        _linkCheckState.update { it.copy(urlInput = url, errorMessage = null) }
    }

    fun inspectYouTubeUrl() {
        val raw = _linkCheckState.value.urlInput.trim()
        if (raw.isEmpty()) {
            _linkCheckState.update { it.copy(errorMessage = "Paste a YouTube video or Shorts URL first.") }
            return
        }
        viewModelScope.launch {
            _linkCheckState.update { it.copy(isInspecting = true, errorMessage = null) }
            val result = repository.inspectYouTubeLink(raw)
            result.onSuccess { meta ->
                _linkCheckState.update {
                    it.copy(isInspecting = false, inspectedVideo = meta, errorMessage = null)
                }
            }.onFailure { err ->
                _linkCheckState.update {
                    it.copy(
                        isInspecting = false,
                        errorMessage = err.message ?: "Could not inspect this YouTube link."
                    )
                }
            }
        }
    }

    fun toggleLanguageMode() {
        _linkCheckState.update { it.copy(isBanglaMode = !it.isBanglaMode) }
    }

    fun toggleChecklistItem(id: String) {
        _linkCheckState.update { state ->
            state.copy(
                checklist = state.checklist.map { item ->
                    if (item.id == id) item.copy(isChecked = !item.isChecked) else item
                }
            )
        }
    }

    fun deleteExportedClip(clip: ExportedClipEntity) {
        viewModelScope.launch {
            repository.deleteClip(clip)
        }
    }

    private fun computeCenteredCrop(videoAspect: Float, targetAspect: Float?): CropRect {
        if (targetAspect == null || videoAspect <= 0f) return CropRect()
        return if (videoAspect > targetAspect) {
            val cropW = (targetAspect / videoAspect).coerceIn(0.15f, 1f)
            val left = (1f - cropW) / 2f
            CropRect(left = left, top = 0f, right = left + cropW, bottom = 1f)
        } else {
            val cropH = (videoAspect / targetAspect).coerceIn(0.15f, 1f)
            val top = (1f - cropH) / 2f
            CropRect(left = 0f, top = top, right = 1f, bottom = top + cropH)
        }
    }

    private fun resolveFileName(uri: Uri): String? {
        return runCatching {
            getApplication<Application>().contentResolver
                .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx)?.substringBeforeLast(".") else null
                    } else null
                }
        }.getOrNull()
    }
}
