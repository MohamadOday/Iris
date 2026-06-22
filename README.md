# Iris Keyboard 🌸

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android_7.0%2B-green.svg)](#)
[![Target SDK](https://img.shields.io/badge/Target_SDK-36-orange.svg)](#)

**Iris** is a state-of-the-art, privacy-centric, and feature-rich keyboard for Android. Forked from the minimal AOSP-based *Simple Keyboard*, Iris transforms typing into an advanced workspace by embedding locally run AI models, real-time translation pipelines, a mechanical sound synthesizer, and dynamic clipboard features—all while keeping your privacy completely intact.

---

## 📖 Table of Contents

- [🤖 Core Features](#-core-features)
  - [AI Copilot Studio](#1-ai-copilot-studio)
  - [Translation Suite](#2-translation-suite)
  - [Synthesized & Custom Soundpacks](#3-synthesized--custom-soundpacks)
  - [Advanced Clipboard Manager](#4-advanced-clipboard-manager)
  - [Media & Expression (GIFs & Emojis)](#5-media--expression-gifs--emojis)
  - [Customization & Themes](#6-customization--themes)
  - [Layouts & Languages](#7-layouts--languages)
- [🔒 Privacy & Enterprise Deployment](#-privacy--enterprise-deployment)
- [⚙️ Building and Development](#%EF%B8%8F-building-and-development)
- [⚖️ Attribution & Licensing](#%EF%B8%8F-attribution--licensing)

---

## 🤖 Core Features

### 1. AI Copilot Studio
Iris integrates a fully custom LLM runtime interface directly into your keyboard panel, allowing you to run AI prompts and text transformations in any application without switching contexts.
* **Three API Backends:**
  * **Ollama (Local):** Run entirely offline on your local network/device (default: `qwen2.5-coder`).
  * **Google Gemini:** Direct cloud connection via your Google Gemini API key.
  * **Custom API (OpenAI-compatible):** Connect to DeepSeek, OpenRouter, OpenAI, or any custom API endpoint with custom request headers.
* **One-Tap AI Assist Tools:**
  * 🧠 **Smart Compose:** Expand short thoughts into structured prose.
  * ✍️ **Simplify:** Condense long sentences for clearer communication.
  * 📝 **Grammar Fix:** Instantly correct syntax, typos, and style issues.
  * 💻 **Explain Code:** Parse and explain snippets right from your text input.
  * 🔧 **Fix Syntax:** Format and clean up code structures.
* **Inline Controls:** Copy, clear, or directly insert the AI's response into the active text field.

### 2. Translation Suite
Translate text instantly using three independent backends:
* **Offline (ML Kit):** Fully offline translation powered by Google ML Kit. Download model packs (~30MB+ per language) directly inside the app. Easily manage, add, or delete model storage.
* **Cloud (Google Translate):** Zero-config online translations utilizing light web scraping.
* **AI Translation:** Use your active AI Copilot provider to translate with custom prompts.
* **Dual-Language Selectors:** Fast source-to-target language dropdowns directly on the keyboard toolbar.

### 3. Synthesized & Custom Soundpacks
Iris features a low-level audio engineering layer that makes typing tactile and satisfying.
* **Procedural PCM Sound Synthesizer:** Real-time generation of realistic key sounds inside `KeySoundSynthesizer.java`, including:
  * **Cherry MX Blue Switches:** Synthesized tactile bump and bottom-out.
  * **Retro Typewriter:** Classic mechanical clacks with a carriage-return sound.
  * **Bubble Wrap:** Delightful pops.
  * **Sci-Fi Synth Beeps:** Futuristic tones.
* **High-Fidelity Sampled Packs:** Built-in classic iOS sound effects.
* **In-App Soundpack Store:** Browse, preview, download, and extract custom mechanical switch soundpacks (ZIP archives) from remote URLs. Powered by a custom `AudioDecoderSlicer` mapping standard, delete, return, and spacebar actions to specific audio frequencies.

### 4. Advanced Clipboard Manager
No more losing text snippets. Iris records your clipboard activity into an organized hub.
* **Clipboard History Panel:** Review and tap past copied texts to paste them instantly.
* **Suggestion Bar:** Displays your latest copied text as an action chip right above the keyboard for 60 seconds (customizable timeout) for rapid access.

### 5. Media & Expression (GIFs & Emojis)
* **Multi-Engine GIF Search:** Search Tenor, GIPHY, or Klipy directly from the keyboard. Bring your own developer key for GIPHY/Klipy, and toggle between normal and high-quality previews.
* **Custom Emoji Panel:** Define your own quick-access emoji strip using a comma-separated list in Settings.

### 6. Customization & Themes
* **6 Built-in Themes:** Material Light, Material Dark, System Default (with or without key borders).
* **AMOLED Black Mode:** Save battery with true-black backdrops on OLED screens.
* **Custom Hex Color Picker:** Customize key caps, backgrounds, and the toolbar.
* **Responsive Sizing:** Seekbars to adjust keyboard height, bottom offset (useful for gesture navigation padding), and key long-press delay.

### 7. Layouts & Languages
* **55+ Key Layouts:** Standard (QWERTY, AZERTY, QWERTZ), alternative ergonomic layouts (Dvorak, Colemak, Workman, BEPO, Ergol, PC QWERTY), and localized layouts for international scripts.
* **90+ Locales Supported:** UI translations available for major languages.
* **Dedicated Globe Key / Spacebar label:** Quick language toggling and visual indication of active locale.

---

## 🔒 Privacy & Enterprise Deployment

* **Zero Spyware, Zero Ads:** Iris does not run tracking, background telemetry, or analytical reporting scripts.
* **Direct Boot Aware:** Available to decrypt and input passwords immediately after your device powers on, before unlock.
* **Settings Backup:** Export all configurations to a JSON file for quick restoration.
* **Enterprise-Ready (MDM):** Supports Android `restrictions` schema (managed configurations) allowing IT administrators to pre-define endpoints, limit GIF access, or lock keyboard profiles across corporate device fleets.

---

## ⚙️ Building and Development

Iris is built using modern Android build tooling. It is written in Java and uses a fast, lightweight Gradle compilation setup.

### Prerequisites
* JDK 17 or later
* Android SDK Platform 36

### Build Commands
To compile a debug APK on your machine or Termux environment:
```bash
# Clean project
./gradlew clean

# Build debug APK
./gradlew assembleDebug
```
The output APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

---

## ⚖️ Attribution & Licensing

Iris Keyboard is released under the **Apache License 2.0**.

This project includes and builds upon several open-source works:
* **Simple Keyboard** (Base) — Licensed under Apache 2.0 (c) rkkr.
* **Mechvibes** (Audio Assets) — Licensed under the MIT License (c) hainguyents13.
* **Apple Inc. System Sounds** (Audio Samples) — Derived from MIT-licensed archives.
* **Google ML Kit SDK** — Integrated for offline translation. Subject to Google APIs and ML Kit Terms of Service.

For full license texts and attributions, see the accompanying [LICENSE](LICENSE) and [NOTICE](NOTICE) files.
