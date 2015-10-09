/*
 * DataStore.java
 * VERSION 3.X
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

	private static final String LOG_TAG = DataStore.class.getName();
	
	private static final String DB_NAME = "archivedb";
	private static String DB_PATH;
	private static final int DB_VERSION = 14;

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
	
	private String sanitize(String s){
		return s.replaceAll("[^-a-zA-Z0-9\\s-_@><&+\\\\/\\*]", "");
	}

	public void initialize() {
		try {
			createDB();
		} catch (IOException e) {
			Log.e(LOG_TAG, "Unable to create database");
			Log.e(LOG_TAG, e.getStackTrace().toString());
		}
		if(!needsUpgrade){
			try {
				openDataBase();
			} catch (SQLException e) {
				Log.e(LOG_TAG, "Unable to open database");
				Log.e(LOG_TAG, e.getStackTrace().toString());
			}
		}
	}

	public void createDB() throws IOException {

		boolean dbExists = checkDB();
		if (dbExists) {
			
		} else {
			Log.d(LOG_TAG,
					"createDB() - Database does not exist");
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
				Log.d(LOG_TAG,
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
		Log.d(LOG_TAG, "copyDB() - Copying database to "
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
		
	}

	public void openDataBase() throws SQLiteException {
		db = SQLiteDatabase.openDatabase(DB_PATH, null,
				SQLiteDatabase.OPEN_READWRITE);
		db.setVersion(DB_VERSION);
		needsUpgrade = false;
	}

	public Cursor getPrefCursor() {
		Cursor cur = db.rawQuery("SELECT * FROM prefsTbl ", null);
		if (cur != null) {
			cur.moveToFirst();
		}
		return cur;
	}

	public String getPref(String pref_name) {
		try{
			Cursor cur = db.rawQuery("SELECT prefValue FROM prefsTbl " 
				+ "WHERE prefName = '" + sanitize(pref_name) + "'", null);
			if (cur != null) {
				cur.moveToFirst();
				String retString = cur.getString(cur.getColumnIndex(PREF_VALUE));
				cur.close();
				return retString;
			}
		} catch (SQLiteException e){
			rebuildPrefsTbl();
			return getPref(pref_name);
		}
		return "NULL";
	}
	
	private void rebuildPrefsTbl(){
		db.execSQL("DROP TABLE IF EXISTS prefsTbl");
		db.execSQL("Insert into prefsTbl(prefName,prefValue) Select 'downloadFormat','VBR'");
		db.execSQL("Insert into prefsTbl(prefName,prefValue) Select 'downloadPath','/archiveApp/'");
		db.execSQL("Insert into prefsTbl(prefName,prefValue) Select 'numResults','10'");
		db.execSQL("Insert into prefsTbl(prefName,prefValue) Select 'streamFormat','VBR'");
		db.execSQL("Insert into prefsTbl(prefName,prefValue) Select 'sortOrder','Date'");
		db.execSQL("Insert into prefsTbl(prefName,prefValue) Select 'artistUpdate','2010-01-01'");
		db.execSQL("Insert into prefsTbl(prefName,prefValue) Select 'splashShown','false'");
		db.execSQL("Insert into prefsTbl(prefName,prefValue) Select 'userId','0'");
		db.execSQL("Insert into prefsTbl(prefName,prefValue) Select 'artistResultType','Top All Time Artists'");
		db.execSQL("Insert into prefsTbl(prefName,prefValue) Select 'showResultType','Top All Time Shows'");
		db.execSQL("INSERT INTO prefsTbl(prefName,prefValue) Select 'showsByArtistResultType','Top All Time Shows'");
	}

	public void updatePref(String pref_name, String pref_value) {
		
		db.execSQL("UPDATE prefsTbl SET prefValue = '"
				+ sanitize(pref_value) + "' WHERE prefName = '"
				+ sanitize(pref_name) + "'");
	}

	public void insertShow(ArchiveShowObj show) {
		Log.d(LOG_TAG, "insertShow(" + show.getIdentifier()
				+ "," + show.getArtistAndTitle() + ")");
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
		Log.d(LOG_TAG, "insertSong(" + song.getFileName()
				+ "," + song.toString() + ")");
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
		 * db.insert(SONG_TBL, null, value); Log.d(LOG_TAG,
		 * "insertSong() - Inserting song [" + row + "," + song.getFileName() +
		 * "," + song.toString() + "," + song.getShowIdentifier() + "," +
		 * song.getShowTitle() + "]"); } else{ Log.d(LOG_TAG,
		 * "insertSong() - Song exists"); }
		 */
	}

	public void setSongDownloaded(ArchiveSongObj song) {
		db.execSQL("UPDATE songTbl "
				+ "SET isDownloaded = 'true' "
				+ "WHERE fileName = '"
				+ song.getFileName() + "'");
	}
	
	public void setSongDeleted(ArchiveSongObj song) {
		db.execSQL("UPDATE songTbl "
				+ "SET isDownloaded = 'false' "
				+ "WHERE fileName = '"
				+ song.getFileName() + "'");
	}
	
	public void setShowDeleted(ArchiveShowObj show){
		db.execSQL("UPDATE songTbl "
				+ "SET isDownloaded = 'false' "
				+ "WHERE EXISTS (SELECT 1 from showTbl show where "
				+ "show.showIdent = '" + show.getIdentifier() + "' and "
				+ "show._id = show_id");
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

	public Cursor getSongsFromShow(String showIdent) {
		Log.d(LOG_TAG, "Returning all songs with identifier "
				+ showIdent);
		return db
		.rawQuery(
				"SELECT song.*,show.showIdent,show.showArtist + ' Live at ' + show.showTitle AS 'showTitle' FROM songTbl song "
						+ "INNER JOIN showTbl show "
						+ "	ON song.show_id = show._id "
						+ "	AND show.showIdent = '" + showIdent + "' "
						+ "AND song.isDownloaded = 'true'",
				null);
		/*
		 * return db.query(SONG_TBL, new String[] { SONG_KEY, SONG_FILENAME,
		 * SONG_TITLE, SONG_SHOW_KEY, SONG_DOWNLOADED }, SONG_SHOW_KEY + "='" +
		 * show_ident + "'", null, null, null, SONG_FILENAME);
		 */
	}

	public Cursor getSongsFromShowKey(long id) {
		Log.d(LOG_TAG, "Returning all songs with key "
				+ id);
		return db
				.rawQuery(
						"SELECT song.*,show.showIdent,show.showArtist + ' Live at ' + show.showTitle AS 'showTitle' FROM songTbl song "
								+ "INNER JOIN showTbl show "
								+ "	ON song.show_id = show._id "
								+ "	AND show._id = '" + id + "' "
								+ "ORDER BY song.fileName",
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
			Log.d(LOG_TAG,
					"Returning Song: " + song.toString() + "-"
							+ song.getFileName() + "-" + song.getShowTitle());
		}
		cur.close();
		return song;
	}
	
	//Playlist functions
	
	//Returns the playlist_id (key) of the newly created playlist to update playlist object
	//Will return -1 if playlist with same name exists or error inserting
	public int storePlaylist(ArchivePlaylistObj playlist){
		
		Cursor cur = db.rawQuery("SELECT _id FROM playlistTbl pl WHERE pl.playlistName like '" + sanitize(playlist.getTitle()) + "'", null);
		int results = cur.getCount();
		cur.close();
		if(results > 0){
			//Playlist exists
			return -1;
		}
		db.execSQL("INSERT INTO playlistTbl(playlistName) "
				+ "SELECT '" + sanitize(playlist.getTitle()) + "'");
		cur = db.rawQuery("SELECT _id FROM playlistTbl pl WHERE pl.playlistName like '" + sanitize(playlist.getTitle()) + "'", null);
		results = cur.getCount();
		if(results == 0){
			cur.close();
			return -1;
		}
		cur.moveToFirst();
		int key = cur.getInt(cur.getColumnIndex("_id"));
		cur.close();
		for(int i = 0; i < playlist.size(); i++){
			insertSongIntoPlaylist(key,playlist.getSong(i),i);
		}
		return key;
	}
	
	public void insertSongIntoPlaylist(int playlist_id, ArchiveSongObj song, int position){
		if(playlist_id <= 0){
			//Playlist not saved yet
			return;
		}
		insertSong(song);
		db.execSQL("INSERT INTO playlistSongsTbl(playlist_id,song_id,trackNum) "
				+ "SELECT " + playlist_id + ",song._id," + position + " FROM songTbl song " +
				"WHERE song.fileName = '" + song.getFileName() + "'");
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
	
	public void insertSongAtEndOfPlaylist(int playlist_id, ArchiveSongObj song){
		if(playlist_id <= 0){
			//Playlist not saved yet
			return;
		}
		insertSong(song);
		Cursor cur = db.rawQuery("SELECT MAX(pls.trackNum) AS 'max' FROM playlistSongsTbl pls" +
				" WHERE pls.playlist_id = " + playlist_id, null);
		cur.moveToFirst();
		int results = cur.getCount();
		if(results == 0){
			cur.close();
			return;
		}
		int position = cur.getInt(cur.getColumnIndex("max"));
		cur.close();
		insertSongIntoPlaylist(playlist_id,song,position);
	}
	
	//Returns cursor of all playlists (_id,playlistName)
	public Cursor getAllPlaylists(){
		try{
			return db.rawQuery("SELECT * FROM playlistTbl " +
				"ORDER BY _id", null);
		} catch (SQLiteException e){
			createPlaylistTbl();
			return getAllPlaylists();
		}
	}
	
	private void createPlaylistTbl(){
		db.execSQL("DROP TABLE IF EXISTS playlistTbl");
		db.execSQL("INSERT INTO playlistTbl(_id,playlistName) " +
			"SELECT 1,'Now Playing'");
		db.execSQL("DROP TABLE IF EXISTS playlistSongsTbl");
		db.execSQL("CREATE TABLE playlistSongsTbl (_id INTEGER PRIMARY KEY, " +
				"playlist_id INTEGER, song_id INTEGER, trackNum INTEGER");
	}
	
	//Deletes specific song from playlist
	public void deleteSongFromPlaylist(long key){
		db.execSQL("DELETE FROM playlistSongsTbl WHERE _id = '" + key + "'");
	}
	
	//Empties all songs in playlist
	public void clearPlaylist(long key){
		db.execSQL("DELETE FROM playlistSongsTbl WHERE playlist_id = '" + key + "'");
	}
	
	public void deletePlaylist(long key){
		clearPlaylist(key);
		db.execSQL("DELETE FROM playlistTbl WHERE _id = " + key);
	}
	
	//Returns cursor of songs associated with playlist
	public Cursor getSongsFromPlaylist(long key){
		return db.rawQuery("SELECT pls._id,pls.song_id,song.fileName,song.songTitle,song.isDownloaded," 
				+ "show.showIdent,show.showArtist + ' Live at ' + show.showTitle AS 'showTitle' "
				+ "FROM playlistSongsTbl pls "
				+ "INNER JOIN songTbl song ON song._id = pls.song_id "
				+ "INNER JOIN showTbl show ON show._id = song.show_id "
				+ "WHERE pls.playlist_id = '" + key + "' " +
						"ORDER BY pls.trackNum", null);
	}
	
	public void renamePlaylist(long key, String name){
		db.execSQL("UPDATE playlistTbl SET playlistName = '" 
				+ sanitize(name) + "' WHERE _id = '" + key + "'");
	}
	
	//Returns playlist object 
	//If the listview binds to the returned object, then we must
	//somehow explicitly update the db if we change the object.
	//This behavior may be changed in 2.1
	public ArchivePlaylistObj getPlaylist(long key){
		Cursor cur = db.rawQuery("SELECT playlistName FROM playlistTbl WHERE _id = " + key,null);
		cur.moveToFirst();
		String name = cur.getString(cur.getColumnIndex("playlistName"));
		cur.close();
		cur = getSongsFromPlaylist(key);
		ArrayList<ArchiveSongObj> songs = new ArrayList<ArchiveSongObj>();
		for(int i = 0; i < cur.getCount(); i++){
			cur.moveToPosition(i);
			songs.add(getSong(cur.getInt(cur.getColumnIndex("song_id"))));
		}
		cur.close();
		return new ArchivePlaylistObj(name,key,songs);
	}
	
	public int updatePlaylist(ArchivePlaylistObj playlist){
		if(playlist.getKey() > 0){
			clearPlaylist(playlist.getKey());
			for(int i = 0; i < playlist.size(); i++){
				insertKnownSongIntoPlaylist((int)playlist.getKey(),playlist.getSong(i),i);
			}
		}
		return (int)playlist.getKey();
	}
	
	//End of Playlist methods
	
	public Cursor getAutoCompleteCursor(String searchString){
		return db.rawQuery("SELECT * FROM autoCompleteTbl "
				+ "WHERE searchText LIKE '" + searchString + "%'", null);
	}
	
	public void addArtist(String artist, String numShows){
		db.execSQL("INSERT INTO artistTbl(artistName,numShows) "
				+ "SELECT '" + artist + "','" + numShows + "' "
				+ "WHERE NOT EXISTS (SELECT 1 FROM artistTbl "
				+ "WHERE artistName LIKE '" + artist + "')");
		/*db.execSQL("UPDATE artistTbl SET numShows = '" 
				+ numShows + "' WHERE artistName LIKE '" + artist + "'");*/
	}
	
	public Cursor getArtists(){
		return db.rawQuery("SELECT * FROM artistTbl ORDER BY artistName",null);
	}
	
	public Cursor getArtist(String firstLetter){
		//Apparently you have to manually implement regular expressions, so fuck that
		//Ugly hack that works
		if(firstLetter.equalsIgnoreCase("!")){
			return db.rawQuery("SELECT * FROM artistTbl WHERE artistName NOT LIKE 'a%' AND artistName NOT LIKE 'b%' " +
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
			return db.rawQuery("SELECT * FROM artistTbl "
					+ "WHERE artistName like '" + firstLetter + "%' ORDER BY artistName",null);
		}
	}
	
	public void insertArtistBulk(ArrayList<ArrayList<String>> artists){
		//db.execSQL("PRAGMA synchronous=OFF");
		/*InsertHelper ih = new InsertHelper(db,"artistTbl");
		ih.prepareForInsert();*/
		
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
			Log.e(LOG_TAG,e.toString());
		} finally{
			db.endTransaction();
		}
		
		//ih.execute();
	}
	
	public void clearArtists(){
		db.execSQL("UPDATE prefsTbl SET prefValue = '2010-01-01' WHERE prefName LIKE 'artistUpdate'");
	}

	/*
	 * public void deleteSong(String fileName, String show_ident){
	 * 
	 * db.delete(SONG_TBL, SONG_SHOW_KEY + "='" + show_ident + "' AND " +
	 * SONG_FILENAME + "='" + fileName + "'", null); }
	 */
	
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

	public Cursor getFavoriteShows() {
		
		/*
		 * return db.query(RECENT_TBL, new String[] { SHOW_KEY, SHOW_IDENT,
		 * SHOW_TITLE, SHOW_HASVBR, SHOW_HASLBR }, null, null, null, null,
		 * null);
		 */
		return db.rawQuery("SELECT * FROM " + FAVORITE_SHOW_VW, null);
	}

	public void deleteFavoriteShow(long show_id) {
		
		db.execSQL("DELETE FROM favoriteShowsTbl WHERE show_id=" + show_id);
	}

	public void clearFavoriteShows() {
		
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
		case 7:
			oldDB.execSQL("CREATE TABLE artistTbl (_id INTEGER PRIMARY KEY, artistName TEXT, numShows TEXT)");
			oldDB.execSQL("INSERT INTO prefsTbl (prefName, prefValue) "
					+ "SELECT 'artistUpdate','2010-01-01'");
		case 8:
			oldDB.execSQL("ALTER TABLE playlistSongsTbl ADD COLUMN trackNum INTEGER");
			oldDB.execSQL("INSERT INTO playlistTbl(_id,playlistName) " +
				"SELECT 1,'Now Playing'");
		case 9:
			oldDB.execSQL("CREATE TABLE favoriteShowsTbl (_id INTEGER PRIMARY KEY, show_id INTEGER)");
			oldDB.execSQL("CREATE VIEW favoriteShowsVw AS "
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
		}
		return true;
	}

}
