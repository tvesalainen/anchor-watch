package org.vesalainen.boatwatch;

import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import org.vesalainen.boatwatch.AnchorWatchService.AnchorWatchBinder;
import static org.vesalainen.boatwatch.BoatWatchConstants.*;
import static org.vesalainen.boatwatch.Settings.Simulate;
import org.vesalainen.util.AbstractProvisioner.Setting;

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
    private Activity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.anchor_view, container, false);
    }


    @Override
    public void onDestroy()
    {
        Settings.detach(activity);
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
        
        anchorView = (AnchorView) activity.findViewById(R.id.anchor_view);
        if (anchorView == null)
        {
            Log.e(LogTitle, "AnchorView not found for id="+R.id.anchor_view);
        }
        Settings.attach(activity);
        Intent intent = new Intent(activity, AnchorWatchService.class);
        activity.bindService(intent, connection, Context.BIND_AUTO_CREATE);
        anchorView.invalidate();
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        activity = getActivity();
        setHasOptionsMenu(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_exit:
                binder.stop();
                activity.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Setting(Simulate)
    public void setSimulate(boolean simulate)
    {
        Log.d(LogTitle, "setSimulate("+simulate+")");
        anchorView.setSimulate(simulate);
    }
    
}
