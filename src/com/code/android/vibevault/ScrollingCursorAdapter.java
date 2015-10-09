package com.code.android.vibevault;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ScrollingCursorAdapter extends CursorAdapter{

	public ScrollingCursorAdapter(Context context, Cursor c) {
		super(context, c);
	}
	
	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		LayoutInflater inflater = LayoutInflater.from(context);
		View v = inflater.inflate(R.layout.search_list_row, null, false);
		return v;
	}
	
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		TextView artistText = (TextView) view.findViewById(R.id.ArtistText);
		TextView showText = (TextView) view.findViewById(R.id.ShowText);
		ImageView ratingsIcon = (ImageView) view.findViewById(R.id.rating);
		String artist = cursor.getString(cursor.getColumnIndex(DataStore.SHOW_ARTIST));
		String showTitle = cursor.getString(cursor.getColumnIndex(DataStore.SHOW_TITLE));
		artistText.setText(artist + " ");
		showText.setText(showTitle);
		artistText.setSelected(true);
		showText.setSelected(true);
		ratingsIcon.setVisibility(View.GONE);
	}
}