/*
 * ActiveDownloadScreen.java
 * VERSION 1.4
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

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import com.code.android.vibevault.R;

public class ActiveDownloadScreen extends Activity {

	private DownloadService dService = null;
	private ListView downloadList;
	
	@Override
	public void onCreate(Bundle savedInstanceState){

		super.onCreate(savedInstanceState);
		setContentView(R.layout.active_download_screen);
		
		downloadList = (ListView) findViewById(R.id.DownloadListView);
		downloadList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> a, View v, int position, long id){

			}
		});
		downloadList.setOnCreateContextMenuListener(new OnCreateContextMenuListener(){
			@Override
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo){
				menu.add(Menu.NONE, VibeVault.RESUME_DOWNLOAD, Menu.NONE, "Resume Download");
				menu.add(Menu.NONE, VibeVault.PAUSE_DOWNLOAD, Menu.NONE, "Pause Download");
				menu.add(Menu.NONE, VibeVault.CANCEL_DOWNLOAD, Menu.NONE, "Cancel Download");
			}
		});
	}
	
	public void onResume()
	{
		super.onResume();
		getApplicationContext().bindService(new Intent(this, DownloadService.class), onDService, BIND_AUTO_CREATE);
		registerReceiver(DownloadReceiver, new IntentFilter(VibeVault.BROADCAST_DOWNLOAD_STATUS));
	}
	
	public void onPause()
	{
		super.onPause();
		getApplicationContext().unbindService(onDService);
		unregisterReceiver(DownloadReceiver);
	}
	
	private void refreshDownloadList(){
		downloadList.setAdapter(new DownloadAdapter(this,R.layout.download_row, 
				VibeVault.downloadSongs));
	}
	
	private BroadcastReceiver DownloadReceiver=new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			refreshDownloadList();
		}
	};
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.help_cleardownloaded_options, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected (MenuItem item){
		switch (item.getItemId()){
			case R.id.clearDownloadedSongs:
				dService.clearDownloaded();
				refreshDownloadList();
				break;
			case R.id.scrollableDialog:
				AlertDialog.Builder ad = new AlertDialog.Builder(this);
				ad.setTitle("Help!");
				View v =LayoutInflater.from(this).inflate(R.layout.scrollable_dialog, null);
				((TextView)v.findViewById(R.id.DialogText)).setText(R.string.download_screen_help);
				ad.setPositiveButton("Okay.", new android.content.DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int arg1) {
					}
				});
				ad.setView(v);
				ad.show();
		}
		return true;
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item){
		AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo)item.getMenuInfo();
		if(dService == null){

		}
		else if(menuInfo!=null){
			switch(item.getItemId()){
			case(VibeVault.RESUME_DOWNLOAD):
				dService.resume(menuInfo.position);
				break;
			case(VibeVault.PAUSE_DOWNLOAD):
				dService.pause(menuInfo.position);
				break;
			case(VibeVault.CANCEL_DOWNLOAD):
				dService.cancel(menuInfo.position);
				VibeVault.downloadSongs.remove(menuInfo.position);
				dService.setDownloadingPosition();
				refreshDownloadList();
				break;
			default:
				return false;
			}
			return true;
		}
		return false;
	}
	
	private class DownloadAdapter extends ArrayAdapter<ArchiveSongObj>{
		
		public DownloadAdapter(Context context, int textViewResourceId, List<ArchiveSongObj> objects){
			super(context, textViewResourceId, objects);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent){
			ArchiveSongObj song = VibeVault.downloadSongs.get(position);
			int status = song.getDownloadStatus();
			if(convertView==null){
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = vi.inflate(R.layout.download_row, null);
			}
			TextView downloadTitle = (TextView) convertView.findViewById(R.id.Download_Title);
			TextView downloadText = (TextView) convertView.findViewById(R.id.Download_Text);
			ImageView downloadIcon = (ImageView) convertView.findViewById(R.id.Download_Icon);
			ProgressBar downloadProgress = (ProgressBar) convertView.findViewById(R.id.Download_Progress);
			if(song != null){
				downloadTitle.setText(song.toString());
				switch(status){
					case DownloadSongThread.ERROR:
						downloadIcon.setImageDrawable(getBaseContext().getResources().getDrawable(R.drawable.ic_delete));
						downloadIcon.setVisibility(View.VISIBLE);
						downloadProgress.setVisibility(View.GONE);
						downloadText.setVisibility(View.GONE);
						break;
					case DownloadSongThread.COMPLETE:
						downloadIcon.setImageDrawable(getBaseContext().getResources().getDrawable(R.drawable.btn_check_buttonless_on));
						downloadIcon.setVisibility(View.VISIBLE);
						downloadProgress.setVisibility(View.GONE);
						downloadText.setVisibility(View.GONE);
						break;
					case DownloadSongThread.DOWNLOADING:
						downloadIcon.setVisibility(View.GONE);
						if(position == dService.getDownloadingIndex()){
							int progress = dService.getProgressInt();
							downloadText.setText(progress + "%");
							downloadProgress.setProgress(progress);
						}
						break;
					case DownloadSongThread.PAUSED:
						downloadIcon.setImageDrawable(getBaseContext().getResources().getDrawable(android.R.drawable.ic_media_pause));
						downloadIcon.setVisibility(View.VISIBLE);
						downloadProgress.setVisibility(View.GONE);
						downloadText.setVisibility(View.VISIBLE);
						if(position == dService.getDownloadingIndex()){
							int progress = dService.getProgressInt();
							downloadText.setText(progress + "%");
							downloadProgress.setProgress(progress);
						}
						break;
					default:
						downloadIcon.setImageDrawable(null);
						downloadIcon.setVisibility(View.GONE);
						downloadProgress.setProgress(0);
						downloadProgress.setProgress(View.VISIBLE);
						downloadText.setText("");
						break;
				}
			}
			return convertView;
		}
	}
	
	
	
	private ServiceConnection onDService=new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder rawBinder) {

			dService=((DownloadService.DServiceBinder)rawBinder).getService();
			refreshDownloadList();
		}

		public void onServiceDisconnected(ComponentName className) {

			dService=null;
		}
	};
}
