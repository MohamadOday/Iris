/*
 * Copyright (C) 2026 Latin IME Customizer
 */

package nabu.iris.keyboard.latin;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import nabu.iris.keyboard.R;
import nabu.iris.keyboard.compat.PreferenceManagerCompat;
import nabu.iris.keyboard.latin.settings.Settings;

/**
 * Helper class to manage the GIF workspace panel, Giphy/Tenor integration, histories, scroll visibility handlers, and caching.
 */
public final class GifPanelHelper {
    private final ClipboardBarController mController;
    private final Context mContext;

    private final View mGifPanel;
    private final EditText mGifSearchInput;
    private final TextView mGifClearBtn;
    private final LinearLayout mGifItemsContainer;
    private final ScrollView mGifScrollView;

    private final LruCache<String, byte[]> mGifCache = new LruCache<String, byte[]>(1536 * 1024) {
        @Override
        protected int sizeOf(String key, byte[] value) {
            return value != null ? value.length : 0;
        }
    };
    private final Handler mGifSearchHandler = new Handler(Looper.getMainLooper());
    private Runnable mGifSearchRunnable;

    private final ViewTreeObserver.OnScrollChangedListener mGifScrollListener = new ViewTreeObserver.OnScrollChangedListener() {
        @Override
        public void onScrollChanged() {
            updateGifVisibilityInScroll();
        }
    };

    public GifPanelHelper(ClipboardBarController controller, View inputView) {
        mController = controller;
        mContext = controller.getContext();

        mGifPanel = inputView.findViewById(R.id.gif_panel);
        mGifSearchInput = inputView.findViewById(R.id.gif_search_input);
        mGifClearBtn = inputView.findViewById(R.id.gif_clear_btn);
        mGifItemsContainer = inputView.findViewById(R.id.gif_items_container);
        mGifScrollView = inputView.findViewById(R.id.gif_scroll_view);

        if (mGifScrollView != null) {
            mGifScrollView.getViewTreeObserver().addOnScrollChangedListener(mGifScrollListener);
        }

        mController.configureSimulatedInput(mGifSearchInput);

        setupGifPanelActions();
    }

    public EditText getGifSearchInput() {
        return mGifSearchInput;
    }

    public void clearGifCache() {
        if (mGifCache != null) {
            mGifCache.evictAll();
        }
    }

    private void setupGifPanelActions() {
        if (mGifClearBtn != null) {
            mGifClearBtn.setOnClickListener(v -> {
                if (mGifSearchInput != null) {
                    mGifSearchInput.setText("");
                }
                loadGifs("");
            });
        }

        if (mGifSearchInput != null) {
            mGifSearchInput.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(final android.text.Editable s) {
                    if (mGifSearchRunnable != null) {
                        mGifSearchHandler.removeCallbacks(mGifSearchRunnable);
                    }
                    mGifSearchRunnable = () -> loadGifs(s.toString());
                    mGifSearchHandler.postDelayed(mGifSearchRunnable, 600);
                }
            });
            
