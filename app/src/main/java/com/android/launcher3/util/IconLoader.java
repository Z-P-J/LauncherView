package com.android.launcher3.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.Log;

import com.android.launcher3.BubbleTextView;
import com.android.launcher3.FastBitmapDrawable;
import com.android.launcher3.ItemInfoWithIcon;
import com.android.launcher3.graphics.ColorExtractor;
import com.android.launcher3.graphics.ShadowGenerator;
import com.qianxun.browser.launcher.R;
import com.zpj.utils.ContextUtils;

import static com.android.launcher3.graphics.ShadowGenerator.BLUR_FACTOR;

public class IconLoader {


    public static void load(BubbleTextView textView, ItemInfoWithIcon info) {
        long start = System.currentTimeMillis();
        int mIconSize = textView.getIconSize();
        final Bitmap icon = BitmapFactory.decodeResource(textView.getResources(), R.mipmap.ic_launcher_home);

        int color = ColorExtractor.findDominantColorByHue(icon);
        if (color == Color.WHITE) {
            color = Color.LTGRAY;
        } else {
            color = Color.WHITE;
        }

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);

        Bitmap bitmap = Bitmap.createBitmap(mIconSize, mIconSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas();
        canvas.setBitmap(bitmap);
        paint.setColor(color);
        canvas.drawCircle(mIconSize / 2f, mIconSize / 2f, mIconSize * (0.5f - BLUR_FACTOR), paint);
        Matrix matrix = new Matrix();
        float scale = 0.8f;
        matrix.setScale(scale, scale);
        matrix.postTranslate((mIconSize - scale * icon.getWidth()) / 2f, (mIconSize - scale * icon.getWidth()) / 2f);
        canvas.drawBitmap(icon, matrix, paint);

        Bitmap newBitmap = Bitmap.createBitmap(mIconSize, mIconSize, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(newBitmap);
        new ShadowGenerator(ContextUtils.getApplicationContext()).recreateIcon(bitmap, canvas);
        bitmap.recycle();

        FastBitmapDrawable iconDrawable = new FastBitmapDrawable(newBitmap, ColorExtractor.findDominantColorByHue(newBitmap));

        textView.setIcon(iconDrawable);

        Log.d("IconLoader", "deltaTime=" + (System.currentTimeMillis() - start));
    }

}
