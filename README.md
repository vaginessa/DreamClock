#DreamClock
Android DayDream Clock which features advanced brightness/opacity control and Android notification (with count) support

###Help
* **Auto-dim**
    * Toggle on and off to enable automatic screen-dimming based on light sensor values.
    * If auto-dim is enabled, select **Adjust brightness thresholds and values** to edit light sensor thresholds and corresponding screen brightness levels.
    * You can configure up to 8 light sensor thresholds
    * Select **ADD** to add a new threshold, **REMOVE** to remove one. The sliders will react in such a way that it is not possible to create a representative curve that does not make sense. Select **SAVE** when finished. Use your device's back button to cancel.

* **Variable Opacity**
    * Toggle on to set an offset relative to the screen brightness for the opacity of the view. This is useful for AMOLED screens when screen brightness alone is not enough. For example, if the screen brightness is at 50%, an offset value of -10 would set the opacity to 40%.
    * Toggle off to set a static opacity value to be used.
* **Slide**
    * Instead of fading clock in and out when moving to a new position, slide it to the new position without fading.
* **Display Notifications**
    * Turn on to enable the display of system notifications. Note this will require enable notification access for DreamClock. It will automatically launch the settings screen where this can be enabled.

###Screenshots
![Screenshot](/screenshots/dreamclock.png?raw=true "Screenshot")