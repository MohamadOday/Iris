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
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import nabu.iris.keyboard.R;

public final class SeekBarDialogPreference extends DialogPreference
        implements SeekBar.OnSeekBarChangeListener {
    public interface ValueProxy {
        int readValue(final String key);
        int readDefaultValue(final String key);
        void writeValue(final int value, final String key);
        void writeDefaultValue(final String key);
        String getValueText(final int value);
        void feedbackValue(final int value);
    }

    private final int mMaxValue;
    private final int mMinValue;
    private final int mStepValue;

    private View mValueContainer;
    private TextView mValueView;
    private TextView mBtnMinus;
    private TextView mBtnPlus;
    private SeekBar mSeekBar;

    private ValueProxy mValueProxy;

    public SeekBarDialogPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.SeekBarDialogPreference, 0, 0);
        mMaxValue = a.getInt(R.styleable.SeekBarDialogPreference_maxValue, 0);
        mMinValue = a.getInt(R.styleable.SeekBarDialogPreference_minValue, 0);
        mStepValue = a.getInt(R.styleable.SeekBarDialogPreference_stepValue, 0);
        a.recycle();
        setDialogLayoutResource(R.layout.seek_bar_dialog);
    }

    public void setInterface(final ValueProxy proxy) {
        mValueProxy = proxy;
        final int value = mValueProxy.readValue(getKey());
        setSummary(mValueProxy.getValueText(value));
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
        mValueContainer = view.findViewById(R.id.seek_bar_value_container);
        mValueView = (TextView) view.findViewById(R.id.seek_bar_dialog_value);
        mBtnMinus = (TextView) view.findViewById(R.id.seek_bar_btn_minus);
        mBtnPlus = (TextView) view.findViewById(R.id.seek_bar_btn_plus);
        mSeekBar = (SeekBar) view.findViewById(R.id.seek_bar_dialog_bar);

        int accentColor = getContext().getResources().getColor(R.color.settings_accent);

        if (mValueContainer != null) {
            GradientDrawable valBg = new GradientDrawable();
            valBg.setShape(GradientDrawable.RECTANGLE);
            valBg.setCornerRadius(dpToPx(16));
            int valBgColor = Color.argb(35, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor));
            valBg.setColor(valBgColor);
            valBg.setStroke(dpToPx(1), Color.argb(80, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)));
            mValueContainer.setBackground(valBg);
        }

        styleQuickStepButton(mBtnMinus, accentColor);
        styleQuickStepButton(mBtnPlus, accentColor);

        if (mBtnMinus != null) {
            mBtnMinus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    animateStepButton(v);
                    int step = mStepValue > 0 ? mStepValue : 1;
                    int currentVal = getValueFromProgress(mSeekBar.getProgress());
                    int nextVal = clipValue(currentVal - step);
                    mSeekBar.setProgress(getProgressFromValue(nextVal));
                }
            });
        }

        if (mBtnPlus != null) {
            mBtnPlus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    animateStepButton(v);
                    int step = mStepValue > 0 ? mStepValue : 1;
                    int currentVal = getValueFromProgress(mSeekBar.getProgress());
                    int nextVal = clipValue(currentVal + step);
                    mSeekBar.setProgress(getProgressFromValue(nextVal));
                }
            });
        }

        mSeekBar.setMax(mMaxValue - mMinValue);
        mSeekBar.setOnSeekBarChangeListener(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mSeekBar.setProgressTintList(ColorStateList.valueOf(accentColor));
            mSeekBar.setThumbTintList(ColorStateList.valueOf(accentColor));
        }

        return view;
    }

    private void styleQuickStepButton(TextView btn, int accentColor) {
        if (btn == null) return;
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        int btnBgColor = Color.argb(25, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor));
        bg.setColor(btnBgColor);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            GradientDrawable mask = new GradientDrawable();
            mask.setShape(GradientDrawable.OVAL);
            mask.setColor(0xFFFFFFFF);
            RippleDrawable ripple = new RippleDrawable(ColorStateList.valueOf(Color.argb(60, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))), bg, mask);
            btn.setBackground(ripple);
        } else {
            btn.setBackground(bg);
        }
    }

    private void animateStepButton(View v) {
        v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(80)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
                    }
                }).start();
    }

    private int getProgressFromValue(final int value) {
        return value - mMinValue;
    }

    private int getValueFromProgress(final int progress) {
        return progress + mMinValue;
    }

    private int clipValue(final int value) {
        final int clippedValue = Math.min(mMaxValue, Math.max(mMinValue, value));
        if (mStepValue <= 1) {
            return clippedValue;
        }
        return clippedValue - (clippedValue % mStepValue);
    }

    private int getClippedValueFromProgress(final int progress) {
        return clipValue(getValueFromProgress(progress));
    }

    @Override
    protected void onBindDialogView(final View view) {
        final int value = mValueProxy.readValue(getKey());
        mValueView.setText(mValueProxy.getValueText(value));
        mSeekBar.setProgress(getProgressFromValue(clipValue(value)));
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
        if (which == DialogInterface.BUTTON_NEUTRAL) {
            final int value = mValueProxy.readDefaultValue(key);
            setSummary(mValueProxy.getValueText(value));
            mValueProxy.writeDefaultValue(key);
            return;
        }
        if (which == DialogInterface.BUTTON_POSITIVE) {
            final int value = getClippedValueFromProgress(mSeekBar.getProgress());
            setSummary(mValueProxy.getValueText(value));
            mValueProxy.writeValue(value, key);
            return;
        }
    }

    @Override
    public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
        final int value = getClippedValueFromProgress(progress);
        mValueView.setText(mValueProxy.getValueText(value));
    }

    @Override
    public void onStartTrackingTouch(final SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(final SeekBar seekBar) {
        mValueProxy.feedbackValue(getClippedValueFromProgress(seekBar.getProgress()));
    }
}
