/*
 * Copyright (C) 2026 Latin IME Customizer
 */

package nabu.iris.keyboard.latin;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.TextView;
import android.widget.Toast;

import nabu.iris.keyboard.R;
import nabu.iris.keyboard.compat.PreferenceManagerCompat;

/**
 * Helper class to manage the clipboard paste suggestions overlay bar and automatic dismiss timers.
 */
public final class ClipboardSuggestionHelper {
    private final ClipboardBarController mController;
    private final Context mContext;
    private final View mClipboardSuggestionBar;
    private final TextView mClipboardSuggestionChip;

    private static String sDismissedText = "";
    private static String sLastShownClipText = null;
    private static long sClipShowStartTime = 0L;

    private final Handler mSuggestionDismissHandler = new Handler(Looper.getMainLooper());
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

    public ClipboardSuggestionHelper(ClipboardBarController controller, View inputView) {
        mController = controller;
        mContext = controller.getContext();

        mClipboardSuggestionBar = inputView.findViewById(R.id.clipboard_suggestion_bar);
        mClipboardSuggestionChip = inputView.findViewById(R.id.clipboard_suggestion_chip);

        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.addPrimaryClipChangedListener(mPrimaryClipListener);
        }
    }

    public void hideClipboardSuggestion() {
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

        final String clipText = mController.getMostRecentClipboardText();
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
                Vibrator vibrator = mController.getVibrator();
                if (vibrator != null) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                        } else {
                            vibrator.vibrate(50);
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

    public void applyTheming(int accentColor, boolean isDark) {
        if (mClipboardSuggestionChip != null) {
            GradientDrawable csBg = new GradientDrawable();
            csBg.setShape(GradientDrawable.RECTANGLE);
            csBg.setCornerRadius(mController.dpToPx(16));
            csBg.setColor(isDark ? 0x22FFFFFF : 0x1A000000);
            csBg.setStroke(mController.dpToPx(1.5f), accentColor);
            mClipboardSuggestionChip.setBackground(csBg);
            mClipboardSuggestionChip.setTextColor(isDark ? 0xFFEEEEEE : 0xFF222222);
        }
    }

    public void onDestroy() {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null && mPrimaryClipListener != null) {
            clipboard.removePrimaryClipChangedListener(mPrimaryClipListener);
        }
        mSuggestionDismissHandler.removeCallbacks(mSuggestionDismissRunnable);
    }
}
