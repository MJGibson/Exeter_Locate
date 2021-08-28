package com.riba2reality.exeterlocateapp;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class DisplayIconView extends View
{
    Paint paint = null;
    Paint mCirclePaint = null;
    Paint mBackgroundPaint = null;

    float MIN_RADIUS_VALUE = 0;
    float mRadius = MIN_RADIUS_VALUE;
    float MAX_RADIUS_VALUE = 100;

    boolean mAnimationOn = true;
    boolean mPaintGoBack = false;


    public DisplayIconView(Context context)
    {
        super(context);
        paint = new Paint();
    }

    public DisplayIconView(Context context, AttributeSet attrs)
    {
        super(context,attrs);
        paint = new Paint();
    }

    @Override
    protected void onDraw(Canvas canvas)
    {

        int w = getMeasuredWidth();
        int h = getMeasuredHeight();
        mCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mCirclePaint.setColor(Color.RED);
        mBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mBackgroundPaint.setColor(Color.RED);
        mBackgroundPaint.setAlpha((int)(256*0.4));
                //.setColor(Util.adjustAlpha(Color.RED, 0.4f));
        //Draw circle
        canvas.drawCircle(w/2, h/2, MIN_RADIUS_VALUE , mCirclePaint);
        if (mAnimationOn) {
            if (mRadius >= MAX_RADIUS_VALUE)
                mPaintGoBack = true;
            else if(mRadius <= MIN_RADIUS_VALUE)
                mPaintGoBack = false;
            //Draw pulsating shadow
            canvas.drawCircle(w / 2, h / 2, mRadius, mBackgroundPaint);
            mRadius = mPaintGoBack ? (mRadius - 0.5f) : (mRadius + 0.5f);
            invalidate();
        }



        super.onDraw(canvas);
//        int x = getWidth();
//        int y = getHeight();
//        int radius;
//        radius = x/4;
//        paint.setStyle(Paint.Style.FILL);
//        paint.setColor(Color.RED);
//        //canvas.drawPaint(paint);
//        // Use Color.parseColor to define HTML colors
//        //paint.setColor(Color.parseColor("#CD5C5C"));
//        canvas.drawCircle(x / 2, y / 4, radius, paint);
    }// end of onDraw

    public void animateButton(boolean animate){
        if (!animate)
            mRadius = MIN_RADIUS_VALUE;
        mAnimationOn = animate;
        invalidate();
    }

}// end of DisplayIconView