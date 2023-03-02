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
package com.android.launcher3.views;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Toast;

import com.android.launcher3.LauncherLayout;
import com.android.launcher3.LauncherState;
import com.ark.browser.launcher.R;
import com.android.launcher3.popup.ArrowPopup;
import com.android.launcher3.shortcuts.DeepShortcutView;
import com.android.launcher3.widget.WidgetsFullSheet;
import com.ark.browser.launcher.SettingsBottomDialog;

import java.util.ArrayList;
import java.util.List;

/**
 * Popup shown on long pressing an empty space in launcher
 */
public class OptionsPopupView extends ArrowPopup
        implements OnClickListener, OnLongClickListener {

    private final ArrayMap<View, OptionItem> mItemMap = new ArrayMap<>();
    private RectF mTargetRect;

    public OptionsPopupView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OptionsPopupView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onClick(View view) {
        handleViewClick(view);
    }

    @Override
    public boolean onLongClick(View view) {
        return handleViewClick(view);
    }

    private boolean handleViewClick(View view) {
        OptionItem item = mItemMap.get(view);
        if (item == null) {
            return false;
        }
        if (item.mClickListener.onLongClick(view)) {
            close(true);
            return true;
        }
        return false;
    }

    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() != MotionEvent.ACTION_DOWN) {
            return false;
        }
        if (mLauncher.getDragLayer().isEventOverView(this, ev)) {
            return false;
        }
        close(true);
        return true;
    }

    @Override
    protected boolean isOfType(int type) {
        return (type & TYPE_OPTIONS_POPUP) != 0;
    }

    @Override
    protected void getTargetObjectLocation(Rect outPos) {
        mTargetRect.roundOut(outPos);
    }

    public static void show(LauncherLayout launcher, RectF targetRect, List<OptionItem> items) {
        OptionsPopupView popup = (OptionsPopupView) LayoutInflater.from(launcher.getContext())
                .inflate(R.layout.longpress_options_menu, launcher.getDragLayer(), false);
        popup.mTargetRect = targetRect;

        for (OptionItem item : items) {
            Log.d("OptionsPopupView", "show");
            DeepShortcutView view = popup.inflateAndAdd(R.layout.system_shortcut, popup);
            view.getIconView().setBackgroundResource(item.mIconRes);
            view.getBubbleText().setText(item.mLabelRes);
            view.setDividerVisibility(View.INVISIBLE);
            view.setOnClickListener(popup);
            view.setOnLongClickListener(popup);
            popup.mItemMap.put(view, item);
        }
        popup.reorderAndShow(popup.getChildCount());
    }

    public static void showDefaultOptions(LauncherLayout launcher, float x, float y) {
        float halfSize = launcher.getResources().getDimension(R.dimen.options_menu_thumb_size) / 2;
        if (x < 0 || y < 0) {
            x = launcher.getDragLayer().getWidth() / 2f;
            y = launcher.getDragLayer().getHeight() / 2f;
        }
        RectF target = new RectF(x - halfSize, y - halfSize, x + halfSize, y + halfSize);

        ArrayList<OptionItem> options = new ArrayList<>();
        options.add(new OptionItem(R.string.wallpaper_button_text, R.drawable.ic_wallpaper,
                new OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        Toast.makeText(v.getContext(), "壁纸选择", Toast.LENGTH_SHORT).show();
                        launcher.getStateManager().goToState(LauncherState.EDIT_MODE);

                        new SettingsBottomDialog().show(v.getContext());

                        return true;
                    }
                }));
        options.add(new OptionItem(R.string.widget_button_text, R.drawable.ic_widget,
                new OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        Toast.makeText(v.getContext(), "微件", Toast.LENGTH_SHORT).show();
                        WidgetsFullSheet.show(launcher, true);
                        return true;
                    }
                }));
        options.add(new OptionItem(R.string.settings_button_text, R.drawable.ic_setting,
                OptionsPopupView::startSettings));

        show(launcher, target, options);
    }

    public static boolean startSettings(View view) {
        Toast.makeText(view.getContext(), "设置", Toast.LENGTH_SHORT).show();
        return true;
    }

    public static class OptionItem {

        private final int mLabelRes;
        private final int mIconRes;
        private final OnLongClickListener mClickListener;

        public OptionItem(int labelRes, int iconRes, OnLongClickListener clickListener) {
            mLabelRes = labelRes;
            mIconRes = iconRes;
            mClickListener = clickListener;
        }
    }
}
