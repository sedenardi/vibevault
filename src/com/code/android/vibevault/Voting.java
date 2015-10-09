package com.code.android.vibevault;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Voting {
		
	private final static String host = "";
	
	public static String vote(String showIdent, String showArtist, String showTitle, String showDate){
		String results = null;
    	int userId = Integer.parseInt(VibeVault.db.getPref("userId"));
		
    	int returnedUserId = 0;
    	String message = "Error voting";
    	try {
    		URI queryString = new URI(host + "vote.php?" +
				"userId=" + userId + 
				"&showIdent=" + URLEncoder.encode(showIdent) +
				"&showArtist=" + URLEncoder.encode(showArtist) +
				"&showTitle=" + URLEncoder.encode(showTitle) +
				"&showDate=" + URLEncoder.encode(showDate) +
				"&showSource=" + "" +
				"&showRating=" + 0.0);
    		
			HttpURLConnection urlConn = (HttpURLConnection) queryString.toURL().openConnection();
			HttpURLConnection httpConn = (HttpURLConnection) urlConn;
			
			InputStream in = httpConn.getInputStream();
			BufferedInputStream bis = new BufferedInputStream(in);
			ByteArrayBuffer baf = new ByteArrayBuffer(50);
			int read = 0;
			int bufSize = 512;
			byte[] buffer = new byte[bufSize];
			while (true) {
				read = bis.read(buffer);
				if (read == -1) {
					break;
				}
				baf.append(buffer, 0, read);
			}
			bis.close();
			
			results = new String(baf.toByteArray());
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return "Syntax Error. Please e-mail developer.";
		} catch (IOException e) {
			e.printStackTrace();
			return "Can not reach voting server.";
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return "Syntax Error. Please e-mail developer.";
		}
		if(results.equalsIgnoreCase("0")){
			return "Invalid parameters.";
		}
		if(results != null){
			JSONObject jObject;
			try {
				jObject = new JSONObject(results);
				JSONArray resultArray = jObject.getJSONArray("results");
				JSONObject resultObject = resultArray.getJSONObject(0);
				returnedUserId = resultObject.optInt("userId");
	    		
				VibeVault.db.updatePref("userId", Integer.toString(returnedUserId));
				message = resultObject.getString("resultText");
	    		
			} catch (JSONException e) {
				e.printStackTrace();
			}
		} 
		return message;
	}
	
	public static String vote(ArchiveShowObj show){
    	String results = null;
    	int userId = Integer.parseInt(VibeVault.db.getPref("userId"));
		
    	int returnedUserId = 0;
    	String message = "Error voting";
    	try {
    		URI queryString = new URI(host + "vote.php?" +
				"userId=" + userId + 
				"&showIdent=" + URLEncoder.encode(show.getIdentifier()) +
				"&showArtist=" + URLEncoder.encode(show.getShowArtist()) +
				"&showTitle=" + URLEncoder.encode(show.getShowTitle()) +
				"&showDate=" + URLEncoder.encode(show.getDate()) +
				"&showSource=" + URLEncoder.encode(show.getShowSource()) +
				"&showRating=" + show.getRating());
    		
			HttpURLConnection urlConn = (HttpURLConnection) queryString.toURL().openConnection();
			HttpURLConnection httpConn = (HttpURLConnection) urlConn;
			
			InputStream in = httpConn.getInputStream();
			BufferedInputStream bis = new BufferedInputStream(in);
			ByteArrayBuffer baf = new ByteArrayBuffer(50);
			int read = 0;
			int bufSize = 512;
			byte[] buffer = new byte[bufSize];
			while (true) {
				read = bis.read(buffer);
				if (read == -1) {
					break;
				}
				baf.append(buffer, 0, read);
			}
			bis.close();
			
			results = new String(baf.toByteArray());
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return "Syntax Error. Please e-mail developer.";
		} catch (IOException e) {
			e.printStackTrace();
			return "Can not reach voting server.";
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return "Syntax Error. Please e-mail developer.";
		}
		if(results.equalsIgnoreCase("0")){
			return "Invalid parameters.";
		}
		if(results != null){
			JSONObject jObject;
			try {
				jObject = new JSONObject(results);
				JSONArray resultArray = jObject.getJSONArray("results");
				JSONObject resultObject = resultArray.getJSONObject(0);
				returnedUserId = resultObject.optInt("userId");
	    		
				VibeVault.db.updatePref("userId", Integer.toString(returnedUserId));
				message = resultObject.getString("resultText");
	    		
			} catch (JSONException e) {
				e.printStackTrace();
			}
		} 
		return message;
    }

	public static ArrayList<ArchiveShowObj> getShows(int resultType, int numResults, int offset){
    	ArrayList<ArchiveShowObj> shows = new ArrayList<ArchiveShowObj>();
    	String results = null;
    	int userId = Integer.parseInt(VibeVault.db.getPref("userId"));
		
    	int returnedUserId = 0;
    	try {
    		URI queryString = new URI(host + "getShows.php?" +
				"resultType=" + resultType +
				"&numResults=" + numResults +
				"&offset=" + offset +
				"&userId=" + userId);
    		
			HttpURLConnection urlConn = (HttpURLConnection) queryString.toURL().openConnection();
			HttpURLConnection httpConn = (HttpURLConnection) urlConn;
			InputStream in = httpConn.getInputStream();
			BufferedInputStream bis = new BufferedInputStream(in);
			ByteArrayBuffer baf = new ByteArrayBuffer(50);
			int read = 0;
			int bufSize = 512;
			byte[] buffer = new byte[bufSize];
			while (true) {
				read = bis.read(buffer);
				if (read == -1) {
					break;
				}
				baf.append(buffer, 0, read);
			}
			bis.close();
			
			results = new String(baf.toByteArray());
		} catch (MalformedURLException e) {
			e.printStackTrace();
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
				
				VibeVault.db.updatePref("userId", Integer.toString(returnedUserId));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
    	return shows;
    }

	public static ArrayList<ArchiveArtistObj> getArtists(int resultType, int numResults, int offset){
    	ArrayList<ArchiveArtistObj> artists = new ArrayList<ArchiveArtistObj>();
    	String results = null;
    	int userId = Integer.parseInt(VibeVault.db.getPref("userId"));
		
    	int returnedUserId = 0;
    	try {
    		URI queryString = new URI(host + "getArtists.php?" +
				"resultType=" + resultType +
				"&numResults=" + numResults +
				"&offset=" + offset +
				"&userId=" + userId);
    		
			HttpURLConnection urlConn = (HttpURLConnection) queryString.toURL().openConnection();
			HttpURLConnection httpConn = (HttpURLConnection) urlConn;
			InputStream in = httpConn.getInputStream();
			BufferedInputStream bis = new BufferedInputStream(in);
			ByteArrayBuffer baf = new ByteArrayBuffer(50);
			int read = 0;
			int bufSize = 512;
			byte[] buffer = new byte[bufSize];
			while (true) {
				read = bis.read(buffer);
				if (read == -1) {
					break;
				}
				baf.append(buffer, 0, read);
			}
			bis.close();
			
			results = new String(baf.toByteArray());
		} catch (MalformedURLException e) {
			e.printStackTrace();
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
				
				VibeVault.db.updatePref("userId", Integer.toString(returnedUserId));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
    	return artists;
    }

	public static ArrayList<ArchiveShowObj> getShowsByArtist(int resultType, int numResults, int offset, int artistId){
    	ArrayList<ArchiveShowObj> shows = new ArrayList<ArchiveShowObj>();
    	String results = null;
    	int userId = Integer.parseInt(VibeVault.db.getPref("userId"));
		
    	int returnedUserId = 0;
    	try {
    		URI queryString = new URI(host + "getShowsByArtist.php?" +
				"resultType=" + resultType +
				"&numResults=" + numResults +
				"&offset=" + offset +
				"&userId=" + userId +
				"&artistId=" + artistId);
    		
			HttpURLConnection urlConn = (HttpURLConnection) queryString.toURL().openConnection();
			HttpURLConnection httpConn = (HttpURLConnection) urlConn;
			InputStream in = httpConn.getInputStream();
			BufferedInputStream bis = new BufferedInputStream(in);
			ByteArrayBuffer baf = new ByteArrayBuffer(50);
			int read = 0;
			int bufSize = 512;
			byte[] buffer = new byte[bufSize];
			while (true) {
				read = bis.read(buffer);
				if (read == -1) {
					break;
				}
				baf.append(buffer, 0, read);
			}
			bis.close();
			
			results = new String(baf.toByteArray());
		} catch (MalformedURLException e) {
			e.printStackTrace();
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
				
				VibeVault.db.updatePref("userId", Integer.toString(returnedUserId));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
    	return shows;
    }
}
