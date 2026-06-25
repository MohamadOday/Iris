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
import nabu.iris.keyboard.latin.RichInputMethodManager;
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
                        .setMessage("We do not store your data.")
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
        
        android.widget.TextView versionText = dialogView.findViewById(R.id.about_version_text);
        android.widget.TextView githubBtn = dialogView.findViewById(R.id.about_github_btn);
        android.widget.TextView telegramBtn = dialogView.findViewById(R.id.about_telegram_btn);
        android.widget.TextView websiteBtn = dialogView.findViewById(R.id.about_website_btn);
        android.view.View homelandRow = dialogView.findViewById(R.id.about_homeland_row);
        final android.view.View easterEggCard = dialogView.findViewById(R.id.about_easter_egg_card);
        final android.widget.TextView hintText = dialogView.findViewById(R.id.about_easter_egg_hint);
        
        String verName = nabu.iris.keyboard.latin.utils.ApplicationUtils.getVersionName(getActivity());
        versionText.setText("Version " + verName);
        
        githubBtn.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                openUrl("https://github.com/MohamadOday");
            }
        });
        
        telegramBtn.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                openUrl("https://t.me/bn3di");
            }
        });
        
        websiteBtn.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                openUrl("https://bn3di.is-a.dev");
            }
        });
        
        final int[] tapCount = {0};
        android.view.View.OnClickListener easterEggTrigger = new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                if (easterEggCard.getVisibility() == android.view.View.VISIBLE) {
                    return;
                }
                tapCount[0]++;
                int remaining = 5 - tapCount[0];
                if (remaining > 0) {
                    android.widget.Toast.makeText(getActivity(), 
                            "Tap " + remaining + " more times for a secret...", 
                            android.widget.Toast.LENGTH_SHORT).show();
                } else {
                    hintText.setText("You unlocked the Cradle of Civilization! 🇮🇶");
                    easterEggCard.setVisibility(android.view.View.VISIBLE);
                    easterEggCard.setAlpha(0f);
                    easterEggCard.setScaleX(0.8f);
                    easterEggCard.setScaleY(0.8f);
                    easterEggCard.animate()
                            .alpha(1f)
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(500)
                            .setInterpolator(new android.view.animation.OvershootInterpolator())
                            .start();
                }
            }
        };
        
        homelandRow.setOnClickListener(easterEggTrigger);
        versionText.setOnClickListener(easterEggTrigger);
        
        new android.app.AlertDialog.Builder(getActivity())
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
