/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.launcher3.shortcuts;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import com.android.launcher3.BubbleTextView;
import com.ark.browser.launcher.R;
import com.android.launcher3.Utilities;

/**
 * A {@link android.widget.FrameLayout} that contains a {@link DeepShortcutView}.
 * This lets us animate the DeepShortcutView (icon and text) separately from the background.
 */
public class DeepShortcutView extends FrameLayout {

    private static final Point sTempPoint = new Point();

    private final Rect mPillRect;

    private BubbleTextView mBubbleText;
    private View mIconView;
    private View mDivider;

    public DeepShortcutView(Context context) {
        this(context, null, 0);
    }

    public DeepShortcutView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DeepShortcutView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mPillRect = new Rect();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mBubbleText = findViewById(R.id.bubble_text);
        mIconView = findViewById(R.id.icon);
        mDivider = findViewById(R.id.divider);
    }

    public void setDividerVisibility(int visibility) {
        mDivider.setVisibility(visibility);
    }

    public BubbleTextView getBubbleText() {
        return mBubbleText;
    }

    public void setWillDrawIcon(boolean willDraw) {
        mIconView.setVisibility(willDraw ? View.VISIBLE : View.INVISIBLE);
    }

    public boolean willDrawIcon() {
        return mIconView.getVisibility() == View.VISIBLE;
    }

    /**
     * Returns the position of the center of the icon relative to the container.
     */
    public Point getIconCenter() {
        sTempPoint.y = sTempPoint.x = getMeasuredHeight() / 2;
        if (Utilities.isRtl(getResources())) {
            sTempPoint.x = getMeasuredWidth() - sTempPoint.x;
        }
        return sTempPoint;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mPillRect.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
    }

//    /**
//     * Returns the shortcut info that is suitable to be added on the homescreen
//     */
//    public ShortcutInfo getFinalInfo() {
//        final ShortcutInfo badged = new ShortcutInfo(mInfo);
//        // Queue an update task on the worker thread. This ensures that the badged
//        // shortcut eventually gets its icon updated.
//        Launcher.getLauncher(getContext()).getModel()
//                .updateAndBindShortcutInfo(badged, mDetail);
//        return badged;
//    }

    public View getIconView() {
        return mIconView;
    }
}