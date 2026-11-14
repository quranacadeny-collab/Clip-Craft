package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class YouTubeOEmbedResponse(
    @Json(name = "title") val title: String = "",
    @Json(name = "author_name") val authorName: String = "",
    @Json(name = "author_url") val authorUrl: String = "",
    @Json(name = "thumbnail_url") val thumbnailUrl: String = "",
    @Json(name = "width") val width: Int = 0,
    @Json(name = "height") val height: Int = 0,
    @Json(name = "provider_name") val providerName: String = "YouTube"
)

data class YouTubeInspectionResult(
    val videoId: String,
    val canonicalUrl: String,
    val title: String,
    val authorName: String,
    val authorUrl: String,
    val thumbnailUrl: String,
    val isVerticalShorts: Boolean
)

object YouTubeUrlParser {
    private val patterns = listOf(
        Regex("(?:https?://)?(?:www\\.|m\\.)?youtube\\.com/watch\\?(?:.*&)?v=([A-Za-z0-9_-]{11})"),
        Regex("(?:https?://)?youtu\\.be/([A-Za-z0-9_-]{11})"),
        Regex("(?:https?://)?(?:www\\.|m\\.)?youtube\\.com/shorts/([A-Za-z0-9_-]{11})"),
        Regex("(?:https?://)?(?:www\\.|m\\.)?youtube\\.com/embed/([A-Za-z0-9_-]{11})")
    )

    fun extractVideoId(rawUrl: String): String? {
        val trimmed = rawUrl.trim()
        for (regex in patterns) {
            val match = regex.find(trimmed)
            if (match != null && match.groupValues.size > 1) {
                return match.groupValues[1]
            }
        }
        return null
    }

    fun toCanonicalWatchUrl(rawUrl: String): String? {
        val id = extractVideoId(rawUrl) ?: return null
        return "https://www.youtube.com/watch?v=$id"
    }
}
