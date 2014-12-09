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

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

/**
 *
 * @author Timo Vesalainen
 */
public class BoatWatchActivity extends Activity
{
    public static final String AlarmAction = "org.vesalainen.boatwatch.AlarmAction";

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
        Log.d("BoatWatchActivity", "onCreate");
        AnchorWatchFragment anchorWatchFragment = new AnchorWatchFragment();
        getFragmentManager().beginTransaction()
                .add(android.R.id.content, anchorWatchFragment, AnchorWatchFragment.Tag)
                .commit();    
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        Intent intent = getIntent();
        if (AlarmAction.equals(intent.getAction()))
        {
            anchorWatchFragment.setAlarm();
        }
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        Log.d("BoatWatchActivity", "onNewIntent");
        super.onNewIntent(intent);
        AnchorWatchFragment anchorWatchFragment = (AnchorWatchFragment) getFragmentManager().findFragmentByTag(AnchorWatchFragment.Tag);
        if (anchorWatchFragment == null)
        {
            anchorWatchFragment = new AnchorWatchFragment();
        }
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, anchorWatchFragment, AnchorWatchFragment.Tag)
                .addToBackStack(null)
                .commit();    
        if (AlarmAction.equals(intent.getAction()))
        {
            anchorWatchFragment.setAlarm();
        }
    }

    @Override
    protected void onStart()
    {
        Log.d("BoatWatchActivity", "onStart");
        super.onStart();
    }

    @Override
    protected void onStop()
    {
        Log.d("BoatWatchActivity", "onStop");
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        Log.d("BoatWatchActivity", "onCreateOptionsMenu");
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(org.vesalainen.boatwatch.R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        Log.d("BoatWatchActivity", "onOptionsItemSelected");
        switch (item.getItemId())
        {
            case R.id.action_settings:
                getFragmentManager().beginTransaction()
                        .replace(android.R.id.content, new SettingsFragment())
                        .addToBackStack(null)
                        .commit();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy()
    {
        Log.d("BoatWatchActivity", "onDestroy");
        super.onDestroy();
    }

}
