package org.vesalainen.boatwatch;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import org.ejml.data.DenseMatrix64F;
import org.vesalainen.ui.AbstractView;
import org.vesalainen.util.math.CircleFitter;
import org.vesalainen.util.math.ConvexPolygon;
import org.vesalainen.util.math.Polygon;

public class AnchorWatchActivity extends Activity
{
    private static final double Cable = 1.0/600.0;
    private static final double DegreeToMeters = 36.0/4000000.0;
    private static final double MaxRadius = 50*DegreeToMeters;
    private static final double MinRadius = 30*DegreeToMeters;
    private static final int Size = 10;

    private LocationManager locationManager;
    private AnchorView anchorView;

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
        anchorView = new AnchorView(getBaseContext());
        setContentView(anchorView);
        
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 1, anchorView);
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
        locationManager.removeUpdates(anchorView);
        super.onDestroy();
    }

    private class AnchorView extends View implements LocationListener
    {
        private final Drawer drawer = new Drawer();
        private final DenseMatrix64F points = new DenseMatrix64F(Size, 2);
        private final DenseMatrix64F center = new DenseMatrix64F(2, 1);
        private final DenseMatrix64F meanCenter = new DenseMatrix64F(2, 1);
        private int index;
        private CircleFitter circleFitter;
        private List<P> centers = new ArrayList<>();
        private double cx;
        private double cy;
        private double delta = 0.0;
        private double finalCost = 0;
        private ConvexPolygon polygon = new ConvexPolygon();
        private int locCount = 0;
        private boolean anchorSet = false;
        
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
            drawer.drawText(0, "Count="+locCount);
            drawer.drawPoints(points, index);
            if (circleFitter != null)
            {
                drawer.drawCircle(circleFitter);
            }
        }

        public void onLocationChanged(Location location)
        {
            locCount++;
            if  (locCount < 3)
            {
                return;
            }
            double latitude = location.getLatitude();
            double longitude = Math.cos(Math.toRadians(latitude))*location.getLongitude();
            drawer.update(longitude, latitude);
            points.set(index, 0, longitude);
            points.set(index, 1, latitude);
            index++;
            if (index == points.numRows)
            {
                for (int ii=0;ii<points.numRows;ii++)
                {
                    Log.d("AnchorWatch", "("+points.data[2*ii]+", "+points.data[2*ii+1]+")");
                }
                polygon.updateConvexPolygon(points);
                points.reshape(polygon.points.numRows, 2);
                points.set(polygon.points);
                CircleFitter.meanCenter(points, meanCenter);
                CircleFitter.limitDistance(meanCenter, center, MinRadius, MaxRadius);
                if (circleFitter == null)
                {
                    double radius = CircleFitter.initialCenter(points, center);
                    circleFitter = new CircleFitter(center);
                }
                if (anchorSet)
                {
                    CircleFitter.filterInnerPoints(points, center, points.numRows/3, 0.8);
                }
                circleFitter.fit(points);
                finalCost = circleFitter.getLevenbergMarquardt().getFinalCost();
                index = points.numRows;
                points.reshape(points.numRows+Size, 2, true);
            }
            postInvalidate();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event)
        {
            int action = event.getAction();
            switch (action)
            {
                case MotionEvent.ACTION_DOWN:
                    for (int ii=0;ii<points.numRows;ii++)
                    {
                        Log.d("AnchorWatch", "("+points.data[2*ii]+", "+points.data[2*ii+1]+")");
                    }
                    float x = event.getX();
                    float y = event.getY();
                    center.data[0] = drawer.fromScreenX(x);
                    center.data[1] = drawer.fromScreenY(y);
                    Log.d("AnchorWatch", "("+x+", "+y+") -> ("+center.data[0]+", "+center.data[1]+")");
                    points.reshape(index, 2, true);
                    CircleFitter.filterInnerPoints(points, center, points.numRows/3, 0.8);
                    index = points.numRows;
                    points.reshape(points.numRows+Size, 2, true);
                    CircleFitter.meanCenter(points, meanCenter);
                    Log.d("AnchorWatch", "meanCenter = ("+meanCenter.data[0]+", "+meanCenter.data[1]+")");
                    CircleFitter.limitDistance(meanCenter, center, MinRadius, MaxRadius);
                    Log.d("AnchorWatch", "center = ("+center.data[0]+", "+center.data[1]+")");
                    if (circleFitter == null)
                    {
                        circleFitter = new CircleFitter(center);
                    }
                    else
                    {
                        circleFitter.getCenter().set(center);
                    }
                    circleFitter.fit(points);
                    Log.d("AnchorWatch", "center = ("+center.data[0]+", "+center.data[1]+")");
                    finalCost = circleFitter.getLevenbergMarquardt().getFinalCost();
                    anchorSet = true;
                    postInvalidate();
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

        private double distance(double cx, double cy, double xx, double yy)
        {
            double dx = cx-xx;
            double dy = cy-yy;
            return Math.sqrt(dx*dx+dy*dy);
        }
        
    }
    private class Drawer extends AbstractView
    {
        private Paint white;
        private Paint black;
        private Paint red;
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
        }

        private void setCanvas(Canvas canvas)
        {
            this.canvas = canvas;
            setScreen(canvas.getWidth(), canvas.getHeight());
        }

        private void drawCircle(float x, float y, float r)
        {
            float sx = (float) toScreenX(x);
            float sy = (float) toScreenY(y);
            float sr = (float) scale(r);
            int w = canvas.getWidth();
            int h = canvas.getHeight();
            String text = "x="+x+" y="+y+" sx="+sx+" sy="+sy+" sr="+sr+" w="+w+" h="+h+" xMax="+xMax+" scale="+scale;
            canvas.drawText(text, 5, 40, red);
            canvas.drawCircle((float) toScreenX(x), (float) toScreenY(y), (float) scale(r), white);
        }

        private void drawPoints(DenseMatrix64F matrix)
        {
            drawPoints(matrix, matrix.numRows);
        }
        private void drawPoints(DenseMatrix64F matrix, int count)
        {
            for (int row = 0;row<count;row++)
            {
                double x = matrix.get(row, 0);
                double y = matrix.get(row, 1);
                float tx = (float) toScreenX(x);
                float ty = (float) toScreenY(y);
                if (count == matrix.numRows)
                {
                    Log.d("AnchorWatch", x+","+y+","+tx+","+ty+",");
                }
                //String text = "x="+x+" y="+y+" tx="+tx+" ty="+ty;
                //canvas.drawText(text, 5, 20*(row+2), paint);
                canvas.drawCircle(tx, ty, 10, white);
            }
        }

        private void drawCircle(CircleFitter circleFitter)
        {
            DenseMatrix64F center = circleFitter.getCenter();
            float x = (float)center.data[0];
            float y = (float)center.data[1];
            float radius = (float) circleFitter.getRadius();
            float sx = (float) toScreenX(x);
            float sy = (float) toScreenY(y);
            float sr = (float) scale(radius);
            update(x, y, radius);
            String fmt = String.format("center (%.6f, %.6f) r=%.0f", x, y, radius/DegreeToMeters);
            drawText(4, fmt);
            drawText(5, "Init cost="+circleFitter.getLevenbergMarquardt().getInitialCost());
            drawText(6, "Final cost="+circleFitter.getLevenbergMarquardt().getFinalCost());
            canvas.drawCircle(sx, sy, sr, red);
            canvas.drawCircle(sx, sy, 6, red);
        }

        private void drawText(String text, int x, int y)
        {
            canvas.drawText(text, x, y, white);
        }
        
        private void drawText(int line, String text)
        {
            canvas.drawText(text, 5, 20*(line+1), white);
        }

        private void drawCenters(List<P> centers)
        {
            for (P p : centers)
            {
                float tx = (float) toScreenX(p.x);
                float ty = (float) toScreenY(p.y);
                canvas.drawCircle(tx, ty, 10, red);
            }
        }
    }        
    private static class P
    {
        private final double x;
        private final double y;

        public P(double x, double y)
        {
            this.x = x;
            this.y = y;
        }

        public P(double[] data)
        {
            this.x = data[0];
            this.y = data[1];
        }
    }
}
