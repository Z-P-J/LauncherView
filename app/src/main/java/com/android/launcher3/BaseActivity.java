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

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.support.annotation.IntDef;

import com.android.launcher3.DeviceProfile.OnDeviceProfileChangeListener;
import com.android.launcher3.util.SystemUiController;

import java.lang.annotation.Retention;
import java.util.ArrayList;

import static com.android.launcher3.util.SystemUiController.UI_STATE_OVERVIEW;
import static java.lang.annotation.RetentionPolicy.SOURCE;

public abstract class BaseActivity extends Activity {

    public static final int INVISIBLE_BY_STATE_HANDLER = 1 << 0;
    public static final int INVISIBLE_BY_APP_TRANSITIONS = 1 << 1;
    public static final int INVISIBLE_ALL =
            INVISIBLE_BY_STATE_HANDLER | INVISIBLE_BY_APP_TRANSITIONS;

    @Retention(SOURCE)
    @IntDef(
            flag = true,
            value = {INVISIBLE_BY_STATE_HANDLER, INVISIBLE_BY_APP_TRANSITIONS})
    public @interface InvisibilityFlags {
    }

    private final ArrayList<OnDeviceProfileChangeListener> mDPChangeListeners = new ArrayList<>();

    protected DeviceProfile mDeviceProfile;
    protected SystemUiController mSystemUiController;

    private static final int ACTIVITY_STATE_STARTED = 1 << 0;
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

    public DeviceProfile getDeviceProfile() {
        return mDeviceProfile;
    }

    public boolean isInMultiWindowModeCompat() {
        return Utilities.ATLEAST_NOUGAT && isInMultiWindowMode();
    }

    public static BaseActivity fromContext(Context context) {
        if (context instanceof BaseActivity) {
            return (BaseActivity) context;
        }
        return ((BaseActivity) ((ContextWrapper) context).getBaseContext());
    }

    public SystemUiController getSystemUiController() {
        if (mSystemUiController == null) {
            mSystemUiController = new SystemUiController(getWindow());
        }
        return mSystemUiController;
    }

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
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode, Configuration newConfig) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig);
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

        // Reset the overridden sysui flags used for the task-swipe launch animation, we do this
        // here instead of at the end of the animation because the start of the new activity does
        // not happen immediately, which would cause us to reset to launcher's sysui flags and then
        // back to the new app (causing a flash)
        getSystemUiController().updateUiState(UI_STATE_OVERVIEW, 0);
    }

    public boolean isStarted() {
        return (mActivityFlags & ACTIVITY_STATE_STARTED) != 0;
    }

    /**
     * isResumed in already defined as a hidden final method in Activity.java
     */
    public boolean hasBeenResumed() {
        return (mActivityFlags & ACTIVITY_STATE_RESUMED) != 0;
    }

    public void addOnDeviceProfileChangeListener(OnDeviceProfileChangeListener listener) {
        mDPChangeListeners.add(listener);
    }

    protected void dispatchDeviceProfileChanged() {
        for (int i = mDPChangeListeners.size() - 1; i >= 0; i--) {
            mDPChangeListeners.get(i).onDeviceProfileChanged(mDeviceProfile);
        }
    }


    /**
     * @return Wether this activity should be considered invisible regardless of actual visibility.
     */
    public boolean isForceInvisible() {
        return mForceInvisible != 0;
    }

}
