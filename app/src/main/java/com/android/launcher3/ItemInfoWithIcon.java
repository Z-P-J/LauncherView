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

package com.android.launcher3;

import android.graphics.Bitmap;

/**
 * Represents an ItemInfo which also holds an icon.
 */
public abstract class ItemInfoWithIcon extends ItemInfo {

    /**
     * A bitmap version of the application icon.
     */
    public Bitmap iconBitmap;

    /**
     * Dominant color in the {@link #iconBitmap}.
     */
    public int iconColor;

    /**
     * Indicates whether we're using a low res icon
     */
    public boolean usingLowResIcon;

    /**
     * Status associated with the system state of the underlying item. This is calculated every
     * time a new info is created and not persisted on the disk.
     */
    public int runtimeStatusFlags = 0;

    protected ItemInfoWithIcon() { }

    protected ItemInfoWithIcon(ItemInfoWithIcon info) {
        super(info);
        iconBitmap = info.iconBitmap;
        iconColor = info.iconColor;
        usingLowResIcon = info.usingLowResIcon;
        runtimeStatusFlags = info.runtimeStatusFlags;
    }

    public void setIconBitmap(Bitmap iconBitmap) {
        this.iconBitmap = iconBitmap;
    }

    @Override
    public boolean isDisabled() {
        return false;
    }
}
