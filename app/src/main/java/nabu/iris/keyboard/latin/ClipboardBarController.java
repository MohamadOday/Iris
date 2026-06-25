/*
 * Copyright (C) 2026 Latin IME Customizer
 */

package nabu.iris.keyboard.latin;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Vibrator;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import nabu.iris.keyboard.R;
import nabu.iris.keyboard.compat.PreferenceManagerCompat;
import nabu.iris.keyboard.keyboard.KeyboardTheme;
import nabu.iris.keyboard.latin.settings.Settings;

/**
 * Controller managing the dynamic Utility Toolbar switcher and delegating specific overlays to sub-helpers.
 */
public final class ClipboardBarController {
    private static final String TAG = "ClipboardBarController";
    
    private final Context mContext;
    private final ClipboardHistoryManager mManager;
    private final OnItemClickListener mListener;
    private final Vibrator mVibrator;
    private final AiCopilotManager mAiManager;

    // View References
    private final View mKeyboardView;
    private final LinearLayout mUtilityToolbar;
    private final LinearLayout mClipboardPanel;
    private final LinearLayout mAiPanel;
    private final LinearLayout mAiSettingsPanel;
    private final View mEmojiPanel;
    private final View mGifPanel;
    private final LinearLayout mTranslatePanel;

    // Toolbar Buttons (ImageView Vectors)
    private final ImageView mTbKeysBtn;
    private final ImageView mTbClipboardBtn;
    private final ImageView mTbAiBtn;
    private final ImageView mTbEmojiBtn;
    private final ImageView mTbGifBtn;
    private final ImageView mTbTranslateBtn;
    private final ImageView mTbSettingsBtn;
    
    // In-console AI config trigger (tune icon inside AI panel header)
    private final ImageView mAiOpenSettingsBtn;

    // Sub-panel helpers
    private final ClipboardSuggestionHelper mSuggestionHelper;
    private final ClipboardPanelHelper mClipboardPanelHelper;
    private final AiPanelHelper mAiPanelHelper;
    private final AiSettingsHelper mAiSettingsHelper;
    private final EmojiPanelHelper mEmojiPanelHelper;
    private final GifPanelHelper mGifPanelHelper;
    private final TranslationPanelHelper mTranslationPanelHelper;

    private EditText mActiveInput = null;

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
        mAiPanel = inputView.findViewById(R.id.ai_panel);
        mAiSettingsPanel = inputView.findViewById(R.id.ai_settings_panel);
        mEmojiPanel = inputView.findViewById(R.id.emoji_panel);
        mGifPanel = inputView.findViewById(R.id.gif_panel);
        mTranslatePanel = inputView.findViewById(R.id.translate_panel);

        // Resolve Toolbar Button Views
        mTbKeysBtn = inputView.findViewById(R.id.tb_keys_btn);
        mTbClipboardBtn = inputView.findViewById(R.id.tb_clipboard_btn);
        mTbAiBtn = inputView.findViewById(R.id.tb_ai_btn);
        mTbEmojiBtn = inputView.findViewById(R.id.tb_emoji_btn);
        mTbGifBtn = inputView.findViewById(R.id.tb_gif_btn);
        mTbTranslateBtn = inputView.findViewById(R.id.tb_translate_btn);
        mTbSettingsBtn = inputView.findViewById(R.id.tb_settings_btn);
        mAiOpenSettingsBtn = inputView.findViewById(R.id.ai_open_settings_btn);

        // Initialize helper delegates
        mSuggestionHelper = new ClipboardSuggestionHelper(this, inputView);
        mClipboardPanelHelper = new ClipboardPanelHelper(this, inputView);
        mAiPanelHelper = new AiPanelHelper(this, inputView);
        mAiSettingsHelper = new AiSettingsHelper(this, inputView);
        mEmojiPanelHelper = new EmojiPanelHelper(this, inputView);
        mGifPanelHelper = new GifPanelHelper(this, inputView);
        mTranslationPanelHelper = new TranslationPanelHelper(this, inputView);

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
                if (mAiPanelHelper != null) {
                    mAiPanelHelper.performAutoGrammarCorrection();
                }
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
        if (mAiOpenSettingsBtn != null) {
            mAiOpenSettingsBtn.setOnClickListener(v -> showAiSettings());
        }

