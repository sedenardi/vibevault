package com.code.android.vibevault;

import java.util.ArrayList;

import com.code.android.vibevault.SearchFragment.SearchActionListener;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;

public class VotesFragment extends Fragment implements LoaderManager.LoaderCallbacks<ArrayList<?>>, ActionBar.OnNavigationListener {
	
	protected static final String LOG_TAG = VotesFragment.class.getName();
	private DialogAndNavigationListener dialogAndNavigationListener;
	private ListView votedList;
	private ArrayList<ArchiveVoteObj> votes;
	protected Button moreButton;
	private int offset = 0;
	private int voteType = Voting.VOTES_SHOWS;
	private int voteResultType = Voting.VOTES_NEWEST_VOTED;
	private int numResults = 10;
	private int artistId = -1;
	private int currentSelectedMode = -1;
	
	private ShareActionProvider mShareActionProvider;
	private StaticDataStore db;
	
	private boolean moreResults = false;
	
	private VotesActionListener votesActionListener;
	private SearchActionListener searchActionListener;
	
	public interface VotesActionListener {
		public void openArtistShowList(ArchiveArtistObj artist);
	}
	
	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		if (voteType != Voting.VOTES_SHOWS_BY_ARTIST) {
			switch(itemPosition){
				case 0:
					this.voteType = Voting.VOTES_SHOWS;
					this.voteResultType = Voting.VOTES_NEWEST_VOTED;
					break;
				case 1:
					this.voteType = Voting.VOTES_SHOWS;
					this.voteResultType = Voting.VOTES_ALL_TIME;
					break;
				case 2:
					this.voteType = Voting.VOTES_SHOWS;
					this.voteResultType = Voting.VOTES_WEEKLY;
					break;
				case 3:
					this.voteType = Voting.VOTES_SHOWS;
					this.voteResultType = Voting.VOTES_DAILY;
					break;
				case 4:
					this.voteType = Voting.VOTES_ARTISTS;
					this.voteResultType = Voting.VOTES_NEWEST_VOTED;
					break;
				case 5:
					this.voteType = Voting.VOTES_ARTISTS;
					this.voteResultType = Voting.VOTES_ALL_TIME;
					break;
				case 6:
					this.voteType = Voting.VOTES_ARTISTS;
					this.voteResultType = Voting.VOTES_WEEKLY;
					break;
				case 7:
					this.voteType = Voting.VOTES_ARTISTS;
					this.voteResultType = Voting.VOTES_DAILY;
					break;
				default:
					this.voteType = Voting.VOTES_SHOWS;
					this.voteResultType = Voting.VOTES_NEWEST_VOTED;
					return false;
			}
		} else {
			voteType = Voting.VOTES_SHOWS_BY_ARTIST;
			switch(itemPosition) {
				case 0:
					voteResultType = Voting.VOTES_NEWEST_VOTED;
					break;
				case 1:
					voteResultType = Voting.VOTES_ALL_TIME;
					break;
				case 2:
					voteResultType = Voting.VOTES_WEEKLY;
					break;
				case 3:
					voteResultType = Voting.VOTES_DAILY;
					break;
				default:
					voteResultType = Voting.VOTES_NEWEST_VOTED;
					break;
			}
		}
		 
		if(currentSelectedMode!=itemPosition){
			executeRefresh();
			currentSelectedMode=itemPosition;
			moreResults = false;
			offset = 0;
		}
		return true;
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
			votesActionListener = (VotesActionListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement VotesActionListener");
		}
		try{
			searchActionListener = (SearchActionListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement ShowDetailsActionListener");
		}
		
	}
	
