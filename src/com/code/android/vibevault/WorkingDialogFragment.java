package com.code.android.vibevault;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.os.Bundle;

public class WorkingDialogFragment extends DialogFragment {

	static WorkingDialogFragment newInstance(String s) {
        WorkingDialogFragment f = new WorkingDialogFragment();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putString("message", s);
        f.setArguments(args);

        return f;
    }
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setRetainInstance(true);
	}
	
	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        
	}
	
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
		return new ProgressDialog.Builder(getActivity()).setMessage(getArguments().getString("message")).create();
    }


	
}
