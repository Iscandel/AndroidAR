package com.arapp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.widget.CheckBox;

public class RenderingSettingsDlg extends DialogFragment {
    protected DialogResultListener myListener;


//    public RenderingSettingsDlg()boolean drawBorder, boolean drawFrame, boolean drawModel) {
//
//        Button okButton = findViewById(R.id.renderingButtonOk);
//        okButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                myReturnValue = true;
//                RenderingSettingsDlg.this.hide();
//            }
//        });
//        Button cancelButton = findViewById(R.id.renderingButtonCancel);
//        cancelButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                myReturnValue = false;
//                RenderingSettingsDlg.this.hide();
//            }
//        });
//
//        CheckBox check = findViewById(R.id.checkDrawBorder);
//        check.setChecked(drawBorder);
//
//        check = findViewById(R.id.checkDrawFrame);
//        check.setChecked(drawFrame);
//
//        check = findViewById(R.id.checkDrawModel);
//        check.setChecked(drawModel);
//    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(inflater.inflate(R.layout.rendering_settings_dialog, null))
                // Add action buttons
                .setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        myListener.onDialogPositiveClick(RenderingSettingsDlg.this);
                    }
                })
                .setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        RenderingSettingsDlg.this.getDialog().cancel();
                    }
                });
        return builder.create();
    }

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the DialogResultListener so we can send events to the host
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

        boolean drawBorder = bundle.getBoolean("drawBorder", true);
        CheckBox check = getDialog().findViewById(R.id.checkDrawBorder);
        check.setChecked(drawBorder);

        boolean drawFrame = bundle.getBoolean("drawFrame", true);
        check = getDialog().findViewById(R.id.checkDrawFrame);
        check.setChecked(drawFrame);

        boolean drawModel = bundle.getBoolean("drawModel", true);
        check = getDialog().findViewById(R.id.checkDrawModel);
        check.setChecked(drawModel);

        boolean openGL = bundle.getBoolean("openGL", true);
        check = getDialog().findViewById(R.id.checkOpenGL);
        check.setChecked(openGL);
    }


    public boolean isDrawBorder() {
        CheckBox check = getDialog().findViewById(R.id.checkDrawBorder);
        return check.isChecked();
    }

    public boolean isDrawFrame() {
        CheckBox check = getDialog().findViewById(R.id.checkDrawFrame);
        return check.isChecked();
    }

    public boolean isDrawModel() {
        CheckBox check = getDialog().findViewById(R.id.checkDrawModel);
        return check.isChecked();
    }

    public boolean isOpenGLRenderer() {
        CheckBox check = getDialog().findViewById(R.id.checkOpenGL);
        return check.isChecked();
    }
}
