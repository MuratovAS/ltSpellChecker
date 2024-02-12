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

import android.service.textservice.SpellCheckerService
import android.util.Log
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo

class LTSpellCheckerService : SpellCheckerService() {
    override fun createSession(): Session {
        if (DBG) {
            Log.d(TAG, "createSession")
        }
        return AndroidSpellCheckerSession()
    }

    private class AndroidSpellCheckerSession : Session() {
        private val TAG = AndroidSpellCheckerSession::class.java
            .simpleName

        fun convertIntegers(integers: ArrayList<Int>): IntArray {
            val ret = IntArray(integers.size)
            val iterator: Iterator<Int> = integers.iterator()
            for (i in ret.indices) {
                ret[i] = iterator.next()
            }
            return ret
        }

        val MAX_REPORTED_ERRORS_STORED = 100
        private var mLocale: String? = null
        private var mReportedErrors: HashSet<String>? = null
        override fun onCreate() {
            if (DBG) {
                Log.d(TAG, "onCreate")
            }
            mLocale = locale
            mReportedErrors = HashSet()
        }

        override fun onCancel() {
            if (DBG) {
                Log.d(TAG, "onCancel")
            }
        }

        override fun onClose() {
            if (DBG) {
                Log.d(TAG, "onClose")
            }
        }

        /**
         * This is the word level spell checking previous for Android 4.4.4. Should not be called
         */
        override fun onGetSuggestions(
            textInfo: TextInfo,
            suggestionsLimit: Int
        ): SuggestionsInfo? {
            if (DBG) {
                Log.d(TAG, "onGetSuggestions call not supported")
            }
            return null
        }

        /**
         * Please consider providing your own implementation of sentence level
         * spell checking. Please note that this sample implementation is just a
         * mock to demonstrate how a sentence level spell checker returns the
         * result. If you don't override this method, the framework converts
         * queries of
         * [SpellCheckerService.Session.onGetSentenceSuggestionsMultiple]
         * to queries of
         * [SpellCheckerService.Session.onGetSuggestionsMultiple]
         * by the default implementation.
         */
        override fun onGetSentenceSuggestionsMultiple(
            textInfos: Array<TextInfo>, suggestionsLimit: Int
        ): Array<SentenceSuggestionsInfo> {
            val retval = ArrayList<SentenceSuggestionsInfo>()
            try {
                for (ti in textInfos) {
                    if (DBG) {
                        Log.d(TAG, "onGetSentenceSuggestionsMultiple: " + ti.text)
                    }
                    val sis = ArrayList<SuggestionsInfo>()
                    val offsets = ArrayList<Int>()
                    val lengths = ArrayList<Int>()
                    removePreviouslyMarkedErrors(ti, sis, offsets, lengths)
                    getSuggestionsFromLT(ti, sis, offsets, lengths)
                    val ssi = getSentenceSuggestionsInfo(sis, offsets, lengths)
                    retval.add(ssi)
                }
            } catch (e: Exception) {
                Log.e(TAG, "onGetSentenceSuggestionsMultiple$e")
            }
            return retval.toTypedArray<SentenceSuggestionsInfo>()
        }

        private fun getSentenceSuggestionsInfo(
            sis: ArrayList<SuggestionsInfo>,
            offsets: ArrayList<Int>,
            lengths: ArrayList<Int>
        ): SentenceSuggestionsInfo {
            val s = sis.toTypedArray<SuggestionsInfo>()
            val o = convertIntegers(offsets)
            val l = convertIntegers(lengths)
            return SentenceSuggestionsInfo(s, o, l)
        }

        private fun getSuggestionsFromLT(
            ti: TextInfo, sis: ArrayList<SuggestionsInfo>,
            offsets: ArrayList<Int>, lengths: ArrayList<Int>
        ) {
            val input = ti.text
            val languageToolRequest = LanguageToolRequest(mLocale!!)
            val suggestions = languageToolRequest
                .getSuggestions(input)
            for (suggestion in suggestions) {
                val si = SuggestionsInfo(
                    SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO,
                    suggestion.Text
                )
                si.setCookieAndSequence(ti.cookie, ti.sequence)
                sis.add(si)
                offsets.add(suggestion.Position)
                lengths.add(suggestion.Length)
                val incorrectText = input.substring(
                    suggestion.Position,
                    suggestion.Position + suggestion.Length
                )
                if (mReportedErrors!!.size < MAX_REPORTED_ERRORS_STORED) {
                    mReportedErrors!!.add(incorrectText)
                    Log.d(TAG, String.format("mReportedErrors size: %d", mReportedErrors!!.size))
                }
            }
        }

        /**
         * Let's imagine that you have the text:  Hi ha "cotxes" blaus
         * In the first request we get the text 'Hi ha "cotxes'. We return the error CA_UNPAIRED_BRACKETS
         * because the sentence is not completed and the ending commas are not introduced yet.
         *
         *
         * In the second request we get the text 'Hi ha "cotxes" blaus al carrer', now with both commas
         * there is no longer an error. However, since we sent the error as answer to the first request
         * the error marker will be there since they are not removed.
         *
         *
         * This function asks the spell checker to remove previously marked errors (all of them for the given string)
         * since we spell check the string every time.
         *
         *
         * Every time that we get a request we do not know how this related to the full sentence or
         * if is it a sentence previously given. As result, we may ask to remove previously marked errors,
         * but this is fine since we evaluate the sentence every time. We only clean the list of reported
         * errors once per session because we do not when a sentence with a previously marked error
         * will be requested again and if the words /marks  that we asked to cleanup previously correspond
         * to that fragment of text.
         */
        private fun removePreviouslyMarkedErrors(
            ti: TextInfo, sis: ArrayList<SuggestionsInfo>,
            offsets: ArrayList<Int>, lengths: ArrayList<Int>
        ) {
            val removeSpan = 0
            val input = ti.text
            for (txt in mReportedErrors!!) {
                var idx = input.indexOf(txt)
                while (idx != -1) {
                    val siNone = SuggestionsInfo(removeSpan, arrayOf(""))
                    siNone.setCookieAndSequence(ti.cookie, ti.sequence)
                    sis.add(siNone)
                    val len = txt.length
                    offsets.add(idx)
                    lengths.add(len)
                    Log.d(TAG, String.format("Asking to remove: '%s' at %d, %d", txt, idx, len))
                    idx = input.indexOf(txt, idx + 1)
                }
            }
        }
    }

    companion object {
        private val TAG = LTSpellCheckerService::class.java
            .simpleName
        private const val DBG = true
    }
}
