/*
 * DataStore.java
 * VERSION 3.1
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
import java.util.HashMap;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

public class StaticDataStore extends SQLiteOpenHelper {

	private static final String LOG_TAG = StaticDataStore.class.getName();
	
	private static final String DB_NAME = "archivedb";
	private static String DB_PATH;
	private static final int DB_VERSION = 18;

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
	
	public static final String FAVORITE_TBL = "favoriteShowsTbl";
	public static final String FAVORITE_KEY = "_id";
	public static final String FAVORITE_SHOW_KEY = "show_id";

	public static final String RECENT_SHOW_VW = "recentShowsVw";
	public static final String DOWNLOADED_SHOW_VW = "downloadedShowsVw";
	public static final String FAVORITE_SHOW_VW = "favoriteShowsVw";
	
	public static final int SHOW_STATUS_NOT_DOWNLOADED = 0;
	public static final int SHOW_STATUS_PARTIALLY_DOWNLOADED = 1;
	public static final int SHOW_STATUS_FULLY_DOWNLOADED = 2;

	public boolean needsUpgrade = false;
	public boolean dbCopied = false;

	private final Context context;

	private SQLiteDatabase db;
	
	private static StaticDataStore dataStore = null;

	public static StaticDataStore getInstance(Context ctx) {
		if (dataStore == null) {
			dataStore = new StaticDataStore(ctx,"StaticDataStore");
		}
		return dataStore;
	}
	
	private StaticDataStore(Context context, String caller) {
		super(context, DB_NAME, null, DB_VERSION);
		this.context = context;
		DB_PATH = context.getDatabasePath(DB_NAME).toString();
		initialize();
		Logging.Log(LOG_TAG, "DB opened by (initialized): " + caller);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {

	}
	
	public void openDB(String caller) {
		if (!db.isOpen()) {
			openDataBase();
			Logging.Log(LOG_TAG, "DB opened by: " + caller);
		}
	}
	
	public void closeDB(String caller) {
		super.close();
		Logging.Log(LOG_TAG,"Database closed by:" + caller);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}
	
	private static String sanitize(String s){
		return s.replaceAll("[^-a-zA-Z0-9\\s-_@><&+\\\\/\\*]", "");
	}
	
	public static ArrayList<ArchiveShowObj> getShowListFromCursor(Cursor cur){
		ArrayList<ArchiveShowObj> shows = new ArrayList<ArchiveShowObj>();
		
		for(cur.moveToFirst(); !cur.isAfterLast(); cur.moveToNext()) {
			shows.add(new ArchiveShowObj(cur.getString(cur
					.getColumnIndex(SHOW_IDENT)), cur.getString(cur
					.getColumnIndex(SHOW_TITLE)), cur.getString(cur
					.getColumnIndex(SHOW_ARTIST)), cur.getString(cur
					.getColumnIndex(SHOW_SOURCE)), cur.getString(cur
					.getColumnIndex(SHOW_HASVBR)), cur.getString(cur
					.getColumnIndex(SHOW_HASLBR)), cur.getInt(cur
					.getColumnIndex("_id"))));
		}

		Logging.Log(LOG_TAG,"Returning " + shows.size() + " shows from the DB.");
		return shows;
	}
	
	public static ArrayList<ArchiveSongObj> getSongListFromCursor(Cursor cur){
		ArrayList<ArchiveSongObj> songs = new ArrayList<ArchiveSongObj>();
		
		for(cur.moveToFirst(); !cur.isAfterLast(); cur.moveToNext()) {
			songs.add(new ArchiveSongObj(
					cur.getString(cur.getColumnIndex(SONG_TITLE)), 
					cur.getString(cur.getColumnIndex("folderName")),
					cur.getString(cur.getColumnIndex(SONG_FILENAME)), 
					cur.getString(cur.getColumnIndex(SHOW_ARTIST)) + " Live at " + cur.getString(cur.getColumnIndex(SHOW_TITLE)),
					cur.getString(cur.getColumnIndex(SHOW_IDENT)),
					Boolean.valueOf(cur.getString(cur.getColumnIndex(SONG_DOWNLOADED))),
					cur.getInt(cur.getColumnIndex(PLAYLISTSONG_SONG_KEY))));
		}
		
		return songs;
	}

	public void initialize() {
		try {
			createDB();
		} catch (IOException e) {
			Logging.Log(LOG_TAG, "Unable to create database");
			Logging.Log(LOG_TAG, e.getStackTrace().toString());
		}
		if(!needsUpgrade){
			try {
				openDataBase();
			} catch (SQLException e) {
				Logging.Log(LOG_TAG, "Unable to open database");
				Logging.Log(LOG_TAG, e.getStackTrace().toString());
			}
		}
	}

	public void createDB() throws IOException {

		boolean dbExists = checkDB();
		if (dbExists) {
		} else {
			Logging.Log(LOG_TAG,
					"createDB() - Database does not exist");
			this.getReadableDatabase();
			this.close();
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
			Logging.Log(LOG_TAG, "checkDB() - Database not found");
		}

		if (checkDB != null) {
			if (checkDB.getVersion() != DB_VERSION) {
				Logging.Log(LOG_TAG,
						"checkDB() - Wrong DB version: old "
								+ checkDB.getVersion() + " new " + DB_VERSION);
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
		Logging.Log(LOG_TAG, "copyDB() - Copying database to "
				+ DB_PATH);
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
		dbCopied = true;
		Logging.Log(LOG_TAG, "copyDB() - Finished copying database");
	}

	public void openDataBase() throws SQLiteException {
		db = SQLiteDatabase.openDatabase(DB_PATH, null,
				SQLiteDatabase.OPEN_READWRITE);
		db.setVersion(DB_VERSION);
		needsUpgrade = false;
	}

	public String getPref(String pref_name) {
		try{
			Cursor cur = db.rawQuery("SELECT prefValue FROM prefsTbl " 
				+ "WHERE prefName = '" + sanitize(pref_name) + "'", null);
			cur.moveToFirst();
			String retString = cur.getString(cur.getColumnIndex(PREF_VALUE));
			cur.close();
			return retString;
		} catch (SQLiteException e){
			return "NULL";
		}
	}
	
//	private void rebuildPrefsTbl(){
//		db.execSQL("DROP TABLE IF EXISTS prefsTbl");
//		db.execSQL("Insert into prefsTbl(prefName,prefValue) Select 'downloadFormat','VBR'");
//		db.execSQL("Insert into prefsTbl(prefName,prefValue) Select 'downloadPath','/archiveApp/'");
//		db.execSQL("Insert into prefsTbl(prefName,prefValue) Select 'numResults','10'");
//		db.execSQL("Insert into prefsTbl(prefName,prefValue) Select 'streamFormat','VBR'");
//		db.execSQL("Insert into prefsTbl(prefName,prefValue) Select 'sortOrder','Date'");
//		db.execSQL("Insert into prefsTbl(prefName,prefValue) Select 'artistUpdate','2010-01-01'");
//		db.execSQL("Insert into prefsTbl(prefName,prefValue) Select 'splashShown','false'");
//		db.execSQL("Insert into prefsTbl(prefName,prefValue) Select 'userId','0'");
//		db.execSQL("Insert into prefsTbl(prefName,prefValue) Select 'artistResultType','Top All Time Artists'");
//		db.execSQL("Insert into prefsTbl(prefName,prefValue) Select 'showResultType','Top All Time Shows'");
//		db.execSQL("INSERT INTO prefsTbl(prefName,prefValue) Select 'showsByArtistResultType','Top All Time Shows'");
//	}

	public void updatePref(String pref_name, String pref_value) {
		Logging.Log(LOG_TAG, "Update " + pref_name + " to " + pref_value);
		db.execSQL("UPDATE prefsTbl SET prefValue = '"
				+ sanitize(pref_value) + "' WHERE prefName = '"
				+ sanitize(pref_name) + "'");
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
	}
	
	public void updateShow(ArchiveShowObj show) {
		db.execSQL("UPDATE showTbl SET "
				+ "showTitle = '" + show.getShowTitle().replaceAll("'", "''") + "', "
				+ "showArtist = '" + show.getShowArtist().replaceAll("'", "''") + "' "
				+ "WHERE showIdent = '" + show.getIdentifier() + "'");
	}
	
	public ArrayList<ArchiveShowObj> getBadShows() {
		Cursor cur = db.rawQuery("select * from showTbl where LTRIM(RTRIM(showTitle)) = '' or LTRIM(RTRIM(showArtist)) = ''", null);
		ArrayList<ArchiveShowObj> shows = getShowListFromCursor(cur);
		cur.close();
		return shows;
	}

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

	public ArrayList<ArchiveShowObj> getRecentShows() {
		Logging.Log(LOG_TAG, "Returning all recent shows");
		/*
		 * return db.query(RECENT_TBL, new String[] { SHOW_KEY, SHOW_IDENT,
		 * SHOW_TITLE, SHOW_HASVBR, SHOW_HASLBR }, null, null, null, null,
		 * null);
		 */
		Cursor cur = db.rawQuery("SELECT * FROM " + RECENT_SHOW_VW, null);
		ArrayList<ArchiveShowObj> shows = getShowListFromCursor(cur);
		cur.close();
		return shows;
	}

	public ArrayList<ArchiveShowObj> getDownloadShows() {
		Logging.Log(LOG_TAG, "Returning all downloaded shows");
		/*
		 * return db.query(SHOW_TBL, new String[] { SHOW_KEY, SHOW_IDENT,
		 * SHOW_TITLE, SHOW_HASVBR, SHOW_HASLBR }, null, null, null, null,
		 * null);
		 */
		Cursor cur = db.rawQuery("SELECT * FROM " + DOWNLOADED_SHOW_VW, null);
		ArrayList<ArchiveShowObj> shows = getShowListFromCursor(cur);
		cur.close();
		return shows;
	}

	public ArchiveShowObj getShow(String identifier) {
		Logging.Log(LOG_TAG, "Getting show: " + identifier);
		ArchiveShowObj show = null;
		Cursor cur = db.query(true, SHOW_TBL,
				new String[] { SHOW_IDENT, SHOW_TITLE, SHOW_ARTIST,
						SHOW_SOURCE, SHOW_HASVBR, SHOW_HASLBR, "_id" }, SHOW_IDENT + "="
						+ "'" + identifier + "'", null, null, null, null, null);
		if (cur != null) {
			cur.moveToFirst();
			show = new ArchiveShowObj(cur.getString(cur
					.getColumnIndex(SHOW_IDENT)), cur.getString(cur
					.getColumnIndex(SHOW_TITLE)), cur.getString(cur
					.getColumnIndex(SHOW_ARTIST)), cur.getString(cur
					.getColumnIndex(SHOW_SOURCE)), cur.getString(cur
					.getColumnIndex(SHOW_HASVBR)), cur.getString(cur
					.getColumnIndex(SHOW_HASLBR)), cur.getInt(cur
					.getColumnIndex("_id")));
		}
		cur.close();
		return show;
	}

	public void deleteRecentShow(long show_id) {
		Logging.Log(LOG_TAG, "Deleting recent show at id=" + show_id);
		db.delete(RECENT_TBL, RECENT_SHOW_KEY + "=" + show_id, null);
	}

	public void clearRecentShows() {
		Logging.Log(LOG_TAG, "Deleting all show");
		db.delete(RECENT_TBL, null, null);
	}

	public int insertSong(ArchiveSongObj song) {
		db.execSQL("INSERT INTO songTbl(fileName,songTitle,show_id,isDownloaded,folderName) "
				+ "SELECT '"
				+ song.getFileName()
				+ "','"
				+ song.toString().replaceAll("'", "''")
				+ "',show._id,'false','' "
				+ "FROM showTbl show "
				+ "WHERE show.showIdent = '"
				+ song.getShowIdentifier()
				+ "' "
				+ "AND NOT EXISTS (SELECT 1 FROM songTbl song WHERE song.fileName = '"
				+ song.getFileName() + "')");
		Cursor cur = db.rawQuery("Select _id as song_id from songTbl "
				+ "where fileName = '" + song.getFileName() + "'", null);
		cur.moveToFirst();
		int id = cur.getInt(cur.getColumnIndex(PLAYLISTSONG_SONG_KEY));
		cur.close();
		if (song.hasFolder()) {
			db.execSQL("Update songTbl set folderName = '" + song.getFolder() + "' where _id = " + id);
		}
		return id;
	}
	
	public void setSongDownloading(ArchiveSongObj song, long id){
		Logging.Log(LOG_TAG,"Start Downloading " + song.getFileName() + ": " + id);
		db.execSQL("UPDATE songTbl "
				+ "SET download_id = '"
				+ id + "' WHERE fileName = '"
				+ song.getFileName() + "'");
	}
	
	public boolean getSongIsDownloading(ArchiveSongObj song) {
		Logging.Log(LOG_TAG,"Get downloading status for " + song.getFileName());
		Cursor cur = db.rawQuery("Select count(1) as count from songTbl "
				+ "where fileName = '" + song.getFileName() + "' and download_id is not null and isDownloaded = 'false'", null);
		cur.moveToFirst();
		int count = cur.getInt(cur.getColumnIndex("count"));
		Logging.Log(LOG_TAG,"Result: " + count);
		cur.close();
		return count > 0;
	}

	public void setSongDownloaded(long id) {
		Logging.Log(LOG_TAG,"Finished Downloading: " + id);
		db.execSQL("UPDATE songTbl "
				+ "SET isDownloaded = 'true' "
				+ "WHERE download_id = '"
				+ id + "'");
	}

	public void setSongDownloaded(String fileName) {
		Logging.Log(LOG_TAG,"Adding Song: " + fileName);
		db.execSQL("UPDATE songTbl "
				+ "SET isDownloaded = 'true' "
				+ "WHERE fileName = '"
				+ fileName + "'");
	}
	
	public void setSongDeleted(ArchiveSongObj song) {
		db.execSQL("UPDATE songTbl "
				+ "SET isDownloaded = 'false', download_id = null "
				+ "WHERE fileName = '"
				+ song.getFileName() + "'");
	}
	
	public void setShowDeleted(ArchiveShowObj show){
		db.execSQL("UPDATE songTbl "
				+ "SET isDownloaded = 'false', download_id = null "
				+ "WHERE EXISTS (SELECT 1 from showTbl show where "
				+ "show.showIdent = '" + show.getIdentifier() + "' and "
				+ "show._id = show_id)");
	}
	
	public boolean getShowExists(ArchiveShowObj show) {
		Cursor cur = db.rawQuery("Select COUNT(1) as count from songTbl song "
				+ "INNER JOIN showTbl show "
				+ "ON song.show_id = show._id "
				+ "AND show.showIdent = '" + show.getIdentifier() + "' "
				+ "AND show.showExists = 'true'", null);
		cur.moveToFirst();
		int count = cur.getInt(cur.getColumnIndex("count"));
		cur.close();
		return count > 0;
	}
	
	public int getShowDownloadStatus(ArchiveShowObj show) {
		Cursor cur = db.rawQuery("Select "
				+ "(Select count(1) from songTbl song "
				+ "inner join showTbl show on show._id = song.show_id and show.showIdent = '" + show.getIdentifier() + "' "
				+ "where song.isDownloaded = 'true') as 'downloaded', "
				+ "(Select count(1) from songTbl song "
				+ "inner join showTbl show on show._id = song.show_id and show.showIdent = '" + show.getIdentifier() + "') "
				+ "as 'total'", null);
		cur.moveToFirst();
		int downloaded = cur.getInt(cur.getColumnIndex("downloaded"));
		int total = cur.getInt(cur.getColumnIndex("total"));
		cur.close();
		if (downloaded > 0) {
			if (downloaded < total) {
				return SHOW_STATUS_PARTIALLY_DOWNLOADED;
			}
			else {
				return SHOW_STATUS_FULLY_DOWNLOADED;
			}
		}
		else {
			return SHOW_STATUS_NOT_DOWNLOADED;
		}
	}
	
	public void setShowExists(ArchiveShowObj show) {
		db.execSQL("Update showTbl set showExists = 'true' where showIdent = '" + show.getIdentifier() + "'");
	}

	public boolean songIsDownloaded(String song_filename) {
		/*Cursor cur = db.query(SONG_TBL, new String[] { SONG_FILENAME },
				SONG_FILENAME + "='" + song_filename + "'", null, null, null,
				null);*/
		Cursor cur = db.rawQuery("SELECT 1 FROM songTbl song WHERE song.fileName = '"
				+ song_filename + "' AND song.isDownloaded LIKE 'true'", null);
		int results = cur.getCount();
		cur.close();
		return results > 0;
	}

	public ArrayList<ArchiveSongObj> getSongsFromShow(String showIdent) {
		Cursor cur = db.rawQuery(
				"SELECT song.*, song._id as song_id,show.showIdent,show.showArtist,show.showTitle FROM songTbl song "
						+ "INNER JOIN showTbl show "
						+ "	ON song.show_id = show._id "
						+ "	AND show.showIdent = '" + showIdent + "'",
				null);
		ArrayList<ArchiveSongObj> songs = getSongListFromCursor(cur);
		cur.close();
		return songs;
	}

	public ArrayList<ArchiveSongObj> getDownloadedSongsFromShow(String showIdent) {
		Cursor cur = db.rawQuery(
				"SELECT song.*, song._id as song_id,show.showIdent,show.showArtist,show.showTitle FROM songTbl song "
						+ "INNER JOIN showTbl show "
						+ "	ON song.show_id = show._id "
						+ "	AND show.showIdent = '" + showIdent + "' "
						+ " AND song.isDownloaded = 'true'",
				null);
		ArrayList<ArchiveSongObj> songs = getSongListFromCursor(cur);
		cur.close();
		return songs;
	}

	public ArrayList<ArchiveSongObj> getSongsFromShowKey(long id) {
		Cursor cur = db.rawQuery(
						"SELECT song.*, song._id as song_id,show.showIdent,show.showArtist,show.showTitle FROM songTbl song "
								+ "INNER JOIN showTbl show "
								+ "	ON song.show_id = show._id "
								+ "	AND show._id = '" + id + "' "
								+ "ORDER BY song.fileName",
						null);
		ArrayList<ArchiveSongObj> songs = getSongListFromCursor(cur);
		cur.close();
		return songs;
	}
	
	public void insertKnownSongIntoPlaylist(int playlist_id, ArchiveSongObj song, int position){
		if(playlist_id <= 0){
			//Playlist not saved yet
			return;
		}
		db.execSQL("INSERT INTO playlistSongsTbl(playlist_id,song_id,trackNum) "
				+ "SELECT " + playlist_id + ",song._id," + position + " FROM songTbl song " +
				"WHERE song.fileName = '" + song.getFileName() + "'");
	}
	
	public void clearNowPlayingSongs() {
		db.execSQL("DELETE FROM playlistSongsTbl");
	}
	
	//Returns cursor of songs associated with playlist
	public ArrayList<ArchiveSongObj> getSongsFromPlaylist(long key){
		Cursor cur = db.rawQuery("SELECT pls._id,pls.song_id,song.fileName,song.songTitle,song.isDownloaded,song.folderName," 
				+ "show.showIdent,show.showArtist,show.showTitle "
				+ "FROM playlistSongsTbl pls "
				+ "INNER JOIN songTbl song ON song._id = pls.song_id "
				+ "INNER JOIN showTbl show ON show._id = song.show_id "
				+ "WHERE pls.playlist_id = '" + key + "' " +
						"ORDER BY pls.trackNum", null);
		ArrayList<ArchiveSongObj> songs = getSongListFromCursor(cur);
		cur.close();
		return songs;
	}
	
	public ArrayList<ArchiveSongObj> getNowPlayingSongs() {
		return getSongsFromPlaylist(0);
	}
	
	public void setNowPlayingSongs(ArrayList<ArchiveSongObj> songs) {
		clearNowPlayingSongs();
		if (songs.size() > 0) {
			StringBuilder sql = new StringBuilder(); 
			sql.append("INSERT INTO playlistSongsTbl(playlist_id,song_id,trackNum)");
			
			int index = 0;
			for (ArchiveSongObj s : songs) {
				sql.append("SELECT 0," + s.getID() + "," + index + " ");
				if (index < songs.size() - 1) {
					sql.append("UNION ALL ");
					index++;
				}
			}
			db.execSQL(sql.toString());
		}
	}
	
	public void addSongToNowPlaying(ArchiveSongObj s) {
		db.execSQL("INSERT INTO playlistSongsTbl(playlist_id,song_id,trackNum) "
				+ "Select 0," + s.getID() + ",MAX(trackNum) "
				+ "from playlistSongsTbl where playlist_id = 0");
	}

	public void addArtist(String artist, String numShows){
		db.execSQL("INSERT INTO artistTbl(artistName,numShows) "
				+ "SELECT '" + artist + "','" + numShows + "' "
				+ "WHERE NOT EXISTS (SELECT 1 FROM artistTbl "
				+ "WHERE artistName LIKE '" + artist + "')");
		/*db.execSQL("UPDATE artistTbl SET numShows = '" 
				+ numShows + "' WHERE artistName LIKE '" + artist + "'");*/
	}
	
	public ArrayList<HashMap<String, String>> getArtist(String firstLetter){
		//Apparently you have to manually implement regular expressions, so fuck that
		//Ugly hack that works
		Cursor cur;
		if(firstLetter.equalsIgnoreCase("!")){
			cur = db.rawQuery("SELECT * FROM artistTbl WHERE artistName NOT LIKE 'a%' AND artistName NOT LIKE 'b%' " +
					"AND artistName NOT LIKE 'c%' AND artistName NOT LIKE 'd%' AND artistName NOT LIKE 'e%' " +
					"AND artistName NOT LIKE 'f%' AND artistName NOT LIKE 'g%' AND artistName NOT LIKE 'h%' " +
					"AND artistName NOT LIKE 'i%' AND artistName NOT LIKE 'j%' AND artistName NOT LIKE 'k%' " +
					"AND artistName NOT LIKE 'l%' AND artistName NOT LIKE 'm%' AND artistName NOT LIKE 'n%' " +
					"AND artistName NOT LIKE 'o%' AND artistName NOT LIKE 'p%' AND artistName NOT LIKE 'q%' " +
					"AND artistName NOT LIKE 'r%' AND artistName NOT LIKE 's%' AND artistName NOT LIKE 't%' " +
					"AND artistName NOT LIKE 'u%' AND artistName NOT LIKE 'v%' AND artistName NOT LIKE 'w%' " +
					"AND artistName NOT LIKE 'x%' AND artistName NOT LIKE 'y%' AND artistName NOT LIKE 'z%' " +
					"ORDER BY artistName", null);
		}
		else{
			cur = db.rawQuery("SELECT * FROM artistTbl "
					+ "WHERE artistName like '" + firstLetter + "%' ORDER BY artistName",null);
		}
		ArrayList<HashMap<String, String>> artists = new ArrayList<HashMap<String, String>>();
		for (cur.moveToFirst(); !cur.isAfterLast(); cur.moveToNext()) {
			String artistName = cur.getString(cur.getColumnIndex("artistName"));
			String numShows = cur.getString(cur.getColumnIndex("numShows"));
			HashMap<String, String> tempMap = new HashMap<String, String>();
			tempMap.put("artist", artistName);
			tempMap.put("shows", numShows);
			artists.add(tempMap);
		}
		cur.close();
		return artists;
	}
	
	public void insertArtistBulk(ArrayList<ArrayList<String>> artists){
		//db.execSQL("PRAGMA synchronous=OFF");
		/*InsertHelper ih = new InsertHelper(db,"artistTbl");
		ih.prepareForInsert();*/
		Logging.Log(LOG_TAG,"Bulk inserting " + artists.size() + " artists");
		db.execSQL("DELETE FROM artistTbl");
		try{
			db.beginTransaction();
			for(int i = 0; i < artists.size(); i++){
				ContentValues artist = new ContentValues();
				artist.put("artistName",artists.get(i).get(0));
				artist.put("numShows",artists.get(i).get(1));
				db.insert("artistTbl",null,artist);
			}
			db.setTransactionSuccessful();
		} catch(SQLException e){
			Logging.Log(LOG_TAG,e.toString());
		} finally{
			db.endTransaction();
		}
		
		//ih.execute();
	}
	
	public void clearArtists(){
		db.execSQL("UPDATE prefsTbl SET prefValue = '2010-01-01' WHERE prefName LIKE 'artistUpdate'");
	}
	
	public void insertFavoriteShow(ArchiveShowObj show) {
		insertShow(show);
		db.execSQL("INSERT INTO favoriteShowsTbl(show_id) "
				+ "SELECT show._id "
				+ "FROM showTbl show "
				+ "WHERE show.showIdent = '"
				+ show.getIdentifier()
				+ "' "
				+ "AND NOT EXISTS (SELECT 1 FROM favoriteShowsVw fav WHERE fav.showIdent = '"
				+ show.getIdentifier() + "')");
	}

	public ArrayList<ArchiveShowObj> getFavoriteShows() {
		Logging.Log(LOG_TAG, "Returning all favorite shows");
		/*
		 * return db.query(RECENT_TBL, new String[] { SHOW_KEY, SHOW_IDENT,
		 * SHOW_TITLE, SHOW_HASVBR, SHOW_HASLBR }, null, null, null, null,
		 * null);
		 */
		Cursor cur = db.rawQuery("SELECT * FROM " + FAVORITE_SHOW_VW, null);
		ArrayList<ArchiveShowObj> shows = getShowListFromCursor(cur);
		cur.close();
		return shows;
	}

	public void deleteFavoriteShow(long show_id) {
		Logging.Log(LOG_TAG, "Deleting favorite show at show_id=" + show_id);
		db.execSQL("DELETE FROM favoriteShowsTbl WHERE show_id=" + show_id);
	}

	public void clearFavoriteShows() {
		Logging.Log(LOG_TAG, "Deleting all show");
		db.execSQL("DELETE FROM favoriteShowsTbl");
	}
	
	public String[] getArtistsStrings(){
		Cursor c = db.rawQuery("SELECT artistName FROM artistTbl", null);
		String[] artists = new String[c.getCount()];
		int i = 0;
        while (c.moveToNext())
        {
             artists[i] = c.getString(c.getColumnIndex("artistName"));
             i++;
        }
        c.close();
        return artists;
	}

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
					downloadedSongs.add(new ArchiveSongObj(
							dlSongCur.getString(dlSongCur.getColumnIndex("songTitle")),
							"",
							dlSongCur.getString(dlSongCur.getColumnIndex("fileName")), 
							dlSongCur.getString(dlSongCur.getColumnIndex("showTitle")),
							dlSongCur.getString(dlSongCur.getColumnIndex("showIdent")), true, -1));
				}
			}
			dlSongCur.close();

			oldDB.execSQL("DROP TABLE IF EXISTS " + "downloadedShowsTbl");
			oldDB.execSQL("DROP TABLE IF EXISTS " + "downloadedSongsTbl");
			oldDB.execSQL("DROP TABLE IF EXISTS " + "recentShowsTbl");
			oldDB.execSQL("DROP TABLE IF EXISTS " + "prefsTbl");

			oldDB.execSQL("CREATE TABLE IF NOT EXISTS prefsTbl (_id INTEGER PRIMARY KEY, prefName TEXT, prefValue TEXT)");
			oldDB.execSQL("CREATE TABLE IF NOT EXISTS songTbl (_id INTEGER PRIMARY KEY, fileName TEXT, songTitle TEXT, show_id INTEGER, isDownloaded TEXT)");
			oldDB.execSQL("CREATE TABLE IF NOT EXISTS showTbl (_id INTEGER PRIMARY KEY, showIdent TEXT, showTitle TEXT, showArtist TEXT, showSource TEXT, hasVBR TEXT, hasLBR TEXT)");
			oldDB.execSQL("CREATE TABLE IF NOT EXISTS recentShowsTbl (_id INTEGER PRIMARY KEY, show_id INTEGER)");
			oldDB.execSQL("CREATE TABLE IF NOT EXISTS playlistTbl (_id INTEGER PRIMARY KEY, playlistName TEXT)");
			oldDB.execSQL("CREATE TABLE IF NOT EXISTS playlistSongsTbl (_id INTEGER PRIMARY KEY, playlist_id INTEGER, song_id INTEGER)");
			oldDB.execSQL("CREATE TABLE IF NOT EXISTS autoCompleteTbl (_id INTEGER PRIMARY KEY, searchText TEXT)");

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

			oldDB.execSQL("CREATE VIEW IF NOT EXISTS recentShowsVw AS "
					+ "SELECT show.* "
					+ "FROM recentShowsTbl recent "
					+ "	INNER JOIN showTbl show "
					+ "		ON recent.show_id = show._id "
					+ "ORDER BY recent._id DESC");
			oldDB.execSQL("CREATE VIEW IF NOT EXISTS downloadedShowsVw " + "AS "
					+ "SELECT show.* FROM showTbl show " + "WHERE EXISTS "
					+ "	(SELECT 1 FROM songTbl song "
					+ "		WHERE song.show_id = show._id "
					+ "		AND song.isDownloaded like '%true%') "
					+ "ORDER BY show.showArtist, show.showTitle");
		case 6:
			oldDB.execSQL("INSERT INTO prefsTbl (prefName, prefValue) "
					+ "SELECT 'sortOrder','Date'");
		case 7:
			oldDB.execSQL("CREATE TABLE IF NOT EXISTS artistTbl (_id INTEGER PRIMARY KEY, artistName TEXT, numShows TEXT)");
			oldDB.execSQL("INSERT INTO prefsTbl (prefName, prefValue) "
					+ "SELECT 'artistUpdate','2010-01-01'");
		case 8:
			oldDB.execSQL("ALTER TABLE playlistSongsTbl ADD COLUMN trackNum INTEGER");
			oldDB.execSQL("INSERT INTO playlistTbl(_id,playlistName) " +
				"SELECT 1,'Now Playing'");
		case 9:
			oldDB.execSQL("CREATE TABLE IF NOT EXISTS favoriteShowsTbl (_id INTEGER PRIMARY KEY, show_id INTEGER)");
			oldDB.execSQL("CREATE VIEW IF NOT EXISTS favoriteShowsVw AS "
					+ "SELECT show.* "
					+ "FROM favoriteShowsTbl fav "
					+ "	INNER JOIN showTbl show "
					+ "		ON fav.show_id = show._id "
					+ "ORDER BY fav._id DESC");
		case 10:
			oldDB.execSQL("INSERT INTO prefsTbl (prefName, prefValue) SELECT 'splashShown','false'");
		case 11:
			oldDB.execSQL("INSERT INTO prefsTbl (prefName, prefValue) SELECT 'userId','0'");
		case 12:
			oldDB.execSQL("INSERT INTO prefsTbl (prefName, prefValue) " +
					"SELECT 'showResultType','Top All Time Shows' " +
					"UNION SELECT 'artistResultType','Top All Time Artists'");
		case 13:
			oldDB.execSQL("INSERT INTO prefsTbl(prefName,prefValue) " +
					"Select 'showsByArtistResultType','Top All Time Shows'");
		case 14:
			oldDB.execSQL("ALTER TABLE songTbl ADD COLUMN download_id INTEGER");
		case 15:
			oldDB.execSQL("ALTER TABLE songTbl ADD COLUMN folderName TEXT");
			oldDB.execSQL("Update songTbl set folderName = ''");
			oldDB.execSQL("ALTER TABLE showTbl ADD COLUMN showExists TEXT");
		case 16:
			oldDB.execSQL("INSERT INTO prefsTbl (prefName, prefValue) SELECT 'nowPlayingPosition','0'");
		case 17:
			oldDB.execSQL("Update songTbl set download_id = null where isDownloaded = 'false'");
		}
		return true;
	}

}
