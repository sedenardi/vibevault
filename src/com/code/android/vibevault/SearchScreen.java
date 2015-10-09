/*
 * SearchScreen.java
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.List;

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
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SlidingDrawer;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.SlidingDrawer.OnDrawerCloseListener;
import android.widget.SlidingDrawer.OnDrawerOpenListener;

import com.code.android.vibevault.R;

public class SearchScreen extends Activity {
	
	private static final String LOG_TAG = SearchScreen.class.getName();
	
	protected ListView searchList;
	protected File appRootDir;
	protected TextView labelText;
	
	/* SlidingDrawer Members... */
	protected SlidingDrawer searchDrawer;
	protected TextView handleText;
	protected EditText generalSearchInput;
	protected EditText artistSearchInput;
	protected EditText monthSearchInput;
	protected EditText yearSearchInput;
	protected Spinner dateModifierSpinner;
	protected ArrayAdapter<CharSequence> spinnerAdapter;
	protected Button searchButton;
	protected Button searchMoreButton;
	protected Button settingsButton;
	protected Button clearButton;
	
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
		this.generalSearchInput = (EditText) this.findViewById(R.id.GeneralSearchBox);
		this.artistSearchInput = (EditText) this.findViewById(R.id.ArtistSearchBox);
		this.monthSearchInput = (EditText) this.findViewById(R.id.MonthSearchBox);
		this.yearSearchInput = (EditText) this.findViewById(R.id.YearSearchBox);
		this.dateModifierSpinner = (Spinner) this.findViewById(R.id.DateSearchSpinner);
		this.searchButton = (Button) this.findViewById(R.id.SearchButton);
		this.settingsButton = (Button) this.findViewById(R.id.SettingsButton);
		this.searchMoreButton = (Button) this.findViewById(R.id.SearchMoreButton);
		this.clearButton = (Button) this.findViewById(R.id.ClearButton);
		this.searchDrawer = (SlidingDrawer) this.findViewById(R.id.SlidingDrawerSearchScreen);
		this.handleText = (TextView) this.findViewById(R.id.HandleTextView);
		
		searchList.setOnCreateContextMenuListener(new OnCreateContextMenuListener(){
			@Override
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo){
				menu.add(Menu.NONE, VibeVault.EMAIL_LINK, Menu.NONE, "Email Link to Show");
				menu.add(Menu.NONE, VibeVault.SHOW_INFO, Menu.NONE, "Show Info");
			}
		});
		
		Object retained = getLastNonConfigurationInstance();
		if(retained instanceof JSONQueryTask){
			
			
			workerTask = (JSONQueryTask)retained;
			workerTask.setActivity(this);
		} else{
			workerTask = new JSONQueryTask(this);
		}
		if(VibeVault.searchResults.size()!=0){
			this.searchMoreButton.setEnabled(true);
			//this.clearButton.setEnabled(true);
		} else{
			this.searchMoreButton.setEnabled(false);
			//this.clearButton.setEnabled(false);
		}
		this.init();
		
		
		Intent intent = getIntent();
		String artist;
		if(intent.hasExtra("Artist")){
			browseArtist(intent.getStringExtra("Artist"));
		}
		
	}
	
	public void browseArtist(String artist){
		artistSearchInput.setText(artist);
			VibeVault.searchResults.clear();
			VibeVault.generalSearchText = "";
			VibeVault.artistSearchText = artist;
			// If the year and date are set properly, or are unset, search.
			if(setDate("","")){
				// "1" is passed to retrieve page number 1.
				executeSearch(makeSearchURLString(1));
				pageNum=1;
				searchDrawer.close();
			}
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
			dialogShown=false;
			
		}
	}
	
	/** Handle user's long-click selection.
	*
	*/
	@Override
	public boolean onContextItemSelected(MenuItem item){
		AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo)item.getMenuInfo();
		if(menuInfo!=null){
			ArchiveShowObj selShow = (ArchiveShowObj)searchList.getAdapter().getItem(menuInfo.position);
			switch(item.getItemId()){
			case VibeVault.EMAIL_LINK:
				final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
				emailIntent.setType("plain/text");
				emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Great show on archive.org: " + selShow.getArtistAndTitle());
				emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Hey,\n\nYou should listen to " + selShow.getArtistAndTitle() + ".  You can find it here: " + selShow.getShowURL() + "\n\nSent using VibeVault for Android.");
				startActivity(Intent.createChooser(emailIntent, "Send mail..."));
				return true;
			case VibeVault.SHOW_INFO:
				AlertDialog.Builder ad = new AlertDialog.Builder(this);
				ad.setTitle("Show Info");
				View v = LayoutInflater.from(this).inflate(R.layout.scrollable_dialog, null);
				((TextView) v.findViewById(R.id.DialogText)).setText(selShow.getSource());
				ad.setPositiveButton("Okay.", new android.content.DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int arg1) {
					}
				});
				ad.setView(v);
				ad.show();
			default:
					break;
			}
			return false;
		}
		return true;
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
	
	private void launchSettingsDialog(){
		final SeekBar seek;
		final Spinner spin;
		final TextView seekValue;
		
		// Make the settings dialog.
		AlertDialog.Builder ad = new AlertDialog.Builder(this);
		ad.setTitle("Search Settings");
		View v = LayoutInflater.from(this).inflate(R.layout.scrollable_settings_dialog, null);
		ad.setPositiveButton("Okay.", new android.content.DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int arg1) {
			}
		});
		
		// Grab all the GUI widgets.
		seek = (SeekBar)v.findViewById(R.id.NumResultsSeekBar);
		seek.setProgress(Integer.valueOf(VibeVault.db.getPref("numResults")) - 10);
		spin = (Spinner)v.findViewById(R.id.SortSpinner);
		seekValue = (TextView)v.findViewById(R.id.SeekBarValue);
		seekValue.setText(VibeVault.db.getPref("numResults"));
		
		// Set the seek bar to its current value, and set up a Listener.
		seek.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				seekValue.setText(String.valueOf(progress + 10));
				VibeVault.db.updatePref("numResults", String.valueOf(progress + 10));
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});
		
		// Set up the spinner, and set up it's OnItemSelectedListener.
		ArrayAdapter<String> spinAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,VibeVault.sortChoices);
		spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spin.setAdapter(spinAdapter);
		int pos = 1;
		String sortOrder = VibeVault.db.getPref("sortOrder");
		for(int i = 0; i < VibeVault.sortChoices.length; i++){
			if (VibeVault.sortChoices[i].equals(sortOrder))
				pos = i;
		}
		spin.setSelection(pos);
		spin.setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> arg0, View view, int arg2, long arg3) {
				int selected = arg0.getSelectedItemPosition();
				VibeVault.db.updatePref("sortOrder", VibeVault.sortChoices[selected]);
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
			
		});
		
		// Show the settings screen.
		ad.setView(v);
		ad.show();
	}
	
	/** Returns true if a valid date, or no date at all, was passed.
	 * Returns false if an improper date is passed.  If an improper date,
	 * or no date, is passed, VibeVault's month and year ints are set to -1.
	 * Otherwise, they are set to the right value.
	 */
	private boolean setDate(String year, String month){
		int curYear = -1;
		int curMonth = -1;
		if(year.equals("")){
			if(month.equals("")){  // Year and month are blank.
				VibeVault.monthSearchInt = -1;
				VibeVault.yearSearchInt = -1;
				return true;
			} else{  // Year is blank, month is full.
				Toast.makeText(getBaseContext(), "Enter a year...", Toast.LENGTH_SHORT).show();
				VibeVault.monthSearchInt = -1;
				VibeVault.yearSearchInt = -1;
				return false;
			}
		} else{
			curYear = Integer.valueOf(year);
			// If the year is proper, set it with the inputted month, or default to January if the month is bad.
			if(curYear>=1800 && curYear <= Calendar.getInstance().get(Calendar.YEAR)){
				VibeVault.yearSearchInt = curYear;
				try{
					curMonth = Integer.valueOf(month);
				} catch (NumberFormatException e){
					VibeVault.monthSearchInt = 1;
				}
				if (curMonth < 1 || curMonth > 12) {
//					Toast.makeText(getBaseContext(), "Bad Month.  Month defaulting to January...", Toast.LENGTH_SHORT).show();
					VibeVault.monthSearchInt = 1;
				} else{
					VibeVault.monthSearchInt = curMonth;
				}
				return true;
			} else{
				Toast.makeText(getBaseContext(), "Year must be between now and 1800...", Toast.LENGTH_SHORT).show();
				VibeVault.monthSearchInt = -1;
				VibeVault.yearSearchInt = -1;
				return false;
			}
		}
	}

	private void init() {
		
		// Set up the date selection spinner.
		spinnerAdapter = ArrayAdapter.createFromResource(this, R.array.date_modifier, android.R.layout.simple_spinner_item);
		spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		dateModifierSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int pos, long id) {
				VibeVault.dateSearchModifierPos=pos;
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
		dateModifierSpinner.setAdapter(spinnerAdapter);
		dateModifierSpinner.setSelection(VibeVault.dateSearchModifierPos);
		// Set up the SlidingDrawer open and close listeners.
		searchDrawer.setOnDrawerOpenListener(new OnDrawerOpenListener(){
			@Override
			public void onDrawerOpened() {
				searchList.setBackgroundDrawable(getResources().getDrawable(R.drawable.backgrounddrawableblue));
				searchList.getBackground().setDither(true);
				searchList.setEnabled(false);
				handleText.setText("Fill in one or more boxes below...");
				handleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
				generalSearchInput.setText(VibeVault.generalSearchText);
				artistSearchInput.setText(VibeVault.artistSearchText);
				if(VibeVault.monthSearchInt!=-1){
					monthSearchInput.setText(String.valueOf(VibeVault.monthSearchInt));
				} else{
					monthSearchInput.setText("");
				}
				if(VibeVault.yearSearchInt!=-1){
					yearSearchInput.setText(String.valueOf(VibeVault.yearSearchInt));
				} else{
					yearSearchInput.setText("");
				}
				dateModifierSpinner.setSelection(VibeVault.dateSearchModifierPos);
			}
		});
		searchDrawer.setOnDrawerCloseListener(new OnDrawerCloseListener(){
			@Override
			public void onDrawerClosed() {
				searchList.setBackgroundColor(Color.BLACK);
				searchList.setEnabled(true);
				handleText.setText("Drag up to search...");
				handleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 28);
			}
		});
		
		// Set listeners in the show details and artist search bars for the enter key.
		OnKeyListener enterListener = new OnKeyListener() {
		    @Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
		        if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
		            (keyCode == KeyEvent.KEYCODE_ENTER)) {
		        	if (!(generalSearchInput.getText().toString().equals("")&&artistSearchInput.getText().toString().equals(""))) {
						VibeVault.generalSearchText = generalSearchInput.getText().toString();
						VibeVault.artistSearchText = artistSearchInput.getText().toString();
						// If the year and date are set properly, or are unset, search.
						String year = yearSearchInput.getText().toString();
						String month = monthSearchInput.getText().toString();
						if(setDate(year,month)){
							// "1" is passed to retrieve page number 1.
							executeSearch(makeSearchURLString(1));
					        pageNum = 1;
							searchDrawer.close();
						}
						return true;
		        	}
		        }
		        return false;
		    }
		};
		this.generalSearchInput.setOnKeyListener(enterListener);
		this.artistSearchInput.setOnKeyListener(enterListener);
		
		this.searchButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!(generalSearchInput.getText().toString().equals("")&&artistSearchInput.getText().toString().equals(""))) {
					VibeVault.searchResults.clear();
					VibeVault.generalSearchText = generalSearchInput.getText().toString();
					VibeVault.artistSearchText = artistSearchInput.getText().toString();
					// If the year and date are set properly, or are unset, search.
					String year = yearSearchInput.getText().toString();
					String month = monthSearchInput.getText().toString();
					if(setDate(year,month)){
						// "1" is passed to retrieve page number 1.
						executeSearch(makeSearchURLString(1));
						pageNum=1;
						searchDrawer.close();
					}
	        	}
			}
		});
		
		this.settingsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				launchSettingsDialog();
			}
		});
		
		this.clearButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				generalSearchInput.setText("");
				artistSearchInput.setText("");
				monthSearchInput.setText("");
				yearSearchInput.setText("");
				VibeVault.generalSearchText = "";
				VibeVault.artistSearchText = "";
				VibeVault.monthSearchInt = -1;
				VibeVault.yearSearchInt = -1;
				searchMoreButton.setEnabled(false);
				VibeVault.searchResults.clear();
				refreshSearchList();
			}
		});
		
		this.searchMoreButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!(generalSearchInput.getText().toString().equals("")&&artistSearchInput.getText().toString().equals(""))) {
					if(VibeVault.generalSearchText.equals("")&&VibeVault.artistSearchText.equals("")){
						Toast.makeText(getBaseContext(), "You need a query first...", Toast.LENGTH_SHORT).show();
						return;
					} else{
						generalSearchInput.setText(VibeVault.generalSearchText);
						artistSearchInput.setText(VibeVault.artistSearchText);
						monthSearchInput.setText(String.valueOf(VibeVault.monthSearchInt));
						yearSearchInput.setText(String.valueOf(VibeVault.yearSearchInt));
						dateModifierSpinner.setSelection(VibeVault.dateSearchModifierPos);
						// pageNum is incremented then searched with to get the next page.
						executeSearch(makeSearchURLString(++pageNum));
						searchDrawer.close();
					}
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
		generalSearchInput.setText(VibeVault.generalSearchText);
		artistSearchInput.setText(VibeVault.artistSearchText);
		if(VibeVault.monthSearchInt!=-1){
			monthSearchInput.setText(String.valueOf(VibeVault.monthSearchInt));
		} else{
			monthSearchInput.setText("");
		}
		if(VibeVault.yearSearchInt!=-1){
			yearSearchInput.setText(String.valueOf(VibeVault.yearSearchInt));
		} else{
			yearSearchInput.setText("");
		}
		dateModifierSpinner.setSelection(VibeVault.dateSearchModifierPos);
		
	}
	

	
	private String makeSearchURLString(int pageNum){
		int numResults = Integer.valueOf(VibeVault.db.getPref("numResults"));
		String sortPref = VibeVault.db.getPref("sortOrder");
		if(sortPref.equalsIgnoreCase("Date")){
			sortPref = "date+desc";
		} else if(sortPref.equalsIgnoreCase("Rating")){
			sortPref= "avg_rating+desc";
		}
		String queryString = null;
		
		try {
			String dateModifier = "";
			if(VibeVault.yearSearchInt!=-1){
				switch(VibeVault.dateSearchModifierPos){
				case 0:	//Before
					dateModifier = "date:[1800-01-01%20TO%20" + VibeVault.yearSearchInt + "-" + String.format("%02d", VibeVault.monthSearchInt) + "-" + "01]%20AND%20";
					break;
				case 1:	//After
					int curDate = Calendar.getInstance().get(Calendar.DATE);
					int curMonth = Calendar.getInstance().get(Calendar.MONTH);
					int curYear = Calendar.getInstance().get(Calendar.YEAR);
					dateModifier = "date:[" + VibeVault.yearSearchInt + "-" + String.format("%02d", VibeVault.monthSearchInt) + "-" + "01" + "%20TO%20" + curYear + "-" + String.format("%02d",curMonth) + "-" + String.format("%02d",curDate) + "]%20AND%20";
					break;
				case 2:	// In Year.
					dateModifier = "date:[" + VibeVault.yearSearchInt + "-01-01%20TO%20" + VibeVault.yearSearchInt + "-12-31]%20AND%20";
					break;
				}
			}
			// We search creator:(random's artist)%20OR%20creator(randoms artist) because
			// archive.org does not like apostrophes in the creator query.
			String specificSearch = "";
			if(VibeVault.generalSearchText.equals("")){
				specificSearch = "(creator:(" + URLEncoder.encode(VibeVault.artistSearchText,"UTF-8") + ")" + "%20OR%20creator:(" + URLEncoder.encode(VibeVault.artistSearchText.replace("'", "").replace("\"", ""),"UTF-8") + "))";
			} else if(VibeVault.artistSearchText.equals("")){
				specificSearch = "description:(" + URLEncoder.encode(VibeVault.generalSearchText,"UTF-8") + ")";
			} else{
				specificSearch = "(creator:(" + URLEncoder.encode(VibeVault.artistSearchText,"UTF-8") + ")" + "%20OR%20creator:(" + URLEncoder.encode(VibeVault.artistSearchText.replace("'", "").replace("\"", ""),"UTF-8") + "))%20AND%20" + "description:(" + URLEncoder.encode(VibeVault.generalSearchText,"UTF-8") + ")";
			}
			String mediaType = "mediatype:(etree)";	
			
			queryString = "http://www.archive.org/advancedsearch.php?q="
			+ "(" + dateModifier + mediaType + "%20AND%20(" + specificSearch + "))"
			+ "&fl[]=date&fl[]=avg_rating&fl[]=source&fl[]=format&fl[]=identifier&fl[]=mediatype&fl[]=title&sort[]="
			+ sortPref + "&sort[]=&sort[]=&rows="
			+ String.valueOf(numResults) + "&page=" + String.valueOf(pageNum) + "&output=json&callback=callback&save=yes";
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return queryString;
	}

	private void refreshSearchList() {
		searchList.setAdapter(new RatingsAdapter(this,
				R.layout.search_list_row, VibeVault.searchResults));
		if(VibeVault.searchResults.size()!=0){
			searchMoreButton.setEnabled(true);
			//clearButton.setEnabled(true);
		} else{
			searchMoreButton.setEnabled(false);
			//clearButton.setEnabled(false);
		}
	}
	
	// ArrayAdapter for the ListView of shows with ratings.
	private class RatingsAdapter extends ArrayAdapter<ArchiveShowObj> {

		public RatingsAdapter(Context context, int textViewResourceId,
				List<ArchiveShowObj> objects) {
			super(context, textViewResourceId, objects);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ArchiveShowObj show = (ArchiveShowObj) searchList
					.getItemAtPosition(position);
			
			if (convertView == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = vi.inflate(R.layout.search_list_row, null);
			}
			TextView artistText = (TextView) convertView
					.findViewById(R.id.ArtistText);
			TextView showText = (TextView) convertView
					.findViewById(R.id.ShowText);
			ImageView ratingsIcon = (ImageView) convertView
					.findViewById(R.id.rating);
			TextView showInfoText = (TextView) convertView.findViewById(R.id.ShowInfoText);
			if (show != null) {
				artistText.setText(show.getShowArtist());
				artistText.setSelected(true);
				showText.setText(show.getShowTitle());
				showText.setSelected(true);
				showInfoText.setVisibility(View.GONE);
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
				ad.setPositiveButton("Okay.", new android.content.DialogInterface.OnClickListener() {
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
		imm.hideSoftInputFromWindow(generalSearchInput.getWindowToken(), 0);
		imm.hideSoftInputFromWindow(artistSearchInput.getWindowToken(), 0);
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
				Log.e(LOG_TAG, "Malformed URL: " + e.toString());
			} catch (IOException e) {
				// DEBUG
				Log.e(LOG_TAG, "IO Exception: " + e.toString());
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
				if(numItems == 0){
					Toast.makeText(getBaseContext(), "Artist may not have content on archive.org...", Toast.LENGTH_SHORT).show();
				}
				for (int i = 0; i < numItems; i++) {
					if (docsArray.getJSONObject(i).optString("mediatype")
							.equals("etree")) {
						// Might be inefficient to keep getting size().
						VibeVault.searchResults.add(VibeVault.searchResults.size(),new ArchiveShowObj(docsArray.getJSONObject(i).optString("title"), docsArray.getJSONObject(i).optString("identifier"), docsArray.getJSONObject(i).optString("date"), docsArray.getJSONObject(i).optDouble("avg_rating"), docsArray.getJSONObject(i).optString("format"), docsArray.getJSONObject(i).optString("source")));
					}
				}
			} catch (JSONException e) {
				if(pageNum>1){
					Toast.makeText(getBaseContext(), "There might not be any more results...  Try again once or twice.", Toast.LENGTH_SHORT).show();
				} else{
					Toast.makeText(getBaseContext(), "Error from archive.org...  Try again later.", Toast.LENGTH_SHORT).show();
				}
				// DEBUG
				Log.e(LOG_TAG, "JSON error: " + JSONString);
				Log.e(LOG_TAG, e.toString());
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