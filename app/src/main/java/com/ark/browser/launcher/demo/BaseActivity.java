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

package com.ark.browser.launcher.demo;

import android.content.Context;
import android.content.ContextWrapper;
import androidx.annotation.IntDef;

import com.android.launcher3.DeviceProfile.OnDeviceProfileChangeListener;
import com.android.launcher3.util.SystemUiController;
import com.zpj.fragmentation.SupportActivity;

import java.lang.annotation.Retention;
import java.util.ArrayList;

import static com.android.launcher3.util.SystemUiController.UI_STATE_OVERVIEW;
import static java.lang.annotation.RetentionPolicy.SOURCE;

public abstract class BaseActivity extends SupportActivity {

    public static final int INVISIBLE_BY_STATE_HANDLER = 1;
    public static final int INVISIBLE_BY_APP_TRANSITIONS = 1 << 1;
    public static final int INVISIBLE_ALL =
            INVISIBLE_BY_STATE_HANDLER | INVISIBLE_BY_APP_TRANSITIONS;

    @Retention(SOURCE)
    @IntDef(
            flag = true,
            value = {INVISIBLE_BY_STATE_HANDLER, INVISIBLE_BY_APP_TRANSITIONS})
    public @interface InvisibilityFlags {
    }

    private static final int ACTIVITY_STATE_STARTED = 1;
    private static final int ACTIVITY_STATE_RESUMED = 1 << 1;
    /**
     * State flag indicating if the user is active or the actitvity when to background as a result
     * of user action.
     */
    private static final int ACTIVITY_STATE_USER_ACTIVE = 1 << 2;

    @Retention(SOURCE)
    @IntDef(
            flag = true,
            value = {ACTIVITY_STATE_STARTED, ACTIVITY_STATE_RESUMED, ACTIVITY_STATE_USER_ACTIVE})
    public @interface ActivityFlags {
    }

    @ActivityFlags
    private int mActivityFlags;

    // When the recents animation is running, the visibility of the Launcher is managed by the
    // animation
    @InvisibilityFlags
    private int mForceInvisible;

    @Override
    protected void onStart() {
        mActivityFlags |= ACTIVITY_STATE_STARTED;
        super.onStart();
    }

    @Override
    protected void onResume() {
        mActivityFlags |= ACTIVITY_STATE_RESUMED | ACTIVITY_STATE_USER_ACTIVE;
        super.onResume();
    }

    @Override
    protected void onUserLeaveHint() {
        mActivityFlags &= ~ACTIVITY_STATE_USER_ACTIVE;
        super.onUserLeaveHint();
    }

    @Override
    protected void onStop() {
        mActivityFlags &= ~ACTIVITY_STATE_STARTED & ~ACTIVITY_STATE_USER_ACTIVE;
        mForceInvisible = 0;
        super.onStop();
    }

    @Override
    protected void onPause() {
        mActivityFlags &= ~ACTIVITY_STATE_RESUMED;
        super.onPause();
    }

    public boolean isStarted() {
        return (mActivityFlags & ACTIVITY_STATE_STARTED) != 0;
    }


    /**
     * @return Wether this activity should be considered invisible regardless of actual visibility.
     */
    public boolean isForceInvisible() {
        return mForceInvisible != 0;
    }

}
