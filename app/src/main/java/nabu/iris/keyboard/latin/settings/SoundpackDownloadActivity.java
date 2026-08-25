package nabu.iris.keyboard.latin.settings;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.media.MediaPlayer;
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
import android.view.WindowInsets;
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
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import nabu.iris.keyboard.R;
import nabu.iris.keyboard.compat.PreferenceManagerCompat;
import nabu.iris.keyboard.latin.AudioAndHapticFeedbackManager;
import nabu.iris.keyboard.latin.AudioDecoderSlicer;
import nabu.iris.keyboard.latin.settings.SettingsValues;

public class SoundpackDownloadActivity extends Activity {
    private static final String TAG = "SoundpackDownload";

    private LinearLayout mMainLayout;
    private LinearLayout mCatalogLayout;
    private EditText mSearchInput;
    private EditText mCustomUrlInput;
    private ProgressBar mMainProgressBar;
    private TextView mLoadingText;
    private TextView mStatsBadge;
    private TextView mScrapeBtn;

    private List<SoundpackItem> mSoundpacks = new ArrayList<>();
    private List<SoundpackItem> mFilteredSoundpacks = new ArrayList<>();
    private MediaPlayer mMediaPlayer;
    private SoundpackItem mPlayingItem = null;

    private int mThemeBgColor;
    private int mThemeCardColor;
    private int mThemeStrokeColor;
    private int mThemeAccentColor;
    private int mThemeTextPrimary;
    private int mThemeTextSecondary;
    private boolean mIsDarkTheme;
    private boolean mIsAmoled;

    private interface DownloadCallback {
        void onProgress(int percent);
        void onComplete();
        void onError(String error);
    }

    private static class SoundpackItem {
        String id;
        String name;
        String downloadUrl;
        String type;
        String status = "Available";
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

        mIsDarkTheme = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(this);
        mIsAmoled = mIsDarkTheme && prefs.getBoolean("pref_amoled_dark_mode", false);

