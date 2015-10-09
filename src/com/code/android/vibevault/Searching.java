package com.code.android.vibevault;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.ContentNode;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Log;

public class Searching {
	
	
	private static final String LOG_TAG = Searching.class.getName();
	
	private static final String SEARCHING_PREFIX = "com.code.android.vibevault.searching.";
	public static final String SEARCHING_UPDATE = SEARCHING_PREFIX + "SEARCHING_UPDATE";
	
	public static final String EXTRA_STATUS = SEARCHING_PREFIX + "EXTRA_STATUS";
	public static final String EXTRA_TOTAL = SEARCHING_PREFIX + "EXTRA_TOTAL";
	public static final String EXTRA_COMPLETED = SEARCHING_PREFIX + "EXTRA_COMPLETED";
	
	public static final int STATUS_DOWNLOADING = 0;
	public static final int STATUS_INSERTING = 1;
	public static final int STATUS_COMPLETED = 2;
	public static final int STATUS_ERROR = 3;

	public static String makeSearchURLString(int pageNum, int yearSearchInt, String artistSearchText, int numSearchResults, String sortResults, int dateType){
		int numResults = numSearchResults;
		String sortPref = sortResults;
		if(sortPref.equalsIgnoreCase("Date")){
			sortPref = "date+desc";
		} else if(sortPref.equalsIgnoreCase("Rating")){
			sortPref= "avg_rating+desc";
		}
		String queryString = null;
		
		try {
			String dateModifier = "";
			// FIXME
				switch(dateType){
					case SearchSettingsDialogFragment.ANYTIME:
							 
							break;
					case SearchSettingsDialogFragment.BEFORE:	//Before
						 
						dateModifier = "date:[1800-01-01%20TO%20" + yearSearchInt + "-01-01]%20AND%20";
						break;
					case SearchSettingsDialogFragment.AFTER:	//After
						int curDate = Calendar.getInstance().get(Calendar.DATE);
						int curMonth = Calendar.getInstance().get(Calendar.MONTH);
						int curYear = Calendar.getInstance().get(Calendar.YEAR);
						dateModifier = "date:[" + yearSearchInt + "-01-01%20TO%20" + curYear + "-" + String.format("%02d",curMonth) + "-" + String.format("%02d",curDate) + "]%20AND%20";
						break;
					case SearchSettingsDialogFragment.DURING:	// In Year.
						dateModifier = "date:[" + yearSearchInt + "-01-01%20TO%20" + yearSearchInt + "-12-31]%20AND%20";
						break;
					}
			// We search creator:(random's artist)%20OR%20creator(randoms artist) because
			// archive.org does not like apostrophes in the creator query.
			String specificSearch = "";
//			if(searchType.equals("Artist")){
				specificSearch = "(creator:(" + URLEncoder.encode(artistSearchText,"UTF-8") + ")" + "%20OR%20creator:(" + URLEncoder.encode(artistSearchText.replace("'", "").replace("\"", ""),"UTF-8") + "))";
//			} else if(searchType.equals("Show/Artist Description")){
//				specificSearch = "(creator:(" + URLEncoder.encode(artistSearchText,"UTF-8") + ")" + "%20OR%20description:(" + URLEncoder.encode(artistSearchText.replace("'", "").replace("\"", ""),"UTF-8") + "))";
//			}
			String mediaType = "mediatype:(etree)";	
			
			queryString = "http://www.archive.org/advancedsearch.php?q="
			+ "(" + dateModifier + mediaType + "%20AND%20format:(mp3)" +  "%20AND%20(" + specificSearch + "))"
			+ "&fl[]=date&fl[]=avg_rating&fl[]=source&fl[]=format&fl[]=identifier&fl[]=mediatype&fl[]=title&sort[]="
			+ sortPref + "&sort[]=&sort[]=&rows="
			+ String.valueOf(numResults) + "&page=" + String.valueOf(pageNum) + "&output=json&callback=callback&save=yes";
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		 
		return queryString;
	}
	
	public static void getShows(String query, ArrayList<ArchiveShowObj> searchResults){
		
		String queryResult = "";

		/* Open up an HTTP connection with the archive.org query. Grab an
		 * input stream or bytes and turn it into a string. It will be of
		 * the form described in JSONQueryExample.txt. We use a
		 * BufferedInputStream because its read() call grabs many bytes at
		 * once (behind the scenes) and puts them into an internal buffer. A
		 * regular InputStream grabs one byte per read() so it has to pester
		 * the OS more and is way slower.
		 */
		try {
			
			HttpGet request = new HttpGet(query);
    		HttpParams params = new BasicHttpParams();
    		int timeout = (int) (15 * DateUtils.SECOND_IN_MILLIS);
    		HttpConnectionParams.setConnectionTimeout(params, timeout);
    		HttpConnectionParams.setSoTimeout(params, timeout);
    		HttpClient client = new DefaultHttpClient(params);
    		
    		HttpResponse response = client.execute(request);
    		StatusLine status = response.getStatusLine();
    		if (status.getStatusCode() == HttpStatus.SC_OK) {
    			ResponseHandler<String> responseHandler = new BasicResponseHandler();
    			queryResult = responseHandler.handleResponse(response);
    		} 		
    		
    		client.getConnectionManager().shutdown();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		 
		
		/*
		 * Parse the JSON String (queryResult) that we got from archive.org. If the
		 * mediatype is etree, create an ArchiveShowObj which encapsulates
		 * the information for a particular result from the archive.org
		 * query. Populate the ArrayList which backs the ListView, and call
		 * the inherited refreshSearchList().
		 */
		JSONObject jObject;
		try {
			jObject = new JSONObject(queryResult.replace("callback(", "")).getJSONObject("response");
			JSONArray docsArray = jObject.getJSONArray("docs");
			int numItems = docsArray.length();
			if(numItems == 0){
				 
			}
			for (int i = 0; i < numItems; i++) {
				if (docsArray.getJSONObject(i).optString("mediatype").equals("etree")) {
					// Might be inefficient to keep getting size().
					searchResults.add(searchResults.size(),new ArchiveShowObj(docsArray.getJSONObject(i).optString("title"), docsArray.getJSONObject(i).optString("identifier"), docsArray.getJSONObject(i).optString("date"), docsArray.getJSONObject(i).optDouble("avg_rating"), docsArray.getJSONObject(i).optString("format"), docsArray.getJSONObject(i).optString("source")));
				}
			}
		} catch (JSONException e) {
			// DEBUG
			 
			 
		}
		
		 
	}

	
	/**
	 * Parse the show details page.
	 * 
	 * I didn't want to use an external .JAR to parse the HTML, but it is better
	 * practice than "rolling my own" parser using regex's or something. We use
	 * HtmlCleaner instead of TagSoup because it is smaller, even though
	 * HtmlCleaner doesn't support 100% of XPath features. We use another
	 * AsyncTask to not block the UI thread. I don't know if this is really
	 * necessary, but I think that it is good idea in case we want to use this
	 * Activity in different ways in the future.
	 */
	public static void getSongs(ArchiveShowObj show, ArrayList<ArchiveSongObj> songs, StaticDataStore db) {
		Searching.getSongs(show, songs, db, true);
	}
	
	public static void getSongs(ArchiveShowObj show, ArrayList<ArchiveSongObj> songs, StaticDataStore db, boolean processSongs) {
		
		HtmlCleaner pageParser = new HtmlCleaner();
		CleanerProperties props = pageParser.getProperties();
		props.setAllowHtmlInsideAttributes(true);
		props.setAllowMultiWordAttributes(true);
		props.setRecognizeUnicodeChars(true);
		props.setOmitComments(true);

		ArrayList<String> songLinks = new ArrayList<String>();
		ArrayList<String> songTitles = new ArrayList<String>();
		String showTitle = show.getArtistAndTitle();
		String showIdent = show.getIdentifier();

		// XPATH says "Select out of all 'table' elements with attribute 'class'
		// equal to 'fileFormats' which contain element 'tr'..."
		// String songXPath = "//table[@class='fileFormats']//tr";
		
		// XPATH says "Select out of all 'script' elements with attribute 'type'
		// equal to 'text/javascript'..."
		String m3uXPath = "//script";
		String titlePath ="//head//title";
		
		if (db.getShowExists(show) && processSongs) {
			
			 
			songs.addAll(db.getSongsFromShow(show.getIdentifier()));
			show.setFullTitle(db.getShow(show.getIdentifier()).getArtistAndTitle());
			return;
		}

		try {
			HttpParams params = new BasicHttpParams();
    		int timeout = (int) (15 * DateUtils.SECOND_IN_MILLIS);
    		HttpConnectionParams.setConnectionTimeout(params, timeout);
    		HttpConnectionParams.setSoTimeout(params, timeout);
    		HttpClient client = new DefaultHttpClient(params);
    		
    		HttpGet page = new HttpGet(show.getShowURL().toString());
    		HttpResponse pageResponse = client.execute(page);
    		StatusLine pageStatus = pageResponse.getStatusLine();
    		if (pageStatus.getStatusCode() == HttpStatus.SC_OK) {
    			ResponseHandler<String> pageResponseHandler = new BasicResponseHandler();
    			TagNode node = pageParser.clean(pageResponseHandler.handleResponse(pageResponse));
			
    			String queryString = show.getLinkPrefix();
    			
				if (db.getPref("downloadFormat").equalsIgnoreCase("LBR")) {
					if (show.hasLBR()) {
						queryString += "_64kb.m3u";
					} else if (show.hasVBR()) {
						queryString += "_vbr.m3u";
						 
					}
				} else {
					if (show.hasVBR()) {
						queryString +=  "_vbr.m3u";
					} else if (show.hasLBR()) {
						queryString += "_64kb.m3u";
						 
					}
				}
								
				HttpGet M3Urequest = new HttpGet(queryString);
					    		
	    		HttpResponse M3Uresponse = client.execute(M3Urequest);
	    		StatusLine M3Ustatus = M3Uresponse.getStatusLine();
	    		if (M3Ustatus.getStatusCode() == HttpStatus.SC_OK) {
	    			ResponseHandler<String> M3UresponseHandler = new BasicResponseHandler();
	    			String m3uString = M3UresponseHandler.handleResponse(M3Uresponse);
	    		
	    			client.getConnectionManager().shutdown();
	    			
					// Now split the .M3U file based on newlines. This will give
					// us the download links, which we store..
					
					
					String m3uLinks[] = m3uString.split("\n");
					for (String link : m3uLinks) {
						songLinks.add(link);
	
					}
	
					// Now use an XPATH evaluation to find all of the javascript scripts on the page.
					// If one of them can be split by "IAD.mrss = ", it should have the track names
					// in it. The second half of the split is valid javascript and can be interpreted,
					// therefore, as JSON. Pull the song titles out of that, and together with the
					// download links make ArchiveSongObjs and add them to the list of songs.
					Object[] titleNodes = node.evaluateXPath(m3uXPath);
					for (Object titleNode : titleNodes) {
	//					 
						List x = ((TagNode) titleNode).getChildren();
						String songTitle = "";
						for(Object y : x){
							if(y instanceof ContentNode){
								songTitle = ((ContentNode)y).toString();
								songTitle = songTitle.trim();
								if(songTitle.startsWith("Play(")){
									String[] titles = songTitle.split("\\{\"title\"");
									for(int i = 1; i < titles.length; i++){
										try{
										String title = titles[i].substring(nthIndexOf(titles[i], '"', 1),nthIndexOf(titles[i], '"', 2));
										songTitles.add(title.substring(title.indexOf('.')+2));
										}
										catch(StringIndexOutOfBoundsException e){
										}
									}											
								}
							}
						}
					}
					if(show.getShowTitle().length()<2){
						 
						String s = ((TagNode)node.evaluateXPath(titlePath)[0]).getChildren().toString().replaceFirst(Pattern.quote("["), "");
						show.setFullTitle(s.substring(0, s.lastIndexOf(": Free")-1));
						showTitle = show.getArtistAndTitle();
						db.updateShow(show);
					}
					
					if (processSongs) {
						if(songLinks.size()==0){
							 
						}
						else {
							//Do things for successful show parse
							db.insertShow(show);
						}
						// If we have the same amount of song titles as song links,
						// we should be all set.
						if (songTitles.size() == songLinks.size()) {
							for (int i = 0; i < songTitles.size(); i++) {
								String songLink = songLinks.get(i);
								String songTitle = songTitles.get(i);
								// If the show has a "selectedSong"
								// meaning that it was opened by
								// the user clicking on a song link, do
								// a comparison to see
								// if the song being added is the
								// selected song. If it is, set
								// selectedPos to the right index so
								// that the song can be played
								// once the ListView is filled.  This is
								// inefficient, though it probably doesn't make a difference,
								// but we might consider making this a bit more efficient/elegant in the future.
								// FIXME.
		//						if (show.hasSelectedSong()) {
		//							if (songLink.equals(show.getSelectedSong())) {
		//								selectedPos = i;
		//							}
		//						} else {
		//							selectedPos = -1;
		//						}
								ArchiveSongObj song = new ArchiveSongObj(
										songTitle,
										songLink, showTitle, showIdent);
								song.setID(db.insertSong(song));
								songs.add(song);
							}
							db.setShowExists(show);
							db.insertRecentShow(show);
						} else {
							 
						}
					}
					
				}
	    		else {
	    			client.getConnectionManager().shutdown();
	    		}
    		}
    		else {
    			client.getConnectionManager().shutdown();
    		}

		} catch (XPatherException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		
		
		// TODO Auto-generated method stub
		
	}
	
	public static int nthIndexOf(String text, char needle, int n)
	{
	    for (int i = 0; i < text.length(); i++)
	    {
	        if (text.charAt(i) == needle)
	        {
	            n--;
	            if (n == 0)
	            {
	                return i;
	            }
	        }
	    }
	    return -1;
	}
	
	public static Boolean updateArtists(StaticDataStore db){
		 
		ArrayList<ArrayList<String>> artists = new ArrayList<ArrayList<String>>();
		int numArtists;
		
		HtmlCleaner pageParser = new HtmlCleaner();
		CleanerProperties props = pageParser.getProperties();
		props.setAllowHtmlInsideAttributes(true);
		props.setAllowMultiWordAttributes(true);
		props.setRecognizeUnicodeChars(true);
		props.setOmitComments(true);

		try {			
			String url = "http://www.archive.org/browse.php?field=/metadata/bandWithMP3s&collection=etree";
			
			HttpParams params = new BasicHttpParams();
    		int timeout = (int) (15 * DateUtils.SECOND_IN_MILLIS);
    		HttpConnectionParams.setConnectionTimeout(params, timeout);
    		HttpConnectionParams.setSoTimeout(params, timeout);
    		HttpClient client = new DefaultHttpClient(params);
    		
    		HttpGet request = new HttpGet(url);
    		HttpResponse response = client.execute(request);
    		StatusLine status = response.getStatusLine();
    		if (status.getStatusCode() == HttpStatus.SC_OK) {
    			ResponseHandler<String> responseHandler = new BasicResponseHandler();
    			TagNode node = pageParser.clean(responseHandler.handleResponse(response));
    			client.getConnectionManager().shutdown();
				// XPATH to get the nodes that we Want.
				Object[] artistsNodes = node.evaluateXPath("//tr[@valign='top']//li");
	
				numArtists = artistsNodes.length;
				
	
				for (int i = 0; i < numArtists; i++) {
					
					// Cast the artistNode as a TagNode.
					TagNode artist = ((TagNode) artistsNodes[i]);
					// Grab the first child node, which is the link to the artist's page.
					// The inner HTML of this node will be the title.
					TagNode artistTitleSubNode = artist.getChildTags()[0];
					// Remove the child node, so that the inner HTML of the artistNode
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
	
				}
				if (artists.size() > 0) {
					db.insertArtistBulk(artists);
					String s = DateFormat.format("yyyy-MM-dd", new GregorianCalendar().getTime()).toString();
					db.updatePref("artistUpdate", s);
					 
				}
				else {
				 
					
				}
    		}
    		else {
    			client.getConnectionManager().shutdown();
    		}
		} catch(Exception e) {
			e.printStackTrace();
			 
		}
		return true;

	}
	
}
