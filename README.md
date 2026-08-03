# Iris Keyboard

[![License](https://img.shields.io/badge/license-Apache--2.0-blue)](LICENSE)
[![Android](https://img.shields.io/badge/Android-7.0%2B-green)](https://github.com/MohamadOday/Iris/releases)
[![Target SDK](https://img.shields.io/badge/target%20SDK-36-orange)](app/build.gradle)

Iris is an Android keyboard based on [Simple Keyboard](https://github.com/rkkr/simple-keyboard). It keeps the small AOSP-style keyboard at its core and adds clipboard history, translation, optional AI tools, GIF search, sound packs, and visual customization.

## Features

- Clipboard history and recent-clipboard suggestions
- Offline translation with the optional ML Kit build
- Online and AI-assisted translation
- AI actions through Ollama, Gemini, or an OpenAI-compatible endpoint
- GIF search using Tenor, GIPHY, or Klipy
- Custom emoji shortcuts
- Built-in and downloadable keyboard sound packs
- Light, dark, AMOLED, and custom-color themes
- Adjustable key size, spacing, keyboard height, and bottom offset
- More than 55 keyboard layouts and 90 supported locales
- JSON settings backup and restore

Network features are optional. Iris does not include analytics or advertising, but text sent to a translation, GIF, or AI provider is subject to that provider's privacy policy.

## Download

APKs are available on the [Releases page](https://github.com/MohamadOday/Iris/releases). Each release contains two variants:

| Variant | Use it when |
| --- | --- |
| `MlKit` | You want downloadable offline translation models and have Google Play services installed. |
| `NoMlKit` | You want the smaller, fully open-source build without Google ML Kit. Online and AI translation remain available. |

After installing the APK, enable Iris under Android's keyboard settings and select it as an input method.

## AI and translation setup

AI features require a provider configured in Iris settings:

- **Ollama:** connect to an Ollama server on your device or local network.
- **Gemini:** provide a Gemini API key.
- **OpenAI-compatible:** provide an endpoint, model, and any required headers.

Offline translation is available only in the `MlKit` variant. Translation models are downloaded separately and may use tens of megabytes per language. Online translation and GIF search require an internet connection; some GIF providers require your own API key.

## Building

Requirements:

- JDK 17
- Android SDK Platform 36

Build a debug APK:

```sh
./gradlew assembleMlkitDebug
./gradlew assembleNomlkitDebug
```

Build both release variants:

```sh
./gradlew assembleRelease
```

Outputs are written to `app/build/outputs/apk/`. Release builds are unsigned unless signing credentials are supplied to Gradle. Tagged releases are built and signed by the GitHub Actions workflow.

## Contributing

Bug reports and pull requests are welcome. When reporting a crash, include the Android version, APK variant, steps to reproduce, and the relevant `adb logcat` output when possible.

For general feedback, contact [@bn3di](https://t.me/bn3di) on Telegram.

## License and attribution

Iris is licensed under the [Apache License 2.0](LICENSE). It is based on Simple Keyboard and contains third-party code or assets described in [NOTICE](NOTICE).

The ML Kit variant includes Google ML Kit and is also subject to Google's applicable terms. The NoMlKit variant excludes that dependency.
