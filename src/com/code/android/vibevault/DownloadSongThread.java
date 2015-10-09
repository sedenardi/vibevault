/*
 * DownloadSongThread.java
 * VERSION 1.3
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

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Observable;

import android.os.Environment;
import android.util.Log;

public class DownloadSongThread extends Observable implements Runnable {

	private ArchiveSongObj song;

	// Max size of download buffer.
	private static final int MAX_BUFFER_SIZE = 1024;

	// These are the status names.
	public static final String STATUSES[] = { "Downloading", "Paused",
			"Complete", "Cancelled", "Error" };

	// These are the status codes.
	public static final int DOWNLOADING = 0;
	public static final int PAUSED = 1;
	public static final int COMPLETE = 2;
	public static final int CANCELLED = 3;
	public static final int ERROR = 4;

	private URL url; // download URL
	private int size; // size of download in bytes
	private int downloaded; // number of bytes downloaded
	private int status; // current status of download
	private int percent;

	// Constructor for Download.
	public DownloadSongThread(ArchiveSongObj song) {
		Log.d(VibeVault.DOWNLOAD_THREAD_TAG,
				"Creating thread " + song.toString());
		this.song = song;
		url = song.getLowBitRate();
		size = -1;
		downloaded = 0;
		percent = 0;
		status = DOWNLOADING;

		// Begin the download.
		download();
	}

	// Get this download's URL.
	public String getUrl() {
		return url.toString();
	}

	public ArchiveSongObj getSong() {
		return song;
	}

	// Get this download's size.
	public int getSize() {
		return size;
	}

	// Get this download's progress.
	public float getProgress() {
		return ((float) downloaded / size) * 100;
	}

	// Get this download's status.
	public int getStatus() {
		return status;
	}

	// Pause this download.
	public void pause() {
		
		status = PAUSED;
		stateChanged();
	}

	// Resume this download.
	public void resume() {
		
		status = DOWNLOADING;
		stateChanged();
		download();
	}

	// Cancel this download.
	public void cancel() {
		
		status = CANCELLED;
		stateChanged();
	}

	// Mark this download as having an error.
	private void error() {
		
		status = ERROR;
		stateChanged();
	}

	// Start or resume downloading.
	private void download() {
		Thread thread = new Thread(this);
		thread.start();
	}

	// Download file.
	public void run() {
		Log.d(VibeVault.DOWNLOAD_THREAD_TAG,
				"Running thread " + song.toString());
		RandomAccessFile file = null;
		boolean showRootExists = createShowDirIfNonExistent();
		InputStream stream = null;

		if (showRootExists) {
			try {
				// Open connection to URL.
				HttpURLConnection connection = (HttpURLConnection) url
						.openConnection();

				// Specify what portion of file to download.
				connection.setRequestProperty("Range", "bytes=" + downloaded
						+ "-");

				// Connect to server.
				connection.connect();

				// Make sure response code is in the 200 range.
				if (connection.getResponseCode() / 100 != 2) {
					error();
				}

				// Check for valid content length.
				int contentLength = connection.getContentLength();
				Log.d(VibeVault.DOWNLOAD_THREAD_TAG,
						"Song total size = " + (contentLength + downloaded));
				if (contentLength < 1) {
					error();
				}
				
				/*
				 * Set the size for this download if it hasn't been already set.
				 */
				if (size == -1) {
					size = contentLength;
					stateChanged();
				}

				if(new File(song.getFilePath()).exists()){
					downloaded = (int) new File(song.getFilePath()).length();
					Log.d(VibeVault.DOWNLOAD_THREAD_TAG,
							"File exists, setting download start to " + downloaded);
					if(downloaded >= size){
						status = COMPLETE;
					}
					else{
						connection.disconnect();
						connection = (HttpURLConnection) url.openConnection();
						connection.setRequestProperty("Range", "bytes=" + downloaded
								+ "-");
						connection.connect();
						if (connection.getResponseCode() / 100 != 2) {
							error();
						}
						contentLength = connection.getContentLength();
						if (contentLength < 1) {
							error();
						}
					}
				}

				percent = (int) Math.ceil(getProgress());
				Log.d(VibeVault.DOWNLOAD_THREAD_TAG,
						"Setting percent to = " + percent);

				// Open file and seek to the end of it.
				file = new RandomAccessFile(new File(song.getFilePath()), "rw");
				if(size <= file.length()){
					status = COMPLETE;
				}
				file.seek(downloaded);

				stream = connection.getInputStream();
				while (status == DOWNLOADING) {
					/*
					 * Size buffer according to how much of the file is left to
					 * download.
					 */
					byte buffer[];
					if (size - downloaded > MAX_BUFFER_SIZE) {
						buffer = new byte[MAX_BUFFER_SIZE];
					} else {
						buffer = new byte[size - downloaded];
					}

					// Read from server into buffer.
					int read = stream.read(buffer);
					if (read == -1)
						break;

					// Write buffer to file.
					file.write(buffer, 0, read);
					downloaded += read;
					if(percent != (int) Math.ceil(getProgress())){
						percent = (int) Math.ceil(getProgress());
						stateChanged();
					}
				}

				/*
				 * Change status to complete if this point was reached because
				 * downloading has finished.
				 */
				if (status == DOWNLOADING) {
					status = COMPLETE;
				}
				if(status == COMPLETE && song.getDownloadShow() != null){
					VibeVault.db.insertShow(song.getDownloadShow());
					VibeVault.db.insertSong(song);
					VibeVault.db.setSongDownloaded(song);
				}
			} catch (Exception e) {
				Log.d(VibeVault.DOWNLOAD_THREAD_TAG, "Error downloading "
						+ song.toString());
				e.printStackTrace();
				error();
			} finally {
				// Close file.
				if (file != null) {
					try {
						file.close();
						if(status == CANCELLED || status == ERROR){
							File delFile = new File(song.getFilePath());
							delFile.delete();
						}
					} catch (Exception e) {
						
					}
				}

				// Close connection to server.
				if (stream != null) {
					try {
						stream.close();
					} catch (Exception e) {
					}
				}
				Log.d(VibeVault.DOWNLOAD_THREAD_TAG, "Download finished "
						+ song.toString());
				stateChanged();
			}
		}
	}

	private boolean createShowDirIfNonExistent() {
		File appRootDir = new File(Environment.getExternalStorageDirectory(),
				VibeVault.APP_DIRECTORY);
		File showRootDir = new File(appRootDir, song.getShowIdentifier());
		if (!showRootDir.isFile() || !showRootDir.isDirectory()) {
			if (Environment.getExternalStorageState().equals(
					Environment.MEDIA_MOUNTED)) {
				showRootDir.mkdirs();
				return true;
			} else {
				return false;
			}
		}
		return true;
	}

	public boolean equals(ArchiveSongObj song) {
		return this.song.equals(song);
	}

	// Notify observers that this download's progress/status has changed.
	private void stateChanged() {
		/*if (status == DOWNLOADING) {
			if(getProgress() > percent){
				setChanged();
				notifyObservers();
				Log.d(VibeVault.DOWNLOAD_THREAD_TAG, "Notifying song: "
						+ song.toString() + " Status: "
						+ status + " Percent: " 
						+ getProgress());
				percent++;
			}
		}
		else{
			setChanged();
			notifyObservers();
			
		}*/
		
		setChanged();
		notifyObservers();
	}
}
