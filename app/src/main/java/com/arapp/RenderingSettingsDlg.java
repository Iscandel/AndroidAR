package com.arapp;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

public class RenderingSettingsDlg extends Dialog {
    protected boolean myReturnValue;

    public RenderingSettingsDlg(Context context, boolean drawBorder, boolean drawFrame, boolean drawModel) {
        super(context);
        setContentView(R.layout.rendering_settings_dialog);

        myReturnValue = false;

        Button okButton = findViewById(R.id.renderingButtonOk);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myReturnValue = true;
                RenderingSettingsDlg.this.hide();
            }
        });
        Button cancelButton = findViewById(R.id.renderingButtonCancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myReturnValue = false;
                RenderingSettingsDlg.this.hide();
            }
        });

        CheckBox check = findViewById(R.id.checkDrawBorder);
        check.setChecked(drawBorder);

        check = findViewById(R.id.checkDrawFrame);
        check.setChecked(drawFrame);

        check = findViewById(R.id.checkDrawModel);
        check.setChecked(drawModel);
    }

    public boolean openModalDialog() {
        show();
        return myReturnValue;
    }

    public boolean isDrawBorder() {
        CheckBox check = findViewById(R.id.checkDrawBorder);
        return check.isChecked();
    }

    public boolean isDrawFrame() {
        CheckBox check = findViewById(R.id.checkDrawFrame);
        return check.isChecked();
    }

    public boolean isDrawModel() {
        CheckBox check = findViewById(R.id.checkDrawModel);
        return check.isChecked();
    }

}
