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

import android.view.animation.Interpolator;

import com.android.launcher3.states.EditModeState;
import com.android.launcher3.states.SpringLoadedState;
import com.android.launcher3.uioverrides.AllAppsState;
import com.android.launcher3.uioverrides.OverviewState;

import java.util.Arrays;

import static com.android.launcher3.anim.Interpolators.ACCEL_2;
import static com.android.launcher3.states.RotationHelper.REQUEST_NONE;


/**
 * Base state for various states used for the Launcher
 */
public class LauncherState {


    /**
     * Set of elements indicating various workspace elements which change visibility across states
     * Note that workspace is not included here as in that case, we animate individual pages
     */
    public static final int NONE = 0;
    public static final int HOTSEAT_ICONS = 1;
    public static final int HOTSEAT_SEARCH_BOX = 1 << 1;
    public static final int ALL_APPS_HEADER = 1 << 2;
    public static final int ALL_APPS_HEADER_EXTRA = 1 << 3; // e.g. app predictions
    public static final int ALL_APPS_CONTENT = 1 << 4;
    public static final int VERTICAL_SWIPE_INDICATOR = 1 << 5;

    protected static final int FLAG_MULTI_PAGE = 1;
    protected static final int FLAG_DISABLE_ACCESSIBILITY = 1 << 1;
    protected static final int FLAG_DISABLE_RESTORE = 1 << 2;
    protected static final int FLAG_WORKSPACE_ICONS_CAN_BE_DRAGGED = 1 << 3;
    protected static final int FLAG_DISABLE_PAGE_CLIPPING = 1 << 4;
    protected static final int FLAG_PAGE_BACKGROUNDS = 1 << 5;
    protected static final int FLAG_DISABLE_INTERACTION = 1 << 6;
    protected static final int FLAG_OVERVIEW_UI = 1 << 7;
    protected static final int FLAG_HIDE_BACK_BUTTON = 1 << 8;
    protected static final int FLAG_HAS_SYS_UI_SCRIM = 1 << 9;

    protected static final PageAlphaProvider DEFAULT_ALPHA_PROVIDER =
            new PageAlphaProvider(ACCEL_2) {
                @Override
                public float getPageAlpha(int pageIndex) {
                    return 1;
                }
            };

    private static final LauncherState[] sAllStates = new LauncherState[5];

    /**
     * TODO: Create a separate class for NORMAL state.
     */
    public static final LauncherState NORMAL = new LauncherState(0, 0,
            FLAG_DISABLE_RESTORE | FLAG_WORKSPACE_ICONS_CAN_BE_DRAGGED | FLAG_HIDE_BACK_BUTTON |
                    FLAG_HAS_SYS_UI_SCRIM);

    /**
     * Various Launcher states arranged in the increasing order of UI layers
     */
    public static final LauncherState SPRING_LOADED = new SpringLoadedState(1);
    public static final LauncherState OVERVIEW = new OverviewState(2);
    public static final LauncherState EDIT_MODE = new EditModeState(3);
    public static final LauncherState ALL_APPS = new AllAppsState(4);
//    public static final LauncherState OPTIONS = new OptionsState(5);

    public final int ordinal;

    /**
     * True if the state can be persisted across activity restarts.
     */
    public final boolean disableRestore;

    /**
     * True if workspace has multiple pages visible.
     */
    public final boolean hasMultipleVisiblePages;

    /**
     * Properties related to state transition animation
     *
     * @see WorkspaceStateTransitionAnimation
     */
    public final boolean hasWorkspacePageBackground;

    public final int transitionDuration;

    /**
     * True if the state allows workspace icons to be dragged.
     */
    public final boolean workspaceIconsCanBeDragged;

    /**
     * True if the workspace pages should not be clipped relative to the workspace bounds
     * for this state.
     */
    public final boolean disablePageClipping;

    /**
     * True if launcher can not be directly interacted in this state;
     */
    public final boolean disableInteraction;

    /**
     * True if the state has overview panel visible.
     */
    public final boolean overviewUi;

    /**
     * True if the back button should be hidden when in this state (assuming no floating views are
     * open, launcher has window focus, etc).
     */
    public final boolean hideBackButton;

