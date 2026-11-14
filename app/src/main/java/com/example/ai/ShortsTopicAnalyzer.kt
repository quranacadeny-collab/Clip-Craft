package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.Locale

data class ShortsTopicSuggestion(
    val id: String,
    val topicTitle: String,
    val hookCaption: String,
    val startMs: Long,
    val endMs: Long,
    val youtubeTitle: String,
    val youtubeTags: String,
    val reason: String
)

class ShortsTopicAnalyzer(private val context: Context) {

    private val topicSchema = Schema.obj(
        properties = mapOf(
            "topics" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "topicTitle" to Schema.string(description = "Short descriptive name of this main topic"),
                        "hookCaption" to Schema.string(description = "2 to 5 word viral hook text to show on the video"),
                        "startSec" to Schema.integer(description = "Start timestamp in whole seconds"),
                        "endSec" to Schema.integer(description = "End timestamp in whole seconds (10 to 58 seconds after startSec)"),
                        "youtubeTitle" to Schema.string(description = "Catchy YouTube Shorts title under 70 chars"),
                        "youtubeTags" to Schema.string(description = "3 to 5 relevant hashtags separated by spaces"),
                        "reason" to Schema.string(description = "One sentence explaining why this moment makes a strong Short")
                    )
                ),
                description = "List of 2 to 4 best highlight Shorts segments extracted from the video"
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = topicSchema
            }
        )
    }

    suspend fun analyzeVideoForShorts(
        videoUri: Uri,
        videoTitle: String,
        durationMs: Long
    ): Result<List<ShortsTopicSuggestion>> = withContext(Dispatchers.IO) {
        try {
            val totalSeconds = (durationMs / 1000L).coerceAtLeast(5L)
            val sampledFrames = extractSampledFrames(videoUri, durationMs, maxFrames = 8)

            val response = withTimeout(60_000L) {
                model.generateContent(
                    content {
                        text(
                            """
                            You are an expert YouTube Shorts editor.
                            Analyze these evenly spaced keyframes from a video titled "$videoTitle" (total duration: $totalSeconds seconds).
                            Identify 2 to 4 distinct main topic highlight segments that would make engaging standalone YouTube Shorts.
                            Rules:
                            - Keep every segment strictly within 0 to $totalSeconds seconds.
                            - Each segment should be between 10 and 58 seconds long (or the full video if the video is shorter than 15 seconds).
                            - Segments must not heavily overlap; pick distinct moments across the timeline.
                            - Provide concise hookCaption (2-5 words) suitable for an on-screen banner, plus an SEO YouTube Shorts title and hashtags.
                            """.trimIndent()
                        )
                        sampledFrames.forEach { (timestampSec, bitmap) ->
                            text("Keyframe at ${formatSec(timestampSec)} ($timestampSec s):")
                            image(bitmap)
                        }
                    }
                )
            }

            sampledFrames.forEach { (_, bmp) -> runCatching { bmp.recycle() } }

            val jsonText = response.text
                ?: return@withContext Result.failure(IllegalStateException("AI returned an empty response."))

            val root = JSONObject(jsonText)
            val array = root.getJSONArray("topics")
            val parsed = mutableListOf<ShortsTopicSuggestion>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val rawStartSec = obj.optLong("startSec", 0L).coerceIn(0L, (totalSeconds - 2L).coerceAtLeast(0L))
                val minEndSec = (rawStartSec + 5L).coerceAtMost(totalSeconds)
                val rawEndSec = obj.optLong("endSec", (rawStartSec + 25L).coerceAtMost(totalSeconds))
                    .coerceIn(minEndSec, totalSeconds)

                val startMs = (rawStartSec * 1000L).coerceIn(0L, (durationMs - 1000L).coerceAtLeast(0L))
                val endMs = (rawEndSec * 1000L).coerceIn(startMs + 1000L, durationMs)

                parsed.add(
                    ShortsTopicSuggestion(
                        id = "topic_${i}_${startMs}",
                        topicTitle = obj.optString("topicTitle", "Highlight #${i + 1}").ifBlank { "Highlight #${i + 1}" },
                        hookCaption = obj.optString("hookCaption", "Watch This Moment").take(36),
                        startMs = startMs,
                        endMs = endMs,
                        youtubeTitle = obj.optString("youtubeTitle", "$videoTitle #Shorts").take(80),
                        youtubeTags = obj.optString("youtubeTags", "#shorts #viral #youtube"),
                        reason = obj.optString("reason", "Strong visual highlight segment.")
                    )
                )
            }

            if (parsed.isEmpty()) {
                Result.failure(IllegalStateException("No highlight topics detected. Try another video."))
            } else {
                Result.success(parsed)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI topic analysis timed out", e)
            Result.failure(Exception("AI analysis timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI topic analysis failed", e)
            Result.failure(Exception(e.localizedMessage ?: "AI analysis failed."))
        }
    }

    private fun extractSampledFrames(
        uri: Uri,
        durationMs: Long,
        maxFrames: Int
    ): List<Pair<Long, Bitmap>> {
        val frames = mutableListOf<Pair<Long, Bitmap>>()
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val safeDuration = durationMs.coerceAtLeast(2_000L)
            val count = maxFrames.coerceIn(3, 8)
            for (i in 0 until count) {
                val fraction = (i + 0.5f) / count.toFloat()
                val timeMs = (safeDuration * fraction).toLong().coerceIn(0L, safeDuration - 100L)
                val rawBmp = retriever.getFrameAtTime(
                    timeMs * 1000L,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )
                if (rawBmp != null) {
                    val scaled = scaleDownBitmap(rawBmp, maxWidth = 480)
                    if (scaled !== rawBmp) rawBmp.recycle()
                    frames.add((timeMs / 1000L) to scaled)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not sample all frames", e)
        } finally {
            runCatching { retriever.release() }
        }
        return frames
    }

    private fun scaleDownBitmap(source: Bitmap, maxWidth: Int): Bitmap {
        if (source.width <= maxWidth) return source
        val ratio = maxWidth.toFloat() / source.width.toFloat()
        val targetH = (source.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, maxWidth, targetH, true)
    }

    private fun formatSec(sec: Long): String {
        val m = sec / 60L
        val s = sec % 60L
        return String.format(Locale.US, "%02d:%02d", m, s)
    }

    companion object {
        private const val TAG = "ShortsTopicAnalyzer"
    }
}
