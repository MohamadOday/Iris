/*
 * Copyright (C) 2026 Latin IME Customizer
 */

package nabu.iris.keyboard.latin;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputConnection;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import nabu.iris.keyboard.R;
import nabu.iris.keyboard.latin.settings.Settings;
import nabu.iris.keyboard.latin.settings.SettingsValues;

/**
 * Helper class to manage the Material 3 Emoji categorised display grid, repeat click action listeners, and committed outputs.
 */
public final class EmojiPanelHelper {
    private final ClipboardBarController mController;
    private final Context mContext;
    private final View mEmojiPanel;
    private final ScrollView mEmojiScrollView;
    private final LinearLayout mEmojiItemsContainer;
    private final ImageView mEmojiDeleteBtn;

    private final TextView mCatSticky;
    private final TextView mCatSmileys;
    private final TextView mCatPeople;
    private final TextView mCatAnimals;
    private final TextView mCatFood;
    private final TextView mCatActivities;
    private final TextView mCatObjects;
    private final TextView mCatSymbols;

    private String mCurrentCategory = "sticky";

    private static final String EMOJIS_STICKY_DEFAULT = "❤️,😂,🔥,👍,😊,✨,🙏,🎉,😍,🥺,👏,💯,🤣,😎,🥳,🥰,🤔,💀,🙌,😭,🚀";
    private static final String EMOJIS_SMILEYS = "😀,😁,😂,🤣,😃,😄,😅,😆,😉,😊,😋,😎,😍,😘,🥰,😗,😙,😚,🙂,🤗,🤩,🤔,🤨,😐,😑,😶,🙄,😏,😣,😥,😮,🤐,😯,😪,😫,🥱,😴,😌,😛,😜,🤪,😝,🤤,😒,😓,😔,😕,🙃,🫠,🤑,😲,☹️,🙁,😖,😞,😟,😤,😢,😭,😦,😧,😨,😩,🤯,😬,😮‍💨,😰,😱,🥵,🥶,😳,😵,😵‍💫,🥴,😠,😡,🤬,😷,🤒,🤕,🤢,🤮,🤧,😇,🥳,🥸,🥺,🥹,🤠,🤡,👻,💀,☠️,👽,👾,🤖,🎃";
    private static final String EMOJIS_PEOPLE = "👍,👎,👏,🙌,👐,🤲,🤝,🙏,✌️,🤞,🤟,🤘,🤙,👈,👉,👆,🖕,👇,☝️,🖐️,✋,🖖,👋,✍️,💪,🦾,🦿,🦵,🦶,👂,🦻,👃,🧠,🫀,🫁,🦷,🦴,👀,👁️,👅,👄,💋,🫂,👶,👧,🧒,👦,👩,🧑,👨,👩‍🦱,🧑‍🦱,👨‍🦱,👩‍🦰,🧑‍🦰,👨‍🦰,👱‍♀️,👱,👱‍♂️,👩‍🦳,🧑‍🦳,👨‍🦳,👩‍🦲,🧑‍🦲,👨‍🦲,🧔‍♀️,🧔,🧔‍♂️";
    private static final String EMOJIS_ANIMALS = "🐶,🐱,🐭,🐹,🐰,🦊,🐻,🐼,🐻‍❄️,🐨,🐯,🦁,🐮,🐷,🐽,🐸,🐵,🙈,🙉,🙊,🐒,🐔,🐧,🐦,🐤,🐣,🐥,🦆,🦅,🦉,🦇,🐺,🐗,🐴,🦄,🐝,🪱,🐛,🦋,🐌,🐞,🐜,🪰,🪲,🪳,🦟,🦗,🕷️,🕸️,🦂,🐢,🐍,🦎,🦖,🦕,🐙,🦑,🦐,🦞,🦀,🐡,🐠,🐟,🐬,🐳,🐋,🦈,🦭,🐊,🐅,🐆,🦓,🦍,🦧,🦣,🐘,🦛,🦏,🐪,🐫,🦒,🦘,🦬,🐃,🐂,🐄,🐎,🐖,🐏,🐑,🦙,🐐,🦌,🐕,🐩,🦮,🐕‍🦺,🐈,🐈‍⬛,🐓,🦃,🦚,🦜,🦢,🦩,🕊️,🐇,🦝,🦨,🦡,🦫,🦦,🦥,🐁,🐀,🐿️,🦔,🌲,🌳,🌴,🌵,🌾,🌿,☘️,🍀,🍁,🍂,🍃,🍄,🌰,🐚,🪨";
    private static final String EMOJIS_FOOD = "🍏,🍎,🍐,🍊,🍋,🍌,🍉,🍇,🍓,🫐,🍈,🍒,🍑,🥭,🍍,🥥,🥝,🍅,🍆,🥑,🥦,🥬,🥒,🌶️,🫑,🌽,🥕,🫒,🧄,🧅,🥔,🍠,🥐,🥯,🍞,🥖,🥨,🧀,🥚,🍳,🧈,🥞,🧇,🥓,🥩,🍗,🍖,🌭,🍔,🍟,🍕,🫓,🥪,🥙,🧆,🌮,🌯,🫔,🥗,🥘,🫕,🥫,🍝,🍜,🍲,🍛,🍣,🍱,🥟,🦪,🍤,🍙,🍚,🍘,🍥,🥠,🥮,🍢,🍡,🍧,🍨,🍦,🥧,🧁,🍰,🎂,🍮,🍭,🍬,🍫,🍿,🍩,🍪,🥜,🍯,🥛,🍼,☕,🫖,🍵,🍶,🍾,🍷,🍸,🍹,🍺,🍻,🥂,🥃,🧃,🧉,🧊";
    private static final String EMOJIS_ACTIVITIES = "⚽,🏀,🏈,⚾,🥎,🎾,🏐,🏉,🥏,🎱,🪀,🏓,🏸,🏒,🏑,🥍,🏏,🪃,🥅,⛳,🪁,🏹,🎣,🤿,🥊,🥋,🎽,🛹,🛼,🛷,⛸️,🥌,🎿,⛷️,🏂,🪂,🏋️‍♀️,🏋️,🏋️‍♂️,🤼‍♀️,🤼,🤼‍♂️,🤸‍♀️,🤸,🤸‍♂️,⛹️‍♀️,⛹️,⛹️‍♂️,🤺,🤾‍♀️,🤾,🤾‍♂️,🏌️‍♀️,🏌️,🏌️‍♂️,🏇,🧘‍♀️,🧘,🧘‍♂️,🏄‍♀️,🏄,🏄‍♂️,🏊‍♀️,🏊,🏊‍♂️,🤽‍♀️,🤽,🤽‍♂️,🚣‍♀️,🚣,🚣‍♂️,🧗‍♀️,🧗,🧗‍♂️,🚵‍♀️,🚵,🚵‍♂️,🚴‍♀️,🚴,🚴‍♂️,🏆,🥇,🥈,🥉,🏅,🎖️,🏵️,🎗️,🎫,🎟️,🎪,🤹,🎭,🩰,🎨,🎬,🎤,🎧,🎼,🎹,🥁,🪘,🎷,🎺,🪗,🎸,🪕,🎻,🎲,♟️,🎯,🎳,🎮,🎰,🧩";
    private static final String EMOJIS_OBJECTS = "💡,🔦,🕯️,📱,💻,⌨️,🖥️,🖨️,🖱️,🔋,🔌,💸,💵,🪙,💎,🔥,✨,🌟,⭐,🌈,⚡,❄️,☀️,🌙,⌚,📷,📸,📹,📼,🔍,🔎,🔮,🧿,🪬,📿,💈,⚗️,🔭,🔬,🕳️,🩹,🩺,💊,💉,🩸,🧬,🚪,🛗,🪞,🪟,🛏️,🛋️,🪑,🚽,🪠,🚿,🛁,🪤,🪒,🧴,🧷,🧹,🧺,🧻,🪣,🧼,🪥,🧽,🧯,🛒,🚬,⚰️,🪦,⚱️";
    private static final String EMOJIS_SYMBOLS = "❤️,🧡,💛,💚,💙,💜,🖤,🤍,🤎,💔,❤️‍🔥,❤️‍🩹,❣️,💕,💞,💓,💗,💖,💘,💝,💟,☮️,✝️,☪️,🕉️,☸️,✡️,🔯,🕎,☯️,☦️,🛐,⛎,♈,♉,♊,♋,♌,♍,♎,♏,♐,♑,♒,♓,🆔,⚛️,🈳,🈹,☢️,☣️,📴,📳,🈶,🈚,🈸,🈺,🈷️,✴️,VS,💮,🉐,㊙️,祝,㊗️,🈴,🈵,🈲,🅰️,🅱️,🆎,🆑,🅾️,🆘,❌,⭕,🛑,⛔,📛,🚫,💯,💢,♨️,🚷,🚯,🚳,🚱,🔞,📵,🚭,❗,❕,❓,❔,‼️,⁉️,🔅,🔆,〽️,⚠️,🚸,🔱,⚜️,🔰,♻️,✅,🈯,💹,❇️,✳️,❎,🌐,💠,Ⓜ️,🩵,🩶,🩷";

