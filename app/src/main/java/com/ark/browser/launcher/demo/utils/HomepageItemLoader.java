package com.ark.browser.launcher.demo.utils;

import static com.ark.browser.launcher.demo.utils.ShadowGenerator.BLUR_FACTOR;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherLayout;
import com.android.launcher3.LauncherManager;
import com.android.launcher3.database.table.FavoriteItemTable;
import com.android.launcher3.database.SQLite;
import com.android.launcher3.graphics.ColorExtractor;
import com.android.launcher3.model.FavoriteItem;
import com.ark.browser.launcher.demo.R;
import com.zpj.utils.Callback;
import com.zpj.utils.ColorUtils;
import com.zpj.utils.ContextUtils;

import java.util.ArrayList;

public class HomepageItemLoader implements LauncherLayout.ItemLoader {
    @Override
    public void onFirstRun() {
        SQLite.with(FavoriteItemTable.class).delete();
        ArrayList<ItemInfo> itemInfoArrayList = new ArrayList<>(HomepageUtils.initHomeNav());
        for (ItemInfo info : itemInfoArrayList) {
            FavoriteItem.from(info).insert();
        }
    }

    @Override
    public View onCreateSearchBar(ViewGroup root) {
        Context context = root.getContext();
        return LayoutInflater.from(context)
                .inflate(R.layout.qsb_default_view, root, false);
    }

    @Override
    public void loadIcon(ItemInfo itemInfo, Callback<Bitmap> callback) {
        Resources resources = ContextUtils.getApplicationContext().getResources();
        int resId = R.mipmap.ic_launcher_home;
        if (DeepLinks.isDeepLink(itemInfo.url)) {
            switch (itemInfo.url) {
                case DeepLinks.DEEPLINK_MANAGER:
                    resId = R.drawable.icon_browser_manager;
                    break;
                case DeepLinks.DEEPLINK_COLLECTIONS:
                    resId = R.drawable.icon_collections;
                    break;
                case DeepLinks.DEEPLINK_BROWSER:
                    resId = R.drawable.icon_browser;
                    break;
                case DeepLinks.DEEPLINK_DOWNLOADS:
                    resId = R.drawable.icon_download_manager;
                    break;
                case DeepLinks.DEEPLINK_SETTINGS:
                    resId = R.drawable.icon_settings;
                    break;
            }
        }
        callback.onCallback(decorateBitmap(BitmapFactory.decodeResource(resources, resId)));
    }

    public static Bitmap decorateBitmap(Bitmap icon) {
        long start = System.currentTimeMillis();
        DeviceProfile grid = LauncherManager.getDeviceProfile();
        int mIconSize = grid.iconSizePx;

        int color = ColorExtractor.findDominantColorByHue(icon);
        color = ColorUtils.getDarkenedColor(color, 1.2f);
//        if (color == Color.WHITE) {
//            color = Color.LTGRAY;
//        } else {
//            color = Color.WHITE;
//        }

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);

        Bitmap bitmap = Bitmap.createBitmap(mIconSize, mIconSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas();
        canvas.setBitmap(bitmap);
        paint.setColor(color);
//        canvas.drawCircle(mIconSize / 2f, mIconSize / 2f, mIconSize * (0.5f - BLUR_FACTOR), paint);
        float r = mIconSize * (0.25f - BLUR_FACTOR);
        float padding = BLUR_FACTOR * mIconSize;
        canvas.drawRoundRect(padding, padding, mIconSize - padding, mIconSize - padding, r, r, paint);
        Matrix matrix = new Matrix();
        float scale = 0.8f;
        matrix.setScale(scale, scale);
        matrix.postTranslate((mIconSize - scale * icon.getWidth()) / 2f, (mIconSize - scale * icon.getWidth()) / 2f);
        canvas.drawBitmap(icon, matrix, paint);

        Bitmap newBitmap = Bitmap.createBitmap(mIconSize, mIconSize, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(newBitmap);
        new ShadowGenerator(ContextUtils.getApplicationContext()).recreateIcon(bitmap, canvas);
        bitmap.recycle();

//        FastBitmapDrawable iconDrawable = new FastBitmapDrawable(newBitmap, ColorExtractor.findDominantColorByHue(newBitmap));
//
//        textView.setIcon(iconDrawable);
//
        Log.d("IconLoader", "deltaTime=" + (System.currentTimeMillis() - start));

        return newBitmap;
    }

}
