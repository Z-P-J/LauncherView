package com.ark.browser.launcher.database;

import android.util.Log;

import com.android.launcher3.FolderInfo;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.Workspace;
import com.android.launcher3.folder.Folder;
import com.android.launcher3.folder.FolderIconPreviewVerifier;
import com.android.launcher3.util.GridOccupancy;
import com.android.launcher3.util.LongArrayMap;
import com.ark.browser.launcher.model.FavoriteItem;
import com.ark.browser.launcher.model.FavoriteItem_Table;
import com.ark.browser.launcher.model.ScreenItem;
import com.ark.browser.launcher.model.ScreenItem_Table;
import com.raizlabs.android.dbflow.annotation.Database;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.database.DatabaseWrapper;
import com.raizlabs.android.dbflow.structure.database.transaction.ProcessModelTransaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static com.android.launcher3.folder.ClippedFolderIconLayoutRule.MAX_NUM_ITEMS_IN_PREVIEW;

@Database(name = HomepageManager.NAME, version = HomepageManager.VERSION)
public class HomepageManager {

//    @Migration(version = 29, database = HomepageManager.class)
//    public static class HomeMigration extends AlterTableMigration<FavoriteItem> {
//
//        public HomeMigration(Class<FavoriteItem> table) {
//            super(table);
//        }
//
//        @Override
//        public void onPreMigrate() {
//            Log.d(TAG, "HomeMigration onPreMigrate");
//            addColumn(SQLiteType.INTEGER, "tabId");
//        }
//    }


    private static final String TAG = "HomepageManager";

    static final String NAME = "launcher";
    public static final int VERSION = 1;

    private final ArrayList<ItemInfo> itemsToRemove = new ArrayList<>();
    private final LongArrayMap<GridOccupancy> occupied = new LongArrayMap<>();

    public final LongArrayMap<ItemInfo> itemsIdMap = new LongArrayMap<>();
    private final ArrayList<ItemInfo> mWorkspaceItems = new ArrayList<>();
    private final ArrayList<Long> mWorkspaceScreens = new ArrayList<>();
    public final LongArrayMap<FolderInfo> folders = new LongArrayMap<>();

//    private final Context mContext;
//    private final InvariantDeviceProfile mIDP;

    private static final HomepageManager INSTANCE = new HomepageManager();

    public static HomepageManager getInstance() {
        return INSTANCE;
    }

    public ArrayList<ItemInfo> getWorkspaceItems() {
        return mWorkspaceItems;
    }

    public ArrayList<Long> getWorkspaceScreens() {
        return mWorkspaceScreens;
    }

    //    public HomepageManager(LauncherAppState app) {
//        mContext = app.getContext();
//        mIDP = app.getInvariantDeviceProfile();
//    }

    public List<FavoriteItem> getAllFavorites() {
        return SQLite.select()
                .from(FavoriteItem.class)
                .queryList();
    }

    public void checkAndAddItem(InvariantDeviceProfile mIDP, ItemInfo info) {
        if (checkItemPlacement(mIDP, info, mWorkspaceScreens)) {
            Log.d("loadWorkspace", "checkAndAddItem checkItemPlacement");
            addItem(info, false);
        } else {
            Log.d("loadWorkspace", "checkAndAddItem markDeleted");
            markDeleted(info, "Item position overlap");
        }
    }

    public synchronized void addItem(ItemInfo item, boolean newItem) {
        itemsIdMap.put(item.id, item);
        switch (item.itemType) {
            case ItemInfo.ITEM_TYPE_FOLDER:
                folders.put(item.id, (FolderInfo) item);
                mWorkspaceItems.add(item);
                break;
            case ItemInfo.ITEM_TYPE_APPLICATION:
                if (item.container == ItemInfo.CONTAINER_DESKTOP ||
                        item.container == ItemInfo.CONTAINER_HOTSEAT) {
                    mWorkspaceItems.add(item);
                } else {
                    if (newItem) {
                        if (!folders.containsKey(item.container)) {
                            // Adding an item to a folder that doesn't exist.
                            String msg = "adding item: " + item + " to a folder that " +
                                    " doesn't exist";
                            Log.e(TAG, msg);
                        }
                    } else {
                        findOrMakeFolder(item.container).add((ShortcutInfo) item, false);
                    }

                }
                break;
            case ItemInfo.ITEM_TYPE_WIDGET:
                if (item.container == ItemInfo.CONTAINER_DESKTOP) {
                    mWorkspaceItems.add(item);
                } else {
                    markDeleted(item, "Item invalid!");

                }
                break;
        }
    }

