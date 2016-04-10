package com.tinyrye.android.database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.database.Cursor;

public abstract class CursorExtractor<T>
{
    protected abstract T convertRow(Cursor rowCursor);

    public void configureFor(Cursor rowCursor) {

    }

    public T firstRow(Cursor rowCursor) {
        configureFor(rowCursor);
        if (rowCursor.moveToNext()) return convertRow(rowCursor);
        else return null;
    }

    public List<T> allRows(Cursor rowCursor) {
        return allRows(rowCursor, new ArrayList<T>());
    }

    public List<T> allRows(Cursor rowCursor, List<T> toAddTo) {
        configureFor(rowCursor);
        while (rowCursor.moveToNext()) toAddTo.add(convertRow(rowCursor));
        return toAddTo;
    }

    /**
     * View the current row as a map of column names to column values strings
     * 
     * @return a map by column name to column values where all values are the String
     * representation as decided by {@link Cursor#getString}.
     */
    public static Map<String,String> toRowStrings(Cursor row) {
        Map<String,String> columnValues = new HashMap<String,String>();
        for (final String column: row.getColumnNames()) columnValues.put(column, row.getString(row.getColumnIndex(column)));
        return columnValues;
    }

    /**
     * View the current row as a map of column names to column values.
     * @param getters Helps 
     * @return a map by column name to column values where all values are the Object
     * representation as decided by the getters assigned to that column
     */
    public static Map<String,Object> getRowValues(Cursor rowCursor, Map<String,Integer> validatedIndices, Map<String,ColumnGetter<?>> getters)
    {
        Map<String,Object> columnValues = new HashMap<String,Object>();
        for (Map.Entry<String,Integer> validatedIndex: validatedIndices.entrySet()) {
            Integer index = validatedIndex.getValue();
            String columnName = validatedIndex.getKey();
            columnValues.put(columnName, getters.get(columnName).extract(rowCursor, index));
        }
        return columnValues;
    }

    /**
     * Convert a list of column names into a map by column names to the corresponding index
     * where the cursor will contain the value.
     */
    public static Map<String,Integer> columnIndices(String[] columns) {
        return columnIndices(Arrays.asList(columns));
    }

    /**
     * Convert a list of column names into a map by column names to the corresponding index
     * where the cursor will contain the value.
     */
    public static Map<String,Integer> columnIndices(List<String> columns)
    {
        final Map<String,Integer> indices = new HashMap<String,Integer>();
        for (int i = 0, size = columns.size(); i < size; i++) {
            indices.put(columns.get(i), new Integer(i));
        }
        return indices;
    }
    
    /**
     * Identify and map the actual columns of the result set.  Map the indices by
     * the column names.
     */
    public static Map<String,Integer> columnIndices(Cursor rowCursor) {
        return columnIndices(rowCursor.getColumnNames());
    }
}