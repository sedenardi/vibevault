package com.code.android.vibevault;

import java.util.ArrayList;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class WidgetRowProvider extends AppWidgetProvider {

	public static final String WIDGET_PREFIX = "com.code.android.vibevault.WidgetRowProvider.";
	public static final String WIDGET_TOGGLE = WIDGET_PREFIX + "TOGGLE";
	public static final String WIDGET_NEXT = WIDGET_PREFIX + "NEXT";
	
	private static final String LOG_TAG = WidgetRowProvider.class.getName();
	
	@Override
	public void onReceive(Context ctx, Intent intent) {
		String action = intent.getAction();
		if (action.equals(WidgetRowProvider.WIDGET_TOGGLE)) {
			ctx.startService(new Intent(PlaybackService.ACTION_TOGGLE));				
		} else if (action.equals(WidgetRowProvider.WIDGET_NEXT)) {
			ctx.startService(new Intent(PlaybackService.ACTION_NEXT));				
		}
		super.onReceive(ctx, intent);
	}
	
	// Called when Widget is initially created.
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		Logging.Log(LOG_TAG,"onUpdate()");
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_row);
		// Set song title if valid.
		StaticDataStore db = StaticDataStore.getInstance(context);
		ArrayList<ArchiveSongObj> songs = db.getNowPlayingSongs();
		if (db.getPref("nowPlayingPosition") != "NULL") {
			int nowPlaying = Integer.parseInt(db.getPref("nowPlayingPosition"));
			if (nowPlaying > -1 && nowPlaying < songs.size()) {
				ArchiveSongObj song = songs.get(nowPlaying);
				remoteViews.setTextViewText(R.id.WidgetRowSongTitle, song.getSongTitle());
				remoteViews.setTextViewText(R.id.WidgetRowArtistTitle, song.getShowArtist());
			}
		}
		// Set up buttons.
		setButtons(context, remoteViews);
		// Update Widget.
		appWidgetManager.updateAppWidget(appWidgetIds, remoteViews);
		Intent serviceIntent = new Intent(PlaybackService.ACTION_POLL);
		context.startService(serviceIntent);
	}
	
	public static void pushUpdate(Context ctx, RemoteViews remoteViews) {
		Logging.Log(LOG_TAG, "pushUpdate()");
		ComponentName widget = new ComponentName(ctx, WidgetRowProvider.class);
		AppWidgetManager manager = AppWidgetManager.getInstance(ctx);
	
		manager.updateAppWidget(widget, remoteViews);
	}
	
	
	private static PendingIntent openNowPlaying(Context ctx) {
		return PendingIntent.getActivity(ctx, 0, 
				new Intent(ctx, SearchScreen.class).putExtra("type", 2)
					.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
					.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), 
				PendingIntent.FLAG_UPDATE_CURRENT);
	}
	
	private static PendingIntent buttonToggle(Context ctx) {
		Intent intent = new Intent(ctx, WidgetRowProvider.class);
		intent.setAction(WIDGET_TOGGLE);
		return PendingIntent.getBroadcast(ctx, 0, intent, 0);
	}
	
	private static PendingIntent buttonNext(Context ctx) {
		Intent intent = new Intent(ctx, WidgetRowProvider.class);
		intent.setAction(WIDGET_NEXT);
		return PendingIntent.getBroadcast(ctx, 0, intent, 0);
	}
	
	public static void setToggleButtonPlaying(RemoteViews remoteViews){
		remoteViews.setInt(R.id.WidgetRowToggleButton, "setBackgroundResource", R.drawable.mediapausebutton);
	}

	public static void setButtons(Context ctx, RemoteViews remoteRowViews) {
		remoteRowViews.setOnClickPendingIntent(R.id.WidgetRowSongTitle, openNowPlaying(ctx));
		remoteRowViews.setOnClickPendingIntent(R.id.WidgetRowToggleButton, buttonToggle(ctx));
		remoteRowViews.setOnClickPendingIntent(R.id.WidgetRowNextButton, buttonNext(ctx));
	}
	
}
