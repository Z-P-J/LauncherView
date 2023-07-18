/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3;

import static android.content.pm.ActivityInfo.CONFIG_ORIENTATION;
import static android.content.pm.ActivityInfo.CONFIG_SCREEN_SIZE;
import static com.android.launcher3.LauncherState.ALL_APPS;
import static com.android.launcher3.LauncherState.NORMAL;
import static com.android.launcher3.util.SystemUiController.UI_STATE_OVERVIEW;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.UserHandle;
import android.text.method.TextKeyListener;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.android.launcher3.database.HomepageManager;
import com.android.launcher3.dragndrop.DragController;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.keyboard.ViewGroupFocusHelper;
import com.android.launcher3.popup.OptionItem;
import com.android.launcher3.states.RotationHelper;
import com.android.launcher3.touch.ItemClickHandler;
import com.android.launcher3.uioverrides.DisplayRotationListener;
import com.android.launcher3.util.SystemUiController;
import com.android.launcher3.util.Themes;
import com.android.launcher3.util.Thunk;
import com.android.launcher3.util.LauncherAnimUtils;
import com.zpj.utils.Callback;
import com.zpj.utils.ContextUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Default launcher application.
 */
public class LauncherLayout extends FrameLayout implements LauncherLoader.Callbacks {
    public static final String TAG = "Launcher";
    static final boolean LOGD = false;

    public static final int NEW_SHORTCUT_BOUNCE_DURATION = 450;
    public static final int NEW_SHORTCUT_STAGGER_DELAY = 85;

    private static final float BOUNCE_ANIMATION_TENSION = 1.3f;

    // Type: int
    private static final String RUNTIME_STATE_CURRENT_SCREEN = "launcher.current_screen";
    // Type: int
    private static final String RUNTIME_STATE = "launcher.state";
    // Type: ActivityResultInfo
    private static final String RUNTIME_STATE_PENDING_ACTIVITY_RESULT = "launcher.activity_result";
    // Type: SparseArray<Parcelable>
    private static final String RUNTIME_STATE_WIDGET_PANEL = "launcher.widget_panel";

    private final RotationHelper mRotationHelper;

    private LauncherStateManager mStateManager;

    private static final int ON_ACTIVITY_RESULT_ANIMATION_DELAY = 500;

    // How long to wait before the new-shortcut animation automatically pans the workspace
    private static final int NEW_APPS_PAGE_MOVE_DELAY = 500;
    private static final int NEW_APPS_ANIMATION_INACTIVE_TIMEOUT_SECONDS = 5;
    @Thunk
    static final int NEW_APPS_ANIMATION_DELAY = 500;

    private Configuration mOldConfig;

    private final float mTouchSlop;

    @Thunk
    Workspace mWorkspace;
    private View mLauncherView;
    @Thunk
    DragLayer mDragLayer;
    private DragController mDragController;

    private final int[] mTmpAddItemCellCoordinates = new int[2];

    @Thunk
    HotSeat mHotSeat;

    private DropTargetBar mDropTargetBar;

    // UI and state for the overview panel
    private View mOverviewPanel;

    @Thunk
    boolean mWorkspaceLoading = true;

    private int mSynchronouslyBoundPage = PagedView.INVALID_PAGE;

    public ViewGroupFocusHelper mFocusHandler;

    private DisplayRotationListener mRotationListener;

    protected SystemUiController mSystemUiController;

    private LauncherLoader loader;

    private boolean mIsMoved;
    private int mMoveDirection = 0;
    private boolean mIsDragging;
    private float mDownX;
    private float mDownY;
    private SlideListener mSlideListener;

    public interface SlideListener {

        void onSlideStart(int direction);

        void onSlideVertical(float dy, int direction);

        void onSlideEnd();

        default boolean canHandleLongPress() {
            return true;
        }

        default boolean canStartDrag() {
            return true;
        }
    }

    public LauncherLayout(@NonNull Context context) {
        this(context, null);
    }

    public LauncherLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LauncherLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LauncherManager.init(this);

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        mRotationListener = new DisplayRotationListener(getContext(), this::onDeviceRotationChanged);

        LauncherAppState app = LauncherAppState.getInstance(getContext());
        initDeviceProfile(app.getInvariantDeviceProfile());

        mRotationHelper = new RotationHelper((Activity) context);

        mStateManager = new LauncherStateManager(this);

        mLauncherView = LayoutInflater.from(context).inflate(R.layout.launcher, null);
        addView(mLauncherView);

        mDragLayer = findViewById(R.id.drag_layer);
        mDragController = new DragController(this);


        mWorkspace = mDragLayer.findViewById(R.id.workspace);
        mWorkspace.init(this);


        mFocusHandler = mDragLayer.getFocusIndicatorHelper();

        mWorkspace.initParentViews(mDragLayer);
        mOverviewPanel = findViewById(R.id.overview_panel);
        mHotSeat = findViewById(R.id.hotseat);

        mLauncherView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        // Setup the drag layer
        mDragLayer.setup(mDragController, mWorkspace);

