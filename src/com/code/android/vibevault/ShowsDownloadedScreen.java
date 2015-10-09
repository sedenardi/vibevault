/*
 * ShowsDownloadedScreen.java
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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import com.code.android.vibevault.R;

public class ShowsDownloadedScreen extends Activity {
	
	private ListView showList;

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
		refreshShowList();
	}
	
	private void refreshShowList(){		
		showList.setAdapter(new ScrollingCursorAdapter(this, VibeVault.db.getDownloadShows()));
	}
	
	private void openShow(long pos){
		ArchiveShowObj show = VibeVault.db.getShow(pos);
		if(show != null){
			Intent i = new Intent(ShowsDownloadedScreen.this, DownloadedShowScreen.class);
			i.putExtra("Show", show);
			startActivity(i);
		}
	}
}
