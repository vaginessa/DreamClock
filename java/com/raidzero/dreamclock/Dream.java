package com.raidzero.dreamclock;

import android.app.AlarmManager;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.service.dreams.DreamService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by posborn on 3/16/15.
 */
public class Dream extends DreamService implements SensorEventListener {
    private final static String tag = "Dream";

    private NotificationMonitor mNotificationMonitor;
    private SensorManager mSensorManager;
    private Sensor mLightSensor;
    private Window mWindow;
    private WindowManager mWindowManager;

    private SharedPreferences mPrefs;
    private TextView mDateDisplay, mAlarmDisplay, mChargeDisplay;

    private View mContentView, mSaverView;
    private LinearLayout mNotificationContainer;
    private Float mMaxOpacity;

    private Handler mHandler = new Handler();

    // receiver for notification updates
    private final BroadcastReceiver mNotificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Debug.Log(tag, "onReceive!");
            updateNotifications();
        }
    };

    // receiver for time changes
    private final BroadcastReceiver mDateTimeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(Intent.ACTION_TIME_TICK)
                    || action.equals(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED)) {
                updateDateDisplay();
            } else if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                updateBatteryDisplay();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        // set up prefs
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        setContentView(R.layout.dreamscape);

        // exit daydream when clicked
        setInteractive(false);
        setFullscreen(true);

        // set full screen (immersive)
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);

        mWindow = getWindow();
        mWindowManager = getWindowManager();

        mSaverView = findViewById(R.id.clock_container);
        mContentView = (View) mSaverView.getParent();

        mNotificationContainer = (LinearLayout) findViewById(R.id.notificationContainer);

        mDateDisplay = (TextView) findViewById(R.id.txt_dateDisplay);
        mAlarmDisplay = (TextView) findViewById(R.id.txt_nextAlarmDisplay);
        mChargeDisplay = (TextView)findViewById(R.id.txt_chargingDisplay);

        updateDateDisplay();
        updateBatteryDisplay();

        mMaxOpacity = (mPrefs.getInt("pref_max_opacity", 100) / 100.0f);
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();

        // register date receiver
        IntentFilter dateTimeFilter = new IntentFilter();
        dateTimeFilter.addAction(Intent.ACTION_TIME_TICK);
        dateTimeFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        dateTimeFilter.addAction(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED);

        registerReceiver(mDateTimeReceiver, dateTimeFilter);

        if (mPrefs.getBoolean("pref_auto_dim", false)) {
            // set up light sensor
            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

            // set up sensor listener
            mSensorManager.registerListener(this, mLightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        // register notification listener, if enabled
        if (mPrefs.getBoolean("pref_include_notifications", false)) {
            registerReceiver(mNotificationReceiver, new IntentFilter("com.raidzero.dreamclock.notifications_updated"));

            mNotificationMonitor = NotificationMonitor.getInstance();
            if (mNotificationMonitor != null) {
                Debug.Log(tag, "Asking for notifications right now");
                updateNotifications();
            }
        }

        // create move runnable and post it now that notifications have possibly been displayed
        Utils.ScreensaverMoveSaverRunnable moveSaverRunnable = new Utils.ScreensaverMoveSaverRunnable(mHandler);
        boolean slideAnim = mPrefs.getBoolean("pref_anim_slide", false);
        moveSaverRunnable.registerViews(mContentView, mSaverView, mMaxOpacity, slideAnim);
        mHandler.post(moveSaverRunnable);
    }

    @Override
    public void onDreamingStopped() {
        super.onDreamingStopped();

        if (mLightSensor != null) {
            mSensorManager.unregisterListener(this);
        }

        try {
            unregisterReceiver(mDateTimeReceiver);
            unregisterReceiver(mNotificationReceiver);
        } catch (Exception ignored) {
            // ignored
        }
    }



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

    private void setScreenBrightness(float currentLux) {

        float brightness;

        // TODO: use user-specifiable brightness constraints
        if (currentLux < 5) {
            brightness = 0.001f;
        } else if (currentLux >5 && currentLux < 40) {
            brightness = 0.5f;
        } else {
            brightness = 0.8f;
        }

        if (mWindow != null) {
            WindowManager.LayoutParams lp = mWindow.getAttributes();
            lp.screenBrightness = brightness;
            mWindow.setAttributes(lp);
            mWindowManager.updateViewLayout(mWindow.getDecorView(), lp);
        }

        // set opacity of the view
        mSaverView.setAlpha(mMaxOpacity);
    }

    private String getFormattedDate(String strDateFormat) {
        SimpleDateFormat sdfDate = new SimpleDateFormat(strDateFormat);
        Date now = new Date();
        String strDate = sdfDate.format(now);
        return strDate;
    }

    // adds notification icons with counts to the notification area
    synchronized private void updateNotifications() {
        ArrayList<StatusBarNotification> notifications = NotificationMonitor.getCurrentNotifications();

        Debug.Log(tag, "updateNotifications() got stuff: " + notifications.size());

        // first, clear out the notification view
        mNotificationContainer.removeAllViews();

        // keep a map of packages and their numbers
        HashMap<String, Integer> pkgCount = new HashMap<>();
        ArrayList<String> pkgs = new ArrayList<>();

        // count up notifications for packages
        for (StatusBarNotification sbn : notifications) {
            String pkgName = sbn.getPackageName();
            pkgs.add(pkgName);
            pkgCount.put(pkgName, countPackages(pkgs, pkgName));
        }

        // now make compound TextViews with this data and add them to notificationLayout
        for(Iterator<Map.Entry<String, Integer>> it = pkgCount.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Integer> entry = it.next();
            String pkgName = entry.getKey();
            int count = entry.getValue();
            Debug.Log(tag, String.format("%s: %d", pkgName, count));

            try {
                Context pkgContext = createPackageContext(pkgName, 0);
                Notification notification = getFirstNotificationForPkgName(notifications, pkgName);
                if (notification != null) {
                    int iconId = notification.icon;
                    Drawable icon = pkgContext.getResources().getDrawable(iconId);

                    // make custom view for superscripted icon! :)
                    NumberedIconView notifIconView = new NumberedIconView(this);
                    notifIconView.setIconAndCount(icon, count);

                    mNotificationContainer.addView(notifIconView);
                }
            } catch (PackageManager.NameNotFoundException e) {
                //ignore
            }
        }
    }

    // returns number of occurrences of string in a string array
    private int countPackages(ArrayList<String> packages, String pkgName) {
        int count = 0;
        for (String s : packages) {
            if (s.equals(pkgName)) {
                count++;
            }
        }

        return count;
    }

    // returns Notification for first found sbn that matches a given package name
    private Notification getFirstNotificationForPkgName(ArrayList<StatusBarNotification> sbns, String pkgName) {
        for (StatusBarNotification sbn : sbns) {
            if (sbn.getPackageName().equals(pkgName)) {
                return sbn.getNotification();
            }
        }

        return null;
    }

    private void updateDateDisplay() {
        // do date
        String dateTxt = getFormattedDate("EEE, MMM d");
        mDateDisplay.setText(dateTxt);

        // do alarm
        String nextAlarm = Settings.System.getString(getContentResolver(),
                Settings.System.NEXT_ALARM_FORMATTED);

        if (nextAlarm != null && !nextAlarm.isEmpty()) {
            mAlarmDisplay.setText(nextAlarm);
            mAlarmDisplay.setVisibility(View.VISIBLE);
        } else {
            mAlarmDisplay.setVisibility(View.GONE);
        }
    }

    private void updateBatteryDisplay() {
        mChargeDisplay.setText(Utils.getChargingStatus(this));
    }
}
