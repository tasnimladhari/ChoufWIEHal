package com.choufwie.hal.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/** Semi-circular risk gauge 0..100 (use 240dp x 130dp). */
public class GaugeView extends View {
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private int value = 0;

    public GaugeView(Context c, AttributeSet a) {
        super(c, a);
        float d = c.getResources().getDisplayMetrics().density;
        for (Paint p : new Paint[]{track, arc}) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(18 * d);
            p.setStrokeCap(Paint.Cap.ROUND);
        }
        track.setColor(0xFFE6DCEB);
        txt.setTextAlign(Paint.Align.CENTER);
        txt.setTextSize(36 * d);
        txt.setFakeBoldText(true);
    }

    public void setValue(int v) {
        value = Math.max(0, Math.min(100, v));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float d = getResources().getDisplayMetrics().density;
        float pad = 14 * d, w = getWidth(), dia = w - 2 * pad;
        rect.set(pad, pad, w - pad, pad + dia);
        c.drawArc(rect, 180, 180, false, track);
        arc.setColor(Ui.levelColor(value));
        c.drawArc(rect, 180, 180f * value / 100f, false, arc);
        txt.setColor(Ui.levelColor(value));
        c.drawText(String.valueOf(value), w / 2f, pad + dia / 2f - 6 * d, txt);
    }
}
