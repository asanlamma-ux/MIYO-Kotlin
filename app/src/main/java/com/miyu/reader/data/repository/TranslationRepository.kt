package com.miyu.reader.data.repository

import android.text.Html
import com.miyu.reader.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class TranslationResult(
    val translatedText: String?,
    val statusMessage: String,
)

@Singleton
class TranslationRepository @Inject constructor() {
    suspend fun translateToEnglish(text: String): TranslationResult = withContext(Dispatchers.IO) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) {
            return@withContext TranslationResult(null, "No text selected.")
        }

        val apiKey = BuildConfig.GOOGLE_TRANSLATE_API_KEY
        if (apiKey.isBlank()) {
            return@withContext TranslationResult(
                translatedText = null,
                statusMessage = "Google Translate is not configured. Add MIYU_GOOGLE_TRANSLATE_API_KEY to enable in-app translation.",
            )
        }

        return@withContext runCatching {
            val endpoint = URL("https://translation.googleapis.com/language/translate/v2?key=$apiKey")
            val payload = JSONObject()
                .put("q", trimmed)
                .put("target", "en")
                .put("format", "text")
                .toString()

            val connection = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8000
                readTimeout = 12000
                doOutput = true
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }

            connection.outputStream.use { output ->
                output.write(payload.toByteArray(Charsets.UTF_8))
            }

            val body = if (connection.responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IllegalStateException("Translation failed (${connection.responseCode}): $error")
            }
            connection.disconnect()

            val translated = JSONObject(body)
                .getJSONObject("data")
                .getJSONArray("translations")
                .getJSONObject(0)
                .getString("translatedText")
                .decodeHtmlEntities()

            TranslationResult(
                translatedText = translated,
                statusMessage = translated,
            )
        }.getOrElse { error ->
            TranslationResult(
                translatedText = null,
                statusMessage = error.message ?: "Translation failed.",
            )
        }
    }

    private fun String.decodeHtmlEntities(): String =
        Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY).toString()
}
