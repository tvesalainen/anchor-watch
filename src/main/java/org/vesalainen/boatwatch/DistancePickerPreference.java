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
import android.util.AttributeSet;
import android.view.View;
import static org.vesalainen.boatwatch.Settings.DistanceUnit;
import org.vesalainen.util.AbstractProvisioner;
import org.vesalainen.util.AbstractProvisioner.Setting;
import org.vesalainen.util.navi.Feet;

/**
 *
 * @author Timo Vesalainen
 */
public class DistancePickerPreference extends NumberPickerPreference
{
    private String distanceUnit;

    public DistancePickerPreference(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    @Override
    protected void initFormat(String format)
    {
        switch (distanceUnit)
        {
            default:
            case "m":
                super.initFormat("%d Meters");
            case "ft":
                super.initFormat("%d Feets");
        }
    }

    @Override
    protected int fromDisplay(int value)
    {
        switch (distanceUnit)
        {
            default:
            case "m":
                return value;
            case "ft":
                return (int) Feet.toMeters(value);
        }
    }

    @Override
    protected int toDisplay(int value)
    {
        switch (distanceUnit)
        {
            default:
            case "m":
                return value;
            case "ft":
                return (int) Feet.fromMeters(value);
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult)
    {
        Settings.detach(getContext());
        super.onDialogClosed(positiveResult);
    }

    @Override
    protected void onBindDialogView(View view)
    {
        super.onBindDialogView(view);
        Settings.attach(getContext());
    }
    
    @Setting(DistanceUnit)
    public void setDistanceUnit(String distanceUnit)
    {
        this.distanceUnit = distanceUnit;
        initFormat(null);
    }

}
