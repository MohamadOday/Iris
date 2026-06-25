/*
 * Copyright (C) 2026 Latin IME Customizer
 */

package nabu.iris.keyboard.latin;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import nabu.iris.keyboard.R;
import nabu.iris.keyboard.compat.PreferenceManagerCompat;
import nabu.iris.keyboard.latin.settings.Settings;

/**
 * Helper class to manage the In-Keyboard AI settings panel for choosing providers (Ollama/Gemini/Custom) and saving API credentials.
 */
public final class AiSettingsHelper {
    private final ClipboardBarController mController;
    private final Context mContext;

    private final LinearLayout mAiSettingsPanel;
    private final TextView mSettingsPanelTitle;
    private final TextView mSetupProvOllama;
    private final TextView mSetupProvGemini;
    private final TextView mSetupProvCustom;
    private final LinearLayout mSetupGeminiContainer;
    private final EditText mSetupGeminiKey;
    private final LinearLayout mSetupHostContainer;
    private final EditText mSetupHostUrl;
    private final LinearLayout mSetupModelContainer;
    private final EditText mSetupModelName;
    private final LinearLayout mSetupHeadersContainer;
    private final EditText mSetupHeadersJson;
    private final TextView mSetupBackBtn;
    private final TextView mSetupSaveBtn;

    private final TextView mSetupGeminiLabel;
    private final TextView mSetupHostLabel;
    private final TextView mSetupModelLabel;
    private final TextView mSetupHeadersLabel;

    private String mConfigProvider = "ollama";

    public AiSettingsHelper(ClipboardBarController controller, View inputView) {
        mController = controller;
        mContext = controller.getContext();

        mAiSettingsPanel = inputView.findViewById(R.id.ai_settings_panel);
        mSettingsPanelTitle = inputView.findViewById(R.id.settings_panel_title);
        mSetupProvOllama = inputView.findViewById(R.id.setup_prov_ollama);
        mSetupProvGemini = inputView.findViewById(R.id.setup_prov_gemini);
        mSetupProvCustom = inputView.findViewById(R.id.setup_prov_custom);
        mSetupGeminiContainer = inputView.findViewById(R.id.setup_gemini_container);
        mSetupGeminiKey = inputView.findViewById(R.id.setup_gemini_key);
        mSetupHostContainer = inputView.findViewById(R.id.setup_host_container);
        mSetupHostUrl = inputView.findViewById(R.id.setup_host_url);
        mSetupModelContainer = inputView.findViewById(R.id.setup_model_container);
        mSetupModelName = inputView.findViewById(R.id.setup_model_name);
        mSetupHeadersContainer = inputView.findViewById(R.id.setup_headers_container);
        mSetupHeadersJson = inputView.findViewById(R.id.setup_headers_json);
        mSetupBackBtn = inputView.findViewById(R.id.setup_back_btn);
        mSetupSaveBtn = inputView.findViewById(R.id.setup_save_btn);

        mSetupGeminiLabel = inputView.findViewById(R.id.setup_gemini_label);
        mSetupHostLabel = inputView.findViewById(R.id.setup_host_label);
        mSetupModelLabel = inputView.findViewById(R.id.setup_model_label);
        mSetupHeadersLabel = inputView.findViewById(R.id.setup_headers_label);

        mController.configureSimulatedInput(mSetupGeminiKey);
        mController.configureSimulatedInput(mSetupHostUrl);
        mController.configureSimulatedInput(mSetupModelName);
        mController.configureSimulatedInput(mSetupHeadersJson);

        setupSettingsPanelActions();
    }

    public EditText getSetupGeminiKey() { return mSetupGeminiKey; }
    public EditText getSetupHostUrl() { return mSetupHostUrl; }
    public EditText getSetupModelName() { return mSetupModelName; }
    public EditText getSetupHeadersJson() { return mSetupHeadersJson; }
    public TextView getSetupSaveBtn() { return mSetupSaveBtn; }

    private void setupSettingsPanelActions() {
        if (mSetupProvOllama != null) {
            mSetupProvOllama.setOnClickListener(v -> switchConfigProvider("ollama"));
        }
        if (mSetupProvGemini != null) {
            mSetupProvGemini.setOnClickListener(v -> switchConfigProvider("gemini"));
        }
        if (mSetupProvCustom != null) {
            mSetupProvCustom.setOnClickListener(v -> switchConfigProvider("custom"));
        }

        if (mSetupBackBtn != null) {
            mSetupBackBtn.setOnClickListener(v -> mController.showAiCopilot());
        }

        if (mSetupSaveBtn != null) {
            mSetupSaveBtn.setOnClickListener(v -> saveAiConfiguration());
        }
    }

