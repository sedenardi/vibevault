/*
 * DownloadService.java
 * VERSION 1.1
 * 
 * Copyright 2010 Andrew Pearson and Sanders DeNardi.
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

import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;
import com.code.android.vibevault.R;

public class DownloadService extends Service implements Observer {

	private static Intent broadcaseDownloadStatus = new Intent(VibeVault.BROADCAST_DOWNLOAD_STATUS);
	private final Binder binder = new DServiceBinder();
	private static DownloadSongThread downloadThread = null;
	private static boolean isDownloading = false;
	
	NotificationManager dNotificationManager;
	
	//int icon = R.drawable.statusbardownload;
	int icon = R.drawable.stat_sys_download_anim0;
	CharSequence tickerText = "Downloading";
	
	Notification notification;
	PendingIntent contentIntent;
	
	TelephonyManager tm = null;
	
	@Override
	public void onCreate(){
		super.onCreate();
		dNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notification = new Notification(icon, tickerText, System.currentTimeMillis());
		contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, DownloadTabs.class), Intent.FLAG_ACTIVITY_NEW_TASK);
		tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		int events = PhoneStateListener.LISTEN_DATA_CONNECTION_STATE;
		tm.listen(phoneStateListener, events);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		return START_STICKY;
	}
	
	@Override
	public IBinder onBind(Intent arg0){
		return(binder);
	}
	
	public class DServiceBinder extends Binder{
		DownloadService getService()
		{
			return(DownloadService.this);
		}
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
	}
	
	public void addSong(ArchiveSongObj song){
		if(!VibeVault.downloadSongs.contains(song) && !song.doesExist()){
			VibeVault.downloadSongs.add(song);
			if(!isDownloading){
				downloadSong(song);
				}
			else{
			}
		}
		else{
		}
	}
	
	public void pause(int pos){
		if(downloadThread != null){
			if(downloadThread.equals(VibeVault.downloadSongs.get(pos))){
				downloadThread.pause();
			}
		}
	}
	
	public void resume(int pos){
		if(downloadThread != null && (downloadThread.getStatus() != DownloadSongThread.COMPLETE
				|| downloadThread.getStatus() != DownloadSongThread.DOWNLOADING)){
			if(downloadThread.equals(VibeVault.downloadSongs.get(pos))){
				downloadThread.resume();
			}
			else{
				downloadSong(VibeVault.downloadSongs.get(pos));
			}
		}
		else{
			downloadSong(VibeVault.downloadSongs.get(pos));
		}
	}
	
	public void cancel(int pos){
		if(downloadThread != null){
			if(downloadThread.equals(VibeVault.downloadSongs.get(pos))){
				downloadThread.cancel();
				dNotificationManager.cancel(VibeVault.DOWNLOAD_NOTIFICATION);
				advanceQueue();
			}
		}
	}
	
	public void downloadSong(ArchiveSongObj song){
		if(downloadThread != null){
			downloadThread.deleteObservers();
		}
		downloadThread = new DownloadSongThread(song);
		downloadThread.addObserver(this);
		isDownloading = true;
		VibeVault.nowDownloadingPosition = VibeVault.downloadSongs.indexOf(song);
	}
	
	public void advanceQueue(){
		if(VibeVault.nowDownloadingPosition < VibeVault.downloadSongs.size() - 1){
			VibeVault.nowDownloadingPosition ++;
			downloadSong(VibeVault.downloadSongs.get(VibeVault.nowDownloadingPosition));
		}
		else{
			isDownloading = false;
			downloadThread.deleteObservers();
			downloadThread = null;
			Log.d(VibeVault.DOWNLOAD_SERVICE_TAG, "advanceQueue() - end of queue");
		}
	}
	
	public void clearDownloaded(){
		Iterator<ArchiveSongObj> iter = VibeVault.downloadSongs.iterator();
		while (iter.hasNext()){
			if(iter.next().getDownloadStatus() == DownloadSongThread.COMPLETE){
				iter.remove();
			}
		}
		if(isDownloading){
			VibeVault.nowDownloadingPosition = VibeVault.downloadSongs.indexOf(downloadThread.getSong());
		}
	}
	
	public void setDownloadingPosition(){
		if(downloadThread != null){
			VibeVault.nowDownloadingPosition = VibeVault.downloadSongs.indexOf(downloadThread.getSong());
		}
	}

	public ArchiveSongObj getCurrentDownload() {
		return downloadThread.getSong();
	}
	
	public ArchiveSongObj getdownloadSong(int pos){
		return VibeVault.downloadSongs.get(pos);
	}

	public String toString(){
		return downloadThread.getSong().toString();
	}
	
	public int getDownloadingIndex(){
		if(downloadThread != null){
			return VibeVault.downloadSongs.indexOf(downloadThread.getSong());
		}
		else{
			return -1;
		}
	}
	
	public int getNumDownloads() {
		return VibeVault.downloadSongs.size();
	}
	
	public float getProgress(){
		return downloadThread.getProgress();
	}
	
	public int getProgressInt(){
		return (int) Math.ceil(getProgress());
	}

	private void setNotification(){
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.contentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.download_progress);
		notification.contentIntent = this.contentIntent;
		notification.contentView.setImageViewResource(R.id.Download_Icon, icon);
        notification.contentView.setTextViewText(R.id.Download_Text, downloadThread.getSong().toString() + " - " + getProgressInt() + "%");
        notification.contentView.setProgressBar(R.id.Download_Progress, 100, getProgressInt(), false);
        dNotificationManager.notify(VibeVault.DOWNLOAD_NOTIFICATION,notification);
	}
	
	public void update(Observable o, Object arg) {
		// Fire table row update notification to table.
		// Use heuristics in DownloadSongThread's stateChanged method.
		VibeVault.downloadSongs.get(getDownloadingIndex()).setDownloadStatus(downloadThread.getStatus());
		if(downloadThread.getStatus()!=DownloadSongThread.DOWNLOADING){
			dNotificationManager.cancel(VibeVault.DOWNLOAD_NOTIFICATION);
			if(downloadThread.getStatus()==DownloadSongThread.COMPLETE){
				advanceQueue();
			}
			else if(downloadThread.getStatus()==DownloadSongThread.CANCELLED ||
					downloadThread.getStatus()==DownloadSongThread.ERROR){
				isDownloading = false;
			}
		}
		else{
			setNotification();
		}
		sendBroadcast(broadcaseDownloadStatus);
	}
	
	private final PhoneStateListener phoneStateListener = new PhoneStateListener(){
		
		@Override 
		public void onDataConnectionStateChanged(int state){
			if(isDownloading){
				switch(state){
				case TelephonyManager.DATA_CONNECTED: resume(VibeVault.nowDownloadingPosition); break;
				case TelephonyManager.DATA_DISCONNECTED: pause(VibeVault.nowDownloadingPosition); break;
				}
			}
		}
	};
}
