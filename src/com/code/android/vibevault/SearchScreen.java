/*
 * SearchScreen.java
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
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
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
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SlidingDrawer;
import android.widget.SlidingDrawer.OnDrawerScrollListener;
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
	protected AutoCompleteTextView artistSearchInput;
	protected EditText yearSearchInput;
	protected Spinner dateModifierSpinner;
	protected ArrayAdapter<CharSequence> spinnerAdapter;
	protected Button searchButton;
	//protected Button searchMoreButton;
	protected Button settingsButton;
	protected Button clearButton;
	
	private int pageNum = 1;
	
	private JSONQueryTask workerTask;
	private boolean dialogShown;
	
    private Vibrator vibrator;	
	
	
	
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
	
	@Override
	 public void onBackPressed() {
		if(searchDrawer!=null&&searchDrawer.isOpened()){
			vibrator.vibrate(25);
			searchDrawer.close();
			return;
		} else{
			super.onBackPressed();
	         return;
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
		this.artistSearchInput = (AutoCompleteTextView) this.findViewById(R.id.ArtistSearchBox);
		this.yearSearchInput = (EditText) this.findViewById(R.id.YearSearchBox);
		this.dateModifierSpinner = (Spinner) this.findViewById(R.id.DateSearchSpinner);
		this.searchButton = (Button) this.findViewById(R.id.SearchButton);
		this.settingsButton = (Button) this.findViewById(R.id.SettingsButton);
		//this.searchMoreButton = (Button) this.findViewById(R.id.SearchMoreButton);
		this.clearButton = (Button) this.findViewById(R.id.ClearButton);
		this.searchDrawer = (SlidingDrawer) this.findViewById(R.id.SlidingDrawerSearchScreen);
		this.handleText = (TextView) this.findViewById(R.id.HandleTextView);
		
		vibrator = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);
		
		searchList.setOnCreateContextMenuListener(new OnCreateContextMenuListener(){
			@Override
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo){
				menu.add(Menu.NONE, VibeVault.EMAIL_LINK, Menu.NONE, "Email Link to Show");
				menu.add(Menu.NONE, VibeVault.SHOW_INFO, Menu.NONE, "Show Info");
				menu.add(Menu.NONE, VibeVault.ADD_TO_FAVORITE_LIST, Menu.NONE, "Bookmark Show");
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
			searchButton.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.morebutton), null, null, null);
			searchButton.setText("More");
			//this.clearButton.setEnabled(true);
		} else{
			searchButton.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.searchbutton_plain), null, null, null);
			searchButton.setText("Search");
		}
		this.init();
		
		
		Intent intent = getIntent();
		if(intent.hasExtra("Artist")){
			browseArtist(intent.getStringExtra("Artist"));
		}
		
		if(!VibeVault.db.getPref("artistUpdate").equals("2010-01-01")){
			artistSearchInput.setAdapter(new ArrayAdapter<String>(this, R.layout.artist_search_row,VibeVault.db.getArtistsStrings()));
		}
		
		if (VibeVault.searchPref.equals("Show/Artist Description")&&artistSearchInput.getText().equals("")){
			artistSearchInput.setHint("Search Descriptions...");
		}
	}
	
	public void browseArtist(String artist){
		artistSearchInput.setText(artist);
			VibeVault.searchResults.clear();
			VibeVault.artistSearchText = artist;
			yearSearchInput.setText("");
			if(setDate()){
				executeSearch(makeSearchURLString(1));
				pageNum=1;
				searchDrawer.close();
			}
	}
	
	/** Bookkeeping method to deal with dialogs over orientation changes.
	*
	*/
	private void onTaskCompleted(){
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
				return true;
			case(VibeVault.ADD_TO_FAVORITE_LIST):
				VibeVault.db.insertFavoriteShow(selShow);
				return true;
			default:
				return false;
			}
		}
		return false;
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
		final Spinner sortSpin;
		final Spinner searchSpin;
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
		sortSpin = (Spinner)v.findViewById(R.id.SortSpinner);
		searchSpin = (Spinner)v.findViewById(R.id.SearchSpinner);
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
		ArrayAdapter<String> sortAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,VibeVault.sortChoices);
		sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		sortSpin.setAdapter(sortAdapter);
		int sortPos = 1;
		String sortOrder = VibeVault.db.getPref("sortOrder");
		for(int i = 0; i < VibeVault.sortChoices.length; i++){
			if (VibeVault.sortChoices[i].equals(sortOrder))
				sortPos = i;
		}
		sortSpin.setSelection(sortPos);
		sortSpin.setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> arg0, View view, int arg2, long arg3) {
				int selected = arg0.getSelectedItemPosition();
				VibeVault.db.updatePref("sortOrder", VibeVault.sortChoices[selected]);
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
			
		});
		
		ArrayAdapter<String> searchAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,VibeVault.searchChoices);
		searchAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		searchSpin.setAdapter(searchAdapter);
		int searchPos = 1;
		String searchOrder = VibeVault.searchPref;
		for(int i = 0; i < VibeVault.searchChoices.length; i++){
			if (VibeVault.searchChoices[i].equals(searchOrder))
				searchPos = i;
		}
		searchSpin.setSelection(searchPos);
		searchSpin.setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> arg0, View view, int arg2, long arg3) {
				int selected = arg0.getSelectedItemPosition();
				VibeVault.searchPref = VibeVault.searchChoices[selected];
				if(VibeVault.searchPref.equals("Artist")&&artistSearchInput.getText().equals("")){
					artistSearchInput.setHint("Search Artists...");
				} else if (VibeVault.searchPref.equals("Show/Artist Description")&&artistSearchInput.getText().equals("")){
					artistSearchInput.setHint("Search Descriptions...");
				}
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
	 * Returns false if an improper date is passed.
	 */
	private boolean setDate(){
		String year = yearSearchInput.getText().toString();
		if(year.equals("")){
			VibeVault.yearSearchInt = -1;
			return true;
		}
			int yearInt = Integer.valueOf(year);
			if(yearInt >= 1800 && yearInt <= Calendar.getInstance().get(Calendar.YEAR)){
				VibeVault.yearSearchInt = yearInt;
				return true;
			} else{
				Toast.makeText(getBaseContext(), "Year must be between now and 1800...", Toast.LENGTH_SHORT).show();
				return false;
			}
	}
	
	private boolean isMoreSearch(String artist, String year){
		if(VibeVault.searchResults.size()==0){
			return false;
		}
		// Explanation:
		// If the artist input field is the same as the stored artist search text AND
		// The year input field is the same as the stored year search int (or if the input is blank, the int is -1),
		// Return true because you should be fetching more results.  Otherwise, false to search initially.
		if(artist.equalsIgnoreCase(VibeVault.artistSearchText)&&((year.equals("")&&VibeVault.yearSearchInt==-1)||((!year.equals(""))&&(Integer.valueOf(year)==VibeVault.yearSearchInt)))){
			return true;
		} else{
			return false;
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
		
		searchDrawer.setOnDrawerScrollListener(new OnDrawerScrollListener(){

			@Override
			public void onScrollEnded() {
			}

			@Override
			public void onScrollStarted() {
				vibrator.vibrate(50);
			}
			
		});
		artistSearchInput.addTextChangedListener(new TextWatcher(){

			@Override
			public void afterTextChanged(Editable s) {
				if(isMoreSearch(s.toString(),yearSearchInput.getText().toString())){
					searchButton.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.morebutton), null, null, null);
					searchButton.setText("More");
				} else{
					searchButton.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.searchbutton_plain), null, null, null);
					searchButton.setText("Search");
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {				
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}
			
		});
		yearSearchInput.addTextChangedListener(new TextWatcher(){

			@Override
			public void afterTextChanged(Editable s) {
				if(isMoreSearch(artistSearchInput.getText().toString(),s.toString())){
					searchButton.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.morebutton), null, null, null);
					searchButton.setText("More");
				} else{
					searchButton.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.searchbutton_plain), null, null, null);
					searchButton.setText("Search");
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}
			
		});
		searchDrawer.setOnDrawerOpenListener(new OnDrawerOpenListener(){
			@Override
			public void onDrawerOpened() {
				searchList.setBackgroundDrawable(getResources().getDrawable(R.drawable.backgrounddrawableblue));
				searchList.getBackground().setDither(true);
				searchList.setEnabled(false);
				handleText.setText("Search Panel");
				handleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
				artistSearchInput.setText(VibeVault.artistSearchText);

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
		        	if (!(artistSearchInput.getText().toString().equals(""))) {
						VibeVault.artistSearchText = artistSearchInput.getText().toString();
						if(setDate()){
							executeSearch(makeSearchURLString(1));
						    pageNum = 1;
								searchDrawer.close();
								return true;
						}
		        	}
		        }
		        return false;
		    }
		};
		this.artistSearchInput.setOnKeyListener(enterListener);
		
		this.searchButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				String query = artistSearchInput.getText().toString();
				// Blank
				if (query.equals("")) {
					vibrator.vibrate(50);
					Toast.makeText(getBaseContext(), "You need a query first...", Toast.LENGTH_SHORT).show();
					return;
				}
				// Search more
				else if (isMoreSearch(artistSearchInput.getText().toString(),yearSearchInput.getText().toString())) {
					searchButton.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.morebutton), null, null, null);
					searchButton.setText("More");
					dateModifierSpinner.setSelection(VibeVault.dateSearchModifierPos);
					// pageNum is incremented then searched with to get the next
					// page.
					executeSearch(makeSearchURLString(++pageNum));
					vibrator.vibrate(50);
					searchDrawer.close();
				}
				// New search
				else {
					searchButton.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.searchbutton_plain), null, null, null);
					searchButton.setText("Search");
					VibeVault.searchResults.clear();
					VibeVault.artistSearchText = artistSearchInput.getText().toString();
					if (setDate()) {
						// "1" is passed to retrieve page number 1.
						vibrator.vibrate(50);
						executeSearch(makeSearchURLString(1));
						pageNum = 1;
						searchDrawer.close();
					}
				}

			}
		});
		
		this.settingsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				launchSettingsDialog();
				vibrator.vibrate(50);
			}
		});
		
		this.clearButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				artistSearchInput.setText("");
				yearSearchInput.setText("");
				VibeVault.artistSearchText = "";
				VibeVault.yearSearchInt = -1;
				searchButton.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.searchbutton_plain), null, null, null);
				searchButton.setText("Search");
				VibeVault.searchResults.clear();
				refreshSearchList();
				vibrator.vibrate(50);
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
		artistSearchInput.setText(VibeVault.artistSearchText);
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
					dateModifier = "date:[1800-01-01%20TO%20" + VibeVault.yearSearchInt + "-01-01]%20AND%20";
					break;
				case 1:	//After
					int curDate = Calendar.getInstance().get(Calendar.DATE);
					int curMonth = Calendar.getInstance().get(Calendar.MONTH);
					int curYear = Calendar.getInstance().get(Calendar.YEAR);
					dateModifier = "date:[" + VibeVault.yearSearchInt + "-01-01%20TO%20" + curYear + "-" + String.format("%02d",curMonth) + "-" + String.format("%02d",curDate) + "]%20AND%20";
					break;
				case 2:	// In Year.
					dateModifier = "date:[" + VibeVault.yearSearchInt + "-01-01%20TO%20" + VibeVault.yearSearchInt + "-12-31]%20AND%20";
					break;
				}
			}
			// We search creator:(random's artist)%20OR%20creator(randoms artist) because
			// archive.org does not like apostrophes in the creator query.
			String specificSearch = "";
			if(VibeVault.searchPref.equals("Artist")){
				specificSearch = "(creator:(" + URLEncoder.encode(VibeVault.artistSearchText,"UTF-8") + ")" + "%20OR%20creator:(" + URLEncoder.encode(VibeVault.artistSearchText.replace("'", "").replace("\"", ""),"UTF-8") + "))";
			} else if(VibeVault.searchPref.equals("Show/Artist Description")){
				specificSearch = "(creator:(" + URLEncoder.encode(VibeVault.artistSearchText,"UTF-8") + ")" + "%20OR%20description:(" + URLEncoder.encode(VibeVault.artistSearchText.replace("'", "").replace("\"", ""),"UTF-8") + "))";
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
		searchList.setAdapter(new RatingsAdapter(this, R.layout.search_list_row, VibeVault.searchResults));
		if(VibeVault.searchResults.size()!=0){
			searchButton.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.morebutton), null, null, null);
			searchButton.setText("More");
			//clearButton.setEnabled(true);
		} else{
			searchDrawer.open();
			searchButton.setText("Search");
			searchButton.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.searchbutton_plain), null, null, null);
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
			if (show != null) {
				artistText.setText(show.getShowArtist() + " ");
				artistText.setSelected(true);
				showText.setText(show.getShowTitle());
				showText.setSelected(true);
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
			
			// This is 0 unless you are finding more results, in which case
			// we use this value later to set the ListView's position to the new results.
			int startVal = VibeVault.searchResults.size();
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
			if(startVal!=0){
				searchList.setSelection(startVal-1);
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
		private void setActivity(SearchScreen activity){
			this.parentScreen = activity;
			if(completed){
				notifyActivityTaskCompleted();
			}
		}
	}
}