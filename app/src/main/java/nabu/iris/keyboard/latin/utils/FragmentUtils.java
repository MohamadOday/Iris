/*
 * Copyright (C) 2013 The Android Open Source Project
 * Copyright (C) 2021 wittmane
 * Copyright (C) 2017 Raimondas Rimkus
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

package nabu.iris.keyboard.latin.utils;

import java.util.HashSet;

import nabu.iris.keyboard.latin.settings.AiSettingsFragment;
import nabu.iris.keyboard.latin.settings.TranslationSettingsFragment;
import nabu.iris.keyboard.latin.settings.AppearanceSettingsFragment;
import nabu.iris.keyboard.latin.settings.KeyPressSettingsFragment;
import nabu.iris.keyboard.latin.settings.LanguagesSettingsFragment;
import nabu.iris.keyboard.latin.settings.PreferencesSettingsFragment;
import nabu.iris.keyboard.latin.settings.SettingsFragment;
import nabu.iris.keyboard.latin.settings.SingleLanguageSettingsFragment;
import nabu.iris.keyboard.latin.settings.ThemeSettingsFragment;

public class FragmentUtils {
    private static final HashSet<String> sLatinImeFragments = new HashSet<>();
    static {
        sLatinImeFragments.add(PreferencesSettingsFragment.class.getName());
        sLatinImeFragments.add(KeyPressSettingsFragment.class.getName());
        sLatinImeFragments.add(AppearanceSettingsFragment.class.getName());
        sLatinImeFragments.add(ThemeSettingsFragment.class.getName());
        sLatinImeFragments.add(SettingsFragment.class.getName());
        sLatinImeFragments.add(LanguagesSettingsFragment.class.getName());
        sLatinImeFragments.add(SingleLanguageSettingsFragment.class.getName());
        sLatinImeFragments.add(AiSettingsFragment.class.getName());
        sLatinImeFragments.add(TranslationSettingsFragment.class.getName());
    }

    public static boolean isValidFragment(String fragmentName) {
        return sLatinImeFragments.contains(fragmentName);
    }
}
