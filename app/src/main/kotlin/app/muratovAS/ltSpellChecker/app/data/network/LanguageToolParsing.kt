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
package app.muratovAS.ltSpellChecker.app.data.network

import android.util.Log
import app.muratovAS.ltSpellChecker.app.data.Suggestion
import org.json.JSONArray
import org.json.JSONObject

class LanguageToolParsing {
    private val tag = LanguageToolParsing::class.java.simpleName

    // Основные категории ошибок LanguageTool, которые считаем грамматическими
    private val grammarCategories = setOf(
        "GRAMMAR",          // Общие грамматические ошибки
        "CONFUSED_WORDS",   // Слова, которые часто путают
        "PUNCTUATION",      // Пунктуация
        "STYLE",            // Стилистические проблемы
        "REDUNDANCY",       // Избыточные выражения
        "CASING",           // Проблемы регистра (кроме начала предложения)
        "COLLOCATIONS",     // Неправильные сочетания слов
        "SEMANTICS",        // Семантические ошибки
        "TYPOGRAPHY",       // Типографические правила
        "MISC"              // Разные другие грамматические проблемы
    )

    // Категории, которые считаем орфографическими (не грамматическими)
    private val spellingCategories = setOf(
        "HUNSPELL_SPELLING_RULE",  // Чисто орфографические ошибки
        "SPELLING"                 // Общие орфографические ошибки
    )

    // Для TYPOS — дополнительная проверка
    private fun JSONObject.isComplexTypo(): Boolean {
        return getJSONObject("rule").getString("id").let { ruleId ->
            ruleId.contains("CONFUSION") || ruleId.contains("COMPOUNDING") // Сложные случаи
        }
    }

    fun getSuggestions(jsonText: String): Array<Suggestion> {
        return try {
            JSONObject(jsonText)
                .getJSONArray("matches")
                .parseMatches()
                .filterNot { it.isUppercaseRule() }
                .map { it.toSuggestion() }
                .toTypedArray()
        } catch (e: Exception) {
            Log.e(tag, "Error parsing suggestions", e)
            emptyArray()
        }
    }

    private fun JSONArray.parseMatches(): List<JSONObject> {
        return List(length()) { idx -> getJSONObject(idx) }
    }

    private fun JSONObject.isUppercaseRule(): Boolean {
        return try {
            getJSONObject("rule").getString("id") == "UPPERCASE_SENTENCE_START"
        } catch (e: Exception) {
            Log.d(tag, e.message.toString())
            false
        }
    }

    private fun JSONObject.toSuggestion(): Suggestion {
        val replacements = getJSONArray("replacements")
        val message = optString("message", "")
        val offset = getInt("offset")
        val length = getInt("length")
        val rule = optJSONObject("rule") ?: JSONObject()
        val categoryId = rule.optJSONObject("category")?.getString("id") ?: ""
        val ruleId = rule.getString("id")

        // Определяем тип предложения
        val isGrammar = when {
            spellingCategories.any { it.equals(categoryId, ignoreCase = true) } -> false
            grammarCategories.any { it.equals(categoryId, ignoreCase = true) } -> true
            categoryId == "TYPOS" && isComplexTypo() -> true  // Сложные опечатки
            else -> false  // Простые опечатки
        }

        return Suggestion().apply {
            text = if (replacements.length() == 0) {
                arrayOf("($message)")
            } else {
                replacements.parseStringValues()
            }
            position = offset
            this.length = length
            isGrammarSuggestion = isGrammar
            Log.d(tag, "Suggestion: $ruleId ($categoryId) " + "Pos:$position Len:${this.length} " + "Grammar:$isGrammarSuggestion")
        }
    }

    private fun JSONArray.parseStringValues(): Array<String> {
        return List(length()) { idx ->
            getJSONObject(idx).getString("value")
        }.toTypedArray()
    }
}