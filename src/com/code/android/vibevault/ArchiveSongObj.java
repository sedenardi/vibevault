/*
 * ArchiveSongObj.java
 * VERSION 3.X
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
import java.net.MalformedURLException;
import java.net.URL;

import android.os.Environment;

public class ArchiveSongObj {
	
	// I'm pretty sure that URL objects are bigger
	// than String objects, so we don't store the
	// URL's as actual URL's unless the user indicates
	// that we are actually going to download or stream.
	private String urlString;
	private String title;
	private String showTitle;
	private String showIdent;
	private String showArtist;
	private String fileName;
	private int status;
	private boolean exists = false;
	private ArchiveShowObj downloadShow;
	
	/**
	 * Create a song object.
	 * 
	 * The constructor will use the song name and show title to search in the
	 * proper sdcard location to see if the file is there. If the file has the
	 * INCOMPLETE_DL_STRING appended to it, it knows that the song is in
	 * progress. If the file exists without that appended to it, it has been
	 * completed. Otherwise, the song has not been downloaded (or started to be
	 * downloaded).
	 * 
	 * @param tit The title of the song.
	 * @param urlStrings An ArrayList of Strings which are the URL's for the different formats of the song.
	 * @param showTit The title of the show which the song is a part of.
	 */
	public ArchiveSongObj(String tit, String urlStr, String showTit, String showIdent){
		String artistAndShowTitle[] = showTit.split(" Live at ");
		if(artistAndShowTitle.length < 2){
			artistAndShowTitle = tit.split(" Live @ ");
		}
		if(artistAndShowTitle.length < 2){
			artistAndShowTitle = tit.split(" Live ");
		}
		showArtist = artistAndShowTitle[0].replaceAll(" - ", "").replaceAll("-","");
		
		urlString = urlStr;
		status = -1;
		title = tit.replace("&apos;", "'").replace("&gt;", ">").replace("&lt;", "<").replace("&quot;", "\"").replace("&amp;","&");
		showTitle = showTit;
		
		this.showIdent = showIdent;
		String splitArray[] = urlStr.split("/");
		fileName = splitArray[splitArray.length-1];
		checkExists();
		/*File showRootDir = new File(Environment.getExternalStorageDirectory() + ArchiveApp.APP_DIRECTORY + showTitle);
		File postCmplt = new File(showRootDir, title.replace(">", "_") + ".mp3");
		if(postCmplt.exists()){
			downloaded=true;
			return;
		} else{
			File posIncmplt = new File(showRootDir, ArchiveApp.INCOMPLETE_DL_STRING + sanitizeForFilename(title) + ".mp3");
			if(posIncmplt.exists()){
				started=true;
				return;
			}
		}*/
	}
	
	// Constructor from DB, doesn't call db
	public ArchiveSongObj(String tit, String fileStr, String showTit, String showIdent, boolean isDownloaded){
		urlString = "http://www.archive.org/download/" + showIdent + "/" + fileStr;
		status = -1;
		title = tit.replace("&apos;", "'").replace("&gt;", ">").replace("&lt;", "<").replace("&quot;", "\"").replace("&amp;","&");
		showTitle = showTit;
		String artistAndShowTitle[] = showTit.split(" Live at ");
		if(artistAndShowTitle.length < 2){
			artistAndShowTitle = tit.split(" Live @ ");
		}
		if(artistAndShowTitle.length < 2){
			artistAndShowTitle = tit.split(" Live ");
		}
		showArtist = artistAndShowTitle[0].replaceAll(" - ", "").replaceAll("-","");
		this.showIdent = showIdent;
		String splitArray[] = fileStr.split("/");
		fileName = fileStr;
		exists = isDownloaded;
		//checkExists();
		/*File showRootDir = new File(Environment.getExternalStorageDirectory() + ArchiveApp.APP_DIRECTORY + showTitle);
		File postCmplt = new File(showRootDir, title.replace(">", "_") + ".mp3");
		if(postCmplt.exists()){
			downloaded=true;
			return;
		} else{
			File posIncmplt = new File(showRootDir, ArchiveApp.INCOMPLETE_DL_STRING + sanitizeForFilename(title) + ".mp3");
			if(posIncmplt.exists()){
				started=true;
				return;
			}
		}*/
	}
	
	public static String sanitizeForFilename(String s){
		return s.replaceAll("[^a-zA-Z0-9]", "");
	}
	
	/** If song is downloaded, return File object representing local .mp3 file.
	 * 
	 * @return File object is local .mp3 for the song.
	 */
	public File getSongFile(){
		if(status == DownloadSongThread.COMPLETE){
			File showRootDir = new File(Environment.getExternalStorageDirectory() + VibeVault.APP_DIRECTORY + showIdent);
			File postCmplt = new File(showRootDir, title + ".mp3");
			return postCmplt;
		} else{
			return null;
		}
	}
	
	public String getFileName(){
		return fileName;
	}
	
	public boolean doesExist(){
		checkExists();
		return exists;
	}
	
	private void checkExists(){
		/*File checkCmplt = new File(getFilePath());
		if(checkCmplt.exists()){
			exists = true;
		}
		else{
			exists = false;
		}*/
		if(VibeVault.db.songIsDownloaded(fileName) && new File(getFilePath()).exists()){
			exists = true;
		}
		else{
			exists = false;
		}
	}
	
	public String getSongPath(){
		checkExists();
		if(exists){
			return getFilePath();
		}
		else{
			return getLowBitRate().toString();
		}
	}
	
	public String getFilePath(){
		return Environment.getExternalStorageDirectory() + 
			VibeVault.APP_DIRECTORY + showIdent + '/' +
			fileName;
	}
	
	public void setDownloadShow(ArchiveShowObj show){
		downloadShow = show;
	}
	
	public ArchiveShowObj getDownloadShow(){
		return downloadShow;
	}

	public void setDownloadStatus(int status){
		this.status = status;
		if(status == DownloadSongThread.COMPLETE){
			checkExists();
		}
	}
	
	public int getDownloadStatus(){
		return status;
	}
	
	public String getShowIdentifier(){
		return showIdent;
	}
	
	public String getShowTitle(){
		return showTitle;
	}
	
	public String getShowArtist(){
		return showArtist;
	}

	/** Returns a URL object of the lowest bitrate for a song.
	 * 
	 * Returns 64kbps if it exists, VBR if 64kbs doesn't exist, and any .mp3 if VBR doesn't exist.
	 * Otherwise, it returns null, which the caller should check for.
	 */
	public URL getLowBitRate(){
		if(urlString!=null){
			try {
				return new URL(urlString);
			} catch (MalformedURLException e) {
				return null;
				// TODO Auto-generated catch block
				
			}
		} else{
			return null;
		}
	}
	
	@Override
	public String toString(){
		return title;
	}
	
	@Override
	public boolean equals(Object obj){
		if(obj instanceof ArchiveSongObj){
			ArchiveSongObj song = (ArchiveSongObj) obj;
			if(fileName.equals(song.fileName)){
				return true;
			}
			else{
				return false;
			}
		}
		else{
			return false;
		}
	}
	
}