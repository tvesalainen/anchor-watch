/*
 * Copyright (C) 2014 Timo Vesalainen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.vesalainen.boatwatch;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import java.util.HashMap;
import java.util.Map;
import org.ejml.data.DenseMatrix64F;
import static org.vesalainen.boatwatch.BoatWatchConstants.LogTitle;
import org.vesalainen.math.Circle;
import org.vesalainen.math.ConvexPolygon;
import org.vesalainen.math.Sector;
import org.vesalainen.navi.AnchorWatch;
import org.vesalainen.navi.AnchorWatch.Watcher;
import org.vesalainen.navi.SafeSector;
import org.vesalainen.navi.SafeSector.Cursor;
import org.vesalainen.ui.AbstractView;
import org.vesalainen.util.navi.Angle;
import org.vesalainen.util.navi.Feet;

/**
 *
 * @author Timo Vesalainen
 */
public class AnchorView extends View implements Watcher
{
    private final Paint pointPaint;
    private final Paint areaPaint;
    private final Paint usedAreaPaint;
    private final Paint estimatedCirclePaint;
    private final Paint safeSectorPaint;
    private final Drawer drawer = new Drawer();
    private double lastX;
    private double lastY;
    private ConvexPolygon area;
    private Circle estimated;
    private SafeSector safe;
    private Map<Integer,Cursor> cursorMap = new HashMap<>();
    private boolean simulate;
    private String distanceUnit = "m";
    private float touchRadius;
    private DenseMatrix64F usedArea;

    public AnchorView(Context context)
    {
        this(context, null, 0);
    }

