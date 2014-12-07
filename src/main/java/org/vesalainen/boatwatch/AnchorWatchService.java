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
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.ejml.data.DenseMatrix64F;
import static org.vesalainen.boatwatch.BoatWatchConstants.*;
import static org.vesalainen.boatwatch.Settings.AlarmTone;
import static org.vesalainen.boatwatch.Settings.Simulate;
import org.vesalainen.math.Circle;
import org.vesalainen.math.ConvexPolygon;
import org.vesalainen.navi.AnchorWatch;
import org.vesalainen.navi.AnchorWatch.Watcher;
import org.vesalainen.navi.AnchorageSimulator;
import org.vesalainen.ui.MouldableSector;
import org.vesalainen.util.AbstractProvisioner.Setting;

/**
 *
 * @author Timo Vesalainen
 */
public class AnchorWatchService extends Service implements LocationListener, Watcher
{
    private static final String BackupFilename = "anchorwatch.ser";
    private AnchorWatch watch;
    private LocationManager locationManager;
    private final IBinder binder = new AnchorWatchBinder();
    private AnchorageSimulator simulator;
    private boolean simulate = true;
    private boolean stopped;
    private String alarmTone;
    private MediaPlayer mediaPlayer;
    private boolean alarmed;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Toast.makeText(this, "onStartCommand", Toast.LENGTH_SHORT).show();
        return START_STICKY;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        try (ObjectInputStream in = new ObjectInputStream(openFileInput(BackupFilename)))
        {
            watch = (AnchorWatch) in.readObject();
            Log.d(LogTitle, "read "+BackupFilename);
        }
        catch (FileNotFoundException ex)
        {
            watch = new AnchorWatch();
            Log.d(LogTitle, "started from cratch");
        }
        catch (IOException | ClassNotFoundException ex)
        {
            Log.e(LogTitle, ex.getMessage(), ex);
            watch = new AnchorWatch();
        }
        watch.addWatcher(this);
        Settings.attach(this);
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return binder;
    }

    @Override
    public void onDestroy()
    {
        watch.removeWatcher(this);
        if (mediaPlayer != null)
        {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (!simulate)
        {
            locationManager.removeUpdates(this);
        }
        Settings.detach(this);
        if (stopped)
        {
            deleteFile(BackupFilename);
            Log.d(LogTitle, "deleted "+BackupFilename);
        }
        else
        {
            try (ObjectOutputStream out = new ObjectOutputStream(openFileOutput(BackupFilename, MODE_PRIVATE)))
            {
                out.writeObject(watch);
                Log.d(LogTitle, "wrote "+BackupFilename);
            }
            catch (IOException ex)
            {
                Log.e(LogTitle, ex.getMessage(), ex);
            }
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
    @Setting(Simulate)
    public void setSimulate(boolean simulate)
    {
        Log.d(LogTitle, "setSimulate("+simulate+")");
        if (simulate)
        {
            if (locationManager != null)
            {
                locationManager.removeUpdates(this);
                locationManager = null;
                watch.reset();
            }
            simulator = new AnchorageSimulator();
            try
            {
                simulator.simulate(watch, 1000, true);
            }
            catch (IOException ex)
            {
                Log.e(LogTitle, ex.getMessage(), ex);
            }
        }
        else
        {
            if (simulator != null)
            {
                simulator.cancel();
                simulator = null;
                watch.reset();
            }
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 1, this);
        }
    }

    @Setting(AlarmTone)
    public void setAlarmTone(String alarmTone)
    {
        this.alarmTone = alarmTone;
    }
    
    @Override
    public void alarm(double distance)
    {
        if (!alarmed)
        {
            alarmed = true;
            if (alarmTone != null && !alarmTone.isEmpty())
            {
                try {
                    mediaPlayer = new MediaPlayer();
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                    mediaPlayer.setDataSource(alarmTone);
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                }
                catch (IOException | IllegalArgumentException | SecurityException | IllegalStateException ex) 
                {
                    Log.e(LogTitle, ex.getMessage(), ex);
                }
            }
        }
    }

    @Override
    public void location(double x, double y)
    {
    }

    @Override
    public void area(ConvexPolygon area)
    {
    }

    @Override
    public void outer(DenseMatrix64F path)
    {
    }

    @Override
    public void estimated(Circle estimated)
    {
    }

    @Override
    public void safeSector(MouldableSector safe)
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
        void stop()
        {
            stopped = true;
            stopSelf();
        }
    }
}
