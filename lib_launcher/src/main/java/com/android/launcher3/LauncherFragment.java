package com.android.launcher3;

import static android.content.pm.ActivityInfo.CONFIG_ORIENTATION;
import static android.content.pm.ActivityInfo.CONFIG_SCREEN_SIZE;

import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.SparseArray;
import android.view.View;

import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.states.RotationHelper;
import com.ark.browser.launcher.R;
import com.zpj.fragmentation.SimpleFragment;

public class LauncherFragment extends SimpleFragment {

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
        AbstractFloatingView.closeAllOpenViews(false);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();
        mLauncherLayout.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mLauncherLayout.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onBackPressedSupport() {
        if (mLauncherLayout.onBackPressed()) {
            return true;
        }
        return super.onBackPressedSupport();
    }

    protected void reapplyUi() {
        mLauncherLayout.getRootView().dispatchInsets();
        mLauncherLayout.getStateManager().reapplyState(true /* cancelCurrentAnimation */);
    }

}
