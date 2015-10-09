/*
 * BrowseArtistsScreen.java
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

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;

import com.code.android.vibevault.R;

public class BrowseArtistsScreen extends Activity {

	private static final String LOG_TAG = BrowseArtistsScreen.class.getName();

	private BroadcastReceiver updateReceiver = new ParserUpdateReceiver();



	private ExpandableListView expandableList;
	private TextView statusTextView;
	private ProgressBar statusProgressBar;

	private boolean goodFetchedDate = false;
	private boolean isDownloading = false;
	private boolean finished = false;
	private int lastGroupPos = 0;

	private ArrayList<ArrayList<HashMap<String, String>>> alphaArtistsList;

	private final String[] symbols = { "1,2,3,#,!,etc...", "A", "B", "C", "D", "E", "F", "G",
			"H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T",
			"U", "V", "W", "X", "Y", "Z" };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initAlphaList();
		this.registerReceiver(updateReceiver, new IntentFilter("BROWSEUPDATE"));
		setContentView(R.layout.browse_artists_screen);
		expandableList = (ExpandableListView) findViewById(R.id.BrowseArtistsExpandableListView);
		statusTextView = (TextView) findViewById(R.id.BrowseArtistsStatusTextView);
		statusProgressBar = (ProgressBar) findViewById(R.id.BrowseArtistsStatusProgressBar);
		expandableAdapter expAdapter = new expandableAdapter();		

		expandableList.setAdapter(expAdapter);

		expandableList.setOnGroupClickListener(new OnGroupClickListener() {

			@Override
			public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
				fillAlphaList(groupPosition);
				return false;
			}
		});
		
		
		expandableList.setOnChildClickListener(new OnChildClickListener() {

			@Override
			public boolean onChildClick(ExpandableListView parent, View v,
					int groupPosition, int childPosition, long id) {
				
				String s = alphaArtistsList.get(groupPosition).get(childPosition).get("artist");
				Intent i = new Intent(BrowseArtistsScreen.this, SearchScreen.class);
				i.putExtra("Requester", "BROWSE_CONSTANT");
				i.putExtra("Artist", s);
				startActivity(i);
				return false;
			}
		});
		
		

		// FOR TESTING, CLEANS AND GUARANTEEES ARTIST TABLE REPOPULATION
		//VibeVault.db.clearArtists();

		checkFetchedStatus();
		
		if(goodFetchedDate){
			expandableList.setEnabled(true);
			expandableList.setBackgroundColor(Color.BLACK);
			// If the fetched date is good, hide the status view.
			statusTextView.setVisibility(View.GONE);
			statusProgressBar.setVisibility(View.GONE);
			isDownloading = false;
			finished = true;
		} else{
			expandableList.setEnabled(false);
			expandableList.setBackgroundColor(Color.rgb(20, 20, 20));
			// If the fetched date is bad, and there is no AsyncTask.
			if(VibeVault.workerTask==null){
				VibeVault.workerTask = new ParseArtistsPageTask();
				VibeVault.workerTask.execute();
				isDownloading = true;
			} else if(VibeVault.workerTask.getStatus().equals(AsyncTask.Status.RUNNING)){
				
				isDownloading = false;
			} else{
				
				VibeVault.workerTask = new ParseArtistsPageTask();
				VibeVault.workerTask.execute();
				isDownloading = true;
			}
		}
	}
	
	private void initAlphaList() {
		alphaArtistsList = new ArrayList<ArrayList<HashMap<String, String>>>();
		int symbolListLength = symbols.length;
		alphaArtistsList.ensureCapacity(symbolListLength);
		for (int i = 0; i < symbolListLength; i++) {

			ArrayList<HashMap<String, String>> symbolArtistsList = new ArrayList<HashMap<String, String>>();
			alphaArtistsList.add(i, symbolArtistsList);
		}
	}
	
	private void fillAlphaList(int listViewPos) {
		// Convert the position on the list to a character by adding
		// the number 64 (1 + 64 = 65 -> "A" in ASCII).
		int arrayPos = 0;
		if (listViewPos == 0) {
			arrayPos = 33;
		} else {
			arrayPos = listViewPos + 64;
		}
		String startLetter = String.valueOf(((char) arrayPos));

		ArrayList<HashMap<String, String>> tempList = new ArrayList<HashMap<String, String>>();
		Cursor cur = VibeVault.db.getArtist(startLetter);
		cur.moveToFirst();
		while (!cur.isAfterLast()) {
			String artistName = cur.getString(cur.getColumnIndex("artistName"));
			String numShows = cur.getString(cur.getColumnIndex("numShows"));
			HashMap<String, String> tempMap = new HashMap<String, String>();
			tempMap.put("artist", artistName);
			tempMap.put("shows", numShows);
			tempList.add(tempMap);
			cur.moveToNext();
		}
		cur.close();
		alphaArtistsList.add(listViewPos, tempList);
	}
	
	private void checkFetchedStatus() {
		GregorianCalendar lastFetched = new GregorianCalendar();
		GregorianCalendar now = new GregorianCalendar();

		try{
		lastFetched.setTime(Date.valueOf(VibeVault.db.getPref("artistUpdate")));
		} catch (IllegalArgumentException e){
			goodFetchedDate = false;
			return;
		}
		long dayDifference = (now.getTimeInMillis() - lastFetched
				.getTimeInMillis())
				/ (24 * 60 * 60 * 1000);
		if (dayDifference > 7) {
			goodFetchedDate = false;
		} else {
			goodFetchedDate = true;
			finished = true;
		}
	}

	private ArrayList<HashMap<String, String>> createGroupList() {
		ArrayList<HashMap<String, String>> result = new ArrayList<HashMap<String, String>>();
		for (int i = 0; i < symbols.length; ++i) {
			HashMap<String, String> m = new HashMap<String, String>();
			m.put("symbol", symbols[i]);
			result.add(m);
		}
		return result;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.help_options, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected (MenuItem item){
		switch (item.getItemId()){
			case R.id.scrollableDialog:
				AlertDialog.Builder ad = new AlertDialog.Builder(this);
				ad.setTitle("Help!");
				View v = LayoutInflater.from(this).inflate(R.layout.scrollable_dialog, null);
				((TextView) v.findViewById(R.id.DialogText)).setText(R.string.browse_artists_screen_help);
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
	public void onResume() {
		super.onResume();
		this.registerReceiver(updateReceiver, new IntentFilter("BROWSEUPDATE"));
	}

	@Override
	public void onPause() {
		super.onPause();
		unregisterReceiver(updateReceiver);
		}
	
	private class ParserUpdateReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String duration = intent.getStringExtra("progress");
			statusTextView.setText(duration);
			
			if(finished || duration.equals("All done.")){
				statusTextView.setVisibility(View.GONE);
				statusProgressBar.setVisibility(View.GONE);
				expandableList.setEnabled(true);
			}
		}
	}

	public class ParseArtistsPageTask extends
			AsyncTask<Void, String, Boolean> {

		private ArrayList<ArrayList<String>> artists = new ArrayList<ArrayList<String>>();
		private int numArtists;

		@Override
		protected void onPreExecute() {
			publishProgress("Fetching artists list...");
		}

		// Update the date in the calendar, set variables and send an Intent.
		// so that the BrowseArtistsScreen knows it is done
		@Override
		protected void onPostExecute(Boolean b) {
			if(b==true){
			finished = true;
			isDownloading = false;
			GregorianCalendar cal = new GregorianCalendar();
			Date d = new Date(cal.get(GregorianCalendar.YEAR), cal
					.get(GregorianCalendar.MONTH), cal
					.get(GregorianCalendar.DATE));
			VibeVault.db.updatePref("artistUpdate", d.toString());
			goodFetchedDate = true;
			expandableList.setBackgroundColor(Color.BLACK);
			Intent intent = new Intent("BROWSEUPDATE");
			intent.putExtra("progress","All done.");
			sendBroadcast(intent);
			} else{
				finished = false;
				isDownloading = false;
				goodFetchedDate = false;
			}
		}

		@Override
		protected void onProgressUpdate(String... values) {
			Intent intent = new Intent("BROWSEUPDATE");
			intent.putExtra("progress",values[0]);
			sendBroadcast(intent);
		}

		@Override
		protected Boolean doInBackground(Void... v) {

			long startTime = 0;
			long midTime = 0;
			long endTime = 0;
			HtmlCleaner pageParser = new HtmlCleaner();
			CleanerProperties props = pageParser.getProperties();
			props.setAllowHtmlInsideAttributes(true);
			props.setAllowMultiWordAttributes(true);
			props.setRecognizeUnicodeChars(true);
			props.setOmitComments(true);

			String artistsXPath = "//tr[@valign='top']//li";
			startTime = SystemClock.elapsedRealtime();
			URL artistsPageURL;
			try {
				artistsPageURL = new URL("http://www.archive.org/browse.php?field=/metadata/bandWithMP3s&collection=etree");
				URLConnection conn = artistsPageURL.openConnection();
				conn.setReadTimeout(100000);
				conn.setConnectTimeout(100000);
				InputStreamReader is = new InputStreamReader(conn.getInputStream());
				TagNode node = pageParser.clean(is);
				is.close();

				Object[] artistsNodes = node.evaluateXPath(artistsXPath);

				numArtists = artistsNodes.length;
				publishProgress("Parsing " + numArtists + "...");

				for (int i = 0; i < numArtists; i++) {
					if (i % 10 == 0) {
						publishProgress("Parsed " + i + " / " + numArtists
								+ " artists...");
					}
					// Cast the artistNode as a TagNode.
					TagNode artist = ((TagNode) artistsNodes[i]);
					// Grab the first child node, which is the link to the
					// artist's page.
					// The inner HTML of this node will be the title.
					TagNode artistTitleSubNode = artist.getChildTags()[0];
					// Remove the child node, so that the inner HTML of the
					// artistNode
					// only contains the number of shows that the artist has.
					artist.removeChild(artistTitleSubNode);
					String artistTitle = pageParser.getInnerHtml(artistTitleSubNode);

					if (artistTitle != null) {
						ArrayList<String> artistPair = new ArrayList<String>();
						artistPair.add(artistTitle.replace("&apos;", "'").replace("&gt;", ">").replace("&lt;", "<").replace("&quot;", "\"").replace("&amp;","&"));
						artistPair.add(pageParser.getInnerHtml(artist).trim());
						/*
						 * VibeVault.db.addArtist(artistTitle, pageParser
						 * .getInnerHtml(artist).trim());
						 */
						artists.add(artistPair);
					}
					midTime = SystemClock.elapsedRealtime();

				}
				if (artists.size() > 0) {
					VibeVault.db.insertArtistBulk(artists);
				}
				endTime = SystemClock.elapsedRealtime();
			} catch(UnknownHostException e){
				publishProgress("Can't resolve archive.org...\nCheck your internet connection.");
				return false;
			}
			catch (XPatherException e) {
				e.printStackTrace();
				return false;
			} catch (MalformedURLException e1) {
				e1.printStackTrace();
				return false;
			} catch(SocketTimeoutException e){
				publishProgress("Can't resolve archive.org...\nCheck your internet connection.");
				return false;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			} catch(Exception e) {
				e.printStackTrace();
				return false;
			} 
			Log.d(LOG_TAG, "ARTIST PARSE: Array build " + (midTime - startTime)
					+ "msec");
			Log.d(LOG_TAG, "ARTIST PARSE: DB bulk insert "
					+ (endTime - midTime) + "msec");
			publishProgress("Parsing finished.");
			return true;

		}

	}
	
	/** Overriden BaseExpanableListAdapter.
	 * 
	 * Gives more control over the appearance of the ExpandableListView,
	 * and allows us to add more functionality down the road if we want to.
	 */
	private class expandableAdapter extends BaseExpandableListAdapter{
		
		// Make sure to close any other expanded groups because if you have multiple
		// groups open and scroll fast, we get a weird index out of bounds exception.
		@Override
	    public void onGroupExpanded(int groupPosition){
	        if(groupPosition != lastGroupPos){
	            expandableList.collapseGroup(lastGroupPos);
	        }
	        super.onGroupExpanded(groupPosition);           
	        lastGroupPos = groupPosition;
	    }
		
		@Override
		public Object getChild(int groupPos, int childPos) {
			return alphaArtistsList.get(groupPos).get(childPos);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return 0;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
			if (convertView == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = vi.inflate(R.layout.row_expandable_child, null);
			}
			TextView artistText = (TextView) convertView.findViewById(R.id.childname);			
			String artist = alphaArtistsList.get(groupPosition).get(childPosition).get("artist");
			String numShows = alphaArtistsList.get(groupPosition).get(childPosition).get("shows");
			String formattedText = artist + " -- " + numShows;
			artistText.setText(formattedText);
			artistText.setSelected(true);
			return convertView;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return(alphaArtistsList.get(groupPosition).size());
		}

		@Override
		public Object getGroup(int groupPosition) {
			return alphaArtistsList.get(groupPosition);
		}

		@Override
		public int getGroupCount() {
			return symbols.length;
		}

		@Override
		public long getGroupId(int groupPosition) {
			return 0;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {
			if (convertView == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = vi.inflate(R.layout.row_expandable_parent, null);
			}
			TextView parentText = (TextView) convertView.findViewById(R.id.childname);
			parentText.setText(symbols[groupPosition]);
			return convertView;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}
		
	}

}