/*
 * ShowDetailsFragment.java
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.code.android.vibevault.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ShowDetailsFragment extends Fragment  {

	protected static final String LOG_TAG = ShowDetailsFragment.class.getName();
	
	private ArrayList<ArchiveSongObj> showSongs;
	
	private TextView showLabel;
	private ListView trackList;

	private String showTitle;
	
	private ArchiveShowObj show;
	
	private ShareActionProvider mShareActionProvider;

	private StaticDataStore db;
	
	private DialogAndNavigationListener dialogAndNavigationListener;
	private ShowDetailsActionListener showDetailsActionListener;
	
	public interface ShowDetailsActionListener{
		public void playShow(int pos, ArrayList<ArchiveSongObj> showSongs);
	}
	
	// Called right before onCreate(), which is right before onCreateView().
	// http://developer.android.com/guide/topics/fundamentals/fragments.html#Lifecycle
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try{
			dialogAndNavigationListener = (DialogAndNavigationListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement DialogListener");
		}
		try{
			showDetailsActionListener = (ShowDetailsActionListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement ShowDetailsActionListener");
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Control whether a fragment instance is retained across Activity re-creation (such as from a configuration change).
		this.setRetainInstance(true);
		
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
        getActivity().getActionBar().setTitle("Show Details");
		db = StaticDataStore.getInstance(getActivity());

	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the fragment and grab a reference to it.
		View v = inflater.inflate(R.layout.show_details_fragment, container, false);
		// Initialize the show's label and its list of tracks.  Set the appropriate listeners.
		showLabel = (TextView) v.findViewById(R.id.ShowLabel);
		trackList = (ListView) v.findViewById(R.id.SongsListView);
		int[] gradientColors = {0, 0xFF127DD4, 0};
		trackList.setDivider(new GradientDrawable(Orientation.RIGHT_LEFT, gradientColors));
		trackList.setDividerHeight(1);
		trackList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> a, View v, int position,	long id) {
				playShow(position);
			}
		});
		// If, for some reason, we already have links, show them.
		if(showSongs!=null && !showSongs.isEmpty()){
			refreshTrackList();
		}
		return v;
	}
	
	// This method is called right after onCreateView() is called.  "Called when the
	// fragment's activity has been created and this fragment's view hierarchy instantiated."
	// http://developer.android.com/guide/topics/fundamentals/fragments.html#Lifecycle
	@Override
	public void onActivityCreated(Bundle savedInstanceState){
	    super.onActivityCreated(savedInstanceState);
	    
	    // Must call in order to get callback to onOptionsItemSelected() and thereby create an ActionBar.
        setHasOptionsMenu(true);
        getActivity().getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        getActivity().getActionBar().setListNavigationCallbacks(null, null);
		getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
		getActivity().getActionBar().setTitle("Show Details");
	    // If this ShowDetailsFragment has an argument (it should be the passed show), grab it and parse it.
		if(this.getArguments()!=null){
			ArchiveShowObj passedShow = (ArchiveShowObj)this.getArguments().getSerializable("show");
			// If This ShowDetailsFragment's show object is null, set it to the passed show.
			// FIXME Maybe put in a check for a null passedShow?
			if(show==null){
				Logging.Log(LOG_TAG, "SHOW IS NULL.");
				if(showSongs==null){
					showSongs = new ArrayList<ArchiveSongObj>();
				}
				if(passedShow!=null){
					show = passedShow;
					Logging.Log(LOG_TAG, "SHOW PASSED: " + show.getIdentifier());
				}
				if(show!=null){
					Logging.Log(LOG_TAG,"Show passed, checking tracks.");
					Logging.Log(LOG_TAG, show.getIdentifier());
					getShowTracks(show);
				} else{
					return;
				}
			}
			// Otherwise, if the ShowDetailsFragment's show is the same as the passed
			// (non-null) show, make sure that its songs list isn't null.  If the shows
			// are different, start parsing the new show.
			else {
				if(show.equals(passedShow)){
					if(showSongs==null){
						getShowTracks(show);
					} else {
						refreshTrackList();
					}
				} else {
					refreshTrackList();
					getShowTracks(show);
				}
			}
		}
	}

//	// Get a show's details.
//	private void executeShowDetailsTask(ArchiveShowObj show) {
//		Bundle b = new Bundle();
//		b.putSerializable("show", show);
//		LoaderManager lm = this.getLoaderManager();
//		if(lm.getLoader(1)!=null){
//			// We already have a loader.
//			lm.restartLoader(1, b, this);
//		} else{
//			// We need a new loader.
//			lm.initLoader(1, b, this);
//		}
//	}


	public static ArrayList<ArchiveSongObj> parseShowJSON(JSONObject json){
		ArrayList<ArchiveSongObj> songs = new ArrayList<ArchiveSongObj>();
		String songTitle = "";
		String songLink = "";
		String showTitle = "";
		String showIdent = "";
		String showArtist = "";
		int trackNo = 0;

		Logging.Log(LOG_TAG, "Attempting to parse show JSON...");
		try {
			JSONObject showMetadata = json.getJSONObject("metadata");
			showIdent = showMetadata.getString("identifier");

			// Get the MP3 files listed in the JSON response.
			// TODO Add in logic for other codecs.
			JSONArray showArray = json.getJSONArray("files");
			JSONObject showObject = new JSONObject();
			Logging.Log(LOG_TAG, "Number of files: " + showArray.length());
			for(int i = 0; i < showArray.length(); i ++){
				showObject = showArray.getJSONObject(i);
				if(showObject.has("format") && showObject.getString("format").equals("VBR MP3")){
					songTitle = "";
					songLink = "";
					showTitle = "";
					if(showObject.has("title")) songTitle = showObject.getString("title");
					if(showObject.has("name")) songLink = "http://archive.org/download/" + showIdent + "/" + showObject.getString("name");
					if(showObject.has("album")) showTitle = showObject.getString("album");
					if(showObject.has("track")) trackNo = showObject.getInt("track");
					if(showObject.has("creator")) showArtist = showObject.getString("creator");
					Logging.Log(LOG_TAG, "Retrieved: " + trackNo + " " + songTitle + " from " + showTitle);
					ArchiveSongObj song = new ArchiveSongObj(songTitle, songLink, showTitle, showIdent, showArtist);
					songs.add(song);
				}
			}
			// If we didn't collect any songs...
			if(songs.isEmpty()){
				Logging.Log(LOG_TAG, "Sorry, no mp3's for this show.  This might be because the show was recently released commercially.");
				return songs;
			}

			// Archive's JSON doesn't always list the tracks in order.
			// Sort by filename to get proper song order.
			Collections.sort(songs, new Comparator<ArchiveSongObj>() {
				@Override
				public int compare(ArchiveSongObj song1, ArchiveSongObj song2) {
					return song1.getFileName().compareTo(song2.getFileName());
				}
			});
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return songs;
	}

	private void getShowTracks(final ArchiveShowObj passedShow) {
		if(db.getShowExists(passedShow)){
			Logging.Log(LOG_TAG, "Show exists.  Setting title to: " + db.getShow(passedShow.getIdentifier()).getArtistAndTitle());
			showSongs.addAll(db.getSongsFromShow(passedShow.getIdentifier()));
			show.setFullTitle(db.getShow(passedShow.getIdentifier()).getArtistAndTitle());
			return;
		}

		Logging.Log(LOG_TAG, "getShowTracks() called.  Attempting to fetch show JSON from archive.");
		dialogAndNavigationListener.showLoadingDialog("Loading show...");
		JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET, "https://archive.org/metadata/" + passedShow.getIdentifier(), null,
				new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						Logging.Log(LOG_TAG, "Size of JSON response: " + response.toString().length());

						showSongs = parseShowJSON(response);
						show = passedShow;
						if(!showSongs.isEmpty()){
							db.insertShow(show);
							db.setShowExists(show);
							db.insertRecentShow(show);
							for(ArchiveSongObj song : showSongs){
								db.insertSong(song);
							}

						}

						refreshTrackList();
						dialogAndNavigationListener.hideDialog();
					}
				},
				new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {

					}
				});
		RequestQueueSingleton.getInstance(this.getActivity().getApplicationContext()).addToRequestQueue(jsObjRequest);
	}


	
//	// Pop up a loading dialog and pass the show to a ShowDetailsAsyncTaskLoader for parsing.
//	@Override
//	public Loader<Bundle> onCreateLoader(int id, Bundle args) {
//		//getShows((ArchiveShowObj) args.get("show"));
//		Logging.Log(LOG_TAG, "NEW LOADER.");
//		dialogAndNavigationListener.showLoadingDialog("Loading show...");
//		return (Loader<Bundle>) new ShowDetailsAsyncTaskLoader(getActivity(), (ArchiveShowObj) args.get("show"));
//	}
//
//	// Set the show's songs to those returned by the loader, and refresh the track list.
//	@SuppressWarnings("unchecked")
//	@Override
//	public void onLoadFinished(Loader<Bundle> arg0, Bundle arg1) {
//		Logging.Log(LOG_TAG, "LOADER FINISHED.");
//		showSongs = (ArrayList<ArchiveSongObj>) arg1.getSerializable("songs");
//		show = (ArchiveShowObj) arg1.getSerializable("show");
//		Logging.Log(LOG_TAG, show.getArtistAndTitle());
//		if(this.show.getShowTitle().length()<2){
//			Logging.Log(LOG_TAG, "BLANK: " + ((ArchiveShowObj)arg1.getSerializable("show")).getShowTitle());
//			this.showLabel.setText(((ArchiveShowObj)arg1.getSerializable("show")).getShowTitle());
//		}
//		refreshTrackList();
//		dialogAndNavigationListener.hideDialog();
//		Intent i = new Intent(Intent.ACTION_SEND);
//		i.setType("text/plain");
//		// Add data to the intent, the receiving app will decide what to do with it.
//		i.putExtra(Intent.EXTRA_SUBJECT, "Vibe Vault");
//		i.putExtra(Intent.EXTRA_TEXT, show.getShowArtist() + " " + show.getShowTitle() + " " + show.getShowURL()
//				+ "\n\nSent using #VibeVault for Android.");
//		if(mShareActionProvider!=null){
//			mShareActionProvider.setShareIntent(i);
//		}
//	}
//
//	@Override
//	public void onLoaderReset(Loader<Bundle> arg0) {
//	}
	
	public ArchiveShowObj getShow(){
		return show;
	}

	private void refreshScreenTitle() {
		if(show!=null){
			showTitle = show.getArtistAndTitle();
			showLabel.setText(showTitle);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		Logging.Log(LOG_TAG,"ONRESUME.");
		refreshTrackList();
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		menu.clear();
		inflater.inflate(R.menu.help_bookmark_share_download_vote, menu);
		MenuItem item = menu.findItem(R.id.ShareButton);
	    mShareActionProvider = (ShareActionProvider) item.getActionProvider();
//	    mShareActionProvider.setShareHistoryFileName(null);
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	// These are the callbacks for the ActionBar buttons.
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.VoteButton:
			if (!showSongs.isEmpty()) {
				new VoteTask().execute(show);
			} else {
				Toast.makeText(getActivity(), R.string.error_empty_show_vote, Toast.LENGTH_SHORT).show();
			}
			return true;
		case R.id.BookmarkButton:
			Toast.makeText(getActivity(), R.string.confirm_bookmarked_message_text, Toast.LENGTH_SHORT).show();
			db.insertFavoriteShow(show);
			return true;
		case R.id.HelpButton:
			dialogAndNavigationListener.showDialog(this.getResources().getString(R.string.show_details_screen_help), "Help");
			return true;
		case R.id.DownloadButton:
			DownloadingAsyncTask task = new DownloadingAsyncTask(getActivity());
			task.execute(showSongs.toArray(new ArchiveSongObj[showSongs.size()]));
			return true;
		case android.R.id.home:
			dialogAndNavigationListener.goHome();
		default:
			return false;
		}
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onDetach() {
	    super.onDetach();
	    Logging.Log(LOG_TAG, "DETACHING.");
	    // FIXME this should now cancel the loader.
	    // VibeVault.showDetailsTask.cancel(true);
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
	}
	
	@Override
	public void onStop() {
		super.onStop();
	}
	
	private void playShow(int pos) {
		showDetailsActionListener.playShow(pos, showSongs);
	}

	/**
	 * Refresh the track list with whatever data List of songs contains.
	 * 
	 */
	private void refreshTrackList() {
		Logging.Log(LOG_TAG, String.valueOf(showSongs.size()));
		trackList.setAdapter(new SongAdapter(getActivity(), R.layout.show_details_screen_row, showSongs, db));
		refreshScreenTitle();
	}

	private class VoteTask extends AsyncTask<ArchiveShowObj, Void, String> {
		@Override
		protected void onPreExecute() {
			Toast.makeText(getActivity(), R.string.confirm_voting_message_text, Toast.LENGTH_SHORT).show();
		}

		@Override
		protected String doInBackground(ArchiveShowObj... shows) {
			return Voting.vote(shows[0], db);
		}

		@Override
		protected void onPostExecute(String result) {
			Toast.makeText(getActivity(), result, Toast.LENGTH_SHORT).show();
		}
	}
}