package com.code.android.vibevault;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.HTTP;

import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class TwitterCon extends Activity {
	
	public static final String TAG = "TWITTER CONNECT";
	public static final String CONSUMER_KEY = "";
	public static final String CONSUMER_SECRET= "";
	
	public static final String REQUEST_URL = "http://api.twitter.com/oauth/request_token";
	public static final String ACCESS_URL = "http://api.twitter.com/oauth/access_token";
	public static final String AUTHORIZE_URL = "http://api.twitter.com/oauth/authorize";
	
	public static final String	OAUTH_CALLBACK_SCHEME = "VibeVault";
	public static final String	OAUTH_CALLBACK_HOST = "tweet";
	public static final String	OAUTH_CALLBACK_URL = OAUTH_CALLBACK_SCHEME + "://" + OAUTH_CALLBACK_HOST;
	
	private Button mLoginButton;
	private Button mPostButton;
	private EditText mMessage;
	private OAuthProvider provider;
	private CommonsHttpOAuthConsumer consumer;
	private Handler mHandler = new Handler();
	
	
	private Vibrator vibrator;
	
	@Override
	public Object onRetainNonConfigurationInstance(){
		return mMessage.getText().toString();
	}
	
	private void postStatus(final String message){
		mHandler.post(new Runnable() {
			public void run() {
				if(message.length()<140){
					try{
					HttpClient client = new DefaultHttpClient();
					// Not sure why but Toast will not appear if after updateStatus() call.
					Toast.makeText(getBaseContext(), "Tweeting...", Toast.LENGTH_SHORT).show();

					
					HttpPost post = new HttpPost("http://twitter.com/statuses/update.xml");  
					final List<NameValuePair> nvps = new ArrayList<NameValuePair>();  
					// 'status' here is the update value you collect from UI  
					nvps.add(new BasicNameValuePair("status", message));  
					post.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));  
					// set this to avoid 417 error (Expectation Failed)  
					post.getParams().setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, false);  
					// sign the request  
					consumer.sign(post);  
					// send the request  
					final HttpResponse response = client.execute(post);  
					// response status should be 200 OK  
					int statusCode = response.getStatusLine().getStatusCode();  
					//final String reason = response.getStatusLine().getReasonPhrase();  
					// release connection  
					response.getEntity().consumeContent();  
					if (statusCode != 200) {  
						Toast.makeText(getBaseContext(), "Twitter error...  Have you posted this already?", Toast.LENGTH_SHORT).show();
					} else{  
						Toast.makeText(getBaseContext(), "Tweeted...", Toast.LENGTH_SHORT).show();
					}
					} catch(IOException e){
						Toast.makeText(getBaseContext(), "Twitter error...", Toast.LENGTH_SHORT).show();
					} catch (OAuthMessageSignerException e) {
						Toast.makeText(getBaseContext(), "Twitter error...", Toast.LENGTH_SHORT).show();
					} catch (OAuthExpectationFailedException e) {
						Toast.makeText(getBaseContext(), "Twitter error...", Toast.LENGTH_SHORT).show();
					} catch (OAuthCommunicationException e) {
						Toast.makeText(getBaseContext(), "Twitter error...", Toast.LENGTH_SHORT).show();
					}
				} else{
					Toast.makeText(getBaseContext(), "140 character Twitter limit.", Toast.LENGTH_LONG).show();
				}
			}
		});
	}
 
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.twitter_screen);
		
		
		
		
		// setup the content view
		//initLayout();
		mLoginButton = (Button) findViewById(R.id.TwitterLoginButton);
		mPostButton = (Button) findViewById(R.id.TwitterPostButton);
		mMessage = (EditText) findViewById(R.id.TwitterPost);
		
		vibrator = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);
		
		
		mLoginButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				try {
					vibrator.vibrate(50);
					consumer = new CommonsHttpOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);  
					provider = new DefaultOAuthProvider("http://twitter.com/oauth/request_token", "http://twitter.com/oauth/access_token", "http://twitter.com/oauth/authorize");  
					String authUrl = provider.retrieveRequestToken(consumer, OAUTH_CALLBACK_URL);
					setConsumerProvider();
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)));
				} catch (Exception e) {
					Toast.makeText(getBaseContext(), e.getMessage(), Toast.LENGTH_LONG).show();  
				}  	
			}
		});
		
		mPostButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				vibrator.vibrate(50);
				postStatus(mMessage.getText().toString());
			}
			
		});
		
		String showTitle = getIntent().getStringExtra("show_title");
		String showURL = getIntent().getStringExtra("show_url");
		if(showTitle != null && showURL !=null){
			mMessage.setText("Listening to " + showURL.replace("www.", ""));
		} else{
			Object retained = getLastNonConfigurationInstance();
			if(retained instanceof String){
				mMessage.setText((String)retained);
			}
		}
		
		checkLoggedInUpdateGUI();
		
	}
	
	/**
	 * Set the consumer and provider from the application service (in the case that the
	 * activity is restarted so the objects are not lost)
	 */
	private void setConsumerProvider() {
		if (provider!=null){
			VibeVault.provider = provider;
		}
		if (consumer!=null){
			VibeVault.consumer = consumer;
		}
	}
	
	private void getConsumerProvider(){
		consumer = VibeVault.consumer;
		provider = VibeVault.provider;
	}
	
 
    
    /**
	 * Called when the OAuthRequestTokenTask finishes (user has authorized the request token).
	 * The callback URL will be intercepted here.
	 */
	@Override
	public void onNewIntent(Intent intent) {
		super.onNewIntent(intent); 
		final Uri uri = intent.getData();
		if (uri != null && uri.toString().startsWith(OAUTH_CALLBACK_URL)) {
			String verifier = uri.getQueryParameter(oauth.signpost.OAuth.OAUTH_VERIFIER);
			try {
				// this will populate token and token_secret in consumer
				provider.retrieveAccessToken(consumer, verifier);

				// Get Access Token and persist it
				storeAccessToken(consumer.getToken(),consumer.getTokenSecret());
				mLoginButton.setEnabled(false);
				mPostButton.setEnabled(true);
			} catch (Exception e) {
				// Log.e(APP, e.getMessage());
				e.printStackTrace();
				Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			}
		} else{
			mLoginButton.setEnabled(true);
			mPostButton.setEnabled(false);
		}
	}
	
	public static String CreateUrl(String original) {
		String tinyUrl = null;
		try {

			HttpClient client = new DefaultHttpClient();
			String urlTemplate = "http://tinyurl.com/api-create.php?url=%s";
			String uri = String.format(urlTemplate, URLEncoder.encode(original));
			HttpGet request = new HttpGet(uri);
			HttpResponse response = client.execute(request);
			HttpEntity entity = response.getEntity();
			InputStream in = entity.getContent();
			try {
				StatusLine statusLine = response.getStatusLine();
				int statusCode = statusLine.getStatusCode();
				if (statusCode == HttpStatus.SC_OK) {
					String enc = "utf-8";
					Reader reader = new InputStreamReader(in, enc);
					BufferedReader bufferedReader = new BufferedReader(reader);
					tinyUrl = bufferedReader.readLine();
					if (tinyUrl != null) {
					} else {
						throw new IOException("empty response");
					}
				} else {
					String errorTemplate = "unexpected response: %d";
					String msg = String.format(errorTemplate, statusCode);
					throw new IOException(msg);
				}
			} finally {
				in.close();
			}
		} catch (IOException e) {
			tinyUrl = "ERROR";
		}
		return tinyUrl;
	}
    
    private void storeAccessToken(String token, String tokenSecret){
    	VibeVault.OAuth_Token = token;
    	VibeVault.OAuth_Token_Secret = tokenSecret;
    }
    
	private void clearCredentials() {
		VibeVault.OAuth_Token = null;
		VibeVault.OAuth_Token_Secret = null;
	}
	
	private void checkLoggedInUpdateGUI(){
		String token = VibeVault.OAuth_Token;
		String secret = VibeVault.OAuth_Token_Secret;
		if(token != null && token.length()!=0 && secret != null && secret.length()!=0){
			getConsumerProvider();
			mLoginButton.setEnabled(false);
			mPostButton.setEnabled(true);
		} else{
			mLoginButton.setEnabled(true);
			mPostButton.setEnabled(false);
			return;
		}
	}
}