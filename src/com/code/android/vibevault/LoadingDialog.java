package com.code.android.vibevault;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.os.Bundle;

public class LoadingDialog extends DialogFragment {
	
	public static LoadingDialog newInstance(String message){
		LoadingDialog dialog = new LoadingDialog();
		Bundle args = new Bundle();
		args.putString("msg", message);
		dialog.setArguments(args);
	    return dialog;
	}
	
	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState) {
		String message = getArguments().getString("msg");
	    final ProgressDialog dialog = new ProgressDialog(getActivity());
	    dialog.setTitle("Loading");
	    dialog.setMessage(message);
	    dialog.setIndeterminate(true);
	    dialog.setCancelable(false);
	    return dialog;
	}


}
