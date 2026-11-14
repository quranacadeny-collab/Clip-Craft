package com.example.ui.components

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.media.AudioTransformMode
import com.example.media.CropRect
import com.example.ui.formatDurationMs
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun VideoCropPreview(
    videoUri: Uri,
    videoAspectRatio: Float,
    cropRect: CropRect,
    trimStartMs: Long,
    trimEndMs: Long,
    mirrorHorizontal: Boolean,
    rotationDegrees: Int,
    audioMode: AudioTransformMode,
    creatorHandle: String,
    hookCaption: String,
    isFreeCrop: Boolean,
    onPanCrop: (Float, Float) -> Unit,
    onResizeCropEdges: (left: Float, top: Float, right: Float, bottom: Float) -> Unit,
    onResetCrop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var isMutedPreview by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }

    val exoPlayer = remember(videoUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
        }
    }

    LaunchedEffect(audioMode, isMutedPreview) {
        exoPlayer.volume = if (isMutedPreview || audioMode == AudioTransformMode.MUTE) 0f else 1f
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            isPlaying = exoPlayer.isPlaying
            currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            if (trimEndMs > trimStartMs && exoPlayer.isPlaying) {
                if (currentPositionMs < trimStartMs || currentPositionMs >= trimEndMs) {
                    exoPlayer.seekTo(trimStartMs)
                }
            }
            delay(150L)
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    val safeAspect = videoAspectRatio.coerceIn(0.45f, 2.2f)
    val accentCoral = MaterialTheme.colorScheme.primary
    val accentTeal = MaterialTheme.colorScheme.secondary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Color(0xFF090B10))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            contentAlignment = Alignment.Center
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .aspectRatio(safeAspect)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                val boxWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                val boxHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    update = { view ->
                        if (view.player !== exoPlayer) {
                            view.player = exoPlayer
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = if (mirrorHorizontal) -1f else 1f
                            rotationZ = rotationDegrees.toFloat()
                        }
                )

                // Interactive Crop Overlay
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(isFreeCrop) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val dx = dragAmount.x / boxWidthPx
                                val dy = dragAmount.y / boxHeightPx
                                if (isFreeCrop) {
                                    // Determine if drag started near an edge or inside
                                    val touchX = change.position.x / boxWidthPx
                                    val touchY = change.position.y / boxHeightPx
                                    val edgeThreshold = 0.08f
                                    when {
                                        kotlin.math.abs(touchX - cropRect.left) < edgeThreshold ->
                                            onResizeCropEdges(dx, 0f, 0f, 0f)
                                        kotlin.math.abs(touchX - cropRect.right) < edgeThreshold ->
                                            onResizeCropEdges(0f, 0f, dx, 0f)
                                        kotlin.math.abs(touchY - cropRect.top) < edgeThreshold ->
                                            onResizeCropEdges(0f, dy, 0f, 0f)
                                        kotlin.math.abs(touchY - cropRect.bottom) < edgeThreshold ->
                                            onResizeCropEdges(0f, 0f, 0f, dy)
                                        else -> onPanCrop(dx, dy)
                                    }
                                } else {
                                    onPanCrop(dx, dy)
                                }
                            }
                        }
                ) {
                    val leftPx = cropRect.left * size.width
                    val topPx = cropRect.top * size.height
                    val rightPx = cropRect.right * size.width
                    val bottomPx = cropRect.bottom * size.height
                    val cropW = (rightPx - leftPx).coerceAtLeast(10f)
                    val cropH = (bottomPx - topPx).coerceAtLeast(10f)

                    val scrimColor = Color.Black.copy(alpha = 0.62f)
                    // Top scrim
                    drawRect(scrimColor, Offset.Zero, Size(size.width, topPx))
                    // Bottom scrim
                    drawRect(scrimColor, Offset(0f, bottomPx), Size(size.width, size.height - bottomPx))
                    // Left scrim
                    drawRect(scrimColor, Offset(0f, topPx), Size(leftPx, cropH))
                    // Right scrim
                    drawRect(scrimColor, Offset(rightPx, topPx), Size(size.width - rightPx, cropH))

                    // Rule-of-thirds interior lines
                    val gridStroke = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    )
                    val thirdW = cropW / 3f
                    val thirdH = cropH / 3f
                    for (i in 1..2) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.35f),
                            start = Offset(leftPx + thirdW * i, topPx),
                            end = Offset(leftPx + thirdW * i, bottomPx),
                            strokeWidth = gridStroke.width,
                            pathEffect = gridStroke.pathEffect
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.35f),
                            start = Offset(leftPx, topPx + thirdH * i),
                            end = Offset(rightPx, topPx + thirdH * i),
                            strokeWidth = gridStroke.width,
                            pathEffect = gridStroke.pathEffect
                        )
                    }

                    // Crop outline
                    drawRect(
                        color = accentCoral,
                        topLeft = Offset(leftPx, topPx),
                        size = Size(cropW, cropH),
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Corner L-brackets
                    val cornerLen = 16.dp.toPx().coerceAtMost(cropW / 3f).coerceAtMost(cropH / 3f)
                    val bracketStroke = Stroke(width = 3.5.dp.toPx())
                    val corners = Path().apply {
                        // Top-left
                        moveTo(leftPx, topPx + cornerLen)
                        lineTo(leftPx, topPx)
                        lineTo(leftPx + cornerLen, topPx)
                        // Top-right
                        moveTo(rightPx - cornerLen, topPx)
                        lineTo(rightPx, topPx)
                        lineTo(rightPx, topPx + cornerLen)
                        // Bottom-left
                        moveTo(leftPx, bottomPx - cornerLen)
                        lineTo(leftPx, bottomPx)
                        lineTo(leftPx + cornerLen, bottomPx)
                        // Bottom-right
                        moveTo(rightPx - cornerLen, bottomPx)
                        lineTo(rightPx, bottomPx)
                        lineTo(rightPx, bottomPx - cornerLen)
                    }
                    drawPath(corners, color = accentTeal, style = bracketStroke)
                }

                // Live hook caption banner preview
                if (hookCaption.isNotBlank()) {
                    Surface(
                        color = accentCoral,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                    ) {
                        Text(
                            text = hookCaption.trim(),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }

                // Live creator handle badge preview
                if (creatorHandle.isNotBlank()) {
                    val label = if (creatorHandle.trim().startsWith("@")) {
                        creatorHandle.trim()
                    } else {
                        "@${creatorHandle.trim()}"
                    }
                    Surface(
                        color = Color(0xCC11141C),
                        shape = RoundedCornerShape(50),
                        border = androidx.compose.foundation.BorderStroke(1.dp, accentTeal),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Transport & helper bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        if (exoPlayer.isPlaying) {
                            exoPlayer.pause()
                        } else {
                            if (exoPlayer.currentPosition < trimStartMs || exoPlayer.currentPosition >= trimEndMs) {
                                exoPlayer.seekTo(trimStartMs)
                            }
                            exoPlayer.play()
                        }
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        .testTag("preview_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause preview" else "Play preview",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { isMutedPreview = !isMutedPreview },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("preview_mute_button")
                ) {
                    Icon(
                        imageVector = if (isMutedPreview || audioMode == AudioTransformMode.MUTE) {
                            Icons.AutoMirrored.Filled.VolumeOff
                        } else {
                            Icons.AutoMirrored.Filled.VolumeUp
                        },
                        contentDescription = "Toggle preview audio",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "${formatDurationMs(currentPositionMs)} / ${formatDurationMs(trimEndMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(
                onClick = onResetCrop,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("reset_crop_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CropFree,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Center Crop", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
