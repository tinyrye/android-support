package com.tinyrye.android.database;

import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FileUtils.forceMkdirParent;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;

import com.softwhistle.io.ErrorHandler;
import com.softwhistle.io.Operations;

public class DbPreparer
{
	private final Context context;
	private final String dbName;
	private DbMigrator dbMigrator;

	public DbPreparer(final Context context, final String dbName) {
		this.context = context;
		this.dbMigrator = dbMigrator;
		this.dbName = dbName;
	}

	public DbPreparer setMigrator(DbMigrator dbMigrator) {
		this.dbMigrator = dbMigrator;
		return this;
	}

	protected Context getContext() { return context; }
	protected String getDbName() { return dbName; }

	/**
	 * Optionally before makeReady() you can choose to clear an existing database
	 * usually if you are in early development and need a blank slate.
	 */
	public DbPreparer reset() {
		final File dbFile = context.getDatabasePath(dbName);
		if (dbFile.exists()) dbFile.delete();
		return this;
	}

	public DbPreparer restoreFromBackup()
	{
		File currentDbFile = context.getDatabasePath(dbName);
		File restoreDbFile = new File(String.format("%s/app-data/%s.restore", Environment.getExternalStorageDirectory().getAbsolutePath(), dbName));
		Log.d(getClass().getName(), String.format("Looking for backup file to restore: %s", restoreDbFile.getAbsolutePath()));
		if (restoreDbFile.exists())
		{
			Log.d(getClass().getName(), String.format("Restoring from backup: %s", restoreDbFile.getAbsolutePath()));
			try {
				Operations.run(() -> {
					forceMkdirParent(currentDbFile);
					copyFile(restoreDbFile, currentDbFile);
					restoreDbFile.delete();
				});
			} 
			catch (RuntimeException ex) { Log.e(getClass().getName(), "Failed to restore database", ex); }
		}
		else {
			Log.d(getClass().getName(), String.format("Restore file does not exist: %s", restoreDbFile.getAbsolutePath()));
		}
		return this;
	}

	/**
	 *
	 */
	public DbPreparer makeReady() {
		new MigratingDbOpenHelper(context, getDbName(), dbMigrator).getReadableDatabase();
		return this;
	}

	public DbPreparer backupToPublic()
	{
		File sourceDbFile = context.getDatabasePath(dbName);
		File destinationDbFile = new File(String.format("%s/app-data/%s.backup", Environment.getExternalStorageDirectory().getAbsolutePath(), dbName));
		Log.d(getClass().getName(), String.format("Backing up DB: %s; destinationDbFile=%s", dbName, destinationDbFile.getAbsolutePath()));
		Operations.run(() -> {
			forceMkdirParent(destinationDbFile);
			copyFile(sourceDbFile, destinationDbFile);
		}, (ex, phase) -> {
			Log.e(getClass().getName(), "Failed to backup database", ex);
			return false;
		});
		return this;
	}

	public DbPreparer logSanityQueryResults(String queryName, String query)
	{
		SQLiteDatabase db = handleTo();
		Cursor rows = db.rawQuery(query, null);
		Log.d(getClass().getName(), String.format("Running sanity query: %s ==> %s", queryName, query));
		int i = 1;
		while (rows.moveToNext()) {
			Log.d(getClass().getName(), String.format("row #%d: %s", i, CursorExtractor.toRowStrings(rows)));
			i++;
		}
		return this;
	}

	public SQLiteDatabase handleTo() {
		final File dbFile = context.getDatabasePath(dbName);
		Log.d(getClass().getName(), String.format("DB file: %s; file=%b; readable=%b; writeable=%b", dbFile.getAbsolutePath(), dbFile.isFile(), dbFile.canRead(), dbFile.canWrite()));
		return context.openOrCreateDatabase(dbName, SQLiteDatabase.OPEN_READWRITE, null);
	}
}