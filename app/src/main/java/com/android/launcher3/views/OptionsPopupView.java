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
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Toast;

import com.android.launcher3.ControlType;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.popup.ArrowPopup;
import com.android.launcher3.shortcuts.DeepShortcutView;
import com.android.launcher3.widget.WidgetsFullSheet;

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
        if (item.mControlTypeForLog > 0) {

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

    public static void show(Launcher launcher, RectF targetRect, List<OptionItem> items) {
        OptionsPopupView popup = (OptionsPopupView) launcher.getLayoutInflater()
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

    public static void showDefaultOptions(Launcher launcher, float x, float y) {
        float halfSize = launcher.getResources().getDimension(R.dimen.options_menu_thumb_size) / 2;
        if (x < 0 || y < 0) {
            x = launcher.getDragLayer().getWidth() / 2;
            y = launcher.getDragLayer().getHeight() / 2;
        }
        RectF target = new RectF(x - halfSize, y - halfSize, x + halfSize, y + halfSize);

        ArrayList<OptionItem> options = new ArrayList<>();
        options.add(new OptionItem(R.string.wallpaper_button_text, R.drawable.ic_wallpaper,
                ControlType.WALLPAPER_BUTTON, OptionsPopupView::startWallpaperPicker));
        options.add(new OptionItem(R.string.widget_button_text, R.drawable.ic_widget,
                ControlType.WIDGETS_BUTTON, OptionsPopupView::onWidgetsClicked));
        options.add(new OptionItem(R.string.settings_button_text, R.drawable.ic_setting,
                ControlType.SETTINGS_BUTTON, OptionsPopupView::startSettings));

        show(launcher, target, options);
    }

    public static boolean onWidgetsClicked(View view) {
        Toast.makeText(view.getContext(), "微件", Toast.LENGTH_SHORT).show();
        WidgetsFullSheet.show(Launcher.getLauncher(view.getContext()), true);
        return true;
    }

    public static boolean startSettings(View view) {
        Toast.makeText(view.getContext(), "设置", Toast.LENGTH_SHORT).show();
        return true;
    }

    /**
     * Event handler for the wallpaper picker button that appears after a long press
     * on the home screen.
     */
    public static boolean startWallpaperPicker(View v) {
        Toast.makeText(v.getContext(), "壁纸选择", Toast.LENGTH_SHORT).show();
        return true;
    }

    public static class OptionItem {

        private final int mLabelRes;
        private final int mIconRes;
        private final int mControlTypeForLog;
        private final OnLongClickListener mClickListener;

        public OptionItem(int labelRes, int iconRes, int controlTypeForLog,
                          OnLongClickListener clickListener) {
            mLabelRes = labelRes;
            mIconRes = iconRes;
            mControlTypeForLog = controlTypeForLog;
            mClickListener = clickListener;
        }
    }
}
