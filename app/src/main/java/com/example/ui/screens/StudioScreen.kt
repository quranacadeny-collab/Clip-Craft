package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ai.ShortsTopicSuggestion
import com.example.media.AspectRatioPreset
import com.example.media.AudioTransformMode
import com.example.media.ExportQuality
import com.example.media.ExportState
import com.example.ui.AiTopicsState
import com.example.ui.StudioUiState
import com.example.ui.components.VideoCropPreview
import com.example.ui.formatDurationMs

@Composable
fun StudioScreen(
    studioState: StudioUiState,
    exportState: ExportState,
    onVideoPicked: (Uri) -> Unit,
    onAnalyzeTopics: () -> Unit,
    onApplyTopic: (ShortsTopicSuggestion) -> Unit,
    onApplyAutoYouTubeReady: () -> Unit,
    onExportAllAiTopics: () -> Unit,
    onSelectAspectPreset: (AspectRatioPreset) -> Unit,
    onPanCrop: (Float, Float) -> Unit,
    onResizeCropEdges: (Float, Float, Float, Float) -> Unit,
    onResetCrop: () -> Unit,
    onTrimChanged: (Long, Long) -> Unit,
    onToggleMirror: () -> Unit,
    onRotate90: () -> Unit,
    onAudioModeChanged: (AudioTransformMode) -> Unit,
    onPickReplacementAudio: (Uri) -> Unit,
    onExportQualityChanged: (ExportQuality) -> Unit,
    onCreatorHandleChanged: (String) -> Unit,
    onHookCaptionChanged: (String) -> Unit,
    onOutputTitleChanged: (String) -> Unit,
    onStartExport: () -> Unit,
    onCancelExport: () -> Unit,
    onDismissExportBanner: () -> Unit,
    onNavigateToLinkCheck: () -> Unit,
    onNavigateToExports: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onVideoPicked(uri)
    }

    val documentVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onVideoPicked(uri)
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onPickReplacementAudio(uri)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Studio Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.studio_tagline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (studioState.sourceVideo != null) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("change_video_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = "Change video",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Change")
                    }
                }
            }

            if (studioState.isLoadingVideo) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text("Reading video stream & dimensions...", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else if (studioState.sourceVideo == null) {
                EmptyStudioHero(
                    onPickFromGallery = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    },
                    onBrowseFiles = {
                        documentVideoLauncher.launch(arrayOf("video/*"))
                    },
                    onOpenLinkCheck = onNavigateToLinkCheck
                )
            } else {
                val video = studioState.sourceVideo

                // Live Preview + Draggable Crop Frame
                VideoCropPreview(
                    videoUri = video.uri,
                    videoAspectRatio = video.aspectRatio,
                    cropRect = studioState.cropRect,
                    trimStartMs = studioState.trimStartMs,
                    trimEndMs = studioState.trimEndMs,
                    mirrorHorizontal = studioState.mirrorHorizontal,
                    rotationDegrees = studioState.rotationDegrees,
                    audioMode = studioState.audioMode,
                    creatorHandle = studioState.creatorHandle,
                    hookCaption = studioState.hookCaption,
                    isFreeCrop = studioState.aspectPreset == AspectRatioPreset.FREE,
                    onPanCrop = onPanCrop,
                    onResizeCropEdges = onResizeCropEdges,
                    onResetCrop = onResetCrop
                )

                // AI Auto-Shorts Topic Finder + One-Tap Auto YouTube-Ready Card
                AiShortsGeneratorCard(
                    aiTopicsState = studioState.aiTopicsState,
                    selectedTopicId = studioState.selectedTopicId,
                    isExporting = exportState is ExportState.Exporting,
                    onAnalyzeTopics = onAnalyzeTopics,
                    onApplyTopic = onApplyTopic,
                    onApplyAutoYouTubeReady = onApplyAutoYouTubeReady,
                    onExportAllAiTopics = onExportAllAiTopics
                )

                // Aspect Ratio Selector Strip
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Crop,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Crop Aspect Ratio (Drag frame to reframe)",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AspectRatioPreset.entries.forEach { preset ->
                            val selected = studioState.aspectPreset == preset
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectAspectPreset(preset) },
                                label = { Text(preset.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("aspect_chip_${preset.name.lowercase()}")
                            )
                        }
                    }
                }

                // Trim Range Slider Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCut,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text("Trim Clip", style = MaterialTheme.typography.titleSmall)
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "Length ${formatDurationMs(studioState.trimEndMs - studioState.trimStartMs)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        val maxDuration = video.durationMs.toFloat().coerceAtLeast(1000f)
                        RangeSlider(
                            value = studioState.trimStartMs.toFloat()..studioState.trimEndMs.toFloat(),
                            onValueChange = { range ->
                                onTrimChanged(range.start.toLong(), range.endInclusive.toLong())
                            },
                            valueRange = 0f..maxDuration,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.secondary,
                                activeTrackColor = MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier.testTag("trim_range_slider")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Start ${formatDurationMs(studioState.trimStartMs)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "End ${formatDurationMs(studioState.trimEndMs)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Copyright-Safe Audio, Quality & Creator Transform Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Audio, Quality & Creator Transform",
                            style = MaterialTheme.typography.titleSmall
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AudioTransformMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = studioState.audioMode == mode,
                                    onClick = { onAudioModeChanged(mode) },
                                    label = { Text(mode.label) },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("audio_mode_${mode.name.lowercase()}")
                                )
                            }
                        }

                        if (studioState.audioMode == AudioTransformMode.REPLACE_AUDIO) {
                            OutlinedButton(
                                onClick = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("pick_replacement_audio_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AudioFile,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = studioState.replacementAudioName
                                        ?: "Choose Voiceover or Royalty-Free Audio",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Export Quality Selector (1080p vs 720p)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HighQuality,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("Gallery Export Quality:", style = MaterialTheme.typography.labelMedium)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ExportQuality.entries.forEach { quality ->
                                FilterChip(
                                    selected = studioState.exportQuality == quality,
                                    onClick = { onExportQualityChanged(quality) },
                                    label = { Text(quality.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("quality_chip_${quality.badge}")
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilterChip(
                                selected = studioState.mirrorHorizontal,
                                onClick = onToggleMirror,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Flip,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text("Mirror Flip") },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("mirror_flip_chip")
                            )

                            OutlinedButton(
                                onClick = onRotate90,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("rotate_90_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.RotateRight,
                                    contentDescription = "Rotate 90 degrees",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Rotate (${studioState.rotationDegrees}°)")
                            }
                        }

                        OutlinedTextField(
                            value = studioState.hookCaption,
                            onValueChange = onHookCaptionChanged,
                            label = { Text("Top Hook Banner Text (Optional)") },
                            placeholder = { Text("e.g. Watch This Tip") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("hook_caption_input")
                        )

                        OutlinedTextField(
                            value = studioState.creatorHandle,
                            onValueChange = onCreatorHandleChanged,
                            label = { Text("Creator Handle Watermark (Optional)") },
                            placeholder = { Text("@YourChannelName") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("creator_handle_input")
                        )

                        OutlinedTextField(
                            value = studioState.outputTitle,
                            onValueChange = onOutputTitleChanged,
                            label = { Text("Export Clip Title") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("export_title_input")
                        )
                    }
                }

                // Export Status & Action Area
                ExportActionSection(
                    exportState = exportState,
                    qualityBadge = studioState.exportQuality.badge,
                    replaceAudioMissing = studioState.audioMode == AudioTransformMode.REPLACE_AUDIO &&
                        studioState.replacementAudioUri == null,
                    onStartExport = onStartExport,
                    onCancelExport = onCancelExport,
                    onDismissExportBanner = onDismissExportBanner,
                    onShareUri = { uriString ->
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "video/mp4"
                            putExtra(Intent.EXTRA_STREAM, Uri.parse(uriString))
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share to YouTube or Apps"))
                    },
                    onViewExports = onNavigateToExports
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AiShortsGeneratorCard(
    aiTopicsState: AiTopicsState,
    selectedTopicId: String?,
    isExporting: Boolean,
    onAnalyzeTopics: () -> Unit,
    onApplyTopic: (ShortsTopicSuggestion) -> Unit,
    onApplyAutoYouTubeReady: () -> Unit,
    onExportAllAiTopics: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Column {
                        Text(
                            text = "AI Auto-Shorts & Transform",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Detect main topics & prep 9:16 YouTube Shorts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAnalyzeTopics,
                    enabled = aiTopicsState !is AiTopicsState.Analyzing && !isExporting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("ai_detect_topics_button")
                ) {
                    if (aiTopicsState is AiTopicsState.Analyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scanning...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Find Main Topics")
                    }
                }

                OutlinedButton(
                    onClick = onApplyAutoYouTubeReady,
                    enabled = !isExporting,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("auto_youtube_ready_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Auto Prep")
                }
            }

            when (aiTopicsState) {
                is AiTopicsState.Idle -> Unit
                is AiTopicsState.Analyzing -> {
                    Text(
                        text = "AI is analyzing video frames to pick the strongest Shorts highlights...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                is AiTopicsState.Error -> {
                    Text(
                        text = aiTopicsState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                is AiTopicsState.Ready -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        aiTopicsState.topics.forEach { topic ->
                            val isSelected = topic.id == selectedTopicId
                            Surface(
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onApplyTopic(topic) }
                                    .testTag("ai_topic_card_${topic.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = topic.topicTitle,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "${formatDurationMs(topic.startMs)} - ${formatDurationMs(topic.endMs)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    Text(
                                        text = "Hook: \"${topic.hookCaption}\" • ${topic.youtubeTags}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = topic.reason,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = onExportAllAiTopics,
                            enabled = !isExporting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("export_all_ai_topics_button")
                        ) {
                            Icon(Icons.Default.MovieCreation, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export All ${aiTopicsState.topics.size} AI Shorts to Gallery")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStudioHero(
    onPickFromGallery: () -> Unit,
    onBrowseFiles: () -> Unit,
    onOpenLinkCheck: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(185.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_studio_banner_1794683954778),
                        contentDescription = "ClipCraft dark creator studio banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xEE151821)
                                    )
                                )
                            )
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "AI SHORTS & 1080P STUDIO",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Auto-Cut Long Videos into Shorts",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Pick any long video you filmed or have rights to use. Gemini AI finds the main highlight topics, reframes to 9:16 Shorts, adds hook captions, swaps audio, and exports 1080p MP4 directly to your phone Gallery.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onPickFromGallery,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("pick_video_gallery_button")
                    ) {
                        Icon(Icons.Default.MovieCreation, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.pick_video_cta))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onBrowseFiles,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("browse_video_files_button")
                        ) {
                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Files")
                        }

                        OutlinedButton(
                            onClick = onOpenLinkCheck,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("goto_link_check_button")
                        ) {
                            Icon(
                                Icons.Default.Link,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Link Check")
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "How Auto-Shorts Works",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                WorkflowStepRow("1", "Select your long video from Gallery or Files.")
                WorkflowStepRow("2", "Tap 'Find Main Topics' to auto-detect Shorts clips + Hook Captions.")
                WorkflowStepRow("3", "Mute/Replace copyrighted music & Export 1080p directly to Gallery.")
            }
        }
    }
}

@Composable
private fun WorkflowStepRow(number: String, text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ExportActionSection(
    exportState: ExportState,
    qualityBadge: String,
    replaceAudioMissing: Boolean,
    onStartExport: () -> Unit,
    onCancelExport: () -> Unit,
    onDismissExportBanner: () -> Unit,
    onShareUri: (String) -> Unit,
    onViewExports: () -> Unit
) {
    when (exportState) {
        is ExportState.Idle -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (replaceAudioMissing) {
                    Text(
                        text = "Select an audio file above or switch Audio Mode to Mute / Keep Original.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                Button(
                    onClick = onStartExport,
                    enabled = !replaceAudioMissing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("export_video_button")
                ) {
                    Icon(Icons.Default.MovieCreation, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export $qualityBadge MP4 to Gallery")
                }
            }
        }

        is ExportState.Exporting -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (exportState.totalClips > 1) {
                                "Rendering Clip ${exportState.currentClip}/${exportState.totalClips}..."
                            } else {
                                "Rendering $qualityBadge MP4..."
                            },
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "${exportState.progressPercent}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    if (exportState.currentTitle.isNotBlank()) {
                        Text(
                            text = exportState.currentTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    LinearProgressIndicator(
                        progress = { exportState.progressPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50)),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    OutlinedButton(
                        onClick = onCancelExport,
                        modifier = Modifier
                            .align(Alignment.End)
                            .minimumInteractiveComponentSize()
                            .testTag("cancel_export_button")
                    ) {
                        Text(stringResource(R.string.cancel_export))
                    }
                }
            }
        }

        is ExportState.Success -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = if (exportState.savedToGallery) {
                                    "Saved to Phone Gallery!"
                                } else {
                                    "Export Complete!"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        IconButton(
                            onClick = onDismissExportBanner,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    Text(
                        text = if (exportState.exportedCount > 1) {
                            "${exportState.exportedCount} Shorts clips saved to Movies/ClipCraft"
                        } else {
                            "${exportState.title} • ${formatDurationMs(exportState.durationMs)} • Movies/ClipCraft"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onShareUri(exportState.contentUri) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("share_exported_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.share_to_youtube))
                        }

                        OutlinedButton(
                            onClick = onViewExports,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("view_exports_button")
                        ) {
                            Text("Library")
                        }
                    }
                }
            }
        }

        is ExportState.Error -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Export Issue",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = exportState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    TextButton(
                        onClick = onDismissExportBanner,
                        modifier = Modifier
                            .align(Alignment.End)
                            .minimumInteractiveComponentSize()
                    ) {
                        Text("Dismiss")
                    }
                }
            }
        }
    }
}
