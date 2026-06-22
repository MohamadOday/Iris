package nabu.iris.keyboard.latin.settings;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.media.MediaPlayer;
import nabu.iris.keyboard.latin.AudioDecoderSlicer;

public class SoundpackDownloadActivity extends Activity {
    private static final String TAG = "SoundpackDownload";

    private LinearLayout mMainLayout;
    private LinearLayout mCatalogLayout;
    private EditText mSearchInput;
    private EditText mCustomUrlInput;
    private Button mImportBtn;
    private Button mScrapeBtn;
    private ProgressBar mMainProgressBar;
    private TextView mLoadingText;
    private TextView mDebugConsole;
    private ScrollView mDebugScroll;

    private List<SoundpackItem> mSoundpacks = new ArrayList<>();
    private List<SoundpackItem> mFilteredSoundpacks = new ArrayList<>();
    private MediaPlayer mMediaPlayer;
    private SoundpackItem mPlayingItem = null;

    private interface DownloadCallback {
        void onProgress(int percent);
        void onComplete();
        void onError(String error);
    }

    private static class SoundpackItem {
        String id;
        String name;
        String downloadUrl;
        String type; // Tactile, Clicky, Linear, Buckling Spring, etc.
        String status = "Available"; // Available, Downloading, Installed
        int progress = 0;
        boolean isDefault = false;
        String previewStatus = "PLAY";

        SoundpackItem(String id, String name, String downloadUrl, String type) {
            this.id = id;
            this.name = name;
            this.downloadUrl = downloadUrl;
            this.type = type;
        }

