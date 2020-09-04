package com.finogeeks.finochatapp.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.finogeeks.utility.utils.ResourceKt;


public class TabIconView extends View {

    /**
     * 改变透明度
     */
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);

    /**
     * focus下bitmap
     */
    private Bitmap mSelectedIcon;

    /**
     * normal下bitmap
     */
    private Bitmap mNormalIcon;

    /**
     * normal bitmap矩阵
     */
    private Rect rect;

    private int mSelectedAlpha = 0;

    private ColorFilter tint = new PorterDuffColorFilter(ResourceKt.attrColor(getContext(),
            com.finogeeks.finochat.R.attr.TP_color_normal), PorterDuff.Mode.SRC_IN);

    public TabIconView(Context context) {
        super(context);
    }

    public TabIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TabIconView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public final void init(int normal, int selected, int width, int height) {
        mNormalIcon = BitmapFactory.decodeResource(getResources(), normal);
        mSelectedIcon = BitmapFactory.decodeResource(getResources(), selected);
        rect = new Rect(0, 0, width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mPaint.setAlpha(255 - mSelectedAlpha);
        mPaint.setColorFilter(null);
        canvas.drawBitmap(mNormalIcon, null, rect, mPaint);

        mPaint.setAlpha(mSelectedAlpha);
        mPaint.setColorFilter(tint);
        canvas.drawBitmap(mSelectedIcon, null, rect, mPaint);
    }

    /**
     * 改变透明度百分比
     *
     * @param offset 百分比
     */
    public final void offsetChanged(float offset) {
        mSelectedAlpha = (int) (255 * (1 - offset));
        invalidate();
    }
}