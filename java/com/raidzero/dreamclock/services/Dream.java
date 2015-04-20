package com.raidzero.dreamclock.services;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.service.dreams.DreamService;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.raidzero.dreamclock.global.BrightnessHelper;
import com.raidzero.dreamclock.global.Debug;
import com.raidzero.dreamclock.data.DreamNotification;
import com.raidzero.dreamclock.global.NumberedIconView;
import com.raidzero.dreamclock.R;
import com.raidzero.dreamclock.global.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by posborn on 3/16/15.
 */
public class Dream extends DreamService implements BrightnessHelper.BrightnessCallbacks {
    private final static String tag = "Dream";

    private NotificationMonitor mNotificationMonitor;

    private SharedPreferences mPrefs;
    private TextView mDateDisplay, mAlarmDisplay, mChargeDisplay;

    private View mContentView, mSaverView;
    private LinearLayout mNotificationContainer;
    private Float mStaticOpacity;

    private Handler mHandler = new Handler();

    private BrightnessHelper mBrightnessHelper;

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

        mSaverView = findViewById(R.id.clock_container);
        mContentView = (View) mSaverView.getParent();

        mNotificationContainer = (LinearLayout) findViewById(R.id.notificationContainer);

        mDateDisplay = (TextView) findViewById(R.id.txt_dateDisplay);
        mAlarmDisplay = (TextView) findViewById(R.id.txt_nextAlarmDisplay);
        mChargeDisplay = (TextView)findViewById(R.id.txt_chargingDisplay);

        updateDateDisplay();
        updateBatteryDisplay();

        mStaticOpacity = (mPrefs.getInt("pref_static_opacity", 100) / 100.0f);
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();

        // set up lux helper if brightness control is enabled
        if (mPrefs.getBoolean("pref_auto_dim", false)) {
            // set up lux helper
            mBrightnessHelper = BrightnessHelper.getInstance();
            mBrightnessHelper.setUp(this);
        } else {
            //set a static brightness
            int brightness = mPrefs.getInt("pref_static_brightness", 75);
            BrightnessHelper.setScreenBrightness(this, brightness / 100.0f);
        }

        // register date receiver
        IntentFilter dateTimeFilter = new IntentFilter();
        dateTimeFilter.addAction(Intent.ACTION_TIME_TICK);
        dateTimeFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        dateTimeFilter.addAction(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED);

        registerReceiver(mDateTimeReceiver, dateTimeFilter);

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
        moveSaverRunnable.registerViews(mContentView, mSaverView, mStaticOpacity, slideAnim);
        mHandler.post(moveSaverRunnable);

        mSaverView.setAlpha(mStaticOpacity);
    }

    @Override
    public void onDreamingStopped() {
        super.onDreamingStopped();

        if (mPrefs.getBoolean("pref_auto_dim", false)) {
            mBrightnessHelper.unregisterLightSensorListener();
        }

        try {
            unregisterReceiver(mDateTimeReceiver);
            unregisterReceiver(mNotificationReceiver);
        } catch (Exception ignored) {
            // ignored
        }
    }

    private String getFormattedDate(String strDateFormat) {
        SimpleDateFormat sdfDate = new SimpleDateFormat(strDateFormat);
        Date now = new Date();
        String strDate = sdfDate.format(now);
        return strDate;
    }

    // adds notification icons with counts to the notification area
    synchronized private void updateNotifications() {
        ArrayList<DreamNotification> notifications = NotificationMonitor.getCurrentNotifications();
        //NotificationMonitor.clearNotifications();

        Debug.Log(tag, "updateNotifications() got stuff: " + notifications.size());

        // first, clear out the notification view
        mNotificationContainer.removeAllViews();

        // now make NumberedIconViews with this data and add them to notificationLayout
        for (DreamNotification notification : notifications) {
            String pkgName = notification.pkgName;
            int count = notification.number;
            Debug.Log(tag, String.format("%s: %d", pkgName, count));

            try {
                Context pkgContext = createPackageContext(pkgName, 0);
                Drawable icon = pkgContext.getResources().getDrawable(notification.iconId);

                // make custom view for superscripted icon! :)
                NumberedIconView notifIconView = new NumberedIconView(this);
                notifIconView.setIconAndCount(icon, count);

                mNotificationContainer.addView(notifIconView);

            } catch (PackageManager.NameNotFoundException e) {
                //ignore
            }
        }
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

    @Override
    public void onBrightnessChanged(float newBrightness) {
        if (mPrefs.getBoolean("pref_opacity", false)) { // use variable opacity
            int offset = Integer.valueOf(mPrefs.getString("pref_variable_opacity", "-5"));

            float alpha = (newBrightness + (offset / 100.0f));
            mSaverView.setAlpha(alpha);
        }
    }
}
