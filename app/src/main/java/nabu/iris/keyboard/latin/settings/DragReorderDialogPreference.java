package nabu.iris.keyboard.latin.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Switch;

import nabu.iris.keyboard.R;

public final class DragReorderDialogPreference extends DialogPreference {

    private Switch mSwitchKeys;
    private Switch mSwitchClipboard;
    private Switch mSwitchAi;
    private Switch mSwitchEmoji;
    private Switch mSwitchSettings;

    public DragReorderDialogPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.utility_drag_dialog);
    }

    @Override
    protected View onCreateDialogView() {
        final View view = super.onCreateDialogView();
        mSwitchKeys = (Switch) view.findViewById(R.id.switch_keys);
        mSwitchClipboard = (Switch) view.findViewById(R.id.switch_clipboard);
        mSwitchAi = (Switch) view.findViewById(R.id.switch_ai);
        mSwitchEmoji = (Switch) view.findViewById(R.id.switch_emoji);
        mSwitchSettings = (Switch) view.findViewById(R.id.switch_settings);
        return view;
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
