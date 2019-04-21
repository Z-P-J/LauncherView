package com.android.launcher3;

import android.content.ComponentName;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Toast;

import com.android.launcher3.Launcher.OnResumeCallback;
import com.android.launcher3.dragndrop.DragOptions;
import com.android.launcher3.logging.LoggerUtils;
import com.android.launcher3.userevent.nano.LauncherLogProto.ControlType;
import com.android.launcher3.userevent.nano.LauncherLogProto.Target;
import com.android.launcher3.util.Themes;

/**
 * Drop target which provides a secondary option for an item.
 *    For app targets: shows as uninstall
 *    For configurable widgets: shows as setup
 */
public class SecondaryDropTarget extends ButtonDropTarget implements OnAlarmListener {

    private static final String TAG = "SecondaryDropTarget";

    public static final int UNINSTALL = R.id.action_uninstall;
    public static final int RECONFIGURE = R.id.action_reconfigure;

    private final Alarm mCacheExpireAlarm;

    protected int mCurrentAccessibilityAction = -1;
    public SecondaryDropTarget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SecondaryDropTarget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mCacheExpireAlarm = new Alarm();
        mCacheExpireAlarm.setOnAlarmListener(this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setupUi(UNINSTALL);
    }

    protected void setupUi(int action) {
        if (action == mCurrentAccessibilityAction) {
            return;
        }
        mCurrentAccessibilityAction = action;

        if (action == UNINSTALL) {
            mHoverColor = getResources().getColor(R.color.uninstall_target_hover_tint);
            setDrawable(R.drawable.ic_uninstall_shadow);
            updateText(R.string.uninstall_drop_target_label);
        } else {
            mHoverColor = Themes.getColorAccent(getContext());
            setDrawable(R.drawable.ic_setup_shadow);
            updateText(R.string.gadget_setup_text);
        }
    }

    @Override
    public void onAlarm(Alarm alarm) {

    }

    @Override
    public Target getDropTargetForLogging() {
        Target t = LoggerUtils.newTarget(Target.Type.CONTROL);
        t.controlType = mCurrentAccessibilityAction == UNINSTALL ? ControlType.UNINSTALL_TARGET
                : ControlType.SETTINGS_BUTTON;
        return t;
    }

    @Override
    protected boolean supportsDrop(ItemInfo info) {
        setupUi(UNINSTALL);

        if (info instanceof ItemInfoWithIcon) {
            return true;
        }
        return false;
    }

    @Override
    public void onDrop(DragObject d, DragOptions options) {
        // Defer onComplete
        d.dragSource = new DeferredOnComplete(d.dragSource, getContext());
        super.onDrop(d, options);
    }

    @Override
    public void completeDrop(final DragObject d) {
        ComponentName target = performDropAction(null, d.dragInfo);
        if (d.dragSource instanceof DeferredOnComplete) {
            DeferredOnComplete deferred = (DeferredOnComplete) d.dragSource;
            if (target != null) {
                mLauncher.setOnResumeCallback(deferred);
            } else {
                deferred.sendFailure();
            }
        }
    }

    /**
     * Performs the drop action and returns the target component for the dragObject or null if
     * the action was not performed.
     */
    protected ComponentName performDropAction(View view, ItemInfo info) {
//        if (mCurrentAccessibilityAction == RECONFIGURE) {
//            return null;
//        }

        Toast.makeText(mLauncher, "卸载", Toast.LENGTH_SHORT).show();
        return null;
    }

    /**
     * A wrapper around {@link DragSource} which delays the {@link #onDropCompleted} action until
     * {@link #onLauncherResume}
     */
    private class DeferredOnComplete implements DragSource, OnResumeCallback {

        private final DragSource mOriginal;

        private DragObject mDragObject;

        public DeferredOnComplete(DragSource original, Context context) {
            mOriginal = original;
        }

        @Override
        public void onDropCompleted(View target, DragObject d,
                boolean success) {
            mDragObject = d;
        }

        @Override
        public void fillInLogContainerData(View v, ItemInfo info, Target target,
                                           Target targetParent) {
            mOriginal.fillInLogContainerData(v, info, target, targetParent);
        }

        @Override
        public void onLauncherResume() {
//            if (成功) {
//                mDragObject.dragSource = mOriginal;
//                mOriginal.onDropCompleted(SecondaryDropTarget.this, mDragObject, true);
//            }
            sendFailure();
        }

        void sendFailure() {
            mDragObject.dragSource = mOriginal;
            mDragObject.cancelled = true;
            mOriginal.onDropCompleted(SecondaryDropTarget.this, mDragObject, false);
        }
    }
}
