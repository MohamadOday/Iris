package nabu.iris.keyboard.latin;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.LruCache;
import android.view.View;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class GifView extends View {
    private static final ExecutorService sDownloadExecutor = Executors.newFixedThreadPool(4);
    
    private Movie mMovie;
    private long mMovieStart;
    private String mUrl;
    private byte[] mBytes;
    private boolean mIsLoading = false;
    private final Paint mPaint;
    private LruCache<String, byte[]> mCache;
    private boolean mIsPaused = false;

    public GifView(Context context) {
        super(context);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(0x1AFFFFFF); // Translucent white placeholder
    }

    public GifView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(0x1AFFFFFF);
    }

    public void loadUrl(final String url, final LruCache<String, byte[]> cache) {
        mUrl = url;
        mCache = cache;
        mMovie = null;
        mBytes = null;
        mIsLoading = false;
        
        if (url == null) {
            invalidate();
            return;
        }

        byte[] cached = cache.get(url);
        if (cached != null) {
            setGifBytes(cached);
            return;
        }

        mIsLoading = true;
        invalidate();

        sDownloadExecutor.execute(() -> {
            try {
                if (mIsPaused || !isAttachedToWindow() || !url.equals(mUrl)) return;
                final byte[] data;
                if (url.startsWith("/") || url.startsWith("file:")) {
                    String path = url.startsWith("file:") ? url.substring(5) : url;
                    java.io.File file = new java.io.File(path);
                    if (!file.exists()) return;
                    java.io.FileInputStream fis = new java.io.FileInputStream(file);
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buf = new byte[2048];
                    int len;
                    while ((len = fis.read(buf)) != -1) {
                        out.write(buf, 0, len);
                    }
                    fis.close();
                    data = out.toByteArray();
                } else {
                    URL u = new URL(url);
                    HttpURLConnection conn = (HttpURLConnection) u.openConnection();
                    conn.setConnectTimeout(6000);
                    conn.setReadTimeout(6000);
                    conn.connect();
                    if (conn.getResponseCode() == 200) {
                        InputStream in = conn.getInputStream();
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        byte[] buf = new byte[2048];
                        int len;
                        while ((len = in.read(buf)) != -1) {
                            out.write(buf, 0, len);
                        }
                        in.close();
                        data = out.toByteArray();
                    } else {
                        return;
                    }
                }
                
                cache.put(url, data);
                post(() -> {
                    if (url.equals(mUrl) && isAttachedToWindow() && !mIsPaused) {
                        setGifBytes(data);
                    }
                });
            } catch (Exception e) {
                // ignore
            }
        });
    }

    private void setGifBytes(byte[] bytes) {
        mBytes = bytes;
        mIsLoading = false;
        try {
            mMovie = Movie.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            // ignore
        }
        mMovieStart = 0;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        int w = getWidth();
        int h = getHeight();

        if (mMovie != null) {
            long now = android.os.SystemClock.uptimeMillis();
            if (mMovieStart == 0) {
                mMovieStart = now;
            }
            int duration = mMovie.duration();
            if (duration == 0) {
                duration = 1000;
            }
            int relTime = (int) ((now - mMovieStart) % duration);
            mMovie.setTime(relTime);
            
            float movieWidth = mMovie.width();
            float movieHeight = mMovie.height();
            if (movieWidth > 0 && movieHeight > 0) {
                canvas.save();
                float scaleX = (float) w / movieWidth;
                float scaleY = (float) h / movieHeight;
                canvas.scale(scaleX, scaleY);
                mMovie.draw(canvas, 0, 0);
                canvas.restore();
            }
            if (isShown() && getWindowVisibility() == VISIBLE) {
                postInvalidateOnAnimation();
            }
        } else {
            canvas.drawRect(0, 0, w, h, mPaint);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mMovie = null;
        mBytes = null;
    }

    public void pauseAnimation() {
        if (!mIsPaused) {
            mIsPaused = true;
            mMovie = null;
            mBytes = null;
            invalidate();
        }
    }

    public void resumeAnimation() {
        if (mIsPaused) {
            mIsPaused = false;
            if (mUrl != null && mCache != null) {
                loadUrl(mUrl, mCache);
            }
        }
    }
}
