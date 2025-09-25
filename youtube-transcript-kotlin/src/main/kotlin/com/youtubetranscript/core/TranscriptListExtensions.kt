package com.youtubetranscript.core

import com.youtubetranscript.models.Transcript
import com.youtubetranscript.models.TranscriptList
import com.youtubetranscript.models.TranslationLanguage
import okhttp3.OkHttpClient
import org.json.JSONObject

/**
 * Extension functions for TranscriptList to handle JSON parsing and building.
 */
fun buildTranscriptList(
    httpClient: OkHttpClient,
    videoId: String,
    captionsJson: JSONObject
): TranscriptList {
    val translationLanguages = parseTranslationLanguages(captionsJson)
    val manuallyCreatedTranscripts = mutableMapOf<String, Transcript>()
    val generatedTranscripts = mutableMapOf<String, Transcript>()

    val captionTracks = captionsJson.optJSONArray("captionTracks")
    if (captionTracks != null) {
        for (i in 0 until captionTracks.length()) {
            val caption = captionTracks.getJSONObject(i)
            val languageCode = caption.optString("languageCode", "")
            val languageName = caption.optJSONObject("name")?.optJSONArray("runs")?.getJSONObject(0)
                ?.optString("text", "") ?: ""
            val baseUrl = caption.optString("baseUrl", "").replace("&fmt=srv3", "")
            val isGenerated = caption.optString("kind", "") == "asr"
            val isTranslatable = caption.optBoolean("isTranslatable", false)

            if (languageCode.isNotEmpty() && baseUrl.isNotEmpty()) {
                val transcript = Transcript(
                    httpClient = httpClient,
                    videoId = videoId,
                    url = baseUrl,
                    language = languageName,
                    languageCode = languageCode,
                    isGenerated = isGenerated,
                    isTranslatable = isTranslatable,
                    translationLanguages = if (isTranslatable) translationLanguages else emptyList()
                )

                if (isGenerated) {
                    generatedTranscripts[languageCode] = transcript
                } else {
                    manuallyCreatedTranscripts[languageCode] = transcript
                }
            }
        }
    }

    return TranscriptList(
        videoId = videoId,
        manuallyCreatedTranscripts = manuallyCreatedTranscripts,
        generatedTranscripts = generatedTranscripts,
        translationLanguages = translationLanguages
    )
}

/**
 * Parse translation languages from captions JSON.
 */
private fun parseTranslationLanguages(captionsJson: JSONObject): List<TranslationLanguage> {
    val translationLanguages = mutableListOf<TranslationLanguage>()

    val translationLanguagesArray = captionsJson.optJSONArray("translationLanguages")
    if (translationLanguagesArray != null) {
        for (i in 0 until translationLanguagesArray.length()) {
            val translationLanguage = translationLanguagesArray.getJSONObject(i)
            val languageCode = translationLanguage.optString("languageCode", "")
            val languageName = translationLanguage.optJSONObject("languageName")
                ?.optJSONArray("runs")
                ?.getJSONObject(0)
                ?.optString("text", "") ?: ""

            if (languageCode.isNotEmpty() && languageName.isNotEmpty()) {
                translationLanguages.add(
                    TranslationLanguage(
                        language = languageName,
                        languageCode = languageCode
                    )
                )
            }
        }
    }

    return translationLanguages
}