        updateToolbarLayout();
        showKeyboard();
    }

    // Accessors for helper delegates
    Context getContext() { return mContext; }
    ClipboardHistoryManager getClipboardHistoryManager() { return mManager; }
    OnItemClickListener getOnItemClickListener() { return mListener; }
    Vibrator getVibrator() { return mVibrator; }
    AiCopilotManager getAiManager() { return mAiManager; }
    View getKeyboardView() { return mKeyboardView; }
    EditText getActiveInput() { return mActiveInput; }
    
    void setActiveInput(EditText et) {
        if (mActiveInput == et) return;
        EditText prev = mActiveInput;
        if (prev != null) {
            prev.setCursorVisible(false);
            styleConfigField(prev, false);
        }
        mActiveInput = et;
        if (mActiveInput != null) {
            styleConfigField(mActiveInput, true);
            mActiveInput.requestFocus();
            mActiveInput.setCursorVisible(true);
        }
    }

    public static boolean handleInlineMath(InputConnection conn) {
        return InlineMathSolver.handleInlineMath(conn);
    }

    public String getMostRecentClipboardText() {
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

    public void showKeyboard() {
        if (mTranslationPanelHelper != null) mTranslationPanelHelper.hideTranslatePanel();
        applyTheming();
        if (mKeyboardView != null) mKeyboardView.setVisibility(View.VISIBLE);
        if (mClipboardPanel != null) mClipboardPanel.setVisibility(View.GONE);
        if (mAiPanel != null) mAiPanel.setVisibility(View.GONE);
        if (mAiSettingsPanel != null) mAiSettingsPanel.setVisibility(View.GONE);
        if (mEmojiPanel != null) mEmojiPanel.setVisibility(View.GONE);
        if (mGifPanelHelper != null) mGifPanelHelper.hideGifPanel();
        setActiveInput(null);
        highlightToolbarTab("keys");
        checkAndShowClipboardSuggestion();
        triggerLayoutRequest();
    }

    public void showClipboard() {
        if (mTranslationPanelHelper != null) mTranslationPanelHelper.hideTranslatePanel();
        hideClipboardSuggestion();
        if (mClipboardPanel != null && mKeyboardView != null) {
            applyTheming();
            syncWorkspaceHeights(mClipboardPanel);
            
            mKeyboardView.setVisibility(View.GONE);
            if (mAiPanel != null) mAiPanel.setVisibility(View.GONE);
            if (mAiSettingsPanel != null) mAiSettingsPanel.setVisibility(View.GONE);
            if (mEmojiPanel != null) mEmojiPanel.setVisibility(View.GONE);
            if (mGifPanelHelper != null) mGifPanelHelper.hideGifPanel();
            mClipboardPanel.setVisibility(View.VISIBLE);
            
            setActiveInput(null);
            highlightToolbarTab("clipboard");
            refresh();
            triggerLayoutRequest();
        }
    }

    public void showAiCopilot() {
        if (mTranslationPanelHelper != null) mTranslationPanelHelper.hideTranslatePanel();
        hideClipboardSuggestion();
        if (mAiPanel != null && mKeyboardView != null) {
            applyTheming();
            syncWorkspaceHeights(mAiPanel);

            mKeyboardView.setVisibility(View.VISIBLE);
            if (mClipboardPanel != null) mClipboardPanel.setVisibility(View.GONE);
            if (mAiSettingsPanel != null) mAiSettingsPanel.setVisibility(View.GONE);
            if (mEmojiPanel != null) mEmojiPanel.setVisibility(View.GONE);
            if (mGifPanelHelper != null) mGifPanelHelper.hideGifPanel();
            mAiPanel.setVisibility(View.VISIBLE);

            if (mAiPanelHelper != null) {
                setActiveInput(mAiPanelHelper.getAiPromptInput());
            }
            highlightToolbarTab("ai");
            triggerLayoutRequest();
        }
    }

    public void showAiSettings() {
        if (mTranslationPanelHelper != null) mTranslationPanelHelper.hideTranslatePanel();
        hideClipboardSuggestion();
        if (mAiSettingsPanel != null && mKeyboardView != null) {
            applyTheming();
            syncWorkspaceHeights(mAiSettingsPanel);

            mKeyboardView.setVisibility(View.VISIBLE);
            if (mClipboardPanel != null) mClipboardPanel.setVisibility(View.GONE);
            if (mAiPanel != null) mAiPanel.setVisibility(View.GONE);
            if (mEmojiPanel != null) mEmojiPanel.setVisibility(View.GONE);
            if (mGifPanelHelper != null) mGifPanelHelper.hideGifPanel();
            mAiSettingsPanel.setVisibility(View.VISIBLE);

            if (mAiSettingsHelper != null) {
                mAiSettingsHelper.showAiSettings();
            }

            highlightToolbarTab("ai");
            triggerLayoutRequest();
        }
    }

    public void showEmojiPanel() {
        if (mTranslationPanelHelper != null) mTranslationPanelHelper.hideTranslatePanel();
        hideClipboardSuggestion();
        if (mEmojiPanel != null && mKeyboardView != null) {
            applyTheming();
            syncWorkspaceHeights(mEmojiPanel);

            mKeyboardView.setVisibility(View.GONE);
            if (mClipboardPanel != null) mClipboardPanel.setVisibility(View.GONE);
            if (mAiPanel != null) mAiPanel.setVisibility(View.GONE);
            if (mAiSettingsPanel != null) mAiSettingsPanel.setVisibility(View.GONE);
            if (mGifPanelHelper != null) mGifPanelHelper.hideGifPanel();
            mEmojiPanel.setVisibility(View.VISIBLE);

            setActiveInput(null);
            highlightToolbarTab("emoji");
            if (mEmojiPanelHelper != null) {
                mEmojiPanelHelper.setupEmojiPanel();
            }
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
            if (mGifPanelHelper != null) mGifPanelHelper.hideGifPanel();
            
            if (mTranslationPanelHelper != null) {
                mTranslationPanelHelper.showTranslatePanel();
            }
            highlightToolbarTab("translate");
            triggerLayoutRequest();
        }
    }

    public void showGifPanel() {
        if (mTranslationPanelHelper != null) mTranslationPanelHelper.hideTranslatePanel();
        hideClipboardSuggestion();
        if (mGifPanel != null && mKeyboardView != null) {
            applyTheming();
            syncWorkspaceHeights(mGifPanel);

            mKeyboardView.setVisibility(View.VISIBLE);
            if (mClipboardPanel != null) mClipboardPanel.setVisibility(View.GONE);
            if (mAiPanel != null) mAiPanel.setVisibility(View.GONE);
            if (mAiSettingsPanel != null) mAiSettingsPanel.setVisibility(View.GONE);
            if (mEmojiPanel != null) mEmojiPanel.setVisibility(View.GONE);
            
            if (mGifPanelHelper != null) {
                mGifPanelHelper.showGifPanel();
            }
            highlightToolbarTab("gif");
            triggerLayoutRequest();
        }
    }

    public void hide() {
        showKeyboard();
    }

    public void show() {
        showClipboard();
    }

    public void refresh() {
        if (mClipboardPanelHelper != null) {
            mClipboardPanelHelper.refresh();
        }
    }

    public void clearGifCache() {
        if (mGifPanelHelper != null) {
            mGifPanelHelper.clearGifCache();
        }
    }

    public void checkAndShowClipboardSuggestion() {
        if (mSuggestionHelper != null) {
            mSuggestionHelper.checkAndShowClipboardSuggestion();
        }
    }

    public void hideClipboardSuggestion() {
        if (mSuggestionHelper != null) {
            mSuggestionHelper.hideClipboardSuggestion();
        }
    }

    public boolean isKeyboardReplacingPanelOpen() {
        return (mClipboardPanel != null && mClipboardPanel.getVisibility() == View.VISIBLE)
            || (mEmojiPanel != null && mEmojiPanel.getVisibility() == View.VISIBLE);
    }

    public boolean isShown() {
        return (mClipboardPanel != null && mClipboardPanel.getVisibility() == View.VISIBLE)
            || (mAiPanel != null && mAiPanel.getVisibility() == View.VISIBLE)
            || (mAiSettingsPanel != null && mAiSettingsPanel.getVisibility() == View.VISIBLE)
            || (mEmojiPanel != null && mEmojiPanel.getVisibility() == View.VISIBLE)
            || (mGifPanel != null && mGifPanel.getVisibility() == View.VISIBLE)
            || (mTranslatePanel != null && mTranslatePanel.getVisibility() == View.VISIBLE);
    }

    public void onDestroy() {
        if (mSuggestionHelper != null) mSuggestionHelper.onDestroy();
        if (mGifPanelHelper != null) mGifPanelHelper.onDestroy();
        if (mTranslationPanelHelper != null) mTranslationPanelHelper.onDestroy();
    }

    private void launchSettings() {
        if (mContext instanceof LatinIME) {
            ((LatinIME) mContext).launchSettings();
        }
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

    void triggerLayoutRequest() {
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

        view.setColorFilter(isActive ? accentColor : normalColor);

        GradientDrawable badge = new GradientDrawable();
        badge.setShape(GradientDrawable.RECTANGLE);
        badge.setCornerRadius(dpToPx(16));
        if (isActive) {
            badge.setColor(getTranslucentColor(accentColor, 24));
        } else {
            badge.setColor(Color.TRANSPARENT);
        }
        view.setBackground(badge);
    }

    private void updateToolbarLayout() {
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

        if (mClipboardPanelHelper != null) mClipboardPanelHelper.applyTheming();
        if (mAiPanelHelper != null) mAiPanelHelper.applyTheming(accentColor, isDark);
        if (mAiSettingsHelper != null) mAiSettingsHelper.applyTheming(accentColor, isDark, textColor, hintColor);
        if (mTranslationPanelHelper != null) mTranslationPanelHelper.applyTheming(accentColor, isDark, textColor, hintColor);
        if (mSuggestionHelper != null) mSuggestionHelper.applyTheming(accentColor, isDark);
    }

    public boolean onCodeInput(final int codePoint) {
        if (mActiveInput == null || !isShown()) {
            return false;
        }
        if (!isViewOrParentVisible(mActiveInput)) {
            return false;
        }

        if (codePoint == '=') {
            if (InlineMathSolver.handleInlineMathForSimulatedInput(mActiveInput)) {
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
                } else {
                    mActiveInput.setText(text.substring(0, text.length() - 1));
                    mActiveInput.setSelection(mActiveInput.getText().length());
                }
            }
            return true;
        }

        if (codePoint == '\n') { // Enter
            if (mAiPanelHelper != null && mActiveInput == mAiPanelHelper.getAiPromptInput()) {
                mAiPanelHelper.runAiPrompt(mAiPanelHelper.getAiPromptInput().getText().toString());
                mAiPanelHelper.getAiPromptInput().setText("");
            } else if (mGifPanelHelper != null && mActiveInput == mGifPanelHelper.getGifSearchInput()) {
                mGifPanelHelper.showGifPanel();
            } else if (mTranslationPanelHelper != null && mActiveInput == mTranslationPanelHelper.getTranslateInput()) {
                mTranslationPanelHelper.triggerTranslation();
            } else if (mAiSettingsHelper != null && (mActiveInput == mAiSettingsHelper.getSetupGeminiKey()
                    || mActiveInput == mAiSettingsHelper.getSetupHostUrl()
                    || mActiveInput == mAiSettingsHelper.getSetupModelName()
                    || mActiveInput == mAiSettingsHelper.getSetupHeadersJson())) {
                if (mAiSettingsHelper.getSetupSaveBtn() != null && mAiSettingsHelper.getSetupSaveBtn().isEnabled()) {
                    mAiSettingsHelper.getSetupSaveBtn().performClick();
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

    // Helper utilities for sub-controllers
    int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                mContext.getResources().getDisplayMetrics()
        );
    }

    int dpToPx(float dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                mContext.getResources().getDisplayMetrics()
        );
    }

    int getTranslucentColor(int color, int alphaPercent) {
        int alpha = (int) (255 * (alphaPercent / 100.0));
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    boolean isColorMonochromeOrTooDark(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        return hsv[1] < 0.15f || hsv[2] < 0.20f;
    }

    int getKeyboardBackgroundColor() {
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

    boolean isColorDark(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255.0;
        return darkness >= 0.5;
    }

    boolean isViewOrParentVisible(View view) {
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

    void styleConfigField(EditText et, boolean isActive) {
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

    void styleToolChip(TextView v, int accentColor, boolean isDark) {
        if (v == null) return;
        v.setTextSize(10.0f);
        v.setTextColor(accentColor);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dpToPx(16));
        bg.setColor(getTranslucentColor(accentColor, 12));
        v.setBackground(bg);
    }

    void configureSimulatedInput(EditText et) {
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
}
