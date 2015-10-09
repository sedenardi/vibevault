/*
 * NowPlayingScreen.java
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

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import com.code.android.vibevault.R;

public class NowPlayingScreen extends Activity
{
	private PlayerService pService = null;
	
	protected TextView songLabel;
	protected TextView showLabel;
	protected Button previous;
	protected Button stop;
	protected Button pause;
	protected Button next;
	
	protected ListView songsListView;
	protected TextView playListLabel;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.now_playing);
		
		songLabel = (TextView)findViewById(R.id.SongLabel);
		showLabel = (TextView)findViewById(R.id.ShowLabel);
		previous = (Button) this.findViewById(R.id.PrevButton);
		stop = (Button) this.findViewById(R.id.StopButton);
		pause = (Button) this.findViewById(R.id.PauseButton);
		next = (Button) this.findViewById(R.id.NextButton);

		this.previous.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				pService.playPrev();
			}
		});
		
		this.stop.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				pService.stop();
			}
		});
		
		this.pause.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(pService.isPaused() || pService.isStopped()){
					pService.play();
				}
				else{
					pService.pause();
				}
			}
		});

		this.next.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				pService.playNext();
			}
		});
		
		songsListView = (ListView)findViewById(R.id.PlayListListView);
		songsListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> a, View v, int position, long id){
				pService.playSongFromPlaylist(position);
			}
		});
		songsListView.setOnCreateContextMenuListener(new OnCreateContextMenuListener(){
			@Override
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo){
				menu.add(Menu.NONE, VibeVault.REMOVE_FROM_QUEUE, Menu.NONE, "Remove from playlist");
			}
		});
		
	}
	
	private void refreshTrackList(){
		songsListView.setAdapter(new PlaylistAdapter(this,
				R.layout.playlist_row, pService.getPlayAList()));
		songsListView.setSelection(VibeVault.nowPlayingPosition);
	}
	
	private void refreshButtons() {
		if(pService.isPaused() || pService.isStopped()) {
			pause.setBackgroundResource(R.drawable.mediaplaybutton);
		}
		else {
			pause.setBackgroundResource(R.drawable.mediapausebutton);
		}
	}

	@Override
	public void onResume()
	{
		super.onResume();
		getApplicationContext().bindService(new Intent(NowPlayingScreen.this, PlayerService.class), onPService, BIND_AUTO_CREATE);
		registerReceiver(TitleReceiver, new IntentFilter(VibeVault.BROADCAST_SONG_TITLE));
		registerReceiver(PlaylistReceiver, new IntentFilter(VibeVault.BROADCAST_PLAYLIST));
		registerReceiver(PlayReceiver, new IntentFilter(VibeVault.BROADCAST_PLAYER_STATUS));
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
		getApplicationContext().unbindService(onPService);
		unregisterReceiver(TitleReceiver);
		unregisterReceiver(PlaylistReceiver);
		unregisterReceiver(PlayReceiver);
		songsListView.setAdapter(null);
	}
	
	private void refreshCurrentSong()
	{
		songLabel.setText(pService.getPlayingSongTitle());
		showLabel.setText(pService.getPlayingShowTitle());
	}
	
	private ServiceConnection onPService=new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder rawBinder) {
			pService=((PlayerService.MPlayerBinder)rawBinder).getService();
			refreshTrackList();
			refreshCurrentSong();
			refreshButtons();
		}

		public void onServiceDisconnected(ComponentName className) {
			pService=null;
		}
	};
	
	private BroadcastReceiver TitleReceiver=new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			refreshCurrentSong();
		}
	};
	
	private BroadcastReceiver PlaylistReceiver=new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			refreshTrackList();
		}
	};
	
	private BroadcastReceiver PlayReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			refreshButtons();
			refreshTrackList();
		}
	};
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.help_options, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected (MenuItem item){
		switch (item.getItemId()){
			case R.id.scrollableDialog:
				AlertDialog.Builder ad = new AlertDialog.Builder(this);
				ad.setTitle("Help!");
				View v =LayoutInflater.from(this).inflate(R.layout.scrollable_dialog, null);
				((TextView)v.findViewById(R.id.DialogText)).setText(R.string.now_playing_screen_help);
				ad.setPositiveButton("Cool.", new android.content.DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int arg1) {
					}
				});
				ad.setView(v);
				ad.show();
		}
		return true;
	}
	
	private class PlaylistAdapter extends ArrayAdapter<ArchiveSongObj> {
		
		public PlaylistAdapter(Context context, int textViewResourceId, List<ArchiveSongObj> objects){
			super(context,textViewResourceId, objects);
		}
		
		@Override 
		public View getView(int position, View convertView, ViewGroup parent){
			ArchiveSongObj song = pService.getSong(position);	
			if(convertView==null){
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = vi.inflate(R.layout.playlist_row, null);
			}
			TextView text = (TextView) convertView.findViewById(R.id.text);
			if(song != null){
				text.setText(song.toString());
				if(position == pService.getPlayingIndex()){
					text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
					text.setTextColor(Color.parseColor("#127DD4"));
				}
				else{
					text.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
					text.setTextColor(Color.LTGRAY);
				}
			}
			return convertView;
		}
	}
	
	
	
	public boolean onContextItemSelected(MenuItem item){
		AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo)item.getMenuInfo();
		if(menuInfo!=null){
			switch(item.getItemId()){
			case(VibeVault.REMOVE_FROM_QUEUE):
				VibeVault.playList.removeSongAt(menuInfo.position);
				pService.dequeue(menuInfo.position);
				break;
			default:
				return false;
			}
			return true;
		}
		return false;
	}
}