    public EmojiPanelHelper(ClipboardBarController controller, View inputView) {
        mController = controller;
        mContext = controller.getContext();

        mEmojiPanel = inputView.findViewById(R.id.emoji_panel);
        mEmojiScrollView = inputView.findViewById(R.id.emoji_scroll_view);
        mEmojiItemsContainer = inputView.findViewById(R.id.emoji_items_container);
        mEmojiDeleteBtn = inputView.findViewById(R.id.emoji_delete_btn);

        mCatSticky = inputView.findViewById(R.id.emoji_cat_sticky);
        mCatSmileys = inputView.findViewById(R.id.emoji_cat_smileys);
        mCatPeople = inputView.findViewById(R.id.emoji_cat_people);
        mCatAnimals = inputView.findViewById(R.id.emoji_cat_animals);
        mCatFood = inputView.findViewById(R.id.emoji_cat_food);
        mCatActivities = inputView.findViewById(R.id.emoji_cat_activities);
        mCatObjects = inputView.findViewById(R.id.emoji_cat_objects);
        mCatSymbols = inputView.findViewById(R.id.emoji_cat_symbols);

        setupCategoryActions();
        setupDeleteButton();
    }

    private void setupCategoryActions() {
        if (mCatSticky != null) mCatSticky.setOnClickListener(v -> switchCategory("sticky"));
        if (mCatSmileys != null) mCatSmileys.setOnClickListener(v -> switchCategory("smileys"));
        if (mCatPeople != null) mCatPeople.setOnClickListener(v -> switchCategory("people"));
        if (mCatAnimals != null) mCatAnimals.setOnClickListener(v -> switchCategory("animals"));
        if (mCatFood != null) mCatFood.setOnClickListener(v -> switchCategory("food"));
        if (mCatActivities != null) mCatActivities.setOnClickListener(v -> switchCategory("activities"));
        if (mCatObjects != null) mCatObjects.setOnClickListener(v -> switchCategory("objects"));
        if (mCatSymbols != null) mCatSymbols.setOnClickListener(v -> switchCategory("symbols"));
    }

