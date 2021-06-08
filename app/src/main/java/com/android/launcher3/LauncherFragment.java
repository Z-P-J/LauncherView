package com.android.launcher3;

import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import android.view.View;

import com.android.launcher3.dragndrop.DragLayer;
import com.zpj.fragmentation.BaseFragment;

public class LauncherFragment extends BaseFragment {

    // Type: int
    private static final String RUNTIME_STATE_CURRENT_SCREEN = "launcher.current_screen";
    // Type: int
    private static final String RUNTIME_STATE = "launcher.state";
    // Type: ActivityResultInfo
    private static final String RUNTIME_STATE_PENDING_ACTIVITY_RESULT = "launcher.activity_result";
    // Type: SparseArray<Parcelable>
    private static final String RUNTIME_STATE_WIDGET_PANEL = "launcher.widget_panel";



    private LauncherLayout mLauncherLayout;


    @Override
    protected int getLayoutId() {
        return R.layout.fragment_launcher;
    }

    @Override
    protected void initView(View view, @Nullable Bundle savedInstanceState) {
        mLauncherLayout = findViewById(R.id.launcher_layout);
        mLauncherLayout.init(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mLauncherLayout.getWorkspace().getChildCount() > 0) {
            outState.putInt(RUNTIME_STATE_CURRENT_SCREEN, mLauncherLayout.getWorkspace().getNextPage());

        }
        outState.putInt(RUNTIME_STATE, mLauncherLayout.getStateManager().getState().ordinal);

        AbstractFloatingView widgets = AbstractFloatingView
                .getOpenView(mLauncherLayout, AbstractFloatingView.TYPE_WIDGETS_FULL_SHEET);
        if (widgets != null) {
            SparseArray<Parcelable> widgetsState = new SparseArray<>();
            widgets.saveHierarchyState(widgetsState);
            outState.putSparseParcelableArray(RUNTIME_STATE_WIDGET_PANEL, widgetsState);
        } else {
            outState.remove(RUNTIME_STATE_WIDGET_PANEL);
        }

        // We close any open folders and shortcut containers since they will not be re-opened,
        // and we need to make sure this state is reflected.
        AbstractFloatingView.closeAllOpenViews(mLauncherLayout, false);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();
        mLauncherLayout.getDragController().cancelDrag();
        mLauncherLayout.getDragController().resetLastGestureUpTime();
    }

    @Override
    public void onStop() {
        super.onStop();
        //        mLauncherLayout.getStateManager().moveToRestState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public void onConfigurationChanged() {
        mLauncherLayout.getDragLayer().recreateControllers();
        // TODO: We can probably avoid rebind when only screen size changed.
        mLauncherLayout.rebindModel();
    }

    @Override
    public boolean onBackPressedSupport() {
        if (mLauncherLayout.onBackPressed()) {
            return true;
        }
        return super.onBackPressedSupport();
    }








    public DragLayer getDragLayer() {
        return mLauncherLayout.getDragLayer();
    }

    public LauncherLayout getLauncherLayout() {
        return mLauncherLayout;
    }

    public <T extends View> T getOverviewPanel() {
        return mLauncherLayout.getOverviewPanel();
    }

    public View getRootView() {
        return mLauncherLayout.getRootView();
    }

    protected void reapplyUi() {
        mLauncherLayout.getRootView().dispatchInsets();
        mLauncherLayout.getStateManager().reapplyState(true /* cancelCurrentAnimation */);
    }

}
