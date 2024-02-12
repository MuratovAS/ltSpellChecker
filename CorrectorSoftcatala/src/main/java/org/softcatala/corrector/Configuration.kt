package org.softcatala.corrector

import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import java.util.Date
import kotlin.concurrent.Volatile

object Configuration {
    private const val languagetoolServerDefault = "https://api.languagetool.org"
    private const val languageDefault = "system"
    private const val motherTongueDefault = ""
    private val preferredVariantsDefault: Set<String> = HashSet()
    private const val PREF_SERVER = "corrector.softcatala.server"
    private const val PREF_LANGUAGE = "corrector.softcatala.language"
    private const val PREF_MOTHER_TONGUE = "corrector.softcatala.mother_tongue"
    private const val PREF_PREFERRED_VARIANTS = "corrector.softcatala.preferred_variants"
    lateinit var SettingsActivity: SpellCheckerSettingsActivity
    private lateinit var sharedPreferences: SharedPreferences

    @Volatile
    private var instance: Configuration? = null
    private var HttpConnections = 0
    private var LastConnection: Date? = null

    @Synchronized
    fun getInstance(): Configuration? {
        if (instance == null) {
            instance = Configuration
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(SettingsActivity)
        }
        return instance
    }

    fun getServer(): String? {
        return sharedPreferences.getString(
            PREF_SERVER,
            languagetoolServerDefault
        )
    }

    fun setServer(server: String): String {
        var sv = server
        if (sv.isEmpty()) {
            sv = languagetoolServerDefault
        }
        sharedPreferences.edit().putString(PREF_SERVER, sv).apply()
        return server
    }

    fun getHttpConnections(): Int {
        return HttpConnections
    }

    fun incConnections() {
        HttpConnections++
    }

    fun getLastConnection(): Date? {
        return LastConnection
    }

    fun setLastConnection(date: Date?) {
        LastConnection = date
    }

    fun getLanguage(): String? {
        return sharedPreferences.getString(
            PREF_LANGUAGE,
            languageDefault
        )
    }

    fun setLanguage(language: String?) {
        sharedPreferences.edit().putString(PREF_LANGUAGE, language).apply()
    }

    fun getMotherTongue(): String? {
        return sharedPreferences.getString(
            PREF_MOTHER_TONGUE,
            motherTongueDefault
        )
    }

    fun setMotherTongue(motherTongue: String?) {
        sharedPreferences.edit().putString(PREF_MOTHER_TONGUE, motherTongue).apply()
    }

    fun getPreferredVariants(): Set<String?>? {
        return sharedPreferences.getStringSet(
            PREF_PREFERRED_VARIANTS,
            preferredVariantsDefault
        )
    }

    fun setPreferredVariants(preferredVariants: Set<String?>?) {
        sharedPreferences.edit()
            .putStringSet(PREF_PREFERRED_VARIANTS, preferredVariants).apply()
    }
}