    public synchronized FolderInfo findOrMakeFolder(long id) {
        // See if a placeholder was created for us already
        FolderInfo folderInfo = folders.get(id);
        if (folderInfo == null) {
            // No placeholder -- create a new instance
            folderInfo = new FolderInfo();
            folders.put(id, folderInfo);
        }
        return folderInfo;
    }

    /**
     * check & update map of what's occupied; used to discard overlapping/invalid items
     */
    public boolean checkItemPlacement(InvariantDeviceProfile mIDP, ItemInfo item, ArrayList<Long> workspaceScreens) {
        long containerIndex = item.screenId;
        if (item.container == ItemInfo.CONTAINER_HOTSEAT) {
            // Return early if we detect that an item is under the hotseat button
            if (mIDP.isAllAppsButtonRank((int) item.screenId)) {
                Log.e(TAG, "Error loading shortcut into hotseat " + item
                        + " into position (" + item.screenId + ":" + item.cellX + ","
                        + item.cellY + ") occupied by all apps");
                return false;
            }

            final GridOccupancy hotseatOccupancy =
                    occupied.get((long) ItemInfo.CONTAINER_HOTSEAT);

            if (item.screenId >= mIDP.numHotseatIcons) {
                Log.e(TAG, "Error loading shortcut " + item
                        + " into hotseat position " + item.screenId
                        + ", position out of bounds: (0 to " + (mIDP.numHotseatIcons - 1)
                        + ")");
                return false;
            }

            if (hotseatOccupancy != null) {
                if (hotseatOccupancy.cells[(int) item.screenId][0]) {
                    Log.e(TAG, "Error loading shortcut into hotseat " + item
                            + " into position (" + item.screenId + ":" + item.cellX + ","
                            + item.cellY + ") already occupied");
                    return false;
                } else {
                    hotseatOccupancy.cells[(int) item.screenId][0] = true;
                    return true;
                }
            } else {
                Log.e(TAG, "hotseatOccupancy=null " + item
                        + " into position (" + item.screenId + ":" + item.cellX + ","
                        + item.cellY + ") already occupied");
//                item.screenId = 0;
                final GridOccupancy occupancy = new GridOccupancy(mIDP.numHotseatIcons, 1);
                occupancy.cells[(int) item.screenId][0] = true;
                occupied.put((long) ItemInfo.CONTAINER_HOTSEAT, occupancy);
                return true;
            }
        } else if (item.container == ItemInfo.CONTAINER_DESKTOP) {
            if (!workspaceScreens.contains((Long) item.screenId)) {
                // The item has an invalid screen id.
                return false;
            }
        } else {
            // Skip further checking if it is not the hotseat or workspace container
            return true;
        }

        final int countX = mIDP.numColumns;
        final int countY = mIDP.numRows;
        if (item.container == ItemInfo.CONTAINER_DESKTOP &&
                item.cellX < 0 || item.cellY < 0 ||
                item.cellX + item.spanX > countX || item.cellY + item.spanY > countY) {
            Log.e(TAG, "Error loading shortcut " + item
                    + " into cell (" + containerIndex + "-" + item.screenId + ":"
                    + item.cellX + "," + item.cellY
                    + ") out of screen bounds ( " + countX + "x" + countY + ")");
            return false;
        }

        if (!occupied.containsKey(item.screenId)) {
            GridOccupancy screen = new GridOccupancy(countX + 1, countY + 1);
            if (item.screenId == Workspace.FIRST_SCREEN_ID) {
                // Mark the first row as occupied (if the feature is enabled)
                // in order to account for the QSB.
                screen.markCells(0, 0, countX + 1, 1, true);
            }
            occupied.put(item.screenId, screen);
        }
        final GridOccupancy occupancy = occupied.get(item.screenId);

        // Check if any workspace icons overlap with each other
        if (occupancy.isRegionVacant(item.cellX, item.cellY, item.spanX, item.spanY)) {
            occupancy.markCells(item, true);
            return true;
        } else {
            Log.e(TAG, "Error loading shortcut " + item
                    + " into cell (" + containerIndex + "-" + item.screenId + ":"
                    + item.cellX + "," + item.cellX + "," + item.spanX + "," + item.spanY
                    + ") already occupied");
            return false;
        }
    }

