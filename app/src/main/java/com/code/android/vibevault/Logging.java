package com.code.android.vibevault;
import android.util.Log;

public class Logging {

	public static final boolean enabled = true;
	
	public static void Log(String LOG_TAG, String message) {
		if (enabled)
			Log.d(LOG_TAG, message);
	}
	
	public static void Log(String LOG_TAG, boolean b) {
		Log(LOG_TAG, String.valueOf(b));
	}
	
	public static void Log(String LOG_TAG, int i) {
		Log(LOG_TAG, String.valueOf(i));
	}
	
}
