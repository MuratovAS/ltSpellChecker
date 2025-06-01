/*
 * Copyright (C) 2014-2017 Jordi Mas i Hernàndez <jmas@softcatala.org>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */
package app.muratovAS.ltSpellChecker.app.data.network

import android.util.Log
import app.muratovAS.ltSpellChecker.BuildConfig
import app.muratovAS.ltSpellChecker.app.App.Companion.httpClient
import app.muratovAS.ltSpellChecker.app.data.Configuration
import app.muratovAS.ltSpellChecker.app.data.Suggestion
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URLEncoder
import java.util.Date
import java.util.Random

class LanguageToolRequest(private val systemLanguage: String) {
    companion object {
        private const val ENCODING = "UTF-8"
        private const val MAX_SESSION_ID = 999999
        private const val MAX_RETRIES = 2
        private const val RETRY_DELAY_MS = 500L
    }

    private val tag = LanguageToolRequest::class.java.simpleName
    private val sessionId = Random().nextInt(MAX_SESSION_ID).toString()
    private val languageToolParsing = LanguageToolParsing()
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private val languageMap = mapOf(
        "ar" to "ar",
        "ast" to "ast-ES",
        "be" to "be-BY",
        "br" to "br-FR",
        "ca" to "ca-ES",
        "zh" to "zh-CN",
        "da" to "da-DK",
        "nl" to "nl",
        "en" to "en-US",
        "eo" to "eo",
        "fr" to "fr",
        "gl" to "gl-ES",
        "de" to "de-DE",
        "el" to "el-GR",
        "ga" to "ga-IE",
        "it" to "it",
        "ja" to "ja-JP",
        "km" to "km-KH",
        "nb" to "nb",
        "no" to "no",
        "fa" to "fa",
        "pl" to "pl-PL",
        "pt" to "pt",
        "ro" to "ro-RO",
        "ru" to "ru-RU",
        "sk" to "sk-SK",
        "sl" to "sl-SI",
        "es" to "es",
        "sv" to "sv",
        "tl" to "tl-PH",
        "ta" to "ta-IN",
        "uk" to "uk-UA"
    )

    suspend fun getSuggestionsAsync(text: String?): Array<Suggestion> = withContext(ioDispatcher) {
        retry(MAX_RETRIES, RETRY_DELAY_MS) {
            try {
                val result = sendPostAsync(text)
                Configuration.incrementConnections()
                Configuration.lastConnection = Date()
                if (BuildConfig.DEBUG) Log.d(tag, "Request result (${text?.take(20)}...)")
                languageToolParsing.getSuggestions(result)
            } catch (e: Exception) {
                Log.e(tag, "Error in async request for text (${text?.take(20)}...)", e)
                throw e
            }
        } ?: emptyArray()
    }

    @Deprecated("Use getSuggestionsAsync instead", ReplaceWith("getSuggestionsAsync(text)"))
    fun getSuggestions(text: String?): Array<Suggestion> = runBlocking {
        getSuggestionsAsync(text)
    }

    private suspend fun sendPostAsync(text: String?): String = withContext(ioDispatcher) {
        val url = buildURL()
        val urlParameters = getPostFields(text)

        if (BuildConfig.DEBUG) {
            Log.d(tag, "URL: $url")
            Log.d(tag, "Parameters: ${urlParameters.take(200)}...")
        }

        val requestBody = urlParameters.toRequestBody("application/x-www-form-urlencoded".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected code $response")
            }

            response.body?.string()?.also { responseBody ->
                if (BuildConfig.DEBUG) Log.d(tag, "Response (${responseBody.take(200)}...)")
            } ?: throw IOException("Empty response body")
        }
    }

    private fun convertLanguage(language: String): String {
        val prefix = language.substringBefore('-')
        return languageMap[prefix] ?: "".also {
            Log.w(tag, "Unsupported language: $language")
        }
    }

    private fun getPostFields(text: String?): String = buildString {
        append(addQueryParameter("", "text", text))

        when (val settingsLanguage = Configuration.language) {
            "system" -> append(addQueryParameter("&", "language", convertLanguage(systemLanguage)))
            "auto" -> {
                append(addQueryParameter("&", "language", settingsLanguage))
                Configuration.preferredVariants.takeIf { it.isNotEmpty() }?.let { variants ->
                    append(addQueryParameter("&", "preferredVariants", variants.joinToString(",")))
                }
            }

            else -> append(addQueryParameter("&", "language", settingsLanguage))
        }

        Configuration.motherTongue?.takeIf { it.isNotEmpty() }?.let { motherTongue ->
            append(addQueryParameter("&", "motherTongue", motherTongue))
        }
    }

    private fun buildURL(): String = "${Configuration.server}/v2/check${addQueryParameter("?", "sessionID", sessionId)}"

    private fun addQueryParameter(separator: String, key: String, value: String?): String =
        "$separator$key=${URLEncoder.encode(value, ENCODING)}"

    private suspend fun <T> retry(
        times: Int = MAX_RETRIES,
        initialDelay: Long = RETRY_DELAY_MS,
        block: suspend () -> T
    ): T? {
        var currentDelay = initialDelay
        repeat(times - 1) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                Log.w(tag, "Attempt ${attempt + 1} failed, retrying in ${currentDelay}ms", e)
                delay(currentDelay)
                currentDelay *= 2
            }
        }
        return try {
            block()
        } catch (e: Exception) {
            Log.w(tag, "retry error", e)
            null
        }
    }
}