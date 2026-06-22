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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Bundle;

import nabu.iris.keyboard.R;
import nabu.iris.keyboard.latin.AudioAndHapticFeedbackManager;

/**
 * "Preferences" settings sub screen.
 *
 * This settings sub screen handles the following input preferences.
 * - Vibrate on keypress
 * - Keypress vibration duration
 * - Sound on keypress
 * - Keypress sound volume
 * - Popup on keypress
 * - Key long press delay
 */
public final class KeyPressSettingsFragment extends SubScreenFragment {
    @Override
    public void onCreate(final Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.prefs_screen_key_press);

        final Context context = getActivity();

        // When we are called from the Settings application but we are not already running, some
        // singleton and utility classes may not have been initialized.  We have to call
        // initialization method of these classes here. See {@link LatinIME#onCreate()}.
        AudioAndHapticFeedbackManager.init(context);

        if (!AudioAndHapticFeedbackManager.getInstance().hasVibrator()) {
            removePreference(Settings.PREF_VIBRATE_ON);
        }

        setupKeypressSoundVolumeSettings();
        setupKeypressSoundpackSettings();
        setupKeyLongpressTimeoutSettings();
    }

    @Override
    public void onResume() {
        super.onResume();
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
                return (int)(floatValue * PERCENTAGE_FLOAT);
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
                return Integer.toString(value);
            }

            @Override
            public void feedbackValue(final int value) {
                AudioAndHapticFeedbackManager.getInstance().playSoundEffect(
                        AudioManager.FX_KEYPRESS_STANDARD, getValueFromPercentage(value));
            }
        });
    }

    private void setupKeyLongpressTimeoutSettings() {
        final SharedPreferences prefs = getSharedPreferences();
        final Resources res = getResources();
        final SeekBarDialogPreference pref = (SeekBarDialogPreference)findPreference(
                Settings.PREF_KEY_LONGPRESS_TIMEOUT);
        if (pref == null) {
            return;
        }
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
        java.io.File soundpacksDir = context.getExternalFilesDir("soundpacks");
        
        java.util.List<String> names = new java.util.ArrayList<>();
        java.util.List<String> values = new java.util.ArrayList<>();

        // Add defaults first
        names.add("iOS (Apple Inc. - Sampled)");
        values.add("default");

        names.add("iOS Deep (Apple Inc. - Sampled)");
        values.add("default_deep");
        
        // Scan for user custom folders
        if (soundpacksDir != null && soundpacksDir.exists()) {
            java.io.File[] dirs = soundpacksDir.listFiles(java.io.File::isDirectory);
            if (dirs != null) {
                for (java.io.File dir : dirs) {
                    String folderName = dir.getName();
                    // Avoid duplicating builtin defaults
                    if (!folderName.equals("default") &&
                        !folderName.equals("default_deep")) {
                        
                        String displayName = null;
                        java.io.File configFile = new java.io.File(dir, "config.json");
                        if (configFile.exists()) {
                            try {
                                java.io.FileInputStream fis = new java.io.FileInputStream(configFile);
                                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                                byte[] buffer = new byte[1024];
                                int len;
                                while ((len = fis.read(buffer)) != -1) {
                                    baos.write(buffer, 0, len);
                                }
                                fis.close();
                                org.json.JSONObject json = new org.json.JSONObject(baos.toString("UTF-8"));
                                displayName = json.optString("name");
                            } catch (Exception e) {
                                // ignore
                            }
                        }
                        if (displayName == null || displayName.trim().isEmpty()) {
                            displayName = folderName.replace("_", " ");
                        }
                        names.add("[Custom] " + displayName);
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

    private void updateSoundpackSummary(android.preference.ListPreference pref, String value, java.util.List<String> names, java.util.List<String> values) {
        int idx = values.indexOf(value);
        if (idx != -1) {
            pref.setSummary(names.get(idx));
        } else {
            pref.setSummary("iOS (Apple Inc. - Sampled)");
        }
    }
}
