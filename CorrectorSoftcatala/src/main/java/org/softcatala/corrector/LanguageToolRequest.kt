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
package org.softcatala.corrector

import android.util.Log
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Date
import java.util.Random

class LanguageToolRequest(private val system_language: String) {
    private val ENCODING = "UTF-8"
    private val TAG = LanguageToolRequest::class.java.simpleName
    private val m_sessionId = getSessionID()
    private fun getSessionID(): String {
        val rand = Random()
        val MAX_NUM = 999999
        val id = rand.nextInt(MAX_NUM)
        return Integer.toString(id)
    }

    private val languageToolParsing = LanguageToolParsing()
    private var mAndroidToLTLangMap = arrayOf(
        arrayOf("ar", "ar"),
        arrayOf("ast", "ast-ES"),
        arrayOf("be", "be-BY"),
        arrayOf("br", "br-FR"),
        arrayOf("ca", "ca-ES"),
        arrayOf("zh", "zh-CN"),
        arrayOf("da", "da-DK"),
        arrayOf("nl", "nl"),
        arrayOf("en", "en-US"),
        arrayOf("eo", "eo"),
        arrayOf("fr", "fr"),
        arrayOf("gl", "gl-ES"),
        arrayOf("de", "de-DE"),
        arrayOf("el", "el-GR"),
        arrayOf("ga", "ga-IE"),
        arrayOf("it", "it"),
        arrayOf("ja", "ja-JP"),
        arrayOf("km", "km-KH"),
        arrayOf("nb", "nb"),
        arrayOf("no", "no"),
        arrayOf("fa", "fa"),
        arrayOf("pl", "pl-PL"),
        arrayOf("pt", "pt"),
        arrayOf("ro", "ro-RO"),
        arrayOf("ru", "ru-RU"),
        arrayOf("sk", "sk-SK"),
        arrayOf("sl", "sl-SI"),
        arrayOf("es", "es"),
        arrayOf("sv", "sv"),
        arrayOf("tl", "tl-PH"),
        arrayOf("ta", "ta-IN"),
        arrayOf("uk", "uk-UA")
    )

    private fun convertLanguage(language: String): String {
        var lang = ""
        for (strings in mAndroidToLTLangMap) {
            if (language.startsWith(strings[0])) {
                lang = strings[1]
                break
            }
        }
        Log.d(TAG, String.format("ConvertLanguage from Android %s to LT %s", language, lang))
        return lang
    }

    fun getSuggestions(text: String?): Array<Suggestion> {
        return request(text)
    }

    private fun getFillPostFields(text: String?): String {
        val queryParameter = StringBuilder()
        queryParameter.append(addQueryParameter("", "useragent", "androidspell"))
        queryParameter.append(addQueryParameter("&", "text", text))
        val settings_language = Configuration.getInstance()!!.getLanguage()
        if (settings_language == "system") {
            queryParameter.append(
                addQueryParameter(
                    "&",
                    "language",
                    convertLanguage(system_language)
                )
            )
        } else {
            queryParameter.append(addQueryParameter("&", "language", settings_language))
            if (settings_language == "auto") {
                val settings_preferred_variants =
                    Configuration.getInstance()!!.getPreferredVariants()
                if (settings_preferred_variants != null) {
                    if (settings_preferred_variants.isNotEmpty()) {
                        queryParameter.append(
                            addQueryParameter(
                                "&",
                                "preferredVariants",
                                java.lang.String.join(",", settings_preferred_variants)
                            )
                        )
                    }
                }
            }
        }
        val settings_mother_tongue = Configuration.getInstance()!!.getMotherTongue()
        if (settings_mother_tongue!!.isNotEmpty()) {
            queryParameter.append(addQueryParameter("&", "motherTongue", settings_mother_tongue))
        }
        return queryParameter.toString()
    }

    // HTTP POST request
    private fun sendPost(text: String?): String {
        try {
            val url = buildURL()
            Log.d(TAG, "URL: $url")
            val obj = URL(url)
            val con = obj.openConnection() as HttpURLConnection
            con.requestMethod = "POST"
            val urlParameters = getFillPostFields(text)
            Log.d(TAG, "Parameters : $urlParameters")

            // Send post request
            con.doOutput = true
            val wr = DataOutputStream(con.outputStream)
            wr.writeBytes(urlParameters)
            wr.flush()
            wr.close()
            val responseCode = con.responseCode
            Log.d(TAG, "Response Code : $responseCode")
            val `in` = BufferedReader(
                InputStreamReader(con.inputStream)
            )
            var inputLine: String?
            val response = StringBuilder()
            Log.d(TAG, "Response : $response")
            while (`in`.readLine().also { inputLine = it } != null) {
                response.append(inputLine)
            }
            `in`.close()
            return response.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Exception ", e)
        }
        return ""
    }

    private fun request(text: String?): Array<Suggestion> {
        try {
            val result = sendPost(text)
            Configuration.getInstance()!!.incConnections()
            Configuration.getInstance()!!.setLastConnection(Date())
            Log.d(TAG, "Request result: $result")
            return languageToolParsing.getSuggestions(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading stream from URL.", e)
        }
        return arrayOf()
    }

    private fun buildURL(): String {
        return Configuration.getInstance()!!.getServer() +
                "/v2/check" +  /* Parameter to help to track requests from the same IP */
                addQueryParameter("?", "sessionID", m_sessionId)
    }

    fun addQueryParameter(separator: String?, key: String?, value: String?): String {
        val sb = StringBuilder()
        sb.append(separator)
        sb.append(key)
        sb.append("=")
        try {
            sb.append(URLEncoder.encode(value, ENCODING))
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }
        return sb.toString()
    }
}
