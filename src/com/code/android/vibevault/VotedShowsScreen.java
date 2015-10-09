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
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

public class VotedShowsScreen extends Activity {
	
	private Spinner resultTypeSpinner;
	private ListView showList;
	private ArrayList<ArchiveShowObj> shows;
	private GetVotedShowsListTask workerTask;
	private boolean dialogShown;
	private int resultType = 1;
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.voted_shows_screen);
		
		resultTypeSpinner = (Spinner) findViewById(R.id.ShowSpinner);
		ArrayAdapter<String> resultAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,VibeVault.showResultTypes);
		resultAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		resultTypeSpinner.setAdapter(resultAdapter);

		String savedResultType = VibeVault.db.getPref("showResultType");
		for(int i = 0; i < VibeVault.showResultTypes.length; i++){
			if (VibeVault.showResultTypes[i].equals(savedResultType))
				resultType = i;
		}
		resultTypeSpinner.setSelection(resultType);
		resultTypeSpinner.setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> arg0, View view, int arg2, long arg3) {
				int selected = arg0.getSelectedItemPosition();
				if (selected != resultType){
					resultType = selected;
					VibeVault.db.updatePref("showResultType", VibeVault.showResultTypes[selected]);
					fetchVotedShows(selected+1);
				}
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
			
		});
		
		showList = (ListView) findViewById(R.id.VotedShowsListView);
		showList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> a, View v, int position, long id){
				openShow(id);
			}
		});
		
		@SuppressWarnings("unchecked")
		ArrayList<ArchiveShowObj> retained = (ArrayList<ArchiveShowObj>) getLastNonConfigurationInstance();
		if(retained == null){
			fetchVotedShows(resultType + 1);
		} else{
			shows = retained;
			refreshShowList();
		}
	}
	
	private void refreshShowList(){
		showList.setAdapter(new RatingsAdapter(this,
				R.layout.search_list_row, shows));
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
				dialog.setMessage("Retrieving Voted Shows...");
				return dialog;
			default:
				return super.onCreateDialog(id);	
		}
	}

	/** Bookkeeping method to deal with dialogs over orientation changes.
	*
	*/
	private void onTaskCompleted(){
		this.refreshShowList();
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
		return shows;
	}
	
	// ArrayAdapter for the ListView of shows with ratings.
	private class RatingsAdapter extends ArrayAdapter<ArchiveShowObj> {

		public RatingsAdapter(Context context, int textViewResourceId,
				List<ArchiveShowObj> objects) {
			super(context, textViewResourceId, objects);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ArchiveShowObj show = (ArchiveShowObj) showList
					.getItemAtPosition(position);
			
			if (convertView == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = vi.inflate(R.layout.voted_show_list_row, null);
			}
			TextView artistText = (TextView) convertView
					.findViewById(R.id.ArtistText);
			TextView showText = (TextView) convertView
					.findViewById(R.id.ShowText);
			TextView votesText = (TextView) convertView
					.findViewById(R.id.RatingText);
			if (show != null) {
				artistText.setText(show.getShowArtist());
				artistText.setSelected(true);
				showText.setText(show.getShowTitle());
				showText.setSelected(true);
				votesText.setText("Votes: " + show.getVotes() + " ");
			}
			return convertView;
		}
	}
	
	private void openShow(long pos){
		ArchiveShowObj show = shows.get((int) pos);
		if(show != null){
			Intent i = new Intent(VotedShowsScreen.this, ShowDetailsScreen.class);
			i.putExtra("Show", show);
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

	public void fetchVotedShows(int resultType){
		this.workerTask = new GetVotedShowsListTask(this);
		workerTask.execute(resultType);
	}
	
	private class GetVotedShowsListTask extends AsyncTask<Integer, String, Void> {
		
		private VotedShowsScreen parentScreen;
		
		private GetVotedShowsListTask(VotedShowsScreen activity){
			this.parentScreen = activity;
		}
		
		@Override
		protected void onPreExecute(){
			parentScreen.showDialog(VibeVault.RETRIEVING_VOTED_DIALOG_ID);
		}

		@Override
		protected Void doInBackground(Integer... res) {
			
			shows = Voting.getShows(res[0], 10, 0);
		return null;
	}
		
		@Override
		protected void onPostExecute(Void v){
			refreshShowList();
			if (shows.isEmpty()){
				Toast.makeText(getApplicationContext(), "No shows available. Check internet connection " +
						"or change sort type.", Toast.LENGTH_SHORT).show();
			}
			notifyActivityTaskCompleted();
		}
		
		private void notifyActivityTaskCompleted(){
			if(parentScreen!=null){
				parentScreen.onTaskCompleted();
			}
		}
		
	}
}