    public void switchCategory(String category) {
        mCurrentCategory = category;
        setupEmojiPanel();
        mController.applyTheming();
        if (mEmojiScrollView != null) {
            mEmojiScrollView.scrollTo(0, 0);
        }
    }

    public void setupEmojiPanel() {
        if (mEmojiItemsContainer == null) return;
        mEmojiItemsContainer.removeAllViews();

        String emojiListStr;
        switch (mCurrentCategory) {
            case "sticky":
                final SettingsValues settingsValues = Settings.getInstance().getCurrent();
                if (settingsValues != null && settingsValues.mEmojiList != null && !settingsValues.mEmojiList.trim().isEmpty()) {
                    emojiListStr = settingsValues.mEmojiList;
                } else {
                    emojiListStr = EMOJIS_STICKY_DEFAULT;
                }
                break;
            case "people":
                emojiListStr = EMOJIS_PEOPLE;
                break;
            case "animals":
                emojiListStr = EMOJIS_ANIMALS;
                break;
            case "food":
                emojiListStr = EMOJIS_FOOD;
                break;
            case "activities":
                emojiListStr = EMOJIS_ACTIVITIES;
                break;
            case "objects":
                emojiListStr = EMOJIS_OBJECTS;
                break;
            case "symbols":
                emojiListStr = EMOJIS_SYMBOLS;
                break;
            case "smileys":
            default:
                emojiListStr = EMOJIS_SMILEYS;
                break;
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
            emojiTv.setTextSize(24);
            emojiTv.setGravity(Gravity.CENTER);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0,
                mController.dpToPx(44),
                1.0f
            );
            emojiTv.setLayoutParams(lp);
            emojiTv.setPadding(mController.dpToPx(2), mController.dpToPx(2), mController.dpToPx(2), mController.dpToPx(2));

            TypedValue outValue = new TypedValue();
            mContext.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true);
            emojiTv.setBackgroundResource(outValue.resourceId);

