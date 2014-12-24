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
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;
import java.util.Locale;

/**
 *
 * @author Timo Vesalainen
 */
public class NumberPickerPreference extends DialogPreference
{
    protected NumberPicker numberPicker;
    protected int minValue;
    protected int maxValue;
    protected int defValue;
    protected int value;
    protected Formatter formatter;
    
    public NumberPickerPreference(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context, attrs);
        setDialogLayoutResource(R.layout.numberpicker_dialog);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        
        setDialogIcon(null);
        
    }

    @Override
    protected void onBindDialogView(View view)
    {
        super.onBindDialogView(view);
        numberPicker = (NumberPicker) view;
        numberPicker.setMinValue(minValue);
        numberPicker.setMaxValue(maxValue);
        numberPicker.setValue(value);
        if (formatter != null)
        {
            numberPicker.setFormatter(formatter);
        }
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue)
    {
        if (restorePersistedValue)
        {
           value = toDisplay(getPersistedInt(defValue));
        }
        else
        {
           value = defValue;
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult)
    {
        if (positiveResult)
        {
            value = numberPicker.getValue();
            persistInt(fromDisplay(value));
        }
    }

    protected int toDisplay(int value)
    {
        return value;
    }
    
    protected int fromDisplay(int value)
    {
        return value;
    }

    private void init(Context context, AttributeSet attrs)
    {
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.NumberPickerPreference, 0, 0);
        try
        {
            minValue = toDisplay(a.getInt(R.styleable.NumberPickerPreference_minValue, Integer.MIN_VALUE));
            maxValue = toDisplay(a.getInt(R.styleable.NumberPickerPreference_maxValue, Integer.MAX_VALUE));
            defValue = toDisplay(a.getInt(R.styleable.NumberPickerPreference_defValue, Integer.MAX_VALUE));
            String format = a.getString(R.styleable.NumberPickerPreference_format);
            initFormat(format);
        }
        finally
        {
            a.recycle();
        }
    }

    protected void initFormat(String format)
    {
        if (format != null)
        {
            formatter = new Formatter(format);
        }
    }
    
    protected class Formatter implements NumberPicker.Formatter
    {
        private final String format;

        public Formatter(String format)
        {
            this.format = format;
        }
        
        @Override
        public String format(int value)
        {
            return String.format(Locale.getDefault(), format, value);
        }
        
    }
}
