/*
 * Copyright (C) 2012 The Android Open Source Project
 * Copyright (C) 2025 Raimondas Rimkus
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
import android.view.HapticFeedbackConstants;
import android.view.View;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.util.Log;
import java.io.File;
import nabu.iris.keyboard.latin.settings.Settings;
import nabu.iris.keyboard.latin.common.Constants;
import nabu.iris.keyboard.latin.settings.SettingsValues;

/**
 * This class gathers audio feedback and haptic feedback functions.
 *
 * It offers a consistent and simple interface that allows LatinIME to forget about the
 * complexity of settings and the like.
 */
public final class AudioAndHapticFeedbackManager {
    private static final long TICK_FREQUENCY = 100;
    private ExecutorService mBackgroundThread;
    private AudioManager mAudioManager;
    private Vibrator mVibrator;

    private SoundPool mSoundPool;
    private int mSoundStandard = -1;
    private int mSoundSpacebar = -1;
    private int mSoundDelete = -1;
    private int mSoundReturn = -1;
    private final java.util.Map<Integer, Integer> mSoundMap = new java.util.HashMap<>();

    private SettingsValues mSettingsValues;
    private boolean mSoundOn;
    private long mLastTickTime = 0;

    private static final AudioAndHapticFeedbackManager sInstance =
            new AudioAndHapticFeedbackManager();

    public static AudioAndHapticFeedbackManager getInstance() {
        return sInstance;
    }

    private AudioAndHapticFeedbackManager() {
        // Intentional empty constructor for singleton.
    }

    public static void init(final Context context) {
        sInstance.initInternal(context);
    }

    private Context mContext;
    private String mCurrentSoundpackName = "";

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
            mBackgroundThread.execute(() -> {
                AudioAttributes attrs = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();
                mSoundPool = new SoundPool.Builder()
                        .setMaxStreams(4)
                        .setAudioAttributes(attrs)
                        .build();

                // Load the correct soundpack from shared preferences
                android.content.SharedPreferences prefs = nabu.iris.keyboard.compat.PreferenceManagerCompat.getDeviceSharedPreferences(mContext);
                String soundpack = Settings.readKeyboardSoundpack(prefs);
                mCurrentSoundpackName = soundpack;
                loadSoundpack(mContext, soundpack);
            });
        }
    }

    private void loadSoundpack(final Context context, final String soundpackName) {
        if (mSoundPool == null) return;

        // Reset previous sound IDs
        mSoundStandard = -1;
        mSoundSpacebar = -1;
        mSoundDelete = -1;
        mSoundReturn = -1;
        mSoundMap.clear();

        File soundpacksDir = context.getExternalFilesDir("soundpacks");
        if (soundpacksDir != null) {
            File packDir = new File(soundpacksDir, soundpackName);
            if (packDir.exists() && packDir.isDirectory()) {
                File[] files = packDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        String name = file.getName();
                        if (name.endsWith(".wav")) {
                            try {
                                String baseName = name.substring(0, name.length() - 4);
                                int keyCode = Integer.parseInt(baseName);
                                int sid = mSoundPool.load(file.getAbsolutePath(), 1);
                                mSoundMap.put(keyCode, sid);
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                }

                File standardFile = new File(packDir, "standard.wav");
                File spacebarFile = new File(packDir, "spacebar.wav");
                File deleteFile = new File(packDir, "delete.wav");
                File returnFile = new File(packDir, "return.wav");

                if (standardFile.exists()) {
                    mSoundStandard = mSoundPool.load(standardFile.getAbsolutePath(), 1);
                }
                if (spacebarFile.exists()) {
                    mSoundSpacebar = mSoundPool.load(spacebarFile.getAbsolutePath(), 1);
                }
                if (deleteFile.exists()) {
                    mSoundDelete = mSoundPool.load(deleteFile.getAbsolutePath(), 1);
                }
                if (returnFile.exists()) {
                    mSoundReturn = mSoundPool.load(returnFile.getAbsolutePath(), 1);
                }
            }
        }

        // Fallback to built-in raw resources if custom pack files not loaded
        if (mSoundStandard == -1) mSoundStandard = loadSound(context, "fx_keypress_standard");
        if (mSoundSpacebar == -1) mSoundSpacebar = loadSound(context, "fx_keypress_spacebar");
        if (mSoundDelete == -1) mSoundDelete = loadSound(context, "fx_keypress_delete");
        if (mSoundReturn == -1) mSoundReturn = loadSound(context, "fx_keypress_return");

        if (mSoundStandard == -1) mSoundStandard = loadSound(context, "fx_standard");
        if (mSoundSpacebar == -1) mSoundSpacebar = loadSound(context, "fx_spacebar");
        if (mSoundDelete == -1) mSoundDelete = loadSound(context, "fx_delete");
        if (mSoundReturn == -1) mSoundReturn = loadSound(context, "fx_return");

        if (mSoundSpacebar == -1) mSoundSpacebar = mSoundStandard;
        if (mSoundDelete == -1) mSoundDelete = mSoundStandard;
        if (mSoundReturn == -1) mSoundReturn = mSoundStandard;
    }

    private int loadSound(final Context context, final String name) {
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
        if (mSettingsValues == null || !mSettingsValues.mSoundOn || mAudioManager == null) {
            return false;
        }
        return mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL;
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

        int scancode = getScancodeFromCode(code);
        if (mSoundPool != null) {
            Integer soundIdObj = mSoundMap.get(scancode);
            if (soundIdObj != null && soundIdObj != -1) {
                float volume = mSettingsValues.mKeypressSoundVolume;
                mBackgroundThread.execute(() -> {
                    mSoundPool.play(soundIdObj, volume, volume, 1, 0, 1.0f);
                });
                return;
            }
        }

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
        playSoundEffect(soundType, mSettingsValues.mKeypressSoundVolume);
    }

    public void playSoundEffect(final int effectType, final float volume) {
        if (mSoundPool == null) {
            if (mAudioManager != null) {
                mBackgroundThread.execute(() -> {
                    mAudioManager.playSoundEffect(effectType, volume);
                });
            }
            return;
        }

        final int soundId;
        switch (effectType) {
        case AudioManager.FX_KEYPRESS_DELETE:
            soundId = mSoundDelete;
            break;
        case AudioManager.FX_KEYPRESS_RETURN:
            soundId = mSoundReturn;
            break;
        case AudioManager.FX_KEYPRESS_SPACEBAR:
            soundId = mSoundSpacebar;
            break;
        default:
            soundId = mSoundStandard;
            break;
        }

        if (soundId != -1) {
            mBackgroundThread.execute(() -> {
                mSoundPool.play(soundId, volume, volume, 1, 0, 1.0f);
            });
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
            boolean shouldReload = !settingsValues.mSoundpackName.equals(mCurrentSoundpackName);
            if (!shouldReload && !settingsValues.mSoundpackName.equals("default") && !settingsValues.mSoundpackName.equals("default_deep")) {
                if (mSoundStandard == -1) {
                    File soundpacksDir = mContext != null ? mContext.getExternalFilesDir("soundpacks") : null;
                    if (soundpacksDir != null) {
                        File packDir = new File(soundpacksDir, settingsValues.mSoundpackName);
                        if (packDir.exists() && packDir.isDirectory() && new File(packDir, "standard.wav").exists()) {
                            shouldReload = true;
                        }
                    }
                }
            }

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
