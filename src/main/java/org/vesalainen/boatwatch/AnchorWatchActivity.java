package org.vesalainen.boatwatch;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import java.util.HashMap;
import java.util.Map;
import org.ejml.data.DenseMatrix64F;
import org.vesalainen.boatwatch.AnchorWatchService.AnchorWatchBinder;
import org.vesalainen.math.Circle;
import org.vesalainen.math.ConvexPolygon;
import org.vesalainen.math.Sector;
import org.vesalainen.navi.AnchorWatch.Watcher;
import org.vesalainen.ui.AbstractView;
import org.vesalainen.ui.MouldableSector;
import org.vesalainen.ui.MouldableSector.Cursor;
import org.vesalainen.util.navi.Angle;

public class AnchorWatchActivity extends Activity
{
    public static final String AnchorWatch = "AnchorWatch";
    private AnchorView anchorView;
    private AnchorWatchBinder binder;
    private boolean bound;

    private ServiceConnection connection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            Log.d(AnchorWatch, "onServiceConnected "+anchorView);
            binder = (AnchorWatchBinder) service;
            binder.addWatcher(anchorView);
            bound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            binder.removeWatcher(anchorView);
            bound = false;
        }
           
    };
    private Intent serviceIntent;
    /**
     * Called when the activity is first created.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     * previously being shut down then this Bundle contains the data it most
     * recently supplied in onSaveInstanceState(Bundle). <b>Note: Otherwise it
     * is null.</b>
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.d(AnchorWatch, "onCreate");
        
        anchorView = new AnchorView(getBaseContext());
        setContentView(anchorView);

        serviceIntent = new Intent(this, AnchorWatchService.class);
        startService(serviceIntent);
        
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        Log.d(AnchorWatch, "onStart");
        Intent intent = new Intent(this, AnchorWatchService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);        
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        if (bound)
        {
            unbindService(connection);
            binder.removeWatcher(anchorView);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(org.vesalainen.boatwatch.R.menu.main, menu);
        return true;
    }

    @Override
    protected void onDestroy()
    {
        if (serviceIntent != null)
        {
            stopService(serviceIntent);
        }
        super.onDestroy();
    }

    private class AnchorView extends View implements Watcher
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
        private MouldableSector safe;
        private Cursor cursor;

        public AnchorView(Context context)
        {
            super(context);
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

        @Override
        protected void onDraw(Canvas canvas)
        {
            super.onDraw(canvas);
            canvas.drawARGB(255, 0, 0, 0);
            drawer.setCanvas(canvas);
            drawer.drawPoint(lastX, lastX, pointPaint);
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
                drawer.drawSector(safe, manualPaint);
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
                        Log.d(AnchorWatch, "down "+x+", "+y);
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (cursor != null)
                    {
                        cursor = cursor.update(x, y);
                        Log.d(AnchorWatch, "leftAngle="+Math.toDegrees(safe.getLeftAngle()));
                        Log.d(AnchorWatch, "rightAngle="+Math.toDegrees(safe.getRightAngle()));
                        invalidate();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    cursor = null;
                    Log.d(AnchorWatch, "up "+x+", "+y);
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
        public void safeSector(MouldableSector safe)
        {
            drawer.updateCircle(safe);
            this.safe = safe;
            postInvalidate();
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
                        sx-sr, 
                        sy-sr, 
                        sx+sr, 
                        sy+sr
                );
                float la = (float) sector.getLeftAngle();
                float ra = (float) sector.getRightAngle();
                float sweep = 360-(float) Math.toDegrees(Angle.normalizeToFullAngle(Angle.angleDiff(ra, la)));
                float dr = 360-(float) Math.toDegrees(ra);
                Log.d(AnchorWatch, "drawArc "+dr+", "+sweep);
                canvas.drawArc(rect, dr, sweep, true, paint);
            }
        }
        private void drawText(String text, int x, int y, Paint paint)
        {
            canvas.drawText(text, x, y, paint);
        }

        private void drawText(int line, String text, Paint paint)
        {
            canvas.drawText(text, 5, 20 * (line + 1), paint);
        }

        private void drawPolygon(ConvexPolygon area, Paint paint)
        {
            DenseMatrix64F m = area.points;
            double[] d = m.data;
            int rows = m.numRows;
            if (rows >= 2)
            {
                int x1 = (int) toScreenX(d[2*(rows-1)]);
                int y1 = (int) toScreenY(d[2*(rows-1)+1]);
                for (int r=0;r<rows;r++)
                {
                    int x2 = (int) toScreenX(d[2*r]);
                    int y2 = (int) toScreenY(d[2*r+1]);
                    canvas.drawLine(x1, y1, x2, y2, paint);
                    x1 = x2;
                    y1 = y2;
                }
            }
        }

    }

}
