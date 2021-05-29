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

package com.android.launcher3.graphics;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Region;
import android.support.v4.graphics.ColorUtils;
import android.util.Property;
import android.view.View;

import com.android.launcher3.CellLayout;
import com.android.launcher3.LauncherActivity;
import com.android.launcher3.Workspace;

/**
 * View scrim which draws behind hotseat and workspace
 */
public class WorkspaceAndHotseatScrim {

    public static Property<WorkspaceAndHotseatScrim, Float> SCRIM_PROGRESS =
            new Property<WorkspaceAndHotseatScrim, Float>(Float.TYPE, "scrimProgress") {
                @Override
                public Float get(WorkspaceAndHotseatScrim scrim) {
                    return scrim.mScrimProgress;
                }

                @Override
                public void set(WorkspaceAndHotseatScrim scrim, Float value) {
                    scrim.setScrimProgress(value);
                }
            };

    private final Rect mHighlightRect = new Rect();
    private final LauncherActivity mLauncher;
    private final View mRoot;

    private Workspace mWorkspace;

    private float mScrimProgress;
    private int mScrimAlpha = 0;

    public WorkspaceAndHotseatScrim(View view) {
        mRoot = view;
        mLauncher = LauncherActivity.fromContext(view);
    }

    public void setWorkspace(Workspace workspace) {
        mWorkspace = workspace;
    }

    public void draw(Canvas canvas) {
        // Draw the background below children.
        if (mScrimAlpha > 0) {
            // Update the scroll position first to ensure scrim cutout is in the right place.
            mWorkspace.computeScrollWithoutInvalidation();
            CellLayout currCellLayout = mWorkspace.getCurrentDragOverlappingLayout();
            canvas.save();
            if (currCellLayout != null && currCellLayout != mLauncher.getLauncherLayout().getHotseat().getLayout()) {
                // Cut a hole in the darkening scrim on the page that should be highlighted, if any.
                mLauncher.getDragLayer()
                        .getDescendantRectRelativeToSelf(currCellLayout, mHighlightRect);
                canvas.clipRect(mHighlightRect, Region.Op.DIFFERENCE);
            }

            canvas.drawColor(ColorUtils.setAlphaComponent(Color.TRANSPARENT, mScrimAlpha));
            canvas.restore();
        }

    }

    private void setScrimProgress(float progress) {
        if (mScrimProgress != progress) {
            mScrimProgress = progress;
            mScrimAlpha = Math.round(255 * mScrimProgress);
            invalidate();
        }
    }






    public void invalidate() {
        mRoot.invalidate();
    }


}
