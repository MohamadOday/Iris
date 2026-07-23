/*
 * Copyright (C) 2026 Latin IME Customizer
 */

package nabu.iris.keyboard.latin;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import nabu.iris.keyboard.compat.PreferenceManagerCompat;
import nabu.iris.keyboard.latin.settings.Settings;

/**
 * Asynchronous network client for managing local (Ollama) and cloud (Gemini) AI prompts.
 */
public final class AiCopilotManager {
    private static final String TAG = "AiCopilotManager";
    private static final ExecutorService sExecutor = Executors.newSingleThreadExecutor();
    private static final int MAX_CHAT_HISTORY = 20;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final Context mContext;

    public static ExecutorService getSharedExecutor() {
        return sExecutor;
    }

    public static class ChatMessage {
        public final String role;
        public final String content;

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    private final java.util.List<ChatMessage> mChatHistory = new java.util.ArrayList<>();

    public void clearChatHistory() {
        synchronized (mChatHistory) {
            mChatHistory.clear();
        }
    }

    public java.util.List<ChatMessage> getChatHistory() {
        return mChatHistory;
    }

    public interface AiCallback {
        void onSuccess(String responseText);
        void onFailure(String errorMessage);
    }

    public AiCopilotManager(Context context) {
        mContext = context;
    }

    public void queryAi(final String prompt, final AiCallback callback) {
        queryAiWithProvider(null, prompt, callback);
    }

    public void queryAiWithProvider(final String targetProvider, final String prompt, final AiCallback callback) {
        sExecutor.execute(() -> {
            SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(mContext);
            String provider = targetProvider;
            if (provider == null || "active".equals(provider)) {
                provider = Settings.readAiProvider(prefs);
            }
            String systemPrompt = Settings.readAiSystemPrompt(prefs);
            float temperature = Settings.readAiTemperature(prefs);
            int maxTokens = Settings.readAiMaxTokens(prefs);
            boolean skipParams = Settings.readAiSkipParams(prefs);
            
            if ("gemini".equals(provider)) {
                String model = Settings.readGeminiModel(prefs);
                queryGemini(prompt, Settings.readGeminiKey(prefs), model, systemPrompt, temperature, skipParams, callback);
            } else if ("custom".equals(provider)) {
                queryCustom(prompt, Settings.readCustomUrl(prefs), Settings.readCustomModel(prefs), Settings.readCustomHeaders(prefs), systemPrompt, temperature, maxTokens, skipParams, callback);
            } else {
                queryOllama(prompt, Settings.readOllamaUrl(prefs), Settings.readOllamaModel(prefs), systemPrompt, temperature, skipParams, callback);
            }
        });
    }

    private void queryGemini(final String prompt, final String apiKey, final String model, final String systemPrompt, final float temperature, final boolean skipParams, final AiCallback callback) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            postFailure(callback, "Error: Gemini API Key is empty. Please set it in AI Copilot Settings!");
            return;
        }

        HttpURLConnection conn = null;
        try {
            String modelName = (model != null && !model.trim().isEmpty()) ? model.trim() : "gemini-2.5-flash";
            URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + apiKey);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(20000);

            // Build the combined prompt with system instruction
            String fullPrompt = (systemPrompt != null && !systemPrompt.trim().isEmpty())
                    ? systemPrompt.trim() + "\n\n" + prompt
                    : prompt;

            JSONObject textPart = new JSONObject();
            textPart.put("text", fullPrompt);

            JSONArray partsArray = new JSONArray();
            partsArray.put(textPart);

            JSONObject partsWrapper = new JSONObject();
            partsWrapper.put("parts", partsArray);

            JSONArray contentsArray = new JSONArray();
            contentsArray.put(partsWrapper);

            JSONObject payload = new JSONObject();
            payload.put("contents", contentsArray);

            // Add generation config with temperature if not skipping
            if (!skipParams) {
                JSONObject generationConfig = new JSONObject();
                generationConfig.put("temperature", temperature);
                payload.put("generationConfig", generationConfig);
            }

            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
            writer.write(payload.toString());
            writer.flush();
            writer.close();

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();