            mGifSearchInput.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH 
                        || actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE
                        || (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER)) {
                    if (mGifSearchRunnable != null) {
                        mGifSearchHandler.removeCallbacks(mGifSearchRunnable);
                    }
                    loadGifs(v.getText().toString());
                    return true;
                }
                return false;
            });
        }
    }

    private List<GifSearchEngine.GifItem> getGifHistory() {
        List<GifSearchEngine.GifItem> list = new ArrayList<>();
        try {
            SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(mContext);
            String jsonStr = prefs.getString("pref_gif_history", "[]");
            JSONArray array = new JSONArray(jsonStr);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                list.add(new GifSearchEngine.GifItem(
                    obj.getString("id"),
                    obj.getString("previewUrl"),
                    obj.getString("fullUrl"),
                    obj.optInt("width", 200),
                    obj.optInt("height", 150)
                ));
            }
        } catch (Exception e) {
            // ignore
        }
        return list;
    }

    private void addToGifHistory(GifSearchEngine.GifItem item) {
        try {
            SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(mContext);
            List<GifSearchEngine.GifItem> current = getGifHistory();
            for (int i = 0; i < current.size(); i++) {
                if (current.get(i).id.equals(item.id)) {
                    current.remove(i);
                    break;
                }
            }
            current.add(0, item);
            while (current.size() > 20) {
                current.remove(current.size() - 1);
            }
            JSONArray array = new JSONArray();
            for (GifSearchEngine.GifItem git : current) {
                JSONObject obj = new JSONObject();
                obj.put("id", git.id);
                obj.put("previewUrl", git.previewUrl);
                obj.put("fullUrl", git.fullUrl);
                obj.put("width", git.width);
                obj.put("height", git.height);
                array.put(obj);
            }
            prefs.edit().putString("pref_gif_history", array.toString()).apply();
        } catch (Exception e) {
            // ignore
        }
    }

    private void removeFromGifHistory(GifSearchEngine.GifItem item) {
        try {
            SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(mContext);
            List<GifSearchEngine.GifItem> current = getGifHistory();
            boolean removed = false;
            for (int i = 0; i < current.size(); i++) {
                if (current.get(i).id.equals(item.id)) {
                    current.remove(i);
                    removed = true;
                    break;
                }
            }
            if (removed) {
                JSONArray array = new JSONArray();
                for (GifSearchEngine.GifItem git : current) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", git.id);
                    obj.put("previewUrl", git.previewUrl);
                    obj.put("fullUrl", git.fullUrl);
                    obj.put("width", git.width);
                    obj.put("height", git.height);
                    array.put(obj);
                }
                prefs.edit().putString("pref_gif_history", array.toString()).apply();
                
                File cacheDir = mContext.getCacheDir();
                File gifsDir = new File(cacheDir, "gifs");
                File gifFile = new File(gifsDir, item.id + ".gif");
                if (gifFile.exists()) {
                    gifFile.delete();
                }
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private boolean supportsGifInsertion() {
        if (!(mContext instanceof LatinIME)) return false;
        LatinIME ime = (LatinIME) mContext;
        android.view.inputmethod.EditorInfo editorInfo = ime.getCurrentInputEditorInfo();
        if (editorInfo == null) return false;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            String[] mimeTypes = editorInfo.contentMimeTypes;
            if (mimeTypes != null) {
                for (String mime : mimeTypes) {
                    if ("image/gif".equals(mime) || "image/*".equals(mime)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void showGifPanel() {
        if (mGifPanel != null) {
            mGifPanel.setVisibility(View.VISIBLE);
            if (!supportsGifInsertion()) {
                Toast.makeText(mContext, "Note: This app does not support direct GIF insertion", Toast.LENGTH_SHORT).show();
            }
            loadGifs(mGifSearchInput != null ? mGifSearchInput.getText().toString() : "");
        }
    }

    public void hideGifPanel() {
        if (mGifPanel != null) {
            mGifPanel.setVisibility(View.GONE);
            if (mGifItemsContainer != null) {
                mGifItemsContainer.removeAllViews();
            }
            clearGifCache();
        }
    }

    private void loadGifs(final String query) {
        if (mGifItemsContainer == null) return;

        mGifItemsContainer.removeAllViews();
        
        TextView loadingTv = new TextView(mContext);
        loadingTv.setText("Loading GIFs...");
        loadingTv.setTextColor(Color.GRAY);
        loadingTv.setTextSize(14);
        loadingTv.setGravity(Gravity.CENTER);
        loadingTv.setPadding(mController.dpToPx(16), mController.dpToPx(16), mController.dpToPx(16), mController.dpToPx(16));
        mGifItemsContainer.addView(loadingTv);

        new Thread(() -> {
            final List<GifSearchEngine.GifItem> results;
            boolean isHistoryMode = (query == null || query.trim().isEmpty());
            
            if (isHistoryMode) {
                results = getGifHistory();
            } else {
                SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(mContext);
                String provider = prefs.getString("pref_gif_provider", "tenor");
                String giphyKey = prefs.getString("pref_giphy_api_key", "");
                String klipyKey = prefs.getString("pref_klipy_api_key", "");
                boolean highQuality = Settings.readGifHighQuality(prefs);
                results = GifSearchEngine.fetchGifs(provider, giphyKey, klipyKey, highQuality, query);
            }

            mGifSearchHandler.post(() -> {
                if (mGifItemsContainer == null) return;
                mGifItemsContainer.removeAllViews();

                if (results.isEmpty()) {
                    TextView emptyTv = new TextView(mContext);
                    if (isHistoryMode) {
                        emptyTv.setText("No sent GIFs history. Type a search query above to find GIFs.");
                    } else {
                        emptyTv.setText("No GIFs found");
                    }
                    emptyTv.setTextColor(Color.GRAY);
                    emptyTv.setTextSize(14);
                    emptyTv.setGravity(Gravity.CENTER);
                    emptyTv.setPadding(mController.dpToPx(16), mController.dpToPx(16), mController.dpToPx(16), mController.dpToPx(16));
                    mGifItemsContainer.addView(emptyTv);
                    return;
                }

                final int MAX_VISIBLE_GIFS = 12;
                int cols = 2;
                LinearLayout currentRow = null;
                int addedCount = 0;

                File cacheDir = mContext.getCacheDir();
                File gifsDir = new File(cacheDir, "gifs");

                for (final GifSearchEngine.GifItem item : results) {
                    if (addedCount >= MAX_VISIBLE_GIFS) break;
                    if (addedCount % cols == 0) {
                        currentRow = new LinearLayout(mContext);
                        currentRow.setOrientation(LinearLayout.HORIZONTAL);
                        currentRow.setGravity(Gravity.CENTER_VERTICAL);
                        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        );
                        rowLp.setMargins(0, 0, 0, mController.dpToPx(8));
                        currentRow.setLayoutParams(rowLp);
                        mGifItemsContainer.addView(currentRow);
                    }

                    GifView gifView = new GifView(mContext);
                    float aspect = 1.33f;
                    if (item.height > 0) {
                        aspect = (float) item.width / item.height;
                    }
                    aspect = Math.max(0.5f, Math.min(2.5f, aspect));
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        0,
                        mController.dpToPx(110),
                        aspect
                    );
                    if (addedCount % cols == 0) {
                        lp.setMargins(0, 0, mController.dpToPx(4), 0);
                    } else {
                        lp.setMargins(mController.dpToPx(4), 0, 0, 0);
                    }
                    gifView.setLayoutParams(lp);

                    File localFile = new File(gifsDir, item.id + ".gif");
                    String loadPath = localFile.exists() ? localFile.getAbsolutePath() : item.previewUrl;
                    gifView.loadUrl(loadPath, mGifCache);

                    gifView.setOnClickListener(v -> {
                        if (!supportsGifInsertion()) {
                            Toast.makeText(mContext, "This app does not support direct GIF insertion", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Toast.makeText(mContext, "Sending GIF...", Toast.LENGTH_SHORT).show();
                        shareGif(item);
                    });

                    if (isHistoryMode) {
                        gifView.setOnLongClickListener(v -> {
                            removeFromGifHistory(item);
                            Toast.makeText(mContext, "Removed from history", Toast.LENGTH_SHORT).show();
                            loadGifs("");
                            return true;
                        });
                    }

                    if (currentRow != null) {
                        currentRow.addView(gifView);
                        addedCount++;
                    }
                }
                mGifSearchHandler.postDelayed(() -> updateGifVisibilityInScroll(), 100);
            });
        }).start();
    }

    private void updateGifVisibilityInScroll() {
        if (mGifScrollView == null || mGifItemsContainer == null) return;
        
        android.graphics.Rect scrollBounds = new android.graphics.Rect();
        mGifScrollView.getHitRect(scrollBounds);
        
        for (int i = 0; i < mGifItemsContainer.getChildCount(); i++) {
            View rowView = mGifItemsContainer.getChildAt(i);
            if (rowView instanceof LinearLayout) {
                LinearLayout row = (LinearLayout) rowView;
                for (int j = 0; j < row.getChildCount(); j++) {
                    View child = row.getChildAt(j);
                    if (child instanceof GifView) {
                        GifView gifView = (GifView) child;
                        if (gifView.getLocalVisibleRect(new android.graphics.Rect())) {
                            gifView.resumeAnimation();
                        } else {
                            gifView.pauseAnimation();
                        }
                    }
                }
            }
        }
    }

    private void shareGif(final GifSearchEngine.GifItem item) {
        new Thread(() -> {
            try {
                File cacheDir = mContext.getCacheDir();
                File gifsDir = new File(cacheDir, "gifs");
                if (!gifsDir.exists()) {
                    gifsDir.mkdirs();
                }
                
                final File gifFile = new File(gifsDir, item.id + ".gif");
                
                if (gifFile.exists()) {
                    mGifSearchHandler.post(() -> {
                        commitGifFile(gifFile);
                        addToGifHistory(item);
                    });
                    return;
                }
                
                URL u = new URL(item.fullUrl);
                HttpURLConnection conn = (HttpURLConnection) u.openConnection();
                conn.setConnectTimeout(12000);
                conn.setReadTimeout(12000);
                conn.connect();
                
                if (conn.getResponseCode() == 200) {
                    InputStream in = conn.getInputStream();
                    java.io.FileOutputStream out = new java.io.FileOutputStream(gifFile);
                    byte[] buf = new byte[4096];
                    int len;
                    while ((len = in.read(buf)) != -1) {
                        out.write(buf, 0, len);
                    }
                    in.close();
                    out.close();
                    
                    mGifSearchHandler.post(() -> {
                        commitGifFile(gifFile);
                        addToGifHistory(item);
                    });
                } else {
                    mGifSearchHandler.post(() -> 
                        Toast.makeText(mContext, "Failed to download GIF", Toast.LENGTH_SHORT).show()
                    );
                }
            } catch (Exception e) {
                mGifSearchHandler.post(() -> 
                    Toast.makeText(mContext, "Error sharing GIF: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void commitGifFile(File file) {
        if (!(mContext instanceof LatinIME)) return;
        LatinIME ime = (LatinIME) mContext;
        InputConnection conn = ime.getCurrentInputConnection();
        android.view.inputmethod.EditorInfo editorInfo = ime.getCurrentInputEditorInfo();
        
        if (conn == null || editorInfo == null) return;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            if (!supportsGifInsertion()) {
                Toast.makeText(mContext, "App does not support GIF insertion", Toast.LENGTH_SHORT).show();
                return;
            }
            
            android.net.Uri contentUri = android.net.Uri.parse("content://nabu.iris.keyboard.gifprovider/" + file.getName());
            
            android.view.inputmethod.InputContentInfo contentInfo = new android.view.inputmethod.InputContentInfo(
                contentUri,
                new android.content.ClipDescription("GIF", new String[]{"image/gif"}),
                null
            );
            
            try {
                conn.commitContent(
                    contentInfo,
                    InputConnection.INPUT_CONTENT_GRANT_READ_URI_PERMISSION,
                    null
                );
            } catch (Exception e) {
                Toast.makeText(mContext, "Failed to insert GIF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(mContext, "GIF insertion requires Android 7.1.1+", Toast.LENGTH_SHORT).show();
        }
    }

    public void onDestroy() {
        if (mGifSearchRunnable != null) {
            mGifSearchHandler.removeCallbacks(mGifSearchRunnable);
        }
        if (mGifScrollView != null && mGifScrollView.getViewTreeObserver().isAlive()) {
            mGifScrollView.getViewTreeObserver().removeOnScrollChangedListener(mGifScrollListener);
        }
        if (mGifItemsContainer != null) {
            mGifItemsContainer.removeAllViews();
        }
        clearGifCache();
    }
}
