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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;
import java.util.Map;
import org.vesalainen.util.AbstractProvisioner;

/**
 *
 * @author Timo Vesalainen
 */
public class Settings
{
    public static final String Simulate = "pref_simulate";
    public static final String AlarmTone = "pref_alarmtone";
    public static final String Mute = "pref_mute_time";
    
    public static Provisioner provisioner;
    
    public static void attach(Context context)
    {
        attach(context, context);
    }
    public static void attach(Context context, Object ob)
    {
        if (provisioner == null)
        {
            provisioner = new Provisioner(context);
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            sharedPreferences.registerOnSharedPreferenceChangeListener(provisioner);
        }
        provisioner.attach(ob);
    }
    public static void detach(Context context)
    {
        detach(context, context);
    }
    public static void detach(Context context, Object ob)
    {
        if (provisioner != null)
        {
            provisioner.detach(ob);
            if (provisioner.isEmpty())
            {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                sharedPreferences.unregisterOnSharedPreferenceChangeListener(provisioner);
                provisioner = null;
            }
        }
    }
    private static class Provisioner extends AbstractProvisioner<Object> implements OnSharedPreferenceChangeListener
    {
        private final Context context;

        public Provisioner(Context context)
        {
            this.context = context;
        }
        
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
        {
            Map<String, ?> all = sharedPreferences.getAll();
            setValue(key, all.get(key));
        }
        
        @Override
        public Object getValue(String name)
        {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            Map<String, ?> all = sharedPreferences.getAll();
            return all.get(name);
        }
        
    }
}
