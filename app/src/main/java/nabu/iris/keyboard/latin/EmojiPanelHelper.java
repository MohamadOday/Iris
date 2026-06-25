/*
 * Copyright (C) 2026 Latin IME Customizer
 */

package nabu.iris.keyboard.latin;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputConnection;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import nabu.iris.keyboard.R;
import nabu.iris.keyboard.latin.settings.Settings;
import nabu.iris.keyboard.latin.settings.SettingsValues;

/**
 * Helper class to manage the Emoji list display grid, repeat click action listeners, and committed outputs.
 */
public final class EmojiPanelHelper {
    private final ClipboardBarController mController;
    private final Context mContext;
    private final View mEmojiPanel;
    private final LinearLayout mEmojiItemsContainer;
    private final ImageView mEmojiDeleteBtn;

    public EmojiPanelHelper(ClipboardBarController controller, View inputView) {
        mController = controller;
        mContext = controller.getContext();

        mEmojiPanel = inputView.findViewById(R.id.emoji_panel);
        mEmojiItemsContainer = inputView.findViewById(R.id.emoji_items_container);
        mEmojiDeleteBtn = inputView.findViewById(R.id.emoji_delete_btn);

        setupDeleteButton();
    }

    public void setupEmojiPanel() {
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
                mController.dpToPx(48),
                1.0f
            );
            emojiTv.setLayoutParams(lp);
            emojiTv.setPadding(mController.dpToPx(4), mController.dpToPx(4), mController.dpToPx(4), mController.dpToPx(4));

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
                    mController.dpToPx(48),
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
            feedback.performAudioFeedback(0);
            feedback.performHapticFeedback(mEmojiPanel);
        }
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
}
