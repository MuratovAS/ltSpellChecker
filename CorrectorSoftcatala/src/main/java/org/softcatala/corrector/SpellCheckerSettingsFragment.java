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

package org.softcatala.corrector;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Date;
import java.util.Objects;
import java.util.Set;


/**
 * Preference screen.
 */

public class SpellCheckerSettingsFragment extends
        PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.spell_checker_settings, rootKey);

        setServer();
        setVersion();
        setLanguageChangeListener();
        setMotherTongueChangeListener();
        setPreferredVariantsChangeListener();
    }

    private void setServer() {
        EditTextPreference serverField = findPreference("server");
        assert serverField != null;
        serverField.setSummary(Configuration.getInstance().getServer());

        serverField.setOnPreferenceChangeListener((preference, newValue) -> {
            String newServer = newValue.toString();
            newServer = Configuration.getInstance().setServer(newServer);
            serverField.setSummary(newServer);
            return true;
        });
    }

    private void setLanguageChangeListener() {
        ListPreference languageField = findPreference("language");
        assert languageField != null;
        languageField.setOnPreferenceChangeListener((preference, newValue) -> {
            Configuration.getInstance().setLanguage(newValue.toString());
            return true;
        });
    }

    private void setMotherTongueChangeListener() {
        ListPreference motherTongueField = findPreference("mother_tongue");
        assert motherTongueField != null;
        motherTongueField.setOnPreferenceChangeListener((preference, newValue) -> {
            Configuration.getInstance().setMotherTongue(newValue.toString());
            return true;
        });
    }

    @SuppressWarnings("unchecked")
    private void setPreferredVariantsChangeListener() {
        MultiSelectListPreference preferredVariantsField = findPreference("preferred_variants");
        assert preferredVariantsField != null;
        preferredVariantsField.setOnPreferenceChangeListener((preference, newValue) -> {
            Configuration.getInstance().setPreferredVariants((Set<String>) newValue);
            return true;
        });
    }

    private void setVersion() {
        Date buildDate = BuildConfig.buildTime;
        Preference version = findPreference("version");
        String v = String.format(getResources().getString(R.string.version_text), getVersion(), buildDate);
        assert version != null;
        version.setSummary(v);
    }

    private String getVersion() {
        try {
            return requireActivity().getPackageManager().getPackageInfo(requireActivity().getPackageName(), 0).versionName;
        } catch (Exception e) {
            return null;
        }
    }

    private void setHttpConnections() {
        Preference http = findPreference("http");
        Date lastConnection = Configuration.getInstance().getLastConnection();
        String status = String.format(getResources().getString(R.string.connections),
                Configuration.getInstance().getHttpConnections(),
                lastConnection == null ? getResources().getString(R.string.connection_none)
                        : lastConnection.toString());

        assert http != null;
        http.setSummary(status);
    }

    @Override
    public void onResume() {
        super.onResume();
        Objects.requireNonNull(
                getPreferenceManager().getSharedPreferences()).registerOnSharedPreferenceChangeListener(this);
        setHttpConnections();
    }

    @Override
    public void onPause() {
        super.onPause();
        Objects.requireNonNull(
                getPreferenceManager().getSharedPreferences()).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        assert key != null;
        if (key.equals("http")) {
            setHttpConnections();
        }
    }
}