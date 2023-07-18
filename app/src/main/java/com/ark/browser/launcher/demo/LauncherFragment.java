package com.ark.browser.launcher.demo;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.android.launcher3.BubbleTextView;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.ItemInfoWithIcon;
import com.android.launcher3.LauncherLayout;
import com.android.launcher3.TabItemInfo;
import com.android.launcher3.popup.OptionItem;
import com.android.launcher3.popup.OptionsPopupView;
import com.ark.browser.launcher.demo.utils.HomepageItemLoader;
import com.ark.browser.launcher.demo.utils.SkinChangeAnimation;
import com.ark.browser.launcher.demo.widget.WidgetsFullSheet;
import com.zpj.fragmentation.SimpleFragment;

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
        
        mLauncherLayout.setClickHandler(new LauncherLayout.ClickHandler() {
            @Override
            public void onClickAppShortcut(View v, ItemInfoWithIcon itemInfo) {
                Toast.makeText(context, "title=" + itemInfo.title + " url=" + itemInfo.url, Toast.LENGTH_SHORT).show();

                if (v instanceof BubbleTextView) {
                    Rect rect = new Rect();
                    ((BubbleTextView) v).getIconBounds(rect);

                    int[] location = new int[2];
                    v.getLocationOnScreen(location);

                    int x = location[0] + rect.centerX();
                    int y = location[1] + rect.centerY();

                    SkinChangeAnimation.with(getContext())
                            .setStartPosition(x, y)
                            .setDuration(1000)
                            .setStartRunnable(() -> {
                                Toast.makeText(context, "start animation", Toast.LENGTH_SHORT).show();
                            })
                            .setDismissRunnable(() -> {
                                Toast.makeText(context, "dismiss animation", Toast.LENGTH_SHORT).show();
                            })
                            .start();

                }

            }

            @Override
            public void onClickTabCard(View v, TabItemInfo itemInfo) {
                Toast.makeText(context, "title=" + itemInfo.title + " url=" + itemInfo.url, Toast.LENGTH_SHORT).show();
            }
        });

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

        mLauncherLayout.setItemLoader(new HomepageItemLoader());

        mLauncherLayout.setSlideListener(new LauncherLayout.SlideListener() {

            AlertDialog alertDialog;


            @Override
            public void onSlideStart(int direction) {
                Toast.makeText(context, "onSlideStart", Toast.LENGTH_SHORT).show();
                if (alertDialog != null) {
                    alertDialog.cancel();
                }
                alertDialog = new AlertDialog.Builder(context)
                        .setTitle("手势滑动监听")
                        .setMessage("移动方向：" + (direction == 1 ? "向上" : "向下"))
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                alertDialog = null;
                            }
                        })
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                alertDialog = null;
                            }
                        })
                        .setPositiveButton("关闭", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .create();
                alertDialog.show();
            }

            @Override
            public void onSlideVertical(float dy, int direction) {

            }

            @Override
            public void onSlideEnd() {

            }

            @Override
            public boolean canHandleLongPress() {
                return alertDialog == null;
            }

            @Override
            public boolean canStartDrag() {
                return alertDialog == null;
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
