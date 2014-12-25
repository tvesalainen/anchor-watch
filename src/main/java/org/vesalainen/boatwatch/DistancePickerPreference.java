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
import org.vesalainen.util.AbstractProvisioner.Setting;
import org.vesalainen.util.navi.Feet;

/**
 *
 * @author Timo Vesalainen
 */
public class DistancePickerPreference extends NumberPickerPreference
{
    private String distanceUnit;
    private final String unitMeters;
    private final String unitFeet;
    private final String formatMeters;
    private final String formatFeet;

    public DistancePickerPreference(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        unitMeters = context.getString(R.string.distance_unit_meters);
        unitFeet = context.getString(R.string.distance_unit_feet);
        formatMeters = context.getString(R.string.distance_unit_format_meters);
        formatFeet = context.getString(R.string.distance_unit_format_feet);
    }

    @Override
    protected void initFormat(String format)
    {
        if (unitFeet.equals(distanceUnit))
        {
            super.initFormat(formatFeet);
        }
        else
        {
            super.initFormat(formatMeters);
        }
    }

    @Override
    protected int fromDisplay(int value)
    {
        if (unitFeet.equals(distanceUnit))
        {
            return (int) Math.round(Feet.toMeters(value));
        }
        else
        {
            return value;
        }
    }

    @Override
    protected int toDisplay(int value)
    {
        if (unitFeet.equals(distanceUnit))
        {
            return (int) Math.round(Feet.fromMeters(value));
        }
        else
        {
            return value;
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult)
    {
        super.onDialogClosed(positiveResult);
        Settings.detach(getContext());
    }

    @Override
    protected void onBindDialogView(View view)
    {
        Settings.attach(getContext(), this);
        super.onBindDialogView(view);
    }
    
    @Setting(DistanceUnit)
    public void setDistanceUnit(String distanceUnit)
    {
        this.distanceUnit = distanceUnit;
        initFormat(null);
    }

}
