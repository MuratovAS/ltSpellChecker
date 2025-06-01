package app.muratovAS.ltSpellChecker.app.services

import android.service.textservice.SpellCheckerService
import android.util.Log
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import app.muratovAS.ltSpellChecker.app.data.Suggestion
import app.muratovAS.ltSpellChecker.app.data.network.LanguageToolRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class LTSpellCheckerService : SpellCheckerService() {
    private val tag = LTSpellCheckerService::class.java.simpleName
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    companion object {
        private const val DBG = true
        private const val MAX_REPORTED_ERRORS_STORED = 100
    }

    override fun onCreate() {
        super.onCreate()
        if (DBG) Log.d(tag, "Service created")
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
        if (DBG) Log.d(tag, "Service destroyed")
    }

    override fun createSession(): Session {
        if (DBG) Log.d(tag, "createSession")
        return AndroidSpellCheckerSession()
    }

    private inner class AndroidSpellCheckerSession : Session() {
        private val tag = AndroidSpellCheckerSession::class.java.simpleName
        private var mLocale: String? = null
        private val mReportedErrors = LinkedHashSet<String>()
        private val sessionJob = SupervisorJob()
        private val sessionContext = Dispatchers.Default + sessionJob

        override fun onCreate() {
            if (DBG) Log.d(tag, "onCreate")
            mLocale = locale
        }

        override fun onCancel() {
            if (DBG) Log.d(tag, "onCancel")
            sessionJob.cancel()
        }

        override fun onClose() {
            if (DBG) Log.d(tag, "onClose")
            sessionJob.cancel()
        }

        /**
         * This is the word level spell checking previous for Android 4.4.4. Should not be called
         */
        override fun onGetSuggestions(textInfo: TextInfo, suggestionsLimit: Int): SuggestionsInfo? {
            if (DBG) Log.d(tag, "onGetSuggestions call not supported")
            return null
        }

        /**
         * Please consider providing your own implementation of sentence level
         * spell checking. Please note that this sample implementation is just a
         * mock to demonstrate how a sentence level spell checker returns the
         * result. If you don't override this method, the framework converts
         * queries of
         * [Session.onGetSentenceSuggestionsMultiple]
         * to queries of
         * [Session.onGetSuggestionsMultiple]
         * by the default implementation.
         */
        override fun onGetSentenceSuggestionsMultiple(
            textInfos: Array<TextInfo>,
            suggestionsLimit: Int
        ): Array<SentenceSuggestionsInfo> {
            return runBlocking {
                textInfos.map { ti ->
                    async(sessionContext) { // Исправлено здесь: используем sessionContext вместо sessionScope
                        processTextInfoAsync(ti)
                    }
                }.awaitAll().toTypedArray()
            }
        }

        private suspend fun processTextInfoAsync(ti: TextInfo): SentenceSuggestionsInfo {
            return withContext(sessionContext) {
                if (DBG) Log.d(tag, "Processing: ${ti.text}")

                val (sis, offsets, lengths) = processTextInfo(ti)
                SentenceSuggestionsInfo(
                    sis.toTypedArray(),
                    offsets.toIntArray(),
                    lengths.toIntArray()
                )
            }
        }

        private fun processTextInfo(ti: TextInfo): Triple<ArrayList<SuggestionsInfo>, ArrayList<Int>, ArrayList<Int>> {
            val sis = ArrayList<SuggestionsInfo>()
            val offsets = ArrayList<Int>()
            val lengths = ArrayList<Int>()

            removePreviouslyMarkedErrors(ti, sis, offsets, lengths)
            getSuggestionsFromLT(ti, sis, offsets, lengths)

            return Triple(sis, offsets, lengths)
        }

        private fun getSuggestionsFromLT(
            ti: TextInfo,
            sis: ArrayList<SuggestionsInfo>,
            offsets: ArrayList<Int>,
            lengths: ArrayList<Int>
        ) {
            val input = ti.text
            mLocale?.let { locale ->
                try {
                    val suggestions = runBlocking {
                        LanguageToolRequest(locale).getSuggestionsAsync(input)
                    }

                    suggestions.forEach { suggestion ->
                        processSuggestion(ti, input, suggestion, sis, offsets, lengths)
                    }
                } catch (e: Exception) {
                    if (DBG) Log.e(tag, "Error getting suggestions from LT", e)
                }
            }
        }

        private fun processSuggestion(
            ti: TextInfo,
            input: String,
            suggestion: Suggestion,
            sis: ArrayList<SuggestionsInfo>,
            offsets: ArrayList<Int>,
            lengths: ArrayList<Int>
        ) {
            val suggestionType = if (suggestion.isGrammarSuggestion) {
                SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_GRAMMAR_ERROR
            } else {
                SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO
            }

            SuggestionsInfo(
                suggestionType,
                suggestion.text
            ).apply {
                setCookieAndSequence(ti.cookie, ti.sequence)
                sis.add(this)
                offsets.add(suggestion.position)
                lengths.add(suggestion.length)

                if (!suggestion.isGrammarSuggestion) {
                    trackReportedError(input, suggestion)
                }
            }
        }

        private fun trackReportedError(input: String, suggestion: Suggestion) {
            val incorrectText = input.substring(
                suggestion.position,
                suggestion.position + suggestion.length
            )

            if (mReportedErrors.size >= MAX_REPORTED_ERRORS_STORED) {
                mReportedErrors.remove(mReportedErrors.first()) // Remove oldest
            }

            mReportedErrors.add(incorrectText)
            if (DBG) Log.d(tag, "Reported errors size: ${mReportedErrors.size}")
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
            ti: TextInfo,
            sis: ArrayList<SuggestionsInfo>,
            offsets: ArrayList<Int>,
            lengths: ArrayList<Int>
        ) {
            val input = ti.text
            mReportedErrors.forEach { txt ->
                var idx = input.indexOf(txt)
                while (idx != -1) {
                    SuggestionsInfo(0, arrayOf("")).apply {
                        setCookieAndSequence(ti.cookie, ti.sequence)
                        sis.add(this)
                    }
                    offsets.add(idx)
                    lengths.add(txt.length)
                    if (DBG) Log.d(tag, "Removing: '$txt' at $idx, ${txt.length}")
                    idx = input.indexOf(txt, idx + 1)
                }
            }
        }
    }
}