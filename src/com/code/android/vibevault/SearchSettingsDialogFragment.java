package com.code.android.vibevault;

import java.util.Calendar;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class SearchSettingsDialogFragment extends DialogFragment {

	protected static final String LOG_TAG = SearchSettingsDialogFragment.class.getName();

	public static final String SEARCH_SORT_CHOICES[] = { "Date", "Rating" };
	public static final String SEARCH_DATE_CHOICES[] = { "Anytime", "Before", "During", "After", "Specific Date" };
	public static final int ANYTIME = 0;
	public static final int BEFORE = 1;
	public static final int DURING = 2;
	public static final int AFTER = 3;
	public static final int SPECIFIC = 4;

	private SeekBar seek;
	private Spinner sortSpin;
	private Spinner dateSpin;
	private TextView seekValue;
	private Button okayButton;
//	private CheckBox saveBox;
	private EditText monthText;
	private EditText dayText;
	private EditText yearText;
	
	private int resultNumber;
	private int searchMonth = -1;
	private int searchDay = -1;
	private int searchYear = -1;
	private int dateTypePos;
	private String resultOrder;
	
	private StaticDataStore db;
	
	private SearchSettingsDialogInterface dialogListener;
	
	public static SearchSettingsDialogFragment newInstanceSearchSettingsDialogFragment(String resultType, int resultNumber, int dateTypePos, int resultMonth, int resultDay, int resultYear){
		SearchSettingsDialogFragment frag = new SearchSettingsDialogFragment();
		frag.resultOrder = resultType;
		frag.resultNumber = resultNumber;
		frag.searchMonth = resultMonth;
		frag.searchDay = resultDay;
		frag.searchYear = resultYear;
		frag.dateTypePos = dateTypePos;
		return frag;
	}

	// Ensures parent activity implements proper interfaces.
	// Called right before onCreate(), which is right before onCreateView().
	// http://developer.android.com/guide/topics/fundamentals/fragments.html#Lifecycle
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			dialogListener = (SearchSettingsDialogInterface) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement SearchSettingsDialogInterface");
		}
	}

	// Anything that uses this class will need to implement this interface.
	public interface SearchSettingsDialogInterface {
		void onSettingsOkayButtonPressed(String searchType, int numResults, int dateTypePos, int month, int day, int year);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		db = StaticDataStore.getInstance(getActivity());
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.scrollable_settings_dialog,
				container);
		v.setBackgroundResource(android.R.drawable.dialog_holo_dark_frame);
		seek = (SeekBar) v.findViewById(R.id.NumResultsSeekBar);
		seek.setProgress(Integer.valueOf(db.getPref("numResults")) - 10);
		sortSpin = (Spinner) v.findViewById(R.id.SortSpinner);
		dateSpin = (Spinner) v.findViewById(R.id.DateSpinner);
		seekValue = (TextView) v.findViewById(R.id.SeekBarValue);
		okayButton = (Button) v.findViewById(R.id.SettingsOkayButton);
		monthText = (EditText) v.findViewById(R.id.MonthText);
		dayText = (EditText) v.findViewById(R.id.DayText);
		yearText = (EditText) v.findViewById(R.id.DateText);
		seekValue.setText(db.getPref("numResults"));

		// Set the seek bar to its current value, and set up a Listener.
		seek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				seekValue.setText(String.valueOf(progress + 10));
				resultNumber = progress + 10;
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});
		
		resultNumber = seek.getProgress()+10;

		// Set up the spinner, and set up it's OnItemSelectedListener.
		ArrayAdapter<String> sortAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, SearchSettingsDialogFragment.SEARCH_SORT_CHOICES);
		sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		sortSpin.setAdapter(sortAdapter);
		int sortPos = 1;
		this.resultOrder = db.getPref("sortOrder");
		for (int i = 0; i < SearchSettingsDialogFragment.SEARCH_SORT_CHOICES.length; i++) {
			if (SearchSettingsDialogFragment.SEARCH_SORT_CHOICES[i].equals(resultOrder))
				sortPos = i;
		}
		sortSpin.setSelection(sortPos);
		sortSpin.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View view, int arg2, long arg3) {
				resultOrder = SEARCH_SORT_CHOICES[arg0.getSelectedItemPosition()];
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
		
		yearText.setText(searchYear>1800?String.valueOf(searchYear):"");
		monthText.setText(searchMonth>0?String.valueOf(searchMonth):"");
		dayText.setText(searchDay>0?String.valueOf(searchDay):"");		

		ArrayAdapter<String> dateAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item,SearchSettingsDialogFragment.SEARCH_DATE_CHOICES);
		dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		dateSpin.setAdapter(dateAdapter);
