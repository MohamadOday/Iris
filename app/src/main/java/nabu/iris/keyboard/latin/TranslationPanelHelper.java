/*
 * Copyright (C) 2026 Latin IME Customizer
 */

package nabu.iris.keyboard.latin;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.widget.Toast;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import nabu.iris.keyboard.R;
import nabu.iris.keyboard.compat.PreferenceManagerCompat;
import nabu.iris.keyboard.latin.settings.Settings;

/**
 * Helper class to manage the translation workspace panel supporting Google Translate API queries, MLKit offline database engines, and AI translation prompts.
 */
public final class TranslationPanelHelper {
    private final ClipboardBarController mController;
    private final Context mContext;

    private final LinearLayout mTranslatePanel;
    private final TextView mTranslateSourceBtn;
    private final ImageView mTranslateArrow;
    private final TextView mTranslateTargetBtn;
    private final TextView mTranslateModeBtn;
    private final LinearLayout mTranslateInputContainer;
    private final EditText mTranslateInput;
    private final ImageView mTranslateClearBtn;
    private final TextView mTranslatePasteBtn;
    private final LinearLayout mTranslateResultContainer;
    private final TextView mTranslateResultPreview;
    private final TextView mTranslateInsertBtn;
    private final ProgressBar mTranslateProgressBar;
    private final TextView mTranslateDownloadLabel;

    private int mDownloadProgress = 0;
    private Runnable mDownloadProgressRunnable;

    private String mTranslateSourceLang = "auto";
    private String mTranslateTargetLang = "es";
    private String mTranslateMode = "scraping";

    private final Handler mTranslateHandler = new Handler(Looper.getMainLooper());
    private Runnable mTranslateRunnable;

