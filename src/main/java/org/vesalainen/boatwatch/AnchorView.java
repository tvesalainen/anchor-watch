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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import org.ejml.data.DenseMatrix64F;
import static org.vesalainen.boatwatch.BoatWatchConstants.LogTitle;
import org.vesalainen.math.Circle;
import org.vesalainen.math.ConvexPolygon;
import org.vesalainen.math.Sector;
import org.vesalainen.navi.AnchorWatch;
import org.vesalainen.ui.AbstractView;
import org.vesalainen.ui.MouldableSector;
import org.vesalainen.ui.MouldableSectorWithInnerCircle;
import org.vesalainen.util.navi.Angle;
import org.vesalainen.util.navi.Feet;

/**
 *
 * @author Timo Vesalainen
 */
public class AnchorView extends View implements AnchorWatch.Watcher
{
    private final Paint pointPaint;
    private final Paint areaPaint;
    private final Paint backgroundPaint;
    private final Paint estimatedPaint;
    private final Paint manualPaint;
    private final Drawer drawer = new Drawer();
    private double lastX;
    private double lastY;
    private ConvexPolygon area;
    private Circle estimated;
    private MouldableSectorWithInnerCircle safe;
    private MouldableSector.Cursor cursor;
    private boolean simulate;
    private String distanceUnit = "m";

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
        backgroundPaint = new Paint();
        backgroundPaint.setStyle(Paint.Style.FILL);
        pointPaint = new Paint();
        pointPaint.setStyle(Paint.Style.STROKE);
        pointPaint.setARGB(255, 255, 255, 255);
        areaPaint = new Paint();
        areaPaint.setStyle(Paint.Style.STROKE);
        areaPaint.setARGB(255, 0, 0, 255);
        estimatedPaint = new Paint();
        estimatedPaint.setStyle(Paint.Style.STROKE);
        estimatedPaint.setARGB(255, 255, 0, 0);
        manualPaint = new Paint();
        manualPaint.setStyle(Paint.Style.STROKE);
        manualPaint.setARGB(255, 0, 255, 0);
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
        canvas.drawARGB(255, 0, 0, 0);
        drawer.setCanvas(canvas);
        if (simulate)
        {
            drawer.drawText("Simulate", (int)getX()/2, (int)getY()/2, pointPaint);
        }
        drawer.drawPoint(lastX, lastY, pointPaint);
        if (area != null)
        {
            drawer.drawPolygon(area, areaPaint);
        }
        if (estimated != null)
        {
            drawer.drawCircle(estimated, estimatedPaint);
        }
        if (safe != null)
        {
            drawer.drawSectorWithInnerCircle(safe, manualPaint);
            double x = safe.getX();
            double y = safe.getY();
            double r = safe.getRadius();
            drawer.drawLine(x, y, x + r, y, manualPaint);
            String txt = getDistance(r);
            drawer.drawText(txt, x + r / 2, y, manualPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        int action = event.getAction();
        double x = drawer.fromScreenX(event.getX());
        double y = drawer.fromScreenY(event.getY());
        switch (action)
        {
            case MotionEvent.ACTION_DOWN:
                if (safe != null)
                {
                    cursor = safe.getCursor(x, y);
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (cursor != null)
                {
                    cursor = cursor.update(x, y);
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                if (cursor != null)
                {
                    cursor.ready(x, y);
                    cursor = null;
                }
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
    }

    @Override
    public void estimated(Circle estimated)
    {
        drawer.updateCircle(estimated);
        this.estimated = estimated;
        postInvalidate();
    }

    @Override
    public void safeSector(MouldableSectorWithInnerCircle safe)
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
            float sr = (float) scale(circle.getRadius());
            canvas.drawCircle(sx, sy, sr, paint);
        }

        private void drawSectorWithInnerCircle(MouldableSectorWithInnerCircle sector, Paint paint)
        {
            if (sector.isCircle())
            {
                drawCircle(sector, paint);
            }
            else
            {
                drawSector(sector, paint);
                Path path = new Path();
                float sx = (float) toScreenX(sector.getX());
                float sy = (float) toScreenY(sector.getY());
                float sr = (float) scale(sector.getRadius());
                RectF rect = new RectF(
                        sx - sr,
                        sy - sr,
                        sx + sr,
                        sy + sr
                );
                float la = (float) sector.getLeftAngle();
                float ra = (float) sector.getRightAngle();
                float sweep = 360 - (float) Math.toDegrees(Angle.normalizeToFullAngle(Angle.angleDiff(ra, la)));
                float dr = 360 - (float) Math.toDegrees(ra);
                path.addArc(rect, dr, sweep);
                Log.d(LogTitle, "dr="+dr+" sweep="+sweep);
                Rect safeBounds = canvas.getClipBounds();
                canvas.clipPath(path);
                drawCircle(sector.getInnerCircle(), paint);
                canvas.clipRect(safeBounds);
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
                float sx = (float) toScreenX(sector.getX());
                float sy = (float) toScreenY(sector.getY());
                float sr = (float) scale(sector.getRadius());
                RectF rect = new RectF(
                        sx - sr,
                        sy - sr,
                        sx + sr,
                        sy + sr
                );
                float la = (float) sector.getLeftAngle();
                float ra = (float) sector.getRightAngle();
                float sweep = 360 - (float) Math.toDegrees(Angle.normalizeToFullAngle(Angle.angleDiff(la, ra)));
                float dl = 360 - (float) Math.toDegrees(la);
                canvas.drawArc(rect, dl, sweep, true, paint);
            }
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

    }

}
