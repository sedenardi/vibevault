package com.code.android.vibevault;

import java.util.ArrayList;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

public class WidgetListProvider extends AppWidgetProvider {

	public static final String WIDGET_PREFIX = "com.code.android.vibevault.WidgetListProvider.";
	public static final String WIDGET_PREV = WIDGET_PREFIX + "PREVIOUS";
	public static final String WIDGET_TOGGLE = WIDGET_PREFIX + "TOGGLE";
	public static final String WIDGET_NEXT = WIDGET_PREFIX + "NEXT";
	public static final String WIDGET_STOP = WIDGET_PREFIX + "STOP";
	public static final String WIDGET_UPDATE = WIDGET_PREFIX + "UPDATE";
	
	public static final String WIDGET_LIST = WIDGET_PREFIX + "LIST";
	public static final String WIDGET_EXTRA = WIDGET_PREFIX + "EXTRA"; 
	
	private static final String LOG_TAG = WidgetListProvider.class.getName();
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Logging.Log(LOG_TAG, "WidgetList:  onReceive()");
		if (intent.getAction().equals(WIDGET_LIST)) {
			Intent playIntent = new Intent(PlaybackService.ACTION_PLAY_POSITION);
			playIntent.putExtra(PlaybackService.EXTRA_PLAYLIST_POSITION, intent.getIntExtra(WIDGET_EXTRA, 0));
			context.startService(playIntent);
		} else if(intent.getAction().equals(WIDGET_NEXT)){
			Intent playIntent = new Intent(PlaybackService.ACTION_NEXT);
			context.startService(playIntent);
		}  else if(intent.getAction().equals(WIDGET_PREV)){
			Intent playIntent = new Intent(PlaybackService.ACTION_PREV);
			context.startService(playIntent);
		} else if(intent.getAction().equals(WIDGET_TOGGLE)){
			Intent playIntent = new Intent(PlaybackService.ACTION_TOGGLE);
			context.startService(playIntent);
		} else if(intent.getAction().equals(WIDGET_STOP)){
			Intent playIntent = new Intent(PlaybackService.ACTION_STOP);
			context.startService(playIntent);
		}
		super.onReceive(context, intent);
	}
	
	// Called when Widget is initially created.
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		Logging.Log(LOG_TAG,"onUpdate()");
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_list);
		songList(context, appWidgetIds, remoteViews);
		
		// Set up buttons.
		setButtons(context, remoteViews);		
		// Set song title if valid.
		StaticDataStore db = StaticDataStore.getInstance(context);
		ArrayList<ArchiveSongObj> songs = db.getNowPlayingSongs();
		String artist = context.getResources().getString(R.string.nothing_playing);
		if (db.getPref("nowPlayingPosition") != "NULL") {
			int nowPlaying = Integer.parseInt(db.getPref("nowPlayingPosition"));
			if (nowPlaying > -1 && nowPlaying < songs.size()) {
				ArchiveSongObj song = songs.get(nowPlaying);
				artist = song.getShowTitle();
			}
		}
		remoteViews.setTextViewText(R.id.WidgetListShowInfo, artist);
		// Update Widget.
		appWidgetManager.updateAppWidget(appWidgetIds, remoteViews);
		Intent serviceIntent = new Intent(PlaybackService.ACTION_POLL);
		context.startService(serviceIntent);
	}
	
	public static void songList(Context context, int[] appWidgetIds, RemoteViews remoteViews){
		Intent intent = new Intent(context, WidgetListService.class);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
		intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
		remoteViews.setRemoteAdapter(R.id.WidgetListView, intent);
		Intent actionIntent = new Intent(context, WidgetListProvider.class);
		actionIntent.setAction(WIDGET_LIST);
		actionIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
		PendingIntent actionPendingIntent = PendingIntent.getBroadcast(context, 0, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		remoteViews.setPendingIntentTemplate(R.id.WidgetListView, actionPendingIntent);
	}
	
	public static void pushUpdate(Context ctx, RemoteViews remoteViews) {
		Logging.Log(LOG_TAG, "pushUpdate()...");
		AppWidgetManager manager = AppWidgetManager.getInstance(ctx);
		int[] listIds = manager.getAppWidgetIds(new ComponentName(ctx, WidgetListProvider.class));
		manager.notifyAppWidgetViewDataChanged(listIds, R.id.WidgetListView);
		
		ComponentName widget = new ComponentName(ctx, WidgetListProvider.class);
		manager.updateAppWidget(widget, remoteViews);
	}
	
	public static PendingIntent openNowPlaying(Context ctx) {
		return PendingIntent.getActivity(ctx, 0, 
				new Intent(ctx, SearchScreen.class).putExtra("type", 2)
					.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
					.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), 
				PendingIntent.FLAG_UPDATE_CURRENT);
	}
	
	private static PendingIntent buttonToggle(Context ctx) {
		Intent intent = new Intent(ctx, WidgetListProvider.class);
		intent.setAction(WIDGET_TOGGLE);
		return PendingIntent.getBroadcast(ctx, 0, intent, 0);
	}
	
	private static PendingIntent buttonNext(Context ctx) {
		Intent intent = new Intent(ctx, WidgetListProvider.class);
		intent.setAction(WIDGET_NEXT);
		return PendingIntent.getBroadcast(ctx, 0, intent, 0);
	}
	
	private static PendingIntent buttonPrev(Context ctx) {
		Intent intent = new Intent(ctx, WidgetListProvider.class);
		intent.setAction(WIDGET_PREV);
		return PendingIntent.getBroadcast(ctx, 0, intent, 0);
	}
	
	private static PendingIntent buttonStop(Context ctx) {
		Intent intent = new Intent(ctx, WidgetListProvider.class);
		intent.setAction(WIDGET_STOP);
		return PendingIntent.getBroadcast(ctx, 0, intent, 0);
	}
	
	public static void togglePlayButtonImage(int status, RemoteViews remoteViews){
		if (status == PlaybackService.STATUS_PLAYING) {
			remoteViews.setInt(R.id.PauseButton, "setBackgroundResource", R.drawable.mediapausebutton);
		} else if (status == PlaybackService.STATUS_PAUSED) {
			remoteViews.setInt(R.id.PauseButton, "setBackgroundResource", R.drawable.mediaplaybutton);
		} else if (status == PlaybackService.STATUS_STOPPED) {
			remoteViews.setInt(R.id.PauseButton, "setBackgroundResource", R.drawable.mediaplaybutton);
		}
	}
	
	public static void setButtons(Context ctx, RemoteViews remoteViews){
		remoteViews.setOnClickPendingIntent(R.id.WidgetListShowInfo, openNowPlaying(ctx));
		remoteViews.setOnClickPendingIntent(R.id.NextButton, buttonNext(ctx));
		remoteViews.setOnClickPendingIntent(R.id.PrevButton, buttonPrev(ctx));
		remoteViews.setOnClickPendingIntent(R.id.PauseButton, buttonToggle(ctx));
		remoteViews.setOnClickPendingIntent(R.id.StopButton, buttonStop(ctx));
	}
	
}
