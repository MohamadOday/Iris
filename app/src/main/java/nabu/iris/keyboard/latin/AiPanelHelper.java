/*
 * Copyright (C) 2026 Latin IME Customizer
 */

package nabu.iris.keyboard.latin;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import nabu.iris.keyboard.R;
import nabu.iris.keyboard.compat.PreferenceManagerCompat;
import nabu.iris.keyboard.latin.settings.Settings;

/**
 * Helper class to manage the AI Copilot UI console panel, chat log messages, inline assist functions, and text copy/insert shortcuts.
 */
public final class AiPanelHelper {
    private final ClipboardBarController mController;
    private final Context mContext;
    private final LinearLayout mAiPanel;
    private final ScrollView mAiConsoleScroll;
    private final LinearLayout mAiChatLog;
    
    private final TextView mAiActionCopy;
    private final TextView mAiActionInsert;
    private final TextView mAiToolSmartCompose;
    private final TextView mAiToolSimplify;
    private final TextView mAiToolGrammarFix;
    private final TextView mAiToolExplain;
    private final TextView mAiToolFix;
    private final EditText mAiPromptInput;
    private final TextView mAiSubmitBtn;
    private final TextView mAiPasteBtn;
    private final TextView mAiClearBtn;

    private String mLatestResponseText = "";
    private String mLatestAction = "";

    public AiPanelHelper(ClipboardBarController controller, View inputView) {
        mController = controller;
        mContext = controller.getContext();

        mAiPanel = inputView.findViewById(R.id.ai_panel);
        mAiConsoleScroll = inputView.findViewById(R.id.ai_console_scroll);
        mAiChatLog = inputView.findViewById(R.id.ai_chat_log);
        
        mAiActionCopy = inputView.findViewById(R.id.ai_action_copy);
        mAiActionInsert = inputView.findViewById(R.id.ai_action_insert);
        mAiToolSmartCompose = inputView.findViewById(R.id.ai_tool_smart_compose);
        mAiToolSimplify = inputView.findViewById(R.id.ai_tool_simplify);
        mAiToolGrammarFix = inputView.findViewById(R.id.ai_tool_grammar_fix);
        mAiToolExplain = inputView.findViewById(R.id.ai_tool_explain);
        mAiToolFix = inputView.findViewById(R.id.ai_tool_fix);
        mAiPromptInput = inputView.findViewById(R.id.ai_prompt_input);
        mAiSubmitBtn = inputView.findViewById(R.id.ai_submit_btn);
        mAiPasteBtn = inputView.findViewById(R.id.ai_paste_btn);
        mAiClearBtn = inputView.findViewById(R.id.ai_clear_btn);

        mController.configureSimulatedInput(mAiPromptInput);
        
        setupAiActions();
        clearChat();
    }

    public EditText getAiPromptInput() {
        return mAiPromptInput;
    }

    public String getLatestResponseText() {
        return mLatestResponseText;
    }

    public void setLatestResponseText(String text) {
        mLatestResponseText = text;
    }

