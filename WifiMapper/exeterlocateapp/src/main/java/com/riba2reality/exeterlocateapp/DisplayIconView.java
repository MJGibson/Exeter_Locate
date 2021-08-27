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
        super.onDraw(canvas);
        int x = getWidth();
        int y = getHeight();
        int radius;
        radius = x/4;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.RED);
        //canvas.drawPaint(paint);
        // Use Color.parseColor to define HTML colors
        //paint.setColor(Color.parseColor("#CD5C5C"));
        canvas.drawCircle(x / 2, y / 4, radius, paint);
    }

}// end of DisplayIconView