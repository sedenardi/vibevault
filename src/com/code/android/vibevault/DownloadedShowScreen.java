/*
 * ADownloadedShowScreen.java
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
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
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import com.code.android.vibevault.R;

public class DownloadedShowScreen extends Activity {

	private PlayerService pService = null;
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

				
				Cursor cur = VibeVault.db.getSongsFromShow(show.getIdentifier());
				cur.moveToFirst();
				while (!cur.isAfterLast()) {
					ArchiveSongObj song = new ArchiveSongObj(cur.getString(cur.getColumnIndex(DataStore.SONG_TITLE)), 
							cur.getString(cur.getColumnIndex(DataStore.SONG_FILENAME)), 
							show.getArtistAndTitle(), 
							show.getIdentifier(), 
							Boolean.valueOf(cur.getString(cur.getColumnIndex(DataStore.SONG_DOWNLOADED))));
					pService.enqueue(song);
					cur.moveToNext();
				}
				cur.close();
				
				int index = pService.enqueue(VibeVault.db.getSong(id));
				pService.playSongFromPlaylist(index);
			}
		});
		trackList.setOnCreateContextMenuListener(new OnCreateContextMenuListener(){
			@Override
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo){
				menu.add(Menu.NONE, VibeVault.ADD_SONG_TO_QUEUE, Menu.NONE, "Add to playlist");
			}
		});
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
		inflater.inflate(R.menu.help_recentshows_nowplaying_options, menu);
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
		getApplicationContext().bindService(new Intent(this, PlayerService.class), onPService, BIND_AUTO_CREATE);
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
		getApplicationContext().unbindService(onPService);
	}
	
	private void refreshTrackList(){
		trackList.setAdapter(new SimpleCursorAdapter(this,R.layout.downloaded_show_screen_row, 
				VibeVault.db.getSongsFromShow(show.getIdentifier()), 
				new String[]{DataStore.SONG_TITLE},
				new int[]{R.id.text}));
	}

	private ServiceConnection onPService=new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder rawBinder) {
			pService=((PlayerService.MPlayerBinder)rawBinder).getService();
			refreshTrackList();
		}

		public void onServiceDisconnected(ComponentName className) {
			pService=null;
		}
	};
	
}
