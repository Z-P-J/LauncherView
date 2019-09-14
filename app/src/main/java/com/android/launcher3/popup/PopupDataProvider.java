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

package com.android.launcher3.popup;

import android.content.ComponentName;
import android.support.annotation.NonNull;
import android.util.Log;

import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.MultiHashMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Provides data for the popup menu that appears after long-clicking on apps.
 */
public class PopupDataProvider {

    private static final boolean LOGD = false;
    private static final String TAG = "PopupDataProvider";

    /**
     * Note that these are in order of priority.
     */
    private static final SystemShortcut[] SYSTEM_SHORTCUTS = new SystemShortcut[]{
            new SystemShortcut.AppInfo(),
            new SystemShortcut.Widgets(),
            new SystemShortcut.Install()
    };

    private final Launcher mLauncher;

    /**
     * Maps launcher activity components to their list of shortcut ids.
     */
    private MultiHashMap<ComponentKey, String> mDeepShortcutMap = new MultiHashMap<>();

    public PopupDataProvider(Launcher launcher) {
        mLauncher = launcher;
    }

    public void setDeepShortcutMap(MultiHashMap<ComponentKey, String> deepShortcutMapCopy) {
        mDeepShortcutMap = deepShortcutMapCopy;
        if (LOGD) Log.d(TAG, "bindDeepShortcutMap: " + mDeepShortcutMap);
    }

    public List<String> getShortcutIdsForItem(ItemInfo info) {

        ComponentName component = info.getTargetComponent();
        if (component == null) {
            return Collections.EMPTY_LIST;
        }

        List<String> ids = mDeepShortcutMap.get(new ComponentKey(component, info.user));
        return ids == null ? Collections.EMPTY_LIST : ids;
    }


    public @NonNull
    List<SystemShortcut> getEnabledSystemShortcutsForItem(ItemInfo info) {
        return new ArrayList<>(Arrays.asList(SYSTEM_SHORTCUTS));
    }

}
