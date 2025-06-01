package app.muratovAS.ltSpellChecker.ui.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import app.muratovAS.ltSpellChecker.BuildConfig
import app.muratovAS.ltSpellChecker.R
import app.muratovAS.ltSpellChecker.app.data.Configuration
import app.muratovAS.ltSpellChecker.app.extensions.toSafeStringSet

/**
 * Preference screen.
 */
class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val tag = SettingsFragment::class.java.simpleName

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.spell_checker_settings, rootKey)
        initPreferences()
    }

    private fun initPreferences() {
        setServer()
        setVersion()
        setListeners()
    }

    private fun setServer() {
        findPreference<EditTextPreference>("server")?.apply {
            summary = Configuration.server
            setOnPreferenceChangeListener { _, newValue ->
                summary = newValue.toString()
                Configuration.server = newValue.toString()
                true
            }
        }
    }

    private fun setListeners() {
        setPreferenceChangeListener("language") { Configuration.language = it.toString() }
        setPreferenceChangeListener("mother_tongue") { Configuration.motherTongue = it.toString() }

        findPreference<MultiSelectListPreference>("preferred_variants")?.setOnPreferenceChangeListener { _, newValue ->
            Configuration.preferredVariants = newValue.toSafeStringSet()
            true
        }
    }

    private inline fun setPreferenceChangeListener(
        key: String,
        crossinline action: (Any) -> Unit
    ) {
        findPreference<ListPreference>(key)?.setOnPreferenceChangeListener { _, newValue ->
            action(newValue)
            true
        }
    }

    private fun setVersion() {
        findPreference<Preference>("version")?.summary = buildVersionText()
    }

    private fun buildVersionText(): String {
        val buildDate = BuildConfig.buildTime
        return resources.getString(R.string.version_text, version, buildDate)
    }

    private val version: String?
        get() = try {
            requireActivity().packageManager
                .getPackageInfo(requireActivity().packageName, 0)
                .versionName
        } catch (e: Exception) {
            Log.d(tag, e.message.toString())
            null
        }

    private fun setHttpConnections() {
        findPreference<Preference>("http")?.summary = buildConnectionStatus()
    }

    private fun buildConnectionStatus(): String {
        val lastConnection = Configuration.lastConnection
        return resources.getString(
            R.string.connections,
            Configuration.connectionsCount,
            lastConnection?.toString() ?: resources.getString(R.string.connection_none)
        )
    }

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        setHttpConnections()
    }

    override fun onPause() {
        super.onPause()
        preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        if (key == "http") {
            setHttpConnections()
        }
    }
}