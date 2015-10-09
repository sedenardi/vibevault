package com.code.android.vibevault;

import java.util.ArrayList;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

public class ShowsDownloadedAsyncTaskLoader extends AsyncTaskLoader<ArrayList<ArchiveShowObj>>{

	private StaticDataStore db;
	
	public ShowsDownloadedAsyncTaskLoader(Context context) {
		super(context);
		db = StaticDataStore.getInstance(context);
		 
	}
	
	@Override
	public void onStartLoading(){
		forceLoad();
	}

	@Override
	public ArrayList<ArchiveShowObj> loadInBackground() {
		 
		ArrayList<ArchiveShowObj> shows = db.getDownloadShows();
		return shows;
	}

	@Override
	public void deliverResult(ArrayList<ArchiveShowObj> shows){
		super.deliverResult(shows);
	}
	
}
