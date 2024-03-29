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
package com.android.launcher3.states;

import static com.android.launcher3.util.LauncherAnimUtils.SPRING_LOADED_TRANSITION_MS;
import static com.android.launcher3.states.RotationHelper.REQUEST_LOCK;

import android.graphics.Rect;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.LauncherLayout;
import com.android.launcher3.LauncherManager;
import com.android.launcher3.LauncherState;
import com.android.launcher3.Workspace;

/**
 * Definition for spring loaded state used during drag and drop.
 */
public class EditModeState extends LauncherState {

    private static final int STATE_FLAGS = FLAG_MULTI_PAGE |
            FLAG_DISABLE_ACCESSIBILITY | FLAG_DISABLE_RESTORE | FLAG_WORKSPACE_ICONS_CAN_BE_DRAGGED |
            FLAG_DISABLE_PAGE_CLIPPING | FLAG_PAGE_BACKGROUNDS | FLAG_HIDE_BACK_BUTTON;

    public EditModeState(int id) {
        super(id, SPRING_LOADED_TRANSITION_MS, STATE_FLAGS);
    }

    @Override
    public float[] getWorkspaceScaleAndTranslation(LauncherLayout launcher) {
        DeviceProfile grid = LauncherManager.getDeviceProfile();
        Workspace ws = launcher.getWorkspace();
        if (ws.getChildCount() == 0) {
            return super.getWorkspaceScaleAndTranslation(launcher);
        }

        if (grid.isVerticalBarLayout()) {
//            float scale = grid.workspaceSpringLoadShrinkFactor;
            float scale = 0.76f;
            return new float[]{scale, 0, 0};
        }

//        float scale = grid.workspaceSpringLoadShrinkFactor;
        float scale = 0.76f;
        Rect insets = launcher.getDragLayer().getInsets();

        float scaledHeight = scale * ws.getNormalChildHeight();
        float shrunkTop = insets.top + grid.dropTargetBarSizePx;
        float shrunkBottom = ws.getMeasuredHeight() - insets.bottom
                - grid.workspacePadding.bottom
                - grid.workspaceSpringLoadedBottomSpace;
        float totalShrunkSpace = shrunkBottom - shrunkTop;

        float desiredCellTop = shrunkTop + (totalShrunkSpace - scaledHeight) / 2;

        float halfHeight = ws.getHeight() / 2f;
        float myCenter = ws.getTop() + halfHeight;
        float cellTopFromCenter = halfHeight - ws.getChildAt(0).getTop();
        float actualCellTop = myCenter - cellTopFromCenter * scale;
        return new float[]{scale, 0, (desiredCellTop - actualCellTop) / scale};
    }

    @Override
    public void onStateEnabled(LauncherLayout launcher) {
        Workspace ws = launcher.getWorkspace();
        ws.showPageIndicatorAtCurrentScroll();
        ws.getPageIndicator().setShouldAutoHide(false);

        launcher.getRotationHelper().setCurrentStateRequest(REQUEST_LOCK);
    }

    @Override
    public float getWorkspaceScrimAlpha(LauncherLayout launcher) {
        return 0.48f;
    }

    @Override
    public void onStateDisabled(final LauncherLayout launcher) {
        launcher.getWorkspace().getPageIndicator().setShouldAutoHide(true);
    }
}