    public final boolean hasSysUiScrim;

    public LauncherState(int id, int transitionDuration, int flags) {
        this.transitionDuration = transitionDuration;

        this.hasWorkspacePageBackground = (flags & FLAG_PAGE_BACKGROUNDS) != 0;
        this.hasMultipleVisiblePages = (flags & FLAG_MULTI_PAGE) != 0;
        this.disableRestore = (flags & FLAG_DISABLE_RESTORE) != 0;
        this.workspaceIconsCanBeDragged = (flags & FLAG_WORKSPACE_ICONS_CAN_BE_DRAGGED) != 0;
        this.disablePageClipping = (flags & FLAG_DISABLE_PAGE_CLIPPING) != 0;
        this.disableInteraction = (flags & FLAG_DISABLE_INTERACTION) != 0;
        this.overviewUi = (flags & FLAG_OVERVIEW_UI) != 0;
        this.hideBackButton = (flags & FLAG_HIDE_BACK_BUTTON) != 0;
        this.hasSysUiScrim = (flags & FLAG_HAS_SYS_UI_SCRIM) != 0;

        this.ordinal = id;
        sAllStates[id] = this;
    }

    public static LauncherState[] values() {
        return Arrays.copyOf(sAllStates, sAllStates.length);
    }

    public float[] getWorkspaceScaleAndTranslation(LauncherLayout launcher) {
        return new float[]{1, 0, 0};
    }

    /**
     * Returns 2 floats designating how to transition overview:
     * scale for the current and adjacent pages
     * translationY factor where 0 is top aligned and 0.5 is centered vertically
     */
    public float[] getOverviewScaleAndTranslationYFactor(LauncherLayout launcher) {
        return new float[]{1.1f, 0f};
    }

    public void onStateEnabled(LauncherLayout launcher) {
        dispatchWindowStateChanged(launcher);
    }

    public void onStateDisabled(LauncherLayout launcher) {
    }

    public int getVisibleElements(LauncherLayout launcher) {
        if (LauncherManager.getDeviceProfile().isVerticalBarLayout()) {
            return HOTSEAT_ICONS | VERTICAL_SWIPE_INDICATOR;
        }
        return HOTSEAT_ICONS | HOTSEAT_SEARCH_BOX | VERTICAL_SWIPE_INDICATOR;
    }

    /**
     * Fraction shift in the vertical translation UI and related properties
     *
     */
    public float getVerticalProgress(LauncherLayout launcher) {
        return 1f;
    }

    public float getWorkspaceScrimAlpha(LauncherLayout launcher) {
        return 0;
    }

    public String getDescription(LauncherLayout launcher) {
        return launcher.getWorkspace().getCurrentPageDescription();
    }

    public PageAlphaProvider getWorkspacePageAlphaProvider(LauncherLayout launcher) {
        if (this != NORMAL || !LauncherManager.getDeviceProfile().shouldFadeAdjacentWorkspaceScreens()) {
            return DEFAULT_ALPHA_PROVIDER;
        }
        final int centerPage = launcher.getWorkspace().getNextPage();
        return new PageAlphaProvider(ACCEL_2) {
            @Override
            public float getPageAlpha(int pageIndex) {
                return pageIndex != centerPage ? 0 : 1f;
            }
        };
    }

    public LauncherState getHistoryForState(LauncherState previousState) {
        // No history is supported
        return NORMAL;
    }

    /**
     * Called when the start transition ends and the user settles on this particular state.
     */
    public void onStateTransitionEnd(LauncherLayout launcher) {
        if (this == NORMAL) {
            // Clear any rotation locks when going to normal state
            launcher.getRotationHelper().setCurrentStateRequest(REQUEST_NONE);
        }
    }

    protected static void dispatchWindowStateChanged(LauncherLayout launcher) {

    }

    public static abstract class PageAlphaProvider {

        public final Interpolator interpolator;

        public PageAlphaProvider(Interpolator interpolator) {
            this.interpolator = interpolator;
        }

        public abstract float getPageAlpha(int pageIndex);
    }
}