        mThemeBgColor = mIsAmoled ? 0xFF000000 : getResources().getColor(R.color.settings_bg);
        mThemeCardColor = mIsAmoled ? 0xFF121214 : getResources().getColor(R.color.settings_card_bg);
        mThemeStrokeColor = mIsAmoled ? 0xFF28282B : getResources().getColor(R.color.settings_card_stroke);
        mThemeAccentColor = Settings.getMaterialYouAccentColor(this);
        mThemeTextPrimary = getResources().getColor(R.color.settings_text_primary);
        mThemeTextSecondary = getResources().getColor(R.color.settings_text_secondary);

        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(mThemeBgColor);
            window.setNavigationBarColor(mThemeBgColor);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int flags = window.getDecorView().getSystemUiVisibility();
                if (!mIsDarkTheme) {
                    flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                    }
                } else {
                    flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                    }
                }
                window.getDecorView().setSystemUiVisibility(flags);
            }
        }

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setTitle("Soundpack Store");
            GradientDrawable abBg = new GradientDrawable();
            abBg.setColor(mThemeBgColor);
            actionBar.setBackgroundDrawable(abBg);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                actionBar.setElevation(0);
            }
        }

        ScrollView rootScroll = new ScrollView(this);
        rootScroll.setFillViewport(true);
        rootScroll.setBackgroundColor(mThemeBgColor);

        mMainLayout = new LinearLayout(this);
        mMainLayout.setOrientation(LinearLayout.VERTICAL);
        mMainLayout.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(24));
        rootScroll.addView(mMainLayout);

        setContentView(rootScroll);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            rootScroll.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets sbInsets = insets.getInsets(WindowInsets.Type.systemBars());
                view.setPadding(0, 0, 0, sbInsets.bottom);
                return WindowInsets.CONSUMED;
            });
        }

        setupHeaderAndScraper();
        setupSearchBar();
        setupCustomImportCard();
        setupCatalogContainer();

        loadSoundpacks();
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }

    private int getTranslucentColor(int color, int alphaPercent) {
        int alpha = (int) (255 * (alphaPercent / 100.0));
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private void setupHeaderAndScraper() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, dpToPx(4), 0, dpToPx(12));

        mStatsBadge = new TextView(this);
        mStatsBadge.setText("Soundpacks");
        mStatsBadge.setTextColor(mThemeAccentColor);
        mStatsBadge.setTextSize(13f);
        mStatsBadge.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));

        GradientDrawable statsBg = new GradientDrawable();
        statsBg.setShape(GradientDrawable.RECTANGLE);
        statsBg.setCornerRadius(dpToPx(12));
        statsBg.setColor(getTranslucentColor(mThemeAccentColor, 15));
        mStatsBadge.setBackground(statsBg);
        mStatsBadge.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));
        header.addView(mStatsBadge);

        View spacer = new View(this);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0, 1, 1.0f);
        spacer.setLayoutParams(sp);
        header.addView(spacer);

        // Scrape Live Website Button
        mScrapeBtn = new TextView(this);
        mScrapeBtn.setText("SCRAPE LIVE WEBSITE");
        mScrapeBtn.setTextColor(mIsDarkTheme ? 0xFF000000 : 0xFFFFFFFF);
        mScrapeBtn.setTextSize(11.5f);
        mScrapeBtn.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        mScrapeBtn.setPadding(dpToPx(14), dpToPx(7), dpToPx(14), dpToPx(7));
        mScrapeBtn.setGravity(Gravity.CENTER);

        GradientDrawable scrapeBg = new GradientDrawable();
        scrapeBg.setShape(GradientDrawable.RECTANGLE);
        scrapeBg.setCornerRadius(dpToPx(12));
        scrapeBg.setColor(mThemeAccentColor);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            GradientDrawable mask = new GradientDrawable();
            mask.setShape(GradientDrawable.RECTANGLE);
            mask.setCornerRadius(dpToPx(12));
            mask.setColor(0xFFFFFFFF);
            RippleDrawable ripple = new RippleDrawable(ColorStateList.valueOf(0x40FFFFFF), scrapeBg, mask);
            mScrapeBtn.setBackground(ripple);
        } else {
            mScrapeBtn.setBackground(scrapeBg);
        }

        mScrapeBtn.setClickable(true);
        mScrapeBtn.setFocusable(true);
        mScrapeBtn.setOnClickListener(v -> scrapeLiveSoundpacks());
        header.addView(mScrapeBtn);

        mMainLayout.addView(header);
    }

    private void setupSearchBar() {
        mSearchInput = new EditText(this);
        mSearchInput.setHint("Search switches...");
        mSearchInput.setHintTextColor(mThemeTextSecondary);
        mSearchInput.setTextColor(mThemeTextPrimary);
        mSearchInput.setTextSize(14f);
        mSearchInput.setSingleLine(true);
        mSearchInput.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));

        GradientDrawable searchBg = new GradientDrawable();
        searchBg.setShape(GradientDrawable.RECTANGLE);
        searchBg.setCornerRadius(dpToPx(16));
        searchBg.setColor(mThemeCardColor);
        if (mThemeStrokeColor != 0) {
            searchBg.setStroke(dpToPx(1), mThemeStrokeColor);
        }
        mSearchInput.setBackground(searchBg);

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
    }

    private void setupCustomImportCard() {
        LinearLayout importCard = new LinearLayout(this);
        importCard.setOrientation(LinearLayout.VERTICAL);
        importCard.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));

        GradientDrawable importBg = new GradientDrawable();
        importBg.setShape(GradientDrawable.RECTANGLE);
        importBg.setCornerRadius(dpToPx(16));
        importBg.setColor(mThemeCardColor);
        if (mThemeStrokeColor != 0) {
            importBg.setStroke(dpToPx(1), mThemeStrokeColor);
        }
        importCard.setBackground(importBg);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dpToPx(14));
        importCard.setLayoutParams(cardParams);

        TextView importTitle = new TextView(this);
        importTitle.setText("IMPORT CUSTOM SOUNDPACK");
        importTitle.setTextColor(mThemeAccentColor);
        importTitle.setTextSize(11.5f);
        importTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        importTitle.setLetterSpacing(0.06f);
        importCard.addView(importTitle);

        LinearLayout importRow = new LinearLayout(this);
        importRow.setOrientation(LinearLayout.HORIZONTAL);
        importRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, dpToPx(8), 0, 0);
        importRow.setLayoutParams(rowParams);

        mCustomUrlInput = new EditText(this);
        mCustomUrlInput.setHint("Paste Mechvibes ZIP or page URL...");
        mCustomUrlInput.setHintTextColor(mThemeTextSecondary);
        mCustomUrlInput.setTextColor(mThemeTextPrimary);
        mCustomUrlInput.setTextSize(13f);
        mCustomUrlInput.setSingleLine(true);
        mCustomUrlInput.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));

        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setShape(GradientDrawable.RECTANGLE);
        inputBg.setCornerRadius(dpToPx(12));
        inputBg.setColor(getTranslucentColor(mThemeAccentColor, 8));
        inputBg.setStroke(dpToPx(1), getTranslucentColor(mThemeAccentColor, 30));
        mCustomUrlInput.setBackground(inputBg);

        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        inputParams.setMargins(0, 0, dpToPx(8), 0);
        mCustomUrlInput.setLayoutParams(inputParams);
        importRow.addView(mCustomUrlInput);

        TextView importBtn = new TextView(this);
        importBtn.setText("IMPORT");
        importBtn.setTextColor(mIsDarkTheme ? 0xFF000000 : 0xFFFFFFFF);
        importBtn.setTextSize(12f);
        importBtn.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        importBtn.setPadding(dpToPx(16), dpToPx(10), dpToPx(16), dpToPx(10));
        importBtn.setGravity(Gravity.CENTER);

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setShape(GradientDrawable.RECTANGLE);
        btnBg.setCornerRadius(dpToPx(12));
        btnBg.setColor(mThemeAccentColor);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            GradientDrawable mask = new GradientDrawable();
            mask.setShape(GradientDrawable.RECTANGLE);
            mask.setCornerRadius(dpToPx(12));
            mask.setColor(0xFFFFFFFF);
            RippleDrawable ripple = new RippleDrawable(ColorStateList.valueOf(0x40FFFFFF), btnBg, mask);
            importBtn.setBackground(ripple);
        } else {
            importBtn.setBackground(btnBg);
        }

        importBtn.setClickable(true);
        importBtn.setFocusable(true);
        importBtn.setOnClickListener(v -> {
            String url = mCustomUrlInput.getText().toString().trim();
            if (url.isEmpty()) {
                Toast.makeText(this, "Please enter a valid URL", Toast.LENGTH_SHORT).show();
                return;
            }
            String name = "Imported Pack";
            int lastSlash = url.lastIndexOf('/');
            if (lastSlash != -1 && lastSlash < url.length() - 1) {
                String sub = url.substring(lastSlash + 1);
                if (sub.endsWith(".zip")) {
                    name = sub.substring(0, sub.length() - 4).replace("-", " ").replace("_", " ");
                } else if (!sub.isEmpty()) {
                    name = sub.replace("-", " ").replace("_", " ");
                }
            }
            startDownload(new SoundpackItem("custom_" + System.currentTimeMillis(), name, url, "Custom Import"));
        });

        importRow.addView(importBtn);
        importCard.addView(importRow);
        mMainLayout.addView(importCard);
    }

    private void setupCatalogContainer() {
        mMainProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        mMainProgressBar.setVisibility(View.GONE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mMainProgressBar.setProgressTintList(ColorStateList.valueOf(mThemeAccentColor));
        }
        mMainLayout.addView(mMainProgressBar);

        mLoadingText = new TextView(this);
        mLoadingText.setTextColor(mThemeTextSecondary);
        mLoadingText.setTextSize(13f);
        mLoadingText.setGravity(Gravity.CENTER);
        mLoadingText.setVisibility(View.GONE);
        mLoadingText.setPadding(0, dpToPx(6), 0, dpToPx(10));
        mMainLayout.addView(mLoadingText);

        mCatalogLayout = new LinearLayout(this);
        mCatalogLayout.setOrientation(LinearLayout.VERTICAL);
        mMainLayout.addView(mCatalogLayout);
    }

    private void loadSoundpacks() {
        mSoundpacks.clear();

        mSoundpacks.add(new SoundpackItem("default", "System Click (Standard)", "", "Tactile", true));
        mSoundpacks.add(new SoundpackItem("default_deep", "Bubble Wrap (Synthesized)", "", "Tactile", true));

        // Curated Real Mechvibes Soundpacks
        mSoundpacks.add(new SoundpackItem("cherrymx_blue_pbt", "Cherry MX Blue",
                "https://mechvibes.com/sound-packs/sound-pack-1200000000002", "Clicky Switch", false));
        mSoundpacks.add(new SoundpackItem("cherrymx_brown_pbt", "Cherry MX Brown",
                "https://mechvibes.com/sound-packs/sound-pack-1200000000003", "Tactile Switch", false));
        mSoundpacks.add(new SoundpackItem("cherrymx_red_pbt", "Cherry MX Red",
                "https://mechvibes.com/sound-packs/sound-pack-1200000000004", "Linear Switch", false));
        mSoundpacks.add(new SoundpackItem("cherrymx_black_abs", "Cherry MX Black",
                "https://mechvibes.com/sound-packs/sound-pack-1200000000001", "Linear Switch", false));
        mSoundpacks.add(new SoundpackItem("holy_pandas", "Holy Pandas",
                "https://mechvibes.com/sound-packs/sound-pack-v2-example-01-holy-pandas", "Tactile Thock", false));
        mSoundpacks.add(new SoundpackItem("nk_creams", "NovelKeys Creams",
                "https://mechvibes.com/sound-packs/sound-pack-1200000000010", "Linear Switch", false));
        mSoundpacks.add(new SoundpackItem("ibm_model_m_ssk", "IBM Model M SSK",
                "https://mechvibes.com/sound-packs/sound-pack-1200000000007", "Buckling Spring", false));
        mSoundpacks.add(new SoundpackItem("topre_realforce_87u", "Topre Realforce",
                "https://mechvibes.com/sound-packs/sound-pack-1200000000005", "Electrostatic Tactile", false));
        mSoundpacks.add(new SoundpackItem("nk_sherbets", "NK Sherbets",
                "https://mechvibes.com/sound-packs/sound-pack-1200000000009", "Tactile Clicky", false));
        mSoundpacks.add(new SoundpackItem("alps_blue", "Alps Blue Keyboard",
                "https://mechvibes.com/sound-packs/sound-pack-1200000000011", "Vintage Clicky", false));

        // Scan Local Installed Packs
        File soundpacksDir = getExternalFilesDir("soundpacks");
        if (soundpacksDir != null && soundpacksDir.exists() && soundpacksDir.isDirectory()) {
            File[] files = soundpacksDir.listFiles();
            if (files != null) {
                Set<String> existingIds = new HashSet<>();
                for (SoundpackItem item : mSoundpacks) {
                    existingIds.add(item.id);
                }

                for (File file : files) {
                    if (file.isDirectory()) {
                        String id = file.getName();
                        if (existingIds.contains(id)) {
                            for (SoundpackItem item : mSoundpacks) {
                                if (item.id.equals(id)) {
                                    item.status = "Installed";
                                }
                            }
                        } else {
                            String displayName = readLocalSoundpackName(file, id);
                            SoundpackItem localItem = new SoundpackItem(id, displayName, "", "Custom");
                            localItem.status = "Installed";
                            mSoundpacks.add(localItem);
                        }
                    }
                }
            }
        }

        mFilteredSoundpacks = new ArrayList<>(mSoundpacks);
        updateCatalogList();
        updateStats();
    }

    private String readLocalSoundpackName(File dir, String folderName) {
        File nameFile = new File(dir, "name.txt");
        if (nameFile.exists()) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(nameFile), "UTF-8"))) {
                String line = br.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    return line.trim();
                }
            } catch (Exception ignored) {}
        }

        File configFile = new File(dir, "config.json");
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
                JSONObject json = new JSONObject(baos.toString("UTF-8"));
                String name = json.optString("name", "");
                if (!name.trim().isEmpty()) {
                    return name.trim();
                }
            } catch (Exception ignored) {}
        }

        String clean = folderName.replace("custom_sound_pack_", "")
                .replace("sound_pack_", "")
                .replace("custom-sound-pack-", "")
                .replace("sound-pack-", "")
                .replace("traveler-", "")
                .replace("-", " ")
                .replace("_", " ");

        try {
            long num = Long.parseLong(clean.trim());
            return "Mechvibes Pack #" + (num % 1000);
        } catch (Exception ignored) {}

        String[] words = clean.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.length() > 0) {
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
            }
        }
        String res = sb.toString().trim();
        return res.isEmpty() ? folderName : res;
    }

    private void updateStats() {
        int installedCount = 0;
        for (SoundpackItem item : mSoundpacks) {
            if (item.isDefault || item.status.equals("Installed")) {
                installedCount++;
            }
        }
        if (mStatsBadge != null) {
            mStatsBadge.setText(mSoundpacks.size() + " Soundpacks • " + installedCount + " Installed");
        }
    }

    private void filterCatalog(String query) {
        String q = query.trim().toLowerCase();
        mFilteredSoundpacks.clear();

        for (SoundpackItem item : mSoundpacks) {
            boolean matchesQuery = q.isEmpty() || item.name.toLowerCase().contains(q) || item.type.toLowerCase().contains(q);
            if (matchesQuery) {
                mFilteredSoundpacks.add(item);
            }
        }

        updateCatalogList();
    }

    private void scrapeLiveSoundpacks() {
        mMainProgressBar.setVisibility(View.VISIBLE);
        mMainProgressBar.setIndeterminate(true);
        mLoadingText.setText("Scraping live packs from mechvibes.com...");
        mLoadingText.setVisibility(View.VISIBLE);
        if (mScrapeBtn != null) mScrapeBtn.setEnabled(false);

        new AsyncTask<Void, Void, List<SoundpackItem>>() {
            @Override
            protected List<SoundpackItem> doInBackground(Void... voids) {
                List<SoundpackItem> list = new ArrayList<>();
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

                    int searchIdx = 0;
                    while (true) {
                        int index = html.indexOf("class=\"sound-pack", searchIdx);
                        if (index == -1) break;

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

                        int detailsIndex = html.indexOf("class=\"pack-details\"", idEnd);
                        String packName = "";
                        if (detailsIndex != -1 && detailsIndex - idEnd < 500) {
                            int nameDivIndex = html.indexOf("<div>", detailsIndex);
                            if (nameDivIndex != -1 && nameDivIndex - detailsIndex < 150) {
                                int nameEndIndex = html.indexOf("</div>", nameDivIndex + 5);
                                if (nameEndIndex != -1) {
                                    packName = html.substring(nameDivIndex + 5, nameEndIndex).trim();
                                    packName = packName.replace("&amp;", "&")
                                            .replace("&lt;", "<")
                                            .replace("&gt;", ">")
                                            .replace("&quot;", "\"")
                                            .replace("&#39;", "'");
                                }
                            }
                        }

                        if (packName.isEmpty()) {
                            String cleanName = id.replace("custom-sound-pack-", "")
                                    .replace("sound-pack-", "")
                                    .replace("traveler-", "");
                            String[] parts = cleanName.split("-");
                            StringBuilder sb = new StringBuilder();
                            for (String p : parts) {
                                if (p.length() > 0) {
                                    sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
                                }
                            }
                            packName = sb.toString().trim();
                        }

                        String type = "Mechanical Switch";
                        String lowerName = packName.toLowerCase();
                        if (lowerName.contains("click") || lowerName.contains("blue") || lowerName.contains("jade") || lowerName.contains("navy")) {
                            type = "Clicky";
                        } else if (lowerName.contains("thock") || lowerName.contains("panda") || lowerName.contains("brown") || lowerName.contains("tactile") || lowerName.contains("topre")) {
                            type = "Tactile";
                        } else if (lowerName.contains("red") || lowerName.contains("black") || lowerName.contains("cream") || lowerName.contains("linear") || lowerName.contains("yellow") || lowerName.contains("silver")) {
                            type = "Linear";
                        } else if (lowerName.contains("model m") || lowerName.contains("buckling") || lowerName.contains("beam")) {
                            type = "Buckling Spring";
                        }

                        String downloadUrl = "https://mechvibes.com/sound-packs/" + id;
                        list.add(new SoundpackItem(id.replace("-", "_"), packName, downloadUrl, type));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Scraper error", e);
                }
                return list;
            }

            @Override
            protected void onPostExecute(List<SoundpackItem> scrapedItems) {
                mMainProgressBar.setVisibility(View.GONE);
                mLoadingText.setVisibility(View.GONE);
                if (mScrapeBtn != null) mScrapeBtn.setEnabled(true);

                if (scrapedItems == null || scrapedItems.isEmpty()) {
                    Toast.makeText(SoundpackDownloadActivity.this, "Could not reach Mechvibes. Please check your internet connection.", Toast.LENGTH_LONG).show();
                    return;
                }

                int newCount = 0;
                Set<String> existingIds = new HashSet<>();
                for (SoundpackItem item : mSoundpacks) {
                    existingIds.add(item.id);
                }

                for (SoundpackItem scraped : scrapedItems) {
                    if (!existingIds.contains(scraped.id)) {
                        mSoundpacks.add(scraped);
                        newCount++;
                    }
                }

                filterCatalog(mSearchInput.getText().toString());
                updateStats();
                Toast.makeText(SoundpackDownloadActivity.this, "Scraped " + scrapedItems.size() + " soundpacks (" + newCount + " new)", Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }

    private void updateCatalogList() {
        mCatalogLayout.removeAllViews();

        SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(this);
        String activeSoundpack = prefs.getString("pref_keypress_soundpack", "default");

        for (final SoundpackItem item : mFilteredSoundpacks) {
            final boolean isActive = item.id.equals(activeSoundpack);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));

            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setShape(GradientDrawable.RECTANGLE);
            cardBg.setCornerRadius(dpToPx(16));
            cardBg.setColor(mThemeCardColor);

            if (isActive) {
                cardBg.setStroke(dpToPx(2), mThemeAccentColor);
            } else if (mThemeStrokeColor != 0) {
                cardBg.setStroke(dpToPx(1), mThemeStrokeColor);
            }
            card.setBackground(cardBg);

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, dpToPx(10));
            card.setLayoutParams(cardParams);

            // Top Row: Title + Type Tag
            LinearLayout topRow = new LinearLayout(this);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView nameText = new TextView(this);
            nameText.setText(item.name);
            nameText.setTextColor(mThemeTextPrimary);
            nameText.setTextSize(15f);
            nameText.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            nameText.setLayoutParams(nameParams);
            topRow.addView(nameText);

            TextView typeTag = new TextView(this);
            typeTag.setText(item.type.toUpperCase());
            typeTag.setTextSize(10f);
            typeTag.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            typeTag.setPadding(dpToPx(8), dpToPx(3), dpToPx(8), dpToPx(3));

            GradientDrawable typeBg = new GradientDrawable();
            typeBg.setShape(GradientDrawable.RECTANGLE);
            typeBg.setCornerRadius(dpToPx(8));
            typeBg.setColor(getTranslucentColor(mThemeAccentColor, 15));
            typeTag.setBackground(typeBg);
            typeTag.setTextColor(mThemeAccentColor);
            topRow.addView(typeTag);

            card.addView(topRow);

            // Bottom Actions Row
            LinearLayout bottomRow = new LinearLayout(this);
            bottomRow.setOrientation(LinearLayout.HORIZONTAL);
            bottomRow.setGravity(Gravity.CENTER_VERTICAL);
            bottomRow.setPadding(0, dpToPx(10), 0, 0);

            // Audio Preview Button
            final TextView previewBtn = new TextView(this);
            boolean isPlayingThis = (mPlayingItem == item && item.previewStatus.equals("STOP"));
            previewBtn.setText(isPlayingThis ? "■ STOP" : "▶ PREVIEW");
            previewBtn.setTextSize(11f);
            previewBtn.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            previewBtn.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));
            previewBtn.setGravity(Gravity.CENTER);

            GradientDrawable prevBg = new GradientDrawable();
            prevBg.setShape(GradientDrawable.RECTANGLE);
            prevBg.setCornerRadius(dpToPx(10));
            if (isPlayingThis) {
                prevBg.setColor(0xFFEF4444);
                previewBtn.setTextColor(0xFFFFFFFF);
            } else {
                prevBg.setColor(getTranslucentColor(mThemeAccentColor, 12));
                previewBtn.setTextColor(mThemeAccentColor);
            }
            previewBtn.setBackground(prevBg);

            previewBtn.setClickable(true);
            previewBtn.setFocusable(true);
            previewBtn.setOnClickListener(v -> togglePreview(item));
            bottomRow.addView(previewBtn);

            View spacer = new View(this);
            LinearLayout.LayoutParams spParams = new LinearLayout.LayoutParams(0, 1, 1.0f);
            spacer.setLayoutParams(spParams);
            bottomRow.addView(spacer);

            TextView actionBtn = new TextView(this);
            actionBtn.setTextSize(11.5f);
            actionBtn.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            actionBtn.setPadding(dpToPx(14), dpToPx(6), dpToPx(14), dpToPx(6));
            actionBtn.setGravity(Gravity.CENTER);

            GradientDrawable actBg = new GradientDrawable();
            actBg.setShape(GradientDrawable.RECTANGLE);
            actBg.setCornerRadius(dpToPx(10));

            if (isActive) {
                actBg.setColor(0xFF10B981);
                actionBtn.setText("ACTIVE ✓");
                actionBtn.setTextColor(0xFFFFFFFF);
            } else if (item.status.equals("Installed") || item.isDefault) {
                actBg.setColor(mThemeAccentColor);
                actionBtn.setText("APPLY");
                actionBtn.setTextColor(mIsDarkTheme ? 0xFF000000 : 0xFFFFFFFF);
                actionBtn.setOnClickListener(v -> applySoundpack(item));
            } else if (item.status.equals("Downloading")) {
                actBg.setColor(getTranslucentColor(mThemeAccentColor, 30));
                actionBtn.setText("DOWNLOADING (" + item.progress + "%)");
                actionBtn.setTextColor(mThemeAccentColor);
            } else {
                actBg.setColor(mThemeAccentColor);
                actionBtn.setText("GET PACK");
                actionBtn.setTextColor(mIsDarkTheme ? 0xFF000000 : 0xFFFFFFFF);
                actionBtn.setOnClickListener(v -> startDownload(item));
            }
            actionBtn.setBackground(actBg);
            bottomRow.addView(actionBtn);

            card.addView(bottomRow);
            mCatalogLayout.addView(card);
        }
    }

    private void togglePreview(SoundpackItem item) {
        if (mPlayingItem == item) {
            stopAudio();
            item.previewStatus = "PLAY";
            mPlayingItem = null;
            updateCatalogList();
            return;
        }

        stopAudio();
        if (mPlayingItem != null) {
            mPlayingItem.previewStatus = "PLAY";
        }

        mPlayingItem = item;
        item.previewStatus = "STOP";
        updateCatalogList();

        playPreviewAudio(item);
    }

    private void playPreviewAudio(SoundpackItem item) {
        try {
            if (item.isDefault) {
                android.media.ToneGenerator toneGen = new android.media.ToneGenerator(
                        android.media.AudioManager.STREAM_NOTIFICATION, 80);
                toneGen.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 120);
                return;
            }

            File soundpacksDir = getExternalFilesDir("soundpacks");
            File packDir = new File(soundpacksDir, item.id);
            if (packDir.exists() && packDir.isDirectory()) {
                File[] files = packDir.listFiles();
                if (files != null && files.length > 0) {
                    for (File f : files) {
                        if (f.getName().endsWith(".wav") || f.getName().endsWith(".mp3") || f.getName().endsWith(".ogg")) {
                            mMediaPlayer = new MediaPlayer();
                            mMediaPlayer.setDataSource(f.getAbsolutePath());
                            mMediaPlayer.setOnCompletionListener(mp -> {
                                stopAudio();
                                if (mPlayingItem != null) mPlayingItem.previewStatus = "PLAY";
                                mPlayingItem = null;
                                updateCatalogList();
                            });
                            mMediaPlayer.prepare();
                            mMediaPlayer.start();
                            return;
                        }
                    }
                }
            }

            if (!item.downloadUrl.isEmpty()) {
                Toast.makeText(this, "Downloading pack to preview sounds...", Toast.LENGTH_SHORT).show();
                startDownload(item);
            }
        } catch (Exception e) {
            Log.e(TAG, "Audio preview failed", e);
            stopAudio();
            if (mPlayingItem != null) mPlayingItem.previewStatus = "PLAY";
            mPlayingItem = null;
            updateCatalogList();
        }
    }

    private void stopAudio() {
        if (mMediaPlayer != null) {
            try {
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.stop();
                }
                mMediaPlayer.release();
            } catch (Exception ignored) {}
            mMediaPlayer = null;
        }
    }

    private void applySoundpack(SoundpackItem item) {
        SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(this);
        prefs.edit().putString("pref_keypress_soundpack", item.id).apply();
        Toast.makeText(this, item.name + " applied as active soundpack!", Toast.LENGTH_SHORT).show();

        try {
            SettingsValues settingsValues = new SettingsValues(prefs, getResources(), null);
            AudioAndHapticFeedbackManager.getInstance().onSettingsChanged(settingsValues);
        } catch (Exception ignored) {}

        updateCatalogList();
    }

    private void startDownload(final SoundpackItem item) {
        if (item.downloadUrl.isEmpty()) {
            Toast.makeText(this, "No download URL available for this pack", Toast.LENGTH_SHORT).show();
            return;
        }

        item.status = "Downloading";
        item.progress = 0;
        updateCatalogList();

        new DownloadTask(item, new DownloadCallback() {
            @Override
            public void onProgress(int percent) {
                item.progress = percent;
                updateCatalogList();
            }

            @Override
            public void onComplete() {
                item.status = "Installed";
                item.progress = 100;
                applySoundpack(item);
                updateStats();
                updateCatalogList();
            }

            @Override
            public void onError(String error) {
                item.status = "Available";
                Toast.makeText(SoundpackDownloadActivity.this, "Download failed: " + error, Toast.LENGTH_LONG).show();
                updateCatalogList();
            }
        }).execute();
    }

    private String resolveZipUrl(String pageUrl) {
        if (pageUrl.endsWith(".zip")) {
            return pageUrl;
        }
        try {
            URL url = new URL(pageUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
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
                    return zipUrl;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "ZIP resolution failed for " + pageUrl, e);
        }
        return pageUrl;
    }

    private class DownloadTask extends AsyncTask<Void, Integer, Boolean> {
        private SoundpackItem mItem;
        private DownloadCallback mCallback;
        private String mError = "";

        DownloadTask(SoundpackItem item, DownloadCallback callback) {
            mItem = item;
            mCallback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            File tempZip = null;
            try {
                String targetUrl = resolveZipUrl(mItem.downloadUrl);

                URL url = new URL(targetUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(14000);
                conn.setReadTimeout(14000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/120 Safari/537.36");
                conn.connect();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                    String newUrl = conn.getHeaderField("Location");
                    conn = (HttpURLConnection) new URL(newUrl).openConnection();
                    conn.setConnectTimeout(14000);
                    conn.setReadTimeout(14000);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/120 Safari/537.36");
                    conn.connect();
                }

                int fileLength = conn.getContentLength();
                tempZip = new File(getCacheDir(), "temp_" + mItem.id + ".zip");
                InputStream input = new BufferedInputStream(conn.getInputStream());
                FileOutputStream output = new FileOutputStream(tempZip);

                byte[] data = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    if (fileLength > 0) {
                        publishProgress((int) (total * 100 / fileLength));
                    }
                    output.write(data, 0, count);
                }
                output.flush();
                output.close();
                input.close();

                File soundpacksDir = getExternalFilesDir("soundpacks");
                File destDir = new File(soundpacksDir, mItem.id);
                if (!destDir.exists()) destDir.mkdirs();

                File workDir = new File(getCacheDir(), "work_" + mItem.id);
                if (!workDir.exists()) workDir.mkdirs();

                ZipInputStream zis = new ZipInputStream(new FileInputStream(tempZip));
                ZipEntry ze;
                while ((ze = zis.getNextEntry()) != null) {
                    String fileName = ze.getName();
                    if (fileName.contains("/") || fileName.contains("\\")) {
                        fileName = fileName.substring(Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\')) + 1);
                    }
                    if (fileName.isEmpty()) continue;

                    File newFile = new File(workDir, fileName);
                    FileOutputStream fos = new FileOutputStream(newFile);
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                    zis.closeEntry();
                }
                zis.close();

                processExtractedFiles(workDir, destDir);
                deleteRecursive(workDir);

                return true;
            } catch (Exception e) {
                Log.e(TAG, "Download/Extraction error", e);
                mError = e.getMessage();
                return false;
            } finally {
                if (tempZip != null && tempZip.exists()) {
                    tempZip.delete();
                }
            }
        }

        private void processExtractedFiles(File workDir, File destDir) {
            try {
                File configFile = new File(workDir, "config.json");
                Map<String, File> decodedCache = new HashMap<>();

                if (configFile.exists()) {
                    FileInputStream fis = new FileInputStream(configFile);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] buf = new byte[1024];
                    int r;
                    while ((r = fis.read(buf)) != -1) {
                        bos.write(buf, 0, r);
                    }
                    fis.close();
                    JSONObject config = new JSONObject(bos.toString("UTF-8"));
                    String soundFile = config.optString("sound", "");
                    JSONObject defines = config.optJSONObject("defines");

                    if (!soundFile.isEmpty() && defines != null) {
                        File audioFile = new File(workDir, cleanPath(soundFile));
                        if (audioFile.exists()) {
                            AudioDecoderSlicer.DecodedAudio decoded = AudioDecoderSlicer.decodeAudio(audioFile);
                            if (decoded != null) {
                                Iterator<String> keys = defines.keys();
                                JSONArray standardArr = null;
                                while (keys.hasNext()) {
                                    String k = keys.next();
                                    JSONArray arr = defines.optJSONArray(k);
                                    if (arr != null && arr.length() >= 2) {
                                        if (standardArr == null) standardArr = arr;
                                        AudioDecoderSlicer.sliceAudioFromDecoded(
                                                decoded, new File(destDir, k + ".wav"),
                                                arr.getInt(0), arr.getInt(1));
                                    }
                                }
                                if (standardArr != null) {
                                    AudioDecoderSlicer.sliceAudioFromDecoded(
                                            decoded, new File(destDir, "standard.wav"),
                                            standardArr.getInt(0), standardArr.getInt(1));
                                }
                            }
                        }
                    } else if (defines != null) {
                        Iterator<String> defineKeys = defines.keys();
                        while (defineKeys.hasNext()) {
                            String key = defineKeys.next();
                            String fileStr = defines.optString(key, "");
                            if (!fileStr.isEmpty()) {
                                fileStr = cleanPath(fileStr);
                                File src = new File(workDir, fileStr);
                                File dest = new File(destDir, key + ".wav");
                                copyAudioFile(src, dest, decodedCache);
                            }
                        }

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
                }

                // Copy all audio files to destDir
                File[] files = workDir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        String name = f.getName().toLowerCase();
                        if (name.endsWith(".wav")) {
                            copyFileDirectly(f, new File(destDir, f.getName()));
                        } else if (name.endsWith(".ogg") || name.endsWith(".mp3")) {
                            String wavName = f.getName().substring(0, f.getName().lastIndexOf('.')) + ".wav";
                            AudioDecoderSlicer.sliceAudio(f, new File(destDir, wavName), 0, 30000);
                        }
                    }
                }

                // Ensure standard fallback wav files exist in destDir
                File[] destFiles = destDir.listFiles();
                File firstWav = null;
                if (destFiles != null) {
                    for (File f : destFiles) {
                        if (f.getName().endsWith(".wav")) {
                            if (firstWav == null) firstWav = f;
                            if (f.getName().equals("standard.wav")) {
                                firstWav = f;
                                break;
                            }
                        }
                    }
                }

                if (firstWav != null) {
                    File std = new File(destDir, "standard.wav");
                    if (!std.exists()) copyFileDirectly(firstWav, std);
                    File spc = new File(destDir, "spacebar.wav");
                    if (!spc.exists()) copyFileDirectly(firstWav, spc);
                    File del = new File(destDir, "delete.wav");
                    if (!del.exists()) copyFileDirectly(firstWav, del);
                    File ret = new File(destDir, "return.wav");
                    if (!ret.exists()) copyFileDirectly(firstWav, ret);
                }

                // Write name.txt and config.json
                File nameFile = new File(destDir, "name.txt");
                FileOutputStream nameOut = new FileOutputStream(nameFile);
                nameOut.write(mItem.name.getBytes("UTF-8"));
                nameOut.close();

                File confOutFile = new File(destDir, "config.json");
                JSONObject infoJson = new JSONObject();
                infoJson.put("name", mItem.name);
                infoJson.put("id", mItem.id);
                infoJson.put("type", mItem.type);
                FileOutputStream confOut = new FileOutputStream(confOutFile);
                confOut.write(infoJson.toString().getBytes("UTF-8"));
                confOut.close();

            } catch (Exception e) {
                Log.e(TAG, "Audio processing failed", e);
            }
        }

        private String cleanPath(String path) {
            int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
            if (lastSlash != -1) {
                return path.substring(lastSlash + 1);
            }
            return path;
        }

        private void copyAudioFile(File src, File dest, Map<String, File> decodedCache) {
            if (!src.exists()) return;
            try {
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
                    boolean success = AudioDecoderSlicer.sliceAudio(src, dest, 0, 30000);
                    if (success) {
                        decodedCache.put(srcPath, dest);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Copy audio failed: " + src.getName(), e);
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
                File[] children = fileOrDirectory.listFiles();
                if (children != null) {
                    for (File child : children) {
                        deleteRecursive(child);
                    }
                }
            }
            fileOrDirectory.delete();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (mCallback != null && values.length > 0) {
                mCallback.onProgress(values[0]);
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                if (mCallback != null) mCallback.onComplete();
            } else {
                if (mCallback != null) mCallback.onError(mError);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        stopAudio();
        super.onDestroy();
    }
}