    public void markDeleted(ItemInfo info, String reason) {
        Log.e(TAG, reason);
        itemsToRemove.add(info);
    }

    public void commitDeletedIfNecessary() {
        if (itemsToRemove.size() > 0) {
            for (ItemInfo info : itemsToRemove) {
                FavoriteItem item = FavoriteItem.from(info);
                item.delete();
                if (info instanceof ShortcutInfo) {
                    mWorkspaceItems.remove(info);
                } else if (info instanceof FolderInfo) {
                    mWorkspaceItems.remove(folders.get(info.id));
                    folders.remove(info.id);
                }
                itemsIdMap.remove(info.id);
            }
        }
    }

    public void onLoaded(InvariantDeviceProfile mIDP) {
        FolderIconPreviewVerifier verifier =
                new FolderIconPreviewVerifier(mIDP);
        // Sort the folder items and make sure all items in the preview are high resolution.
        for (FolderInfo folder : folders) {
            Collections.sort(folder.contents, Folder.ITEM_POS_COMPARATOR);
            verifier.setFolderInfo(folder);

            int numItemsInPreview = 0;
            for (ShortcutInfo info : folder.contents) {
                if (info.usingLowResIcon
                        && info.itemType == ItemInfo.ITEM_TYPE_APPLICATION
                        && verifier.isItemInPreview(info.rank)) {
                    numItemsInPreview++;
                }

                if (numItemsInPreview >= MAX_NUM_ITEMS_IN_PREVIEW) {
                    break;
                }
            }
        }

//            c.commitRestoredItems();

        // Remove any empty screens
        ArrayList<Long> unusedScreens = new ArrayList<>(mWorkspaceScreens);
        for (ItemInfo item : itemsIdMap) {
            long screenId = item.screenId;
            if (item.container == ItemInfo.CONTAINER_DESKTOP) {
                unusedScreens.remove(screenId);
            }
        }

        // If there are any empty screens remove them, and update.
        if (unusedScreens.size() != 0) {
            mWorkspaceScreens.removeAll(unusedScreens);
            Log.d(TAG, "onLoaded-->updateWorkspaceScreenOrder unusedScreens=" + unusedScreens);
            HomepageManager.getInstance().updateWorkspaceScreenOrder(mWorkspaceScreens);
        }
    }


    public static FavoriteItem getFavoriteByUrl(String url) {
        List<FavoriteItem> historyList = SQLite.select()
                .from(FavoriteItem.class)
                .where(FavoriteItem_Table.url.is(url))
                .queryList();
        if (historyList.isEmpty()) {
            return null;
        } else {
            for (int i = 1; i < historyList.size(); i++) {
                historyList.get(i).delete();
            }
            return historyList.get(0);
        }
    }

    public void deleteAllFavorites() {
        SQLite.delete()
                .from(FavoriteItem.class)
                .execute();
        clear();
    }

