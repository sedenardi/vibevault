/*
 * ArchiveShowObj.java
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

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

public class ArchiveShowObj extends ArchiveVoteObj implements Serializable {
	
	private static final String LOG_TAG = ArchiveShowObj.class.getName();

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String ArchiveShowPrefix = "http://www.archive.org/details/";
	
	private String wholeTitle = "";
	private URL showURL = null;
	private String identifier = "";
	private String date = "";
	private double rating = 0.0;
	private String source = "";
	private String showArtist = "";
	private String showTitle = "";
	private boolean vbrShow = false;
	private boolean lbrShow = false;
	private boolean hasSelectedSong = false;
	private String selectedSong = "";
	
	/** Create an object which represents a show returned from an archive.org search.
	 * 
	 * @param tit Show's title.
	 * @param id Show's "identifier" (unique part of its URL).
	 * @param dat Show's date.
	 * @param rat Show's rating.
	 * @param format Show's format list.
	 * @parm src Show's source.
	 */
	public ArchiveShowObj(String tit, String id, String dat, double rat, String format, String src) {
		wholeTitle = tit;
		String artistAndShowTitle[] = tit.split(" Live at ");
		if(artistAndShowTitle.length < 2){
			artistAndShowTitle = tit.split(" Live @ ");
		}
		if(artistAndShowTitle.length < 2){
			artistAndShowTitle = tit.split(" Live ");
		}
		showArtist = artistAndShowTitle[0].replaceAll(" - ", "").replaceAll("-","");
		if(artistAndShowTitle.length >= 2){
			showTitle = artistAndShowTitle[1];
		}
		identifier = id;
		date = dat;
		rating = rat;
		source = src;
		this.parseFormatList(format);
		try{
			showURL = new URL(ArchiveShowPrefix + identifier);
		} catch(MalformedURLException e){
			// url is null in this case!
		}
	}
	
	public String getShowSource(){
		return source;
	}
	
	public String getShowArtist(){
		return showArtist;
	}
	
	public String getShowTitle(){
		return showTitle;
	}
	
	// Constructor called from DB version > 5
	public ArchiveShowObj(String ident, String title, String artist, String src, String hasVBR, String hasLBR, int dbid){
		wholeTitle = artist + " Live at " + title;
		identifier = ident;
		showTitle = title;
		showArtist = artist;
		source = src;
		vbrShow = Boolean.valueOf(hasVBR);
		lbrShow = Boolean.valueOf(hasLBR);
		DBID = dbid;
		try{
			showURL = new URL(ArchiveShowPrefix + identifier);
		} catch(MalformedURLException e){
			// url is null in this case!
		}
	}
	
	// Constructor called from DB version <= 5
	public ArchiveShowObj(String id, String tit, String hasVBR, String hasLBR){
		wholeTitle = tit;
		String artistAndShowTitle[] = tit.split(" Live at ");
		if(artistAndShowTitle.length < 2){
			artistAndShowTitle = tit.split(" Live @ ");
		}
		if(artistAndShowTitle.length < 2){
			artistAndShowTitle = tit.split(" Live ");
		}
		showArtist = artistAndShowTitle[0].replaceAll(" - ", "").replaceAll("-","");
		if(artistAndShowTitle.length >= 2){
			showTitle = artistAndShowTitle[1];
		}
		identifier = id;
		if(hasVBR.equals("1")){
			vbrShow = true;
		}
		if(hasLBR.equals("1")){
			lbrShow = true;
		}
		try{
			showURL = new URL(ArchiveShowPrefix + identifier);
		} catch(MalformedURLException e){
			// url is null in this case!
		}
	}
	
	public ArchiveShowObj(String linkString, boolean hasSelected) {
		wholeTitle = "";
		// This should take care of any prefix (eg. http://, http://www., www.).
		identifier = linkString.split("archive.org/details/")[1];
		showTitle = "";
		showArtist = "";
		source = "";
		vbrShow = true;
		lbrShow = true;
		hasSelectedSong = hasSelected;
		try{
			showURL = new URL(linkString);
		} catch(MalformedURLException e){
			// url is null in this case!
		}
	}
	
	// Constructor called from vote return
	public ArchiveShowObj(String ident, String title, String artist, String date, String src, double rat, int numVotes){
		wholeTitle = title;
		identifier = ident;
		showTitle = title;
		showArtist = artist;
		String artistAndShowTitle[] = title.split(" Live at ");
		if(artistAndShowTitle.length < 2){
			artistAndShowTitle = title.split(" Live @ ");
		}
		if(artistAndShowTitle.length < 2){
			artistAndShowTitle = title.split(" Live ");
		}
		if(artistAndShowTitle.length >= 2){
			showTitle = artistAndShowTitle[1];
		}
		source = src;
		vbrShow = true;
		lbrShow = false;
		rating = rat;
		votes = numVotes;
		try{
			showURL = new URL(ArchiveShowPrefix + identifier);
		} catch(MalformedURLException e){
			// url is null in this case!
		}
	}
	
	public void setFullTitle(String s){
		Logging.Log(LOG_TAG, s);
		wholeTitle = s;
		String artistAndShowTitle[] = s.split(" Live at ");
		if(artistAndShowTitle.length < 2){
			artistAndShowTitle = s.split(" Live @ ");
		}
		if(artistAndShowTitle.length < 2){
			artistAndShowTitle = s.split(" Live ");
		}
		showArtist = artistAndShowTitle[0];
		Logging.Log(LOG_TAG, "SHOW ARTIST: " + showArtist);
		if(artistAndShowTitle.length >= 2){
			showTitle = artistAndShowTitle[1];
		}
	}

	private void parseFormatList(String formatList){
		if(formatList.contains("64Kbps MP3")){
			lbrShow = true;
		}
		if(formatList.contains("64Kbps M3U")){
			lbrShow = true;
		}
		if(formatList.contains("VBR MP3")){
			vbrShow = true;
		}
		if(formatList.contains("VBR M3U")){
			vbrShow = true;
		}
	}
	
	public boolean hasVBR(){
		return vbrShow;
	}
	
	public boolean hasLBR(){
		return lbrShow;
	}
	
	//should only be called from the voted shows activity
	public double getRating(){
		return rating;
	}
	
	public String getSource(){
		return source;
	}
	
	public String getDate(){
		return date;
	}

	@Override
	public String toString() {
		return String.format(wholeTitle);
	}

	public String getArtistAndTitle(){
		return wholeTitle;
	}
	
	public String getIdentifier(){
		return identifier;
	}
	
	public String getLinkPrefix(){
		return "http://www.archive.org/download/" + identifier + "/" + identifier;
	}
	
	// Used when the ArchiveShowObj is created by an Intent from
	// the user clicking on a song link.
	public boolean hasSelectedSong(){
		return hasSelectedSong;
	}
	
	// Used when the ArchiveShowObj is created by an Intent from
	// the user clicking on a song link.
	public void setSelectedSong(String s){
		selectedSong = s;
	}
	
	// Used when the ArchiveShowObj is created by an Intent from
	// the user clicking on a song link.
	public String getSelectedSong(){
		return selectedSong;
	}
	
	// CALLER MUST CHECK FOR NULL RETURN VALUE!
	// \/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/
	public URL getShowURL() {
		return showURL;
	}
	
	@Override
	public boolean equals(Object obj){
		ArchiveShowObj show = (ArchiveShowObj) obj;
		if(this.identifier == show.getIdentifier()){
			return true;
		}
		else{
			return false;
		}
	}

}