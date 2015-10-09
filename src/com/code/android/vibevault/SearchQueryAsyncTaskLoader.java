package com.code.android.vibevault;

import java.util.ArrayList;



import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

public class SearchQueryAsyncTaskLoader extends AsyncTaskLoader<ArrayList<ArchiveShowObj>> {
	
	private static final String LOG_TAG = SearchQueryAsyncTaskLoader.class.getName();
		
    private String mQuery = "";
	private ArrayList<ArchiveShowObj> mSearchResults = null;
	private boolean sameArtist = false;

    
	public SearchQueryAsyncTaskLoader(Context context, String passedQuery) {
		super(context);
//		sameArtist=mQuery.equals(passedQuery)?true:false;
		 
		if(mQuery.equals(passedQuery)){
			sameArtist = true;
			 

			 
		} else{
			sameArtist = false;
			 

			 
		}
		mQuery = passedQuery;
	}

	@Override
	public ArrayList<ArchiveShowObj> loadInBackground() {
		 
		if(mSearchResults == null || sameArtist == false){
			mSearchResults = new ArrayList<ArchiveShowObj>();
		}
		Searching.getShows(mQuery, mSearchResults);
		return mSearchResults;
	}
	
	@Override
	public void deliverResult(ArrayList<ArchiveShowObj> shows) {
		
		 
		 
		if(isReset()) {
			// An async query came in while the loader is stopped. We
			// don't need the result.
			if (shows != null) {
				 
			}
		}
		if(isStarted()) {
			// If the Loader is currently started, we can immediately
			// deliver its results.
			super.deliverResult(shows);
		}
		super.deliverResult(shows);
		// At this point we can release the resources associated with
		// mSearchResults if needed; now that the new result is delivered we
		// know that it is no longer in use.
//		if (mSearchResults != null) {
//			mSearchResults = null;
//		}
	}
	
	@Override protected void onStartLoading() {
        if (mSearchResults != null) {
            // If we currently have a result available, deliver it immediately.
            deliverResult(mSearchResults);
        }

        if (takeContentChanged() || mSearchResults == null) {
            // If the data has changed since the last time it was loaded
            // or is not currently available, start a load.
            forceLoad();
        }
    }
    

    /**
     * Handles a request to stop the Loader.
     */
    @Override protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }
	
	


}