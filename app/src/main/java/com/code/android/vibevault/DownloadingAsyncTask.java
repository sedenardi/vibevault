package com.code.android.vibevault;

import android.content.Context;
import android.os.AsyncTask;

public class DownloadingAsyncTask extends AsyncTask<ArchiveSongObj,Integer,Integer> {
	
	private static final String LOG_TAG = DownloadingAsyncTask.class.getName();

	private Context ctx;
	private StaticDataStore db;
	
	public DownloadingAsyncTask(Context context) {
		Logging.Log(LOG_TAG,"Constructor");
		ctx = context;
		db = StaticDataStore.getInstance(ctx);
	}
	
	@Override
	protected Integer doInBackground(ArchiveSongObj... params) {
		for (ArchiveSongObj s : params) {
			Downloading.downloadSong(ctx, s, db);
		}
		return null;
	}

	@Override
	protected void onPostExecute(Integer result){
	}
}
