package com.tinyrye.android.provider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import com.tinyrye.android.database.DbMigrator;
import com.tinyrye.android.database.DbPreparer;

public class SQLiteSimpleContentProvider extends ContentProvider
{
	private String databaseName;

	protected SQLiteDatabase db;

	public SQLiteSimpleContentProvider(String databaseName) {
		this.databaseName = databaseName;
	}

	@Override
	public boolean onCreate()
	{
		try
		{
			DbPreparer dbPreparer = new DbPreparer(getContext(), databaseName);
			dbPreparer.setMigrator(new DbMigrator().scanMigrations(getContext().getAssets(), DbMigrator.getMigrationsPath()));
			beforeMakeReady(dbPreparer);
			dbPreparer.makeReady();
			afterMakeReady(dbPreparer);
			db = dbPreparer.handleTo();
		}
		catch (android.database.sqlite.SQLiteCantOpenDatabaseException ex) {
			Log.e(getClass().getName(), String.format("Could not initialize database: %s", ex.getMessage()), getRootCause(ex));
		}
		catch (IOException ex) {
			Log.e(getClass().getName(), String.format("Could not initialize database: %s", ex.getMessage()), getRootCause(ex));
		}
		return (db != null);
	}

	protected DbPreparer beforeMakeReady(DbPreparer preparer) {
		return preparer;
	}

	protected DbPreparer afterMakeReady(DbPreparer preparer) {
		return preparer;
	}

	@Override
	public String getType(final Uri target) {
		return "text/plain";
	}

	@Override
	public Cursor query(final Uri target, String[] columns, String selection,
						String[] selectionArgs, final String sortOrder)
	{
		// Standard Query Builder: no group by, having or limit clause;
		//    no joining either
		String tableName = getTableName(target);
		if (columns == null) columns = getColumns(target);
		selection = completeSelection(target, selection);
		selectionArgs = completeSelectionArgs(target, selectionArgs);
		return db.query(
				tableName, columns, selection, selectionArgs,
				/* groupBy */ null, /* having */ null,
				sortOrder);
	}

	@Override
	public Uri insert(final Uri target, final ContentValues insertValues) {
		return target.buildUpon().appendPath(Long.valueOf(db.insert(getTableName(target), null, insertValues)).toString()).build();
	}

	@Override
	public int update(final Uri target, final ContentValues updateValues, final String selection, final String[] selectionArgs) {
		return db.update(getTableName(target), updateValues, completeSelection(target, selection), completeSelectionArgs(target, selectionArgs));
	}

	@Override
	public int delete(final Uri target, final String selection, final String[] selectionArgs) {
		return db.delete(getTableName(target), completeSelection(target, selection), completeSelectionArgs(target, selectionArgs));
	}

	protected String getTableName(final Uri target)
	{
		final List<String> pathSegments = target.getPathSegments();
		if (pathSegments.size() > 1) {
			if (isNumber(pathSegments.get(pathSegments.size() - 1))) return pathSegments.get(pathSegments.size() - 2);
			else return pathSegments.get(pathSegments.size() - 1);
		}
		else if (pathSegments.size() == 1) return pathSegments.get(0);
		else return null;
	}

	protected String[] getColumns(final Uri target) {
		return null;
	}

	protected String completeSelection(Uri target, String selection) {
		return ensureIdInSelectionIfTargeted(target, selection);
	}

	protected String[] completeSelectionArgs(Uri target, String[] selectionArgs) {
		return ensureIdInSelectionArgsIfTargeted(target, selectionArgs);
	}

	/**
	 * If the URI/target specifies a specific row by id, then make sure the where clause
	 * has an id-by-number equals condition.
	 * 
	 * @return a where clause where the id clause append to end
	 */
	protected String ensureIdInSelectionIfTargeted(final Uri target, String selection) {
		if (hasId(target)) return appendToSelection(selection, getIdParamClause(target));
		else return selection;
	}

	/**
	 * If the URI/target specifies a specific row by id, then make sure the where clause
	 * has an id-by-number equals condition.
	 * 
	 * @return selection args with the id appended to it
	 */
	protected String[] ensureIdInSelectionArgsIfTargeted(final Uri target, String[] selectionArgs) {
		if (hasId(target)) return appendToSelectionArgs(selectionArgs, getId(target));
		else return selectionArgs;
	}

	protected String appendToSelection(final String selection, final String additionalCriteria) {
		if ((selection != null) && ! selection.trim().isEmpty()) return String.format("%s AND %s", selection, additionalCriteria);
		else return additionalCriteria;
	}

	protected String[] appendToSelectionArgs(final String[] selectionArgs, final Object ... additionalArgs) {
		final List<String> combined = new ArrayList<String>();
		if ((selectionArgs != null) && (selectionArgs.length > 0)) combined.addAll(Arrays.asList(selectionArgs));
		for (final Object additionalArg: additionalArgs) combined.add(additionalArg.toString());
		return combined.toArray(new String[0]);
	}
	
	protected boolean hasId(final Uri target) {
		final List<String> pathSegments = target.getPathSegments();
		return ((pathSegments.size() > 1) && isNumber(pathSegments.get(pathSegments.size() - 1)));
	}

	protected String getIdParamClause(final Uri target) {
		return String.format("%s = ?", getIdColumnName(target));
	}

	protected String getIdColumnName(final Uri target) {
		return BaseColumns._ID;
	}

	protected Integer getId(Uri target) {
		return new Integer(target.getLastPathSegment());
	}

	/**
	 * Confirms that the URI path goes from parent table name, parent record id, to child table
	 * name.
	 * @return the parent record id from the URI path
	 */
	protected Integer getByParentId(final Uri target, final String parentTableName, final String childTableName)
	{
		final List<String> pathSegmentsBefore = getPathSegmentsBefore(target, childTableName);
		if ((pathSegmentsBefore.size() >= 2)
			 && parentTableName.equals(pathSegmentsBefore.get(pathSegmentsBefore.size() - 2)))
	    {
	    	try { return Integer.valueOf(pathSegmentsBefore.get(pathSegmentsBefore.size() - 1)); }
	    	catch (NumberFormatException ex) { return null; }
	    }
	    else {
	    	return null;
		}
	}

	protected List<String> getPathSegmentsBefore(final Uri target, final String terminatingSegment)
	{
		final List<String> pathSegments = target.getPathSegments();
		if (pathSegments.size() > 1)
		{
			for (int i = pathSegments.size() - 1; i >= 1; i--) {
				if (pathSegments.get(i).equals(terminatingSegment)) {
					return pathSegments.subList(0, i);
				}
			}
		}
		return new ArrayList<String>();
    }

	protected boolean isNumber(final String number) {
		try { Integer.valueOf(number); return true; }
		catch (NumberFormatException ex) { return false; }
	}

	protected List<String> prefaceColumnNames(final String tablePrefix, final List<String> columns)
	{
		for (int i = 0; i < columns.size(); i++) {
			columns.set(i, String.format("%s.%s AS %s", tablePrefix, columns.get(i), columns.get(i)));
		}
		return columns;
	}

	protected Throwable getRootCause(Throwable ex) {
		if (ex.getCause() == null) return ex;
		else return getRootCause(ex.getCause());
	}
}