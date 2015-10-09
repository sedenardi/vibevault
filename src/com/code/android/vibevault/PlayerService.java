/*
 * PlayerService.java
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

import java.util.ArrayList;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.code.android.vibevault.R;

public class PlayerService extends Service {
	
	private Intent broadcastSong = new Intent(VibeVault.BROADCAST_SONG_TITLE);
	private Intent broadcastPlayStatus = new Intent(VibeVault.BROADCAST_PLAYER_STATUS);
	private Intent broadcastPlaylist = new Intent(VibeVault.BROADCAST_PLAYLIST);
	private final Binder binder = new MPlayerBinder();

	private static MediaPlayer mPlayer = null;
	private static boolean playReady = false;
	private static boolean paused = false;
	private static boolean pauseFromPhone = false;
	//private static ArchivePlaylistObj playList;
	private static ArchiveSongObj currentSong;
	private static int currentPos = -1;
	private String currentSongTitle = "Nothing Playing...";
	private String currentSongShow = "Nothing Playing...";
	private static String playListTitle = "Empty";
	
	NotificationManager pNotificationManager;
	
	int icon = R.drawable.musicnote;
	CharSequence tickerText = "Playing";
	
	Notification notification;
	PendingIntent contentIntent;
	
	TelephonyManager tm = null;
	
	@Override
	public void onCreate()
	{
		super.onCreate();

		if(mPlayer == null)
		{
			mPlayer = new MediaPlayer();
			mPlayer.reset();
			mPlayer.setOnPreparedListener(asyncReady);
			mPlayer.setOnCompletionListener(playDone);
		}
		currentSong = null;
		currentSongTitle = "Nothing Playing...";
		currentSongShow = "Nothing Playing...";
		//playList = new ArchivePlaylistObj();
		pNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notification = new Notification(icon, tickerText, System.currentTimeMillis());
		contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, NowPlayingScreen.class), Intent.FLAG_ACTIVITY_NEW_TASK);
		
		tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		int events = PhoneStateListener.LISTEN_CALL_STATE;
		tm.listen(phoneStateListener, events);
		registerReceiver(headphoneIntentReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId){

		return START_STICKY;
	}
	
	@Override
	public IBinder onBind(Intent arg0) 
	{

		return(binder);
	}
	
	public class MPlayerBinder extends Binder
	{
		PlayerService getService()
		{
			return(PlayerService.this);
		}
	}

	@Override
	public void onDestroy()
	{

		super.onDestroy();
	}
	
	public void playSong(ArchiveSongObj song)
	{

		if(currentSong == null || !currentSong.equals(song) || isStopped()){
			try 
			{
				currentSong = song;

				mPlayer.reset();
				mPlayer.setDataSource(currentSong.getSongPath());
				mPlayer.prepareAsync();
				setNowPlayingText();
				currentPos = VibeVault.playList.getList().indexOf(song);
			} 
			catch (Exception e) 
			{
				Log.e(VibeVault.PLAYER_SERVICE_TAG,"Error playing song: " + e.getStackTrace().toString());
				e.printStackTrace();
			}
		}
	}
	
	public void playSongFromPlaylist(int position)
	{
		if(!VibeVault.playList.isEmpty() && position > -1 && position < VibeVault.playList.size())
		{
			currentPos = position;
			VibeVault.nowPlayingPosition = currentPos;

			playSong(VibeVault.playList.getSong(currentPos));
		}
	}
	
	public int getPlayingIndex(){
		return currentSong != null ? currentPos : -1;
	}
	
	public int enqueue(ArchiveSongObj song){

		return VibeVault.playList.enqueue(song);
	}
	
	public void dequeue(int position){
		if(position > -1 && position <= VibeVault.playList.size()){
			if(currentPos == position){
				stop();
				if(VibeVault.playList.size() >= 1){
					if(position == VibeVault.playList.size()){
						currentPos = currentPos - 1;
						VibeVault.nowPlayingPosition = currentPos;
					}
					currentSong = VibeVault.playList.getSong(currentPos);
				}
				else{
					currentPos = -1;
					VibeVault.nowPlayingPosition = currentPos;
					currentSong = null;
				}
			}
			else{
				currentPos = VibeVault.playList.exists(currentSong);
				VibeVault.nowPlayingPosition = currentPos;
			}
			setNowPlayingText();
			sendBroadcast(broadcastSong);
			sendBroadcast(broadcastPlaylist);
		}
	}
	
	public void setNowPlayingText(){
		if(currentPos > -1 && currentPos < VibeVault.playList.size() && currentSong != null){
			currentSongTitle = currentSong.toString();
			currentSongShow = currentSong.getShowTitle();
		}
		else{
			currentSongTitle = "Nothing Playing";
			currentSongShow = "";
		}
	}
	
	public boolean isPaused()
	{
		return paused && playReady;
	}
	
	public boolean isPlaying()
	{
		return playReady && !paused;
	}
	
	public boolean isStopped()
	{
		return !playReady;
	}
	
	public void play()
	{
		if(currentSong != null){
			if(isStopped()){

				playSong(currentSong);
			}
			else if(isPaused()){

				mPlayer.start();
				paused = false;
				sendBroadcast(broadcastPlayStatus);
				setNotification();
			}
		}
	}
	
	public void stop(){
		if(isPlaying() || isPaused()){
			mPlayer.stop();
			playReady = false;
			sendBroadcast(broadcastPlayStatus);
			pNotificationManager.cancel(VibeVault.PLAYER_NOTIFICATION);

		}
	}

	public void pause()
	{
		if(isPlaying())
		{
			mPlayer.pause();
			paused = true;
			sendBroadcast(broadcastPlayStatus);

		}
	}
	
	public void playPrev()
	{
		if(currentPos > 0)
		{

			currentPos--;
			VibeVault.nowPlayingPosition = currentPos;
			playSong(VibeVault.playList.getSong(currentPos));
		}
	}
	
	public void updatePlaying(){
		if(currentSong!=null){
			currentPos = VibeVault.playList.getList().indexOf(currentSong);
		}
	}
	
	public void playNext()
	{
		if(currentPos + 1 < VibeVault.playList.size())
		{

			currentPos++;
			VibeVault.nowPlayingPosition = currentPos;
			playSong(VibeVault.playList.getSong(currentPos));
		}
	}
	
	public String getPlayListTitle()
	{
		return playListTitle;
	}
	
	public ArrayList<ArchiveSongObj> getPlayAList(){
		return VibeVault.playList.getList();
	}
	
	// CAN RETURN NULL CALLER MUST CHECK!!!!!!!!
	public ArchiveSongObj getPlayingSong(){
		if(VibeVault.playList.size()!=0){
			return VibeVault.playList.getSong(currentPos);
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
	
	public String getPlayingSongTitle(){
		return currentSongTitle;
	}
	
	public String getPlayingShowTitle(){
		return currentSongShow;
	}
	
	private void setNotification(){

		notification.setLatestEventInfo(getApplicationContext(), currentSongTitle, currentSongShow, contentIntent);
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		pNotificationManager.notify(VibeVault.PLAYER_NOTIFICATION,notification);
	}
	
	private OnPreparedListener asyncReady = new OnPreparedListener()
	{
		public void onPrepared(MediaPlayer arg0) 
		{
			playReady = true;
			sendBroadcast(broadcastPlayStatus);
			sendBroadcast(broadcastSong);
			if(!pauseFromPhone){
				mPlayer.start();
				paused = false;
				setNotification();

			}
		}
	};
	
	private OnCompletionListener playDone = new OnCompletionListener()
	{
		public void onCompletion(MediaPlayer mp) 
		{

			if(currentPos + 1 < VibeVault.playList.size()){
				playNext();
			}
			else{
				playReady = false;
				sendBroadcast(broadcastPlayStatus);
				pNotificationManager.cancel(VibeVault.PLAYER_NOTIFICATION);
			}
		}
	};
	
	private final PhoneStateListener phoneStateListener = new PhoneStateListener(){
		
		@Override public void onCallStateChanged(int state, String incomingNumber){

			switch(state){
			case TelephonyManager.CALL_STATE_OFFHOOK: 
				pause(); 
				pauseFromPhone = false;
				break;
			case TelephonyManager.CALL_STATE_IDLE:
				if(pauseFromPhone){ 
					play(); }
				break;
			}
		}
	};
	
	private BroadcastReceiver headphoneIntentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)){
				int state = intent.getIntExtra("state",1);
				if(state == 0){
					pause();
				}
			}
		}
	};
}
