package com.tinyrye.android.database.getters;

import android.database.Cursor;

import com.tinyrye.android.database.ColumnGetter;

public abstract class NullCheckingColumnGetter<T> implements ColumnGetter<T>
{
	protected abstract T extractOnNotNull(Cursor rowCursor, int index);
	
    @Override
    public T extract(Cursor rowCursor, int index) {
    	if (! rowCursor.isNull(index)) return extractOnNotNull(rowCursor, index);
        else return null;
    }
}