//		dateTypePos = Integer.valueOf(db.getPref("dateTypePos"));
		dateSpin.setSelection(dateTypePos);
		dateSpin.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View view,int arg2, long arg3) {
				dateTypePos = arg0.getSelectedItemPosition();
				if (dateTypePos == ANYTIME) {
					monthText.setEnabled(false);
					dayText.setEnabled(false);
					yearText.setEnabled(false);
				} else if (dateTypePos == BEFORE || dateTypePos == AFTER) {
					monthText.setEnabled(true);
					dayText.setEnabled(true);
					yearText.setEnabled(true);
				} else if (dateTypePos == DURING) {
					monthText.setEnabled(true);
					dayText.setEnabled(false);
					yearText.setEnabled(true);
				} else if (dateTypePos == SPECIFIC) {
					monthText.setEnabled(true);
					dayText.setEnabled(true);
					yearText.setEnabled(true);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
		
		this.okayButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(dateTypePos == BEFORE || dateTypePos == DURING || dateTypePos == AFTER || dateTypePos == SPECIFIC){
					if (yearText.getText().length() > 0) {
						try{
							searchYear = Integer.valueOf(yearText.getText().toString());
							if(searchYear<1800 || searchYear >Calendar.getInstance().get(Calendar.YEAR)){
								Toast.makeText(getActivity(), R.string.error_invalid_year_bounds_message_text, Toast.LENGTH_SHORT).show();
								return;
							}
						} catch(NumberFormatException e){
							Toast.makeText(getActivity(), R.string.error_text_number_conversion_message_text, Toast.LENGTH_SHORT).show();
							Logging.Log(LOG_TAG, "Enter a valid year.");
							return;
						}
						if (monthText.getText().length() > 0) {
							try{
								searchMonth = Integer.valueOf(monthText.getText().toString());
								if(searchMonth < 0 || searchMonth > 13){
									Toast.makeText(getActivity(), R.string.error_invalid_month_message_text, Toast.LENGTH_SHORT).show();
									return;
								}
							} catch(NumberFormatException e){
								Toast.makeText(getActivity(), R.string.error_text_number_conversion_message_text, Toast.LENGTH_SHORT).show();
								Logging.Log(LOG_TAG, "Enter a valid month.");
								return;
							} catch(IllegalArgumentException e) {
								Toast.makeText(getActivity(), R.string.error_invalid_month_message_text, Toast.LENGTH_SHORT).show();
								Logging.Log(LOG_TAG, e.toString());
								return;
							}
						}
						else {
							searchMonth = 0;
						}
					} else {
						Toast.makeText(getActivity(), R.string.error_invalid_year_bounds_message_text, Toast.LENGTH_SHORT).show();
						return;
					}
					if (dateTypePos == SPECIFIC) {
						if (dayText.getText().length() > 0 && monthText.getText().length() > 0) {
							try{
								searchDay = Integer.valueOf(dayText.getText().toString());
								if(searchDay < 0 || searchDay > 31){
									Toast.makeText(getActivity(), R.string.error_invalid_date_message_text, Toast.LENGTH_SHORT).show();
									return;
								}
							} catch(NumberFormatException e){
								Toast.makeText(getActivity(),R.string.error_text_number_conversion_message_text, Toast.LENGTH_SHORT).show();
								Logging.Log(LOG_TAG, "Enter a valid month.");
								return;
							} catch(IllegalArgumentException e){
								Toast.makeText(getActivity(), R.string.error_text_number_conversion_message_text, Toast.LENGTH_SHORT).show();
								Logging.Log(LOG_TAG, "Enter a valid month.");
								return;
							}
						} else {
							Toast.makeText(getActivity(), R.string.error_incomplete_date_message_text, Toast.LENGTH_SHORT).show();
							return;
						}
					}
				}
				savePreferencesToDB();
				dialogListener.onSettingsOkayButtonPressed(resultOrder, resultNumber, dateTypePos, searchMonth, searchDay, searchYear);
				getDialog().dismiss();
			}
		});
		this.getDialog().setTitle("Search Settings");
		return v;
	}
	
	private void savePreferencesToDB(){
		Logging.Log(LOG_TAG, "SAVING...");
		db.updatePref("sortOrder", resultOrder);
		db.updatePref("numResults", String.valueOf(resultNumber));
//		db.updatePref("dateResults", String.valueOf(resultDate));
//		db.updatePref("dateTypePos", String.valueOf(dateTypePos));
	}

}
