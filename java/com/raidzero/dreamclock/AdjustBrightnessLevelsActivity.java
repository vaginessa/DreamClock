package com.raidzero.dreamclock;


import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by posborn on 3/30/15.
 */
public class AdjustBrightnessLevelsActivity extends Activity implements View.OnClickListener, SensorEventListener, DialogUtility.DialogCallbacks, SeekBar.OnSeekBarChangeListener {
    private static final String tag = "AdjustBrightnessLevelsActivity";

    private SharedPreferences mPrefs;
    private Button mAddButton, mRemoveButton, mSaveButton;
    private LinearLayout mSeekbarContainer;
    private TextView mLuxDisplay, mNoValuesDisplay;

    // arrays of lux & brightness values
    private ArrayList<Integer> mLuxValues = new ArrayList<>();
    private ArrayList<Integer> mBrightnessValues = new ArrayList<>();

    // array of seekbar
    private ArrayList<SeekBar> mSeekBars = new ArrayList<>();
    // array of textview for seekbar labels
    private ArrayList<TextView> mLabelViews = new ArrayList<>();

    private static final int MAX_THRESHOLDS = 8; // 8 seems like a nice number

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.adjust_brightness_levels);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        mSeekbarContainer = (LinearLayout) findViewById(R.id.thresholds_container);

        mAddButton = (Button) findViewById(R.id.add_threshold);
        mRemoveButton = (Button) findViewById(R.id.remove_threshold);
        mSaveButton = (Button) findViewById(R.id.save_threshold);

        mLuxDisplay = (TextView) findViewById(R.id.txt_luxDisplay);
        mNoValuesDisplay = (TextView) findViewById(R.id.txt_noValuesDisplay);

        // register listeners
        mAddButton.setOnClickListener(this);
        mRemoveButton.setOnClickListener(this);
        mSaveButton.setOnClickListener(this);

        SensorManager mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        // set up sensor listener
        mSensorManager.registerListener(this, mLightSensor, SensorManager.SENSOR_DELAY_NORMAL);

        readSavedValues();
        updateButtonVisibilities();
    }

    @Override
    public void onClick(View view) {
        // add button
        if (view.getId() == mAddButton.getId()) {
            if (mSeekBars.size() < MAX_THRESHOLDS) {
                DialogUtility.getUserInput(this, "Lux Threshold", InputType.TYPE_CLASS_NUMBER);
            }
        }

        // remove button
        if (view.getId() == mRemoveButton.getId()) {
            Debug.Log(tag, "Remove!");
            SeekBar seekBar = popLastSeekbar();
            TextView textView = popLastLabel();

            // remove from arrays of lux values
            mLuxValues.remove(mLuxValues.size() - 1);

            // remove views
            mSeekbarContainer.removeView(textView);
            mSeekbarContainer.removeView(seekBar);
        }

        if (view.getId() == mSaveButton.getId()) {
            Debug.Log(tag, "Save!");

            writeSharedPrefs();
        }

        updateButtonVisibilities();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor.getType() == Sensor.TYPE_LIGHT)
        {
            float currentLux = sensorEvent.values[0];
            mLuxDisplay.setText(" " + String.valueOf(currentLux));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // dont care about this
    }

    @Override
    public void onInputReceived(String value) {
        if (value != null && !value.isEmpty()) {
            Debug.Log(tag, "Add! " + value);

            // store this value in the lux levels array
            mLuxValues.add(Integer.valueOf(value));

            TextView labelView = new TextView(this);
            labelView.setText(value + " lux:");
            labelView.setPadding(2, 0, 0, 0); // left pad

            // store this textview in array
            mLabelViews.add(labelView);

            SeekBar newSeekbar = new SeekBar(this);
            newSeekbar.setMax(100);

            // set this initial value, if necessary
            if (mSeekBars.size() > 0) {
                SeekBar lastSeekbar = mSeekBars.get(mSeekBars.size() - 1);
                int minValue = lastSeekbar.getProgress();
                newSeekbar.setProgress(minValue);
            }

            newSeekbar.setOnSeekBarChangeListener(this);
            newSeekbar.setTag(mLuxValues.size());

            // store this seekbar in array
            mSeekBars.add(newSeekbar);

            // add label and seekbar to view
            mSeekbarContainer.addView(labelView);
            mSeekbarContainer.addView(newSeekbar);

            updateButtonVisibilities();
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int seekBarTag = (int) seekBar.getTag();

        Debug.Log(tag, String.format("seekbar #%d value: %d", seekBarTag, progress));

        // adjust all seekbars above down, if necessary
        for (int i = seekBarTag - 1; i >= 0; i--) {
            SeekBar sb = mSeekBars.get(i);
            if (sb.getProgress() > progress) {
                sb.setProgress(progress);
            }
        }

        // now do below, up
        for (int i = seekBarTag; i < mSeekBars.size(); i++) {
            SeekBar sb = mSeekBars.get(i);
            if (sb.getProgress() < progress) {
                sb.setProgress(progress);
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // ignored
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // ingored
    }

    private SeekBar popLastSeekbar() {
        SeekBar sb = mSeekBars.get(mSeekBars.size() - 1);
        mSeekBars.remove(mSeekBars.size() - 1);
        return sb;
    }

    private TextView popLastLabel() {
        TextView tv = mLabelViews.get(mLabelViews.size() -1);
        mLabelViews.remove(mLabelViews.size() -1);
        return tv;
    }

    private void updateButtonVisibilities() {
        int numThresholds = mSeekBars.size();

        // show/hide add/remove buttons
        if (numThresholds == MAX_THRESHOLDS) {
            mAddButton.setVisibility(View.GONE);
        } else {
            mAddButton.setVisibility(View.VISIBLE);
        }

        if (numThresholds == 0) {
            mRemoveButton.setVisibility(View.GONE);
            mSaveButton.setVisibility(View.GONE);
        } else {
            mRemoveButton.setVisibility(View.VISIBLE);
            mSaveButton.setVisibility(View.VISIBLE);
        }
    }

    private void writeSharedPrefs() {
        // write lux thresholds to string
        String luxStr = "";

        for (int i = 0; i < mLuxValues.size(); i++) {
            SeekBar seekBar = mSeekBars.get(i);
            int luxValue = mLuxValues.get(i);
            int briValue = seekBar.getProgress();

            luxStr += String.format("%d:%d", luxValue, briValue);

            // add delimiter
            if (i < mLuxValues.size() - 1) {
                luxStr += "|";
            }
        }

        // write to shared prefs!
        SharedPreferences.Editor editor = mPrefs.edit();

        editor.putString("luxThresholds", luxStr);

        editor.apply();

        finish();
    }

    private void readSavedValues() {
        mLuxValues.clear();
        mBrightnessValues.clear();

        String luxStr = mPrefs.getString("luxThresholds", "");

        if (luxStr.equals("")) {
            return; // nothing to see here
        }

        String[] thresholds = luxStr.split("\\|");

        // convert strings to ints
        if (!luxStr.isEmpty()) {
            for (String s : thresholds) {
                int luxValue, briValue;
                String[] data = s.split(":");

                luxValue = Integer.valueOf(data[0]);
                briValue = Integer.valueOf(data[1]);

                mLuxValues.add(luxValue);
                mBrightnessValues.add(briValue);
            }
        }

        // at this point the two arrays are ready to go, lets make some sliders
        int i = 0;
        for (Integer luxValue : mLuxValues) {
            int briValue = mBrightnessValues.get(i++);
            TextView luxDisplay = new TextView(this);
            SeekBar seekBar = new SeekBar(this);

            luxDisplay.setText(String.format("%d lux:", luxValue));
            mLabelViews.add(luxDisplay);

            seekBar.setProgress(briValue);
            seekBar.setTag(i);
            seekBar.setOnSeekBarChangeListener(this);

            mSeekBars.add(seekBar);

            mSeekbarContainer.addView(luxDisplay);
            mSeekbarContainer.addView(seekBar);
        }
    }

}
