package org.vesalainen.boatwatch;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import static org.vesalainen.boatwatch.BoatWatchConstants.*;
import static org.vesalainen.boatwatch.Settings.Simulate;
import org.vesalainen.util.AbstractProvisioner.Setting;

public class AnchorWatchFragment extends Fragment
{
    public static final String Tag = "org.vesalainen.boatwatch.AnchorWatchFragment";
    private AnchorView anchorView;
    private BoatWatchActivity boatWatchActivity;
    private Intent serviceIntent;
    private AnchorWatchService.AnchorWatchBinder binder;
    private boolean bound;

    private ServiceConnection connection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            Log.d("BoatWatchActivity", "onServiceConnected");
            binder = (AnchorWatchService.AnchorWatchBinder) service;
            binder.addWatcher(anchorView);
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            Log.d("BoatWatchActivity", "onServiceDisconnected");
            binder.removeWatcher(anchorView);
            bound = false;
        }

    };
    private String action;

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
    }

    @Override
    public void onStart()
    {
        Log.d("AnchorWatchFragment", "onStart");
        super.onStart();
        
        Settings.attach(boatWatchActivity, this);
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
        Settings.detach(boatWatchActivity, this);
        if (bound)
        {
            boatWatchActivity.unbindService(connection);
        }
        super.onStop();
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
        Log.d(LogTitle, "setSimulate("+simulate+")");
        anchorView.setSimulate(simulate);
    }

    void setAlarm()
    {
        AlarmDialogFragment adf = new AlarmDialogFragment();
        adf.show(getFragmentManager(), null);
    }

    public class AlarmDialogFragment extends DialogFragment
    {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.alarm_dialog_title)
                    .setPositiveButton(R.string.alarm_dialog_mute, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            binder.mute();
                        }
                    })
                    .setNegativeButton(R.string.alarm_dialog_exit, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            binder.stop();
                            boatWatchActivity.finish();
                        }
                    });
            return builder.create();
        }

    }
}
