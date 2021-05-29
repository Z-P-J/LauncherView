package com.android.launcher3;

import android.content.ComponentCallbacks2;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.method.TextKeyListener;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;

import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.states.RotationHelper;
import com.android.launcher3.util.SystemUiController;
import com.android.launcher3.util.Themes;
import com.android.launcher3.views.OptionsPopupView;

import static android.content.pm.ActivityInfo.CONFIG_ORIENTATION;
import static android.content.pm.ActivityInfo.CONFIG_SCREEN_SIZE;
import static com.android.launcher3.LauncherState.ALL_APPS;
import static com.android.launcher3.LauncherState.NORMAL;

public class LauncherActivity extends BaseDraggingActivity {

    private static final String TAG = "LauncherActivity";


    private LauncherLayout mLauncherLayout;

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
        mLauncherLayout = findViewById(R.id.launcher_layout);
        mLauncherLayout.init(savedInstanceState);








        // For handling default keys
        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
        getSystemUiController().updateUiState(SystemUiController.UI_STATE_BASE_WINDOW,
                Themes.getAttrBoolean(this, R.attr.isWorkspaceDarkText));


        mRotationHelper.initialize();

    }


    // Type: int
    private static final String RUNTIME_STATE_CURRENT_SCREEN = "launcher.current_screen";
    // Type: int
    private static final String RUNTIME_STATE = "launcher.state";
    // Type: ActivityResultInfo
    private static final String RUNTIME_STATE_PENDING_ACTIVITY_RESULT = "launcher.activity_result";
    // Type: SparseArray<Parcelable>
    private static final String RUNTIME_STATE_WIDGET_PANEL = "launcher.widget_panel";

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mLauncherLayout.getWorkspace().getChildCount() > 0) {
            outState.putInt(RUNTIME_STATE_CURRENT_SCREEN, mLauncherLayout.getWorkspace().getNextPage());

        }
        outState.putInt(RUNTIME_STATE, mLauncherLayout.getStateManager().getState().ordinal);


        AbstractFloatingView widgets = AbstractFloatingView
                .getOpenView(this, AbstractFloatingView.TYPE_WIDGETS_FULL_SHEET);
        if (widgets != null) {
            SparseArray<Parcelable> widgetsState = new SparseArray<>();
            widgets.saveHierarchyState(widgetsState);
            outState.putSparseParcelableArray(RUNTIME_STATE_WIDGET_PANEL, widgetsState);
        } else {
            outState.remove(RUNTIME_STATE_WIDGET_PANEL);
        }

        // We close any open folders and shortcut containers since they will not be re-opened,
        // and we need to make sure this state is reflected.
        AbstractFloatingView.closeAllOpenViews(this, false);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStop() {
        super.onStop();
        FirstFrameAnimatorHelper.setIsVisible(false);

        mLauncherLayout.getStateManager().moveToRestState();

        // Workaround for b/78520668, explicitly trim memory once UI is hidden
        onTrimMemory(TRIM_MEMORY_UI_HIDDEN);
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirstFrameAnimatorHelper.setIsVisible(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mLauncherLayout.getDragController().cancelDrag();
        mLauncherLayout.getDragController().resetLastGestureUpTime();
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
            mLauncherLayout.getDragLayer().recreateControllers();

            // TODO: We can probably avoid rebind when only screen size changed.
            mLauncherLayout.rebindModel();
        }

        mOldConfig.setTo(newConfig);
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            // KEYCODE_MENU is sent by some tests, for example
            // LauncherJankTests#testWidgetsContainerFling. Don't just remove its handling.
            if (!mLauncherLayout.getDragController().isDragging() && !mLauncherLayout.getWorkspace().isSwitchingState() &&
                    mLauncherLayout.isInState(NORMAL)) {
                // Close any open floating views.
                AbstractFloatingView.closeAllOpenViews(this);

                // Setting the touch point to (-1, -1) will show the options popup in the center of
                // the screen.
                OptionsPopupView.showDefaultOptions(mLauncherLayout, -1, -1);
            }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (finishAutoCancelActionMode()) {
            Log.d(TAG, "finishAutoCancelActionMode=true");
            return;
        }
        Log.d(TAG, "finishAutoCancelActionMode=false");

        if (mLauncherLayout.getDragController().isDragging()) {
            mLauncherLayout.getDragController().cancelDrag();
            return;
        }

        // Note: There should be at most one log per method call. This is enforced implicitly
        // by using if-else statements.
        AbstractFloatingView topView = AbstractFloatingView.getTopOpenView(mLauncherLayout);
        Log.d(TAG, "topView=" + topView + " isInState(ALL_APPS)=" + mLauncherLayout.isInState(ALL_APPS));
        if (topView != null && topView.onBackPressed()) {
            // Handled by the floating view.
        }
//        else if (!isInState(NORMAL)) {
//            LauncherState lastState = mStateManager.getLastState();
//            mStateManager.goToState(lastState);
//        }
        else if (mLauncherLayout.isInState(ALL_APPS)) {
            mLauncherLayout.getStateManager().goToState(NORMAL);
        } else {
            // Back button is a no-op here, but give at least some feedback for the button press
            mLauncherLayout.getWorkspace().showOutlinesTemporarily();
        }
    }

    @Override
    public DragLayer getDragLayer() {
        return mLauncherLayout.getDragLayer();
    }

    public LauncherLayout getLauncherLayout() {
        return mLauncherLayout;
    }

    @Override
    public <T extends View> T getOverviewPanel() {
        return mLauncherLayout.getOverviewPanel();
    }

    @Override
    public View getRootView() {
        return mLauncherLayout.getRootView();
    }

    @Override
    protected void reapplyUi() {
        mLauncherLayout.getRootView().dispatchInsets();
        mLauncherLayout.getStateManager().reapplyState(true /* cancelCurrentAnimation */);
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
