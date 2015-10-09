/*
 * HomeScreen.java
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import com.code.android.vibevault.R;

public class HomeScreen extends Activity {

	private Button searchButton;
	private Button recentButton;
	private Button downloadButton;
	private Button playingButton;
//	private Button settingsButton;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.home_screen);
		
		searchButton = (Button) findViewById(R.id.HomeSearch);
		searchButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v){
				Intent i = new Intent(HomeScreen.this, SearchScreen.class);
				startActivity(i);
			}
		});
		recentButton = (Button) findViewById(R.id.HomeRecent);
		recentButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v){
				Intent i = new Intent(HomeScreen.this, RecentShowsScreen.class);
				startActivity(i);
			}
		});
		downloadButton = (Button) findViewById(R.id.HomeDownload);
		downloadButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v){
				Intent i = new Intent(HomeScreen.this, DownloadTabs.class);
				startActivity(i);
			}
		});
		playingButton = (Button) findViewById(R.id.HomePlaying);
		playingButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v){
				Intent i = new Intent(HomeScreen.this, NowPlayingScreen.class);
				startActivity(i);
			}
		});
//		settingsButton = (Button) findViewById(R.id.HomeSettings);
//		settingsButton.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v){
//				//Intent i = new Intent(HomeScreen.this, SettingsScreen.class);
//				//startActivity(i);
//			}
//		});
	}
	
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
				((TextView)v.findViewById(R.id.DialogText)).setText(R.string.home_screen_help);
				ad.setPositiveButton("Cool.", new android.content.DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int arg1) {
					}
				});
				ad.setView(v);
				ad.show();
		}
		return true;
	}
	
	@Override
	public void onResume(){
		super.onResume();
		startService(new Intent(this, PlayerService.class));
		startService(new Intent(this, DownloadService.class));
	}
	
}