    public void addOrMoveItemInDatabase(ItemInfo item,
                                               long container, long screenId, int cellX, int cellY) {
        Log.d(TAG, "addOrMoveItemInDatabase item=" + item + " container=" + container +
                " screenId=" + screenId + " cellX=" + cellX + " cellY=" + cellY);
        if (item.container == ItemInfo.NO_ID) {
            // From all apps
            addItemToDatabase(item, container, screenId, cellX, cellY);
        } else {
            // From somewhere else
            moveItemInDatabase(item, container, screenId, cellX, cellY);
        }
    }

    public void addItemToDatabase(final ItemInfo item,
                                         long container, long screenId, int cellX, int cellY) {
        Log.d(TAG, "addItemToDatabase");
        updateItemInfoProps(item, container, screenId, cellX, cellY);
        FavoriteItem favoriteItem = FavoriteItem.from(item);
        favoriteItem.insert();
        item.id = favoriteItem.getId();
        addItem(item, true);
        Log.d("addItemToDatabase", "id=" + item.id);
    }

    public void deleteItemFromDatabase(ItemInfo item) {
        Log.d(TAG, "deleteItemFromDatabase");
        FavoriteItem.from(item).delete();
        removeItem(item);
    }

    public synchronized void removeItem(ItemInfo... items) {
        removeItem(Arrays.asList(items));
    }

    public synchronized void removeItem(Iterable<? extends ItemInfo> items) {
        for (ItemInfo item : items) {
            switch (item.itemType) {
                case ItemInfo.ITEM_TYPE_FOLDER:
                    folders.remove(item.id);
                    mWorkspaceItems.remove(item);
                    break;
                case ItemInfo.ITEM_TYPE_APPLICATION:
                    mWorkspaceItems.remove(item);
                    break;
            }
            itemsIdMap.remove(item.id);
        }
    }

    public void moveItemInDatabase(final ItemInfo item,
                                          long container, long screenId, int cellX, int cellY) {
        Log.d(TAG, "moveItemInDatabase");
        updateItemInfoProps(item, container, screenId, cellX, cellY);
        boolean result = FavoriteItem.from(item).update();
        Log.d(TAG, "moveItemInDatabase result=" + result);
    }

    public void moveItemsInDatabase(final ArrayList<ItemInfo> items, long container, int screen) {
        Log.d(TAG, "moveItemsInDatabase222");
        for (ItemInfo item : items) {
            updateItemInfoProps(item, container, screen, item.cellX, item.cellY);
            boolean result = FavoriteItem.from(item).update();
            Log.d(TAG, "moveItemsInDatabase222 result=" + result);
        }
    }

    public void modifyItemInDatabase(final ItemInfo item,
                                            long container, long screenId, int cellX, int cellY, int spanX, int spanY) {
        Log.d(TAG, "modifyItemInDatabase item=" + item);
        Log.d(TAG, "modifyItemInDatabase container=" + container + " screenId=" + screenId +
                " cellX=" + cellX + " cellY=" + cellY + " spanX=" + spanX + " spanY=" + spanY);
        updateItemInfoProps(item, container, screenId, cellX, cellY);
        item.spanX = spanX;
        item.spanY = spanY;
        FavoriteItem.from(item).update();
    }

    /**
     * Update an item to the database in a specified container.
     */
    public static void updateItemInDatabase(ItemInfo item) {
        Log.d(TAG, "updateItemInDatabase");
        FavoriteItem.from(item).update();
    }

    private void updateItemInfoProps(
            ItemInfo item, long container, long screenId, int cellX, int cellY) {
        Log.d(TAG, "updateItemInfoProps");
        item.container = container;
        item.cellX = cellX;
        item.cellY = cellY;
        // We store hotseat items in canonical form which is this orientation invariant position
        // in the hotseat
        if (container == ItemInfo.CONTAINER_HOTSEAT) {
//            item.screenId = mHasVerticalHotseat
//                    ? LauncherAppState.getIDP(mContext).numHotseatIcons - cellY - 1 : cellX;
            item.screenId = cellX;
        } else {
            item.screenId = screenId;
        }
//        item.screenId = screenId;
    }

