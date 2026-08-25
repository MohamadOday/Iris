/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import nabu.iris.keyboard.R;
import nabu.iris.keyboard.compat.PreferenceManagerCompat;
import nabu.iris.keyboard.latin.RichInputMethodManager;
import nabu.iris.keyboard.latin.utils.ApplicationUtils;

public final class SettingsFragment extends InputMethodSettingsFragment {
    private static final String TAG = "SettingsFragment";
    private static final int REQUEST_EXPORT = 42201;
    private static final int REQUEST_IMPORT = 42202;

    @Override
    public void onCreate(final Bundle icicle) {
        super.onCreate(icicle);
        setHasOptionsMenu(true);
        addPreferencesFromResource(R.xml.prefs);
        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.setTitle(
                ApplicationUtils.getActivityTitleResId(getActivity(), SettingsActivity.class));
        final Resources res = getResources();

        findPreference("privacy_policy").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new android.app.AlertDialog.Builder(getActivity())
                        .setTitle(R.string.privacy_policy)
                        .setMessage("Iris Keyboard respects your privacy. All keystrokes, personal dictionary entries, and soundpacks are processed completely on your device.\n\nOptional cloud AI tools only send the specific prompt you submit when explicitly triggered.")
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                return true;
            }
        });
        findPreference("license").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                openUrl(res.getString(R.string.license_url));
                return true;
            }
        });
        findPreference("about").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showAboutDialog();
                return true;
            }
        });

        findPreference("export_settings").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                exportSettings();
                return true;
            }
        });

        findPreference("import_settings").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                importSettings();
                return true;
            }
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        SettingsActivity.stylePreferenceFragment(this);
    }

    private void openUrl(String uri) {
        try {
            final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            startActivity(browserIntent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Browser not found");
        }
    }

    private void exportSettings() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "iris_keyboard_settings.json");
        try {
            startActivityForResult(intent, REQUEST_EXPORT);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getActivity(), "No file manager found", Toast.LENGTH_SHORT).show();
        }
    }

    private void importSettings() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        try {
            startActivityForResult(intent, REQUEST_IMPORT);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getActivity(), "No file manager found", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        Uri uri = data.getData();
        if (requestCode == REQUEST_EXPORT) {
            handleExport(uri);
        } else if (requestCode == REQUEST_IMPORT) {
            handleImport(uri);
        }
    }

    private void handleExport(Uri uri) {
        try (OutputStream os = getActivity().getContentResolver().openOutputStream(uri)) {
            if (os == null) {
                throw new java.io.IOException("Could not open output stream");
            }
            SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(getActivity());
            JSONObject json = new JSONObject();
            Map<String, ?> allEntries = prefs.getAll();
            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof Set) {
                    JSONArray arr = new JSONArray();
                    for (Object item : (Set<?>) value) {
                        arr.put(item);
                    }
                    json.put(key, arr);
                } else {
                    json.put(key, value);
                }
            }
            os.write(json.toString(4).getBytes("UTF-8"));
            Toast.makeText(getActivity(), "Settings exported successfully", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Export failed", e);
            Toast.makeText(getActivity(), "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void handleImport(Uri uri) {
        try (InputStream is = getActivity().getContentResolver().openInputStream(uri)) {
            if (is == null) {
                throw new java.io.IOException("Could not open input stream");
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            JSONObject json = new JSONObject(sb.toString());

            // Check if imported soundpack is missing on this device
            String importedSoundpack = json.optString("pref_keypress_soundpack", "default");
            boolean soundpackMissing = false;
            if (!importedSoundpack.equals("default") && !importedSoundpack.equals("default_deep")) {
                java.io.File soundpacksDir = getActivity().getExternalFilesDir("soundpacks");
                java.io.File packDir = soundpacksDir != null ? new java.io.File(soundpacksDir, importedSoundpack) : null;
                if (packDir == null || !packDir.exists() || !packDir.isDirectory()) {
                    soundpackMissing = true;
                }
            }

            SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(getActivity());
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = json.get(key);
                if (value instanceof Boolean) {
                    editor.putBoolean(key, (Boolean) value);
                } else if (value instanceof Integer) {
                    editor.putInt(key, (Integer) value);
                } else if (value instanceof Long) {
                    editor.putLong(key, (Long) value);
                } else if (value instanceof Double) {
                    editor.putFloat(key, ((Double) value).floatValue());
                } else if (value instanceof String) {
                    editor.putString(key, (String) value);
                } else if (value instanceof JSONArray) {
                    JSONArray arr = (JSONArray) value;
                    Set<String> set = new HashSet<>();
                    for (int i = 0; i < arr.length(); i++) {
                        set.add(arr.getString(i));
                    }
                    editor.putStringSet(key, set);
                }
            }
            editor.commit();

            // Reload subtypes (languages) immediately so they take effect in memory
            RichInputMethodManager.getInstance().reloadSubtypes(getActivity());

            Toast.makeText(getActivity(), "Settings imported successfully", Toast.LENGTH_LONG).show();
            
            final Activity activity = getActivity();
            if (activity != null && !activity.isFinishing()) {
                if (soundpackMissing) {
                    final String finalSoundpack = importedSoundpack;
                    new AlertDialog.Builder(activity)
                        .setTitle("Soundpack Not Found")
                        .setMessage("The imported settings use the soundpack '" + finalSoundpack.replace("_", " ") + "', which is not downloaded. Would you like to download it now or reset to the default soundpack?")
                        .setPositiveButton("Download", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(activity, SoundpackDownloadActivity.class);
                                activity.startActivity(intent);
                                activity.recreate();
                            }
                        })
                        .setNegativeButton("Reset to Default", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                SharedPreferences p = PreferenceManagerCompat.getDeviceSharedPreferences(activity);
                                p.edit().putString("pref_keypress_soundpack", "default").apply();
                                activity.recreate();
                            }
                        })
                        .setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                activity.recreate();
                            }
                        })
                        .setCancelable(false)
                        .show();
                } else {
                    activity.recreate();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Import failed", e);
            Toast.makeText(getActivity(), "Import failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showAboutDialog() {
        if (getActivity() == null) return;
        
        android.view.LayoutInflater inflater = getActivity().getLayoutInflater();
        android.view.View dialogView = inflater.inflate(R.layout.about_dialog, null);
        
        android.widget.TextView versionChip = dialogView.findViewById(R.id.about_version_chip);
        android.view.View pillarsCard = dialogView.findViewById(R.id.about_pillars_card);
        android.view.View devCard = dialogView.findViewById(R.id.about_dev_card);
        android.widget.TextView githubBtn = dialogView.findViewById(R.id.about_github_btn);
        android.widget.TextView telegramBtn = dialogView.findViewById(R.id.about_telegram_btn);
        android.widget.TextView websiteBtn = dialogView.findViewById(R.id.about_website_btn);
        final android.view.View easterEggCard = dialogView.findViewById(R.id.about_easter_egg_card);
        
        boolean isDark = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        android.content.SharedPreferences prefs = nabu.iris.keyboard.compat.PreferenceManagerCompat.getDeviceSharedPreferences(getActivity());
        boolean isAmoled = isDark && prefs.getBoolean("pref_amoled_dark_mode", false);
        
        int cardBgColor = isAmoled ? 0xFF121214 : getResources().getColor(R.color.settings_card_bg);
        int cardStroke = isAmoled ? 0xFF28282B : getResources().getColor(R.color.settings_card_stroke);
        int accentColor = Settings.getMaterialYouAccentColor(getActivity());
        
        // Style Version Chip
        if (versionChip != null) {
            String verName = nabu.iris.keyboard.latin.utils.ApplicationUtils.getVersionName(getActivity());
            versionChip.setText("v" + verName + " • Open Source");
            versionChip.setTextColor(accentColor);
            android.graphics.drawable.GradientDrawable chipBg = new android.graphics.drawable.GradientDrawable();
            chipBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            chipBg.setCornerRadius(dpToPx(12));
            int chipTint = android.graphics.Color.argb(35, android.graphics.Color.red(accentColor),
                    android.graphics.Color.green(accentColor), android.graphics.Color.blue(accentColor));
            chipBg.setColor(chipTint);
            versionChip.setBackground(chipBg);
        }
        
        // Style Cards
        android.graphics.drawable.GradientDrawable cardDrawable = new android.graphics.drawable.GradientDrawable();
        cardDrawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        cardDrawable.setCornerRadius(dpToPx(16));
        cardDrawable.setColor(cardBgColor);
        if (cardStroke != 0) cardDrawable.setStroke(dpToPx(1), cardStroke);
        
        if (pillarsCard != null) pillarsCard.setBackground(cardDrawable);
        if (devCard != null) devCard.setBackground(cardDrawable);
        
        // Style Buttons
        applyActionButtonStyle(githubBtn, cardStroke, accentColor);
        applyActionButtonStyle(telegramBtn, cardStroke, accentColor);
        applyActionButtonStyle(websiteBtn, cardStroke, accentColor);
        
        if (githubBtn != null) {
            githubBtn.setOnClickListener(v -> openUrl("https://github.com/MohamadOday"));
        }
        if (telegramBtn != null) {
            telegramBtn.setOnClickListener(v -> openUrl("https://t.me/bn3di"));
        }
        if (websiteBtn != null) {
            websiteBtn.setOnClickListener(v -> openUrl("https://bn3di.is-a.dev"));
        }
        
        // Easter Egg: Tap version chip 5 times
        final int[] tapCount = {0};
        if (versionChip != null) {
            versionChip.setOnClickListener(v -> {
                if (easterEggCard.getVisibility() == android.view.View.VISIBLE) return;
                tapCount[0]++;
                if (tapCount[0] >= 5) {
                    if (easterEggCard != null) {
                        android.graphics.drawable.GradientDrawable eggBg = new android.graphics.drawable.GradientDrawable();
                        eggBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                        eggBg.setCornerRadius(dpToPx(16));
                        eggBg.setColor(cardBgColor);
                        eggBg.setStroke(dpToPx(1.5f), accentColor);
                        easterEggCard.setBackground(eggBg);
                        
                        easterEggCard.setVisibility(android.view.View.VISIBLE);
                        easterEggCard.setAlpha(0f);
                        easterEggCard.setScaleX(0.85f);
                        easterEggCard.setScaleY(0.85f);
                        easterEggCard.animate()
                                .alpha(1f)
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(400)
                                .setInterpolator(new android.view.animation.OvershootInterpolator())
                                .start();
                    }
                    android.widget.Toast.makeText(getActivity(), "🏛️ Mesopotamia Heritage Unlocked!", android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        new android.app.AlertDialog.Builder(getActivity())
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .show();
    }
    
    private void applyActionButtonStyle(android.widget.TextView btn, int strokeColor, int accentColor) {
        if (btn == null) return;
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dpToPx(10));
        int tint = android.graphics.Color.argb(20, android.graphics.Color.red(accentColor),
                android.graphics.Color.green(accentColor), android.graphics.Color.blue(accentColor));
        bg.setColor(tint);
        if (strokeColor != 0) bg.setStroke(dpToPx(1), strokeColor);
        btn.setBackground(bg);
    }
    
    private int dpToPx(float dp) {
        return (int) android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }
}
