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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;

import com.android.launcher3.graphics.ColorExtractor;
import com.android.launcher3.graphics.ShadowGenerator;
import com.android.launcher3.util.Thunk;
import com.qianxun.browser.database.HomepageManager;
import com.qianxun.browser.launcher.R;
import com.qianxun.browser.model.FavoriteItem;
import com.qianxun.browser.model.ScreenItem;
import com.qianxun.browser.utils.HomepageUtils;
import com.raizlabs.android.dbflow.sql.language.Delete;
import com.zpj.utils.ContextUtils;
import com.zpj.utils.PrefsHelper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Maintains in-memory state of the Launcher. It is expected that there should be only one
 * LauncherModel object held in a static. Also provide APIs for updating the database state
 * for the Launcher.
 */
public class LauncherLoader {

    static final String TAG = "Launcher.Model";

    private static final int ITEMS_CHUNK = 6; // batch size for the workspace icons

    private final Context mContext;
    @Thunk
    final LauncherAppState mApp;
    @Thunk
    final Object mLock = new Object();

    @Thunk
    WeakReference<Callbacks> mCallbacks;

    LauncherLoader(Context context, LauncherAppState app) {
        mContext = context;
        mApp = app;
    }

    static void checkItemInfoLocked(
            final long itemId, final ItemInfo item, StackTraceElement[] stackTrace) {
        ItemInfo modelItem = HomepageManager.getInstance().itemsIdMap.get(itemId);
        Log.d(TAG, "modelItem=" + modelItem);
        Log.d(TAG, "item=" + item);
        if (modelItem != null && item != modelItem) {
            // check all the data is consistent
            if (modelItem instanceof ShortcutInfo && item instanceof ShortcutInfo) {
                ShortcutInfo modelShortcut = (ShortcutInfo) modelItem;
                ShortcutInfo shortcut = (ShortcutInfo) item;
                if (modelShortcut.title.toString().equals(shortcut.title.toString()) &&
                        modelShortcut.id == shortcut.id &&
                        modelShortcut.itemType == shortcut.itemType &&
                        modelShortcut.container == shortcut.container &&
                        modelShortcut.screenId == shortcut.screenId &&
                        modelShortcut.cellX == shortcut.cellX &&
                        modelShortcut.cellY == shortcut.cellY &&
                        modelShortcut.spanX == shortcut.spanX &&
                        modelShortcut.spanY == shortcut.spanY) {
                    // For all intents and purposes, this is the same object
                    return;
                }
            }

            // the modelItem needs to match up perfectly with item if our model is
            // to be consistent with the database-- for now, just require
            // modelItem == item or the equality check above
            String msg = "item: " + ((item != null) ? item.toString() : "null") +
                    "modelItem: " +
                    ((modelItem != null) ? modelItem.toString() : "null") +
                    "Error: ItemInfo passed to checkItemInfo doesn't match original";
            RuntimeException e = new RuntimeException(msg);
            if (stackTrace != null) {
                e.setStackTrace(stackTrace);
            }
            throw e;
        }
    }

    static void checkItemInfo(final ItemInfo item) {
        final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        final long itemId = item.id;
        checkItemInfoLocked(itemId, item, stackTrace);
    }

    /**
     * Set this as the current Launcher activity object for the loader.
     */
    public void initialize(Callbacks callbacks) {
        synchronized (mLock) {
            mCallbacks = new WeakReference<>(callbacks);
        }
    }

    public boolean isCurrentCallbacks(Callbacks callbacks) {
        return (mCallbacks != null && mCallbacks.get() == callbacks);
    }

    private final AtomicBoolean isLoaded = new AtomicBoolean(false);


    private void loadWorkspace() {

        HomepageManager.getInstance().clear();

        Log.d(TAG, "loadWorkspace");


        final Bitmap icon = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.ic_launcher_home);

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

        DeviceProfile grid = LauncherActivity.fromContext(mContext).getDeviceProfile();
        int iconSize = grid.iconSizePx - grid.iconDrawablePaddingPx;

