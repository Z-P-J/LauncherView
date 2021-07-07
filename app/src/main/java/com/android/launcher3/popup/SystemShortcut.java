package com.android.launcher3.popup;

import android.view.View;
import android.widget.Toast;

import com.android.launcher3.BaseDraggingActivity;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherActivity;
import com.qianxun.browser.launcher.R;

/**
 * Represents a system shortcut for a given app. The shortcut should have a static label and
 * icon, and an onClickListener that depends on the item that the shortcut services.
 * <p>
 * Example system shortcuts, defined as inner classes, include Widgets and AppInfo.
 */
public abstract class SystemShortcut<T extends BaseDraggingActivity> extends ItemInfo {
    public final int iconResId;
    public final int labelResId;

    public SystemShortcut(int iconResId, int labelResId) {
        this.iconResId = iconResId;
        this.labelResId = labelResId;
    }

    public abstract View.OnClickListener getOnClickListener(T activity, ItemInfo itemInfo);

    public static class Widgets extends SystemShortcut<LauncherActivity> {

        public Widgets() {
            super(R.drawable.ic_widget, R.string.widget_button_text);
        }

        @Override
        public View.OnClickListener getOnClickListener(final LauncherActivity launcher,
                                                       final ItemInfo itemInfo) {
            return v -> Toast.makeText(launcher, "微件", Toast.LENGTH_SHORT).show();
        }
    }

    public static class AppInfo extends SystemShortcut {
        public AppInfo() {
            super(R.drawable.ic_info_no_shadow, R.string.app_info_drop_target_label);
        }

        @Override
        public View.OnClickListener getOnClickListener(
                BaseDraggingActivity activity, ItemInfo itemInfo) {
            return v -> Toast.makeText(activity, "应用信息", Toast.LENGTH_SHORT).show();
        }
    }

    public static class Install extends SystemShortcut {
        public Install() {
            super(R.drawable.ic_install_no_shadow, R.string.install_drop_target_label);
        }

        @Override
        public View.OnClickListener getOnClickListener(
                BaseDraggingActivity activity, ItemInfo itemInfo) {
            return v -> Toast.makeText(activity, "安装", Toast.LENGTH_SHORT).show();
        }
    }

}
