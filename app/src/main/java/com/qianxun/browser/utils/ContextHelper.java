package com.qianxun.browser.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import androidx.core.content.ContextCompat;

import com.android.launcher3.base.App;


/**
 * Created by yh on 2016/2/18.
 */
public class ContextHelper {

    private static Context application;
    private static Activity activity;
    private static Class splashCls;

    public static Context getAppContext() {
        if (application == null) {
            application = App.getContext();
        }
        return application.getApplicationContext();
    }

    public static Activity getActivity() {
//        if (activity == null) {
//            activity = ChromeLauncherActivity.ACTIVITY;
//        }
        return activity;
    }

    public static void setActivity(Activity mActivity) {
        activity = mActivity;
    }



    public static Resources getResources() {
        Context context = getAppContext();
        if (context != null) {
            return context.getResources();
        }
        return null;
    }

    public static void setSplashCls(Class cls) {
        ContextHelper.splashCls = cls;
    }

    public static Class getSplashCls() {
        return splashCls;
    }

    /**
     * 资源ID获取String
     */
    public static String getString(int stringId) {
        return getAppContext().getString(stringId);
    }

    public static String getString(int stringId, Object... formatArgs) {
        return getAppContext().getString(stringId, formatArgs);
    }


    /**
     * 获取颜色
     */
    public static int getColor(int color) {
        if (getAppContext() == null) {
            return 0;
        }
        return ContextCompat.getColor(getAppContext(), color);
    }

    public static int getDimensionPixelSize(int dimenId) {
        return getResources().getDimensionPixelSize(dimenId);
    }
}

