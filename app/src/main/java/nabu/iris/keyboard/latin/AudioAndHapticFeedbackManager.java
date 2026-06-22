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
        if (mBackgroundThread == null) {
            mBackgroundThread = Executors.newSingleThreadExecutor();
        }
        if (mSoundPool == null) {
            mBackgroundThread.execute(() -> {
                ensureBuiltinSoundpacksGenerated(mContext);

                mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
                mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);

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

    private void ensureBuiltinSoundpacksGenerated(final Context context) {
        try {
            File soundpacksDir = context.getExternalFilesDir("soundpacks");
            if (soundpacksDir == null) return;
            if (!soundpacksDir.exists()) {
                soundpacksDir.mkdirs();
            }

            // Create default soundpacks
            String[] packs = {"Cherry_MX_Blue", "Retro_Typewriter", "Bubble_Wrap", "Sci-Fi_Synth"};
            int[] packIds = {
                KeySoundSynthesizer.SOUNDPACK_CHERRY,
                KeySoundSynthesizer.SOUNDPACK_TYPEWRITER,
                KeySoundSynthesizer.SOUNDPACK_BUBBLE,
                KeySoundSynthesizer.SOUNDPACK_SCIFI
            };

            for (int p = 0; p < packs.length; p++) {
                File packDir = new File(soundpacksDir, packs[p]);
                if (!packDir.exists()) {
                    packDir.mkdirs();
                }

                String[] keyTypes = {"standard", "spacebar", "delete", "return"};
                for (String keyType : keyTypes) {
                    File file = new File(packDir, keyType + ".wav");
                    if (!file.exists()) {
                        short[] pcm = KeySoundSynthesizer.synthesize(packIds[p], keyType);
                        KeySoundSynthesizer.writeWavFile(file, pcm, 44100);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("AudioFeedback", "Error generating builtin soundpacks", e);
        }
    }

    private void loadSoundpack(final Context context, final String soundpack) {
        if (mSoundPool != null) {
            // Unload all scancode-specific sounds
            for (int soundId : mSoundMap.values()) {
                if (soundId != -1) {
                    mSoundPool.unload(soundId);
                }
            }
            mSoundMap.clear();

            if (mSoundStandard != -1) mSoundPool.unload(mSoundStandard);
            if (mSoundSpacebar != -1) mSoundPool.unload(mSoundSpacebar);
            if (mSoundDelete != -1) mSoundPool.unload(mSoundDelete);
            if (mSoundReturn != -1) mSoundPool.unload(mSoundReturn);
        }

        mSoundStandard = -1;
        mSoundSpacebar = -1;
        mSoundDelete = -1;
        mSoundReturn = -1;

        if (soundpack == null || soundpack.equals("default")) {
            mSoundStandard = loadSound(context, "keypress_standard");
            mSoundSpacebar = loadSound(context, "keypress_spacebar");
            mSoundDelete = loadSound(context, "keypress_delete");
            mSoundReturn = loadSound(context, "keypress_return");

            // Populate mapping for standard iOS profile
            mSoundMap.put(14, mSoundDelete); // backspace
            mSoundMap.put(28, mSoundReturn); // enter
            mSoundMap.put(57, mSoundSpacebar); // spacebar
            mSoundMap.put(15, mSoundSpacebar); // tab -> Alternate
            mSoundMap.put(42, mSoundSpacebar); // shift left -> Alternate
            mSoundMap.put(54, mSoundSpacebar); // shift right -> Alternate
            mSoundMap.put(29, mSoundSpacebar); // ctrl left -> Alternate
            mSoundMap.put(56, mSoundSpacebar); // alt left -> Alternate
        } else if (soundpack.equals("default_deep")) {
            mSoundStandard = loadSound(context, "keypress_standard_deep");
            mSoundSpacebar = loadSound(context, "keypress_spacebar_deep");
            mSoundDelete = loadSound(context, "keypress_delete_deep");
            mSoundReturn = loadSound(context, "keypress_return_deep");

            // Populate mapping for iOS Deep profile
            mSoundMap.put(14, mSoundDelete); // backspace
            mSoundMap.put(28, mSoundReturn); // enter
            mSoundMap.put(57, mSoundSpacebar); // spacebar
            mSoundMap.put(15, mSoundSpacebar); // tab -> Alternate
            mSoundMap.put(42, mSoundSpacebar); // shift left -> Alternate
            mSoundMap.put(54, mSoundSpacebar); // shift right -> Alternate
            mSoundMap.put(29, mSoundSpacebar); // ctrl left -> Alternate
            mSoundMap.put(56, mSoundSpacebar); // alt left -> Alternate
        } else {
            File soundpacksDir = context.getExternalFilesDir("soundpacks");
            if (soundpacksDir != null) {
                File packDir = new File(soundpacksDir, soundpack);
                if (packDir.exists() && packDir.isDirectory()) {
                    mSoundStandard = loadWavFile(packDir, "standard");
                    mSoundSpacebar = loadWavFile(packDir, "spacebar");
                    mSoundDelete = loadWavFile(packDir, "delete");
                    mSoundReturn = loadWavFile(packDir, "return");

                    // Scan the folder for <scancode>.wav files and load them
                    File[] files = packDir.listFiles();
                    if (files != null) {
                        for (File f : files) {
                            String name = f.getName();
                            if (name.endsWith(".wav")) {
                                String base = name.substring(0, name.length() - 4);
                                try {
                                    int scancode = Integer.parseInt(base);
                                    int soundId = mSoundPool.load(f.getAbsolutePath(), 1);
                                    mSoundMap.put(scancode, soundId);
                                } catch (NumberFormatException e) {
                                    // Ignore non-numeric filenames
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private int loadWavFile(File directory, String keyType) {
        String[] extensions = {".wav", ".ogg", ".mp3"};
        for (String ext : extensions) {
            File file = new File(directory, keyType + ext);
            if (file.exists()) {
                return mSoundPool.load(file.getAbsolutePath(), 1);
            }
        }
        return -1;
    }

    private int loadSound(final Context context, final String name) {
        int resId = context.getResources().getIdentifier(name, "raw", context.getPackageName());
        if (resId != 0) {
            return mSoundPool.load(context, resId, 1);
        }
        return -1;
    }

    public boolean hasVibrator() {
        return mVibrator != null && mVibrator.hasVibrator();
    }

    private boolean reevaluateIfSoundIsOn() {
        if (mSettingsValues == null || !mSettingsValues.mSoundOn || mAudioManager == null) {
            return false;
        }
        return mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL;
    }

    public static int getScancodeFromCode(int code) {
        switch (code) {
            // Special keys
            case Constants.CODE_DELETE: return 14;
            case Constants.CODE_ENTER: return 28;
            case Constants.CODE_SHIFT_ENTER: return 28;
            case Constants.CODE_SPACE: return 57;
            case Constants.CODE_TAB: return 15;
            case Constants.CODE_SHIFT: return 42;
            case Constants.CODE_CAPSLOCK: return 58;

            // Numbers
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

            // Letters
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

            // Symbols
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

            default: return 30; // Fallback to standard key 'A'
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

        // Fallback to legacy playSoundEffect
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
        if (!mSettingsValues.mVibrateOn || mVibrator == null) {
            return;
        }
        mBackgroundThread.execute(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                mVibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
            } else if (viewToPerformHapticFeedbackOn != null) {
                viewToPerformHapticFeedbackOn.performHapticFeedback(
                        HapticFeedbackConstants.KEYBOARD_TAP,
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
            }
        });
    }

    public void performTickFeedback() {
        if (!mSettingsValues.mVibrateOn
                || mVibrator == null
                || System.currentTimeMillis() - mLastTickTime < TICK_FREQUENCY ) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mLastTickTime = System.currentTimeMillis();
            mBackgroundThread.execute(() -> {
                mVibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK));
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