                JSONObject responseJson = new JSONObject(response.toString());
                JSONArray candidates = responseJson.getJSONArray("candidates");
                if (candidates.length() > 0) {
                    JSONObject firstCandidate = candidates.getJSONObject(0);
                    JSONObject content = firstCandidate.getJSONObject("content");
                    JSONArray parts = content.getJSONArray("parts");
                    if (parts.length() > 0) {
                        String result = parts.getJSONObject(0).getString("text");
                        postSuccess(callback, result);
                    } else {
                        postFailure(callback, "Error: No response text found in candidate parts.");
                    }
                } else {
                    postFailure(callback, "Error: No candidates returned from Gemini API.");
                }
            } else {
                BufferedReader err = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
                StringBuilder errorResponse = new StringBuilder();
                String line;
                while ((line = err.readLine()) != null) {
                    errorResponse.append(line);
                }
                err.close();
                postFailure(callback, "HTTP " + responseCode + ": " + errorResponse.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "Gemini API Request error", e);
            postFailure(callback, "Network error: " + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private void queryOllama(final String prompt, final String baseUrl, final String modelName, final String systemPrompt, final float temperature, final boolean skipParams, final AiCallback callback) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(baseUrl.trim() + "/api/generate");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(30000);

            JSONObject payload = new JSONObject();
            payload.put("model", modelName.trim());
            payload.put("prompt", prompt);
            payload.put("stream", false);

            // Add system prompt if configured
            if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
                payload.put("system", systemPrompt.trim());
            }

            // Add options with temperature if not skipping
            if (!skipParams) {
                JSONObject options = new JSONObject();
                options.put("temperature", temperature);
                payload.put("options", options);
            }

            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
            writer.write(payload.toString());
            writer.flush();
            writer.close();

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();

                JSONObject responseJson = new JSONObject(response.toString());
                String result = responseJson.getString("response");
                postSuccess(callback, result);
            } else {
                postFailure(callback, "HTTP " + responseCode + " connecting to Ollama.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Ollama API Request error", e);
            postFailure(callback, "Connection failed: Is Ollama running in Termux at " + baseUrl + "? Error: " + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private void queryCustom(final String prompt, final String baseUrl, final String modelName, final String headersJsonStr, final String systemPrompt, final float temperature, final int maxTokens, final boolean skipParams, final AiCallback callback) {
        HttpURLConnection conn = null;
        try {
            String cleanUrl = baseUrl.trim();
            if (!cleanUrl.endsWith("/")) {
                cleanUrl += "/";
            }
            URL url = new URL(cleanUrl + "chat/completions");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);

            // Dynamically apply parsed custom HTTP headers
            if (headersJsonStr != null && !headersJsonStr.trim().isEmpty()) {
                try {
                    JSONObject headersJson = new JSONObject(headersJsonStr.trim());
                    java.util.Iterator<String> keys = headersJson.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        String value = headersJson.getString(key);
                        conn.setRequestProperty(key, value);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed parsing custom HTTP headers JSON", e);
                }
            }

            // Build messages array with optional system prompt
            JSONArray messages = new JSONArray();

            if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
                JSONObject sysMessage = new JSONObject();
                sysMessage.put("role", "system");
                sysMessage.put("content", systemPrompt.trim());
                messages.put(sysMessage);
            }

            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.put(userMessage);

            JSONObject payload = new JSONObject();
            payload.put("model", modelName.trim());
            payload.put("messages", messages);
            if (!skipParams) {
                payload.put("temperature", temperature);
                payload.put("max_tokens", maxTokens);
            }

            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
            writer.write(payload.toString());
            writer.flush();
            writer.close();

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();

                JSONObject responseJson = new JSONObject(response.toString());
                JSONArray choices = responseJson.getJSONArray("choices");
                if (choices.length() > 0) {
                    String result = choices.getJSONObject(0).getJSONObject("message").getString("content");
                    postSuccess(callback, result);
                } else {
                    postFailure(callback, "Error: No choices returned from Custom OpenAI API.");
                }
            } else {
                BufferedReader err = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
                StringBuilder errorResponse = new StringBuilder();
                String line;
                while ((line = err.readLine()) != null) {
                    errorResponse.append(line);
                }
                err.close();
                postFailure(callback, "HTTP " + responseCode + " connecting to custom API: " + errorResponse.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "Custom API Request error", e);
            postFailure(callback, "Connection failed: Is your Custom API URL (" + baseUrl + ") reachable? Details: " + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public void queryChat(final String prompt, final AiCallback callback) {
        sExecutor.execute(() -> {
            SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(mContext);
            String provider = Settings.readAiProvider(prefs);
            String systemPrompt = Settings.readAiSystemPrompt(prefs);
            float temperature = Settings.readAiTemperature(prefs);
            int maxTokens = Settings.readAiMaxTokens(prefs);
            boolean skipParams = Settings.readAiSkipParams(prefs);

            synchronized (mChatHistory) {
                mChatHistory.add(new ChatMessage("user", prompt));
                while (mChatHistory.size() > MAX_CHAT_HISTORY) {
                    mChatHistory.remove(0);
                }
            }

            AiCallback wrappedCallback = new AiCallback() {
                @Override
                public void onSuccess(String responseText) {
                    synchronized (mChatHistory) {
                        mChatHistory.add(new ChatMessage("assistant", responseText));
                    }
                    callback.onSuccess(responseText);
                }

                @Override
                public void onFailure(String errorMessage) {
                    synchronized (mChatHistory) {
                        if (!mChatHistory.isEmpty()) {
                            mChatHistory.remove(mChatHistory.size() - 1);
                        }
                    }
                    callback.onFailure(errorMessage);
                }
            };

            if ("gemini".equals(provider)) {
                String model = Settings.readGeminiModel(prefs);
                queryGeminiChat(Settings.readGeminiKey(prefs), model, systemPrompt, temperature, skipParams, wrappedCallback);
            } else if ("custom".equals(provider)) {
                queryCustomChat(Settings.readCustomUrl(prefs), Settings.readCustomModel(prefs), Settings.readCustomHeaders(prefs), systemPrompt, temperature, maxTokens, skipParams, wrappedCallback);
            } else {
                queryOllamaChat(Settings.readOllamaUrl(prefs), Settings.readOllamaModel(prefs), systemPrompt, temperature, skipParams, wrappedCallback);
            }
        });
    }

    private void queryGeminiChat(final String apiKey, final String model, final String systemPrompt, final float temperature, final boolean skipParams, final AiCallback callback) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            postFailure(callback, "Error: Gemini API Key is empty. Please set it in AI Copilot Settings!");
            return;
        }

        HttpURLConnection conn = null;
        try {
            String modelName = (model != null && !model.trim().isEmpty()) ? model.trim() : "gemini-2.5-flash";
            URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + apiKey);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(20000);

            JSONObject payload = new JSONObject();

            if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
                JSONObject sysInstruction = new JSONObject();
                JSONArray parts = new JSONArray();
                JSONObject part = new JSONObject();
                part.put("text", systemPrompt.trim());
                parts.put(part);
                sysInstruction.put("parts", parts);
                payload.put("systemInstruction", sysInstruction);
            }

            JSONArray contentsArray = new JSONArray();
            synchronized (mChatHistory) {
                for (ChatMessage msg : mChatHistory) {
                    JSONObject partsWrapper = new JSONObject();
                    partsWrapper.put("role", "user".equals(msg.role) ? "user" : "model");
                    
                    JSONArray partsArray = new JSONArray();
                    JSONObject textPart = new JSONObject();
                    textPart.put("text", msg.content);
                    partsArray.put(textPart);
                    
                    partsWrapper.put("parts", partsArray);
                    contentsArray.put(partsWrapper);
                }
            }
            payload.put("contents", contentsArray);

            if (!skipParams) {
                JSONObject generationConfig = new JSONObject();
                generationConfig.put("temperature", temperature);
                payload.put("generationConfig", generationConfig);
            }

            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
            writer.write(payload.toString());
            writer.flush();
            writer.close();

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();

                JSONObject responseJson = new JSONObject(response.toString());
                JSONArray candidates = responseJson.getJSONArray("candidates");
                if (candidates.length() > 0) {
                    JSONObject firstCandidate = candidates.getJSONObject(0);
                    JSONObject content = firstCandidate.getJSONObject("content");
                    JSONArray parts = content.getJSONArray("parts");
                    if (parts.length() > 0) {
                        String result = parts.getJSONObject(0).getString("text");
                        postSuccess(callback, result);
                    } else {
                        postFailure(callback, "Error: No response text found in candidate parts.");
                    }
                } else {
                    postFailure(callback, "Error: No candidates returned from Gemini API.");
                }
            } else {
                BufferedReader err = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
                StringBuilder errorResponse = new StringBuilder();
                String line;
                while ((line = err.readLine()) != null) {
                    errorResponse.append(line);
                }
                err.close();
                postFailure(callback, "HTTP " + responseCode + ": " + errorResponse.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "Gemini API Request error", e);
            postFailure(callback, "Network error: " + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private void queryOllamaChat(final String baseUrl, final String modelName, final String systemPrompt, final float temperature, final boolean skipParams, final AiCallback callback) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(baseUrl.trim() + "/api/chat");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(30000);

            JSONObject payload = new JSONObject();
            payload.put("model", modelName.trim());
            payload.put("stream", false);

            JSONArray messages = new JSONArray();

            if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
                JSONObject sysMessage = new JSONObject();
                sysMessage.put("role", "system");
                sysMessage.put("content", systemPrompt.trim());
                messages.put(sysMessage);
            }

            synchronized (mChatHistory) {
                for (ChatMessage msg : mChatHistory) {
                    JSONObject chatMsg = new JSONObject();
                    chatMsg.put("role", msg.role);
                    chatMsg.put("content", msg.content);
                    messages.put(chatMsg);
                }
            }
            payload.put("messages", messages);

            if (!skipParams) {
                JSONObject options = new JSONObject();
                options.put("temperature", temperature);
                payload.put("options", options);
            }

            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
            writer.write(payload.toString());
            writer.flush();
            writer.close();

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();

                JSONObject responseJson = new JSONObject(response.toString());
                JSONObject messageJson = responseJson.getJSONObject("message");
                String result = messageJson.getString("content");
                postSuccess(callback, result);
            } else {
                postFailure(callback, "HTTP " + responseCode + " connecting to Ollama.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Ollama API Request error", e);
            postFailure(callback, "Connection failed: Is Ollama running in Termux at " + baseUrl + "? Error: " + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private void queryCustomChat(final String baseUrl, final String modelName, final String headersJsonStr, final String systemPrompt, final float temperature, final int maxTokens, final boolean skipParams, final AiCallback callback) {
        HttpURLConnection conn = null;
        try {
            String cleanUrl = baseUrl.trim();
            if (!cleanUrl.endsWith("/")) {
                cleanUrl += "/";
            }
            URL url = new URL(cleanUrl + "chat/completions");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);

            if (headersJsonStr != null && !headersJsonStr.trim().isEmpty()) {
                try {
                    JSONObject headersJson = new JSONObject(headersJsonStr.trim());
                    java.util.Iterator<String> keys = headersJson.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        String value = headersJson.getString(key);
                        conn.setRequestProperty(key, value);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed parsing custom HTTP headers JSON", e);
                }
            }

            JSONArray messages = new JSONArray();

            if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
                JSONObject sysMessage = new JSONObject();
                sysMessage.put("role", "system");
                sysMessage.put("content", systemPrompt.trim());
                messages.put(sysMessage);
            }

            synchronized (mChatHistory) {
                for (ChatMessage msg : mChatHistory) {
                    JSONObject chatMsg = new JSONObject();
                    chatMsg.put("role", msg.role);
                    chatMsg.put("content", msg.content);
                    messages.put(chatMsg);
                }
            }

            JSONObject payload = new JSONObject();
            payload.put("model", modelName.trim());
            payload.put("messages", messages);
            if (!skipParams) {
                payload.put("temperature", temperature);
                payload.put("max_tokens", maxTokens);
            }

            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
            writer.write(payload.toString());
            writer.flush();
            writer.close();

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();

                JSONObject responseJson = new JSONObject(response.toString());
                JSONArray choices = responseJson.getJSONArray("choices");
                if (choices.length() > 0) {
                    String result = choices.getJSONObject(0).getJSONObject("message").getString("content");
                    postSuccess(callback, result);
                } else {
                    postFailure(callback, "Error: No choices returned from Custom OpenAI API.");
                }
            } else {
                BufferedReader err = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
                StringBuilder errorResponse = new StringBuilder();
                String line;
                while ((line = err.readLine()) != null) {
                    errorResponse.append(line);
                }
                err.close();
                postFailure(callback, "HTTP " + responseCode + " connecting to custom API: " + errorResponse.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "Custom API Request error", e);
            postFailure(callback, "Connection failed: Is your Custom API URL (" + baseUrl + ") reachable? Details: " + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private void postSuccess(final AiCallback callback, final String result) {
        mMainHandler.post(() -> callback.onSuccess(result));
    }

    private void postFailure(final AiCallback callback, final String error) {
        mMainHandler.post(() -> callback.onFailure(error));
    }
}
