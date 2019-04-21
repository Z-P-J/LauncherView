package com.android.launcher3.popup;

import android.view.View;
import android.widget.Toast;

import com.android.launcher3.BaseDraggingActivity;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;

/**
 * Represents a system shortcut for a given app. The shortcut should have a static label and
 * icon, and an onClickListener that depends on the item that the shortcut services.
 *
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

    public static class Widgets extends SystemShortcut<Launcher> {

        public Widgets() {
            super(R.drawable.ic_widget, R.string.widget_button_text);
        }

        @Override
        public View.OnClickListener getOnClickListener(final Launcher launcher,
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
//            return (view) -> {
//                Rect sourceBounds = activity.getViewBounds(view);
//                Bundle opts = activity.getActivityLaunchOptionsAsBundle(view);
//                new PackageManagerHelper(activity).startDetailsActivityForInfo(
//                        itemInfo, sourceBounds, opts);
//                activity.getUserEventDispatcher().logActionOnControl(Action.Touch.TAP,
//                        ControlType.APPINFO_TARGET, view);
//            };
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
//            boolean supportsWebUI = (itemInfo instanceof ShortcutInfo) &&
//                    ((ShortcutInfo) itemInfo).hasStatusFlag(ShortcutInfo.FLAG_SUPPORTS_WEB_UI);
//            boolean isInstantApp = false;
//            if (itemInfo instanceof com.android.launcher3.AppInfo) {
//                com.android.launcher3.AppInfo appInfo = (com.android.launcher3.AppInfo) itemInfo;
//                isInstantApp = InstantAppResolver.newInstance(activity).isInstantApp(appInfo);
//            }
//            boolean enabled = supportsWebUI || isInstantApp;
//            if (!enabled) {
//                return null;
//            }
//            return createOnClickListener(activity, itemInfo);
            return v -> Toast.makeText(activity, "安装", Toast.LENGTH_SHORT).show();
        }

//        public View.OnClickListener createOnClickListener(
//                BaseDraggingActivity activity, ItemInfo itemInfo) {
//            return view -> {
//                Intent intent = new PackageManagerHelper(view.getContext()).getMarketIntent(
//                        itemInfo.getTargetComponent().getPackageName());
//                activity.startActivitySafely(view, intent, itemInfo);
//                AbstractFloatingView.closeAllOpenViews(activity);
//            };
//        }
    }
}
