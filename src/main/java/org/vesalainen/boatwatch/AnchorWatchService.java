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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.vesalainen.navi.AnchorWatch;
import org.vesalainen.navi.AnchorWatch.Watcher;
import org.vesalainen.navi.AnchorageSimulator;

/**
 *
 * @author Timo Vesalainen
 */
public class AnchorWatchService extends Service implements LocationListener
{
    private final AnchorWatch watch = new AnchorWatch();
    private LocationManager locationManager;
    private final IBinder binder = new AnchorWatchBinder();
    private AnchorageSimulator simulator;
    private boolean simulate = true;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Toast.makeText(this, "onStartCommand", Toast.LENGTH_SHORT).show();
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        Toast.makeText(this, "onCreate", Toast.LENGTH_SHORT).show();
        if (!simulate)
        {
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 1, this);
        }
        else
        {
            simulator = new AnchorageSimulator();
            try
            {
                simulator.simulate(watch, 1000, true);
            }
            catch (IOException ex)
            {
                Log.e(AnchorWatchActivity.AnchorWatch, ex.getMessage(), ex);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        Toast.makeText(this, "onBind", Toast.LENGTH_SHORT).show();
        return binder;
    }

    @Override
    public void onDestroy()
    {
        Toast.makeText(this, "onDestroy", Toast.LENGTH_SHORT).show();
        if (!simulate)
        {
            locationManager.removeUpdates(this);
        }
        super.onDestroy();
    }

    @Override
    public void onLocationChanged(Location location)
    {
        watch.update(location.getLongitude(), location.getLatitude());
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras)
    {
    }

    @Override
    public void onProviderEnabled(String provider)
    {
    }

    @Override
    public void onProviderDisabled(String provider)
    {
    }
    
    public class AnchorWatchBinder extends Binder
    {
        void addWatcher(Watcher watcher)
        {
            watch.addWatcher(watcher);
        }
        void removeWatcher(Watcher watcher)
        {
            watch.removeWatcher(watcher);
        }
    }
}
