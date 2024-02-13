/*
 * Copyright (C) 2015 Jordi Mas i Hernàndez <jmas@softcatala.org>
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

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.softcatala.corrector.Configuration

/**
 * Preference screen.
 */
class SpellCheckerSettingsFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.spell_checker_settings, rootKey)
        setServer()
        setVersion()
        setLanguageChangeListener()
        setMotherTongueChangeListener()
        setPreferredVariantsChangeListener()
    }

    private fun setServer() {
        val serverField = findPreference<EditTextPreference>("server")!!
        serverField.summary = Configuration.getInstance()!!.getServer()
        serverField.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any ->
                var newServer: String? = newValue.toString()
                newServer = Configuration.getInstance()!!.setServer(newServer!!)
                serverField.summary = newServer
                true
            }
    }

    private fun setLanguageChangeListener() {
        val languageField = findPreference<ListPreference>("language")!!
        languageField.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any ->
                Configuration.getInstance()!!
                    .setLanguage(newValue.toString())
                true
            }
    }

    private fun setMotherTongueChangeListener() {
        val motherTongueField = findPreference<ListPreference>("mother_tongue")!!
        motherTongueField.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any ->
                Configuration.getInstance()!!
                    .setMotherTongue(newValue.toString())
                true
            }
    }

    private fun setPreferredVariantsChangeListener() {
        val preferredVariantsField =
            findPreference<MultiSelectListPreference>("preferred_variants")!!
        preferredVariantsField.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
                Configuration.getInstance()!!
                    .setPreferredVariants(newValue as Set<String?>?)
                true
            }
    }

    private fun setVersion() {
        val buildDate = BuildConfig.buildTime
        val version = findPreference<Preference>("version")
        val v = String.format(resources.getString(R.string.version_text), this.version, buildDate)
        assert(version != null)
        version!!.summary = v
    }

    private val version: String?
        get() = try {
            requireActivity().packageManager.getPackageInfo(
                requireActivity().packageName,
                0
            ).versionName
        } catch (e: Exception) {
            null
        }

    private fun setHttpConnections() {
        val http = findPreference<Preference>("http")
        val lastConnection = Configuration.getInstance()!!
            .getLastConnection()
        val status = String.format(
            resources.getString(R.string.connections),
            Configuration.getInstance()!!.getHttpConnections(),
            lastConnection?.toString() ?: resources.getString(R.string.connection_none)
        )
        assert(http != null)
        http!!.summary = status
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
        assert(key != null)
        if (key == "http") {
            setHttpConnections()
        }
    }
}