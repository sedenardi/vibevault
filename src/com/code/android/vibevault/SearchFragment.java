/*
 * SearchFragment.java
 * VERSION X.
 * 
 * Copyright 2012 Andrew Pearson and Sanders DeNardi.
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Loader;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
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
import android.view.MenuItem.OnActionExpandListener;
import android.view.MenuItem.OnMenuItemClickListener;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.app.LoaderManager;

import com.code.android.vibevault.R;
import com.code.android.vibevault.SearchSettingsDialogFragment.SearchSettingsDialogInterface;

public class SearchFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<ArchiveShowObj>>, SearchSettingsDialogInterface {

	private static final String LOG_TAG = SearchFragment.class.getName();

	private static final int MENU_INFO = 0;
	private static final int MENU_BOOKMARK = 1;
	private static final int MENU_EMAIL = 2;
	
	protected ListView searchList;
	protected File appRootDir;
	protected TextView labelText;


	protected AutoCompleteTextView artistSearchInput;
	protected Button searchButton;
	protected Button searchMoreButton;
	protected TextView searchHintTextView;
	protected Menu actionBarMenu;
	
	// These are now initialized in onCreate().
	private int pageNum;
	private boolean isSearchMore = false;
	private boolean dateChanged = false;
	private ArrayList<ArchiveShowObj> searchResults;
	private String artistSearchText;
	private int dateSearchModifierPos;
	private int numResultsPref;
	private String sortPref;
	private int datePref;

	private StaticDataStore db;

	// Listeners for dialogs and for triggering a new Activity/Fragment when a
	// user clicks on a show.
	private SearchActionListener searchActionListener;
	private DialogAndNavigationListener dialogAndNavigationListener;

	// A parent activity will have to implement this callback interface.
	public interface SearchActionListener {
		public void onShowSelected(ArchiveShowObj show);
	}
	
	
	/** Overriden fragment lifecycle methods  */
	
	// Ensures parent activity implements proper interfaces.
	// Called right before onCreate(), which is right before onCreateView().
	// http://developer.android.com/guide/topics/fundamentals/fragments.html#Lifecycle
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			searchActionListener = (SearchActionListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement ActionListener");
		}
		try{
			dialogAndNavigationListener = (DialogAndNavigationListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement DialogListener");
		}
	}
	
	// Called when the fragment is first created.  According to the Android API,
	// "should initialize essential components of the fragment that you want
	// to retain when the fragment is paused or stopped, then resumed."
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		 
		searchResults = new ArrayList<ArchiveShowObj>();
		artistSearchText = "";
		datePref = 0;
		dateSearchModifierPos = 0;
		pageNum = 1;
		// Control whether a fragment instance is retained across Activity re-creation (such as from a configuration change).
		this.setRetainInstance(true);
		// Create the directory for our app if it don't exist.
		// FIXME should we move this elsewhere?  Maybe to the home screen?
		appRootDir = new File(Environment.getExternalStorageDirectory(),Downloading.APP_DIRECTORY);
		if (!appRootDir.isFile() || !appRootDir.isDirectory()) {
			if (Environment.getExternalStorageState().equals(
					Environment.MEDIA_MOUNTED)) {
				appRootDir.mkdirs();
			} else {
				Toast.makeText(
						getActivity(),
						"sdcard is unwritable...  is it mounted on the computer?",
						Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		 
		// Inflate the fragment and grab a reference to it.
		View v = inflater.inflate(R.layout.search_fragment, container, false);
		// Initialize the various elements of the SearchScreen.
		this.searchList = (ListView) v.findViewById(R.id.ResultsListView);
		this.searchButton = (Button) v.findViewById(R.id.SearchButton);
		searchList.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
			@Override
			public void onCreateContextMenu(ContextMenu menu, View v,
					ContextMenu.ContextMenuInfo menuInfo) {
				menu.add(Menu.NONE, MENU_EMAIL, Menu.NONE,
						"Email Link to Show");
				menu.add(Menu.NONE, MENU_INFO, Menu.NONE,
						"Show Info");
				menu.add(Menu.NONE, MENU_BOOKMARK,
						Menu.NONE, "Bookmark Show");
			}
		});
		searchList.setFooterDividersEnabled(false);
		int[] gradientColors = {0, 0xFF127DD4, 0};
		searchList.setDivider(new GradientDrawable(Orientation.RIGHT_LEFT, gradientColors));
		searchList.setDividerHeight(1);
		this.searchMoreButton = new Button(getActivity());
		this.searchMoreButton.setText("Get More Results");
		this.searchMoreButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
		this.searchMoreButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				 

				if (isMoreSearch(artistSearchInput.getText().toString())) {
					 
					((InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(artistSearchInput.getWindowToken(), 0);
					isSearchMore=true;
					// pageNum is incremented then searched with to get the next page.
					executeSearch(Searching.makeSearchURLString(++pageNum, datePref, artistSearchText,  numResultsPref, sortPref, dateSearchModifierPos));
					// vibrator.vibrate(50);
				}
			}		
		});
		searchList.addFooterView(searchMoreButton);
		if (searchResults.size() != 0) {
			this.searchMoreButton.setVisibility(View.VISIBLE);
		} else {
			this.searchMoreButton.setVisibility(View.GONE);
		}
		searchList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> a, View v, int position, long id) {
				ArchiveShowObj show = (ArchiveShowObj) searchList.getItemAtPosition(position);
				 
				searchActionListener.onShowSelected(show);
			}
		});
		
		return v;
	}
	
	// This method is called right after onCreateView() is called. "Called when the
	// fragment's activity has been created and this fragment's view hierarchy instantiated."
	// http://developer.android.com/guide/topics/fundamentals/fragments.html#Lifecycle
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
	}
	
	@Override
	public void onResume() {
		super.onResume();
		this.refreshSearchList();
	}

	@Override	
	public void onPause() {
		super.onPause();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
//		if(getActivity().getFragmentManager().getBackStackEntryCount()<1){
//			getActivity().finish();
//		}
	}
	
	@Override
	public void onStart() {
		super.onStart();
		db = StaticDataStore.getInstance(getActivity());
		 
		numResultsPref = Integer.valueOf(db.getPref("numResults"));
		sortPref = db.getPref("sortOrder");
		
		
		if(this.getArguments().containsKey("Artist")){
			String artist = this.getArguments().getString("Artist");
			if(artist!=null){
				this.browseArtist(artist);
				this.getArguments().remove("Artist");
			}
	}
		// Must call in order to get callback to onOptionsItemSelected()
		setHasOptionsMenu(true);
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
        getActivity().getActionBar().setTitle("Search");
		LoaderManager lm = this.getLoaderManager();
		if(lm.getLoader(0)!=null){
			// The second argument is the query for a new loader, but here we are trying to
			// reconnect to an already existing loader, and "If a loader already exists
			// (a new one does not need to be created), this parameter will be ignored and
			// the last arguments continue to be used.If a loader already exists (a new one
			// does not need to be created), this parameter will be ignored and the last arguments
			// continue to be used.
			// http://developer.android.com/reference/android/app/LoaderManager.html
			lm.initLoader(0, null, this);
		}
	}
	
	@Override
	public void onStop() {
		super.onStop();
	}	
	
	// Set ActionBar actions.
	@Override
	public boolean onOptionsItemSelected (MenuItem item){
		switch (item.getItemId()){
			case android.R.id.home:
				dialogAndNavigationListener.goHome();
				break;
			case R.id.HelpButton:
				dialogAndNavigationListener.showDialog(this.getResources().getString(R.string.search_screen_help), "Help");
				break;
			case R.id.SettingsButton:
				Bundle b = new Bundle();
				b.putString("order", this.sortPref);
				b.putInt("number", this.numResultsPref);
				b.putInt("date", this.datePref);
				b.putInt("datepos", this.dateSearchModifierPos);
				dialogAndNavigationListener.showSettingsDialog(b);
				break;
			default:
	            return super.onOptionsItemSelected(item);
		}
		return true;
	}
	

	
	// Must call in order to get callback to onOptionsItemSelected()
	// and thereby create an ActionBar.
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		if(!menu.hasVisibleItems()){
			 
			this.actionBarMenu = menu;
			inflater.inflate(R.menu.search_help_options, menu);
			MenuItem menuItem = menu.findItem(R.id.SearchActionBarButton);
			// Ridiculous code to automatically open the keyboard when the search button is pressed.
			menuItem.setOnMenuItemClickListener(new OnMenuItemClickListener(){
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					artistSearchInput.post(new Runnable(){
						@Override
						public void run() {
							 
							artistSearchInput.requestFocus();
							InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
							imm.showSoftInput(artistSearchInput, InputMethodManager.SHOW_IMPLICIT);
						}
					});
					return true;
				}	
			});

		final ImageButton b = (ImageButton)menu.findItem(R.id.SearchActionBarButton).getActionView().findViewById(R.id.SearchButton);
		artistSearchInput = (AutoCompleteTextView)menu.findItem(R.id.SearchActionBarButton).getActionView().findViewById(R.id.ArtistSearchBox);
		// Set the adapter for autocomplete.
		if (!db.getPref("artistUpdate").equals("2010-01-01")) {
			artistSearchInput.setAdapter(new ArrayAdapter<String>(getActivity(), R.layout.artist_search_row, db.getArtistsStrings()));
		}
		artistSearchInput.setText(artistSearchText);
		// This changes the appearance of the search button to reflect whether or not we are searching for "more" shows with the same query.
		artistSearchInput.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				if (isMoreSearch(s.toString())) {
					searchMoreButton.setVisibility(View.VISIBLE);
				} else {
					searchMoreButton.setVisibility(View.GONE);
				}
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}
			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				if(s.toString().contains("\n")){
					artistSearchInput.setText(s.toString().replace("\n", ""));
					b.callOnClick();
				}
			}
		});
		
		if (!db.getPref("artistUpdate").equals("2010-01-01")) {
			artistSearchInput.setAdapter(new ArrayAdapter<String>(getActivity(), R.layout.artist_search_row, db.getArtistsStrings()));
		}
		// Set listeners in the show details and artist search bars for the enter key.
		// This is not guaranteed to work for soft keyboards because they are IME devices (read online).
		// Analogous code is in the onTextChanged() method overriden in the artistSearchInput's
		// TextChangedListener.
		this.artistSearchInput.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				 
				if ((event != null)) {
						if((keyCode == KeyEvent.KEYCODE_ENTER)){
						 
						// Act like the search button has been pressed.
						return b.callOnClick();
					}
				}
				return false;
			}
		});
		b.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				// Blank
				if (artistSearchInput.getText().toString().equals("")) {
					// vibrator.vibrate(50);
					Toast.makeText(getActivity(), "You need a query first...", Toast.LENGTH_SHORT).show();
					return;
				}
				// Search more
				else if (isMoreSearch(artistSearchInput.getText().toString())) {
					 
					((InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(artistSearchInput.getWindowToken(), 0);

					isSearchMore=true;
					// pageNum is incremented then searched with to get the next page.
					executeSearch(Searching.makeSearchURLString(++pageNum, datePref, artistSearchText,  numResultsPref, sortPref, dateSearchModifierPos));
					// vibrator.vibrate(50);
				}
				// New search
				else {
					artistSearchText = artistSearchInput.getText().toString();
					isSearchMore=false;
					 
					((InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(artistSearchInput.getWindowToken(), 0);
					searchResults.clear();
					pageNum = 1;
					// "1" is passed to retrieve page number 1.
					// vibrator.vibrate(50);
					executeSearch(Searching.makeSearchURLString(pageNum, datePref, artistSearchText,  numResultsPref, sortPref, dateSearchModifierPos));
				}

			}
		});
		menuItem.setOnActionExpandListener(new OnActionExpandListener() {
	        @Override
	        public boolean onMenuItemActionCollapse(MenuItem item) {
	            return true;  // Return true to collapse action view
	        }
	        @Override
	        public boolean onMenuItemActionExpand(MenuItem item) {
	            return true;  // Return true to expand action view
	        }
	    });
		}
		// FIXME I really do not like having this here, but it is necessary in order to have
		// the ActionBar MenuItem for searching being expanded into its action layout by default.
		// Calling refreshSearchList() at other points in the code (like at the end of onActivityCreated()
		// or something), ends up being reached before the ActionBar is created, so that there is no
		// MenuItem to expand to the search action view.
		if(this.searchResults.size()==0){
			refreshSearchList();
		}
	}
	


	// private Vibrator vibrator;


	
	// Search and display the shows for a passed artist.
	public void browseArtist(String artist) {
		searchResults.clear();
		artistSearchText = artist;
			executeSearch(Searching.makeSearchURLString(pageNum++, datePref, artistSearchText,  numResultsPref, sortPref, dateSearchModifierPos));
	}



	private void executeSearch(String query) {
		dateChanged = false;
		Bundle b = new Bundle();
		b.putString("query", query);
		LoaderManager lm = this.getLoaderManager();
		if(lm.getLoader(0)!=null){
			// We already have a loader.
			 
			lm.restartLoader(0, b, this);
		} else{
			// We need a new loader.
			lm.initLoader(0, b, this);
		}
	}