    public void showAiSettings() {
        SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(mContext);
        mConfigProvider = Settings.readAiProvider(prefs);
        
        if (mSetupGeminiKey != null) mSetupGeminiKey.setText(Settings.readGeminiKey(prefs));
        
        if ("custom".equals(mConfigProvider)) {
            if (mSetupHostUrl != null) mSetupHostUrl.setText(Settings.readCustomUrl(prefs));
            if (mSetupModelName != null) mSetupModelName.setText(Settings.readCustomModel(prefs));
        } else {
            if (mSetupHostUrl != null) mSetupHostUrl.setText(Settings.readOllamaUrl(prefs));
            if (mSetupModelName != null) mSetupModelName.setText(Settings.readOllamaModel(prefs));
        }
        if (mSetupHeadersJson != null) mSetupHeadersJson.setText(Settings.readCustomHeaders(prefs));

        updateSegmentedProviderButtons();
        toggleSettingsContainers();

        if ("gemini".equals(mConfigProvider)) {
            mController.setActiveInput(mSetupGeminiKey);
        } else {
            mController.setActiveInput(mSetupHostUrl);
        }
    }

    private void switchConfigProvider(String provider) {
        mConfigProvider = provider;
        
        SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(mContext);
        if ("custom".equals(provider)) {
            if (mSetupHostUrl != null) mSetupHostUrl.setText(Settings.readCustomUrl(prefs));
            if (mSetupModelName != null) mSetupModelName.setText(Settings.readCustomModel(prefs));
        } else {
            if (mSetupHostUrl != null) mSetupHostUrl.setText(Settings.readOllamaUrl(prefs));
            if (mSetupModelName != null) mSetupModelName.setText(Settings.readOllamaModel(prefs));
        }

        updateSegmentedProviderButtons();
        toggleSettingsContainers();
    }

    private void updateSegmentedProviderButtons() {
        SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(mContext);
        int customColor = Settings.readKeyboardColor(prefs, mContext);
        int backgroundColor = mController.getKeyboardBackgroundColor();
        boolean isDark = mController.isColorDark(backgroundColor);

        int normalColor = isDark ? 0xAAFFFFFF : 0x88000000;
        int activeColor = customColor;
        if (activeColor == 0 || mController.isColorMonochromeOrTooDark(activeColor)) {
            activeColor = mContext.getResources().getColor(R.color.settings_accent);
        }

        styleSegmentedButton(mSetupProvOllama, "ollama".equals(mConfigProvider), activeColor, isDark, normalColor);
        styleSegmentedButton(mSetupProvGemini, "gemini".equals(mConfigProvider), activeColor, isDark, normalColor);
        styleSegmentedButton(mSetupProvCustom, "custom".equals(mConfigProvider), activeColor, isDark, normalColor);
    }

    private void styleSegmentedButton(TextView v, boolean isActive, int accentColor, boolean isDark, int normalColor) {
        if (v == null) return;
        v.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        v.setTextColor(isActive ? accentColor : normalColor);

        GradientDrawable badge = new GradientDrawable();
        badge.setShape(GradientDrawable.RECTANGLE);
        badge.setCornerRadius(mController.dpToPx(16));
        if (isActive) {
            badge.setColor(mController.getTranslucentColor(accentColor, 24));
        } else {
            badge.setColor(isDark ? 0x0CFFFFFF : 0x05000000);
        }
        v.setBackground(badge);
    }

