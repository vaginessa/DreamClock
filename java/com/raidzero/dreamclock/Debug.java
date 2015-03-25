package com.raidzero.dreamclock;

import android.util.Log;

/**
 * Created by posborn on 3/25/15.
 */
public class Debug {
    private static final boolean debug = BuildConfig.DEBUG;

    public static void Log(String tag, String msg) {
        if (debug) {
            Log.d(tag, msg);
        }
    }

    public static void Log(String tag, String msg, Exception e) {
        if (debug) {
            Log.d(tag, msg);
            e.printStackTrace();
        }
    }
}
