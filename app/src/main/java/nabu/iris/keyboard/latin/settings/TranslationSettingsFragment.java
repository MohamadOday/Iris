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

        final Preference clearPref = findPreference(KEY_CLEAR_MODELS);
        if (clearPref != null) {
            clearPref.setOnPreferenceClickListener(pref -> {
                clearDownloadedMlKitModels();
                return true;
            });
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

        try {
            com.google.mlkit.common.model.RemoteModelManager modelManager = 
                    com.google.mlkit.common.model.RemoteModelManager.getInstance();
            
            modelManager.getDownloadedModels(com.google.mlkit.nl.translate.TranslateRemoteModel.class)
                    .addOnSuccessListener(models -> {
                        if (models == null || models.isEmpty()) {
                            mHandler.post(() -> {
                                Toast.makeText(getActivity(), "No offline models to clear.", Toast.LENGTH_SHORT).show();
                                if (clearPref != null) {
                                    clearPref.setSummary("Deletes all downloaded local ML Kit translation models to reclaim space and RAM (~30MB+ per model).");
                                    clearPref.setEnabled(true);
                                }
                            });
                            return;
                        }

                        int count = models.size();
                        final java.util.concurrent.atomic.AtomicInteger deletedCount = new java.util.concurrent.atomic.AtomicInteger(0);

                        for (com.google.mlkit.nl.translate.TranslateRemoteModel model : models) {
                            modelManager.deleteDownloadedModel(model)
                                    .addOnCompleteListener(task -> {
                                        int deleted = deletedCount.incrementAndGet();
                                        if (deleted == count) {
                                            mHandler.post(() -> {
                                                Toast.makeText(getActivity(), "Cleared " + count + " offline model(s) successfully!", Toast.LENGTH_SHORT).show();
                                                if (clearPref != null) {
                                                    clearPref.setSummary("Deletes all downloaded local ML Kit translation models to reclaim space and RAM (~30MB+ per model).");
                                                    clearPref.setEnabled(true);
                                                }
                                            });
                                        }
                                    });
                        }
                    })
                    .addOnFailureListener(e -> mHandler.post(() -> {
                        Toast.makeText(getActivity(), "Failed to retrieve models: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        if (clearPref != null) {
                            clearPref.setSummary("Deletes all downloaded local ML Kit translation models to reclaim space and RAM (~30MB+ per model).");
                            clearPref.setEnabled(true);
                        }
                    }));
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Error clearing models: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            if (clearPref != null) {
                clearPref.setSummary("Deletes all downloaded local ML Kit translation models to reclaim space and RAM (~30MB+ per model).");
                clearPref.setEnabled(true);
            }
        }
    }
}
