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
    
    private static final MapList<String,InstanceMethod> map = new HashMapList<>();
    private static SharedPreferences.OnSharedPreferenceChangeListener listener;
    
    public static void attach(Context context, Object ob)
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (listener == null)
        {
            listener = new Listener();
            sharedPreferences.registerOnSharedPreferenceChangeListener(listener);
        }
        Map<String, ?> all = sharedPreferences.getAll();
        for (Method method : ob.getClass().getMethods())
        {
            Setting setting = method.getAnnotation(Setting.class);
            if (setting != null)
            {
                String name = setting.value();
                Class<?>[] params = method.getParameterTypes();
                if (params.length != 1)
                {
                    throw new IllegalArgumentException("@Setting("+name+") argument count != 1");
                }
                InstanceMethod instanceMethod = new InstanceMethod(ob, method);
                Object value = all.get(name);
                if (value != null)
                {
                    instanceMethod.invoke(value);
                }
                map.add(name, instanceMethod);
            }
        }
    }
    public static void detach(Object ob)
    {
        Iterator<Map.Entry<String, List<InstanceMethod>>> ki = map.entrySet().iterator();
        while (ki.hasNext())
        {
            Map.Entry<String, List<InstanceMethod>> entry = ki.next();
            Iterator<InstanceMethod> li = entry.getValue().iterator();
            while (li.hasNext())
            {
                InstanceMethod im = li.next();
                if (im.instance == ob)
                {
                    li.remove();
                }
            }
            if (entry.getValue().isEmpty())
            {
                ki.remove();
            }
        }
    }
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Setting
    {
        String value();
    }
    private static class InstanceMethod
    {
        Object instance;
        Method method;

        private InstanceMethod(Object instance, Method method)
        {
            this.instance = instance;
            this.method = method;
        }
        
        private void invoke(Object arg)
        {
            try
            {
                method.invoke(instance, arg);
            }
            catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex)
            {
                Log.e(LogTitle, "Couldn't invoke "+method+" with arg="+arg, ex);
            }
        }
    }
    private static class Listener implements OnSharedPreferenceChangeListener
    {

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
        {
            Map<String, ?> all = sharedPreferences.getAll();
            for (InstanceMethod im : map.get(key))
            {
                Object value = all.get(key);
                if (value != null)
                {
                    im.invoke(value);
                }
            }
        }
        
    }
}
