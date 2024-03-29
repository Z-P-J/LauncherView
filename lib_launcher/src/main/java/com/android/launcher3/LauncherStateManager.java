/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.IntDef;

import com.android.launcher3.anim.AnimationSuccessListener;
import com.android.launcher3.anim.AnimatorSetBuilder;
import com.android.launcher3.anim.PropertySetter;
import com.android.launcher3.anim.PropertySetter.AnimatedPropertySetter;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static android.view.View.VISIBLE;
import static com.android.launcher3.LauncherState.NORMAL;
import static com.android.launcher3.anim.AnimatorSetBuilder.ANIM_OVERVIEW_FADE;
import static com.android.launcher3.anim.AnimatorSetBuilder.ANIM_OVERVIEW_SCALE;
import static com.android.launcher3.anim.AnimatorSetBuilder.ANIM_WORKSPACE_FADE;
import static com.android.launcher3.anim.AnimatorSetBuilder.ANIM_WORKSPACE_SCALE;
import static com.android.launcher3.anim.Interpolators.ACCEL;
import static com.android.launcher3.anim.Interpolators.DEACCEL;
import static com.android.launcher3.anim.Interpolators.DEACCEL_1_7;
import static com.android.launcher3.anim.Interpolators.OVERSHOOT_1_2;
import static com.android.launcher3.anim.Interpolators.clampToProgress;
import static com.android.launcher3.anim.PropertySetter.NO_ANIM_PROPERTY_SETTER;

/**
 * TODO: figure out what kind of tests we can write for this
 * <p>
 * Things to test when changing the following class.
 * - Home from workspace
 * - from center screen
 * - from other screens
 * - Home from all apps
 * - from center screen
 * - from other screens
 * - Back from all apps
 * - from center screen
 * - from other screens
 * - Launch app from workspace and quit
 * - with back
 * - with home
 * - Launch app from all apps and quit
 * - with back
 * - with home
 * - Go to a screen that's not the default, then all
 * apps, and launch and app, and go back
 * - with back
 * -with home
 * - On workspace, long press power and go back
 * - with back
 * - with home
 * - On all apps, long press power and go back
 * - with back
 * - with home
 * - On workspace, power off
 * - On all apps, power off
 * - Launch an app and turn off the screen while in that app
 * - Go back with home key
 * - Go back with back key  TODO: make this not go to workspace
 * - From all apps
 * - From workspace
 * - Enter and exit car mode (becase it causes an extra configuration changed)
 * - From all apps
 * - From the center workspace
 * - From another workspace
 */
public class LauncherStateManager {

    public static final String TAG = "StateManager";

