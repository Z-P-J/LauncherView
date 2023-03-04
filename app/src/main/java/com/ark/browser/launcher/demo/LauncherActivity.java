package com.ark.browser.launcher.demo;

import android.content.ComponentCallbacks2;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.widget.Toast;

import com.android.launcher3.LauncherFragment;

public class LauncherActivity extends BaseActivity {

    private static final String TAG = "LauncherActivity";

    // When starting an action mode, setting this tag will cause the action mode to be cancelled
    // automatically when user interacts with the launcher.
    public static final Object AUTO_CANCEL_ACTION_MODE = new Object();

    private LauncherFragment mLauncherFragment;

    private ActionMode mCurrentActionMode;

    @Override
    public void onActionModeStarted(ActionMode mode) {
        super.onActionModeStarted(mode);
        mCurrentActionMode = mode;
    }

    @Override
    public void onActionModeFinished(ActionMode mode) {
        super.onActionModeFinished(mode);
        mCurrentActionMode = null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_launcher);
        mLauncherFragment = findFragment(LauncherFragment.class);
        if (mLauncherFragment == null) {
            mLauncherFragment = new LauncherFragment();
            loadRootFragment(R.id.container, mLauncherFragment);
        }

        // For handling default keys
        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

    }

    @Override
    protected void onStop() {
        super.onStop();

        // Workaround for b/78520668, explicitly trim memory once UI is hidden
        onTrimMemory(TRIM_MEMORY_UI_HIDDEN);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            // The widget preview db can result in holding onto over
            // 3MB of memory for caching which isn't necessary.
            SQLiteDatabase.releaseMemory();
        }
    }

    @Override
    public void onBackPressedSupport() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            pop();
            return;
        }
        if (finishAutoCancelActionMode()) {
            Log.d(TAG, "finishAutoCancelActionMode=true");
            return;
        }
        Log.d(TAG, "finishAutoCancelActionMode=false");
        Toast.makeText(this, "TOOD 退出", Toast.LENGTH_SHORT).show();
    }

    public boolean finishAutoCancelActionMode() {
        if (mCurrentActionMode != null && AUTO_CANCEL_ACTION_MODE == mCurrentActionMode.getTag()) {
            mCurrentActionMode.finish();
            return true;
        }
        return false;
    }

}
