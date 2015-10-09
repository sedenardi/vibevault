/*
 * ShowsDownloadedScreen.java
 * VERSION 2.0
 * 
 * Copyright 2011 Andrew Pearson and Sanders DeNardi.
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

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

import com.code.android.vibevault.R;

public class ShowsDownloadedScreen extends Activity {
	
	private static final String LOG_TAG = ShowsDownloadedScreen.class.getName();
	
	private PlaybackService pService = null;
	private ListView showList;
	
	@Override
	public void onResume(){
		super.onResume();
		getApplicationContext().bindService(new Intent(this, PlaybackService.class), conn, BIND_AUTO_CREATE);
		refreshShowList();
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
		getApplicationContext().unbindService(conn);
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.download_shows_screen);
		
		showList = (ListView) findViewById(R.id.DownloadShowsListView);
		showList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> a, View v, int position, long id){
				openShow(id);
			}
		});
		showList.setOnCreateContextMenuListener(new OnCreateContextMenuListener(){
			@Override
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo){
				menu.add(Menu.NONE, VibeVault.DELETE_SHOW, Menu.NONE, "Delete Show");
			}
		});
		refreshShowList();
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item){
		AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo)item.getMenuInfo();
		if(menuInfo!=null){
			ArchiveShowObj selShow = VibeVault.db.getShow(menuInfo.id);
			switch(item.getItemId()){
			case (VibeVault.DELETE_SHOW):
				Cursor c = VibeVault.db.getSongsFromShow(selShow.getIdentifier());
				int numSongs = c.getCount();
				ArrayList<ArchiveSongObj> songs = new ArrayList<ArchiveSongObj>();
				for(int i = 0; i < numSongs; i++){
					c.moveToPosition(i);
					ArchiveSongObj songObjToDel = VibeVault.db.getSong(
							c.getInt(c.getColumnIndex("_id")));					
					songs.add(songObjToDel);
				}
				c.close();
				DeletionTask task = new DeletionTask();
				task.execute(songs.toArray(new ArchiveSongObj[0]));
				refreshShowList();
				break;
			default:
				return false;
			}
			return true;
		}
		return false;
	}
	
	private void refreshShowList(){		
		Cursor listCur = VibeVault.db.getDownloadShows();
		this.startManagingCursor(listCur);
		showList.setAdapter(new ScrollingCursorAdapter(this, listCur));
	}
	
	private void openShow(long pos){
		ArchiveShowObj show = VibeVault.db.getShow(pos);
		if(show != null){
			Intent i = new Intent(ShowsDownloadedScreen.this, DownloadedShowScreen.class);
			i.putExtra("Show", show);
			startActivity(i);
		}
	}
	
	
	public class DeletionTask extends AsyncTask<ArchiveSongObj, Boolean, Integer> {

		@Override
		protected Integer doInBackground(ArchiveSongObj... songs) {
			int numDeletedSongs = 0;
			for(ArchiveSongObj song : songs){
				pService.updatePlaying();
				ArchiveSongObj curSong = pService.getPlayingSong();
				if(curSong!=null&&curSong.equals(song)){
					publishProgress(false);
					continue;
				}
				File songToDelete = new File(song.getFilePath());
				boolean deleted = songToDelete.delete();
				if(deleted){
					VibeVault.db.setSongDeleted(song);
					numDeletedSongs++;
					publishProgress(true);
				}
			}
			return numDeletedSongs;
		}
		
		@Override
		protected void onProgressUpdate(Boolean... b){
			if(b[0]==false){
				Toast.makeText(getBaseContext(), "You can't delete a playing song.", Toast.LENGTH_SHORT).show();
			}
			refreshShowList();
		}
		
		
		protected void onPostExecute(Integer i){
			Toast.makeText(getBaseContext(), "Deleted " + i + " song(s)...", Toast.LENGTH_SHORT).show();
			refreshShowList();
		}
		
		
	}

	private ServiceConnection conn = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			pService = ((PlaybackService.ListenBinder) service).getService();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.w(LOG_TAG, "DISCONNECT");
			pService = null;
		}
	};
	
	
}
