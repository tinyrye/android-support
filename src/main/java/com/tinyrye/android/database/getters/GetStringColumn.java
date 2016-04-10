package com.tinyrye.android.database.getters;

import android.database.Cursor;

import com.tinyrye.android.database.ColumnGetter;

public class GetStringColumn implements ColumnGetter<String>
{
	@Override
	public String extract(Cursor rowCursor, int index) {
    	return rowCursor.getString(index);
    }
}