    // We separate the state animations into "atomic" and "non-atomic" components. The atomic
    // components may be run atomically - that is, all at once, instead of user-controlled. However,
    // atomic components are not restricted to this purpose; they can be user-controlled alongside
    // non atomic components as well.
    @IntDef(flag = true, value = {
            NON_ATOMIC_COMPONENT,
            ATOMIC_COMPONENT
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface AnimationComponents {
    }

    public static final int NON_ATOMIC_COMPONENT = 1 << 0;
    public static final int ATOMIC_COMPONENT = 1 << 1;

    public static final int ANIM_ALL = NON_ATOMIC_COMPONENT | ATOMIC_COMPONENT;

    private final AnimationConfig mConfig = new AnimationConfig();
    private final Handler mUiHandler;
    private final LauncherLayout mLauncher;

    private LauncherState mState = NORMAL;

    private LauncherState mLastStableState = NORMAL;
    private LauncherState mCurrentStableState = NORMAL;

    private LauncherState mRestState;

    public LauncherStateManager(LauncherLayout l) {
        mUiHandler = new Handler(Looper.getMainLooper());
        mLauncher = l;
    }

    public LauncherState getState() {
        return mState;
    }

    /**
     * @see #goToState(LauncherState, boolean)
     */
    public void goToState(LauncherState state) {
        goToState(state, true);
    }

    /**
     * Changes the Launcher state to the provided state after the given delay.
     */
    public void goToState(LauncherState state, long delay, Runnable onCompleteRunnable) {
        goToState(state, true, delay, onCompleteRunnable);
    }

    /**
     * @see #goToState(LauncherState, boolean)
     */
    public void goToState(LauncherState state, boolean animated) {
        goToState(state, animated, 0, null);
    }

    /**
     * Changes the Launcher state to the provided state after the given delay.
     */
    public void goToState(LauncherState state, long delay) {
        goToState(state, true, delay, null);
    }

    public void reapplyState() {
        reapplyState(false);
    }

    public void reapplyState(boolean cancelCurrentAnimation) {
        if (cancelCurrentAnimation) {
            cancelAnimation();
        }
        if (mConfig.mCurrentAnimation == null) {
            mLauncher.getWorkspace().setState(mState);
        }
    }

    private void goToState(LauncherState state, boolean animated, long delay,
                           final Runnable onCompleteRunnable) {
        if (mLauncher.isInState(state)) {
            if (mConfig.mCurrentAnimation == null) {
                // Run any queued runnable
                if (onCompleteRunnable != null) {
                    onCompleteRunnable.run();
                }
                return;
            } else if (!mConfig.userControlled && animated && mConfig.mTargetState == state) {
                // We are running the same animation as requested
                if (onCompleteRunnable != null) {
                    mConfig.mCurrentAnimation.addListener(new AnimationSuccessListener() {
                        @Override
                        public void onAnimationSuccess(Animator animator) {
                            onCompleteRunnable.run();
                        }
                    });
                }
                return;
            }
        }

        // Cancel the current animation. This will reset mState to mCurrentStableState, so store it.
        LauncherState fromState = mState;
        mConfig.reset();

        if (!animated) {
            onStateTransitionStart(state);
            mLauncher.getWorkspace().setState(mState);

            onStateTransitionEnd(state);

            // Run any queued runnable
            if (onCompleteRunnable != null) {
                onCompleteRunnable.run();
            }

            return;
        }

        // Since state NORMAL can be reached from multiple states, just assume that the
        // transition plays in reverse and use the same duration as previous state.
        mConfig.duration = state == NORMAL ? fromState.transitionDuration : state.transitionDuration;

        AnimatorSetBuilder builder = new AnimatorSetBuilder();
        prepareForAtomicAnimation(fromState, state, builder);
        AnimatorSet animation = createAnimationToNewWorkspaceInternal(
                state, builder, onCompleteRunnable);
        Runnable runnable = new StartAnimRunnable(animation);
        if (delay > 0) {
            mUiHandler.postDelayed(runnable, delay);
        } else {
            mUiHandler.post(runnable);
        }
    }

    /**
     * Prepares for a non-user controlled animation from fromState to toState. Preparations include:
     * - Setting interpolators for various animations included in the state transition.
     * - Setting some start values (e.g. scale) for views that are hidden but about to be shown.
     */
    public void prepareForAtomicAnimation(LauncherState fromState, LauncherState toState,
                                          AnimatorSetBuilder builder) {
        if (fromState == NORMAL && toState.overviewUi) {
            builder.setInterpolator(ANIM_WORKSPACE_SCALE, OVERSHOOT_1_2);
            builder.setInterpolator(ANIM_WORKSPACE_FADE, OVERSHOOT_1_2);
            builder.setInterpolator(ANIM_OVERVIEW_SCALE, OVERSHOOT_1_2);
            builder.setInterpolator(ANIM_OVERVIEW_FADE, OVERSHOOT_1_2);
        } else if (fromState.overviewUi && toState == NORMAL) {
            builder.setInterpolator(ANIM_WORKSPACE_SCALE, DEACCEL);
            builder.setInterpolator(ANIM_WORKSPACE_FADE, ACCEL);
            builder.setInterpolator(ANIM_OVERVIEW_SCALE, clampToProgress(ACCEL, 0, 0.9f));
            builder.setInterpolator(ANIM_OVERVIEW_FADE, DEACCEL_1_7);
            Workspace workspace = mLauncher.getWorkspace();

            // Start from a higher workspace scale, but only if we're invisible so we don't jump.
            boolean isWorkspaceVisible = workspace.getVisibility() == VISIBLE;
            if (isWorkspaceVisible) {
                CellLayout currentChild = (CellLayout) workspace.getChildAt(
                        workspace.getCurrentPage());
                isWorkspaceVisible = currentChild.getVisibility() == VISIBLE
                        && currentChild.getShortcutsAndWidgets().getAlpha() > 0;
            }
            if (!isWorkspaceVisible) {
                workspace.setScaleX(0.92f);
                workspace.setScaleY(0.92f);
            }
        }
    }

    protected AnimatorSet createAnimationToNewWorkspaceInternal(final LauncherState state,
                                                                AnimatorSetBuilder builder,
                                                                final Runnable onCompleteRunnable) {
        Workspace workspace = mLauncher.getWorkspace();
        builder.startTag(workspace);
        workspace.setStateWithAnimation(state, builder, mConfig);

        final AnimatorSet animation = builder.build();
        animation.addListener(new AnimationSuccessListener() {

            @Override
            public void onAnimationStart(Animator animation) {
                // Change the internal state only when the transition actually starts
                onStateTransitionStart(state);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                mState = mCurrentStableState;
            }

            @Override
            public void onAnimationSuccess(Animator animator) {
                // Run any queued runnables
                if (onCompleteRunnable != null) {
                    onCompleteRunnable.run();
                }
                onStateTransitionEnd(state);
            }
        });
        mConfig.setAnimation(animation, state);
        return mConfig.mCurrentAnimation;
    }

    private void onStateTransitionStart(LauncherState state) {
        mState.onStateDisabled(mLauncher);
        mState = state;
        mState.onStateEnabled(mLauncher);

        if (state.disablePageClipping) {
            // Only disable clipping if needed, otherwise leave it as previous value.
            mLauncher.getWorkspace().setClipChildren(false);
        }
    }

    private void onStateTransitionEnd(LauncherState state) {
        // Only change the stable states after the transitions have finished
        if (state != mCurrentStableState) {
            mLastStableState = state.getHistoryForState(mCurrentStableState);
            mCurrentStableState = state;
        }

        state.onStateTransitionEnd(mLauncher);
        mLauncher.getWorkspace().setClipChildren(!state.disablePageClipping);
        LauncherManager.finishAutoCancelActionMode();

        if (state == NORMAL) {
            setRestState(null);
        }

        mLauncher.getDragLayer().requestFocus();
    }

    public void onWindowFocusChanged() {

    }

    public LauncherState getLastState() {
        return mLastStableState;
    }

    public void moveToRestState() {
        if (mConfig.mCurrentAnimation != null && mConfig.userControlled) {
            // The user is doing something. Lets not mess it up
            return;
        }
        if (mState.disableRestore) {
            goToState(getRestState());
            // Reset history
            mLastStableState = NORMAL;
        }
    }

    public LauncherState getRestState() {
        return mRestState == null ? NORMAL : mRestState;
    }

    public void setRestState(LauncherState restState) {
        mRestState = restState;
    }

    /**
     * Cancels the current animation.
     */
    public void cancelAnimation() {
        mConfig.reset();
    }

    private void clearCurrentAnimation() {
        if (mConfig.mCurrentAnimation != null) {
            mConfig.mCurrentAnimation.removeListener(mConfig);
            mConfig.mCurrentAnimation = null;
        }
    }

    private class StartAnimRunnable implements Runnable {

        private final AnimatorSet mAnim;

        public StartAnimRunnable(AnimatorSet anim) {
            mAnim = anim;
        }

        @Override
        public void run() {
            if (mConfig.mCurrentAnimation != mAnim) {
                return;
            }
            mAnim.start();
        }
    }

    public static class AnimationConfig extends AnimatorListenerAdapter {
        public long duration;
        public boolean userControlled;
        public @AnimationComponents
        int animComponents = ANIM_ALL;
        private PropertySetter mPropertySetter;

        private AnimatorSet mCurrentAnimation;
        private LauncherState mTargetState;

        /**
         * Cancels the current animation and resets config variables.
         */
        public void reset() {
            duration = 0;
            userControlled = false;
            animComponents = ANIM_ALL;
            mPropertySetter = null;
            mTargetState = null;

            if (mCurrentAnimation != null) {
                mCurrentAnimation.setDuration(0);
                mCurrentAnimation.cancel();
            }

            mCurrentAnimation = null;
        }

        public PropertySetter getPropertySetter(AnimatorSetBuilder builder) {
            if (mPropertySetter == null) {
                mPropertySetter = duration == 0 ? NO_ANIM_PROPERTY_SETTER
                        : new AnimatedPropertySetter(duration, builder);
            }
            return mPropertySetter;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (mCurrentAnimation == animation) {
                mCurrentAnimation = null;
            }
        }

        public void setAnimation(AnimatorSet animation, LauncherState targetState) {
            mCurrentAnimation = animation;
            mTargetState = targetState;
            mCurrentAnimation.addListener(this);
        }

        public boolean playAtomicComponent() {
            return (animComponents & ATOMIC_COMPONENT) != 0;
        }

        public boolean playNonAtomicComponent() {
            return (animComponents & NON_ATOMIC_COMPONENT) != 0;
        }
    }

    public interface StateHandler {

        /**
         * Updates the UI to {@param state} without any animations
         */
        void setState(LauncherState state);

        /**
         * Sets the UI to {@param state} by animating any changes.
         */
        void setStateWithAnimation(LauncherState toState,
                                   AnimatorSetBuilder builder, AnimationConfig config);
    }

}
