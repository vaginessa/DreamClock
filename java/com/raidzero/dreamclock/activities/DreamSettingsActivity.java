package com.raidzero.dreamclock.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

import com.raidzero.dreamclock.global.Debug;
import com.raidzero.dreamclock.services.NotificationMonitor;
import com.raidzero.dreamclock.R;

/**
 * Created by posborn on 3/16/15.
 */
public class DreamSettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String tag = "DreamSettingsActivity";

    private CheckBoxPreference mNotificationsEnabled;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.dream_settings);

        mNotificationsEnabled = (CheckBoxPreference) findPreference("pref_include_notifications");
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("pref_include_notifications") && NotificationMonitor.getInstance() == null) {
            if (sharedPreferences.getBoolean("pref_include_notifications", false)) {

                // show message before going to notification access screen
                new AlertDialog.Builder(this)
                        .setTitle(getResources().getString(R.string.notification_access_dialog_title))
                        .setMessage(getResources().getString(R.string.notification_access_dialog_message))
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent=new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                                startActivity(intent);
                            }
                        })
                        .show();
            }
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        // launch adjust thresholds activity
        if (preference.getKey().equals("pref_adjust_thresholds")) {
            Debug.Log(tag, "Adjust thresholds");
            Intent i = new Intent(this, AdjustBrightnessLevelsActivity.class);
            startActivity(i);
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onResume() {
        super.onResume();

        // uncheck the notification preference if notification access is disabled
        if (NotificationMonitor.getInstance() == null) {
            mNotificationsEnabled.setChecked(false);
        }

        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }
}
