package com.code.android.vibevault;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;

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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.format.DateUtils;
import android.util.Log;

public class Voting {
		
	private final static String LOG_TAG = Voting.class.getName();
	
	private final static String host = "http://sandersdenardi.com/vibevault/php/";

	public static final int VOTES_SHOWS = 0;
	public static final int VOTES_ARTISTS = 1;
	public static final int VOTES_SHOWS_BY_ARTIST = 2;
	
	public static final int VOTES_ALL_TIME = 1;
	public static final int VOTES_DAILY = 2;
	public static final int VOTES_WEEKLY = 3;
	public static final int VOTES_NEWEST_ADDED = 4;
	public static final int VOTES_NEWEST_VOTED = 5;
	
	public static String vote(String showIdent, String showArtist, String showTitle, String showDate, StaticDataStore db){
		
		String results = null;
    	int userId = Integer.parseInt(db.getPref("userId"));
		
    	int returnedUserId = 0;
    	String message = "Error voting";
    	try {
    		URI queryString = new URI(host + "vote.php?" +
				"userId=" + userId + 
				"&showIdent=" + URLEncoder.encode(showIdent, "UTF-8") +
				"&showArtist=" + URLEncoder.encode(showArtist.replace("'", ""), "UTF-8") +
				"&showTitle=" + URLEncoder.encode(showTitle.replace("'", ""), "UTF-8") +
				"&showDate=" + URLEncoder.encode(showDate, "UTF-8") +
				"&showSource=" + "" +
				"&showRating=" + 0.0);
    		 
    		
    		HttpGet request = new HttpGet(queryString);
    		HttpParams params = new BasicHttpParams();
    		int timeout = (int) (15 * DateUtils.SECOND_IN_MILLIS);
    		HttpConnectionParams.setConnectionTimeout(params, timeout);
    		HttpConnectionParams.setSoTimeout(params, timeout);
    		HttpClient client = new DefaultHttpClient(params);
    		
    		HttpResponse response = client.execute(request);
    		StatusLine status = response.getStatusLine();
    		if (status.getStatusCode() != HttpStatus.SC_OK) {
    			client.getConnectionManager().shutdown();
    			return "Can not reach external server. Check internet connection.";
    		}
    		
    		ResponseHandler<String> responseHandler = new BasicResponseHandler();
    		results = responseHandler.handleResponse(response);
    		
    		client.getConnectionManager().shutdown();
		} catch (IOException e) {
			e.printStackTrace();
			return "Can not reach external server. Check internet connection.";
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return "Syntax Error. Please e-mail developer.";
		}
    	
		if(results.equalsIgnoreCase("0")){
			return "Error parsing server response. Please e-mail developer.";
		}
		if(results != null){
			JSONObject jObject;
			try {
				jObject = new JSONObject(results);
				JSONArray resultArray = jObject.getJSONArray("results");
				JSONObject resultObject = resultArray.getJSONObject(0);
				returnedUserId = resultObject.optInt("userId");
	    		
				db.updatePref("userId", Integer.toString(returnedUserId));
				message = resultObject.getString("resultText");
	    		
			} catch (JSONException e) {
				e.printStackTrace();
				return "Error parsing server response. Please e-mail developer.";
			}
		} 
		return message;
	}
	
