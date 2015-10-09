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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;

import com.code.android.vibevault.VibeVault;

public class PlaybackService extends Service implements OnPreparedListener,
    OnBufferingUpdateListener, OnCompletionListener, OnErrorListener,
    OnInfoListener {

  private static final String LOG_TAG = PlaybackService.class.getName();

  private static final String SERVICE_PREFIX = "com.code.android.vibevault.";
  public static final String SERVICE_CHANGE_NAME = SERVICE_PREFIX + "CHANGE";
  public static final String SERVICE_PLAYLIST_NAME = SERVICE_PREFIX + "PLAYLIST";
  public static final String SERVICE_UPDATE_NAME = SERVICE_PREFIX + "UPDATE";
  
  public static final String EXTRA_TITLE = "title";
  public static final String EXTRA_DOWNLOADED = "downloaded";
  public static final String EXTRA_DURATION = "duration";
  public static final String EXTRA_POSITION = "position";
  public static final String EXTRA_STATUS = "status";
  
  private ArchiveSongObj currentSong;

  private MediaPlayer mediaPlayer;
  private boolean isPrepared = false;
  private boolean isPreparing = false;
  private boolean isPaused = false;
  private boolean isStreaming = true;

  private StreamProxy proxy;
  private NotificationManager notificationManager;
  private static final int NOTIFICATION_ID = 1;
  private int bindCount = 0;
  
  private TelephonyManager telephonyManager;
  private PhoneStateListener listener;
  private boolean isPausedInCall = false;
  private Intent lastChangeBroadcast;
  private Intent lastUpdateBroadcast;
  private int lastBufferPercent = 0;
  private Thread updateProgressThread;

  // Amount of time to rewind playback when resuming after call 
  private final static int RESUME_REWIND_TIME = 3000;

  @Override
  public void onCreate() {
    mediaPlayer = new MediaPlayer();
    mediaPlayer.setOnBufferingUpdateListener(this);
    mediaPlayer.setOnCompletionListener(this);
    mediaPlayer.setOnErrorListener(this);
    mediaPlayer.setOnInfoListener(this);
    mediaPlayer.setOnPreparedListener(this);
    notificationManager = (NotificationManager) getSystemService(
        Context.NOTIFICATION_SERVICE);
    Log.w(LOG_TAG, "Playback service created");

    telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
    // Create a PhoneStateListener to watch for offhook and idle events
    listener = new PhoneStateListener() {
      @Override
      public void onCallStateChanged(int state, String incomingNumber) {
        switch (state) {
        case TelephonyManager.CALL_STATE_OFFHOOK:
        case TelephonyManager.CALL_STATE_RINGING:
          // Phone going offhook or ringing, pause the player.
          if (isPlaying()) {
            pause();
            isPausedInCall = true;
          }
          break;
        case TelephonyManager.CALL_STATE_IDLE:
          // Phone idle. Rewind a couple of seconds and start playing.
          if (isPausedInCall) {
            seekTo(Math.max(0, getPosition() - RESUME_REWIND_TIME));
            play();
          }
          break;
        }
      }
    };

    // Register the listener with the telephony manager.
    telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
    sendLastChangeBroadcast();
  }

  @Override
  public IBinder onBind(Intent arg0) {
    bindCount++;
    
    return new ListenBinder();
  }

  @Override
  public boolean onUnbind(Intent arg0) {
    bindCount--;
    
    if (!isPlaying() && bindCount == 0 && !isPreparing && VibeVault.playList.isEmpty()) {
      Log.w(LOG_TAG, "Will stop self");
      stopSelf();
    } else {
      
    }
    return false;
  }

  synchronized public boolean isPlaying() {
    if (isPrepared) {
      return mediaPlayer.isPlaying();
    }
    return false;
  }
  
  synchronized public boolean isStopped(){
	  return !isPlaying();
  }

  synchronized public int getPosition() {
    if (isPrepared) {
      return mediaPlayer.getCurrentPosition();
    }
    return 0;
  }

  synchronized public int getDuration() {
    if (isPrepared) {
      return mediaPlayer.getDuration();
    }
    return 0;
  }

  synchronized public int getCurrentPosition() {
    if (isPrepared) {
      return mediaPlayer.getCurrentPosition();
    }
    return 0;
  }

  synchronized public void seekTo(int pos) {
    if (isPrepared) {
      mediaPlayer.seekTo(pos);
    }
  }

  synchronized public void play() {
    if (!isPrepared || currentSong == null) {
      Log.e(LOG_TAG, "play - not prepared");
      
      return;
    }
    isPaused = false;
    
    mediaPlayer.start();

    int icon = R.drawable.musicnote;
    CharSequence contentText = currentSong.getShowTitle();
    long when = System.currentTimeMillis();
    Notification notification = new Notification(icon, contentText, when);
    notification.flags = Notification.FLAG_NO_CLEAR
        | Notification.FLAG_ONGOING_EVENT;
    Context c = getApplicationContext();
    CharSequence title = currentSong.toString();
    Intent notificationIntent;
    notificationIntent = new Intent(this, NowPlayingScreen.class);
    notificationIntent.setAction(Intent.ACTION_VIEW);
    notificationIntent.addCategory(Intent.CATEGORY_DEFAULT);
    notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    PendingIntent contentIntent = PendingIntent.getActivity(c, 0,
        notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
    notification.setLatestEventInfo(c, title, contentText, contentIntent);
    notificationManager.notify(NOTIFICATION_ID, notification);

    sendLastChangeBroadcast();
  }
  
  private void sendLastChangeBroadcast(){
	  if (lastChangeBroadcast != null) {
	      getApplicationContext().removeStickyBroadcast(lastChangeBroadcast);
	    }
	    lastChangeBroadcast = new Intent(SERVICE_CHANGE_NAME);
	    if(mediaPlayer.isPlaying()){
	    	lastChangeBroadcast.putExtra(EXTRA_TITLE, currentSong.getShowTitle() + " - " + currentSong.toString());
	    	lastChangeBroadcast.putExtra(EXTRA_STATUS, "playing");
	    }
	    else if(isPaused){
	    	lastChangeBroadcast.putExtra(EXTRA_TITLE, currentSong.getShowTitle() + " - " + currentSong.toString());
	    	lastChangeBroadcast.putExtra(EXTRA_STATUS, "paused");
	    }
	    else if(isPreparing){
	    	lastChangeBroadcast.putExtra(EXTRA_TITLE, "Buffering " + currentSong.toString() + "...");
	    	lastChangeBroadcast.putExtra(EXTRA_STATUS, "preparing");
	    }
	    else{
	    	lastChangeBroadcast.putExtra(EXTRA_TITLE, "Nothing Playing...");
	    	lastChangeBroadcast.putExtra(EXTRA_STATUS, "stopped");
	    }
	    getApplicationContext().sendStickyBroadcast(lastChangeBroadcast);
	  }

  synchronized public void pause() {
    
    if (isPrepared) {
      mediaPlayer.pause();
      isPaused = true;
    }
    sendLastChangeBroadcast();
    notificationManager.cancel(NOTIFICATION_ID);
  }
  
  public boolean isPaused(){
	  return isPaused;
  }
  
  public boolean isStreaming(){
	  return isStreaming;
  }

  synchronized public void stop() {
    
    if (isPrepared) {
      if (proxy != null) {
        proxy.stop();
        proxy = null;
      }
      mediaPlayer.stop();
      isPrepared = false;
    }
    sendLastChangeBroadcast();
    updateProgress();
    cleanup();
  }
  
  public void playSong(ArchiveSongObj song){
	  
	  if(currentSong == null || !currentSong.equals(song) || !isPlaying()){
	    try {
	    	currentSong = song;
			listen(song.getSongPath(),!song.doesExist());
		} catch (Exception e) {
			
			e.printStackTrace();
		}
	  }
  }

  public void playSongFromPlaylist(int position){
	  if(!VibeVault.playList.isEmpty() && position > -1 && position < VibeVault.playList.size())
		{
			VibeVault.nowPlayingPosition = position;
			
			playSong(VibeVault.playList.getSong(VibeVault.nowPlayingPosition));
		} else{
			stop();
		}
  }
	
	public int getPlayingIndex(){
		return currentSong != null ? VibeVault.nowPlayingPosition : -1;
	}
	
	public int enqueue(ArchiveSongObj song){
		
		return VibeVault.playList.enqueue(song);
	}
	
	public void dequeue(int position){
		if(position > -1 && position <= VibeVault.playList.size()){
			if(VibeVault.nowPlayingPosition == position){
				stop();
				if(VibeVault.playList.size() >= 1){
					if(position == VibeVault.playList.size()){
						VibeVault.nowPlayingPosition = VibeVault.nowPlayingPosition - 1;
					}
					currentSong = VibeVault.playList.getSong(VibeVault.nowPlayingPosition);
				}
				else{
					VibeVault.nowPlayingPosition = -1;
					currentSong = null;
				}
			}
			else{
				VibeVault.nowPlayingPosition = VibeVault.playList.exists(currentSong);
			}
		    getApplicationContext().sendBroadcast(new Intent(SERVICE_PLAYLIST_NAME));
		}
	}
	
	public void playPrev()
	{
		if(VibeVault.nowPlayingPosition > 0)
		{
			
			VibeVault.nowPlayingPosition--;
			playSong(VibeVault.playList.getSong(VibeVault.nowPlayingPosition));
		}
	}
	
	public void playNext()
	{
		if(VibeVault.nowPlayingPosition + 1 < VibeVault.playList.size())
		{
			
			VibeVault.nowPlayingPosition++;
			playSong(VibeVault.playList.getSong(VibeVault.nowPlayingPosition));
		}
	}
	
	// CAN RETURN NULL CALLER MUST CHECK!!!!!!!!
	public ArchiveSongObj getPlayingSong(){
		if(VibeVault.playList.size()!=0&&!isStopped()){
			return VibeVault.playList.getSong(VibeVault.nowPlayingPosition);
		} else{
			return null;
		}
	}
	
	public ArchiveSongObj getSong(int index){
		if(index > -1 && index < VibeVault.playList.size()){
			return VibeVault.playList.getSong(index);
		}
		else{
			return null;
		}
	}
	
	/*public String getPlayingSongTitle(){
		return currentSong.toString();
	}
	
	public String getPlayingShowTitle(){
		return currentSong.getShowTitle();
	}*/
	
	public String getPlayingShowArtist(){
		return currentSong.getShowArtist();
	}
	
	public void updatePlaying(){
		if(currentSong!=null){
			VibeVault.nowPlayingPosition = VibeVault.playList.getList().indexOf(currentSong);
		}
	}
  
  /**
   * Start listening to the given URL.
   */
  public void listen(String url, boolean stream)
      throws IllegalArgumentException, IllegalStateException, IOException {
    // First, clean up any existing audio.
	  
	  if (isPlaying()) {
      stop();
    }
	isStreaming = stream;
    
    String playUrl = url;
    // From 2.2 on (SDK ver 8), the local mediaplayer can handle Shoutcast
    // streams natively. Let's detect that, and not proxy.
    
    int sdkVersion = 0;
    try {
      sdkVersion = Integer.parseInt(Build.VERSION.SDK);
    } catch (NumberFormatException e) {
    }

    /*if (stream && sdkVersion < 8) {
      if (proxy == null) {
        proxy = new StreamProxy();
        proxy.init();
        proxy.start();
      }
      String proxyUrl = String.format("http://127.0.0.1:%d/%s",
          proxy.getPort(), url);
      playUrl = proxyUrl;
    }*/

    synchronized (this) {
      
      mediaPlayer.reset();
      mediaPlayer.setDataSource(playUrl);
      mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
      
      isPreparing = true;
      mediaPlayer.prepareAsync();
      sendLastChangeBroadcast();
      
    }
  }

  @Override
  public void onPrepared(MediaPlayer mp) {
    
    synchronized (this) {
      if (mediaPlayer != null) {
        isPrepared = true;
      }
    }
    play();
    isPreparing = false;
    updateProgressThread = new Thread(new Runnable() {
      public void run() {
        // Initially, don't send any updates, since it takes a while for the
        // media player to settle down. 
        try {
          Thread.sleep(2000);
        } catch (InterruptedException e) {
          return;
        }
        while (true) {
          updateProgress();
          try {
            Thread.sleep(500);
          } catch (InterruptedException e) {
            break;
          }
        }
      }
    });
    updateProgressThread.start();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.w(LOG_TAG, "Service exiting");

    if (updateProgressThread != null) {
      updateProgressThread.interrupt();
      try {
        updateProgressThread.join(3000);
      } catch (InterruptedException e) {
        Log.e(LOG_TAG, "", e);
      }
    }

    stop();
    synchronized (this) {
      if (mediaPlayer != null) {
        mediaPlayer.release();
        mediaPlayer = null;
      }
    }

    telephonyManager.listen(listener, PhoneStateListener.LISTEN_NONE);
  }

  public class ListenBinder extends Binder {

    public PlaybackService getService() {
      return PlaybackService.this;
    }
  }

  @Override
  public void onBufferingUpdate(MediaPlayer mp, int progress) {
    if (isPrepared) {
      lastBufferPercent = progress;
      updateProgress();
    }
  }

  /**
   * Sends an UPDATE broadcast with the latest info.
   */
  private synchronized void updateProgress() {
    if (lastUpdateBroadcast != null) {
        getApplicationContext().removeStickyBroadcast(lastUpdateBroadcast);
      }
    if (isPrepared && mediaPlayer != null && (mediaPlayer.isPlaying() || isPaused)) {
      // Update broadcasts are sticky, so when a new receiver connects, it will
      // have the data without polling.
      
      lastUpdateBroadcast = new Intent(SERVICE_UPDATE_NAME);
      lastUpdateBroadcast.putExtra(EXTRA_DURATION, mediaPlayer.getDuration());
      lastUpdateBroadcast.putExtra(EXTRA_DOWNLOADED, isStreaming ?
          (int) ((lastBufferPercent / 100.0) * mediaPlayer.getDuration()) : mediaPlayer.getDuration());
      lastUpdateBroadcast.putExtra(EXTRA_POSITION,
          mediaPlayer.getCurrentPosition());
      getApplicationContext().sendStickyBroadcast(lastUpdateBroadcast);
    }
    else{
    	lastUpdateBroadcast = new Intent(SERVICE_UPDATE_NAME);
        lastUpdateBroadcast.putExtra(EXTRA_DURATION, 0);
        lastUpdateBroadcast.putExtra(EXTRA_DOWNLOADED, 0);
        lastUpdateBroadcast.putExtra(EXTRA_POSITION, 0);
        getApplicationContext().sendStickyBroadcast(lastUpdateBroadcast);
    }
  }
  
  @Override
  public void onCompletion(MediaPlayer mp) {
    Log.w(LOG_TAG, "onComplete()");

    synchronized (this) {
      if (!isPrepared) {
        // This file was not good and MediaPlayer quit
        Log.w(LOG_TAG,
            "MediaPlayer refused to play current item. Bailing on prepare.");
      }
    }

    cleanup();
    
    playNext();
    
    if (bindCount == 0 && !isPlaying() && !isPreparing && VibeVault.playList.isEmpty()) {
        Log.w(LOG_TAG, "Stopping Service stopSelf()");
      stopSelf();
    }
  }

  @Override
  public boolean onError(MediaPlayer mp, int what, int extra) {
    Log.w(LOG_TAG, "onError(" + what + ", " + extra + ")");
    synchronized (this) {
      if (!isPrepared) {
        // This file was not good and MediaPlayer quit
        Log.w(LOG_TAG,
            "MediaPlayer refused to play current item. Bailing on prepare.");
      }
    }
    return false;
  }

  @Override
  public boolean onInfo(MediaPlayer arg0, int arg1, int arg2) {
    Log.w(LOG_TAG, "onInfo(" + arg1 + ", " + arg2 + ")");
    return false;
  }

  /**
   * Remove all intents and notifications about the last media.
   */
  private void cleanup() {
    notificationManager.cancel(NOTIFICATION_ID);
    if (lastChangeBroadcast != null) {
      getApplicationContext().removeStickyBroadcast(lastChangeBroadcast);
    }
    if (lastUpdateBroadcast != null) {
      getApplicationContext().removeStickyBroadcast(lastUpdateBroadcast);
    }
  }
}