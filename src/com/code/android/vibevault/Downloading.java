package com.code.android.vibevault;

import java.io.File;
import java.util.ArrayList;

import android.app.DownloadManager;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.provider.MediaStore;

public class Downloading{
	
	private static final String LOG_TAG = Downloading.class.getName();
	
	//public static final String APP_DIRECTORY = "/archiveApp/";
	
	public static String getAppDirectory(StaticDataStore db){
		return db.getPref("downloadPath");
	}
	
	public static final boolean createArchiveDir(Context context, StaticDataStore db)
	{
		String dir = getAppDirectory(db);
		File appRootDir = new File(Environment.getExternalStorageDirectory(),dir);
		if (!appRootDir.isFile() || !appRootDir.isDirectory()) {
			if (Environment.getExternalStorageState().equals(
					Environment.MEDIA_MOUNTED)) {
				appRootDir.mkdirs();
			} else {
				return false;
			}
		}
		return true;
	}
	
	public static void downloadSong(Context context, ArchiveSongObj song, StaticDataStore db)
	{
		DownloadManager mgr = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
		if (!createArchiveDir(context, db)) {
			return;
		}
		boolean isDownloading = db.getSongIsDownloading(song);
//		DownloadManager.Query query = null;
//		query = new DownloadManager.Query();
//		query.setFilterByStatus(DownloadManager.STATUS_PAUSED|DownloadManager.STATUS_PENDING|
//				DownloadManager.STATUS_RUNNING|DownloadManager.STATUS_SUCCESSFUL);
//		Cursor cur = mgr.query(query);
//		int index = cur.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME);
//		Logging.Log(LOG_TAG, "Checking if song is currently downloading: " + song.getFilePath(db));
//		for(cur.moveToFirst(); !cur.isAfterLast(); cur.moveToNext())
//		{
//			Logging.Log(LOG_TAG, "In download system: " + cur.getString(index));
//			if (song.getFilePath(db) == cur.getString(index))
//				isDownloading = true;
//		}
//		cur.close();
		if (createShowDirIfNonExistent(song, db) && !isDownloading && !song.doesExist(db))
		{
			Uri source = Uri.parse(song.getLowBitRate().toString());
			Uri destination = Uri.fromFile(new File(song.getFilePath(db)));
			
			Logging.Log(LOG_TAG, "Downloading file " + source.toString());
			
			DownloadManager.Request request = new DownloadManager.Request(source);
			request.setTitle(song.getSongTitle());
			request.setDescription(song.getShowTitle());
			request.setDestinationUri(destination);
			request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
			
			long id = mgr.enqueue(request);
			
			Logging.Log(LOG_TAG,"Download ID for " + song.getSongTitle() + ": " + id);
			db.setSongDownloading(song, id);
		}
	}
	
