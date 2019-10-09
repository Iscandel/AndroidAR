package com.arapp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.RadioButton;

import com.ar.renderer.ARRenderer;

/**
 *
 */
public class ModelSettingsDlg extends DialogFragment {
    protected DialogResultListener myListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = requireActivity().getLayoutInflater();


        builder.setView(inflater.inflate(R.layout.model_settings_dialog, null))
                .setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        myListener.onDialogPositiveClick(com.arapp.ModelSettingsDlg.this);
                    }
                })
                .setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        com.arapp.ModelSettingsDlg.this.getDialog().cancel();
                    }
                });
        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            myListener = (DialogResultListener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(context.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        Bundle bundle = getArguments();

        int modelType = bundle.getInt("modelType", 0);
        if(modelType == ARRenderer.ModelType.FOX.ordinal()) {
            RadioButton radio = getDialog().findViewById(R.id.radioFoxModel);
            radio.setChecked(true);
        } else {
            RadioButton radio = getDialog().findViewById(R.id.radioDonaModel);
            radio.setChecked(true);
        }
    }


    public ARRenderer.ModelType getSelectedModel() {
        RadioButton radio = getDialog().findViewById(R.id.radioFoxModel);
        if(radio.isChecked())
            return ARRenderer.ModelType.FOX;
        else
            return ARRenderer.ModelType.DONA;
    }
}

