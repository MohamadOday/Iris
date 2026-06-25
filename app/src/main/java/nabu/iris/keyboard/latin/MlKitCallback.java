/*
 * Copyright (C) 2026 Latin IME Customizer
 */

package nabu.iris.keyboard.latin;

/**
 * Callback interface to communicate translation status back to the Translation panel UI.
 */
public interface MlKitCallback {
    void onSuccess(String translatedText);
    void onFailure(String errorMessage);
    void onDownloadStart();
    void onDownloadComplete();
    void onDownloadFailure(String errorMessage);
    void onTranslatingOffline();
}
