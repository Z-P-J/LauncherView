package com.android.launcher3;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.android.launcher3.popup.OptionItem;
import com.android.launcher3.popup.OptionsPopupView;
import com.android.launcher3.widget.WidgetsFullSheet;
import com.ark.browser.launcher.R;
import com.ark.browser.launcher.SettingsBottomDialog;
import com.zpj.fragmentation.SimpleFragment;
import com.zpj.utils.Callback;

import java.util.ArrayList;
import java.util.List;

public class LauncherFragment extends SimpleFragment {

    private LauncherLayout mLauncherLayout;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_launcher;
    }

    @Override
    protected void initView(View view, @Nullable Bundle savedInstanceState) {
        mLauncherLayout = findViewById(R.id.launcher_layout);

        mLauncherLayout.setOptionItemProvider(new LauncherLayout.OptionItemProvider() {
            @Override
            public List<OptionItem> createOptions() {
                ArrayList<OptionItem> options = new ArrayList<>();
                options.add(new OptionItem(R.string.wallpaper_button_text, R.drawable.ic_wallpaper,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Toast.makeText(v.getContext(), "壁纸选择", Toast.LENGTH_SHORT).show();
//                        launcher.getStateManager().goToState(LauncherState.EDIT_MODE);
                                new SettingsBottomDialog().show(v.getContext());
                            }
                        }));
                options.add(new OptionItem(R.string.widget_button_text, R.drawable.ic_widget,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Toast.makeText(v.getContext(), "微件", Toast.LENGTH_SHORT).show();
                                WidgetsFullSheet.show(true);
                            }
                        }));
                options.add(new OptionItem(R.string.settings_button_text, R.drawable.ic_setting,
                        OptionsPopupView::startSettings));
                return options;
            }

            @Override
            public List<OptionItem> createOptions(ItemInfo itemInfo) {
                ArrayList<OptionItem> options = new ArrayList<>();
                options.add(new OptionItem(R.string.widget_button_text, R.drawable.ic_widget,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Toast.makeText(v.getContext(), "微件", Toast.LENGTH_SHORT).show();
                            }
                        }));
                options.add(new OptionItem(R.string.app_info_drop_target_label, R.drawable.ic_info_no_shadow,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Toast.makeText(v.getContext(), "应用信息", Toast.LENGTH_SHORT).show();
                            }
                        }));
                options.add(new OptionItem(R.string.install_drop_target_label, R.drawable.ic_install_no_shadow,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Toast.makeText(v.getContext(), "安装", Toast.LENGTH_SHORT).show();
                            }
                        }));
                return options;
            }
        });

        mLauncherLayout.init(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        mLauncherLayout.onSaveInstanceState(outState);
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
    public boolean onBackPressedSupport() {
        if (mLauncherLayout.onBackPressed()) {
            return true;
        }
        return super.onBackPressedSupport();
    }

}
