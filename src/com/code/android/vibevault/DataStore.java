/*
 * DataStore.java
 * VERSION 1.1
 * 
 * Copyright 2010 Andrew Pearson and Sanders DeNardi.
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
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

public class DataStore extends SQLiteOpenHelper{
		
	private static final String DB_NAME = "archivedb";
	private static String DB_PATH;
	private static final int DB_VERSION = 5;
	
	public static final String PREF_TABLE = "prefsTbl";
	public static final String PREF_NAME = "prefName";
	public static final String PREF_VALUE = "prefValue";
	
	public static final String RECENT_SHOW_TABLE = "recentShowsTbl";
	public static final String DOWNLOADED_SHOW_TABLE = "downloadedShowsTbl";
	public static final String SHOW_INDEX = "_id";
	public static final String SHOW_IDENT = "showIdent";
	public static final String SHOW_TITLE = "showTitle";
	public static final String SHOW_HASVBR = "hasVBR";
	public static final String SHOW_HASLBR = "hasLBR";
	
	public static final String DOWNLOADED_SONG_TABLE = "downloadedSongsTbl";
	public static final String SONG_INDEX = "_id";
	public static final String SONG_FILENAME = "fileName";
	public static final String SONG_TITLE = "songTitle";
	public static final String SONG_SHOW_IDENT = "showIdent";
	public static final String SONG_SHOW_TITLE = "showTitle";
	
	private final Context context;
	
	private SQLiteDatabase db;
	
	public DataStore (Context context){
			super(context, DB_NAME, null, DB_VERSION);
			this.context = context;
			DB_PATH = context.getDatabasePath(DB_NAME).toString();
	}
		
	@Override 
	public void onCreate(SQLiteDatabase db){
		
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
		
	}
	
	public void createDB() throws IOException{
		boolean dbExists = checkDB();
		if(dbExists){
		}
		else{
			this.getReadableDatabase();
			try{
				copyDB();
			}
			catch (IOException e){
				throw new Error("Error copying database");
			}
		}
	}
	
	public boolean checkDB(){
		SQLiteDatabase checkDB = null;
		try{
			checkDB = SQLiteDatabase.openDatabase(DB_PATH, null, SQLiteDatabase.OPEN_READWRITE);
		}
		catch (SQLiteException e){
		}
		
		if(checkDB != null){
			if(checkDB.getVersion() != DB_VERSION){
				checkDB.execSQL("DROP TABLE IF EXISTS " + PREF_TABLE);
				checkDB.execSQL("DROP TABLE IF EXISTS " + "showsTbl");
				checkDB.close();
				return false;
			}
			else{
				checkDB.close();
				return true;
			}
		}
		else{
			return false;
		}
	}
	
	private void copyDB() throws IOException{
		InputStream is = context.getAssets().open(DB_NAME);
		OutputStream os = new FileOutputStream(DB_PATH);
		
		byte[] buffer = new byte[1024];
		int length;
		while((length = is.read(buffer)) > 0){
			os.write(buffer,0,length);
		}
		
		os.flush();
		os.close();
		is.close();
	}
	
	public void openDataBase() throws SQLiteException{
		db = SQLiteDatabase.openDatabase(DB_PATH, null, SQLiteDatabase.OPEN_READWRITE);
		db.setVersion(DB_VERSION);
	}
	
	public Cursor getPrefCursor(String pref_name){
		Cursor cur = db.query(true, PREF_TABLE, new String[] {
				PREF_NAME, PREF_VALUE},
				PREF_NAME + "=" + pref_name,
				null,null,null,null,null);
		if(cur != null){
			cur.moveToFirst();
		}
		return cur;
	}
	
	public void updatePref(String pref_name, String pref_value){
		ContentValues value = new ContentValues();
		value.put(PREF_VALUE, pref_value);
		db.update(PREF_TABLE, value, PREF_NAME + "=" + pref_name, null);
	}
	
	public void insertShow(ArchiveShowObj show, String table){
		boolean dupe = showExists(show.getIdentifier(), table);
		if(!dupe){
			ContentValues value = new ContentValues();
			value.put(SHOW_IDENT, show.getIdentifier());
			value.put(SHOW_TITLE, show.getTitle());
			value.put(SHOW_HASVBR, show.hasVBR());
			value.put(SHOW_HASLBR, show.hasLBR());
			long row = db.insert(table, null, value);
		}
		else{
		}
	}
	
	public boolean showExists(String show_ident, String table){
		Cursor cur = db.query(table, new String[]{SHOW_IDENT}, 
		SHOW_IDENT + "='" + show_ident + "'", null, null, null, null);
		int results = cur.getCount();
		cur.close();
		return results > 0;
	}
	
	public Cursor getRecentShows(){
		return db.query(RECENT_SHOW_TABLE, new String[] {
				SHOW_INDEX, SHOW_IDENT, SHOW_TITLE, SHOW_HASVBR, SHOW_HASLBR
		}, null, null, null, null, null);
	}
	
	public Cursor getDownloadShows(){
		return db.query(DOWNLOADED_SHOW_TABLE, new String[] {
				SHOW_INDEX, SHOW_IDENT, SHOW_TITLE, SHOW_HASVBR, SHOW_HASLBR
		}, null, null, null, null, null);
	}
	
	public ArchiveShowObj getShow(long show_id, String table){
		Cursor cur = db.query(true, table, new String[] {
				SHOW_IDENT, SHOW_TITLE, SHOW_HASVBR, SHOW_HASLBR
				}, SHOW_INDEX + "=" + show_id, null, null, null, null, null);
		if(cur != null){
			cur.moveToFirst();
		}
		ArchiveShowObj show = new ArchiveShowObj(
				cur.getString(cur.getColumnIndex(DataStore.SHOW_IDENT)),
				cur.getString(cur.getColumnIndex(DataStore.SHOW_TITLE)),
				cur.getString(cur.getColumnIndex(DataStore.SHOW_HASVBR)),
				cur.getString(cur.getColumnIndex(DataStore.SHOW_HASLBR)));
		cur.close();
		return show;
	}
	
	public void deleteShow(long show_id, String table){
		db.delete(table, SHOW_INDEX + "=" + show_id, null);
	}
	
	public void clearShows(String table){
		db.delete(table, null, null);
	}
	
	public void insertSong(ArchiveSongObj song){
		boolean dupe = songExists(song.getFileName());
		if(!dupe){
			ContentValues value = new ContentValues();
			value.put(SONG_FILENAME, song.getFileName());
			value.put(SONG_TITLE, song.toString());
			value.put(SONG_SHOW_IDENT, song.getShowIdentifier());
			value.put(SONG_SHOW_TITLE, song.getShowTitle());
			long row = db.insert(DOWNLOADED_SONG_TABLE, null, value);
		}
		else{
		}
	}
	
	public boolean songExists(String song_filename){
		Cursor cur = db.query(DOWNLOADED_SONG_TABLE, new String[]{SONG_FILENAME}, 
				SONG_FILENAME + "='" + song_filename + "'", null, null, null, null);
		int results = cur.getCount();
		cur.close();
		return results > 0;
	}
	
	public Cursor getSongsFromShow(String show_ident){
		return db.query(DOWNLOADED_SONG_TABLE, new String[] {
				SONG_INDEX, SONG_FILENAME, SONG_TITLE, SONG_SHOW_IDENT, SONG_SHOW_TITLE
				}, SONG_SHOW_IDENT + "='" + show_ident + "'", null, null, null, SONG_FILENAME);
	}
	
	public ArchiveSongObj getSongAtIndex(String show_ident, int position){
		Cursor cur = db.query(DOWNLOADED_SONG_TABLE, new String[] {
				SONG_FILENAME, SONG_TITLE, SONG_SHOW_IDENT, SONG_SHOW_TITLE
				}, SONG_SHOW_IDENT + "='" + show_ident + "'", null, null, null, SONG_FILENAME);
		cur.moveToPosition(position);
		ArchiveSongObj song = new ArchiveSongObj(
				cur.getString(cur.getColumnIndex(DataStore.SONG_TITLE)),
				cur.getString(cur.getColumnIndex(DataStore.SONG_FILENAME)),
				cur.getString(cur.getColumnIndex(DataStore.SONG_SHOW_TITLE)),
				cur.getString(cur.getColumnIndex(DataStore.SONG_SHOW_IDENT)));
		cur.close();
		return song;
	}
	
	public ArchiveSongObj getSong(long song_id){
		Cursor cur = db.query(true, DOWNLOADED_SONG_TABLE, new String[] {
				SONG_FILENAME, SONG_TITLE, SONG_SHOW_IDENT, SONG_SHOW_TITLE
				}, SONG_INDEX + "=" + song_id, null, null, null, null, null);
		if(cur != null){
			cur.moveToFirst();
		}
		ArchiveSongObj song = new ArchiveSongObj(
				cur.getString(cur.getColumnIndex(DataStore.SONG_TITLE)),
				cur.getString(cur.getColumnIndex(DataStore.SONG_FILENAME)),
				cur.getString(cur.getColumnIndex(DataStore.SONG_SHOW_TITLE)),
				cur.getString(cur.getColumnIndex(DataStore.SONG_SHOW_IDENT)));
		cur.close();
		return song;
	}
	
	public void deleteSong(String fileName, String show_ident){
		db.delete(DataStore.DOWNLOADED_SONG_TABLE, SONG_SHOW_IDENT + "='" + show_ident + 
				"' AND " + SONG_FILENAME + "='" + fileName + "'", null);
	}
	
}
