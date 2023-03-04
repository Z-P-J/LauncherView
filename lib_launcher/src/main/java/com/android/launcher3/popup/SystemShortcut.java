package com.android.launcher3.popup;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import com.android.launcher3.ItemInfo;
import com.ark.browser.launcher.R;

/**
 * Represents a system shortcut for a given app. The shortcut should have a static label and
 * icon, and an onClickListener that depends on the item that the shortcut services.
 * <p>
 * Example system shortcuts, defined as inner classes, include Widgets and AppInfo.
 */
public abstract class SystemShortcut<T extends Context> extends ItemInfo {
    public final int iconResId;
    public final int labelResId;

    public SystemShortcut(int iconResId, int labelResId) {
        this.iconResId = iconResId;
        this.labelResId = labelResId;
    }

    public abstract View.OnClickListener getOnClickListener(T context, ItemInfo itemInfo);

    public static class Widgets extends SystemShortcut<Context> {

        public Widgets() {
            super(R.drawable.ic_widget, R.string.widget_button_text);
        }

        @Override
        public View.OnClickListener getOnClickListener(final Context context,
                                                       final ItemInfo itemInfo) {
            return v -> Toast.makeText(context, "微件", Toast.LENGTH_SHORT).show();
        }
    }

    public static class AppInfo extends SystemShortcut {
        public AppInfo() {
            super(R.drawable.ic_info_no_shadow, R.string.app_info_drop_target_label);
        }

        @Override
        public View.OnClickListener getOnClickListener(
                Context context, ItemInfo itemInfo) {
            return v -> Toast.makeText(context, "应用信息", Toast.LENGTH_SHORT).show();
        }
    }

    public static class Install extends SystemShortcut {
        public Install() {
            super(R.drawable.ic_install_no_shadow, R.string.install_drop_target_label);
        }

        @Override
        public View.OnClickListener getOnClickListener(
                Context context, ItemInfo itemInfo) {
            return v -> Toast.makeText(context, "安装", Toast.LENGTH_SHORT).show();
        }
    }

}
