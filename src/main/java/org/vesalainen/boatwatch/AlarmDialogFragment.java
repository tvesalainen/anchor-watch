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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import org.vesalainen.boatwatch.AnchorWatchService.AnchorWatchBinder;

/**
 *
 * @author Timo Vesalainen
 */
public class AlarmDialogFragment extends DialogFragment
{
    private AnchorWatchBinder binder;
    private boolean bound;

    private ServiceConnection connection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            Log.d("AlarmDialogFragment", "onServiceConnected");
            binder = (AnchorWatchService.AnchorWatchBinder) service;
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            Log.d("AlarmDialogFragment", "onServiceDisconnected");
            bound = false;
        }

    };

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.alarm_dialog_title).setPositiveButton(R.string.alarm_dialog_mute, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                binder.mute();
            }
        }).setNegativeButton(R.string.alarm_dialog_exit, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                binder.stop();
                getActivity().finish();
            }
        });
        return builder.create();
    }
    
    @Override
    public void onStart()
    {
        Log.d("AnchorWatchFragment", "onStart");
        super.onStart();
        
        Intent intent = new Intent(getActivity(), AnchorWatchService.class);
        getActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop()
    {
        Log.d("AnchorWatchFragment", "onStop");
        if (bound)
        {
            getActivity().unbindService(connection);
        }
        super.onStop();
    }

}
