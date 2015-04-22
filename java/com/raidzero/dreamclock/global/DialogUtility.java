package com.raidzero.dreamclock.global;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.raidzero.dreamclock.R;

/**
 * Created by posborn on 3/30/15.
 */
public class DialogUtility {
    private static final String tag = "DialogUtility";

    public interface DialogCallbacks {
        void onInputReceived(String value);
    }

    public interface ThresholdCallback {
        void onInputReceived(int lux, int brightness, int opacity);
    }

    public static void getUserInput(Context context, String title, int inputType) {
        final DialogCallbacks dialogCallbacks = (DialogCallbacks) context;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle(title);

        final EditText input = new EditText(context);
        input.setInputType(inputType);

        builder.setView(input);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String value = input.getText().toString();
                dialogCallbacks.onInputReceived(value);
            }
        });

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogCallbacks.onInputReceived(null);
            }
        });

        // create dialog
        AlertDialog dialog = builder.create();

        // show it & pop up keyboard
        dialog.show();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    public static void getThreshold(Context context, final ThresholdCallback callback, String title) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        final View v = View.inflate(context, R.layout.create_threshold, null);

        SeekBar brightnessSeekbar = (SeekBar) v.findViewById(R.id.seekBrightness);
        SeekBar opacitySeekbar = (SeekBar) v.findViewById(R.id.seekOpacity);
        opacitySeekbar.setProgress(100);
        opacitySeekbar.setRotation(180.0f);

        final TextView brightnessDisplay = (TextView) v.findViewById(R.id.threshold_brightnessDisplay);
        final TextView opacityDisplay = (TextView) v.findViewById(R.id.threshold_opacityDisplay);

        SeekBar.OnSeekBarChangeListener brightnessListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                brightnessDisplay.setText(String.valueOf(i) + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        };

        SeekBar.OnSeekBarChangeListener opacityListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                opacityDisplay.setText(String.valueOf(i) + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        };

        brightnessSeekbar.setOnSeekBarChangeListener(brightnessListener);
        opacitySeekbar.setOnSeekBarChangeListener(opacityListener);

        builder.setTitle(title);
        builder.setView(v);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                int lux = -1;
                int brightness = -1;
                int opacity = -1;
                try {
                    lux = Integer.valueOf(
                            ((EditText) v.findViewById(R.id.threshold_luxValue)).getText().toString());
                    brightness = ((SeekBar) v.findViewById(R.id.seekBrightness)).getProgress();
                    opacity = ((SeekBar) v.findViewById(R.id.seekOpacity)).getProgress();
                } catch (Exception e) {
                    // leave them as -1
                }

                callback.onInputReceived(lux, brightness, opacity);
            }
        });

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                callback.onInputReceived(-1, -1, -1);
            }
        });

        // create dialog
        AlertDialog dialog = builder.create();

        // show it
        dialog.show();

    }
}
