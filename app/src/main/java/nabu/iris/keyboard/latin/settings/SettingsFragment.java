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
import android.content.ActivityNotFoundException;
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
                openUrl(res.getString(R.string.privacy_policy_url));
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
            Toast.makeText(getActivity(), "Settings imported successfully", Toast.LENGTH_LONG).show();
            
            if (getActivity() != null) {
                getActivity().recreate();
            }
        } catch (Exception e) {
            Log.e(TAG, "Import failed", e);
            Toast.makeText(getActivity(), "Import failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
