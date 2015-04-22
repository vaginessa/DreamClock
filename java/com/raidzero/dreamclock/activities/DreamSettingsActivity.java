package com.raidzero.dreamclock.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import com.raidzero.dreamclock.activities.AdjustBrightnessLevelsActivity;
import com.raidzero.dreamclock.services.NotificationMonitor;
import com.raidzero.dreamclock.R;

/**
 * Created by posborn on 3/16/15.
 */
public class DreamSettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String tag = "DreamSettingsActivity";

    private CheckBoxPreference mNotificationsEnabled, mAutoDim, mOpacity;
    private PreferenceScreen mPrefScreen;
    private PreferenceCategory mCatBrightness;
    private SharedPreferences mPrefs;
    private Preference mStaticBrightness; // custom preference
    private EditTextPreference mVariableOpacity;
    private Preference mAdjustThresholds; // dummy preference

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.dream_settings);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mPrefScreen = (PreferenceScreen) findPreference("pref_screen_main");
        mCatBrightness = (PreferenceCategory) findPreference("pref_cat_brightness");

        mAutoDim = (CheckBoxPreference) findPreference("pref_auto_dim");
        mOpacity = (CheckBoxPreference) findPreference("pref_opacity");
        mAdjustThresholds = findPreference("pref_saved_thresholds");
        mVariableOpacity = (EditTextPreference) findPreference("pref_variable_opacity");
        mStaticBrightness = findPreference("pref_static_brightness");

        mNotificationsEnabled = (CheckBoxPreference) findPreference("pref_include_notifications");

        // show/hide screen brightness based on whats set
        if (!mPrefs.getBoolean("pref_auto_dim", false)) { // no auto dimming
            mCatBrightness.removePreference(mAdjustThresholds);
            mCatBrightness.addPreference(mStaticBrightness);
        } else {
            mCatBrightness.addPreference(mAdjustThresholds);
            mCatBrightness.removePreference(mStaticBrightness);
        }
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

        if (key.equals("pref_auto_dim")) {
            if (!mPrefs.getBoolean(key, false)) { // no auto dimming
                mCatBrightness.removePreference(mAdjustThresholds);
                mCatBrightness.addPreference(mStaticBrightness);
            } else {
                mCatBrightness.addPreference(mAdjustThresholds);
                mCatBrightness.removePreference(mStaticBrightness);
            }
        }

        // just update the variable opacity summary
        if (key.equals("pref_variable_opacity")) {
            if (mPrefs.getBoolean("pref_opacity", false)) { // true means use variable
                int savedOffset = Integer.valueOf(mPrefs.getString("pref_variable_opacity", "-5"));
                mOpacity.setSummary(
                        String.format(getString(R.string.pref_opacity_main_variable_summary),
                                savedOffset));
            }
        }
    }

    /*
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        // launch adjust thresholds activity
        if (preference.getKey().equals("pref_saved_thresholds")) {
            Debug.Log(tag, "Adjust thresholds");
            Intent i = new Intent(this, AdjustBrightnessLevelsActivity.class);
            startActivity(i);
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }*/

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
