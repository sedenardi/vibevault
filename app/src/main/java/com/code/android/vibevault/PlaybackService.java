// Copyright 2009 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.code.android.vibevault;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.RemoteControlClient.MetadataEditor;
import android.media.RemoteControlClient;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.util.ArrayList;
import com.code.android.vibevault.R;

public class PlaybackService extends Service implements 
		OnPreparedListener, OnBufferingUpdateListener, OnCompletionListener,
		OnErrorListener, AudioManager.OnAudioFocusChangeListener{

	private static final String LOG_TAG = PlaybackService.class.getName();

	private static final String SERVICE_PREFIX = "com.code.android.vibevault.playbackservice.";
	public static final String SERVICE_POSITION = SERVICE_PREFIX + "POSITON";
	public static final String SERVICE_STATE = SERVICE_PREFIX + "STATE";
	
	// Intent Extras
	public static final String EXTRA_STATUS = SERVICE_PREFIX + "EXTRA_STATUS";
	public static final String EXTRA_PLAY_DURATION = SERVICE_PREFIX + "EXTRA_PLAY_DURATION";
	public static final String EXTRA_PLAY_PROGRESS = SERVICE_PREFIX + "EXTRA_PLAY_PROGRESS";
	public static final String EXTRA_BUFFER_PROGRESS = SERVICE_PREFIX + "EXTRA_BUFFER_PROGRESS";
	public static final String EXTRA_TITLE = SERVICE_PREFIX + "EXTRA_TITLE";
	public static final String EXTRA_PLAYLIST = SERVICE_PREFIX + "EXTRA_PLAYLIST";
	public static final String EXTRA_PLAYLIST_POSITION = SERVICE_PREFIX + "EXTRA_PLAYLIST_POSITION";
	public static final String EXTRA_SEEK_POSITON = SERVICE_PREFIX + "EXTRA_SEEK_POSITON";
	public static final String EXTRA_SONG = SERVICE_PREFIX + "EXTRA_SONG";
	public static final String EXTRA_DO_PLAY = SERVICE_PREFIX + "EXTRA_DO_PLAY";
	public static final String EXTRA_MOVE_FROM = SERVICE_PREFIX + "EXTRA_MOVE_FROM";
	public static final String EXTRA_MOVE_TO = SERVICE_PREFIX + "EXTRA_MOVE_TO";
	
	// Actions
	public static final String ACTION_TOGGLE = SERVICE_PREFIX + "ACTION_TOGGLE";
	public static final String ACTION_PLAY = SERVICE_PREFIX + "ACTION_PLAY";
	public static final String ACTION_PLAY_POSITION = SERVICE_PREFIX + "ACTION_PLAY_POSITION";
	public static final String ACTION_PAUSE = SERVICE_PREFIX + "ACTION_PAUSE";
	public static final String ACTION_NEXT = SERVICE_PREFIX + "ACTION_NEXT";
	public static final String ACTION_PREV = SERVICE_PREFIX + "ACTION_PREV";
	public static final String ACTION_STOP = SERVICE_PREFIX + "ACTION_STOP";
	public static final String ACTION_SEEK = SERVICE_PREFIX + "ACTION_SEEK";
	public static final String ACTION_QUEUE_SONG = SERVICE_PREFIX + "ACTION_QUEUE_SONG";
	public static final String ACTION_QUEUE_SHOW = SERVICE_PREFIX + "ACTION_QUEUE_SHOW";
	public static final String ACTION_MOVE = SERVICE_PREFIX + "ACTION_MOVE";
	public static final String ACTION_DELETE = SERVICE_PREFIX + "ACTION_DELETE";
	public static final String ACTION_DOWNLOAD = SERVICE_PREFIX + "ACTION_DOWNLOAD";
	public static final String ACTION_POLL = SERVICE_PREFIX + "ACTION_POLL";
	
	// Statuses
	public static final int STATUS_STOPPED = 0; // Player is stopped
	public static final int STATUS_BUFFERING = 1; // Player is buffering and is
													// not playing
	public static final int STATUS_PLAYING = 2; // Player is playing
	public static final int STATUS_PAUSED = 3; // Player is paused and can be
												// played instantaneously

	private int playerStatus;
	
	private ArrayList<ArchiveSongObj> songArray;
	private int nowPlayingPosition = -1;

	private MediaPlayer mediaPlayer;
	private boolean isStreaming = false;
	private WifiLock wifiLock;

	private NotificationManager notificationManager;
	private Notification.Builder mBuilder;
	private static final int NOTIFICATION_ID = 1;
	
	private AudioManager audioManager;
	private ComponentName remoteReceiver;
	private RemoteControlClient remoteControlClient;
	private Bitmap albumArt;
	
	private StaticDataStore db;

	private TelephonyManager telephonyManager;
	private PhoneStateListener listener;
	private boolean isPausedInCall = false;
	private boolean isPausedByFocus = false;
	private boolean isDuckedByFocus = false;
	private final static float DUCK_VOLUME = 0.1f;
	private int lastBufferPercent = 0;
	private static Thread updateProgressThread;
	// Amount of time to rewind playback when resuming after call
	private final static int RESUME_REWIND_TIME = 3000;

	@Override
	public void onCreate() {
		Logging.Log(LOG_TAG,"PlaybackService: Creating Service");
		
		db = StaticDataStore.getInstance(this);
		
		songArray = db.getNowPlayingSongs();
		if (db.getPref("nowPlayingPosition") != "NULL") {
			nowPlayingPosition = Integer.parseInt(db.getPref("nowPlayingPosition"));
		}
		
		wifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, "vvLock");
		
		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		remoteReceiver = new ComponentName(this, RemoteControlReceiver.class);
		albumArt = BitmapFactory.decodeResource(getResources(), R.drawable.big_icon);
		
		telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		// Create a PhoneStateListener to watch for offhook and idle events
		listener = new PhoneStateListener() {
			@Override
			public void onCallStateChanged(int state, String incomingNumber) {
				switch (state) {
				case TelephonyManager.CALL_STATE_OFFHOOK:
				case TelephonyManager.CALL_STATE_RINGING:
					// Phone going offhook or ringing, pause the player.
					if (playerStatus == STATUS_PLAYING) {
						pause();
						isPausedInCall = true;
					}
					break;
				case TelephonyManager.CALL_STATE_IDLE:
					// Phone idle. Rewind a couple of seconds and start playing.
					if (isPausedInCall && playerStatus == STATUS_PAUSED) {
						mediaPlayer.seekTo(mediaPlayer.getDuration() - RESUME_REWIND_TIME);
						play();
						isPausedInCall = false;
					}
					break;
				}
			}
		};

		// Register the listener with the telephony manager.
		telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
		playerStatus = STATUS_STOPPED;
		Logging.Log(LOG_TAG,"PlaybackService: Creating Service - Done");
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {
			String action = intent.getAction();
			Logging.Log(LOG_TAG,"PlaybackService: onStart action - " + action);
			if (action == ACTION_TOGGLE) togglePlayPause();
			else if (action == ACTION_PLAY) play();
			else if (action == ACTION_PLAY_POSITION) playPos(intent); 
			else if (action == ACTION_PAUSE) pause(); 
			else if (action == ACTION_NEXT) next(); 
			else if (action == ACTION_PREV) previous(); 
			else if (action == ACTION_STOP) stop();
			else if (action == ACTION_SEEK) seekTo(intent);
			else if (action == ACTION_QUEUE_SONG) queueSong(intent);
			else if (action == ACTION_QUEUE_SHOW) queueShow(intent);
			else if (action == ACTION_MOVE) moveSong(intent);
			else if (action == ACTION_DELETE) deleteSong(intent);
			else if (action == ACTION_DOWNLOAD) downloadShow();
			else if (action == ACTION_POLL) {
				sendStateChangeBroadcast();
				sendPositionChangeBroadcast();
			}
		}
		
		return START_NOT_STICKY;
	}
	
	private void createMediaPlayer() {
		if (mediaPlayer == null){
			Logging.Log(LOG_TAG,"PlaybackService: mediaPlayer null, creating.");
			mediaPlayer = new MediaPlayer();
			
			mediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
			
			mediaPlayer.setOnBufferingUpdateListener(this);
			mediaPlayer.setOnCompletionListener(this);
			mediaPlayer.setOnErrorListener(this);
			mediaPlayer.setOnPreparedListener(this);
		}
		else {
			mediaPlayer.reset();
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Logging.Log(LOG_TAG,"PlaybackService destroy");
		playerStatus = PlaybackService.STATUS_STOPPED; 
		releaseResources();		
		telephonyManager.listen(listener, PhoneStateListener.LISTEN_NONE);
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int progress) {
		synchronized(this) {
			lastBufferPercent = progress;
		}
		sendPositionChangeBroadcast();
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		playerStatus = STATUS_STOPPED;
		if (nowPlayingPosition < (songArray.size() - 1)) {
			Logging.Log(LOG_TAG,"PlaybackService completed, playing next song");
			next();
		}
		else {
			Logging.Log(LOG_TAG,"PlaybackService completed, end of playlist");
			playerStatus = STATUS_STOPPED;
			notificationManager.cancel(NOTIFICATION_ID);
			
			if (updateProgressThread != null) {
				updateProgressThread.interrupt();
				try {
					updateProgressThread.join(500);
				} catch (InterruptedException e) {
				}
			}
			notificationManager.cancel(NOTIFICATION_ID);
			remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
			releaseResources();			
			sendStateChangeBroadcast();
		}
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		playerStatus = STATUS_STOPPED;
		sendStateChangeBroadcast();
		remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
		audioManager.abandonAudioFocus(this);
		releaseResources();
		if (extra == -1004) {
			Toast.makeText(this, R.string.error_playing_song_message_text, Toast.LENGTH_SHORT).show();
		}
		return true;
	}

	@Override
	public void onAudioFocusChange(int focusChange) {
		Logging.Log(LOG_TAG, "Focus changed: " + focusChange);
		switch (focusChange) {
			case AudioManager.AUDIOFOCUS_GAIN:
				if (isPausedByFocus && playerStatus == STATUS_PAUSED){
					play();
					isPausedByFocus = false;
				} else if (isDuckedByFocus && playerStatus == STATUS_PLAYING) {
					mediaPlayer.setVolume(1.0f,1.0f);
					isDuckedByFocus = false;
				}
				break;
			case AudioManager.AUDIOFOCUS_LOSS:
				stop();
				break;
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
				if (playerStatus == STATUS_PAUSED) {
					pause();
					isPausedByFocus = true;
				}
				break;
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
				if (playerStatus == STATUS_PLAYING) {
					mediaPlayer.setVolume(DUCK_VOLUME, DUCK_VOLUME);
					isDuckedByFocus = true;
				}
				break;
		}
	}

	private void listen(String url, boolean stream){
		Logging.Log(LOG_TAG, "Setting mediaplayer (stream=" + stream + "): " + url);
				
		createMediaPlayer();
		
		isStreaming = stream;
		try {
			mediaPlayer.setDataSource(url);
		} catch (Exception e) {
			Logging.Log(LOG_TAG, "MediaPlayer Exception");
			e.printStackTrace();
		} 
		mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		
		if (isStreaming) {
			wifiLock.acquire();
		} else if (wifiLock.isHeld()) {
			wifiLock.release();
		}

		Logging.Log(LOG_TAG, "Preparing mediaplayer");
		mediaPlayer.prepareAsync();
		playerStatus = STATUS_BUFFERING;
		sendStateChangeBroadcast();
		setUpNotification();
		setUpRemoteControl(true);
		remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_BUFFERING);
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		startPlayer();
	}

	private void startPlayer() {
		int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
		if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED)  {
			playerStatus = STATUS_STOPPED;
		}
		else {
			Logging.Log(LOG_TAG, "Starting mediaplayer");
			mediaPlayer.setVolume(1.0f, 1.0f);
			mediaPlayer.start();
	
			
			if (updateProgressThread != null) {
				updateProgressThread.interrupt();
				try {
					updateProgressThread.join(500);
				} catch (InterruptedException e) {
				}
			}
			updateProgressThread = new Thread() {
				public void run() {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						return;
					}
					while (!this.isInterrupted()) {
						sendPositionChangeBroadcast();
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							break;
						}
					}
				}
			};
			updateProgressThread.start();
			playerStatus = STATUS_PLAYING;
			setUpRemoteControl(false);
			setUpNotification();		
			remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
			sendStateChangeBroadcast();
		}
	}
	
	private void setUpRemoteControl(boolean buffering) {
		audioManager.registerMediaButtonEventReceiver(remoteReceiver);
		if (remoteControlClient == null) {
			Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
			intent.setComponent(remoteReceiver);
			remoteControlClient = new RemoteControlClient(PendingIntent.getBroadcast(this, 0, intent, 0));
			audioManager.registerRemoteControlClient(remoteControlClient);
			remoteControlClient.setTransportControlFlags(
					RemoteControlClient.FLAG_KEY_MEDIA_PLAY |
					RemoteControlClient.FLAG_KEY_MEDIA_PAUSE |
					RemoteControlClient.FLAG_KEY_MEDIA_NEXT |
					RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS |
					RemoteControlClient.FLAG_KEY_MEDIA_STOP |
					RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE );
		}
		remoteControlClient.editMetadata(true)
			.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, getCurrentSong().getShowArtist())
			.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, getCurrentSong().getShowTitle())
			.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, getCurrentSong().getSongTitle() + (buffering ? " (buffering)" : ""))
			.putBitmap(MetadataEditor.BITMAP_KEY_ARTWORK, albumArt)
			.apply();
	}
	
	@SuppressWarnings("deprecation")
	private void setUpNotification() {
		ArchiveSongObj currentSong = getCurrentSong();
		String state = "";
		if (playerStatus == STATUS_BUFFERING)
			state = " (buffering)";
		else if (playerStatus == STATUS_PAUSED)
			state = " (paused)";
		mBuilder = new Notification.Builder(this)
				.setSmallIcon(R.drawable.musicnote)
				.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.icon))
				.setContentTitle(currentSong.getSongTitle() + state)
				.setContentText(currentSong.getShowTitle())
				.setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, SearchScreen.class).putExtra("type", 2).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_UPDATE_CURRENT))
				.setOngoing(true);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			finishNewNotification(mBuilder, currentSong, state);
		}
		else {
			startForeground(NOTIFICATION_ID, mBuilder.getNotification());
		}
	}
	
	@TargetApi(16)
	private void finishNewNotification(Notification.Builder mBuilder, ArchiveSongObj currentSong, String state) {
//		mBuilder.addAction(R.drawable.previousbutton, "", PendingIntent.getService(this, 0, new Intent(PlaybackService.ACTION_PREV), PendingIntent.FLAG_UPDATE_CURRENT));
//		if (playerStatus == STATUS_PLAYING || playerStatus == STATUS_BUFFERING)
//			mBuilder.addAction(R.drawable.pausebutton, "", PendingIntent.getService(this, 0, new Intent(PlaybackService.ACTION_PAUSE), PendingIntent.FLAG_UPDATE_CURRENT));
//		else
//			mBuilder.addAction(R.drawable.playbutton, "", PendingIntent.getService(this, 0, new Intent(PlaybackService.ACTION_PLAY), PendingIntent.FLAG_UPDATE_CURRENT));
//		mBuilder.addAction(R.drawable.nextbutton, "", PendingIntent.getService(this, 0, new Intent(PlaybackService.ACTION_NEXT), PendingIntent.FLAG_UPDATE_CURRENT));
//		mBuilder.setContent(remoteViews);
		RemoteViews collapsedViews = new RemoteViews(getPackageName(), R.layout.notification_collapsed_view);
		RemoteViews expandedViews = new RemoteViews(getPackageName(), R.layout.notification_expanded_view);
		
		collapsedViews.setTextViewText(R.id.notification_collapsed_title, currentSong.getSongTitle() + state);
		collapsedViews.setTextViewText(R.id.notification_collapsed_text, currentSong.getShowTitle());
		collapsedViews.setOnClickPendingIntent(R.id.notification_collapsed_next, PendingIntent.getService(this, 0, new Intent(PlaybackService.ACTION_NEXT), PendingIntent.FLAG_UPDATE_CURRENT));
		collapsedViews.setOnClickPendingIntent(R.id.notification_collapsed_close, PendingIntent.getService(this, 0, new Intent(PlaybackService.ACTION_STOP), PendingIntent.FLAG_UPDATE_CURRENT));

		expandedViews.setTextViewText(R.id.notification_expanded_title, currentSong.getSongTitle() + state);
		expandedViews.setTextViewText(R.id.notification_expanded_text, currentSong.getShowTitle());
		expandedViews.setOnClickPendingIntent(R.id.notification_expanded_prev, PendingIntent.getService(this, 0, new Intent(PlaybackService.ACTION_PREV), PendingIntent.FLAG_UPDATE_CURRENT));
		expandedViews.setOnClickPendingIntent(R.id.notification_expanded_next, PendingIntent.getService(this, 0, new Intent(PlaybackService.ACTION_NEXT), PendingIntent.FLAG_UPDATE_CURRENT));
		expandedViews.setOnClickPendingIntent(R.id.notification_expanded_close, PendingIntent.getService(this, 0, new Intent(PlaybackService.ACTION_STOP), PendingIntent.FLAG_UPDATE_CURRENT));
				
		if (playerStatus == STATUS_PLAYING || playerStatus == STATUS_BUFFERING) {
			collapsedViews.setImageViewBitmap(R.id.notification_collapsed_play, BitmapFactory.decodeResource(getResources(), R.drawable.pausebutton));
			collapsedViews.setOnClickPendingIntent(R.id.notification_collapsed_play, PendingIntent.getService(this, 0, new Intent(PlaybackService.ACTION_PAUSE), PendingIntent.FLAG_UPDATE_CURRENT));
			expandedViews.setImageViewBitmap(R.id.notification_expanded_play, BitmapFactory.decodeResource(getResources(), R.drawable.pausebutton));
			expandedViews.setOnClickPendingIntent(R.id.notification_expanded_play, PendingIntent.getService(this, 0, new Intent(PlaybackService.ACTION_PAUSE), PendingIntent.FLAG_UPDATE_CURRENT));
		} else {
			collapsedViews.setImageViewBitmap(R.id.notification_collapsed_play, BitmapFactory.decodeResource(getResources(), R.drawable.playbutton));
			collapsedViews.setOnClickPendingIntent(R.id.notification_collapsed_play, PendingIntent.getService(this, 0, new Intent(PlaybackService.ACTION_PLAY), PendingIntent.FLAG_UPDATE_CURRENT));
			expandedViews.setImageViewBitmap(R.id.notification_expanded_play, BitmapFactory.decodeResource(getResources(), R.drawable.playbutton));
			expandedViews.setOnClickPendingIntent(R.id.notification_expanded_play, PendingIntent.getService(this, 0, new Intent(PlaybackService.ACTION_PLAY), PendingIntent.FLAG_UPDATE_CURRENT));
		}
		
		Notification n = mBuilder.build();
		n.contentView = collapsedViews;
		n.bigContentView = expandedViews;
		startForeground(NOTIFICATION_ID, n);
	}
	
	private void releaseResources() {
		Logging.Log(LOG_TAG,"Releasing Resources");
		// Set service as background and remove notifications
		stopForeground(true);
		audioManager.unregisterRemoteControlClient(remoteControlClient);
		audioManager.unregisterMediaButtonEventReceiver(remoteReceiver);
		audioManager.abandonAudioFocus(this);
		if (mediaPlayer != null){
			mediaPlayer.reset();
			mediaPlayer.release();
			mediaPlayer = null;
		}
		
		if (wifiLock.isHeld()) {
			wifiLock.release();
		}
	}
	
	private void pausePlayer() {
		mediaPlayer.pause();
		playerStatus = STATUS_PAUSED;
		setUpNotification();
		if (updateProgressThread != null) {
			updateProgressThread.interrupt();
			try {
				updateProgressThread.join(500);
			} catch (InterruptedException e) {
			}
		}
		remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
		sendStateChangeBroadcast();
	}
	
	private void stopPlayer() {
		mediaPlayer.stop();
		mediaPlayer.reset();
		playerStatus = STATUS_STOPPED;
		if (updateProgressThread != null) {
			updateProgressThread.interrupt();
			try {
				updateProgressThread.join(500);
			} catch (InterruptedException e) {
			}
		}
		notificationManager.cancel(NOTIFICATION_ID);
		remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
		releaseResources();
		sendStateChangeBroadcast();
	}
	
	private void togglePlayPause() {
		if (playerStatus == STATUS_PLAYING) {
			pausePlayer();
		} else if (playerStatus == STATUS_PAUSED) {
			startPlayer();
		}
		else if (playerStatus == STATUS_STOPPED && songArray.size() > 0) {
			nowPlayingPosition = (nowPlayingPosition == -1 || nowPlayingPosition > songArray.size()) ? 0 : nowPlayingPosition;
			db.updatePref("nowPlayingPosition", String.valueOf(nowPlayingPosition));
			playPosition(nowPlayingPosition);
		}
	}
	
	private void play() {
		if (playerStatus == STATUS_PAUSED) {
			startPlayer();
		}
		else if (playerStatus == STATUS_STOPPED && songArray.size() > 0) {
			nowPlayingPosition = (nowPlayingPosition == -1 || nowPlayingPosition > songArray.size()) ? 0 : nowPlayingPosition;
			db.updatePref("nowPlayingPosition", String.valueOf(nowPlayingPosition));
			playPosition(nowPlayingPosition);
		}
	}
	
	private void pause() {
		if (playerStatus == STATUS_PLAYING) {
			pausePlayer();
		}
	}
	
	private void stop() {
		if (playerStatus != STATUS_STOPPED) {
			stopPlayer();
		}
	}
	
	private void next() {
		if (nowPlayingPosition < songArray.size() - 1) {
			playPosition(nowPlayingPosition+1);
		}
	}
	
	private void previous() {
		if (nowPlayingPosition > 0) {
			playPosition(nowPlayingPosition-1);
		}
	}
	
	private void seekTo(Intent intent) {
		if (playerStatus == STATUS_PLAYING || playerStatus == STATUS_PAUSED) {
			mediaPlayer.seekTo(intent.getIntExtra(EXTRA_SEEK_POSITON, 0));
		}
	}
	
	private void playPos(Intent intent) {
		playPosition(intent.getIntExtra(EXTRA_PLAYLIST_POSITION,0));
	}
	
	private void playPosition(int pos) {
		Logging.Log(LOG_TAG, "playPosition called: nowplaying=" + nowPlayingPosition + " newpos=" + pos);
		if (nowPlayingPosition != pos || playerStatus == STATUS_STOPPED) {
			nowPlayingPosition = pos;
			db.updatePref("nowPlayingPosition", String.valueOf(nowPlayingPosition));
			ArchiveSongObj currentSong = getCurrentSong();
			Logging.Log(LOG_TAG, "CURRENT SONG: " + currentSong.getSongTitle());
			try {
				listen(currentSong.getSongPath(db),!currentSong.doesExist(db));
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			sendStateChangeBroadcast();
		}
		else if (nowPlayingPosition == pos && playerStatus == STATUS_PAUSED){
			Logging.Log(LOG_TAG,"About to play, position set...");
			startPlayer();
		}
		else if (nowPlayingPosition == pos && playerStatus == STATUS_PLAYING) {
			pause();
		}
	}
	
	private void queueSong(Intent intent) {
		songArray.add((ArchiveSongObj)intent.getSerializableExtra(EXTRA_SONG));
		db.addSongToNowPlaying((ArchiveSongObj)intent.getSerializableExtra(EXTRA_SONG));
		if(intent.getBooleanExtra(EXTRA_DO_PLAY, false)){
			this.playPosition(songArray.size()-1);
		}
		sendStateChangeBroadcast();
	}
	
	@SuppressWarnings("unchecked")
	private void queueShow(Intent intent) {
		stop();
		songArray.clear();
		songArray.addAll((ArrayList<ArchiveSongObj>)intent.getSerializableExtra(EXTRA_PLAYLIST));
		db.setNowPlayingSongs(songArray);
		Logging.Log(LOG_TAG,"NUM SONGS: " + songArray.size());
		nowPlayingPosition = -1;
		if (intent.getBooleanExtra(EXTRA_DO_PLAY, false)) {
			playPosition(intent.getIntExtra(EXTRA_PLAYLIST_POSITION, 0));
		}
		sendStateChangeBroadcast();
	}
	
	private void moveSong(Intent intent) {
		int from = intent.getIntExtra(EXTRA_MOVE_FROM, 0);
		int to = intent.getIntExtra(EXTRA_MOVE_TO, 0);
		Logging.Log(LOG_TAG,"Currently playing song is #" + this.nowPlayingPosition + " out of " + (this.songArray.size()-1));
		Logging.Log(LOG_TAG,"Moved from: " + from + " to: " + to);
		ArchiveSongObj song = songArray.get(from);
		songArray.remove(from);
		songArray.add(to, song);
		db.setNowPlayingSongs(songArray);
		if(from==nowPlayingPosition){
			Logging.Log(LOG_TAG,"Setting nowPlayingPosition to \"to\".");
			nowPlayingPosition=to;
		} else if(from<nowPlayingPosition){
			if(to>=nowPlayingPosition){
				Logging.Log(LOG_TAG,"Decrementing nowPlayingPosition.");
				nowPlayingPosition--;
			}
		} else{
			if(to<=nowPlayingPosition){
				Logging.Log(LOG_TAG,"Incrementing nowPlayingPosition.");
				nowPlayingPosition++;
			}
		}
		db.updatePref("nowPlayingPosition", String.valueOf(nowPlayingPosition));
		Logging.Log(LOG_TAG,"After move playing song is #" + this.nowPlayingPosition + " out of " + (this.songArray.size()-1));
//		nowPlayingPosition = nowPlayingPosition == from ? to : nowPlayingPosition > from ? 1 : 0;
		sendStateChangeBroadcast();
	}
	
	private void deleteSong(Intent intent) {
		
	}
	
	/** Warning, the caller of this method must check for null returns.
	 * 
	 */
	private ArchiveSongObj getCurrentSong() {
		if(nowPlayingPosition==-1){
			return null;
		}
		return songArray.get(nowPlayingPosition);
	}
	
	private int getCurrentDuration() {
		if (playerStatus == STATUS_PLAYING || playerStatus == STATUS_PAUSED) {
			return mediaPlayer.getDuration();
		}
		else {
			return 0;
		}
	}
	
	private int getPlayProgress() {
		if (playerStatus == STATUS_PLAYING || playerStatus == STATUS_PAUSED) {
			return mediaPlayer.getCurrentPosition();
		}
		else {
			return 0;
		}
	}
	
	private int getBufferProgress() {
		synchronized(this) {
			if (playerStatus == STATUS_PLAYING || playerStatus == STATUS_PAUSED){
				return (int) (isStreaming ? ((lastBufferPercent / 100.0) * mediaPlayer.getDuration()) : mediaPlayer.getDuration());
			}
			else {
				return 0;
			}
		}
	}

	private void sendStateChangeBroadcast() {
		Intent stateBroadcast = new Intent(SERVICE_STATE);
		stateBroadcast.putExtra(EXTRA_STATUS,this.playerStatus);
		stateBroadcast.putExtra(EXTRA_PLAY_PROGRESS, getPlayProgress());
		stateBroadcast.putExtra(EXTRA_PLAY_DURATION, getCurrentDuration());
		stateBroadcast.putExtra(EXTRA_BUFFER_PROGRESS, getBufferProgress());
		stateBroadcast.putExtra(EXTRA_PLAYLIST, songArray);
		stateBroadcast.putExtra(EXTRA_PLAYLIST_POSITION, this.nowPlayingPosition);
		if (nowPlayingPosition > -1 && nowPlayingPosition < songArray.size()) {
			stateBroadcast.putExtra(EXTRA_TITLE,playerStatus == STATUS_PAUSED ? songArray.get(nowPlayingPosition).getSongTitle() 
					: songArray.get(nowPlayingPosition).getSongArtistAndTitle());
		}
		else {
			stateBroadcast.putExtra(EXTRA_TITLE, "");
		}
		
		sendBroadcast(stateBroadcast);
	}
	
	private void sendPositionChangeBroadcast() {
		Intent positionBroadcast = new Intent(SERVICE_POSITION);
		positionBroadcast.putExtra(EXTRA_STATUS,this.playerStatus);
		positionBroadcast.putExtra(EXTRA_PLAY_PROGRESS, getPlayProgress());
		positionBroadcast.putExtra(EXTRA_PLAY_DURATION, getCurrentDuration());
		positionBroadcast.putExtra(EXTRA_BUFFER_PROGRESS, getBufferProgress());
		sendBroadcast(positionBroadcast);
	}
	
	private void downloadShow() {
		if (songArray.size() > 0) {
			DownloadingAsyncTask task = new DownloadingAsyncTask(this);
			task.execute(songArray.toArray(new ArchiveSongObj[songArray.size()]));
		}
	}
}