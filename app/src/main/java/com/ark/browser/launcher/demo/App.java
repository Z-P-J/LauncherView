package com.ark.browser.launcher.demo;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.zpj.utils.ContextUtils;
import com.zpj.utils.PrefsHelper;

public class App extends Application {

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;

        Log.d("App", "getApplication=" + getApplication());
        Log.d("App", "getApplication=" + ContextUtils.getApplicationContext());
        Log.d("App", "getApplication=" + PrefsHelper.with().getBoolean("is_first_run", true));

    }

    public static Application getApplication() {
        Application app = null;
        try {
            app = (Application) Class.forName("android.app.AppGlobals").getMethod("getInitialApplication").invoke(null);
            if (app == null)
                throw new IllegalStateException("Static initialization of Applications must be on main thread.");
        } catch (final Exception e) {
            try {
                app = (Application) Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null);
            } catch (final Exception ex) {

            }
        }
        return app;
    }

    public static Context getContext() {
        return context;
    }
}
