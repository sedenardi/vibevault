package com.code.android.vibevault;

import java.util.ArrayList;

import com.code.android.vibevault.BrowseArtistsFragment.BrowseActionListener;
import com.code.android.vibevault.SearchFragment.SearchActionListener;
import com.code.android.vibevault.SearchSettingsDialogFragment.SearchSettingsDialogInterface;
import com.code.android.vibevault.ShowDetailsFragment.ShowDetailsActionListener;
import com.code.android.vibevault.VotesFragment.VotesActionListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class SearchScreen extends Activity implements SearchActionListener, DialogAndNavigationListener, SearchSettingsDialogInterface, ShowDetailsActionListener, NowPlayingFragment.PlayerListener, BrowseActionListener, VotesActionListener {

	private static final String LOG_TAG = SearchScreen.class.getName();
	
	@Override
	protected void onResume(){
		super.onResume();
		if(this.getIntent()!=null&&this.getIntent().hasExtra("type")){
			 
		}

//		NowPlayingFragment nowPlayingFrag = (NowPlayingFragment) getFragmentManager().findFragmentByTag("nowplaying");
//		if(nowPlayingFrag==null){
//			getFragmentManager().
//		}
	}
	
	private void clearBackStack(){
		for(int i = 0; i<this.getFragmentManager().getBackStackEntryCount(); ++i){
			this.getFragmentManager().popBackStack();
		}
	}
	
	@Override
	public void onBackPressed(){
		super.onBackPressed();
		if(this.getFragmentManager().getBackStackEntryCount()==0){
			this.finish();
		}
	}
	
	@Override
	protected void onNewIntent(Intent i){
		super.onNewIntent(i);
		this.setIntent(i);
		if(i!=null){
			this.setIntent(i);
			if(i.hasExtra("type")){
				int type = i.getExtras().getInt("type");
				if(type!=2){
					this.clearBackStack();
				}
				instantiateFragment(i.getExtras().getInt("type"));
			}
		}
	}
	
	private void instantiateFragment(int type){
		switch(type){
	    	case 0:
	    		this.instantiateSearchFragmentForActivity(null);
	    		break;
	    	case 1:
	    		this.instantiateShowDetailsFragmentForActivity(null);
	    		break;
	    	case 2:
	    		this.instantiateNowPlayingFragmentForActivity(-1, null);
	    		break;
	    	case 3:
	    		this.instantiateShowsStoredFragmentForActivity();
	    		break;
	    	case 4:
	    		this.instantiateVotesFragmentForActivity(null);
	    		break;
	    	case 5:
	    		this.instantiateBrowseArtistsFragmentForActivity();
	    		break;
	    	case 6:
	    		this.instantiateShowsDownloadedFramentForActivity();
	    		break;
	    	default:
		}
		 
	}
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // See what type of Fragment we want to launch.
        int type = this.getIntent().getExtras().getInt("type");
        
		// See if this Screen was spawned by the user clicking on a link.
		// If so, form an ArchiveShowObj from the URL.
		if (this.getIntent().getScheme()!=null&&this.getIntent().getScheme().equals("http")) {
	        ArchiveShowObj show = null;
			 
			type = 1;
			String linkString =  this.getIntent().getData().toString();
			 
			if (linkString.contains("/download/")) {
				String[] paths = linkString.split("/");
				for (int i = 0; i < paths.length; i++) {
					if (paths[i].equals("download")) {
						show = new ArchiveShowObj(new String("http://www.archive.org/details/" + paths[i + 1]), true);
						show.setSelectedSong(linkString);
					}
				}
			// Show link clicked on (not an individual song link).
			} else {
				show = new ArchiveShowObj(linkString, false);
			}
			this.instantiateShowDetailsFragmentForActivity(show);
		} else if(savedInstanceState==null){
        instantiateFragment(type);
		}
    }
	/**
	 * The instantiate___Activity() methods could be combined into one larger method,
	 * but I am keeping it like this for now in case we want to customize the actions
	 * performed when certain Fragments are opened, and for debugging purposes.  Making
	 * this one large method would save some lines of code, but be harder to read, in my opinion.
	 * @param artist 
	 */
	
	private void instantiateSearchFragmentForActivity(String artist){
		FragmentManager fm = getFragmentManager();
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		SearchFragment frag = (SearchFragment) fm.findFragmentByTag("searchfrag");
		
		 
		Bundle b = this.getIntent().getExtras();
		if(artist!=null){
			b = new Bundle();
			b.putString("Artist", artist);
		}
		if(frag==null){
			frag = new SearchFragment();
			frag.setArguments(b);
			ft.replace(android.R.id.content, frag,"searchfrag");
			ft.addToBackStack(null);
		} else{
			frag.getArguments().putAll(b);
			ft.replace(android.R.id.content, frag,"searchfrag");
			if(fm.getBackStackEntryCount()==0){
				ft.addToBackStack(null);
			}
		}
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
	    ft.commit();
	}
	
	// This can take a null show object.
	private void instantiateShowDetailsFragmentForActivity(ArchiveShowObj show){
		FragmentManager fm = getFragmentManager();
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		ShowDetailsFragment frag = (ShowDetailsFragment) fm.findFragmentByTag("showdetails");
		if(frag==null){
			frag = new ShowDetailsFragment();
			if(show!=null){
    			Bundle b = new Bundle();
    			b.putSerializable("show", show);
    			frag.setArguments(b);
			}
			if(!frag.isAdded()){
				ft.replace(android.R.id.content, frag,"showdetails");
				ft.addToBackStack(null);
			}
			
		} else{
			if(show!=null){
    			Bundle b = new Bundle();
    			b.putSerializable("show", show);
    			if(frag.getArguments()!=null){
    				frag.getArguments().putAll(b);
    			} else{
    				frag.setArguments(b);
    			}
    		} else{
    			frag.getArguments().putAll(this.getIntent().getExtras());
    		}
			if(!frag.isAdded()){
				ft.replace(android.R.id.content, frag,"showdetails");
			}
			if(fm.getBackStackEntryCount()==0){
				ft.addToBackStack(null);
			}
		}
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
	    ft.commit();
	     
	}
	
	private void instantiateNowPlayingFragmentForActivity(int pos, ArrayList<ArchiveSongObj> showSongs){
		FragmentManager fm = getFragmentManager();
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		NowPlayingFragment frag = (NowPlayingFragment) fm.findFragmentByTag("nowplaying");
		
		Bundle b = this.getIntent().getExtras();
		if(pos >=0 && showSongs != null){
			b = new Bundle();
			b.putSerializable("position", pos);
			b.putSerializable("showsongs", showSongs);
			 
		}
		
		if(frag==null){
			 
			frag = new NowPlayingFragment();
			frag.setArguments(b);
			ft.replace(android.R.id.content, frag,"nowplaying");
			ft.addToBackStack(null);
		} else{
			 
			frag.getArguments().putAll(b);
			ft.replace(android.R.id.content, frag,"nowplaying");
			if(fm.getBackStackEntryCount()==0){
				ft.addToBackStack(null);
			}
		}
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
	    ft.commit();

	}
	
	private void instantiateShowsStoredFragmentForActivity(){
		FragmentManager fm = getFragmentManager();
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		ShowsStoredFragment frag = (ShowsStoredFragment) fm.findFragmentByTag("showsstored");
		if(frag==null){
			frag = new ShowsStoredFragment();
			frag.setArguments(this.getIntent().getExtras());
			ft.replace(android.R.id.content, frag,"showsstored");
			ft.addToBackStack(null);
		} else{
			frag.getArguments().putAll(this.getIntent().getExtras());
			ft.replace(android.R.id.content, frag,"showsstored");
			if(fm.getBackStackEntryCount()==0){
				ft.addToBackStack(null);
			}
		}
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
	    ft.commit();

	}
	
	private void instantiateBrowseArtistsFragmentForActivity(){
		FragmentManager fm = getFragmentManager();
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		BrowseArtistsFragment frag = (BrowseArtistsFragment) fm.findFragmentByTag("browsefrag");
		if(frag==null){
			frag = new BrowseArtistsFragment();
			frag.setArguments(this.getIntent().getExtras());
			ft.replace(android.R.id.content, frag,"browsefrag");
			ft.addToBackStack(null);
		} else{
			frag.getArguments().putAll(this.getIntent().getExtras());
			ft.replace(android.R.id.content, frag,"browsefrag");
			if(fm.getBackStackEntryCount()==0){
				ft.addToBackStack(null);
			}
		}
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
	    ft.commit();

	}
	
	private void instantiateVotesFragmentForActivity(ArchiveArtistObj artist){
		FragmentManager fm = getFragmentManager();
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		VotesFragment frag = (VotesFragment) fm.findFragmentByTag("votesfrag");
		
		Bundle b = this.getIntent().getExtras();
		// This means that we are opening up a VotingFragment with
		// an artist and not a show.  Ths requires creating a new VotesFragment.
		if(artist!=null){
			b = new Bundle();
			b.putSerializable("ArchiveArtist", artist);
		}
		if(frag==null || artist != null){
			frag = new VotesFragment();
			frag.setArguments(b);
			ft.replace(android.R.id.content, frag,"votesfrag");
			ft.addToBackStack(null);
		} else{
			frag.getArguments().putAll(b);
			ft.replace(android.R.id.content, frag,"votesfrag");
			if(fm.getBackStackEntryCount()==0){
				ft.addToBackStack(null);
			}
		}
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
	    ft.commit();

	}
	
	private void instantiateShowsDownloadedFramentForActivity(){
		FragmentManager fm = getFragmentManager();
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		ShowsDownloadedFragment frag = (ShowsDownloadedFragment) fm.findFragmentByTag("downloadfrag");
		if(frag==null){
			frag = new ShowsDownloadedFragment();
			ft.replace(android.R.id.content, frag,"downloadfrag");
			ft.addToBackStack(null);
		} else {
			ft.replace(android.R.id.content, frag,"downloadfrag");
			if(fm.getBackStackEntryCount()==0){
				ft.addToBackStack(null);
			}
		}
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
	    ft.commit();

	}
	
	@Override
	public void onShowSelected(ArchiveShowObj show) {
		 
		 
		this.instantiateShowDetailsFragmentForActivity(show);
		 
	}
	
	@Override
	public void showLoadingDialog (String message) {
		// DialogFragment.show() will take care of adding the fragment
		// in a transaction. We also want to remove any currently showing
		// dialog, so make our own transaction and take care of that here.
		// Note that if there was a previous dialog, it might still be
		// being removed from the Activity, in which case we don't try
		// to remove it again, because we would get an error.
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		Fragment prev = getFragmentManager().findFragmentByTag("dialog");
		if (prev != null) {
			if (prev.isRemoving()) {
			} else {
				ft.remove(prev);
			}
		}
		// Create and show the dialog.
		DialogFragment newFragment = LoadingDialog.newInstance(message);
		newFragment.show(ft, "dialog");
	}
	
	@Override
	public void showSettingsDialog(Bundle b) {
		SearchSettingsDialogFragment settingsFrag = SearchSettingsDialogFragment.newInstanceSearchSettingsDialogFragment(b.getString("type"), b.getInt("number"), b.getInt("date"), b.getInt("datepos"));
		// DialogFragment.show() will take care of adding the fragment
		// in a transaction. We also want to remove any currently showing
		// dialog, so make our own transaction and take care of that here.
		// Note that if there was a previous dialog, it might still be
		// being removed from the Activity, in which case we don't try
		// to remove it again, because we would get an error.
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		Fragment prev = getFragmentManager().findFragmentByTag("dialog");
		if (prev != null) {
			if (prev.isRemoving()) {
			} else {
				ft.remove(prev);
			}
		}
		// Create and show the dialog.
		settingsFrag.show(ft, "dialog");
	}
	
	@Override
	public void showDialog(String message, String title){
		// DialogFragment.show() will take care of adding the fragment
		// in a transaction. We also want to remove any currently showing
		// dialog, so make our own transaction and take care of that here.
		// Note that if there was a previous dialog, it might still be
		// being removed from the Activity, in which case we don't try
		// to remove it again, because we would get an error.
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		Fragment prev = getFragmentManager().findFragmentByTag("dialog");
		if (prev != null) {
			if (prev.isRemoving()) {
			} else {
				ft.remove(prev);
			}
		}
		// Create and show the dialog.
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(message).setTitle(title).setPositiveButton("Okay", new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	        	   
	           }
	       });
		builder.setMessage(message).setTitle(title).setNeutralButton("Donate!", new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	        	   Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=vibevault%40gmail%2ecom&lc=US&item_name=Vibe%20Vault&currency_code=USD&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHosted"));
	        	   startActivity(browserIntent);	           }
	       });
		builder.create().show();
		ft.commit();
	}

	@Override
	public void hideDialog() {
		// If a dialog currently exists, remove it, unless it is currently being removed.
		// It would be nice to be able to call dismiss but Android will crash if you commit
		// (which calling dismiss will do) without allowing state loss.  See this post:
		// https://groups.google.com/forum/#!topic/android-developers/dXZZjhRjkMk/discussion
	    FragmentTransaction ft = getFragmentManager().beginTransaction();
		LoadingDialog prev = (LoadingDialog)getFragmentManager().findFragmentByTag("dialog");
	    if (prev != null) {
	    	if(prev.isRemoving()){
	    	} else{
	    		ft.remove(prev);
	    	}
	    }
	    ft.commitAllowingStateLoss();
	}

	@Override
	public void goHome() {
		Intent i = new Intent(SearchScreen.this, HomeScreen.class);
		startActivity(i);
		this.finish();
	}

	@Override
	public void onSettingsOkayButtonPressed(String searchType, int numResults, int dateResults, int dateTypePos) {
		SearchFragment searchFrag = (SearchFragment)this.getFragmentManager().findFragmentByTag("searchfrag");
		if(searchFrag!=null){
			searchFrag.onSettingsOkayButtonPressed(searchType, numResults, dateResults, dateTypePos);
		}
	}

	@Override
	public void playShow(int pos, ArrayList<ArchiveSongObj> showSongs) {
		
		this.instantiateNowPlayingFragmentForActivity(pos, showSongs);
	}

	@Override
	public void registerReceivers(BroadcastReceiver playerChangedBroadcast, BroadcastReceiver playlistChangedBroadcast) {
		registerReceiver(playerChangedBroadcast, new IntentFilter(PlaybackService.SERVICE_UPDATE));
		registerReceiver(playlistChangedBroadcast, new IntentFilter(PlaybackService.SERVICE_PLAYLIST));
	}

	@Override
	public void unregisterReceivers(BroadcastReceiver playerChangedBroadcast, BroadcastReceiver playlistChangedBroadcast) {
		unregisterReceiver(playerChangedBroadcast);
		unregisterReceiver(playlistChangedBroadcast);
	}
	
	@Override
	public void browse(String artist) {
		this.instantiateSearchFragmentForActivity(artist);
	}
	
	@Override
	public void openArtistShowList(ArchiveArtistObj artist) {
		this.instantiateVotesFragmentForActivity(artist);
	}




	
}
