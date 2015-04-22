package com.raidzero.dreamclock.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import com.raidzero.dreamclock.R;
import com.raidzero.dreamclock.global.Debug;
import com.raidzero.dreamclock.global.Utils;

/**
 * Created by posborn on 3/30/15.
 */
public class NumberedIconView extends View {
    private static final String tag = "NumberedIconView";

    private Drawable mIcon;
    private Bitmap mIconBmp;
    private int mCount, mIconWidth, mCountSize, mCountOverlap;
    private Context mContext;

    private Paint mPaint = new Paint();

    private int mLeft, mTop, mRight, mBottom;
    private Rect mIconBounds;

    public NumberedIconView(Context context) {
        super(context);
        mContext = context;

        // get dimensions of stuff from resources
        mCountSize = (int) mContext.getResources().getDimension(R.dimen.notification_count_size);
        mIconWidth = (int) mContext.getResources().getDimension(R.dimen.notification_size);
        mCountOverlap = (int) mContext.getResources().getDimension(R.dimen.notification_count_overlap);

        // set up the paint for notification count
        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setTextSize((float) mCountSize);
    }

    public NumberedIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NumberedIconView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public NumberedIconView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setIconAndCount(Drawable icon, int count) {
        mIcon = icon;
        mCount = count;
        setScales();
    }

    private void setScales() {
        // resize bitmap to fit mIconWidth
        Bitmap iconBmp = ((BitmapDrawable) mIcon).getBitmap();
        Bitmap scaledBmp = Utils.getScaledBitmap(iconBmp, mIconWidth);
        mIconBmp = scaledBmp;
        mIcon = new BitmapDrawable(scaledBmp);

        // set new icon size
        int iconHeight = mIconBmp.getHeight();
        int iconWidth = mIconBmp.getWidth();

        // whole view bounds
        mLeft = 0; mTop = 0;

        // only adjust right bound if count is going to be displayed
        if (mCount > 1) {
            mRight = iconWidth + mCountSize;
        } else {
            mRight = iconWidth;
        }

        mBottom = iconHeight + mCountSize - mCountOverlap;

        // set drawable bounds
        mIconBounds = new Rect(mLeft, mTop + (mCountSize - mCountOverlap), iconWidth, iconHeight + (mCountSize - mCountOverlap));
        mIcon.setBounds(mIconBounds);

        Debug.Log(tag, String.format("mLeft: %d, mTop: %d, mRight: %d, mBottom: %d", mLeft, mTop, mRight, mBottom));
        Debug.Log(tag, String.format("mIconBounds left: %d, top: %d, right: %d, bottom: %d", mLeft, mTop, iconWidth, iconHeight));
        Debug.Log(tag, String.format("text x: %d, y: %d", mIconBounds.right, mIconBounds.top + mCountOverlap));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // draw icon on canvas
        mIcon.draw(canvas);

        if (mCount > 1) {
            // draw text on canvas - to right of icon and just above
            canvas.drawText(String.valueOf(mCount), mIconBounds.right, mIconBounds.top + mCountOverlap, mPaint);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Debug.Log(tag, String.format("mRight: %d, mBottom: %d", mRight, mBottom));

        super.onMeasure(
                MeasureSpec.makeMeasureSpec(mRight, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(mBottom, MeasureSpec.EXACTLY));
    }
}