        mWorkspace.setup(mDragController);
        // Until the workspace is bound, ensure that we keep the wallpaper offset locked to the
        // default state, otherwise we will update to the wrong offsets in RTL
        mWorkspace.bindAndInitFirstWorkspaceScreen(null /* recycled qsb */);
        mDragController.addDragListener(mWorkspace);

        // Get the search/delete/uninstall bar
        mDropTargetBar = mDragLayer.findViewById(R.id.drop_target_bar);

        // Setup the drag controller (drop targets have to be added in reverse order in priority)
        mDragController.setMoveTarget(mWorkspace);
        mDropTargetBar.setup(mDragController);

        mStateManager.goToState(NORMAL, false /* animated */);


        getRootView().dispatchInsets();

    }

    public void setSlideListener(SlideListener listener) {
        mSlideListener = listener;
    }

    public boolean canHandleLongPress() {
        return mSlideListener != null && mSlideListener.canHandleLongPress();
    }

    public boolean canStartDrag() {
        // We prevent dragging when we are loading the workspace as it is possible to pick up a view
        // that is subsequently removed from the workspace in startBinding().
        if (isWorkspaceLocked()) {
            return false;
        }
        // Return early if an item is already being dragged (e.g. when long-pressing two shortcuts)
        if (getDragController().isDragging()) {
            return false;
        }
        return mSlideListener != null && mSlideListener.canStartDrag();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownX = event.getRawX();
                mDownY = event.getRawY();
                mIsMoved = false;
                mMoveDirection = 0;
                mIsDragging = mDragController.isDragging() || AbstractFloatingView.getTopOpenView() != null;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mIsDragging) {
                    break;
                }
                if (AbstractFloatingView.getTopOpenView() != null) {
                    mIsDragging = true;
                    break;
                }
                if (mDragController.isDragging()) {
                    mIsDragging = true;
                    break;
                }
                float dx = event.getRawX() - mDownX;
                float dy = event.getRawY() - mDownY;
                if (!mIsMoved) {
                    if (Math.abs(dy) > mTouchSlop && Math.abs(dy) > Math.abs(dx)) {
                        mIsMoved = true;
                        mMoveDirection = dy > 0 ? -1 : 1;
                        MotionEvent e = MotionEvent.obtain(event);
                        e.setAction(MotionEvent.ACTION_CANCEL);
                        super.dispatchTouchEvent(e);
                        if (mSlideListener != null) {
                            mSlideListener.onSlideStart(mMoveDirection);
                        }
                    }
                }
                if (mIsMoved) {
                    if (mSlideListener != null) {
                        mSlideListener.onSlideVertical(dy, mMoveDirection);
                    }
                    return true;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (mIsDragging) {
                    mIsDragging = false;
                    break;
                }
                if (mIsMoved) {
                    if (mSlideListener != null) {
                        mSlideListener.onSlideEnd();
                    }
                    MotionEvent e = MotionEvent.obtain(event);
                    e.setAction(MotionEvent.ACTION_CANCEL);
                    super.dispatchTouchEvent(e);
                    return true;
                }
                break;
        }
        boolean result = super.dispatchTouchEvent(event);
        Log.d(TAG, "dispatchTouchEvent event=" + MotionEvent.actionToString(event.getAction()) + " result=" + result);
        return result;
    }

    public void init(Bundle savedInstanceState) {
        mOldConfig = new Configuration(getResources().getConfiguration());

        restoreState(savedInstanceState);

        // We only load the page synchronously if the user rotates (or triggers a
        // configuration change) while launcher is in the foreground
        int currentScreen = PagedView.INVALID_RESTORE_PAGE;
        if (savedInstanceState != null) {
            currentScreen = savedInstanceState.getInt(RUNTIME_STATE_CURRENT_SCREEN, currentScreen);
        }

        mWorkspace.setCurrentPage(currentScreen);
        setWorkspaceLoading(true);

        LauncherAppState app = LauncherAppState.getInstance(getContext());
        loader = new LauncherLoader(getContext(), app);
        loader.initialize(this);
        loader.startLoader(currentScreen);

        mRotationHelper.initialize();

        getSystemUiController().updateUiState(SystemUiController.UI_STATE_BASE_WINDOW,
                Themes.getAttrBoolean(getContext(), R.attr.isWorkspaceDarkText));
    }

    public void onSaveInstanceState(Bundle outState) {
        if (mWorkspace.getChildCount() > 0) {
            outState.putInt(RUNTIME_STATE_CURRENT_SCREEN, mWorkspace.getNextPage());

        }
        outState.putInt(RUNTIME_STATE, mStateManager.getState().ordinal);

        AbstractFloatingView widgets = AbstractFloatingView
                .getOpenView(AbstractFloatingView.TYPE_WIDGETS_FULL_SHEET);
        if (widgets != null) {
            SparseArray<Parcelable> widgetsState = new SparseArray<>();
            widgets.saveHierarchyState(widgetsState);
            outState.putSparseParcelableArray(RUNTIME_STATE_WIDGET_PANEL, widgetsState);
        } else {
            outState.remove(RUNTIME_STATE_WIDGET_PANEL);
        }

        // We close any open folders and shortcut containers since they will not be re-opened,
        // and we need to make sure this state is reflected.
        AbstractFloatingView.closeAllOpenViews(false);
    }

    public void onPause() {
        mDragController.cancelDrag();
        mDragController.resetLastGestureUpTime();
        // Reset the overridden sysui flags used for the task-swipe launch animation, we do this
        // here instead of at the end of the animation because the start of the new activity does
        // not happen immediately, which would cause us to reset to launcher's sysui flags and then
        // back to the new app (causing a flash)
        getSystemUiController().updateUiState(UI_STATE_OVERVIEW, 0);
    }

    public void onDestroy() {
        TextKeyListener.getInstance().release();
        LauncherAnimUtils.onDestroyActivity();
        mRotationListener.disable();
        mRotationHelper.destroy();
        LauncherManager.destroy();
    }


    public void rebindWorkspace(InvariantDeviceProfile idp) {
        idp.update(getContext());
        initDeviceProfile(idp);
        loader.startLoader(mWorkspace.getCurrentPage());
    }

    @Override
    public void rebindModel() {
//        int currentPage = mWorkspace.getNextPage();
//        if (mModel.startLoader(currentPage)) {
//            mWorkspace.setCurrentPage(currentPage);
//            setWorkspaceLoading(true);
//        }
    }

    public LauncherStateManager getStateManager() {
        return mStateManager;
    }

    public boolean isDraggingEnabled() {
        // We prevent dragging when we are loading the workspace as it is possible to pick up a view
        // that is subsequently removed from the workspace in startBinding().
        return !isWorkspaceLoading();
    }

    public int getViewIdForItem(ItemInfo info) {
        // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
        // This cast is safe as long as the id < 0x00FFFFFF
        // Since we jail all the dynamically generated views, there should be no clashes
        // with any other views.
        return (int) info.id;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        mStateManager.onWindowFocusChanged();
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

    public boolean isInState(LauncherState state) {
        return mStateManager.getState() == state;
    }

    /**
     * Restores the previous state, if it exists.
     *
     * @param savedState The previous state.
     */
    private void restoreState(Bundle savedState) {
        if (savedState == null) {
            return;
        }

        int stateOrdinal = savedState.getInt(RUNTIME_STATE, NORMAL.ordinal);
        LauncherState[] stateValues = LauncherState.values();
        LauncherState state = stateValues[stateOrdinal];
        if (!state.disableRestore) {
            mStateManager.goToState(state, false /* animated */);
        }
    }

    /**
     * Creates a view representing a shortcut.
     *
     * @param info The data structure describing the shortcut.
     */
    View createShortcut(ShortcutInfo info) {
        return createShortcut((ViewGroup) mWorkspace.getChildAt(mWorkspace.getCurrentPage()), info);
    }

    /**
     * Creates a view representing a shortcut inflated from the specified resource.
     *
     * @param parent The group the shortcut belongs to.
     * @param info   The data structure describing the shortcut.
     * @return A View inflated from layoutResId.
     */
    public View createShortcut(ViewGroup parent, ShortcutInfo info) {
        BubbleTextView favorite = (BubbleTextView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.app_icon, parent, false);
        favorite.applyFromShortcutInfo(info);
        favorite.setOnClickListener(ItemClickHandler.INSTANCE);
        favorite.setOnFocusChangeListener(mFocusHandler);
        return favorite;
    }

    public View createWidget(ViewGroup parent, ShortcutInfo info) {
        BubbleTextView favorite = (BubbleTextView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.app_icon, parent, false);
        favorite.applyFromShortcutInfo(info);
        favorite.setOnClickListener(ItemClickHandler.INSTANCE);
        favorite.setOnFocusChangeListener(mFocusHandler);
        return favorite;
    }

//    public FolderIcon findFolderIcon(final long folderIconId) {
//        return (FolderIcon) mWorkspace.getFirstMatch(new ItemOperator() {
//            @Override
//            public boolean evaluate(ItemInfo info, View view) {
//                return info != null && info.id == folderIconId;
//            }
//        });
//    }


    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mWorkspace.removeFolderListeners();
    }

    @Override
    public LauncherRootView getRootView() {
        return (LauncherRootView) mLauncherView;
    }

    public DragLayer getDragLayer() {
        return mDragLayer;
    }

    public Workspace getWorkspace() {
        return mWorkspace;
    }

    public HotSeat getHotseat() {
        return mHotSeat;
    }

    public <T extends View> T getOverviewPanel() {
        return (T) mOverviewPanel;
    }

    public DropTargetBar getDropTargetBar() {
        return mDropTargetBar;
    }

    public int getOrientation() {
        return mOldConfig.orientation;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);
        mWorkspace.restoreInstanceStateForChild(mSynchronouslyBoundPage);
    }

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        return super.onSaveInstanceState();
    }


    public boolean onBackPressed() {
        if (mDragController.isDragging()) {
            mDragController.cancelDrag();
            return true;
        }

        // Note: There should be at most one log per method call. This is enforced implicitly
        // by using if-else statements.
        AbstractFloatingView topView = AbstractFloatingView.getTopOpenView();
        Log.d(TAG, "topView=" + topView + " isInState(ALL_APPS)=" + isInState(ALL_APPS));
        if (topView != null && topView.onBackPressed()) {
            // Handled by the floating view.
            return true;
        } else if (!isInState(NORMAL)) {
            mStateManager.goToState(NORMAL);
            return true;
        } else {
            // Back button is a no-op here, but give at least some feedback for the button press
            mWorkspace.showOutlinesTemporarily();
        }
        return false;
    }


    public DragController getDragController() {
        return mDragController;
    }

    public boolean isWorkspaceLocked() {
        return mWorkspaceLoading;
    }

    public boolean isWorkspaceLoading() {
        return mWorkspaceLoading;
    }

    private void setWorkspaceLoading(boolean value) {
        mWorkspaceLoading = value;
    }

    FolderIcon addFolder(CellLayout layout, long container, final long screenId, int cellX,
                         int cellY) {
        final FolderInfo folderInfo = new FolderInfo();
        folderInfo.title = getContext().getText(R.string.folder_name);

        // Update the model
        HomepageManager.getInstance().addItemToDatabase(folderInfo, container, screenId, cellX, cellY);

        // Create the view
        FolderIcon newFolder = FolderIcon.fromXml(R.layout.folder_icon, this, layout, folderInfo);
        mWorkspace.addInScreen(newFolder, folderInfo);
        // Force measure the new folder icon
        CellLayout parent = mWorkspace.getParentCellLayoutForView(newFolder);
        parent.getShortcutsAndWidgets().measureChild(newFolder);
        return newFolder;
    }

    public void addPendingItem(TabItemInfo info, long container, long screenId,
                               int[] cell, int spanX, int spanY) {
        info.container = container;
        info.screenId = screenId;
        if (cell != null) {
            info.cellX = cell[0];
            info.cellY = cell[1];
        }
        info.spanX = spanX;
        info.spanY = spanY;

        addAppWidgetFromDrop(info);
    }

    /**
     * Process a widget drop.
     */
    private void addAppWidgetFromDrop(TabItemInfo info) {
        View hostView = null;
        final long appWidgetId = System.currentTimeMillis();
        if (hostView != null) {
            // In the case where we've prebound the widget, we remove it from the DragLayer
            if (LOGD) {
                Log.d(TAG, "Removing widget view from drag layer and setting boundWidget to null");
            }
            getDragLayer().removeView(hostView);

            addAppWidgetImpl(appWidgetId, info, hostView);
        } else {
            addAppWidgetImpl(appWidgetId, info, createTabItemView(info));
        }
    }

    public boolean addTabItem(long appWidgetId, String title, String url) {
        TabItemInfo tabItemInfo = new TabItemInfo();
        tabItemInfo.container = ItemInfo.CONTAINER_DESKTOP;
        tabItemInfo.screenId = 0;
        tabItemInfo.itemType = ItemInfo.ITEM_TYPE_WIDGET;
        tabItemInfo.spanX = 1;
        tabItemInfo.spanY = 1;
        tabItemInfo.title = title;
        tabItemInfo.url = url;
        tabItemInfo.id = ItemInfo.NO_ID;
//        addAppWidgetImpl(appWidgetId, tabItemInfo, null);

        Workspace workspace = getWorkspace();
        for (long screenId : workspace.getScreenOrder()) {
            if (addTabItemToScreen(screenId, tabItemInfo)) {
                return true;
            }
        }
        if (workspace.addExtraEmptyScreen()) {
            long screenId = workspace.commitExtraEmptyScreen();
            return addTabItemToScreen(screenId, tabItemInfo);
        }
        return false;
    }

    private boolean addTabItemToScreen(long screenId, TabItemInfo itemInfo) {
        Workspace workspace = getWorkspace();
        CellLayout cellLayout = workspace.getScreenWithId(screenId);
        int countX = cellLayout.getCountX();
        int countY = cellLayout.getCountY();
        Log.d(TAG, "countX=" + countX + " countY=" + countY);
        int[] cellXY = new int[2];
        if (cellLayout.findCellForSpan(cellXY, 1, 1)) {
            itemInfo.screenId = screenId;
            itemInfo.cellX = cellXY[0];
            itemInfo.cellY = cellXY[1];

            Log.d(TAG, "addItemToDatabase cellx=" + cellXY[0] + " cellY=" + cellXY[1] + " url=" + itemInfo.url);
            View child = createTabItemView(itemInfo);
            addAppWidgetImpl(itemInfo.tabId, itemInfo, child);
            return true;
        }
        return false;
    }

    void addAppWidgetImpl(long appWidgetId, TabItemInfo info,
                          View boundWidget) {
        Runnable onComplete = new Runnable() {
            @Override
            public void run() {
                // Exit spring loaded mode if necessary after adding the widget
                mStateManager.goToState(NORMAL);
            }
        };
        completeAddAppWidget(appWidgetId, info, boundWidget);
        mWorkspace.removeExtraEmptyScreenDelayed(true, onComplete, 0, false);
    }

    private View createTabItemView(TabItemInfo itemInfo) {
        View card = LayoutInflater.from(getContext()).inflate(R.layout.item_card_tab, null, false);
        card.setVisibility(View.VISIBLE);
        card.setTag(itemInfo);

        TextView textView = card.findViewById(R.id.tv_name);
        textView.setText(itemInfo.title);

        ImageView ivIcon = card.findViewById(R.id.iv_icon);
        if (mItemLoader != null) {
            mItemLoader.loadIcon(itemInfo, ivIcon::setImageBitmap);
        }

        card.setOnClickListener(v -> {
            ClickHandler clickHandler = LauncherManager.getLauncherLayout().getClickHandler();
            if (clickHandler != null) {
                clickHandler.onClickTabCard(v, itemInfo);
            }
        });

        return card;
    }

    void completeAddAppWidget(long appWidgetId, TabItemInfo itemInfo, View hostView) {
        HomepageManager.getInstance().addItemToDatabase(itemInfo,
                itemInfo.container, itemInfo.screenId, itemInfo.cellX, itemInfo.cellY);

        hostView.setVisibility(View.VISIBLE);
        hostView.setTag(itemInfo);
        itemInfo.tabId = appWidgetId;
        mWorkspace.addInScreen(hostView, itemInfo);
    }

    /**
     * Unbinds the view for the specified item, and removes the item and all its children.
     *
     * @param v            the view being removed.
     * @param itemInfo     the {@link ItemInfo} for this view.
     * @param deleteFromDb whether or not to delete this item from the db.
     */
    public boolean removeItem(View v, final ItemInfo itemInfo, boolean deleteFromDb) {
        if (itemInfo instanceof TabItemInfo) {
            mWorkspace.removeWorkspaceItem(v);
            if (deleteFromDb) {
                HomepageManager.getInstance().deleteItemFromDatabase(itemInfo);
            }
        } else if (itemInfo instanceof ShortcutInfo) {
            // Remove the shortcut from the folder before removing it from launcher
            View folderIcon = mWorkspace.getHomescreenIconByItemId(itemInfo.container);
            if (folderIcon instanceof FolderIcon) {
                ((FolderInfo) folderIcon.getTag()).remove((ShortcutInfo) itemInfo, true);
            } else {
                mWorkspace.removeWorkspaceItem(v);
            }
            if (deleteFromDb) {
                HomepageManager.getInstance().deleteItemFromDatabase(itemInfo);
            }
        } else if (itemInfo instanceof FolderInfo) {
            final FolderInfo folderInfo = (FolderInfo) itemInfo;
            if (v instanceof FolderIcon) {
                ((FolderIcon) v).removeListeners();
            }
            mWorkspace.removeWorkspaceItem(v);
            if (deleteFromDb) {
                HomepageManager.getInstance().deleteFolderAndContentsFromDatabase(folderInfo);
            }
        } else {
            return false;
        }
        return true;
    }

    public boolean removeItem(String url, boolean deleteFromDb) {
        View iconView = mWorkspace.getIconByUrl(url);
        if (iconView == null) {
            return false;
        } else {
            ItemInfo itemInfo = (ItemInfo) iconView.getTag();
            View folderIcon = mWorkspace.getHomescreenIconByItemId(itemInfo.container);
            if (folderIcon instanceof FolderIcon) {
                ((FolderInfo) folderIcon.getTag()).remove((ShortcutInfo) itemInfo, true);
            } else {
                mWorkspace.removeWorkspaceItem(iconView);
            }
            if (deleteFromDb) {
                HomepageManager.getInstance().deleteItemFromDatabase(itemInfo);
            }
        }
        mWorkspace.stripEmptyScreens();
        return true;
    }

    public boolean addItem(String title, String url) {
        ShortcutInfo shortcutInfo = new ShortcutInfo();
        shortcutInfo.url = url;
        shortcutInfo.title = title;
        shortcutInfo.itemType = ItemInfo.ITEM_TYPE_APPLICATION;
        shortcutInfo.container = ItemInfo.CONTAINER_DESKTOP;
        shortcutInfo.spanX = 1;
        shortcutInfo.spanY = 1;
        shortcutInfo.screenId = 0;

        Workspace workspace = getWorkspace();
        for (long screenId : workspace.getScreenOrder()) {
            if (addToScreen(screenId, shortcutInfo)) {
                return true;
            }
        }
        if (workspace.addExtraEmptyScreen()) {
            long screenId = workspace.commitExtraEmptyScreen();
            return addToScreen(screenId, shortcutInfo);
        }
        return false;
    }

    private boolean addToScreen(long screenId, ShortcutInfo shortcutInfo) {
        Workspace workspace = getWorkspace();
        CellLayout cellLayout = workspace.getScreenWithId(screenId);
        int countX = cellLayout.getCountX();
        int countY = cellLayout.getCountY();
        Log.d(TAG, "countX=" + countX + " countY=" + countY);
        int[] cellXY = new int[2];
        if (cellLayout.findCellForSpan(cellXY, 1, 1)) {
            shortcutInfo.screenId = screenId;
            shortcutInfo.cellX = cellXY[0];
            shortcutInfo.cellY = cellXY[1];

            HomepageManager.getInstance().addItemToDatabase(shortcutInfo,
                    ItemInfo.CONTAINER_DESKTOP,
                    screenId,
                    cellXY[0],
                    cellXY[1]);
            Log.d(TAG, "addItemToDatabase cellx=" + cellXY[0] + " cellY=" + cellXY[1] + " url=" + shortcutInfo.url);
            View child = createShortcut((ViewGroup) workspace.getChildAt(workspace.getPageIndexForScreenId(screenId)), shortcutInfo);
            workspace.addInScreen(child, shortcutInfo);
            return true;
        }
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Log.d(TAG, "keyEvent=" + event);
        return (event.getKeyCode() == KeyEvent.KEYCODE_HOME) || super.dispatchKeyEvent(event);
    }


    boolean isHotseatLayout(View layout) {
        // TODO: Remove this method
        return mHotSeat != null && layout != null &&
                (layout instanceof CellLayout) && (layout == mHotSeat.getLayout());
    }

    /**
     * Returns the CellLayout of the specified container at the specified screen.
     */
    public CellLayout getCellLayout(long container, long screenId) {
        if (container == ItemInfo.CONTAINER_HOTSEAT) {
            if (mHotSeat != null) {
                return mHotSeat.getLayout();
            } else {
                return null;
            }
        } else {
            return mWorkspace.getScreenWithId(screenId);
        }
    }

    /**
     * Implementation of the method from LauncherModel.Callbacks.
     */
    @Override
    public int getCurrentWorkspaceScreen() {
        if (mWorkspace != null) {
            return mWorkspace.getCurrentPage();
        } else {
            return 0;
        }
    }

    /**
     * Refreshes the shortcuts shown on the workspace.
     * <p>
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void startBinding() {
        // Floating panels (except the full widget sheet) are associated with individual icons. If
        // we are starting a fresh bind, close all such panels as all the icons are about
        // to go away.
        AbstractFloatingView.closeOpenViews(true,
                AbstractFloatingView.TYPE_ALL & ~AbstractFloatingView.TYPE_REBIND_SAFE);

        setWorkspaceLoading(true);

        // Clear the workspace because it's going to be rebound
        mWorkspace.clearDropTargets();
        mWorkspace.removeAllWorkspaceScreens();

        if (mHotSeat != null) {
            mHotSeat.resetLayout(getDeviceProfile().isVerticalBarLayout());
        }
    }

    @Override
    public void bindScreens(ArrayList<Long> orderedScreenIds) {
        // Make sure the first screen is always at the start.
        if (orderedScreenIds.indexOf(Workspace.FIRST_SCREEN_ID) != 0) {
            orderedScreenIds.remove(Workspace.FIRST_SCREEN_ID);
            orderedScreenIds.add(0, Workspace.FIRST_SCREEN_ID);

            Log.d(TAG, "bindScreens-->updateWorkspaceScreenOrder orderedScreenIds=" + orderedScreenIds);
            HomepageManager.getInstance().updateWorkspaceScreenOrder(orderedScreenIds);
        }
        bindAddScreens(orderedScreenIds);
    }

    private void bindAddScreens(ArrayList<Long> orderedScreenIds) {
        int count = orderedScreenIds.size();
        for (int i = 0; i < count; i++) {
            long screenId = orderedScreenIds.get(i);
            if (screenId != Workspace.FIRST_SCREEN_ID) {
                // No need to bind the first screen, as its always bound.
                mWorkspace.insertNewWorkspaceScreenBeforeEmptyScreen(screenId);
            }
        }
    }

    @Override
    public void bindAppsAdded(ArrayList<Long> newScreens, ArrayList<ItemInfo> addNotAnimated,
                              ArrayList<ItemInfo> addAnimated) {
        // Add the new screens
        if (newScreens != null) {
            bindAddScreens(newScreens);
        }

        // We add the items without animation on non-visible pages, and with
        // animations on the new page (which we will try and snap to).
        if (addNotAnimated != null && !addNotAnimated.isEmpty()) {
            bindItems(addNotAnimated, false);
        }
        if (addAnimated != null && !addAnimated.isEmpty()) {
            bindItems(addAnimated, true);
        }

        // Remove the extra empty screen
        mWorkspace.removeExtraEmptyScreen(false, false);
    }

    /**
     * Bind the items start-end from the list.
     * <p>
     * Implementation of the method from LauncherModel.Callbacks.
     */
    @Override
    public void bindItems(final List<ItemInfo> items, final boolean forceAnimateIcons) {
        // Get the list of added items and intersect them with the set of items here
        final AnimatorSet anim = LauncherAnimUtils.createAnimatorSet();
        final Collection<Animator> bounceAnims = new ArrayList<>();
        final boolean animateIcons = forceAnimateIcons && canRunNewAppsAnimation();
        Workspace workspace = mWorkspace;
        long newItemsScreenId = -1;
        int end = items.size();
        for (int i = 0; i < end; i++) {
            final ItemInfo item = items.get(i);

            // Short circuit if we are loading dock items for a configuration which has no dock
            if (item.container == ItemInfo.CONTAINER_HOTSEAT &&
                    mHotSeat == null) {
                continue;
            }

            final View view;
            switch (item.itemType) {
                case ItemInfo.ITEM_TYPE_APPLICATION: {
                    ShortcutInfo info = (ShortcutInfo) item;
                    view = createShortcut(info);
                    break;
                }
                case ItemInfo.ITEM_TYPE_FOLDER: {
                    view = FolderIcon.fromXml(R.layout.folder_icon, this,
                            (ViewGroup) workspace.getChildAt(workspace.getCurrentPage()),
                            (FolderInfo) item);
                    break;
                }
                case ItemInfo.ITEM_TYPE_WIDGET: {
                    TabItemInfo tabItemInfo = (TabItemInfo) item;
//                    view = LayoutInflater.from(getContext()).inflate(R.layout.item_tab_card, null, false);
//                    view.setTag(item);

                    view = createTabItemView(tabItemInfo);
                    break;
                }
                default:
                    throw new RuntimeException("Invalid Item Type");
            }

            /*
             * Remove colliding items.
             */
            if (item.container == ItemInfo.CONTAINER_DESKTOP) {
                CellLayout cl = mWorkspace.getScreenWithId(item.screenId);
                if (cl != null && cl.isOccupied(item.cellX, item.cellY)) {
                    View v = cl.getChildAt(item.cellX, item.cellY);
                    Object tag = v.getTag();
                    String desc = "Collision while binding workspace item: " + item
                            + ". Collides with " + tag;
                    Log.d(TAG, desc);
                    HomepageManager.getInstance().deleteItemFromDatabase(item);
                    continue;
                }
            }
            workspace.addInScreenFromBind(view, item);
            if (animateIcons) {
                // Animate all the applications up now
                view.setAlpha(0f);
                view.setScaleX(0f);
                view.setScaleY(0f);
                bounceAnims.add(createNewAppBounceAnimation(view, i));
                newItemsScreenId = item.screenId;
            }
        }

        if (animateIcons) {
            // Animate to the correct page
            if (newItemsScreenId > -1) {
                long currentScreenId = mWorkspace.getScreenIdForPageIndex(mWorkspace.getNextPage());
                final int newScreenIndex = mWorkspace.getPageIndexForScreenId(newItemsScreenId);
                final Runnable startBounceAnimRunnable = new Runnable() {
                    public void run() {
                        anim.playTogether(bounceAnims);
                        anim.start();
                    }
                };
                if (newItemsScreenId != currentScreenId) {
                    // We post the animation slightly delayed to prevent slowdowns
                    // when we are loading right after we return to launcher.
                    mWorkspace.postDelayed(new Runnable() {
                        public void run() {
                            if (mWorkspace != null) {
                                AbstractFloatingView.closeAllOpenViews(false);

                                mWorkspace.snapToPage(newScreenIndex);
                                mWorkspace.postDelayed(startBounceAnimRunnable,
                                        NEW_APPS_ANIMATION_DELAY);
                            }
                        }
                    }, NEW_APPS_PAGE_MOVE_DELAY);
                } else {
                    mWorkspace.postDelayed(startBounceAnimRunnable, NEW_APPS_ANIMATION_DELAY);
                }
            }
        }
        workspace.requestLayout();
    }

    public void onPageBoundSynchronously(int page) {
        mSynchronouslyBoundPage = page;
    }

