/*
 * Copyright (C) 2026 Latin IME Customizer
 */

package nabu.iris.keyboard.latin;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
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
    
    private LinearLayout mTabsLayout;
    private String mSelectedTab = "all";
    private String mSearchQuery = "";

    public ClipboardPanelHelper(ClipboardBarController controller, View inputView) {
        mController = controller;
        mContext = controller.getContext();
        
        mClipboardPanel = inputView.findViewById(R.id.clipboard_panel);
        mItemsList = inputView.findViewById(R.id.clipboard_items_list);
        
        setupClipboardControls();
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

    private void setupClipboardControls() {
        if (mClipboardPanel == null) return;

        LinearLayout controlsRow = new LinearLayout(mContext);
        controlsRow.setOrientation(LinearLayout.HORIZONTAL);
        controlsRow.setGravity(Gravity.CENTER_VERTICAL);
        
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(mController.dpToPx(8), mController.dpToPx(2), mController.dpToPx(8), mController.dpToPx(2));
        controlsRow.setLayoutParams(rowParams);

        mTabsLayout = new LinearLayout(mContext);
        mTabsLayout.setOrientation(LinearLayout.HORIZONTAL);
        mTabsLayout.setGravity(Gravity.CENTER_VERTICAL);
        
        LinearLayout.LayoutParams tabsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        mTabsLayout.setLayoutParams(tabsParams);
        controlsRow.addView(mTabsLayout);

        mClipboardPanel.addView(controlsRow, 0);
    }

    public void applyTheming() {
        if (mClipboardPanel != null) {
            mClipboardPanel.setBackgroundColor(mController.getKeyboardBackgroundColor());
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

        int normalColor = isDark ? 0x99FFFFFF : 0x88000000;
        int activeColor = customColor;
        if (activeColor == 0 || mController.isColorMonochromeOrTooDark(activeColor)) {
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
            tabBtn.setPadding(mController.dpToPx(8), mController.dpToPx(5), mController.dpToPx(8), mController.dpToPx(5));
            tabBtn.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            btnParams.setMargins(mController.dpToPx(2), 0, mController.dpToPx(2), 0);
            tabBtn.setLayoutParams(btnParams);

            boolean isActive = mSelectedTab.equals(key);
            tabBtn.setTextColor(isActive ? activeColor : normalColor);

            GradientDrawable tabBg = new GradientDrawable();
            tabBg.setShape(GradientDrawable.RECTANGLE);
            tabBg.setCornerRadius(mController.dpToPx(16));
            if (isActive) {
                tabBg.setColor(mController.getTranslucentColor(activeColor, 24));
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
            TextView emptyView = new TextView(mContext);
            emptyView.setText("No clipboard snippets found.");
            emptyView.setTextSize(12);
            emptyView.setGravity(Gravity.CENTER);
            emptyView.setPadding(32, mController.dpToPx(32), 32, mController.dpToPx(32));
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
            rowLayout.setPadding(mController.dpToPx(10), mController.dpToPx(7), mController.dpToPx(10), mController.dpToPx(7));

            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setShape(GradientDrawable.RECTANGLE);
            cardBg.setCornerRadius(mController.dpToPx(10));
            if (item.isPinned) {
                cardBg.setColor(mController.getTranslucentColor(accentColor, 10));
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
            clipText.setTextSize(12);
            clipText.setMaxLines(2);
            clipText.setEllipsize(TextUtils.TruncateAt.END);
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
            rowLayout.addView(spacer, new LinearLayout.LayoutParams(mController.dpToPx(12), 1));

            // Pin button
            final TextView pinBtn = new TextView(mContext);
            pinBtn.setText(item.isPinned ? "★" : "☆");
            pinBtn.setTextSize(11);
            pinBtn.setGravity(Gravity.CENTER);
            pinBtn.setLayoutParams(new LinearLayout.LayoutParams(mController.dpToPx(26), mController.dpToPx(26)));
            
            GradientDrawable pinBg = new GradientDrawable();
            pinBg.setShape(GradientDrawable.OVAL);
            if (item.isPinned) {
                pinBg.setColor(mController.getTranslucentColor(accentColor, 20));
                pinBg.setStroke(mController.dpToPx(1), accentColor);
                pinBtn.setTextColor(accentColor);
            } else {
                pinBg.setColor(isDark ? 0x11FFFFFF : 0x08000000);
                pinBg.setStroke(mController.dpToPx(1), isDark ? 0x22FFFFFF : 0x1A000000);
                pinBtn.setTextColor(isDark ? 0x88FFFFFF : 0x88000000);
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
            TextView delBtn = new TextView(mContext);
            delBtn.setText("✕");
            delBtn.setTextSize(10);
            delBtn.setGravity(Gravity.CENTER);
            delBtn.setLayoutParams(new LinearLayout.LayoutParams(mController.dpToPx(26), mController.dpToPx(26)));
            
            GradientDrawable delBg = new GradientDrawable();
            delBg.setShape(GradientDrawable.OVAL);
            delBg.setColor(isDark ? 0x22FF1744 : 0x15FF1744);
            delBg.setStroke(mController.dpToPx(1), 0xFFFF1744);
            delBtn.setBackground(delBg);
            delBtn.setTextColor(0xFFFF1744);
            delBtn.setOnClickListener(v -> {
                manager.deleteItem(item.text);
                refresh();
            });
            rowLayout.addView(delBtn);

            mItemsList.addView(rowLayout);
        }
    }
}
