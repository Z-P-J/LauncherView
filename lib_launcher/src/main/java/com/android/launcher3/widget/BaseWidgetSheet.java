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
package com.android.launcher3.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Toast;

import com.android.launcher3.DragSource;
import com.android.launcher3.DropTarget.DragObject;
import com.android.launcher3.LauncherManager;
import com.android.launcher3.util.Utilities;
import com.android.launcher3.touch.ItemLongClickListener;
import com.android.launcher3.util.SystemUiController;
import com.android.launcher3.util.Themes;
import com.android.launcher3.views.AbstractSlideInView;
import com.android.launcher3.R;

/**
 * Base class for various widgets popup
 */
abstract class BaseWidgetSheet extends AbstractSlideInView
        implements OnClickListener, OnLongClickListener, DragSource {


    /* Touch handling related member variables. */
    private Toast mWidgetInstructionToast;

//    protected final ColorScrim mColorScrim;

    public BaseWidgetSheet(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
//        mColorScrim = ColorScrim.createExtractedColorScrim(this);
    }

    @Override
    public final void onClick(View v) {
        // Let the user know that they have to long press to add a widget
        if (mWidgetInstructionToast != null) {
            mWidgetInstructionToast.cancel();
        }

        CharSequence msg = Utilities.wrapForTts(
                getContext().getText(R.string.long_press_widget_to_add),
                getContext().getString(R.string.long_accessible_way_to_add));
        mWidgetInstructionToast = Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT);
        mWidgetInstructionToast.show();
    }

    @Override
    public final boolean onLongClick(View v) {
        return ItemLongClickListener.canStartDrag(mLauncher);
    }

    protected void setTranslationShift(float translationShift) {
        super.setTranslationShift(translationShift);
//        mColorScrim.setProgress(1 - mTranslationShift);
    }

    //
    // Drag related handling methods that implement {@link DragSource} interface.
    //

    @Override
    public void onDropCompleted(View target, DragObject d, boolean success) {
    }


    protected void onCloseComplete() {
        super.onCloseComplete();
        clearNavBarColor();
    }

    protected void clearNavBarColor() {
        LauncherManager.getSystemUiController().updateUiState(
                SystemUiController.UI_STATE_WIDGET_BOTTOM_SHEET, 0);
    }

    protected void setupNavBarColor() {
        boolean isSheetDark = Themes.getAttrBoolean(getContext(), R.attr.isMainColorDark);
        LauncherManager.getSystemUiController().updateUiState(
                SystemUiController.UI_STATE_WIDGET_BOTTOM_SHEET,
                isSheetDark ? SystemUiController.FLAG_DARK_NAV : SystemUiController.FLAG_LIGHT_NAV);
    }

}
