/*
 * Copyright (C) 2026 Latin IME Customizer
 */

package nabu.iris.keyboard.latin.settings;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.widget.Toast;

import nabu.iris.keyboard.R;
import nabu.iris.keyboard.latin.AiCopilotManager;

/**
 * "AI Copilot Studio" settings sub screen.
 *
 * Dynamically updates summaries with live values and provides a connection test feature.
 */
public final class AiSettingsFragment extends SubScreenFragment {

    private static final String CAT_GEMINI = "cat_gemini";
    private static final String CAT_OLLAMA = "cat_ollama";
    private static final String CAT_CUSTOM = "cat_custom";
    private static final String KEY_TEST_CONNECTION = "pref_ai_test_connection";
    private static final String KEY_SYSTEM_PROMPT = "pref_ai_system_prompt";
    private static final String KEY_TEMPERATURE = "pref_ai_temperature";
    private static final String KEY_MAX_TOKENS = "pref_ai_max_tokens";
    private static final String KEY_AI_ENABLED = "pref_ai_enabled";
    private static final String KEY_GEMINI_MODEL = "pref_gemini_model";

    @Override
    public void onCreate(final Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.prefs_screen_ai);

        final Preference testPref = findPreference(KEY_TEST_CONNECTION);
        if (testPref != null) {
            testPref.setOnPreferenceClickListener(pref -> {
                runConnectionTest();
                return true;
            });
        }

        updateProviderVisibility();
        updateAllSummaries();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateProviderVisibility();
        updateAllSummaries();
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences prefs, final String key) {
        if (KEY_AI_ENABLED.equals(key)) {
            boolean enabled = prefs.getBoolean(KEY_AI_ENABLED, true);
            prefs.edit().putBoolean("pref_utility_show_ai", enabled).apply();
            updateProviderVisibility();
        }
        if (Settings.PREF_AI_PROVIDER.equals(key)) {
            updateProviderVisibility();
        }
        updateAllSummaries();
    }

    private void updateProviderVisibility() {
        final SharedPreferences prefs = getSharedPreferences();
        final boolean aiEnabled = prefs.getBoolean(KEY_AI_ENABLED, true);
        final String provider = Settings.readAiProvider(prefs);

        final ListPreference providerPref = (ListPreference) findPreference(Settings.PREF_AI_PROVIDER);
        if (providerPref != null) {
            providerPref.setEnabled(aiEnabled);
            if (providerPref.getEntry() != null) {
                providerPref.setSummary(providerPref.getEntry());
            }
        }

        final Preference sysPromptPref = findPreference(KEY_SYSTEM_PROMPT);
        if (sysPromptPref != null) {
            sysPromptPref.setEnabled(aiEnabled);
        }

        final PreferenceCategory catGemini = (PreferenceCategory) findPreference(CAT_GEMINI);
        if (catGemini != null) {
            catGemini.setEnabled(aiEnabled && "gemini".equals(provider));
        }

        final PreferenceCategory catOllama = (PreferenceCategory) findPreference(CAT_OLLAMA);
        if (catOllama != null) {
            catOllama.setEnabled(aiEnabled && "ollama".equals(provider));
        }

        final PreferenceCategory catCustom = (PreferenceCategory) findPreference(CAT_CUSTOM);
        if (catCustom != null) {
            catCustom.setEnabled(aiEnabled && "custom".equals(provider));
        }

        final Preference testPref = findPreference(KEY_TEST_CONNECTION);
        if (testPref != null) {
            testPref.setEnabled(aiEnabled);
        }
    }

    private void updateAllSummaries() {
        updateListSummary(Settings.PREF_AI_PROVIDER);

        updateEditTextSummary(Settings.PREF_GEMINI_KEY, "Not set", true);
        updateEditTextSummary(KEY_GEMINI_MODEL, "gemini-2.5-flash", false);

        updateEditTextSummary(Settings.PREF_OLLAMA_URL, "http://localhost:11434", false);
        updateEditTextSummary(Settings.PREF_OLLAMA_MODEL, "qwen2.5-coder", false);

        updateEditTextSummary(Settings.PREF_CUSTOM_URL, "https://api.openai.com/v1", false);
        updateEditTextSummary(Settings.PREF_CUSTOM_MODEL, "gpt-4o-mini", false);
        updateEditTextSummary(Settings.PREF_CUSTOM_HEADERS, "Not set", true);

        updateEditTextSummary(KEY_SYSTEM_PROMPT, "Default system prompt", false);

        updateListSummary(KEY_TEMPERATURE);
        updateListSummary(KEY_MAX_TOKENS);
    }

    private void updateEditTextSummary(String key, String defaultDisplay, boolean isSensitive) {
        final EditTextPreference pref = (EditTextPreference) findPreference(key);
        if (pref == null) return;

        String value = pref.getText();
        if (value == null || value.trim().isEmpty()) {
            pref.setSummary(defaultDisplay);
        } else if (isSensitive) {
            if (value.length() > 8) {
                pref.setSummary("••••••••" + value.substring(value.length() - 4));
            } else {
                pref.setSummary("••••••••");
            }
        } else {
            pref.setSummary(value);
        }
    }

    private void updateListSummary(String key) {
        final ListPreference pref = (ListPreference) findPreference(key);
        if (pref != null && pref.getEntry() != null) {
            pref.setSummary(pref.getEntry());
        }
    }

    private void runConnectionTest() {
        if (getActivity() == null) return;

        final SharedPreferences prefs = getSharedPreferences();
        final String provider = Settings.readAiProvider(prefs);

        final AlertDialog loadingDialog = new AlertDialog.Builder(getActivity())
                .setTitle("Testing Connection")
                .setMessage("Sending ping request to " + provider.toUpperCase() + " endpoint...")
                .setCancelable(false)
                .show();

        new AiCopilotManager(getActivity()).queryAi(
                "Hi! Please reply with exactly one word: 'CONNECTED'",
                new AiCopilotManager.AiCallback() {
                    @Override
                    public void onSuccess(final String text) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            loadingDialog.dismiss();
                            new AlertDialog.Builder(getActivity())
                                    .setTitle("Connection Successful! ✓")
                                    .setMessage("Response received from " + provider.toUpperCase() + ":\n\n\"" + text.trim() + "\"")
                                    .setPositiveButton(android.R.string.ok, null)
                                    .show();
                        });
                    }

                    @Override
                    public void onFailure(final String errorMessage) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            loadingDialog.dismiss();
                            new AlertDialog.Builder(getActivity())
                                    .setTitle("Connection Failed")
                                    .setMessage("Could not connect to " + provider.toUpperCase() + ":\n\n" + errorMessage)
                                    .setPositiveButton(android.R.string.ok, null)
                                    .show();
                        });
                    }
                }
        );
    }
}
