package com.code.android.vibevault;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

public class WidgetListService extends RemoteViewsService{

	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		return new WidgetListViewsFactory(this.getApplicationContext(), intent);
	}

}

class WidgetListViewsFactory implements RemoteViewsService.RemoteViewsFactory {

	private static final String LOG_TAG = WidgetListViewsFactory.class.getName();
	
	private StaticDataStore db;
	private ArrayList<ArchiveSongObj> songs;
	private int nowPlayingPosition = -1;
	
	private Context ctx;
	
	public WidgetListViewsFactory(Context ctx, Intent intent) {
		this.ctx = ctx;
	}

	@Override
	public void onCreate() {
		db = StaticDataStore.getInstance(ctx);
	}
	
	@Override
	public int getCount() {
		return songs.size();
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public RemoteViews getLoadingView() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RemoteViews getViewAt(int position) {
		RemoteViews row = new RemoteViews(ctx.getPackageName(), R.layout.widget_list_row);
		row.setTextViewText(R.id.SongTitle, songs.get(position).getSongTitle());
				
		int color = (position == nowPlayingPosition) ? Color.YELLOW : Color.rgb(18, 125, 212);
		row.setTextColor(R.id.SongTitle, color);
		
		Intent fillInIntent = new Intent();
		fillInIntent.putExtra(WidgetListProvider.WIDGET_EXTRA, position);
		row.setOnClickFillInIntent(R.id.SongTitle, fillInIntent);
		
		return row;
	}

	@Override
	public int getViewTypeCount() {
		// TODO Auto-generated method stub
		return 1;
	}

	@Override
	public boolean hasStableIds() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onDataSetChanged() {
		songs = db.getNowPlayingSongs();
		if (db.getPref("nowPlayingPosition") != "NULL")
		{
			nowPlayingPosition = Integer.parseInt(db.getPref("nowPlayingPosition"));
		}
		Logging.Log(LOG_TAG, "onDataSetChanged() - nowPlayingPos:" + nowPlayingPosition);
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		
	}

}