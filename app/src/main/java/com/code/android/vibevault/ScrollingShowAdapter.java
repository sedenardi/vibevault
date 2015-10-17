package com.code.android.vibevault;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.PopupMenu.OnMenuItemClickListener;

public class ScrollingShowAdapter extends ArrayAdapter<ArchiveShowObj> {
	
	private static final String LOG_TAG = ScrollingShowAdapter.class.getName();

	public static final int MENU_RECENT = 0;
	public static final int MENU_BOOKMARK = 1;
	public static final int MENU_DOWNLOAD = 2;

	Context context;
	int textResourceId;
	List<ArchiveShowObj> shows = null;
	LayoutInflater inflater;
	private StaticDataStore db = null;	
	private int menu_type = -1;
	
	private ShareActionProvider mShareActionProvider;

	
	public ScrollingShowAdapter(Context context, int textViewResourceId,
			List<ArchiveShowObj> objects, StaticDataStore db, int menu_type) {
		super(context, textViewResourceId, objects);
		this.context = context;
		this.textResourceId = textViewResourceId;
		this.shows = objects;
		inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.db = db;
		this.menu_type = menu_type;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent){
		final ArchiveShowObj show = shows.get(position);
		if (convertView == null){
			convertView = inflater.inflate(R.layout.playlist_row, parent, false);
		}
		TextView showText = (TextView) convertView.findViewById(R.id.SongTitle);
		TextView artistText = (TextView) convertView.findViewById(R.id.ArtistTitle);
		ImageView ratingsIcon = (ImageView) convertView.findViewById(R.id.icon);
		final ImageView menuIcon = (ImageView) convertView.findViewById(R.id.menuIcon);
		final PopupMenu menu = new PopupMenu(this.getContext(), menuIcon);
		String artist = show.getShowArtist();
		String showTitle = show.getShowTitle();
		artistText.setText(artist);
		showText.setText(showTitle);
		showText.setSelected(true);
		artistText.setSingleLine();
		showText.setMarqueeRepeatLimit(-1);
		showText.setSingleLine();
		showText.setHorizontallyScrolling(true);
		ratingsIcon.setVisibility(View.GONE);
		menuIcon.setVisibility(View.VISIBLE);
		

		
		
		
		menuIcon.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				
				if(db.getShowExists(show)){
					int status = db.getShowDownloadStatus(show);
					if(menu.getMenu().size()==0){
						menu.getMenuInflater().inflate(R.menu.show_options_menu, menu.getMenu());
						if(status == StaticDataStore.SHOW_STATUS_NOT_DOWNLOADED){
							menu.getMenu().add(Menu.NONE, 100, Menu.NONE, "Download Show");
						} else if(status == StaticDataStore.SHOW_STATUS_FULLY_DOWNLOADED){
							menu.getMenu().add(Menu.NONE, 101, Menu.NONE, "Delete Show");
						} else{
							menu.getMenu().add(Menu.NONE, 100, Menu.NONE, "Download Remaining");
							menu.getMenu().add(Menu.NONE, 101, Menu.NONE, "Delete Downloaded");
	
						}
						if (menu_type == ScrollingShowAdapter.MENU_RECENT) {
							menu.getMenu().add(Menu.NONE, 102, Menu.NONE, "Remove From Recent");
						} else if (menu_type == ScrollingShowAdapter.MENU_BOOKMARK) {
							menu.getMenu().add(Menu.NONE, 103, Menu.NONE, "Remove From Bookmarks");							
						} else if (menu_type == ScrollingShowAdapter.MENU_DOWNLOAD) {
							
						}
					}
				}
				
				menu.setOnMenuItemClickListener(new OnMenuItemClickListener(){
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						Intent i = new Intent(Intent.ACTION_SEND);
						i.setType("text/plain");
						// Add data to the intent, the receiving app will decide what to do with it.
						i.putExtra(Intent.EXTRA_SUBJECT, "Vibe Vault");
						i.putExtra(Intent.EXTRA_TEXT, show.getShowArtist() + " " + show.getShowTitle() + " " + show.getShowURL()
								+ "\n\nSent using #VibeVault for Android.");
						MenuItem share = menu.getMenu().findItem(R.id.ShareButton);
					    mShareActionProvider = (ShareActionProvider) share.getActionProvider();
						if(mShareActionProvider!=null){
							mShareActionProvider.setShareIntent(i);
						}
						switch (item.getItemId()) {
							case (100):
								DownloadingAsyncTask task = new DownloadingAsyncTask(context);
								ArrayList<ArchiveSongObj> songs = db.getSongsFromShow(show.getIdentifier());
								task.execute(songs.toArray(new ArchiveSongObj[songs.size()]));
								break;
							case (R.id.AddButton):
								Intent intent = new Intent(PlaybackService.ACTION_QUEUE_SHOW);
								intent.putExtra(PlaybackService.EXTRA_DO_PLAY, false);
								intent.putExtra(PlaybackService.EXTRA_PLAYLIST, db.getSongsFromShow(show.getIdentifier()));
								getContext().startService(intent);
								break;
							case (101):
								DeleteTask d = new DeleteTask(context);
								d.execute(show);
								shows.remove(show);
								notifyDataSetChanged();
								break;
							case (102):
								RemoveTask r = new RemoveTask(context,ScrollingShowAdapter.MENU_RECENT);
								r.execute(show.DBID);
								shows.remove(show);
								notifyDataSetChanged();
								break;
							case (103):
								RemoveTask b = new RemoveTask(context,ScrollingShowAdapter.MENU_BOOKMARK);
								b.execute(show.DBID);
								shows.remove(show);
								notifyDataSetChanged();
								break;
							default:
								return false;
							}
						return true;
					}
				});
				menu.show();
			}
		});
		
		return convertView;
	}
	
	private class RemoveTask extends AsyncTask<Integer,Void,Boolean> {

		private Context ctx;
		private StaticDataStore db;
		private int removeType = -1;
		
		public RemoveTask(Context context, int type) {
			ctx = context;
			db = StaticDataStore.getInstance(ctx);
			removeType = type;
		}
		
		@Override
		protected Boolean doInBackground(Integer... arg0) {
			if (removeType == ScrollingShowAdapter.MENU_RECENT) {
				db.deleteRecentShow(arg0[0]);
			} else if (removeType == ScrollingShowAdapter.MENU_BOOKMARK) {
				db.deleteFavoriteShow(arg0[0]);
			}
			return true;
		}
		
	}

	private class DeleteTask extends AsyncTask<ArchiveShowObj,Void,Boolean> {

		private Context ctx;
		private StaticDataStore db;
		
		public DeleteTask(Context context) {
			Logging.Log(LOG_TAG, "Constructor");
			ctx = context;
			db = StaticDataStore.getInstance(ctx);
		}
		
		@Override
		protected Boolean doInBackground(ArchiveShowObj... params) {
			Logging.Log(LOG_TAG, "Background Thread");
			return Downloading.deleteShow(ctx, params[0], db);
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			Logging.Log(LOG_TAG,"Post Execute");
			if (result) {
				Toast.makeText(ctx, R.string.confirm_show_deleted_message_text, Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(ctx, R.string.error_show_not_deleted_message_text, Toast.LENGTH_SHORT).show();
			}
		}
	}
	
}
