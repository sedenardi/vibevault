package com.code.android.vibevault;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.os.Bundle;
import android.view.KeyEvent;

public class LoadingDialog extends DialogFragment {
	
	private static final String ARG_MESSAGE = "msg";
	private static final String ARG_TITLE = "useTitle";
	
	public static LoadingDialog newInstance(String message, boolean useTitle) {
		LoadingDialog dialog = new LoadingDialog();
		Bundle args = new Bundle();
		args.putString(ARG_MESSAGE, message);
		args.putBoolean(ARG_TITLE, useTitle);
		dialog.setArguments(args);
	    return dialog;
	}
	
	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState) {
		String message = getArguments().getString(ARG_MESSAGE);
		boolean useTitle = getArguments().getBoolean(ARG_TITLE);
	    final ProgressDialog dialog = new ProgressDialog(getActivity());
	    if (useTitle) {
	    	dialog.setTitle("Loading");
	    }
	    dialog.setMessage(message);
	    dialog.setIndeterminate(true);
	    dialog.setCancelable(false);
	    
	    // Disable the back button
 		OnKeyListener keyListener = new OnKeyListener() { 
 			@Override
 			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {				
 				return keyCode == KeyEvent.KEYCODE_BACK;
 			}		
 		};
	    
 		dialog.setOnKeyListener(keyListener);
	    return dialog;
	}


}