    public static List<Long> loadWorkspaceScreensDb() {
        Log.d(TAG, "loadWorkspaceScreensDb");
        List<ScreenItem> screenItemList = SQLite.select()
                .from(ScreenItem.class)
                .orderBy(ScreenItem_Table.screenRank, false)
                .queryList();
        List<Long> list = new ArrayList<>();
        for (ScreenItem item : screenItemList) {
            list.add(item.getScreenRank());
        }
        return list;
    }

    public void loadWorkspaceScreens() {
        Log.d(TAG, "loadWorkspaceScreensDb");
        List<ScreenItem> screenItemList = SQLite.select()
                .from(ScreenItem.class)
                .orderBy(ScreenItem_Table.screenRank, false)
                .queryList();
        mWorkspaceScreens.clear();
        for (ScreenItem item : screenItemList) {
            mWorkspaceScreens.add(item.getScreenRank());
        }
    }

    public void deleteFolderAndContentsFromDatabase(final FolderInfo info) {
        SQLite.delete()
                .from(FavoriteItem.class)
                .where(FavoriteItem_Table.container.is(info.id))
                .execute();
        info.contents.clear();
        SQLite.delete()
                .from(FavoriteItem.class)
                .where(FavoriteItem_Table._id.is(info.id))
                .execute();
        removeItem(info);
    }

    public void updateWorkspaceScreenOrder(final ArrayList<Long> screens) {
        Log.d(TAG, "updateWorkspaceScreenOrder screens=" + screens);
        final ArrayList<Long> screensCopy = new ArrayList<Long>(screens);

        // Remove any negative screen ids -- these aren't persisted
        Iterator<Long> iter = screensCopy.iterator();
        while (iter.hasNext()) {
            long id = iter.next();
            if (id < 0) {
                iter.remove();
            }
        }
        Log.d(TAG, "updateWorkspaceScreenOrder screensCopy=" + screensCopy);

        SQLite.delete()
                .from(ScreenItem.class)
                .execute();
        List<ScreenItem> screenItemList = new ArrayList<>();
        int count = screensCopy.size();
        for (int i = 0; i < count; i++) {
            ScreenItem item = new ScreenItem();
            long screenId = screensCopy.get(i);
            item.setId(i + 1);
            item.setScreenRank(screenId);
            screenItemList.add(item);
        }
        Log.d(TAG, "updateWorkspaceScreenOrder screenItemList=" + screenItemList);

        FlowManager.getDatabase(HomepageManager.class)
//                .beginTransactionAsync(new ProcessModelTransaction.Builder<>(new ProcessModelTransaction.ProcessModel<ScreenItem>() {
//                    @Override
//                    public void processModel(ScreenItem screenItem, DatabaseWrapper wrapper) {
//                        screenItem.save();
//                    }
//                }).addAll(screenItemList).build())
//                .build()
                .beginTransactionAsync(new ProcessModelTransaction.Builder<>(new ProcessModelTransaction.ProcessModel<ScreenItem>() {
                    @Override
                    public void processModel(ScreenItem screenItem, DatabaseWrapper wrapper) {
                        Log.d(TAG, "updateWorkspaceScreenOrder screenItem=" + screenItem + " exists=" + screenItem.exists());
                        boolean result = screenItem.save();
                        Log.d(TAG, "updateWorkspaceScreenOrder screenItem=" + screenItem + " result=" + result);
                    }
                }).addAll(screenItemList).build())
                .error((transaction, error) -> {
                    throw new RuntimeException(error);
                }).success(transaction -> {
            synchronized (INSTANCE) {
                mWorkspaceScreens.clear();
                mWorkspaceScreens.addAll(screensCopy);
            }
        })
                .build()
                .execute();
    }

    public synchronized void clear() {
        mWorkspaceItems.clear();
        folders.clear();
        itemsIdMap.clear();
        mWorkspaceScreens.clear();
        occupied.clear();
        itemsToRemove.clear();
    }

}
