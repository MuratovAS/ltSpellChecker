package app.muratovAS.ltSpellChecker.app.data

import androidx.core.content.edit
import app.muratovAS.ltSpellChecker.app.App
import java.util.Date

object Configuration {
    private const val LANGUAGE_TOOL_SERVER_DEFAULT = "https://api.languagetool.org"
    private const val LANGUAGE_DEFAULT = "system"
    private const val MOTHER_TONGUE_DEFAULT = ""
    private const val PREF_SERVER = "ltspellchecker.server"
    private const val PREF_LANGUAGE = "ltspellchecker.language"
    private const val PREF_MOTHER_TONGUE = "ltspellchecker.mother_tongue"
    private const val PREF_PREFERRED_VARIANTS = "ltspellchecker.preferred_variants"

    private val PREFERRED_VARIANTS_DEFAULT: Set<String> = HashSet()
    private var _connectionsCount = 0
    private var _lastConnection: Date? = null

    val connectionsCount: Int
        get() = _connectionsCount

    var lastConnection: Date?
        get() = _lastConnection
        set(value) {
            _lastConnection = value
        }

    fun incrementConnections() {
        _connectionsCount++
    }

    var server: String
        get() = App.prefs.getString(PREF_SERVER, LANGUAGE_TOOL_SERVER_DEFAULT) ?: LANGUAGE_TOOL_SERVER_DEFAULT
        set(value) {
            App.prefs.edit {
                putString(PREF_SERVER, value.ifEmpty { LANGUAGE_TOOL_SERVER_DEFAULT })
            }
        }

    var language: String?
        get() = App.prefs.getString(PREF_LANGUAGE, LANGUAGE_DEFAULT)
        set(value) {
            App.prefs.edit { putString(PREF_LANGUAGE, value) }
        }

    var motherTongue: String?
        get() = App.prefs.getString(PREF_MOTHER_TONGUE, MOTHER_TONGUE_DEFAULT)
        set(value) {
            App.prefs.edit { putString(PREF_MOTHER_TONGUE, value) }
        }

    var preferredVariants: Set<String>
        get() = App.prefs.getStringSet(PREF_PREFERRED_VARIANTS, PREFERRED_VARIANTS_DEFAULT) ?: PREFERRED_VARIANTS_DEFAULT
        set(value) {
            App.prefs.edit { putStringSet(PREF_PREFERRED_VARIANTS, value) }
        }
}