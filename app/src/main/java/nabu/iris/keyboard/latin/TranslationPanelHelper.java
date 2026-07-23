/*
 * Copyright (C) 2026 Latin IME Customizer
 */

package nabu.iris.keyboard.latin;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;



import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import nabu.iris.keyboard.R;
import nabu.iris.keyboard.compat.PreferenceManagerCompat;

/**
 * Helper class to manage the translation workspace panel supporting Google Translate web scraping, MLKit offline database engines, and AI translation prompts.
 */
public final class TranslationPanelHelper {
    private final ClipboardBarController mController;
    private final Context mContext;

    private final LinearLayout mTranslatePanel;
    private final TextView mTranslateSourceBtn;
    private final TextView mTranslateTargetBtn;
    private final TextView mTranslateModeBtn;
    private final EditText mTranslateInput;
    private final TextView mTranslateClearBtn;
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

    private static final String[] mLangNames = {"Auto-detect", "English", "Spanish", "French", "German", "Italian", "Portuguese", "Chinese", "Japanese", "Korean", "Russian", "Arabic", "Hindi", "Turkish", "Polish", "Dutch"};
    private static final String[] mLangCodes = {"auto", "en", "es", "fr", "de", "it", "pt", "zh", "ja", "ko", "ru", "ar", "hi", "tr", "pl", "nl"};
    private static final String[] mTgtLangNames = {"English", "Spanish", "French", "German", "Italian", "Portuguese", "Chinese", "Japanese", "Korean", "Russian", "Arabic", "Hindi", "Turkish", "Polish", "Dutch"};
    private static final String[] mTgtLangCodes = {"en", "es", "fr", "de", "it", "pt", "zh", "ja", "ko", "ru", "ar", "hi", "tr", "pl", "nl"};

