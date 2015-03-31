package com.raidzero.dreamclock.data;

import java.util.ArrayList;

/**
 * Created by posborn on 3/31/15.
 */
public class DreamNotification {
    public String pkgName;
    public int iconId;
    public int number;

    public DreamNotification(String pkgName, int iconId, int number) {
        this.pkgName = pkgName;
        this.iconId = iconId;
        this.number = number;
    }

    /**
     * Searches a given list of DreamNotifications for a package name
     * @param notifications ArrayList of DreamNotifications
     * @param pkgName String - package name to search for
     * @return true/false
     */
    public static boolean contains(ArrayList<DreamNotification> notifications, String pkgName) {
        for (DreamNotification d : notifications) {
            if (pkgName.equals(d.pkgName)) {
                return true;
            }
        }

        return false;
    }
}
