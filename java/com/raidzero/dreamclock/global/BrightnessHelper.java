package com.raidzero.dreamclock.global;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;
import android.service.dreams.DreamService;
import android.view.Window;
import android.view.WindowManager;

import com.raidzero.dreamclock.data.Threshold;

import java.util.ArrayList;

/**
 * Created by posborn on 3/31/15.
 */
public class BrightnessHelper implements SensorEventListener {
    private final String tag = "BrightnessHelper";

    // class-related fields
    private boolean initialized;
    private ArrayList<Threshold> mThresholds = new ArrayList<>();

    private SharedPreferences mPrefs;
    private Window mWindow;
    private WindowManager mWindowManager;
    private SensorManager mSensorManager;

    private BrightnessCallbacks mCallback;
    // This class is a singleton, so keep an instance
    private static BrightnessHelper instance = null;

    /**
     * interface for callbacks
     */
    public interface BrightnessCallbacks {
        void onBrightnessChanged(float newBrightness);
    }

    /**
     * protected constructor so only this can instantiate
     */
    protected BrightnessHelper() {

    }

    /**
     * get an instance of the LuxHelper
     *
     * @return LuxHelper instance
     */
    public static BrightnessHelper getInstance() {
        if (instance == null) {
            instance = new BrightnessHelper();
        }

        return instance;
    }

    /**
     * sets up references to various system-level things
     *
     * @param context
     */
    public void setUp(Context context) {
        mCallback = (BrightnessCallbacks) context;

        // dream service
        DreamService dreamService = (DreamService) context;

        // sensor manager
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        // light sensor
        Sensor lightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        // sharedPrefs
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        // window manager
        mWindowManager = dreamService.getWindowManager();

        // window
        mWindow = dreamService.getWindow();

        // register light sensor listener
        mSensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);

        readSavedLuxThresholds();

        initialized = true;
    }

    /**
     * unregister the list sensor listener
     */
    public void unregisterLightSensorListener() {
        try {
            mSensorManager.unregisterListener(this);
        } catch (Exception e) {
            // ignored
        }
    }

    /**
     * reads saved lux thresholds from shared prefs
     */
    private void readSavedLuxThresholds() {
        // read saved lux thresholds
        String savedLuxThresholdsStr = mPrefs.getString("pref_saved_thresholds", Common.DEFAULT_BRIGHTNESS_CURVE);
        mThresholds.clear();

        Debug.Log(tag, "savedThresholdStr: " + savedLuxThresholdsStr);

        if (!savedLuxThresholdsStr.isEmpty()) {

            String[] luxData = savedLuxThresholdsStr.split("\\|");

            for (String data : luxData) {
                String[] lineData = data.split(":");

                mThresholds.add(new Threshold(
                        Integer.valueOf(lineData[0]),
                        Integer.valueOf(lineData[1]),
                        Integer.valueOf(lineData[2])
                ));
            }
        }
    }

    /**
     * determine best brightness level given a lux value
     *
     * @param value lux value
     * @return float resolved brightness value
     */
    private int[] getLevelForLuxValue(float value) {
        int luxValue = (int) value;

        int[] rtnData = new int[2]; // default value of half screen brightness

        int[] luxLevels = new int[mThresholds.size()];
        int[] brightnessLevels = new int[mThresholds.size()];
        int[] opacityLevels = new int[mThresholds.size()];

        for (int i = 0; i < mThresholds.size(); i++) {
            Threshold t = mThresholds.get(i);

            luxLevels[i] = t.lux();
            brightnessLevels[i] = t.brightness();
            opacityLevels[i] = t.opacity();
        }

        for (int i = 0; i < mThresholds.size(); i++) {
            int brightnessOffset = i;

            // get the next LOWEST value, if there is one
            if (i > 0) {
                brightnessOffset = i - 1;
            }

            int threshold = luxLevels[i];
            int brightness = brightnessLevels[brightnessOffset];
            int opacity = opacityLevels[brightnessOffset];

            rtnData[0] = brightness;
            rtnData[1] = opacity;

            if (luxValue <= threshold) {
                return rtnData;
            }
        }

        return rtnData;
    }

    /**
     * sets the screen brightness given a lux value
     * @param currentLux lux value
     */
    private void setScreenBrightness(float currentLux) {
        if (initialized) {
            int[] levelData = getLevelForLuxValue(currentLux);
            float brightness = levelData[0] / 100.0f;
            float opacity = levelData[1] / 100.0f;

            Debug.Log(tag, String.format("brightness: %f, opacity: %f", brightness, opacity));
            WindowManager.LayoutParams lp = mWindow.getAttributes();
            lp.screenBrightness = brightness;
            mWindow.setAttributes(lp);
            mWindowManager.updateViewLayout(mWindow.getDecorView(), lp);

            mCallback.onBrightnessChanged(opacity);
        }
    }

    /**
     * used to set brightness of a given window
     * @param dreamService dream service instance
     * @param brightness desired brightness
     */
    public static void setScreenBrightness(DreamService dreamService, float brightness) {
        Window window = dreamService.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.screenBrightness = brightness;
        window.setAttributes(lp);

        WindowManager windowManager = dreamService.getWindowManager();
        windowManager.updateViewLayout(window.getDecorView(), lp);
    }

    /**
     * SensorEvent callbacks follow
     */
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor.getType() == Sensor.TYPE_LIGHT)
        {
            float currentLux = sensorEvent.values[0];
            setScreenBrightness(currentLux);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // ignored
    }
}
