package com.android.launcher3.util;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;

import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.LauncherAppState;

public class WidgetUtil {

//    public static void updateWidgetSizeRanges(AppWidgetHostView widgetView, Launcher launcher,
//                                       int spanX, int spanY) {
//        Rect sTmpRect = new Rect();
//        getWidgetSizeRanges(launcher, spanX, spanY, sTmpRect);
//        widgetView.updateAppWidgetSize(null, sTmpRect.left, sTmpRect.top,
//                sTmpRect.right, sTmpRect.bottom);
//    }

    public static Rect getWidgetSizeRanges(Context context, int spanX, int spanY, Rect rect) {
        InvariantDeviceProfile inv = LauncherAppState.getIDP(context);

        // Initiate cell sizes.
        Point[] sCellSize = new Point[2];
        sCellSize[0] = inv.landscapeProfile.getCellSize();
        sCellSize[1] = inv.portraitProfile.getCellSize();

        if (rect == null) {
            rect = new Rect();
        }
        final float density = context.getResources().getDisplayMetrics().density;

        // Compute landscape size
        int landWidth = (int) ((spanX * sCellSize[0].x) / density);
        int landHeight = (int) ((spanY * sCellSize[0].y) / density);

        // Compute portrait size
        int portWidth = (int) ((spanX * sCellSize[1].x) / density);
        int portHeight = (int) ((spanY * sCellSize[1].y) / density);
        rect.set(portWidth, landHeight, landWidth, portHeight);
        return rect;
    }

}
