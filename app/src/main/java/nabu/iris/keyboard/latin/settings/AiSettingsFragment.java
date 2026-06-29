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
 * Dynamically shows/hides provider-specific categories based on the active AI provider,
 * updates summaries with live values, and provides a connection test feature.
 */
public final class AiSettingsFragment extends SubScreenFragment {

    // Preference keys for categories (to show/hide dynamically)
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

        setupPreferenceInterceptors();

        // Wire up test connection button
        final Preference testPref = findPreference(KEY_TEST_CONNECTION);
        // Note: The listener will be wrapped inside setupPreferenceInterceptors.
        // We only set the original click listener here.
        if (testPref != null) {
            testPref.setOnPreferenceClickListener(pref -> {
                runConnectionTest();
                return true;
            });
        }

        // Initial dynamic visibility + summaries
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
        // Refresh visibility when the provider changes
        if (Settings.PREF_AI_PROVIDER.equals(key)) {
            updateProviderVisibility();
        }
        // Always refresh summaries for any AI key change
        updateAllSummaries();
    }

    private void setupPreferenceInterceptors() {
        final String[] allDependentKeys = {
            "pref_ai_provider", "pref_ai_system_prompt",
            "pref_gemini_key", "pref_gemini_model",
            "pref_ollama_url", "pref_ollama_model",
            "pref_custom_url", "pref_custom_model", "pref_custom_headers",
            "pref_ai_temperature", "pref_ai_max_tokens", "pref_ai_skip_params", "pref_ai_test_connection"
        };

        for (final String key : allDependentKeys) {
            final Preference pref = findPreference(key);
            if (pref == null) continue;

            final Preference.OnPreferenceClickListener originalListener = pref.getOnPreferenceClickListener();
            pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference p) {
                    final SharedPreferences prefs = getSharedPreferences();
                    boolean aiEnabled = prefs.getBoolean(KEY_AI_ENABLED, true);
                    if (!aiEnabled) {
                        Toast.makeText(getActivity(), "Please enable AI Copilot first", Toast.LENGTH_SHORT).show();
                        return true; // Intercept click
                    }

                    // Check provider-specific constraints
                    String provider = Settings.readAiProvider(prefs);
                    if (key.startsWith("pref_gemini_") && !"gemini".equals(provider)) {
                        Toast.makeText(getActivity(), "Please set AI Provider to 'Gemini Cloud' first", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    if (key.startsWith("pref_ollama_") && !"ollama".equals(provider)) {
                        Toast.makeText(getActivity(), "Please set AI Provider to 'Local Ollama' first", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    if (key.startsWith("pref_custom_") && !"custom".equals(provider)) {
                        Toast.makeText(getActivity(), "Please set AI Provider to 'Custom API' first", Toast.LENGTH_SHORT).show();
                        return true;
                    }

                    if (originalListener != null) {
                        return originalListener.onPreferenceClick(p);
                    }
                    return false; // Allow opening dialog
                }
            });
        }
    }

    /**
     * Show only the category relevant to the currently selected provider.
     */
    private void updateProviderVisibility() {
        final SharedPreferences prefs = getSharedPreferences();
        final String provider = Settings.readAiProvider(prefs);

        // Keep all categories fully enabled so they are clickable,
        // but update the ListPreference summary.
        final ListPreference providerPref = (ListPreference) findPreference(Settings.PREF_AI_PROVIDER);
        if (providerPref != null && providerPref.getEntry() != null) {
            providerPref.setSummary(providerPref.getEntry());
        }
    }

    /**
     * Update preference summaries to reflect current saved values.
     * This gives users immediate visual feedback of their configuration.
     */
    private void updateAllSummaries() {
        // Provider
        updateListSummary(Settings.PREF_AI_PROVIDER);

        // Gemini
        updateEditTextSummary(Settings.PREF_GEMINI_KEY, "Not set", true);
        updateEditTextSummary(KEY_GEMINI_MODEL, "gemini-2.5-flash", false);

        // Ollama
        updateEditTextSummary(Settings.PREF_OLLAMA_URL, "http://localhost:11434", false);
        updateEditTextSummary(Settings.PREF_OLLAMA_MODEL, "qwen2.5-coder", false);

        // Custom
        updateEditTextSummary(Settings.PREF_CUSTOM_URL, "https://api.openai.com/v1", false);
        updateEditTextSummary(Settings.PREF_CUSTOM_MODEL, "gpt-4o-mini", false);
        updateEditTextSummary(Settings.PREF_CUSTOM_HEADERS, "Not set", true);

        // System Prompt
        updateEditTextSummary(KEY_SYSTEM_PROMPT, "Default system prompt", false);

        // Temperature
        updateListSummary(KEY_TEMPERATURE);

        // Max tokens
        updateListSummary(KEY_MAX_TOKENS);
    }

    /**
     * Updates an EditTextPreference summary with the current saved value.
     * If isSensitive is true, the value is masked with dots for security.
     */
    private void updateEditTextSummary(String key, String defaultDisplay, boolean isSensitive) {
        final EditTextPreference pref = (EditTextPreference) findPreference(key);
        if (pref == null) return;

        String value = pref.getText();
        if (value == null || value.trim().isEmpty() ||
                value.equals("YOUR_KEY") || value.contains("YOUR_KEY")) {
            pref.setSummary(defaultDisplay);
        } else if (isSensitive) {
            // Mask sensitive values: show first 4 + last 4 chars
            if (value.length() > 10) {
                pref.setSummary(value.substring(0, 4) + "••••••" + value.substring(value.length() - 4));
            } else {
                pref.setSummary("••••••••");
            }
        } else {
            // Truncate long values for clean display
            if (value.length() > 50) {
                pref.setSummary(value.substring(0, 47) + "...");
            } else {
                pref.setSummary(value);
            }
        }
    }

    /**
     * Updates a ListPreference summary with the currently selected entry name.
     */
    private void updateListSummary(String key) {
        final ListPreference pref = (ListPreference) findPreference(key);
        if (pref != null && pref.getEntry() != null) {
            pref.setSummary(pref.getEntry());
        }
    }

    /**
     * Sends a lightweight test prompt to the configured AI provider and shows results in a dialog.
     */
    private void runConnectionTest() {
        if (getActivity() == null) return;

        final Preference testPref = findPreference(KEY_TEST_CONNECTION);
        if (testPref != null) {
            testPref.setSummary("Testing connection...");
            testPref.setEnabled(false);
        }

        final AiCopilotManager manager = new AiCopilotManager(getActivity());
        manager.queryAi("Reply with only: CONNECTION_OK", new AiCopilotManager.AiCallback() {
            @Override
            public void onSuccess(String responseText) {
                if (getActivity() == null) return;
                if (testPref != null) {
                    testPref.setSummary("✓ Connection successful!");
                    testPref.setEnabled(true);
                }
                new AlertDialog.Builder(getActivity())
                        .setTitle("Connection Test — Success")
                        .setMessage("Your AI provider responded successfully!\n\nResponse:\n" + responseText)
                        .setPositiveButton("OK", null)
                        .show();
            }

            @Override
            public void onFailure(String errorMessage) {
                if (getActivity() == null) return;
                if (testPref != null) {
                    testPref.setSummary("✗ Connection failed — tap to retry");
                    testPref.setEnabled(true);
                }
                new AlertDialog.Builder(getActivity())
                        .setTitle("Connection Test — Failed")
                        .setMessage("Could not reach your AI provider.\n\nError:\n" + errorMessage)
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }
}
