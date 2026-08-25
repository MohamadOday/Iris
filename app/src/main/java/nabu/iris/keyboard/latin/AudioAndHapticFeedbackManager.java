/*
 * Copyright (C) 2012 The Android Open Source Project
 * Copyright (C) 2025 Raimondas Rimkus
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

package nabu.iris.keyboard.latin;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import nabu.iris.keyboard.R;
import nabu.iris.keyboard.compat.PreferenceManagerCompat;
import nabu.iris.keyboard.latin.common.Constants;
import nabu.iris.keyboard.latin.settings.Settings;
import nabu.iris.keyboard.latin.settings.SettingsValues;

/**
 * Robust audio and haptic feedback manager for Iris Keyboard.
 * Supports Apple iOS sampled clicks, custom Mechvibes soundpacks, audio slicing, and varied acoustic key sounds.
 */
public final class AudioAndHapticFeedbackManager {
    private static final String TAG = "AudioHapticFeedback";
    private static final long TICK_FREQUENCY = 100;

    private ExecutorService mBackgroundThread;
    private AudioManager mAudioManager;
    private Vibrator mVibrator;

    private SoundPool mSoundPool;
    private int mSoundStandard = -1;
    private int mSoundSpacebar = -1;
    private int mSoundDelete = -1;
    private int mSoundReturn = -1;

    private final Map<Integer, Integer> mSoundMap = new HashMap<>();
    private final List<Integer> mGeneralSoundList = new ArrayList<>();

    private SettingsValues mSettingsValues;
    private boolean mSoundOn;
    private long mLastTickTime = 0;

    private Context mContext;
    private String mCurrentSoundpackName = "";

    private static final AudioAndHapticFeedbackManager sInstance =
            new AudioAndHapticFeedbackManager();

    public static AudioAndHapticFeedbackManager getInstance() {
        return sInstance;
    }

    private AudioAndHapticFeedbackManager() {
        // Singleton
    }

    public static void init(final Context context) {
        sInstance.initInternal(context);
    }

    private void initInternal(final Context context) {
        if (mContext == null) {
            mContext = context.getApplicationContext();
        }
        if (mAudioManager == null && mContext != null) {
            mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        }
        if (mVibrator == null && mContext != null) {
            mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        }
        if (mBackgroundThread == null) {
            mBackgroundThread = Executors.newSingleThreadExecutor();
        }

        if (mSoundPool == null) {
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            mSoundPool = new SoundPool.Builder()
                    .setMaxStreams(8)
                    .setAudioAttributes(attrs)
                    .build();

            mBackgroundThread.execute(() -> {
                android.content.SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(mContext);
                String soundpack = Settings.readKeyboardSoundpack(prefs);
                mCurrentSoundpackName = soundpack;
                loadSoundpack(mContext, soundpack);
            });
        }
    }

