/*
 * ADownloadedShowScreen.java
 * VERSION 2.0
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

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import com.code.android.vibevault.R;

public class DownloadedShowScreen extends Activity {

	private static final String LOG_TAG = DownloadedShowScreen.class.getName();
	
	private PlaybackService pService = null;
	private ArchiveShowObj show;
	private TextView showLabel;
	private ListView trackList;
	private String showTitle;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.downloaded_show_screen);
		Bundle b = getIntent().getExtras();
		show = (ArchiveShowObj)b.get("Show");
		
		showTitle = show.getArtistAndTitle();
		showLabel = (TextView) findViewById(R.id.ShowLabel);
		showLabel.setText(showTitle);

		trackList = (ListView) findViewById(R.id.SongsListView);
		trackList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> a, View v, int position, long id){
				
				playShow(id);
			}
		});
		trackList.setOnCreateContextMenuListener(new OnCreateContextMenuListener(){
			@Override
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo){
				menu.add(Menu.NONE, VibeVault.ADD_SONG_TO_QUEUE, Menu.NONE, "Add to playlist");
				menu.add(Menu.NONE, VibeVault.DELETE_SONG, Menu.NONE, "Delete Song");
			}
		});
	}
	
	public void playShow(long songID){
		// Set playlist key to Now Playing key and remove all songs from the list.
		VibeVault.playList.setKey(1);
		VibeVault.playList.clear();
		// Get a Cursor for the songs in the show, iterate through it, and add
		// the songs to the playlist.
		Cursor cur = VibeVault.db.getSongsFromShow(show.getIdentifier());
		cur.moveToFirst();
		while (!cur.isAfterLast()) {
			ArchiveSongObj song = new ArchiveSongObj(cur.getString(cur.getColumnIndex(DataStore.SONG_TITLE)), 
					cur.getString(cur.getColumnIndex(DataStore.SONG_FILENAME)), 
					show.getArtistAndTitle(), 
					show.getIdentifier(), 
					Boolean.valueOf(cur.getString(cur.getColumnIndex(DataStore.SONG_DOWNLOADED))));
			Log.d(LOG_TAG,
					"Returning Song: " + song.toString() + "-"
							+ song.getFileName() + "-" + song.getShowTitle());
			pService.enqueue(song);
			cur.moveToNext();
		}
		cur.close();
		// Set the index of the currently playing song, play it, and go to the NowPlayingScreen.
		int index = pService.enqueue(VibeVault.db.getSong(songID));
		pService.playSongFromPlaylist(index);
		Intent i = new Intent(DownloadedShowScreen.this,NowPlayingScreen.class);
		startActivity(i);
	}
	
	/** Handle user's long-click selection.
	*
	*/
	@Override
	public boolean onContextItemSelected(MenuItem item){
		AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo)item.getMenuInfo();
		if(menuInfo!=null){
			ArchiveSongObj selSong = VibeVault.db.getSong(menuInfo.id);
			switch(item.getItemId()){
			case (VibeVault.ADD_SONG_TO_QUEUE):
				pService.enqueue(selSong);
				break;
			case (VibeVault.DELETE_SONG):
				DeletionTask task = new DeletionTask();
				task.execute(selSong);
				break;
			default:
				return false;
			}
			return true;
		}
		return false;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.help_recentshows_nowplaying_deleteshow_options, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected (MenuItem item){
		switch (item.getItemId()){
			case R.id.nowPlaying: 	//Open playlist activity
				Intent i = new Intent(DownloadedShowScreen.this, NowPlayingScreen.class);
				
				startActivity(i);
				break;
			case R.id.recentShows:
				Intent rs = new Intent(DownloadedShowScreen.this, RecentShowsScreen.class);
				
				startActivity(rs);
				break;
			case R.id.deleteShow:
				Cursor c = VibeVault.db.getSongsFromShow(show.getIdentifier());
				int numSongs = c.getCount();
				ArrayList<ArchiveSongObj> songs = new ArrayList<ArchiveSongObj>();
				for(int j = 0; j < numSongs; j++){
					c.moveToPosition(j);
					ArchiveSongObj songObjToDel = VibeVault.db.getSong(
							c.getInt(c.getColumnIndex("_id")));
					songs.add(songObjToDel);
					}
				c.close();
				DeletionTask task = new DeletionTask();
				task.execute(songs.toArray(new ArchiveSongObj[0]));
				break;
			case R.id.scrollableDialog:
				AlertDialog.Builder ad = new AlertDialog.Builder(this);
				ad.setTitle("Help!");
				View v =LayoutInflater.from(this).inflate(R.layout.scrollable_dialog, null);
				((TextView)v.findViewById(R.id.DialogText)).setText(R.string.downloaded_show_screen_help);
				ad.setPositiveButton("Okay.", new android.content.DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int arg1) {
					}
				});
				ad.setView(v);
				ad.show();
				break;
			default:
				break;
		}
		return true;
	}
	
	@Override
	public void onResume(){
		super.onResume();
		attachToPlaybackService();
		refreshTrackList();
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
		detachFromPlaybackService();
	}
	
	private void attachToPlaybackService() {
		
		Intent serviceIntent = new Intent(this, PlaybackService.class);

		// Explicitly start the service. Don't use BIND_AUTO_CREATE, since it
		// causes an implicit service stop when the last binder is removed.
		getApplicationContext().startService(serviceIntent);
		getApplicationContext().bindService(serviceIntent, conn, 0);
	}

	private void detachFromPlaybackService() {
		getApplicationContext().unbindService(conn);
	}
	
	private void refreshTrackList(){
		Cursor listCur = VibeVault.db.getSongsFromShow(show.getIdentifier());
		this.startManagingCursor(listCur);
		trackList.setAdapter(new SimpleCursorAdapter(this,R.layout.downloaded_show_screen_row, 
				listCur, 
				new String[]{DataStore.SONG_TITLE},
				new int[]{R.id.text}));
	}
	
	
	public class DeletionTask extends AsyncTask<ArchiveSongObj, Boolean, Integer> {

		@Override
		protected Integer doInBackground(ArchiveSongObj... songs) {
			int numDeletedSongs = 0;
			for(ArchiveSongObj song : songs){
				pService.updatePlaying();
				ArchiveSongObj curSong = pService.getPlayingSong();
				if(curSong!=null&&curSong.equals(song)){
					publishProgress(false);
					continue;
				}
				//VibeVault.playList.removeSong(song);
				File songToDelete = new File(song.getFilePath());
				boolean deleted = songToDelete.delete();
				if(deleted){
					VibeVault.db.setSongDeleted(song);
					numDeletedSongs++;
					publishProgress(true);
				}
			}
			return numDeletedSongs;
		}
		
		@Override
		protected void onProgressUpdate(Boolean... b){
			if(b[0]==false){
				Toast.makeText(getBaseContext(), "You can't delete a playing song.", Toast.LENGTH_SHORT).show();
			}
			refreshTrackList();
		}
		
		
		protected void onPostExecute(Integer i){
			Toast.makeText(getBaseContext(), "Deleted " + i + " song(s)...", Toast.LENGTH_SHORT).show();
			refreshTrackList();
		}
		
		
	}

	private ServiceConnection conn = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			pService = ((PlaybackService.ListenBinder) service).getService();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.w(LOG_TAG, "DISCONNECT");
			pService = null;
		}
	};
	
}
