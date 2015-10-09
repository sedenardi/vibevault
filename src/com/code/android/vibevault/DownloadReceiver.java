package com.code.android.vibevault;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

public class DownloadReceiver extends BroadcastReceiver{
	
	private static final String LOG_TAG = DownloadReceiver.class.getName();

	private StaticDataStore db;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction()))
		{
			Logging.Log(LOG_TAG, "Received Intent: " + intent.getAction());

			db = StaticDataStore.getInstance(context);
			
			long receivedID = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
			DownloadManager mgr = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
			
			DownloadManager.Query query = null;
			query = new DownloadManager.Query();
			query.setFilterById(receivedID);
			Cursor cur = mgr.query(query);
			int index = cur.getColumnIndex(DownloadManager.COLUMN_STATUS);
			if(cur.moveToFirst())
			{
				if(cur.getInt(index) == DownloadManager.STATUS_SUCCESSFUL){
					Logging.Log(LOG_TAG, "Song downloaded successfully: " + receivedID);
					db.setSongDownloaded(receivedID);
				}
			}
			cur.close();
		}
	}
}
