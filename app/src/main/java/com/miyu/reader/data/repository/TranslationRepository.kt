package com.miyu.reader.data.repository

import android.text.Html
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

data class TranslationResult(
    val translatedText: String?,
    val statusMessage: String,
)

@Singleton
class TranslationRepository @Inject constructor() {
    suspend fun translateToEnglish(text: String): TranslationResult = withContext(Dispatchers.IO) {
        val trimmed = text.replace(Regex("\\s+"), " ").trim().take(MAX_CHARS)
        if (trimmed.isBlank()) {
            return@withContext TranslationResult(null, "No text selected.")
        }

        return@withContext runCatching {
            val encodedQuery = URLEncoder.encode(trimmed, Charsets.UTF_8.name())
            val encodedPair = URLEncoder.encode("Autodetect|en", Charsets.UTF_8.name())
            val endpoint = URL("$MYMEMORY_ENDPOINT?q=$encodedQuery&langpair=$encodedPair")

            val connection = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 20000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "MIYO-Kotlin/1.0")
            }

            val body = if (connection.responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IllegalStateException("Translation failed (${connection.responseCode}): $error")
            }
            connection.disconnect()

            val translated = JSONObject(body)
                .optJSONObject("responseData")
                ?.optString("translatedText")
                ?.trim()
                .orEmpty()
                .decodeHtmlEntities()
                .trim()

            if (translated.isBlank()) {
                throw IllegalStateException("Translation unavailable. Check your connection or try shorter text.")
            }

            TranslationResult(
                translatedText = translated,
                statusMessage = translated,
            )
        }.getOrElse { error ->
            TranslationResult(
                translatedText = null,
                statusMessage = error.message ?: "Translation unavailable. Check your connection or try shorter text.",
            )
        }
    }

    private fun String.decodeHtmlEntities(): String =
        Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY).toString()

    private companion object {
        const val MAX_CHARS = 440
        const val MYMEMORY_ENDPOINT = "https://api.mymemory.translated.net/get"
    }
}
