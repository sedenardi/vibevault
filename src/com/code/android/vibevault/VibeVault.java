/*
 * ArchiveApp.java
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

import java.io.IOException;
import java.util.ArrayList;

import android.app.Application;
import android.database.SQLException;
import android.util.Log;

public class VibeVault extends Application {

	public static final String HOME_SCREEN_TAG = "HomeScreen";
	public static final String SHOW_DETAILS_TAG = "ShowDetailsScreen";
	public static final String SEARCH_SCREEN_TAG = "SearchScreen";
	public static final String NOW_PLAYING_TAG = "NowPlaying";
	public static final String RECENT_SHOWS_TAG = "RecentShows";
	public static final String PLAYER_SERVICE_TAG = "PlayerService";
	public static final String DOWNLOAD_SERVICE_TAG = "DownloadService";
	public static final String DATA_STORE_TAG = "DataStore";
	public static final String DOWNLOAD_THREAD_TAG = "DownloadSongThread";
	public static final String ACTIVE_DOWNLOAD_TAG = "ActiveDownloadScreen";
	public static final String SHOWS_DOWNLOADED_TAG = "ShowsDownloadedScreen";
	public static final String DOWNLOADED_SHOW_TAG = "DownloadedShowScreen";

	public static final int PLAYER_NOTIFICATION = 300;
	public static final int DOWNLOAD_NOTIFICATION = 301;

	public static final String BROADCAST_SONG_TITLE = "com.code.android.archive.SONGTITLE";
	public static final String BROADCAST_PLAYER_STATUS = "com.code.android.archive.PLAYERSTATUS";
	public static final String BROADCAST_PLAYLIST = "com.code.android.archive.PLAYLIST";
	public static final String BROADCAST_DOWNLOAD_STATUS = "com.code.android.archive.DOWNLOADSTATUS";

	public static final int LOADING_DIALOG_ID = 0;
	public static final int STREAM_CONTEXT_ID = 1;
	public static final int DOWNLOAD_SONG = 2;
	public static final int DOWNLOAD_SHOW = 3;
	public static final int ADD_SONG_TO_QUEUE = 4;
	public static final int REMOVE_FROM_QUEUE = 5;
	public static final int REMOVE_FROM_RECENT_LIST = 6;
	public static final int SEARCHING_DIALOG_ID = 7;
	public static final int OPEN_DOWNLOAD_SHOW = 8;
	public static final int PAUSE_DOWNLOAD = 9;
	public static final int RESUME_DOWNLOAD = 10;
	public static final int CANCEL_DOWNLOAD = 11;
	protected static final int EMAIL_LINK = 12;
	
	/* Default number of objects to return in a JSON query for a show. */
	public static final int DEFAULT_SHOW_SEARCH_NUM = 10;

	public static final String APP_DIRECTORY = "/archiveApp/";
	//public static final String DOWNLOAD_PATH = "/archiveApp/";
	
	public static String searchText;
	public static ArrayList<ArchiveShowObj> searchResults;
	public static ArchivePlaylistObj playList;
	public static ArrayList<ArchiveSongObj> downloadSongs;
	public static int nowPlayingPosition = 0;
	public static int nowDownloadingPosition = 0;
	//public static ArrayList<ArchiveShowObj> downloadedShows;

	public static DataStore db;

	public void onCreate() {
		super.onCreate();
		searchText = "";
		searchResults = new ArrayList<ArchiveShowObj>();
		playList = new ArchivePlaylistObj();
		downloadSongs = new ArrayList<ArchiveSongObj>();
		db = new DataStore(this);
		try {
			db.createDB();
		} catch (IOException e) {
			Log.e("ArchiveApp", "Unable to create database");
			Log.e("ArchiveApp", e.getStackTrace().toString());
		}
		try {
			db.openDataBase();
		} catch (SQLException e) {
			Log.e("ArchiveApp", "Unable to open database");
			Log.e("ArchiveApp", e.getStackTrace().toString());
		}
	}
	
}
