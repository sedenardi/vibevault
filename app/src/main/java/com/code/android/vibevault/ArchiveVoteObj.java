package com.code.android.vibevault;

import java.io.Serializable;

public abstract class ArchiveVoteObj implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected int votes;
	protected int DBID;
	
	public int getVotes() {
		return votes;
	}
	
	public int getID() {
		return DBID;
	}
	
	public void setID(int newID) {
		DBID = newID;
	}
}
