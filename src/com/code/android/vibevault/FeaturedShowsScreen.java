/*
 * FeaturedShowsScreen.java
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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

public class FeaturedShowsScreen extends Activity {
	
	private static final String LOG_TAG = FeaturedShowsScreen.class.getName();
	
	protected ListView featuredShowsList;
	protected Button getMoreShowsButton;
	private GetSelectedShowsListTask workerTask;
	private boolean dialogShown;
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.featured_shows_screen);
		
		getMoreShowsButton = new Button(this);
		getMoreShowsButton.setText("More Featured Shows");
		getMoreShowsButton.setTextColor(Color.rgb(18, 125, 212));
		getMoreShowsButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
		getMoreShowsButton.setPadding(0, 6, 0, 6);
		
		getMoreShowsButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if(VibeVault.moreFeaturedShows.size()!=0){
					fetchSelectedShows(VibeVault.moreFeaturedShows.get(0));
				} else{
					Toast.makeText(getBaseContext(), "More featured shows weekly...", Toast.LENGTH_SHORT).show();
				}
			}
		});
		
		this.featuredShowsList = (ListView) this.findViewById(R.id.SelectedShowsListView);
		featuredShowsList.addFooterView(getMoreShowsButton);
		if(VibeVault.featuredShows.size()==0){
			getMoreShowsButton.setVisibility(View.GONE);
		} else{
			getMoreShowsButton.setVisibility(View.VISIBLE);
		}
		featuredShowsList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> a, View v, int position, long id) {
				ArchiveShowObj show = (ArchiveShowObj)featuredShowsList.getItemAtPosition(position);
				Intent i = new Intent(FeaturedShowsScreen.this, ShowDetailsScreen.class);
				i.putExtra("Show", show);
				startActivity(i);
			}
		});
		featuredShowsList.setOnCreateContextMenuListener(new OnCreateContextMenuListener(){
			@Override
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo){
				menu.add(Menu.NONE, VibeVault.EMAIL_LINK, Menu.NONE, "Email Link to Show");
			}
		});
		
		Object retained = getLastNonConfigurationInstance();
		if(retained instanceof GetSelectedShowsListTask){
			
			workerTask = (GetSelectedShowsListTask)retained;
			workerTask.setActivity(this);
		} else{
			workerTask = new GetSelectedShowsListTask(this);
			if(VibeVault.featuredShows.size()==0){
				this.fetchSelectedShows("http://andrewpearson.org/vibevault/shows/vvshows1");
			}
			this.refreshSelectedShowsList();
		}
	}
	
	public void refreshSelectedShowsList() {		
		featuredShowsList.setAdapter(new RatingsAdapter(this, R.layout.search_list_row, VibeVault.featuredShows));
		if(VibeVault.featuredShows.size()==0){
			getMoreShowsButton.setVisibility(View.GONE);
		} else{
			getMoreShowsButton.setVisibility(View.VISIBLE);
		}
	}
	
	/** Dialog preparation method.
	*
	* Includes Thread bookkeeping to prevent not leaking Views on orientation changes.
	*/
	@Override
	protected void onPrepareDialog(int id, Dialog dialog){
		super.onPrepareDialog(id, dialog);
		if(id==VibeVault.RETRIEVING_DIALOG_ID){
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
			case VibeVault.RETRIEVING_DIALOG_ID:
				
				ProgressDialog dialog = new ProgressDialog(this);
				dialog.setMessage("Retrieving Featured Shows...");
				return dialog;
			default:
				return super.onCreateDialog(id);	
		}
	}

	/** Bookkeeping method to deal with dialogs over orientation changes.
	*
	*/
	private void onTaskCompleted(){
		this.refreshSelectedShowsList();
		if(dialogShown){
			try{
				dismissDialog(VibeVault.RETRIEVING_DIALOG_ID);
			} catch(IllegalArgumentException e){
				
				e.printStackTrace();
			}
			dialogShown=false;
			
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
	
	/** Handle user's long-click selection.
	*
	*/
	@Override
	public boolean onContextItemSelected(MenuItem item){
		AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo)item.getMenuInfo();
		if(menuInfo!=null){
			ArchiveShowObj selShow = (ArchiveShowObj)featuredShowsList.getAdapter().getItem(menuInfo.position);
			switch(item.getItemId()){
			case VibeVault.EMAIL_LINK:
				final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
				emailIntent.setType("plain/text");
				emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Great show on archive.org: " + selShow.getArtistAndTitle());
				emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Hey,\n\nYou should listen to " + selShow.getArtistAndTitle() + ".  You can find it here: " + selShow.getShowURL() + "\n\nSent using VibeVault for Android.");
				startActivity(Intent.createChooser(emailIntent, "Send mail..."));
				return true;
			default:
				break;
			}
			return false;
		}
		return true;
	}
	
	public void fetchSelectedShows(String show){
		this.workerTask = new GetSelectedShowsListTask(this);
		workerTask.execute(show);
	}
	
	// ArrayAdapter for the ListView of shows with ratings.
	private class RatingsAdapter extends ArrayAdapter<ArchiveShowObj> {

		public RatingsAdapter(Context context, int textViewResourceId,
				List<ArchiveShowObj> objects) {
			super(context, textViewResourceId, objects);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ArchiveShowObj show = (ArchiveShowObj) featuredShowsList.getItemAtPosition(position);
			
			if (convertView == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = vi.inflate(R.layout.search_list_row, null);
			}
			TextView artistText = (TextView) convertView.findViewById(R.id.ArtistText);
			TextView showText = (TextView) convertView.findViewById(R.id.ShowText);
			ImageView ratingsIcon = (ImageView) convertView.findViewById(R.id.rating);
			TextView showInfoText = (TextView) convertView.findViewById(R.id.ShowInfoText);
			if (show != null) {
				artistText.setText(show.getShowArtist());
				artistText.setSelected(true);
				showText.setText(show.getShowTitle());
				showText.setSelected(true);
				showInfoText.setText(show.getSource());
				switch ((int) show.getRating()) {
				case 1:
					ratingsIcon.setImageDrawable(getBaseContext().getResources().getDrawable(R.drawable.star1));
					break;
				case 2:
					ratingsIcon.setImageDrawable(getBaseContext().getResources().getDrawable(R.drawable.star2));
					break;
				case 3:
					ratingsIcon.setImageDrawable(getBaseContext().getResources().getDrawable(R.drawable.star3));
					break;
				case 4:
					ratingsIcon.setImageDrawable(getBaseContext().getResources().getDrawable(R.drawable.star4));
					break;
				case 5:
					ratingsIcon.setImageDrawable(getBaseContext().getResources().getDrawable(R.drawable.star5));
					break;
				default:
					ratingsIcon.setImageDrawable(getBaseContext().getResources().getDrawable(R.drawable.star0));
					break;
				}
			}
			return convertView;
		}
	}
	
	
	
	private class GetSelectedShowsListTask extends AsyncTask<String, String, Void> {
		
		private FeaturedShowsScreen parentScreen;
		private boolean completed;
		
		private GetSelectedShowsListTask(FeaturedShowsScreen activity){
			this.parentScreen = activity;
		}
		
		@Override
		protected void onPreExecute(){
			parentScreen.showDialog(VibeVault.RETRIEVING_DIALOG_ID);
		}

		@Override
		protected Void doInBackground(String... listAddress) {
			
			URL listURL = null;
			String listString = null;
			
			if(listAddress[0]==null){
				return null;
			}
			
			try {
				listURL = new URL(listAddress[0]);
			} catch (MalformedURLException e) {
				
				return null;
			}
			try {
				URLConnection listConnection;
				listConnection = listURL.openConnection();
				if(listConnection==null){
					
					return null;
				}
				InputStream inStream = listConnection.getInputStream();
				BufferedInputStream bis = new BufferedInputStream(inStream);
				ByteArrayBuffer baf = new ByteArrayBuffer(50);
				int read = 0;
				int bufSize = 512;
				byte[] buffer = new byte[bufSize];
				while(bis.available()==0){
					bis.close();
					inStream.close();
					listConnection = listURL.openConnection();
					inStream = listConnection.getInputStream();
					bis = new BufferedInputStream(inStream);
				}
				while (true) {
					read = bis.read(buffer);
					if (read == -1) {
						break;
					}
					baf.append(buffer, 0, read);
				}
				listString = new String(baf.toByteArray());
				bis.close();
				inStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if(listString==null){
				return null;
			}
			
			JSONObject jObject;
			boolean firstFetch = false;
			try {
				// Root JSON object.
				jObject = new JSONObject(listString);
				jObject = jObject.getJSONObject("response");
				// If there are no featured songs already in the ListView or no featured shows
				// in the Selected Shows list, get the list of other featured shows.
				if(VibeVault.moreFeaturedShows.size()==0 && VibeVault.featuredShows.size()==0){
					JSONArray showsArray = jObject.getJSONArray("otherLinks");
					int numShows = showsArray.length();
					for(int i = 0; i < numShows; i++){
						VibeVault.moreFeaturedShows.add((String)showsArray.getString(i));
					}
					firstFetch = true;
				}
				// Get the featured shows.
				JSONArray docsArray = jObject.getJSONArray("selectedShows");
				int numItems = docsArray.length();
				for (int i = 0; i < numItems; i++) {
					JSONObject songObject = docsArray.getJSONObject(i);
					VibeVault.featuredShows.add(VibeVault.featuredShows.size(),new ArchiveShowObj(songObject.optString("title"), songObject.optString("identifier"), songObject.optString("date"), songObject.optDouble("avg_rating"), songObject.optString("format"), songObject.optString("show_info")));
				}
				// If this isn't the first time you are fetching the information, it is coming from
				// the list of more featured shows, so you need to pop the top show because you just got it.
				if(!firstFetch){
					VibeVault.moreFeaturedShows.remove(0);
				}
			} catch (JSONException e) {
				Log.e(LOG_TAG, "JSON error: " + listString);
				Log.e(LOG_TAG, e.toString());
			}
		return null;
	}
		
		@Override
		protected void onPostExecute(Void v){
			refreshSelectedShowsList();
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
		private void setActivity(FeaturedShowsScreen activity){
			this.parentScreen = activity;
			if(completed){
				notifyActivityTaskCompleted();
			}
		}
		
	}

}