//	/**
//	 * Returns true if a valid date, or no date at all, was passed. Returns
//	 * false if an improper date is passed.
//	 */
//	private boolean setDate() {
//		// FIXME This will need to be coordinated with the search settings.
//		String year = "";
//		if (year.equals("")) {
//			yearSearchInt = -1;
//			return true;
//		}
//		int yearInt = Integer.valueOf(year);
//		if (yearInt >= 1800 && yearInt <= Calendar.getInstance().get(Calendar.YEAR)) {
//			yearSearchInt = yearInt;
//			return true;
//		} else {
//			Toast.makeText(getActivity(), "Year must be between now and 1800...", Toast.LENGTH_SHORT).show();
//			return false;
//		}
//	}

	private boolean isMoreSearch(String artist) {
		if (searchResults.size() == 0) {
			return false;
		}
		// Explanation:
		// If the artist input field is the same as the stored artist search text AND
		// The year input field is the same as the stored year search int (or if
		// the input is blank, the int is -1),
		// Return true because you should be fetching more results. Otherwise,
		// false to search initially.
		if (!dateChanged) {
			if(artist.equalsIgnoreCase(artistSearchText)){
				 
				 
				return true;	
			} else{
				this.datePref=0;
				this.dateSearchModifierPos=SearchSettingsDialogFragment.ANYTIME;
				return false;
			}
		} else {
			 
			 
//			dateSearchModifierPos = 0;
//			this.datePref = 0;
			return false;
		}
	}

	// Pop up a loading dialog and pass the query to a SearchQueryAsyncTaskLoader for parsing.
	@Override
	public Loader<List<ArchiveShowObj>> onCreateLoader(int id, Bundle b) {
		 
		dialogAndNavigationListener.showLoadingDialog("Searching...");
		return (Loader) new SearchQueryAsyncTaskLoader(getActivity(), b.getString("query"));
	}

	// Set the search results to those returned by the loader, and refresh the search list.
	@Override
	public void onLoadFinished(Loader<List<ArchiveShowObj>> arg0, List<ArchiveShowObj> arg1) {
		 
		if(isSearchMore){
			isSearchMore=false;
			Parcelable state = this.searchList.onSaveInstanceState();
			searchResults.addAll(arg1);
			this.refreshSearchList();
			this.searchList.onRestoreInstanceState(state);
		} else if(!searchResults.containsAll(searchResults)||searchResults.isEmpty()){
			// The containsAll() call above is necessary because onLoadFinished() is being called on rotations,
			// and otherwise it will replace the searchResults with whatever the Loader most recently returned.
			searchResults = (ArrayList<ArchiveShowObj>) arg1;
			this.refreshSearchList();
		}
		dialogAndNavigationListener.hideDialog();
		if (searchResults.size() != 0) {
			this.searchMoreButton.setVisibility(View.VISIBLE);
			this.searchMoreButton.setEnabled(true);

		} else {
			Toast.makeText(getActivity(), "Sorry, no search results.", Toast.LENGTH_SHORT).show();
			this.searchMoreButton.setVisibility(View.GONE);
			this.searchMoreButton.setEnabled(false);

		}
		
	}

	@Override
	public void onLoaderReset(Loader<List<ArchiveShowObj>> arg0) {
		// FIXME.
	}

	private void refreshSearchList() {
		 
		searchList.setAdapter(new RatingsAdapter(getActivity(), R.layout.search_list_row, searchResults));
		if(searchResults.isEmpty()){
			 
			if(actionBarMenu!=null){
				 
				actionBarMenu.findItem(R.id.SearchActionBarButton).expandActionView();
				artistSearchInput.requestFocus();
				InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(artistSearchInput, InputMethodManager.SHOW_IMPLICIT);
			}
//			getActivity().getActionBar()
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
				LayoutInflater vi = (LayoutInflater) getActivity()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = vi.inflate(R.layout.search_list_row, null);
			}
			TextView artistText = (TextView) convertView
					.findViewById(R.id.ArtistText);
			TextView showText = (TextView) convertView
					.findViewById(R.id.ShowText);
			TextView sourceText = (TextView) convertView.findViewById(R.id.SourceText);
			ImageView ratingsIcon = (ImageView) convertView
					.findViewById(R.id.rating);
			if (show != null) {
				artistText.setText(show.getShowArtist() + " ");
				artistText.setSelected(true);
				artistText.setSingleLine();
				showText.setText(show.getShowTitle());
				showText.setSelected(true);
				showText.setSingleLine();
				sourceText.setText(show.getSource());
				sourceText.setSelected(true);
				sourceText.setSingleLine();
				switch ((int) show.getRating()) {
				case 1:
					ratingsIcon.setImageDrawable(getActivity().getResources()
							.getDrawable(R.drawable.star1));
					break;
				case 2:
					ratingsIcon.setImageDrawable(getActivity().getResources()
							.getDrawable(R.drawable.star2));
					break;
				case 3:
					ratingsIcon.setImageDrawable(getActivity().getResources()
							.getDrawable(R.drawable.star3));
					break;
				case 4:
					ratingsIcon.setImageDrawable(getActivity().getResources()
							.getDrawable(R.drawable.star4));
					break;
				case 5:
					ratingsIcon.setImageDrawable(getActivity().getResources()
							.getDrawable(R.drawable.star5));
					break;
				default:
					ratingsIcon.setImageDrawable(getActivity().getResources()
							.getDrawable(R.drawable.star0));
					break;
				}
			}
			return convertView;
		}
	}


	@Override
	public void onSettingsOkayButtonPressed(String sortType, int numResults, int dateResults, int datePos) {
		 
		 

		this.sortPref = sortType;
		this.numResultsPref = numResults;
		if(datePref!=dateResults || dateSearchModifierPos != datePos){
			 
			 
			this.datePref = dateResults;
			this.dateSearchModifierPos = datePos;
			dateChanged = true;
			this.searchMoreButton.setEnabled(false);
		} else {
			dateChanged = false;
			this.searchMoreButton.setEnabled(true);
		}
	}

}