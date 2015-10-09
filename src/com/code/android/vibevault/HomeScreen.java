/*
 * HomeScreen.java
 * VERSION 3.1
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.SQLException;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.net.Uri;
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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.code.android.vibevault.R;

public class HomeScreen extends Activity {
	
	private static final String LOG_TAG = HomeScreen.class.getName();
	
	private static final int UPGRADE_DB = 20;

	private ImageButton searchButton;
	private ImageButton recentButton;
	private ImageButton downloadButton;
	private ImageButton playingButton;
//	private Button settingsButton;
	private ImageButton featuredShowsButton;
	private ImageButton browseArtistsButton;
	private ImageView separator1;
	private ImageView separator2;
	private ImageView separator3;
	
	private UpgradeTask upgradeTask;
	//private ArtistUpdateTask artistUpdateTask;
	//private ShowUpdateTask showUpdateTask;
	private boolean dialogShown;
	
	private StaticDataStore db;
	
	public void showDialog(String message, String title){
		// DialogFragment.show() will take care of adding the fragment
		// in a transaction. We also want to remove any currently showing
		// dialog, so make our own transaction and take care of that here.
		// Note that if there was a previous dialog, it might still be
		// being removed from the Activity, in which case we don't try
		// to remove it again, because we would get an error.
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		Fragment prev = getFragmentManager().findFragmentByTag("dialog");
		if (prev != null) {
			if (prev.isRemoving()) {
			} else {
				ft.remove(prev);
			}
		}
		// Create and show the dialog.
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(message).setTitle(title).setPositiveButton("Okay", new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	        	   
	           }
	       });
		builder.setMessage(message).setTitle(title).setNeutralButton("Donate!", new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	        	   Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=vibevault%40gmail%2ecom&lc=US&item_name=Vibe%20Vault&currency_code=USD&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHosted"));
	        	   startActivity(browserIntent);	           
        	   }
	       });
		builder.create().show();
		ft.commit();
	}
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.home_screen);
		
		Object retained = getLastNonConfigurationInstance();
		
		if(retained instanceof UpgradeTask){		
			 
			upgradeTask = (UpgradeTask)retained;
			upgradeTask.setActivity(this);
		} else{
			//upgradeTask = new UpgradeTask(this);
		}
		
		int[] gradientColors = {0, 0xFF127DD4, 0};
		int curOrientation = this.getResources().getConfiguration().orientation;
		// FIXME
		separator1 = (ImageView)findViewById(R.id.separator1);
		separator1.setBackgroundDrawable(new GradientDrawable(curOrientation==Configuration.ORIENTATION_PORTRAIT?Orientation.RIGHT_LEFT:Orientation.RIGHT_LEFT, gradientColors));
		//separator1.setBackground(new GradientDrawable(Orientation.RIGHT_LEFT, gradientColors));
		separator2 = (ImageView)findViewById(R.id.separator2);
		separator2.setBackgroundDrawable(new GradientDrawable(curOrientation==Configuration.ORIENTATION_PORTRAIT?Orientation.RIGHT_LEFT:Orientation.TOP_BOTTOM, gradientColors));
		//separator2.setBackground(new GradientDrawable(Orientation.RIGHT_LEFT, gradientColors));
		separator3 = (ImageView)findViewById(R.id.separator3);
		separator3.setBackgroundDrawable(new GradientDrawable(curOrientation==Configuration.ORIENTATION_PORTRAIT?Orientation.TOP_BOTTOM:Orientation.TOP_BOTTOM, gradientColors));
		//separator3.setBackground(new GradientDrawable(Orientation.TOP_BOTTOM, gradientColors));
		
		searchButton = (ImageButton) findViewById(R.id.HomeSearch);
		recentButton = (ImageButton) findViewById(R.id.HomeRecent);
		downloadButton = (ImageButton) findViewById(R.id.HomeDownload);
		playingButton = (ImageButton) findViewById(R.id.HomePlaying);
		featuredShowsButton = (ImageButton) findViewById(R.id.HomeFeatured);
		browseArtistsButton = (ImageButton) findViewById(R.id.HomeBrowse);
		
		
		db = StaticDataStore.getInstance(this);
//		setImageButtonToToast();
//		upgradeTask = new UpgradeTask(this);
//		upgradeTask.execute();	
		if (db.needsUpgrade && upgradeTask == null) { //DB needs updating
			setImageButtonToToast();
			upgradeTask = new UpgradeTask(this);
			upgradeTask.execute();			
		} else { // DB Up to date, check artist date
			setImageButtonToFragments();
			if (needsArtistFetching() && upgradeTask == null) {
				upgradeTask = new UpgradeTask(this);
				upgradeTask.execute();
			}
		}
		
		if (db.dbCopied && !Boolean.parseBoolean(db.getPref("splashShown"))) {
			this.showDialog(this.getResources().getString(R.string.splash_screen), "Welcome to Vibe Vault 4");
			db.updatePref("splashShown", "true");
		} else if (db.needsUpgrade) {
			this.showDialog(this.getResources().getString(R.string.splash_screen), "Welcome to Vibe Vault 4");
		}
	}
	
	private void setImageButtonToToast() {
		searchButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v){
				Toast.makeText(HomeScreen.this, "Upgrading DB", Toast.LENGTH_SHORT).show();
			}
		});
		recentButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v){
				Toast.makeText(HomeScreen.this, "Upgrading DB", Toast.LENGTH_SHORT).show();
			}
		});
		downloadButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v){
				Toast.makeText(HomeScreen.this, "Upgrading DB", Toast.LENGTH_SHORT).show();
			}
		});
		playingButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v){
				Toast.makeText(HomeScreen.this, "Upgrading DB", Toast.LENGTH_SHORT).show();
			}
		});
		featuredShowsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v){
				Toast.makeText(HomeScreen.this, "Upgrading DB", Toast.LENGTH_SHORT).show();
			}
		});
		browseArtistsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v){
				Toast.makeText(HomeScreen.this, "Upgrading DB", Toast.LENGTH_SHORT).show();
			}
		});
	}
	
	private void setImageButtonToFragments() {
		searchButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v){
				Intent i = new Intent(HomeScreen.this, SearchScreen.class);
				i.putExtra("type", 0);
				i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
				i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(i);
			}
		});
		recentButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v){
				Intent i = new Intent(HomeScreen.this, SearchScreen.class);
				i.putExtra("type", 3);
				i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
				i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(i);
			}
		});
		downloadButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v){
				Intent i = new Intent(HomeScreen.this, SearchScreen.class);
				i.putExtra("type", 6);
				i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
				i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(i);
			}
		});
		playingButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v){
				Intent i = new Intent(HomeScreen.this, SearchScreen.class);
				i.putExtra("type", 2);
				i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
				i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(i);
			}
		});
		featuredShowsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v){
				Intent i = new Intent(HomeScreen.this, SearchScreen.class);
				i.putExtra("type", 4);
				i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
				i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(i);
			}
		});
		browseArtistsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v){
				Intent i = new Intent(HomeScreen.this, SearchScreen.class);
				i.putExtra("type", 5);
				i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
				i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(i);
			}
		});
	}
	
	private boolean needsArtistFetching() {
		String dateString = db.getPref("artistUpdate");
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
		try {
			 
			Date dbDate = format.parse(dateString);
			GregorianCalendar cal1 = new GregorianCalendar();
			cal1.add(Calendar.MONTH, -2);
			Date upgradeDate = cal1.getTime();
			GregorianCalendar cal2 = new GregorianCalendar();
			cal2.add(Calendar.YEAR, 1);
			Date yearLater = cal2.getTime();
			 
			if (upgradeDate.after(dbDate) || yearLater.before(dbDate)) {
				return true;
			}
			else {
				return false;
			}
		} catch (java.text.ParseException e) {
			 
			e.printStackTrace();
			return false;
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
	}
	
	@Override
	public void onPause() {
		super.onPause();
	}
	
//	private class ShowUpdateTask extends AsyncTask<Integer, Integer, Integer> {
//
//		@Override
//		protected Integer doInBackground(Integer... params) {
//			ArrayList<ArchiveShowObj> shows = db.getBadShows();
//			 
//			if (shows.size() > 0) {
//				 
//				for (ArchiveShowObj s : shows) {
//					Searching.getSongs(s, null, db, false);
//				}
//			}
//			return null;
//		}
//		
//		protected void onPostExecute(Integer arg0) {
//			 
//		}
//		
//	}
//	
//	private class ArtistUpdateTask extends AsyncTask<Integer, Integer, Integer> {
//
//		private boolean success = false;
//		
//		@Override
//		protected Integer doInBackground(Integer... arg0) {
//			success = Searching.updateArtists(db);
//			return null;
//		}
//		
//		@Override
//		protected void onPostExecute(Integer arg0) {
//			String message = success ? "Updated Artists" : "Failed to Update Artists";
//			Toast.makeText(HomeScreen.this, message, Toast.LENGTH_SHORT).show();			
//		}
//		
//	}
	
	private class UpgradeTask extends AsyncTask<String, Integer, String> {

		private HomeScreen parentScreen;
		private boolean success = false;
		private boolean completed = false;
		
		private UpgradeTask(HomeScreen activity){
			this.parentScreen = activity;
		}
		
		protected void onPreExecute(){
			 
			if (db.needsUpgrade) {
				parentScreen.showDialog(UPGRADE_DB);
			}
		}
		
		@Override
		protected String doInBackground(String... upgradeString) {
			/*Upgrade or copy*/
			//Upgrade existing
			if (db.needsUpgrade) {
				 
				success = db.upgradeDB();
				//Copy new one if failure upgrading
				
				if(!success){
					try {
						db.copyDB();
					} catch (IOException e) {
						throw new Error("Error copying database");
					}
				}
				//Finally open DB
				try {
					db.openDataBase();
				} catch (SQLException e) {
					 
					 
				}
				//DB is now ready to use
				db.updatePref("splashShown", "true");
				publishProgress(25);
			}
			
			//Fix bad shows
			ArrayList<ArchiveShowObj> shows = db.getBadShows();
			 
			if (shows.size() > 0) {
				 
				for (ArchiveShowObj s : shows) {
					Searching.getSongs(s, null, db, false);
				}
			}
			publishProgress(50);
			
			//Update Artists if necessary
			if (needsArtistFetching()) {
				Searching.updateArtists(db);
				publishProgress(75);
			}
			return "Completed";
		}

		protected void onPostExecute(String upgradeString) {	
		}
		
		protected void onProgressUpdate(Integer... progress) {
			if (progress[0] == 25) {
				parentScreen.dismissDialog(UPGRADE_DB);
				setImageButtonToFragments();	
				completed=true;
				notifyActivityTaskCompleted();
			}
			if (progress[0] == 50) {
				 
			}
			if (progress[0] == 75) {
				String message = "Updated Artists";
				Toast.makeText(HomeScreen.this, message, Toast.LENGTH_SHORT).show();
			}
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
		if (upgradeTask != null) {
			upgradeTask.setActivity(null);
		}
		return upgradeTask;
	}
	
	/** Dialog preparation method.
	*
	* Includes Thread bookkeeping to prevent not leaking Views on orientation changes.
	*/
	@Override
	protected void onPrepareDialog(int id, Dialog dialog){
		super.onPrepareDialog(id, dialog);
		if(id==UPGRADE_DB){
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
			case UPGRADE_DB:
				
				ProgressDialog dialog = new ProgressDialog(this);
				dialog.setMessage("Updating Database, ");
				return dialog;
			default:
				return super.onCreateDialog(id);	
		}
	}
	
	private void onTaskCompleted(){
		if(dialogShown){
			try{
				dismissDialog(UPGRADE_DB);
			} catch(IllegalArgumentException e){
				
				e.printStackTrace();
			}
			
		}
	}
}