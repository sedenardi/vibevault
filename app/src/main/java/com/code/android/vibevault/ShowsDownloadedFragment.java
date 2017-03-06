package com.code.android.vibevault;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.code.android.vibevault.SearchFragment.SearchActionListener;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Loader;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class ShowsDownloadedFragment extends Fragment implements LoaderManager.LoaderCallbacks<ArrayList<ArchiveShowObj>>, OnItemClickListener, ActionBar.OnNavigationListener {
	
	private static final String LOG_TAG = ShowsDownloadedFragment.class.getName();

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
		Logging.Log(LOG_TAG, "Activity Created");
	}
	
	@Override
	public boolean onOptionsItemSelected (MenuItem item){
		switch (item.getItemId()){
			case R.id.scrollableDialog:
				dialogAndNavigationListener.showDialog(this.getResources().getString(R.string.downloaded_show_screen_help), "Help");
				break;
			case R.id.SyncFolder:
				DirectorySyncTask t = new DirectorySyncTask(this);
				t.execute("");
				break;
			case R.id.ChangeFolder:
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setTitle("Enter New Folder Name");
				final EditText input = new EditText(getActivity());
				input.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
				String path = Downloading.getAppDirectory(db);
				input.setText(path.substring(1, (path.length() - 1)));
				builder.setView(input);
				builder.setPositiveButton("OK", null); //set listener below
				builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();						
					}
				});
				final AlertDialog dialog = builder.create();
				dialog.show();
				dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						((InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(input.getWindowToken(), 0);
						String inputText = input.getText().toString();
						String newPath = "/" + inputText + "/";
						String rawText = inputText.replace("/", "");
						Pattern pattern = Pattern.compile("[A-Za-z0-9]+(?:[\\s-][A-Za-z0-9]+)*");
						Matcher matcher = pattern.matcher(rawText);
						String error = "";
						if (rawText.equalsIgnoreCase(Downloading.getAppDirectory(db).replace("/", ""))) {
							error = getString(R.string.error_directory_name_match_message_text);
						} else if (inputText.charAt(0) == '/' || inputText.charAt(inputText.length()-1) == '/') {
							error = getString(R.string.error_directory_name_slash_message_text);
						} else if (rawText.length() == 0) {
							error = getString(R.string.error_directory_name_blank_message_text);
						} else if (rawText.length() > 32) {
							error = getString(R.string.error_directory_name_length_message_text);						
						} else if (!matcher.matches()) {
							error = getString(R.string.error_directory_name_characters_message_text);					
						}
						if (error.equals("")) {
							((InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(input.getWindowToken(), 0);
							dialog.dismiss();
							ChangeDirectoryTask c = new ChangeDirectoryTask(getActivity());
							c.execute(newPath);
						} else {
							((InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(input, 0);
							Toast.makeText(getActivity(), error, Toast.LENGTH_LONG).show();
						}
					}					
				});
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
		inflater.inflate(R.menu.help_sync_folder, menu);
	    super.onCreateOptionsMenu(menu, inflater);
	}
	
	
	@Override
	public Loader<ArrayList<ArchiveShowObj>> onCreateLoader(int id, Bundle args) {
		Logging.Log(LOG_TAG, "Created Loader");
		return new ShowsDownloadedAsyncTaskLoader(getActivity());
	}

	@Override
	public void onLoadFinished(Loader<ArrayList<ArchiveShowObj>> arg0,
			ArrayList<ArchiveShowObj> arg1) {
		Logging.Log(LOG_TAG, "Loader Finished");
		ScrollingShowAdapter showAdapter = new ScrollingShowAdapter(getActivity(), 
				R.id.DownloadedListView, arg1, db, ScrollingShowAdapter.MENU_DOWNLOAD);
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
	
	private class DirectorySyncTask extends AsyncTask<String, Void, Void> {
		
		private ShowsDownloadedFragment parentScreen;
		
		public DirectorySyncTask(ShowsDownloadedFragment frag) {
			parentScreen = frag;
		}
		
		@Override
		protected Void doInBackground(String... showFields) {
			Downloading.syncFilesDirectory(getActivity().getBaseContext(), db);
			return null;
		}

		@Override
		protected void onPostExecute(Void v) {
			LoaderManager lm = parentScreen.getLoaderManager();
			lm.restartLoader(1, null, parentScreen);
		}
	}
	
	private class ChangeDirectoryTask extends AsyncTask<String, Void, Boolean> {
		
		Context ctx;
		
		public ChangeDirectoryTask(Context context) {
			ctx = context;
		}
		
		@Override
		protected void onPreExecute(){
			dialogAndNavigationListener.showLoadingDialog(getString(R.string.dialog_moving), false);
		}
		
		@Override
		protected Boolean doInBackground(String... newFolder) {
			String oldFolder = Downloading.getAppDirectory(db);
			boolean success = Downloading.changeDownloadFolder(ctx, oldFolder, newFolder[0]);
			if (success) {
				db.updatePref("downloadPath", newFolder[0]);
				Downloading.syncFilesDirectory(getActivity(), db);
				Downloading.deleteFileOrDirectory(new File(Environment.getExternalStorageDirectory(),oldFolder));
			}
			else {
				Downloading.changeDownloadFolder(ctx, newFolder[0], oldFolder);
			}
			return success;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			dialogAndNavigationListener.hideDialog();
			
			CharSequence res = result ? getActivity().getApplicationContext().getResources().getText(R.string.directory_copy_message_text_success) : 
				getActivity().getApplicationContext().getResources().getText(R.string.directory_copy_message_text_failure);
			Toast.makeText(getActivity().getBaseContext(), res, Toast.LENGTH_LONG).show();
		}
	}

}