	public static boolean createShowDirIfNonExistent(ArchiveSongObj song, StaticDataStore db) {
		String dir = getAppDirectory(db);
		File appRootDir = new File(Environment.getExternalStorageDirectory(),dir);
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
		File songFile = new File(song.getFilePath(db));
		String path = songFile.getAbsolutePath();
		Logging.Log(LOG_TAG, "Deleting: " + path);
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
	
	public static String syncFilesDirectory(Context context, StaticDataStore db) {
		if (!createArchiveDir(context, db)) {
			return "Error creating directory on SDCard";
		}
		String dir = getAppDirectory(db);
		int showsAdded = 0;
		int songsAdded = 0;
		File appRootDir = new File(Environment.getExternalStorageDirectory(),dir);
		File[] dirs = appRootDir.listFiles();
		if (dirs != null) {
			if (dirs.length > 0) {
				for (File d : dirs) {
					if (d.isDirectory()) {
						String showIdent = d.getName();
						Logging.Log(LOG_TAG,"Found " + showIdent);
						ArchiveShowObj show = new ArchiveShowObj(ArchiveShowObj.ArchiveShowPrefix + showIdent,false);
						if (!db.getShowExists(show)) {
							Logging.Log(LOG_TAG,showIdent + " doesn't exist in DB");
							ArrayList<ArchiveSongObj> songs = new ArrayList<ArchiveSongObj>();
							Searching.getSongs(show, songs, db);
							showsAdded++;
							File[] files = d.listFiles();
							if (files != null) {
								if (files.length > 0) {
									for (File f : files) {
										if (f.isFile()) {
											String fileName = f.getName();
											Logging.Log(LOG_TAG, "Found " + fileName + ", adding to DB");
											db.setSongDownloaded(fileName);
											songsAdded++;
										}
									}
								}
							}
						} else if (db.getShowDownloadStatus(show) != StaticDataStore.SHOW_STATUS_FULLY_DOWNLOADED) {
							ArrayList<ArchiveSongObj> songs = db.getSongsFromShow(show.getIdentifier());
							int localSongs = 0;
							Logging.Log(LOG_TAG,showIdent + " not fully downloaded");
							File[] files = d.listFiles();
							if (files != null) {
								if (files.length > 0) {
									for (File f : files) {
										if (f.isFile()) {
											String fileName = f.getName();
											Logging.Log(LOG_TAG, "Found " + fileName);
											if (!db.songIsDownloaded(fileName)) {
												Logging.Log(LOG_TAG, fileName + " adding to DB");
												db.setSongDownloaded(fileName);
												songsAdded++;
												localSongs++;
											}
										}
									}
								}
							}
							if (songs.size() == localSongs) {
								showsAdded++;
							}
						}
					}
				}
			}
		}
		int showsRemoved = 0;
		int songsRemoved = 0;
		ArrayList<ArchiveShowObj> shows = db.getDownloadShows();
		if (shows.size() > 0) {
			for (ArchiveShowObj show : shows) {
				File showDir = new File(appRootDir,show.getIdentifier());
				ArrayList<ArchiveSongObj> songs = db.getSongsFromShow(show.getIdentifier());
				if (!showDir.isDirectory()) {
					if (songs.size() > 0) {
						for (ArchiveSongObj song : songs) {
							if (db.songIsDownloaded(song.getFileName())) {
								db.setSongDeleted(song);
								songsRemoved++;
							}
						}
					}
					db.setShowDeleted(show);
					showsRemoved++;
				} else {
					boolean showDeleted = true;
					if (songs.size() > 0) {
						for (ArchiveSongObj song : songs) {
							if (db.songIsDownloaded(song.getFileName())) {								
								File showFile = new File(showDir,song.getFileName());
								if (!showFile.isFile()) {
									db.setSongDeleted(song);
									songsRemoved++;
								} else {
									showDeleted = false;
								}
							}
						}
					}
					if (showDeleted) {
						db.setShowDeleted(show);
						showsRemoved++;
					}
				}
			}
		}
		return showsAdded + " shows and " + songsAdded + " songs added, " + showsRemoved + " shows and " + songsRemoved + " songs removed.";
	}
	
	//in bytes
	public static long getDownloadFolderSize(File dir) {
		long size = 0;
		for (File f : dir.listFiles()) {
			if (f.isFile()) {
				size += f.length();
			} else {
				size += getDownloadFolderSize(f);
			}				
		}
		return size;
	}
	
	//in bytes
	public static double getFreeSpaceAvailable() {
		StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
		return (double)stat.getAvailableBlocks() * (double)stat.getBlockSize();
	}
	
	public static boolean changeDownloadFolder(Context context, String oldPath, String newPath) {
		File oldRootDir = new File(Environment.getExternalStorageDirectory(), oldPath);
		File newRootDir = new File(Environment.getExternalStorageDirectory(), newPath);
		boolean copyResult = false;
		copyResult = manualDirectoryMove(oldRootDir, newRootDir);
		if (copyResult) {
			changeDirectorySongsToNewPath(context, newRootDir, oldPath, newPath);
		}
		return copyResult;
	}
	
	public static boolean manualDirectoryMove(File oldRootDir, File newRootDir) {
		boolean result = true;
		if (!newRootDir.exists()) {
			Logging.Log(LOG_TAG, "Making directory: " + newRootDir.getAbsolutePath());
			result = result && newRootDir.mkdirs();
		}
		if (result) {
			for (File f : oldRootDir.listFiles()) {
				if (f.isDirectory()) {
					File newDir = new File(newRootDir, f.getName());
					result = result && manualDirectoryMove(f, newDir);
				} else {
					File newFile = new File(newRootDir, f.getName());
					Logging.Log(LOG_TAG, "Renaming file " + f.getAbsolutePath() + " to " + newFile.getAbsolutePath());
					if (newFile.exists()) {
						result = result && newFile.delete();
					}
					result = result && f.renameTo(newFile);
				}
			}
		}
		return result;
	}
	
	public static void changeDirectorySongsToNewPath(Context context, File newRootDir, String oldPath, String newPath) {
		for (File f : newRootDir.listFiles()) {
			if (f.isDirectory()) {
				changeDirectorySongsToNewPath(context, f, oldPath, newPath);
			} else {
				String oldFilePath = f.getAbsolutePath().replace(newPath, oldPath);
				changeSongPathInMediaStore(context, oldFilePath, f.getAbsolutePath());
			}
		}
	}
	
	public static void changeSongPathInMediaStore(Context context, String oldPath, String newPath) {
		ContentValues values = new ContentValues();
		values.put(MediaStore.MediaColumns.DATA, newPath);
		int rows = context.getContentResolver().update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values, 
				MediaStore.MediaColumns.DATA + "='" + oldPath + "'", null);
		if (rows == 1) {
			Logging.Log(LOG_TAG, "Changed the path for successful: " + oldPath);
		} else {
			Logging.Log(LOG_TAG, "Changed the path for failed: " + oldPath);
		}
	}
	
	public static boolean deleteFileOrDirectory(File path) {
		boolean success = true;
		if (path.isDirectory()) {
			for (File f : path.listFiles()) {
				success = success && deleteFileOrDirectory(f);
			}
		}
		Logging.Log(LOG_TAG, "Deleting file " + path.getAbsolutePath());
		return success && path.delete();
	}
	
	public static ArrayList<File> getFileSequenceToDelete(File pathToDelete) {
		ArrayList<File> sequence = new ArrayList<File>();
		if (pathToDelete.isDirectory()) {
			for (File f : pathToDelete.listFiles()) {
				sequence.addAll(getFileSequenceToDelete(f));
			}
		}
		sequence.add(pathToDelete);
		return sequence;
	}
}
