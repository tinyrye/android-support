package com.tinyrye.android.database.getters;

import android.database.Cursor;

public class GetLongColumn extends NullCheckingColumnGetter<Long>
{
	@Override
    public Long extractOnNotNull(Cursor rowCursor, int index) {
    	return new Long(rowCursor.getLong(index));
    }
}