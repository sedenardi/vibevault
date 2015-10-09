package com.code.android.vibevault;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

public class VotedArtistsScreen extends Activity {
	

	private Spinner resultTypeSpinner;
	private ListView artistList;
	private ArrayList<ArchiveArtistObj> artists;
	private GetVotedArtistsListTask workerTask;
	private boolean dialogShown;
	private int resultType = 1;
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.voted_artists_screen);
		
		resultTypeSpinner = (Spinner) findViewById(R.id.ArtistSpinner);
		ArrayAdapter<String> resultAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,VibeVault.artistResultTypes);
		resultAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		resultTypeSpinner.setAdapter(resultAdapter);

		String savedResultType = VibeVault.db.getPref("artistResultType");
		for(int i = 0; i < VibeVault.artistResultTypes.length; i++){
			if (VibeVault.artistResultTypes[i].equals(savedResultType))
				resultType = i;
		}
		resultTypeSpinner.setSelection(resultType);
		resultTypeSpinner.setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> arg0, View view, int arg2, long arg3) {
				int selected = arg0.getSelectedItemPosition();
				if (selected != resultType){
					resultType = selected;
					VibeVault.db.updatePref("artistResultType", VibeVault.artistResultTypes[selected]);
					fetchVotedArtists(selected+1);
				}
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
			
		});
		
		artistList = (ListView) findViewById(R.id.VotedArtistsListView);
		artistList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> a, View v, int position, long id){
				openShowsByArtist(position);
			}
		});
		
		@SuppressWarnings("unchecked")
		ArrayList<ArchiveArtistObj> retained = (ArrayList<ArchiveArtistObj>) getLastNonConfigurationInstance();
		if(retained == null){
			fetchVotedArtists(resultType + 1);
		} else{
			artists = retained;
			refreshArtistList();
		}		
	}
	
	private void refreshArtistList(){
		artistList.setAdapter(new RatingsAdapter(this,
				R.layout.search_list_row, artists));
	}
	
	/** Dialog preparation method.
	*
	* Includes Thread bookkeeping to prevent not leaking Views on orientation changes.
	*/
	@Override
	protected void onPrepareDialog(int id, Dialog dialog){
		super.onPrepareDialog(id, dialog);
		if(id==VibeVault.RETRIEVING_VOTED_DIALOG_ID){
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
			case VibeVault.RETRIEVING_VOTED_DIALOG_ID:
				
				ProgressDialog dialog = new ProgressDialog(this);
				dialog.setMessage("Retrieving Voted Artists...");
				return dialog;
			default:
				return super.onCreateDialog(id);	
		}
	}

	/** Bookkeeping method to deal with dialogs over orientation changes.
	*
	*/
	private void onTaskCompleted(){
		this.refreshArtistList();
		if(dialogShown){
			try{
				dismissDialog(VibeVault.RETRIEVING_VOTED_DIALOG_ID);
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
		return artists;
	}
	
	private void openShowsByArtist(int pos){
		ArchiveArtistObj artist = artists.get((int) pos);
		if(artist != null){
			Intent i = new Intent(VotedArtistsScreen.this, VotedShowsByArtistScreen.class);
			i.putExtra("artistName", artist.getArtistName());
			i.putExtra("artistId", artist.getArtistId());
			startActivity(i);
		}
	}
	
	@Override
	public void onPause(){
		super.onPause();
	}
	
	@Override
	public void onResume(){
		super.onResume();
	}

	public void fetchVotedArtists(int resultType){
		this.workerTask = new GetVotedArtistsListTask(this);
		workerTask.execute(resultType);
	}
	private class GetVotedArtistsListTask extends AsyncTask<Integer, String, Void> {
		
		private VotedArtistsScreen parentScreen;
		
		private GetVotedArtistsListTask(VotedArtistsScreen activity){
			this.parentScreen = activity;
		}
		
		@Override
		protected void onPreExecute(){
			parentScreen.showDialog(VibeVault.RETRIEVING_VOTED_DIALOG_ID);
		}

		@Override
		protected Void doInBackground(Integer... res) {
			
			artists = Voting.getArtists(res[0], 10, 0);
		return null;
	}
		
		@Override
		protected void onPostExecute(Void v){
			refreshArtistList();
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
		
	}
	
	// ArrayAdapter for the ListView of shows with ratings.
	private class RatingsAdapter extends ArrayAdapter<ArchiveArtistObj> {

		public RatingsAdapter(Context context, int textViewResourceId,
				List<ArchiveArtistObj> objects) {
			super(context, textViewResourceId, objects);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ArchiveArtistObj artist = (ArchiveArtistObj) artistList
					.getItemAtPosition(position);
			
			if (convertView == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = vi.inflate(R.layout.voted_show_list_row, null);
			}
			TextView artistText = (TextView) convertView
					.findViewById(R.id.ArtistText);
			TextView voteText = (TextView) convertView
					.findViewById(R.id.ShowText);
			TextView votesText = (TextView) convertView
				.findViewById(R.id.RatingText);
			if (artist != null) {
				artistText.setText(artist.getArtistName() + " ");
				artistText.setSelected(true);
				voteText.setText("Last voted: " + artist.getVoteTime());
				votesText.setText("Votes: " + artist.getVotes() + " ");
				
			}
			return convertView;
		}
	}
}
