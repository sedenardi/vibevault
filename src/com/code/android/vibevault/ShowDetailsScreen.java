/*
 * ShowDetailsScreen.java
 * VERSION 1.1
 * 
 * Copyright 2010 Andrew Pearson and Sanders DeNardi.
 * 
 * This file is part of Vibe Vault.
 * 
 * Vibe Vault is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * 
 */

package com.code.android.vibevault;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.util.ByteArrayBuffer;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.code.android.vibevault.R;

public class ShowDetailsScreen extends Activity {
	

	private PlayerService pService = null;
	private DownloadService dService = null;
	
	private ArrayList<ArchiveSongObj> downloadLinks;
	private ArchiveShowObj show;
	private TextView showLabel;
	private ListView trackList;
	
	private ParseShowDetailsPageTask workerTask;
	private boolean dialogShown;
	private String showTitle;
	
	/** Create the activity, taking into account ongoing dialogs or already downloaded data.
	*
	* If there is a retained ParseShowDetailsPageTask, set its parent
	* activity to the newly created ShowDetailsScreen (the old one was
	* destroyed because of an orientation change or something.  This
	* way, the ParseShowDetailsPageTask does not leak any of the Views
	* from the old ShowDetailsScreen.  Also, grab the songs from the
	* ParseShowDetailsPageTask to refresh the list of songs with.
	*/
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.show_details_screen);
		Bundle b = getIntent().getExtras();
		show = (ArchiveShowObj)b.get("Show");
		
		showTitle = show.getTitle();
		showLabel = (TextView) findViewById(R.id.ShowLabel);
		showLabel.setText(showTitle);

		trackList = (ListView) findViewById(R.id.SongsListView);
		trackList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> a, View v, int position, long id){
				playShow(position);
			}
		});
		trackList.setOnCreateContextMenuListener(new OnCreateContextMenuListener(){
			@Override
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo){
				menu.add(Menu.NONE, VibeVault.ADD_SONG_TO_QUEUE, Menu.NONE, "Add to playlist");
				menu.add(Menu.NONE, VibeVault.DOWNLOAD_SONG, Menu.NONE, "Download Song");
				menu.add(Menu.NONE, VibeVault.DOWNLOAD_SHOW, Menu.NONE, "Download Entire Show");
				menu.add(Menu.NONE, VibeVault.EMAIL_LINK, Menu.NONE, "Email Link to Song");
			}
		});

		downloadLinks = new ArrayList<ArchiveSongObj>();
		Object retained = getLastNonConfigurationInstance();
		if(retained instanceof ParseShowDetailsPageTask){
			
			workerTask = (ParseShowDetailsPageTask)retained;
			workerTask.setActivity(this);
			downloadLinks = workerTask.songs;
		} else if (show.getShowURL()!=null){
			workerTask = new ParseShowDetailsPageTask(this);
			workerTask.execute(show);
		}
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		getApplicationContext().bindService(new Intent(this, DownloadService.class), onDService, BIND_AUTO_CREATE);
		getApplicationContext().bindService(new Intent(this, PlayerService.class), onPService, BIND_AUTO_CREATE);
		registerReceiver(TitleReceiver, new IntentFilter(VibeVault.BROADCAST_SONG_TITLE));
		refreshTrackList();
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
		getApplicationContext().unbindService(onDService);
		getApplicationContext().unbindService(onPService);
		unregisterReceiver(TitleReceiver);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.help_recentshows_nowplaying_email_options, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected (MenuItem item){
		switch (item.getItemId()){
			case R.id.nowPlaying: 	//Open playlist activity
				Intent i = new Intent(ShowDetailsScreen.this, NowPlayingScreen.class);
				startActivity(i);
				break;
			case R.id.recentShows:
				Intent rs = new Intent(ShowDetailsScreen.this, RecentShowsScreen.class);
				startActivity(rs);
				break;
			case R.id.scrollableDialog:
				AlertDialog.Builder ad = new AlertDialog.Builder(this);
				ad.setTitle("Help!");
				View v =LayoutInflater.from(this).inflate(R.layout.scrollable_dialog, null);
				((TextView)v.findViewById(R.id.DialogText)).setText(R.string.show_details_screen_help);
				ad.setPositiveButton("Cool.", new android.content.DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int arg1) {
					}
				});
				ad.setView(v);
				ad.show();
				break;
			case R.id.emailLink:
				final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
				emailIntent.setType("plain/text");
				emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Great show on archive.org: " + show.getTitle());
				emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Hey,\n\nYou should listen to " + show.getTitle() + ".  You can find it here: " + show.getShowURL());
				startActivity(Intent.createChooser(emailIntent, "Send mail..."));
				break;
			default:
				break;
		}
		return true;
	}
	
	private void playShow(int pos){
		if(!downloadLinks.isEmpty()){
			VibeVault.playList.setPlayList(downloadLinks);
			pService.playSongFromPlaylist(pos);
		}
	}

	
	/** Handle user's long-click selection.
	*
	*/
	@Override
	public boolean onContextItemSelected(MenuItem item){
		AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo)item.getMenuInfo();
		if(menuInfo!=null){
			ArchiveSongObj selSong = (ArchiveSongObj)trackList.getAdapter().getItem(menuInfo.position);
			switch(item.getItemId()){
			case(VibeVault.STREAM_CONTEXT_ID):
				// Start streaming.
				int track = pService.enqueue(selSong);
				pService.playSongFromPlaylist(track);
				break;
			case(VibeVault.DOWNLOAD_SONG):
				selSong.setDownloadShow(show);
				dService.addSong(selSong);
				break;
			case (VibeVault.DOWNLOAD_SHOW):
				for(int j = 0; j < downloadLinks.size(); j++){
					downloadLinks.get(j).setDownloadShow(show);
					dService.addSong(downloadLinks.get(j));
				}
				break;
			case (VibeVault.ADD_SONG_TO_QUEUE):
				pService.enqueue(selSong);
				break;
			case (VibeVault.EMAIL_LINK):
				final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
				emailIntent.setType("plain/text");
				emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Great song on archive.org: " + selSong.toString());
				emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Hey,\n\nI found a song you should listen to.  It's called " + selSong.toString() + " and it's off of " + selSong.getShowTitle() + ".  You can get it here: " + selSong.getLowBitRate());
				startActivity(Intent.createChooser(emailIntent, "Send mail..."));
				break;
			default:
				return false;
			}
			return true;
		}
		return false;
	}
	
	/** Persist worker Thread across orientation changes.
	*
	* Includes Thread bookkeeping to prevent not leaking Views on orientation changes.
	*/
	@Override
	public Object onRetainNonConfigurationInstance(){
		workerTask.setActivity(null);
		return workerTask;
	}
	
	/** Dialog preparation method.
	*
	* Includes Thread bookkeeping to prevent not leaking Views on orientation changes.
	*/
	@Override
	protected void onPrepareDialog(int id, Dialog dialog){
		super.onPrepareDialog(id, dialog);
		if(id==VibeVault.LOADING_DIALOG_ID){
			dialogShown = true;
		}
	}
	
	/** Dialog creation method.
	*
	* Includes Thread bookkeeping to prevent not leaking Views on orientation changes.
	*/
	@Override
	protected Dialog onCreateDialog(int id){
		switch(id){
			case VibeVault.LOADING_DIALOG_ID:
				ProgressDialog dialog = new ProgressDialog(this);
				dialog.setMessage("Loading");
				return dialog;
			default:
				return super.onCreateDialog(id);	
		}
	}

	/** Refresh the track list with whatever data List of songs contains.
	*
	*/	
	private void refreshTrackList() {
		trackList.setAdapter(new ColoredAdapter(this, R.layout.show_details_screen_row, downloadLinks));
	}
	
	/** Bookkeeping method to deal with dialogs over orientation changes.
	*
	*/
	private void onTaskCompleted(){
		if(dialogShown){
			try{
				dismissDialog(VibeVault.LOADING_DIALOG_ID);
			} catch(IllegalArgumentException e){
				e.printStackTrace();
			}
			dialogShown=false;
			refreshTrackList();
		}
	}
	
	/** Update the List of songs, and tell the user if no songs are in the list.
	*
	*/
	private void setSongs(ArrayList<ArchiveSongObj> songs){
		if(songs.size()==0){
			Toast.makeText(getBaseContext(), "Looks like the show's got no downloadable content...  Maybe try again later...", Toast.LENGTH_SHORT).show();
		}
		downloadLinks = songs;
		refreshTrackList();
	}
	
	
	/** Parse the show details page.
	 * 
	 * I didn't want to use anexternal .JAR to parse the HTML, but
	 * it is better practice than "rolling my own" parser using regex's
	 * or something.  We use HtmlCleaner instead of TagSoup because
	 * it is smaller, even though HtmlCleaner doesn't support 100% of
	 * XPath features.  We use another AsyncTask to not block the UI thread.
	 * I don't know if this is really necessary, but I think that it is good
	 * idea in case we want to use this Activity in different ways in the future.
	 */
	private static class ParseShowDetailsPageTask extends AsyncTask<ArchiveShowObj, Void, Void> {
		
		private ShowDetailsScreen parentScreen;
		private boolean completed;
		private ArrayList<ArchiveSongObj> songs = new ArrayList<ArchiveSongObj>();
		private ArchiveShowObj taskShow;
		
		private ParseShowDetailsPageTask(ShowDetailsScreen activity){
			this.parentScreen = activity;
		}
		
		@Override
		protected void onPreExecute(){
			parentScreen.showDialog(VibeVault.LOADING_DIALOG_ID);
		}

		@Override
		protected Void doInBackground(ArchiveShowObj... show){
			
			taskShow = show[0];
			HtmlCleaner pageParser = new HtmlCleaner();
			CleanerProperties props = pageParser.getProperties();
			props.setAllowHtmlInsideAttributes(true);
			props.setAllowMultiWordAttributes(true);
			props.setRecognizeUnicodeChars(true);
			props.setOmitComments(true);
			
			ArrayList<String> songLinks = new ArrayList<String>();
			// XPATH says "Select out of all 'table' elements with attribute 'class' 
			// equal to 'fileFormats' which contain element 'tr'..."
			// String songXPath = "//table[@class='fileFormats']//tr";
			String m3uXPath = "//script[@type='text/javascript']";
			
			
			
			try {
				// Get the show's title, and create a TagNode of The page.
				URLConnection conn = show[0].getShowURL().openConnection();
				String showTitle = show[0].getTitle();
				String showIdent = show[0].getIdentifier();
				InputStreamReader is = new InputStreamReader(conn.getInputStream());
				TagNode node = pageParser.clean(is);
				is.close();
				
				
				URL m3uURL = null;
				if(show[0].hasVBR()){
					m3uURL = new URL(show[0].getLinkPrefix()+"_vbr.m3u");
				} else if(show[0].hasLBR()){
					m3uURL = new URL(show[0].getLinkPrefix()+"_64kb.m3u");
				}
				if(m3uURL!=null){
				} else{
					m3uURL = new URL(show[0].getLinkPrefix()+"_vbr.m3u");
				}
				
				if(m3uURL!=null){
					// Grab the M3U stream...
					URLConnection test = m3uURL.openConnection();
					if(test==null){
					}
					InputStream inStream = test.getInputStream();
					BufferedInputStream bis = new BufferedInputStream(inStream);
					ByteArrayBuffer baf = new ByteArrayBuffer(50);
					int read = 0;
					int bufSize = 512;
					byte[] buffer = new byte[bufSize];
					while(bis.available()==0){
						bis.close();
						inStream.close();
						test = m3uURL.openConnection();
						inStream = test.getInputStream();
						bis = new BufferedInputStream(inStream);
					}
					while (true) {
						read = bis.read(buffer);
						if (read == -1) {
							break;
						}
						baf.append(buffer, 0, read);
					}
					String m3uString = new String(baf.toByteArray());
					bis.close();
					inStream.close();
					
					// Now split the .M3U file based on newlines.  This will give us
					// the download links, which we store..
					String m3uLinks[] = m3uString.split("\n");
					for(String link : m3uLinks){
						songLinks.add(link);
					}
					
					// Now use an XPATH evaluation to find all of the javascript scripts on the page.
					// If one of them can be split by "IAD.playlists = ", it should have the track names
					// in it.  The second half of the split is valid javascript and can be interpreted,
					// therefore, as JSON.  Pull the song titles out of that, and together with the download
					// links make ArchiveSongObjs and add them to the list of songs.  Then, return, because
					// you don't need to try the other method of song aggregation.
					Object[] titleNodes = node.evaluateXPath(m3uXPath);
					for(Object titleNode : titleNodes){
							String jsonString = ((TagNode)titleNode).getChildren().toString();
							String jsonArray[] = jsonString.split("IAD.playlists = ");
						if ((jsonArray.length) > 1) {
							try {
								JSONObject jObject = new JSONObject(jsonArray[1]);
								JSONArray jArray = jObject.getJSONArray("names");
								if(jArray.length() == songLinks.size()){
									for (int i = 0; i < jArray.length(); i++) {
										ArchiveSongObj song = new ArchiveSongObj(jArray.get(i).toString(), songLinks.get(i), showTitle, showIdent);
										songs.add(song);
									}
								}
								else{
								}
								return null;
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				}
			} catch (XPatherException e) {
				Log.e(VibeVault.SHOW_DETAILS_TAG, e.getMessage());
			} catch (IOException e) {
				Log.e(VibeVault.SHOW_DETAILS_TAG, e.getMessage());
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void v){
			completed=true;
			parentScreen.setSongs(songs);
			notifyActivityTaskCompleted();
			VibeVault.db.insertShow(taskShow,DataStore.RECENT_SHOW_TABLE);
		}
		
		// The parent could be null if you changed orientations
		// and this method was called before the new ShowDetailsScreen
		// could set itself as this Thread's parent.
		private void notifyActivityTaskCompleted(){
			if(parentScreen!=null){
				parentScreen.onTaskCompleted();
			}
		}
		
		// When a ShowDetailsScreen is reconstructed (like after an orientation change),
		// we call this method on the retained ShowDetailsScreen (if one exists) to set
		// its parent Activity as the new ShowDetailsScreen because the old one has been destroyed.
		// This prevents leaking any of the data associated with the old ShowDetailsScreen.
		private void setActivity(ShowDetailsScreen activity){
			this.parentScreen = activity;
			if(completed){
				notifyActivityTaskCompleted();
			}
		}
	}
	
	private class ColoredAdapter extends ArrayAdapter<ArchiveSongObj> {

		public ColoredAdapter(Context context, int textViewResourceId, List<ArchiveSongObj> objects){
			super(context, textViewResourceId, objects);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent){
			ArchiveSongObj song = downloadLinks.get(position);	
			if(convertView==null){
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = vi.inflate(R.layout.show_details_screen_row, null);
			}
			TextView text = (TextView) convertView.findViewById(R.id.text);
			text.setText(song.toString());
			ImageView icon = (ImageView) convertView.findViewById(R.id.icon);
			if (song != null) {
				if (VibeVault.db.songExists(song.getFileName())) {
					icon.setImageDrawable(getBaseContext().getResources().getDrawable(android.R.drawable.star_big_on));
					VibeVault.db.insertShow(show, DataStore.DOWNLOADED_SHOW_TABLE);
					VibeVault.db.insertSong(song);
				} else{
					VibeVault.db.deleteSong(song.getFileName(), show.getIdentifier());
					icon.setImageDrawable(null);
				}
			}
			return convertView;
		}
		
	}
	
	private ServiceConnection onDService=new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder rawBinder) {
			dService=((DownloadService.DServiceBinder)rawBinder).getService();
		}

		public void onServiceDisconnected(ComponentName className) {
			dService=null;
		}
	};

	private ServiceConnection onPService=new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder rawBinder) {
			pService=((PlayerService.MPlayerBinder)rawBinder).getService();
		}

		public void onServiceDisconnected(ComponentName className) {
			pService=null;
		}
	};
	
	private BroadcastReceiver TitleReceiver=new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
		}
	};
}