    private void setupAiActions() {
        if (mAiSubmitBtn != null && mAiPromptInput != null) {
            mAiSubmitBtn.setOnClickListener(v -> {
                String promptText = mAiPromptInput.getText().toString().trim();
                if (!promptText.isEmpty()) {
                    mLatestAction = "Custom";
                    runAiPrompt(promptText);
                    mAiPromptInput.setText("");
                }
            });

            mAiPromptInput.setOnEditorActionListener((v, actionId, event) -> {
                String promptText = mAiPromptInput.getText().toString().trim();
                if (!promptText.isEmpty()) {
                    mLatestAction = "Custom";
                    runAiPrompt(promptText);
                    mAiPromptInput.setText("");
                    return true;
                }
                return false;
            });
        }

        if (mAiPasteBtn != null && mAiPromptInput != null) {
            mAiPasteBtn.setOnClickListener(v -> {
                String clipText = mController.getMostRecentClipboardText();
                if (clipText != null && !clipText.isEmpty()) {
                    String text = mAiPromptInput.getText().toString();
                    int selStart = mAiPromptInput.getSelectionStart();
                    int selEnd = mAiPromptInput.getSelectionEnd();
                    if (selStart >= 0 && selEnd >= 0) {
                        int min = Math.min(selStart, selEnd);
                        int max = Math.max(selStart, selEnd);
                        String newText = text.substring(0, min) + clipText + text.substring(max);
                        mAiPromptInput.setText(newText);
                        mAiPromptInput.setSelection(min + clipText.length());
                    } else {
                        mAiPromptInput.append(clipText);
                        mAiPromptInput.setSelection(mAiPromptInput.getText().length());
                    }
                } else {
                    Toast.makeText(mContext, "Clipboard is empty!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (mAiClearBtn != null) {
            mAiClearBtn.setOnClickListener(v -> clearChat());
        }

        // Copy/Insert handlers
        if (mAiActionCopy != null) {
            mAiActionCopy.setOnClickListener(v -> {
                String output = mLatestResponseText;
                if (output != null && !output.trim().isEmpty()) {
                    ClipboardHistoryManager manager = mController.getClipboardHistoryManager();
                    if (manager != null) {
                        manager.addItem(output);
                    }
                    Toast.makeText(mContext, "Copied response to clipboard history!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mContext, "No response to copy yet!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (mAiActionInsert != null) {
            mAiActionInsert.setOnClickListener(v -> {
                String output = mLatestResponseText;
                if (output != null && !output.trim().isEmpty() && mContext instanceof LatinIME) {
                    LatinIME ime = (LatinIME) mContext;
                    InputConnection conn = ime.getCurrentInputConnection();
                    if (conn != null) {
                        if ("Simplify".equals(mLatestAction)) {
                            int selectionEnd = ime.mInputLogic.mConnection.getExpectedSelectionEnd();
                            if (selectionEnd >= 0) {
                                conn.setSelection(selectionEnd, selectionEnd);
                            }
                            CharSequence before = conn.getTextBeforeCursor(1, 0);
                            String prefix = " ";
                            if (before != null && before.length() > 0) {
                                char lastChar = before.charAt(before.length() - 1);
                                if (Character.isWhitespace(lastChar)) {
                                    prefix = "";
                                }
                            }
                            conn.commitText(prefix + output, 1);
                        } else {
                            conn.commitText(output, 1);
                        }
                        mController.showKeyboard();
                    }
                } else {
                    Toast.makeText(mContext, "No response to insert yet!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Assist tool actions
        if (mAiToolSmartCompose != null) {
            mAiToolSmartCompose.setOnClickListener(v -> runInlineAssist("SmartCompose"));
        }
        if (mAiToolSimplify != null) {
            mAiToolSimplify.setOnClickListener(v -> runInlineAssist("Simplify"));
        }
        if (mAiToolGrammarFix != null) {
            mAiToolGrammarFix.setOnClickListener(v -> runInlineAssist("Grammar"));
        }
        if (mAiToolExplain != null) {
            mAiToolExplain.setOnClickListener(v -> runInlineAssist("Explain"));
        }
        if (mAiToolFix != null) {
            mAiToolFix.setOnClickListener(v -> runInlineAssist("Fix"));
        }
    }

    public void runAiPrompt(String prompt) {
        runAiPrompt(prompt, prompt);
    }

    public void runAiPrompt(String prompt, String displayPrompt) {
        if (mAiChatLog == null) return;
        
        addMessageBubble("user", displayPrompt);
        final TextView responseBubble = addMessageBubble("bot", "[🧠 Processing prompt...] Requesting AI Engine output...");
        if (mAiSubmitBtn != null) mAiSubmitBtn.setEnabled(false);

        AiCopilotManager manager = mController.getAiManager();
        if (manager == null) return;
        manager.queryChat(prompt, new AiCopilotManager.AiCallback() {
            @Override
            public void onSuccess(String responseText) {
                mLatestResponseText = responseText;
                if (responseBubble != null) {
                    responseBubble.setText(responseText);
                }
                if (mAiSubmitBtn != null) mAiSubmitBtn.setEnabled(true);
            }

            @Override
            public void onFailure(String errorMessage) {
                if (responseBubble != null) {
                    responseBubble.setText("Copilot request failed:\n\n" + errorMessage);
                    responseBubble.setTextColor(0xFFFF5252);
                }
                if (mAiSubmitBtn != null) mAiSubmitBtn.setEnabled(true);
            }
        });
    }

    public void runInlineAssist(String action) {
        if (!(mContext instanceof LatinIME)) return;
        InputConnection conn = ((LatinIME) mContext).getCurrentInputConnection();
        if (conn == null) return;

        CharSequence selection = conn.getSelectedText(0);
        String text = (selection != null) ? selection.toString() : "";

        if (text.isEmpty()) {
            CharSequence preceding = conn.getTextBeforeCursor(200, 0);
            text = (preceding != null) ? preceding.toString().trim() : "";
        }

        EditText activeInput = mController.getActiveInput();
        if (text.isEmpty() && activeInput != null && mController.isViewOrParentVisible(activeInput)) {
            int start = activeInput.getSelectionStart();
            int end = activeInput.getSelectionEnd();
            if (start >= 0 && end >= 0 && start != end) {
                int min = Math.min(start, end);
                int max = Math.max(start, end);
                text = activeInput.getText().toString().substring(min, max);
            }
        }

        if (text.isEmpty() && mAiPromptInput != null && mController.isViewOrParentVisible(mAiPromptInput)) {
            String promptText = mAiPromptInput.getText().toString().trim();
            if (!promptText.isEmpty()) {
                text = promptText;
            }
        }

        if (text.isEmpty()) {
            SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(mContext);
            String savedKey = Settings.readGeminiKey(prefs);
            ClipboardHistoryManager histManager = mController.getClipboardHistoryManager();
            if (histManager != null) {
                List<ClipboardHistoryManager.ClipboardItem> items = histManager.getItems();
                for (ClipboardHistoryManager.ClipboardItem item : items) {
                    if (item.text == null) continue;
                    String val = item.text.trim();
                    if (val.isEmpty()) continue;

                    if (!savedKey.isEmpty() && val.equals(savedKey.trim())) {
                        continue;
                    }
                    if (val.startsWith("AIzaSy") && val.length() == 39 && !val.contains(" ")) {
                        continue;
                    }
                    if (val.startsWith("sk-") && !val.contains(" ")) {
                        continue;
                    }
                    text = item.text;
                    break;
                }
            }
        }

        if (text.isEmpty()) {
            addMessageBubble("bot", "Error: No prompt context found! Select some text or copy something first.");
            return;
        }

        mLatestAction = action;

        String finalPrompt;
        String displayPrompt;
        if ("SmartCompose".equals(action)) {
            finalPrompt = "Rewrite, expand, or execute the following instructions to draft a clean output. Return ONLY the final output:\n\n" + text;
            addMessageBubble("user", "🧠 Smart Compose:\n" + text);
            final TextView responseBubble = addMessageBubble("bot", "[🧠 Smart Composing...] Writing output...");
            AiCopilotManager manager = mController.getAiManager();
            if (manager != null) {
                manager.queryChat(finalPrompt, new AiCopilotManager.AiCallback() {
                    @Override
                    public void onSuccess(String responseText) {
                        mLatestResponseText = responseText;
                        if (responseBubble != null) {
                            responseBubble.setText(responseText);
                        }
                        conn.commitText(responseText, 1);
                        mController.showKeyboard();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        if (responseBubble != null) {
                            responseBubble.setText("Smart Compose failed:\n" + errorMessage);
                        }
                    }
                });
            }
            return;
        } else if ("Simplify".equals(action)) {
            finalPrompt = "Simplify the following text to make it extremely clear, concise, and punchy. Return ONLY the simplified text:\n\n" + text;
            displayPrompt = "✍️ Simplify:\n" + text;
        } else if ("Grammar".equals(action)) {
            finalPrompt = "You are a native American English speaker. Correct the grammar, spelling, punctuation, and phrasing in the following text. Do NOT make it sound like an AI, robotic, or overly artificial. Keep the phrasing extremely natural, casual, and authentic, exactly like how an American would type it. Return ONLY the corrected text without any extra explanation, wrapping, or markdown:\n\n" + text;
            displayPrompt = "📝 Grammar Fix:\n" + text;
        } else if ("Explain".equals(action)) {
            finalPrompt = "Explain what the following code or text does in brief, clear developer bullet points:\n\n" + text;
            displayPrompt = "💻 Explain Code:\n" + text;
        } else { // Fix
            finalPrompt = "Fix any spelling, grammar, or programming syntax errors in this text. Return ONLY the corrected text without wrapping or markdown details:\n\n" + text;
            displayPrompt = "🔧 Fix Syntax:\n" + text;
        }

        runAiPrompt(finalPrompt, displayPrompt);
    }

    public TextView addMessageBubble(String sender, String text) {
        if (mAiChatLog == null) return null;

        LinearLayout wrapper = new LinearLayout(mContext);
        wrapper.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        wrapper.setOrientation(LinearLayout.VERTICAL);

        TextView bubbleText = new TextView(mContext);
        bubbleText.setText(text);
        bubbleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        bubbleText.setPadding(mController.dpToPx(12), mController.dpToPx(8), mController.dpToPx(12), mController.dpToPx(8));

        SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(mContext);
        int customColor = Settings.readKeyboardColor(prefs, mContext);
        int backgroundColor = mController.getKeyboardBackgroundColor();
        boolean isDark = mController.isColorDark(backgroundColor);
        
        int accentColor = customColor;
        if (accentColor == 0 || mController.isColorMonochromeOrTooDark(accentColor)) {
            accentColor = mContext.getResources().getColor(R.color.settings_accent);
        }

        LinearLayout.LayoutParams bubbleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        bubbleParams.bottomMargin = mController.dpToPx(8);

        if ("user".equalsIgnoreCase(sender)) {
            wrapper.setGravity(Gravity.END);
            bubbleParams.gravity = Gravity.END;
            bubbleParams.leftMargin = mController.dpToPx(48);

            GradientDrawable userBg = new GradientDrawable();
            userBg.setShape(GradientDrawable.RECTANGLE);
            userBg.setColor(accentColor);
            userBg.setCornerRadii(new float[] {
                (float) mController.dpToPx(18), (float) mController.dpToPx(18),
                (float) mController.dpToPx(18), (float) mController.dpToPx(18),
                0f, 0f,
                (float) mController.dpToPx(18), (float) mController.dpToPx(18)
            });
            bubbleText.setBackground(userBg);
            bubbleText.setTextColor(mController.isColorDark(accentColor) ? Color.WHITE : 0xFF222222);
        } else {
            wrapper.setGravity(Gravity.START);
            bubbleParams.gravity = Gravity.START;
            bubbleParams.rightMargin = mController.dpToPx(48);

            GradientDrawable botBg = new GradientDrawable();
            botBg.setShape(GradientDrawable.RECTANGLE);
            botBg.setColor(isDark ? 0x14FFFFFF : 0x0C000000);
            botBg.setCornerRadii(new float[] {
                (float) mController.dpToPx(18), (float) mController.dpToPx(18),
                (float) mController.dpToPx(18), (float) mController.dpToPx(18),
                (float) mController.dpToPx(18), (float) mController.dpToPx(18),
                0f, 0f
            });
            bubbleText.setBackground(botBg);
            bubbleText.setTextColor(isDark ? 0xFFEEEEEE : 0xFF222222);
        }

        bubbleText.setLayoutParams(bubbleParams);
        wrapper.addView(bubbleText);
        mAiChatLog.addView(wrapper);

        if (mAiConsoleScroll != null) {
            mAiConsoleScroll.post(() -> mAiConsoleScroll.fullScroll(View.FOCUS_DOWN));
        }

        return bubbleText;
    }

    public void clearChat() {
        if (mAiChatLog != null) {
            mAiChatLog.removeAllViews();
            addMessageBubble("bot", "System Ready. Tap an AI Assist action or type a prompt...");
        }
        AiCopilotManager manager = mController.getAiManager();
        if (manager != null) {
            manager.clearChatHistory();
        }
    }

    private boolean isSentenceDelimiter(char c) {
        return c == '.' || c == '?' || c == '!' || c == '\n';
    }

    public void performAutoGrammarCorrection() {
        if (!(mContext instanceof LatinIME)) return;
        final InputConnection conn = ((LatinIME) mContext).getCurrentInputConnection();
        if (conn == null) return;

        Vibrator vibrator = mController.getVibrator();
        if (vibrator != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
                } else {
                    vibrator.vibrate(40);
                }
            } catch (Exception e) {}
        }

        CharSequence selection = conn.getSelectedText(0);
        String originalText = "";
        boolean isSelection = false;
        int replaceLength = 0;

        if (selection != null && selection.length() > 0) {
            originalText = selection.toString();
            isSelection = true;
        } else {
            CharSequence preceding = conn.getTextBeforeCursor(400, 0);
            if (preceding == null || preceding.length() == 0) {
                Toast.makeText(mContext, "No text to correct", Toast.LENGTH_SHORT).show();
                return;
            }

            String precedingText = preceding.toString();
            int end = precedingText.length() - 1;
            while (end >= 0 && (Character.isWhitespace(precedingText.charAt(end)) || isSentenceDelimiter(precedingText.charAt(end)))) {
                end--;
            }

            int boundary = -1;
            for (int i = end; i >= 0; i--) {
                if (isSentenceDelimiter(precedingText.charAt(i))) {
                    boundary = i;
                    break;
                }
            }

            int sentenceStart = boundary + 1;
            originalText = precedingText.substring(sentenceStart);
            replaceLength = originalText.length();
        }

        final String textToCorrect = originalText;
        if (textToCorrect.trim().isEmpty()) {
            Toast.makeText(mContext, "No sentence to correct", Toast.LENGTH_SHORT).show();
            return;
        }

        int leadingSpaceCount = 0;
        while (leadingSpaceCount < textToCorrect.length() && textToCorrect.charAt(leadingSpaceCount) == ' ') {
            leadingSpaceCount++;
        }
        final int finalLeadingSpaces = leadingSpaceCount;
        final boolean finalIsSelection = isSelection;
        final int finalReplaceLength = replaceLength;

        Toast.makeText(mContext, "Correcting grammar...", Toast.LENGTH_SHORT).show();

        String prompt = "You are a native American English speaker. Correct the grammar, spelling, punctuation, and phrasing in the following text. "
                      + "Do NOT make it sound like an AI, robotic, or overly artificial. Keep the phrasing extremely natural, casual, and authentic, exactly like how an American would type it. "
                      + "Provide ONLY the corrected version of the text, with absolutely no explanations, no chat, no markdown, no quotes, and no formatting. "
                      + "Preserve the original language, tone, capitalization style, and spacing as much as possible, just make it grammatically correct. "
                      + "Here is the text to correct:\n\n"
                      + textToCorrect.trim();

        AiCopilotManager manager = mController.getAiManager();
        if (manager != null) {
            manager.queryAi(prompt, new AiCopilotManager.AiCallback() {
                @Override
                public void onSuccess(String responseText) {
                    String corrected = responseText.trim();
                    if (corrected.startsWith("\"") && corrected.endsWith("\"") && !textToCorrect.trim().startsWith("\"")) {
                        corrected = corrected.substring(1, corrected.length() - 1).trim();
                    } else if (corrected.startsWith("'") && corrected.endsWith("'") && !textToCorrect.trim().startsWith("'")) {
                        corrected = corrected.substring(1, corrected.length() - 1).trim();
                    }

                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < finalLeadingSpaces; i++) {
                        sb.append(" ");
                    }
                    sb.append(corrected);
                    String finalResult = sb.toString();

                    if (finalIsSelection) {
                        conn.commitText(finalResult, 1);
                    } else {
                        conn.deleteSurroundingText(finalReplaceLength, 0);
                        conn.commitText(finalResult, 1);
                    }
                }

                @Override
                public void onFailure(String errorMessage) {
                    Toast.makeText(mContext, "Correction failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public void applyTheming(int accentColor, boolean isDark) {
        if (mAiConsoleScroll != null) {
            GradientDrawable consoleBg = new GradientDrawable();
            consoleBg.setShape(GradientDrawable.RECTANGLE);
            consoleBg.setCornerRadius(mController.dpToPx(10));
            consoleBg.setColor(isDark ? 0xFF0A0A0A : 0xFFF5F5F5);
            consoleBg.setStroke(mController.dpToPx(1.5f), accentColor);
            mAiConsoleScroll.setBackground(consoleBg);
        }

        if (mAiActionCopy != null) {
            mAiActionCopy.setTextColor(isDark ? 0xCCFFFFFF : 0xAA000000);
            mAiActionCopy.setBackground(null);
        }

        if (mAiActionInsert != null) {
            mAiActionInsert.setTextColor(accentColor);
            mAiActionInsert.setBackground(null);
        }

        mController.styleToolChip(mAiToolSmartCompose, accentColor, isDark);
        mController.styleToolChip(mAiToolSimplify, accentColor, isDark);
        mController.styleToolChip(mAiToolGrammarFix, accentColor, isDark);
        mController.styleToolChip(mAiToolExplain, accentColor, isDark);
        mController.styleToolChip(mAiToolFix, accentColor, isDark);

        if (mAiPromptInput != null) {
            mController.styleConfigField(mAiPromptInput, mController.getActiveInput() == mAiPromptInput);
        }

        if (mAiSubmitBtn != null) {
            GradientDrawable sbBg = new GradientDrawable();
            sbBg.setShape(GradientDrawable.RECTANGLE);
            sbBg.setCornerRadius(mController.dpToPx(16));
            sbBg.setColor(accentColor);
            mAiSubmitBtn.setBackground(sbBg);
            mAiSubmitBtn.setTextColor(Color.WHITE);
        }

        if (mAiPasteBtn != null) {
            GradientDrawable pbBg = new GradientDrawable();
            pbBg.setShape(GradientDrawable.RECTANGLE);
            pbBg.setCornerRadius(mController.dpToPx(16));
            pbBg.setColor(isDark ? 0x22FFFFFF : 0x1A000000);
            pbBg.setStroke(mController.dpToPx(1), accentColor);
            mAiPasteBtn.setBackground(pbBg);
            mAiPasteBtn.setTextColor(accentColor);
        }

        if (mAiClearBtn != null) {
            mAiClearBtn.setTextColor(0xFFFF5252);
            mAiClearBtn.setBackground(null);
        }
    }
}
