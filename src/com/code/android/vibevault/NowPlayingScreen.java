/*
 * NowPlayingScreen.java
 * VERSION 1.4
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

import java.util.List;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.SlidingDrawer;
import android.widget.Spinner;
import android.widget.Toast;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.SlidingDrawer.OnDrawerCloseListener;
import android.widget.SlidingDrawer.OnDrawerOpenListener;
import android.widget.SlidingDrawer.OnDrawerScrollListener;

import com.code.android.vibevault.R;

public class NowPlayingScreen extends Activity {
	
	private static final String LOG_TAG = NowPlayingScreen.class.getName();
	
	private PlaybackService player;

	private BroadcastReceiver changeReceiver = new PlaybackChangeReceiver();
	private BroadcastReceiver updateReceiver = new PlaybackUpdateReceiver();
	
    private Vibrator vibrator;
	
	protected TextView nowPlayingTextView;

	protected Button previous;
	protected Button stop;
	protected Button pause;
	protected Button next;
	
	protected SeekBar progressBar;
	protected TextView timeCurrent;
	protected TextView timeTotal;

	protected DraggableListView songsListView;
	protected PlaylistAdapter adapter = null;
	protected TextView playListLabel;
	
	protected SlidingDrawer slidingDrawer;
	protected TextView playListHandle;
	protected Button saveAsButton;
	protected Button saveButton;
	protected Button removeButton;
	protected EditText playListEditText;
	protected Spinner playListSpinner;
	
	protected Button shareButton;
	
	private QuickAction qa;

	
	private void refreshPlayListSpinner() {
		Cursor spinCur = VibeVault.db.getAllPlaylists();
		this.startManagingCursor(spinCur);
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
				android.R.layout.simple_spinner_item, spinCur,
				new String[] { "playlistName" },
				new int[] { android.R.id.text1 });
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		playListSpinner.setAdapter(adapter);
		for(int i = 0; i < playListSpinner.getCount(); i++){
			if(playListSpinner.getItemIdAtPosition(i)==VibeVault.playList.getKey()){
				playListSpinner.setSelection(i);
			}
		}
		toggleSaveButton();
	}
	
	private void toggleSaveButton(){
		if(VibeVault.playList.isDirty() && VibeVault.playList.getKey()!=1){
			saveButton.setEnabled(true);
		}else{
			saveButton.setEnabled(false);
		}
	}
	
	@Override
	 public void onBackPressed() {
		if(slidingDrawer!=null&&slidingDrawer.isOpened()){
			vibrator.vibrate(50);
			slidingDrawer.close();
			return;
		} else{
			super.onBackPressed();
	         return;
		}
    }
	
	private void initPlayControls(){
		this.previous.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				player.playPrev();
				vibrator.vibrate(50);
			}
		});
		this.stop.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
				player.stop();
				vibrator.vibrate(50);
			}
		});

		this.pause.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(player == null){
					attachToPlaybackService();
				}
				if (player.isPaused()) {
					
					player.play();
				} 
				else if(player.isStopped()){
					player.playSongFromPlaylist(VibeVault.nowPlayingPosition);
				}
				else {
					player.pause();
					
				}
				vibrator.vibrate(50);
			}
		});

		this.next.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				player.playNext();
				vibrator.vibrate(50);
			}			
		});
		
		progressBar.setProgressDrawable(getResources().getDrawable(R.drawable.nowplayingbar));
		progressBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				if (fromUser) {
					/*int possibleProgress = progress > seekBar
							.getSecondaryProgress() ? seekBar
							.getSecondaryProgress() : progress;
					// Only seek to position if we've downloaded the content.
					Log.d(LOG_TAG, "seekBarChange - possibleProgress: "
							+ possibleProgress);
					Log.d(LOG_TAG, "seekBarChange - secondaryProgress: "
							+ seekBar.getSecondaryProgress());
					Log.d(LOG_TAG, "seekBarChange - position: "
							+ progress);
					Log.d(LOG_TAG,
							"seekBarChange - getMax(): " + seekBar.getMax());*/
					player.seekTo(progress);
				}
			}
			@Override
			public void onStartTrackingTouch(SeekBar arg0) {
			}
			@Override
			public void onStopTrackingTouch(SeekBar arg0) {
			}
		});
		
		final ActionItem voteButton = new ActionItem();
		
		voteButton.setTitle("Vote");
		voteButton.setIcon(getResources().getDrawable(R.drawable.vote));
		voteButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				vibrator.vibrate(50);
				new Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
						// Give the button time to draw its “pressed” state.
						// Without this delay, the pressed button is transparent
						// while the exit animation runs.
						qa.dismiss();
					}
				}, 15);
				vote();
			}
		});

		final ActionItem facebookButton = new ActionItem();

		facebookButton.setTitle("Facebook");
		facebookButton.setIcon(getResources().getDrawable(
				R.drawable.facebook_icon));
		facebookButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ArchiveSongObj voteSong = player.getPlayingSong();
				if(voteSong!=null){
					Intent i = new Intent(NowPlayingScreen.this, FacebookCon.class);
					i.putExtra("show_title", voteSong.getShowArtist() + " - " + voteSong.getShowTitle());
					i.putExtra("show_url", voteSong.getLowBitRate());
					vibrator.vibrate(50);
					new Handler().postDelayed(new Runnable() {
						@Override
						public void run() {
							// Give the button time to draw its “pressed” state.
							// Without this delay, the pressed button is transparent
							// while the exit animation runs.
							qa.dismiss();
						}
					}, 10);
					startActivity(i);
				} else{
					Toast.makeText(getBaseContext(), "No song playing or paused to vote for.", Toast.LENGTH_SHORT).show();
				}
				
			}
		});

		final ActionItem twitterButton = new ActionItem();

		twitterButton.setTitle("Twitter");
		twitterButton.setIcon(getResources().getDrawable(
				R.drawable.twitter_icon));
		twitterButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ArchiveSongObj voteSong = player.getPlayingSong();
				if(voteSong!=null){
					Intent i = new Intent(NowPlayingScreen.this, TwitterCon.class);
					i.putExtra("show_title", voteSong.getShowArtist() + " - " + voteSong.getShowTitle());
					i.putExtra("show_url", voteSong.getLowBitRate());
					vibrator.vibrate(50);
					new Handler().postDelayed(new Runnable() {
						@Override
						public void run() {
							// Give the button time to draw its “pressed” state.
							// Without this delay, the pressed button is transparent
							// while the exit animation runs.
							qa.dismiss();
						}
					}, 10);
					startActivity(i);
			} else{
				Toast.makeText(getBaseContext(), "No song playing or paused to vote for.", Toast.LENGTH_SHORT).show();
			}
		}
		});
		
		shareButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				qa = new QuickAction(v);

				qa.addActionItem(voteButton);
				qa.addActionItem(facebookButton);
				qa.addActionItem(twitterButton);
				qa.setAnimStyle(QuickAction.ANIM_AUTO);

				qa.show();
			}

		});
	}

	private void initPlayListControls(){
		saveButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				vibrator.vibrate(50);
				Toast.makeText(NowPlayingScreen.this, "Saving...", Toast.LENGTH_SHORT).show();
				VibeVault.playList.savePlayList();
				toggleSaveButton();
			}
			
		});
		
		saveAsButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				vibrator.vibrate(50);
				
				AlertDialog.Builder alert = new AlertDialog.Builder(NowPlayingScreen.this);

				alert.setTitle("Save Playlist");
				alert.setMessage("Enter PlayList Name:");

				// Set an EditText view to get user input 
				final EditText input = new EditText(v.getContext());
				InputFilter[] lengthFilter = new InputFilter[1];
				lengthFilter[0] = new InputFilter.LengthFilter(20);
				input.setFilters(lengthFilter);
				input.setInputType(InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE);
				input.setMaxLines(1);
				input.setPadding(5, 0, 5, 0);
				
				alert.setView(input);

				alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
				  String title = input.getText().toString();
					if(title.length()==0){
						Toast.makeText(NowPlayingScreen.this, "Enter a playlist name...", Toast.LENGTH_SHORT).show();
						return;
					} else{
						VibeVault.playList.setTitle(title);
						VibeVault.playList.setKey(VibeVault.db.storePlaylist(VibeVault.playList));
						Toast.makeText(NowPlayingScreen.this, "Saved " + title + "...", Toast.LENGTH_SHORT).show();
						refreshPlayListSpinner();
					}
				  }
				});

				alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				  public void onClick(DialogInterface dialog, int whichButton) {
				    dialog.cancel();
				   }
				});

				alert.show();
			}
		});
		
		removeButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				vibrator.vibrate(50);
				long selected = playListSpinner.getSelectedItemId();
				if(selected!=1){
					VibeVault.db.deletePlaylist(selected);
					VibeVault.playList = new ArchivePlaylistObj();
					refreshTrackList();
					refreshPlayListSpinner();
					playListHandle.setText("Playlist: " + VibeVault.playList.getTitle());
				}
			}
		});
		
		playListHandle.setText("Playlist: " + VibeVault.playList.getTitle());
		
		refreshPlayListSpinner();
		playListSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				// If this is the Now Playing PlayList, refresh the TrackList, set the
				// handle's text, and set the currently playing PlayList key to the Now Playing key.
				if(arg3==1){
					refreshTrackList();
					playListHandle.setText("Playlist: Now Playing");
					VibeVault.playList.setKey(1);
					return;
				}
				
				VibeVault.playList = VibeVault.db.getPlaylist(arg3);
				refreshTrackList();
				playListHandle.setText("Playlist: " + VibeVault.playList.getTitle());
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
		
		playListHandle.setSelected(true);
		
		slidingDrawer.setOnDrawerScrollListener(new OnDrawerScrollListener(){

			@Override
			public void onScrollEnded() {
			}

			@Override
			public void onScrollStarted() {
				vibrator.vibrate(50);
			}
			
		});
		slidingDrawer.setOnDrawerOpenListener(new OnDrawerOpenListener(){
			@Override
			public void onDrawerOpened() {
				songsListView.setEnabled(false);
				songsListView.setBackgroundDrawable(getResources().getDrawable(R.drawable.backgrounddrawableblue));
				songsListView.getBackground().setDither(true);
				toggleSaveButton();
			}
		});
		slidingDrawer.setOnDrawerCloseListener(new OnDrawerCloseListener(){
			@Override
			public void onDrawerClosed() {
				songsListView.setEnabled(true);
				songsListView.setBackgroundColor(Color.BLACK);
				}
		});
	}
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.now_playing);
		
		slidingDrawer = (SlidingDrawer) findViewById(R.id.SlidingDrawerNowPlayingScreen);
		playListHandle = (TextView) findViewById(R.id.HandleTextView);
		previous = (Button) this.findViewById(R.id.PrevButton);
		stop = (Button) this.findViewById(R.id.StopButton);
		pause = (Button) this.findViewById(R.id.PauseButton);
		next = (Button) this.findViewById(R.id.NextButton);
		progressBar = (SeekBar) this.findViewById(R.id.SeekBarNowPlaying);
		timeCurrent = (TextView) this.findViewById(R.id.TimeCurrent);
		timeTotal = (TextView) this.findViewById(R.id.TimeTotal);
		nowPlayingTextView = (TextView) this.findViewById(R.id.PlayingLabelTextView);
		
		saveAsButton = (Button) findViewById(R.id.SaveAsButton);
		saveButton = (Button) findViewById(R.id.SaveButton);
		removeButton = (Button) findViewById(R.id.RemoveButton);
		playListSpinner = (Spinner) findViewById(R.id.PlayListSpinner);
		
		vibrator = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);
		
		shareButton = (Button) findViewById(R.id.ShareButton);

		songsListView = (DraggableListView) findViewById(R.id.PlayListListView);
		songsListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> a, View v, int position,
					long id) {
				player.playSongFromPlaylist(position);
			}
		});
		songsListView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
			@Override
			public void onCreateContextMenu(ContextMenu menu, View v,
					ContextMenu.ContextMenuInfo menuInfo) {
				menu.add(Menu.NONE, VibeVault.REMOVE_FROM_QUEUE,
						Menu.NONE, "Remove from playlist");
			}
		});
		songsListView.setDropListener(new DraggableListView.DropListener() {
			@Override
			public void drop(int from, int to) {
				if(from==VibeVault.nowPlayingPosition){
					VibeVault.nowPlayingPosition = to;
				} else if(from<VibeVault.nowPlayingPosition){
					if(to<VibeVault.nowPlayingPosition){
					} else{
						VibeVault.nowPlayingPosition--;
					}
				} else{
					if(to<=VibeVault.nowPlayingPosition){
						VibeVault.nowPlayingPosition++;
					} else{
						
					}
				}
				ArchiveSongObj selected = adapter.getItem(from);
				
				VibeVault.playList.removeSong(selected);
				VibeVault.playList.add(selected,to);
				
				
				refreshTrackList();
				toggleSaveButton();
				
				if(songsListView.lastVisible<=adapter.getCount()){
					songsListView.setSelection(songsListView.firstVisible);
				}
			}
		});
		songsListView.setRemoveListener(new DraggableListView.RemoveListener() {
			@Override
			public void remove(int which) {
				player.stop();
				if(which<VibeVault.nowPlayingPosition){
					VibeVault.nowPlayingPosition--;
				} else if(which == VibeVault.nowPlayingPosition){
					VibeVault.nowPlayingPosition=-1;
				}
				VibeVault.playList.removeSong(adapter.getItem(which));
				toggleSaveButton();
			}
		});
		
		initPlayControls();
		initPlayListControls();
		hideGUIFeaturesIfOldSDK();
		songsListView.setBackgroundColor(Color.BLACK);
		
		}

	private void refreshTrackList(){
		toggleSaveButton();
		
		adapter = new PlaylistAdapter(this, R.layout.playlist_row, VibeVault.playList.getList());
		songsListView.setAdapter(adapter);
		//songsListView.setSelection(VibeVault.nowPlayingPosition);
	}

	@Override
	public void onResume()
	{
		super.onResume();
		attachToPlaybackService();
		refreshTrackList();
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
		detachFromPlaybackService();
		songsListView.setAdapter(null);
	}
	
	/*private void refreshCurrentSong()
	{
		String nowPlayingString = "";
		if(player.isStopped()){
			nowPlayingString = "Nothing playing...";
		} else{
			nowPlayingString = (player.getPlayingShowTitle() + " -- " + player.getPlayingSongTitle());
		}
		nowPlayingTextView.setText(nowPlayingString);
		nowPlayingTextView.setSelected(true);
	}*/

	private void attachToPlaybackService() {
		
		Intent serviceIntent = new Intent(this, PlaybackService.class);

		// Explicitly start the service. Don't use BIND_AUTO_CREATE, since it
		// causes an implicit service stop when the last binder is removed.
		this.startService(serviceIntent);
		this.bindService(serviceIntent, conn, 0);

		this.registerReceiver(updateReceiver, new IntentFilter(
				PlaybackService.SERVICE_UPDATE_NAME));
		this.registerReceiver(playlistReceiver, new IntentFilter(
				PlaybackService.SERVICE_PLAYLIST_NAME));
		this.registerReceiver(changeReceiver, new IntentFilter(
				PlaybackService.SERVICE_CHANGE_NAME));
	}

	private void detachFromPlaybackService() {
		this.unbindService(conn);
		unregisterReceiver(changeReceiver);
		unregisterReceiver(updateReceiver);
		unregisterReceiver(playlistReceiver);
	}

	private ServiceConnection conn = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.w(LOG_TAG, "CONNECT to PlaybackService");
			player = ((PlaybackService.ListenBinder) service).getService();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.w(LOG_TAG, "DISCONNECT from PlaybackService");
			player = null;
		}
	};
	
	private BroadcastReceiver playlistReceiver=new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			refreshTrackList();
		}
	};
	
	private class PlaybackChangeReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String title = intent.getStringExtra(PlaybackService.EXTRA_TITLE);
			nowPlayingTextView.setText(title);
			nowPlayingTextView.setSelected(true);
			refreshTrackList();
			songsListView.setSelection(VibeVault.nowPlayingPosition);
			String status = intent.getStringExtra(PlaybackService.EXTRA_STATUS);
			if(status.equals("playing")) {
				pause.setBackgroundResource(R.drawable.pausebutton);
			}
			else {
				pause.setBackgroundResource(R.drawable.playbutton);
			}
			hideGUIFeaturesIfOldSDK();
		}
	}
	
	private void hideGUIFeaturesIfOldSDK(){
		if(Build.VERSION.SDK_INT < 8 && player==null){
			progressBar.setVisibility(View.GONE);
			timeCurrent.setVisibility(View.GONE);
			timeTotal.setVisibility(View.GONE);
			return;
		}
		if (Build.VERSION.SDK_INT < 8) {
			if (player.isStreaming()) {
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

	private class PlaybackUpdateReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			int duration = intent
					.getIntExtra(PlaybackService.EXTRA_DURATION, 1);
			int position = intent
					.getIntExtra(PlaybackService.EXTRA_POSITION, 0);
			int downloaded = intent.getIntExtra(
					PlaybackService.EXTRA_DOWNLOADED, 1);
			progressBar.setEnabled(true);
			progressBar.setMax(duration);
			progressBar.setProgress(position);
			progressBar.setSecondaryProgress(downloaded);
			timeCurrent.setText(getElapsedTimeHoursMinutesSecondsString(position));
			timeTotal.setText(getElapsedTimeHoursMinutesSecondsString(duration));
		}
	}
	
	public String getElapsedTimeHoursMinutesSecondsString(int msec) {
	    String format = String.format("%%0%dd", 2);  
	    msec = msec / 1000;  
	    String seconds = String.format(format, msec % 60);  
	    String minutes = String.format(format, (msec % 3600) / 60);  
	    //String hours = String.format(format, msec / 3600);  
	    String time =  minutes + ":" + seconds;  
	    return time;  
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.help_options, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected (MenuItem item){
		switch (item.getItemId()){
			case R.id.scrollableDialog:
				AlertDialog.Builder ad = new AlertDialog.Builder(this);
				ad.setTitle("Help!");
				View v =LayoutInflater.from(this).inflate(R.layout.scrollable_dialog, null);
				((TextView)v.findViewById(R.id.DialogText)).setText(R.string.now_playing_screen_help);
				ad.setPositiveButton("Okay.", new android.content.DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int arg1) {
					}
				});
				ad.setView(v);
				ad.show();
		}
		return true;
	}
	
	private class PlaylistAdapter extends ArrayAdapter<ArchiveSongObj> {
		
		public PlaylistAdapter(Context context, int textViewResourceId, List<ArchiveSongObj> objects){
			super(context,textViewResourceId, objects);
		}
		
		@Override 
		public View getView(int position, View convertView, ViewGroup parent){
			ArchiveSongObj song = player.getSong(position);	
			if(convertView==null){
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = vi.inflate(R.layout.playlist_row, null);
			}
			TextView songText = (TextView) convertView.findViewById(R.id.SongTitle);
			TextView artistText = (TextView) convertView.findViewById(R.id.ArtistTitle);
			if(song != null){
				songText.setText(song.toString());
				artistText.setText(song.getShowArtist());
				if(position == VibeVault.nowPlayingPosition){
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
			return convertView;
		}
	}
	
	
	
	public boolean onContextItemSelected(MenuItem item){
		AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo)item.getMenuInfo();
		if(menuInfo!=null){
			switch(item.getItemId()){
			case(VibeVault.REMOVE_FROM_QUEUE):
				VibeVault.playList.removeSongAt(menuInfo.position);
				player.dequeue(menuInfo.position);
				break;
			default:
				return false;
			}
			return true;
		}
		return false;
	}
	
	private void vote(){
		ArchiveSongObj voteSong = player.getPlayingSong();
		if(voteSong!=null){
			VoteTask t = new VoteTask();
			String showDate = voteSong.getShowTitle();
			try{
				// 10 because 4 chars for year, 2 for month, 2 for day, and 2 hyphens.
				showDate = showDate.substring(showDate.length()-10);
			} catch(IndexOutOfBoundsException e){
				showDate = "";
			}
			t.execute(voteSong.getShowIdentifier(),voteSong.getShowArtist(),voteSong.getShowTitle(),showDate);
		} else{
			Toast.makeText(getBaseContext(), "No song playing or paused to vote for.", Toast.LENGTH_SHORT).show();
		}
	}
	
	private class VoteTask extends AsyncTask<String, Void, String> {
		@Override
		protected void onPreExecute() {
			Toast.makeText(getBaseContext(), "Voting...", Toast.LENGTH_SHORT).show();
		}

		@Override
		protected String doInBackground(String... showFields) {
			return Voting.vote(showFields[0], showFields[1], showFields[2], showFields[3]);
		}

		@Override
		protected void onPostExecute(String result) {
			Toast.makeText(getBaseContext(), result, Toast.LENGTH_SHORT).show();
		}
	}
}
