package nabu.iris.keyboard.latin.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.Switch;

import nabu.iris.keyboard.R;

public final class DragReorderDialogPreference extends DialogPreference {

    private Switch mSwitchKeys;
    private Switch mSwitchClipboard;
    private Switch mSwitchAi;
    private Switch mSwitchTranslate;
    private Switch mSwitchGif;
    private Switch mSwitchEmoji;
    private Switch mSwitchSettings;

    public DragReorderDialogPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.utility_drag_dialog);
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getContext().getResources().getDisplayMetrics()
        );
    }

    @Override
    protected View onCreateDialogView() {
        final View view = super.onCreateDialogView();
        mSwitchKeys = (Switch) view.findViewById(R.id.switch_keys);
        mSwitchClipboard = (Switch) view.findViewById(R.id.switch_clipboard);
        mSwitchAi = (Switch) view.findViewById(R.id.switch_ai);
        mSwitchTranslate = (Switch) view.findViewById(R.id.switch_translate);
        mSwitchGif = (Switch) view.findViewById(R.id.switch_gif);
        mSwitchEmoji = (Switch) view.findViewById(R.id.switch_emoji);
        mSwitchSettings = (Switch) view.findViewById(R.id.switch_settings);

        int accentColor = getContext().getResources().getColor(R.color.settings_accent);
        int cardColor = getContext().getResources().getColor(R.color.settings_card_bg);
        int strokeColor = getContext().getResources().getColor(R.color.settings_card_stroke);

        styleSwitch(mSwitchKeys, accentColor, cardColor, strokeColor);
        styleSwitch(mSwitchClipboard, accentColor, cardColor, strokeColor);
        styleSwitch(mSwitchAi, accentColor, cardColor, strokeColor);
        styleSwitch(mSwitchTranslate, accentColor, cardColor, strokeColor);
        styleSwitch(mSwitchGif, accentColor, cardColor, strokeColor);
        styleSwitch(mSwitchEmoji, accentColor, cardColor, strokeColor);
        styleSwitch(mSwitchSettings, accentColor, cardColor, strokeColor);

        return view;
    }

    private void styleSwitch(Switch sw, int accentColor, int cardColor, int strokeColor) {
        if (sw == null) return;

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dpToPx(16));
        bg.setColor(cardColor);
        if (strokeColor != 0) {
            bg.setStroke(dpToPx(1), strokeColor);
        }
        sw.setBackground(bg);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int[][] states = new int[][] {
                new int[] { android.R.attr.state_checked },
                new int[] { -android.R.attr.state_checked }
            };
            int[] thumbColors = new int[] {
                accentColor,
                0xFF9E9E9E
            };
            int trackColor = Color.argb(70, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor));
            int[] trackColors = new int[] {
                trackColor,
                0x33888888
            };
            sw.setThumbTintList(new ColorStateList(states, thumbColors));
            sw.setTrackTintList(new ColorStateList(states, trackColors));
        }
    }

    @Override
    protected void onBindDialogView(final View view) {
        super.onBindDialogView(view);
        final SharedPreferences prefs = getSharedPreferences();
        if (mSwitchKeys != null) {
            mSwitchKeys.setChecked(prefs.getBoolean("pref_utility_show_keys", true));
        }
        if (mSwitchClipboard != null) {
            mSwitchClipboard.setChecked(prefs.getBoolean("pref_utility_show_clipboard", true));
        }
        if (mSwitchAi != null) {
            mSwitchAi.setChecked(prefs.getBoolean("pref_utility_show_ai", true));
        }
        if (mSwitchTranslate != null) {
            mSwitchTranslate.setChecked(prefs.getBoolean("pref_utility_show_translate", true));
        }
        if (mSwitchGif != null) {
            mSwitchGif.setChecked(prefs.getBoolean("pref_utility_show_gif", true));
        }
        if (mSwitchEmoji != null) {
            mSwitchEmoji.setChecked(prefs.getBoolean("pref_show_emoji_key", true));
        }
        if (mSwitchSettings != null) {
            mSwitchSettings.setChecked(prefs.getBoolean("pref_utility_show_settings", true));
        }
    }

    @Override
    protected void onPrepareDialogBuilder(final AlertDialog.Builder builder) {
        builder.setPositiveButton(android.R.string.ok, this)
                .setNegativeButton(android.R.string.cancel, this);
    }

    @Override
    public void onClick(final DialogInterface dialog, final int which) {
        super.onClick(dialog, which);
        if (which == DialogInterface.BUTTON_POSITIVE) {
            final SharedPreferences.Editor editor = getSharedPreferences().edit();
            if (mSwitchKeys != null) {
                editor.putBoolean("pref_utility_show_keys", mSwitchKeys.isChecked());
            }
            if (mSwitchClipboard != null) {
                editor.putBoolean("pref_utility_show_clipboard", mSwitchClipboard.isChecked());
            }
            if (mSwitchAi != null) {
                editor.putBoolean("pref_utility_show_ai", mSwitchAi.isChecked());
            }
            if (mSwitchTranslate != null) {
                editor.putBoolean("pref_utility_show_translate", mSwitchTranslate.isChecked());
            }
            if (mSwitchGif != null) {
                editor.putBoolean("pref_utility_show_gif", mSwitchGif.isChecked());
            }
            if (mSwitchEmoji != null) {
                editor.putBoolean("pref_show_emoji_key", mSwitchEmoji.isChecked());
            }
            if (mSwitchSettings != null) {
                editor.putBoolean("pref_utility_show_settings", mSwitchSettings.isChecked());
            }
            editor.apply();

            callChangeListener(true);
        }
    }
}
