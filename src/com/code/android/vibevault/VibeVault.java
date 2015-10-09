/*
 * ArchiveApp.java
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

import java.util.ArrayList;

import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;

import com.code.android.vibevault.BrowseArtistsScreen.ParseArtistsPageTask;
import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.Facebook;

import android.app.Application;

public class VibeVault extends Application {

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
	public static final int EMAIL_LINK = 12;
	public static final int SHOW_INFO = 13;
	public static final int UPGRADING_DB = 14;
	public static final int RETRIEVING_DIALOG_ID = 15;
	public static final int DELETE_SONG = 16;
	public static final int DELETE_SHOW = 17;
	public static final int FETCHING_ARTISTS_ID = 18;
	public static final int REMOVE_FROM_FAVORITE_LIST = 19;
	public static final int ADD_TO_FAVORITE_LIST = 20;
	public static final int RETRIEVING_VOTED_DIALOG_ID = 21;
	
	/* Default number of objects to return in a JSON query for a show. */
	public static final int DEFAULT_SHOW_SEARCH_NUM = 10;

	public static final String APP_DIRECTORY = "/archiveApp/";
	//public static final String DOWNLOAD_PATH = "/archiveApp/";
	
	public static String artistSearchText;
	public static int dateSearchModifierPos;
	public static int yearSearchInt = -1;
	public static ArrayList<ArchiveShowObj> searchResults;
	public static ArchivePlaylistObj playList;
	public static ArrayList<ArchiveSongObj> downloadSongs;
	public static int nowPlayingPosition = 0;
	public static int nowDownloadingPosition = 0;
	public static final String sortChoices[] = {"Date", "Rating"};
	public static final String searchChoices[] = {"Show/Artist Description", "Artist"};
	public static final String showResultTypes[] = 
	{"Top All Time Shows","Top Daily Shows","Top Weekly Shows","Newly Added Shows"};
	public static final String artistResultTypes[] = 
	{"Top All Time Artists","Top Daily Artists","Top Weekly Artists","Newly Added Artists"};
	public static final String showsByArtistResultTypes[] = 
	{"Top All Time Shows","Top Daily Shows","Top Weekly Shows","Newly Added Shows"};
	public static String searchPref = "Artist";
	public static ArrayList<ArchiveShowObj> featuredShows;
	public static ArrayList<String> moreFeaturedShows;
	public static ArrayList<ArchivePlaylistObj> savedPlayLists;
	//public static ArrayList<ArchiveShowObj> downloadedShows;
	public static Facebook mFacebook;
	public static AsyncFacebookRunner mAsyncRunner;
	public static String OAuth_Token_Secret = null;
	public static String OAuth_Token = null;
	public static OAuthProvider provider;
	public static CommonsHttpOAuthConsumer consumer;


	public static ParseArtistsPageTask workerTask;

	
	public static DataStore db;

	public void onCreate() {
		
		super.onCreate();
		artistSearchText = "";
		dateSearchModifierPos = 2;
		yearSearchInt = -1;
		searchResults = new ArrayList<ArchiveShowObj>();
		playList = new ArchivePlaylistObj();
		downloadSongs = new ArrayList<ArchiveSongObj>();
		featuredShows = new ArrayList<ArchiveShowObj>();
		moreFeaturedShows = new ArrayList<String>();
		db = new DataStore(this);
		db.initialize();
	}
}
