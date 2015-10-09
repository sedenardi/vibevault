/*
 * ArchiveShowObj.java
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

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

public class ArchiveShowObj implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String title = "";
	private URL showURL = null;
	private String identifier = "";
	private String date = "";
	private double rating = 0.0;
	private String source = "";
	private boolean vbrShow = false;
	private boolean lbrShow = false;
	
	
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
		title = tit;
		identifier = id;
		date = dat;
		rating = rat;
		source = src;
		this.parseFormatList(format);
		try{
			showURL = new URL("http://www.archive.org/details/" + identifier);
		} catch(MalformedURLException e){
			// url is null in this case!
		}
	}
	
	// Constructor called from the DB
	public ArchiveShowObj(String id, String tit, String hasVBR, String hasLBR){
		title = tit;
		identifier = id;
		if(hasVBR.equals("1")){
			vbrShow = true;
		}
		if(hasLBR.equals("1")){
			lbrShow = true;
		}
		try{
			showURL = new URL("http://www.archive.org/details/" + identifier);
		} catch(MalformedURLException e){
			// url is null in this case!
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
		//System.out.println(formatList);
	}
	
	public boolean hasVBR(){
		return vbrShow;
	}
	
	public boolean hasLBR(){
		return lbrShow;
	}
	
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
		return String.format(title);
	}

	public String getTitle(){
		return title;
	}
	
	public String getIdentifier(){
		return identifier;
	}
	
	public String getLinkPrefix(){
		return "http://www.archive.org/download/" + identifier + "/" + identifier;
	}
	
	// CALLER MUST CHECK FOR NULL RETURN VALUE!
	// \/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/
	public URL getShowURL() {
		return showURL;
	}

}