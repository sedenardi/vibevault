package com.code.android.vibevault;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;

public class DisplayDialog extends DialogFragment {
	
	public static DisplayDialog newInstance(String message, String title){
		DisplayDialog dialog = new DisplayDialog();
		Bundle args = new Bundle();
		args.putString("msg", message);
		args.putString("title", title);
		dialog.setArguments(args);
	    return dialog;
	}
	
	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState) {
		String message = getArguments().getString("msg");
		String title = getArguments().getString("title");
	    return new AlertDialog.Builder(getActivity()).setTitle(title).setMessage(message).create();
	}


}
