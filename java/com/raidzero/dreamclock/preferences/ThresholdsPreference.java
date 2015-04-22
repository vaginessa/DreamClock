package com.raidzero.dreamclock.preferences;

import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.raidzero.dreamclock.R;
import com.raidzero.dreamclock.data.Threshold;
import com.raidzero.dreamclock.global.Common;
import com.raidzero.dreamclock.global.Debug;
import com.raidzero.dreamclock.global.DialogUtility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.WeakHashMap;

/**
 * Created by posborn on 4/21/15.
 */
public class ThresholdsPreference extends DialogPreference implements
        SensorEventListener, View.OnClickListener, DialogUtility.ThresholdCallback,
        SeekBar.OnSeekBarChangeListener, DialogInterface.OnClickListener {

    private static final String tag = "ThresholdsPreference";

    // textview - light sensor value
    private TextView mLightSensorReadout;

    // list of Thresholds
    private ArrayList<Threshold> mThresholds = new ArrayList<>();

    // thresholds container
    private LinearLayout mThresholdsContainer;

    // connect thresholds to seekbars
    private WeakHashMap<SeekBar, Threshold> mBrightnessMap = new WeakHashMap<>();
    private WeakHashMap<SeekBar, Threshold> mOpacityMap = new WeakHashMap<>();

    // connect seekbars to threshodss
    private WeakHashMap<Threshold, SeekBar> mBrightnessThresholdMap = new WeakHashMap<>();
    private WeakHashMap<Threshold, SeekBar> mOpacityThresholdMap = new WeakHashMap<>();

    // connect seekbars to textviews
    private WeakHashMap<SeekBar, TextView> mBrightmessTextMap = new WeakHashMap<>();
    private WeakHashMap<SeekBar, TextView> mOpacityTextMap = new WeakHashMap<>();

    // array of brightness seekbars
    private ArrayList<SeekBar> mBrightnessSeekbars = new ArrayList<>();

    // add/remove buttons
    private Button mAddBtn, mRemoveBtn;

    public ThresholdsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setPersistent(true);
        setDialogLayoutResource(R.layout.adjust_brightness_levels);
    }

    @Override
    public void onBindDialogView(View v) {
        // set up light sensor listener
        SensorManager sensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        Sensor lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        // set up sensor listener
        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);

        mLightSensorReadout = (TextView) v.findViewById(R.id.txt_luxDisplay);
        mAddBtn = (Button) v.findViewById(R.id.add_threshold);
        mRemoveBtn = (Button) v.findViewById(R.id.remove_threshold);
        mThresholdsContainer = (LinearLayout) v.findViewById(R.id.thresholds_container);

        // button click listeners
        mAddBtn.setOnClickListener(this);
        mRemoveBtn.setOnClickListener(this);

        // read the thresholds
        mThresholds.clear();
        String prefStr = getPersistedString(Common.DEFAULT_BRIGHTNESS_CURVE);

        Debug.Log(tag, "loading str: " + prefStr);

        String[] prefData = prefStr.split("\\|");

        for (String tData : prefData) {
            String[] t = tData.split(":");

            mThresholds.add(new Threshold(
                    Integer.valueOf(t[0]), Integer.valueOf(t[1]), Integer.valueOf(t[2])));
        }

        drawThresholds();

        super.onBindDialogView(v);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor.getType() == Sensor.TYPE_LIGHT)
        {
            float currentLux = sensorEvent.values[0];
            mLightSensorReadout.setText(" " + String.valueOf(currentLux));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // ignore
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == mAddBtn.getId()) {
            DialogUtility.getThreshold(getContext(), this, "Define a threshold");
        } else if (view.getId() == mRemoveBtn.getId()) {
            mThresholds.remove(mThresholds.size() - 1);
            drawThresholds();
        }
    }

    // save the thresholds in a string
    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            // build a string from the thresholds and persist it
            Debug.Log(tag, "OK clicked");
            String prefStr = "";

            for (int i = 0; i < mThresholds.size(); i++) {
                Threshold t = mThresholds.get(i);
                SeekBar opacityBar = mOpacityThresholdMap.get(t);
                SeekBar brightnessBar = mBrightnessThresholdMap.get(t);

                prefStr += String.format("%d:%d:%d", t.lux(),
                        brightnessBar.getProgress(), opacityBar.getProgress());

                if (i < mThresholds.size() - 1) {
                    prefStr += "|";
                }
            }

            Debug.Log(tag, "saving str: " + prefStr);
            persistString(prefStr);
        }

    }

    private void drawThresholds() {
        mThresholdsContainer.removeAllViews();
        Collections.sort(mThresholds); // sort :)

        int i = 0;
        for (Threshold t : mThresholds) {
            View tv = View.inflate(getContext(), R.layout.threshold, null);

            TextView luxTextView = (TextView) tv.findViewById(R.id.threshold_luxDisplay);
            TextView brightnessTextView = (TextView) tv.findViewById(R.id.threshold_brightnessDisplay);
            TextView opacityTextView = (TextView) tv.findViewById(R.id.threshold_opacityDisplay);
            SeekBar opacitySeekbar = (SeekBar) tv.findViewById(R.id.threshold_seekOpacity);
            SeekBar brightnessSeekbar = (SeekBar) tv.findViewById(R.id.threshold_seekBrightess);

            luxTextView.setText(String.valueOf(t.lux()));
            brightnessTextView.setText(String.valueOf(t.brightness()) + "%");
            opacityTextView.setText(String.valueOf(t.opacity()) + "%");

            brightnessSeekbar.setTag("brightness " + ++i);
            brightnessSeekbar.setProgress(t.brightness());
            opacitySeekbar.setTag("opacity");
            opacitySeekbar.setProgress(t.opacity());
            opacitySeekbar.setRotation(180.0f);

            brightnessSeekbar.setOnSeekBarChangeListener(this);
            opacitySeekbar.setOnSeekBarChangeListener(this);

            // remember brightness seekbar
            mBrightnessSeekbars.add(brightnessSeekbar);

            // add seekbars to map
            mBrightnessMap.put(brightnessSeekbar, t);
            mOpacityMap.put(opacitySeekbar, t);

            // add thresholds to map
            mBrightnessThresholdMap.put(t, brightnessSeekbar);
            mOpacityThresholdMap.put(t, opacitySeekbar);

            mBrightmessTextMap.put(brightnessSeekbar, brightnessTextView);
            mOpacityTextMap.put(opacitySeekbar, opacityTextView);

            mThresholdsContainer.addView(tv);
        }
    }

    @Override
    public void onInputReceived(int lux, int brightness, int opacity) {
        Log.d("tag", "got stuff: " + String.format("%d, %d, %d", lux, brightness, opacity));

        if (lux == -1) {
            return; // dont do anything
        }

        if (!Threshold.luxExists(lux)) {
            Threshold.addLuxThreshold(lux);

            mThresholds.add(new Threshold(lux, brightness, opacity));
            drawThresholds();
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
        // update the corresponding readout view and slide brightness seekbars around
        String tag = seekBar.getTag().toString();

        if (tag.startsWith("brightness")) {
            mBrightmessTextMap.get(seekBar).setText(progress + "%");

            /*
            String[] tagData = tag.split(" ");
            int index = Integer.valueOf(tagData[1]);

            // adjust all seekbars above down, if necessary
            for (int i =  index - 1; i >= 0; i--) {
                SeekBar sb = mBrightnessSeekbars.get(i);
                if (sb.getProgress() > progress) {
                    sb.setProgress(progress);
                }
            }

            // now do below, up
            for (int i = index; i < mBrightnessSeekbars.size(); i++) {
                SeekBar sb = mBrightnessSeekbars.get(i);
                if (sb.getProgress() < progress) {
                    sb.setProgress(progress);
                }
            }
            */

        } else if (tag.equals("opacity")) {
            mOpacityTextMap.get(seekBar).setText(progress + "%");

        }

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // ignored
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // ignored
    }
}
