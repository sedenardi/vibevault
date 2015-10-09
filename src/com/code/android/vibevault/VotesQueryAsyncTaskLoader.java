package com.code.android.vibevault;

import java.util.ArrayList;

import android.content.AsyncTaskLoader;
import android.content.Context;

public class VotesQueryAsyncTaskLoader<T> extends AsyncTaskLoader<ArrayList<?>> {
	
	private int voteQueryType = -1;
	private int voteQueryResultType = -1;
	private int queryResults = 10;
	private int queryOffset = 0;
	private int artistId = -1;
	private ArrayList<?> votes = null;
	
	private StaticDataStore db;
	
	public VotesQueryAsyncTaskLoader(Context context, int voteType, int resultType, int numResults, int resultOffset, int artistId) {
		super(context);
		db = StaticDataStore.getInstance(context);
		this.voteQueryType = voteType;
		this.voteQueryResultType = resultType;
		this.queryResults = numResults;
		this.queryOffset = resultOffset;
		this.artistId = artistId;
	}
	
	@Override
	public void onStartLoading(){
		if(!takeContentChanged() && votes!=null) {
			 
		    deliverResult(votes);
		  } else {
		    forceLoad();
		}
	}

	@Override
	public ArrayList<?> loadInBackground() {
		ArrayList<?> votes;
		switch(this.voteQueryType){
			case Voting.VOTES_SHOWS:
				 
				votes = Voting.getShows(this.voteQueryResultType, queryResults, queryOffset, db);
				break;
			case Voting.VOTES_ARTISTS:
				 
				votes = Voting.getArtists(this.voteQueryResultType, queryResults, queryOffset, db);
				break;
			case Voting.VOTES_SHOWS_BY_ARTIST:
				 
				votes = Voting.getShowsByArtist(this.voteQueryResultType, queryResults, queryOffset, artistId, db);
				break;
			default:
				votes = new ArrayList<ArchiveVoteObj>();
				break;
		}
		return votes;
	}
	
	
	
	@Override
	public void deliverResult(ArrayList<?> o){
		 
		super.deliverResult(o);
	}


}