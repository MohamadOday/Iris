package nabu.iris.keyboard.latin;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public final class GifSearchEngine {

    public static class GifItem {
        public final String id;
        public final String previewUrl;
        public final String fullUrl;
        public final int width;
        public final int height;

        public GifItem(String id, String previewUrl, String fullUrl, int width, int height) {
            this.id = id;
            this.previewUrl = previewUrl;
            this.fullUrl = fullUrl;
            this.width = width;
            this.height = height;
        }
    }

    private static final String TENOR_KEY = "AIzaSyAp4Ie-x-F5nLqwoqvDFrJGI4purWdGIVo";
    private static final String TENOR_BASE = "https://tenor.googleapis.com/v2";
    private static final String KLIPY_BASE = "https://api.klipy.com/v2";

    public static List<GifItem> fetchGifs(String provider, String giphyKey, String klipyKey, boolean highQuality, String query) {
        List<GifItem> list = new ArrayList<>();
        try {
            String urlStr;
            boolean isSearch = (query != null && !query.trim().isEmpty());

            if ("giphy".equals(provider)) {
                String key = (giphyKey != null && !giphyKey.trim().isEmpty()) ? giphyKey : "";
                if (isSearch) {
                    urlStr = "https://api.giphy.com/v1/gifs/search?api_key=" + key 
                            + "&q=" + URLEncoder.encode(query, "UTF-8") 
                            + "&limit=20&rating=g";
                } else {
                    urlStr = "https://api.giphy.com/v1/gifs/trending?api_key=" + key 
                            + "&limit=20&rating=g";
                }
            } else { // tenor or klipy
                String base = "klipy".equals(provider) ? KLIPY_BASE : TENOR_BASE;
                String key = TENOR_KEY;
                if ("klipy".equals(provider)) {
                    key = (klipyKey != null && !klipyKey.trim().isEmpty()) ? klipyKey : TENOR_KEY;
                }
                if (isSearch) {
                    urlStr = base + "/search?client_key=tenor_android&country=US&q=" 
                            + URLEncoder.encode(query, "UTF-8")
                            + "&component=containing_app&ar_range=all&limit=20&media_filter=gif%2Cmp4%2Ctinygif%2Cmediumgif&appversion=andrfbm_2.1.78_374&contentfilter=low&locale=en&key=" 
                            + key;
                } else {
                    urlStr = base + "/featured?client_key=tenor_android&country=US&component=containing_app&ar_range=all&limit=20&media_filter=gif%2Cmp4%2Ctinygif%2Cmediumgif&appversion=andrfbm_2.1.78_374&contentfilter=low&locale=en&key=" 
                            + key;
                }
            }

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("User-Agent", "com.riffsy.FBMGIFApp/2.1.78/374/en_US (Android 14/34; SptLib/1.4.1; Infinix+X6833B)");
            
            // Accept gzip encoding if available
            conn.setRequestProperty("Accept-Encoding", "gzip");
            conn.connect();

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                return list;
            }

            InputStream in = conn.getInputStream();
            if ("gzip".equalsIgnoreCase(conn.getContentEncoding())) {
                in = new java.util.zip.GZIPInputStream(in);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[2048];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            in.close();

            String jsonStr = out.toString("UTF-8");
            JSONObject root = new JSONObject(jsonStr);

            if ("giphy".equals(provider)) {
                JSONArray data = root.optJSONArray("data");
                if (data != null) {
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject item = data.getJSONObject(i);
                        String id = item.optString("id");
                        JSONObject images = item.optJSONObject("images");
                        if (images != null) {
                            JSONObject previewObj = null;
                            if (highQuality) {
                                previewObj = images.optJSONObject("fixed_height");
                            }
                            if (previewObj == null) {
                                previewObj = images.optJSONObject("fixed_height_small");
                            }
                            if (previewObj == null) {
                                previewObj = images.optJSONObject("preview_gif");
                            }
                            JSONObject originalObj = images.optJSONObject("original");

                            if (previewObj != null && originalObj != null) {
                                String pUrl = previewObj.optString("url");
                                String fUrl = originalObj.optString("url");
                                int w = previewObj.optInt("width", 200);
                                int h = previewObj.optInt("height", 150);
                                list.add(new GifItem(id, pUrl, fUrl, w, h));
                            }
                        }
                    }
                }
            } else { // tenor / klipy
                JSONArray results = root.optJSONArray("results");
                if (results != null) {
                    for (int i = 0; i < results.length(); i++) {
                        JSONObject item = results.getJSONObject(i);
                        String id = item.optString("id");
                        JSONObject formats = item.optJSONObject("media_formats");
                        if (formats != null) {
                            JSONObject previewObj = null;
                            if (highQuality) {
                                previewObj = formats.optJSONObject("mediumgif");
                            }
                            if (previewObj == null) {
                                previewObj = formats.optJSONObject("tinygif");
                            }
                            JSONObject gifObj = formats.optJSONObject("gif");
                            if (previewObj != null && gifObj != null) {
                                String pUrl = previewObj.optString("url");
                                String fUrl = gifObj.optString("url");
                                int w = 200;
                                int h = 150;
                                JSONArray dims = previewObj.optJSONArray("dims");
                                if (dims != null && dims.length() >= 2) {
                                    w = dims.optInt(0, 200);
                                    h = dims.optInt(1, 150);
                                }
                                list.add(new GifItem(id, pUrl, fUrl, w, h));
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            // ignore
        }
        return list;
    }
}
