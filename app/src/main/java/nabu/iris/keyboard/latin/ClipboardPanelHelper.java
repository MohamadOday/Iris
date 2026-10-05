/*
 * Copyright (C) 2026 Latin IME Customizer
 * Copyright (C) 2026 Iris Keyboard Project
 */

package nabu.iris.keyboard.latin;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import nabu.iris.keyboard.R;
import nabu.iris.keyboard.compat.PreferenceManagerCompat;
import nabu.iris.keyboard.latin.settings.Settings;

/**
 * Helper class to manage the clipboard list display panel, search filtering, tab creation, and swipe actions.
 */
public final class ClipboardPanelHelper {
    private final ClipboardBarController mController;
    private final Context mContext;
    private final LinearLayout mClipboardPanel;
    private final LinearLayout mItemsList;

    private final ImageView mBackBtn;
    private final TextView mTitleText;
    private final TextView mClearAllBtn;
    private final LinearLayout mTabsLayout;
    private final LinearLayout mSearchBox;
    private final EditText mSearchInput;
    private final ImageView mSearchClear;

    private String mSelectedTab = "all";
    private String mSearchQuery = "";

    public ClipboardPanelHelper(ClipboardBarController controller, View inputView) {
        mController = controller;
        mContext = controller.getContext();

        mClipboardPanel = inputView.findViewById(R.id.clipboard_panel);
        mItemsList = inputView.findViewById(R.id.clipboard_items_list);

        mBackBtn = inputView.findViewById(R.id.clipboard_back_btn);
        mTitleText = inputView.findViewById(R.id.clipboard_header_title);
        mClearAllBtn = inputView.findViewById(R.id.clipboard_clear_all_btn);
        mTabsLayout = inputView.findViewById(R.id.clipboard_tabs_layout);
        mSearchBox = inputView.findViewById(R.id.clipboard_search_box);
        mSearchInput = inputView.findViewById(R.id.clipboard_search_input);
        mSearchClear = inputView.findViewById(R.id.clipboard_search_clear);

        setupControls();
    }

    public String getSelectedTab() {
        return mSelectedTab;
    }

    public void setSelectedTab(String tab) {
        mSelectedTab = tab;
    }

    public String getSearchQuery() {
        return mSearchQuery;
    }

    public void setSearchQuery(String query) {
        mSearchQuery = query;
    }

