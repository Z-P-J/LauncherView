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
package com.ark.browser.launcher.demo.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;

import com.android.launcher3.Insettable;
import com.android.launcher3.LauncherLayout;
import com.android.launcher3.LauncherManager;
import com.android.launcher3.dragndrop.DragOptions;
import com.ark.browser.launcher.demo.R;
import com.ark.browser.launcher.demo.views.TopRoundedCornerView;

/**
 * Popup for showing the full list of available widgets
 */
public class WidgetsFullSheet extends BaseWidgetSheet
        implements Insettable {

    private static final long DEFAULT_OPEN_DURATION = 267;
    private static final long FADE_IN_DURATION = 150;
    private static final float VERTICAL_START_POSITION = 0.3f;

    private final Rect mInsets = new Rect();

    private View ivTest;

    public WidgetsFullSheet(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public WidgetsFullSheet(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mContent = findViewById(R.id.container);
        ivTest = findViewById(R.id.iv_test);
        ivTest.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return beginDraggingWidget(ivTest);
            }
        });
    }

    private boolean beginDraggingWidget(View v) {

        int[] loc = new int[2];
        mLauncher.getDragLayer().getLocationInDragLayer(v, loc);
        new PendingItemDragHelper(v).startDrag(
                new Rect(0, 0, v.getWidth(), v.getHeight()), v.getWidth(), v.getWidth(),
                new Point(loc[0], loc[1]), this, new DragOptions());
        close(true);
        return true;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    public void setInsets(Rect insets) {
        mInsets.set(insets);

        if (insets.bottom > 0) {
            setupNavBarColor();
        } else {
            clearNavBarColor();
        }

        ((TopRoundedCornerView) mContent).setNavBarScrimHeight(mInsets.bottom);
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthUsed;
        if (mInsets.bottom > 0) {
            widthUsed = 0;
        } else {
            Rect padding = LauncherManager.getDeviceProfile().workspacePadding;
            widthUsed = Math.max(padding.left + padding.right,
                    2 * (mInsets.left + mInsets.right));
        }

        int heightUsed = mInsets.top + LauncherManager.getDeviceProfile().edgeMarginPx;
        measureChildWithMargins(mContent, widthMeasureSpec,
                widthUsed, heightMeasureSpec, heightUsed);
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec),
                MeasureSpec.getSize(heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = r - l;
        int height = b - t;

        // Content is laid out as center bottom aligned
        int contentWidth = mContent.getMeasuredWidth();
        int contentLeft = (width - contentWidth) / 2;
        mContent.layout(contentLeft, height - mContent.getMeasuredHeight(),
                contentLeft + contentWidth, height);

        setTranslationShift(mTranslationShift);
    }

    private void open(boolean animate) {
        if (animate) {
            if (mLauncher.getDragLayer().getInsets().bottom > 0) {
                mContent.setAlpha(0);
                setTranslationShift(VERTICAL_START_POSITION);
            }
            mOpenCloseAnimator.setValues(
                    PropertyValuesHolder.ofFloat(TRANSLATION_SHIFT, TRANSLATION_SHIFT_OPENED));
            mOpenCloseAnimator
                    .setDuration(DEFAULT_OPEN_DURATION)
                    .setInterpolator(AnimationUtils.loadInterpolator(
                            getContext(), android.R.interpolator.linear_out_slow_in));
            mOpenCloseAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mOpenCloseAnimator.removeListener(this);
                }
            });
            post(() -> {
                mOpenCloseAnimator.start();
                mContent.animate().alpha(1).setDuration(FADE_IN_DURATION);
            });
        } else {
            setTranslationShift(TRANSLATION_SHIFT_OPENED);
        }
    }

    @Override
    protected void handleClose(boolean animate) {
        handleClose(animate, DEFAULT_OPEN_DURATION);
    }

    @Override
    protected boolean isOfType(int type) {
        return (type & TYPE_WIDGETS_FULL_SHEET) != 0;
    }

    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent ev) {
        return super.onControllerInterceptTouchEvent(ev);
    }

    public static WidgetsFullSheet show(LauncherLayout launcher, boolean animate) {
        WidgetsFullSheet sheet = (WidgetsFullSheet) LayoutInflater.from(launcher.getContext())
                .inflate(R.layout.widgets_full_sheet, launcher.getDragLayer(), false);
        sheet.mLauncher = launcher;
        sheet.mIsOpen = true;
        launcher.getDragLayer().addView(sheet);
        sheet.open(animate);
        return sheet;
    }

    public static WidgetsFullSheet show(boolean animate) {
        LauncherLayout launcher = LauncherManager.getLauncherLayout();
        WidgetsFullSheet sheet = (WidgetsFullSheet) LayoutInflater.from(launcher.getContext())
                .inflate(R.layout.widgets_full_sheet, launcher.getDragLayer(), false);
        sheet.mLauncher = launcher;
        sheet.mIsOpen = true;
        launcher.getDragLayer().addView(sheet);
        sheet.open(animate);
        return sheet;
    }

}