	public static String vote(ArchiveShowObj show, StaticDataStore db){
		
    	String results = null;
    	int userId = Integer.parseInt(db.getPref("userId"));
		
    	int returnedUserId = 0;
    	String message = "Error voting";
    	try {
    		URI queryString = new URI(host + "vote.php?" +
				"userId=" + userId + 
				"&showIdent=" + URLEncoder.encode(show.getIdentifier(), "UTF-8") +
				"&showArtist=" + URLEncoder.encode(show.getShowArtist().replace("'", ""), "UTF-8") +
				"&showTitle=" + URLEncoder.encode(show.getShowTitle().replace("'", ""), "UTF-8") +
				"&showDate=" + URLEncoder.encode(show.getDate(), "UTF-8") +
				"&showSource=" + URLEncoder.encode(show.getShowSource(), "UTF-8") +
				"&showRating=" + show.getRating());
    		 
    		
    		HttpGet request = new HttpGet(queryString);
    		HttpParams params = new BasicHttpParams();
    		int timeout = (int) (15 * DateUtils.SECOND_IN_MILLIS);
    		HttpConnectionParams.setConnectionTimeout(params, timeout);
    		HttpConnectionParams.setSoTimeout(params, timeout);
    		HttpClient client = new DefaultHttpClient(params);
    		
    		HttpResponse response = client.execute(request);
    		StatusLine status = response.getStatusLine();
    		if (status.getStatusCode() != HttpStatus.SC_OK) {
    			client.getConnectionManager().shutdown();
    			return "Can not reach external server. Check internet connection.";
    		}
    		
    		ResponseHandler<String> responseHandler = new BasicResponseHandler();
    		results = responseHandler.handleResponse(response);
    		
    		client.getConnectionManager().shutdown();
		} catch (IOException e) {
			e.printStackTrace();
			return "Can not reach external server. Check internet connection.";
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return "Syntax Error. Please e-mail developer.";
		}
		if(results.equalsIgnoreCase("0")){
			return "Error parsing server response. Please e-mail developer.";
		}
		if(results != null){
			JSONObject jObject;
			try {
				jObject = new JSONObject(results);
				JSONArray resultArray = jObject.getJSONArray("results");
				JSONObject resultObject = resultArray.getJSONObject(0);
				returnedUserId = resultObject.optInt("userId");
	    		
				db.updatePref("userId", Integer.toString(returnedUserId));
				message = resultObject.getString("resultText");
	    		
			} catch (JSONException e) {
				e.printStackTrace();
				return "Error parsing server response. Please e-mail developer.";
			}
		} 
		return message;
    }