//    @Override
//    public void executeOnNextDraw(ViewOnDrawExecutor executor) {
//        executor.attachTo(this);
//    }
//
//    @Override
//    public void finishFirstPageBind(final ViewOnDrawExecutor executor) {
//        AlphaProperty property = mDragLayer.getAlphaProperty(ALPHA_INDEX_LAUNCHER_LOAD);
//        if (property.getValue() < 1) {
//            ObjectAnimator anim = ObjectAnimator.ofFloat(property, MultiValueAlpha.VALUE, 1);
//            if (executor != null) {
//                anim.addListener(new AnimatorListenerAdapter() {
//                    @Override
//                    public void onAnimationEnd(Animator animation) {
//                        executor.onLoadAnimationCompleted();
//                    }
//                });
//            }
//            anim.start();
//        } else if (executor != null) {
//            executor.onLoadAnimationCompleted();
//        }
//    }

    /**
     * Callback saying that there aren't any more items to bind.
     * <p>
     * Implementation of the method from LauncherModel.Callbacks.
     */
    @Override
    public void finishBindingItems() {
        mWorkspace.restoreInstanceStateForRemainingPages();

        setWorkspaceLoading(false);
    }

    private boolean canRunNewAppsAnimation() {
        long diff = System.currentTimeMillis() - mDragController.getLastGestureUpTime();
        return diff > (NEW_APPS_ANIMATION_INACTIVE_TIMEOUT_SECONDS * 1000);
    }

    private ValueAnimator createNewAppBounceAnimation(View v, int i) {
        ValueAnimator bounceAnim = LauncherAnimUtils.ofViewAlphaAndScale(v, 1, 1, 1);
        bounceAnim.setDuration(NEW_SHORTCUT_BOUNCE_DURATION);
        bounceAnim.setStartDelay(i * NEW_SHORTCUT_STAGGER_DELAY);
        bounceAnim.setInterpolator(new OvershootInterpolator(BOUNCE_ANIMATION_TENSION));
        return bounceAnim;
    }


    /**
     * Some shortcuts were updated in the background.
     * Implementation of the method from LauncherModel.Callbacks.
     *
     * @param updated list of shortcuts which have changed.
     */
    @Override
    public void bindShortcutsChanged(ArrayList<ShortcutInfo> updated, final UserHandle user) {
        if (!updated.isEmpty()) {
            mWorkspace.updateShortcuts(updated);
        }
    }


    public RotationHelper getRotationHelper() {
        return mRotationHelper;
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        int diff = newConfig.diff(mOldConfig);
        if ((diff & (CONFIG_ORIENTATION | CONFIG_SCREEN_SIZE)) != 0) {
            initDeviceProfile(mDeviceProfile.inv);
            dispatchDeviceProfileChanged();
            reapplyUi();
            onConfigurationChanged();
        }

        mOldConfig.setTo(newConfig);
        super.onConfigurationChanged(newConfig);
    }

    public void onConfigurationChanged() {
        getDragLayer().recreateControllers();
        // TODO: We can probably avoid rebind when only screen size changed.
        rebindModel();
    }

    protected void reapplyUi() {
        getRootView().dispatchInsets();
        mStateManager.reapplyState(true /* cancelCurrentAnimation */);
    }


    private final ArrayList<DeviceProfile.OnDeviceProfileChangeListener> mDPChangeListeners = new ArrayList<>();

    protected DeviceProfile mDeviceProfile;

    public DeviceProfile getDeviceProfile() {
        return mDeviceProfile;
    }

    public void addOnDeviceProfileChangeListener(DeviceProfile.OnDeviceProfileChangeListener listener) {
        mDPChangeListeners.add(listener);
    }

    public void dispatchDeviceProfileChanged() {
        for (int i = mDPChangeListeners.size() - 1; i >= 0; i--) {
            mDPChangeListeners.get(i).onDeviceProfileChanged(mDeviceProfile);
        }
    }

    public Activity getActivity() {
        return ContextUtils.getActivity(getContext());
    }

    public Window getWindow() {
        return getActivity().getWindow();
    }

    public WindowManager getWindowManager() {
        return getActivity().getWindowManager();
    }

    public SystemUiController getSystemUiController() {
        if (mSystemUiController == null) {
            mSystemUiController = new SystemUiController(getWindow());
        }
        return mSystemUiController;
    }

    private void initDeviceProfile(InvariantDeviceProfile idp) {
        // Load configuration-specific DeviceProfile
        mDeviceProfile = idp.getDeviceProfile(getContext());
        if (LauncherManager.isInMultiWindowModeCompat()) {
            Display display = getWindowManager().getDefaultDisplay();
            Point mwSize = new Point();
            display.getSize(mwSize);
            mDeviceProfile = mDeviceProfile.getMultiWindowProfile(getContext(), mwSize);
        }
        onDeviceProfileInitiated();
    }

    protected void onDeviceProfileInitiated() {
        if (mDeviceProfile.isVerticalBarLayout()) {
            mRotationListener.enable();
            mDeviceProfile.updateIsSeascape(getWindowManager());
        } else {
            mRotationListener.disable();
        }
    }

    private void onDeviceRotationChanged() {
        if (mDeviceProfile.updateIsSeascape(getWindowManager())) {
            reapplyUi();
        }
    }

    private ItemLoader mItemLoader;
    private OptionItemProvider mOptionItemProvider;
    private ClickHandler mClickHandler;

    public void setItemLoader(ItemLoader itemLoader) {
        mItemLoader = itemLoader;
    }

    public ItemLoader getItemLoader() {
        return mItemLoader;
    }

    public void setOptionItemProvider(OptionItemProvider provider) {
        mOptionItemProvider = provider;
    }

    public OptionItemProvider getOptionItemProvider() {
        return mOptionItemProvider;
    }

    public void setClickHandler(ClickHandler clickHandler) {
        mClickHandler = clickHandler;
    }

    public ClickHandler getClickHandler() {
        return mClickHandler;
    }

    public interface ClickHandler {
        void onClickAppShortcut(View v, ItemInfoWithIcon itemInfo);

        void onClickTabCard(View v, TabItemInfo itemInfo);
    }

    public interface OptionItemProvider {
        List<OptionItem> createOptions();

        List<OptionItem> createOptions(ItemInfo itemInfo);
    }

    public interface ItemLoader {

        @WorkerThread
        void onFirstRun();

        View onCreateSearchBar(ViewGroup root);

        void loadIcon(ItemInfo itemInfo, Callback<Bitmap> callback);

    }

}
