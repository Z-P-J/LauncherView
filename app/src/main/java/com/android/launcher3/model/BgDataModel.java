/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.util.MutableInt;

import com.android.launcher3.FolderInfo;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.util.LongArrayMap;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * All the data stored in-memory and managed by the LauncherModel
 */
public class BgDataModel {

    private static final String TAG = "BgDataModel";

    /**
     * Map of all the ItemInfos (shortcuts, folders, and widgets) created by
     * LauncherModel to their ids
     */
    public final LongArrayMap<ItemInfo> itemsIdMap = new LongArrayMap<>();

    /**
     * List of all the folders and shortcuts directly on the home screen (no widgets
     * or shortcuts within folders).
     */
    public final ArrayList<ItemInfo> workspaceItems = new ArrayList<>();

    /**
     * Map of id to FolderInfos of all the folders created by LauncherModel
     */
    public final LongArrayMap<FolderInfo> folders = new LongArrayMap<>();

    /**
     * Ordered list of workspace screens ids.
     */
    public final ArrayList<Long> workspaceScreens = new ArrayList<>();

    /**
     * Maps all launcher activities to the id's of their shortcuts (if they have any).
     */
//    public final MultiHashMap<ComponentKey, String> deepShortcutMap = new MultiHashMap<>();

    /**
     * Id when the model was last bound
     */
    public int lastBindId = 0;

    /**
     * Clears all the data
     */
    public synchronized void clear() {
        workspaceItems.clear();
        folders.clear();
        itemsIdMap.clear();
        workspaceScreens.clear();
    }

    public synchronized void removeItem(Context context, ItemInfo... items) {
        removeItem(context, Arrays.asList(items));
    }

    public synchronized void removeItem(Context context, Iterable<? extends ItemInfo> items) {
        for (ItemInfo item : items) {
            switch (item.itemType) {
                case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                    folders.remove(item.id);
                    if (FeatureFlags.IS_DOGFOOD_BUILD) {
                        for (ItemInfo info : itemsIdMap) {
                            if (info.container == item.id) {
                                // We are deleting a folder which still contains items that
                                // think they are contained by that folder.
                                String msg = "deleting a folder (" + item + ") which still " +
                                        "contains items (" + info + ")";
                                Log.e(TAG, msg);
                            }
                        }
                    }
                    workspaceItems.remove(item);
                    break;
                case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                    workspaceItems.remove(item);
                    break;
            }
            itemsIdMap.remove(item.id);
        }
    }

    public synchronized void addItem(Context context, ItemInfo item, boolean newItem) {
        itemsIdMap.put(item.id, item);
        switch (item.itemType) {
            case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                folders.put(item.id, (FolderInfo) item);
                workspaceItems.add(item);
                break;
            case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                if (item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP ||
                        item.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                    workspaceItems.add(item);
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
        }
    }

    /**
     * Return an existing FolderInfo object if we have encountered this ID previously,
     * or make a new one.
     */
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
}
