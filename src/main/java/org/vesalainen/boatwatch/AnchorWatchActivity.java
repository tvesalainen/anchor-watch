package org.vesalainen.boatwatch;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import org.ejml.data.DenseMatrix64F;
import org.vesalainen.ui.AbstractView;
import org.vesalainen.util.math.CircleFitter;

public class AnchorWatchActivity extends Activity
{
    private static final int Size = 40;
    private enum State {Gather, Initial, Optimize, Filter, Optimize2 }; 
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
        private Drawer drawer = new Drawer();
        private DenseMatrix64F points = new DenseMatrix64F(Size, 2);
        private DenseMatrix64F center = new DenseMatrix64F(2, 1);
        private int index;
        private State state = State.Gather;
        private CircleFitter circleFitter;
        
        public AnchorView(Context context)
        {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas)
        {
            super.onDraw(canvas);
            drawer.setCanvas(canvas);
            String text = state.name()+" "+index;
            drawer.drawText(text, 5, 20);
            switch (state)
            {
                case Gather:
                    drawer.drawPoints(points, index);
                    break;
                case Initial:
                    CircleFitter.initialCenter(points, center);
                    circleFitter = new CircleFitter(center);
                    drawer.drawPoints(points);
                    state = State.Optimize;
                    postInvalidateDelayed(2000);
                    break;
                case Optimize:
                    circleFitter.fit(points);
                    drawer.drawPoints(points);
                    state = State.Filter;
                    postInvalidateDelayed(2000);
                    break;
                case Filter:
                    CircleFitter.filterInnerPoints(points, center);
                    drawer.drawPoints(points);
                    state = State.Optimize2;
                    postInvalidateDelayed(2000);
                    break;
                case Optimize2:
                    circleFitter.fit(points);
                    drawer.drawPoints(points);
                    state = State.Gather;
                    points.reshape(Size, 2);
                    index = 0;
                    break;
            }
            if (circleFitter != null)
            {
                drawer.drawCircle(circleFitter);
            }
        }

        public void onLocationChanged(Location location)
        {
            if (state == State.Gather)
            {
                double latitude = location.getLatitude();
                double longitude = Math.cos(Math.toRadians(latitude))*location.getLongitude();
                drawer.update(longitude, latitude);
                points.set(index, 0, longitude);
                points.set(index, 1, latitude);
                index++;
                if (index == points.numRows)
                {
                    if (circleFitter == null)
                    {
                        state = State.Initial;
                    }
                    else
                    {
                        state = State.Optimize;
                    }
                }
                postInvalidateDelayed(1000);
            }
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
        
    }
    private class Drawer extends AbstractView
    {
        private Paint paint;
        private Canvas canvas;

        public Drawer()
        {
            super();
            paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setAntiAlias(true);
        }

        private void setCanvas(Canvas canvas)
        {
            this.canvas = canvas;
            setScreen(canvas.getWidth(), canvas.getHeight());
        }

        private void drawCircle(float x, float y, float r)
        {
            float sx = (float) translateX(x);
            float sy = (float) translateY(y);
            float sr = (float) scale(r);
            int w = canvas.getWidth();
            int h = canvas.getHeight();
            String text = "x="+x+" y="+y+" sx="+sx+" sy="+sy+" sr="+sr+" w="+w+" h="+h+" xMax="+xMax+" scale="+scale;
            canvas.drawText(text, 5, 40, paint);
            canvas.drawCircle((float) translateX(x), (float) translateY(y), (float) scale(r), paint);
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
                float tx = (float) translateX(x);
                float ty = (float) translateY(y);
                //String text = "x="+x+" y="+y+" tx="+tx+" ty="+ty;
                //canvas.drawText(text, 5, 20*(row+2), paint);
                canvas.drawPoint(tx, ty, paint);
            }
        }

        private void drawCircle(CircleFitter circleFitter)
        {
            DenseMatrix64F center = circleFitter.getCenter();
            float x = (float)center.get(0, 0);
            float y = (float)center.get(0, 1);
            float radius = (float) circleFitter.getRadius();
            drawLine(4, "center=("+x+", "+y+") r="+radius);
            drawLine(5, "Init cost="+circleFitter.getLevenbergMarquardt().getInitialCost());
            drawLine(6, "Final cost="+circleFitter.getLevenbergMarquardt().getFinalCost());
            drawCircle(x, y, radius);
        }

        private void drawText(String text, int x, int y)
        {
            canvas.drawText(text, x, y, paint);
        }
        
        private void drawLine(int line, String text)
        {
            canvas.drawText(text, 5, 20*(line+1), paint);
        }
        
    }
}
