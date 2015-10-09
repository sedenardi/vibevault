package com.code.android.vibevault;

import java.util.ArrayList;

import com.code.android.vibevault.SearchFragment.SearchActionListener;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Loader;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class ShowsDownloadedFragment extends Fragment implements LoaderManager.LoaderCallbacks<ArrayList<ArchiveShowObj>>, OnItemClickListener, ActionBar.OnNavigationListener {

	private DialogAndNavigationListener dialogAndNavigationListener;
	
	private ListView downloadedList;
		
	private StaticDataStore db;
	
	private SearchActionListener searchActionListener;
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try{
			dialogAndNavigationListener = (DialogAndNavigationListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement DialogListener");
		}
		try {
			searchActionListener = (SearchActionListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement ActionListener");
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		this.setRetainInstance(false);
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
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		View v = inflater.inflate(R.layout.downloaded_shows_fragment, container, false);
		downloadedList = (ListView) v.findViewById(R.id.DownloadedListView);
		downloadedList.setOnItemClickListener(this);
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		setHasOptionsMenu(true);
		getActivity().getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        getActivity().getActionBar().setListNavigationCallbacks(null, null);
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
		getActivity().getActionBar().setTitle("Downloaded");
		
		LoaderManager lm = this.getLoaderManager();
		lm.initLoader(1, null, this);
		 
	}
	
	@Override
	public boolean onOptionsItemSelected (MenuItem item){
		switch (item.getItemId()){
			case R.id.HelpButton:
				dialogAndNavigationListener.showDialog(this.getResources().getString(R.string.downloaded_show_screen_help), "Help");
				break;
			case android.R.id.home:
				dialogAndNavigationListener.goHome();
				break;
			default:
	            return super.onOptionsItemSelected(item);
		}
		return true;
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		menu.clear();
		inflater.inflate(R.menu.help, menu);
	    super.onCreateOptionsMenu(menu, inflater);
	}
	
	
	@Override
	public Loader<ArrayList<ArchiveShowObj>> onCreateLoader(int id, Bundle args) {
		 
		return new ShowsDownloadedAsyncTaskLoader(getActivity());
	}

	@Override
	public void onLoadFinished(Loader<ArrayList<ArchiveShowObj>> arg0,
			ArrayList<ArchiveShowObj> arg1) {
		 
		ScrollingShowAdapter showAdapter = new ScrollingShowAdapter(getActivity(), R.id.DownloadedListView, arg1, db);
		downloadedList.setAdapter(showAdapter);
		
	}

	@Override
	public void onLoaderReset(Loader<ArrayList<ArchiveShowObj>> arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		
		searchActionListener.onShowSelected((ArchiveShowObj)arg0.getAdapter().getItem(arg2));
	}

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		// TODO Auto-generated method stub
		return false;
	}

}
