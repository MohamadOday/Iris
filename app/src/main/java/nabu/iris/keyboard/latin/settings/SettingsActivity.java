/*
 * Copyright (C) 2012 The Android Open Source Project
 * Copyright (C) 2025 Raimondas Rimkus
 * Copyright (C) 2021 wittmane
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

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.WrapperListAdapter;

import nabu.iris.keyboard.R;
import nabu.iris.keyboard.latin.utils.FragmentUtils;

public class SettingsActivity extends PreferenceActivity {
    private static final String DEFAULT_FRAGMENT = SettingsFragment.class.getName();
    private static final String TAG = SettingsActivity.class.getSimpleName();

    @Override
    protected void onStart() {
        super.onStart();

        boolean enabled = false;
        try {
            enabled = isInputMethodOfThisImeEnabled();
        } catch (Exception e) {
            Log.e(TAG, "Exception in check if input method is enabled", e);
        }

        if (!enabled) {
            final Context context = this;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.setup_message);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Intent intent = new Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    finish();
                }
            });
            builder.setCancelable(false);

            builder.create().show();
        }
    }

    private boolean isInputMethodOfThisImeEnabled() {
        final InputMethodManager imm =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        final String imePackageName = getPackageName();
        for (final InputMethodInfo imi : imm.getEnabledInputMethodList()) {
            if (imi.getPackageName().equals(imePackageName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onCreate(final Bundle savedState) {
        super.onCreate(savedState);

        boolean isDarkTheme = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        android.content.SharedPreferences prefs = nabu.iris.keyboard.compat.PreferenceManagerCompat.getDeviceSharedPreferences(this);
        boolean isAmoled = isDarkTheme && prefs.getBoolean("pref_amoled_dark_mode", false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            int themeColor = isAmoled ? 0xFF000000 : getResources().getColor(R.color.settings_bg);
            window.setStatusBarColor(themeColor);
            window.setNavigationBarColor(themeColor);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int flags = window.getDecorView().getSystemUiVisibility();
                if (!isDarkTheme) {
                    flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                    }
                } else {
                    flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                    }
                }
                window.getDecorView().setSystemUiVisibility(flags);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                final View container = (View) getListView().getParent().getParent();
                container.setOnApplyWindowInsetsListener((view, windowInsets) -> {
                    android.graphics.Insets insets = windowInsets.getInsets(WindowInsets.Type.systemBars());
                    ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
                    mlp.topMargin = insets.top;
                    mlp.leftMargin = insets.left;
                    mlp.bottomMargin = insets.bottom;
                    mlp.rightMargin = insets.right;
                    view.setLayoutParams(mlp);
                    return WindowInsets.CONSUMED;
                });
            } catch (Exception e) {
                // Ignore
            }
        }

        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);

            int actionBarColor = isAmoled ? 0xFF000000 : getResources().getColor(R.color.settings_bg);
            GradientDrawable abBg = new GradientDrawable();
            abBg.setColor(actionBarColor);
            actionBar.setBackgroundDrawable(abBg);
            actionBar.setElevation(0);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            super.onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Intent getIntent() {
        final Intent intent = super.getIntent();
        final String fragment = intent.getStringExtra(EXTRA_SHOW_FRAGMENT);
        if (fragment == null) {
            intent.putExtra(EXTRA_NO_HEADERS, true);
            intent.putExtra(EXTRA_SHOW_FRAGMENT, DEFAULT_FRAGMENT);
        }
        intent.putExtra(EXTRA_NO_HEADERS, true);
        return intent;
    }

    @Override
    public boolean isValidFragment(final String fragmentName) {
        return FragmentUtils.isValidFragment(fragmentName);
    }

    public static void stylePreferenceFragment(PreferenceFragment fragment) {
        if (fragment == null || fragment.getActivity() == null) return;

        final Context context = fragment.getActivity();
        if (fragment.getView() == null) return;
        final ListView listView = (ListView) fragment.getView().findViewById(android.R.id.list);
        if (listView == null) return;

        boolean isDarkTheme = (context.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        android.content.SharedPreferences prefs = nabu.iris.keyboard.compat.PreferenceManagerCompat.getDeviceSharedPreferences(context);
        boolean isAmoled = isDarkTheme && prefs.getBoolean("pref_amoled_dark_mode", false);

        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setSelector(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));

        int paddingSide = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 16, context.getResources().getDisplayMetrics());
        int paddingTop = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 12, context.getResources().getDisplayMetrics());
        listView.setPadding(paddingSide, paddingTop, paddingSide, paddingTop + paddingSide);
        listView.setClipToPadding(false);

        int bgColor = isAmoled ? 0xFF000000 : context.getResources().getColor(R.color.settings_bg);
        listView.setBackgroundColor(bgColor);

        try {
            View parent = (View) listView.getParent();
            if (parent != null) {
                parent.setBackgroundColor(bgColor);
                View grandParent = (View) parent.getParent();
                if (grandParent != null) {
                    grandParent.setBackgroundColor(bgColor);
                }
            }
        } catch (Exception e) {
            // Ignore
        }

        ListAdapter currentAdapter = listView.getAdapter();
        if (currentAdapter != null && !(currentAdapter instanceof CardPreferenceAdapter)) {
            listView.setAdapter(new CardPreferenceAdapter(currentAdapter, context, isDarkTheme));
        }
    }

    public static class CardPreferenceAdapter implements WrapperListAdapter {
        private final ListAdapter mOriginal;
        private final Context mContext;
        private final boolean mIsDarkTheme;

        public CardPreferenceAdapter(ListAdapter original, Context context, boolean isDarkTheme) {
            mOriginal = original;
            mContext = context;
            mIsDarkTheme = isDarkTheme;
        }

        @Override
        public ListAdapter getWrappedAdapter() {
            return mOriginal;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return mOriginal.areAllItemsEnabled();
        }

        @Override
        public boolean isEnabled(int position) {
            return mOriginal.isEnabled(position);
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {
            mOriginal.registerDataSetObserver(observer);
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
            mOriginal.unregisterDataSetObserver(observer);
        }

        @Override
        public int getCount() {
            return mOriginal.getCount();
        }

        @Override
        public Object getItem(int position) {
            return mOriginal.getItem(position);
        }

        @Override
        public long getItemId(int position) {
            return mOriginal.getItemId(position);
        }

        @Override
        public boolean hasStableIds() {
            return mOriginal.hasStableIds();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = mOriginal.getView(position, convertView, parent);
            stylePreferenceView(position, view, parent.getContext());
            return view;
        }

        @Override
        public int getItemViewType(int position) {
            return mOriginal.getItemViewType(position);
        }

        @Override
        public int getViewTypeCount() {
            return mOriginal.getViewTypeCount();
        }

        @Override
        public boolean isEmpty() {
            return mOriginal.isEmpty();
        }

        private int dpToPx(Context context, int dp) {
            return (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    dp,
                    context.getResources().getDisplayMetrics()
            );
        }

        private void stylePreferenceView(int position, View view, Context context) {
            Object item = null;
            try {
                item = mOriginal.getItem(position);
            } catch (Exception e) {
                // Ignore
            }

            android.content.SharedPreferences prefs = nabu.iris.keyboard.compat.PreferenceManagerCompat.getDeviceSharedPreferences(context);
            boolean isDarkTheme = (context.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                    == android.content.res.Configuration.UI_MODE_NIGHT_YES;
            boolean isAmoled = isDarkTheme && prefs.getBoolean("pref_amoled_dark_mode", false);

            int cardColor = isAmoled ? 0xFF121214 : context.getResources().getColor(R.color.settings_card_bg);
            int strokeColor = isAmoled ? 0xFF28282B : context.getResources().getColor(R.color.settings_card_stroke);
            int accentColor = context.getResources().getColor(R.color.settings_accent);
            int textPrimary = context.getResources().getColor(R.color.settings_text_primary);
            int textSecondary = context.getResources().getColor(R.color.settings_text_secondary);

            if (item instanceof PreferenceCategory) {
                view.setBackground(null);
                view.setPadding(dpToPx(context, 12), dpToPx(context, 18), dpToPx(context, 12), dpToPx(context, 6));
                
                TextView titleView = findTitleTextView(view);
                if (titleView != null) {
                    titleView.setTextColor(accentColor);
                    titleView.setTextSize(12.0f);
                    titleView.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD));
                    titleView.setAllCaps(true);
                    titleView.setLetterSpacing(0.06f);
                }
                return;
            }

            boolean isFirst = false;
            boolean isLast = false;

            if (position == 0) {
                isFirst = true;
            } else {
                try {
                    Object prevItem = mOriginal.getItem(position - 1);
                    if (prevItem instanceof PreferenceCategory) {
                        isFirst = true;
                    }
                } catch (Exception e) {
                    isFirst = true;
                }
            }

            if (position == mOriginal.getCount() - 1) {
                isLast = true;
            } else {
                try {
                    Object nextItem = mOriginal.getItem(position + 1);
                    if (nextItem instanceof PreferenceCategory) {
                        isLast = true;
                    }
                } catch (Exception e) {
                    isLast = true;
                }
            }

            float r = dpToPx(context, 16);
            float[] cornerRadii;
            if (isFirst && isLast) {
                cornerRadii = new float[]{r, r, r, r, r, r, r, r};
            } else if (isFirst) {
                cornerRadii = new float[]{r, r, r, r, 0, 0, 0, 0};
            } else if (isLast) {
                cornerRadii = new float[]{0, 0, 0, 0, r, r, r, r};
            } else {
                cornerRadii = new float[]{0, 0, 0, 0, 0, 0, 0, 0};
            }

            GradientDrawable normalBg = new GradientDrawable();
            normalBg.setShape(GradientDrawable.RECTANGLE);
            normalBg.setColor(cardColor);
            if (strokeColor != 0) {
                normalBg.setStroke(dpToPx(context, 1), strokeColor);
            }
            normalBg.setCornerRadii(cornerRadii);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                GradientDrawable maskBg = new GradientDrawable();
                maskBg.setShape(GradientDrawable.RECTANGLE);
                maskBg.setColor(0xFFFFFFFF);
                maskBg.setCornerRadii(cornerRadii);

                int rippleColor = getTranslucentColor(accentColor, isDarkTheme ? 25 : 18);
                ColorStateList rippleCsl = ColorStateList.valueOf(rippleColor);
                RippleDrawable ripple = new RippleDrawable(rippleCsl, normalBg, maskBg);
                view.setBackground(ripple);
            } else {
                view.setBackground(normalBg);
            }

            view.setPadding(dpToPx(context, 16), dpToPx(context, 14), dpToPx(context, 16), dpToPx(context, 14));

            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            ViewGroup.MarginLayoutParams marginParams;
            if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                marginParams = (ViewGroup.MarginLayoutParams) layoutParams;
            } else {
                marginParams = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            int marginTop = isFirst ? dpToPx(context, 4) : 0;
            int marginBottom = isLast ? dpToPx(context, 8) : dpToPx(context, 1);
            marginParams.setMargins(0, marginTop, 0, marginBottom);
            view.setLayoutParams(marginParams);

            styleTexts(view, textPrimary, textSecondary);
            styleIcons(view, accentColor);
            styleWidgets(view, accentColor);
        }

        private TextView findTitleTextView(View view) {
            if (view instanceof TextView) {
                TextView tv = (TextView) view;
                if (tv.getId() == android.R.id.title) {
                    return tv;
                }
            }
            if (view instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) view;
                for (int i = 0; i < vg.getChildCount(); i++) {
                    TextView tv = findTitleTextView(vg.getChildAt(i));
                    if (tv != null) return tv;
                }
            }
            return null;
        }

        private void styleTexts(View view, int textPrimary, int textSecondary) {
            if (view instanceof TextView) {
                TextView tv = (TextView) view;
                if (tv.getId() == android.R.id.title) {
                    tv.setTextColor(textPrimary);
                    tv.setTextSize(15.5f);
                    tv.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
                } else if (tv.getId() == android.R.id.summary) {
                    tv.setTextColor(textSecondary);
                    tv.setTextSize(13.0f);
                    tv.setPadding(0, dpToPx(mContext, 3), 0, 0);
                }
            } else if (view instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) view;
                for (int i = 0; i < vg.getChildCount(); i++) {
                    styleTexts(vg.getChildAt(i), textPrimary, textSecondary);
                }
            }
        }

        private void styleIcons(View view, int accentColor) {
            if (view instanceof ImageView) {
                ImageView iv = (ImageView) view;
                if (iv.getId() == android.R.id.icon) {
                    if (iv.getDrawable() != null) {
                        iv.setVisibility(View.VISIBLE);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            iv.setImageTintList(ColorStateList.valueOf(accentColor));
                        }
                    } else {
                        iv.setVisibility(View.GONE);
                    }
                }
            } else if (view instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) view;
                for (int i = 0; i < vg.getChildCount(); i++) {
                    styleIcons(vg.getChildAt(i), accentColor);
                }
            }
        }

        private void styleWidgets(View view, int accentColor) {
            if (view instanceof android.widget.CompoundButton) {
                android.widget.CompoundButton cb = (android.widget.CompoundButton) view;
                if (cb instanceof android.widget.Switch) {
                    android.widget.Switch sw = (android.widget.Switch) cb;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        int trackColor = getTranslucentColor(accentColor, 40);
                        sw.setThumbTintList(ColorStateList.valueOf(accentColor));
                        sw.setTrackTintList(ColorStateList.valueOf(trackColor));
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        cb.setButtonTintList(ColorStateList.valueOf(accentColor));
                    }
                }
            } else if (view instanceof android.widget.SeekBar) {
                android.widget.SeekBar sb = (android.widget.SeekBar) view;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    sb.setProgressTintList(ColorStateList.valueOf(accentColor));
                    sb.setThumbTintList(ColorStateList.valueOf(accentColor));
                }
            } else if (view instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) view;
                for (int i = 0; i < vg.getChildCount(); i++) {
                    styleWidgets(vg.getChildAt(i), accentColor);
                }
            }
        }

        private int getTranslucentColor(int color, int alphaPercent) {
            int alpha = (int) (255 * (alphaPercent / 100.0));
            return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
        }
    }
}
