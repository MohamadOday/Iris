/*
 * Copyright (C) 2026 Latin IME Customizer
 */

package nabu.iris.keyboard.latin.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.widget.Toast;

import nabu.iris.keyboard.R;
import nabu.iris.keyboard.latin.MlKitClearCallback;
import nabu.iris.keyboard.latin.MlKitTranslatorWrapper;

/**
 * "Translation Settings" sub screen.
 */
public final class TranslationSettingsFragment extends SubScreenFragment {

    private static final String KEY_CLEAR_MODELS = "pref_translate_clear_models";
    private static final String KEY_CUSTOM_PROMPT = "pref_translate_custom_prompt";
    private static final String KEY_AI_PROVIDER = "pref_translate_ai_provider";
    private static final String KEY_TRANSLATE_MODE = "pref_translate_mode";
    private static final String KEY_SHORTCUT_TOGGLE = "pref_utility_show_translate";

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate(final Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.prefs_screen_translation);

        if (!MlKitTranslatorWrapper.isSupported()) {
            final Preference mlkitCategory = findPreference("cat_translate_mlkit");
            if (mlkitCategory != null) {
                getPreferenceScreen().removePreference(mlkitCategory);
            }
            final ListPreference modePref = (ListPreference) findPreference(KEY_TRANSLATE_MODE);
            if (modePref != null) {
                final CharSequence[] entries = modePref.getEntries();
                final CharSequence[] entryValues = modePref.getEntryValues();
                if (entries != null && entryValues != null) {
                    final java.util.List<CharSequence> newEntries = new java.util.ArrayList<>();
                    final java.util.List<CharSequence> newValues = new java.util.ArrayList<>();
                    for (int i = 0; i < entryValues.length; i++) {
                        if (!"mlkit".equals(entryValues[i].toString())) {
                            newEntries.add(entries[i]);
                            newValues.add(entryValues[i]);
                        }
                    }
                    modePref.setEntries(newEntries.toArray(new CharSequence[0]));
                    modePref.setEntryValues(newValues.toArray(new CharSequence[0]));
                    if ("mlkit".equals(modePref.getValue())) {
                        modePref.setValue("scraping");
                    }
                }
            }
        } else {
            final Preference clearPref = findPreference(KEY_CLEAR_MODELS);
            if (clearPref != null) {
                clearPref.setOnPreferenceClickListener(pref -> {
                    clearDownloadedMlKitModels();
                    return true;
                });
            }
        }

        updateAllSummaries();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateAllSummaries();
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences prefs, final String key) {
        updateAllSummaries();
    }

    private void updateAllSummaries() {
        // Mode
        updateListSummary(KEY_TRANSLATE_MODE);
        // AI Provider
        updateListSummary(KEY_AI_PROVIDER);
        // Custom Prompt
        updateEditTextSummary(KEY_CUSTOM_PROMPT, "Default prompt template");
    }

    private void updateListSummary(String key) {
        final ListPreference pref = (ListPreference) findPreference(key);
        if (pref != null && pref.getEntry() != null) {
            pref.setSummary(pref.getEntry());
        }
    }

    private void updateEditTextSummary(String key, String defaultDisplay) {
        final EditTextPreference pref = (EditTextPreference) findPreference(key);
        if (pref == null) return;
        String value = pref.getText();
        if (value == null || value.trim().isEmpty()) {
            pref.setSummary(defaultDisplay);
        } else {
            if (value.length() > 60) {
                pref.setSummary(value.substring(0, 57) + "...");
            } else {
                pref.setSummary(value);
            }
        }
    }

    private void clearDownloadedMlKitModels() {
        if (getActivity() == null) return;
        
        final Preference clearPref = findPreference(KEY_CLEAR_MODELS);
        if (clearPref != null) {
            clearPref.setSummary("Clearing offline models...");
            clearPref.setEnabled(false);
        }

        MlKitTranslatorWrapper.clearDownloadedModels(new MlKitClearCallback() {
            @Override
            public void onSuccess(final int count) {
                mHandler.post(() -> {
                    Toast.makeText(getActivity(), "Cleared " + count + " offline model(s) successfully!", Toast.LENGTH_SHORT).show();
                    if (clearPref != null) {
                        clearPref.setSummary("Deletes all downloaded local ML Kit translation models to reclaim space and RAM (~30MB+ per model).");
                        clearPref.setEnabled(true);
                    }
                });
            }

            @Override
            public void onNoModels() {
                mHandler.post(() -> {
                    Toast.makeText(getActivity(), "No offline models to clear.", Toast.LENGTH_SHORT).show();
                    if (clearPref != null) {
                        clearPref.setSummary("Deletes all downloaded local ML Kit translation models to reclaim space and RAM (~30MB+ per model).");
                        clearPref.setEnabled(true);
                    }
                });
            }

            @Override
            public void onFailure(final String errorMessage) {
                mHandler.post(() -> {
                    Toast.makeText(getActivity(), "Failed to clear models: " + errorMessage, Toast.LENGTH_SHORT).show();
                    if (clearPref != null) {
                        clearPref.setSummary("Deletes all downloaded local ML Kit translation models to reclaim space and RAM (~30MB+ per model).");
                        clearPref.setEnabled(true);
                    }
                });
            }
        });
    }
}