    public TranslationPanelHelper(ClipboardBarController controller, View inputView) {
        mController = controller;
        mContext = controller.getContext();

        mTranslatePanel = inputView.findViewById(R.id.translate_panel);
        mTranslateSourceBtn = inputView.findViewById(R.id.translate_source_btn);
        mTranslateTargetBtn = inputView.findViewById(R.id.translate_target_btn);
        mTranslateModeBtn = inputView.findViewById(R.id.translate_mode_btn);
        mTranslateInput = inputView.findViewById(R.id.translate_input);
        mTranslateClearBtn = inputView.findViewById(R.id.translate_clear_btn);
        mTranslateResultPreview = inputView.findViewById(R.id.translate_result_preview);
        mTranslateInsertBtn = inputView.findViewById(R.id.translate_insert_btn);
        mTranslateProgressBar = inputView.findViewById(R.id.translate_progress_bar);
        mTranslateDownloadLabel = inputView.findViewById(R.id.translate_download_label);

        TextView translateArrow = inputView.findViewById(R.id.translate_arrow);
        if (translateArrow != null) {
            boolean isRtl = mContext.getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
            translateArrow.setText(isRtl ? " ← " : " → ");
        }

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
            mTranslateMode = prefs.getString("pref_translate_mode", "scraping");
            
            mController.setActiveInput(mTranslateInput);
            updateTranslateModeButton();
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

        if (mTranslateInsertBtn != null) {
            mTranslateInsertBtn.setOnClickListener(v -> {
                if (mTranslateResultPreview != null) {
                    String output = mTranslateResultPreview.getText().toString();
                    if (!output.isEmpty() && !output.startsWith("Error:") && !output.equals("Translating...") && !output.equals("Checking offline models...")) {
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
                    if (mTranslateRunnable != null) {
                        mTranslateHandler.removeCallbacks(mTranslateRunnable);
                    }
                    mTranslateRunnable = () -> triggerTranslation();
                    mTranslateHandler.postDelayed(mTranslateRunnable, 500);
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

        mTranslateResultPreview.setText("Translating...");
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
        AiCopilotManager.getSharedExecutor().execute(() -> {
            HttpURLConnection conn = null;
            try {
                String encodedText = URLEncoder.encode(text, "UTF-8");
                String urlStr = "https://translate.google.com/m?sl=" + mTranslateSourceLang + "&tl=" + mTranslateTargetLang + "&q=" + encodedText;
                URL url = new URL(urlStr);
                
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();
                    
                    String html = response.toString();
                    
                    int startIdx = html.indexOf("<div class=\"result-container\">");
                    if (startIdx == -1) {
                        startIdx = html.indexOf("<div class=\"t0\">");
                    }
                    if (startIdx != -1) {
                        int contentStart = html.indexOf(">", startIdx) + 1;
                        int endIdx = html.indexOf("</div>", contentStart);
                        if (endIdx != -1) {
                            String rawResult = html.substring(contentStart, endIdx);
                            final String translated = Html.fromHtml(rawResult).toString();
                            
                            mTranslateHandler.post(() -> {
                                showTranslateProgress(false);
                                mTranslateResultPreview.setText(translated);
                                mTranslateInsertBtn.setVisibility(View.VISIBLE);
                            });
                            return;
                        }
                    }
                    postTranslationFailure("Error parsing HTML response.");
                } else {
                    postTranslationFailure("HTTP Error: " + responseCode);
                }
            } catch (Exception e) {
                postTranslationFailure("Network Error: " + e.getMessage());
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
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
                mTranslateHandler.post(() -> mTranslateResultPreview.setText("Translating offline..."));
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
            mTranslateDownloadLabel.setText("Downloading... 0%");
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
                        mTranslateDownloadLabel.setText("Downloading... " + mDownloadProgress + "%");
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
            mTranslateDownloadLabel.setText("Downloading... 100%");
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

    private void showLanguageDialog(final boolean isSource) {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                nabu.iris.keyboard.latin.utils.DialogUtils.getPlatformDialogThemeContext(mContext));
        builder.setTitle(isSource ? "Select Source Language" : "Select Target Language");
        
        final String[] names = isSource ? mLangNames : mTgtLangNames;
        final String[] codes = isSource ? mLangCodes : mTgtLangCodes;
        
        builder.setItems(names, (dialog, which) -> {
            if (isSource) {
                mTranslateSourceLang = codes[which];
                if (mTranslateSourceBtn != null) {
                    mTranslateSourceBtn.setText(names[which]);
                }
            } else {
                mTranslateTargetLang = codes[which];
                if (mTranslateTargetBtn != null) {
                    mTranslateTargetBtn.setText(names[which]);
                }
            }
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
            btnBg.setCornerRadius(mController.dpToPx(8));
            btnBg.setColor(isDark ? 0x14FFFFFF : 0x08000000);
            mTranslateSourceBtn.setBackground(btnBg);
            mTranslateSourceBtn.setTextColor(textColor);
        }

        if (mTranslateTargetBtn != null) {
            GradientDrawable btnBg = new GradientDrawable();
            btnBg.setShape(GradientDrawable.RECTANGLE);
            btnBg.setCornerRadius(mController.dpToPx(8));
            btnBg.setColor(isDark ? 0x14FFFFFF : 0x08000000);
            mTranslateTargetBtn.setBackground(btnBg);
            mTranslateTargetBtn.setTextColor(textColor);
        }

        if (mTranslateModeBtn != null) {
            GradientDrawable btnBg = new GradientDrawable();
            btnBg.setShape(GradientDrawable.RECTANGLE);
            btnBg.setCornerRadius(mController.dpToPx(8));
            btnBg.setColor(mController.getTranslucentColor(accentColor, 12));
            mTranslateModeBtn.setBackground(btnBg);
            mTranslateModeBtn.setTextColor(accentColor);
        }

        if (mTranslateInput != null) {
            mController.styleConfigField(mTranslateInput, mController.getActiveInput() == mTranslateInput);
        }

        if (mTranslateClearBtn != null) {
            mTranslateClearBtn.setTextColor(0xFFFF5252);
            mTranslateClearBtn.setBackground(null);
        }

        if (mTranslateResultPreview != null) {
            mTranslateResultPreview.setTextColor(textColor);
        }

        if (mTranslateInsertBtn != null) {
            GradientDrawable insBg = new GradientDrawable();
            insBg.setShape(GradientDrawable.RECTANGLE);
            insBg.setCornerRadius(mController.dpToPx(16));
            insBg.setColor(isDark ? 0x2200E676 : 0x1A00E676);
            insBg.setStroke(mController.dpToPx(1), 0xFF00E676);
            mTranslateInsertBtn.setBackground(insBg);
            mTranslateInsertBtn.setTextColor(0xFF00E676);
        }
    }

    public void onDestroy() {
        if (mTranslateRunnable != null) {
            mTranslateHandler.removeCallbacks(mTranslateRunnable);
        }
        releaseActiveTranslator();
    }
}
