package com.tinyrye.android.database.getters;

import android.database.Cursor;

public class GetDoubleColumn extends NullCheckingColumnGetter<Double>
{
	@Override
    public Double extractOnNotNull(Cursor rowCursor, int index) {
    	return new Double(rowCursor.getDouble(index));
    }
}