        Bitmap bitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas();
        canvas.setBitmap(bitmap);
        paint.setColor(color);
        canvas.drawCircle(iconSize / 2f, iconSize / 2f, iconSize * 0.5f, paint);
        Matrix matrix = new Matrix();
        matrix.setScale(0.8f, 0.8f);
        matrix.postTranslate(0.1f * iconSize, 0.1f * iconSize);
        canvas.drawBitmap(icon, matrix, paint);
//


        Bitmap newBitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(newBitmap);
        new ShadowGenerator(ContextUtils.getApplicationContext()).recreateIcon(bitmap, canvas);
        bitmap.recycle();


        Log.d(TAG, "loadWorkspace--1");

        if (PrefsHelper.with().getBoolean("is_first_run", true)) {
            Log.d(TAG, "loadWorkspace--1.1");
            Delete.table(FavoriteItem.class);
            Log.d(TAG, "loadWorkspace--1.2");
            ArrayList<ItemInfo> itemInfoArrayList = new ArrayList<>(HomepageUtils.initHomeNav());
            for (ItemInfo info : itemInfoArrayList) {
                FavoriteItem item = FavoriteItem.from(info);
                item.save();
            }
            ScreenItem screenItem = new ScreenItem();
            screenItem.setModified(System.currentTimeMillis());
            screenItem.setScreenRank(0);
            screenItem.save();
            PrefsHelper.with().putBoolean("is_first_run", false);
        }

        HomepageManager.getInstance().loadWorkspaceScreens();


        Log.d(TAG, "loadWorkspace--2");
        Log.d(TAG, "loadWorkspace 11111111111");

        HomepageManager manager = HomepageManager.getInstance();
        List<FavoriteItem> list = manager.getAllFavorites();
        Log.d(TAG, "loadWorkspace list.size=" + list.size());
        for (FavoriteItem item : list) {
            Log.d(TAG, "loadWorkspace item=" + item);
            Log.d(TAG, "loadWorkspace item.getItemType()=" + item.getItemType());
            switch (item.getItemType()) {
                case ItemInfo.ITEM_TYPE_APPLICATION:
                    Log.d(TAG, "loadWorkspace 1");
                    ShortcutInfo info = new ShortcutInfo();
                    item.applyCommonProperties(info);
                    info.setIconBitmap(newBitmap);
                    Log.d(TAG, "loadWorkspace 2");
                    manager.checkAndAddItem(mApp.getInvariantDeviceProfile(), info);
                    Log.d(TAG, "loadWorkspace 3");
                    break;
                case ItemInfo.ITEM_TYPE_FOLDER:
                    Log.d(TAG, "loadWorkspace 4");
                    FolderInfo folderInfo = manager.findOrMakeFolder(item.getId());
                    Log.d(TAG, "loadWorkspace 5");
                    item.applyCommonProperties(folderInfo);
                    if (TextUtils.isEmpty(folderInfo.title)) {
                        folderInfo.title = "文件夹";
                    }
                    Log.d(TAG, "loadWorkspace 6");
                    manager.checkAndAddItem(mApp.getInvariantDeviceProfile(), folderInfo);
                    Log.d(TAG, "loadWorkspace 7");
                    break;
                case ItemInfo.ITEM_TYPE_WIDGET:
                    Log.d(TAG, "loadWorkspace 8");
                    TabItemInfo itemInfo = new TabItemInfo();
                    item.applyCommonProperties(itemInfo);
                    Log.d(TAG, "loadWorkspace 9");
                    manager.checkAndAddItem(mApp.getInvariantDeviceProfile(), itemInfo);
                    Log.d(TAG, "loadWorkspace 10");
                    break;
                default:
                    break;
            }
            Log.d(TAG, "loadWorkspace 8");
        }

//            // Remove dead items
        manager.commitDeletedIfNecessary();

