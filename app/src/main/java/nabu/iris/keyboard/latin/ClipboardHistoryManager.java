package nabu.iris.keyboard.latin;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class ClipboardHistoryManager {
    private static final String PREF_FILE = "clipboard_history_prefs";
    private static final String PREF_KEY_HISTORY = "clipboard_history_json";
    private static final int MAX_ITEMS = 20;

    private final SharedPreferences mPrefs;
    private final List<ClipboardItem> mItems = new ArrayList<>();

    public static class ClipboardItem {
        public final String text;
        public final long timestamp;
        public boolean isPinned;

        public ClipboardItem(String text, long timestamp, boolean isPinned) {
            this.text = text;
            this.timestamp = timestamp;
            this.isPinned = isPinned;
        }

        public JSONObject toJSONObject() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("text", text);
            obj.put("timestamp", timestamp);
            obj.put("isPinned", isPinned);
            return obj;
        }

        public static ClipboardItem fromJSONObject(JSONObject obj) throws JSONException {
            return new ClipboardItem(
                    obj.getString("text"),
                    obj.optLong("timestamp", System.currentTimeMillis()),
                    obj.optBoolean("isPinned", false)
            );
        }
    }

    public ClipboardHistoryManager(Context context) {
        mPrefs = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
        loadHistory();
    }

    private synchronized void loadHistory() {
        mItems.clear();
        String json = mPrefs.getString(PREF_KEY_HISTORY, null);
        if (json != null) {
            try {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    mItems.add(ClipboardItem.fromJSONObject(arr.getJSONObject(i)));
                }
            } catch (JSONException e) {
                // Ignore or clear malformed data
            }
        }
    }

    private synchronized void saveHistory() {
        try {
            JSONArray arr = new JSONArray();
            for (ClipboardItem item : mItems) {
                arr.put(item.toJSONObject());
            }
            mPrefs.edit().putString(PREF_KEY_HISTORY, arr.toString()).apply();
        } catch (JSONException e) {
            // Ignore
        }
    }

    public synchronized List<ClipboardItem> getItems() {
        List<ClipboardItem> sorted = new ArrayList<>(mItems);
        Collections.sort(sorted, new Comparator<ClipboardItem>() {
            @Override
            public int compare(ClipboardItem o1, ClipboardItem o2) {
                if (o1.isPinned && !o2.isPinned) return -1;
                if (!o1.isPinned && o2.isPinned) return 1;
                return Long.compare(o2.timestamp, o1.timestamp);
            }
        });
        return sorted;
    }

    public synchronized void addItem(String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        text = text.trim();

        ClipboardItem existing = null;
        for (ClipboardItem item : mItems) {
            if (item.text.equals(text)) {
                existing = item;
                break;
            }
        }

        if (existing != null) {
            mItems.remove(existing);
            mItems.add(new ClipboardItem(existing.text, System.currentTimeMillis(), existing.isPinned));
        } else {
            mItems.add(new ClipboardItem(text, System.currentTimeMillis(), false));
        }

        // Limit to MAX_ITEMS by evicting the oldest unpinned items
        while (mItems.size() > MAX_ITEMS) {
            ClipboardItem oldestUnpinned = null;
            for (ClipboardItem item : mItems) {
                if (!item.isPinned) {
                    if (oldestUnpinned == null || item.timestamp < oldestUnpinned.timestamp) {
                        oldestUnpinned = item;
                    }
                }
            }

            if (oldestUnpinned != null) {
                mItems.remove(oldestUnpinned);
            } else {
                // Evict oldest pinned item if everything is pinned (safety fallback)
                ClipboardItem oldestPinned = null;
                for (ClipboardItem item : mItems) {
                    if (oldestPinned == null || item.timestamp < oldestPinned.timestamp) {
                        oldestPinned = item;
                    }
                }
                if (oldestPinned != null) {
                    mItems.remove(oldestPinned);
                } else {
                    break;
                }
            }
        }

        saveHistory();
    }

    public synchronized void deleteItem(String text) {
        if (text == null) return;
        ClipboardItem toRemove = null;
        for (ClipboardItem item : mItems) {
            if (item.text.equals(text)) {
                toRemove = item;
                break;
            }
        }
        if (toRemove != null) {
            mItems.remove(toRemove);
            saveHistory();
        }
    }

    public synchronized void togglePin(String text) {
        if (text == null) return;
        for (ClipboardItem item : mItems) {
            if (item.text.equals(text)) {
                item.isPinned = !item.isPinned;
                saveHistory();
                break;
            }
        }
    }

    public synchronized void clearUnpinned() {
        List<ClipboardItem> pinned = new ArrayList<>();
        for (ClipboardItem item : mItems) {
            if (item.isPinned) {
                pinned.add(item);
            }
        }
        mItems.clear();
        mItems.addAll(pinned);
        saveHistory();
    }
}
