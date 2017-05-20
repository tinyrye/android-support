package com.tinyrye.android.database;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.apache.commons.io.IOUtils;
import com.softwhistle.io.Operations;

import com.tinyrye.android.util.TieredComparison;

public class DbMigrator
{
	public static class Migration implements Serializable
	{
		private static final long serialVersionUID = 1L;

		public Migration() {}
		
		/**
		 * @param migrationOrder Array of two numbers: the version then number.
		 */
		public Migration(final Integer[] migrationOrder, final String scriptName, final String script) {
			this.version = migrationOrder[0];
			this.number = migrationOrder[1];
			this.scriptName = scriptName;
			this.script = script;
		}

		public String scriptName;
		public Integer version;
		public Integer number;
		public String script;
	}

	public static class MigrationByVersionAndNumberComparator implements Comparator<Migration> {
		@Override public int compare(final Migration m1, final Migration m2) {
			return new TieredComparison()
						.append(m1.version, m2.version)
						.append(m1.number, m2.number)
					    .get();
		}
	}

	public static String getMigrationsPath() {
		return "database/migrations";
	}

	public static String getMigrationsPath(final Class<?> modelReference) {
		return String.format("%s/%s", getMigrationsPath(), modelReference.getPackage().getName().replaceAll("\\.", "\\/"));
	}

	public List<Migration> migrations = new ArrayList<Migration>();
	private Integer currentDbVersion;
	private Boolean dryRun = Boolean.FALSE;

	public Boolean isDryRun() { return dryRun; }
	public void setDryRun(final Boolean dryRun) { this.dryRun = dryRun; }

	/**
	 * Load SQL scripts as migrations to run when the database helper creates, upgrades, or downgrades
	 * the database.
	 * 
	 * As a result determine the database version number and the order of the scripts.
	 */
	public DbMigrator scanMigrations(final AssetManager migrationProvider, final String migrationDirectory) throws IOException
	{
		migrations.clear();
		final String[] migrationFiles = migrationProvider.list(migrationDirectory);
		for (final String migrationFile: migrationFiles) {
			Log.d("dbMigrator", "Found migration: " + migrationFile + " in " + migrationDirectory);
			importMigration(migrationProvider, migrationDirectory, migrationFile);
		}
		Collections.sort(migrations, new MigrationByVersionAndNumberComparator());
		currentDbVersion = migrations.get(migrations.size() - 1).version;
		return this;
	}

	public Integer getCurrentDbVersion() {
		return currentDbVersion;
	}
	
	protected void importMigration(
			final AssetManager migrationProvider,
			final String migrationDirectory,
			final String migrationFile)
		throws IOException
	{
		migrations.add(
			new Migration(
				parseMigrationOrder(migrationFile), migrationFile,
				readMigrationScript(migrationProvider, migrationDirectory, migrationFile)
			));
	}
	
	protected Integer[] parseMigrationOrder(final String scriptName) {
		final Pattern scriptNamePattern = Pattern.compile("^(\\d{1,4})-(\\d{1,4})-\\S+$");
		final Matcher scriptNameMatch = scriptNamePattern.matcher(scriptName);
		if (! scriptNameMatch.find()) throw new IllegalArgumentException("Invalid script name format: " + scriptName);
		return new Integer[] { Integer.valueOf(scriptNameMatch.group(1)), Integer.valueOf(scriptNameMatch.group(2)) };
	}

	public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
		for (final Migration migration: migrations) {
			if (migration.version > oldVersion && migration.version <= newVersion) {
				runMigration(db, migration);
			}
		}
	}

	public void runAll(final SQLiteDatabase db) {
		for (final Migration migration: migrations) {
			runMigration(db, migration);
		}
	}

	protected void runMigration(final SQLiteDatabase db, final Migration m) {
		Log.d(getClass().getName(), "Running migration: dryRun=" + dryRun + "; version=" + m.version + "; number=" + m.number + "; name=" + m.scriptName);
		if (dryRun) return;
		if (m.scriptName.endsWith("sql")) db.execSQL(m.script);
		else if (m.scriptName.endsWith("cmd")) runCustomMigration(db, m);
		else throw new IllegalArgumentException(String.format("Migration must be a sql or exec file: scriptName=%s", m.scriptName));
	}

	protected void runCustomMigration(final SQLiteDatabase db, final Migration m) {
		onUnknownCustomMigration(m);
	}

	protected void onUnknownCustomMigration(final Migration m) {
		throw new IllegalArgumentException(String.format("Custom migration is not recognized, and cannot be run: scriptName=%s", m.scriptName));
	}

	private String readMigrationScript(final AssetManager migrationProvider, final String migrationDirectory, final String scriptFileName) {
		return Operations.open(() -> migrationProvider.open(migrationDirectory + "/" + scriptFileName))
			.apply((str) -> IOUtils.toString(str, Charset.defaultCharset()));
	}
}