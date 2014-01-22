package org.dhappy.habits;

import java.util.Calendar;
import org.dhappy.habits.R;
import org.dhappy.habits.contentprovider.HabitContentProvider;
import org.dhappy.habits.database.EventTable;
import org.dhappy.habits.database.GoalTable;
import org.dhappy.habits.database.HabitTable;

import android.app.Activity;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.util.Log;

/*
 * HabitDetailActivity allows to enter a new habit item 
 * or to change an existing
 */
public class EventDetailActivity extends Activity
	implements AdapterView.OnItemSelectedListener {
  private Spinner mHabitSelect;
  private EditText mDescriptionText;
  private TimePicker mEventTime;
  private DatePicker mEventDate;

  private Uri eventUri;
  private long habitId;
  SimpleCursorAdapter mAdapter;
  
  @Override
  protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    setContentView(R.layout.goal_edit);

    mHabitSelect = (Spinner) findViewById(R.id.habit);
    mEventTime = (TimePicker) findViewById(R.id.event_time);
    mEventDate = (DatePicker) findViewById(R.id.event_date);
    mDescriptionText = (EditText) findViewById(R.id.habit_edit_description);
    Button confirmButton = (Button) findViewById(R.id.habit_edit_button);
    Button cancelButton = (Button) findViewById(R.id.habit_cancel_button);

    String[] queryCols = new String[] { HabitTable.TABLE_HABIT + "." + HabitTable.COLUMN_ID, HabitTable.COLUMN_COLOR, HabitTable.COLUMN_NAME };
    String[] from = new String[] { HabitTable.COLUMN_COLOR, HabitTable.COLUMN_NAME };
    int[] to = new int[] { R.id.color_block, R.id.label };

    Cursor cursor = getContentResolver().query(HabitContentProvider.HABITS_URI, queryCols, null, null, null);
    mAdapter = new SimpleCursorAdapter(this, R.layout.habit_select_row, cursor, from, to, 0);

    mAdapter.setViewBinder(new ViewBinder() {
		@Override
		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
			if(columnIndex == 1) { // Time
				view.setBackgroundColor(Color.parseColor(cursor.getString(columnIndex)));
			 	return true;
			}

			return false;
		}
    });

    mHabitSelect.setAdapter(mAdapter);
    
    mHabitSelect.setOnItemSelectedListener(this);
    
    Bundle extras = getIntent().getExtras();

    // Check from the saved Instance
    eventUri = (bundle == null) ? null : (Uri) bundle.getParcelable(HabitContentProvider.EVENT_CONTENT_ITEM_TYPE);

    // Or passed from the other activity
    if (extras != null) {
      eventUri = extras.getParcelable(HabitContentProvider.EVENT_CONTENT_ITEM_TYPE);

      fillData(eventUri);
    }

    confirmButton.setOnClickListener(new View.OnClickListener() {
        public void onClick(View view) {
          setResult(RESULT_OK);
          saveState();
          finish();
        }
      });

  	cancelButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
      	setResult(RESULT_CANCELED);
      	finish();
      }
    });
  }

  private void fillData(Uri uri) {
    String[] projection = { GoalTable.COLUMN_HABIT_ID, EventTable.COLUMN_TIME, EventTable.TABLE_EVENT + "." + EventTable.COLUMN_DESCRIPTION };
    Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
    if (cursor != null) {
      cursor.moveToFirst();

      int habitId = cursor.getInt(cursor.getColumnIndexOrThrow(GoalTable.COLUMN_HABIT_ID));
      for(int pos = mAdapter.getCount(); pos >= 0; pos--) {
    	  if(mAdapter.getItemId(pos) == habitId) {
    		  mHabitSelect.setSelection(pos);
    		  break;
    	  }
      }
      
      mDescriptionText.setText(cursor.getString(cursor.getColumnIndexOrThrow(EventTable.COLUMN_DESCRIPTION)));
      
      Calendar eventTime = Calendar.getInstance();
      long seconds = cursor.getInt(cursor.getColumnIndexOrThrow(EventTable.COLUMN_TIME));
      eventTime.setTimeInMillis(seconds * 1000);
      
      mEventDate.updateDate(eventTime.get(Calendar.YEAR),
    		  eventTime.get(Calendar.MONTH),
    		  eventTime.get(Calendar.DAY_OF_MONTH));
      mEventTime.setCurrentHour(eventTime.get(Calendar.HOUR_OF_DAY));
      mEventTime.setCurrentMinute(eventTime.get(Calendar.MINUTE));
      
      cursor.close();
    }
  }

  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    saveState();
    outState.putParcelable(HabitContentProvider.GOAL_CONTENT_ITEM_TYPE, eventUri);
  }

  @Override
  protected void onPause() {
    super.onPause();
  }

  private void saveState() {
    String description = mDescriptionText.getText().toString();

    Calendar eventTime = Calendar.getInstance();
    eventTime.set(mEventDate.getYear(),
    			  mEventDate.getMonth(),
    			  mEventDate.getDayOfMonth(),
    			  mEventTime.getCurrentHour(),
    			  mEventTime.getCurrentMinute());
    
    ContentValues values = new ContentValues();	
    values.put(EventTable.COLUMN_HABIT_ID, habitId);
    values.put(EventTable.COLUMN_TIME, Math.floor(eventTime.getTimeInMillis() / 1000));
    values.put(EventTable.COLUMN_DESCRIPTION, description);

    if (eventUri == null) {
      // New habit
      eventUri = getContentResolver().insert(HabitContentProvider.EVENTS_URI, values);
    } else {
      // Update habit
      getContentResolver().update(eventUri, values, null, null);
    }

    Log.w(EventDetailActivity.class.getName(), "Event Time: " + eventTime);
  }
  
  @Override
  public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
	  habitId = id;
  }

  @Override
  public void onNothingSelected(AdapterView<?> arg0) {
  }
}