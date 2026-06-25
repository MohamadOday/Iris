/*
 * Copyright (C) 2026 Latin IME Customizer
 */

package nabu.iris.keyboard.latin;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

/**
 * Real implementation of the ML Kit offline translator wrapper.
 */
public final class MlKitTranslatorWrapper {
    private static Translator mActiveTranslator = null;
    private static String mActiveTranslatorSource = null;
    private static String mActiveTranslatorTarget = null;

    public static boolean isSupported() {
        return true;
    }

    public static void translate(final String sourceLang, final String targetLang, final String text, final MlKitCallback callback) {
        if ("auto".equals(sourceLang)) {
            callback.onFailure("Auto-detect not supported offline.");
            return;
        }

        final String sourceTag = TranslateLanguage.fromLanguageTag(sourceLang);
        final String targetTag = TranslateLanguage.fromLanguageTag(targetLang);

        if (sourceTag == null || targetTag == null) {
            callback.onFailure("Unsupported offline language pair.");
            return;
        }

        try {
            if (mActiveTranslator != null && 
                sourceTag.equals(mActiveTranslatorSource) && 
                targetTag.equals(mActiveTranslatorTarget)) {
                
                performTranslation(mActiveTranslator, text, callback);
            } else {
                release();
                
                TranslatorOptions options = new TranslatorOptions.Builder()
                        .setSourceLanguage(sourceTag)
                        .setTargetLanguage(targetTag)
                        .build();
                
                final Translator translator = Translation.getClient(options);
                mActiveTranslator = translator;
                mActiveTranslatorSource = sourceTag;
                mActiveTranslatorTarget = targetTag;
                
                DownloadConditions conditions = new DownloadConditions.Builder().build();
                callback.onDownloadStart();
                
                translator.downloadModelIfNeeded(conditions)
                        .addOnSuccessListener(unused -> {
                            if (mActiveTranslator == translator) {
                                callback.onDownloadComplete();
                                performTranslation(translator, text, callback);
                            }
                        })
                        .addOnFailureListener(e -> {
                            if (mActiveTranslator == translator) {
                                callback.onDownloadFailure("Download failed: " + e.getMessage());
                            }
                        });
            }
        } catch (Exception e) {
            callback.onFailure("ML Kit Init Error: " + e.getMessage());
        }
    }

    private static void performTranslation(final Translator translator, final String text, final MlKitCallback callback) {
        callback.onTranslatingOffline();
        translator.translate(text)
                .addOnSuccessListener(translatedText -> {
                    if (mActiveTranslator == translator) {
                        callback.onSuccess(translatedText);
                    }
                })
                .addOnFailureListener(e -> {
                    if (mActiveTranslator == translator) {
                        callback.onFailure("Translation failed: " + e.getMessage());
                    }
                });
    }

    public static void clearDownloadedModels(final MlKitClearCallback callback) {
        try {
            com.google.mlkit.common.model.RemoteModelManager modelManager = 
                    com.google.mlkit.common.model.RemoteModelManager.getInstance();
            
            modelManager.getDownloadedModels(com.google.mlkit.nl.translate.TranslateRemoteModel.class)
                    .addOnSuccessListener(models -> {
                        if (models == null || models.isEmpty()) {
                            callback.onNoModels();
                            return;
                        }

                        final int count = models.size();
                        final java.util.concurrent.atomic.AtomicInteger deletedCount = new java.util.concurrent.atomic.AtomicInteger(0);

                        for (com.google.mlkit.nl.translate.TranslateRemoteModel model : models) {
                            modelManager.deleteDownloadedModel(model)
                                    .addOnCompleteListener(task -> {
                                        int deleted = deletedCount.incrementAndGet();
                                        if (deleted == count) {
                                            callback.onSuccess(count);
                                        }
                                    });
                        }
                    })
                    .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
        } catch (Exception e) {
            callback.onFailure(e.getMessage());
        }
    }

    public static void release() {
        if (mActiveTranslator != null) {
            try {
                mActiveTranslator.close();
            } catch (Exception e) {
                // Ignore
            }
            mActiveTranslator = null;
            mActiveTranslatorSource = null;
            mActiveTranslatorTarget = null;
        }
    }
}