    private static final int MAX_CACHE_SIZE = 100;
    private static final Map<String, String> sTranslationCache = Collections.synchronizedMap(
            new LinkedHashMap<String, String>(MAX_CACHE_SIZE, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                    return size() > MAX_CACHE_SIZE;
                }
            }
    );

    private static final String[] mLangNames = {"Auto-detect", "English", "Spanish", "French", "German", "Italian", "Portuguese", "Chinese", "Japanese", "Korean", "Russian", "Arabic", "Hindi", "Turkish", "Polish", "Dutch"};
    private static final String[] mLangCodes = {"auto", "en", "es", "fr", "de", "it", "pt", "zh", "ja", "ko", "ru", "ar", "hi", "tr", "pl", "nl"};
    private static final String[] mTgtLangNames = {"English", "Spanish", "French", "German", "Italian", "Portuguese", "Chinese", "Japanese", "Korean", "Russian", "Arabic", "Hindi", "Turkish", "Polish", "Dutch"};
    private static final String[] mTgtLangCodes = {"en", "es", "fr", "de", "it", "pt", "zh", "ja", "ko", "ru", "ar", "hi", "tr", "pl", "nl"};

    public TranslationPanelHelper(ClipboardBarController controller, View inputView) {
        mController = controller;
        mContext = controller.getContext();

        mTranslatePanel = inputView.findViewById(R.id.translate_panel);
        mTranslateSourceBtn = inputView.findViewById(R.id.translate_source_btn);
        mTranslateArrow = inputView.findViewById(R.id.translate_arrow);
        mTranslateTargetBtn = inputView.findViewById(R.id.translate_target_btn);
        mTranslateModeBtn = inputView.findViewById(R.id.translate_mode_btn);
        mTranslateInputContainer = inputView.findViewById(R.id.translate_input_container);
        mTranslateInput = inputView.findViewById(R.id.translate_input);
        mTranslateClearBtn = inputView.findViewById(R.id.translate_clear_btn);
        mTranslatePasteBtn = inputView.findViewById(R.id.translate_paste_btn);
        mTranslateResultContainer = inputView.findViewById(R.id.translate_result_container);
        mTranslateResultPreview = inputView.findViewById(R.id.translate_result_preview);
        mTranslateInsertBtn = inputView.findViewById(R.id.translate_insert_btn);
        mTranslateProgressBar = inputView.findViewById(R.id.translate_progress_bar);
        mTranslateDownloadLabel = inputView.findViewById(R.id.translate_download_label);

        if (mTranslateArrow != null) {
            mTranslateArrow.setOnClickListener(v -> swapLanguages());
        }

        SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(mContext);
        mTranslateSourceLang = prefs.getString("pref_translate_source_lang", "auto");
        mTranslateTargetLang = prefs.getString("pref_translate_target_lang", "es");
        mTranslateMode = prefs.getString("pref_translate_mode", "scraping");

        if (mTranslateSourceBtn != null) {
            mTranslateSourceBtn.setText(getLanguageName(mTranslateSourceLang));
        }
        if (mTranslateTargetBtn != null) {
            mTranslateTargetBtn.setText(getLanguageName(mTranslateTargetLang));
        }
        updateTranslateModeButton();

        mController.configureSimulatedInput(mTranslateInput);

        setupTranslationPanelActions();
    }

    public EditText getTranslateInput() {
        return mTranslateInput;
    }

    public void showTranslatePanel() {
        if (mTranslatePanel != null) {
            mTranslatePanel.setVisibility(View.VISIBLE);
            SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(mContext);
            mTranslateSourceLang = prefs.getString("pref_translate_source_lang", "auto");
            mTranslateTargetLang = prefs.getString("pref_translate_target_lang", "es");
            mTranslateMode = prefs.getString("pref_translate_mode", "scraping");

            if (mTranslateSourceBtn != null) {
                mTranslateSourceBtn.setText(getLanguageName(mTranslateSourceLang));
            }
            if (mTranslateTargetBtn != null) {
                mTranslateTargetBtn.setText(getLanguageName(mTranslateTargetLang));
            }
            updateTranslateModeButton();
            updateInputContainerFocus();

            mController.setActiveInput(mTranslateInput);
            triggerTranslation();
        }
    }

    public void hideTranslatePanel() {
        if (mTranslatePanel != null) {
            mTranslatePanel.setVisibility(View.GONE);
        }
        releaseActiveTranslator();
    }

    private void setupTranslationPanelActions() {
        if (mTranslateSourceBtn != null) {
            mTranslateSourceBtn.setOnClickListener(v -> showLanguageDialog(true));
        }

        if (mTranslateTargetBtn != null) {
            mTranslateTargetBtn.setOnClickListener(v -> showLanguageDialog(false));
        }

        if (mTranslateModeBtn != null) {
            mTranslateModeBtn.setOnClickListener(v -> toggleTranslateMode());
        }

        if (mTranslateClearBtn != null) {
            mTranslateClearBtn.setOnClickListener(v -> {
                if (mTranslateInput != null) {
                    mTranslateInput.setText("");
                }
            });
        }

        if (mTranslatePasteBtn != null && mTranslateInput != null) {
            mTranslatePasteBtn.setOnClickListener(v -> {
                String clipText = mController.getMostRecentClipboardText();
                if (clipText != null && !clipText.isEmpty()) {
                    String text = mTranslateInput.getText().toString();
                    int selStart = mTranslateInput.getSelectionStart();
                    int selEnd = mTranslateInput.getSelectionEnd();
                    if (selStart >= 0 && selEnd >= 0) {
                        int min = Math.min(selStart, selEnd);
                        int max = Math.max(selStart, selEnd);
                        String newText = text.substring(0, min) + clipText + text.substring(max);
                        mTranslateInput.setText(newText);
                        mTranslateInput.setSelection(min + clipText.length());
                    } else {
                        mTranslateInput.append(clipText);
                        mTranslateInput.setSelection(mTranslateInput.getText().length());
                    }
                } else {
                    Toast.makeText(mContext, "Clipboard is empty", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (mTranslateInsertBtn != null) {
            mTranslateInsertBtn.setOnClickListener(v -> {
                if (mTranslateResultPreview != null) {
                    String output = mTranslateResultPreview.getText().toString();
                    if (!output.isEmpty() && !output.startsWith("Error:") && !output.equals("Translating") && !output.equals("Checking offline models")) {
                        if (mContext instanceof LatinIME) {
                            LatinIME ime = (LatinIME) mContext;
                            InputConnection conn = ime.getCurrentInputConnection();
                            if (conn != null) {
                                conn.commitText(output, 1);
                                mController.showKeyboard();
                            }
                        }
                    }
                }
            });
        }

        if (mTranslateInput != null) {
            mTranslateInput.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(final android.text.Editable s) {
                    if (mTranslateClearBtn != null) {
                        mTranslateClearBtn.setVisibility(s != null && s.length() > 0 ? View.VISIBLE : View.GONE);
                    }
                    if (mTranslateRunnable != null) {
                        mTranslateHandler.removeCallbacks(mTranslateRunnable);
                    }
                    mTranslateRunnable = () -> triggerTranslation();
                    mTranslateHandler.postDelayed(mTranslateRunnable, 900);
                }
            });
        }
    }

    private void toggleTranslateMode() {
        if ("scraping".equals(mTranslateMode)) {
            mTranslateMode = "mlkit";
        } else if ("mlkit".equals(mTranslateMode)) {
            mTranslateMode = "ai";
        } else {
            mTranslateMode = "scraping";
        }
        
        SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(mContext);
        prefs.edit().putString("pref_translate_mode", mTranslateMode).apply();
        
        updateTranslateModeButton();
        triggerTranslation();
    }

    private void updateTranslateModeButton() {
        if (mTranslateModeBtn != null) {
            switch (mTranslateMode) {
                case "scraping":
                    mTranslateModeBtn.setText("Mode: (Google Translate)");
                    break;
                case "mlkit":
                    mTranslateModeBtn.setText("Mode: Offline (ML)");
                    break;
                case "ai":
                    mTranslateModeBtn.setText("Mode: AI Copilot");
                    break;
            }
        }
    }

    public void triggerTranslation() {
        if (mTranslateInput == null || mTranslateResultPreview == null || mTranslateInsertBtn == null) return;
        
        final String text = mTranslateInput.getText().toString().trim();
        if (text.isEmpty()) {
            mTranslateResultPreview.setText("");
            mTranslateInsertBtn.setVisibility(View.GONE);
            showTranslateProgress(false);
            return;
        }

        mTranslateResultPreview.setText("Translating");
        mTranslateInsertBtn.setVisibility(View.GONE);
        showTranslateProgress(true);

        if ("scraping".equals(mTranslateMode)) {
            translateViaScraping(text);
        } else if ("mlkit".equals(mTranslateMode)) {
            translateViaMlKit(text);
        } else if ("ai".equals(mTranslateMode)) {
            translateViaAi(text);
        }
    }

    private void translateViaScraping(final String text) {
        final String cacheKey = mTranslateSourceLang + ":" + mTranslateTargetLang + ":" + text;
        String cached = sTranslationCache.get(cacheKey);
        if (cached != null) {
            final String cachedResult = cached;
            mTranslateHandler.post(() -> {
                showTranslateProgress(false);
                mTranslateResultPreview.setText(cachedResult);
                mTranslateInsertBtn.setVisibility(View.VISIBLE);
            });
            return;
        }

        AiCopilotManager.getSharedExecutor().execute(() -> {
            HttpURLConnection conn = null;
            try {
                String encodedText = URLEncoder.encode(text, "UTF-8");
                String urlStr = "https://translate.googleapis.com/translate_a/single?client=dict-chrome-ex&sl="
                        + mTranslateSourceLang + "&tl=" + mTranslateTargetLang + "&dt=t&q=" + encodedText;
                URL url = new URL(urlStr);

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();

                    JSONArray root = new JSONArray(response.toString());
                    JSONArray sentences = root.getJSONArray(0);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < sentences.length(); i++) {
                        JSONArray segment = sentences.getJSONArray(i);
                        if (segment.length() > 0 && !segment.isNull(0)) {
                            sb.append(segment.getString(0));
                        }
                    }
                    String translated = sb.toString().trim();
                    if (!translated.isEmpty()) {
                        sTranslationCache.put(cacheKey, translated);
                        final String finalResult = translated;
                        mTranslateHandler.post(() -> {
                            showTranslateProgress(false);
                            mTranslateResultPreview.setText(finalResult);
                            mTranslateInsertBtn.setVisibility(View.VISIBLE);
                        });
                        return;
                    }
                }
            } catch (Exception e) {
                // Primary endpoint failed, fallback to secondary endpoint
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }

            translateViaFallback(text, cacheKey);
        });
    }

    private void translateViaFallback(final String text, final String cacheKey) {
        HttpURLConnection conn = null;
        try {
            String encodedText = URLEncoder.encode(text, "UTF-8");
            String source = "auto".equals(mTranslateSourceLang) ? "autodetect" : mTranslateSourceLang;
            String pair = source + "|" + mTranslateTargetLang;
            String urlStr = "https://api.mymemory.translated.net/get?q=" + encodedText + "&langpair=" + pair;
            URL url = new URL(urlStr);

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();

                JSONObject json = new JSONObject(response.toString());
                JSONObject responseData = json.optJSONObject("responseData");
                String rawTranslated = responseData != null ? responseData.optString("translatedText") : "";
                if (!rawTranslated.isEmpty() && !rawTranslated.contains("MYMEMORY WARNING")) {
                    final String translated = Html.fromHtml(rawTranslated).toString().trim();
                    sTranslationCache.put(cacheKey, translated);
                    mTranslateHandler.post(() -> {
                        showTranslateProgress(false);
                        mTranslateResultPreview.setText(translated);
                        mTranslateInsertBtn.setVisibility(View.VISIBLE);
                    });
                    return;
                }
            }
            postTranslationFailure("Rate limit encountered. Please retry shortly.");
        } catch (Exception e) {
            postTranslationFailure("Network error: " + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private void translateViaMlKit(final String text) {
        if (!MlKitTranslatorWrapper.isSupported()) {
            postTranslationFailure("Offline translation is not supported in this build flavor.");
            return;
        }

        MlKitTranslatorWrapper.translate(mTranslateSourceLang, mTranslateTargetLang, text, new MlKitCallback() {
            @Override
            public void onSuccess(final String translatedText) {
                mTranslateHandler.post(() -> {
                    showTranslateProgress(false);
                    mTranslateResultPreview.setText(translatedText);
                    mTranslateInsertBtn.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onFailure(final String errorMessage) {
                postTranslationFailure(errorMessage);
            }

            @Override
            public void onDownloadStart() {
                mTranslateHandler.post(() -> {
                    mTranslateResultPreview.setText("");
                    mTranslateInsertBtn.setVisibility(View.GONE);
                    startDownloadProgressAnimation();
                });
            }

            @Override
            public void onDownloadComplete() {
                mTranslateHandler.post(() -> stopDownloadProgressAnimation());
            }

            @Override
            public void onDownloadFailure(final String errorMessage) {
                mTranslateHandler.post(() -> {
                    stopDownloadProgressAnimation();
                    postTranslationFailure(errorMessage);
                });
            }

            @Override
            public void onTranslatingOffline() {
                mTranslateHandler.post(() -> mTranslateResultPreview.setText("Translating offline"));
            }
        });
    }

    private void translateViaAi(final String text) {
        SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(mContext);
        String promptTemplate = prefs.getString("pref_translate_custom_prompt", "").trim();
        if (promptTemplate.isEmpty()) {
            promptTemplate = "Translate the following text to [Language]. Output ONLY the translated text. Do not include any explanations, warnings, headers, greetings, markdown blocks, or surrounding quotes:\n\n[Text]";
        }
        
        String targetLangName = getLanguageName(mTranslateTargetLang);
        String prompt = promptTemplate
                .replace("[Language]", targetLangName)
                .replace("[Lang]", targetLangName)
                .replace("[Text]", text);
        
        String targetProvider = prefs.getString("pref_translate_ai_provider", "active");
        
        AiCopilotManager aiManager = mController.getAiManager();
        if (aiManager != null) {
            aiManager.queryAiWithProvider(targetProvider, prompt, new AiCopilotManager.AiCallback() {
                @Override
                public void onSuccess(String responseText) {
                    String cleaned = responseText.trim();
                    if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length() > 2) {
                        cleaned = cleaned.substring(1, cleaned.length() - 1);
                    }
                    final String result = cleaned;
                    mTranslateHandler.post(() -> {
                        showTranslateProgress(false);
                        mTranslateResultPreview.setText(result);
                        mTranslateInsertBtn.setVisibility(View.VISIBLE);
                    });
                }

                @Override
                public void onFailure(String errorMessage) {
                    postTranslationFailure("AI: " + errorMessage);
                }
            });
        }
    }

    private String getLanguageName(String code) {
        if ("auto".equals(code)) {
            return "Auto-detect";
        }
        switch (code) {
            case "en": return "English";
            case "es": return "Spanish";
            case "fr": return "French";
            case "de": return "German";
            case "it": return "Italian";
            case "pt": return "Portuguese";
            case "zh": return "Chinese";
            case "ja": return "Japanese";
            case "ko": return "Korean";
            case "ru": return "Russian";
            case "ar": return "Arabic";
            case "hi": return "Hindi";
            case "tr": return "Turkish";
            case "pl": return "Polish";
            case "nl": return "Dutch";
            default: return "Spanish";
        }
    }

    private void releaseActiveTranslator() {
        MlKitTranslatorWrapper.release();
    }

    private void startDownloadProgressAnimation() {
        mDownloadProgress = 0;
        if (mTranslateDownloadLabel != null) {
            mTranslateDownloadLabel.setVisibility(View.VISIBLE);
            mTranslateDownloadLabel.setText("Downloading: 0%");
        }
        if (mTranslateProgressBar != null) {
            mTranslateProgressBar.setVisibility(View.VISIBLE);
            mTranslateProgressBar.setProgress(0);
        }
        mDownloadProgressRunnable = new Runnable() {
            @Override
            public void run() {
                if (mDownloadProgress < 90) {
                    int step = mDownloadProgress < 50 ? 4 : 2;
                    mDownloadProgress = Math.min(90, mDownloadProgress + step);
                    if (mTranslateDownloadLabel != null) {
                        mTranslateDownloadLabel.setText("Downloading: " + mDownloadProgress + "%");
                    }
                    if (mTranslateProgressBar != null) {
                        mTranslateProgressBar.setProgress(mDownloadProgress);
                    }
                    mTranslateHandler.postDelayed(this, 400);
                }
            }
        };
        mTranslateHandler.post(mDownloadProgressRunnable);
    }

    private void stopDownloadProgressAnimation() {
        if (mDownloadProgressRunnable != null) {
            mTranslateHandler.removeCallbacks(mDownloadProgressRunnable);
            mDownloadProgressRunnable = null;
        }
        if (mTranslateProgressBar != null) {
            mTranslateProgressBar.setProgress(100);
        }
        if (mTranslateDownloadLabel != null) {
            mTranslateDownloadLabel.setText("Downloading: 100%");
        }
        mTranslateHandler.postDelayed(() -> {
            if (mTranslateDownloadLabel != null) mTranslateDownloadLabel.setVisibility(View.GONE);
            if (mTranslateProgressBar != null) mTranslateProgressBar.setVisibility(View.GONE);
        }, 400);
    }

    private void showTranslateProgress(boolean show) {
        if (mTranslateProgressBar != null) {
            if (show) {
                mTranslateProgressBar.setProgress(50);
                mTranslateProgressBar.setVisibility(View.VISIBLE);
            } else {
                mTranslateProgressBar.setVisibility(View.GONE);
            }
        }
        if (!show && mTranslateDownloadLabel != null) {
            mTranslateDownloadLabel.setVisibility(View.GONE);
        }
    }

    private void postTranslationFailure(final String message) {
        mTranslateHandler.post(() -> {
            showTranslateProgress(false);
            mTranslateResultPreview.setText("Error: " + message);
            mTranslateInsertBtn.setVisibility(View.GONE);
        });
    }

    private void swapLanguages() {
        if ("auto".equals(mTranslateSourceLang)) {
            Toast.makeText(mContext, "Cannot swap when source is Auto-detect", Toast.LENGTH_SHORT).show();
            return;
        }
        String tempCode = mTranslateSourceLang;
        mTranslateSourceLang = mTranslateTargetLang;
        mTranslateTargetLang = tempCode;
        SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(mContext);
        prefs.edit()
                .putString("pref_translate_source_lang", mTranslateSourceLang)
                .putString("pref_translate_target_lang", mTranslateTargetLang)
                .apply();
        if (mTranslateSourceBtn != null) {
            mTranslateSourceBtn.setText(getLanguageName(mTranslateSourceLang));
        }
        if (mTranslateTargetBtn != null) {
            mTranslateTargetBtn.setText(getLanguageName(mTranslateTargetLang));
        }
        triggerTranslation();
    }

    private void showLanguageDialog(final boolean isSource) {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                nabu.iris.keyboard.latin.utils.DialogUtils.getPlatformDialogThemeContext(mContext));
        builder.setTitle(isSource ? "Select Source Language" : "Select Target Language");
        
        final String[] names = isSource ? mLangNames : mTgtLangNames;
        final String[] codes = isSource ? mLangCodes : mTgtLangCodes;
        String currentCode = isSource ? mTranslateSourceLang : mTranslateTargetLang;
        int checkedIndex = -1;
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equals(currentCode)) {
                checkedIndex = i;
                break;
            }
        }
        
        builder.setSingleChoiceItems(names, checkedIndex, (dialog, which) -> {
            SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(mContext);
            if (isSource) {
                mTranslateSourceLang = codes[which];
                prefs.edit().putString("pref_translate_source_lang", mTranslateSourceLang).apply();
                if (mTranslateSourceBtn != null) {
                    mTranslateSourceBtn.setText(names[which]);
                }
            } else {
                mTranslateTargetLang = codes[which];
                prefs.edit().putString("pref_translate_target_lang", mTranslateTargetLang).apply();
                if (mTranslateTargetBtn != null) {
                    mTranslateTargetBtn.setText(names[which]);
                }
            }
            dialog.dismiss();
            triggerTranslation();
        });
        
        AlertDialog dialog = builder.create();
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams lp = window.getAttributes();
            View keyboardView = mController.getKeyboardView();
            if (keyboardView != null) {
                lp.token = keyboardView.getWindowToken();
            }
            lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
            window.setAttributes(lp);
            window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        }
        dialog.show();
    }

    public void applyTheming(int accentColor, boolean isDark, int textColor, int hintColor) {
        if (mTranslatePanel != null) {
            mTranslatePanel.setBackgroundColor(mController.getKeyboardBackgroundColor());
        }

        if (mTranslateSourceBtn != null) {
            GradientDrawable btnBg = new GradientDrawable();
            btnBg.setShape(GradientDrawable.RECTANGLE);
            btnBg.setCornerRadius(mController.dpToPx(14));
            btnBg.setColor(isDark ? 0x18FFFFFF : 0x0E000000);
            btnBg.setStroke(mController.dpToPx(1), isDark ? 0x24FFFFFF : 0x1A000000);
            mTranslateSourceBtn.setBackground(btnBg);
            mTranslateSourceBtn.setTextColor(textColor);
        }

        if (mTranslateArrow != null) {
            mTranslateArrow.setColorFilter(accentColor);
            GradientDrawable arrBg = new GradientDrawable();
            arrBg.setShape(GradientDrawable.RECTANGLE);
            arrBg.setCornerRadius(mController.dpToPx(14));
            arrBg.setColor(isDark ? 0x14FFFFFF : 0x0A000000);
            mTranslateArrow.setBackground(arrBg);
        }

        if (mTranslateTargetBtn != null) {
            GradientDrawable btnBg = new GradientDrawable();
            btnBg.setShape(GradientDrawable.RECTANGLE);
            btnBg.setCornerRadius(mController.dpToPx(14));
            btnBg.setColor(isDark ? 0x18FFFFFF : 0x0E000000);
            btnBg.setStroke(mController.dpToPx(1), isDark ? 0x24FFFFFF : 0x1A000000);
            mTranslateTargetBtn.setBackground(btnBg);
            mTranslateTargetBtn.setTextColor(textColor);
        }

        if (mTranslateModeBtn != null) {
            GradientDrawable btnBg = new GradientDrawable();
            btnBg.setShape(GradientDrawable.RECTANGLE);
            btnBg.setCornerRadius(mController.dpToPx(14));
            btnBg.setColor(mController.getTranslucentColor(accentColor, 18));
            btnBg.setStroke(mController.dpToPx(1), mController.getTranslucentColor(accentColor, 40));
            mTranslateModeBtn.setBackground(btnBg);
            mTranslateModeBtn.setTextColor(accentColor);
        }

        updateInputContainerFocus();

        if (mTranslateInput != null) {
            mTranslateInput.setBackground(null);
            mTranslateInput.setTextColor(textColor);
            mTranslateInput.setHintTextColor(hintColor);
        }

        if (mTranslateClearBtn != null) {
            mTranslateClearBtn.setColorFilter(isDark ? 0xAAFFFFFF : 0x88000000);
            GradientDrawable clrBg = new GradientDrawable();
            clrBg.setShape(GradientDrawable.RECTANGLE);
            clrBg.setCornerRadius(mController.dpToPx(14));
            clrBg.setColor(isDark ? 0x18FFFFFF : 0x0E000000);
            mTranslateClearBtn.setBackground(clrBg);
        }

        if (mTranslatePasteBtn != null) {
            GradientDrawable pbBg = new GradientDrawable();
            pbBg.setShape(GradientDrawable.RECTANGLE);
            pbBg.setCornerRadius(mController.dpToPx(14));
            pbBg.setColor(isDark ? 0x1AFFFFFF : 0x0E000000);
            pbBg.setStroke(mController.dpToPx(1), isDark ? 0x24FFFFFF : 0x1A000000);
            mTranslatePasteBtn.setBackground(pbBg);
            mTranslatePasteBtn.setTextColor(textColor);
        }

        if (mTranslateResultContainer != null) {
            GradientDrawable resBg = new GradientDrawable();
            resBg.setShape(GradientDrawable.RECTANGLE);
            resBg.setCornerRadius(mController.dpToPx(16));
            resBg.setColor(isDark ? 0x14FFFFFF : 0x0A000000);
            resBg.setStroke(mController.dpToPx(1), isDark ? 0x22FFFFFF : 0x18000000);
            mTranslateResultContainer.setBackground(resBg);
        }

        if (mTranslateResultPreview != null) {
            mTranslateResultPreview.setTextColor(textColor);
        }

        if (mTranslateInsertBtn != null) {
            GradientDrawable insBg = new GradientDrawable();
            insBg.setShape(GradientDrawable.RECTANGLE);
            insBg.setCornerRadius(mController.dpToPx(14));
            insBg.setColor(mController.getTranslucentColor(accentColor, 30));
            insBg.setStroke(mController.dpToPx(1), mController.getTranslucentColor(accentColor, 65));
            mTranslateInsertBtn.setBackground(insBg);
            mTranslateInsertBtn.setTextColor(accentColor);
        }
    }

    public void updateInputContainerFocus() {
        if (mTranslateInputContainer == null) return;
        SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(mContext);
        int customColor = Settings.readKeyboardColor(prefs, mContext);
        int backgroundColor = mController.getKeyboardBackgroundColor();
        boolean isDark = mController.isColorDark(backgroundColor);
        int accentColor = customColor;
        if (accentColor == 0 || mController.isColorMonochromeOrTooDark(accentColor)) {
            accentColor = mContext.getResources().getColor(R.color.settings_accent);
        }
        boolean isFocused = (mController.getActiveInput() == mTranslateInput);
        GradientDrawable inpBg = new GradientDrawable();
        inpBg.setShape(GradientDrawable.RECTANGLE);
        inpBg.setCornerRadius(mController.dpToPx(20));
        inpBg.setColor(isDark ? 0x14FFFFFF : 0x0A000000);
        inpBg.setStroke(mController.dpToPx(1.5f), isFocused ? accentColor : (isDark ? 0x22FFFFFF : 0x1A000000));
        mTranslateInputContainer.setBackground(inpBg);
    }

    public void onDestroy() {
        if (mTranslateRunnable != null) {
            mTranslateHandler.removeCallbacks(mTranslateRunnable);
        }
        releaseActiveTranslator();
    }
}
