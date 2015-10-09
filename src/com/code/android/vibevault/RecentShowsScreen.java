/*
 * RecentShowsScreen.java
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
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

public class RecentShowsScreen extends Activity {

	private ListView showList;
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.recent_shows_screen);
		
		showList = (ListView) findViewById(R.id.RecentShowsListView);
		showList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> a, View v, int position, long id){
				openShow(id);
			}
		});
		showList.setOnCreateContextMenuListener(new OnCreateContextMenuListener(){
			@Override
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo){
				menu.add(Menu.NONE, VibeVault.REMOVE_FROM_RECENT_LIST, Menu.NONE, "Remove Show");
				menu.add(Menu.NONE, VibeVault.EMAIL_LINK, Menu.NONE, "Email Link to Show");
				}
		});
		refreshShowList();
		
	}
	
	private void refreshShowList(){
		showList.setAdapter(new SimpleCursorAdapter(this,R.layout.show_list_row, VibeVault.db.getRecentShows(), 
				new String[]{DataStore.SHOW_TITLE},
				new int[]{R.id.show_title}));
	}
	
	private void openShow(long pos){
		ArchiveShowObj show = VibeVault.db.getShow(pos,DataStore.RECENT_SHOW_TABLE);
		if(show != null){
			Intent i = new Intent(RecentShowsScreen.this, ShowDetailsScreen.class);
			i.putExtra("Show", show);
			startActivity(i);
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item){
		AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo)item.getMenuInfo();
		if(menuInfo!=null){
			switch(item.getItemId()){
			case(VibeVault.REMOVE_FROM_RECENT_LIST):
				VibeVault.db.deleteShow(menuInfo.id,DataStore.RECENT_SHOW_TABLE);
				refreshShowList();
				break;
			case (VibeVault.EMAIL_LINK):
				final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
				emailIntent.setType("plain/text");
				ArchiveShowObj show = VibeVault.db.getShow(menuInfo.id,DataStore.RECENT_SHOW_TABLE);
				emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Great show on archive.org: " + show.getTitle());
				emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Hey,\n\nYou should listen to " + show.getTitle() + ".  You can find it here: " + show.getShowURL());
				startActivity(Intent.createChooser(emailIntent, "Send mail..."));
				break;
			default:
				return false;
			}
		}
		return false;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.help_nowplaying_clearrecent_options, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected (MenuItem item){
		switch (item.getItemId()){
			case R.id.nowPlaying: 	//Open playlist activity
				Intent np = new Intent(RecentShowsScreen.this, NowPlayingScreen.class);
				startActivity(np);
				break;
			case R.id.clearRecentShows:
				VibeVault.db.clearShows(DataStore.RECENT_SHOW_TABLE);
				refreshShowList();
				break;
			case R.id.scrollableDialog:
				AlertDialog.Builder ad = new AlertDialog.Builder(this);
				ad.setTitle("Help!");
				View v =LayoutInflater.from(this).inflate(R.layout.scrollable_dialog, null);
				((TextView)v.findViewById(R.id.DialogText)).setText(R.string.recent_shows_screen_help);
				ad.setPositiveButton("Cool.", new android.content.DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int arg1) {
					}
				});
				ad.setView(v);
				ad.show();
				break;
		}
		return true;
	}
	
	@Override
	public void onPause(){
		super.onPause();
	}
	
	@Override
	public void onResume(){
		super.onResume();
	}
	
}
