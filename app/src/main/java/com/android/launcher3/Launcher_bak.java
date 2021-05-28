///*
// * Copyright (C) 2008 The Android Open Source Project
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package com.android.launcher3;
//
//import android.animation.Animator;
//import android.animation.AnimatorSet;
//import android.animation.ValueAnimator;
//import android.content.ActivityNotFoundException;
//import android.content.ComponentCallbacks2;
//import android.content.Context;
//import android.content.ContextWrapper;
//import android.content.Intent;
//import android.content.IntentSender;
//import android.content.res.Configuration;
//import android.database.sqlite.SQLiteDatabase;
//import android.graphics.Point;
//import android.os.Bundle;
//import android.os.Parcelable;
//import android.os.UserHandle;
//import android.support.annotation.Nullable;
//import android.text.method.TextKeyListener;
//import android.util.Log;
//import android.util.SparseArray;
//import android.view.Display;
//import android.view.KeyEvent;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.view.accessibility.AccessibilityEvent;
//import android.view.animation.OvershootInterpolator;
//
//import com.android.launcher3.Workspace.ItemOperator;
//import com.android.launcher3.dragndrop.DragController;
//import com.android.launcher3.dragndrop.DragLayer;
//import com.android.launcher3.folder.FolderIcon;
//import com.android.launcher3.keyboard.ViewGroupFocusHelper;
//import com.android.launcher3.states.RotationHelper;
//import com.android.launcher3.touch.ItemClickHandler;
//import com.android.launcher3.util.SystemUiController;
//import com.android.launcher3.util.Themes;
//import com.android.launcher3.util.Thunk;
//import com.android.launcher3.views.OptionsPopupView;
//import com.qianxun.browser.database.HomepageManager;
//import com.zpj.utils.KeyboardUtils;
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.List;
//
//import static android.content.pm.ActivityInfo.CONFIG_ORIENTATION;
//import static android.content.pm.ActivityInfo.CONFIG_SCREEN_SIZE;
//import static com.android.launcher3.LauncherState.ALL_APPS;
//import static com.android.launcher3.LauncherState.NORMAL;
//
///**
// * Default launcher application.
// */
//public class Launcher_bak extends BaseDraggingActivity implements LauncherLoader.Callbacks {
//    public static final String TAG = "Launcher";
//    static final boolean LOGD = false;
//
//    public static final int NEW_SHORTCUT_BOUNCE_DURATION = 450;
//    public static final int NEW_SHORTCUT_STAGGER_DELAY = 85;
//
//    private static final float BOUNCE_ANIMATION_TENSION = 1.3f;
//
//    // Type: int
//    private static final String RUNTIME_STATE_CURRENT_SCREEN = "launcher.current_screen";
//    // Type: int
//    private static final String RUNTIME_STATE = "launcher.state";
//    // Type: ActivityResultInfo
//    private static final String RUNTIME_STATE_PENDING_ACTIVITY_RESULT = "launcher.activity_result";
//    // Type: SparseArray<Parcelable>
//    private static final String RUNTIME_STATE_WIDGET_PANEL = "launcher.widget_panel";
//
//    private LauncherStateManager mStateManager;
//
//    private static final int ON_ACTIVITY_RESULT_ANIMATION_DELAY = 500;
//
//    // How long to wait before the new-shortcut animation automatically pans the workspace
//    private static final int NEW_APPS_PAGE_MOVE_DELAY = 500;
//    private static final int NEW_APPS_ANIMATION_INACTIVE_TIMEOUT_SECONDS = 5;
//    @Thunk
//    static final int NEW_APPS_ANIMATION_DELAY = 500;
//
//    private Configuration mOldConfig;
//
//    @Thunk
//    Workspace mWorkspace;
//    private View mLauncherView;
//    @Thunk
//    DragLayer mDragLayer;
//    private DragController mDragController;
//
//    private final int[] mTmpAddItemCellCoordinates = new int[2];
//
//    @Thunk
//    HotSeat mHotSeat;
//    @Nullable
//    private View mHotseatSearchBox;
//
//    private DropTargetBar mDropTargetBar;
//
//    // UI and state for the overview panel
//    private View mOverviewPanel;
//
//    @Thunk
//    boolean mWorkspaceLoading = true;
//
//    private OnResumeCallback mOnResumeCallback;
//
//    private int mSynchronouslyBoundPage = PagedView.INVALID_PAGE;
//
//    public ViewGroupFocusHelper mFocusHandler;
//
//    private RotationHelper mRotationHelper;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//
//        super.onCreate(savedInstanceState);
//
//        LauncherAppState app = LauncherAppState.getInstance(this);
//        mOldConfig = new Configuration(getResources().getConfiguration());
//
//        initDeviceProfile(app.getInvariantDeviceProfile());
//
//        mDragController = new DragController(this);
//        mStateManager = new LauncherStateManager(this);
//
//        mLauncherView = LayoutInflater.from(this).inflate(R.layout.launcher, null);
//
//        setupViews();
//
//        mRotationHelper = new RotationHelper(this);
//
//        restoreState(savedInstanceState);
//
//        // We only load the page synchronously if the user rotates (or triggers a
//        // configuration change) while launcher is in the foreground
//        int currentScreen = PagedView.INVALID_RESTORE_PAGE;
//        if (savedInstanceState != null) {
//            currentScreen = savedInstanceState.getInt(RUNTIME_STATE_CURRENT_SCREEN, currentScreen);
//        }
//
//        mWorkspace.setCurrentPage(currentScreen);
//        setWorkspaceLoading(true);
//
//        LauncherLoader loader = new LauncherLoader(this, app);
//        loader.initialize(this);
//        loader.startLoader(currentScreen);
//
//
//        // For handling default keys
//        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
//
//        setContentView(mLauncherView);
//        getRootView().dispatchInsets();
//
//        getSystemUiController().updateUiState(SystemUiController.UI_STATE_BASE_WINDOW,
//                Themes.getAttrBoolean(this, R.attr.isWorkspaceDarkText));
//
//        mRotationHelper.initialize();
//    }
//
//    @Override
//    public void onConfigurationChanged(Configuration newConfig) {
//        int diff = newConfig.diff(mOldConfig);
//        if ((diff & (CONFIG_ORIENTATION | CONFIG_SCREEN_SIZE)) != 0) {
//            initDeviceProfile(mDeviceProfile.inv);
//            dispatchDeviceProfileChanged();
//            reapplyUi();
//            mDragLayer.recreateControllers();
//
//            // TODO: We can probably avoid rebind when only screen size changed.
//            rebindModel();
//        }
//
//        mOldConfig.setTo(newConfig);
//        super.onConfigurationChanged(newConfig);
//    }
//
//    @Override
//    protected void reapplyUi() {
//        getRootView().dispatchInsets();
//        getStateManager().reapplyState(true /* cancelCurrentAnimation */);
//    }
//
//    @Override
//    public void rebindModel() {
////        int currentPage = mWorkspace.getNextPage();
////        if (mModel.startLoader(currentPage)) {
////            mWorkspace.setCurrentPage(currentPage);
////            setWorkspaceLoading(true);
////        }
//    }
//
//    private void initDeviceProfile(InvariantDeviceProfile idp) {
//        // Load configuration-specific DeviceProfile
//        mDeviceProfile = idp.getDeviceProfile(this);
//        if (isInMultiWindowModeCompat()) {
//            Display display = getWindowManager().getDefaultDisplay();
//            Point mwSize = new Point();
//            display.getSize(mwSize);
//            mDeviceProfile = mDeviceProfile.getMultiWindowProfile(this, mwSize);
//        }
//        onDeviceProfileInitiated();
//    }
//
//    public RotationHelper getRotationHelper() {
//        return mRotationHelper;
//    }
//
//    public LauncherStateManager getStateManager() {
//        return mStateManager;
//    }
//
//    @Override
//    public <T extends View> T findViewById(int id) {
//        return mLauncherView.findViewById(id);
//    }
//
//    public boolean isDraggingEnabled() {
//        // We prevent dragging when we are loading the workspace as it is possible to pick up a view
//        // that is subsequently removed from the workspace in startBinding().
//        return !isWorkspaceLoading();
//    }
//
//    public int getViewIdForItem(ItemInfo info) {
//        // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
//        // This cast is safe as long as the id < 0x00FFFFFF
//        // Since we jail all the dynamically generated views, there should be no clashes
//        // with any other views.
//        return (int) info.id;
//    }
//
//    @Override
//    protected void onStop() {
//        super.onStop();
//        FirstFrameAnimatorHelper.setIsVisible(false);
//
//        getStateManager().moveToRestState();
//
//        // Workaround for b/78520668, explicitly trim memory once UI is hidden
//        onTrimMemory(TRIM_MEMORY_UI_HIDDEN);
//    }
//
//    @Override
//    protected void onStart() {
//        super.onStart();
//        FirstFrameAnimatorHelper.setIsVisible(true);
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//
//        setOnResumeCallback(null);
//    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//        mDragController.cancelDrag();
//        mDragController.resetLastGestureUpTime();
//    }
//
//    @Override
//    public void onWindowFocusChanged(boolean hasFocus) {
//        super.onWindowFocusChanged(hasFocus);
//        mStateManager.onWindowFocusChanged();
//    }
//
//    public interface LauncherOverlay {
//
//        /**
//         * Touch interaction leading to overscroll has begun
//         */
//        void onScrollInteractionBegin();
//
//        /**
//         * Touch interaction related to overscroll has ended
//         */
//        void onScrollInteractionEnd();
//
//        /**
//         * Scroll progress, between 0 and 100, when the user scrolls beyond the leftmost
//         * screen (or in the case of RTL, the rightmost screen).
//         */
//        void onScrollChange(float progress, boolean rtl);
//
//    }
//
//    public boolean isInState(LauncherState state) {
//        return mStateManager.getState() == state;
//    }
//
//    /**
//     * Restores the previous state, if it exists.
//     *
//     * @param savedState The previous state.
//     */
//    private void restoreState(Bundle savedState) {
//        if (savedState == null) {
//            return;
//        }
//
//        int stateOrdinal = savedState.getInt(RUNTIME_STATE, NORMAL.ordinal);
//        LauncherState[] stateValues = LauncherState.values();
//        LauncherState state = stateValues[stateOrdinal];
//        if (!state.disableRestore) {
//            mStateManager.goToState(state, false /* animated */);
//        }
//    }
//
//    /**
//     * Finds all the views we need and configure them properly.
//     */
//    private void setupViews() {
//        mDragLayer = findViewById(R.id.drag_layer);
//        mFocusHandler = mDragLayer.getFocusIndicatorHelper();
//        mWorkspace = mDragLayer.findViewById(R.id.workspace);
//        mWorkspace.initParentViews(mDragLayer);
//        mOverviewPanel = findViewById(R.id.overview_panel);
//        mHotSeat = findViewById(R.id.hotseat);
//        mHotseatSearchBox = findViewById(R.id.search_container_hotseat);
//
//        mLauncherView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
//
//        // Setup the drag layer
//        mDragLayer.setup(mDragController, mWorkspace);
//
//        mWorkspace.setup(mDragController);
//        // Until the workspace is bound, ensure that we keep the wallpaper offset locked to the
//        // default state, otherwise we will update to the wrong offsets in RTL
//        mWorkspace.bindAndInitFirstWorkspaceScreen(null /* recycled qsb */);
//        mDragController.addDragListener(mWorkspace);
//
//        // Get the search/delete/uninstall bar
//        mDropTargetBar = mDragLayer.findViewById(R.id.drop_target_bar);
//
//        // Setup the drag controller (drop targets have to be added in reverse order in priority)
//        mDragController.setMoveTarget(mWorkspace);
//        mDropTargetBar.setup(mDragController);
//
//        mStateManager.goToState(NORMAL, false /* animated */);
//    }
//
//    /**
//     * Creates a view representing a shortcut.
//     *
//     * @param info The data structure describing the shortcut.
//     */
//    View createShortcut(ShortcutInfo info) {
//        return createShortcut((ViewGroup) mWorkspace.getChildAt(mWorkspace.getCurrentPage()), info);
//    }
//
//    /**
//     * Creates a view representing a shortcut inflated from the specified resource.
//     *
//     * @param parent The group the shortcut belongs to.
//     * @param info   The data structure describing the shortcut.
//     * @return A View inflated from layoutResId.
//     */
//    public View createShortcut(ViewGroup parent, ShortcutInfo info) {
//        BubbleTextView favorite = (BubbleTextView) LayoutInflater.from(parent.getContext())
//                .inflate(R.layout.app_icon, parent, false);
//        favorite.applyFromShortcutInfo(info);
//        favorite.setOnClickListener(ItemClickHandler.INSTANCE);
//        favorite.setOnFocusChangeListener(mFocusHandler);
//        return favorite;
//    }
//
//    public FolderIcon findFolderIcon(final long folderIconId) {
//        return (FolderIcon) mWorkspace.getFirstMatch(new ItemOperator() {
//            @Override
//            public boolean evaluate(ItemInfo info, View view) {
//                return info != null && info.id == folderIconId;
//            }
//        });
//    }
//
//    @Override
//    public void onAttachedToWindow() {
//        super.onAttachedToWindow();
//
//        FirstFrameAnimatorHelper.initializeDrawListener(getWindow().getDecorView());
//    }
//
//    @Override
//    public void onDetachedFromWindow() {
//        super.onDetachedFromWindow();
//    }
//
//    @Override
//    public LauncherRootView getRootView() {
//        return (LauncherRootView) mLauncherView;
//    }
//
//    @Override
//    public DragLayer getDragLayer() {
//        return mDragLayer;
//    }
//
//    public Workspace getWorkspace() {
//        return mWorkspace;
//    }
//
//    public HotSeat getHotseat() {
//        return mHotSeat;
//    }
//
//    public View getHotSeatSearchBox() {
//        return mHotseatSearchBox;
//    }
//
//    public <T extends View> T getOverviewPanel() {
//        return (T) mOverviewPanel;
//    }
//
//    public DropTargetBar getDropTargetBar() {
//        return mDropTargetBar;
//    }
//
//    public int getOrientation() {
//        return mOldConfig.orientation;
//    }
//
//    @Override
//    protected void onNewIntent(Intent intent) {
//        super.onNewIntent(intent);
//
//        boolean alreadyOnHome = hasWindowFocus() && ((intent.getFlags() &
//                Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
//                != Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
//
//        // Check this condition before handling isActionMain, as this will get reset.
//        boolean shouldMoveToDefaultScreen = alreadyOnHome && isInState(NORMAL)
//                && AbstractFloatingView.getTopOpenView(this) == null;
//        boolean isActionMain = Intent.ACTION_MAIN.equals(intent.getAction());
//
//        if (isActionMain) {
//            // In all these cases, only animate if we're already on home
//            AbstractFloatingView.closeAllOpenViews(this, isStarted());
//
//            if (!isInState(NORMAL)) {
//                // Only change state, if not already the same. This prevents cancelling any
//                // animations running as part of resume
//                mStateManager.goToState(NORMAL);
//            }
//
//            if (shouldMoveToDefaultScreen && !mWorkspace.isTouchActive()) {
//                mWorkspace.post(mWorkspace::moveToDefaultScreen);
//            }
//
//            final View v = getWindow().peekDecorView();
//            if (v != null && v.getWindowToken() != null) {
//                KeyboardUtils.hideSoftInputKeyboard(v);
//            }
//        }
//    }
//
//    @Override
//    public void onRestoreInstanceState(Bundle state) {
//        super.onRestoreInstanceState(state);
//        mWorkspace.restoreInstanceStateForChild(mSynchronouslyBoundPage);
//    }
//
//    @Override
//    protected void onSaveInstanceState(Bundle outState) {
//        if (mWorkspace.getChildCount() > 0) {
//            outState.putInt(RUNTIME_STATE_CURRENT_SCREEN, mWorkspace.getNextPage());
//
//        }
//        outState.putInt(RUNTIME_STATE, mStateManager.getState().ordinal);
//
//
//        AbstractFloatingView widgets = AbstractFloatingView
//                .getOpenView(this, AbstractFloatingView.TYPE_WIDGETS_FULL_SHEET);
//        if (widgets != null) {
//            SparseArray<Parcelable> widgetsState = new SparseArray<>();
//            widgets.saveHierarchyState(widgetsState);
//            outState.putSparseParcelableArray(RUNTIME_STATE_WIDGET_PANEL, widgetsState);
//        } else {
//            outState.remove(RUNTIME_STATE_WIDGET_PANEL);
//        }
//
//        // We close any open folders and shortcut containers since they will not be re-opened,
//        // and we need to make sure this state is reflected.
//        AbstractFloatingView.closeAllOpenViews(this, false);
//
//        super.onSaveInstanceState(outState);
//    }
//
//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//
////        unregisterReceiver(mScreenOffReceiver);
//        mWorkspace.removeFolderListeners();
//
//        mRotationHelper.destroy();
//
//        TextKeyListener.getInstance().release();
//
//        LauncherAnimUtils.onDestroyActivity();
//
//        clearPendingBinds();
//    }
//
//    public DragController getDragController() {
//        return mDragController;
//    }
//
//    @Override
//    public void startIntentSenderForResult(IntentSender intent, int requestCode,
//                                           Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags, Bundle options) {
//        try {
//            super.startIntentSenderForResult(intent, requestCode,
//                    fillInIntent, flagsMask, flagsValues, extraFlags, options);
//        } catch (IntentSender.SendIntentException e) {
//            throw new ActivityNotFoundException();
//        }
//    }
//
//    public boolean isWorkspaceLocked() {
//        return mWorkspaceLoading;
//    }
//
//    public boolean isWorkspaceLoading() {
//        return mWorkspaceLoading;
//    }
//
//    private void setWorkspaceLoading(boolean value) {
//        mWorkspaceLoading = value;
//    }
//
//    FolderIcon addFolder(CellLayout layout, long container, final long screenId, int cellX,
//                         int cellY) {
//        final FolderInfo folderInfo = new FolderInfo();
//        folderInfo.title = getText(R.string.folder_name);
//
//        // Update the model
//        HomepageManager.getInstance().addItemToDatabase(folderInfo, container, screenId, cellX, cellY);
//
//        // Create the view
//        FolderIcon newFolder = FolderIcon.fromXml(R.layout.folder_icon, this, layout, folderInfo);
//        mWorkspace.addInScreen(newFolder, folderInfo);
//        // Force measure the new folder icon
//        CellLayout parent = mWorkspace.getParentCellLayoutForView(newFolder);
//        parent.getShortcutsAndWidgets().measureChild(newFolder);
//        return newFolder;
//    }
//
//    /**
//     * Unbinds the view for the specified item, and removes the item and all its children.
//     *
//     * @param v            the view being removed.
//     * @param itemInfo     the {@link ItemInfo} for this view.
//     * @param deleteFromDb whether or not to delete this item from the db.
//     */
//    public boolean removeItem(View v, final ItemInfo itemInfo, boolean deleteFromDb) {
//        if (itemInfo instanceof ShortcutInfo) {
//            // Remove the shortcut from the folder before removing it from launcher
//            View folderIcon = mWorkspace.getHomescreenIconByItemId(itemInfo.container);
//            if (folderIcon instanceof FolderIcon) {
//                ((FolderInfo) folderIcon.getTag()).remove((ShortcutInfo) itemInfo, true);
//            } else {
//                mWorkspace.removeWorkspaceItem(v);
//            }
//            if (deleteFromDb) {
//                HomepageManager.getInstance().deleteItemFromDatabase(itemInfo);
//            }
//        } else if (itemInfo instanceof FolderInfo) {
//            final FolderInfo folderInfo = (FolderInfo) itemInfo;
//            if (v instanceof FolderIcon) {
//                ((FolderIcon) v).removeListeners();
//            }
//            mWorkspace.removeWorkspaceItem(v);
//            if (deleteFromDb) {
//                HomepageManager.getInstance().deleteFolderAndContentsFromDatabase(folderInfo);
//            }
//        } else {
//            return false;
//        }
//        return true;
//    }
//
//    @Override
//    public boolean dispatchKeyEvent(KeyEvent event) {
//        Log.d(TAG, "keyEvent=" + event);
//        return (event.getKeyCode() == KeyEvent.KEYCODE_HOME) || super.dispatchKeyEvent(event);
//    }
//
//    @Override
//    public void onBackPressed() {
//        if (finishAutoCancelActionMode()) {
//            Log.d(TAG, "finishAutoCancelActionMode=true");
//            return;
//        }
//        Log.d(TAG, "finishAutoCancelActionMode=false");
//
//        if (mDragController.isDragging()) {
//            mDragController.cancelDrag();
//            return;
//        }
//
//        // Note: There should be at most one log per method call. This is enforced implicitly
//        // by using if-else statements.
//        AbstractFloatingView topView = AbstractFloatingView.getTopOpenView(this);
//        Log.d(TAG, "topView=" + topView + " isInState(ALL_APPS)=" + isInState(ALL_APPS));
//        if (topView != null && topView.onBackPressed()) {
//            // Handled by the floating view.
//        }
////        else if (!isInState(NORMAL)) {
////            LauncherState lastState = mStateManager.getLastState();
////            mStateManager.goToState(lastState);
////        }
//        else if (isInState(ALL_APPS)) {
//            mStateManager.goToState(NORMAL);
//        } else {
//            // Back button is a no-op here, but give at least some feedback for the button press
//            mWorkspace.showOutlinesTemporarily();
//        }
//    }
//
//    boolean isHotseatLayout(View layout) {
//        // TODO: Remove this method
//        return mHotSeat != null && layout != null &&
//                (layout instanceof CellLayout) && (layout == mHotSeat.getLayout());
//    }
//
//    /**
//     * Returns the CellLayout of the specified container at the specified screen.
//     */
//    public CellLayout getCellLayout(long container, long screenId) {
//        if (container == ItemInfo.CONTAINER_HOTSEAT) {
//            if (mHotSeat != null) {
//                return mHotSeat.getLayout();
//            } else {
//                return null;
//            }
//        } else {
//            return mWorkspace.getScreenWithId(screenId);
//        }
//    }
//
//    @Override
//    public void onTrimMemory(int level) {
//        super.onTrimMemory(level);
//        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
//            // The widget preview db can result in holding onto over
//            // 3MB of memory for caching which isn't necessary.
//            SQLiteDatabase.releaseMemory();
//
//            // This clears all widget bitmaps from the widget tray
//            // TODO(hyunyoungs)
//        }
//    }
//
//    @Override
//    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
//        final boolean result = super.dispatchPopulateAccessibilityEvent(event);
//        final List<CharSequence> text = event.getText();
//        text.clear();
//        // Populate event with a fake title based on the current state.
//        // TODO: When can workspace be null?
//        text.add(mWorkspace == null
//                ? getString(R.string.all_apps_home_button_label)
//                : mStateManager.getState().getDescription(this));
//        return result;
//    }
//
//    public void setOnResumeCallback(OnResumeCallback callback) {
//        if (mOnResumeCallback != null) {
//            mOnResumeCallback.onLauncherResume();
//        }
//        mOnResumeCallback = callback;
//    }
//
//    /**
//     * Implementation of the method from LauncherModel.Callbacks.
//     */
//    @Override
//    public int getCurrentWorkspaceScreen() {
//        if (mWorkspace != null) {
//            return mWorkspace.getCurrentPage();
//        } else {
//            return 0;
//        }
//    }
//
//    /**
//     * Clear any pending bind callbacks. This is called when is loader is planning to
//     * perform a full rebind from scratch.
//     */
//    @Override
//    public void clearPendingBinds() {
//
//    }
//
//    /**
//     * Refreshes the shortcuts shown on the workspace.
//     * <p>
//     * Implementation of the method from LauncherModel.Callbacks.
//     */
//    public void startBinding() {
//        // Floating panels (except the full widget sheet) are associated with individual icons. If
//        // we are starting a fresh bind, close all such panels as all the icons are about
//        // to go away.
//        AbstractFloatingView.closeOpenViews(this, true,
//                AbstractFloatingView.TYPE_ALL & ~AbstractFloatingView.TYPE_REBIND_SAFE);
//
//        setWorkspaceLoading(true);
//
//        // Clear the workspace because it's going to be rebound
//        mWorkspace.clearDropTargets();
//        mWorkspace.removeAllWorkspaceScreens();
//
//        if (mHotSeat != null) {
//            mHotSeat.resetLayout(mDeviceProfile.isVerticalBarLayout());
//        }
//    }
//
//    @Override
//    public void bindScreens(ArrayList<Long> orderedScreenIds) {
//        // Make sure the first screen is always at the start.
//        if (orderedScreenIds.indexOf(Workspace.FIRST_SCREEN_ID) != 0) {
//            orderedScreenIds.remove(Workspace.FIRST_SCREEN_ID);
//            orderedScreenIds.add(0, Workspace.FIRST_SCREEN_ID);
//
//            Log.d(TAG, "bindScreens-->updateWorkspaceScreenOrder orderedScreenIds=" + orderedScreenIds);
//            HomepageManager.getInstance().updateWorkspaceScreenOrder(orderedScreenIds);
//        }
//        bindAddScreens(orderedScreenIds);
//    }
//
//    private void bindAddScreens(ArrayList<Long> orderedScreenIds) {
//        int count = orderedScreenIds.size();
//        for (int i = 0; i < count; i++) {
//            long screenId = orderedScreenIds.get(i);
//            if (screenId != Workspace.FIRST_SCREEN_ID) {
//                // No need to bind the first screen, as its always bound.
//                mWorkspace.insertNewWorkspaceScreenBeforeEmptyScreen(screenId);
//            }
//        }
//    }
//
//    @Override
//    public void bindAppsAdded(ArrayList<Long> newScreens, ArrayList<ItemInfo> addNotAnimated,
//                              ArrayList<ItemInfo> addAnimated) {
//        // Add the new screens
//        if (newScreens != null) {
//            bindAddScreens(newScreens);
//        }
//
//        // We add the items without animation on non-visible pages, and with
//        // animations on the new page (which we will try and snap to).
//        if (addNotAnimated != null && !addNotAnimated.isEmpty()) {
//            bindItems(addNotAnimated, false);
//        }
//        if (addAnimated != null && !addAnimated.isEmpty()) {
//            bindItems(addAnimated, true);
//        }
//
//        // Remove the extra empty screen
//        mWorkspace.removeExtraEmptyScreen(false, false);
//    }
//
//    /**
//     * Bind the items start-end from the list.
//     * <p>
//     * Implementation of the method from LauncherModel.Callbacks.
//     */
//    @Override
//    public void bindItems(final List<ItemInfo> items, final boolean forceAnimateIcons) {
//        // Get the list of added items and intersect them with the set of items here
//        final AnimatorSet anim = LauncherAnimUtils.createAnimatorSet();
//        final Collection<Animator> bounceAnims = new ArrayList<>();
//        final boolean animateIcons = forceAnimateIcons && canRunNewAppsAnimation();
//        Workspace workspace = mWorkspace;
//        long newItemsScreenId = -1;
//        int end = items.size();
//        for (int i = 0; i < end; i++) {
//            final ItemInfo item = items.get(i);
//
//            // Short circuit if we are loading dock items for a configuration which has no dock
//            if (item.container == ItemInfo.CONTAINER_HOTSEAT &&
//                    mHotSeat == null) {
//                continue;
//            }
//
//            final View view;
//            switch (item.itemType) {
//                case ItemInfo.ITEM_TYPE_APPLICATION: {
//                    ShortcutInfo info = (ShortcutInfo) item;
//                    view = createShortcut(info);
//                    break;
//                }
//                case ItemInfo.ITEM_TYPE_FOLDER: {
//                    view = FolderIcon.fromXml(R.layout.folder_icon, this,
//                            (ViewGroup) workspace.getChildAt(workspace.getCurrentPage()),
//                            (FolderInfo) item);
//                    break;
//                }
//                default:
//                    throw new RuntimeException("Invalid Item Type");
//            }
//
//            /*
//             * Remove colliding items.
//             */
//            if (item.container == ItemInfo.CONTAINER_DESKTOP) {
//                CellLayout cl = mWorkspace.getScreenWithId(item.screenId);
//                if (cl != null && cl.isOccupied(item.cellX, item.cellY)) {
//                    View v = cl.getChildAt(item.cellX, item.cellY);
//                    Object tag = v.getTag();
//                    String desc = "Collision while binding workspace item: " + item
//                            + ". Collides with " + tag;
//                    Log.d(TAG, desc);
//                    HomepageManager.getInstance().deleteItemFromDatabase(item);
//                    continue;
//                }
//            }
//            workspace.addInScreenFromBind(view, item);
//            if (animateIcons) {
//                // Animate all the applications up now
//                view.setAlpha(0f);
//                view.setScaleX(0f);
//                view.setScaleY(0f);
//                bounceAnims.add(createNewAppBounceAnimation(view, i));
//                newItemsScreenId = item.screenId;
//            }
//        }
//
//        if (animateIcons) {
//            // Animate to the correct page
//            if (newItemsScreenId > -1) {
//                long currentScreenId = mWorkspace.getScreenIdForPageIndex(mWorkspace.getNextPage());
//                final int newScreenIndex = mWorkspace.getPageIndexForScreenId(newItemsScreenId);
//                final Runnable startBounceAnimRunnable = new Runnable() {
//                    public void run() {
//                        anim.playTogether(bounceAnims);
//                        anim.start();
//                    }
//                };
//                if (newItemsScreenId != currentScreenId) {
//                    // We post the animation slightly delayed to prevent slowdowns
//                    // when we are loading right after we return to launcher.
//                    mWorkspace.postDelayed(new Runnable() {
//                        public void run() {
//                            if (mWorkspace != null) {
//                                AbstractFloatingView.closeAllOpenViews(Launcher_bak.this, false);
//
//                                mWorkspace.snapToPage(newScreenIndex);
//                                mWorkspace.postDelayed(startBounceAnimRunnable,
//                                        NEW_APPS_ANIMATION_DELAY);
//                            }
//                        }
//                    }, NEW_APPS_PAGE_MOVE_DELAY);
//                } else {
//                    mWorkspace.postDelayed(startBounceAnimRunnable, NEW_APPS_ANIMATION_DELAY);
//                }
//            }
//        }
//        workspace.requestLayout();
//    }
//
//    public void onPageBoundSynchronously(int page) {
//        mSynchronouslyBoundPage = page;
//    }
//
////    @Override
////    public void executeOnNextDraw(ViewOnDrawExecutor executor) {
////        executor.attachTo(this);
////    }
////
////    @Override
////    public void finishFirstPageBind(final ViewOnDrawExecutor executor) {
////        AlphaProperty property = mDragLayer.getAlphaProperty(ALPHA_INDEX_LAUNCHER_LOAD);
////        if (property.getValue() < 1) {
////            ObjectAnimator anim = ObjectAnimator.ofFloat(property, MultiValueAlpha.VALUE, 1);
////            if (executor != null) {
////                anim.addListener(new AnimatorListenerAdapter() {
////                    @Override
////                    public void onAnimationEnd(Animator animation) {
////                        executor.onLoadAnimationCompleted();
////                    }
////                });
////            }
////            anim.start();
////        } else if (executor != null) {
////            executor.onLoadAnimationCompleted();
////        }
////    }
//
//    /**
//     * Callback saying that there aren't any more items to bind.
//     * <p>
//     * Implementation of the method from LauncherModel.Callbacks.
//     */
//    @Override
//    public void finishBindingItems() {
//        mWorkspace.restoreInstanceStateForRemainingPages();
//
//        setWorkspaceLoading(false);
//    }
//
//    private boolean canRunNewAppsAnimation() {
//        long diff = System.currentTimeMillis() - mDragController.getLastGestureUpTime();
//        return diff > (NEW_APPS_ANIMATION_INACTIVE_TIMEOUT_SECONDS * 1000);
//    }
//
//    private ValueAnimator createNewAppBounceAnimation(View v, int i) {
//        ValueAnimator bounceAnim = LauncherAnimUtils.ofViewAlphaAndScale(v, 1, 1, 1);
//        bounceAnim.setDuration(NEW_SHORTCUT_BOUNCE_DURATION);
//        bounceAnim.setStartDelay(i * NEW_SHORTCUT_STAGGER_DELAY);
//        bounceAnim.setInterpolator(new OvershootInterpolator(BOUNCE_ANIMATION_TENSION));
//        return bounceAnim;
//    }
//
//
//    /**
//     * Some shortcuts were updated in the background.
//     * Implementation of the method from LauncherModel.Callbacks.
//     *
//     * @param updated list of shortcuts which have changed.
//     */
//    @Override
//    public void bindShortcutsChanged(ArrayList<ShortcutInfo> updated, final UserHandle user) {
//        if (!updated.isEmpty()) {
//            mWorkspace.updateShortcuts(updated);
//        }
//    }
//
//    @Override
//    public boolean onKeyUp(int keyCode, KeyEvent event) {
//        if (keyCode == KeyEvent.KEYCODE_MENU) {
//            // KEYCODE_MENU is sent by some tests, for example
//            // LauncherJankTests#testWidgetsContainerFling. Don't just remove its handling.
//            if (!mDragController.isDragging() && !mWorkspace.isSwitchingState() &&
//                    isInState(NORMAL)) {
//                // Close any open floating views.
//                AbstractFloatingView.closeAllOpenViews(this);
//
//                // Setting the touch point to (-1, -1) will show the options popup in the center of
//                // the screen.
//                OptionsPopupView.showDefaultOptions(this, -1, -1);
//            }
//            return true;
//        }
//        return super.onKeyUp(keyCode, event);
//    }
//
//    public static Launcher_bak getLauncher(Context context) {
//        if (context instanceof Launcher_bak) {
//            return (Launcher_bak) context;
//        }
//        return ((Launcher_bak) ((ContextWrapper) context).getBaseContext());
//    }
//
//    /**
//     * Callback for listening for onResume
//     */
//    public interface OnResumeCallback {
//
//        void onLauncherResume();
//    }
//}
