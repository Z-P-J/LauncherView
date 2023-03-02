package com.android.launcher3;

import android.content.ComponentCallbacks2;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Point;
import android.os.Bundle;
import android.text.method.TextKeyListener;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.Toast;

import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.states.RotationHelper;
import com.android.launcher3.util.SystemUiController;
import com.android.launcher3.util.Themes;
import com.ark.browser.launcher.R;

import static android.content.pm.ActivityInfo.CONFIG_ORIENTATION;
import static android.content.pm.ActivityInfo.CONFIG_SCREEN_SIZE;

public class LauncherActivity extends BaseDraggingActivity {

    private static final String TAG = "LauncherActivity";


    private LauncherFragment mLauncherFragment;
//    private LauncherLayout mLauncherLayout;

    private RotationHelper mRotationHelper;
    private Configuration mOldConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LauncherAppState app = LauncherAppState.getInstance(this);
        mOldConfig = new Configuration(getResources().getConfiguration());
        initDeviceProfile(app.getInvariantDeviceProfile());

        mRotationHelper = new RotationHelper(this);

        setContentView(R.layout.activity_launcher);
        mLauncherFragment = findFragment(LauncherFragment.class);
        if (mLauncherFragment == null) {
            mLauncherFragment = new LauncherFragment();
            loadRootFragment(R.id.container, mLauncherFragment);
        }








        // For handling default keys
        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
        getSystemUiController().updateUiState(SystemUiController.UI_STATE_BASE_WINDOW,
                Themes.getAttrBoolean(this, R.attr.isWorkspaceDarkText));


        mRotationHelper.initialize();

    }

    @Override
    protected void onStop() {
        super.onStop();
        FirstFrameAnimatorHelper.setIsVisible(false);

        // Workaround for b/78520668, explicitly trim memory once UI is hidden
        onTrimMemory(TRIM_MEMORY_UI_HIDDEN);
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirstFrameAnimatorHelper.setIsVisible(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRotationHelper.destroy();
        TextKeyListener.getInstance().release();

        LauncherAnimUtils.onDestroyActivity();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            // The widget preview db can result in holding onto over
            // 3MB of memory for caching which isn't necessary.
            SQLiteDatabase.releaseMemory();

            // This clears all widget bitmaps from the widget tray
            // TODO(hyunyoungs)
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        FirstFrameAnimatorHelper.initializeDrawListener(getWindow().getDecorView());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        int diff = newConfig.diff(mOldConfig);
        if ((diff & (CONFIG_ORIENTATION | CONFIG_SCREEN_SIZE)) != 0) {
            initDeviceProfile(mDeviceProfile.inv);
            dispatchDeviceProfileChanged();
            reapplyUi();
            mLauncherFragment.onConfigurationChanged();
        }

        mOldConfig.setTo(newConfig);
        super.onConfigurationChanged(newConfig);
    }

//    @Override
//    public boolean onKeyUp(int keyCode, KeyEvent event) {
//        if (keyCode == KeyEvent.KEYCODE_MENU) {
//            // KEYCODE_MENU is sent by some tests, for example
//            // LauncherJankTests#testWidgetsContainerFling. Don't just remove its handling.
//            if (!mLauncherLayout.getDragController().isDragging() && !mLauncherLayout.getWorkspace().isSwitchingState() &&
//                    mLauncherLayout.isInState(NORMAL)) {
//                // Close any open floating views.
//                AbstractFloatingView.closeAllOpenViews(this);
//
//                // Setting the touch point to (-1, -1) will show the options popup in the center of
//                // the screen.
//                OptionsPopupView.showDefaultOptions(mLauncherLayout, -1, -1);
//            }
//            return true;
//        }
//        return super.onKeyUp(keyCode, event);
//    }

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

    @Override
    public DragLayer getDragLayer() {
        return mLauncherFragment.getDragLayer();
    }

    public LauncherLayout getLauncherLayout() {
        return mLauncherFragment.getLauncherLayout();
    }

    @Override
    public <T extends View> T getOverviewPanel() {
        return mLauncherFragment.getOverviewPanel();
    }

    @Override
    public View getRootView() {
        return mLauncherFragment.getRootView();
    }

    @Override
    protected void reapplyUi() {
        mLauncherFragment.reapplyUi();
    }

    public RotationHelper getRotationHelper() {
        return mRotationHelper;
    }

    private void initDeviceProfile(InvariantDeviceProfile idp) {
        // Load configuration-specific DeviceProfile
        mDeviceProfile = idp.getDeviceProfile(this);
        if (isInMultiWindowModeCompat()) {
            Display display = getWindowManager().getDefaultDisplay();
            Point mwSize = new Point();
            display.getSize(mwSize);
            mDeviceProfile = mDeviceProfile.getMultiWindowProfile(this, mwSize);
        }
        onDeviceProfileInitiated();
    }

    public void rebindWorkspace(InvariantDeviceProfile idp) {
        idp.update(this);
        initDeviceProfile(idp);
        mLauncherFragment.getLauncherLayout().rebindWorkspace(idp);
    }





    public interface LauncherOverlay {

        /**
         * Touch interaction leading to overscroll has begun
         */
        void onScrollInteractionBegin();

        /**
         * Touch interaction related to overscroll has ended
         */
        void onScrollInteractionEnd();

        /**
         * Scroll progress, between 0 and 100, when the user scrolls beyond the leftmost
         * screen (or in the case of RTL, the rightmost screen).
         */
        void onScrollChange(float progress, boolean rtl);

    }

}
