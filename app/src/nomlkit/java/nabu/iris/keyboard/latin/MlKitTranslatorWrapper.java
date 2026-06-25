/*
 * Copyright (C) 2026 Latin IME Customizer
 */

package nabu.iris.keyboard.latin;

/**
 * Stub/No-op implementation of the ML Kit translator wrapper for the nomlkit build.
 */
public final class MlKitTranslatorWrapper {
    public static boolean isSupported() {
        return false;
    }

    public static void translate(String sourceLang, String targetLang, String text, MlKitCallback callback) {
        callback.onFailure("Offline translation is not supported in this version. Use Google Translate or AI Copilot mode.");
    }

    public static void clearDownloadedModels(MlKitClearCallback callback) {
        callback.onNoModels();
    }

    public static void release() {
        // No-op
    }
}
