package com.code.android.vibevault;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.AsyncFacebookRunner.RequestListener;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class FacebookCon extends Activity {
	
	public static final String TAG = "FACEBOOK CONNECT";
	public static final String APP_ID = "";
	private static final String[] PERMS = new String[] {"publish_stream" };
	private Button mLoginButton;
	private Button mPostButton;
	private EditText mMessage;
	private Handler mHandler = new Handler();
		
	private Vibrator vibrator;
	
	@Override
	public Object onRetainNonConfigurationInstance(){
		return mMessage.getText().toString();
	}
 
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.facebook_screen);
		
		
 
		// setup the facebook session
		if(VibeVault.mFacebook==null || VibeVault.mAsyncRunner ==null){

			VibeVault.mFacebook = new Facebook(APP_ID);
			VibeVault.mAsyncRunner = new AsyncFacebookRunner(VibeVault.mFacebook);
		}
		
		// setup the content view
		//initLayout();
		mLoginButton = (Button) findViewById(R.id.FBLoginButton);
		mPostButton = (Button) findViewById(R.id.FBPostButton);
		mMessage = (EditText) findViewById(R.id.FBPost);
		
		vibrator = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);
		
		if (VibeVault.mFacebook.isSessionValid()) {
			mLoginButton.setText("Logout");
			mPostButton.setEnabled(true);
		} else{
			mLoginButton.setText("Login");
			mPostButton.setEnabled(false);
		}
		mLoginButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				vibrator.vibrate(50);
				if (VibeVault.mFacebook.isSessionValid()) {
					Toast.makeText(getBaseContext(), "Logging out...", Toast.LENGTH_SHORT);
					AsyncFacebookRunner asyncRunner = new AsyncFacebookRunner(
							VibeVault.mFacebook);
					asyncRunner.logout(FacebookCon.this, new LogoutRequestListener());
				} else {
					VibeVault.mFacebook.authorize(FacebookCon.this, PERMS, new LoginDialogListener());
				}
			}
		});
		
		mPostButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				vibrator.vibrate(50);
				mHandler.post(new Runnable() {
					public void run() {
						Bundle parameters = new Bundle();
						parameters.putString("message", mMessage.getText().toString());
						VibeVault.mFacebook.dialog(FacebookCon.this, "stream.publish", parameters, new WallPostDialogListener());
					}
				});
			}
			
		});
		
		String showTitle = getIntent().getStringExtra("show_title");
		String showURL = getIntent().getStringExtra("show_url");
		if(showTitle != null && showURL !=null){
			mMessage.setText("Listening to " + showTitle + "...  Check it out: " + showURL);
		} else{
			Object retained = getLastNonConfigurationInstance();
			if(retained instanceof String){
				mMessage.setText((String)retained);
			}
		}
 
	}
	
 
	private class LoginDialogListener implements DialogListener {
 
		@Override
		public void onComplete(Bundle values) {
			Toast.makeText(getBaseContext(), "Facebook login successful...", Toast.LENGTH_SHORT).show();
			mLoginButton.setText("Logout");
			mPostButton.setEnabled(true);
		}
 
		@Override
		public void onFacebookError(FacebookError e) {
			mLoginButton.setText("Login");
			mPostButton.setEnabled(false);
		}
 
		@Override
		public void onError(DialogError e) {
			mLoginButton.setText("Login");
			mPostButton.setEnabled(false);
		}
 
		@Override
		public void onCancel() {
			mLoginButton.setText("Login");
			mPostButton.setEnabled(false);
		}
 
	}
	

	private class WallPostRequestListener implements com.facebook.android.AsyncFacebookRunner.RequestListener {

		@Override
		public void onComplete(String response, Object state) {
		}

		@Override
		public void onIOException(IOException e, Object state) {
		}

		@Override
		public void onFileNotFoundException(FileNotFoundException e,
				Object state) {
		}

		@Override
		public void onMalformedURLException(MalformedURLException e,
				Object state) {
		}

		@Override
		public void onFacebookError(FacebookError e, Object state) {
		}

	}
	
	private class WallPostDialogListener implements
			com.facebook.android.Facebook.DialogListener {

		/**
		 * Called when the dialog has completed successfully
		 */
		public void onComplete(Bundle values) {
			final String postId = values.getString("post_id");
			if (postId != null) {
				Toast.makeText(getBaseContext(), "Posted...", Toast.LENGTH_SHORT).show();
				VibeVault.mAsyncRunner.request(postId, new WallPostRequestListener());
			} else {
				Toast.makeText(getBaseContext(), "Facebook error...", Toast.LENGTH_SHORT).show();
			}
		}

		@Override
		public void onCancel() {
		}

		@Override
		public void onError(DialogError e) {
		}

		@Override
		public void onFacebookError(FacebookError e) {
		}
	}
 
	private class LogoutRequestListener implements RequestListener {
 
		@Override
		public void onComplete(String response, Object state) {
 
			// Dispatch on its own thread
			mHandler.post(new Runnable() {
				public void run() {
					mLoginButton.setText("Login");
					mPostButton.setEnabled(false);
				}
			});
		}
 
		@Override
		public void onIOException(IOException e, Object state) {
			// TODO Auto-generated method stub
 
		}
 
		@Override
		public void onFileNotFoundException(FileNotFoundException e,
				Object state) {
			// TODO Auto-generated method stub
 
		}
 
		@Override
		public void onMalformedURLException(MalformedURLException e,
				Object state) {
			// TODO Auto-generated method stub
 
		}
 
		@Override
		public void onFacebookError(FacebookError e, Object state) {
			// TODO Auto-generated method stub
 
		}
 
	}
 
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		VibeVault.mFacebook.authorizeCallback(requestCode, resultCode, data);
	}
}