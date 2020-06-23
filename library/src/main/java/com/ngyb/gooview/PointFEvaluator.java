package com.ngyb.gooview;

import android.animation.TypeEvaluator;
import android.graphics.PointF;

/**
 * 作者：南宫燚滨
 * 描述：
 * 邮箱：nangongyibin@gmail.com
 * 日期：2020/6/23 09:16
 */
public class PointFEvaluator implements TypeEvaluator<PointF> {
    private PointF pointF;

    public PointFEvaluator() {
    }

    public PointFEvaluator(PointF pointF) {
        this.pointF = pointF;
    }

    @Override
    public PointF evaluate(float fraction, PointF startValue, PointF endValue) {
        float x = startValue.x + fraction * (endValue.x - startValue.x);
        float y = startValue.y + fraction * (endValue.y - startValue.y);
        if (pointF != null) {
            pointF.set(x, y);
            return pointF;
        } else {
            return new PointF(x, y);
        }
    }
}
