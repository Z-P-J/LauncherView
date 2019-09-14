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

package com.android.launcher3.popup;

import android.animation.AnimatorSet;
import android.animation.LayoutTransition;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.DragSource;
import com.android.launcher3.DropTarget;
import com.android.launcher3.DropTarget.DragObject;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.dragndrop.DragController;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.dragndrop.DragOptions;
import com.android.launcher3.shortcuts.DeepShortcutView;
import com.android.launcher3.touch.ItemLongClickListener;

import java.util.ArrayList;
import java.util.List;

/**
 * A container for shortcuts to deep links and notifications associated with an app.
 */
@TargetApi(Build.VERSION_CODES.N)
public class PopupContainerWithArrow extends ArrowPopup implements DragSource,
        DragController.DragListener, View.OnLongClickListener,
        View.OnTouchListener {

    private final List<DeepShortcutView> mShortcuts = new ArrayList<>();
    private final PointF mInterceptTouchDown = new PointF();
    private final Point mIconLastTouchPos = new Point();

    private final int mStartDragThreshold;

    private BubbleTextView mOriginalIcon;

    private ViewGroup mSystemShortcutContainer;

    public PopupContainerWithArrow(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mStartDragThreshold = getResources().getDimensionPixelSize(
                R.dimen.deep_shortcuts_start_drag_threshold);
    }

    public PopupContainerWithArrow(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PopupContainerWithArrow(Context context) {
        this(context, null, 0);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            mInterceptTouchDown.set(ev.getX(), ev.getY());
        }
        // Stop sending touch events to deep shortcut views if user moved beyond touch slop.
        return Math.hypot(mInterceptTouchDown.x - ev.getX(), mInterceptTouchDown.y - ev.getY())
                > ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return super.onTouchEvent(ev);
    }

    @Override
    protected boolean isOfType(int type) {
        return (type & TYPE_ACTION_POPUP) != 0;
    }

    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            DragLayer dl = mLauncher.getDragLayer();
            if (!dl.isEventOverView(this, ev)) {
                close(true);

                // We let touches on the original icon go through so that users can launch
                // the app with one tap if they don't find a shortcut they want.
                return mOriginalIcon == null || !dl.isEventOverView(mOriginalIcon, ev);
            }
        }
        return false;
    }

    /**
     * Shows the notifications and deep shortcuts associated with {@param icon}.
     *
     * @return the container if shown or null.
     */
    public static PopupContainerWithArrow showForIcon(BubbleTextView icon) {
        Launcher launcher = Launcher.getLauncher(icon.getContext());
        if (getOpen(launcher) != null) {
            // There is already an items container open, so don't open this one.
            icon.clearFocus();
            return null;
        }
        ItemInfo itemInfo = (ItemInfo) icon.getTag();
//        if (!DeepShortcutManager.supportsShortcuts(itemInfo)) {
//            return null;
//        }

        PopupDataProvider popupDataProvider = launcher.getPopupDataProvider();
//        List<String> shortcutIds = popupDataProvider.getShortcutIdsForItem(itemInfo);
        List<SystemShortcut> systemShortcuts = popupDataProvider
                .getEnabledSystemShortcutsForItem(itemInfo);

        final PopupContainerWithArrow container =
                (PopupContainerWithArrow) launcher.getLayoutInflater().inflate(
                        R.layout.popup_container, launcher.getDragLayer(), false);
        container.populateAndShow(icon, new ArrayList<>(), systemShortcuts);
        return container;
    }

    @Override
    protected void onInflationComplete(boolean isReversed) {

        // Update dividers
        int count = getChildCount();
        DeepShortcutView lastView = null;
        for (int i = 0; i < count; i++) {
            View view = getChildAt(i);
            if (view.getVisibility() == VISIBLE && view instanceof DeepShortcutView) {
                if (lastView != null) {
                    lastView.setDividerVisibility(VISIBLE);
                }
                lastView = (DeepShortcutView) view;
                lastView.setDividerVisibility(INVISIBLE);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.P)
    private void populateAndShow(final BubbleTextView originalIcon, final List<String> shortcutIds, List<SystemShortcut> systemShortcuts) {
        mOriginalIcon = originalIcon;

        int viewsToFlip = getChildCount();
        mSystemShortcutContainer = this;

        if (!shortcutIds.isEmpty()) {
            for (int i = shortcutIds.size(); i > 0; i--) {
                mShortcuts.add(inflateAndAdd(R.layout.deep_shortcut, this));
            }

            if (!systemShortcuts.isEmpty()) {
                mSystemShortcutContainer = inflateAndAdd(R.layout.system_shortcut_icons, this);
                for (SystemShortcut shortcut : systemShortcuts) {
                    initializeSystemShortcut(
                            R.layout.system_shortcut_icon_only, mSystemShortcutContainer, shortcut);
                }
            }
        } else if (!systemShortcuts.isEmpty()) {
            for (SystemShortcut shortcut : systemShortcuts) {
                initializeSystemShortcut(R.layout.system_shortcut, this, shortcut);
            }
        }

        reorderAndShow(viewsToFlip);

        ItemInfo originalItemInfo = (ItemInfo) originalIcon.getTag();
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            setAccessibilityPaneTitle(getTitleForAccessibility());
        }

        mLauncher.getDragController().addDragListener(this);
        mOriginalIcon.forceHideBadge(true);

        // All views are added. Animate layout from now on.
        setLayoutTransition(new LayoutTransition());
    }

    private String getTitleForAccessibility() {
        return getContext().getString(R.string.action_deep_shortcut);
    }

    @Override
    protected void getTargetObjectLocation(Rect outPos) {
        mLauncher.getDragLayer().getDescendantRectRelativeToSelf(mOriginalIcon, outPos);
        outPos.top += mOriginalIcon.getPaddingTop();
        outPos.left += mOriginalIcon.getPaddingLeft();
        outPos.right -= mOriginalIcon.getPaddingRight();
        outPos.bottom = outPos.top + (mOriginalIcon.getIcon() != null
                ? mOriginalIcon.getIcon().getBounds().height()
                : mOriginalIcon.getHeight());
    }

    private void initializeSystemShortcut(int resId, ViewGroup container, SystemShortcut info) {
        View view = inflateAndAdd(resId, container);
        if (view instanceof DeepShortcutView) {
            // Expanded system shortcut, with both icon and text shown on white background.
            final DeepShortcutView shortcutView = (DeepShortcutView) view;
            shortcutView.getIconView().setBackgroundResource(info.iconResId);
            shortcutView.getBubbleText().setText(info.labelResId);
        } else if (view instanceof ImageView) {
            // Only the system shortcut icon shows on a gray background header.
            final ImageView shortcutIcon = (ImageView) view;
            shortcutIcon.setImageResource(info.iconResId);
            shortcutIcon.setContentDescription(getContext().getText(info.labelResId));
        }
        view.setTag(info);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                info.getOnClickListener(mLauncher,
                        (ItemInfo) mOriginalIcon.getTag()).onClick(v);
                close(true);
            }
        });
    }

    /**
     * Determines when the deferred drag should be started.
     * <p>
     * Current behavior:
     * - Start the drag if the touch passes a certain distance from the original touch down.
     */
    public DragOptions.PreDragCondition createPreDragCondition() {
        return new DragOptions.PreDragCondition() {

            @Override
            public boolean shouldStartDrag(double distanceDragged) {
                return distanceDragged > mStartDragThreshold;
            }

            @Override
            public void onPreDragStart(DropTarget.DragObject dragObject) {
                if (mIsAboveIcon) {
                    // Hide only the icon, keep the text visible.
                    mOriginalIcon.setIconVisible(false);
                    mOriginalIcon.setVisibility(VISIBLE);
                } else {
                    // Hide both the icon and text.
                    mOriginalIcon.setVisibility(INVISIBLE);
                }
            }

            @Override
            public void onPreDragEnd(DropTarget.DragObject dragObject, boolean dragStarted) {
                mOriginalIcon.setIconVisible(true);
                if (dragStarted) {
                    // Make sure we keep the original icon hidden while it is being dragged.
                    mOriginalIcon.setVisibility(INVISIBLE);
                } else {
                    if (!mIsAboveIcon) {
                        // Show the icon but keep the text hidden.
                        mOriginalIcon.setVisibility(VISIBLE);
                        mOriginalIcon.setTextVisibility(false);
                    }
                }
            }
        };
    }

    @Override
    public void onDropCompleted(View target, DragObject d, boolean success) {
    }

    @Override
    public void onDragStart(DropTarget.DragObject dragObject, DragOptions options) {
        // Either the original icon or one of the shortcuts was dragged.
        // Hide the container, but don't remove it yet because that interferes with touch events.
        mDeferContainerRemoval = true;
        animateClose();
    }

    @Override
    public void onDragEnd() {
        if (!mIsOpen) {
            if (mOpenCloseAnimator != null) {
                // Close animation is running.
                mDeferContainerRemoval = false;
            } else {
                // Close animation is not running.
                if (mDeferContainerRemoval) {
                    closeComplete();
                }
            }
        }
    }

    @Override
    protected void onCreateCloseAnimation(AnimatorSet anim) {
        // Animate original icon's text back in.
        anim.play(mOriginalIcon.createTextAlphaAnimator(true /* fadeIn */));
        mOriginalIcon.forceHideBadge(false);
    }

    @Override
    protected void closeComplete() {
        super.closeComplete();
        mOriginalIcon.setTextVisibility(mOriginalIcon.shouldTextBeVisible());
        mOriginalIcon.forceHideBadge(false);
    }

    @Override
    public boolean onTouch(View v, MotionEvent ev) {
        // Touched a shortcut, update where it was touched so we can drag from there on long click.
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                mIconLastTouchPos.set((int) ev.getX(), (int) ev.getY());
                break;
        }
        return false;
    }

    @Override
    public boolean onLongClick(View v) {
        if (!ItemLongClickListener.canStartDrag(mLauncher)) return false;
        // Return early if not the correct view
        if (!(v.getParent() instanceof DeepShortcutView)) return false;

        // Long clicked on a shortcut.
        DeepShortcutView sv = (DeepShortcutView) v.getParent();
        sv.setWillDrawIcon(false);

        // Move the icon to align with the center-top of the touch point
        Point iconShift = new Point();
        iconShift.x = mIconLastTouchPos.x - sv.getIconCenter().x;
        iconShift.y = mIconLastTouchPos.y - mLauncher.getDeviceProfile().iconSizePx;

//        DragView dv = mLauncher.getWorkspace().beginDragShared(sv.getIconView(),
//                this, sv.getFinalInfo(),
//                new ShortcutDragPreviewProvider(sv.getIconView(), iconShift), new DragOptions());
//        dv.animateShift(-iconShift.x, -iconShift.y);

        // TODO: support dragging from within folder without having to close it
        AbstractFloatingView.closeOpenContainer(mLauncher, AbstractFloatingView.TYPE_FOLDER);
        return false;
    }

    /**
     * Returns a PopupContainerWithArrow which is already open or null
     */
    public static PopupContainerWithArrow getOpen(Launcher launcher) {
        return getOpenView(launcher, TYPE_ACTION_POPUP);
    }
}
