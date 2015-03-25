package com.raidzero.dreamclock;

import android.app.Notification;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by posborn on 3/16/15.
 */
public class NotificationMonitor extends NotificationListenerService {
    private static final String tag = "NotificationMonitor";

    private static ArrayList<StatusBarNotification> mNotifications = new ArrayList<>();
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

                // higher than min priority and not ongoing
                if (notification.priority > Notification.PRIORITY_MIN &&
                        ((Notification.FLAG_ONGOING_EVENT & notification.flags) == 0)) {
                    mNotifications.add(sbn);
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

    public static ArrayList<StatusBarNotification> getCurrentNotifications() {
        return mNotifications;
    }

}
