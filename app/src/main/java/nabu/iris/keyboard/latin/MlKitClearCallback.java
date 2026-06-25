/*
 * Copyright (C) 2026 Latin IME Customizer
 */

package nabu.iris.keyboard.latin;

/**
 * Callback interface to communicate model deletion status back to settings fragments.
 */
public interface MlKitClearCallback {
    void onSuccess(int count);
    void onNoModels();
    void onFailure(String errorMessage);
}
