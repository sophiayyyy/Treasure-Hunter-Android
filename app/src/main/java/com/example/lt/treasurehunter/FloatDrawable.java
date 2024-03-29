package com.example.lt.treasurehunter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

public class FloatDrawable extends Drawable {

    private Context mContext;
    private int offset = 50;
    private Paint mLinePaint = new Paint();
    private Paint mLinePaint2 = new Paint();

    {
        mLinePaint.setARGB(200, 50, 50, 50);
        mLinePaint.setStrokeWidth(1F);
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setAntiAlias(true);
        mLinePaint.setColor(Color.WHITE);
        //
        mLinePaint2.setARGB(200, 50, 50, 50);
        mLinePaint2.setStrokeWidth(7F);
        mLinePaint2.setStyle(Paint.Style.STROKE);
        mLinePaint2.setAntiAlias(true);
        mLinePaint2.setColor(Color.WHITE);
    }

    public FloatDrawable(Context context) {
        super();
        this.mContext = context;

    }

    public int getBorderWidth() {
        return dipToPx(mContext, offset);//According to the pixel value calculated by dip
    }

    public int getBorderHeight() {
        return dipToPx(mContext, offset);
    }

    @Override
    public void draw(Canvas canvas) {

        int left = getBounds().left;
        int top = getBounds().top;
        int right = getBounds().right;
        int bottom = getBounds().bottom;

        Rect mRect = new Rect(left + dipToPx(mContext, offset) / 2,
                top + dipToPx(mContext, offset) / 2,
                right - dipToPx(mContext, offset) / 2,
                bottom - dipToPx(mContext, offset) / 2);
        //draw default rectangle
        canvas.drawRect(mRect, mLinePaint);
        //draw four corners
        canvas.drawLine((left + dipToPx(mContext, offset) / 2 - 3.5f),
                top + dipToPx(mContext, offset) / 2,
                left + dipToPx(mContext, offset) - 8f,
                top + dipToPx(mContext, offset) / 2, mLinePaint2);
        canvas.drawLine(left + dipToPx(mContext, offset) / 2,
                top + dipToPx(mContext, offset) / 2,
                left + dipToPx(mContext, offset) / 2,
                top + dipToPx(mContext, offset) / 2 + 30, mLinePaint2);
        canvas.drawLine(right - dipToPx(mContext, offset) + 8f,
                top + dipToPx(mContext, offset) / 2,
                right - dipToPx(mContext, offset) / 2,
                top + dipToPx(mContext, offset) / 2, mLinePaint2);
        canvas.drawLine(right - dipToPx(mContext, offset) / 2,
                top + dipToPx(mContext, offset) / 2 - 3.5f,
                right - dipToPx(mContext, offset) / 2,
                top + dipToPx(mContext, offset) / 2 + 30, mLinePaint2);
        canvas.drawLine((left + dipToPx(mContext, offset) / 2 - 3.5f),
                bottom - dipToPx(mContext, offset) / 2,
                left + dipToPx(mContext, offset) - 8f,
                bottom - dipToPx(mContext, offset) / 2, mLinePaint2);
        canvas.drawLine((left + dipToPx(mContext, offset) / 2),
                bottom - dipToPx(mContext, offset) / 2,
                left + dipToPx(mContext, offset) / 2,
                bottom - dipToPx(mContext, offset) / 2 - 30f, mLinePaint2);
        canvas.drawLine((right - dipToPx(mContext, offset) + 8f),
                bottom - dipToPx(mContext, offset) / 2,
                right - dipToPx(mContext, offset) / 2,
                bottom - dipToPx(mContext, offset) / 2, mLinePaint2);
        canvas.drawLine((right - dipToPx(mContext, offset) / 2),
                bottom - dipToPx(mContext, offset) / 2 - 30f,
                right - dipToPx(mContext, offset) / 2,
                bottom - dipToPx(mContext, offset) / 2 + 3.5f, mLinePaint2);
    }

    @Override
    public void setBounds(Rect bounds) {
        super.setBounds(new Rect(bounds.left - dipToPx(mContext, offset) / 2,
                bounds.top - dipToPx(mContext, offset) / 2,
                bounds.right + dipToPx(mContext, offset) / 2,
                bounds.bottom + dipToPx(mContext, offset) / 2));
    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(ColorFilter cf) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.UNKNOWN;
    }

    public int dipToPx(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}