    private void toggleSettingsContainers() {
        if (mSetupGeminiContainer == null) return;

        if ("gemini".equals(mConfigProvider)) {
            mSetupGeminiContainer.setVisibility(View.VISIBLE);
            mSetupHostContainer.setVisibility(View.GONE);
            mSetupModelContainer.setVisibility(View.GONE);
            mSetupHeadersContainer.setVisibility(View.GONE);
        } else if ("custom".equals(mConfigProvider)) {
            mSetupGeminiContainer.setVisibility(View.GONE);
            mSetupHostContainer.setVisibility(View.VISIBLE);
            mSetupModelContainer.setVisibility(View.VISIBLE);
            mSetupHeadersContainer.setVisibility(View.VISIBLE);
            
            if (mSetupHostUrl != null) mSetupHostUrl.setHint("https://api.openai.com/v1");
        } else { // ollama
            mSetupGeminiContainer.setVisibility(View.GONE);
            mSetupHostContainer.setVisibility(View.VISIBLE);
            mSetupModelContainer.setVisibility(View.VISIBLE);
            mSetupHeadersContainer.setVisibility(View.GONE);

            if (mSetupHostUrl != null) mSetupHostUrl.setHint("http://localhost:11434");
        }
    }

    private void saveAiConfiguration() {
        SharedPreferences.Editor editor = PreferenceManagerCompat.getDeviceSharedPreferences(mContext).edit();
        editor.putString(Settings.PREF_AI_PROVIDER, mConfigProvider);

        if ("gemini".equals(mConfigProvider)) {
            if (mSetupGeminiKey != null) {
                editor.putString(Settings.PREF_GEMINI_KEY, mSetupGeminiKey.getText().toString().trim());
            }
        } else if ("custom".equals(mConfigProvider)) {
            if (mSetupHostUrl != null) {
                editor.putString(Settings.PREF_CUSTOM_URL, mSetupHostUrl.getText().toString().trim());
            }
            if (mSetupModelName != null) {
                editor.putString(Settings.PREF_CUSTOM_MODEL, mSetupModelName.getText().toString().trim());
            }
            if (mSetupHeadersJson != null) {
                editor.putString(Settings.PREF_CUSTOM_HEADERS, mSetupHeadersJson.getText().toString().trim());
            }
        } else { // ollama
            if (mSetupHostUrl != null) {
                editor.putString(Settings.PREF_OLLAMA_URL, mSetupHostUrl.getText().toString().trim());
            }
            if (mSetupModelName != null) {
                editor.putString(Settings.PREF_OLLAMA_MODEL, mSetupModelName.getText().toString().trim());
            }
        }

        editor.apply();
        Toast.makeText(mContext, "AI Config Saved!", Toast.LENGTH_SHORT).show();
        mController.showAiCopilot();
    }

    public void applyTheming(int accentColor, boolean isDark, int textColor, int hintColor) {
        if (mAiSettingsPanel != null) {
            mAiSettingsPanel.setBackgroundColor(mController.getKeyboardBackgroundColor());
        }
        if (mSettingsPanelTitle != null) {
            mSettingsPanelTitle.setTextColor(textColor);
        }

        mController.styleConfigField(mSetupGeminiKey, mController.getActiveInput() == mSetupGeminiKey);
        mController.styleConfigField(mSetupHostUrl, mController.getActiveInput() == mSetupHostUrl);
        mController.styleConfigField(mSetupModelName, mController.getActiveInput() == mSetupModelName);
        mController.styleConfigField(mSetupHeadersJson, mController.getActiveInput() == mSetupHeadersJson);

        if (mSetupGeminiLabel != null) mSetupGeminiLabel.setTextColor(hintColor);
        if (mSetupHostLabel != null) mSetupHostLabel.setTextColor(hintColor);
        if (mSetupModelLabel != null) mSetupModelLabel.setTextColor(hintColor);
        if (mSetupHeadersLabel != null) mSetupHeadersLabel.setTextColor(hintColor);

        if (mSetupSaveBtn != null) {
            GradientDrawable svBg = new GradientDrawable();
            svBg.setShape(GradientDrawable.RECTANGLE);
            svBg.setCornerRadius(mController.dpToPx(16));
            svBg.setColor(accentColor);
            mSetupSaveBtn.setBackground(svBg);
            mSetupSaveBtn.setTextColor(Color.WHITE);
        }
        if (mSetupBackBtn != null) {
            mSetupBackBtn.setTextColor(textColor);
            mSetupBackBtn.setTextSize(22);
            GradientDrawable bkBg = new GradientDrawable();
            bkBg.setShape(GradientDrawable.RECTANGLE);
            bkBg.setCornerRadius(mController.dpToPx(16));
            bkBg.setColor(isDark ? 0x0AFFFFFF : 0x05000000);
            mSetupBackBtn.setBackground(bkBg);
        }
        
        updateSegmentedProviderButtons();
    }
}
