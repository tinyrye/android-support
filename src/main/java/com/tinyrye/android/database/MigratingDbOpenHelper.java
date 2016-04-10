package com.tinyrye.android.database;

import android.content.Context;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import android.util.Log;

public class MigratingDbOpenHelper extends SQLiteOpenHelper
{
	private final DbMigrator dbMigrator;

	public MigratingDbOpenHelper(final Context context, final String dbName, final DbMigrator dbMigrator) {
		super(context, dbName, null, dbMigrator.getCurrentDbVersion());
		this.dbMigrator = dbMigrator;
	}

	@Override
	public void onCreate(final SQLiteDatabase db) {
		Log.d(getClass().getName(), String.format("Creating db: %s", getDatabaseName()));
		db.create(null);
		dbMigrator.runAll(db);
	}

	@Override
	public void onUpgrade(final SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.d(getClass().getName(), String.format("Upgrading DB: oldVersion=%d; newVersion=%d", oldVersion, newVersion));
		dbMigrator.onUpgrade(db, oldVersion, newVersion);
	}
}