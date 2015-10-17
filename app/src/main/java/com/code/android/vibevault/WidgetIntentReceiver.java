package com.code.android.vibevault;

import java.util.ArrayList;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class WidgetIntentReceiver extends BroadcastReceiver{
	
	private static final String LOG_TAG = WidgetIntentReceiver.class.getName();
	
	@Override
	public void onReceive(Context ctx, Intent intent) {
		Logging.Log(LOG_TAG, "onReceive()");
		if (intent.getAction().equals(PlaybackService.SERVICE_STATE)) {
			RemoteViews remoteRowViews = new RemoteViews(ctx.getPackageName(), R.layout.widget_row);
			RemoteViews remoteListViews = new RemoteViews(ctx.getPackageName(), R.layout.widget_list);
			Logging.Log(LOG_TAG, "Receiving PlaybackService.SERVICE_STATE...");
			// Switch Widget play/pause button images based on PlaybackService status.
			int status = intent.getIntExtra(PlaybackService.EXTRA_STATUS, PlaybackService.STATUS_STOPPED);
			switch(status){
				case PlaybackService.STATUS_PLAYING:
					remoteRowViews.setInt(R.id.WidgetRowToggleButton, "setBackgroundResource", R.drawable.mediapausebutton);
					remoteListViews.setInt(R.id.PauseButton, "setBackgroundResource", R.drawable.mediapausebutton);
					break;
				case PlaybackService.STATUS_PAUSED:
				case PlaybackService.STATUS_STOPPED:
					remoteRowViews.setInt(R.id.WidgetRowToggleButton, "setBackgroundResource", R.drawable.mediaplaybutton);
					remoteListViews.setInt(R.id.PauseButton, "setBackgroundResource", R.drawable.mediaplaybutton);
					break;
				default:
			}
			// Set song title if valid.
			ArrayList<ArchiveSongObj> songs = (ArrayList<ArchiveSongObj>) intent.getSerializableExtra(PlaybackService.EXTRA_PLAYLIST);
			int currentPos = intent.getIntExtra(PlaybackService.EXTRA_PLAYLIST_POSITION, 0);
			if (currentPos > -1 && currentPos < songs.size()) {
				ArchiveSongObj song = songs.get(currentPos);
				remoteRowViews.setTextViewText(R.id.WidgetRowSongTitle, song.getSongTitle());
				remoteRowViews.setTextViewText(R.id.WidgetRowArtistTitle, song.getShowArtist());
				remoteListViews.setTextViewText(R.id.WidgetListShowInfo, songs.get(currentPos).getSongArtistAndTitle());
			}
			Logging.Log(LOG_TAG, "Pushing update to widget...");
			// Set up buttons.
			WidgetListProvider.setButtons(ctx, remoteListViews);
			WidgetRowProvider.setButtons(ctx, remoteRowViews);
			// Set up list of songs.
			WidgetListProvider.songList(ctx, intent.getExtras().getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS), remoteListViews);
			// Update Widgets.
			WidgetRowProvider.pushUpdate(ctx, remoteRowViews);
			WidgetListProvider.pushUpdate(ctx, remoteListViews);
		}
	}
}