    private synchronized void loadSoundpack(final Context context, final String soundpackName) {
        if (mSoundPool == null) {
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            mSoundPool = new SoundPool.Builder()
                    .setMaxStreams(8)
                    .setAudioAttributes(attrs)
                    .build();
        }

        // Reset previous sound IDs
        mSoundStandard = -1;
        mSoundSpacebar = -1;
        mSoundDelete = -1;
        mSoundReturn = -1;
        mSoundMap.clear();
        mGeneralSoundList.clear();

        File soundpacksDir = context != null ? context.getExternalFilesDir("soundpacks") : null;
        if (soundpacksDir != null && soundpackName != null && !soundpackName.isEmpty()
                && !soundpackName.equals("default") && !soundpackName.equals("default_deep")) {
            File packDir = new File(soundpacksDir, soundpackName);
            if (packDir.exists() && packDir.isDirectory()) {
                File[] files = packDir.listFiles();
                if (files != null && files.length > 0) {
                    for (File file : files) {
                        String name = file.getName().toLowerCase();
                        if (name.endsWith(".wav")) {
                            try {
                                String baseName = name.substring(0, name.length() - 4);
                                int sid = mSoundPool.load(file.getAbsolutePath(), 1);
                                if (sid > 0) {
                                    mGeneralSoundList.add(sid);

                                    // Check if integer scancode
                                    try {
                                        int keyCode = Integer.parseInt(baseName);
                                        if (keyCode > 10) {
                                            mSoundMap.put(keyCode, sid);
                                        }
                                    } catch (NumberFormatException ignored) {}

                                    // Check named key sounds
                                    if (name.equals("standard.wav") || name.contains("standard") || name.contains("press") || name.contains("click")) {
                                        if (mSoundStandard == -1) mSoundStandard = sid;
                                    }
                                    if (name.equals("spacebar.wav") || name.contains("space")) {
                                        if (mSoundSpacebar == -1) mSoundSpacebar = sid;
                                    }
                                    if (name.equals("delete.wav") || name.contains("delete") || name.contains("backspace")) {
                                        if (mSoundDelete == -1) mSoundDelete = sid;
                                    }
                                    if (name.equals("return.wav") || name.contains("return") || name.contains("enter")) {
                                        if (mSoundReturn == -1) mSoundReturn = sid;
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Failed loading sound: " + file.getName(), e);
                            }
                        }
                    }

                    // Assign fallbacks within the custom pack
                    if (mSoundStandard == -1 && !mGeneralSoundList.isEmpty()) {
                        mSoundStandard = mGeneralSoundList.get(0);
                    }
                    if (mSoundSpacebar == -1) {
                        mSoundSpacebar = mSoundMap.containsKey(57) ? mSoundMap.get(57) : mSoundStandard;
                    }
                    if (mSoundDelete == -1) {
                        mSoundDelete = mSoundMap.containsKey(14) ? mSoundMap.get(14) : mSoundStandard;
                    }
                    if (mSoundReturn == -1) {
                        mSoundReturn = mSoundMap.containsKey(28) ? mSoundMap.get(28) : mSoundStandard;
                    }
                }
            }
        }

        // Built-in Apple iOS Sampled Soundpacks (res/raw/keypress_*.ogg)
        if ("default_deep".equals(soundpackName)) {
            if (mSoundStandard == -1) mSoundStandard = loadSound(context, "keypress_standard_deep");
            if (mSoundSpacebar == -1) mSoundSpacebar = loadSound(context, "keypress_spacebar_deep");
            if (mSoundDelete == -1) mSoundDelete = loadSound(context, "keypress_delete_deep");
            if (mSoundReturn == -1) mSoundReturn = loadSound(context, "keypress_return_deep");
        } else {
            if (mSoundStandard == -1) mSoundStandard = loadSound(context, "keypress_standard");
            if (mSoundSpacebar == -1) mSoundSpacebar = loadSound(context, "keypress_spacebar");
            if (mSoundDelete == -1) mSoundDelete = loadSound(context, "keypress_delete");
            if (mSoundReturn == -1) mSoundReturn = loadSound(context, "keypress_return");
        }

        // Final safety fallbacks
        if (mSoundSpacebar == -1) mSoundSpacebar = mSoundStandard;
        if (mSoundDelete == -1) mSoundDelete = mSoundStandard;
        if (mSoundReturn == -1) mSoundReturn = mSoundStandard;
    }

    private int loadSound(final Context context, final String name) {
        if (context == null || mSoundPool == null) return -1;
        int resId = context.getResources().getIdentifier(name, "raw", context.getPackageName());
        if (resId != 0) {
            return mSoundPool.load(context, resId, 1);
        }
        return -1;
    }

    public boolean hasVibrator() {
        if (mVibrator == null && mContext != null) {
            mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        }
        return mVibrator != null && mVibrator.hasVibrator();
    }

    public boolean hasSound() {
        return mSoundPool != null;
    }

    private boolean reevaluateIfSoundIsOn() {
        if (mSettingsValues == null || !mSettingsValues.mSoundOn) {
            return false;
        }
        if (mAudioManager != null && mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT) {
            return false;
        }
        return true;
    }

    public static int getScancodeFromCode(int code) {
        switch (code) {
            case Constants.CODE_DELETE: return 14;
            case Constants.CODE_ENTER: return 28;
            case Constants.CODE_SHIFT_ENTER: return 28;
            case Constants.CODE_SPACE: return 57;
            case Constants.CODE_TAB: return 15;
            case Constants.CODE_SHIFT: return 42;
            case Constants.CODE_CAPSLOCK: return 58;

            case '1': return 2;
            case '2': return 3;
            case '3': return 4;
            case '4': return 5;
            case '5': return 6;
            case '6': return 7;
            case '7': return 8;
            case '8': return 9;
            case '9': return 10;
            case '0': return 11;

            case 'q': case 'Q': return 16;
            case 'w': case 'W': return 17;
            case 'e': case 'E': return 18;
            case 'r': case 'R': return 19;
            case 't': case 'T': return 20;
            case 'y': case 'Y': return 21;
            case 'u': case 'U': return 22;
            case 'i': case 'I': return 23;
            case 'o': case 'O': return 24;
            case 'p': case 'P': return 25;
            case 'a': case 'A': return 30;
            case 's': case 'S': return 31;
            case 'd': case 'D': return 32;
            case 'f': case 'F': return 33;
            case 'g': case 'G': return 34;
            case 'h': case 'H': return 35;
            case 'j': case 'J': return 36;
            case 'k': case 'K': return 37;
            case 'l': case 'L': return 38;
            case 'z': case 'Z': return 44;
            case 'x': case 'X': return 45;
            case 'c': case 'C': return 46;
            case 'v': case 'V': return 47;
            case 'b': case 'B': return 48;
            case 'n': case 'N': return 49;
            case 'm': case 'M': return 50;

            case '-': return 12;
            case '=': return 13;
            case '[': return 26;
            case ']': return 27;
            case ';': return 39;
            case '\'': return 40;
            case '`': return 41;
            case '\\': return 43;
            case ',': return 51;
            case '.': return 52;
            case '/': return 53;

            default: return 30;
        }
    }

    public void performAudioFeedback(final int code) {
        if (!mSoundOn) {
            return;
        }

        final float volume = (mSettingsValues != null && mSettingsValues.mKeypressSoundVolume > 0f)
                ? mSettingsValues.mKeypressSoundVolume : 0.5f;

        if (mSoundPool != null) {
            int soundId = -1;

            // 1. Direct Scancode Match
            int scancode = getScancodeFromCode(code);
            if (mSoundMap.containsKey(scancode)) {
                soundId = mSoundMap.get(scancode);
            }

            // 2. Special Key Matching
            if (soundId == -1 || soundId == 0) {
                if (code == Constants.CODE_SPACE && mSoundSpacebar > 0) {
                    soundId = mSoundSpacebar;
                } else if (code == Constants.CODE_DELETE && mSoundDelete > 0) {
                    soundId = mSoundDelete;
                } else if (code == Constants.CODE_ENTER && mSoundReturn > 0) {
                    soundId = mSoundReturn;
                }
            }

            // 3. Varied General Key Sound from pack
            if ((soundId == -1 || soundId == 0) && !mGeneralSoundList.isEmpty()) {
                int index = (Math.abs(code) ^ (scancode * 31)) % mGeneralSoundList.size();
                soundId = mGeneralSoundList.get(index);
            }

            // 4. Standard Pack Fallback (Apple iOS click or Custom pack standard)
            if ((soundId == -1 || soundId == 0) && mSoundStandard > 0) {
                soundId = mSoundStandard;
            }

            if (soundId > 0) {
                final int finalSoundId = soundId;
                if (mBackgroundThread != null) {
                    mBackgroundThread.execute(() -> {
                        try {
                            mSoundPool.play(finalSoundId, volume, volume, 1, 0, 1.0f);
                        } catch (Exception ignored) {}
                    });
                }
                return;
            }
        }

        // 5. System Audio Fallback
        final int soundType;
        switch (code) {
            case Constants.CODE_DELETE:
                soundType = AudioManager.FX_KEYPRESS_DELETE;
                break;
            case Constants.CODE_ENTER:
                soundType = AudioManager.FX_KEYPRESS_RETURN;
                break;
            case Constants.CODE_SPACE:
                soundType = AudioManager.FX_KEYPRESS_SPACEBAR;
                break;
            default:
                soundType = AudioManager.FX_KEYPRESS_STANDARD;
                break;
        }
        playSoundEffect(soundType, volume);
    }

    public void playSoundEffect(final int effectType, final float volume) {
        if (mSoundPool == null) {
            if (mAudioManager != null && mBackgroundThread != null) {
                mBackgroundThread.execute(() -> {
                    try {
                        mAudioManager.playSoundEffect(effectType, volume);
                    } catch (Exception ignored) {}
                });
            }
            return;
        }

        int soundId = mSoundStandard;
        switch (effectType) {
            case AudioManager.FX_KEYPRESS_DELETE:
                if (mSoundDelete > 0) soundId = mSoundDelete;
                break;
            case AudioManager.FX_KEYPRESS_RETURN:
                if (mSoundReturn > 0) soundId = mSoundReturn;
                break;
            case AudioManager.FX_KEYPRESS_SPACEBAR:
                if (mSoundSpacebar > 0) soundId = mSoundSpacebar;
                break;
        }

        if (soundId > 0) {
            final int finalSoundId = soundId;
            if (mBackgroundThread != null) {
                mBackgroundThread.execute(() -> {
                    try {
                        mSoundPool.play(finalSoundId, volume, volume, 1, 0, 1.0f);
                    } catch (Exception ignored) {}
                });
            }
        }
    }

    public void performHapticFeedback(final View viewToPerformHapticFeedbackOn) {
        if (mSettingsValues == null || !mSettingsValues.mVibrateOn) {
            return;
        }
        if (mVibrator == null && mContext != null) {
            mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        }
        if (mVibrator == null || !mVibrator.hasVibrator()) {
            if (viewToPerformHapticFeedbackOn != null) {
                viewToPerformHapticFeedbackOn.performHapticFeedback(
                        HapticFeedbackConstants.KEYBOARD_TAP,
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
            }
            return;
        }

        if (mBackgroundThread != null) {
            mBackgroundThread.execute(() -> {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        mVibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        mVibrator.vibrate(20);
                    }
                } catch (Exception e) {
                    try {
                        if (viewToPerformHapticFeedbackOn != null) {
                            viewToPerformHapticFeedbackOn.performHapticFeedback(
                                    HapticFeedbackConstants.KEYBOARD_TAP,
                                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                        }
                    } catch (Exception ignored) {}
                }
            });
        }
    }

    public void performTickFeedback() {
        if (mSettingsValues == null || !mSettingsValues.mVibrateOn
                || System.currentTimeMillis() - mLastTickTime < TICK_FREQUENCY) {
            return;
        }
        if (mVibrator == null && mContext != null) {
            mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        }
        if (mVibrator == null || !mVibrator.hasVibrator()) {
            return;
        }

        mLastTickTime = System.currentTimeMillis();
        if (mBackgroundThread != null) {
            mBackgroundThread.execute(() -> {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        mVibrator.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        mVibrator.vibrate(10);
                    }
                } catch (Exception ignored) {}
            });
        }
    }

    public void onSettingsChanged(final SettingsValues settingsValues) {
        mSettingsValues = settingsValues;
        mSoundOn = reevaluateIfSoundIsOn();

        if (settingsValues != null && settingsValues.mSoundpackName != null) {
            boolean shouldReload = !settingsValues.mSoundpackName.equals(mCurrentSoundpackName) || mSoundStandard <= 0;
            if (shouldReload) {
                mCurrentSoundpackName = settingsValues.mSoundpackName;
                if (mContext != null && mBackgroundThread != null) {
                    mBackgroundThread.execute(() -> {
                        loadSoundpack(mContext, mCurrentSoundpackName);
                    });
                }
            }
        }
    }

    public void onRingerModeChanged() {
        mSoundOn = reevaluateIfSoundIsOn();
    }
}
