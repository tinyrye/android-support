package com.tinyrye.android.database;

import java.util.HashMap;
import java.util.Map;

import android.database.Cursor;

public interface ColumnGetter<T>
{
    T extract(Cursor rowCursor, int index);
}