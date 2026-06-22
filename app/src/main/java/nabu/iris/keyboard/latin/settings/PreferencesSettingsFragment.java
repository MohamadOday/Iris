/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (C) 2025 Raimondas Rimkus
 * Copyright (C) 2021 wittmane
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nabu.iris.keyboard.latin.settings;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import nabu.iris.keyboard.R;
import nabu.iris.keyboard.keyboard.KeyboardLayoutSet;

/**
 * "Preferences" settings sub screen.
 *
 * This settings sub screen handles the following input preferences.
 * - Auto-capitalization
 * - Show separate number row
 * - Show special characters
 * - Show language switch key
 * - Show on-screen keyboard
 * - Switch to other keyboards
 * - Space swipe cursor move
 * - Delete swipe
 */
public final class PreferencesSettingsFragment extends SubScreenFragment {
    @Override
    public void onCreate(final Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.prefs_screen_preferences);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) {
            removePreference(Settings.PREF_USE_ON_SCREEN);
        }

        setupUtilityBackgroundColorSettings();
    }

    private void setupUtilityBackgroundColorSettings() {
        final ColorDialogPreference pref = (ColorDialogPreference)findPreference(
                Settings.PREF_UTILITY_BACKGROUND_COLOR);
        if (pref == null) {
            return;
        }
        final SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
        pref.setInterface(new ColorDialogPreference.ValueProxy() {
            @Override
            public void writeValue(final int value, final String key) {
                prefs.edit().putInt(key, value).apply();
            }

            @Override
            public int readValue(final String key) {
                int defaultColor = Settings.readKeyboardColor(prefs, getActivity());
                return prefs.getInt(key, defaultColor);
            }

            @Override
            public void writeDefaultValue(final String key) {
                prefs.edit().remove(key).apply();
            }
        });
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences prefs, final String key) {
        if (key.equals(Settings.PREF_SHOW_SPECIAL_CHARS) ||
                key.equals(Settings.PREF_SHOW_NUMBER_ROW) ||
                key.equals(Settings.PREF_EMOJI_LIST) ||
                key.equals(Settings.PREF_UTILITY_SHOW_KEYS) ||
                key.equals(Settings.PREF_UTILITY_SHOW_CLIPBOARD) ||
                key.equals(Settings.PREF_UTILITY_SHOW_AI) ||
                key.equals(Settings.PREF_SHOW_EMOJI_KEY) ||
                key.equals(Settings.PREF_UTILITY_SHOW_SETTINGS) ||
                key.equals(Settings.PREF_UTILITY_BUTTON_ORDER) ||
                key.equals(Settings.PREF_UTILITY_BACKGROUND_COLOR)) {
            KeyboardLayoutSet.onKeyboardThemeChanged();
        }
    }
}
