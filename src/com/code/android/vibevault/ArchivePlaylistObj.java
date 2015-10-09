/*
 * ArchivePlaylistObj.java
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

public class ArchivePlaylistObj {
	
	private ArrayList<ArchiveSongObj> playList;
	private String title = "";
	private long key = -1;
	
	public ArchivePlaylistObj(){
		playList = new ArrayList<ArchiveSongObj>();
	}
	
	public ArchivePlaylistObj(ArrayList<ArchiveSongObj> list){
		playList = list;
	}
	
	public ArchivePlaylistObj(String plTitle, long id, ArrayList<ArchiveSongObj> list){
		title = plTitle;
		key = id;
		playList = list;
	}
	
	public void setPlayList(ArrayList<ArchiveSongObj> list){
		playList.clear();
		playList.addAll(list);
	}
	
	public String getTitle(){
		return title;
	}
	
	public void setTitle(String newTitle){
		title = newTitle;
	}
	
	public int size(){
		return playList.size();
	}
	
	public ArrayList<ArchiveSongObj> getList(){
		return playList;
	}
	
	public ArchiveSongObj getSong(int pos){
		return playList.get(pos);
	}
	
	public void queueFront(ArchiveSongObj song){
		playList.add(0, song);
	}
	
	public int enqueue(ArchiveSongObj song){
		if(!playList.contains(song)){
			playList.add(song);
		}
		return playList.indexOf(song);
	}
	
	public boolean isEmpty(){
		return playList.isEmpty();
	}
	
	public void removeSongAt(int location){
		playList.remove(location);
	}
	
	//returns index of song if it exists, -1 if not
	public int exists(ArchiveSongObj song){
		if(!playList.isEmpty()){
			for(int i = 0; i < playList.size(); i++){
				if(playList.get(i).equals(song)){
					return i;
				}
			}
			return -1;
		}
		else{
			return -1;
		}
	}
	
	public boolean equalsList(ArrayList<ArchiveSongObj> list){
		return playList.equals(list);
	}
	
	public String toString(){
		if(!playList.isEmpty()){
			return playList.toString();
		}
		else{
			return "Playlist is empty.";
		}
	}

}
