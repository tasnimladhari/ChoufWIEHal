package com.choufwie.hal.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/** Minimal bar chart (values 0..100) used for the 7-day forecast and the stress trend. */
public class BarChartView extends View {
    private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private float[] vals = new float[0];
    private String[] labels = new String[0];

    public BarChartView(Context c, AttributeSet a) {
        super(c, a);
        txt.setTextAlign(Paint.Align.CENTER);
        txt.setTextSize(11 * c.getResources().getDisplayMetrics().density);
        txt.setColor(0xFF5A4A63);
    }

    public void setData(float[] v, String[] l) {
        vals = v;
        labels = l;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float d = getResources().getDisplayMetrics().density;
        if (vals.length == 0) {
            c.drawText("No data yet: do a check-in", getWidth() / 2f, getHeight() / 2f, txt);
            return;
        }
        float w = getWidth(), h = getHeight(), top = 18 * d, bottom = 22 * d;
        float slot = w / vals.length, barW = slot * 0.55f;
        for (int i = 0; i < vals.length; i++) {
            float bh = (h - top - bottom) * Math.max(0, Math.min(100, vals[i])) / 100f;
            float left = i * slot + (slot - barW) / 2f;
            rect.set(left, h - bottom - bh, left + barW, h - bottom);
            bar.setColor(Ui.levelColor((int) vals[i]));
            c.drawRoundRect(rect, 6 * d, 6 * d, bar);
            c.drawText(String.valueOf((int) vals[i]), left + barW / 2f, h - bottom - bh - 4 * d, txt);
            c.drawText(labels[i], i * slot + slot / 2f, h - 6 * d, txt);
        }
    }
}
