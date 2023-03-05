package com.android.launcher3.popup;

import android.view.View;

public class OptionItem {

    private final int mLabelRes;
    private final int mIconRes;
    private final View.OnClickListener mClickListener;

    public OptionItem(int labelRes, int iconRes, View.OnClickListener clickListener) {
        mLabelRes = labelRes;
        mIconRes = iconRes;
        mClickListener = clickListener;
    }

    public void onClick(View view) {
        if (mClickListener != null) {
            mClickListener.onClick(view);
        }
    }

    public int getLabelRes() {
        return mLabelRes;
    }

    public int getIconRes() {
        return mIconRes;
    }

}
