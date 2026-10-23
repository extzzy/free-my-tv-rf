package io.freemytv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Choreographer;
import android.view.View;

/**
 * The screensaver, in the same style as the home screen: the big clock with the date, the weather and the prices over
 * the home screen's night sky photo (slightly darker), drifting slowly so nothing stays burned in one place. Any key
 * exits (as on Apple TV).
 */
final class DreamView extends View implements Choreographer.FrameCallback, HomeData.Listener {
    private static final double DRIFT_PERIOD_S = 600;
    // The high and low and the stale prices line, made again only when they change (drawn on every frame).
    private String range = "", staleLine = "", staleFor = "";
    private HomeData.Weather rangeWeather;
    private int rangeGeneration = -1, rangePrefs = -1, staleGeneration = -1;

    private final HomeData data;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Recording content = Recording.create("clock");
    private final Paint photoPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint black = new Paint();
    private final RectF screen = new RectF();
    private Bitmap photo;
    private boolean running, dirty = true, loading;
    private long startedAt, lastFrame;
    private int w, h;
    private float k = 1;
    private long minute = -1;
    private String clock = "", date = "";
    private Paint datePaint, clockPaint, weatherPaint, weatherDim, quotePaint, stalePaint;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!running) return;
            data.refreshIfStale();
            dirty = true;
            handler.postDelayed(this, 60_000);
        }
    };

    DreamView(Context c) {
        super(c);
        data = HomeData.get(c);
        content.setUseCompositingLayer(true, null);
        black.setColor(0xFF000000);
    }

    void start() {
        if (running) return;
        running = true;
        startedAt = SystemClock.uptimeMillis();
        lastFrame = 0;
        data.addListener(this);
        data.refreshIfStale();
        handler.postDelayed(tick, 60_000);
        Choreographer.getInstance().postFrameCallback(this);
        invalidate();
    }

    void stop() {
        if (!running) return;
        running = false;
        Choreographer.getInstance().removeFrameCallback(this);
        handler.removeCallbacks(tick);
        data.removeListener(this);
    }

    @Override public void onData() {
        dirty = true;
        invalidate();
    }

    @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        w = width;
        h = height;
        k = w / 1920f;
        screen.set(0, 0, w, h);
        content.setPosition(0, 0, w, h);
        loadPhoto();
        datePaint = Ui.text(Ui.semibold, 36 * k, 0xD9FFFFFF);
        datePaint.setTextAlign(Paint.Align.CENTER);
        clockPaint = Ui.numbers(Ui.thin, 280 * k, 0xF2FFFFFF);
        clockPaint.setTextAlign(Paint.Align.CENTER);
        clockPaint.setLetterSpacing(-0.01f);
        weatherPaint = Ui.numbers(Ui.medium, 30 * k, 0xC7FFFFFF);
        weatherDim = Ui.numbers(Ui.regular, 28 * k, 0x80FFFFFF);
        quotePaint = Ui.numbers(Ui.medium, 24 * k, 0x99FFFFFF);
        stalePaint = Ui.text(Ui.regular, 20 * k, 0x59FFFFFF);
        stalePaint.setTextAlign(Paint.Align.CENTER);
        dirty = true;
    }

    /** The home screen's photo (assets/sky.jpg) at the screen's size, decoded once in the background, kept on the GPU. */
    private void loadPhoto() {
        if (loading || (photo != null && photo.getWidth() == w)) return;
        loading = true;
        final int width = w, height = h;
        final Context c = getContext().getApplicationContext();
        new Thread(() -> {
            Bitmap shown = null;
            try (java.io.InputStream in = c.getAssets().open("sky.jpg")) {
                Bitmap decoded = BitmapFactory.decodeStream(in);
                Bitmap full = decoded.getWidth() == width && decoded.getHeight() == height ? decoded
                        : Bitmap.createScaledBitmap(decoded, width, height, true);
                Bitmap gpu = full.copy(Bitmap.Config.HARDWARE, false);
                shown = gpu != null ? gpu : full;
            } catch (java.io.IOException | RuntimeException | OutOfMemoryError e) {
                AppLog.w("screensaver: sky photo unavailable: " + e.getMessage());
            }
            final Bitmap ready = shown;
            handler.post(() -> {
                loading = false;
                if (ready != null) photo = ready;
                invalidate();
            });
        }, "dream-photo").start();
    }

    // MARK: - Frames: 20 per second, locked to every 3rd display frame (60 Hz)
    // Motion is slow (under half a pixel per frame). Measured on the TV: ~4 ms of GPU per frame of ours, but drawing
    // every frame, it only reaches ~50 fps (the system also composites the UI on the GPU); at 20 there's room to spare.

    @Override public void doFrame(long frameTimeNanos) {
        if (!running) return;
        if (frameTimeNanos - lastFrame >= 45_000_000L) {
            lastFrame = frameTimeNanos;
            invalidate();
        }
        Choreographer.getInstance().postFrameCallback(this);
    }

    @Override protected void onDraw(Canvas canvas) {
        if (w == 0) return;
        long up = SystemClock.uptimeMillis();
        long now = System.currentTimeMillis();
        if (now / 60_000 != minute) {
            minute = now / 60_000;
            clock = Ui.clock(now);
            date = Ui.date(now);
            dirty = true;
        }
        float t = (up - startedAt) / 1000f;
        if (photo != null) {
            canvas.drawBitmap(photo, null, screen, photoPaint);
            black.setAlpha(77);     // a little darker than the home screen: the clock stands out, the TV rests
            canvas.drawRect(screen, black);
            black.setAlpha(255);
        } else {
            canvas.drawRect(screen, black);
        }
        if (dirty) {
            Canvas rc = content.beginRecording(w, h);
            try {
                drawContent(rc);
            } finally {
                content.endRecording();
            }
            dirty = false;
        }
        double phase = 2 * Math.PI * t / DRIFT_PERIOD_S;
        content.setTranslationX((float) (40 * k * Math.sin(phase)));
        content.setTranslationY((float) (26 * k * Math.sin(2 * phase + 0.7)));
        content.draw(canvas);
    }

    private void drawContent(Canvas c) {
        float cx = w / 2f;
        float clockCap = Ui.cap(clockPaint);
        float clockBase = h * 0.5f + clockCap / 2 - 10 * k;
        c.drawText(date, cx, clockBase - clockCap - 34 * k, datePaint);
        c.drawText(clock, cx, clockBase, clockPaint);

        // Weather: symbol, temperature, high and low, centered under the clock.
        HomeData.Weather wx = data.weather;
        if (!Double.isNaN(wx.temp) && Prefs.weather()) {
            float base = clockBase + 76 * k;
            String symbol = Symbols.weather(wx.code, wx.isDay);
            String now = HomeData.degrees(wx.temp);
            if (wx != rangeWeather || Ui.generation != rangeGeneration || Prefs.version != rangePrefs) {
                range = HomeData.highLow(wx, "  ");
                rangeWeather = wx;
                rangeGeneration = Ui.generation;
                rangePrefs = Prefs.version;
            }
            float sw = Symbols.width(getContext(), symbol, 32 * k);
            float width = sw + 12 * k + weatherPaint.measureText(now) + 18 * k + weatherDim.measureText(range);
            float x = cx - width / 2;
            Symbols.draw(getContext(), c, symbol, x + sw / 2, base - Ui.halfCap(weatherPaint), 32 * k, 0xC7FFFFFF);
            x += sw + 12 * k;
            c.drawText(now, x, base, weatherPaint);
            x += weatherPaint.measureText(now) + 18 * k;
            c.drawText(range, x, base, weatherDim);
        }

        // Prices near the bottom edge: small white logo + value, all four on one line.
        float cap = Ui.cap(quotePaint), spacing = 56 * k, gap = 10 * k;
        int[] coins = HomeData.active();
        String[] values = new String[HomeData.coinCount()];
        float total = spacing * (values.length - 1);
        for (int i = 0; i < values.length; i++) {
            values[i] = HomeData.price(data.prices[coins[i]]);
            total += Glyphs.width(getContext(), HomeData.COINS[coins[i]], cap) + gap + quotePaint.measureText(values[i]);
        }
        float x = cx - total / 2, base = h - 92 * k;
        for (int i = 0; i < values.length; i++) {
            float gw = Glyphs.width(getContext(), HomeData.COINS[coins[i]], cap);
            Glyphs.coin(getContext(), c, HomeData.COINS[coins[i]], x + gw / 2, base - cap / 2, cap, 0x80FFFFFF);
            x += gw + gap;
            c.drawText(values[i], x, base, quotePaint);
            x += quotePaint.measureText(values[i]) + spacing;
        }
        String stale = HomeData.staleness(data.pricesAt, 5 * 60_000L);
        if (!stale.isEmpty() && values.length > 0) {
            if (!stale.equals(staleFor) || Ui.generation != staleGeneration) {
                staleLine = Ui.trf("prices from {ago}", "ago", stale);
                staleFor = stale;
                staleGeneration = Ui.generation;
            }
            c.drawText(staleLine, cx, base + 38 * k, stalePaint);
        }
    }
}