    private void setupControls() {
        if (mBackBtn != null) {
            mBackBtn.setOnClickListener(v -> mController.showKeyboard());
        }

        if (mClearAllBtn != null) {
            mClearAllBtn.setOnClickListener(v -> showClearAllConfirmation());
        }

        if (mSearchInput != null) {
            mController.configureSimulatedInput(mSearchInput);
            mSearchInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    mSearchQuery = s != null ? s.toString().trim() : "";
                    if (mSearchClear != null) {
                        mSearchClear.setVisibility(mSearchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                    refresh();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        if (mSearchClear != null) {
            mSearchClear.setOnClickListener(v -> {
                if (mSearchInput != null) {
                    mSearchInput.setText("");
                }
            });
        }
    }

    private void showClearAllConfirmation() {
        ClipboardHistoryManager manager = mController.getClipboardHistoryManager();
        if (manager == null || manager.getItems().isEmpty()) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("Clear Clipboard History");
        builder.setMessage("Delete all unpinned clipboard items?");
        builder.setPositiveButton("Clear", (dialog, which) -> {
            manager.clearUnpinned();
            refresh();
        });
        builder.setNegativeButton("Cancel", null);

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

    public void applyTheming() {
        int backgroundColor = mController.getKeyboardBackgroundColor();
        boolean isDark = mController.isColorDark(backgroundColor);

        SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(mContext);
        int customColor = Settings.readKeyboardColor(prefs, mContext);
        int accentColor = customColor;
        if (accentColor == 0 || mController.isColorMonochromeOrTooDark(accentColor)) {
            accentColor = mContext.getResources().getColor(R.color.settings_accent);
        }

        int textPrimary = isDark ? 0xFFE6E1E5 : 0xFF1D1B20;
        int textSecondary = isDark ? 0xFFCAC4D0 : 0xFF49454E;
        int searchBg = isDark ? 0x1AFFFFFF : 0x0D000000;
        int searchStroke = isDark ? 0x2EFFFFFF : 0x1F000000;

        if (mClipboardPanel != null) {
            mClipboardPanel.setBackgroundColor(backgroundColor);
        }

        if (mTitleText != null) {
            mTitleText.setTextColor(textPrimary);
        }

        if (mBackBtn != null) {
            mBackBtn.setColorFilter(textPrimary);
            GradientDrawable backBg = new GradientDrawable();
            backBg.setShape(GradientDrawable.OVAL);
            backBg.setColor(Color.TRANSPARENT);
            mBackBtn.setBackground(backBg);
        }

        if (mClearAllBtn != null) {
            mClearAllBtn.setTextColor(accentColor);
            GradientDrawable clearBg = new GradientDrawable();
            clearBg.setShape(GradientDrawable.RECTANGLE);
            clearBg.setCornerRadius(mController.dpToPx(14));
            clearBg.setColor(mController.getTranslucentColor(accentColor, isDark ? 18 : 12));
            clearBg.setStroke(mController.dpToPx(1), mController.getTranslucentColor(accentColor, 40));
            mClearAllBtn.setBackground(clearBg);
        }

        if (mSearchBox != null) {
            GradientDrawable sBg = new GradientDrawable();
            sBg.setShape(GradientDrawable.RECTANGLE);
            sBg.setCornerRadius(mController.dpToPx(16));
            sBg.setColor(searchBg);
            sBg.setStroke(mController.dpToPx(1), searchStroke);
            mSearchBox.setBackground(sBg);
        }

        if (mSearchInput != null) {
            mSearchInput.setTextColor(textPrimary);
            mSearchInput.setHintTextColor(textSecondary);
        }

        ImageView searchIcon = mClipboardPanel != null ? mClipboardPanel.findViewById(R.id.clipboard_search_icon) : null;
        if (searchIcon != null) {
            searchIcon.setColorFilter(textSecondary);
        }

        if (mSearchClear != null) {
            mSearchClear.setColorFilter(textSecondary);
        }

        buildTabs();
    }

    public void buildTabs() {
        if (mTabsLayout == null) return;
        mTabsLayout.removeAllViews();

        SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(mContext);
        int customColor = Settings.readKeyboardColor(prefs, mContext);
        int backgroundColor = mController.getKeyboardBackgroundColor();
        boolean isDark = mController.isColorDark(backgroundColor);

        int normalColor = isDark ? 0xFFCAC4D0 : 0xFF49454E;
        int activeColor = customColor;
        if (activeColor == 0 || mController.isColorMonochromeOrTooDark(activeColor)) {
            activeColor = mContext.getResources().getColor(R.color.settings_accent);
        }

        String[] tabKeys = {"all", "pinned", "links"};
        String[] tabTitles = {"All", "Pinned", "Links"};

        for (int i = 0; i < tabKeys.length; i++) {
            final String key = tabKeys[i];
            TextView tabBtn = new TextView(mContext);
            tabBtn.setText(tabTitles[i]);
            tabBtn.setTextSize(10.5f);
            tabBtn.setGravity(Gravity.CENTER);
            tabBtn.setPadding(mController.dpToPx(10), mController.dpToPx(5), mController.dpToPx(10), mController.dpToPx(5));
            tabBtn.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            btnParams.setMargins(0, 0, mController.dpToPx(6), 0);
            tabBtn.setLayoutParams(btnParams);

            boolean isActive = mSelectedTab.equals(key);
            tabBtn.setTextColor(isActive ? activeColor : normalColor);

            GradientDrawable tabBg = new GradientDrawable();
            tabBg.setShape(GradientDrawable.RECTANGLE);
            tabBg.setCornerRadius(mController.dpToPx(12));
            if (isActive) {
                tabBg.setColor(mController.getTranslucentColor(activeColor, isDark ? 22 : 16));
                tabBg.setStroke(mController.dpToPx(1), activeColor);
            } else {
                tabBg.setColor(isDark ? 0x0FFFFFFF : 0x08000000);
                tabBg.setStroke(mController.dpToPx(1), isDark ? 0x20FFFFFF : 0x15000000);
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

        ClipboardHistoryManager manager = mController.getClipboardHistoryManager();
        if (manager == null) return;
        List<ClipboardHistoryManager.ClipboardItem> allItems = manager.getItems();
        List<ClipboardHistoryManager.ClipboardItem> items = new ArrayList<>();

        for (ClipboardHistoryManager.ClipboardItem item : allItems) {
            if (mSearchQuery != null && !mSearchQuery.isEmpty()) {
                if (!item.text.toLowerCase().contains(mSearchQuery.toLowerCase())) {
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
        int backgroundColor = mController.getKeyboardBackgroundColor();
        boolean isDark = mController.isColorDark(backgroundColor);
        int hintColor = isDark ? 0x88FFFFFF : 0x88000000;

        int accentColor = customColor;
        if (accentColor == 0 || mController.isColorMonochromeOrTooDark(accentColor)) {
            accentColor = mContext.getResources().getColor(R.color.settings_accent);
        }

        if (items.isEmpty()) {
            LinearLayout emptyLayout = new LinearLayout(mContext);
            emptyLayout.setOrientation(LinearLayout.VERTICAL);
            emptyLayout.setGravity(Gravity.CENTER);
            emptyLayout.setPadding(32, mController.dpToPx(28), 32, mController.dpToPx(28));

            TextView emptyView = new TextView(mContext);
            emptyView.setText(mSearchQuery.isEmpty() ? "No clipboard snippets yet." : "No snippets matched search.");
            emptyView.setTextSize(12.5f);
            emptyView.setGravity(Gravity.CENTER);
            emptyView.setTextColor(hintColor);
            emptyLayout.addView(emptyView);

            mItemsList.addView(emptyLayout);
            return;
        }

        int normalOutline = isDark ? 0x22FFFFFF : 0x1A000000;
        int cardFill = isDark ? 0x14FFFFFF : 0x0A000000;

        for (final ClipboardHistoryManager.ClipboardItem item : items) {
            LinearLayout rowLayout = new LinearLayout(mContext);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setGravity(Gravity.CENTER_VERTICAL);
            rowLayout.setPadding(mController.dpToPx(12), mController.dpToPx(8), mController.dpToPx(10), mController.dpToPx(8));

            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setShape(GradientDrawable.RECTANGLE);
            cardBg.setCornerRadius(mController.dpToPx(14));
            if (item.isPinned) {
                cardBg.setColor(mController.getTranslucentColor(accentColor, isDark ? 16 : 10));
                cardBg.setStroke(mController.dpToPx(1), accentColor);
            } else {
                cardBg.setColor(cardFill);
                cardBg.setStroke(mController.dpToPx(1), normalOutline);
            }
            rowLayout.setBackground(cardBg);

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            rowParams.setMargins(0, mController.dpToPx(3), 0, mController.dpToPx(3));
            rowLayout.setLayoutParams(rowParams);

            final TextView clipText = new TextView(mContext);
            clipText.setText(item.text);
            clipText.setTextSize(12.5f);
            clipText.setMaxLines(2);
            clipText.setEllipsize(TextUtils.TruncateAt.END);
            clipText.setTextColor(isDark ? 0xFFE6E1E5 : 0xFF1D1B20);
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

                            if (!isSwiping && Math.abs(diffY) > mController.dpToPx(8) && Math.abs(diffY) > Math.abs(diffX)) {
                                isScrolling = true;
                                return false;
                            }
                            if (Math.abs(diffX) > mController.dpToPx(8)) {
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
                                    manager.deleteItem(item.text);
                                    Vibrator vibrator = mController.getVibrator();
                                    if (vibrator != null) {
                                        try {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
                                            } else {
                                                vibrator.vibrate(30);
                                            }
                                        } catch (Exception e) {}
                                    }
                                    refresh();
                                }).start();
                            } else {
                                v.animate().translationX(0).alpha(1.0f).setDuration(200).start();
                                if (!isSwiping) {
                                    float finalDiffY = event.getY() - startY;
                                    if (Math.abs(finalDiffX) < mController.dpToPx(8) && Math.abs(finalDiffY) < mController.dpToPx(8)) {
                                        ClipboardBarController.OnItemClickListener listener = mController.getOnItemClickListener();
                                        if (listener != null) {
                                            listener.onItemClick(item.text);
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
            rowLayout.addView(spacer, new LinearLayout.LayoutParams(mController.dpToPx(10), 1));

            // Pin button
            final ImageView pinBtn = new ImageView(mContext);
            pinBtn.setImageResource(R.drawable.ic_pin);
            pinBtn.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            pinBtn.setPadding(mController.dpToPx(5), mController.dpToPx(5), mController.dpToPx(5), mController.dpToPx(5));
            pinBtn.setLayoutParams(new LinearLayout.LayoutParams(mController.dpToPx(28), mController.dpToPx(28)));

            GradientDrawable pinBg = new GradientDrawable();
            pinBg.setShape(GradientDrawable.OVAL);
            if (item.isPinned) {
                pinBg.setColor(mController.getTranslucentColor(accentColor, 22));
                pinBg.setStroke(mController.dpToPx(1), accentColor);
                pinBtn.setColorFilter(accentColor);
            } else {
                pinBg.setColor(isDark ? 0x11FFFFFF : 0x08000000);
                pinBg.setStroke(mController.dpToPx(1), isDark ? 0x22FFFFFF : 0x18000000);
                pinBtn.setColorFilter(isDark ? 0x88FFFFFF : 0x88000000);
            }
            pinBtn.setBackground(pinBg);
            pinBtn.setOnClickListener(v -> {
                manager.togglePin(item.text);
                refresh();
            });
            rowLayout.addView(pinBtn);

            View spacer2 = new View(mContext);
            rowLayout.addView(spacer2, new LinearLayout.LayoutParams(mController.dpToPx(6), 1));

            // Delete button
            ImageView delBtn = new ImageView(mContext);
            delBtn.setImageResource(R.drawable.ic_close);
            delBtn.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            delBtn.setPadding(mController.dpToPx(6), mController.dpToPx(6), mController.dpToPx(6), mController.dpToPx(6));
            delBtn.setLayoutParams(new LinearLayout.LayoutParams(mController.dpToPx(28), mController.dpToPx(28)));

            GradientDrawable delBg = new GradientDrawable();
            delBg.setShape(GradientDrawable.OVAL);
            delBg.setColor(isDark ? 0x1AEE5253 : 0x14EE5253);
            delBg.setStroke(mController.dpToPx(1), 0x55EE5253);
            delBtn.setBackground(delBg);
            delBtn.setColorFilter(0xFFEE5253);
            delBtn.setOnClickListener(v -> {
                manager.deleteItem(item.text);
                refresh();
            });
            rowLayout.addView(delBtn);

            mItemsList.addView(rowLayout);
        }
    }
}
