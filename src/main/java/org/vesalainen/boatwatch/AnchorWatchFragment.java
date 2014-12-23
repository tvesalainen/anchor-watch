package org.vesalainen.boatwatch;

import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import org.vesalainen.boatwatch.AnchorWatchService.AnchorWatchBinder;
import static org.vesalainen.boatwatch.BoatWatchConstants.*;
import static org.vesalainen.boatwatch.Settings.DistanceUnit;
import static org.vesalainen.boatwatch.Settings.Simulate;
import org.vesalainen.util.AbstractProvisioner.Setting;

public class AnchorWatchFragment extends Fragment
{
    public static final String Tag = "org.vesalainen.boatwatch.AnchorWatchFragment";
    private AnchorView anchorView;
    private BoatWatchActivity boatWatchActivity;
    private Intent serviceIntent;
    private AnchorWatchBinder binder;
    private boolean bound;

    private ServiceConnection connection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            Log.d("AnchorWatchFragment", "onServiceConnected");
            binder = (AnchorWatchService.AnchorWatchBinder) service;
            binder.addWatcher(anchorView);
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            Log.d("AnchorWatchFragment", "onServiceDisconnected");
            binder.removeWatcher(anchorView);
            bound = false;
        }

    };
    private float touchRadius;

    @Override
    public void onAttach(Activity activity)
    {
        Log.d("AnchorWatchFragment", "onAttach");
        super.onAttach(activity);
        boatWatchActivity = (BoatWatchActivity) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        Log.d("AnchorWatchFragment", "onCreate");
        super.onCreate(savedInstanceState);
        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        touchRadius = 24*metrics.density;
        Log.d("AnchorWatchFragment", "touchRadius="+touchRadius);
        setHasOptionsMenu(true);
        serviceIntent = new Intent(boatWatchActivity, AnchorWatchService.class);
        boatWatchActivity.startService(serviceIntent);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        Log.d("AnchorWatchFragment", "onCreateView");
        return inflater.inflate(R.layout.anchor_view, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        Log.d("AnchorWatchFragment", "onActivityCreated");
        super.onActivityCreated(savedInstanceState);
        anchorView = (AnchorView) boatWatchActivity.findViewById(R.id.anchor_view);
        if (anchorView == null)
        {
            Log.e(LogTitle, "AnchorView not found for id="+R.id.anchor_view);
        }
        anchorView.setTouchRadius(touchRadius);
        Settings.attach(boatWatchActivity, this);
    }

    @Override
    public void onStart()
    {
        Log.d("AnchorWatchFragment", "onStart");
        super.onStart();
        
        Intent intent = new Intent(boatWatchActivity, AnchorWatchService.class);
        boatWatchActivity.bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_exit:
                binder.stop();
                boatWatchActivity.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStop()
    {
        Log.d("AnchorWatchFragment", "onStop");
        if (bound)
        {
            boatWatchActivity.unbindService(connection);
        }
        super.onStop();
    }

    @Override
    public void onDetach()
    {
        Settings.detach(boatWatchActivity, this);
        super.onDetach();
    }

    @Override
    public void onDestroy()
    {
        Log.d("AnchorWatchFragment", "onDestroy");
        super.onDestroy();
    }

    @Setting(Simulate)
    public void setSimulate(boolean simulate)
    {
        anchorView.setSimulate(simulate);
    }

    @Setting(DistanceUnit)
    public void setDistanceUnit(String distanceUnit)
    {
        anchorView.setDistanceUnit(distanceUnit);
    }

}