	public static ArrayList<ArchiveShowObj> getShows(int resultType, int numResults, int offset, StaticDataStore db){
		
    	ArrayList<ArchiveShowObj> shows = new ArrayList<ArchiveShowObj>();
    	String results = null;
    	int userId = Integer.parseInt(db.getPref("userId"));
		
    	int returnedUserId = 0;
    	try {
    		URI queryString = new URI(host + "getShows.php?" +
				"resultType=" + resultType +
				"&numResults=" + numResults +
				"&offset=" + offset +
				"&userId=" + userId);
    		
    		HttpGet request = new HttpGet(queryString);
    		HttpParams params = new BasicHttpParams();
    		int timeout = (int) (15 * DateUtils.SECOND_IN_MILLIS);
    		HttpConnectionParams.setConnectionTimeout(params, timeout);
    		HttpConnectionParams.setSoTimeout(params, timeout);
    		HttpClient client = new DefaultHttpClient(params);
    		
    		HttpResponse response = client.execute(request);
    		StatusLine status = response.getStatusLine();
    		if (status.getStatusCode() != HttpStatus.SC_OK) {
    			client.getConnectionManager().shutdown();
    			return shows;
    		}
    		
    		ResponseHandler<String> responseHandler = new BasicResponseHandler();
    		results = responseHandler.handleResponse(response);
    		
    		client.getConnectionManager().shutdown();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		if(results != null){
			JSONObject jObject;
			try {
				jObject = new JSONObject(results);
				JSONArray showArray = jObject.getJSONArray("shows");
				int numItems = showArray.length();
				for (int i = 0; i < numItems; i++) {
					JSONObject showObject = showArray.getJSONObject(i);
					ArchiveShowObj newShow = new ArchiveShowObj(showObject.optString("identifier"), 
							showObject.optString("title"), 
							showObject.optString("artist"), 
							showObject.optString("date"),
							showObject.optString("source"), 
							showObject.optDouble("rating"), 
							showObject.optInt("votes"));
					returnedUserId = showObject.optInt("userId");
					
					shows.add(newShow);
				}
				
				db.updatePref("userId", Integer.toString(returnedUserId));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
    	return shows;
    }

	public static ArrayList<ArchiveArtistObj> getArtists(int resultType, int numResults, int offset, StaticDataStore db){
		
		ArrayList<ArchiveArtistObj> artists = new ArrayList<ArchiveArtistObj>();
    	String results = null;
    	int userId = Integer.parseInt(db.getPref("userId"));
		
    	int returnedUserId = 0;
    	try {
    		URI queryString = new URI(host + "getArtists.php?" +
				"resultType=" + resultType +
				"&numResults=" + numResults +
				"&offset=" + offset +
				"&userId=" + userId);
    		
    		HttpGet request = new HttpGet(queryString);
    		HttpParams params = new BasicHttpParams();
    		int timeout = (int) (15 * DateUtils.SECOND_IN_MILLIS);
    		HttpConnectionParams.setConnectionTimeout(params, timeout);
    		HttpConnectionParams.setSoTimeout(params, timeout);
    		HttpClient client = new DefaultHttpClient(params);
    		
    		HttpResponse response = client.execute(request);
    		StatusLine status = response.getStatusLine();
    		if (status.getStatusCode() != HttpStatus.SC_OK) {
    			client.getConnectionManager().shutdown();
    			return artists;
    		}
    		
    		ResponseHandler<String> responseHandler = new BasicResponseHandler();
    		results = responseHandler.handleResponse(response);
    		
    		client.getConnectionManager().shutdown();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		if(results != null){
			JSONObject jObject;
			try {
				jObject = new JSONObject(results);
				JSONArray artistArray = jObject.getJSONArray("artists");
				int numItems = artistArray.length();
				for (int i = 0; i < numItems; i++) {
					JSONObject artistObject = artistArray.getJSONObject(i);
					ArchiveArtistObj newArtist = new ArchiveArtistObj(artistObject.optInt("artistId"), 
							artistObject.optString("artist"), 
							artistObject.optDouble("rating"), 
							artistObject.optInt("votes"),
							artistObject.optString("lastVote"));
					returnedUserId = artistObject.optInt("userId");
					
					artists.add(newArtist);
				}
				
				db.updatePref("userId", Integer.toString(returnedUserId));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
    	return artists;
    }

	public static ArrayList<ArchiveShowObj> getShowsByArtist(int resultType, int numResults, int offset, int artistId, StaticDataStore db){
		
		ArrayList<ArchiveShowObj> shows = new ArrayList<ArchiveShowObj>();
    	String results = null;
    	int userId = Integer.parseInt(db.getPref("userId"));
		
    	int returnedUserId = 0;
    	try {
    		URI queryString = new URI(host + "getShowsByArtist.php?" +
				"resultType=" + resultType +
				"&numResults=" + numResults +
				"&offset=" + offset +
				"&userId=" + userId +
				"&artistId=" + artistId);
    		
    		HttpGet request = new HttpGet(queryString);
    		HttpParams params = new BasicHttpParams();
    		int timeout = (int) (15 * DateUtils.SECOND_IN_MILLIS);
    		HttpConnectionParams.setConnectionTimeout(params, timeout);
    		HttpConnectionParams.setSoTimeout(params, timeout);
    		HttpClient client = new DefaultHttpClient(params);
    		
    		HttpResponse response = client.execute(request);
    		StatusLine status = response.getStatusLine();
    		if (status.getStatusCode() != HttpStatus.SC_OK) {
    			client.getConnectionManager().shutdown();
    			return shows;
    		}
    		
    		ResponseHandler<String> responseHandler = new BasicResponseHandler();
    		results = responseHandler.handleResponse(response);
    		
    		client.getConnectionManager().shutdown();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		if(results != null){
			JSONObject jObject;
			try {
				jObject = new JSONObject(results);
				JSONArray showArray = jObject.getJSONArray("shows");
				int numItems = showArray.length();
				for (int i = 0; i < numItems; i++) {
					JSONObject showObject = showArray.getJSONObject(i);
					ArchiveShowObj newShow = new ArchiveShowObj(showObject.optString("identifier"), 
							showObject.optString("title"), 
							showObject.optString("artist"), 
							showObject.optString("date"),
							showObject.optString("source"), 
							showObject.optDouble("rating"), 
							showObject.optInt("votes"));
					returnedUserId = showObject.optInt("userId");
					
					shows.add(newShow);
				}
				
				db.updatePref("userId", Integer.toString(returnedUserId));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
    	return shows;
    }
}
