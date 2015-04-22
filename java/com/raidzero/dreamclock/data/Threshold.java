package com.raidzero.dreamclock.data;

import java.util.ArrayList;

/**
 * Created by posborn on 4/21/15.
 */
public class Threshold implements Comparable<Threshold> {
    private int lux, brightness, opacity;

    private static ArrayList<Integer> mLuxValues = new ArrayList<>();

    public Threshold(int lux, int brightness, int opacity) {
        mLuxValues.add(lux);

        this.lux = lux;
        this.brightness = brightness;
        this.opacity = opacity;
    }

    public static boolean luxExists(int lux) {
        return mLuxValues.contains(lux);
    }

    public static void addLuxThreshold(int lux) {
        mLuxValues.add(lux);
    }

    public void setBrightness(int brightness) {
        this.brightness = brightness;
    }

    public void setOpacity(int opacity) {
        this.opacity = opacity;
    }

    public int lux() {
        return lux;
    }

    public int brightness() {
        return brightness;
    }

    public int opacity() {
        return opacity;
    }

    @Override
    public int compareTo(Threshold threshold) {
        return this.lux - threshold.lux();
    }
}
