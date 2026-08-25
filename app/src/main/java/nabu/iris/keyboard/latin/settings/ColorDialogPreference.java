/*
 * Copyright (C) 2013 The Android Open Source Project
 * Copyright (C) 2022 Raimondas Rimkus
 * Copyright (C) 2026 Iris Keyboard Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nabu.iris.keyboard.latin.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import nabu.iris.keyboard.R;

public final class ColorDialogPreference extends DialogPreference
        implements SeekBar.OnSeekBarChangeListener {
    public interface ValueProxy {
        int readValue(final String key);
        void writeDefaultValue(final String key);
        void writeValue(final int value, final String key);
    }

    private View mPreviewCard;
    private TextView mValueView;
    private TextView mRedValText;
    private TextView mGreenValText;
    private TextView mBlueValText;
    private SeekBar mSeekBarRed;
    private SeekBar mSeekBarGreen;
    private SeekBar mSeekBarBlue;
    private LinearLayout mPresetsContainer;

    private ValueProxy mValueProxy;

    private static final int[] PRESET_COLORS = new int[] {
            0xFF7C4DFF, // Electric Purple
            0xFF0284C7, // Ocean Blue
            0xFF10B981, // Emerald Green
            0xFFF59E0B, // Amber
            0xFFEC4899, // Hot Pink
            0xFFEF4444, // Crimson Red
            0xFF18181B, // AMOLED Deep Black
            0xFF3F3F46, // Graphite
            0xFFF4F4F5  // Clean Frost
    };

    public ColorDialogPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.color_dialog);
    }

    public void setInterface(final ValueProxy proxy) {
        mValueProxy = proxy;
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
        mPreviewCard = view.findViewById(R.id.color_preview_card);
        mValueView = (TextView) view.findViewById(R.id.seek_bar_dialog_value);
        mRedValText = (TextView) view.findViewById(R.id.color_red_val);
        mGreenValText = (TextView) view.findViewById(R.id.color_green_val);
        mBlueValText = (TextView) view.findViewById(R.id.color_blue_val);

        mSeekBarRed = (SeekBar) view.findViewById(R.id.seek_bar_dialog_bar_red);
        mSeekBarRed.setMax(255);
        mSeekBarRed.setOnSeekBarChangeListener(this);
        applySeekBarColor(mSeekBarRed, 0xFFEF4444);

        mSeekBarGreen = (SeekBar) view.findViewById(R.id.seek_bar_dialog_bar_green);
        mSeekBarGreen.setMax(255);
        mSeekBarGreen.setOnSeekBarChangeListener(this);
        applySeekBarColor(mSeekBarGreen, 0xFF10B981);

        mSeekBarBlue = (SeekBar) view.findViewById(R.id.seek_bar_dialog_bar_blue);
        mSeekBarBlue.setMax(255);
        mSeekBarBlue.setOnSeekBarChangeListener(this);
        applySeekBarColor(mSeekBarBlue, 0xFF3B82F6);

        mPresetsContainer = (LinearLayout) view.findViewById(R.id.color_presets_container);
        setupPresets();

        return view;
    }

    private void applySeekBarColor(SeekBar seekBar, int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            seekBar.setProgressTintList(ColorStateList.valueOf(color));
            seekBar.setThumbTintList(ColorStateList.valueOf(color));
        } else {
            seekBar.getProgressDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
            seekBar.getThumb().setColorFilter(color, PorterDuff.Mode.SRC_IN);
        }
    }

    private void setupPresets() {
        if (mPresetsContainer == null) return;
        mPresetsContainer.removeAllViews();

        for (final int color : PRESET_COLORS) {
            View swatch = new View(getContext());
            int size = dpToPx(38);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins(0, 0, dpToPx(10), 0);
            swatch.setLayoutParams(lp);

            GradientDrawable swatchBg = new GradientDrawable();
            swatchBg.setShape(GradientDrawable.OVAL);
            swatchBg.setColor(color);
            swatchBg.setStroke(dpToPx(2), 0x33FFFFFF);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                GradientDrawable mask = new GradientDrawable();
                mask.setShape(GradientDrawable.OVAL);
                mask.setColor(0xFFFFFFFF);
                RippleDrawable ripple = new RippleDrawable(ColorStateList.valueOf(0x40FFFFFF), swatchBg, mask);
                swatch.setBackground(ripple);
            } else {
                swatch.setBackground(swatchBg);
            }

            swatch.setClickable(true);
            swatch.setFocusable(true);
            swatch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(100)
                            .withEndAction(new Runnable() {
                                @Override
                                public void run() {
                                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
                                }
                            }).start();
                    mSeekBarRed.setProgress(Color.red(color));
                    mSeekBarGreen.setProgress(Color.green(color));
                    mSeekBarBlue.setProgress(Color.blue(color));
                    setHeaderText(color);
                }
            });

            mPresetsContainer.addView(swatch);
        }
    }

    @Override
    protected void onBindDialogView(final View view) {
        final int color = mValueProxy.readValue(getKey());
        mSeekBarRed.setProgress(Color.red(color));
        mSeekBarGreen.setProgress(Color.green(color));
        mSeekBarBlue.setProgress(Color.blue(color));
        setHeaderText(color);
    }

    @Override
    protected void onPrepareDialogBuilder(final AlertDialog.Builder builder) {
        builder.setPositiveButton(android.R.string.ok, this)
                .setNegativeButton(android.R.string.cancel, this)
                .setNeutralButton(R.string.button_default, this);
    }

    @Override
    public void onClick(final DialogInterface dialog, final int which) {
        super.onClick(dialog, which);
        final String key = getKey();
        if (which == DialogInterface.BUTTON_POSITIVE) {
            final int value = Color.rgb(
                    mSeekBarRed.getProgress(),
                    mSeekBarGreen.getProgress(),
                    mSeekBarBlue.getProgress());
            mValueProxy.writeValue(value, key);
            return;
        }
        if (which == DialogInterface.BUTTON_NEUTRAL) {
            mValueProxy.writeDefaultValue(key);
            return;
        }
    }

    @Override
    public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
        int color = Color.rgb(
                mSeekBarRed.getProgress(),
                mSeekBarGreen.getProgress(),
                mSeekBarBlue.getProgress());
        setHeaderText(color);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    private void setHeaderText(int color) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);

        if (mRedValText != null) mRedValText.setText(String.valueOf(r));
        if (mGreenValText != null) mGreenValText.setText(String.valueOf(g));
        if (mBlueValText != null) mBlueValText.setText(String.valueOf(b));

        if (mValueView != null) {
            mValueView.setText("#" + getValueText(color));
            boolean bright = (r * 299 + g * 587 + b * 114) / 1000 > 150;
            mValueView.setTextColor(bright ? 0xFF000000 : 0xFFFFFFFF);

            GradientDrawable pillBg = new GradientDrawable();
            pillBg.setShape(GradientDrawable.RECTANGLE);
            pillBg.setCornerRadius(dpToPx(14));
            pillBg.setColor(bright ? 0x26000000 : 0x33FFFFFF);
            mValueView.setBackground(pillBg);
        }

        if (mPreviewCard != null) {
            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setShape(GradientDrawable.RECTANGLE);
            cardBg.setCornerRadius(dpToPx(20));
            cardBg.setColor(color);
            cardBg.setStroke(dpToPx(1), 0x33FFFFFF);
            mPreviewCard.setBackground(cardBg);
        }
    }

    private String getValueText(final int value) {
        String temp = Integer.toHexString(value);
        for (; temp.length() < 8; temp = "0" + temp);
        return temp.substring(2).toUpperCase();
    }
}
