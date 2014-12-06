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
import android.util.Log;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import static org.vesalainen.boatwatch.BoatWatchConstants.*;
import org.vesalainen.util.AbstractProvisioner;
import org.vesalainen.util.HashMapList;
import org.vesalainen.util.MapList;

/**
 *
 * @author Timo Vesalainen
 */
public class Settings
{
    public static final String Simulate = "pref_simulate";
    public static final String AlarmTone = "pref_alarmtone";
    
    public static Provisioner provisioner;
    
    public static void attach(Context context)
    {
        if (provisioner == null)
        {
            provisioner = new Provisioner(context);
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            sharedPreferences.registerOnSharedPreferenceChangeListener(provisioner);
        }
        provisioner.attach(context);
    }
    public static void detach(Context context)
    {
        if (provisioner != null)
        {
            provisioner.detach(context);
            if (provisioner.isEmpty())
            {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                sharedPreferences.unregisterOnSharedPreferenceChangeListener(provisioner);
                provisioner = null;
            }
        }
    }
    private static class Provisioner extends AbstractProvisioner<Context> implements OnSharedPreferenceChangeListener
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
