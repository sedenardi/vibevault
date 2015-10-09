/*
 * HomeScreen.java
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

import java.io.IOException;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.SQLException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;
import com.code.android.vibevault.R;

public class HomeScreen extends Activity {
	
	private static final String LOG_TAG = HomeScreen.class.getName();

	private ImageButton searchButton;
	private ImageButton recentButton;
	private ImageButton downloadButton;
	private ImageButton playingButton;
//	private Button settingsButton;
	private ImageButton featuredShowsButton;
	private ImageButton browseArtistsButton;
	
	private InitTask workerTask;
	private boolean dialogShown;
	
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.home_screen);
		
		Object retained = getLastNonConfigurationInstance();
		if(retained instanceof InitTask){
			
			
			workerTask = (InitTask)retained;
			workerTask.setActivity(this);
		} else{
			workerTask = new InitTask(this);
		}
		
		
		searchButton = (ImageButton) findViewById(R.id.HomeSearch);
		searchButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v){
				Intent i = new Intent(HomeScreen.this, SearchScreen.class);
				startActivity(i);
			}
		});
		recentButton = (ImageButton) findViewById(R.id.HomeRecent);
		recentButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v){
				Intent i = new Intent(HomeScreen.this, StoredShowTabs.class);
				startActivity(i);
			}
		});
		downloadButton = (ImageButton) findViewById(R.id.HomeDownload);
		downloadButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v){
				Intent i = new Intent(HomeScreen.this, DownloadTabs.class);
				startActivity(i);
			}
		});
		playingButton = (ImageButton) findViewById(R.id.HomePlaying);
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
		featuredShowsButton = (ImageButton) findViewById(R.id.HomeFeatured);
		featuredShowsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v){
				Intent i = new Intent(HomeScreen.this, VoteTabs.class);
				startActivity(i);
			}
		});
		browseArtistsButton = (ImageButton) findViewById(R.id.HomeBrowse);
		browseArtistsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v){
				Intent i = new Intent(HomeScreen.this, BrowseArtistsScreen.class);
				startActivity(i);
			}
		});

		
		if(VibeVault.db.needsUpgrade){
			workerTask = new InitTask(this);
			workerTask.execute();
		} else{
			if(!Boolean.parseBoolean(VibeVault.db.getPref("splashShown"))){
				AlertDialog.Builder ad = new AlertDialog.Builder(this);
				ad.setTitle("Welcome!");
				View v = LayoutInflater.from(this).inflate(R.layout.scrollable_dialog, null);
				((TextView) v.findViewById(R.id.DialogText)).setText(R.string.splash_screen);
				ad.setPositiveButton("Okay.", new android.content.DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int arg1) {
					}
				});
				ad.setView(v);
				ad.show();
				VibeVault.db.updatePref("splashShown", "true");
			}
		}
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
				ad.setPositiveButton("Okay.", new android.content.DialogInterface.OnClickListener() {
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
		startService(new Intent(this, DownloadService.class));
	}
	
	private class InitTask extends AsyncTask<String, Integer, String> {

		private HomeScreen parentScreen;
		private boolean success = false;
		private boolean completed = false;
		
		private InitTask(HomeScreen activity){
			this.parentScreen = activity;
		}
		
		protected void onPreExecute(){
			parentScreen.showDialog(VibeVault.UPGRADING_DB);
		}
		
		@Override
		protected String doInBackground(String... upgradeString) {

			success = VibeVault.db.upgradeDB();
			return "Completed";

		}

		protected void onPostExecute(String upgradeString) {
			if(!success){
				try {
					VibeVault.db.copyDB();
				} catch (IOException e) {
					throw new Error("Error copying database");
				}
			}
			try {
				VibeVault.db.openDataBase();
			} catch (SQLException e) {
				Log.e(LOG_TAG, "Unable to open database");
				Log.e(LOG_TAG, e.getStackTrace().toString());
			}
			completed=true;
			notifyActivityTaskCompleted();
		}
		
		// The parent could be null if you changed orientations
		// and this method was called before the new SearchScreen
		// could set itself as this Thread's parent.
		private void notifyActivityTaskCompleted(){
			if(parentScreen!=null){
				parentScreen.onTaskCompleted();
			}
		}
		
		// When a SearchScreen is reconstructed (like after an orientation change),
		// we call this method on the retained SearchScreen (if one exists) to set
		// its parent Activity as the new SearchScreen because the old one has been destroyed.
		// This prevents leaking any of the data associated with the old SearchScreen.
		private void setActivity(HomeScreen activity){
			this.parentScreen = activity;
			if(completed){
				notifyActivityTaskCompleted();
			}
		}
		
	}
	
	/** Persist worker Thread across orientation changes.
	*
	* Includes Thread bookkeeping to prevent not leaking Views on orientation changes.
	*/
	@Override
	public Object onRetainNonConfigurationInstance(){
		workerTask.setActivity(null);
		return workerTask;
	}
	
	/** Dialog preparation method.
	*
	* Includes Thread bookkeeping to prevent not leaking Views on orientation changes.
	*/
	@Override
	protected void onPrepareDialog(int id, Dialog dialog){
		super.onPrepareDialog(id, dialog);
		if(id==VibeVault.UPGRADING_DB){
			dialogShown = true;
		}
	}
	
	/** Dialog creation method.
	*
	* Includes Thread bookkeeping to prevent not leaking Views on orientation changes.
	*/
	@Override
	protected Dialog onCreateDialog(int id){
		switch(id){
			case VibeVault.UPGRADING_DB:
				
				ProgressDialog dialog = new ProgressDialog(this);
				dialog.setMessage("Upgrading Database");
				return dialog;
			default:
				return super.onCreateDialog(id);	
		}
	}
	
	private void onTaskCompleted(){
		if(dialogShown){
			try{
				dismissDialog(VibeVault.UPGRADING_DB);
			} catch(IllegalArgumentException e){
				
				e.printStackTrace();
			}
			
		}
		if(!Boolean.parseBoolean(VibeVault.db.getPref("splashShown"))){
			AlertDialog.Builder ad = new AlertDialog.Builder(this);
			ad.setTitle("Welcome!");
			View v = LayoutInflater.from(this).inflate(R.layout.scrollable_dialog, null);
			((TextView) v.findViewById(R.id.DialogText)).setText(R.string.splash_screen);
			ad.setPositiveButton("Okay.", new android.content.DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int arg1) {
				}
			});
			ad.setView(v);
			ad.show();
			VibeVault.db.updatePref("splashShown", "true");
		}
	}
}