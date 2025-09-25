package com.youtubetranscript

/**
 * Base exception class for YouTube transcript operations.
 * 
 * This class and its subclasses mirror the exception hierarchy from the Python library.
 */
open class TranscriptException(
    message: String,
    val videoId: String = "",
    val availableLanguages: List<String> = emptyList()
) : Exception(message) {
    
    override fun toString(): String {
        val baseMessage = if (videoId.isNotEmpty()) {
            "TranscriptException for video $videoId: $message"
        } else {
            "TranscriptException: $message"
        }
        
        return if (availableLanguages.isNotEmpty()) {
            "$baseMessage (Available languages: ${availableLanguages.joinToString(", ")})"
        } else {
            baseMessage
        }
    }
}

/**
 * Exception thrown when a video is unavailable.
 */
class VideoUnavailableException(
    videoId: String,
    message: String = "Video is unavailable"
) : TranscriptException(message, videoId)

/**
 * Exception thrown when transcripts are disabled for a video.
 */
class TranscriptsDisabledException(
    videoId: String,
    message: String = "Transcripts are disabled for this video"
) : TranscriptException(message, videoId)

/**
 * Exception thrown when no transcript is found for the requested languages.
 */
class NoTranscriptFoundException(
    videoId: String,
    requestedLanguages: List<String>,
    availableLanguages: List<String>
) : TranscriptException(
    "No transcript found for languages: ${requestedLanguages.joinToString(", ")}",
    videoId,
    availableLanguages
)

/**
 * Exception thrown when a video is age-restricted.
 */
class AgeRestrictedException(
    videoId: String,
    message: String = "Video is age-restricted"
) : TranscriptException(message, videoId)

/**
 * Exception thrown when the IP is blocked by YouTube.
 */
class IpBlockedException(
    videoId: String,
    message: String = "IP address is blocked by YouTube"
) : TranscriptException(message, videoId)

/**
 * Exception thrown when a request is blocked.
 */
class RequestBlockedException(
    videoId: String,
    message: String = "Request was blocked"
) : TranscriptException(message, videoId)

/**
 * Exception thrown when a video is not playable.
 */
class VideoUnplayableException(
    videoId: String,
    reason: String,
    message: String = "Video is not playable"
) : TranscriptException("$message: $reason", videoId)

/**
 * Exception thrown when YouTube data cannot be parsed.
 */
class YouTubeDataUnparsableException(
    videoId: String,
    message: String = "YouTube data could not be parsed"
) : TranscriptException(message, videoId)

/**
 * Exception thrown when an invalid video ID is provided.
 */
class InvalidVideoIdException(
    videoId: String,
    message: String = "Invalid video ID"
) : TranscriptException(message, videoId)

/**
 * Exception thrown when a transcript is not translatable.
 */
class NotTranslatableException(
    videoId: String,
    message: String = "Transcript is not translatable"
) : TranscriptException(message, videoId)

/**
 * Exception thrown when a translation language is not available.
 */
class TranslationLanguageNotAvailableException(
    videoId: String,
    languageCode: String,
    message: String = "Translation language not available"
) : TranscriptException("$message: $languageCode", videoId)

/**
 * Exception thrown when a consent cookie is required.
 */
class FailedToCreateConsentCookieException(
    videoId: String,
    message: String = "Failed to create consent cookie"
) : TranscriptException(message, videoId)

/**
 * Exception thrown when a PO token is required.
 */
class PoTokenRequiredException(
    videoId: String,
    message: String = "PO token required"
) : TranscriptException(message, videoId)
