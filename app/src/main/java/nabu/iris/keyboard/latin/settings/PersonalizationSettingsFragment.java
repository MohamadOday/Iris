/*
 * Copyright (C) 2026 Latin IME Customizer
 */

package nabu.iris.keyboard.latin.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import nabu.iris.keyboard.R;

public final class PersonalizationSettingsFragment extends SubScreenFragment {
    private static final String TAG = "PersonalizationSettings";
    private static final int REQUEST_IMAGE_PICK = 4501;
    private static final int REQUEST_KBD_IMAGE_PICK = 4502;

    public static final String PREF_KEY_SHAPE = "pref_key_shape";
    public static final String PREF_KEY_BG_IMAGE_ACTIVE = "pref_key_bg_image_active";
    
    public static final String PREF_KEY_SIZE_MODE = "pref_key_size_mode";
    public static final String PREF_KEY_SIZE_SCALE = "pref_key_size_scale";
    public static final String PREF_KEY_WIDTH_SCALE = "pref_key_width_scale";
    public static final String PREF_KEY_HEIGHT_SCALE = "pref_key_height_scale";

    public static final String PREF_KEY_GAP_X = "pref_key_gap_x";
    public static final String PREF_KEY_GAP_Y = "pref_key_gap_y";

    public static final String PREF_KBD_BG_IMAGE_ACTIVE = "pref_kbd_bg_image_active";
    public static final String PREF_KBD_BG_IMAGE_STYLE = "pref_kbd_bg_image_style";

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_screen_personalization);

        setupKeyShapePreference();
        setupKeySizeModePreference();
        setupKeySizeScalePreference();
        setupKeyWidthScalePreference();
        setupKeyHeightScalePreference();
        setupKeySizeResetPreference();

        setupKeyGapXPreference();
        setupKeyGapYPreference();
        setupKeyGapResetPreference();

        setupImagePickerPreference();
        setupClearImagePreference();

        setupKbdImagePickerPreference();
        setupKbdBgStylePreference();
        setupKbdClearImagePreference();

        updateImagePreferenceSummary();
        updateKbdImagePreferenceSummary();
        updateKeySizeSummaries();
        updateKeyGapSummaries();
    }

    private void setupKeyShapePreference() {
        final ListPreference pref = (ListPreference) findPreference(PREF_KEY_SHAPE);
        if (pref == null) return;

        pref.setSummary(pref.getEntry());
        pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int index = pref.findIndexOfValue((String) newValue);
                if (index >= 0) {
                    preference.setSummary(pref.getEntries()[index]);
                }
                return true;
            }
        });
    }

    private void setupKeySizeModePreference() {
        final ListPreference pref = (ListPreference) findPreference(PREF_KEY_SIZE_MODE);
        if (pref == null) return;

        pref.setSummary(pref.getEntry());
        pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int index = pref.findIndexOfValue((String) newValue);
                if (index >= 0) {
                    preference.setSummary(pref.getEntries()[index]);
                }
                pref.setValue((String) newValue);
                return true;
            }
        });
    }

    private void setupKeySizeScalePreference() {
        final SeekBarDialogPreference pref = (SeekBarDialogPreference) findPreference(PREF_KEY_SIZE_SCALE);
        if (pref == null) return;

        final SharedPreferences prefs = getSharedPreferences();
        pref.setInterface(new SeekBarDialogPreference.ValueProxy() {
            @Override
            public void writeValue(final int value, final String key) {
                prefs.edit().putInt(key, value).apply();
                updateKeySizeSummaries();
            }

            @Override
            public void writeDefaultValue(final String key) {
                prefs.edit().remove(key).apply();
                updateKeySizeSummaries();
            }

            @Override
            public int readValue(final String key) {
                return prefs.getInt(key, 100);
            }

            @Override
            public int readDefaultValue(final String key) {
                return 100;
            }

            @Override
            public String getValueText(final int value) {
                return value + "%";
            }

            @Override
            public void feedbackValue(final int value) {}
        });

        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference p) {
                String mode = prefs.getString(PREF_KEY_SIZE_MODE, "uniform");
                if ("custom".equals(mode)) {
                    Toast.makeText(getActivity(), "Please set Key Size Mode to 'Uniform Scale' first", Toast.LENGTH_SHORT).show();
                    return true; // Intercept click
                }
                return false; // Proceed to open dialog
            }
        });
    }

    private void setupKeyWidthScalePreference() {
        final SeekBarDialogPreference pref = (SeekBarDialogPreference) findPreference(PREF_KEY_WIDTH_SCALE);
        if (pref == null) return;

        final SharedPreferences prefs = getSharedPreferences();
        pref.setInterface(new SeekBarDialogPreference.ValueProxy() {
            @Override
            public void writeValue(final int value, final String key) {
                prefs.edit().putInt(key, value).apply();
                updateKeySizeSummaries();
            }

            @Override
            public void writeDefaultValue(final String key) {
                prefs.edit().remove(key).apply();
                updateKeySizeSummaries();
            }

            @Override
            public int readValue(final String key) {
                return prefs.getInt(key, 100);
            }

            @Override
            public int readDefaultValue(final String key) {
                return 100;
            }

            @Override
            public String getValueText(final int value) {
                return value + "%";
            }

            @Override
            public void feedbackValue(final int value) {}
        });

        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference p) {
                String mode = prefs.getString(PREF_KEY_SIZE_MODE, "uniform");
                if (!"custom".equals(mode)) {
                    Toast.makeText(getActivity(), "Please set Key Size Mode to 'Custom Width & Height' first", Toast.LENGTH_SHORT).show();
                    return true; // Intercept click
                }
                return false; // Proceed to open dialog
            }
        });
    }

    private void setupKeyHeightScalePreference() {
        final SeekBarDialogPreference pref = (SeekBarDialogPreference) findPreference(PREF_KEY_HEIGHT_SCALE);
        if (pref == null) return;

        final SharedPreferences prefs = getSharedPreferences();
        pref.setInterface(new SeekBarDialogPreference.ValueProxy() {
            @Override
            public void writeValue(final int value, final String key) {
                prefs.edit().putInt(key, value).apply();
                updateKeySizeSummaries();
            }

            @Override
            public void writeDefaultValue(final String key) {
                prefs.edit().remove(key).apply();
                updateKeySizeSummaries();
            }

            @Override
            public int readValue(final String key) {
                return prefs.getInt(key, 100);
            }

            @Override
            public int readDefaultValue(final String key) {
                return 100;
            }

            @Override
            public String getValueText(final int value) {
                return value + "%";
            }

            @Override
            public void feedbackValue(final int value) {}
        });

        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference p) {
                String mode = prefs.getString(PREF_KEY_SIZE_MODE, "uniform");
                if (!"custom".equals(mode)) {
                    Toast.makeText(getActivity(), "Please set Key Size Mode to 'Custom Width & Height' first", Toast.LENGTH_SHORT).show();
                    return true; // Intercept click
                }
                return false; // Proceed to open dialog
            }
        });
    }

    private void setupKeySizeResetPreference() {
        final Preference pref = findPreference("pref_reset_key_size");
        if (pref == null) return;

        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                SharedPreferences.Editor editor = getSharedPreferences().edit();
                editor.putString(PREF_KEY_SIZE_MODE, "uniform");
                editor.putInt(PREF_KEY_SIZE_SCALE, 100);
                editor.putInt(PREF_KEY_WIDTH_SCALE, 100);
                editor.putInt(PREF_KEY_HEIGHT_SCALE, 100);
                editor.apply();

                final ListPreference modePref = (ListPreference) findPreference(PREF_KEY_SIZE_MODE);
                if (modePref != null) {
                    modePref.setValue("uniform");
                    modePref.setSummary(modePref.getEntry());
                }

                updateKeySizeSummaries();
                Toast.makeText(getActivity(), "Key size configurations reset to defaults", Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }

    private void updateKeySizeSummaries() {
        final Preference sizePref = findPreference(PREF_KEY_SIZE_SCALE);
        if (sizePref != null) {
            int value = getSharedPreferences().getInt(PREF_KEY_SIZE_SCALE, 100);
            sizePref.setSummary("Current scale: " + value + "%");
        }
        final Preference widthPref = findPreference(PREF_KEY_WIDTH_SCALE);
        if (widthPref != null) {
            int value = getSharedPreferences().getInt(PREF_KEY_WIDTH_SCALE, 100);
            widthPref.setSummary("Current width scale: " + value + "%");
        }
        final Preference heightPref = findPreference(PREF_KEY_HEIGHT_SCALE);
        if (heightPref != null) {
            int value = getSharedPreferences().getInt(PREF_KEY_HEIGHT_SCALE, 100);
            heightPref.setSummary("Current height scale: " + value + "%");
        }
    }

    private void setupKeyGapXPreference() {
        final SeekBarDialogPreference pref = (SeekBarDialogPreference) findPreference(PREF_KEY_GAP_X);
        if (pref == null) return;

        final SharedPreferences prefs = getSharedPreferences();
        pref.setInterface(new SeekBarDialogPreference.ValueProxy() {
            @Override
            public void writeValue(final int value, final String key) {
                prefs.edit().putInt(key, value).apply();
                updateKeyGapSummaries();
            }

            @Override
            public void writeDefaultValue(final String key) {
                prefs.edit().remove(key).apply();
                updateKeyGapSummaries();
            }

            @Override
            public int readValue(final String key) {
                return prefs.getInt(key, 2);
            }

            @Override
            public int readDefaultValue(final String key) {
                return 2;
            }

            @Override
            public String getValueText(final int value) {
                return value + " dp";
            }

            @Override
            public void feedbackValue(final int value) {}
        });
    }

    private void setupKeyGapYPreference() {
        final SeekBarDialogPreference pref = (SeekBarDialogPreference) findPreference(PREF_KEY_GAP_Y);
        if (pref == null) return;

        final SharedPreferences prefs = getSharedPreferences();
        pref.setInterface(new SeekBarDialogPreference.ValueProxy() {
            @Override
            public void writeValue(final int value, final String key) {
                prefs.edit().putInt(key, value).apply();
                updateKeyGapSummaries();
            }

            @Override
            public void writeDefaultValue(final String key) {
                prefs.edit().remove(key).apply();
                updateKeyGapSummaries();
            }

            @Override
            public int readValue(final String key) {
                return prefs.getInt(key, 3);
            }

            @Override
            public int readDefaultValue(final String key) {
                return 3;
            }

            @Override
            public String getValueText(final int value) {
                return value + " dp";
            }

            @Override
            public void feedbackValue(final int value) {}
        });
    }

    private void setupKeyGapResetPreference() {
        final Preference pref = findPreference("pref_reset_key_gap");
        if (pref == null) return;

        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                SharedPreferences.Editor editor = getSharedPreferences().edit();
                editor.putInt(PREF_KEY_GAP_X, 2);
                editor.putInt(PREF_KEY_GAP_Y, 3);
                editor.apply();

                updateKeyGapSummaries();
                Toast.makeText(getActivity(), "Key spacing reset to defaults", Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }

    private void updateKeyGapSummaries() {
        final Preference gapXPref = findPreference(PREF_KEY_GAP_X);
        if (gapXPref != null) {
            int value = getSharedPreferences().getInt(PREF_KEY_GAP_X, 2);
            gapXPref.setSummary("Horizontal spacing: " + value + " dp");
        }
        final Preference gapYPref = findPreference(PREF_KEY_GAP_Y);
        if (gapYPref != null) {
            int value = getSharedPreferences().getInt(PREF_KEY_GAP_Y, 3);
            gapYPref.setSummary("Vertical spacing: " + value + " dp");
        }
    }

    private void setupImagePickerPreference() {
        final Preference pref = findPreference("pref_select_key_bg_image");
        if (pref == null) return;

        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                try {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    startActivityForResult(Intent.createChooser(intent, "Select Key Background"), REQUEST_IMAGE_PICK);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to start image picker", e);
                    Toast.makeText(getActivity(), "No file picker available", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });
    }

    private void setupClearImagePreference() {
        final Preference pref = findPreference("pref_clear_key_bg_image");
        if (pref == null) return;

        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                File file = new File(getActivity().getFilesDir(), "key_background_custom.png");
                if (file.exists()) {
                    file.delete();
                }
                getSharedPreferences().edit().putBoolean(PREF_KEY_BG_IMAGE_ACTIVE, false).apply();
                updateImagePreferenceSummary();
                Toast.makeText(getActivity(), "Custom key background image cleared", Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }

    private void setupKbdImagePickerPreference() {
        final Preference pref = findPreference("pref_select_kbd_bg_image");
        if (pref == null) return;

        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                try {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    startActivityForResult(Intent.createChooser(intent, "Select Keyboard Background"), REQUEST_KBD_IMAGE_PICK);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to start keyboard image picker", e);
                    Toast.makeText(getActivity(), "No file picker available", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });
    }

    private void setupKbdBgStylePreference() {
        final ListPreference pref = (ListPreference) findPreference(PREF_KBD_BG_IMAGE_STYLE);
        if (pref == null) return;

        pref.setSummary(pref.getEntry());
        pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int index = pref.findIndexOfValue((String) newValue);
                if (index >= 0) {
                    preference.setSummary(pref.getEntries()[index]);
                }
                return true;
            }
        });
    }

    private void setupKbdClearImagePreference() {
        final Preference pref = findPreference("pref_clear_kbd_bg_image");
        if (pref == null) return;

        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                File file = new File(getActivity().getFilesDir(), "keyboard_background_custom.png");
                if (file.exists()) {
                    file.delete();
                }
                getSharedPreferences().edit().putBoolean(PREF_KBD_BG_IMAGE_ACTIVE, false).apply();
                updateKbdImagePreferenceSummary();
                Toast.makeText(getActivity(), "Custom keyboard background image cleared", Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }

    private void updateImagePreferenceSummary() {
        final Preference pref = findPreference("pref_select_key_bg_image");
        if (pref == null) return;

        boolean active = getSharedPreferences().getBoolean(PREF_KEY_BG_IMAGE_ACTIVE, false);
        if (active) {
            File file = new File(getActivity().getFilesDir(), "key_background_custom.png");
            if (file.exists()) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(file.getAbsolutePath(), options);
                pref.setSummary("Active: Custom Background (" + options.outWidth + "x" + options.outHeight + " px)");
            } else {
                pref.setSummary("No image selected");
            }
        } else {
            pref.setSummary("Pick a small image for key backgrounds. Recommended size: 128x128 px (max 256x256 px) to keep memory usage low.");
        }
    }

    private void updateKbdImagePreferenceSummary() {
        final Preference pref = findPreference("pref_select_kbd_bg_image");
        if (pref == null) return;

        boolean active = getSharedPreferences().getBoolean(PREF_KBD_BG_IMAGE_ACTIVE, false);
        if (active) {
            File file = new File(getActivity().getFilesDir(), "keyboard_background_custom.png");
            if (file.exists()) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(file.getAbsolutePath(), options);
                pref.setSummary("Active: Custom Keyboard Background (" + options.outWidth + "x" + options.outHeight + " px)");
            } else {
                pref.setSummary("No image selected");
            }
        } else {
            pref.setSummary("Choose an image for the entire keyboard background. Recommended max dimension: 512 px.");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) {
            return;
        }

        Uri imageUri = data.getData();
        if (requestCode == REQUEST_IMAGE_PICK) {
            try {
                InputStream is = getActivity().getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                if (is != null) {
                    is.close();
                }
                if (bitmap == null) {
                    Toast.makeText(getActivity(), "Failed to decode image", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Scale down bitmap to prevent high memory usage. Limit max dimension to 128 px.
                Bitmap scaledBitmap = scaleBitmap(bitmap, 128);

                File file = new File(getActivity().getFilesDir(), "key_background_custom.png");
                FileOutputStream fos = new FileOutputStream(file);
                scaledBitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
                fos.close();

                getSharedPreferences().edit().putBoolean(PREF_KEY_BG_IMAGE_ACTIVE, true).apply();
                updateImagePreferenceSummary();

                Toast.makeText(getActivity(), "Custom key background image saved!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Failed to copy background image", e);
                Toast.makeText(getActivity(), "Failed to process image", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_KBD_IMAGE_PICK) {
            try {
                InputStream is = getActivity().getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                if (is != null) {
                    is.close();
                }
                if (bitmap == null) {
                    Toast.makeText(getActivity(), "Failed to decode image", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Scale down bitmap to prevent high memory usage. Limit max dimension to 512 px.
                Bitmap scaledBitmap = scaleBitmap(bitmap, 512);

                File file = new File(getActivity().getFilesDir(), "keyboard_background_custom.png");
                FileOutputStream fos = new FileOutputStream(file);
                scaledBitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
                fos.close();

                getSharedPreferences().edit().putBoolean(PREF_KBD_BG_IMAGE_ACTIVE, true).apply();
                updateKbdImagePreferenceSummary();

                Toast.makeText(getActivity(), "Custom keyboard background image saved!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Failed to copy keyboard background image", e);
                Toast.makeText(getActivity(), "Failed to process image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Bitmap scaleBitmap(Bitmap src, int maxDim) {
        int width = src.getWidth();
        int height = src.getHeight();
        if (width <= maxDim && height <= maxDim) {
            return src;
        }
        float ratio = (float) width / (float) height;
        int newWidth, newHeight;
        if (width > height) {
            newWidth = maxDim;
            newHeight = Math.round(maxDim / ratio);
        } else {
            newHeight = maxDim;
            newWidth = Math.round(maxDim * ratio);
        }
        return Bitmap.createScaledBitmap(src, newWidth, newHeight, true);
    }
}
