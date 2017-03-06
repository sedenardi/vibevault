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
import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.NodeList;

import android.text.format.DateFormat;
import android.text.format.DateUtils;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

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

	public static String makeSearchURLString(int pageNum, int monthSearchInt, int daySearchInt, int yearSearchInt, String artistSearchText, int numSearchResults, String sortResults, int dateType){
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
					Logging.Log(LOG_TAG, "ANYTIME.");
					break;
				case SearchSettingsDialogFragment.BEFORE:	//Before
					Logging.Log(LOG_TAG, "BEFORE.");
					dateModifier = "date:[1800-01-01%20TO%20" + yearSearchInt + "-" + (monthSearchInt>0?String.format("%02d",monthSearchInt):"01") + "-01]%20AND%20";
					break;
				case SearchSettingsDialogFragment.AFTER:	//After
					int curDate = Calendar.getInstance().get(Calendar.DATE);
					int curMonth = Calendar.getInstance().get(Calendar.MONTH);
					int curYear = Calendar.getInstance().get(Calendar.YEAR);
					dateModifier = "date:[" + (monthSearchInt>0?yearSearchInt:yearSearchInt+1) + "-" +
							(monthSearchInt>0?String.format("%02d",monthSearchInt):"01")  + "-01%20TO%20" + curYear + "-" + String.format("%02d",curMonth) + "-" + String.format("%02d",curDate) + "]%20AND%20";
					break;
				case SearchSettingsDialogFragment.DURING:	// In Year.
					dateModifier = "date:[" + yearSearchInt + "-" + (monthSearchInt>0?String.format("%02d",monthSearchInt):"01") +
							"-01%20TO%20" + yearSearchInt + "-" + (monthSearchInt>0?String.format("%02d",monthSearchInt):"12") + "-31]%20AND%20";
					break;
				case SearchSettingsDialogFragment.SPECIFIC:
					String specific = yearSearchInt + "-" + String.format("%02d",monthSearchInt) + "-" + String.format("%02d",daySearchInt);
					dateModifier = "date:[" + specific + "%20TO%20" + specific + "]%20AND%20";
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
					+ String.valueOf(numResults) + "&page=" + String.valueOf(pageNum) + "&output=json&save=yes";
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		Logging.Log(LOG_TAG,queryString);
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

		Logging.Log(LOG_TAG, "JSON grabbed.");
		
		/*
		 * Parse the JSON String (queryResult) that we got from archive.org. If the
		 * mediatype is etree, create an ArchiveShowObj which encapsulates
		 * the information for a particular result from the archive.org
		 * query. Populate the ArrayList which backs the ListView, and call
		 * the inherited refreshSearchList().
		 */
		JSONObject jObject;
		try {
			jObject = new JSONObject(queryResult).getJSONObject("response");
			JSONArray docsArray = jObject.getJSONArray("docs");
			int numItems = docsArray.length();
			if(numItems == 0){
				Logging.Log(LOG_TAG, "Artist may not have content on archive.org...");
			}
			for (int i = 0; i < numItems; i++) {
				if (docsArray.getJSONObject(i).optString("mediatype").equals("etree")) {
					// Might be inefficient to keep getting size().
					searchResults.add(searchResults.size(),new ArchiveShowObj(docsArray.getJSONObject(i).optString("title"), docsArray.getJSONObject(i).optString("identifier"), docsArray.getJSONObject(i).optString("date"), docsArray.getJSONObject(i).optDouble("avg_rating"), docsArray.getJSONObject(i).optString("format"), docsArray.getJSONObject(i).optString("source")));
				}
			}
		} catch (JSONException e) {
			// DEBUG
			Logging.Log(LOG_TAG, "JSON error: " + queryResult);
			Logging.Log(LOG_TAG, e.toString());
		}

		Logging.Log(LOG_TAG, "Returning results.");
	}

	public static Boolean updateArtists(StaticDataStore db){
		Logging.Log(LOG_TAG, "Fetching Artists");
		ArrayList<ArrayList<String>> artists = new ArrayList<ArrayList<String>>();

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

				org.w3c.dom.Document doc = new DomSerializer(new CleanerProperties()).createDOM(node);
				XPath xpath = XPathFactory.newInstance().newXPath();
				NodeList artistNodes = (NodeList) xpath.evaluate("//div[@class='row']//div[@class='col-sm-4']/a", doc, XPathConstants.NODESET);
				NodeList numberNodes = (NodeList) xpath.evaluate("//div[@class='row']//div[@class='col-sm-4']/text()[preceding-sibling::a]", doc, XPathConstants.NODESET);
				Logging.Log(LOG_TAG, "artistNodes: " + artistNodes.getLength());
				Logging.Log(LOG_TAG, "numberNodes: " + numberNodes.getLength());

				if(artistNodes.getLength() == numberNodes.getLength()){
					for (int i = 0; i < artistNodes.getLength(); i++) {
						ArrayList<String> artistPair = new ArrayList<String>();
						artistPair.add(artistNodes.item(i).getTextContent().replace("&apos;", "'").replace("&gt;", ">").replace("&lt;", "<").replace("&quot;", "\"").replace("&amp;", "&"));
						artistPair.add(numberNodes.item(i).getTextContent());
						artists.add(artistPair);
					}
				}
				if (artists.size() > 0) {
					db.insertArtistBulk(artists);
					String s = DateFormat.format("yyyy-MM-dd", new GregorianCalendar().getTime()).toString();
					db.updatePref("artistUpdate", s);
					Logging.Log(LOG_TAG, "Finished Fetching Artists");
				}
				else {
					Logging.Log(LOG_TAG, "Error Fetching Artists");
				}
			}
			else {
				client.getConnectionManager().shutdown();
			}
		} catch(Exception e) {
			e.printStackTrace();
			Logging.Log(LOG_TAG, "Error Fetching Artists");
		}
		return true;

	}

}