/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.launcher3.model;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.util.LongSparseArray;

import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.Utilities;
import com.android.launcher3.Workspace;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.util.GridOccupancy;
import com.android.launcher3.util.LongArrayMap;

import java.net.URISyntaxException;
import java.util.ArrayList;

/**
 * Extension of {@link Cursor} with utility methods for workspace loading.
 */
public class LoaderCursor extends CursorWrapper {

    private static final String TAG = "LoaderCursor";

    public final LongSparseArray<UserHandle> allUsers = new LongSparseArray<>();

    private final Context mContext;
    private final InvariantDeviceProfile mIDP;

    private final ArrayList<Long> itemsToRemove = new ArrayList<>();
    private final ArrayList<Long> restoredRows = new ArrayList<>();
    private final LongArrayMap<GridOccupancy> occupied = new LongArrayMap<>();

    private final int iconPackageIndex;
    private final int iconResourceIndex;
    private final int iconIndex;
    public final int titleIndex;

    private final int idIndex;
    private final int containerIndex;
    private final int itemTypeIndex;
    private final int screenIndex;
    private final int cellXIndex;
    private final int cellYIndex;
    private final int profileIdIndex;
    private final int restoredIndex;
    private final int intentIndex;

    // Properties loaded per iteration
    public long serialNumber;
    public UserHandle user;
    public long id;
    public long container;
    public int itemType;
    public int restoreFlag;

    public LoaderCursor(Cursor c, LauncherAppState app) {
        super(c);
        mContext = app.getContext();
        mIDP = app.getInvariantDeviceProfile();

        // Init column indices
        iconIndex = getColumnIndexOrThrow(LauncherSettings.Favorites.ICON);
        iconPackageIndex = getColumnIndexOrThrow(LauncherSettings.Favorites.ICON_PACKAGE);
        iconResourceIndex = getColumnIndexOrThrow(LauncherSettings.Favorites.ICON_RESOURCE);
        titleIndex = getColumnIndexOrThrow(LauncherSettings.Favorites.TITLE);

        idIndex = getColumnIndexOrThrow(LauncherSettings.Favorites._ID);
        containerIndex = getColumnIndexOrThrow(LauncherSettings.Favorites.CONTAINER);
        itemTypeIndex = getColumnIndexOrThrow(LauncherSettings.Favorites.ITEM_TYPE);
        screenIndex = getColumnIndexOrThrow(LauncherSettings.Favorites.SCREEN);
        cellXIndex = getColumnIndexOrThrow(LauncherSettings.Favorites.CELLX);
        cellYIndex = getColumnIndexOrThrow(LauncherSettings.Favorites.CELLY);
        profileIdIndex = getColumnIndexOrThrow(LauncherSettings.Favorites.PROFILE_ID);
        restoredIndex = getColumnIndexOrThrow(LauncherSettings.Favorites.RESTORED);
        intentIndex = getColumnIndexOrThrow(LauncherSettings.Favorites.INTENT);
    }

    @Override
    public boolean moveToNext() {
        boolean result = super.moveToNext();
        if (result) {
            // Load common properties.
            itemType = getInt(itemTypeIndex);
            container = getInt(containerIndex);
            id = getLong(idIndex);
            serialNumber = getInt(profileIdIndex);
            user = allUsers.get(serialNumber);
            restoreFlag = getInt(restoredIndex);
        }
        return result;
    }

    public Intent parseIntent() {
        String intentDescription = getString(intentIndex);
        try {
            return TextUtils.isEmpty(intentDescription) ?
                    null : Intent.parseUri(intentDescription, 0);
        } catch (URISyntaxException e) {
            Log.e(TAG, "Error parsing Intent");
            return null;
        }
    }

    /**
     * Marks the current item for removal
     */
    public void markDeleted(String reason) {
        Log.e(TAG, reason);
        itemsToRemove.add(id);
    }

    /**
     * Removes any items marked for removal.
     *
     * @return true is any item was removed.
     */
    public boolean commitDeleted() {
        if (itemsToRemove.size() > 0) {
            // Remove dead items
            mContext.getContentResolver().delete(LauncherSettings.Favorites.CONTENT_URI,
                    Utilities.createDbSelectionQuery(
                            LauncherSettings.Favorites._ID, itemsToRemove), null);
            return true;
        }
        return false;
    }

    /**
     * Marks the current item as restored
     */
    public void markRestored() {
        if (restoreFlag != 0) {
            restoredRows.add(id);
            restoreFlag = 0;
        }
    }

    public void commitRestoredItems() {
        if (restoredRows.size() > 0) {
            // Update restored items that no longer require special handling
            ContentValues values = new ContentValues();
            values.put(LauncherSettings.Favorites.RESTORED, 0);
            mContext.getContentResolver().update(LauncherSettings.Favorites.CONTENT_URI, values,
                    Utilities.createDbSelectionQuery(
                            LauncherSettings.Favorites._ID, restoredRows), null);
        }
    }

    /**
     * Applies the following properties:
     * {@link ItemInfo#id}
     * {@link ItemInfo#container}
     * {@link ItemInfo#screenId}
     * {@link ItemInfo#cellX}
     * {@link ItemInfo#cellY}
     */
    public void applyCommonProperties(ItemInfo info) {
        info.id = id;
        info.container = container;
        info.screenId = getInt(screenIndex);
        info.cellX = getInt(cellXIndex);
        info.cellY = getInt(cellYIndex);
    }

    /**
     * Adds the {@param info} to {@param dataModel} if it does not overlap with any other item,
     * otherwise marks it for deletion.
     */
    public void checkAndAddItem(ItemInfo info, BgDataModel dataModel) {
        if (checkItemPlacement(info, dataModel.workspaceScreens)) {
            dataModel.addItem(mContext, info, false);
        } else {
            markDeleted("Item position overlap");
        }
    }

    /**
     * check & update map of what's occupied; used to discard overlapping/invalid items
     */
    protected boolean checkItemPlacement(ItemInfo item, ArrayList<Long> workspaceScreens) {
        long containerIndex = item.screenId;
        if (item.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
            // Return early if we detect that an item is under the hotseat button
            if (!FeatureFlags.NO_ALL_APPS_ICON &&
                    mIDP.isAllAppsButtonRank((int) item.screenId)) {
                Log.e(TAG, "Error loading shortcut into hotseat " + item
                        + " into position (" + item.screenId + ":" + item.cellX + ","
                        + item.cellY + ") occupied by all apps");
                return false;
            }

            final GridOccupancy hotseatOccupancy =
                    occupied.get((long) LauncherSettings.Favorites.CONTAINER_HOTSEAT);

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
                final GridOccupancy occupancy = new GridOccupancy(mIDP.numHotseatIcons, 1);
                occupancy.cells[(int) item.screenId][0] = true;
                occupied.put((long) LauncherSettings.Favorites.CONTAINER_HOTSEAT, occupancy);
                return true;
            }
        } else if (item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
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
        if (item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP &&
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
                screen.markCells(0, 0, countX + 1, 1, FeatureFlags.QSB_ON_FIRST_SCREEN);
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
}
