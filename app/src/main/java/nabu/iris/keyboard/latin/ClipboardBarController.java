/*
 * Copyright (C) 2026 Latin IME Customizer
 */

package nabu.iris.keyboard.latin;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import nabu.iris.keyboard.R;
import nabu.iris.keyboard.compat.PreferenceManagerCompat;
import nabu.iris.keyboard.keyboard.KeyboardTheme;
import nabu.iris.keyboard.latin.settings.Settings;
import nabu.iris.keyboard.latin.settings.SettingsActivity;
import nabu.iris.keyboard.latin.settings.SettingsValues;

import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.google.mlkit.common.model.DownloadConditions;

/**
 * Controller managing the dynamic Utility Toolbar switcher and the Clipboard / AI Copilot overlays.
 */
public final class ClipboardBarController {
    private static final String TAG = "ClipboardBarController";
    private final Context mContext;
    private final ClipboardHistoryManager mManager;
    private final OnItemClickListener mListener;

    // View References
    private final View mKeyboardView;
    private final LinearLayout mUtilityToolbar;
    private final LinearLayout mClipboardPanel;
    private final LinearLayout mItemsList;
    private final LinearLayout mAiPanel;
    private final LinearLayout mAiSettingsPanel;
    private final View mEmojiPanel;
    private final LinearLayout mEmojiItemsContainer;

    // Toolbar Buttons (ImageView Vectors)
    private final ImageView mTbKeysBtn;
    private final ImageView mTbClipboardBtn;
    private final ImageView mTbAiBtn;
    private final ImageView mTbEmojiBtn;
    private final ImageView mTbSettingsBtn;

    // In-console AI config trigger (tune icon inside AI panel header)
    private final ImageView mAiOpenSettingsBtn;

    // AI Panel Widgets
    private final ScrollView mAiConsoleScroll;
    private final TextView mAiConsoleText;
    private final LinearLayout mAiChatLog;
    private String mLatestResponseText = "";
    private String mLatestAction = "";
    private final TextView mAiActionCopy;
    private final TextView mAiActionInsert;
    private final TextView mAiToolSmartCompose;
    private final TextView mAiToolSimplify;
    private final TextView mAiToolGrammarFix;
    private final TextView mAiToolExplain;
    private final TextView mAiToolFix;
    private final EditText mAiPromptInput;
    private final TextView mAiSubmitBtn;
    private final TextView mAiPasteBtn;
    private final TextView mAiClearBtn;
    private EditText mActiveInput = null;

    // In-Keyboard Settings Dashboard Widgets
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

    // Clipboard Specifics
    private final View mClipboardSuggestionBar;
    private final TextView mClipboardSuggestionChip;
    private static String sDismissedText = "";
    private static String sLastShownClipText = null;
    private static long sClipShowStartTime = 0L;

    // GIF Specifics
    private final View mGifPanel;
    private final EditText mGifSearchInput;
    private final TextView mGifClearBtn;
    private final LinearLayout mGifItemsContainer;
    private final ImageView mTbGifBtn;
    private final android.util.LruCache<String, byte[]> mGifCache = new android.util.LruCache<String, byte[]>(3 * 1024 * 1024) {
        @Override
        protected int sizeOf(String key, byte[] value) {
            return value != null ? value.length : 0;
        }
    };
    private final android.os.Handler mGifSearchHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final android.widget.ScrollView mGifScrollView;
    private final android.view.ViewTreeObserver.OnScrollChangedListener mGifScrollListener = new android.view.ViewTreeObserver.OnScrollChangedListener() {
        @Override
        public void onScrollChanged() {
            updateGifVisibilityInScroll();
        }
    };
    private Runnable mGifSearchRunnable;
    private final android.os.Handler mSuggestionDismissHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable mSuggestionDismissRunnable = new Runnable() {
        @Override
        public void run() {
            if (mClipboardSuggestionBar != null) {
                mClipboardSuggestionBar.setVisibility(View.GONE);
            }
        }
    };
    private final android.content.ClipboardManager.OnPrimaryClipChangedListener mPrimaryClipListener = 
        new android.content.ClipboardManager.OnPrimaryClipChangedListener() {
            @Override
            public void onPrimaryClipChanged() {
                sLastShownClipText = null;
                sDismissedText = "";
                checkAndShowClipboardSuggestion();
            }
        };

    private LinearLayout mTabsLayout;
    private String mSearchQuery = "";
    private String mSelectedTab = "all";
    private final ImageView mEmojiDeleteBtn;

    // Utilities & Configuration State
    private final Vibrator mVibrator;
    private final AiCopilotManager mAiManager;
    private String mConfigProvider = "ollama"; // Temporary state before saving

    // Translation panel widgets & state
    private final LinearLayout mTranslatePanel;
    private final TextView mTranslateSourceBtn;
    private final TextView mTranslateTargetBtn;
    private final TextView mTranslateModeBtn;
    private final EditText mTranslateInput;
    private final TextView mTranslateClearBtn;
    private final TextView mTranslateResultPreview;
    private final TextView mTranslateInsertBtn;
    private final ImageView mTbTranslateBtn;
    private final ProgressBar mTranslateProgressBar;
    private final TextView mTranslateDownloadLabel;
    private int mDownloadProgress = 0;
    private Runnable mDownloadProgressRunnable;

    private String mTranslateSourceLang = "auto";
    private String mTranslateTargetLang = "es";
    private String mTranslateMode = "scraping"; // scraping, mlkit, ai
    private final android.os.Handler mTranslateHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable mTranslateRunnable;
    private final java.util.concurrent.ExecutorService mTranslationExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
    private Translator mActiveTranslator = null;
    private String mActiveTranslatorSource = null;
    private String mActiveTranslatorTarget = null;
    
    private final String[] mLangNames = {"Auto-detect", "English", "Spanish", "French", "German", "Italian", "Portuguese", "Chinese", "Japanese", "Korean", "Russian", "Arabic", "Hindi", "Turkish", "Polish", "Dutch"};
    private final String[] mLangCodes = {"auto", "en", "es", "fr", "de", "it", "pt", "zh", "ja", "ko", "ru", "ar", "hi", "tr", "pl", "nl"};
    private final String[] mTgtLangNames = {"English", "Spanish", "French", "German", "Italian", "Portuguese", "Chinese", "Japanese", "Korean", "Russian", "Arabic", "Hindi", "Turkish", "Polish", "Dutch"};
    private final String[] mTgtLangCodes = {"en", "es", "fr", "de", "it", "pt", "zh", "ja", "ko", "ru", "ar", "hi", "tr", "pl", "nl"};

    public interface OnItemClickListener {
        void onItemClick(String text);
    }

    public ClipboardBarController(Context context, View inputView, ClipboardHistoryManager manager, OnItemClickListener listener) {
        mContext = context;
        mManager = manager;
        mListener = listener;

        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        mAiManager = new AiCopilotManager(context);

        // Resolve Layout Elements
        mKeyboardView = inputView.findViewById(R.id.keyboard_view);
        mUtilityToolbar = inputView.findViewById(R.id.utility_toolbar);
        mClipboardPanel = inputView.findViewById(R.id.clipboard_panel);
        mItemsList = inputView.findViewById(R.id.clipboard_items_list);
        mAiPanel = inputView.findViewById(R.id.ai_panel);
        mAiSettingsPanel = inputView.findViewById(R.id.ai_settings_panel);
        mEmojiPanel = inputView.findViewById(R.id.emoji_panel);
        mEmojiItemsContainer = inputView.findViewById(R.id.emoji_items_container);
        mEmojiDeleteBtn = inputView.findViewById(R.id.emoji_delete_btn);

        mGifPanel = inputView.findViewById(R.id.gif_panel);
        mGifSearchInput = inputView.findViewById(R.id.gif_search_input);
        mGifClearBtn = inputView.findViewById(R.id.gif_clear_btn);
        mGifItemsContainer = inputView.findViewById(R.id.gif_items_container);
        mGifScrollView = inputView.findViewById(R.id.gif_scroll_view);
        if (mGifScrollView != null) {
            mGifScrollView.getViewTreeObserver().addOnScrollChangedListener(mGifScrollListener);
        }

        // Resolve Translation Panel Elements
        mTranslatePanel = inputView.findViewById(R.id.translate_panel);
        mTranslateSourceBtn = inputView.findViewById(R.id.translate_source_btn);
        mTranslateTargetBtn = inputView.findViewById(R.id.translate_target_btn);
        mTranslateModeBtn = inputView.findViewById(R.id.translate_mode_btn);
        mTranslateInput = inputView.findViewById(R.id.translate_input);
        mTranslateClearBtn = inputView.findViewById(R.id.translate_clear_btn);
        mTranslateResultPreview = inputView.findViewById(R.id.translate_result_preview);
        mTranslateInsertBtn = inputView.findViewById(R.id.translate_insert_btn);
        mTbTranslateBtn = inputView.findViewById(R.id.tb_translate_btn);
        mTranslateProgressBar = inputView.findViewById(R.id.translate_progress_bar);
        mTranslateDownloadLabel = inputView.findViewById(R.id.translate_download_label);

        TextView translateArrow = inputView.findViewById(R.id.translate_arrow);
        if (translateArrow != null) {
            boolean isRtl = context.getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
            translateArrow.setText(isRtl ? " ← " : " → ");
        }

        configureSimulatedInput(mTranslateInput);



        // Resolve Toolbar Button Views
        mTbKeysBtn = inputView.findViewById(R.id.tb_keys_btn);
        mTbClipboardBtn = inputView.findViewById(R.id.tb_clipboard_btn);
        mTbAiBtn = inputView.findViewById(R.id.tb_ai_btn);
        mTbEmojiBtn = inputView.findViewById(R.id.tb_emoji_btn);
        mTbGifBtn = inputView.findViewById(R.id.tb_gif_btn);
        mTbSettingsBtn = inputView.findViewById(R.id.tb_settings_btn);
        mAiOpenSettingsBtn = inputView.findViewById(R.id.ai_open_settings_btn);

        configureSimulatedInput(mGifSearchInput);

        // Resolve AI Panel views
        mAiConsoleScroll = inputView.findViewById(R.id.ai_console_scroll);
        mAiChatLog = inputView.findViewById(R.id.ai_chat_log);
        mAiConsoleText = null;
        mAiActionCopy = inputView.findViewById(R.id.ai_action_copy);
        mAiActionInsert = inputView.findViewById(R.id.ai_action_insert);
        mAiToolSmartCompose = inputView.findViewById(R.id.ai_tool_smart_compose);
        mAiToolSimplify = inputView.findViewById(R.id.ai_tool_simplify);
        mAiToolGrammarFix = inputView.findViewById(R.id.ai_tool_grammar_fix);
        mAiToolExplain = inputView.findViewById(R.id.ai_tool_explain);
        mAiToolFix = inputView.findViewById(R.id.ai_tool_fix);
        mAiPromptInput = inputView.findViewById(R.id.ai_prompt_input);
        mAiSubmitBtn = inputView.findViewById(R.id.ai_submit_btn);
        mAiPasteBtn = inputView.findViewById(R.id.ai_paste_btn);
        mAiClearBtn = inputView.findViewById(R.id.ai_clear_btn);
        mClipboardSuggestionBar = inputView.findViewById(R.id.clipboard_suggestion_bar);
        mClipboardSuggestionChip = inputView.findViewById(R.id.clipboard_suggestion_chip);

        // Resolve In-Keyboard Settings Dashboard views
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

        configureSimulatedInput(mAiPromptInput);
        configureSimulatedInput(mSetupGeminiKey);
        configureSimulatedInput(mSetupHostUrl);
        configureSimulatedInput(mSetupModelName);
        configureSimulatedInput(mSetupHeadersJson);

        mSetupGeminiLabel = inputView.findViewById(R.id.setup_gemini_label);
        mSetupHostLabel = inputView.findViewById(R.id.setup_host_label);
        mSetupModelLabel = inputView.findViewById(R.id.setup_model_label);
        mSetupHeadersLabel = inputView.findViewById(R.id.setup_headers_label);

        // Attach Quick-Access Toolbar Click Actions
        if (mTbKeysBtn != null) {
            mTbKeysBtn.setOnClickListener(v -> showKeyboard());
        }
        if (mTbClipboardBtn != null) {
            mTbClipboardBtn.setOnClickListener(v -> {
                if (mClipboardPanel != null && mClipboardPanel.getVisibility() == View.VISIBLE) {
                    showKeyboard();
                } else {
                    showClipboard();
                }
            });
        }
        if (mTbAiBtn != null) {
            mTbAiBtn.setOnClickListener(v -> {
                if (mAiPanel != null && mAiPanel.getVisibility() == View.VISIBLE) {
                    showKeyboard();
                } else {
                    showAiCopilot();
                }
            });
            mTbAiBtn.setOnLongClickListener(v -> {
                performAutoGrammarCorrection();
                return true;
            });
        }
        if (mTbEmojiBtn != null) {
            mTbEmojiBtn.setOnClickListener(v -> {
                if (mEmojiPanel != null && mEmojiPanel.getVisibility() == View.VISIBLE) {
                    showKeyboard();
                } else {
                    showEmojiPanel();
                }
            });
        }
        if (mTbGifBtn != null) {
            mTbGifBtn.setOnClickListener(v -> {
                if (mGifPanel != null && mGifPanel.getVisibility() == View.VISIBLE) {
                    showKeyboard();
                } else {
                    showGifPanel();
                }
            });
        }
        if (mTbSettingsBtn != null) {
            mTbSettingsBtn.setOnClickListener(v -> launchSettings());
        }

        if (mTbTranslateBtn != null) {
            mTbTranslateBtn.setOnClickListener(v -> {
                if (mTranslatePanel != null && mTranslatePanel.getVisibility() == View.VISIBLE) {
                    showKeyboard();
                } else {
                    showTranslatePanel();
                }
            });
        }

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
                                showKeyboard();
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



        if (mGifClearBtn != null) {
            mGifClearBtn.setOnClickListener(v -> {
                if (mGifSearchInput != null) {
                    mGifSearchInput.setText("");
                }
                loadGifs("");
            });
        }

        if (mGifSearchInput != null) {
            mGifSearchInput.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(final android.text.Editable s) {
                    if (mGifSearchRunnable != null) {
                        mGifSearchHandler.removeCallbacks(mGifSearchRunnable);
                    }
                    mGifSearchRunnable = () -> loadGifs(s.toString());
                    mGifSearchHandler.postDelayed(mGifSearchRunnable, 600);
                }
            });
            
