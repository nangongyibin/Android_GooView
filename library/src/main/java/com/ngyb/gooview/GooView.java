package com.ngyb.gooview;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.ngyb.utils.GeometryUtils;

/**
 * 作者：南宫燚滨
 * 描述：
 * 邮箱：nangongyibin@gmail.com
 * 日期：2020/6/23 07:58
 */
public class GooView extends View {
    float maxDistance = 150f;//2圆最远的距离
    private Paint paint;
    //drag圆的圆心
    PointF dragCenter = new PointF(500f, 400f);
    PointF stickyCenter = new PointF(500f, 400f);
    PointF controlPoint = new PointF(400f, 400f);
    PointF[] stickPoints = new PointF[]{new PointF(500f, 370f), new PointF(500f, 430f)};
    PointF[] dragkPoints = new PointF[]{new PointF(300f, 370f), new PointF(300f, 430f)};
    //drag圆的半径
    float dragRadius = 30f;
    float stickyRadius = 30f;
    double link;
    boolean isOutOfRange = false;

    public GooView(Context context) {
        this(context, null);
    }

    public GooView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GooView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        paint = new Paint();
        paint.setColor(Color.RED);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //动态根据两个圆岩心的距离计算sticky圆的半径
        calculateStickyRadius();
        float dx = dragCenter.x - stickyCenter.x;
        float dy = dragCenter.y - stickyCenter.y;
        if (dx != 0) {
            link = dy / dx;
        }
        dragkPoints = GeometryUtils.getIntersectionPoints(dragCenter, dragRadius, link);
        stickPoints = GeometryUtils.getIntersectionPoints(stickyCenter, stickyRadius, link);
        controlPoint = GeometryUtils.getPointByPercent(dragCenter, stickyCenter, 0.618f);
        //绘制两个圆
        canvas.drawCircle(dragCenter.x, dragCenter.y, dragRadius, paint);
        canvas.drawCircle(stickyCenter.x, stickyCenter.y, stickyRadius, paint);
        if (!isOutOfRange) {
            //绘制两个圆中间链接的部分
            Path path = new Path();
            path.moveTo(stickPoints[0].x, stickPoints[0].y);//指定一个起点
            path.quadTo(controlPoint.x, controlPoint.y, dragkPoints[0].x, dragkPoints[0].y);
            path.lineTo(dragkPoints[1].x, dragkPoints[1].y);//连线到第二个曲线的起点
            path.quadTo(controlPoint.x, controlPoint.y, stickPoints[1].x, stickPoints[1].y);
            path.close();//path会自动关闭,没有必要
            canvas.drawPath(path, paint);
        }
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(stickyCenter.x, stickyCenter.y, maxDistance, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void calculateStickyRadius() {
        //获取2圆圆心的距离
        float distance = GeometryUtils.getDistanceBetweenPoints(dragCenter, stickyCenter);
        float fraction = distance / maxDistance;
        stickyRadius = dragRadius * fraction;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                dragCenter.set(event.getX(), event.getY());
                isOutOfRange = GeometryUtils.getDistanceBetweenPoints(dragCenter, stickyCenter) > maxDistance;
                break;
            case MotionEvent.ACTION_UP:
                if (isOutOfRange) {
                    //播放爆炸动画
                    playBooAnim(dragCenter.x, dragCenter.y);
                    dragCenter.set(stickyCenter);
                } else {
                    //回弹回去
                    PointFEvaluator fEvaluator = new PointFEvaluator();
                    PointF start = new PointF(dragCenter.x, dragCenter.y);
                    ValueAnimator animator = ValueAnimator.ofObject(fEvaluator, start, stickyCenter);
                    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            PointF value = (PointF) animation.getAnimatedValue();
                            dragCenter.set(value);
                            invalidate();
                        }
                    });
                    animator.setDuration(400);
                    animator.setInterpolator(new OvershootInterpolator(4));
                    animator.start();
                }
                break;
        }
        invalidate();
        return true;
    }

    private void playBooAnim(float x, float y) {
        final FrameLayout parent = (FrameLayout) getParent();
        final ImageView imageView = new ImageView(getContext());
        imageView.setLayoutParams(new FrameLayout.LayoutParams(70, 70));
        imageView.setBackgroundResource(R.drawable.anim_bg);
        parent.addView(imageView);
        //设置位置
        imageView.setTranslationX(x - 35);
        imageView.setTranslationY(y - 35);
        AnimationDrawable drawable = (AnimationDrawable) imageView.getBackground();
        drawable.start();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                parent.removeView(imageView);
            }
        }, 601);
    }
}
