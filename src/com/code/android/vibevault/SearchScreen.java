/*
 * SearchScreen.java
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import com.code.android.vibevault.R;

public class SearchScreen extends Activity {
	
	protected ListView searchList;
	protected File appRootDir;
	protected EditText searchInput;
	protected TextView labelText;
	protected Button searchButton;
	protected Button searchMoreButton;
	
	private int pageNum = 1;
	
	private JSONQueryTask workerTask;
	private boolean dialogShown;
	
	
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
		if(id==VibeVault.SEARCHING_DIALOG_ID){
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
			case VibeVault.SEARCHING_DIALOG_ID:
				ProgressDialog dialog = new ProgressDialog(this);
				dialog.setMessage("Finding Shows");
				return dialog;
			default:
				return super.onCreateDialog(id);	
		}
	}
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search_screen);
		
		this.searchList = (ListView) this.findViewById(R.id.ResultsListView);
		this.searchInput = (EditText) this.findViewById(R.id.SearchBox);
		this.searchButton = (Button) this.findViewById(R.id.SearchButton);
		this.searchMoreButton = (Button) this.findViewById(R.id.SearchMoreButton);
		
		Object retained = getLastNonConfigurationInstance();
		if(retained instanceof JSONQueryTask){
			workerTask = (JSONQueryTask)retained;
			workerTask.setActivity(this);
		} else{
			workerTask = new JSONQueryTask(this);
		}
		if(VibeVault.searchResults.size()!=0){
			this.searchMoreButton.setVisibility(View.VISIBLE);
		} else{
			this.searchMoreButton.setVisibility(View.INVISIBLE);
		}
		this.init();
	}
	
	/** Bookkeeping method to deal with dialogs over orientation changes.
	*
	*/
	private void onTaskCompleted(){
		this.refreshSearchList();
		if(dialogShown){
			try{
				dismissDialog(VibeVault.SEARCHING_DIALOG_ID);
			} catch(IllegalArgumentException e){
				e.printStackTrace();
			}
		}
	}
	
	public void onResume()
	{
		super.onResume();
		this.refreshSearchList();
	}
	
	public void onPause()
	{
		super.onPause();
	}
	
	// You can only call execute() once on an AsyncTask.
	// This makes a new AsyncTask and calls execute on it.
	private void executeSearch(String query){
		workerTask = new JSONQueryTask(this);
		workerTask.execute(query);
	}

	private void init() {
		
		this.searchInput.setOnKeyListener(new OnKeyListener() {
		    @Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
		        if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
		            (keyCode == KeyEvent.KEYCODE_ENTER)) {
		        	if (!searchInput.getText().toString().equals("")) {
						VibeVault.searchText = searchInput.getText().toString();
						// You pass the "1" to retrieve page number 1.
						executeSearch(makeSearchURLString(searchInput.getText().toString(),1));
				        return true;
		        	}
		        }
		        return false;
		    }
		});
		
		this.searchButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				VibeVault.searchResults.clear();
				if (!searchInput.getText().toString().equals("")) {
					VibeVault.searchText = searchInput.getText().toString();
					// You pass the "1" to retrieve page number 1.
					executeSearch(makeSearchURLString(searchInput.getText().toString(),1));
				}
			}
		});
		
		this.searchMoreButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!searchInput.getText().toString().equals("")) {
					if(VibeVault.searchText.equals("")){
						Toast.makeText(getBaseContext(), "You need a query first...", Toast.LENGTH_SHORT).show();
						return;
					}
					executeSearch(makeSearchURLString(VibeVault.searchText,++pageNum));
				}
			}
		});

		searchList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> a, View v, int position, long id) {
				ArchiveShowObj show = (ArchiveShowObj)searchList.getItemAtPosition(position);
				Intent i = new Intent(SearchScreen.this, ShowDetailsScreen.class);
				i.putExtra("Show", show);
				startActivity(i);
			}
		});

		// Create the directory for our app if it don't exist.
		appRootDir = new File(Environment.getExternalStorageDirectory(), VibeVault.APP_DIRECTORY);
		if(!appRootDir.isFile() || !appRootDir.isDirectory()){
			if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
				appRootDir.mkdirs();
			}
			else{
				Toast.makeText(getBaseContext(), "sdcard is unwritable...  is it mounted on the computer?", Toast.LENGTH_SHORT).show();
			}
		}
		searchInput.setText(VibeVault.searchText);
	}
	

	
	private String makeSearchURLString(String query, int pageNum){
		int numResults = VibeVault.DEFAULT_SHOW_SEARCH_NUM;
		String queryString = null;
		try {
			queryString = "http://www.archive.org/advancedsearch.php?q="
			+ URLEncoder.encode(query,"UTF-8")
			+ "&fl[]=date&fl[]=avg_rating&fl[]=source&fl[]=format&fl[]=identifier&fl[]=mediatype&fl[]=title&sort[]=createdate+desc&sort[]=&sort[]=&rows="
			+ String.valueOf(numResults) + "&page=" + String.valueOf(pageNum) + "&output=json&callback=callback&save=yes";
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return queryString;
	}

	private void refreshSearchList() {
		searchList.setAdapter(new ArrayAdapter<ArchiveShowObj>(this,
				android.R.layout.simple_list_item_1, VibeVault.searchResults));
		if(VibeVault.searchResults.size()!=0){
			searchMoreButton.setVisibility(View.VISIBLE);
		} else{
			searchMoreButton.setVisibility(View.INVISIBLE);
		}
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
				Intent np = new Intent(SearchScreen.this, NowPlayingScreen.class);
				//Intent np = new Intent(SearchScreen.this, HomeScreen.class);
				startActivity(np);
				break;
			case R.id.recentShows:
				Intent rs = new Intent(SearchScreen.this, RecentShowsScreen.class);
				startActivity(rs);
				break;
			case R.id.scrollableDialog:
				AlertDialog.Builder ad = new AlertDialog.Builder(this);
				ad.setTitle("Help!");
				View v = LayoutInflater.from(this).inflate(R.layout.scrollable_dialog, null);
				((TextView) v.findViewById(R.id.DialogText)).setText(R.string.search_screen_help);
				ad.setPositiveButton("Cool.", new android.content.DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int arg1) {
					}
				});
				ad.setView(v);
				ad.show();
		}
		return true;
	}
	
	private void closeKeyboard(){
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
	}

	/** Search archive.org, parse the results, update the ListView in the parent activity.
	 * 
	 */
	private class JSONQueryTask extends AsyncTask<String, Integer, String> {
		
		private SearchScreen parentScreen;
		private boolean completed;
		
		private JSONQueryTask(SearchScreen activity){
			this.parentScreen = activity;
		}
		
		/* We query archive.org which returns JSON.  We use JSON instead
		 * of XML because it's smaller in size. doInBackground() returns
		 * a String to onPostExecute(), which is invoked on the UI after
		 * the background computation finishes.
		 */
		
		protected void onPreExecute(){
			parentScreen.showDialog(VibeVault.SEARCHING_DIALOG_ID);
			//Close virtual keyboard
			closeKeyboard();
		}
		
		@Override
		protected String doInBackground(String... queryString) {
			
			if(queryString[0]==null){
				return null;
			}

			String archiveQuery = queryString[0];
			String queryResult = "";

			/* Open up an HTTP connection with the archive.org query. Grab an
			 * input stream or bytes and turn it into a string. It will be of
			 * the form described in JSONQueryExample.txt. We use a
			 * BufferedInputStream because its read() call grabs many bytes at
			 * once (behind the scenes) and puts them into an internal buffer. A
			 * regular InputStream grabs one byte per read() so it has to pester
			 * the OS more and is way slower.
			 */
			InputStream in = null;
			try {
				URL url = new URL(archiveQuery);
				HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
				HttpURLConnection httpConn = (HttpURLConnection) urlConn;
				httpConn.setAllowUserInteraction(false);
				httpConn.connect();
				in = httpConn.getInputStream();
				BufferedInputStream bis = new BufferedInputStream(in);
				ByteArrayBuffer baf = new ByteArrayBuffer(50);
				int read = 0;
				int bufSize = 512;
				byte[] buffer = new byte[bufSize];
				while (true) {
					read = bis.read(buffer);
					if (read == -1) {
						break;
					}
					baf.append(buffer, 0, read);
				}
				bis.close();
				queryResult = new String(baf.toByteArray());

			} catch (MalformedURLException e) {
				// DEBUG
				Log.e(VibeVault.SEARCH_SCREEN_TAG, "Malformed URL: " + e.toString());
			} catch (IOException e) {
				// DEBUG
				Log.e(VibeVault.SEARCH_SCREEN_TAG, "IO Exception: " + e.toString());
			}
			return queryResult;

		}

		protected void onPostExecute(String JSONString) {
			
			
			if(JSONString==null){
				Toast.makeText(getBaseContext(), "Invalid query?", Toast.LENGTH_SHORT).show();
			}
			/*
			 * Parse the JSON String that we got from archive.org. If the
			 * mediatype is etree, create an ArchiveShowObj which encapsulates
			 * the information for a particular result from the archive.org
			 * query. Populate the ArrayList which backs the ListView, and call
			 * the inherited refreshSearchList().
			 */
			JSONObject jObject;
			try {
				jObject = new JSONObject(JSONString.replace("callback(", ""))
						.getJSONObject("response");
				JSONArray docsArray = jObject.getJSONArray("docs");
				int numItems = docsArray.length();
				for (int i = 0; i < numItems; i++) {
					if (docsArray.getJSONObject(i).optString("mediatype")
							.equals("etree")) {
						VibeVault.searchResults.add(0,new ArchiveShowObj(docsArray.getJSONObject(i).optString("title"), docsArray.getJSONObject(i).optString("identifier"), docsArray.getJSONObject(i).optString("date"), docsArray.getJSONObject(i).optDouble("avg_rating"), docsArray.getJSONObject(i).optString("format"), docsArray.getJSONObject(i).optString("source")));
					}
				}
			} catch (JSONException e) {
				if(pageNum>1){
					Toast.makeText(getBaseContext(), "There might not be any more results...  Try again once or twice.", Toast.LENGTH_SHORT).show();
				} else{
					Toast.makeText(getBaseContext(), "Bad query...", Toast.LENGTH_SHORT).show();
				}
				// DEBUG
				Log.e(VibeVault.SEARCH_SCREEN_TAG, "JSON error: " + JSONString);
				Log.e(VibeVault.SEARCH_SCREEN_TAG, e.toString());
			}
			refreshSearchList();
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
		private void setActivity(SearchScreen activity){
			this.parentScreen = activity;
			if(completed){
				notifyActivityTaskCompleted();
			}
		}
	}
}