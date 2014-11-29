package org.vesalainen.boatwatch;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.ArcShape;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import org.ejml.data.DenseMatrix64F;
import org.vesalainen.boatwatch.AnchorWatchService.AnchorWatchBinder;
import org.vesalainen.math.Circle;
import org.vesalainen.math.CircleFitter;
import org.vesalainen.math.ConvexPolygon;
import org.vesalainen.math.Sector;
import org.vesalainen.navi.AnchorWatch.Watcher;
import org.vesalainen.ui.AbstractView;
import org.vesalainen.ui.MouldableSector;

public class AnchorWatchActivity extends Activity
{
    private static final String AnchorWatch = "AnchorWatch";
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

        private final Drawer drawer = new Drawer();
        private double lastX;
        private double lastY;
        private ConvexPolygon area;
        private Circle estimated;
        private MouldableSector safe;

        public AnchorView(Context context)
        {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas)
        {
            super.onDraw(canvas);
            canvas.drawARGB(255, 0, 0, 0);
            drawer.setCanvas(canvas);
            drawer.drawPoint(lastX, lastX);
            if (area != null)
            {
                drawer.drawPolygon(area);
            }
            if (estimated != null)
            {
                drawer.drawCircle(estimated);
            }
            if (safe != null)
            {
                drawer.drawSector(safe);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event)
        {
            int action = event.getAction();
            switch (action)
            {
                case MotionEvent.ACTION_DOWN:
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
            invalidate();
        }

        @Override
        public void area(ConvexPolygon area)
        {
            drawer.updatePolygon(area);
            this.area = area;
            invalidate();
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
            invalidate();
        }

        @Override
        public void safeSector(MouldableSector safe)
        {
            drawer.updateCircle(safe);
            this.safe = safe;
            invalidate();
        }

    }

    private class Drawer extends AbstractView
    {

        private final Paint white;
        private final Paint black;
        private final Paint red;
        private final Paint green;
        private Canvas canvas;

        public Drawer()
        {
            super();
            black = new Paint();
            black.setStyle(Paint.Style.FILL);
            white = new Paint();
            white.setStyle(Paint.Style.STROKE);
            white.setARGB(255, 255, 255, 255);
            red = new Paint();
            red.setStyle(Paint.Style.STROKE);
            red.setARGB(255, 255, 0, 0);
            green = new Paint();
            green.setStyle(Paint.Style.STROKE);
            green.setARGB(255, 0, 255, 0);
        }

        private void setCanvas(Canvas canvas)
        {
            this.canvas = canvas;
            setScreen(canvas.getWidth(), canvas.getHeight());
        }

        private void drawPoint(double x, double y)
        {
            float tx = (float) toScreenX(x);
            float ty = (float) toScreenY(y);
            canvas.drawCircle(tx, ty, 5, white);
        }
        private void drawCircle(Circle circle)
        {
            float sx = (float) toScreenX(circle.getX());
            float sy = (float) toScreenY(circle.getY());
            float sr = (float) scale(circle.getRadius());
            canvas.drawCircle(sx, sy, sr, green);
        }
        private void drawSector(Sector sector)
        {
            float x = (float) toScreenX(sector.getX());
            float y = (float) toScreenY(sector.getY());
            float r = (float) scale(sector.getRadius());
            RectF rect = new RectF(
                    x-r, 
                    y-r, 
                    x+r, 
                    y+r
            );
            canvas.drawArc(
                    rect, 
                    (float)Math.toDegrees(sector.getRightAngle()), 
                    (float)Math.toDegrees(sector.getLeftAngle()),
                    true, 
                    red);
        }
        private void drawText(String text, int x, int y)
        {
            canvas.drawText(text, x, y, white);
        }

        private void drawText(int line, String text)
        {
            canvas.drawText(text, 5, 20 * (line + 1), white);
        }

        private void drawPolygon(ConvexPolygon area)
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
                    canvas.drawLine(x1, y1, x2, y2, white);
                    x1 = x2;
                    y1 = y2;
                }
            }
        }

    }

}
