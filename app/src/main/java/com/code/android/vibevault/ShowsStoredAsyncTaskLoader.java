package com.code.android.vibevault;

import java.util.ArrayList;

import android.content.AsyncTaskLoader;
import android.content.Context;

public class ShowsStoredAsyncTaskLoader extends AsyncTaskLoader<ArrayList<ArchiveShowObj>> {

	protected static final String LOG_TAG = ShowsStoredAsyncTaskLoader.class.getName();
	public static final int STORED_RECENT_SHOWS = 0;
	public static final int STORED_FAVORITES_SHOWS = 1;
	
	private StaticDataStore db;
	
	private int storedType = -1;
	
	public ShowsStoredAsyncTaskLoader(Context context, int stored_type) {
		super(context);
		db = StaticDataStore.getInstance(context);
		this.storedType = stored_type;
	}
	
	@Override
	public void onStartLoading() {
		forceLoad();
	}

	@Override
	public ArrayList<ArchiveShowObj> loadInBackground() {
		Logging.Log(LOG_TAG,"Started with arg: " + storedType);
		ArrayList<ArchiveShowObj> shows;
		switch(this.storedType){
			case STORED_RECENT_SHOWS:
				shows = db.getRecentShows();
				break;
			case STORED_FAVORITES_SHOWS:
				shows = db.getFavoriteShows();
				break;
			default:
				shows = new ArrayList<ArchiveShowObj>();
				break;
		}
		return shows;
	}
	
	@Override
	public void deliverResult(ArrayList<ArchiveShowObj> o){
		super.deliverResult(o);
	}

}
