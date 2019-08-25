package com.example.chargecontrol;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class Battery_View extends View
{
    public static final String TAG = "TAG_BATTERY_VIEW";

    AttributeSet attr_Set;

    int height = 1050;        // Evtl anpassen
    int width = 630;
    int space = 10;

    int Battery_Level;

    Paint mPaint_Outside;
    Paint mPaint_Battery;
    Paint mPaint_Line;
    Rect mRect_Outside;
    Rect mReckt_Battery;

    int x1, x2, y1, y2;

    float angle = 0f;

    public Battery_View(Context context)
    {
        super(context);
        init(null);
    }

    public Battery_View(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(attrs);
    }

    public Battery_View(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public Battery_View(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    void init(@Nullable AttributeSet attrs)
    {
        attr_Set = attrs;
        Battery_Level = 0;
        mPaint_Outside = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint_Battery = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint_Line = new Paint(Paint.ANTI_ALIAS_FLAG);
        mRect_Outside = new Rect();
        mReckt_Battery = new Rect();

        x1 = space;
        x2 = width - space;
        y1 = height/2;
        y2 = y1;

        mRect_Outside.left = 0;
        mRect_Outside.top = 0;
        mRect_Outside.bottom = height;
        mRect_Outside.right = width;

        mPaint_Outside.setColor(getResources().getColor(R.color.Battery_Border));
        mPaint_Outside.setStyle(Paint.Style.STROKE);    // Dass es nicht gefüllt ist

        mPaint_Battery.setColor(getResources().getColor(R.color.Battery_green));
        mPaint_Line.setColor(getResources().getColor(R.color.black));
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        // postInvalidate() wird das aktualisiert, sobald es zeit hat

        canvas.drawRect(mRect_Outside, mPaint_Outside);
        canvas.drawRect(mReckt_Battery, mPaint_Battery);

       // canvas.drawLine(x1, y1, x2, y2, mPaint_Line); // Linie zum Test
    }

    public void draw_Battery_Level(int level)
    {
        Battery_Level = level;

        mRect_Outside.bottom = height;
        mRect_Outside.right = width;

        mReckt_Battery.bottom = height - space;
        mReckt_Battery.right = width - space;
        mReckt_Battery.left = space;
        if(Battery_Level > 0)
            mReckt_Battery.top = space + (int)((float)(height) - (float)(Battery_Level * (height/100.0)));

        postInvalidate();
    }

    public void change_orientation(float angle)
    {
        this.angle = angle;     // Winkel im Bogenmaß (-PI...PI)

        int temp_h = height/2 - space;
        float angle_45_grad = 45 * (float)(Math.PI/180);
        float angle_90_grad = (float)Math.PI / 2;
        float angle_135_grad = 135 * (float)(Math.PI/180);
        float abs_angle = Math.abs(angle);
        boolean negativ = angle < 0;

        if(abs_angle < 0.785398)        // < 45°
        {
            x1 = space;
            y1 = (height/2) - (int)((float)temp_h * Math.sin(abs_angle / angle_45_grad));

            if(negativ)
                y1 = height - y1;
        }
        else if(abs_angle > 2.356194)      // > 135°
        {
            x1 = width - space;
            y1 = space + (int)((float)temp_h * Math.sin((abs_angle - angle_90_grad)/ angle_45_grad));
            if(negativ)
                y1 = height - y1;
        }
        else                            // 45...135
        {
            x1 = space + (int) ((float) (width - 2 * space) * (float) Math.sin(Math.abs((abs_angle - angle_45_grad) / (Math.PI / 2))));      // -> 45° = 0  -> 135° = 1)
            y1 = space;
            if(negativ)
                x1 = width - x1;
        }

        x2 = width - x1;
        y2 = height - y1;

        postInvalidate();
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh)
    {
        height = h;
        width = w;
        draw_Battery_Level(Battery_Level);
        change_orientation(0);
    }
}
