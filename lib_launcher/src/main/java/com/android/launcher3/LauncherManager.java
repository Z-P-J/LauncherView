package com.android.launcher3;

import com.android.launcher3.dragndrop.DragController;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.util.SystemUiController;
import com.android.launcher3.util.Utilities;

public class LauncherManager {

    private static volatile LauncherManager INSTANCE;

    private LauncherLayout mLauncherLayout;


    public static LauncherManager get() {
        if (INSTANCE == null) {
            synchronized (LauncherManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new LauncherManager();
                }
            }
        }
        return INSTANCE;
    }

    private static LauncherManager getAndCheck() {
        LauncherManager manager = get();
        if (manager.mLauncherLayout == null) {
            throw new RuntimeException("LauncherManager must init firstly!");
        }
        return manager;
    }

    public static void init(LauncherLayout launcherLayout) {
        get().mLauncherLayout = launcherLayout;
    }

    public static void destroy() {
        if (INSTANCE != null) {
            synchronized (LauncherManager.class) {
                if (INSTANCE != null) {
                    INSTANCE.mLauncherLayout = null;
                    INSTANCE = null;
                }
            }
        }
    }

    public static DragLayer getDragLayer() {
        return getLauncherLayout().getDragLayer();
    }

    public static LauncherLayout getLauncherLayout() {
        return getAndCheck().mLauncherLayout;
    }

    public static LauncherStateManager getStateManager() {
        return getLauncherLayout().getStateManager();
    }

    public static DragController getDragController() {
        return getLauncherLayout().getDragController();
    }

    public static Workspace getWorkspace() {
        return getLauncherLayout().getWorkspace();
    }

    public static HotSeat getHotseat() {
        return getLauncherLayout().getHotseat();
    }

    public static DeviceProfile getDeviceProfile() {
        return getLauncherLayout().getDeviceProfile();
    }

    public static SystemUiController getSystemUiController() {
        return getLauncherLayout().getSystemUiController();
    }

    public static boolean isInMultiWindowModeCompat() {
        return Utilities.ATLEAST_NOUGAT && getLauncherLayout().getActivity().isInMultiWindowMode();
    }

    public static boolean finishAutoCancelActionMode() {
//        if (mCurrentActionMode != null && AUTO_CANCEL_ACTION_MODE == mCurrentActionMode.getTag()) {
//            mCurrentActionMode.finish();
//            return true;
//        }
        return false;
    }

    public static void rebindWorkspace(InvariantDeviceProfile idp) {
        getLauncherLayout().rebindWorkspace(idp);
    }

}
