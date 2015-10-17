package com.code.android.vibevault;

import java.util.ArrayList;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.os.Bundle;

public class ShowDetailsAsyncTaskLoader extends AsyncTaskLoader<Bundle> {
	private static final String LOG_TAG = ShowDetailsAsyncTaskLoader.class.getName();
		
    private ArchiveShowObj mShow = null;
	private ArrayList<ArchiveSongObj> mSongs = null;
	private Bundle b = null;
	
	private StaticDataStore db;
	
	public ShowDetailsAsyncTaskLoader(Context context, ArchiveShowObj passedShow) {
		super(context);
		db = StaticDataStore.getInstance(context);
		// If there is an ArrayList and the query is different, clear the ArrayList.
		if(mSongs!=null && !mShow.equals(passedShow)){
			mSongs.clear();
		}
		mShow = passedShow;
	}

	
	@Override
	public Bundle loadInBackground() {
		if(b == null){
			b = new Bundle();
		}
		if(mSongs == null){
			mSongs = new ArrayList<ArchiveSongObj>();
		}
		Searching.getSongs(mShow, mSongs, db);
		Logging.Log(LOG_TAG, "Title: " + mShow.getShowTitle());
		b.putSerializable("songs", mSongs);
		b.putSerializable("show", mShow);
		return b;
	}
	
	@Override
	public void deliverResult(Bundle b) {
		
		Logging.Log(LOG_TAG, "Delivering results.");
		
		if(isReset()) {
			// An async query came in while the loader is stopped. We
			// don't need the result.
			if (b != null) {
				Logging.Log(LOG_TAG, "RESET.");
			}
		}
		super.deliverResult(b);
	}
	
	@Override 
	protected void onStartLoading() {
        if (b != null) {
        	Logging.Log(LOG_TAG, "Already have some results.");
            // If we currently have a result available, deliver it immediately.
            deliverResult(b);
        }

        if (takeContentChanged() || b == null) {
            // If the data has changed since the last time it was loaded
            // or is not currently available, start a load.
            forceLoad();
        }
    }
	
	/**
     * Handles a request to stop the Loader.
     */
    @Override 
    protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

}