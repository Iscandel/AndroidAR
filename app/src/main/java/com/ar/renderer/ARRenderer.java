package com.ar.renderer;

import org.opencv.core.Mat;

public interface ARRenderer {
    public enum ModelType {
        FOX,
        DONA
    }

    void setIntrinsicParamsMatrix(Mat K);

    void setExtrinsicMatrix(Mat translation, Mat rotation);

    Mat render(Mat frame, Mat homography, Mat proj, int imgRefWidth, int imgRefHeight);

    void shouldDrawModel(boolean draw);

    void shouldDrawFrame(boolean draw);

    void shouldDrawBorder(boolean draw);

    boolean isDrawBorder();
    boolean isDrawFrame();
    boolean isDrawModel();

    void setModel(ModelType type);

    void setModelScale(double x, double y, double z);

    void setModelPosition(double x, double y, double z);

    void setModelRotation(double axisX, double axisY, double axisZ, double angleDegrees);


}
