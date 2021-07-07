package com.qianxun.browser.ui.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.LauncherActivity;
import com.android.launcher3.LauncherAppState;
import com.qianxun.browser.launcher.R;
import com.zpj.fragmentation.dialog.base.BottomDragDialogFragment;

public class SettingsBottomDialog extends BottomDragDialogFragment<SettingsBottomDialog> {

    @Override
    protected int getContentLayoutId() {
        return R.layout.fragment_dialog_settings;
    }

    @Override
    protected void initView(View view, @Nullable Bundle savedInstanceState) {
        super.initView(view, savedInstanceState);
        Button btn1 = findViewById(R.id.btn_1);
        Button btn2 = findViewById(R.id.btn_2);
        Button btn3 = findViewById(R.id.btn_3);
        Button btn4 = findViewById(R.id.btn_4);
        SeekBar seekBar = findViewById(R.id.seek_bar);
        LauncherAppState app = LauncherAppState.getInstance(context);
        InvariantDeviceProfile idp = app.getInvariantDeviceProfile();
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                idp.numRows = 4;
                idp.numColumns = 5;
                LauncherActivity.fromContext(context).rebindWorkspace(idp);
            }
        });

        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                idp.numRows = 4;
                idp.numColumns = 6;
                LauncherActivity.fromContext(context).rebindWorkspace(idp);
            }
        });

        btn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                idp.numRows = 5;
                idp.numColumns = 5;
                LauncherActivity.fromContext(context).rebindWorkspace(idp);
            }
        });

        btn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                idp.numRows = 5;
                idp.numColumns = 6;
                LauncherActivity.fromContext(context).rebindWorkspace(idp);
            }
        });


        final float iconSize = idp.iconSize;
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float scale = progress / 50f;
                idp.iconSize = iconSize * scale;
                Log.d("SettingsBottomDialog", "iconSize=" + (iconSize * scale));
                LauncherActivity.fromContext(context).rebindWorkspace(idp);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }

}