    public AnchorView(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public AnchorView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.AnchorView, 0, 0);
        try
        {
            pointPaint = new Paint();
            pointPaint.setStyle(Paint.Style.STROKE);
            pointPaint.setColor(a.getColor(R.styleable.AnchorView_pointColor, Color.WHITE));
            areaPaint = new Paint();
            areaPaint.setStyle(Paint.Style.STROKE);
            areaPaint.setColor(a.getColor(R.styleable.AnchorView_areaColor, Color.BLUE));
            usedAreaPaint = new Paint();
            usedAreaPaint.setStyle(Paint.Style.STROKE);
            usedAreaPaint.setColor(a.getColor(R.styleable.AnchorView_usedAreaColor, Color.CYAN));
            estimatedCirclePaint = new Paint();
            estimatedCirclePaint.setStyle(Paint.Style.STROKE);
            estimatedCirclePaint.setColor(a.getColor(R.styleable.AnchorView_estimatedCircleColor, Color.MAGENTA));
            safeSectorPaint = new Paint();
            safeSectorPaint.setStyle(Paint.Style.STROKE);
            safeSectorPaint.setColor(a.getColor(R.styleable.AnchorView_safeSectorColor, Color.GREEN));
        }
        finally
        {
            a.recycle();
        }
    }

    public void setSimulate(boolean simulate)
    {
        Log.d(LogTitle, "AnchorView.setSimulate("+simulate+")");
        this.simulate = simulate;
    }
    
    void setDistanceUnit(String distanceUnit)
    {
        this.distanceUnit = distanceUnit;
    }
    
    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        drawer.setCanvas(canvas);
        if (simulate)
        {
            int width = canvas.getWidth();
            int height = canvas.getHeight();
            int h = height / 10;
            String text = getResources().getText(R.string.simulate).toString();
            for (int ii=0,jj=0;ii<height;ii+=h,jj+=h)
            {
                drawer.drawText(text, jj % width, ii, pointPaint);
            }
        }
        drawer.drawPoint(lastX, lastY, pointPaint);
        if (area != null)
        {
            drawer.drawPolygon(area, areaPaint);
        }
        if (usedArea != null)
        {
            drawer.drawPath(area, usedAreaPaint);
            usedArea = null;
        }
        if (estimated != null)
        {
            drawer.drawCircle(estimated, estimatedCirclePaint);
        }
        if (safe != null)
        {
            drawer.drawSectorWithInnerCircle(safe, safeSectorPaint);
            double x = safe.getX();
            double y = safe.getY();
            double r = safe.getRadius();
            drawer.drawLine(x, y, x + r, y, safeSectorPaint);
            String txt = getDistance(r);
            drawer.drawText(txt, x + r / 2, y, safeSectorPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        double r = drawer.scaleFromScreen(touchRadius);
        Cursor cursor;
        
        int pointerIndex = event.getActionIndex();
        int pointerId = event.getPointerId(pointerIndex);
        Log.d(LogTitle, "pointerIndex="+pointerIndex+" pointerId="+pointerId);
        int actionMasked = event.getActionMasked();
        switch (actionMasked)
        {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                if (safe != null)
                {
                    double x = drawer.fromScreenX(event.getX(pointerIndex));
                    double y = drawer.fromScreenY(event.getY(pointerIndex));
                    cursor = safe.getCursor(x, y, r);
                    if (cursor != null)
                    {
                        cursorMap.put(pointerId, cursor);
                        Log.d(LogTitle, "put "+pointerId+" "+cursor);
                        invalidate();
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                int cnt = event.getPointerCount();
                for (int ii=0;ii<cnt;ii++)
                {
                    int p = event.getPointerId(ii);
                    cursor = cursorMap.get(p);
                    if (cursor != null)
                    {
                        Log.d(LogTitle, "use "+p+" "+cursor);
                        double x = drawer.fromScreenX(event.getX(ii));
                        double y = drawer.fromScreenY(event.getY(ii));
                        cursorMap.put(p, cursor.update(x, y));
                        invalidate();
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                cursor = cursorMap.get(pointerId);
                if (cursor != null)
                {
                    double x = drawer.fromScreenX(event.getX(pointerIndex));
                    double y = drawer.fromScreenY(event.getY(pointerIndex));
                    cursor.ready(x, y);
                    cursorMap.remove(pointerId);
                    Log.d(LogTitle, "rem "+pointerId+" "+cursor);
                    drawer.reset();
                    drawer.updatePoint(lastX, lastY);
                    if (area != null)
                    {
                        drawer.updatePolygon(area);
                    }
                    if (estimated != null)
                    {
                        drawer.updateCircle(estimated);
                    }
                    if (safe != null)
                    {
                        drawer.updateCircle(safe);
                    }
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                cursorMap.remove(pointerId);
                break;
        }
        return true;
    }

    public void onStatusChanged(String provider, int status, Bundle extras)
    {
    }

    public void onProviderEnabled(String provider)
    {
    }

    public void onProviderDisabled(String provider)
    {
    }

    @Override
    public void alarm(double distance)
    {
    }

    @Override
    public void location(double x, double y)
    {
        lastX = x;
        lastY = y;
        drawer.updatePoint(x, y);
        postInvalidate();
    }

    @Override
    public void area(ConvexPolygon area)
    {
        drawer.updatePolygon(area);
        this.area = area;
        postInvalidate();
    }

    @Override
    public void outer(DenseMatrix64F path)
    {
        this.usedArea = path;
        postInvalidate();
    }

    @Override
    public void estimated(Circle estimated)
    {
        drawer.updateCircle(estimated);
        this.estimated = estimated;
        postInvalidate();
    }

    @Override
    public void safeSector(SafeSector safe)
    {
        drawer.updateCircle(safe);
        this.safe = safe;
        postInvalidate();
    }

    private String getDistance(double r)
    {
        switch (distanceUnit)
        {
            default:
            case "m":
                return String.format("%.0f m", AnchorWatch.toMeters(r));
            case "ft":
                return String.format("%.0f ft", Feet.fromMeters(AnchorWatch.toMeters(r)));
        }
    }

    public void setTouchRadius(float touchRadius)
    {
        this.touchRadius = touchRadius;
    }

    private class Drawer extends AbstractView
    {

        private Canvas canvas;

        public Drawer()
        {
            super();
        }

        private void setCanvas(Canvas canvas)
        {
            this.canvas = canvas;
            setScreen(canvas.getWidth(), canvas.getHeight());
        }

        private void drawPoint(double x, double y, Paint paint)
        {
            float tx = (float) toScreenX(x);
            float ty = (float) toScreenY(y);
            canvas.drawCircle(tx, ty, 5, paint);
        }

        private void drawCircle(Circle circle, Paint paint)
        {
            float sx = (float) toScreenX(circle.getX());
            float sy = (float) toScreenY(circle.getY());
            float sr = (float) scaleToScreen(circle.getRadius());
            canvas.drawCircle(sx, sy, sr, paint);
        }

        private void drawSectorWithInnerCircle(SafeSector sector, Paint paint)
        {
            if (sector.isCircle())
            {
                drawCircle(sector, paint);
            }
            else
            {
                drawSector(sector, paint);
                RectF rect = getRectF(sector.getInnerCircle());
                float la = (float) sector.getLeftAngle();
                float ra = (float) sector.getRightAngle();
                float sweep = 360 - (float) Math.toDegrees(Angle.normalizeToFullAngle(Angle.angleDiff(ra, la)));
                float dr = 360 - (float) Math.toDegrees(ra);
                canvas.drawArc(rect, dr, sweep, true, paint);
            }
        }
        private void drawSector(Sector sector, Paint paint)
        {
            if (sector.isCircle())
            {
                drawCircle(sector, paint);
            }
            else
            {
                RectF rect = getRectF(sector);
                float la = (float) sector.getLeftAngle();
                float ra = (float) sector.getRightAngle();
                float sweep = 360 - (float) Math.toDegrees(Angle.normalizeToFullAngle(Angle.angleDiff(la, ra)));
                float dl = 360 - (float) Math.toDegrees(la);
                canvas.drawArc(rect, dl, sweep, true, paint);
            }
        }

        private RectF getRectF(Circle circle)
        {
                float sx = (float) toScreenX(circle.getX());
                float sy = (float) toScreenY(circle.getY());
                float sr = (float) scaleToScreen(circle.getRadius());
                return new RectF(
                        sx - sr,
                        sy - sr,
                        sx + sr,
                        sy + sr
                );
        }
        private void drawText(String text, double x, double y, Paint paint)
        {
            int ix = (int) toScreenX(x);
            int iy = (int) toScreenY(y);
            canvas.drawText(text, ix, iy, paint);
        }

        private void drawText(String text, int x, int y, Paint paint)
        {
            canvas.drawText(text, x, y, paint);
        }

        private void drawLine(double x1, double y1, double x2, double y2, Paint paint)
        {
            int ix1 = (int) toScreenX(x1);
            int iy1 = (int) toScreenY(y1);
            int ix2 = (int) toScreenX(x2);
            int iy2 = (int) toScreenY(y2);
            canvas.drawLine(ix1, iy1, ix2, iy2, paint);
        }

        private void drawPolygon(ConvexPolygon area, Paint paint)
        {
            DenseMatrix64F m = area.points;
            double[] d = m.data;
            int rows = m.numRows;
            if (rows >= 2)
            {
                int x1 = (int) toScreenX(d[2 * (rows - 1)]);
                int y1 = (int) toScreenY(d[2 * (rows - 1) + 1]);
                for (int r = 0; r < rows; r++)
                {
                    int x2 = (int) toScreenX(d[2 * r]);
                    int y2 = (int) toScreenY(d[2 * r + 1]);
                    canvas.drawLine(x1, y1, x2, y2, paint);
                    x1 = x2;
                    y1 = y2;
                }
            }
        }

        private void drawPath(ConvexPolygon area, Paint paint)
        {
            DenseMatrix64F m = area.points;
            double[] d = m.data;
            int rows = m.numRows;
            if (rows >= 1)
            {
                int x1 = (int) toScreenX(d[0]);
                int y1 = (int) toScreenY(d[1]);
                for (int r = 1; r < rows; r++)
                {
                    int x2 = (int) toScreenX(d[2 * r]);
                    int y2 = (int) toScreenY(d[2 * r + 1]);
                    canvas.drawLine(x1, y1, x2, y2, paint);
                    x1 = x2;
                    y1 = y2;
                }
            }
        }

    }

}
