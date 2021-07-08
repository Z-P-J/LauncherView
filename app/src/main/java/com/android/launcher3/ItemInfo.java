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

/**
 * Represents an item in the launcher.
 */
public class ItemInfo {

    public static final int NO_ID = -1;

    /**
     * The id in the settings database for this item
     */
    public long id = NO_ID;

    public long tabId = NO_ID;

    /**
     * One of {@link ItemInfo#ITEM_TYPE_APPLICATION},
     * {@link ItemInfo#ITEM_TYPE_FOLDER},
     */
    public int itemType;

    /**
     * The id of the container that holds this item. For the desktop, this will be
     * {@link ItemInfo#CONTAINER_DESKTOP}. For the all applications folder it
     * will be {@link #NO_ID} (since it is not stored in the settings DB). For user folders
     * it will be the id of the folder.
     */
    public long container = NO_ID;

    /**
     * Indicates the screen in which the shortcut appears if the container types is
     * {@link ItemInfo#CONTAINER_DESKTOP}. (i.e., ignore if the container type is
     * {@link ItemInfo#CONTAINER_HOTSEAT})
     */
    public long screenId = -1;

    /**
     * Indicates the X position of the associated cell.
     */
    public int cellX = -1;

    /**
     * Indicates the Y position of the associated cell.
     */
    public int cellY = -1;

    /**
     * Indicates the X cell span.
     */
    public int spanX = 1;

    /**
     * Indicates the Y cell span.
     */
    public int spanY = 1;

    /**
     * Indicates the minimum X cell span.
     */
    public int minSpanX = 1;

    /**
     * Indicates the minimum Y cell span.
     */
    public int minSpanY = 1;

    /**
     * Indicates the position in an ordered list.
     */
    public int rank = 0;

    /**
     * Title of the item
     */
    public CharSequence title;

    public String url;

    /**
     * Content description of the item.
     */
    public CharSequence contentDescription;

    public ItemInfo() {
    }

    ItemInfo(ItemInfo info) {
        copyFrom(info);
        // tempdebug:
        LauncherLoader.checkItemInfo(this);
    }

    public void copyFrom(ItemInfo info) {
        id = info.id;
        cellX = info.cellX;
        cellY = info.cellY;
        spanX = info.spanX;
        spanY = info.spanY;
        rank = info.rank;
        screenId = info.screenId;
        itemType = info.itemType;
        container = info.container;
        contentDescription = info.contentDescription;
    }

    @Override
    public final String toString() {
        return getClass().getSimpleName() + "(" + dumpProperties() + ")";
    }

    protected String dumpProperties() {
        return "id=" + id
                + " type=" + itemTypeToString(itemType)
                + " container=" + containerToString((int) container)
                + " screen=" + screenId
                + " cell(" + cellX + "," + cellY + ")"
                + " span(" + spanX + "," + spanY + ")"
                + " minSpan(" + minSpanX + "," + minSpanY + ")"
                + " rank=" + rank
                + " title=" + title;
    }

    public static final int CONTAINER_DESKTOP = -100;
    public static final int CONTAINER_HOTSEAT = -101;

    static String containerToString(int container) {
        switch (container) {
            case CONTAINER_DESKTOP:
                return "desktop";
            case CONTAINER_HOTSEAT:
                return "hotseat";
            default:
                return String.valueOf(container);
        }
    }

    public static final int ITEM_TYPE_APPLICATION = 0;
    public static final int ITEM_TYPE_FOLDER = 2;
    public static final int ITEM_TYPE_WIDGET = 4;
    static String itemTypeToString(int type) {
        switch (type) {
            case ITEM_TYPE_APPLICATION:
                return "APP";
            case ITEM_TYPE_FOLDER:
                return "FOLDER";
            case ITEM_TYPE_WIDGET:
                return "WIDGET";
            default:
                return String.valueOf(type);
        }
    }

    /**
     * Whether this item is disabled.
     */
    public boolean isDisabled() {
        return false;
    }

    public int getViewIdForItem() {
        // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
        // This cast is safe as long as the id < 0x00FFFFFF
        // Since we jail all the dynamically generated views, there should be no clashes
        // with any other views.
        return (int) id;
    }
}
