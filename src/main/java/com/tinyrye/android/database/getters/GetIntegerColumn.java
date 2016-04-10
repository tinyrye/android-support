package com.tinyrye.android.database.getters;

import android.database.Cursor;

public class GetIntegerColumn extends NullCheckingColumnGetter<Integer>
{
	@Override
    public Integer extractOnNotNull(Cursor rowCursor, int index) {
    	return new Integer(rowCursor.getInt(index));
    }
}