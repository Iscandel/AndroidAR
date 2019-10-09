package com.arapp;

import android.support.v4.app.DialogFragment;

public interface DialogResultListener {
    void onDialogPositiveClick(DialogFragment dialog);
    void onDialogNegativeClick(DialogFragment dialog);

}
