package com.ark.browser.launcher.demo;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

import androidx.annotation.Nullable;

import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherManager;
import com.zpj.fragmentation.dialog.impl.BottomActionDialogFragment;

public class SettingsBottomDialog extends BottomActionDialogFragment<SettingsBottomDialog> {

    @Override
    protected int getContentLayoutId() {
        return com.ark.browser.launcher.R.layout.fragment_dialog_settings;
    }

    @Override
    protected void initView(View view, @Nullable Bundle savedInstanceState) {
        super.initView(view, savedInstanceState);
        Button btn1 = findViewById(com.ark.browser.launcher.R.id.btn_1);
        Button btn2 = findViewById(com.ark.browser.launcher.R.id.btn_2);
        Button btn3 = findViewById(com.ark.browser.launcher.R.id.btn_3);
        Button btn4 = findViewById(com.ark.browser.launcher.R.id.btn_4);
        SeekBar seekBar = findViewById(com.ark.browser.launcher.R.id.seek_bar);
        LauncherAppState app = LauncherAppState.getInstance(context);
        InvariantDeviceProfile idp = app.getInvariantDeviceProfile();
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                idp.numRows = 4;
                idp.numColumns = 5;
                LauncherManager.rebindWorkspace(idp);
            }
        });

        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                idp.numRows = 4;
                idp.numColumns = 6;
                LauncherManager.rebindWorkspace(idp);
            }
        });

        btn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                idp.numRows = 5;
                idp.numColumns = 5;
                LauncherManager.rebindWorkspace(idp);
            }
        });

        btn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                idp.numRows = 5;
                idp.numColumns = 6;
                LauncherManager.rebindWorkspace(idp);
            }
        });


        final float iconSize = idp.iconSize;
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float scale = progress / 50f;
                idp.iconSize = iconSize * scale;
                Log.d("SettingsBottomDialog", "iconSize=" + (iconSize * scale));
                LauncherManager.rebindWorkspace(idp);
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
