/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.launcher3.touch;

import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.android.launcher3.FolderInfo;
import com.android.launcher3.ItemInfoWithIcon;
import com.android.launcher3.LauncherLayout;
import com.android.launcher3.LauncherManager;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.folder.Folder;
import com.android.launcher3.folder.FolderIcon;

/**
 * Class for handling clicks on workspace and all-apps items
 */
public class ItemClickHandler {

    /**
     * Instance used for click handling on items
     */
    public static final OnClickListener INSTANCE = ItemClickHandler::onClick;

    private static void onClick(View v) {
        // Make sure that rogue clicks don't get through while allapps is launching, or after the
        // view has detached (it's possible for this to happen if the view is removed mid touch).
        if (v.getWindowToken() == null) {
            return;
        }

        if (!LauncherManager.getWorkspace().isFinishedSwitchingState()) {
            return;
        }

        Object tag = v.getTag();
        if (tag instanceof ShortcutInfo) {
            onClickAppShortcut(v, (ItemInfoWithIcon) tag, v.getContext());
        } else if (tag instanceof FolderInfo) {
            if (v instanceof FolderIcon) {
                onClickFolderIcon(v);
            }
        }
    }

    /**
     * Event handler for a folder icon click.
     *
     * @param v The view that was clicked. Must be an instance of {@link FolderIcon}.
     */
    private static void onClickFolderIcon(View v) {
        Folder folder = ((FolderIcon) v).getFolder();
        if (!folder.isOpen() && !folder.isDestroyed()) {
            // Open the requested folder
            folder.animateOpen();
        }
    }

    /**
     * Event handler for an app shortcut click.
     *
     * @param v The view that was clicked. Must be a tagged with a {@link ShortcutInfo}.
     */
    private static void onClickAppShortcut(View v, ItemInfoWithIcon shortcut, Context context) {
        LauncherLayout.ClickHandler clickHandler = LauncherManager.getLauncherLayout().getClickHandler();
        if (clickHandler != null) {
            clickHandler.onClickAppShortcut(v, shortcut);
        }
    }
}