	// Called when the fragment is first created.  According to the Android API,
	// "should initialize essential components of the fragment that you want
	// to retain when the fragment is paused or stopped, then resumed."
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		 
		votes = new ArrayList<ArchiveVoteObj>();
		// Control whether a fragment instance is retained across Activity re-creation (such as from a configuration change).
		this.setRetainInstance(true);
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
	}
	
	@Override
	public void onStop() {
		super.onStop();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		 
		// Inflate the fragment and grab a reference to it.
		View v = inflater.inflate(R.layout.votes_fragment, container, false);
		this.votedList = (ListView) v.findViewById(R.id.VotesListView);
		votedList.setFooterDividersEnabled(false);
		int[] gradientColors = {0, 0xFF127DD4, 0};
		votedList.setDivider(new GradientDrawable(Orientation.RIGHT_LEFT, gradientColors));
		votedList.setDividerHeight(1);
		this.moreButton = new Button(getActivity());
		this.moreButton.setText("More");
		this.moreButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
		this.moreButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				offset = votedList.getCount();
				moreResults = true;
				executeRefresh();
			}
			
		});
		votedList.addFooterView(moreButton);
		moreButton.setVisibility(this.votes.size()>0?View.VISIBLE:View.GONE);		
		this.votedList.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> a, View v, int position, long id) {
				Object o = a.getAdapter().getItem(position);
				if(o != null){
					if(o instanceof ArchiveShowObj){
						searchActionListener.onShowSelected(((ArchiveShowObj)o));
					}
					else if(o instanceof ArchiveArtistObj){
						votesActionListener.openArtistShowList((ArchiveArtistObj)o);
					}
				}
			}
		});
		return v;
	}
	
	private void executeRefresh(){
		if(voteType==-1){
			return;
		}
		LoaderManager lm = getLoaderManager();
		Bundle b = new Bundle();
		b.putIntArray("queryArray", new int[] {voteType, voteResultType, numResults, offset, artistId});
		lm.restartLoader(2, b, VotesFragment.this);
	}
	
	private void openShowsByArtist(ArchiveArtistObj artist){
		if(artist != null){
			this.voteType = Voting.VOTES_SHOWS_BY_ARTIST;
			this.voteResultType = Voting.VOTES_ALL_TIME;
			this.artistId = artist.getArtistId();
	        getActivity().getActionBar().setDisplayHomeAsUpEnabled(false);
	        getActivity().getActionBar().setTitle("Shows By Artist");
	        getActivity().getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
	        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(), R.array.voting_by_artist, android.R.layout.simple_spinner_dropdown_item);
	        getActivity().getActionBar().setListNavigationCallbacks(adapter, this);
		}
	}
	
	// This method is called right after onCreateView() is called. "Called when the
	// fragment's activity has been created and this fragment's view hierarchy instantiated."
	// http://developer.android.com/guide/topics/fundamentals/fragments.html#Lifecycle
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		// Must call in order to get callback to onOptionsItemSelected()
		setHasOptionsMenu(true);
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
        getActivity().getActionBar().setTitle("Top Voted");
        getActivity().getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(), R.array.voting_array, android.R.layout.simple_spinner_dropdown_item);
        getActivity().getActionBar().setListNavigationCallbacks(adapter, this);		        
		Bundle b = new Bundle();
		b.putIntArray("queryArray", new int[] {this.voteType, this.voteResultType, numResults, this.offset, artistId});
		
		if(voteType == -1){
			return;
		}

	}
	
	// Set ActionBar actions.
	@Override
	public boolean onOptionsItemSelected (MenuItem item){
		switch (item.getItemId()){
			case android.R.id.home:
				dialogAndNavigationListener.goHome();
				break;
			case R.id.HelpButton:
				dialogAndNavigationListener.showDialog(this.getResources().getString(R.string.voting_screen), "Help");
				break;
			default:
				break;
		}
        return super.onOptionsItemSelected(item);
	}
	
	// Must call in order to get callback to onOptionsItemSelected()
	// and thereby create an ActionBar.
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		if(!menu.hasVisibleItems()){
		inflater.inflate(R.menu.help, menu);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Loader<ArrayList<?>> onCreateLoader(int id, Bundle args) {
		this.dialogAndNavigationListener.showLoadingDialog("Getting votes...");
		int[] queryVals = args.getIntArray("queryArray");
		return (Loader) new VotesQueryAsyncTaskLoader(getActivity(), queryVals[0], queryVals[1], queryVals[2], queryVals[3], queryVals[4]);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onLoadFinished(Loader<ArrayList<?>> arg0, ArrayList<?> arg1) {
		this.dialogAndNavigationListener.hideDialog();
		 
		if (moreResults) {
			Parcelable state = this.votedList.onSaveInstanceState();
			this.votes.addAll((ArrayList<ArchiveVoteObj>)arg1);
			this.refreshVoteList();
			this.votedList.onRestoreInstanceState(state);
			moreResults=false;
		} else {
			this.votes = (ArrayList<ArchiveVoteObj>) arg1;
			this.refreshVoteList();

		}
		CharSequence title = getActivity().getActionBar().getTitle();
		if (voteType == Voting.VOTES_SHOWS_BY_ARTIST && title == "Top Voted") {
	        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
	        getActivity().getActionBar().setTitle("Shows By Artist");
	        getActivity().getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
	        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(), R.array.voting_by_artist, android.R.layout.simple_spinner_dropdown_item);
	        getActivity().getActionBar().setListNavigationCallbacks(adapter, this);		
		} else if (voteType != Voting.VOTES_SHOWS_BY_ARTIST && title == "Shows By Artist") {
	        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
	        getActivity().getActionBar().setTitle("Top Voted");
	        getActivity().getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
	        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(), R.array.voting_array, android.R.layout.simple_spinner_dropdown_item);
	        getActivity().getActionBar().setListNavigationCallbacks(adapter, this);	
		}
		moreButton.setVisibility(this.votes.size()>0?View.VISIBLE:View.GONE);

	}
	
	@Override
	public void onResume() {
		super.onResume();
		this.refreshVoteList();
		if(this.getArguments()!=null&&this.getArguments().containsKey("ArchiveArtist")){
			ArchiveArtistObj artist = (ArchiveArtistObj)this.getArguments().get("ArchiveArtist");
			if(artist!=null){
				this.openShowsByArtist(artist);
				this.getArguments().remove("ArchiveArtist");
			}
		}
	}

	@Override	
	public void onPause() {
		super.onPause();
	}
	
	private void refreshVoteList(){
		this.votedList.setAdapter(new VoteAdapter(getActivity(), R.layout.voted_show_list_row, votes));

	}

	@Override
	public void onLoaderReset(Loader<ArrayList<?>> arg0) {
		// TODO Auto-generated method stub
		
	}
	
	private class VoteAdapter extends ArrayAdapter<ArchiveVoteObj> {
				
		public VoteAdapter(Context context, int textViewResourceId, ArrayList<ArchiveVoteObj> votes){
			super(context, textViewResourceId, votes);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Object voteObject = getItem(position);
			if(voteObject instanceof ArchiveArtistObj){
				if (convertView == null) {
					LayoutInflater vi = (LayoutInflater) getActivity()
							.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					convertView = vi.inflate(R.layout.voted_show_list_row, null);
				}
				
				TextView artistText = (TextView) convertView.findViewById(R.id.ArtistText);
				TextView showText = (TextView) convertView.findViewById(R.id.ShowText);
				TextView votesText = (TextView) convertView.findViewById(R.id.RatingText);
				artistText.setVisibility(View.VISIBLE);
				votesText.setVisibility(View.VISIBLE);
				artistText.setText((ArchiveArtistObj)voteObject + " ");
				artistText.setSelected(true);
				showText.setText("Last voted: " + ((ArchiveArtistObj)voteObject).getVoteTime());
				votesText.setText("Votes: " + ((ArchiveArtistObj)voteObject).getVotes() + " ");
				return convertView;
			} else if(voteObject instanceof ArchiveShowObj){
				final ArchiveShowObj show = (ArchiveShowObj) voteObject;
				if (convertView == null) {
					LayoutInflater vi = (LayoutInflater) getActivity()
							.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					convertView = vi.inflate(R.layout.voted_show_list_row, null);
				}
				TextView artistText = (TextView) convertView.findViewById(R.id.ArtistText);
				TextView showText = (TextView) convertView.findViewById(R.id.ShowText);
				TextView votesText = (TextView) convertView.findViewById(R.id.RatingText);
				final ImageView menuIcon = (ImageView) convertView.findViewById(R.id.MenuIcon);
				final PopupMenu menu = new PopupMenu(this.getContext(), menuIcon);
				artistText.setVisibility(View.VISIBLE);
				votesText.setVisibility(View.VISIBLE);
				artistText.setText(show.getShowArtist());
				artistText.setSelected(true);
				showText.setText(show.getShowTitle());
				showText.setSelected(true);
				votesText.setText("Votes: " + show.getVotes() + " ");
				artistText.setSelected(true);
				artistText.setMarqueeRepeatLimit(-1);
				artistText.setSingleLine();
				artistText.setHorizontallyScrolling(true);
				showText.setSelected(true);
				showText.setMarqueeRepeatLimit(-1);
				showText.setSingleLine();
				showText.setHorizontallyScrolling(true);
				
				menuIcon.setVisibility(View.VISIBLE);
				
				if(db.getShowExists(show)){
					int status = db.getShowDownloadStatus(show);
					menu.getMenuInflater().inflate(R.menu.show_options_menu, menu.getMenu());
					if(status == StaticDataStore.SHOW_STATUS_NOT_DOWNLOADED){
						menu.getMenu().add(Menu.NONE, 100, Menu.NONE, "Download show");
					} else if(status == StaticDataStore.SHOW_STATUS_FULLY_DOWNLOADED){
						menu.getMenu().add(Menu.NONE, 101, Menu.NONE, "Delete show");
					} else{
						menu.getMenu().add(Menu.NONE, 100, Menu.NONE, "Download remaining");
						menu.getMenu().add(Menu.NONE, 101, Menu.NONE, "Delete downloaded");
					}
				}				
				
				menuIcon.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View v) {
						
						menu.setOnMenuItemClickListener(new OnMenuItemClickListener(){
							@Override
							public boolean onMenuItemClick(MenuItem item) {
								Intent i = new Intent(Intent.ACTION_SEND);
								i.setType("text/plain");
								// Add data to the intent, the receiving app will decide what to do with it.
								i.putExtra(Intent.EXTRA_SUBJECT, "Vibe Vault");
								i.putExtra(Intent.EXTRA_TEXT, show.getShowArtist() + " " + show.getShowTitle() + " " + show.getShowURL()
										+ "\n\nSent using #VibeVault for Android.");
								MenuItem share = menu.getMenu().findItem(R.id.ShareButton);
							    mShareActionProvider = (ShareActionProvider) share.getActionProvider();
								if(mShareActionProvider!=null){
									mShareActionProvider.setShareIntent(i);
								}
								switch (item.getItemId()) {
								case (100):
									DownloadingAsyncTask task = new DownloadingAsyncTask(getActivity());
									ArrayList<ArchiveSongObj> songs = db.getSongsFromShow(show.getIdentifier());
									task.execute(songs.toArray(new ArchiveSongObj[songs.size()]));
									break;
									case (R.id.AddButton):
										Intent intent = new Intent(PlaybackService.ACTION_QUEUE_SHOW);
										intent.putExtra(PlaybackService.EXTRA_DO_PLAY, false);
										getContext().startService(intent);
										break;
									case (101):
										if(Downloading.deleteShow(getContext(), show, db)){
											Toast.makeText(getContext(), "Deleted.", Toast.LENGTH_SHORT).show();
										} else{
											Toast.makeText(getContext(), "Error, songs not deleted.", Toast.LENGTH_SHORT).show();
										}
									default:
										return false;
									}
								return true;
							}
						});
						menu.show();
					}
				});
				
				
				
				
				
				return convertView;
			} else{
				if (convertView == null) {
					LayoutInflater vi = (LayoutInflater) getActivity()
							.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					convertView = vi.inflate(R.layout.voted_show_list_row, null);
				}
				return convertView;
			}			
		}		
	}


	


}