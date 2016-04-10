package com.tinyrye.android.database.getters;

import android.database.Cursor;

public class GetFloatColumn extends NullCheckingColumnGetter<Float>
{
	@Override
    public Float extractOnNotNull(Cursor rowCursor, int index) {
    	return new Float(rowCursor.getFloat(index));
    }
}