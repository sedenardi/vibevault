/*
 * ArchiveArtistObj.java
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

public class ArchiveArtistObj extends ArchiveVoteObj implements Serializable {

	private static final long serialVersionUID = 1L;
	private int artistId;
	private String artistName;
	private double rating;
	private String voteTime;
	
	public ArchiveArtistObj(int id, String name, double rat, int vote, String lastVote) {
		artistId = id;
		artistName = name;
		rating = rat;
		votes = vote;
		voteTime = lastVote;
	}
	
	public String getVoteTime(){
		return voteTime;
	}
	
	public int getArtistId() {
		return artistId;
	}
	
	public String getArtistName() {
		return artistName;
	}
	
	public double getRating(){
		return rating;
	}
	
	public String toString(){
		return artistName;
	}

}