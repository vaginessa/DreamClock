package com.raidzero.dreamclock;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

/**
 * Created by posborn on 3/30/15.
 */
public class DialogUtility {
    private static final String tag = "DialogUtility";

    public interface DialogCallbacks {
        void onInputReceived(String value);
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
}
