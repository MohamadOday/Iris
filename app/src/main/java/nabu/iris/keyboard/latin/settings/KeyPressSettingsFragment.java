/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (C) 2025 Raimondas Rimkus
 * Copyright (C) 2026 Iris Keyboard Project
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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.Preference;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import nabu.iris.keyboard.R;
import nabu.iris.keyboard.latin.AudioAndHapticFeedbackManager;

/**
 * "Keypress" settings sub screen.
 */
public final class KeyPressSettingsFragment extends SubScreenFragment {

    @Override
    public void onCreate(final Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.prefs_screen_key_press);

        setupKeypressSoundVolumeSettings();
        setupKeyLongpressTimeoutSettings();
        setupKeypressSoundpackSettings();
    }

    @Override
    public void onResume() {
        super.onResume();
        final Preference soundEffectPreference = findPreference(Settings.PREF_SOUND_ON);
        if (soundEffectPreference != null) {
            setPreferenceEnabled(Settings.PREF_SOUND_ON, true);
        }
        setupKeypressSoundpackSettings();
    }

    private void setupKeypressSoundVolumeSettings() {
        final SeekBarDialogPreference pref = (SeekBarDialogPreference)findPreference(
                Settings.PREF_KEYPRESS_SOUND_VOLUME);
        if (pref == null) {
            return;
        }
        final SharedPreferences prefs = getSharedPreferences();
        final Resources res = getResources();
        pref.setInterface(new SeekBarDialogPreference.ValueProxy() {
            private static final float PERCENTAGE_FLOAT = 100.0f;

            private float getValueFromPercentage(final int percentage) {
                return percentage / PERCENTAGE_FLOAT;
            }

            private int getPercentageFromValue(final float floatValue) {
                return Math.round(floatValue * PERCENTAGE_FLOAT);
            }

            @Override
            public void writeValue(final int value, final String key) {
                prefs.edit().putFloat(key, getValueFromPercentage(value)).apply();
            }

            @Override
            public void writeDefaultValue(final String key) {
                prefs.edit().remove(key).apply();
            }

            @Override
            public int readValue(final String key) {
                return getPercentageFromValue(Settings.readKeypressSoundVolume(prefs));
            }

            @Override
            public int readDefaultValue(final String key) {
                return getPercentageFromValue(Settings.readDefaultKeypressSoundVolume());
            }

            @Override
            public String getValueText(final int value) {
                if (value < 0) {
                    return res.getString(R.string.settings_system_default);
                }
                return res.getString(R.string.abbreviation_unit_percent, value);
            }

            @Override
            public void feedbackValue(final int value) {
                AudioAndHapticFeedbackManager.getInstance().playSoundEffect(
                        AudioManager.FX_KEYPRESS_STANDARD, getValueFromPercentage(value));
            }
        });
    }

    private void setupKeyLongpressTimeoutSettings() {
        final SeekBarDialogPreference pref = (SeekBarDialogPreference)findPreference(
                Settings.PREF_KEY_LONGPRESS_TIMEOUT);
        if (pref == null) {
            return;
        }
        final SharedPreferences prefs = getSharedPreferences();
        final Resources res = getResources();
        pref.setInterface(new SeekBarDialogPreference.ValueProxy() {
            @Override
            public void writeValue(final int value, final String key) {
                prefs.edit().putInt(key, value).apply();
            }

            @Override
            public void writeDefaultValue(final String key) {
                prefs.edit().remove(key).apply();
            }

            @Override
            public int readValue(final String key) {
                return Settings.readKeyLongpressTimeout(prefs, res);
            }

            @Override
            public int readDefaultValue(final String key) {
                return Settings.readDefaultKeyLongpressTimeout(res);
            }

            @Override
            public String getValueText(final int value) {
                return res.getString(R.string.abbreviation_unit_milliseconds, value);
            }

            @Override
            public void feedbackValue(final int value) {}
        });
    }

    private void setupKeypressSoundpackSettings() {
        final android.preference.ListPreference pref = (android.preference.ListPreference) findPreference(
                Settings.PREF_KEYPRESS_SOUNDPACK);
        if (pref == null) {
            return;
        }
        final Context context = getActivity();
        if (context == null) return;
        File soundpacksDir = context.getExternalFilesDir("soundpacks");
        
        List<String> names = new ArrayList<>();
        List<String> values = new ArrayList<>();

        // Built-in Defaults
        names.add("System Click (Standard)");
        values.add("default");

        names.add("Bubble Wrap (Synthesized)");
        values.add("default_deep");
        
        // Scan for downloaded & custom soundpacks
        if (soundpacksDir != null && soundpacksDir.exists()) {
            File[] dirs = soundpacksDir.listFiles(File::isDirectory);
            if (dirs != null) {
                for (File dir : dirs) {
                    String folderName = dir.getName();
                    if (!folderName.equals("default") && !folderName.equals("default_deep")) {
                        String displayName = readSoundpackDisplayName(dir, folderName);
                        names.add(displayName);
                        values.add(folderName);
                    }
                }
            }
        }

        pref.setEntries(names.toArray(new CharSequence[0]));
        pref.setEntryValues(values.toArray(new CharSequence[0]));
        
        String currentVal = pref.getValue();
        if (currentVal == null) {
            currentVal = "default";
            pref.setValue("default");
        }
        updateSoundpackSummary(pref, currentVal, names, values);

        pref.setOnPreferenceChangeListener((preference, newValue) -> {
            String newValStr = (String) newValue;
            updateSoundpackSummary(pref, newValStr, names, values);
            return true;
        });
    }

    private String readSoundpackDisplayName(File dir, String folderName) {
        // 1. Check name.txt
        File nameFile = new File(dir, "name.txt");
        if (nameFile.exists()) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(nameFile), "UTF-8"))) {
                String line = br.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    return line.trim();
                }
            } catch (Exception ignored) {}
        }

        // 2. Check config.json
        File configFile = new File(dir, "config.json");
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
                JSONObject json = new JSONObject(baos.toString("UTF-8"));
                String name = json.optString("name", "");
                if (!name.trim().isEmpty()) {
                    return name.trim();
                }
            } catch (Exception ignored) {}
        }

        // 3. Check official SoundpackCatalog resolver
        return SoundpackCatalog.resolveName(folderName);
    }

    private void updateSoundpackSummary(android.preference.ListPreference pref, String value, List<String> names, List<String> values) {
        int idx = values.indexOf(value);
        if (idx != -1) {
            pref.setSummary(names.get(idx));
        } else {
            pref.setSummary(SoundpackCatalog.resolveName(value));
        }
    }
}
