//package com.android.launcher3;
//
//import android.content.res.Configuration;
//import android.graphics.Point;
//import android.os.Bundle;
//import android.text.method.TextKeyListener;
//import android.view.Display;
//import android.view.View;
//
//import com.android.launcher3.states.RotationHelper;
//import com.android.launcher3.views.BaseDragLayer;
//
//public class LauncherActivity extends BaseDraggingActivity {
//
//    private RotationHelper mRotationHelper;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//        LauncherAppState app = LauncherAppState.getInstance(this);
//        mOldConfig = new Configuration(getResources().getConfiguration());
//        initDeviceProfile(app.getInvariantDeviceProfile());
//
//        mRotationHelper = new RotationHelper(this);
//
//
//
//
//
//
//
//        mRotationHelper.initialize();
//
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        mRotationHelper.destroy();
//        TextKeyListener.getInstance().release();
//
//        LauncherAnimUtils.onDestroyActivity();
//    }
//
//    @Override
//    public BaseDragLayer getDragLayer() {
//        return null;
//    }
//
//    @Override
//    public <T extends View> T getOverviewPanel() {
//        return null;
//    }
//
//    @Override
//    public View getRootView() {
//        return null;
//    }
//
//    @Override
//    protected void reapplyUi() {
//
//    }
//
//    public RotationHelper getRotationHelper() {
//        return mRotationHelper;
//    }
//
//    private void initDeviceProfile(InvariantDeviceProfile idp) {
//        // Load configuration-specific DeviceProfile
//        mDeviceProfile = idp.getDeviceProfile(this);
//        if (isInMultiWindowModeCompat()) {
//            Display display = getWindowManager().getDefaultDisplay();
//            Point mwSize = new Point();
//            display.getSize(mwSize);
//            mDeviceProfile = mDeviceProfile.getMultiWindowProfile(this, mwSize);
//        }
//        onDeviceProfileInitiated();
//    }
//
//}
