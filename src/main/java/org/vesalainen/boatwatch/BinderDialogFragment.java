/*
 * Copyright (C) 2015 Timo Vesalainen
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

import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import static org.vesalainen.boatwatch.BoatWatchConstants.*;

/**
 *
 * @author Timo Vesalainen
 */
public class BinderDialogFragment extends DialogFragment
{
    protected final int resTitle;
    protected AnchorWatchService.AnchorWatchBinder binder;
    protected boolean bound;

    public BinderDialogFragment(int resTitle)
    {
        this.resTitle = resTitle;
    }
    
    protected ServiceConnection connection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            Log.d(LogTitle, "onServiceConnected");
            binder = (AnchorWatchService.AnchorWatchBinder) service;
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            Log.d(LogTitle, "onServiceDisconnected");
            bound = false;
        }

    };

    @Override
    public void onStart()
    {
        Log.d(LogTitle, "onStart");
        super.onStart();
        
        Intent intent = new Intent(getActivity(), AnchorWatchService.class);
        getActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop()
    {
        Log.d(LogTitle, "onStop");
        if (bound)
        {
            getActivity().unbindService(connection);
        }
        super.onStop();
    }

}
