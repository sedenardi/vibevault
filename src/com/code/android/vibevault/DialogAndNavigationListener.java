package com.code.android.vibevault;

import android.os.Bundle;

public interface DialogAndNavigationListener {

	public void showLoadingDialog(String message);
	
	public void showDialog(String message, String title);
	
	public void hideDialog();
	
	public void showSettingsDialog(Bundle b);
	
	public void goHome();
	
}