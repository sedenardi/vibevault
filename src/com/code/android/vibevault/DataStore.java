/*
 * DataStore.java
 * VERSION 1.4
 * 
 * Copyright 2011 Andrew Pearson and Sanders DeNardi.
 * 
 * This file is part of Vibe Vault.
 * 
 * Vibe Vault is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * 
 */

package com.code.android.vibevault;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DataStore extends SQLiteOpenHelper {

	private static final String DB_NAME = "archivedb";
	private static String DB_PATH;
	private static final int DB_VERSION = 7;

	public static final String PREF_TBL = "prefsTbl";
	public static final String PREF_KEY = "_id";
	public static final String PREF_NAME = "prefName";
	public static final String PREF_VALUE = "prefValue";

	public static final String SHOW_TBL = "showTbl";
	public static final String SHOW_KEY = "_id";
	public static final String SHOW_IDENT = "showIdent";
	public static final String SHOW_TITLE = "showTitle";
	public static final String SHOW_ARTIST = "showArtist";
	public static final String SHOW_SOURCE = "showSource";
	public static final String SHOW_HASVBR = "hasVBR";
	public static final String SHOW_HASLBR = "hasLBR";

	public static final String SONG_TBL = "songTbl";
	public static final String SONG_KEY = "_id";
	public static final String SONG_FILENAME = "fileName";
	public static final String SONG_TITLE = "songTitle";
	public static final String SONG_SHOW_KEY = "show_id";
	public static final String SONG_DOWNLOADED = "isDownloaded";

	public static final String RECENT_TBL = "recentShowsTbl";
	public static final String RECENT_KEY = "_id";
	public static final String RECENT_SHOW_KEY = "show_id";

	public static final String PLAYLIST_TBL = "playlistTbl";
	public static final String PLAYLIST_KEY = "_id";
	public static final String PLAYLIST_NAME = "playlistName";

	public static final String PLAYLISTSONG_TBL = "playlistTbl";
	public static final String PLAYLISTSONG_KEY = "_id";
	public static final String PLAYLISTSONG_PLAYLIST_KEY = "playlist_id";
	public static final String PLAYLISTSONG_SONG_KEY = "song_id";

	public static final String RECENT_SHOW_VW = "recentShowsVw";
	public static final String DOWNLOADED_SHOW_VW = "downloadedShowsVw";

	public boolean needsUpgrade = false;

	private final Context context;

	private SQLiteDatabase db;

	public DataStore(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		this.context = context;
		DB_PATH = context.getDatabasePath(DB_NAME).toString();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}

	public void initialize() {
		try {
			createDB();
		} catch (IOException e) {
			Log.e(VibeVault.DATA_STORE_TAG, "Unable to create database");
			Log.e(VibeVault.DATA_STORE_TAG, e.getStackTrace().toString());
		}
		if(!needsUpgrade){
			try {
				openDataBase();
			} catch (SQLException e) {
				Log.e(VibeVault.DATA_STORE_TAG, "Unable to open database");
				Log.e(VibeVault.DATA_STORE_TAG, e.getStackTrace().toString());
			}
		}
	}

	public void createDB() throws IOException {

		boolean dbExists = checkDB();
		if (dbExists) {

		} else {

			this.getReadableDatabase();
			try {
				copyDB();
			} catch (IOException e) {
				throw new Error("Error copying database");
			}
		}
	}

	public boolean checkDB() {
		SQLiteDatabase checkDB = null;
		try {
			checkDB = SQLiteDatabase.openDatabase(DB_PATH, null,
					SQLiteDatabase.OPEN_READWRITE);
		} catch (SQLiteException e) {

		}

		if (checkDB != null) {
			if (checkDB.getVersion() != DB_VERSION) {

				// checkDB.execSQL("DROP TABLE IF EXISTS " + PREF_TABLE);
				// checkDB.execSQL("DROP TABLE IF EXISTS " + "showsTbl");
				checkDB.close();
				needsUpgrade = true;
				return needsUpgrade;
			} else {
				checkDB.close();
				return true;
			}
		} else {
			return false;
		}
	}

	public void copyDB() throws IOException {

		InputStream is = context.getAssets().open(DB_NAME);
		OutputStream os = new FileOutputStream(DB_PATH);

		byte[] buffer = new byte[1024];
		int length;
		while ((length = is.read(buffer)) > 0) {
			os.write(buffer, 0, length);
		}

		os.flush();
		os.close();
		is.close();

	}

	public void openDataBase() throws SQLiteException {
		db = SQLiteDatabase.openDatabase(DB_PATH, null,
				SQLiteDatabase.OPEN_READWRITE);
		db.setVersion(DB_VERSION);
		needsUpgrade = false;
	}

	public Cursor getPrefCursor(String pref_name) {
		Cursor cur = db.query(true, PREF_TBL, new String[] { PREF_KEY,
				PREF_NAME, PREF_VALUE }, null, null, null, null, null, null);
		if (cur != null) {
			cur.moveToFirst();
		}
		return cur;
	}

	public String getPref(String pref_name) {
		Cursor cur = db.query(PREF_TBL, new String[] { PREF_VALUE },
				PREF_NAME + "='" + pref_name + "'", null, null, null, null, null);
		if (cur != null) {
			cur.moveToFirst();
			String retString = cur.getString(cur.getColumnIndex(PREF_VALUE));
			cur.close();
			return retString;
		}
		return "NULL";
	}

	public void updatePref(String pref_name, String pref_value) {
		db.execSQL("UPDATE prefsTbl SET prefValue = '"
				+ pref_value + "' WHERE prefName = '"
				+ pref_name + "'");
	}

	public void insertShow(ArchiveShowObj show) {

		db.execSQL("INSERT INTO showTbl(showIdent,showTitle,showArtist,showSource,hasVBR,hasLBR) "
				+ "SELECT '"
				+ show.getIdentifier()
				+ "','"
				+ show.getShowTitle().replaceAll("'", "''")
				+ "','"
				+ show.getShowArtist().replaceAll("'", "''")
				+ "','"
				+ show.getShowSource().replaceAll("'", "''")
				+ "','"
				+ show.hasVBR()
				+ "','"
				+ show.hasLBR()
				+ "' "
				+ "WHERE NOT EXISTS (SELECT 1 FROM showTbl show WHERE show.showIdent = '"
				+ show.getIdentifier() + "')");
		/*
		 * boolean dupe = showExists(show.getIdentifier()); if(!dupe){
		 * ContentValues value = new ContentValues(); value.put(SHOW_IDENT,
		 * show.getIdentifier()); value.put(SHOW_TITLE, show.getShowTitle());
		 * value.put(SHOW_ARTIST, show.getShowArtist()); value.put(SHOW_SOURCE,
		 * show.getShowSource()); value.put(SHOW_HASVBR, show.hasVBR());
		 * value.put(SHOW_HASLBR, show.hasLBR()); long row = db.insert(SHOW_TBL,

		 * "insertShow() - Inserting show [" + row + "," + show.getIdentifier()
		 * + "," + show.getArtistAndTitle() + "," + show.hasVBR() + "," +

		 * "insertShow() - Show exists"); }
		 */
	}

	/*
	 * public boolean showExists(String show_ident){ Cursor cur =
	 * db.query(SHOW_TBL, new String[]{SHOW_IDENT}, SHOW_IDENT + "='" +
	 * show_ident + "'", null, null, null, null); int results = cur.getCount();
	 * cur.close(); return results > 0; }
	 */

	public void insertRecentShow(ArchiveShowObj show) {
		insertShow(show);
		db.execSQL("INSERT INTO recentShowsTbl(show_id) "
				+ "SELECT show._id "
				+ "FROM showTbl show "
				+ "WHERE show.showIdent = '"
				+ show.getIdentifier()
				+ "' "
				+ "AND NOT EXISTS (SELECT 1 FROM recentShowsVw recent WHERE recent.showIdent = '"
				+ show.getIdentifier() + "')");
	}

	public Cursor getRecentShows() {

		/*
		 * return db.query(RECENT_TBL, new String[] { SHOW_KEY, SHOW_IDENT,
		 * SHOW_TITLE, SHOW_HASVBR, SHOW_HASLBR }, null, null, null, null,
		 * null);
		 */
		return db.rawQuery("SELECT * FROM " + RECENT_SHOW_VW, null);
	}

	public Cursor getDownloadShows() {

		/*
		 * return db.query(SHOW_TBL, new String[] { SHOW_KEY, SHOW_IDENT,
		 * SHOW_TITLE, SHOW_HASVBR, SHOW_HASLBR }, null, null, null, null,
		 * null);
		 */
		return db.rawQuery("SELECT * FROM " + DOWNLOADED_SHOW_VW, null);
	}

	public ArchiveShowObj getShow(long show_id) {

		ArchiveShowObj show = null;
		Cursor cur = db.query(true, SHOW_TBL,
				new String[] { SHOW_IDENT, SHOW_TITLE, SHOW_ARTIST,
						SHOW_SOURCE, SHOW_HASVBR, SHOW_HASLBR }, SHOW_KEY + "="
						+ show_id, null, null, null, null, null);
		if (cur != null) {
			cur.moveToFirst();
			show = new ArchiveShowObj(cur.getString(cur
					.getColumnIndex(SHOW_IDENT)), cur.getString(cur
					.getColumnIndex(SHOW_TITLE)), cur.getString(cur
					.getColumnIndex(SHOW_ARTIST)), cur.getString(cur
					.getColumnIndex(SHOW_SOURCE)), cur.getString(cur
					.getColumnIndex(SHOW_HASVBR)), cur.getString(cur
					.getColumnIndex(SHOW_HASLBR)));
		}
		cur.close();
		return show;
	}

	public void deleteRecentShow(long show_id) {

		db.delete(RECENT_TBL, RECENT_SHOW_KEY + "=" + show_id, null);
	}

	public void clearRecentShows() {

		db.delete(RECENT_TBL, null, null);
	}

	public void insertSong(ArchiveSongObj song) {

		db.execSQL("INSERT INTO songTbl(fileName,songTitle,show_id,isDownloaded) "
				+ "SELECT '"
				+ song.getFileName()
				+ "','"
				+ song.toString().replaceAll("'", "''")
				+ "',show._id,'false' "
				+ "FROM showTbl show "
				+ "WHERE show.showIdent = '"
				+ song.getShowIdentifier()
				+ "' "
				+ "AND NOT EXISTS (SELECT 1 FROM songTbl song WHERE song.fileName = '"
				+ song.getFileName() + "')");
		/*
		 * boolean dupe = songExists(song.getFileName()); if(!dupe){
		 * ContentValues value = new ContentValues(); value.put(SONG_FILENAME,
		 * song.getFileName()); value.put(SONG_TITLE, song.toString());
		 * value.put(SONG_SHOW_KEY, song.getShowIdentifier());
		 * value.put(SONG_DOWNLOADED, song.getShowTitle()); long row =

		 * "insertSong() - Inserting song [" + row + "," + song.getFileName() +
		 * "," + song.toString() + "," + song.getShowIdentifier() + "," +

		 * "insertSong() - Song exists"); }
		 */
	}

	public void setSongDownloaded(ArchiveSongObj song) {
		db.execSQL("UPDATE songTbl "
				+ "SET isDownloaded = 'true' "
				+ "WHERE fileName = '"
				+ song.getFileName() + "'");
	}

	public boolean songIsDownloaded(String song_filename) {

		/*Cursor cur = db.query(SONG_TBL, new String[] { SONG_FILENAME },
				SONG_FILENAME + "='" + song_filename + "'", null, null, null,
				null);*/
		Cursor cur = db.rawQuery("Select 1 FROM songTbl song WHERE song.fileName = '"
				+ song_filename + "' AND song.isDownloaded like 'true'", null);
		int results = cur.getCount();
		cur.close();
		return results > 0;
	}

	public Cursor getSongsFromShow(String showIdent) {

		return db
				.rawQuery(
						"SELECT song.*,show.showIdent,show.showArtist + ' Live at ' + show.showTitle AS 'showTitle' FROM songTbl song "
								+ "INNER JOIN showTbl show "
								+ "	ON song.show_id = show._id "
								+ "	AND show.showIdent = '" + showIdent + "'",
						null);
		/*
		 * return db.query(SONG_TBL, new String[] { SONG_KEY, SONG_FILENAME,
		 * SONG_TITLE, SONG_SHOW_KEY, SONG_DOWNLOADED }, SONG_SHOW_KEY + "='" +
		 * show_ident + "'", null, null, null, SONG_FILENAME);
		 */
	}

	public Cursor getSongsFromShowKey(long id) {

		return db
				.rawQuery(
						"SELECT song.*,show.showIdent,show.showArtist + ' Live at ' + show.showTitle AS 'showTitle' FROM songTbl song "
								+ "INNER JOIN showTbl show "
								+ "	ON song.show_id = show._id "
								+ "	AND show._id = '" + id + "'",
						null);
		/*
		 * return db.query(SONG_TBL, new String[] { SONG_KEY, SONG_FILENAME,
		 * SONG_TITLE, SONG_SHOW_KEY, SONG_DOWNLOADED }, SONG_SHOW_KEY + "='" +
		 * show_ident + "'", null, null, null, SONG_FILENAME);
		 */
	}

	public ArchiveSongObj getSong(long song_id) {

		ArchiveSongObj song = null;
		Cursor cur = db
				.rawQuery(
						"SELECT song.*,show.showIdent,show.showArtist,show.showTitle FROM songTbl song "
								+ "INNER JOIN showTbl show "
								+ "	ON show._id = song.show_id "
								+ "WHERE song._id = " + song_id, null);
		/*
		 * Cursor cur = db.query(true, SONG_TBL, new String[] { SONG_FILENAME,
		 * SONG_TITLE, SONG_SHOW_KEY, SONG_DOWNLOADED }, SONG_KEY + "=" +
		 * song_id, null, null, null, null, null);
		 */
		if (cur != null) {
			cur.moveToFirst();
			song = new ArchiveSongObj(cur.getString(cur
					.getColumnIndex(SONG_TITLE)), cur.getString(cur
					.getColumnIndex(SONG_FILENAME)), cur.getString(cur
					.getColumnIndex(SHOW_ARTIST))
					+ " Live at "
					+ cur.getString(cur.getColumnIndex(SHOW_TITLE)),
					cur.getString(cur.getColumnIndex(SHOW_IDENT)),
					Boolean.valueOf(cur.getString(cur
							.getColumnIndex(SONG_DOWNLOADED))));

		}
		cur.close();
		return song;
	}
	
	public boolean createPlaylist(String name){

		Cursor cur = db.rawQuery("Select 1 FROM playlistTbl pl WHERE pl.playlistName like '" + name + "'", null);
		int results = cur.getCount();
		cur.close();
		if(results > 0){
			return false;
		}
		db.execSQL("Insert into playlistTbl(playlistName) "
				+ "Select '" + name + "'");
		return true;
	}
	
	public boolean createPlaylist(ArchivePlaylistObj playlist){

		Cursor cur = db.rawQuery("Select 1 FROM playlistTbl pl WHERE pl.playlistName like '" + playlist.getTitle() + "'", null);
		int results = cur.getCount();
		cur.close();
		if(results > 0){
			return false;
		}
		db.execSQL("Insert into playlistTbl(playlistName) "
				+ "Select '" + playlist.getTitle() + "'");
		for(int i = 0; i < playlist.size(); i++){
			insertSongIntoPlaylist(playlist.getTitle(),playlist.getSong(i));
		}
		return true;
	}
	
	public void insertSongIntoPlaylist(String playlist, ArchiveSongObj song){
		insertSong(song);
		db.execSQL("Insert into playlistSongsTbl(playlist_id,song_id) "
				+ "Select pl._id,song._id from playlistTbl pl "
				+ "inner join songTbl song on song.fileName = '" + song.getFileName() + "' "
				+ "where pl.playlistName = '" + playlist + "'");
	}
	
	public void deleteSongFromPlaylist(long key){
		db.execSQL("Delete from playlistSongsTbl where _id = '" + key + "'");
	}
	
	public void clearPlaylist(long key){
		db.execSQL("Delete from playlistSongsTbl where playlist_id = '" + key + "'");
	}
	
	public Cursor getSongsFromPlaylist(long key){
		return db.rawQuery("Select pls._id,song.fileName,song.songTitle,song.isDownloaded," 
				+ "show.showIdent,show.showArtist + ' Live at ' + show.showTitle AS 'showTitle' "
				+ "from playlistSongsTbl pls "
				+ "inner join songTbl song on song._id = pls.song_id "
				+ "inner join showTbl show on show._id = song.show_id "
				+ "where pls.playlist_id = '" + key + "'", null);
	}
	
	public Cursor getPlaylists(){
		return db.rawQuery("Select * from playlistTbl", null);
	}
	
	public boolean updatePlaylistName(long key, String name){

		Cursor cur = db.rawQuery("Select 1 FROM playlistTbl pl WHERE pl.playlistName like '" + name + "'", null);
		int results = cur.getCount();
		cur.close();
		if(results > 0){
			return false;
		}
		db.execSQL("Update pl Set pl.playlistName = '" 
				+ name + "' from playlistTbl pl where pl._id = '" + key + "'");
		return true;
	}
	
	public Cursor getAutoCompleteCursor(String searchString){
		return db.rawQuery("Select * from autoCompleteTbl "
				+ "where searchText like '" + searchString + "%'", null);
	}

	/*
	 * public void deleteSong(String fileName, String show_ident){

	 * db.delete(SONG_TBL, SONG_SHOW_KEY + "='" + show_ident + "' AND " +
	 * SONG_FILENAME + "='" + fileName + "'", null); }
	 */

	public boolean upgradeDB() {
		SQLiteDatabase oldDB = SQLiteDatabase.openDatabase(DB_PATH, null,
				SQLiteDatabase.OPEN_READWRITE);
		if(oldDB == null){
			return false;
		}
		int oldVersion = oldDB.getVersion();
		switch (oldVersion) {
		case 5:
			ArrayList<ArchiveShowObj> downloadedShows = new ArrayList<ArchiveShowObj>();
			ArrayList<ArchiveShowObj> recentShows = new ArrayList<ArchiveShowObj>();
			ArrayList<ArchiveSongObj> downloadedSongs = new ArrayList<ArchiveSongObj>();

			Cursor dlShowCur = oldDB.query("downloadedShowsTbl", new String[] {
					"_id", "showIdent", "showTitle", "hasVBR", "hasLBR" },
					null, null, null, null, null);
			if (dlShowCur != null) {
				for (int i = 0; i < dlShowCur.getCount(); i++) {
					dlShowCur.moveToPosition(i);
					downloadedShows.add(new ArchiveShowObj(dlShowCur
							.getString(dlShowCur.getColumnIndex("showIdent")),
							dlShowCur.getString(dlShowCur
									.getColumnIndex("showTitle")), dlShowCur
									.getString(dlShowCur
											.getColumnIndex("hasVBR")),
							dlShowCur.getString(dlShowCur
									.getColumnIndex("hasLBR"))));
				}
			}
			dlShowCur.close();

			Cursor recentShowCur = oldDB.query("recentShowsTbl", new String[] {
					"_id", "showIdent", "showTitle", "hasVBR", "hasLBR" },
					null, null, null, null, null);
			if (recentShowCur != null) {
				recentShowCur.moveToFirst();
				for (int i = 0; i < recentShowCur.getCount(); i++) {
					recentShowCur.moveToPosition(i);
					recentShows.add(new ArchiveShowObj(recentShowCur
							.getString(recentShowCur
									.getColumnIndex("showIdent")),
							recentShowCur.getString(recentShowCur
									.getColumnIndex("showTitle")),
							recentShowCur.getString(recentShowCur
									.getColumnIndex("hasVBR")), recentShowCur
									.getString(recentShowCur
											.getColumnIndex("hasLBR"))));
				}
			}
			recentShowCur.close();

			Cursor dlSongCur = oldDB.query("downloadedSongsTbl", new String[] {
					"_id", "fileName", "songTitle", "showIdent", "showTitle" },
					null, null, null, null, null);
			if (dlSongCur != null) {
				dlSongCur.moveToFirst();
				for (int i = 0; i < dlSongCur.getCount(); i++) {
					dlSongCur.moveToPosition(i);
					downloadedSongs.add(new ArchiveSongObj(dlSongCur
							.getString(dlSongCur.getColumnIndex("songTitle")),
							dlSongCur.getString(dlSongCur
									.getColumnIndex("fileName")), dlSongCur
									.getString(dlSongCur
											.getColumnIndex("showTitle")),
							dlSongCur.getString(dlSongCur
									.getColumnIndex("showIdent")), true));
				}
			}
			dlSongCur.close();

			oldDB.execSQL("DROP TABLE IF EXISTS " + "downloadedShowsTbl");
			oldDB.execSQL("DROP TABLE IF EXISTS " + "downloadedSongsTbl");
			oldDB.execSQL("DROP TABLE IF EXISTS " + "recentShowsTbl");
			oldDB.execSQL("DROP TABLE IF EXISTS " + "prefsTbl");

			oldDB.execSQL("CREATE TABLE prefsTbl (_id INTEGER PRIMARY KEY, prefName TEXT, prefValue TEXT)");
			oldDB.execSQL("CREATE TABLE songTbl (_id INTEGER PRIMARY KEY, fileName TEXT, songTitle TEXT, show_id INTEGER, isDownloaded TEXT)");
			oldDB.execSQL("CREATE TABLE showTbl (_id INTEGER PRIMARY KEY, showIdent TEXT, showTitle TEXT, showArtist TEXT, showSource TEXT, hasVBR TEXT, hasLBR TEXT)");
			oldDB.execSQL("CREATE TABLE recentShowsTbl (_id INTEGER PRIMARY KEY, show_id INTEGER)");
			oldDB.execSQL("CREATE TABLE playlistTbl (_id INTEGER PRIMARY KEY, playlistName TEXT)");
			oldDB.execSQL("CREATE TABLE playlistSongsTbl (_id INTEGER PRIMARY KEY, playlist_id INTEGER, song_id INTEGER)");
			oldDB.execSQL("CREATE TABLE autoCompleteTbl (_id INTEGER PRIMARY KEY, searchText TEXT)");

			oldDB.execSQL("INSERT INTO prefsTbl (prefName, prefValue) "
					+ "SELECT 'numResults','10' "
					+ "UNION SELECT 'downloadPath','/archiveApp/' "
					+ "UNION SELECT 'streamFormat','VBR' "
					+ "UNION SELECT 'downloadFormat','VBR'");

			for (int i = 0; i < downloadedShows.size(); i++) {
				Cursor cur = oldDB.query("showTbl",
						new String[] { "showIdent" }, "showIdent" + "='"
								+ downloadedShows.get(i).getIdentifier() + "'",
						null, null, null, null);
				int results = cur.getCount();
				cur.close();
				if (results == 0) {
					ContentValues value = new ContentValues();
					value.put("showIdent", downloadedShows.get(i)
							.getIdentifier());
					value.put("showTitle", downloadedShows.get(i)
							.getShowTitle());
					value.put("showArtist", downloadedShows.get(i)
							.getShowArtist());
					value.put("showSource", downloadedShows.get(i)
							.getShowSource());
					value.put("hasVBR", downloadedShows.get(i).hasVBR());
					value.put("hasLBR", downloadedShows.get(i).hasLBR());
					oldDB.insert("showTbl", null, value);
				}
			}

			for (int i = 0; i < recentShows.size(); i++) {
				Cursor cur = oldDB.query("showTbl", new String[] { "_id",
						"showIdent" }, "showIdent" + "='"
						+ recentShows.get(i).getIdentifier() + "'", null, null,
						null, null);
				int results = cur.getCount();
				cur.close();
				if (results == 0) {
					ContentValues value = new ContentValues();
					value.put("showIdent", recentShows.get(i).getIdentifier());
					value.put("showTitle", recentShows.get(i).getShowTitle());
					value.put("showArtist", recentShows.get(i).getShowArtist());
					value.put("showSource", recentShows.get(i).getShowSource());
					value.put("hasVBR", recentShows.get(i).hasVBR());
					value.put("hasLBR", recentShows.get(i).hasLBR());
					oldDB.insert("showTbl", null, value);
				}
				cur = oldDB.query("showTbl",
						new String[] { "_id", "showIdent" }, "showIdent" + "='"
								+ recentShows.get(i).getIdentifier() + "'",
						null, null, null, null);
				if (cur != null) {
					cur.moveToFirst();
					ContentValues value = new ContentValues();
					value.put("show_id", cur.getInt(cur.getColumnIndex("_id")));
					oldDB.insert("recentShowsTbl", null, value);
				}
				cur.close();
			}

			for (int i = 0; i < downloadedSongs.size(); i++) {
				Cursor cur = oldDB.query("songTbl", new String[] { "_id",
						"fileName" }, "fileName" + "='"
						+ downloadedSongs.get(i).getFileName() + "'", null,
						null, null, null);
				int results = cur.getCount();
				cur.close();
				if (results == 0) {
					cur = oldDB.query("showTbl", new String[] { "_id",
							"showIdent" }, "showIdent" + "='"
							+ downloadedSongs.get(i).getShowIdentifier() + "'",
							null, null, null, null);
					if (cur != null) {
						cur.moveToFirst();
						ContentValues value = new ContentValues();
						value.put("fileName", downloadedSongs.get(i)
								.getFileName());
						value.put("songTitle", downloadedSongs.get(i)
								.toString());
						value.put("show_id",
								cur.getInt(cur.getColumnIndex("_id")));
						value.put("isDownloaded", "true");
						oldDB.insert("songTbl", null, value);
					}
					cur.close();
				}
			}

			oldDB.execSQL("CREATE VIEW recentShowsVw AS "
					+ "SELECT show.* "
					+ "FROM recentShowsTbl recent "
					+ "	INNER JOIN showTbl show "
					+ "		ON recent.show_id = show._id "
					+ "ORDER BY recent._id DESC");
			oldDB.execSQL("CREATE VIEW downloadedShowsVw " + "AS "
					+ "SELECT show.* FROM showTbl show " + "WHERE EXISTS "
					+ "	(SELECT 1 FROM songTbl song "
					+ "		WHERE song.show_id = show._id "
					+ "		AND song.isDownloaded like '%true%') "
					+ "ORDER BY show.showArtist, show.showTitle");
		case 6:
			oldDB.execSQL("INSERT INTO prefsTbl (prefName, prefValue) "
					+ "SELECT 'sortOrder','Date'");
		}
		return true;
	}

}