            mGifSearchInput.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH 
                        || actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE
                        || (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER)) {
                    if (mGifSearchRunnable != null) {
                        mGifSearchHandler.removeCallbacks(mGifSearchRunnable);
                    }
                    loadGifs(v.getText().toString());
                    return true;
                }
                return false;
            });
        }
        if (mAiOpenSettingsBtn != null) {
            mAiOpenSettingsBtn.setOnClickListener(v -> showAiSettings());
        }

        // Attach custom search filters programmatically inside clipboard panel
        setupClipboardControls();

        // Attach AI click actions
        setupAiActions();

        // Attach Settings panel interactions
        setupSettingsPanelActions();

        // Default: display standard key matrix
        if (mAiChatLog != null) {
            clearChat();
        }
        setupDeleteButton();
        updateToolbarLayout();
        
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.addPrimaryClipChangedListener(mPrimaryClipListener);
        }
        
        showKeyboard();
    }

    private void setupClipboardControls() {
        if (mClipboardPanel == null) return;

        LinearLayout controlsRow = new LinearLayout(mContext);
        controlsRow.setOrientation(LinearLayout.HORIZONTAL);
        controlsRow.setGravity(Gravity.CENTER_VERTICAL);
        
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(dpToPx(8), dpToPx(2), dpToPx(8), dpToPx(2));
        controlsRow.setLayoutParams(rowParams);

        // Tabs selection taking the entire width
        mTabsLayout = new LinearLayout(mContext);
        mTabsLayout.setOrientation(LinearLayout.HORIZONTAL);
        mTabsLayout.setGravity(Gravity.CENTER_VERTICAL);
        
        LinearLayout.LayoutParams tabsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        mTabsLayout.setLayoutParams(tabsParams);
        controlsRow.addView(mTabsLayout);

        // Add controls below top bar dynamically
        mClipboardPanel.addView(controlsRow, 0);
    }

    private String getMostRecentClipboardText() {
        try {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                android.content.ClipData clip = clipboard.getPrimaryClip();
                if (clip != null && clip.getItemCount() > 0) {
                    CharSequence text = clip.getItemAt(0).getText();
                    if (text != null && text.length() > 0) {
                        return text.toString();
                    }
                }
            }
        } catch (Exception e) {
            // Ignore system clipboard access errors
        }

        if (mManager != null) {
            List<ClipboardHistoryManager.ClipboardItem> items = mManager.getItems();
            if (items != null && !items.isEmpty()) {
                ClipboardHistoryManager.ClipboardItem mostRecent = null;
                for (ClipboardHistoryManager.ClipboardItem item : items) {
                    if (mostRecent == null || item.timestamp > mostRecent.timestamp) {
                        mostRecent = item;
                    }
                }
                if (mostRecent != null) {
                    return mostRecent.text;
                }
            }
        }
        return null;
    }

    private void setupAiActions() {
        if (mAiSubmitBtn != null && mAiPromptInput != null) {
            mAiSubmitBtn.setOnClickListener(v -> {
                String promptText = mAiPromptInput.getText().toString().trim();
                if (!promptText.isEmpty()) {
                    mLatestAction = "Custom";
                    runAiPrompt(promptText);
                    mAiPromptInput.setText("");
                }
            });

            mAiPromptInput.setOnEditorActionListener((v, actionId, event) -> {
                String promptText = mAiPromptInput.getText().toString().trim();
                if (!promptText.isEmpty()) {
                    mLatestAction = "Custom";
                    runAiPrompt(promptText);
                    mAiPromptInput.setText("");
                    return true;
                }
                return false;
            });
        }

        if (mAiPasteBtn != null && mAiPromptInput != null) {
            mAiPasteBtn.setOnClickListener(v -> {
                String clipText = getMostRecentClipboardText();
                if (clipText != null && !clipText.isEmpty()) {
                    String text = mAiPromptInput.getText().toString();
                    int selStart = mAiPromptInput.getSelectionStart();
                    int selEnd = mAiPromptInput.getSelectionEnd();
                    if (selStart >= 0 && selEnd >= 0) {
                        int min = Math.min(selStart, selEnd);
                        int max = Math.max(selStart, selEnd);
                        String newText = text.substring(0, min) + clipText + text.substring(max);
                        mAiPromptInput.setText(newText);
                        mAiPromptInput.setSelection(min + clipText.length());
                    } else {
                        mAiPromptInput.append(clipText);
                        mAiPromptInput.setSelection(mAiPromptInput.getText().length());
                    }
                } else {
                    Toast.makeText(mContext, "Clipboard is empty!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (mAiClearBtn != null) {
            mAiClearBtn.setOnClickListener(v -> clearChat());
        }

        // Copy/Insert handlers
        if (mAiActionCopy != null) {
            mAiActionCopy.setOnClickListener(v -> {
                String output = mLatestResponseText;
                if (output != null && !output.trim().isEmpty()) {
                    mManager.addItem(output);
                    Toast.makeText(mContext, "Copied response to clipboard history!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mContext, "No response to copy yet!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (mAiActionInsert != null) {
            mAiActionInsert.setOnClickListener(v -> {
                String output = mLatestResponseText;
                if (output != null && !output.trim().isEmpty() && mContext instanceof LatinIME) {
                    LatinIME ime = (LatinIME) mContext;
                    InputConnection conn = ime.getCurrentInputConnection();
                    if (conn != null) {
                        if ("Simplify".equals(mLatestAction)) {
                            // Move cursor to the end of selection so we append instead of overwriting it
                            int selectionEnd = ime.mInputLogic.mConnection.getExpectedSelectionEnd();
                            if (selectionEnd >= 0) {
                                conn.setSelection(selectionEnd, selectionEnd);
                            }
                            // Prepend a space if the character before cursor is not a space
                            CharSequence before = conn.getTextBeforeCursor(1, 0);
                            String prefix = " ";
                            if (before != null && before.length() > 0) {
                                char lastChar = before.charAt(before.length() - 1);
                                if (Character.isWhitespace(lastChar)) {
                                    prefix = "";
                                }
                            }
                            conn.commitText(prefix + output, 1);
                        } else {
                            conn.commitText(output, 1);
                        }
                        showKeyboard();
                    }
                } else {
                    Toast.makeText(mContext, "No response to insert yet!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Assist tool actions
        if (mAiToolSmartCompose != null) {
            mAiToolSmartCompose.setOnClickListener(v -> runInlineAssist("SmartCompose"));
        }
        if (mAiToolSimplify != null) {
            mAiToolSimplify.setOnClickListener(v -> runInlineAssist("Simplify"));
        }
        if (mAiToolGrammarFix != null) {
            mAiToolGrammarFix.setOnClickListener(v -> runInlineAssist("Grammar"));
        }
        if (mAiToolExplain != null) {
            mAiToolExplain.setOnClickListener(v -> runInlineAssist("Explain"));
        }
        if (mAiToolFix != null) {
            mAiToolFix.setOnClickListener(v -> runInlineAssist("Fix"));
        }
    }

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
            mSetupBackBtn.setOnClickListener(v -> showAiCopilot());
        }

        if (mSetupSaveBtn != null) {
            mSetupSaveBtn.setOnClickListener(v -> saveAiConfiguration());
        }
    }

    private void runInlineAssist(String action) {
        if (!(mContext instanceof LatinIME)) return;
        InputConnection conn = ((LatinIME) mContext).getCurrentInputConnection();
        if (conn == null) return;

        CharSequence selection = conn.getSelectedText(0);
        String text = (selection != null) ? selection.toString() : "";

        // If no selected text, fetch preceding text as prompt or context
        if (text.isEmpty()) {
            CharSequence preceding = conn.getTextBeforeCursor(200, 0);
            text = (preceding != null) ? preceding.toString().trim() : "";
        }

        // If still empty, check selection inside keyboard's own active input
        if (text.isEmpty() && mActiveInput != null && isViewOrParentVisible(mActiveInput)) {
            int start = mActiveInput.getSelectionStart();
            int end = mActiveInput.getSelectionEnd();
            if (start >= 0 && end >= 0 && start != end) {
                int min = Math.min(start, end);
                int max = Math.max(start, end);
                text = mActiveInput.getText().toString().substring(min, max);
            }
        }

        // If still empty, check full text of keyboard's own prompt input
        if (text.isEmpty() && mAiPromptInput != null && isViewOrParentVisible(mAiPromptInput)) {
            String promptText = mAiPromptInput.getText().toString().trim();
            if (!promptText.isEmpty()) {
                text = promptText;
            }
        }

        // If still empty, check active clipboard (skipping API keys/configs)
        if (text.isEmpty()) {
            SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(mContext);
            String savedKey = Settings.readGeminiKey(prefs);
            List<ClipboardHistoryManager.ClipboardItem> items = mManager.getItems();
            for (ClipboardHistoryManager.ClipboardItem item : items) {
                if (item.text == null) continue;
                String val = item.text.trim();
                if (val.isEmpty()) continue;

                // Skip if it matches the saved API key
                if (!savedKey.isEmpty() && val.equals(savedKey.trim())) {
                    continue;
                }
                // Skip if it matches a generic Gemini API key pattern (39 chars, starts with AIzaSy)
                if (val.startsWith("AIzaSy") && val.length() == 39 && !val.contains(" ")) {
                    continue;
                }
                // Skip if it matches a generic OpenAI key pattern (starts with sk-)
                if (val.startsWith("sk-") && !val.contains(" ")) {
                    continue;
                }
                text = item.text;
                break;
            }
        }

        if (text.isEmpty()) {
            addMessageBubble("bot", "Error: No prompt context found! Select some text or copy something first.");
            return;
        }

        mLatestAction = action;

        String finalPrompt;
        String displayPrompt;
        if ("SmartCompose".equals(action)) {
            finalPrompt = "Rewrite, expand, or execute the following instructions to draft a clean output. Return ONLY the final output:\n\n" + text;
            addMessageBubble("user", "🧠 Smart Compose:\n" + text);
            final TextView responseBubble = addMessageBubble("bot", "[🧠 Smart Composing...] Writing output...");
            mAiManager.queryChat(finalPrompt, new AiCopilotManager.AiCallback() {
                @Override
                public void onSuccess(String responseText) {
                    mLatestResponseText = responseText;
                    if (responseBubble != null) {
                        responseBubble.setText(responseText);
                    }
                    conn.commitText(responseText, 1);
                    showKeyboard();
                }

                @Override
                public void onFailure(String errorMessage) {
                    if (responseBubble != null) {
                        responseBubble.setText("Smart Compose failed:\n" + errorMessage);
                    }
                }
            });
            return;
        } else if ("Simplify".equals(action)) {
            finalPrompt = "Simplify the following text to make it extremely clear, concise, and punchy. Return ONLY the simplified text:\n\n" + text;
            displayPrompt = "✍️ Simplify:\n" + text;
        } else if ("Grammar".equals(action)) {
            finalPrompt = "You are a native American English speaker. Correct the grammar, spelling, punctuation, and phrasing in the following text. Do NOT make it sound like an AI, robotic, or overly artificial. Keep the phrasing extremely natural, casual, and authentic, exactly like how an American would type it. Return ONLY the corrected text without any extra explanation, wrapping, or markdown:\n\n" + text;
            displayPrompt = "📝 Grammar Fix:\n" + text;
        } else if ("Explain".equals(action)) {
            finalPrompt = "Explain what the following code or text does in brief, clear developer bullet points:\n\n" + text;
            displayPrompt = "💻 Explain Code:\n" + text;
        } else { // Fix
            finalPrompt = "Fix any spelling, grammar, or programming syntax errors in this text. Return ONLY the corrected text without wrapping or markdown details:\n\n" + text;
            displayPrompt = "🔧 Fix Syntax:\n" + text;
        }

        runAiPrompt(finalPrompt, displayPrompt);
    }

    private void runAiPrompt(String prompt) {
        runAiPrompt(prompt, prompt);
    }

    private void runAiPrompt(String prompt, String displayPrompt) {
        if (mAiChatLog == null) return;
        
        addMessageBubble("user", displayPrompt);
        final TextView responseBubble = addMessageBubble("bot", "[🧠 Processing prompt...] Requesting AI Engine output...");
        if (mAiSubmitBtn != null) mAiSubmitBtn.setEnabled(false);

        mAiManager.queryChat(prompt, new AiCopilotManager.AiCallback() {
            @Override
            public void onSuccess(String responseText) {
                mLatestResponseText = responseText;
                if (responseBubble != null) {
                    responseBubble.setText(responseText);
                }
                if (mAiSubmitBtn != null) mAiSubmitBtn.setEnabled(true);
            }

            @Override
            public void onFailure(String errorMessage) {
                if (responseBubble != null) {
                    responseBubble.setText("Copilot request failed:\n\n" + errorMessage);
                    responseBubble.setTextColor(0xFFFF5252);
                }
                if (mAiSubmitBtn != null) mAiSubmitBtn.setEnabled(true);
            }
        });
    }

    public void showKeyboard() {
        hideTranslatePanel();
        applyTheming();
        if (mKeyboardView != null) mKeyboardView.setVisibility(View.VISIBLE);
        if (mClipboardPanel != null) mClipboardPanel.setVisibility(View.GONE);
        if (mAiPanel != null) mAiPanel.setVisibility(View.GONE);
        if (mAiSettingsPanel != null) mAiSettingsPanel.setVisibility(View.GONE);
        if (mEmojiPanel != null) mEmojiPanel.setVisibility(View.GONE);
        hideGifPanel();
        setActiveInput(null);
        highlightToolbarTab("keys");
        checkAndShowClipboardSuggestion();
        triggerLayoutRequest();
    }

    public void showClipboard() {
        hideTranslatePanel();
        hideClipboardSuggestion();
        if (mClipboardPanel != null && mKeyboardView != null) {
            applyTheming();
            syncWorkspaceHeights(mClipboardPanel);
            
            mKeyboardView.setVisibility(View.GONE);
            if (mAiPanel != null) mAiPanel.setVisibility(View.GONE);
            if (mAiSettingsPanel != null) mAiSettingsPanel.setVisibility(View.GONE);
            if (mEmojiPanel != null) mEmojiPanel.setVisibility(View.GONE);
            hideGifPanel();
            mClipboardPanel.setVisibility(View.VISIBLE);
            
            setActiveInput(null);
            highlightToolbarTab("clipboard");
            refresh();
            triggerLayoutRequest();
        }
    }

    public void showAiCopilot() {
        hideTranslatePanel();
        hideClipboardSuggestion();
        if (mAiPanel != null && mKeyboardView != null) {
            applyTheming();
            syncWorkspaceHeights(mAiPanel);

            mKeyboardView.setVisibility(View.VISIBLE);
            if (mClipboardPanel != null) mClipboardPanel.setVisibility(View.GONE);
            if (mAiSettingsPanel != null) mAiSettingsPanel.setVisibility(View.GONE);
            if (mEmojiPanel != null) mEmojiPanel.setVisibility(View.GONE);
            hideGifPanel();
            mAiPanel.setVisibility(View.VISIBLE);

            setActiveInput(mAiPromptInput);
            highlightToolbarTab("ai");
            triggerLayoutRequest();
        }
    }

    public void showAiSettings() {
        hideTranslatePanel();
        hideClipboardSuggestion();
        if (mAiSettingsPanel != null && mKeyboardView != null) {
            applyTheming();
            syncWorkspaceHeights(mAiSettingsPanel);

            mKeyboardView.setVisibility(View.VISIBLE);
            if (mClipboardPanel != null) mClipboardPanel.setVisibility(View.GONE);
            if (mAiPanel != null) mAiPanel.setVisibility(View.GONE);
            if (mEmojiPanel != null) mEmojiPanel.setVisibility(View.GONE);
            hideGifPanel();
            mAiSettingsPanel.setVisibility(View.VISIBLE);

            // Load saved settings directly into inputs for raw convenience!
            SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(mContext);
            mConfigProvider = Settings.readAiProvider(prefs);
            
            if (mSetupGeminiKey != null) mSetupGeminiKey.setText(Settings.readGeminiKey(prefs));
            
            // Populate Host URL and Model based on active provider
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
                setActiveInput(mSetupGeminiKey);
            } else {
                setActiveInput(mSetupHostUrl);
            }

            highlightToolbarTab("ai");
            triggerLayoutRequest();
        }
    }

    public void showEmojiPanel() {
        hideTranslatePanel();
        hideClipboardSuggestion();
        if (mEmojiPanel != null && mKeyboardView != null) {
            applyTheming();
            syncWorkspaceHeights(mEmojiPanel);

            mKeyboardView.setVisibility(View.GONE);
            if (mClipboardPanel != null) mClipboardPanel.setVisibility(View.GONE);
            if (mAiPanel != null) mAiPanel.setVisibility(View.GONE);
            if (mAiSettingsPanel != null) mAiSettingsPanel.setVisibility(View.GONE);
            hideGifPanel();
            mEmojiPanel.setVisibility(View.VISIBLE);

            setActiveInput(null);
            highlightToolbarTab("emoji");
            setupEmojiPanel();
            triggerLayoutRequest();
        }
    }

    public void showTranslatePanel() {
        hideClipboardSuggestion();
        if (mTranslatePanel != null && mKeyboardView != null) {
            applyTheming();
            syncWorkspaceHeights(mTranslatePanel);

            mKeyboardView.setVisibility(View.VISIBLE);
            if (mClipboardPanel != null) mClipboardPanel.setVisibility(View.GONE);
            if (mAiPanel != null) mAiPanel.setVisibility(View.GONE);
            if (mAiSettingsPanel != null) mAiSettingsPanel.setVisibility(View.GONE);
            if (mEmojiPanel != null) mEmojiPanel.setVisibility(View.GONE);
            hideGifPanel();
            mTranslatePanel.setVisibility(View.VISIBLE);

            SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(mContext);
            mTranslateMode = prefs.getString("pref_translate_mode", "scraping");

            setActiveInput(mTranslateInput);
            highlightToolbarTab("translate");
            updateTranslateModeButton();
            triggerTranslation();
            triggerLayoutRequest();
        }
    }

    private void hideTranslatePanel() {
        if (mTranslatePanel != null) {
            mTranslatePanel.setVisibility(View.GONE);
        }
        releaseActiveTranslator();
    }

    private void setupEmojiPanel() {
        if (mEmojiItemsContainer == null) return;
        mEmojiItemsContainer.removeAllViews();

        final SettingsValues settingsValues = Settings.getInstance().getCurrent();
        if (settingsValues == null) return;

        String emojiListStr = settingsValues.mEmojiList;
        if (emojiListStr == null || emojiListStr.trim().isEmpty()) {
            emojiListStr = "😀,😁,😂,🤣,😃,😄,😅,😆,😉,😊,😋,😎,😍,😘,🥰,😗,😙,😚";
        }

        String[] rawEmojis = emojiListStr.split(",");
        int cols = 7;
        LinearLayout currentRow = null;
        int addedCount = 0;

        for (String raw : rawEmojis) {
            final String emoji = raw.trim();
            if (emoji.isEmpty()) continue;

            if (addedCount % cols == 0) {
                currentRow = new LinearLayout(mContext);
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                currentRow.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                );
                currentRow.setLayoutParams(rowLp);
                mEmojiItemsContainer.addView(currentRow);
            }

            TextView emojiTv = new TextView(mContext);
            emojiTv.setText(emoji);
            emojiTv.setTextSize(26);
            emojiTv.setGravity(Gravity.CENTER);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0,
                dpToPx(48),
                1.0f
            );
            emojiTv.setLayoutParams(lp);
            emojiTv.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));

            TypedValue outValue = new TypedValue();
            mContext.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true);
            emojiTv.setBackgroundResource(outValue.resourceId);

            emojiTv.setOnTouchListener(new View.OnTouchListener() {
                private android.os.Handler handler;
                private Runnable runnable;
                private boolean isRepeating = false;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    int action = event.getAction();
                    if (action == MotionEvent.ACTION_DOWN) {
                        commitEmoji(emoji);
                        isRepeating = false;
                        v.setPressed(true);

                        if (handler == null) {
                            handler = new android.os.Handler(android.os.Looper.getMainLooper());
                        }
                        runnable = new Runnable() {
                            @Override
                            public void run() {
                                isRepeating = true;
                                commitEmoji(emoji);
                                if (handler != null) {
                                    handler.postDelayed(this, 100);
                                }
                            }
                        };
                        handler.postDelayed(runnable, 400);
                        return true;
                    } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                        v.setPressed(false);
                        if (handler != null && runnable != null) {
                            handler.removeCallbacks(runnable);
                        }
                        return true;
                    }
                    return false;
                }
            });

            if (currentRow != null) {
                currentRow.addView(emojiTv);
                addedCount++;
            }
        }

        if (currentRow != null && currentRow.getChildCount() < cols) {
            int remaining = cols - currentRow.getChildCount();
            for (int i = 0; i < remaining; i++) {
                View emptyView = new View(mContext);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0,
                    dpToPx(48),
                    1.0f
                );
                emptyView.setLayoutParams(lp);
                currentRow.addView(emptyView);
            }
        }
    }

    private void commitEmoji(String emoji) {
        if (mContext instanceof LatinIME) {
            LatinIME ime = (LatinIME) mContext;
            InputConnection conn = ime.getCurrentInputConnection();
            if (conn != null) {
                conn.commitText(emoji, 1);
            }

            AudioAndHapticFeedbackManager feedback = AudioAndHapticFeedbackManager.getInstance();
            feedback.performAudioFeedback(0); // 0 corresponds to CODE_UNSPECIFIED, which uses standard key sound
            feedback.performHapticFeedback(mEmojiPanel);
        }
    }

    public boolean isKeyboardReplacingPanelOpen() {
        return (mClipboardPanel != null && mClipboardPanel.getVisibility() == View.VISIBLE)
            || (mEmojiPanel != null && mEmojiPanel.getVisibility() == View.VISIBLE);
    }

    private void launchSettings() {
        if (mContext instanceof LatinIME) {
            ((LatinIME) mContext).launchSettings();
        }
    }

    private void switchConfigProvider(String provider) {
        mConfigProvider = provider;
        
        // Dynamically shift host/model hints and values when switching tabs
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
        int backgroundColor = getKeyboardBackgroundColor();
        boolean isDark = isColorDark(backgroundColor);

        int normalColor = isDark ? 0xAAFFFFFF : 0x88000000;
        int activeColor = customColor;
        if (activeColor == 0 || isColorMonochromeOrTooDark(activeColor)) {
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
        badge.setCornerRadius(dpToPx(16)); // Capsule rounded toggles
        if (isActive) {
            badge.setColor(getTranslucentColor(accentColor, 24)); // 24% active container
            // No border stroke
        } else {
            badge.setColor(isDark ? 0x0CFFFFFF : 0x05000000); // Soft glass backdrop
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
        showAiCopilot(); // Slide back smoothly to active console
    }

    private void syncWorkspaceHeights(View workspaceView) {
        ViewGroup.LayoutParams params = workspaceView.getLayoutParams();
        if (workspaceView == mAiPanel) {
            params.height = dpToPx(260);
        } else if (workspaceView == mAiSettingsPanel) {
            params.height = dpToPx(185);
        } else if (workspaceView == mGifPanel) {
            params.height = dpToPx(160);
        } else if (workspaceView == mTranslatePanel) {
            params.height = dpToPx(185);
        } else {
            if (mKeyboardView == null) return;

            int targetHeight = mKeyboardView.getHeight();
            if (targetHeight <= 0) {
                targetHeight = mKeyboardView.getMeasuredHeight();
            }
            if (targetHeight <= 0 && mKeyboardView instanceof nabu.iris.keyboard.keyboard.KeyboardView) {
                nabu.iris.keyboard.keyboard.KeyboardView kv = (nabu.iris.keyboard.keyboard.KeyboardView) mKeyboardView;
                if (kv.getKeyboard() != null) {
                    targetHeight = kv.getKeyboard().mOccupiedHeight 
                            + kv.getPaddingTop() + kv.getPaddingBottom();
                }
            }
            if (targetHeight <= 0) {
                targetHeight = dpToPx(230);
            }
            params.height = targetHeight;
        }
        workspaceView.setLayoutParams(params);
    }

    private void triggerLayoutRequest() {
        if (mUtilityToolbar != null) {
            mUtilityToolbar.post(() -> {
                mUtilityToolbar.requestLayout();
                View parent = (View) mUtilityToolbar.getParent();
                if (parent != null) {
                    parent.requestLayout();
                    if (parent.getParent() != null) {
                        ((View) parent.getParent()).requestLayout();
                    }
                }
            });
        }
    }

    private void highlightToolbarTab(String tabKey) {
        if (mTbKeysBtn == null) return;

        SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(mContext);
        int customColor = Settings.readKeyboardColor(prefs, mContext);
        int backgroundColor = getKeyboardBackgroundColor();
        boolean isDark = isColorDark(backgroundColor);

        int normalColor = isDark ? 0xAAFFFFFF : 0x88000000;
        int activeColor = customColor;
        if (activeColor == 0 || isColorMonochromeOrTooDark(activeColor)) {
            activeColor = mContext.getResources().getColor(R.color.settings_accent);
        }

        styleToolbarButton(mTbKeysBtn, "keys".equals(tabKey), activeColor, isDark, normalColor);
        styleToolbarButton(mTbClipboardBtn, "clipboard".equals(tabKey), activeColor, isDark, normalColor);
        styleToolbarButton(mTbTranslateBtn, "translate".equals(tabKey), activeColor, isDark, normalColor);
        styleToolbarButton(mTbAiBtn, "ai".equals(tabKey), activeColor, isDark, normalColor);
        styleToolbarButton(mTbEmojiBtn, "emoji".equals(tabKey), activeColor, isDark, normalColor);
        styleToolbarButton(mTbGifBtn, "gif".equals(tabKey), activeColor, isDark, normalColor);
        styleToolbarButton(mTbSettingsBtn, false, activeColor, isDark, normalColor);
    }

    private void styleToolbarButton(ImageView view, boolean isActive, int accentColor, boolean isDark, int normalColor) {
        if (view == null) return;
        
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) params;
            lp.setMargins(dpToPx(6), dpToPx(3), dpToPx(6), dpToPx(3));
            view.setLayoutParams(lp);
        }

        // Dynamically color vector outline icons to match active settings themes!
        view.setColorFilter(isActive ? accentColor : normalColor);

        GradientDrawable badge = new GradientDrawable();
        badge.setShape(GradientDrawable.RECTANGLE);
        badge.setCornerRadius(dpToPx(16)); // Fully rounded capsule indicator
        if (isActive) {
            badge.setColor(getTranslucentColor(accentColor, 24)); // 24% active fill container (M3 style)
            // No border stroke under M3 style
        } else {
            badge.setColor(Color.TRANSPARENT); // Inactive is transparent under M3 style
        }
        view.setBackground(badge);
    }

    public void show() {
        showClipboard();
    }

    public void hide() {
        showKeyboard();
    }

    public boolean isShown() {
        return (mClipboardPanel != null && mClipboardPanel.getVisibility() == View.VISIBLE)
            || (mAiPanel != null && mAiPanel.getVisibility() == View.VISIBLE)
            || (mAiSettingsPanel != null && mAiSettingsPanel.getVisibility() == View.VISIBLE)
            || (mEmojiPanel != null && mEmojiPanel.getVisibility() == View.VISIBLE)
            || (mGifPanel != null && mGifPanel.getVisibility() == View.VISIBLE)
            || (mTranslatePanel != null && mTranslatePanel.getVisibility() == View.VISIBLE);
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                mContext.getResources().getDisplayMetrics()
        );
    }

    private int dpToPx(float dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                mContext.getResources().getDisplayMetrics()
        );
    }

    private int getTranslucentColor(int color, int alphaPercent) {
        int alpha = (int) (255 * (alphaPercent / 100.0));
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private boolean isColorMonochromeOrTooDark(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        return hsv[1] < 0.15f || hsv[2] < 0.20f;
    }

    private int getKeyboardBackgroundColor() {
        SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(mContext);
        KeyboardTheme theme = KeyboardTheme.getKeyboardTheme(prefs);
        int customColor = Settings.readKeyboardColor(prefs, mContext);

        if (mKeyboardView != null) {
            android.graphics.drawable.Drawable kbBg = mKeyboardView.getBackground();
            if (kbBg instanceof android.graphics.drawable.ColorDrawable) {
                return ((android.graphics.drawable.ColorDrawable) kbBg).getColor();
            }
        }

        int backgroundColor = 0xFF121212;
        if (theme.mCustomColorSupport && customColor != 0) {
            backgroundColor = customColor;
        } else {
            TypedValue typedValue = new TypedValue();
            if (mContext.getTheme().resolveAttribute(android.R.attr.colorBackground, typedValue, true)) {
                backgroundColor = typedValue.data;
            }
        }
        return backgroundColor;
    }

    private boolean isColorDark(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255.0;
        return darkness >= 0.5;
    }

    public void updateToolbarLayout() {
        if (mUtilityToolbar == null) return;

        SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(mContext);
        boolean showKeys = prefs.getBoolean("pref_utility_show_keys", true);
        boolean showClipboard = prefs.getBoolean("pref_utility_show_clipboard", true);
        boolean showAi = prefs.getBoolean("pref_utility_show_ai", true);
        boolean showGif = prefs.getBoolean("pref_utility_show_gif", true);
        boolean showTranslate = prefs.getBoolean("pref_utility_show_translate", true);
        boolean showEmoji = prefs.getBoolean(Settings.PREF_SHOW_EMOJI_KEY, true);
        boolean showSettings = prefs.getBoolean("pref_utility_show_settings", true);

        String orderStr = prefs.getString("pref_utility_button_order", "keys,clipboard,translate,ai,gif,emoji,settings");
        if (orderStr == null || orderStr.trim().isEmpty()) {
            orderStr = "keys,clipboard,translate,ai,gif,emoji,settings";
        }
        String[] parts = orderStr.split(",");

        // Clear and rebuild layout programmatically
        mUtilityToolbar.removeAllViews();

        boolean anyVisible = false;

        boolean addedKeys = false;
        boolean addedClipboard = false;
        boolean addedAi = false;
        boolean addedTranslate = false;
        boolean addedEmoji = false;
        boolean addedSettings = false;
        boolean addedGif = false;

        for (String part : parts) {
            String btn = part.trim().toLowerCase();
            if ("keys".equals(btn) && showKeys && mTbKeysBtn != null && !addedKeys) {
                mUtilityToolbar.addView(mTbKeysBtn);
                mTbKeysBtn.setVisibility(View.VISIBLE);
                addedKeys = true;
                anyVisible = true;
            } else if ("clipboard".equals(btn) && showClipboard && mTbClipboardBtn != null && !addedClipboard) {
                mUtilityToolbar.addView(mTbClipboardBtn);
                mTbClipboardBtn.setVisibility(View.VISIBLE);
                addedClipboard = true;
                anyVisible = true;
            } else if ("translate".equals(btn) && showTranslate && mTbTranslateBtn != null && !addedTranslate) {
                mUtilityToolbar.addView(mTbTranslateBtn);
                mTbTranslateBtn.setVisibility(View.VISIBLE);
                addedTranslate = true;
                anyVisible = true;
            } else if ("ai".equals(btn) && showAi && mTbAiBtn != null && !addedAi) {
                mUtilityToolbar.addView(mTbAiBtn);
                mTbAiBtn.setVisibility(View.VISIBLE);
                addedAi = true;
                anyVisible = true;
            } else if ("gif".equals(btn) && showGif && mTbGifBtn != null && !addedGif) {
                mUtilityToolbar.addView(mTbGifBtn);
                mTbGifBtn.setVisibility(View.VISIBLE);
                addedGif = true;
                anyVisible = true;
            } else if ("emoji".equals(btn) && showEmoji && mTbEmojiBtn != null && !addedEmoji) {
                mUtilityToolbar.addView(mTbEmojiBtn);
                mTbEmojiBtn.setVisibility(View.VISIBLE);
                addedEmoji = true;
                anyVisible = true;
            } else if ("settings".equals(btn) && showSettings && mTbSettingsBtn != null && !addedSettings) {
                mUtilityToolbar.addView(mTbSettingsBtn);
                mTbSettingsBtn.setVisibility(View.VISIBLE);
                addedSettings = true;
                anyVisible = true;
            }
        }

        // Add any enabled buttons that were not in the order list
        if (showKeys && mTbKeysBtn != null && !addedKeys) {
            mUtilityToolbar.addView(mTbKeysBtn);
            mTbKeysBtn.setVisibility(View.VISIBLE);
            anyVisible = true;
        }
        if (showClipboard && mTbClipboardBtn != null && !addedClipboard) {
            mUtilityToolbar.addView(mTbClipboardBtn);
            mTbClipboardBtn.setVisibility(View.VISIBLE);
            anyVisible = true;
        }
        if (showTranslate && mTbTranslateBtn != null && !addedTranslate) {
            mUtilityToolbar.addView(mTbTranslateBtn);
            mTbTranslateBtn.setVisibility(View.VISIBLE);
            anyVisible = true;
        }
        if (showAi && mTbAiBtn != null && !addedAi) {
            mUtilityToolbar.addView(mTbAiBtn);
            mTbAiBtn.setVisibility(View.VISIBLE);
            anyVisible = true;
        }
        if (showEmoji && mTbEmojiBtn != null && !addedEmoji) {
            mUtilityToolbar.addView(mTbEmojiBtn);
            mTbEmojiBtn.setVisibility(View.VISIBLE);
            anyVisible = true;
        }
        if (showGif && mTbGifBtn != null && !addedGif) {
            mUtilityToolbar.addView(mTbGifBtn);
            mTbGifBtn.setVisibility(View.VISIBLE);
            anyVisible = true;
        }
        if (showSettings && mTbSettingsBtn != null && !addedSettings) {
            mUtilityToolbar.addView(mTbSettingsBtn);
            mTbSettingsBtn.setVisibility(View.VISIBLE);
            anyVisible = true;
        }

        mUtilityToolbar.setVisibility(anyVisible ? View.VISIBLE : View.GONE);
    }

    public void applyTheming() {
        updateToolbarLayout();

        SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(mContext);
        int customColor = Settings.readKeyboardColor(prefs, mContext);
        int backgroundColor = getKeyboardBackgroundColor();
        boolean isDark = isColorDark(backgroundColor);

        int normalOutline = isDark ? 0x22FFFFFF : 0x1A000000;
        int accentColor = customColor;
        if (accentColor == 0 || isColorMonochromeOrTooDark(accentColor)) {
            accentColor = mContext.getResources().getColor(R.color.settings_accent);
        }
        int textColor = isDark ? 0xFFE0E0E0 : 0xFF333333;
        int hintColor = isDark ? 0x66FFFFFF : 0x66000000;

        // Theme the permanent quickbar toolbar
        if (mUtilityToolbar != null) {
            int tbBgColor = prefs.getInt("pref_utility_background_color", backgroundColor);
            if (tbBgColor == 0) {
                tbBgColor = backgroundColor;
            }
            GradientDrawable tbBg = new GradientDrawable();
            tbBg.setShape(GradientDrawable.RECTANGLE);
            tbBg.setColor(tbBgColor);
            tbBg.setStroke(dpToPx(1), normalOutline);
            mUtilityToolbar.setBackground(tbBg);
        }

        // Theme the clipboard list panel
        if (mClipboardPanel != null) {
            mClipboardPanel.setBackgroundColor(backgroundColor);
        }



        // Theme the AI Panel Workspace
        if (mAiPanel != null) {
            mAiPanel.setBackgroundColor(backgroundColor);
        }

        // Theme the Translate Panel Workspace
        if (mTranslatePanel != null) {
            mTranslatePanel.setBackgroundColor(backgroundColor);
        }

        if (mTranslateSourceBtn != null) {
            GradientDrawable btnBg = new GradientDrawable();
            btnBg.setShape(GradientDrawable.RECTANGLE);
            btnBg.setCornerRadius(dpToPx(8));
            btnBg.setColor(isDark ? 0x14FFFFFF : 0x08000000);
            mTranslateSourceBtn.setBackground(btnBg);
            mTranslateSourceBtn.setTextColor(textColor);
        }

        if (mTranslateTargetBtn != null) {
            GradientDrawable btnBg = new GradientDrawable();
            btnBg.setShape(GradientDrawable.RECTANGLE);
            btnBg.setCornerRadius(dpToPx(8));
            btnBg.setColor(isDark ? 0x14FFFFFF : 0x08000000);
            mTranslateTargetBtn.setBackground(btnBg);
            mTranslateTargetBtn.setTextColor(textColor);
        }

        if (mTranslateModeBtn != null) {
            GradientDrawable btnBg = new GradientDrawable();
            btnBg.setShape(GradientDrawable.RECTANGLE);
            btnBg.setCornerRadius(dpToPx(8));
            btnBg.setColor(getTranslucentColor(accentColor, 12));
            mTranslateModeBtn.setBackground(btnBg);
            mTranslateModeBtn.setTextColor(accentColor);
        }

        if (mTranslateInput != null) {
            styleConfigField(mTranslateInput, mActiveInput == mTranslateInput);
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
            insBg.setCornerRadius(dpToPx(16));
            insBg.setColor(isDark ? 0x2200E676 : 0x1A00E676);
            insBg.setStroke(dpToPx(1), 0xFF00E676);
            mTranslateInsertBtn.setBackground(insBg);
            mTranslateInsertBtn.setTextColor(0xFF00E676);
        }



        // Theme AI console config trigger button
        if (mAiOpenSettingsBtn != null) {
            mAiOpenSettingsBtn.setColorFilter(accentColor);
            mAiOpenSettingsBtn.setBackground(null);
        }

        // Theme monospaced console preview
        if (mAiConsoleScroll != null) {
            GradientDrawable consoleBg = new GradientDrawable();
            consoleBg.setShape(GradientDrawable.RECTANGLE);
            consoleBg.setCornerRadius(dpToPx(10));
            consoleBg.setColor(isDark ? 0xFF0A0A0A : 0xFFF5F5F5); // Deep terminal black or light bone
            consoleBg.setStroke(dpToPx(1.5f), accentColor); // Glowing custom accent border!
            mAiConsoleScroll.setBackground(consoleBg);
        }

        if (mAiConsoleText != null) {
            mAiConsoleText.setTextColor(isDark ? 0xFFEEEEEE : 0xFF222222);
        }

        // Theme Copy & Insert badges inside header bar
        if (mAiActionCopy != null) {
            mAiActionCopy.setTextColor(isDark ? 0xCCFFFFFF : 0xAA000000);
            mAiActionCopy.setBackground(null);
        }

        if (mAiActionInsert != null) {
            mAiActionInsert.setTextColor(accentColor);
            mAiActionInsert.setBackground(null);
        }

        // Theme Assist Tool chips
        styleToolChip(mAiToolSmartCompose, accentColor, isDark);
        styleToolChip(mAiToolSimplify, accentColor, isDark);
        styleToolChip(mAiToolGrammarFix, accentColor, isDark);
        styleToolChip(mAiToolExplain, accentColor, isDark);
        styleToolChip(mAiToolFix, accentColor, isDark);

        // Theme prompt input field
        if (mAiPromptInput != null) {
            styleConfigField(mAiPromptInput, mActiveInput == mAiPromptInput);
        }

        if (mAiSubmitBtn != null) {
            GradientDrawable sbBg = new GradientDrawable();
            sbBg.setShape(GradientDrawable.RECTANGLE);
            sbBg.setCornerRadius(dpToPx(16)); // Fully rounded button
            sbBg.setColor(accentColor);
            mAiSubmitBtn.setBackground(sbBg);
            mAiSubmitBtn.setTextColor(Color.WHITE);
        }

        if (mAiPasteBtn != null) {
            GradientDrawable pbBg = new GradientDrawable();
            pbBg.setShape(GradientDrawable.RECTANGLE);
            pbBg.setCornerRadius(dpToPx(16));
            pbBg.setColor(isDark ? 0x22FFFFFF : 0x1A000000);
            pbBg.setStroke(dpToPx(1), accentColor);
            mAiPasteBtn.setBackground(pbBg);
            mAiPasteBtn.setTextColor(accentColor);
        }

        if (mClipboardSuggestionChip != null) {
            GradientDrawable csBg = new GradientDrawable();
            csBg.setShape(GradientDrawable.RECTANGLE);
            csBg.setCornerRadius(dpToPx(16));
            csBg.setColor(isDark ? 0x22FFFFFF : 0x1A000000);
            csBg.setStroke(dpToPx(1.5f), accentColor);
            mClipboardSuggestionChip.setBackground(csBg);
            mClipboardSuggestionChip.setTextColor(isDark ? 0xFFEEEEEE : 0xFF222222);
        }

        // Theme In-Keyboard settings drawer dynamically
        if (mAiSettingsPanel != null) {
            mAiSettingsPanel.setBackgroundColor(backgroundColor);
        }
        if (mSettingsPanelTitle != null) {
            mSettingsPanelTitle.setTextColor(textColor);
        }

        styleConfigField(mSetupGeminiKey, mActiveInput == mSetupGeminiKey);
        styleConfigField(mSetupHostUrl, mActiveInput == mSetupHostUrl);
        styleConfigField(mSetupModelName, mActiveInput == mSetupModelName);
        styleConfigField(mSetupHeadersJson, mActiveInput == mSetupHeadersJson);

        if (mAiClearBtn != null) {
            mAiClearBtn.setTextColor(0xFFFF5252);
            mAiClearBtn.setBackground(null);
        }

        if (mSetupGeminiLabel != null) mSetupGeminiLabel.setTextColor(hintColor);
        if (mSetupHostLabel != null) mSetupHostLabel.setTextColor(hintColor);
        if (mSetupModelLabel != null) mSetupModelLabel.setTextColor(hintColor);
        if (mSetupHeadersLabel != null) mSetupHeadersLabel.setTextColor(hintColor);

        if (mSetupSaveBtn != null) {
            GradientDrawable svBg = new GradientDrawable();
            svBg.setShape(GradientDrawable.RECTANGLE);
            svBg.setCornerRadius(dpToPx(16)); // Fully rounded button
            svBg.setColor(accentColor);
            mSetupSaveBtn.setBackground(svBg);
            mSetupSaveBtn.setTextColor(Color.WHITE);
        }
        if (mSetupBackBtn != null) {
            mSetupBackBtn.setTextColor(textColor);
            mSetupBackBtn.setTextSize(22);
            GradientDrawable bkBg = new GradientDrawable();
            bkBg.setShape(GradientDrawable.RECTANGLE);
            bkBg.setCornerRadius(dpToPx(16)); // Fully rounded button
            bkBg.setColor(isDark ? 0x0AFFFFFF : 0x05000000);
            mSetupBackBtn.setBackground(bkBg);
        }

        buildTabs();
    }

    private void styleConfigField(EditText et, boolean isActive) {
        if (et == null) return;
        SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(mContext);
        int customColor = Settings.readKeyboardColor(prefs, mContext);
        int backgroundColor = getKeyboardBackgroundColor();
        boolean isDark = isColorDark(backgroundColor);
        
        int accentColor = customColor;
        if (accentColor == 0 || isColorMonochromeOrTooDark(accentColor)) {
            accentColor = mContext.getResources().getColor(R.color.settings_accent);
        }
        
        et.setTextColor(isDark ? 0xFFEEEEEE : 0xFF222222);
        et.setHintTextColor(isDark ? 0x55FFFFFF : 0x55000000);
        
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dpToPx(12));
        bg.setColor(isDark ? 0x14FFFFFF : 0x08000000);
        
        if (isActive) {
            bg.setStroke(dpToPx(1.5f), accentColor);
        }
        et.setBackground(bg);
    }

    private void styleActionBadge(TextView v, int activeColor, int outlineColor, boolean isDark) {
        if (v == null) return;
        v.setTextColor(isDark ? 0xDDFFFFFF : 0xFF222222);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dpToPx(16)); // Fully rounded capsule
        bg.setColor(getTranslucentColor(activeColor, 16)); // Filled container
        // No stroke under M3 style
        v.setBackground(bg);
    }

    private void styleToolChip(TextView v, int accentColor, boolean isDark) {
        if (v == null) return;
        v.setTextSize(10.0f);
        v.setTextColor(accentColor); // Dynamic accent text
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dpToPx(16)); // Pill shape
        bg.setColor(getTranslucentColor(accentColor, 12)); // 12% alpha accent background
        v.setBackground(bg);
    }

    private void buildTabs() {
        if (mTabsLayout == null) return;
        mTabsLayout.removeAllViews();

        SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(mContext);
        int customColor = Settings.readKeyboardColor(prefs, mContext);
        int backgroundColor = getKeyboardBackgroundColor();
        boolean isDark = isColorDark(backgroundColor);

        int normalColor = isDark ? 0x99FFFFFF : 0x88000000;
        int activeColor = customColor;
        if (activeColor == 0 || isColorMonochromeOrTooDark(activeColor)) {
            activeColor = mContext.getResources().getColor(R.color.settings_accent);
        }

        String[] tabKeys = {"all", "pinned", "links"};
        String[] tabTitles = {"ALL", "★ PIN", "🔗 LINK"};

        for (int i = 0; i < tabKeys.length; i++) {
            final String key = tabKeys[i];
            TextView tabBtn = new TextView(mContext);
            tabBtn.setText(tabTitles[i]);
            tabBtn.setTextSize(9.0f);
            tabBtn.setGravity(Gravity.CENTER);
            tabBtn.setPadding(dpToPx(8), dpToPx(5), dpToPx(8), dpToPx(5));
            tabBtn.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            btnParams.setMargins(dpToPx(2), 0, dpToPx(2), 0);
            tabBtn.setLayoutParams(btnParams);

            boolean isActive = mSelectedTab.equals(key);
            tabBtn.setTextColor(isActive ? activeColor : normalColor);

            GradientDrawable tabBg = new GradientDrawable();
            tabBg.setShape(GradientDrawable.RECTANGLE);
            tabBg.setCornerRadius(dpToPx(16)); // Fully rounded capsule indicators
            if (isActive) {
                tabBg.setColor(getTranslucentColor(activeColor, 24)); // 24% active container
                // No stroke/border
            } else {
                tabBg.setColor(Color.TRANSPARENT);
            }
            tabBtn.setBackground(tabBg);

            tabBtn.setClickable(true);
            tabBtn.setFocusable(true);
            tabBtn.setOnClickListener(v -> {
                mSelectedTab = key;
                buildTabs();
                refresh();
            });
            
            mTabsLayout.addView(tabBtn);
        }
    }

    public void refresh() {
        if (mItemsList == null) return;
        mItemsList.removeAllViews();

        List<ClipboardHistoryManager.ClipboardItem> allItems = mManager.getItems();
        List<ClipboardHistoryManager.ClipboardItem> items = new java.util.ArrayList<>();
        
        for (ClipboardHistoryManager.ClipboardItem item : allItems) {
            if (mSearchQuery != null && !mSearchQuery.isEmpty()) {
                if (!item.text.toLowerCase().contains(mSearchQuery)) {
                    continue;
                }
            }
            if (mSelectedTab.equals("pinned") && !item.isPinned) {
                continue;
            }
            if (mSelectedTab.equals("links")) {
                String text = item.text.toLowerCase();
                if (!text.contains("http://") && !text.contains("https://") && !text.contains("www.")) {
                    continue;
                }
            }
            items.add(item);
        }

        SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(mContext);
        int customColor = Settings.readKeyboardColor(prefs, mContext);
        int backgroundColor = getKeyboardBackgroundColor();
        boolean isDark = isColorDark(backgroundColor);
        int hintColor = isDark ? 0x88FFFFFF : 0x88000000;

        int accentColor = customColor;
        if (accentColor == 0 || isColorMonochromeOrTooDark(accentColor)) {
            accentColor = mContext.getResources().getColor(R.color.settings_accent);
        }

        if (items.isEmpty()) {
            TextView emptyView = new TextView(mContext);
            emptyView.setText("No clipboard snippets found.");
            emptyView.setTextSize(12);
            emptyView.setGravity(Gravity.CENTER);
            emptyView.setPadding(32, dpToPx(32), 32, dpToPx(32));
            emptyView.setTextColor(hintColor);
            mItemsList.addView(emptyView);
            return;
        }

        int normalOutline = isDark ? 0x1AFFFFFF : 0x15000000;
        int cardFill = isDark ? 0x0EFFFFFF : 0x08000000;

        for (final ClipboardHistoryManager.ClipboardItem item : items) {
            LinearLayout rowLayout = new LinearLayout(mContext);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setGravity(Gravity.CENTER_VERTICAL);
            rowLayout.setPadding(dpToPx(10), dpToPx(7), dpToPx(10), dpToPx(7));

            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setShape(GradientDrawable.RECTANGLE);
            cardBg.setCornerRadius(dpToPx(10));
            if (item.isPinned) {
                cardBg.setColor(getTranslucentColor(accentColor, 10));
                cardBg.setStroke(dpToPx(1), accentColor);
            } else {
                cardBg.setColor(cardFill);
                cardBg.setStroke(dpToPx(1), normalOutline);
            }
            rowLayout.setBackground(cardBg);

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            rowParams.setMargins(0, dpToPx(3), 0, dpToPx(3));
            rowLayout.setLayoutParams(rowParams);

            final TextView clipText = new TextView(mContext);
            clipText.setText(item.text);
            clipText.setTextSize(12);
            clipText.setMaxLines(2);
            clipText.setEllipsize(android.text.TextUtils.TruncateAt.END);
            clipText.setTextColor(isDark ? 0xFFEEEEEE : 0xFF222222);
            clipText.setGravity(Gravity.CENTER_VERTICAL);
            
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1.0f
            );
            clipText.setLayoutParams(textParams);

            clipText.setOnTouchListener(new View.OnTouchListener() {
                private float startX = 0;
                private float startY = 0;
                private boolean isSwiping = false;
                private boolean isScrolling = false;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            startX = event.getX();
                            startY = event.getY();
                            isSwiping = false;
                            isScrolling = false;
                            v.getParent().requestDisallowInterceptTouchEvent(false);
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            float diffX = event.getX() - startX;
                            float diffY = event.getY() - startY;
                            if (isScrolling) return false;
                            
                            if (!isSwiping && Math.abs(diffY) > dpToPx(8) && Math.abs(diffY) > Math.abs(diffX)) {
                                isScrolling = true;
                                return false;
                            }
                            if (Math.abs(diffX) > dpToPx(8)) {
                                isSwiping = true;
                                v.getParent().requestDisallowInterceptTouchEvent(true);
                                v.setTranslationX(diffX);
                                float alpha = 1.0f - Math.abs(diffX) / (float) v.getWidth();
                                v.setAlpha(Math.max(0.1f, alpha));
                                return true;
                            }
                            break;
                        case MotionEvent.ACTION_CANCEL:
                            isSwiping = false;
                            isScrolling = false;
                            v.animate().translationX(0).alpha(1.0f).setDuration(200).start();
                            return true;
                        case MotionEvent.ACTION_UP:
                            if (isScrolling) {
                                isScrolling = false;
                                isSwiping = false;
                                return false;
                            }
                            float finalDiffX = v.getTranslationX();
                            if (isSwiping && Math.abs(finalDiffX) > v.getWidth() / 3.0f) {
                                float targetX = finalDiffX > 0 ? v.getWidth() : -v.getWidth();
                                v.animate().translationX(targetX).alpha(0.0f).setDuration(150).withEndAction(() -> {
                                    mManager.deleteItem(item.text);
                                    if (mVibrator != null) {
                                        try {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                mVibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
                                            } else {
                                                mVibrator.vibrate(30);
                                            }
                                        } catch (Exception e) {}
                                    }
                                    refresh();
                                }).start();
                            } else {
                                v.animate().translationX(0).alpha(1.0f).setDuration(200).start();
                                if (!isSwiping) {
                                    float finalDiffY = event.getY() - startY;
                                    if (Math.abs(finalDiffX) < dpToPx(8) && Math.abs(finalDiffY) < dpToPx(8)) {
                                        if (mListener != null) {
                                            mListener.onItemClick(item.text);
                                        }
                                    }
                                }
                            }
                            isSwiping = false;
                            isScrolling = false;
                            return true;
                    }
                    return false;
                }
            });

            rowLayout.addView(clipText);

            View spacer = new View(mContext);
            rowLayout.addView(spacer, new LinearLayout.LayoutParams(dpToPx(12), 1));

            // Pin button
            final TextView pinBtn = new TextView(mContext);
            pinBtn.setText(item.isPinned ? "★" : "☆");
            pinBtn.setTextSize(11);
            pinBtn.setGravity(Gravity.CENTER);
            pinBtn.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(26), dpToPx(26)));
            
            GradientDrawable pinBg = new GradientDrawable();
            pinBg.setShape(GradientDrawable.OVAL);
            if (item.isPinned) {
                pinBg.setColor(getTranslucentColor(accentColor, 20));
                pinBg.setStroke(dpToPx(1), accentColor);
                pinBtn.setTextColor(accentColor);
            } else {
                pinBg.setColor(isDark ? 0x11FFFFFF : 0x08000000);
                pinBg.setStroke(dpToPx(1), isDark ? 0x22FFFFFF : 0x1A000000);
                pinBtn.setTextColor(isDark ? 0x88FFFFFF : 0x88000000);
            }
            pinBtn.setBackground(pinBg);
            pinBtn.setOnClickListener(v -> {
                mManager.togglePin(item.text);
                refresh();
            });
            rowLayout.addView(pinBtn);

            View spacer2 = new View(mContext);
            rowLayout.addView(spacer2, new LinearLayout.LayoutParams(dpToPx(6), 1));

            // Delete button
            TextView delBtn = new TextView(mContext);
            delBtn.setText("✕");
            delBtn.setTextSize(10);
            delBtn.setGravity(Gravity.CENTER);
            delBtn.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(26), dpToPx(26)));
            
            GradientDrawable delBg = new GradientDrawable();
            delBg.setShape(GradientDrawable.OVAL);
            delBg.setColor(isDark ? 0x22FF1744 : 0x15FF1744);
            delBg.setStroke(dpToPx(1), 0xFFFF1744);
            delBtn.setBackground(delBg);
            delBtn.setTextColor(0xFFFF1744);
            delBtn.setOnClickListener(v -> {
                mManager.deleteItem(item.text);
                refresh();
            });
            rowLayout.addView(delBtn);

            mItemsList.addView(rowLayout);
        }
    }

    private TextView addMessageBubble(String sender, String text) {
        if (mAiChatLog == null) return null;

        LinearLayout wrapper = new LinearLayout(mContext);
        wrapper.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        wrapper.setOrientation(LinearLayout.VERTICAL);

        TextView bubbleText = new TextView(mContext);
        bubbleText.setText(text);
        bubbleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        bubbleText.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));

        SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(mContext);
        int customColor = Settings.readKeyboardColor(prefs, mContext);
        int backgroundColor = getKeyboardBackgroundColor();
        boolean isDark = isColorDark(backgroundColor);
        
        int accentColor = customColor;
        if (accentColor == 0 || isColorMonochromeOrTooDark(accentColor)) {
            accentColor = mContext.getResources().getColor(R.color.settings_accent);
        }

        LinearLayout.LayoutParams bubbleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        bubbleParams.bottomMargin = dpToPx(8);

        if ("user".equalsIgnoreCase(sender)) {
            wrapper.setGravity(Gravity.END);
            bubbleParams.gravity = Gravity.END;
            bubbleParams.leftMargin = dpToPx(48); // Avoid stretching full width

            GradientDrawable userBg = new GradientDrawable();
            userBg.setShape(GradientDrawable.RECTANGLE);
            userBg.setColor(accentColor);
            userBg.setCornerRadii(new float[] {
                (float) dpToPx(18), (float) dpToPx(18),
                (float) dpToPx(18), (float) dpToPx(18),
                0f, 0f,
                (float) dpToPx(18), (float) dpToPx(18)
            });
            bubbleText.setBackground(userBg);
            bubbleText.setTextColor(isColorDark(accentColor) ? Color.WHITE : 0xFF222222);
        } else {
            wrapper.setGravity(Gravity.START);
            bubbleParams.gravity = Gravity.START;
            bubbleParams.rightMargin = dpToPx(48);

            GradientDrawable botBg = new GradientDrawable();
            botBg.setShape(GradientDrawable.RECTANGLE);
            botBg.setColor(isDark ? 0x14FFFFFF : 0x0C000000); // Borderless filled container
            botBg.setCornerRadii(new float[] {
                (float) dpToPx(18), (float) dpToPx(18),
                (float) dpToPx(18), (float) dpToPx(18),
                (float) dpToPx(18), (float) dpToPx(18),
                0f, 0f
            });
            bubbleText.setBackground(botBg);
            bubbleText.setTextColor(isDark ? 0xFFEEEEEE : 0xFF222222);
        }

        bubbleText.setLayoutParams(bubbleParams);
        wrapper.addView(bubbleText);
        mAiChatLog.addView(wrapper);

        // Auto scroll to bottom
        if (mAiConsoleScroll != null) {
            mAiConsoleScroll.post(() -> mAiConsoleScroll.fullScroll(View.FOCUS_DOWN));
        }

        return bubbleText;
    }

    private void clearChat() {
        if (mAiChatLog != null) {
            mAiChatLog.removeAllViews();
            addMessageBubble("bot", "System Ready. Tap an AI Assist action or type a prompt...");
        }
        if (mAiManager != null) {
            mAiManager.clearChatHistory();
        }
    }

    private boolean isSentenceDelimiter(char c) {
        return c == '.' || c == '?' || c == '!' || c == '\n';
    }

    private void performAutoGrammarCorrection() {
        if (!(mContext instanceof LatinIME)) return;
        final InputConnection conn = ((LatinIME) mContext).getCurrentInputConnection();
        if (conn == null) return;

        // Perform tactile buzz
        if (mVibrator != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    mVibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
                } else {
                    mVibrator.vibrate(40);
                }
            } catch (Exception e) {}
        }

        CharSequence selection = conn.getSelectedText(0);
        String originalText = "";
        boolean isSelection = false;
        int replaceLength = 0;

        if (selection != null && selection.length() > 0) {
            originalText = selection.toString();
            isSelection = true;
        } else {
            // Find current sentence by scanning backwards
            CharSequence preceding = conn.getTextBeforeCursor(400, 0);
            if (preceding == null || preceding.length() == 0) {
                Toast.makeText(mContext, "No text to correct", Toast.LENGTH_SHORT).show();
                return;
            }

            String precedingText = preceding.toString();
            int end = precedingText.length() - 1;
            // Skip trailing whitespace and punctuation first
            while (end >= 0 && (Character.isWhitespace(precedingText.charAt(end)) || isSentenceDelimiter(precedingText.charAt(end)))) {
                end--;
            }

            int boundary = -1;
            for (int i = end; i >= 0; i--) {
                if (isSentenceDelimiter(precedingText.charAt(i))) {
                    boundary = i;
                    break;
                }
            }

            int sentenceStart = boundary + 1;
            originalText = precedingText.substring(sentenceStart);
            replaceLength = originalText.length();
        }

        final String textToCorrect = originalText;
        if (textToCorrect.trim().isEmpty()) {
            Toast.makeText(mContext, "No sentence to correct", Toast.LENGTH_SHORT).show();
            return;
        }

        // Keep leading spaces
        int leadingSpaceCount = 0;
        while (leadingSpaceCount < textToCorrect.length() && textToCorrect.charAt(leadingSpaceCount) == ' ') {
            leadingSpaceCount++;
        }
        final int finalLeadingSpaces = leadingSpaceCount;
        final boolean finalIsSelection = isSelection;
        final int finalReplaceLength = replaceLength;

        Toast.makeText(mContext, "Correcting grammar...", Toast.LENGTH_SHORT).show();

        String prompt = "You are a native American English speaker. Correct the grammar, spelling, punctuation, and phrasing in the following text. "
                      + "Do NOT make it sound like an AI, robotic, or overly artificial. Keep the phrasing extremely natural, casual, and authentic, exactly like how an American would type it. "
                      + "Provide ONLY the corrected version of the text, with absolutely no explanations, no chat, no markdown, no quotes, and no formatting. "
                      + "Preserve the original language, tone, capitalization style, and spacing as much as possible, just make it grammatically correct. "
                      + "Here is the text to correct:\n\n"
                      + textToCorrect.trim();

        mAiManager.queryAi(prompt, new AiCopilotManager.AiCallback() {
            @Override
            public void onSuccess(String responseText) {
                String corrected = responseText.trim();
                // Clean potential quotes added by the AI
                if (corrected.startsWith("\"") && corrected.endsWith("\"") && !textToCorrect.trim().startsWith("\"")) {
                    corrected = corrected.substring(1, corrected.length() - 1).trim();
                } else if (corrected.startsWith("'") && corrected.endsWith("'") && !textToCorrect.trim().startsWith("'")) {
                    corrected = corrected.substring(1, corrected.length() - 1).trim();
                }

                // Restore leading spaces
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < finalLeadingSpaces; i++) {
                    sb.append(" ");
                }
                sb.append(corrected);
                String finalResult = sb.toString();

                if (finalIsSelection) {
                    conn.commitText(finalResult, 1);
                } else {
                    conn.deleteSurroundingText(finalReplaceLength, 0);
                    conn.commitText(finalResult, 1);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(mContext, "Correction failed: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void configureSimulatedInput(EditText et) {
        if (et == null) return;
        et.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        et.setFocusable(true);
        et.setFocusableInTouchMode(true);
        et.setShowSoftInputOnFocus(false);
        et.setCursorVisible(true);
        et.setOnClickListener(v -> setActiveInput(et));
        et.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                setActiveInput(et);
            }
        });
    }

    private void startCursorBlink() {
        if (mActiveInput != null) {
            mActiveInput.setCursorVisible(true);
        }
    }

    private void stopCursorBlink() {
        if (mActiveInput != null) {
            mActiveInput.setCursorVisible(false);
        }
    }

    private void setActiveInput(EditText et) {
        if (mActiveInput == et) return;
        EditText prev = mActiveInput;
        stopCursorBlink();
        mActiveInput = et;
        if (prev != null) {
            styleConfigField(prev, false);
        }
        if (mActiveInput != null) {
            styleConfigField(mActiveInput, true);
            mActiveInput.requestFocus();
            startCursorBlink();
        }
    }

    private boolean isViewOrParentVisible(View view) {
        if (view == null) return false;
        View current = view;
        while (current != null) {
            if (current.getVisibility() != View.VISIBLE) {
                return false;
            }
            if (current.getParent() instanceof View) {
                current = (View) current.getParent();
            } else {
                break;
            }
        }
        return true;
    }

    public boolean onCodeInput(final int codePoint) {
        if (mActiveInput == null || !isShown()) {
            return false;
        }
        if (!isViewOrParentVisible(mActiveInput)) {
            return false;
        }

        if (codePoint == '=') {
            if (handleInlineMathForSimulatedInput()) {
                return true;
            }
        }

        if (codePoint == -5) { // Backspace
            String text = mActiveInput.getText().toString();
            if (text.length() > 0) {
                int selStart = mActiveInput.getSelectionStart();
                int selEnd = mActiveInput.getSelectionEnd();
                if (selStart >= 0 && selEnd >= 0 && selStart != selEnd) {
                    int min = Math.min(selStart, selEnd);
                    int max = Math.max(selStart, selEnd);
                    String newText = text.substring(0, min) + text.substring(max);
                    mActiveInput.setText(newText);
                    mActiveInput.setSelection(min);
                } else if (selStart > 0) {
                    String newText = text.substring(0, selStart - 1) + text.substring(selStart);
                    mActiveInput.setText(newText);
                    mActiveInput.setSelection(selStart - 1);
                } else if (selStart == 0) {
                    // Do nothing
                } else {
                    mActiveInput.setText(text.substring(0, text.length() - 1));
                    mActiveInput.setSelection(mActiveInput.getText().length());
                }
            }
            return true;
        }

        if (codePoint == '\n') { // Enter
            if (mActiveInput == mAiPromptInput) {
                if (mAiSubmitBtn != null && mAiSubmitBtn.isEnabled()) {
                    mAiSubmitBtn.performClick();
                }
            } else if (mActiveInput == mGifSearchInput) {
                if (mGifSearchRunnable != null) {
                    mGifSearchHandler.removeCallbacks(mGifSearchRunnable);
                }
                loadGifs(mGifSearchInput.getText().toString());
            } else if (mActiveInput == mTranslateInput) {
                if (mTranslateRunnable != null) {
                    mTranslateHandler.removeCallbacks(mTranslateRunnable);
                }
                triggerTranslation();

            } else {
                if (mSetupSaveBtn != null && mSetupSaveBtn.isEnabled()) {
                    mSetupSaveBtn.performClick();
                }
            }
            return true;
        }

        if (codePoint < 0) {
            return false;
        }

        String textToAppend = new String(Character.toChars(codePoint));
        String text = mActiveInput.getText().toString();
        int selStart = mActiveInput.getSelectionStart();
        int selEnd = mActiveInput.getSelectionEnd();
        if (selStart >= 0 && selEnd >= 0) {
            int min = Math.min(selStart, selEnd);
            int max = Math.max(selStart, selEnd);
            String newText = text.substring(0, min) + textToAppend + text.substring(max);
            mActiveInput.setText(newText);
            mActiveInput.setSelection(min + textToAppend.length());
        } else {
            mActiveInput.append(textToAppend);
            mActiveInput.setSelection(mActiveInput.getText().length());
        }
        return true;
    }

    public boolean onTextInput(final String rawText) {
        if (mActiveInput == null || !isShown()) {
            return false;
        }
        if (!isViewOrParentVisible(mActiveInput)) {
            return false;
        }

        String text = mActiveInput.getText().toString();
        int selStart = mActiveInput.getSelectionStart();
        int selEnd = mActiveInput.getSelectionEnd();
        if (selStart >= 0 && selEnd >= 0) {
            int min = Math.min(selStart, selEnd);
            int max = Math.max(selStart, selEnd);
            String newText = text.substring(0, min) + rawText + text.substring(max);
            mActiveInput.setText(newText);
            mActiveInput.setSelection(min + rawText.length());
        } else {
            mActiveInput.append(rawText);
            mActiveInput.setSelection(mActiveInput.getText().length());
        }
        return true;
    }

    private void setupDeleteButton() {
        if (mEmojiDeleteBtn != null) {
            mEmojiDeleteBtn.setOnTouchListener(new View.OnTouchListener() {
                private android.os.Handler handler;
                private Runnable runnable;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    int action = event.getAction();
                    if (action == MotionEvent.ACTION_DOWN) {
                        performDelete();
                        v.setPressed(true);

                        if (handler == null) {
                            handler = new android.os.Handler(android.os.Looper.getMainLooper());
                        }
                        runnable = new Runnable() {
                            @Override
                            public void run() {
                                performDelete();
                                if (handler != null) {
                                    handler.postDelayed(this, 80);
                                }
                            }
                        };
                        handler.postDelayed(runnable, 400);
                        return true;
                    } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                        v.setPressed(false);
                        if (handler != null && runnable != null) {
                            handler.removeCallbacks(runnable);
                        }
                        return true;
                    }
                    return false;
                }
            });
        }
    }

    private void performDelete() {
        if (mContext instanceof LatinIME) {
            LatinIME ime = (LatinIME) mContext;
            ime.sendDownUpKeyEvents(android.view.KeyEvent.KEYCODE_DEL);

            AudioAndHapticFeedbackManager feedback = AudioAndHapticFeedbackManager.getInstance();
            feedback.performAudioFeedback(nabu.iris.keyboard.latin.common.Constants.CODE_DELETE);
            feedback.performHapticFeedback(mEmojiPanel);
        }
    }

    private void hideClipboardSuggestion() {
        if (mClipboardSuggestionBar != null) {
            mClipboardSuggestionBar.setVisibility(View.GONE);
        }
        mSuggestionDismissHandler.removeCallbacks(mSuggestionDismissRunnable);
    }

    public void checkAndShowClipboardSuggestion() {
        if (mClipboardSuggestionBar == null || mClipboardSuggestionChip == null) return;

        mSuggestionDismissHandler.removeCallbacks(mSuggestionDismissRunnable);

        boolean isWindowShown = false;
        if (mContext instanceof LatinIME) {
            isWindowShown = ((LatinIME) mContext).isInputViewShown();
        }

        if (!isWindowShown) {
            mClipboardSuggestionBar.setVisibility(View.GONE);
            return;
        }

        String clipText = getMostRecentClipboardText();
        if (clipText != null && !clipText.trim().isEmpty() && !clipText.equals(sDismissedText)) {
            // Track when this specific clipboard text was first shown
            if (!clipText.equals(sLastShownClipText)) {
                sLastShownClipText = clipText;
                sClipShowStartTime = System.currentTimeMillis();
            }

            long elapsedMs = System.currentTimeMillis() - sClipShowStartTime;

            SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(mContext);
            int timeoutSecs = 60;
            try {
                String timeoutStr = prefs.getString("pref_clip_suggestion_timeout", "60");
                timeoutSecs = Integer.parseInt(timeoutStr.trim());
            } catch (Exception e) {
                // fallback
            }

            long timeoutMs = timeoutSecs * 1000L;
            if (timeoutSecs > 0 && elapsedMs >= timeoutMs) {
                mClipboardSuggestionBar.setVisibility(View.GONE);
                return;
            }

            String preview = clipText.replace("\n", " ").trim();
            if (preview.length() > 25) {
                preview = preview.substring(0, 22) + "...";
            }
            mClipboardSuggestionChip.setText("📋 Paste: " + preview);
            
            mClipboardSuggestionChip.setOnClickListener(v -> {
                if (mContext instanceof LatinIME) {
                    InputConnection conn = ((LatinIME) mContext).getCurrentInputConnection();
                    if (conn != null) {
                        conn.commitText(clipText, 1);
                    }
                }
                mClipboardSuggestionBar.setVisibility(View.GONE);
                sDismissedText = clipText;
                mSuggestionDismissHandler.removeCallbacks(mSuggestionDismissRunnable);
            });

            mClipboardSuggestionChip.setOnLongClickListener(v -> {
                if (mVibrator != null) {
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            mVibrator.vibrate(android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                        } else {
                            mVibrator.vibrate(50);
                        }
                    } catch (Exception e) {}
                }
                mClipboardSuggestionBar.setVisibility(View.GONE);
                sDismissedText = clipText;
                mSuggestionDismissHandler.removeCallbacks(mSuggestionDismissRunnable);
                Toast.makeText(mContext, "Suggestion dismissed", Toast.LENGTH_SHORT).show();
                return true;
            });

            mClipboardSuggestionBar.setVisibility(View.VISIBLE);

            if (timeoutSecs > 0) {
                long remainingMs = timeoutMs - elapsedMs;
                if (remainingMs > 0) {
                    mSuggestionDismissHandler.postDelayed(mSuggestionDismissRunnable, remainingMs);
                } else {
                    mClipboardSuggestionBar.setVisibility(View.GONE);
                }
            }
        } else {
            mClipboardSuggestionBar.setVisibility(View.GONE);
        }
    }

    private static String extractMathExpression(String text) {
        if (text == null || text.isEmpty()) return null;
        int start = text.length() - 1;
        boolean hasOperator = false;
        while (start >= 0) {
            char c = text.charAt(start);
            if (Character.isDigit(c) || c == '.' || c == ' ' || c == '(' || c == ')') {
                if (c == '(' || c == ')') hasOperator = true;
                start--;
            } else if (c == '+' || c == '-' || c == '*' || c == '/' || c == '%' || c == '^') {
                hasOperator = true;
                start--;
            } else {
                break;
            }
        }
        if (!hasOperator) return null;
        String expr = text.substring(start + 1).trim();
        if (expr.isEmpty()) return null;
        return expr;
    }

    private static double evaluateMath(final String str) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char)ch);
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if      (eat('+')) x += parseTerm();
                    else if (eat('-')) x -= parseTerm();
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if      (eat('*')) x *= parseFactor();
                    else if (eat('/')) {
                        double divisor = parseFactor();
                        if (divisor == 0) throw new ArithmeticException("Division by zero");
                        x /= divisor;
                    }
                    else if (eat('%')) {
                        double divisor = parseFactor();
                        if (divisor == 0) throw new ArithmeticException("Division by zero");
                        x %= divisor;
                    }
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor();
                if (eat('-')) return -parseFactor();

                double x;
                int startPos = this.pos;
                if (eat('(')) {
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else {
                    throw new RuntimeException("Unexpected character: " + (char)ch);
                }

                if (eat('^')) x = Math.pow(x, parseFactor());

                return x;
            }
        }.parse();
    }

    private static String formatMathResult(double val) {
        if (Double.isInfinite(val) || Double.isNaN(val)) {
            throw new ArithmeticException("Invalid math result");
        }
        if (val == (long) val) {
            return String.valueOf((long) val);
        }
        java.text.DecimalFormat df = new java.text.DecimalFormat("#.######");
        return df.format(val);
    }

    public static boolean handleInlineMath(InputConnection conn) {
        if (conn == null) return false;
        CharSequence before = conn.getTextBeforeCursor(128, 0);
        if (before == null || before.length() == 0) return false;
        
        String expr = extractMathExpression(before.toString());
        if (expr == null) return false;
        
        try {
            double res = evaluateMath(expr);
            String resStr = formatMathResult(res);
            conn.deleteSurroundingText(expr.length(), 0);
            conn.commitText(resStr, 1);
            return true;
        } catch (Exception e) {
            // Not a valid math expression, ignore
        }
        return false;
    }

    private boolean handleInlineMathForSimulatedInput() {
        if (mActiveInput == null) return false;
        String text = mActiveInput.getText().toString();
        int selStart = mActiveInput.getSelectionStart();
        int selEnd = mActiveInput.getSelectionEnd();
        if (selStart < 0 || selStart != selEnd) return false;
        
        String before = text.substring(0, selStart);
        String expr = extractMathExpression(before);
        if (expr == null) return false;
        
        try {
            double res = evaluateMath(expr);
            String resStr = formatMathResult(res);
            String after = text.substring(selStart);
            String newBefore = before.substring(0, before.length() - expr.length()) + resStr;
            mActiveInput.setText(newBefore + after);
            mActiveInput.setSelection(newBefore.length());
            return true;
        } catch (Exception e) {
            // ignore
        }
        return false;
    }

    private List<GifSearchEngine.GifItem> getGifHistory() {
        List<GifSearchEngine.GifItem> list = new java.util.ArrayList<>();
        try {
            SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(mContext);
            String jsonStr = prefs.getString("pref_gif_history", "[]");
            org.json.JSONArray array = new org.json.JSONArray(jsonStr);
            for (int i = 0; i < array.length(); i++) {
                org.json.JSONObject obj = array.getJSONObject(i);
                list.add(new GifSearchEngine.GifItem(
                    obj.getString("id"),
                    obj.getString("previewUrl"),
                    obj.getString("fullUrl"),
                    obj.optInt("width", 200),
                    obj.optInt("height", 150)
                ));
            }
        } catch (Exception e) {
            // ignore
        }
        return list;
    }

    private void addToGifHistory(GifSearchEngine.GifItem item) {
        try {
            SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(mContext);
            List<GifSearchEngine.GifItem> current = getGifHistory();
            for (int i = 0; i < current.size(); i++) {
                if (current.get(i).id.equals(item.id)) {
                    current.remove(i);
                    break;
                }
            }
            current.add(0, item);
            while (current.size() > 20) {
                current.remove(current.size() - 1);
            }
            org.json.JSONArray array = new org.json.JSONArray();
            for (GifSearchEngine.GifItem git : current) {
                org.json.JSONObject obj = new org.json.JSONObject();
                obj.put("id", git.id);
                obj.put("previewUrl", git.previewUrl);
                obj.put("fullUrl", git.fullUrl);
                obj.put("width", git.width);
                obj.put("height", git.height);
                array.put(obj);
            }
            prefs.edit().putString("pref_gif_history", array.toString()).apply();
        } catch (Exception e) {
            // ignore
        }
    }

    private void removeFromGifHistory(GifSearchEngine.GifItem item) {
        try {
            SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(mContext);
            List<GifSearchEngine.GifItem> current = getGifHistory();
            boolean removed = false;
            for (int i = 0; i < current.size(); i++) {
                if (current.get(i).id.equals(item.id)) {
                    current.remove(i);
                    removed = true;
                    break;
                }
            }
            if (removed) {
                org.json.JSONArray array = new org.json.JSONArray();
                for (GifSearchEngine.GifItem git : current) {
                    org.json.JSONObject obj = new org.json.JSONObject();
                    obj.put("id", git.id);
                    obj.put("previewUrl", git.previewUrl);
                    obj.put("fullUrl", git.fullUrl);
                    obj.put("width", git.width);
                    obj.put("height", git.height);
                    array.put(obj);
                }
                prefs.edit().putString("pref_gif_history", array.toString()).apply();
                
                // Delete cached file
                File cacheDir = mContext.getCacheDir();
                File gifsDir = new File(cacheDir, "gifs");
                File gifFile = new File(gifsDir, item.id + ".gif");
                if (gifFile.exists()) {
                    gifFile.delete();
                }
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private boolean supportsGifInsertion() {
        if (!(mContext instanceof LatinIME)) return false;
        LatinIME ime = (LatinIME) mContext;
        android.view.inputmethod.EditorInfo editorInfo = ime.getCurrentInputEditorInfo();
        if (editorInfo == null) return false;
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
            String[] mimeTypes = editorInfo.contentMimeTypes;
            if (mimeTypes != null) {
                for (String mime : mimeTypes) {
                    if ("image/gif".equals(mime) || "image/*".equals(mime)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void showGifPanel() {
        hideTranslatePanel();
        hideClipboardSuggestion();
        if (mGifPanel != null && mKeyboardView != null) {
            applyTheming();
            syncWorkspaceHeights(mGifPanel);

            mKeyboardView.setVisibility(View.VISIBLE);
            if (mClipboardPanel != null) mClipboardPanel.setVisibility(View.GONE);
            if (mAiPanel != null) mAiPanel.setVisibility(View.GONE);
            if (mAiSettingsPanel != null) mAiSettingsPanel.setVisibility(View.GONE);
            if (mEmojiPanel != null) mEmojiPanel.setVisibility(View.GONE);
            mGifPanel.setVisibility(View.VISIBLE);

            setActiveInput(mGifSearchInput);
            highlightToolbarTab("gif");

            if (!supportsGifInsertion()) {
                Toast.makeText(mContext, "Note: This app does not support direct GIF insertion", Toast.LENGTH_SHORT).show();
            }

            loadGifs(mGifSearchInput != null ? mGifSearchInput.getText().toString() : "");
            triggerLayoutRequest();
        }
    }

    public void hideGifPanel() {
        if (mGifPanel != null) {
            mGifPanel.setVisibility(View.GONE);
            if (mGifItemsContainer != null) {
                mGifItemsContainer.removeAllViews();
            }
        }
    }

    public void clearGifCache() {
        if (mGifCache != null) {
            mGifCache.evictAll();
        }
    }

    private void loadGifs(final String query) {
        if (mGifItemsContainer == null) return;

        mGifItemsContainer.removeAllViews();
        
        TextView loadingTv = new TextView(mContext);
        loadingTv.setText("Loading GIFs...");
        loadingTv.setTextColor(Color.GRAY);
        loadingTv.setTextSize(14);
        loadingTv.setGravity(Gravity.CENTER);
        loadingTv.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        mGifItemsContainer.addView(loadingTv);

        new Thread(() -> {
            final List<GifSearchEngine.GifItem> results;
            boolean isHistoryMode = (query == null || query.trim().isEmpty());
            
            if (isHistoryMode) {
                results = getGifHistory();
            } else {
                SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(mContext);
                String provider = prefs.getString("pref_gif_provider", "tenor");
                String giphyKey = prefs.getString("pref_giphy_api_key", "");
                String klipyKey = prefs.getString("pref_klipy_api_key", "");
                boolean highQuality = Settings.readGifHighQuality(prefs);
                results = GifSearchEngine.fetchGifs(provider, giphyKey, klipyKey, highQuality, query);
            }

            mSuggestionDismissHandler.post(() -> {
                if (mGifItemsContainer == null) return;
                mGifItemsContainer.removeAllViews();

                if (results.isEmpty()) {
                    TextView emptyTv = new TextView(mContext);
                    if (isHistoryMode) {
                        emptyTv.setText("No sent GIFs history. Type a search query above to find GIFs.");
                    } else {
                        emptyTv.setText("No GIFs found");
                    }
                    emptyTv.setTextColor(Color.GRAY);
                    emptyTv.setTextSize(14);
                    emptyTv.setGravity(Gravity.CENTER);
                    emptyTv.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
                    mGifItemsContainer.addView(emptyTv);
                    return;
                }

                int cols = 2;
                LinearLayout currentRow = null;
                int addedCount = 0;

                File cacheDir = mContext.getCacheDir();
                File gifsDir = new File(cacheDir, "gifs");

                for (final GifSearchEngine.GifItem item : results) {
                    if (addedCount % cols == 0) {
                        currentRow = new LinearLayout(mContext);
                        currentRow.setOrientation(LinearLayout.HORIZONTAL);
                        currentRow.setGravity(Gravity.CENTER_VERTICAL);
                        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        );
                        rowLp.setMargins(0, 0, 0, dpToPx(8));
                        currentRow.setLayoutParams(rowLp);
                        mGifItemsContainer.addView(currentRow);
                    }

                    GifView gifView = new GifView(mContext);
                    float aspect = 1.33f;
                    if (item.height > 0) {
                        aspect = (float) item.width / item.height;
                    }
                    aspect = Math.max(0.5f, Math.min(2.5f, aspect));
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        0,
                        dpToPx(110),
                        aspect
                    );
                    if (addedCount % cols == 0) {
                        lp.setMargins(0, 0, dpToPx(4), 0);
                    } else {
                        lp.setMargins(dpToPx(4), 0, 0, 0);
                    }
                    gifView.setLayoutParams(lp);

                    File localFile = new File(gifsDir, item.id + ".gif");
                    String loadPath = localFile.exists() ? localFile.getAbsolutePath() : item.previewUrl;
                    gifView.loadUrl(loadPath, mGifCache);

                    gifView.setOnClickListener(v -> {
                        if (!supportsGifInsertion()) {
                            Toast.makeText(mContext, "This app does not support direct GIF insertion", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Toast.makeText(mContext, "Sending GIF...", Toast.LENGTH_SHORT).show();
                        shareGif(item);
                    });

                    if (isHistoryMode) {
                        gifView.setOnLongClickListener(v -> {
                            removeFromGifHistory(item);
                            Toast.makeText(mContext, "Removed from history", Toast.LENGTH_SHORT).show();
                            loadGifs("");
                            return true;
                        });
                    }

                    if (currentRow != null) {
                        currentRow.addView(gifView);
                        addedCount++;
                    }
                }
                mSuggestionDismissHandler.postDelayed(() -> updateGifVisibilityInScroll(), 100);
            });
        }).start();
    }

    private void updateGifVisibilityInScroll() {
        if (mGifScrollView == null || mGifItemsContainer == null) return;
        
        android.graphics.Rect scrollBounds = new android.graphics.Rect();
        mGifScrollView.getHitRect(scrollBounds);
        
        for (int i = 0; i < mGifItemsContainer.getChildCount(); i++) {
            View rowView = mGifItemsContainer.getChildAt(i);
            if (rowView instanceof LinearLayout) {
                LinearLayout row = (LinearLayout) rowView;
                for (int j = 0; j < row.getChildCount(); j++) {
                    View child = row.getChildAt(j);
                    if (child instanceof GifView) {
                        GifView gifView = (GifView) child;
                        if (gifView.getLocalVisibleRect(new android.graphics.Rect())) {
                            gifView.resumeAnimation();
                        } else {
                            gifView.pauseAnimation();
                        }
                    }
                }
            }
        }
    }

    private void shareGif(final GifSearchEngine.GifItem item) {
        new Thread(() -> {
            try {
                File cacheDir = mContext.getCacheDir();
                File gifsDir = new File(cacheDir, "gifs");
                if (!gifsDir.exists()) {
                    gifsDir.mkdirs();
                }
                
                final File gifFile = new File(gifsDir, item.id + ".gif");
                
                if (gifFile.exists()) {
                    mSuggestionDismissHandler.post(() -> {
                        commitGifFile(gifFile);
                        addToGifHistory(item);
                    });
                    return;
                }
                
                URL u = new URL(item.fullUrl);
                HttpURLConnection conn = (HttpURLConnection) u.openConnection();
                conn.setConnectTimeout(12000);
                conn.setReadTimeout(12000);
                conn.connect();
                
                if (conn.getResponseCode() == 200) {
                    InputStream in = conn.getInputStream();
                    java.io.FileOutputStream out = new java.io.FileOutputStream(gifFile);
                    byte[] buf = new byte[4096];
                    int len;
                    while ((len = in.read(buf)) != -1) {
                        out.write(buf, 0, len);
                    }
                    in.close();
                    out.close();
                    
                    mSuggestionDismissHandler.post(() -> {
                        commitGifFile(gifFile);
                        addToGifHistory(item);
                    });
                } else {
                    mSuggestionDismissHandler.post(() -> 
                        Toast.makeText(mContext, "Failed to download GIF", Toast.LENGTH_SHORT).show()
                    );
                }
            } catch (Exception e) {
                mSuggestionDismissHandler.post(() -> 
                    Toast.makeText(mContext, "Error sharing GIF: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void commitGifFile(File file) {
        if (!(mContext instanceof LatinIME)) return;
        LatinIME ime = (LatinIME) mContext;
        InputConnection conn = ime.getCurrentInputConnection();
        android.view.inputmethod.EditorInfo editorInfo = ime.getCurrentInputEditorInfo();
        
        if (conn == null || editorInfo == null) return;
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
            if (!supportsGifInsertion()) {
                Toast.makeText(mContext, "App does not support GIF insertion", Toast.LENGTH_SHORT).show();
                return;
            }
            
            android.net.Uri contentUri = android.net.Uri.parse("content://nabu.iris.keyboard.gifprovider/" + file.getName());
            
            android.view.inputmethod.InputContentInfo contentInfo = new android.view.inputmethod.InputContentInfo(
                contentUri,
                new android.content.ClipDescription("GIF", new String[]{"image/gif"}),
                null
            );
            
            try {
                conn.commitContent(
                    contentInfo,
                    InputConnection.INPUT_CONTENT_GRANT_READ_URI_PERMISSION,
                    null
                );
            } catch (Exception e) {
                Toast.makeText(mContext, "Failed to insert GIF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(mContext, "GIF insertion requires Android 7.1.1+", Toast.LENGTH_SHORT).show();
        }
    }

    public void onDestroy() {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null && mPrimaryClipListener != null) {
            clipboard.removePrimaryClipChangedListener(mPrimaryClipListener);
        }
        mSuggestionDismissHandler.removeCallbacks(mSuggestionDismissRunnable);
        if (mGifSearchRunnable != null) {
            mGifSearchHandler.removeCallbacks(mGifSearchRunnable);
        }
        if (mTranslateRunnable != null) {
            mTranslateHandler.removeCallbacks(mTranslateRunnable);
        }
        releaseActiveTranslator();
        try {
            mTranslationExecutor.shutdown();
        } catch (Exception e) {
            // Ignore
        }
        if (mGifScrollView != null && mGifScrollView.getViewTreeObserver().isAlive()) {
            mGifScrollView.getViewTreeObserver().removeOnScrollChangedListener(mGifScrollListener);
        }
        if (mGifItemsContainer != null) {
            mGifItemsContainer.removeAllViews();
        }
    }

    private void showLanguageDialog(final boolean isSource) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(
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
        
        android.app.AlertDialog dialog = builder.create();
        android.view.Window window = dialog.getWindow();
        if (window != null) {
            android.view.WindowManager.LayoutParams lp = window.getAttributes();
            if (mKeyboardView != null) {
                lp.token = mKeyboardView.getWindowToken();
            }
            lp.type = android.view.WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
            window.setAttributes(lp);
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        }
        dialog.show();
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

    private void triggerTranslation() {
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
        mTranslationExecutor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                String encodedText = java.net.URLEncoder.encode(text, "UTF-8");
                String urlStr = "https://translate.google.com/m?sl=" + mTranslateSourceLang + "&tl=" + mTranslateTargetLang + "&q=" + encodedText;
                URL url = new URL(urlStr);
                
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream(), "UTF-8"));
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
                            final String translated = android.text.Html.fromHtml(rawResult).toString();
                            
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
        if ("auto".equals(mTranslateSourceLang)) {
            postTranslationFailure("Auto-detect not supported offline.");
            return;
        }

        final String sourceTag = TranslateLanguage.fromLanguageTag(mTranslateSourceLang);
        final String targetTag = TranslateLanguage.fromLanguageTag(mTranslateTargetLang);

        if (sourceTag == null || targetTag == null) {
            postTranslationFailure("Unsupported offline language pair.");
            return;
        }
        
        try {
            if (mActiveTranslator != null && 
                sourceTag.equals(mActiveTranslatorSource) && 
                targetTag.equals(mActiveTranslatorTarget)) {
                
                performMlKitTranslation(mActiveTranslator, text);
            } else {
                releaseActiveTranslator();
                
                TranslatorOptions options = new TranslatorOptions.Builder()
                        .setSourceLanguage(sourceTag)
                        .setTargetLanguage(targetTag)
                        .build();
                
                final Translator translator = Translation.getClient(options);
                mActiveTranslator = translator;
                mActiveTranslatorSource = sourceTag;
                mActiveTranslatorTarget = targetTag;
                
                DownloadConditions conditions = new DownloadConditions.Builder().build();
                
                mTranslateResultPreview.setText("");
                mTranslateInsertBtn.setVisibility(View.GONE);
                startDownloadProgressAnimation();
                
                translator.downloadModelIfNeeded(conditions)
                        .addOnSuccessListener(unused -> {
                            mTranslateHandler.post(() -> {
                                if (mActiveTranslator == translator) {
                                    stopDownloadProgressAnimation();
                                    performMlKitTranslation(translator, text);
                                }
                            });
                        })
                        .addOnFailureListener(e -> {
                            mTranslateHandler.post(() -> {
                                if (mActiveTranslator == translator) {
                                    stopDownloadProgressAnimation();
                                    postTranslationFailure("Download failed: " + e.getMessage());
                                }
                            });
                        });
            }
        } catch (Exception e) {
            postTranslationFailure("ML Kit Init Error: " + e.getMessage());
        }
    }

    private void performMlKitTranslation(final Translator translator, final String text) {
        mTranslateResultPreview.setText("Translating offline...");
        translator.translate(text)
                .addOnSuccessListener(translatedText -> {
                    mTranslateHandler.post(() -> {
                        if (mActiveTranslator == translator) {
                            showTranslateProgress(false);
                            mTranslateResultPreview.setText(translatedText);
                            mTranslateInsertBtn.setVisibility(View.VISIBLE);
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    mTranslateHandler.post(() -> {
                        if (mActiveTranslator == translator) {
                            postTranslationFailure("Translation failed: " + e.getMessage());
                        }
                    });
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
        
        mAiManager.queryAiWithProvider(targetProvider, prompt, new AiCopilotManager.AiCallback() {
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
        if (mActiveTranslator != null) {
            try {
                mActiveTranslator.close();
            } catch (Exception e) {
                // Ignore
            }
            mActiveTranslator = null;
            mActiveTranslatorSource = null;
            mActiveTranslatorTarget = null;
        }
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
                    // Accelerate quickly at start, slow toward 90%
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
        // Hide after a brief moment
        mTranslateHandler.postDelayed(() -> {
            if (mTranslateDownloadLabel != null) mTranslateDownloadLabel.setVisibility(View.GONE);
            if (mTranslateProgressBar != null) mTranslateProgressBar.setVisibility(View.GONE);
        }, 400);
    }

    private void showTranslateProgress(boolean show) {
        if (mTranslateProgressBar != null) {
            if (show) {
                // For non-download operations, use indeterminate-style full bar
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


}
