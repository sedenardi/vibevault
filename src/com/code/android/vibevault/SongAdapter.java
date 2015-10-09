package com.code.android.vibevault;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.PopupMenu.OnMenuItemClickListener;

public class SongAdapter extends ArrayAdapter<ArchiveSongObj> {

	private Context context;
	private ArrayList<ArchiveSongObj> showSongs = null;
	private StaticDataStore db = null;
	
	public SongAdapter(Context context, int textViewResourceId, List<ArchiveSongObj> objects, StaticDataStore db) {
		super(context, textViewResourceId, objects);
		showSongs = (ArrayList<ArchiveSongObj>)objects;
		this.context = context;
		this.db = db;
	}
	
	public void setSongSelected(int pos){
		
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final ArchiveSongObj song = showSongs.get(position);
		if (convertView == null) {
			LayoutInflater vi = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = vi.inflate(R.layout.playlist_row, null);
		}
		if(song!=null){
			final TextView songText = (TextView) convertView.findViewById(R.id.SongTitle);
			final TextView artistText = (TextView) convertView.findViewById(R.id.ArtistTitle);
			final ImageView menuIcon = (ImageView) convertView.findViewById(R.id.menuIcon);
			final PopupMenu menu = new PopupMenu(context, menuIcon);
			final ImageView icon = (ImageView) convertView.findViewById(R.id.icon);
			songText.setText(song.getSongTitle());
			songText.setSelected(true);
			songText.setMarqueeRepeatLimit(-1);
			songText.setSingleLine();
			songText.setHorizontallyScrolling(true);
			artistText.setText(song.getShowArtist());
			artistText.setSelected(true);
			artistText.setSelected(true);
			artistText.setMarqueeRepeatLimit(-1);
			artistText.setSingleLine();
			artistText.setHorizontallyScrolling(true);
			menuIcon.setVisibility(View.VISIBLE);


			if (db.songIsDownloaded(song.getFileName())) {
					icon.setImageResource(R.drawable.downloadedicon);
					icon.setVisibility(View.VISIBLE);
					menu.getMenu().add(Menu.NONE, 102, Menu.NONE, "Delete song");
				} else {
					icon.setVisibility(View.GONE);
					menu.getMenu().add(Menu.NONE, 103, Menu.NONE, "Download song");
			}
			menuIcon.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					if(menu.getMenu().size()==0){
						menu.getMenuInflater().inflate(R.menu.song_options_menu, menu.getMenu());
					}
					if (db.songIsDownloaded(song.getFileName())) {
						menu.show();
	
					} else {
						menu.show();
				}
				}
			});
			menu.setOnMenuItemClickListener(new OnMenuItemClickListener(){
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					switch (item.getItemId()) {
						case (103):
							DownloadingAsyncTask task = new DownloadingAsyncTask(context);
							task.execute(song);
							break;
						case (R.id.AddButton):
							Intent intent = new Intent(PlaybackService.ACTION_QUEUE_SONG);
							intent.putExtra(PlaybackService.EXTRA_SONG, song);
							intent.putExtra(PlaybackService.EXTRA_DO_PLAY, false);
							getContext().startService(intent);
							break;
						case (102):
							if(Downloading.deleteSong(getContext(), song, db)){
								Toast.makeText(getContext(), "Song deleted.", Toast.LENGTH_SHORT).show();
								icon.setVisibility(View.GONE);
							} else{
								Toast.makeText(getContext(), "Error, song not deleted.", Toast.LENGTH_SHORT).show();
							}
						break;
						default:
							return false;
						}
					return true;
				}
			});
			
		}
		return convertView;
	}

}