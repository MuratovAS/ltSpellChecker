/*
 * Copyright (C) 2014-2016 Jordi Mas i Hernàndez <jmas@softcatala.org>
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
import org.json.JSONObject

class LanguageToolParsing {
    private val tag = LanguageToolParsing::class.java.simpleName

    fun getSuggestions(jsonText: String): Array<Suggestion> {
        val suggestions = ArrayList<Suggestion>()
        try {
            val json = JSONObject(jsonText)
            val matches = json.getJSONArray("matches")
            for (i in 0 until matches.length()) {
                val match = matches.getJSONObject(i)
                val replacements = match.getJSONArray("replacements")
                val rule = match.getJSONObject("rule")
                val ruleId = rule.getString("id")

                // Since we process fragments we need to skip the upper case
                // suggestion
                if (ruleId == "UPPERCASE_SENTENCE_START") continue
                val suggestion = Suggestion()
                if (replacements.length() == 0) {
                    val message = match.getString("message")
                    val msgText = String.format("(%s)", message)
                    suggestion.Text = arrayOf(msgText)
                } else {
                    val list = ArrayList<String>()
                    for (r in 0 until replacements.length()) {
                        val replacement = replacements.getJSONObject(r)
                        val value = replacement.getString("value")
                        list.add(value)
                    }
                    suggestion.Text = list.toTypedArray<String>()
                }
                suggestion.Position = match.getInt("offset")
                suggestion.Length = match.getInt("length")
                suggestions.add(suggestion)
                Log.d(tag, "Request result: " + suggestion.Position + " Len:" + suggestion.Length)
            }
        } catch (e: Exception) {
            Log.e(tag, "GetSuggestions", e)
        }
        return suggestions.toTypedArray<Suggestion>()
    }
}
