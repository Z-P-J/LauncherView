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
package com.android.launcher3.uioverrides;

import static com.android.launcher3.util.LauncherAnimUtils.ALL_APPS_TRANSITION_MS;
import static com.android.launcher3.anim.Interpolators.DEACCEL_2;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.LauncherLayout;
import com.android.launcher3.LauncherManager;
import com.android.launcher3.LauncherState;
import com.ark.browser.launcher.R;

/**
 * Definition for AllApps state
 */
public class AllAppsState extends LauncherState {

    private static final float PARALLAX_COEFFICIENT = .125f;

    private static final int STATE_FLAGS = FLAG_DISABLE_ACCESSIBILITY;

    private static final PageAlphaProvider PAGE_ALPHA_PROVIDER = new PageAlphaProvider(DEACCEL_2) {
        @Override
        public float getPageAlpha(int pageIndex) {
            return 0;
        }
    };

    public AllAppsState(int id) {
        super(id, ALL_APPS_TRANSITION_MS, STATE_FLAGS);
    }

    @Override
    public void onStateEnabled(LauncherLayout launcher) {
        AbstractFloatingView.closeAllOpenViews();
        dispatchWindowStateChanged(launcher);
    }

    @Override
    public String getDescription(LauncherLayout launcher) {
        return launcher.getContext().getString(R.string.all_apps_button_label);
    }

    @Override
    public int getVisibleElements(LauncherLayout launcher) {
        return ALL_APPS_HEADER | ALL_APPS_CONTENT;
    }

    @Override
    public float[] getWorkspaceScaleAndTranslation(LauncherLayout launcher) {
        return new float[]{1f, 0, LauncherManager.getDeviceProfile().heightPx * PARALLAX_COEFFICIENT};
    }

    @Override
    public PageAlphaProvider getWorkspacePageAlphaProvider(LauncherLayout launcher) {
        return PAGE_ALPHA_PROVIDER;
    }

    @Override
    public float getVerticalProgress(LauncherLayout launcher) {
        return 0f;
    }
}