            final int touchSlop = android.view.ViewConfiguration.get(mContext).getScaledTouchSlop();
            emojiTv.setOnTouchListener(new View.OnTouchListener() {
                private android.os.Handler handler;
                private Runnable runnable;
                private float startX;
                private float startY;
                private boolean isMoved = false;
                private boolean isRepeating = false;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    int action = event.getActionMasked();
                    if (action == MotionEvent.ACTION_DOWN) {
                        startX = event.getRawX();
                        startY = event.getRawY();
                        isMoved = false;
                        isRepeating = false;
                        v.setPressed(true);

                        if (handler == null) {
                            handler = new android.os.Handler(android.os.Looper.getMainLooper());
                        }
                        runnable = new Runnable() {
                            @Override
                            public void run() {
                                if (!isMoved) {
                                    isRepeating = true;
                                    commitEmoji(emoji);
                                    if (handler != null) {
                                        handler.postDelayed(this, 100);
                                    }
                                }
                            }
                        };
                        handler.postDelayed(runnable, 400);
                        return true;
                    } else if (action == MotionEvent.ACTION_MOVE) {
                        if (!isMoved) {
                            float dx = Math.abs(event.getRawX() - startX);
                            float dy = Math.abs(event.getRawY() - startY);
                            if (dx > touchSlop || dy > touchSlop) {
                                isMoved = true;
                                v.setPressed(false);
                                if (handler != null && runnable != null) {
                                    handler.removeCallbacks(runnable);
                                }
                            }
                        }
                        return false;
                    } else if (action == MotionEvent.ACTION_UP) {
                        v.setPressed(false);
                        if (handler != null && runnable != null) {
                            handler.removeCallbacks(runnable);
                        }
                        if (!isMoved && !isRepeating) {
                            commitEmoji(emoji);
                        }
                        return true;
                    } else if (action == MotionEvent.ACTION_CANCEL) {
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
                    mController.dpToPx(44),
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

    public void applyTheming(int accentColor, boolean isDark) {
        if (mEmojiPanel != null) {
            mEmojiPanel.setBackgroundColor(mController.getKeyboardBackgroundColor());
        }

        styleCategoryChip(mCatSticky, "sticky".equals(mCurrentCategory), accentColor, isDark);
        styleCategoryChip(mCatSmileys, "smileys".equals(mCurrentCategory), accentColor, isDark);
        styleCategoryChip(mCatPeople, "people".equals(mCurrentCategory), accentColor, isDark);
        styleCategoryChip(mCatAnimals, "animals".equals(mCurrentCategory), accentColor, isDark);
        styleCategoryChip(mCatFood, "food".equals(mCurrentCategory), accentColor, isDark);
        styleCategoryChip(mCatActivities, "activities".equals(mCurrentCategory), accentColor, isDark);
        styleCategoryChip(mCatObjects, "objects".equals(mCurrentCategory), accentColor, isDark);
        styleCategoryChip(mCatSymbols, "symbols".equals(mCurrentCategory), accentColor, isDark);

        if (mEmojiDeleteBtn != null) {
            mEmojiDeleteBtn.setColorFilter(isDark ? Color.WHITE : 0xFF1D1B20);
            GradientDrawable delBg = new GradientDrawable();
            delBg.setShape(GradientDrawable.RECTANGLE);
            delBg.setCornerRadius(mController.dpToPx(14));
            delBg.setColor(isDark ? 0x22FFFFFF : 0x14000000);
            delBg.setStroke(mController.dpToPx(1), isDark ? 0x2EFFFFFF : 0x1E000000);
            mEmojiDeleteBtn.setBackground(delBg);
        }
    }

    private void styleCategoryChip(TextView v, boolean isSelected, int accentColor, boolean isDark) {
        if (v == null) return;
        v.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        GradientDrawable chipBg = new GradientDrawable();
        chipBg.setShape(GradientDrawable.RECTANGLE);
        chipBg.setCornerRadius(mController.dpToPx(13));
        if (isSelected) {
            chipBg.setColor(mController.getTranslucentColor(accentColor, 24));
            chipBg.setStroke(mController.dpToPx(1), mController.getTranslucentColor(accentColor, 60));
            v.setTextColor(accentColor);
        } else {
            chipBg.setColor(isDark ? 0x14FFFFFF : 0x08000000);
            chipBg.setStroke(mController.dpToPx(1), isDark ? 0x20FFFFFF : 0x14000000);
            v.setTextColor(isDark ? 0xCCFFFFFF : 0x88000000);
        }
        v.setBackground(chipBg);
    }
}
