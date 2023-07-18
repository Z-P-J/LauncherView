package com.ark.browser.launcher.demo.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.cardview.widget.CardView;

import com.zpj.utils.ContextUtils;
import com.zpj.utils.ScreenUtils;

/**
 * @author Z-P-J
 * 参考 https://github.com/wuyr/RippleAnimation
 */
public final class SkinChangeAnimation {

    private Context context;

    //DecorView
    private ViewGroup mRootView;
    private AnimationView animationView;

    //扩散的起点
    private float mStartX = 0, mStartY = 0;
    private long mDuration = 500;
    private Runnable dismissRunnable;
    private Runnable startRunnable;

    private Paint mPaint;

    private boolean isStarted;

    private SkinChangeAnimation(Context context) {
        this.context = context;
        //获取activity的根视图,用来添加本View
        mRootView = (ViewGroup) ContextUtils.getActivity(context).getWindow().getDecorView();

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        //设置为擦除模式
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    public static SkinChangeAnimation with(Context context) {
        return new SkinChangeAnimation(context);
    }

    public SkinChangeAnimation setStartPosition(float startX, float startY) {
        this.mStartX = startX;
        this.mStartY = startY;
        return this;
    }

    public SkinChangeAnimation setDuration(long mDuration) {
        this.mDuration = mDuration;
        return this;
    }

    public SkinChangeAnimation setDismissRunnable(Runnable dismissRunnable) {
        this.dismissRunnable = dismissRunnable;
        return this;
    }

    public SkinChangeAnimation setStartRunnable(Runnable startRunnable) {
        this.startRunnable = startRunnable;
        return this;
    }

    public void start() {
        if (!isStarted) {
            isStarted = true;

            updateMaxRadius();

            updateBackground();

            animationView = new AnimationView(context);
            animationView.setPivotX(mStartX);
            animationView.setPivotY(mStartY);
            animationView.setScaleX(0f);
            animationView.setScaleY(0f);
            animationView.setRadius(1000);
//            animationView.setBackgroundColor(Color.BLACK);
            animationView.setCardBackgroundColor(Color.WHITE);
            animationView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            mRootView.addView(animationView);

            startAnimation();


        }
    }

    /**
     * 根据起始点将屏幕分成4个小矩形,mMaxRadius就是取它们中最大的矩形的对角线长度
     * 这样的话, 无论起始点在屏幕中的哪一个位置上, 我们绘制的圆形总是能覆盖屏幕
     */
    private void updateMaxRadius() {
        if (mStartX == 0 && mStartY == 0) {
            mStartX = ScreenUtils.getScreenWidth(context) / 2f;
            mStartY = ScreenUtils.getScreenHeight(context) / 2f;
        }
    }


    /**
     * 更新屏幕截图
     */
    private void updateBackground() {
        mRootView.setDrawingCacheEnabled(true);
        mRootView.setDrawingCacheEnabled(false);
    }

    private void startAnimation() {
        animationView.animate()
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (dismissRunnable != null) {
                            dismissRunnable.run();
                        }
                        animationView.animate()
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        isStarted = false;
                                        //动画播放完毕, 移除View
                                        if (mRootView != null) {
                                            mRootView.removeView(animationView);
                                            mRootView = null;
                                        }
                                        if (mPaint != null) {
                                            mPaint = null;
                                        }
                                        context = null;
                                        animationView = null;
                                    }
                                })
                                .setUpdateListener(null)
                                .alpha(0f)
                                .setDuration(200)
                                .start();
                    }

                    @Override
                    public void onAnimationStart(Animator animation) {
                        if (startRunnable != null) {
                            startRunnable.run();
                        }
                    }
                })
                .setUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        float f = animation.getAnimatedFraction();
                        animationView.setRadius((1 - f) * 1000);
                    }
                })
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(360)
                .start();
    }

    public class AnimationView extends CardView
            implements View.OnClickListener, View.OnLongClickListener, View.OnTouchListener {

        public AnimationView(Context context) {
            super(context);
            setOnClickListener(this);
            setOnLongClickListener(this);
            setOnTouchListener(this);
        }

        @Override
        public void onClick(View v) {

        }

        @Override
        public boolean onLongClick(View v) {
            return true;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return true;
        }
    }

}
