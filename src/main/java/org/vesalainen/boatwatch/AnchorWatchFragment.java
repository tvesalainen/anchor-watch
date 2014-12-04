package org.vesalainen.boatwatch;

import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceFragment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import org.ejml.data.DenseMatrix64F;
import org.vesalainen.boatwatch.AnchorWatchService.AnchorWatchBinder;
import static org.vesalainen.boatwatch.BoatWatchConstants.*;
import org.vesalainen.math.Circle;
import org.vesalainen.math.ConvexPolygon;
import org.vesalainen.math.Sector;
import org.vesalainen.navi.AnchorWatch;
import org.vesalainen.navi.AnchorWatch.Watcher;
import org.vesalainen.ui.AbstractView;
import org.vesalainen.ui.MouldableSector;
import org.vesalainen.ui.MouldableSector.Cursor;
import org.vesalainen.util.navi.Angle;

public class AnchorWatchFragment extends Fragment
{
    private AnchorView anchorView;
    private AnchorWatchBinder binder;
    private boolean bound;

    private ServiceConnection connection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            Log.d(LogTitle, "onServiceConnected " + anchorView);
            binder = (AnchorWatchService.AnchorWatchBinder) service;
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
    private Activity activity;

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        anchorView = (AnchorView) inflater.inflate(R.layout.anchor_view, container, false);
        return anchorView;
    }


    @Override
    public void onDestroy()
    {
        if (serviceIntent != null)
        {
            activity.stopService(serviceIntent);
        }
        super.onDestroy();
    }

    @Override
    public void onStop()
    {
        if (bound)
        {
            activity.unbindService(connection);
            binder.removeWatcher(anchorView);
        }
        super.onStop();
    }

    @Override
    public void onStart()
    {
        super.onStart();
        
        Intent intent = new Intent(activity, AnchorWatchService.class);
        activity.bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        activity = getActivity();
        serviceIntent = new Intent(activity, AnchorWatchService.class);
        activity.startService(serviceIntent);

    }


}
