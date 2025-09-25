package com.youtubetranscript.core

import com.youtubetranscript.TranscriptException
import com.youtubetranscript.models.TranscriptList
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Fetches transcript lists from YouTube.
 *
 * This class handles the two-step process:
 * 1. Fetch YouTube video page to extract INNERTUBE_API_KEY
 * 2. Use YouTube's internal API to get structured caption data
 */
class TranscriptListFetcher(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()
) {

    companion object {
        private const val USER_AGENT =
            "com.google.android.youtube/20.10.38 (Linux; U; Android 11) gzip"
        private const val WATCH_URL = "https://www.youtube.com/watch?v={video_id}"
        private const val INNERTUBE_API_URL =
            "https://www.youtube.com/youtubei/v1/player?key={api_key}"
        private const val INNERTUBE_CONTEXT =
            """{"client":{"clientName":"ANDROID","clientVersion":"20.10.38"}}"""
    }

    /**
     * Fetch transcript list for a video.
     */
    suspend fun fetch(videoId: String): TranscriptList {
        val captionsJson = fetchCaptionsJson(videoId)
        return buildTranscriptList(httpClient, videoId, captionsJson)
    }

    /**
     * Fetch captions JSON using the two-step process.
     */
    private suspend fun fetchCaptionsJson(videoId: String): JSONObject {
        // Step 1: Fetch YouTube video page
        val html = fetchVideoHtml(videoId)

        // Step 2: Extract INNERTUBE_API_KEY
        val apiKey = extractInnertubeApiKey(html, videoId)

        // Step 3: Fetch InnerTube data
        val innertubeData = fetchInnertubeData(videoId, apiKey)

        // Step 4: Extract captions JSON
        return extractCaptionsJson(innertubeData, videoId)
    }

    /**
     * Fetch YouTube video HTML page.
     */
    private suspend fun fetchVideoHtml(videoId: String): String {
        val url = WATCH_URL.replace("{video_id}", videoId)
        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", USER_AGENT)
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw TranscriptException("Failed to fetch video page: ${response.code}", videoId)
        }

        return response.body?.string() ?: throw TranscriptException("Empty response", videoId)
    }

    /**
     * Extract INNERTUBE_API_KEY from HTML.
     */
    private fun extractInnertubeApiKey(html: String, videoId: String): String {
        val pattern = """"INNERTUBE_API_KEY":\s*"([a-zA-Z0-9_-]+)"""".toRegex()
        val match = pattern.find(html)

        if (match != null) {
            return match.groupValues[1]
        }

        if (html.contains("class=\"g-recaptcha\"")) {
            throw TranscriptException("IP blocked by YouTube", videoId)
        }

        throw TranscriptException("Could not extract API key from video page", videoId)
    }

    /**
     * Fetch InnerTube data using the API key.
     */
    private suspend fun fetchInnertubeData(videoId: String, apiKey: String): JSONObject {
        val url = INNERTUBE_API_URL.replace("{api_key}", apiKey)
        
        val requestBody = """
            {
                "context": $INNERTUBE_CONTEXT,
                "videoId": "$videoId"
            }
        """.trimIndent()
        
        val request = Request.Builder()
            .url(url)
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .addHeader("User-Agent", USER_AGENT)
            .addHeader("Accept-Language", "en-US")
            .addHeader("Content-Type", "application/json")
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "No error body"
            throw TranscriptException("Failed to fetch InnerTube data: ${response.code} - $errorBody", videoId)
        }

        val responseBody =
            response.body?.string() ?: throw TranscriptException("Empty response", videoId)
        return JSONObject(responseBody)
    }

    /**
     * Extract captions JSON from InnerTube data.
     */
    private fun extractCaptionsJson(innertubeData: JSONObject, videoId: String): JSONObject {
        // Check playability status
        val playabilityStatus = innertubeData.optJSONObject("playabilityStatus")
        if (playabilityStatus != null) {
            val status = playabilityStatus.optString("status")
            if (status != "OK" && status.isNotEmpty()) {
                val reason = playabilityStatus.optString("reason", "Unknown error")
                throw TranscriptException("Video not playable: $reason", videoId)
            }
        }

        // Extract captions
        val captions = innertubeData.optJSONObject("captions")
        val captionsJson = captions?.optJSONObject("playerCaptionsTracklistRenderer")

        if (captionsJson == null || !captionsJson.has("captionTracks")) {
            throw TranscriptException("Transcripts are disabled for this video", videoId)
        }

        return captionsJson
    }
}
