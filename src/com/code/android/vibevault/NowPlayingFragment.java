/*
 * NowPlayingScreen.java
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

import java.util.ArrayList;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.ShareActionProvider;
import android.widget.TableLayout;
import android.widget.Toast;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.SeekBar.OnSeekBarChangeListener;
import com.code.android.vibevault.R;

public class NowPlayingFragment extends Fragment {
	
	private static final String LOG_TAG = NowPlayingFragment.class.getName();

	private static final int MENU_REMOVE = 0;
	
	private static final String TIME_FORMAT = String.format("%%0%dd", 2);  
	
	private BroadcastReceiver playerChangedReceiver = new PlaybackChangeReceiver();
	private BroadcastReceiver playlistChangedReceiver = new PlaylistChangeReceiver();
	
    // private Vibrator vibrator;
	
	protected TextView nowPlayingTextView;

	protected TableLayout playerLayout;
	protected RelativeLayout buttonHolder;
	
	protected Button previous;
	protected Button stop;
	protected Button pause;
	protected Button next;
	
	protected SeekBar progressBar;
	protected TextView timeCurrent;
	protected TextView timeTotal;

	protected DraggableListView songsListView;
	protected PlaylistAdapter adapter = null;
			
	private PlayerListener playerListener;
	
	private ShareActionProvider mShareActionProvider;
	
	private DialogAndNavigationListener dialogAndNavigationListener;
	
	private Parcelable draggableListViewState = null;
	
	private StaticDataStore db;
	
	private int currentPos = -1;
	
	private String nowPlaying = "";

	private boolean newExternalPositionPlayed = false;
	
	public interface PlayerListener {
		public void registerReceivers(BroadcastReceiver playerChangedBroadcast, BroadcastReceiver playlistChangedBroadcast);
		public void unregisterReceivers(BroadcastReceiver playerChangedBroadcast, BroadcastReceiver playlistChangedBroadcast);
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
		try {
			playerListener = (PlayerListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement PlayerListener");
		}
	}
	
	// This method is called right after onCreateView() is called. "Called when the
	// fragment's activity has been created and this fragment's view hierarchy instantiated."
	// http://developer.android.com/guide/topics/fundamentals/fragments.html#Lifecycle
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		// If this ShowDetailsFragment has arguments (a passed show), grab it and parse it.
		if (this.getArguments() != null) {
			this.attachToPlaybackService();
		}		
		// Must call in order to get callback to onOptionsItemSelected() and thereby create an ActionBar.
        setHasOptionsMenu(true);
        getActivity().getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        getActivity().getActionBar().setListNavigationCallbacks(null, null);
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
        getActivity().getActionBar().setTitle("Now Playing");
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		menu.clear();
		inflater.inflate(R.menu.help_bookmark_share_download_vote, menu);
		MenuItem item = menu.findItem(R.id.ShareButton);
	    mShareActionProvider = (ShareActionProvider) item.getActionProvider();
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	// These are the callbacks for the ActionBar buttons.
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.VoteButton:
				vote();
				return true;
			case R.id.HelpButton:
				dialogAndNavigationListener.showDialog(this.getResources().getString(R.string.now_playing_screen_help), "Help");
				return true;
			case android.R.id.home:
				dialogAndNavigationListener.goHome();
				return true;
			case R.id.DownloadButton:
				NowPlayingFragment.this.getActivity().startService(new Intent(PlaybackService.ACTION_DOWNLOAD));
				return true;
			case R.id.BookmarkButton:
				if(currentPos>=0&&currentPos<this.adapter.getCount()){
					ArchiveSongObj voteSong = this.adapter.getItem(currentPos);
					if(voteSong!=null){
						ArchiveShowObj bookmarkShow = db.getShow(voteSong.getShowIdentifier());
						if(bookmarkShow!=null){
							db.openDataBase();
							db.insertFavoriteShow(bookmarkShow);
							db.close();
							Toast.makeText(getActivity(), "Bookmarked!", Toast.LENGTH_SHORT).show();
							return true;
						}
					}
				}
				Toast.makeText(getActivity().getBaseContext(), "No song playing or paused to bookmark.", Toast.LENGTH_SHORT).show();
				return false;
			default:
				return false;
		}
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the fragment and grab a reference to it.
		View v = inflater.inflate(R.layout.now_playing, container, false);
		
		// Initialize the interface.
		playerLayout = (TableLayout) v.findViewById(R.id.PlayerBackground);
		previous = (Button) v.findViewById(R.id.PrevButton);
		stop = (Button) v.findViewById(R.id.StopButton);
		pause = (Button) v.findViewById(R.id.PauseButton);
		next = (Button) v.findViewById(R.id.NextButton);
		progressBar = (SeekBar) v.findViewById(R.id.SeekBarNowPlaying);
		timeCurrent = (TextView) v.findViewById(R.id.TimeCurrent);
		timeTotal = (TextView) v.findViewById(R.id.TimeTotal);
		nowPlayingTextView = (TextView) v.findViewById(R.id.PlayingLabelTextView);
		buttonHolder = (RelativeLayout) v.findViewById(R.id.ButtonHolder);
		
		// Initialize the DraggableListView of songs, settings listeners for clicking,
		// long-pressing, dragging, and removing.
		int[] gradientBackground = {0xFF041625, 0};
		playerLayout.setBackgroundDrawable(new GradientDrawable(Orientation.TOP_BOTTOM, gradientBackground));
		buttonHolder.setBackgroundDrawable(new GradientDrawable(Orientation.TOP_BOTTOM, gradientBackground));
		songsListView = (DraggableListView) v.findViewById(R.id.PlayListListView);
		int[] gradientColors = {0, 0xFF127DD4, 0};
		songsListView.setDivider(new GradientDrawable(Orientation.RIGHT_LEFT, gradientColors));
		songsListView.setDividerHeight(1);
		songsListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> a, View v, int position, long id) {
				Intent intent = new Intent(PlaybackService.ACTION_PLAY_POSITION);
				intent.putExtra(PlaybackService.EXTRA_PLAYLIST_POSITION, position);
				v.getContext().startService(intent);
			}
		});
//		songsListView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
//			@Override
//			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
//				menu.add(Menu.NONE, MENU_REMOVE, Menu.NONE, "Remove from playlist");
//			}
//		});
		songsListView.setDragListener(new DraggableListView.DragListener() {
			@Override
			public void drag(int from, int to) {
				// It is not necessary to do anything with "from" and "to", because the action happens when
				// the song is actually "dropped" in the setDropListene.  We save the instance state, however,
				// because this allows us to maintain the ListView's position on refreshTrackList() calls where
				// the ListView is redrawn.
				draggableListViewState = songsListView.onSaveInstanceState();
			}
		});
		songsListView.setDropListener(new DraggableListView.DropListener() {
			@Override
			public void drop(int from, int to) {
				Intent intent = new Intent(PlaybackService.ACTION_MOVE);
				intent.putExtra(PlaybackService.EXTRA_MOVE_FROM, from);
				intent.putExtra(PlaybackService.EXTRA_MOVE_TO, to);
				NowPlayingFragment.this.getActivity().startService(intent);
			}
		});
		songsListView.setRemoveListener(new DraggableListView.RemoveListener() {
			@Override
			public void remove(int which) {
				Intent intent = new Intent(PlaybackService.ACTION_DELETE);
				intent.putExtra(PlaybackService.EXTRA_PLAYLIST_POSITION, which);
				NowPlayingFragment.this.getActivity().startService(intent);
			}
		});
		
		initPlayControls();
//		songsListView.setBackgroundColor(Color.BLACK);

		return v;
	}
	
	
	// Initialize the controls for the player.
	private void initPlayControls(){
		this.previous.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				v.getContext().startService(new Intent(PlaybackService.ACTION_PREV));
				if(currentPos < songsListView.getFirstVisiblePosition() || currentPos > songsListView.getLastVisiblePosition()){
					if(currentPos <= 0){
						songsListView.setSelection(0);
					} else{
						songsListView.setSelection(currentPos-1);
					}
					 
					draggableListViewState = songsListView.onSaveInstanceState();
				}
			}
		});
		this.stop.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				v.getContext().startService(new Intent(PlaybackService.ACTION_STOP));
			}
		});

		this.pause.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				v.getContext().startService(new Intent(PlaybackService.ACTION_TOGGLE));
			}
		});

		this.next.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				v.getContext().startService(new Intent(PlaybackService.ACTION_NEXT));
				if(currentPos < songsListView.getFirstVisiblePosition() || currentPos > songsListView.getLastVisiblePosition()){
					if(currentPos <= 0){
						songsListView.setSelection(currentPos);
					} else{ 
						songsListView.setSelection(currentPos-1);
					}
					 
					draggableListViewState = songsListView.onSaveInstanceState();
				}
			}			
		});
		
		progressBar.setProgressDrawable(getResources().getDrawable(R.drawable.nowplayingbar));
		progressBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				if (fromUser) {
					Intent intent = new Intent(PlaybackService.ACTION_SEEK);
					intent.putExtra(PlaybackService.EXTRA_SEEK_POSITON, progress);
					NowPlayingFragment.this.getActivity().startService(intent);
				}
			}
			@Override
			public void onStartTrackingTouch(SeekBar arg0) {
			}
			@Override
			public void onStopTrackingTouch(SeekBar arg0) {
			}
		});
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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
		checkAndUpdatePlayerSongs();
		this.newExternalPositionPlayed=true;
	}
	
	@Override
	public void onStop() {
		super.onStop();
	}

	private void refreshTrackList(ArrayList<ArchiveSongObj> list){
		 
		adapter = new PlaylistAdapter(getActivity(), R.layout.playlist_row, list);
		songsListView.setAdapter(adapter);
		if(draggableListViewState!=null){
			this.songsListView.onRestoreInstanceState(draggableListViewState);
		}
		this.updateShareIntent();
		// FIXME
		// There seems to be some sort of race condition issue here.  If we don't wait,
		// we are unable to play songs, because
		if(newExternalPositionPlayed == true && !adapter.isEmpty()) {
			 
			songsListView.postDelayed(new Runnable() {
				@Override
				public void run() {
					 
					if(currentPos <= 0){
						songsListView.smoothScrollToPosition(0);
					} else {
						int scrollPosition = currentPos+songsListView.getChildCount()-2;
						 
						 
						 
						scrollPosition = scrollPosition>=adapter.getCount()?adapter.getCount()-1:scrollPosition;
						 
						songsListView.smoothScrollToPosition(scrollPosition);
					}
				}
			}, 1000);
			newExternalPositionPlayed = false;			
		}
//		songsListView.setSelection(currentPos);
	}

	@Override
	public void onResume() {
		super.onResume();
		attachToPlaybackService();
		updateShareIntent();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		detachFromPlaybackService();
		songsListView.setAdapter(null);
	}

	private void attachToPlaybackService() {
		playerListener.registerReceivers(playerChangedReceiver, playlistChangedReceiver);
		Intent intent = new Intent(PlaybackService.ACTION_POLL);
		this.getActivity().startService(intent);
		 
	}

	private void detachFromPlaybackService() {
		playerListener.unregisterReceivers(playerChangedReceiver, playlistChangedReceiver);
	}
	
	private void checkAndUpdatePlayerSongs(){
		if(this.getArguments()!=null){
			if(this.getArguments().containsKey("position")){
				currentPos = this.getArguments().getInt("position");
				 
				this.getArguments().remove("position");
				this.newExternalPositionPlayed=true;
			}
			ArrayList<ArchiveSongObj>showSongs = (ArrayList<ArchiveSongObj>)this.getArguments().getSerializable("showsongs");
			if(showSongs!=null){
				Intent intent = new Intent(PlaybackService.ACTION_QUEUE_SHOW);
				intent.putExtra(PlaybackService.EXTRA_PLAYLIST, showSongs);
				intent.putExtra(PlaybackService.EXTRA_PLAYLIST_POSITION, currentPos);
				intent.putExtra(PlaybackService.EXTRA_DO_PLAY, true);
				NowPlayingFragment.this.getActivity().startService(intent);
				this.getArguments().remove("showsongs");
			}
		}
	}
	
	private class PlaybackChangeReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			int pauseIcon = R.drawable.mediapausebutton;
			
			int status = intent.getIntExtra(PlaybackService.EXTRA_STATUS, PlaybackService.STATUS_STOPPED);
			switch(status) {
				case PlaybackService.STATUS_BUFFERING:
					nowPlaying = "Buffering...";
					progressBar.setEnabled(false);
					break;
				case PlaybackService.STATUS_PAUSED:
					pauseIcon = R.drawable.mediaplaybutton;
					progressBar.setEnabled(true);
					nowPlaying = intent.getStringExtra(PlaybackService.EXTRA_TITLE);
					progressBar.setMax(intent.getIntExtra(PlaybackService.EXTRA_PLAY_DURATION, 0));
					progressBar.setProgress(intent.getIntExtra(PlaybackService.EXTRA_PLAY_PROGRESS, 0));
					progressBar.setSecondaryProgress(intent.getIntExtra(PlaybackService.EXTRA_BUFFER_PROGRESS, 0));
					timeCurrent.setText(getElapsedTimeHoursMinutesSecondsString(intent.getIntExtra(PlaybackService.EXTRA_PLAY_PROGRESS, 0)));
					timeTotal.setText(getElapsedTimeHoursMinutesSecondsString(intent.getIntExtra(PlaybackService.EXTRA_PLAY_DURATION, 0)));
					break;
				case PlaybackService.STATUS_PLAYING:
					progressBar.setEnabled(true);
					nowPlaying = intent.getStringExtra(PlaybackService.EXTRA_TITLE);
					progressBar.setMax(intent.getIntExtra(PlaybackService.EXTRA_PLAY_DURATION, 0));
					progressBar.setProgress(intent.getIntExtra(PlaybackService.EXTRA_PLAY_PROGRESS, 0));
					progressBar.setSecondaryProgress(intent.getIntExtra(PlaybackService.EXTRA_BUFFER_PROGRESS, 0));
					timeCurrent.setText(getElapsedTimeHoursMinutesSecondsString(intent.getIntExtra(PlaybackService.EXTRA_PLAY_PROGRESS, 0)));
					timeTotal.setText(getElapsedTimeHoursMinutesSecondsString(intent.getIntExtra(PlaybackService.EXTRA_PLAY_DURATION, 0)));
					break;
				case PlaybackService.STATUS_STOPPED:
					nowPlaying = "";
					pauseIcon = R.drawable.mediaplaybutton;
					progressBar.setEnabled(false);
					break;
			}
			if(!nowPlayingTextView.getText().equals(nowPlaying)){
				nowPlayingTextView.setText(nowPlaying);
				nowPlayingTextView.setSelected(true);
				nowPlayingTextView.setMarqueeRepeatLimit(-1);
				nowPlayingTextView.setSingleLine();
				nowPlayingTextView.setHorizontallyScrolling(true);
			}

			pause.setBackgroundResource(pauseIcon);
			hideGUIFeaturesIfOldSDK(status);
		}
	}
	
	private void hideGUIFeaturesIfOldSDK(int status){
		if (Build.VERSION.SDK_INT < 8) {
			if (status != PlaybackService.STATUS_STOPPED) {
				progressBar.setVisibility(View.GONE);
				timeCurrent.setVisibility(View.GONE);
				timeTotal.setVisibility(View.GONE);
			} else{
				progressBar.setVisibility(View.VISIBLE);
				timeCurrent.setVisibility(View.VISIBLE);
				timeTotal.setVisibility(View.VISIBLE);
			}
		}
	}

	private class PlaylistChangeReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			 
			ArrayList<ArchiveSongObj> songs = (ArrayList<ArchiveSongObj>) intent.getSerializableExtra(PlaybackService.EXTRA_PLAYLIST);
			currentPos = intent.getIntExtra(PlaybackService.EXTRA_PLAYLIST_POSITION, 0);
			draggableListViewState = songsListView.onSaveInstanceState();
			refreshTrackList(songs);
		}
	}
	
	private void updateShareIntent(){
		 
		if(songsListView!=null&&songsListView.getAdapter()!=null && !songsListView.getAdapter().isEmpty()){
			 
			ArchiveSongObj song = (ArchiveSongObj)this.songsListView.getAdapter().getItem(this.currentPos>=0&&this.currentPos<this.songsListView.getAdapter().getCount()?this.currentPos:0);
			if(song!=null){
				 
				Intent i = new Intent(Intent.ACTION_SEND);
				i.setType("text/plain");
				// Add data to the intent, the receiving app will decide what to do with it.
				i.putExtra(Intent.EXTRA_SUBJECT, "Vibe Vault");
				i.putExtra(Intent.EXTRA_TEXT, "Listen to " + song.getSongTitle() + " by " + song.getShowArtist() + ": " + song.getLowBitRate()
						+ "\n\nSent using #VibeVault for Android.");
				if(mShareActionProvider!=null){
					mShareActionProvider.setShareIntent(i);
				}
			}
		}
		
	}
	
	public String getElapsedTimeHoursMinutesSecondsString(int msec) {
	    msec = msec / 1000;
	    return String.format(TIME_FORMAT, (msec % 3600) / 60) + ":" + String.format(TIME_FORMAT, msec % 60);
	}
	
	private class PlaylistAdapter extends ArrayAdapter<ArchiveSongObj> {

		private final static int DELETE_SONG = 102;
		private final static int DELETE_SHOW = 103;
		
		public PlaylistAdapter(Context context, int textViewResourceId, ArrayList<ArchiveSongObj> objects){
			super(context,textViewResourceId, objects);
		}
		
		@Override 
		public View getView(int position, View convertView, ViewGroup parent){
			final ArchiveSongObj song = this.getItem(position);
			if(convertView==null){
				LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = vi.inflate(R.layout.playlist_row, null);
			}
			final TextView songText = (TextView) convertView.findViewById(R.id.SongTitle);
			final TextView artistText = (TextView) convertView.findViewById(R.id.ArtistTitle);
			final ImageView menuIcon = (ImageView) convertView.findViewById(R.id.menuIcon);
			final PopupMenu menu = new PopupMenu(getActivity(), menuIcon);
			// If we have an ArchiveSongObject for this View's position, set the data to display.
			if(song != null){
				songText.setText(song.toString());
				artistText.setText(song.getShowArtist());
				if(position == currentPos){
					songText.setTextColor(Color.YELLOW);
					artistText.setTextColor(Color.YELLOW);
				}
				else{
					songText.setTextColor(Color.rgb(18, 125, 212));
					artistText.setTextColor(Color.WHITE);
				}
			}
			artistText.setSelected(true);
			songText.setSelected(true);
			artistText.setMarqueeRepeatLimit(-1);
			artistText.setSingleLine();
			artistText.setHorizontallyScrolling(true);
			songText.setMarqueeRepeatLimit(-1);
			songText.setSingleLine();
			songText.setHorizontallyScrolling(true);
			menuIcon.setVisibility(View.VISIBLE);
			// Set up the PopupMenu for the View.
			menuIcon.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					// Putting the inflation code here speeds up the getView() method, because
					// inflation only occurs when the user clicks to see the PopupMenu.
					menu.getMenuInflater().inflate(R.menu.song_options_menu, menu.getMenu());
					if (db.songIsDownloaded(song.getFileName())) {
						menu.getMenu().add(Menu.NONE, DELETE_SONG, Menu.NONE, "Delete song");
					} else {
						menu.getMenu().add(Menu.NONE, DELETE_SHOW, Menu.NONE, "Download song");
					}
					menu.getMenu().removeItem(R.id.AddButton);
					menu.setOnMenuItemClickListener(new OnMenuItemClickListener(){
						@Override
						public boolean onMenuItemClick(MenuItem item) {
							updateShareIntent();
							switch (item.getItemId()) {
								case (R.id.DownloadButton):
									 
									DownloadingAsyncTask task = new DownloadingAsyncTask(getActivity());
									task.execute(song);
									break;
//								case (R.id.AddButton):
//									Intent intent = new Intent(PlaybackService.ACTION_QUEUE_SONG);
//									intent.putExtra(PlaybackService.EXTRA_SONG, song);
//									intent.putExtra(PlaybackService.EXTRA_DO_PLAY, false);
//									getActivity().startService(intent);
//									break;
								case (DELETE_SONG):
									if(Downloading.deleteSong(getContext(), song, db)){
										Toast.makeText(getContext(), "Song deleted.", Toast.LENGTH_SHORT).show();
									} else{
										Toast.makeText(getContext(), "Error, song not deleted.", Toast.LENGTH_SHORT).show();
									}
									break;
								case (DELETE_SHOW):
									DownloadingAsyncTask deletionTask = new DownloadingAsyncTask(getActivity());
									deletionTask.execute(song);
									break;
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
		}
	}
	
	public boolean onContextItemSelected(MenuItem item){
		AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo)item.getMenuInfo();
		if(menuInfo!=null){
			switch(item.getItemId()){
				case(MENU_REMOVE):
					Intent intent = new Intent(PlaybackService.ACTION_DELETE);
					intent.putExtra(PlaybackService.EXTRA_PLAYLIST_POSITION, menuInfo.position);
					this.getActivity().startService(intent);
					break;
				default:
					return false;
			}
			return true;
		}
		return false;
	}
	
	private void vote(){
		if(currentPos>=0&&currentPos<this.adapter.getCount()){
			ArchiveSongObj voteSong = this.adapter.getItem(currentPos);
			if(voteSong!=null){
				VoteTask t = new VoteTask();
				String showDate = voteSong.getShowTitle();
				try{
					// FIXME Is it a bad idea that this is hardcoded?
					// 10 because 4 chars for year, 2 for month, 2 for day, and 2 hyphens.
					showDate = showDate.substring(showDate.length()-10);
				} catch(IndexOutOfBoundsException e){
					showDate = "";
				}
				t.execute(voteSong.getShowIdentifier(),voteSong.getShowArtist(),voteSong.getShowTitle(),showDate);
				return;
			}
		}
		Toast.makeText(getActivity().getBaseContext(), "No song playing or paused to vote for.", Toast.LENGTH_SHORT).show();
	}
	
	private class VoteTask extends AsyncTask<String, Void, String> {
				
		@Override
		protected void onPreExecute() {
			Toast.makeText(getActivity().getBaseContext(), "Voting...", Toast.LENGTH_SHORT).show();
		}

		@Override
		protected String doInBackground(String... showFields) {
			return Voting.vote(showFields[0], showFields[1], showFields[2], showFields[3], db);
		}

		@Override
		protected void onPostExecute(String result) {
			Toast.makeText(getActivity().getBaseContext(), result, Toast.LENGTH_SHORT).show();
		}
	}
}