package com.code.android.vibevault;

import java.io.File;
import java.util.ArrayList;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

public class Downloading{
	
	private static final String LOG_TAG = Downloading.class.getName();
	
	public static final String APP_DIRECTORY = "/archiveApp/";
	
	public static void downloadSong(Context context, ArchiveSongObj song, StaticDataStore db)
	{
		DownloadManager mgr = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
	
		boolean isDownloading = false;
		DownloadManager.Query query = null;
		query = new DownloadManager.Query();
		query.setFilterByStatus(DownloadManager.STATUS_PAUSED|DownloadManager.STATUS_PENDING|
				DownloadManager.STATUS_RUNNING|DownloadManager.STATUS_SUCCESSFUL);
		Cursor cur = mgr.query(query);
		int index = cur.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME);
		for(cur.moveToFirst(); !cur.isAfterLast(); cur.moveToNext())
		{
			isDownloading = (song.getFilePath() == cur.getString(index));
		}
		cur.close();
		if (createShowDirIfNonExistent(song) && !isDownloading && !song.doesExist(db))
		{
			Uri source = Uri.parse(song.getLowBitRate().toString());
			Uri destination = Uri.fromFile(new File(song.getFilePath()));
			
			 
			
			DownloadManager.Request request = new DownloadManager.Request(source);
			request.setTitle(song.getSongTitle());
			request.setDescription(song.getShowTitle());
			request.setDestinationUri(destination);
			request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
			
			long id = mgr.enqueue(request);
			
			 
			db.setSongDownloading(song, id);
		}
	}
	
	public static boolean createShowDirIfNonExistent(ArchiveSongObj song) {
		File appRootDir = new File(Environment.getExternalStorageDirectory(),APP_DIRECTORY);
		File showRootDir = new File(appRootDir, song.getShowIdentifier());
		if (!showRootDir.isDirectory()) {
			if (Environment.getExternalStorageState().equals(
					Environment.MEDIA_MOUNTED)) {
				showRootDir.mkdirs();
				return true;
			} else {
				return false;
			}
		}
		else {
			return true;
		}
	}
	
	public static boolean deleteSong(Context context, ArchiveSongObj song, StaticDataStore db) {
		File songFile = new File(song.getFilePath());
		String path = songFile.getAbsolutePath();
		 
		boolean success = songFile.delete();
		if(success){
			db.setSongDeleted(song);			
			removeSongFromMediaStore(context, path);
		}
		return success;
	}
	
	public static boolean deleteShow(Context context, ArchiveShowObj show, StaticDataStore db) {
		ArrayList<ArchiveSongObj> songs = db.getDownloadedSongsFromShow(show.getIdentifier());
		boolean success = true;
		for (ArchiveSongObj s : songs) {
			success = success && deleteSong(context, s, db);
		}
		return success;
	}
	
	public static void removeSongFromMediaStore(Context context, String path) {		
		context.getContentResolver().delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, 
				MediaStore.MediaColumns.DATA + "='" + path + "'", null);
	}
}
