package com.raidzero.dreamclock.services;

import android.app.Notification;
import android.content.Intent;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.raidzero.dreamclock.global.Debug;
import com.raidzero.dreamclock.data.DreamNotification;

import java.util.ArrayList;

/**
 * Created by posborn on 3/16/15.
 */
public class NotificationMonitor extends NotificationListenerService {
    private static final String tag = "NotificationMonitor";

    private static ArrayList<DreamNotification> mNotifications = new ArrayList<>();
    public static NotificationMonitor instance;

    public static NotificationMonitor getInstance() {
        return instance;
    }

    // make it known that the listener is registered
    @Override
    public void onCreate() {
        super.onCreate();
        Debug.Log(tag, "onCreate()");
        instance = this;
    }

    // make it known the service is not registered
    @Override
    public void onDestroy() {
        super.onDestroy();
        Debug.Log(tag, "onDestroy()");
        instance = null;
    }

    @Override
         public IBinder onBind(Intent intent) {
        Debug.Log(tag, "onBind()");
        return super.onBind(intent);
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Debug.Log(tag, "onListenerConnected()");
        getNotifications();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        getNotifications();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        getNotifications();
    }

    private void getNotifications() {
        mNotifications.clear();
        StatusBarNotification[] currentNotifications = getActiveNotifications();

        if (currentNotifications != null) {

            // filter out the non interesting stuff
            for (StatusBarNotification sbn : currentNotifications) {

                Notification notification = sbn.getNotification();
                String pkgName = sbn.getPackageName();
                int iconId = notification.icon;
                int number = notification.number;
                int flags = notification.flags;

                // higher than min priority and not ongoing
                if (notification.priority > Notification.PRIORITY_MIN &&
                        ((Notification.FLAG_ONGOING_EVENT & flags) == 0)) {

                    // only add to list if we don't already have this package name
                    if (!DreamNotification.contains(mNotifications, pkgName)) {

                        // always 1
                        if (number == 0) {
                            number = 1;
                        }

                        Debug.Log(tag, "notification: " + sbn.getPackageName() + ": " + number);

                        // add filtered notification
                        mNotifications.add(new DreamNotification(pkgName, iconId, number));
                    }
                }
            }
        } else {
            Debug.Log(tag, "currentNotifications null");
        }

        broadcastNotifications();
    }

    public void broadcastNotifications() {
        Log.d(tag, "updatePkgNames() " + mNotifications.size());
        if (mNotifications.size() > 0) {
            Intent broadcastIntent = new Intent("com.raidzero.dreamclock.notifications_updated");
            sendBroadcast(broadcastIntent);
            Debug.Log(tag, "broadcasted stuff!");
        }
    }

    public static ArrayList<DreamNotification> getCurrentNotifications() {
        return mNotifications;
    }

}