        manager.onLoaded(mApp.getInvariantDeviceProfile());
    }

    public void bindWorkspace(int mPageToBindFirst) {

        Callbacks callbacks = mCallbacks.get();
        // Don't use these two variables in any of the callback runnables.
        // Otherwise we hold a reference to them.
        if (callbacks == null) {
            // This launcher has exited and nobody bothered to tell us.  Just bail.
            Log.w(TAG, "LoaderTask running with no launcher");
            return;
        }

        // Save a copy of all the bg-thread collections

        ArrayList<ItemInfo> workspaceItems = new ArrayList<>(HomepageManager.getInstance().getWorkspaceItems());
        final ArrayList<Long> orderedScreenIds = new ArrayList<>(HomepageManager.getInstance().getWorkspaceScreens());

        final int currentScreen;
        {
            int currScreen = mPageToBindFirst != PagedView.INVALID_RESTORE_PAGE
                    ? mPageToBindFirst : callbacks.getCurrentWorkspaceScreen();
            if (currScreen >= orderedScreenIds.size()) {
                // There may be no workspace screens (just hotseat items and an empty page).
                currScreen = PagedView.INVALID_RESTORE_PAGE;
            }
            currentScreen = currScreen;
        }
        final boolean validFirstPage = currentScreen >= 0;
        final long currentScreenId =
                validFirstPage ? orderedScreenIds.get(currentScreen) : -1;

        // Separate the items that are on the current screen, and all the other remaining items
        ArrayList<ItemInfo> currentWorkspaceItems = new ArrayList<>();
        ArrayList<ItemInfo> otherWorkspaceItems = new ArrayList<>();

        filterCurrentWorkspaceItems(currentScreenId, workspaceItems, currentWorkspaceItems,
                otherWorkspaceItems);
        sortWorkspaceItemsSpatially(currentWorkspaceItems);
        sortWorkspaceItemsSpatially(otherWorkspaceItems);

        // Tell the workspace that we're about to start binding items
        callbacks.startBinding();

        // Bind workspace screens
        callbacks.bindScreens(orderedScreenIds);

        // Load items on the current page.
        bindWorkspaceItems(currentWorkspaceItems);

//        callbacks.finishFirstPageBind(
//                validFirstPage ? (ViewOnDrawExecutor) deferredExecutor : null);

        bindWorkspaceItems(otherWorkspaceItems);

        callbacks.finishBindingItems();

        if (validFirstPage) {
            if (currentScreen != PagedView.INVALID_RESTORE_PAGE) {
                callbacks.onPageBoundSynchronously(currentScreen);
            }
//            callbacks.executeOnNextDraw((ViewOnDrawExecutor) deferredExecutor);
        }
    }





    /**
     * Filters the set of items who are directly or indirectly (via another container) on the
     * specified screen.
     */
    public static <T extends ItemInfo> void filterCurrentWorkspaceItems(long currentScreenId,
                                                                        ArrayList<T> allWorkspaceItems,
                                                                        ArrayList<T> currentScreenItems,
                                                                        ArrayList<T> otherScreenItems) {
        // Purge any null ItemInfos
        Iterator<T> iter = allWorkspaceItems.iterator();
        while (iter.hasNext()) {
            ItemInfo i = iter.next();
            if (i == null) {
                iter.remove();
            }
        }

        // Order the set of items by their containers first, this allows use to walk through the
        // list sequentially, build up a list of containers that are in the specified screen,
        // as well as all items in those containers.
        Set<Long> itemsOnScreen = new HashSet<>();
        Collections.sort(allWorkspaceItems, new Comparator<ItemInfo>() {
            @Override
            public int compare(ItemInfo lhs, ItemInfo rhs) {
                return Utilities.longCompare(lhs.container, rhs.container);
            }
        });
        for (T info : allWorkspaceItems) {
            if (info.container == ItemInfo.CONTAINER_DESKTOP) {
                if (info.screenId == currentScreenId) {
                    currentScreenItems.add(info);
                    itemsOnScreen.add(info.id);
                } else {
                    otherScreenItems.add(info);
                }
            } else if (info.container == ItemInfo.CONTAINER_HOTSEAT) {
                currentScreenItems.add(info);
                itemsOnScreen.add(info.id);
            } else {
                if (itemsOnScreen.contains(info.container)) {
                    currentScreenItems.add(info);
                    itemsOnScreen.add(info.id);
                } else {
                    otherScreenItems.add(info);
                }
            }
        }
    }

    private void sortWorkspaceItemsSpatially(ArrayList<ItemInfo> workspaceItems) {
        final InvariantDeviceProfile profile = mApp.getInvariantDeviceProfile();
        final int screenCols = profile.numColumns;
        final int screenCellCount = profile.numColumns * profile.numRows;
        Collections.sort(workspaceItems, new Comparator<ItemInfo>() {
            @Override
            public int compare(ItemInfo lhs, ItemInfo rhs) {
                if (lhs.container == rhs.container) {
                    // Within containers, order by their spatial position in that container
                    switch ((int) lhs.container) {
                        case ItemInfo.CONTAINER_DESKTOP: {
                            long lr = (lhs.screenId * screenCellCount +
                                    lhs.cellY * screenCols + lhs.cellX);
                            long rr = (rhs.screenId * screenCellCount +
                                    rhs.cellY * screenCols + rhs.cellX);
                            return Utilities.longCompare(lr, rr);
                        }
                        case ItemInfo.CONTAINER_HOTSEAT: {
                            // We currently use the screen id as the rank
                            return Utilities.longCompare(lhs.screenId, rhs.screenId);
                        }
                        default:
                            return 0;
                    }
                } else {
                    // Between containers, order by hotseat, desktop
                    return Utilities.longCompare(lhs.container, rhs.container);
                }
            }
        });
    }

    private void bindWorkspaceItems(final ArrayList<ItemInfo> workspaceItems) {

        // Bind the workspace items
        int N = workspaceItems.size();
        for (int i = 0; i < N; i += ITEMS_CHUNK) {
            final int start = i;
            final int chunkSize = (i + ITEMS_CHUNK <= N) ? ITEMS_CHUNK : (N - i);
            mCallbacks.get().bindItems(workspaceItems.subList(start, start + chunkSize), false);
        }
    }

    /**
     * Starts the loader. Tries to bind {@params synchronousBindPage} synchronously if possible.
     *
     * @return true if the page could be bound synchronously.
     */
    public boolean startLoader(int synchronousBindPage) {
        // Enable queue before starting loader. It will get disabled in Launcher#finishBindingItems
        synchronized (mLock) {

            if (isLoaded.get()) {
                Callbacks callbacks = mCallbacks.get();
                // Don't use these two variables in any of the callback runnables.
                // Otherwise we hold a reference to them.
                if (callbacks == null) {
                    // This launcher has exited and nobody bothered to tell us.  Just bail.
                    Log.w(TAG, "LoaderTask running with no launcher");
                    return false;
                }
                bindWorkspace(synchronousBindPage);
            } else {
                loadWorkspace();
                bindWorkspace(synchronousBindPage);
            }
        }
        return false;
    }

    /**
     * If there is already a loader task running, tell it to stop.
     */
    public void stopLoader() {
        synchronized (mLock) {

        }
    }

    public Callbacks getCallback() {
        return mCallbacks != null ? mCallbacks.get() : null;
    }

    public interface Callbacks {
        void rebindModel();

        int getCurrentWorkspaceScreen();

        void startBinding();

        void bindItems(List<ItemInfo> shortcuts, boolean forceAnimateIcons);

        void bindScreens(ArrayList<Long> orderedScreenIds);

//        void finishFirstPageBind(ViewOnDrawExecutor executor);

        void finishBindingItems();

        void bindAppsAdded(ArrayList<Long> newScreens,
                           ArrayList<ItemInfo> addNotAnimated,
                           ArrayList<ItemInfo> addAnimated);

        void bindShortcutsChanged(ArrayList<ShortcutInfo> updated, UserHandle user);

        void onPageBoundSynchronously(int page);

    }


}
