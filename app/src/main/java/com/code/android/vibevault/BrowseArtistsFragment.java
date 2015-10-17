package com.code.android.vibevault;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;

public class BrowseArtistsFragment extends Fragment{
	
	//private static final String LOG_TAG = BrowseArtistsFragment.class.getName();

	private ExpandableListView expandableList;

	private int lastGroupPos = 0;
	
	private StaticDataStore db = null;

	private ArrayList<ArrayList<HashMap<String, String>>> alphaArtistsList;

	private final String[] symbols = { "1,2,3,#,!,etc...", "A", "B", "C", "D", "E", "F", "G",
			"H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T",
			"U", "V", "W", "X", "Y", "Z" };
	
	private BrowseActionListener browseActionListener;
	
	private DialogAndNavigationListener dialogAndNavigationListener;
	
	public interface BrowseActionListener{
		public void browse(String artist);
	}	
	
	// Called right before onCreate(), which is right before onCreateView().
	// http://developer.android.com/guide/topics/fundamentals/fragments.html#Lifecycle
	@Override
	public void onAttach(Activity activity) {
		
		super.onAttach(activity);
		try{
			browseActionListener = (BrowseActionListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement BrowseActionListener");
		} try{
			dialogAndNavigationListener = (DialogAndNavigationListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement DialogListener");
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		// Control whether a fragment instance is retained across Activity re-creation (such as from a configuration change).
		this.setRetainInstance(true);		
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
//		if(getActivity().getFragmentManager().getBackStackEntryCount()<1){
//			getActivity().finish();
//		}
	}
	
	@Override
	public void onStart() {
		super.onStart();
		db = StaticDataStore.getInstance(getActivity());
	}
	
	@Override
	public void onStop() {
		super.onStop();
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		initAlphaList();

		// Inflate the fragment and grab a reference to it.
		View v = inflater.inflate(R.layout.browse_artists_fragment, container, false);
		// Initialize the ExpandableListView
		expandableList = (ExpandableListView) v.findViewById(R.id.BrowseArtistsExpandableListView);
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
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
				browseActionListener.browse(alphaArtistsList.get(groupPosition).get(childPosition).get("artist"));
				return false;
			}
		});
		return v;
	}
	
	// This method is called right after onCreateView() is called.  "Called when the
	// fragment's activity has been created and this fragment's view hierarchy instantiated."
	// http://developer.android.com/guide/topics/fundamentals/fragments.html#Lifecycle
	@Override
	public void onActivityCreated(Bundle savedInstanceState){
	    super.onActivityCreated(savedInstanceState);
		// Must call in order to get callback to onOptionsItemSelected()
		// and thereby create an ActionBar.
	    setHasOptionsMenu(true);
	    getActivity().getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
	    getActivity().getActionBar().setListNavigationCallbacks(null, null);
	    getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
	    getActivity().getActionBar().setTitle("Browse");
	}
	
	// This is what the ActionBar is created from.
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		menu.clear();
		inflater.inflate(R.menu.help, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	// Set ActionBar actions.
	@Override
	public boolean onOptionsItemSelected (MenuItem item){
		switch (item.getItemId()){
			case android.R.id.home:
				dialogAndNavigationListener.goHome();
				break;
			case R.id.HelpButton:
				dialogAndNavigationListener.showDialog(this.getResources().getString(R.string.browse_artists_screen_help), "Help");
				break;
			default:
	            return super.onOptionsItemSelected(item);
		}
		return true;
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
		ArrayList<HashMap<String, String>> tempList = db.getArtist(startLetter);
		alphaArtistsList.add(listViewPos, tempList);
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
				LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
				LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
