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

import android.content.ComponentName;
import android.content.Intent;

import com.android.launcher3.util.ContentWriter;

/**
 * Represents a launchable icon on the workspaces and in folders.
 */
public class ShortcutInfo extends ItemInfoWithIcon {

    public static final int DEFAULT = 0;

    /**
     * The shortcut was restored from a backup and it not ready to be used. This is automatically
     * set during backup/restore
     */
    public static final int FLAG_RESTORED_ICON = 1;

    /**
     * The icon was added as an auto-install app, and is not ready to be used. This flag can't
     * be present along with {@link #FLAG_RESTORED_ICON}, and is set during default layout
     * parsing.
     */
    public static final int FLAG_AUTOINSTALL_ICON = 2; //0B10;

    /**
     * The icon is being installed. If {@link #FLAG_RESTORED_ICON} or {@link #FLAG_AUTOINSTALL_ICON}
     * is set, then the icon is either being installed or is in a broken state.
     */
    public static final int FLAG_INSTALL_SESSION_ACTIVE = 4; // 0B100;

    /**
     * Indicates that the widget restore has started.
     */
    public static final int FLAG_RESTORE_STARTED = 8; //0B1000;

    /**
     * Web UI supported.
     */
    public static final int FLAG_SUPPORTS_WEB_UI = 16; //0B10000;

    /**
     * If isShortcut=true and customIcon=false, this contains a reference to the
     * shortcut icon as an application's resource.
     */
    public Intent.ShortcutIconResource iconResource;

    /**
     * A message to display when the user tries to start a disabled shortcut.
     * This is currently only used for deep shortcuts.
     */
    public CharSequence disabledMessage;

    public int status;

    /**
     * The installation progress [0-100] of the package that this shortcut represents.
     */
    private int mInstallProgress;

    public ShortcutInfo() {
        itemType = LauncherSettings.BaseLauncherColumns.ITEM_TYPE_APPLICATION;
    }

    public ShortcutInfo(ShortcutInfo info) {
        super(info);
        title = info.title;
        iconResource = info.iconResource;
        status = info.status;
        mInstallProgress = info.mInstallProgress;
    }

//    /**
//     * Creates a {@link ShortcutInfo} from a {@link ShortcutInfoCompat}.
//     */
//    @TargetApi(Build.VERSION_CODES.N)
//    public ShortcutInfo(ShortcutInfoCompat shortcutInfo, Context context) {
//        user = shortcutInfo.getUserHandle();
//        itemType = LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT;
//    }

    @Override
    public void onAddToDatabase(ContentWriter writer) {
        super.onAddToDatabase(writer);
        writer.put(LauncherSettings.BaseLauncherColumns.TITLE, title)
                .put(LauncherSettings.Favorites.RESTORED, status);

        if (!usingLowResIcon) {
            writer.putIcon(iconBitmap, user);
        }
        if (iconResource != null) {
            writer.put(LauncherSettings.BaseLauncherColumns.ICON_PACKAGE, iconResource.packageName)
                    .put(LauncherSettings.BaseLauncherColumns.ICON_RESOURCE,
                            iconResource.resourceName);
        }
    }

    public boolean hasStatusFlag(int flag) {
        return (status & flag) != 0;
    }

    @Override
    public ComponentName getTargetComponent() {
        ComponentName cn = super.getTargetComponent();
        if (cn == null && (itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION
                || hasStatusFlag(FLAG_SUPPORTS_WEB_UI))) {
            // Legacy shortcuts and promise icons with web UI may not have a componentName but just
            // a packageName. In that case create a dummy componentName instead of adding additional
            // check everywhere.
//            String pkg = intent.getPackage();
//            return pkg == null ? null : new ComponentName(pkg, IconCache.EMPTY_CLASS_NAME);
            return null;
        }
        return cn;
    }
}