        SoundpackItem(String id, String name, String downloadUrl, String type, boolean isDefault) {
            this.id = id;
            this.name = name;
            this.downloadUrl = downloadUrl;
            this.type = type;
            this.isDefault = isDefault;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Apply Hardcoded Dark Theme
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(0xFF0B0C10);
            window.setNavigationBarColor(0xFF0B0C10);
        }

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setTitle("Soundpack Store");
            GradientDrawable abBg = new GradientDrawable();
            abBg.setColor(0xFF161622);
            actionBar.setBackgroundDrawable(abBg);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                actionBar.setElevation(0);
            }
        }

        // Initialize UI programmatically
        mMainLayout = new LinearLayout(this);
        mMainLayout.setOrientation(LinearLayout.VERTICAL);
        mMainLayout.setBackgroundColor(0xFF0B0C10);
        mMainLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        // Premium Monospace Technical Header - Compact & Borderless
        LinearLayout headerPanel = new LinearLayout(this);
        headerPanel.setOrientation(LinearLayout.VERTICAL);
        headerPanel.setPadding(0, 0, 0, 0);
        headerPanel.setGravity(Gravity.CENTER_HORIZONTAL);
        
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headerParams.setMargins(0, 0, 0, dpToPx(8));
        headerPanel.setLayoutParams(headerParams);
        
        TextView techTitle = new TextView(this);
        techTitle.setText("MECH SOUNDREPOSITORY v2.5");
        techTitle.setTextColor(0xFF7C4DFF); // Premium Electric Purple
        techTitle.setTextSize(11.5f);
        techTitle.setTypeface(Typeface.MONOSPACE);
        techTitle.setLetterSpacing(0.08f);
        techTitle.setGravity(Gravity.CENTER);
        headerPanel.addView(techTitle);
        
        mMainLayout.addView(headerPanel);

        // 1. Frosted Custom Import Card
        LinearLayout importCard = new LinearLayout(this);
        importCard.setOrientation(LinearLayout.VERTICAL);
        importCard.setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12));
        
        GradientDrawable importCardBg = new GradientDrawable();
        importCardBg.setShape(GradientDrawable.RECTANGLE);
        importCardBg.setCornerRadius(dpToPx(12));
        importCardBg.setColor(0x0AFFFFFF); // Frosted translucent background
        importCardBg.setStroke(dpToPx(1), 0x1AFFFFFF);
        importCard.setBackground(importCardBg);
        
        LinearLayout.LayoutParams importCardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        importCardParams.setMargins(0, 0, 0, dpToPx(12));
        importCard.setLayoutParams(importCardParams);

        TextView importTitle = new TextView(this);
        importTitle.setText("IMPORT CUSTOM SOUNDPACK");
        importTitle.setTextColor(0xFF7C4DFF); // Premium Electric Purple
        importTitle.setTextSize(11);
        importTitle.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        importTitle.setLetterSpacing(0.08f);
        importCard.addView(importTitle);

        LinearLayout importRow = new LinearLayout(this);
        importRow.setOrientation(LinearLayout.HORIZONTAL);
        importRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, dpToPx(6), 0, 0);
        importRow.setLayoutParams(rowParams);

        mCustomUrlInput = new EditText(this);
        mCustomUrlInput.setHint("Paste Mechvibes ZIP direct URL...");
        mCustomUrlInput.setHintTextColor(0x66FFFFFF);
        mCustomUrlInput.setTextColor(Color.WHITE);
        mCustomUrlInput.setTextSize(13);
        mCustomUrlInput.setSingleLine(true);
        mCustomUrlInput.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));
        
        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setShape(GradientDrawable.RECTANGLE);
        inputBg.setCornerRadius(dpToPx(8));
        inputBg.setColor(0x0EFFFFFF);
        inputBg.setStroke(dpToPx(1), 0x1AFFFFFF);
        mCustomUrlInput.setBackground(inputBg);
        
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        inputParams.setMargins(0, 0, dpToPx(8), 0);
        mCustomUrlInput.setLayoutParams(inputParams);
        importRow.addView(mCustomUrlInput);

        mImportBtn = new Button(this);
        mImportBtn.setText("IMPORT");
        mImportBtn.setTextColor(0xFF0B0C10);
        mImportBtn.setTextSize(12);
        mImportBtn.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        mImportBtn.setPadding(dpToPx(16), dpToPx(10), dpToPx(16), dpToPx(10));
        applyCyberButtonStyle(mImportBtn, 0xFF7C4DFF, 0xFF7C4DFF, 0x1A7C4DFF, true); // Electric Purple theme
        importRow.addView(mImportBtn);

        mImportBtn.setOnClickListener(v -> {
            String url = mCustomUrlInput.getText().toString().trim();
            if (url.isEmpty()) {
                Toast.makeText(this, "Please enter a valid URL", Toast.LENGTH_SHORT).show();
                return;
            }
            String name = "Imported_Pack";
            int lastSlash = url.lastIndexOf('/');
            if (lastSlash != -1 && lastSlash < url.length() - 1) {
                String sub = url.substring(lastSlash + 1);
                if (sub.endsWith(".zip")) {
                    name = sub.substring(0, sub.length() - 4).replace("-", "_").replace(" ", "_");
                }
            }
            startDownload(new SoundpackItem(name, name.replace("_", " "), url, "Custom Import"));
        });

        importCard.addView(importRow);
        mMainLayout.addView(importCard);

        // 2. Add Live Web Scraper and Title Row
        LinearLayout scraperRow = new LinearLayout(this);
        scraperRow.setOrientation(LinearLayout.HORIZONTAL);
        scraperRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams scraperRowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        scraperRowParams.setMargins(0, 0, 0, dpToPx(8));
        scraperRow.setLayoutParams(scraperRowParams);

        TextView catalogTitle = new TextView(this);
        catalogTitle.setText("PREMIUM SWITCH CATALOG");
        catalogTitle.setTextColor(0xFF7C4DFF); // Premium Electric Purple
        catalogTitle.setTextSize(11);
        catalogTitle.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        catalogTitle.setLetterSpacing(0.08f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        catalogTitle.setLayoutParams(titleParams);
        scraperRow.addView(catalogTitle);

        mScrapeBtn = new Button(this);
        mScrapeBtn.setText("SCRAPE LIVE WEBSITE");
        mScrapeBtn.setTextColor(0xFF7C4DFF); // Premium Electric Purple
        mScrapeBtn.setTextSize(10.5f);
        mScrapeBtn.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        mScrapeBtn.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));
        applyCyberButtonStyle(mScrapeBtn, 0xFF7C4DFF, 0xFF7C4DFF, 0x1A7C4DFF, false); // Electric Purple theme
        scraperRow.addView(mScrapeBtn);

        mScrapeBtn.setOnClickListener(v -> {
            scrapeLiveSoundpacks();
        });

        mMainLayout.addView(scraperRow);

        // 3. Search Bar for Catalog (frosted styling)
        mSearchInput = new EditText(this);
        mSearchInput.setHint("Search switches...");
        mSearchInput.setHintTextColor(0x66FFFFFF);
        mSearchInput.setTextColor(Color.WHITE);
        mSearchInput.setTextSize(13);
        mSearchInput.setSingleLine(true);
        mSearchInput.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));
        mSearchInput.setBackground(inputBg);
        
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        searchParams.setMargins(0, 0, 0, dpToPx(12));
        mSearchInput.setLayoutParams(searchParams);
        
        mSearchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCatalog(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        mMainLayout.addView(mSearchInput);

        // 4. Main Loader indicators
        mMainProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        mMainProgressBar.setVisibility(View.GONE);
        mMainLayout.addView(mMainProgressBar);

        mLoadingText = new TextView(this);
        mLoadingText.setTextColor(Color.WHITE);
        mLoadingText.setTextSize(12);
        mLoadingText.setGravity(Gravity.CENTER);
        mLoadingText.setVisibility(View.GONE);
        mMainLayout.addView(mLoadingText);

        // 5. Scroll View for Catalog Card List
        ScrollView scrollView = new ScrollView(this);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        scrollParams.setMargins(0, 0, 0, dpToPx(12));
        scrollView.setLayoutParams(scrollParams);

        mCatalogLayout = new LinearLayout(this);
        mCatalogLayout.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(mCatalogLayout);
        mMainLayout.addView(scrollView);

        // 6. Diagnostics Log Console Layout at the bottom
        TextView logsTitle = new TextView(this);
        logsTitle.setText("DIAGNOSTICS & SYSTEM LOGS");
        logsTitle.setTextColor(0xFF7C4DFF); // Premium Electric Purple
        logsTitle.setTextSize(9.5f);
        logsTitle.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        logsTitle.setLetterSpacing(0.08f);
        mMainLayout.addView(logsTitle);

        mDebugScroll = new ScrollView(this);
        LinearLayout.LayoutParams debugScrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(55)); // Shrunk to 55dp height
        debugScrollParams.setMargins(0, dpToPx(4), 0, 0);
        mDebugScroll.setLayoutParams(debugScrollParams);

        mDebugConsole = new TextView(this);
        mDebugConsole.setText("> Diagnostics initialized. Ready to download.\n");
        mDebugConsole.setTextColor(0xFF00FF00); // Hacker green
        mDebugConsole.setTextSize(9.5f);
        mDebugConsole.setTypeface(Typeface.MONOSPACE);
        mDebugConsole.setPadding(dpToPx(6), dpToPx(5), dpToPx(6), dpToPx(5)); // Compact padding
        
        GradientDrawable consoleBg = new GradientDrawable();
        consoleBg.setShape(GradientDrawable.RECTANGLE);
        consoleBg.setCornerRadius(dpToPx(8));
        consoleBg.setColor(0xFF050505); // Deep pure black
        consoleBg.setStroke(dpToPx(1), 0x337C4DFF); // Electric Purple outline stroke
        mDebugConsole.setBackground(consoleBg);
        
        mDebugScroll.addView(mDebugConsole);
        mMainLayout.addView(mDebugScroll);

        setContentView(mMainLayout);

        // Populate standard defaults
        populateDefaultCatalog();
        loadScrapedSoundpacks();
        mFilteredSoundpacks.clear();
        mFilteredSoundpacks.addAll(mSoundpacks);
        updateCatalogList();
    }

    private void saveScrapedSoundpacks() {
        try {
            JSONArray array = new JSONArray();
            for (SoundpackItem item : mSoundpacks) {
                if (!item.isDefault) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", item.id);
                    obj.put("name", item.name);
                    obj.put("downloadUrl", item.downloadUrl);
                    obj.put("type", item.type);
                    array.put(obj);
                }
            }
            File file = new File(getFilesDir(), "scraped_soundpacks.json");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(array.toString(2).getBytes("UTF-8"));
            fos.close();
        } catch (Exception e) {
            Log.e(TAG, "Failed to save scraped soundpacks", e);
        }
    }

    private void loadScrapedSoundpacks() {
        try {
            File file = new File(getFilesDir(), "scraped_soundpacks.json");
            if (!file.exists()) return;
            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int len;
            while ((len = fis.read(buf)) != -1) {
                baos.write(buf, 0, len);
            }
            fis.close();
            String jsonStr = baos.toString("UTF-8");
            JSONArray array = new JSONArray(jsonStr);
            
            File rootDir = getExternalFilesDir("soundpacks");
            
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String id = obj.getString("id");
                String name = obj.getString("name");
                String downloadUrl = obj.getString("downloadUrl");
                String type = obj.getString("type");
                
                SoundpackItem item = new SoundpackItem(id, name, downloadUrl, type, false);
                if (rootDir != null && rootDir.exists()) {
                    File dir = new File(rootDir, item.id);
                    if (dir.exists() && dir.isDirectory() && new File(dir, "standard.wav").exists()) {
                        item.status = "Installed";
                    }
                }
                mSoundpacks.add(item);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load scraped soundpacks", e);
        }
    }

    private void populateDefaultCatalog() {
        mSoundpacks.add(new SoundpackItem("cherrymx_blue_pbt", "Cherry MX Blue",
                "https://mechvibes.com/sound-packs/sound-pack-1200000000002", "Clicky Switch", true));
        mSoundpacks.add(new SoundpackItem("cherrymx_brown_pbt", "Cherry MX Brown",
                "https://mechvibes.com/sound-packs/sound-pack-1200000000003", "Tactile Switch", true));
        mSoundpacks.add(new SoundpackItem("cherrymx_red_pbt", "Cherry MX Red",
                "https://mechvibes.com/sound-packs/sound-pack-1200000000004", "Linear Switch", true));
        mSoundpacks.add(new SoundpackItem("cherrymx_black_abs", "Cherry MX Black",
                "https://mechvibes.com/sound-packs/sound-pack-1200000000001", "Linear Switch", true));
        mSoundpacks.add(new SoundpackItem("holy_pandas", "Holy Pandas",
                "https://mechvibes.com/sound-packs/sound-pack-v2-example-01-holy-pandas", "Tactile Thock", true));
        mSoundpacks.add(new SoundpackItem("nk_creams", "NovelKeys Creams",
                "https://mechvibes.com/sound-packs/sound-pack-1200000000010", "Linear Switch", true));
        mSoundpacks.add(new SoundpackItem("ibm_model_m_ssk", "IBM Model M SSK",
                "https://mechvibes.com/sound-packs/sound-pack-1200000000007", "Buckling Spring", true));
        mSoundpacks.add(new SoundpackItem("topre_realforce_87u", "Topre Realforce",
                "https://mechvibes.com/sound-packs/sound-pack-1200000000005", "Electrostatic Tactile", true));
        mSoundpacks.add(new SoundpackItem("nk_sherbets", "NK Sherbets",
                "https://mechvibes.com/sound-packs/sound-pack-1200000000009", "Tactile Clicky", true));
        mSoundpacks.add(new SoundpackItem("alps_blue", "Alps Blue Keyboard",
                "https://mechvibes.com/sound-packs/sound-pack-1200000000011", "Vintage Clicky", true));

        // Initialize status based on whether directories exist
        File rootDir = getExternalFilesDir("soundpacks");
        if (rootDir != null && rootDir.exists()) {
            for (SoundpackItem item : mSoundpacks) {
                File dir = new File(rootDir, item.id);
                if (dir.exists() && dir.isDirectory() && new File(dir, "standard.wav").exists()) {
                    item.status = "Installed";
                }
            }
        }
        mFilteredSoundpacks.addAll(mSoundpacks);
    }

    private void filterCatalog(String query) {
        mFilteredSoundpacks.clear();
        String lowercaseQuery = query.toLowerCase();
        for (SoundpackItem item : mSoundpacks) {
            if (item.name.toLowerCase().contains(lowercaseQuery) || item.type.toLowerCase().contains(lowercaseQuery)) {
                mFilteredSoundpacks.add(item);
            }
        }
        updateCatalogList();
    }

    private void applyCyberButtonStyle(final Button btn, final int baseColor, final int outlineColor, final int fillColor, final boolean isSolid) {
        final GradientDrawable normalBg = new GradientDrawable();
        normalBg.setShape(GradientDrawable.RECTANGLE);
        normalBg.setCornerRadius(dpToPx(8));
        if (isSolid) {
            normalBg.setColor(baseColor);
        } else {
            normalBg.setColor(fillColor);
            normalBg.setStroke(dpToPx(1), outlineColor);
        }

        final GradientDrawable pressedBg = new GradientDrawable();
        pressedBg.setShape(GradientDrawable.RECTANGLE);
        pressedBg.setCornerRadius(dpToPx(8));
        if (isSolid) {
            pressedBg.setColor(getTranslucentColor(baseColor, 80));
        } else {
            pressedBg.setColor(getTranslucentColor(outlineColor, 35));
            pressedBg.setStroke(dpToPx(1), outlineColor);
        }

        btn.setBackground(normalBg);
        btn.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                btn.setBackground(pressedBg);
                btn.setScaleX(0.96f);
                btn.setScaleY(0.96f);
            } else if (event.getAction() == android.view.MotionEvent.ACTION_UP || 
                       event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                btn.setBackground(normalBg);
                btn.setScaleX(1.0f);
                btn.setScaleY(1.0f);
            }
            return false;
        });
    }

    private int getTranslucentColor(int color, int alphaPercent) {
        int alpha = (int) (255 * (alphaPercent / 100.0));
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private void updateCatalogList() {
        mCatalogLayout.removeAllViews();

        for (final SoundpackItem item : mFilteredSoundpacks) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));

            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setShape(GradientDrawable.RECTANGLE);
            cardBg.setCornerRadius(dpToPx(14));

            if (item.status.equals("Installed")) {
                cardBg.setColor(0x137C4DFF); // Muted cyber Electric Purple background when installed
                cardBg.setStroke(dpToPx(2), 0xFF7C4DFF); // Strong glowing Electric Purple border
            } else {
                cardBg.setColor(0x0AFFFFFF); // Translucent charcoal fill
                cardBg.setStroke(dpToPx(1), 0x1AFFFFFF);
            }
            card.setBackground(cardBg);

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, dpToPx(12));
            card.setLayoutParams(cardParams);

            // Left text details
            LinearLayout textLayout = new LinearLayout(this);
            textLayout.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            textLayout.setLayoutParams(textParams);

            TextView nameText = new TextView(this);
            nameText.setText(item.name);
            nameText.setTextColor(Color.WHITE);
            nameText.setTextSize(14.5f);
            nameText.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            textLayout.addView(nameText);

            // Double badge layout: Switch Type + Installation Status
            LinearLayout tagsLayout = new LinearLayout(this);
            tagsLayout.setOrientation(LinearLayout.HORIZONTAL);
            tagsLayout.setGravity(Gravity.CENTER_VERTICAL);
            tagsLayout.setPadding(0, dpToPx(6), 0, 0);

            // 1. Switch Class tag badge
            TextView badge = new TextView(this);
            badge.setText(item.type.replace(" Switch", "").toUpperCase());
            badge.setTextSize(8.0f);
            badge.setPadding(dpToPx(8), dpToPx(2), dpToPx(8), dpToPx(2));
            badge.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
            
            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setShape(GradientDrawable.RECTANGLE);
            badgeBg.setCornerRadius(dpToPx(6));
            
            int badgeColor;
            String typeLower = item.type.toLowerCase();
            if (typeLower.contains("clicky") || typeLower.contains("classic") || typeLower.contains("vintage")) {
                badgeColor = 0xFFFFB300; // Cyber Amber
            } else if (typeLower.contains("tactile") || typeLower.contains("thock")) {
                badgeColor = 0xFF00E5FF; // Neon Cyan
            } else if (typeLower.contains("linear")) {
                badgeColor = 0xFFFF4081; // Hot Pink
            } else if (typeLower.contains("buckling") || typeLower.contains("spring")) {
                badgeColor = 0xFFA1887F; // Bronze
            } else if (typeLower.contains("electrostatic")) {
                badgeColor = 0xFF7C4DFF; // Purple
            } else {
                badgeColor = 0xFFE0E0E0; // Silver
            }
            
            badgeBg.setColor(getTranslucentColor(badgeColor, 12));
            badgeBg.setStroke(dpToPx(1), getTranslucentColor(badgeColor, 35));
            badge.setBackground(badgeBg);
            badge.setTextColor(badgeColor);
            tagsLayout.addView(badge);
            
            // Separator spacing
            View badgeSpacer = new View(this);
            LinearLayout.LayoutParams bsParams = new LinearLayout.LayoutParams(dpToPx(8), 1);
            badgeSpacer.setLayoutParams(bsParams);
            tagsLayout.addView(badgeSpacer);
            
            // 2. Installation Status tag badge
            TextView statusBadge = new TextView(this);
            statusBadge.setText(item.status.toUpperCase());
            statusBadge.setTextSize(8.0f);
            statusBadge.setPadding(dpToPx(8), dpToPx(2), dpToPx(8), dpToPx(2));
            statusBadge.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
            
            GradientDrawable statusBg = new GradientDrawable();
            statusBg.setShape(GradientDrawable.RECTANGLE);
            statusBg.setCornerRadius(dpToPx(6));
            
            int statusColor;
            if (item.status.equals("Installed")) {
                statusColor = 0xFF7C4DFF; // Glowing Neon Electric Purple
            } else if (item.status.equals("Downloading")) {
                statusColor = 0xFF90CAF9; // Warm Blue
            } else {
                statusColor = 0xFF9E9EAE; // Muted Silver
            }
            statusBg.setColor(getTranslucentColor(statusColor, 12));
            statusBg.setStroke(dpToPx(1), getTranslucentColor(statusColor, 35));
            statusBadge.setBackground(statusBg);
            statusBadge.setTextColor(statusColor);
            tagsLayout.addView(statusBadge);

            textLayout.addView(tagsLayout);
            card.addView(textLayout);

            // Play/Preview Button — compact inline chip (TextView, not Button, to avoid Android minimum size)
            TextView playBtn = new TextView(this);
            playBtn.setTextSize(10);
            playBtn.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));

            int playColor;
            String playText;
            boolean playEnabled = true;

            if (item.previewStatus.equals("STOP")) {
                playColor = 0xFFFF1744; // Neon Red
                playText = "■ STOP";
            } else if (item.previewStatus.equals("BUFFERING")) {
                playColor = 0xFF9E9EAE; // Silver
                playText = "BUFFERING...";
                playEnabled = false;
            } else {
                playColor = 0xFF00E5FF; // Neon Cyan
                playText = "▶ PLAY";
            }

            playBtn.setText(playText);
            playBtn.setTextColor(playColor);
            playBtn.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
            playBtn.setEnabled(playEnabled);
            playBtn.setGravity(android.view.Gravity.CENTER);

            GradientDrawable playBg = new GradientDrawable();
            playBg.setShape(GradientDrawable.RECTANGLE);
            playBg.setCornerRadius(dpToPx(6));
            playBg.setColor(getTranslucentColor(playColor, 15));
            playBg.setStroke(dpToPx(1), getTranslucentColor(playColor, 60));
            playBtn.setBackground(playBg);

            LinearLayout.LayoutParams playParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dpToPx(28));
            playParams.setMargins(0, 0, dpToPx(6), 0);
            playBtn.setLayoutParams(playParams);

            playBtn.setOnClickListener(v -> {
                playPreview(item);
            });

            if (!item.status.equals("Downloading")) {
                card.addView(playBtn);
            }

            // Action Button
            Button actionBtn = new Button(this);
            actionBtn.setTextSize(11);
            actionBtn.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
            
            int btnColor;
            String btnText;
            boolean btnEnabled = true;
            if (item.status.equals("Installed")) {
                btnColor = 0xFFFF1744; // Neon Red
                btnText = "DELETE";
            } else if (item.status.equals("Downloading")) {
                btnColor = 0xFFFFFFFF; // White
                btnText = item.progress + "%";
                btnEnabled = false;
            } else {
                btnColor = 0xFF7C4DFF; // Cyber Electric Purple
                btnText = "DOWNLOAD";
            }
            
            actionBtn.setText(btnText);
            actionBtn.setTextColor(btnColor);
            actionBtn.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));
            actionBtn.setEnabled(btnEnabled);
            
            applyCyberButtonStyle(actionBtn, btnColor, btnColor, getTranslucentColor(btnColor, 12), false);
            
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            actionBtn.setLayoutParams(btnParams);

            actionBtn.setOnClickListener(v -> {
                if (item.status.equals("Installed")) {
                    deleteSoundpack(item);
                } else if (item.status.equals("Available")) {
                    startDownload(item);
                }
            });

            card.addView(actionBtn);
            mCatalogLayout.addView(card);
        }
    }

    private void deleteSoundpack(SoundpackItem item) {
        File rootDir = getExternalFilesDir("soundpacks");
        if (rootDir != null) {
            File dir = new File(rootDir, item.id);
            if (dir.exists()) {
                deleteRecursive(dir);
            }
            item.status = "Available";
            Toast.makeText(this, item.name + " uninstalled successfully", Toast.LENGTH_SHORT).show();
            updateCatalogList();
        }
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }
        fileOrDirectory.delete();
    }

    private void startDownload(final SoundpackItem item) {
        item.status = "Downloading";
        item.progress = 0;
        updateCatalogList();

        File tempZip = new File(getCacheDir(), item.id + ".zip");
        File destFolder = new File(getExternalFilesDir("soundpacks"), item.id);

        DownloadTask task = new DownloadTask(tempZip, destFolder, item.name, new DownloadCallback() {
            @Override
            public void onProgress(int percent) {
                item.progress = percent;
                updateCatalogList();
            }

            @Override
            public void onComplete() {
                item.status = "Installed";
                Toast.makeText(SoundpackDownloadActivity.this, "Successfully installed " + item.name + "!", Toast.LENGTH_LONG).show();
                updateCatalogList();
            }

            @Override
            public void onError(String error) {
                item.status = "Available";
                Toast.makeText(SoundpackDownloadActivity.this, "Error installing " + item.name + ": " + error, Toast.LENGTH_LONG).show();
                updateCatalogList();
            }
        });

        task.execute(item.downloadUrl);
    }

    private void addLog(final String msg) {
        runOnUiThread(() -> {
            if (mDebugConsole != null) {
                mDebugConsole.append("> " + msg + "\n");
                if (mDebugScroll != null) {
                    mDebugScroll.post(() -> mDebugScroll.fullScroll(View.FOCUS_DOWN));
                }
            }
        });
    }

    private String resolveZipUrl(String pageUrl) {
        if (pageUrl.endsWith(".zip")) {
            return pageUrl;
        }
        addLog("Resolving ZIP link from details page: " + pageUrl);
        try {
            URL url = new URL(pageUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/120 Safari/537.36");
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            
            InputStream in = new BufferedInputStream(conn.getInputStream());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            in.close();
            
            String html = out.toString("UTF-8");
            int zipIndex = html.indexOf(".zip");
            if (zipIndex != -1) {
                int startQuote = html.lastIndexOf('"', zipIndex);
                int startParen = html.lastIndexOf('(', zipIndex);
                int start = Math.max(startQuote, startParen);
                if (start != -1) {
                    String zipUrl = html.substring(start + 1, zipIndex + 4);
                    if (zipUrl.startsWith("/")) {
                        zipUrl = "https://mechvibes.com" + zipUrl;
                    }
                    addLog("Found direct ZIP download URL: " + zipUrl);
                    return zipUrl;
                }
            }
            addLog("Warning: Could not find direct ZIP link in HTML content.");
        } catch (Exception e) {
            addLog("ZIP resolution failed: " + e.getMessage());
        }
        return pageUrl; // Fallback
    }

    private void stopPreviewPlayback() {
        if (mMediaPlayer != null) {
            try {
                mMediaPlayer.stop();
                mMediaPlayer.release();
            } catch (Exception e) {
                // Ignore
            }
            mMediaPlayer = null;
        }
        if (mPlayingItem != null) {
            mPlayingItem.previewStatus = "PLAY";
            mPlayingItem = null;
        }
        updateCatalogList();
    }

    private void playPreview(final SoundpackItem item) {
        if (mPlayingItem == item) {
            stopPreviewPlayback();
            return;
        }
        stopPreviewPlayback();

        // 1. Check if installed
        File rootDir = getExternalFilesDir("soundpacks");
        File installedWav = null;
        if (rootDir != null) {
            File dir = new File(rootDir, item.id);
            if (dir.exists() && dir.isDirectory()) {
                File wav = new File(dir, "standard.wav");
                if (wav.exists()) {
                    installedWav = wav;
                }
            }
        }

        if (installedWav != null) {
            playWavFile(installedWav, item);
            return;
        }

        // 2. Check if cached preview exists
        final File previewWav = new File(getCacheDir(), item.id + "_preview.wav");
        if (previewWav.exists()) {
            playWavFile(previewWav, item);
            return;
        }

        // 3. Otherwise, fetch and prepare
        item.previewStatus = "BUFFERING";
        mPlayingItem = item;
        updateCatalogList();

        new PreparePreviewTask(item, previewWav).execute();
    }

    private void playWavFile(final File wavFile, final SoundpackItem item) {
        try {
            mMediaPlayer = new MediaPlayer();
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                mMediaPlayer.setAudioAttributes(
                    new android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());
            }
            mMediaPlayer.setDataSource(wavFile.getAbsolutePath());
            mMediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                item.previewStatus = "STOP";
                mPlayingItem = item;
                updateCatalogList();
            });
            mMediaPlayer.setOnCompletionListener(mp -> stopPreviewPlayback());
            mMediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(SoundpackDownloadActivity.this,
                    "Playback error (" + what + "/" + extra + ")", Toast.LENGTH_SHORT).show();
                stopPreviewPlayback();
                return true;
            });
            item.previewStatus = "BUFFERING";
            mPlayingItem = item;
            updateCatalogList();
            mMediaPlayer.prepareAsync();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to play preview: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            stopPreviewPlayback();
        }
    }

    private boolean extractPreviewWav(File zipFile, File destPreviewWav) {
        File workDir = new File(getCacheDir(), "work_preview_" + System.currentTimeMillis());
        try {
            if (!workDir.exists()) workDir.mkdirs();
            ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
            ZipEntry ze;
            byte[] buffer = new byte[4096];
            File configJsonFile = null;
            File mainAudioFile = null;
            List<File> allExtractedAudioFiles = new ArrayList<>();

            while ((ze = zis.getNextEntry()) != null) {
                String fileName = ze.getName();
                int lastSlash = fileName.lastIndexOf('/');
                if (lastSlash != -1) {
                    fileName = fileName.substring(lastSlash + 1);
                }
                if (fileName.isEmpty() || ze.isDirectory()) {
                    zis.closeEntry();
                    continue;
                }
                File file = new File(workDir, fileName);
                FileOutputStream fos = new FileOutputStream(file);
                int len;
                while ((len = zis.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
                zis.closeEntry();

                if (fileName.equals("config.json")) {
                    configJsonFile = file;
                } else if (fileName.endsWith(".ogg") || fileName.endsWith(".mp3") || fileName.endsWith(".wav")) {
                    allExtractedAudioFiles.add(file);
                    if (fileName.contains("sound")) {
                        mainAudioFile = file;
                    }
                }
            }
            zis.close();

            if (configJsonFile == null || !configJsonFile.exists()) {
                deleteRecursive(workDir);
                return false;
            }

            FileInputStream fis = new FileInputStream(configJsonFile);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int len;
            while ((len = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            fis.close();
            String configJsonStr = baos.toString("UTF-8");
            JSONObject json = new JSONObject(configJsonStr);
            JSONObject defines = json.optJSONObject("defines");
            if (defines == null) {
                deleteRecursive(workDir);
                return false;
            }

            boolean usesSlices = false;
            Iterator<String> keys = defines.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object val = defines.get(key);
                if (val instanceof JSONArray) {
                    usesSlices = true;
                    break;
                }
            }

            boolean success = false;
            if (usesSlices) {
                if (mainAudioFile == null && !allExtractedAudioFiles.isEmpty()) {
                    mainAudioFile = allExtractedAudioFiles.get(0);
                }
                if (mainAudioFile != null && mainAudioFile.exists()) {
                    JSONArray standardArr = defines.optJSONArray("30");
                    if (standardArr == null) {
                        Iterator<String> defineKeys = defines.keys();
                        while (defineKeys.hasNext()) {
                            String k = defineKeys.next();
                            JSONArray arr = defines.optJSONArray(k);
                            if (arr != null) {
                                standardArr = arr;
                                break;
                            }
                        }
                    }
                    if (standardArr != null) {
                        success = AudioDecoderSlicer.sliceAudio(mainAudioFile, destPreviewWav, standardArr.getInt(0), standardArr.getInt(1));
                    }
                }
            } else {
                String standardFileStr = defines.optString("30", "");
                if (standardFileStr.isEmpty()) {
                    Iterator<String> defineKeys = defines.keys();
                    while (defineKeys.hasNext()) {
                        String k = defineKeys.next();
                        String str = defines.optString(k);
                        if (str != null && !str.isEmpty()) {
                            standardFileStr = str;
                            break;
                        }
                    }
                }
                if (!standardFileStr.isEmpty()) {
                    int lastSlash = standardFileStr.lastIndexOf('/');
                    if (lastSlash != -1) {
                        standardFileStr = standardFileStr.substring(lastSlash + 1);
                    }
                    File src = new File(workDir, standardFileStr);
                    if (src.exists()) {
                        if (src.getName().endsWith(".wav")) {
                            FileInputStream in = new FileInputStream(src);
                            FileOutputStream out = new FileOutputStream(destPreviewWav);
                            while ((len = in.read(buffer)) != -1) {
                                out.write(buffer, 0, len);
                            }
                            in.close();
                            out.close();
                            success = true;
                        } else {
                            success = AudioDecoderSlicer.sliceAudio(src, destPreviewWav, 0, 30000);
                        }
                    }
                }
            }

            deleteRecursive(workDir);
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Failed to extract preview WAV", e);
            deleteRecursive(workDir);
            return false;
        }
    }

    private class PreparePreviewTask extends AsyncTask<Void, Void, Boolean> {
        private final SoundpackItem item;
        private final File previewWav;

        PreparePreviewTask(SoundpackItem item, File previewWav) {
            this.item = item;
            this.previewWav = previewWav;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                // Use a distinct prefix so DownloadTask cleanup doesn't delete this cache
                File cachedZip = new File(getCacheDir(), "preview_" + item.id + ".zip");
                if (!cachedZip.exists()) {
                    String urlString = resolveZipUrl(item.downloadUrl);
                    URL url = new URL(urlString);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setInstanceFollowRedirects(true);
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/120 Safari/537.36");

                    int redirectCount = 0;
                    while (redirectCount < 5) {
                        int status = conn.getResponseCode();
                        if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                            status == HttpURLConnection.HTTP_MOVED_PERM ||
                            status == 307 || status == 308) {
                            String newUrl = conn.getHeaderField("Location");
                            if (newUrl != null) {
                                url = new URL(newUrl);
                                conn = (HttpURLConnection) url.openConnection();
                                conn.setConnectTimeout(10000);
                                conn.setReadTimeout(10000);
                                redirectCount++;
                                continue;
                            }
                        }
                        break;
                    }

                    InputStream input = new BufferedInputStream(conn.getInputStream());
                    FileOutputStream output = new FileOutputStream(cachedZip);
                    byte[] data = new byte[4096];
                    int count;
                    while ((count = input.read(data)) != -1) {
                        output.write(data, 0, count);
                    }
                    output.flush();
                    output.close();
                    input.close();
                }

                return extractPreviewWav(cachedZip, previewWav);
            } catch (Exception e) {
                Log.e(TAG, "PreparePreviewTask failed", e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (isFinishing() || isDestroyed()) return;
            if (mPlayingItem == item) {
                if (success && previewWav.exists()) {
                    playWavFile(previewWav, item);
                } else {
                    Toast.makeText(SoundpackDownloadActivity.this, "Failed to load preview sound", Toast.LENGTH_SHORT).show();
                    stopPreviewPlayback();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPreviewPlayback();
    }

    private void scrapeLiveSoundpacks() {
        mMainProgressBar.setVisibility(View.VISIBLE);
        mMainProgressBar.setIndeterminate(true);
        mLoadingText.setText("Connecting to mechvibes.com soundpacks...");
        mLoadingText.setVisibility(View.VISIBLE);

        final java.util.HashSet<String> defaultIds = new java.util.HashSet<>();
        for (SoundpackItem item : mSoundpacks) {
            if (item.isDefault) {
                defaultIds.add(item.id.replace("_", "-"));
            }
        }

        new AsyncTask<Void, Void, List<SoundpackItem>>() {
            @Override
            protected List<SoundpackItem> doInBackground(Void... voids) {
                List<SoundpackItem> list = new ArrayList<>();
                boolean success = false;
                try {
                    URL url = new URL("https://mechvibes.com/sound-packs/");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/120 Safari/537.36");
                    conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                    
                    InputStream in = new BufferedInputStream(conn.getInputStream());
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) != -1) {
                        out.write(buf, 0, len);
                    }
                    in.close();
                    
                    String html = out.toString("UTF-8");
                    
                    // Extremely robust and clean parsing of class="sound-pack " blocks
                    int searchIdx = 0;
                    while (true) {
                        int index = html.indexOf("class=\"sound-pack", searchIdx);
                        if (index == -1) break;

                        // Extract pack="[PACK_ID]"
                        int packAttrIndex = html.indexOf("pack=\"", index);
                        if (packAttrIndex == -1 || packAttrIndex - index > 150) {
                            searchIdx = index + 15;
                            continue;
                        }

                        int idStart = packAttrIndex + 6;
                        int idEnd = html.indexOf("\"", idStart);
                        if (idEnd == -1) break;
                        String id = html.substring(idStart, idEnd);

                        searchIdx = idEnd;

                        // Find the <div class="pack-details"> and first <div> which is the human-readable name!
                        int detailsIndex = html.indexOf("class=\"pack-details\"", idEnd);
                        String packName = "";
                        if (detailsIndex != -1 && detailsIndex - idEnd < 500) {
                            int nameDivIndex = html.indexOf("<div>", detailsIndex);
                            if (nameDivIndex != -1 && nameDivIndex - detailsIndex < 150) {
                                int nameEndIndex = html.indexOf("</div>", nameDivIndex + 5);
                                if (nameEndIndex != -1) {
                                    packName = html.substring(nameDivIndex + 5, nameEndIndex).trim();
                                    // Decode HTML entities
                                    packName = packName.replace("&amp;", "&")
                                                       .replace("&lt;", "<")
                                                       .replace("&gt;", ">")
                                                       .replace("&quot;", "\"")
                                                       .replace("&#39;", "'");
                                }
                            }
                        }

                        if (packName.isEmpty()) {
                            // Fallback to pretty-printing ID if name parse failed
                            String cleanName = id.replace("custom-sound-pack-", "")
                                                 .replace("sound-pack-", "")
                                                 .replace("traveler-", "")
                                                 .replace("travler-", "")
                                                 .replace("-", " ");
                            if (cleanName.length() > 1) {
                                cleanName = Character.toUpperCase(cleanName.charAt(0)) + cleanName.substring(1);
                            }
                            packName = cleanName;
                        }

                        // Determine the category based on name or ID to auto-classify switches!
                        String type = "Community Switch";
                        String lowerName = packName.toLowerCase();
                        if (lowerName.contains("clicky") || lowerName.contains("blue") || lowerName.contains("click")) {
                            type = "Clicky Switch";
                        } else if (lowerName.contains("tactile") || lowerName.contains("brown") || lowerName.contains("panda")) {
                            type = "Tactile Switch";
                        } else if (lowerName.contains("linear") || lowerName.contains("red") || lowerName.contains("black") || lowerName.contains("yellow")) {
                            type = "Linear Switch";
                        } else if (lowerName.contains("thock") || lowerName.contains("creams")) {
                            type = "Tactile Thock";
                        } else if (lowerName.contains("model m") || lowerName.contains("buckling")) {
                            type = "Buckling Spring";
                        } else if (lowerName.contains("topre")) {
                            type = "Electrostatic Tactile";
                        }

                        // Deduplicate URL
                        boolean exists = false;
                        for (SoundpackItem existing : list) {
                            if (existing.id.equals(id.replace("-", "_"))) {
                                exists = true;
                                break;
                            }
                        }

                        if (!exists) {
                            // Deduplicate standard defaults
                            if (!defaultIds.contains(id)) {
                                String downloadUrl = "https://mechvibes.com/sound-packs/" + id;
                                list.add(new SoundpackItem(id.replace("-", "_"), packName, downloadUrl, type));
                            }
                        }
                    }
                    success = true;
                } catch (Exception e) {
                    Log.e(TAG, "Scraping failed", e);
                }
                return success ? list : null;
            }

            @Override
            protected void onPostExecute(List<SoundpackItem> scrapedItems) {
                if (isFinishing() || isDestroyed()) return;
                mMainProgressBar.setVisibility(View.GONE);
                mLoadingText.setVisibility(View.GONE);

                if (scrapedItems == null) {
                    Toast.makeText(SoundpackDownloadActivity.this, "Scraping failed. Check network connection.", Toast.LENGTH_LONG).show();
                    return;
                }

                // Add to mSoundpacks list ensuring deduplication
                int newCount = 0;
                for (SoundpackItem scraped : scrapedItems) {
                    boolean found = false;
                    for (SoundpackItem old : mSoundpacks) {
                        if (old.id.equals(scraped.id)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        mSoundpacks.add(scraped);
                        newCount++;
                    }
                }

                mFilteredSoundpacks.clear();
                mFilteredSoundpacks.addAll(mSoundpacks);
                updateCatalogList();
                saveScrapedSoundpacks();
                
                if (newCount > 0) {
                    Toast.makeText(SoundpackDownloadActivity.this, "Loaded " + newCount + " new community soundpacks!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SoundpackDownloadActivity.this, "Catalog is up to date.", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }

    private class DownloadTask extends AsyncTask<String, Integer, String> {
        private final File tempZip;
        private final File destFolder;
        private final String packName;
        private final DownloadCallback callback;

        DownloadTask(File tempZip, File destFolder, String packName, DownloadCallback callback) {
            this.tempZip = tempZip;
            this.destFolder = destFolder;
            this.packName = packName;
            this.callback = callback;
        }



        @Override
        protected String doInBackground(String... urls) {
            try {
                String initialUrl = urls[0];
                addLog("Initializing download task...");
                
                // Resolve direct ZIP download link first!
                String urlString = resolveZipUrl(initialUrl);
                addLog("Target download URL: " + urlString);
                
                HttpURLConnection conn = null;
                int redirectCount = 0;
                while (redirectCount < 5) {
                    addLog("Connecting (attempt " + (redirectCount + 1) + ")...");
                    URL url = new URL(urlString);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setInstanceFollowRedirects(true);
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/120 Safari/537.36");
                    
                    int status = conn.getResponseCode();
                    addLog("Response Status Code: " + status + " (" + conn.getResponseMessage() + ")");
                    
                    if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                        status == HttpURLConnection.HTTP_MOVED_PERM ||
                        status == 307 || status == 308) {
                        
                        String newUrl = conn.getHeaderField("Location");
                        if (newUrl != null) {
                            addLog("Redirecting from " + urlString + " to " + newUrl);
                            urlString = newUrl;
                            redirectCount++;
                            continue;
                        }
                    }
                    break;
                }

                if (conn == null) {
                    return "Could not establish connection";
                }

                int fileLength = conn.getContentLength();
                addLog("Download Stream opened. File Length: " + fileLength + " bytes");
                InputStream input = new BufferedInputStream(conn.getInputStream());
                FileOutputStream output = new FileOutputStream(tempZip);

                addLog("Downloading ZIP archive to temp storage...");
                byte[] data = new byte[4096];
                long total = 0;
                int count;
                int lastPercent = -1;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    if (fileLength > 0) {
                        int percent = (int) (total * 100 / fileLength);
                        if (percent >= lastPercent + 2 || percent == 100) {
                            publishProgress(percent);
                            lastPercent = percent;
                        }
                    }
                    output.write(data, 0, count);
                }

                output.flush();
                output.close();
                input.close();
                addLog("Download complete. Temp ZIP size: " + tempZip.length() + " bytes");

                // Unzip and slice soundpack!
                addLog("Starting Zip extraction & processing pipeline...");
                boolean installSuccess = extractAndSliceSoundpack(tempZip, destFolder, packName);
                if (!installSuccess) {
                    return "Failed to parse soundpack configuration or slice audio files";
                }

                addLog("Soundpack " + destFolder.getName() + " installed successfully!");
                return null;
            } catch (Exception e) {
                Log.e(TAG, "Download failed", e);
                addLog("EXCEPTION ENCOUNTERED IN BACKGROUND TASK:");
                addLog("Error Message: " + e.toString());
                addLog("Stack Trace:\n" + android.util.Log.getStackTraceString(e));
                return e.getMessage();
            } finally {
                if (tempZip.exists()) {
                    tempZip.delete();
                }
            }
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            if (isFinishing() || isDestroyed()) return;
            callback.onProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            if (isFinishing() || isDestroyed()) return;
            if (result == null) {
                callback.onComplete();
            } else {
                callback.onError(result);
            }
        }

        private boolean extractAndSliceSoundpack(File zipFile, File destDir, String packName) {
            try {
                if (!destDir.exists()) {
                    destDir.mkdirs();
                }
                java.util.Map<String, File> decodedCache = new java.util.HashMap<>();

                // Extract Zip contents to a temporary work folder
                File workDir = new File(destDir.getParentFile(), "work_" + destDir.getName());
                if (!workDir.exists()) {
                    workDir.mkdirs();
                }

                addLog("Extracting Zip files...");
                ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
                ZipEntry ze;
                byte[] buffer = new byte[4096];

                File configJsonFile = null;
                File mainAudioFile = null;
                List<File> allExtractedAudioFiles = new ArrayList<>();

                while ((ze = zis.getNextEntry()) != null) {
                    String fileName = ze.getName();
                    // Resolve nested folders by taking only the basename
                    int lastSlash = fileName.lastIndexOf('/');
                    if (lastSlash != -1) {
                        fileName = fileName.substring(lastSlash + 1);
                    }

                    if (fileName.isEmpty() || ze.isDirectory()) {
                        zis.closeEntry();
                        continue;
                    }

                    File file = new File(workDir, fileName);
                    FileOutputStream fos = new FileOutputStream(file);
                    int len;
                    while ((len = zis.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                    zis.closeEntry();
                    
                    Log.d(TAG, "Extracted file: " + fileName);

                    if (fileName.equals("config.json")) {
                        configJsonFile = file;
                    } else if (fileName.endsWith(".ogg") || fileName.endsWith(".mp3") || fileName.endsWith(".wav")) {
                        allExtractedAudioFiles.add(file);
                        if (fileName.contains("sound")) {
                            mainAudioFile = file;
                        }
                    }
                }
                zis.close();

                if (configJsonFile == null || !configJsonFile.exists()) {
                    addLog("ERROR: config.json not found in Zip file root.");
                    deleteRecursive(workDir);
                    return false;
                }

                // Read config.json
                addLog("Reading and parsing config.json...");
                FileInputStream fis = new FileInputStream(configJsonFile);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int len;
                while ((len = fis.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
                fis.close();
                String configJsonStr = baos.toString("UTF-8");
                JSONObject json = new JSONObject(configJsonStr);

                JSONObject defines = json.optJSONObject("defines");
                if (defines == null) {
                    addLog("ERROR: config.json 'defines' block is null or invalid.");
                    deleteRecursive(workDir);
                    return false;
                }

                // Check if this uses individual keys or a single merged file
                boolean usesSlices = false;
                // If any define value is an Array (e.g. [offset, duration]), we slice!
                Iterator<String> keys = defines.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    Object val = defines.get(key);
                    if (val instanceof JSONArray) {
                        usesSlices = true;
                        break;
                    }
                }

                if (usesSlices) {
                    addLog("Configuration type: Merged audio slicing mode.");
                    if (mainAudioFile == null && !allExtractedAudioFiles.isEmpty()) {
                        mainAudioFile = allExtractedAudioFiles.get(0);
                    }

                    if (mainAudioFile == null || !mainAudioFile.exists()) {
                        addLog("ERROR: No merged audio file (.ogg/.mp3/.wav) found in extracted files.");
                        deleteRecursive(workDir);
                        return false;
                    }
                    
                    addLog("Using merged audio file: " + mainAudioFile.getName() + " (" + mainAudioFile.length() + " bytes)");

                    addLog("Decoding merged audio file to memory...");
                    AudioDecoderSlicer.DecodedAudio decoded = AudioDecoderSlicer.decodeAudio(mainAudioFile);
                    if (decoded == null) {
                        addLog("ERROR: Failed to decode merged audio file.");
                        deleteRecursive(workDir);
                        return false;
                    }

                    // Slice every defined key in the config
                    int sliceCount = 0;
                    Iterator<String> defineKeys = defines.keys();
                    while (defineKeys.hasNext()) {
                        String key = defineKeys.next();
                        JSONArray arr = defines.optJSONArray(key);
                        if (arr != null && arr.length() >= 2) {
                            try {
                                int offset = arr.getInt(0);
                                int duration = arr.getInt(1);
                                File destFile = new File(destDir, key + ".wav");
                                boolean success = AudioDecoderSlicer.sliceAudioFromDecoded(decoded, destFile, offset, duration);
                                if (success) {
                                    sliceCount++;
                                }
                            } catch (Exception e) {
                                // Ignore faulty individual key mapping
                            }
                        }
                    }
                    addLog("Successfully sliced " + sliceCount + " key sounds.");

                    // Generate standard fallbacks for backwards compatibility
                    JSONArray standardArr = defines.optJSONArray("30"); // Keycode 'A'
                    if (standardArr == null) {
                        Iterator<String> dk = defines.keys();
                        while (dk.hasNext()) {
                            String k = dk.next();
                            JSONArray arr = defines.optJSONArray(k);
                            if (arr != null) {
                                standardArr = arr;
                                break;
                            }
                        }
                    }

                    JSONArray spacebarArr = defines.optJSONArray("57");
                    if (spacebarArr == null) spacebarArr = standardArr;

                    JSONArray deleteArr = defines.optJSONArray("14");
                    if (deleteArr == null) deleteArr = standardArr;

                    JSONArray returnArr = defines.optJSONArray("28");
                    if (returnArr == null) returnArr = standardArr;

                    if (standardArr != null) AudioDecoderSlicer.sliceAudioFromDecoded(decoded, new File(destDir, "standard.wav"), standardArr.getInt(0), standardArr.getInt(1));
                    if (spacebarArr != null) AudioDecoderSlicer.sliceAudioFromDecoded(decoded, new File(destDir, "spacebar.wav"), spacebarArr.getInt(0), spacebarArr.getInt(1));
                    if (deleteArr != null) AudioDecoderSlicer.sliceAudioFromDecoded(decoded, new File(destDir, "delete.wav"), deleteArr.getInt(0), deleteArr.getInt(1));
                    if (returnArr != null) AudioDecoderSlicer.sliceAudioFromDecoded(decoded, new File(destDir, "return.wav"), returnArr.getInt(0), returnArr.getInt(1));

                } else {
                    addLog("Configuration type: Separate audio file copying mode.");
                    int copyCount = 0;
                    Iterator<String> defineKeys = defines.keys();
                    while (defineKeys.hasNext()) {
                        String key = defineKeys.next();
                        String fileStr = defines.optString(key, "");
                        if (!fileStr.isEmpty()) {
                            fileStr = cleanPath(fileStr);
                            File src = new File(workDir, fileStr);
                            File dest = new File(destDir, key + ".wav");
                            copyAudioFile(src, dest, decodedCache);
                            copyCount++;
                        }
                    }
                    addLog("Successfully copied/decoded " + copyCount + " separate key sounds.");

                    // Generate standard fallbacks for backwards compatibility
                    String standardFileStr = defines.optString("30", "");
                    if (standardFileStr.isEmpty()) {
                        Iterator<String> dk = defines.keys();
                        while (dk.hasNext()) {
                            String k = dk.next();
                            String str = defines.optString(k, "");
                            if (!str.isEmpty()) {
                                standardFileStr = str;
                                break;
                            }
                        }
                    }
                    String spacebarFileStr = defines.optString("57", standardFileStr);
                    String deleteFileStr = defines.optString("14", standardFileStr);
                    String returnFileStr = defines.optString("28", standardFileStr);

                    if (!standardFileStr.isEmpty()) copyAudioFile(new File(workDir, cleanPath(standardFileStr)), new File(destDir, "standard.wav"), decodedCache);
                    if (!spacebarFileStr.isEmpty()) copyAudioFile(new File(workDir, cleanPath(spacebarFileStr)), new File(destDir, "spacebar.wav"), decodedCache);
                    if (!deleteFileStr.isEmpty()) copyAudioFile(new File(workDir, cleanPath(deleteFileStr)), new File(destDir, "delete.wav"), decodedCache);
                    if (!returnFileStr.isEmpty()) copyAudioFile(new File(workDir, cleanPath(returnFileStr)), new File(destDir, "return.wav"), decodedCache);
                }

                // Save name metadata in config.json inside destination directory
                try {
                    JSONObject destJson = new JSONObject();
                    destJson.put("name", packName);
                    File configFile = new File(destDir, "config.json");
                    FileOutputStream fos = new FileOutputStream(configFile);
                    fos.write(destJson.toString(2).getBytes("UTF-8"));
                    fos.close();
                } catch (Exception e) {
                    addLog("Warning: Could not write config.json metadata: " + e.getMessage());
                }

                // Cleanup Work Directory
                addLog("Cleaning up temporary work folder...");
                deleteRecursive(workDir);
                return true;

            } catch (Exception e) {
                addLog("UNZIPPING PIPELINE EXCEPTION:");
                addLog("Error Message: " + e.toString());
                addLog("Stack Trace:\n" + android.util.Log.getStackTraceString(e));
                return false;
            }
        }

        private String cleanPath(String path) {
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash != -1) {
                return path.substring(lastSlash + 1);
            }
            return path;
        }

        private void copyAudioFile(File src, File dest, java.util.Map<String, File> decodedCache) {
            if (!src.exists()) {
                addLog("Warning: audio file not found: " + src.getName());
                return;
            }
            try {
                // If it is already a WAV file, copy directly
                if (src.getName().endsWith(".wav")) {
                    copyFileDirectly(src, dest);
                } else {
                    String srcPath = src.getAbsolutePath();
                    if (decodedCache.containsKey(srcPath)) {
                        File cachedWav = decodedCache.get(srcPath);
                        if (cachedWav != null && cachedWav.exists()) {
                            copyFileDirectly(cachedWav, dest);
                            return;
                        }
                    }
                    // Decode OGG/MP3 to standard PCM WAV directly!
                    addLog("Decoding " + src.getName() + " to WAV...");
                    boolean success = AudioDecoderSlicer.sliceAudio(src, dest, 0, 30000); // Decode full file up to 30s
                    if (success) {
                        decodedCache.put(srcPath, dest);
                    }
                }
            } catch (Exception e) {
                addLog("Copying file failed: " + src.getName() + " -> " + e.getMessage());
            }
        }

        private void copyFileDirectly(File src, File dest) throws java.io.IOException {
            FileInputStream in = new FileInputStream(src);
            FileOutputStream out = new FileOutputStream(dest);
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            in.close();
            out.close();
        }

        private void deleteRecursive(File fileOrDirectory) {
            if (fileOrDirectory.isDirectory()) {
                for (File child : fileOrDirectory.listFiles()) {
                    deleteRecursive(child);
                }
            }
            fileOrDirectory.delete();
        }
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            super.onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
