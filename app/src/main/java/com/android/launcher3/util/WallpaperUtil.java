package com.android.launcher3.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.Pair;

import com.android.launcher3.R;
import com.android.launcher3.uioverrides.dynamicui.ColorExtractionAlgorithm;
import com.android.launcher3.uioverrides.dynamicui.WallpaperColorsCompat;

import static com.android.launcher3.graphics.ColorExtractor.findDominantColorByHue;

public class WallpaperUtil {

    private static final String VERSION_PREFIX = "1,";
    private static final String KEY_COLORS = "wallpaper_parsed_colors";
    private static final String ACTION_EXTRACTION_COMPLETE =
            "com.android.launcher3.uioverrides.dynamicui.WallpaperManagerCompatVL.EXTRACTION_COMPLETE";
    private static final int MAX_WALLPAPER_EXTRACTION_AREA = 112 * 112;
    private static final int FALLBACK_COLOR = Color.WHITE;

    private static final int DEFAULT_THEME_RES = R.style.LauncherTheme;


    private final ColorExtractionAlgorithm mExtractionType;
    private Drawable drawable;
    private OnChangeListener mListener;

    private WallpaperUtil(Context context) {
        mExtractionType = ColorExtractionAlgorithm.newInstance(context);
    }

    public static WallpaperUtil with(Context context) {
        return new WallpaperUtil(context);
    }

    public WallpaperUtil setDrawable(Drawable drawable) {
        this.drawable = drawable;
        Log.d("LauncherRootView", "drawable=" + drawable);
        return this;
    }

    public WallpaperUtil setOnChangeListener(OnChangeListener mListener) {
        this.mListener = mListener;
        return this;
    }

    public void run() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (drawable != null) {
                    // Calculate how big the bitmap needs to be.
                    // This avoids unnecessary processing and allocation inside Palette.
                    final int requestedArea = drawable.getIntrinsicWidth() *
                            drawable.getIntrinsicHeight();
                    double scale = 1;
                    if (requestedArea > MAX_WALLPAPER_EXTRACTION_AREA) {
                        scale = Math.sqrt(MAX_WALLPAPER_EXTRACTION_AREA / (double) requestedArea);
                    }
                    Bitmap bitmap = Bitmap.createBitmap((int) (drawable.getIntrinsicWidth() * scale),
                            (int) (drawable.getIntrinsicHeight() * scale), Bitmap.Config.ARGB_8888);
                    final Canvas bmpCanvas = new Canvas(bitmap);
                    drawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
                    drawable.draw(bmpCanvas);
                    String value = VERSION_PREFIX + 1;

                    int color = findDominantColorByHue(bitmap, MAX_WALLPAPER_EXTRACTION_AREA);
                    value += "," + color;
                    Log.d("LauncherRootView", "value=" + value);

                    WallpaperColorsCompat mColorsCompat = parseValue(value).second;
                    update(mColorsCompat);
                }
            }
        }).start();
    }

    /**
     * Parses the stored value and returns the wallpaper id and wallpaper colors.
     */
    private Pair<Integer, WallpaperColorsCompat> parseValue(String value) {
        String[] parts = value.split(",");
        Integer wallpaperId = Integer.parseInt(parts[1]);
        if (parts.length == 2) {
            // There is no wallpaper color info present, eg when live wallpaper has no preview.
            return Pair.create(wallpaperId, null);
        }

        int primary = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
        int secondary = parts.length > 3 ? Integer.parseInt(parts[3]) : 0;
        int tertiary = parts.length > 4 ? Integer.parseInt(parts[4]) : 0;

        return Pair.create(wallpaperId, new WallpaperColorsCompat(primary, secondary, tertiary,
                0 /* hints */));
    }

    private void update(WallpaperColorsCompat wallpaperColors) {
        Pair<Integer, Integer> colors = mExtractionType.extractInto(wallpaperColors);
        int mainColor;
        int secondaryColor;
        if (colors != null) {
            mainColor = colors.first;
            secondaryColor = colors.second;
        } else {
            mainColor = FALLBACK_COLOR;
            secondaryColor = FALLBACK_COLOR;
        }
        boolean supportsDarkText = wallpaperColors != null
                ? (wallpaperColors.getColorHints()
                & WallpaperColorsCompat.HINT_SUPPORTS_DARK_TEXT) > 0 : false;
        boolean isDark = wallpaperColors != null
                ? (wallpaperColors.getColorHints()
                & WallpaperColorsCompat.HINT_SUPPORTS_DARK_THEME) > 0 : false;
        WallpaperColorInfo info = new WallpaperColorInfo(mainColor, secondaryColor, isDark, supportsDarkText);
        if (mListener != null) {
            mListener.onExtractedColorsChanged(info);
        }
    }

    public class WallpaperColorInfo{
        private int mMainColor;
        private int mSecondaryColor;
        private boolean mIsDark;
        private boolean mSupportsDarkText;

        WallpaperColorInfo(int mMainColor, int mSecondaryColor, boolean mIsDark, boolean mSupportsDarkText) {
            this.mMainColor = mMainColor;
            this.mSecondaryColor = mSecondaryColor;
            this.mIsDark = mIsDark;
            this.mSupportsDarkText = mSupportsDarkText;
        }

        public int getMainColor() {
            return mMainColor;
        }

        public int getSecondaryColor() {
            return mSecondaryColor;
        }

        public boolean isDark() {
            return mIsDark;
        }

        public boolean isSupportsDarkText() {
            return mSupportsDarkText;
        }

        @Override
        public String toString() {
            return "WallpaperColorInfo{" +
                    "mMainColor=" + mMainColor +
                    ", mSecondaryColor=" + mSecondaryColor +
                    ", mIsDark=" + mIsDark +
                    ", mSupportsDarkText=" + mSupportsDarkText +
                    '}';
        }
    }

    public interface OnChangeListener {
        void onExtractedColorsChanged(WallpaperColorInfo wallpaperColorInfo);
    }

}
